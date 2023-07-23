package brut.androlib.apk;

import java.util.Objects;

public class YamlLine {

    public int indent = 0;
    private String key = "";
    private String value = "";
    public boolean isComment;
    public boolean isEmpty;
    public boolean hasColon;
    public boolean isNull;
    public boolean isItem;

    public YamlLine(String line) {
        // special end line marker
        isNull = Objects.isNull(line);
        if (isNull) {
            return;
        }
        isEmpty = line.trim().isEmpty();
        if (isEmpty) {
            return;
        }
        // count indent - space only
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ') {
                indent++;
            } else {
                break;
            }
        }
        // remove whitespace
        line = line.trim();
        char first = line.charAt(0);

        isComment = first == '#' || first == '!';
        isItem = first == '-';
        if (isComment) {
            // for comment fill value
            value = line.substring(1).trim();
        } else {
            // value line
            hasColon = line.contains(":");
            if (isItem) {
                // array item line has only the value
                value = line.substring(1).trim();
            }  else {
                // split line to key - value
                String[] parts = line.split(":");
                if (parts.length > 0) {
                    key = parts[0].trim();
                    if (parts.length > 1) {
                        value = parts[1].trim();
                    }
                }
            }
        }
    }

    public static String unescape(String value) {
        return YamlStringEscapeUtils.unescapeString(value);
    }

    public String getValue() {
        if (value.equals("null"))
            return null;
        String res = unescape(value);
        // remove quotation marks
        res = res.replaceAll("^\"|\"$", "");
        res = res.replaceAll("^'|'$", "");
        return res;
    }

    public String getKey() {
        String res = unescape(key);
        // remove quotation marks
        res = res.replaceAll("^\"|\"$", "");
        res = res.replaceAll("^'|'$", "");
        return res;
    }

    public boolean getValueBool() {
        return Objects.equals(value, "true");
    }

    public int getValueInt() {
        return Integer.parseInt(value);
    }
}
