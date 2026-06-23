package com.github.tvbox.osc.subtitle.format;

import com.github.tvbox.osc.subtitle.exception.FatalParsingException;
import com.github.tvbox.osc.subtitle.model.Subtitle;
import com.github.tvbox.osc.subtitle.model.Time;
import com.github.tvbox.osc.subtitle.model.TimedTextObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class FormatVTT implements TimedTextFileFormat {

    @Override
    public TimedTextObject parseFile(String fileName, InputStream is) throws IOException, FatalParsingException {
        TimedTextObject tto = new TimedTextObject();
        tto.fileName = fileName;
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        int index = 1;

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("WEBVTT") || line.startsWith("NOTE")) {
                continue;
            }
            if (!line.contains("-->")) {
                String next = br.readLine();
                if (next == null) {
                    break;
                }
                line = next.trim();
            }
            if (!line.contains("-->")) {
                continue;
            }

            String[] times = line.split("-->");
            if (times.length < 2) {
                continue;
            }
            String start = normalizeTime(times[0].trim());
            String end = normalizeTime(times[1].trim().split("\\s+")[0]);

            ArrayList<String> textLines = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    break;
                }
                textLines.add(line);
            }
            if (textLines.isEmpty()) {
                continue;
            }

            Subtitle subtitle = new Subtitle();
            subtitle.start = new Time("hh:mm:ss,ms", start);
            subtitle.end = new Time("hh:mm:ss,ms", end);
            subtitle.content = joinLines(textLines);
            tto.captions.put(index++, subtitle);
        }
        tto.built = true;
        return tto;
    }

    @Override
    public String[] toFile(TimedTextObject tto) {
        return null;
    }

    private String joinLines(ArrayList<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                builder.append("\n");
            }
            builder.append(lines.get(i));
        }
        return builder.toString();
    }

    private String normalizeTime(String value) throws FatalParsingException {
        String time = value.replace('.', ',').trim();
        if (time.length() == 9) {
            time = "00:" + time;
        }
        if (time.length() != 12) {
            throw new FatalParsingException("Invalid VTT timestamp: " + value);
        }
        return time;
    }
}
