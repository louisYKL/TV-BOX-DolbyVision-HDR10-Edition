package xyz.doikki.videoplayer.player;

import android.media.MediaDataSource;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

final class HttpRangeMediaDataSource extends MediaDataSource {
    private static final String TAG = "HttpRangeDataSource";
    private static final long DEFAULT_WINDOW_SIZE = 512L * 1024L;
    private static final long MAX_WINDOW_SIZE = 2L * 1024L * 1024L;
    private static final long PROBE_WINDOW_SIZE = 256L * 1024L;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 150L;
    private static final String HEADER_PROBE_CONTAINER = "x-tvbox-probe-container";
    private static final String HEADER_PROBE_DOLBY_VISION = "x-tvbox-probe-dolbyvision";
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final String url;
    private final HashMap<String, String> headers = new HashMap<>();
    private boolean closed;
    private long totalSize = -1L;
    private long windowStart = -1L;
    private long windowEnd = -1L;
    private byte[] windowData = new byte[0];
    private int debugLoadCount;
    private static Method sRuntimeLogInfoMethod;
    private static boolean sRuntimeLogLookupDone;

    HttpRangeMediaDataSource(String url, Map<String, String> headers) {
        this.url = url;
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String key = entry.getKey().trim();
                if (isManagedRequestHeader(key)) {
                    continue;
                }
                String value = entry.getValue().trim();
                if (!value.isEmpty()) {
                    this.headers.put(key, value);
                }
            }
        }
    }

    @Override
    public synchronized int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
        if (closed) {
            throw new IOException("MediaDataSource already closed");
        }
        if (size <= 0) {
            return 0;
        }
        if (position < 0L) {
            return -1;
        }
        if (totalSize >= 0L && position >= totalSize) {
            return -1;
        }
        int totalCopied = 0;
        long cursor = position;
        int requestedSize = size;
        while (size > 0) {
            ensureWindowLoaded(cursor, size);
            if (totalSize >= 0L && cursor >= totalSize) {
                break;
            }
            if (windowData.length == 0 || cursor < windowStart || cursor > windowEnd) {
                break;
            }
            int startIndex = (int) (cursor - windowStart);
            int available = windowData.length - startIndex;
            if (available <= 0) {
                break;
            }
            int copyLength = Math.min(size, available);
            System.arraycopy(windowData, startIndex, buffer, offset, copyLength);
            offset += copyLength;
            size -= copyLength;
            totalCopied += copyLength;
            cursor += copyLength;
            if (copyLength < available) {
                break;
            }
            if (totalSize >= 0L && cursor >= totalSize) {
                break;
            }
        }
        if (debugLoadCount < 20) {
            logInfo("echo-range-source readAt pos=" + position + " size=" + requestedSize + " copied=" + totalCopied + " total=" + totalSize);
        }
        return totalCopied > 0 ? totalCopied : -1;
    }

    @Override
    public synchronized long getSize() throws IOException {
        if (closed) {
            throw new IOException("MediaDataSource already closed");
        }
        ensureSizeKnown();
        return totalSize;
    }

    @Override
    public synchronized void close() {
        closed = true;
        windowData = new byte[0];
        windowStart = -1L;
        windowEnd = -1L;
    }

    private void ensureSizeKnown() throws IOException {
        if (totalSize >= 0L) {
            return;
        }
        loadWindow(0L, PROBE_WINDOW_SIZE);
        if (totalSize < 0L) {
            loadWindow(0L, Long.MAX_VALUE);
        }
        if (totalSize < 0L) {
            throw new IOException("Unable to determine content length for " + url);
        }
    }

    private void ensureWindowLoaded(long position, int requestedSize) throws IOException {
        if (windowData.length > 0 && position >= windowStart && position <= windowEnd) {
            return;
        }
        long windowSize = Math.max(DEFAULT_WINDOW_SIZE, (long) requestedSize * 2L);
        windowSize = Math.min(windowSize, MAX_WINDOW_SIZE);
        loadWindow(position, windowSize);
    }

    private void loadWindow(long start, long minWindowSize) throws IOException {
        if (totalSize >= 0L && start >= totalSize) {
            windowData = new byte[0];
            windowStart = start;
            windowEnd = start - 1L;
            return;
        }
        long requestedWindow = Math.max(1L, minWindowSize);
        long desiredEnd = requestedWindow == Long.MAX_VALUE ? -1L : start + requestedWindow - 1L;
        if (totalSize >= 0L) {
            desiredEnd = desiredEnd >= 0L ? Math.min(desiredEnd, totalSize - 1L) : totalSize - 1L;
        }
        Response response = openRangeResponse(start, desiredEnd);
        try {
            if (response.code() == 416) {
                long unsatisfiedTotal = ContentRangeInfo.parseUnsatisfiedTotal(response.header("Content-Range"));
                if (unsatisfiedTotal > 0L) {
                    totalSize = unsatisfiedTotal;
                }
                if (totalSize >= 0L && start >= totalSize) {
                    windowData = new byte[0];
                    windowStart = start;
                    windowEnd = start - 1L;
                    return;
                }
            }
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Unexpected HTTP " + response.code() + " for " + start + "-" + desiredEnd);
            }
            ContentRangeInfo rangeInfo = ContentRangeInfo.parse(response.header("Content-Range"));
            if (rangeInfo != null && rangeInfo.total > 0L) {
                totalSize = rangeInfo.total;
            } else if (start == 0L) {
                long bodyLength = response.body().contentLength();
                if (bodyLength > 0L) {
                    totalSize = bodyLength;
                }
            }

            long payloadStart = rangeInfo != null ? rangeInfo.start : 0L;
            long payloadEnd;
            if (rangeInfo != null) {
                payloadEnd = rangeInfo.end;
            } else {
                long bodyLength = response.body().contentLength();
                payloadEnd = bodyLength > 0L ? payloadStart + bodyLength - 1L : desiredEnd;
            }
            if (payloadEnd < start) {
                throw new IOException("Invalid payload window " + payloadStart + "-" + payloadEnd + " for " + start);
            }
            if (start > 0L && rangeInfo == null) {
                throw new IOException("Server ignored range request for offset " + start);
            }
            if (rangeInfo != null && rangeInfo.start > start) {
                throw new IOException("Range response starts after requested offset: " + rangeInfo.start + " > " + start);
            }

            long skipBytes = Math.max(0L, start - payloadStart);
            long availableAfterSkip = payloadEnd - start + 1L;
            long targetLength = desiredEnd >= start ? desiredEnd - start + 1L : availableAfterSkip;
            long bytesToRead = targetLength > 0L ? Math.min(targetLength, availableAfterSkip) : availableAfterSkip;

            InputStream stream = response.body().byteStream();
            discardFully(stream, skipBytes);
            byte[] data = readUpTo(stream, bytesToRead);
            if (data.length == 0 && totalSize >= 0L && start < totalSize) {
                throw new IOException("Empty range payload before EOF at " + start + "/" + totalSize);
            }

            windowData = data;
            windowStart = start;
            windowEnd = data.length > 0 ? start + data.length - 1L : start - 1L;

            if (debugLoadCount < 12) {
                debugLoadCount++;
                logInfo("echo-range-source loadWindow start=" + start
                        + " end=" + desiredEnd
                        + " payload=" + payloadStart + "-" + payloadEnd
                        + " read=" + data.length
                        + " total=" + totalSize
                        + " code=" + response.code());
            }
        } finally {
            response.close();
        }
    }

    private Response openRangeResponse(long start, long end) throws IOException {
        IOException lastError = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Request.Builder builder = new Request.Builder()
                        .url(url)
                        .header("Connection", "close")
                        .header("Accept-Encoding", "identity");
                builder.header("Range", end >= start ? "bytes=" + start + "-" + end : "bytes=" + start + "-");
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    if (!TextUtils.isEmpty(entry.getKey())
                            && entry.getValue() != null
                            && !isManagedRequestHeader(entry.getKey())) {
                        builder.header(entry.getKey(), entry.getValue());
                    }
                }
                return CLIENT.newCall(builder.build()).execute();
            } catch (IOException e) {
                lastError = e;
                logInfo("echo-range-source retry " + attempt + "/" + MAX_RETRIES
                        + " start=" + start + " end=" + end + " err=" + e.getMessage());
                if (attempt >= MAX_RETRIES) {
                    break;
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw lastError == null ? new IOException("Unknown range request failure") : lastError;
    }

    private static void logInfo(String message) {
        Log.i(TAG, message);
        writeRuntimeLog(message);
    }

    private static void writeRuntimeLog(String message) {
        try {
            Method method = getRuntimeLogInfoMethod();
            if (method != null) {
                method.invoke(null, message);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Method getRuntimeLogInfoMethod() {
        if (sRuntimeLogLookupDone) {
            return sRuntimeLogInfoMethod;
        }
        synchronized (HttpRangeMediaDataSource.class) {
            if (sRuntimeLogLookupDone) {
                return sRuntimeLogInfoMethod;
            }
            try {
                Class<?> logClass = Class.forName("com.github.tvbox.osc.util.LOG");
                sRuntimeLogInfoMethod = logClass.getMethod("i", String.class);
            } catch (Throwable ignored) {
                sRuntimeLogInfoMethod = null;
            }
            sRuntimeLogLookupDone = true;
            return sRuntimeLogInfoMethod;
        }
    }

    private static void discardFully(InputStream stream, long bytesToSkip) throws IOException {
        long remaining = bytesToSkip;
        byte[] scratch = new byte[8192];
        while (remaining > 0L) {
            int read = stream.read(scratch, 0, (int) Math.min(scratch.length, remaining));
            if (read < 0) {
                throw new IOException("Unexpected EOF while skipping " + bytesToSkip + " bytes");
            }
            remaining -= read;
        }
    }

    private static byte[] readUpTo(InputStream stream, long maxBytes) throws IOException {
        if (maxBytes <= 0L) {
            return new byte[0];
        }
        if (maxBytes > Integer.MAX_VALUE) {
            throw new IOException("Requested window too large: " + maxBytes);
        }
        byte[] buffer = new byte[(int) maxBytes];
        int total = 0;
        while (total < buffer.length) {
            int read = stream.read(buffer, total, buffer.length - total);
            if (read < 0) {
                break;
            }
            total += read;
        }
        return total == buffer.length ? buffer : Arrays.copyOf(buffer, total);
    }

    private static boolean isManagedRequestHeader(String key) {
        if (TextUtils.isEmpty(key)) {
            return true;
        }
        String lower = key.trim().toLowerCase();
        return "range".equals(lower)
                || "accept-ranges".equals(lower)
                || "content-range".equals(lower)
                || "content-length".equals(lower)
                || "connection".equals(lower)
                || "accept-encoding".equals(lower)
                || "host".equals(lower)
                || HEADER_PROBE_CONTAINER.equals(lower)
                || HEADER_PROBE_DOLBY_VISION.equals(lower);
    }

    private static final class ContentRangeInfo {
        private final long start;
        private final long end;
        private final long total;

        private ContentRangeInfo(long start, long end, long total) {
            this.start = start;
            this.end = end;
            this.total = total;
        }

        private static ContentRangeInfo parse(String headerValue) {
            if (TextUtils.isEmpty(headerValue)) {
                return null;
            }
            try {
                String value = headerValue.trim();
                if (!value.startsWith("bytes")) {
                    return null;
                }
                String[] rangeAndTotal = value.substring("bytes".length()).trim().split("/", 2);
                if (rangeAndTotal.length != 2) {
                    return null;
                }
                String[] startEnd = rangeAndTotal[0].trim().split("-", 2);
                if (startEnd.length != 2) {
                    return null;
                }
                long start = Long.parseLong(startEnd[0].trim());
                long end = Long.parseLong(startEnd[1].trim());
                long total = "*".equals(rangeAndTotal[1].trim()) ? -1L : Long.parseLong(rangeAndTotal[1].trim());
                if (start < 0L || end < start) {
                    return null;
                }
                return new ContentRangeInfo(start, end, total);
            } catch (Throwable ignored) {
                return null;
            }
        }

        private static long parseUnsatisfiedTotal(String headerValue) {
            if (TextUtils.isEmpty(headerValue)) {
                return -1L;
            }
            try {
                String value = headerValue.trim();
                if (!value.startsWith("bytes")) {
                    return -1L;
                }
                int slash = value.lastIndexOf('/');
                if (slash < 0 || slash + 1 >= value.length()) {
                    return -1L;
                }
                String total = value.substring(slash + 1).trim();
                if ("*".equals(total)) {
                    return -1L;
                }
                return Long.parseLong(total);
            } catch (Throwable ignored) {
                return -1L;
            }
        }
    }
}
