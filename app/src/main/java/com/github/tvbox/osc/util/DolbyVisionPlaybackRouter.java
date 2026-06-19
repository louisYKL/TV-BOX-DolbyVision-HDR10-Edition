package com.github.tvbox.osc.util;

import android.content.Context;

import java.util.Map;

/**
 * 三路由策略（依据真实探测的设备能力，见 {@link HdrDeviceSupport}）：
 *  路由1：DV 源 + 设备无 DV 解码器 → 兼容播放器(MPV) 把 DV 映射成 HDR10/HDR10+（显示端不支持 HDR 时降级 SDR），并激发 HDR。
 *  路由2：DV 源 + 设备有 DV 解码器且显示端支持 DV → 系统播放器走原生解码器，激发杜比视界。
 *  路由3：普通 SDR/HDR10/HDR10+ 的 MKV/MP4 → 系统播放器原生硬解（最强性能），由设备自身解码器激活 HDR。
 *
 * 关键纠错：HDR10/HDR10+/DV 只能由视频流探测结果触发，禁止根据标题、文件名里的 DV/HDR/10Bit 字样判断。
 * 容器类型（MKV/WebM）可以根据 URL/容器探测决定播放器兼容链，但不参与 HDR/DV 类型判断。
 * 在无 DV 解码器的设备上，真实探测到的 DV 源绝不能交给系统播放器。
 *
 * 双层 DV / Profile 8 纠错：这类流带 HDR10 基础层时，不做重映射；直接用硬解播放 HDR10 基础层并请求 HDR 输出。
 * 只有 Profile 5 或探测不到 HDR10 基础层的 DV 才走兼容映射链。
 */
public final class DolbyVisionPlaybackRouter {
    private DolbyVisionPlaybackRouter() {
    }

    public static final class Decision {
        public final boolean looksLikeDolbyVision;
        public final boolean useCompatPlayer;
        public final boolean preferHdrFallback;
        public final boolean preferSdrFallback;
        public final boolean needsBuiltInMapping;
        public final String compatMode;
        public final boolean requiresHdrOutput;
        public final int playerType;
        public final String reason;

        private Decision(boolean looksLikeDolbyVision,
                         boolean useCompatPlayer,
                         boolean preferHdrFallback,
                         boolean preferSdrFallback,
                         boolean needsBuiltInMapping,
                         String compatMode,
                         boolean requiresHdrOutput,
                         int playerType,
                         String reason) {
            this.looksLikeDolbyVision = looksLikeDolbyVision;
            this.useCompatPlayer = useCompatPlayer;
            this.preferHdrFallback = preferHdrFallback;
            this.preferSdrFallback = preferSdrFallback;
            this.needsBuiltInMapping = needsBuiltInMapping;
            this.compatMode = compatMode == null ? "" : compatMode;
            this.requiresHdrOutput = requiresHdrOutput;
            this.playerType = playerType;
            this.reason = reason;
        }
    }

    public static Decision resolve(Context context, int requestedPlayerType, String url, Map<String, String> headers, String... extraHints) {
        VideoStreamProbe.Result streamProbe = VideoStreamProbe.probe(context, url, headers);
        return resolve(context, requestedPlayerType, url, headers, streamProbe, extraHints);
    }

    public static Decision resolve(Context context,
                                   int requestedPlayerType,
                                   String url,
                                   Map<String, String> headers,
                                   VideoStreamProbe.Result streamProbe,
                                   String... extraHints) {
        if (streamProbe == null) {
            streamProbe = VideoStreamProbe.Result.unknown("preprobe-null");
        }
        HdrDeviceSupport.Capabilities caps = HdrDeviceSupport.query(context);

        boolean streamDetectedDv = streamProbe.probed && streamProbe.hasDolbyVision;
        boolean matroskaLike = isMatroskaLike(url, extraHints) || streamProbe.isMatroska;

        // HDR/DV 只能由视频流探测结果决定，禁止根据标题/文件名里的 DV/HDR/10Bit 字样判断。
        boolean looksLikeDolbyVision = streamDetectedDv;

        boolean requiresHdrOutput = streamProbe.hasDolbyVision
                || streamProbe.hasHdr10
                || streamProbe.hasHdr10Plus;

        // 地面真相：本设备固件 extractor(FF_Extractor/OMX.NVT) 打不开 HTTP 上的 HEVC MKV/WebM，
        // 一律 av_open_input_file fail(-22)；而 MPV 自带 ffmpeg 能正常硬解。故 MKV/WebM 必须走 MPV，
        // MP4/TS 等才交系统播放器（原生硬解+原生 HDR 激活，性能最佳）。
        boolean systemCanOpenContainer = !matroskaLike;

        if (looksLikeDolbyVision) {
            // 路由2：设备能端到端原生 DV 且容器系统播放器能打开 → 系统播放器原生杜比视界。
            if (caps.supportsNativeDolbyVision() && systemCanOpenContainer) {
                LOG.i("echo-dolby-route route=native-dv player=" + PlayerHelper.PLAYER_TYPE_SYSTEM
                        + " caps=" + caps.summary
                        + " streamDv=" + streamDetectedDv
                        + " probe=" + streamProbe.summary + " url=" + safeSnippet(url));
                return new Decision(true, false, false, false, false, "", true,
                        PlayerHelper.PLAYER_TYPE_SYSTEM, "native-dolby-vision");
            }

            // 双层/Profile 7/8/DV+HDR：只播放 HDR10 基础层，避免在低功耗电视上做 GPU/CPU 重映射。
            boolean canUseHdr10BaseLayer = streamProbe.hasHdr10BaseLayer
                    || (streamProbe.hasDolbyVision && streamProbe.hasHdr10)
                    || (matroskaLike && streamProbe.dolbyVisionProfile < 0)
                    || streamProbe.dolbyVisionProfile == 7
                    || streamProbe.dolbyVisionProfile == 8;
            if (canUseHdr10BaseLayer && caps.displaySupportsHdr() && caps.hevcMain10Decoder) {
                LOG.i("echo-dolby-route route=dv-base-hdr10 player=" + PlayerHelper.PLAYER_TYPE_DOLBY_VISION_COMPAT
                        + " caps=" + caps.summary
                        + " streamDv=" + streamDetectedDv
                        + " profile=" + streamProbe.dolbyVisionProfile
                        + " hdr10Base=" + streamProbe.hasHdr10BaseLayer
                        + " matroska=" + matroskaLike + " probe=" + streamProbe.summary
                        + " url=" + safeSnippet(url));
                return new Decision(true, true, true, false, false, "dv-base-hdr", true,
                        PlayerHelper.PLAYER_TYPE_DOLBY_VISION_COMPAT,
                        "dv-hdr10-base-layer");
            }

            // Profile 5 或无 HDR10 基础层 → 兼容播放器(MPV/libplacebo)做映射。
            // 显示端不支持 HDR → 降级 SDR。
            boolean preferHdr = caps.displaySupportsHdr();
            String reason = (matroskaLike ? "dv-matroska-" : "dv-")
                    + (preferHdr ? "map-hdr" : "map-sdr")
                    + (caps.supportsNativeDolbyVision() ? "-mkv-no-system" : "-no-dv-decoder");
            LOG.i("echo-dolby-route route=dv-map player=" + PlayerHelper.PLAYER_TYPE_DOLBY_VISION_COMPAT
                    + " caps=" + caps.summary + " preferHdr=" + preferHdr
                    + " streamDv=" + streamDetectedDv
                    + " profile=" + streamProbe.dolbyVisionProfile
                    + " hdr10Base=" + streamProbe.hasHdr10BaseLayer
                    + " matroska=" + matroskaLike + " probe=" + streamProbe.summary
                    + " url=" + safeSnippet(url));
            return new Decision(true, true, preferHdr, !preferHdr, true, preferHdr ? "map-hdr" : "map-sdr", preferHdr,
                    PlayerHelper.PLAYER_TYPE_DOLBY_VISION_COMPAT, reason);
        }

        // 路由3：普通 SDR/HDR10/HDR10+ 且容器系统播放器能打开（MP4/TS）→ 系统播放器原生硬解 + 原生 HDR。
        if (systemCanOpenContainer) {
            LOG.i("echo-dolby-route route=system-native player=" + PlayerHelper.PLAYER_TYPE_SYSTEM
                    + " caps=" + caps.summary
                    + " hdr10=" + streamProbe.hasHdr10 + " hdr10Plus=" + streamProbe.hasHdr10Plus
                    + " matroska=" + matroskaLike
                    + " requiresHdr=" + requiresHdrOutput + " probe=" + streamProbe.summary
                    + " url=" + safeSnippet(url));
            return new Decision(false, false, false, false, false, "", requiresHdrOutput,
                    requestedPlayerType, "system-native-hdr");
        }

        // 路由4：普通 MKV/WebM（系统播放器打不开）→ 兼容播放器(MPV)。
        // HDR10/HDR10+ 源在 MPV 映射并激发 HDR；SDR 源直通。
        boolean mkvPreferHdr = (streamProbe.hasHdr10 || streamProbe.hasHdr10Plus)
                && caps.displaySupportsHdr();
        LOG.i("echo-dolby-route route=mkv-compat player=" + PlayerHelper.PLAYER_TYPE_DOLBY_VISION_COMPAT
                + " caps=" + caps.summary + " preferHdr=" + mkvPreferHdr
                + " hdr10=" + streamProbe.hasHdr10 + " hdr10Plus=" + streamProbe.hasHdr10Plus
                + " matroska=" + matroskaLike
                + " probe=" + streamProbe.summary + " url=" + safeSnippet(url));
        return new Decision(false, true, mkvPreferHdr, !mkvPreferHdr, false,
                mkvPreferHdr ? "base-hdr" : "sdr", mkvPreferHdr,
                PlayerHelper.PLAYER_TYPE_DOLBY_VISION_COMPAT,
                mkvPreferHdr ? "matroska-compat-hdr" : "matroska-compat-sdr");
    }

    private static boolean isMatroskaLike(String url, String... extraHints) {
        if (containsMatroskaMarker(url)) {
            return true;
        }
        if (extraHints != null) {
            for (String hint : extraHints) {
                if (containsMatroskaMarker(hint)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsMatroskaMarker(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String lower = value.toLowerCase();
        return lower.contains(".mkv") || lower.contains(".webm");
    }

    private static String safeSnippet(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 220 ? value.substring(0, 220) : value;
    }
}
