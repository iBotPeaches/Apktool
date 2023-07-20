package brut.androlib.apk;

import brut.androlib.exceptions.AndrolibException;

import java.io.InputStream;
import java.util.*;

public class YamlReader {

    private ArrayList<YamlLine> mLines;
    private int mCurrent = 0;

    public YamlReader(InputStream in) {
        mLines = new ArrayList<>();
        mLines.add(new YamlLine(null));
        read(in);
    }

    public int getLineNo() {
        return mCurrent + 1;
    }

    public void pushLine() {
        if (mCurrent > 0)
            mCurrent--;
    }

    public void read(InputStream in) {
        Scanner scanner = new Scanner(in);
        mLines = new ArrayList<>();
        while (scanner.hasNextLine()) {
            mLines.add(new YamlLine(scanner.nextLine()));
        }
        mLines.add(new YamlLine(null));
    }

    public YamlLine getLine() {
        return mLines.get(mCurrent);
    }

    public int getIndent() {
        return getLine().indent;
    }

    public boolean isEnd() {
        return getLine().isNull;
    }

    public boolean isCommentOrEmpty() {
        YamlLine line = getLine();
        return line.isEmpty || line.isComment;
    }

    public void skipInsignificant() {
        if (isEnd())
            return;
        while (isCommentOrEmpty()) {
            mCurrent++;
            if (isEnd())
                break;
        }
    }

    public boolean nextLine() {
        if (isEnd())
            return false;
        while (true) {
            mCurrent++;
            // skip comments
            if (isCommentOrEmpty())
                continue;
            return !isEnd();
        }
    }

    interface Checker {
        boolean check(YamlLine line);
    }

    interface Updater<T> {
        void update(T items, YamlReader reader) throws AndrolibException;
    }

    /**
    * Read root object from start to end
    */
    public <T extends YamlSerializable> T readRoot(T obj) throws AndrolibException {
        if (isEnd())
            return obj;
        int objIndent = 0;
        skipInsignificant();
        while (true) {
            if (isEnd())
                return obj;
            YamlLine line = getLine();
            // skip don't checked line or lines with other indent
            if (objIndent != line.indent || !line.hasColon) {
                nextLine();
                continue;
            }
            obj.readItem(this);
            nextLine();
        }
    }

    /**
     * Read object. Reader stand on the object name.
     * The object data should be placed on the next line
     * and have indent.
     */
    public <T> T readObject(T obj,
                            Checker check,
                            Updater<T> updater) throws AndrolibException {
        if (isEnd())
            return obj;
        int prevIndent = getIndent();
        // detect indent for the object data
        nextLine();
        YamlLine line = getLine();
        int objIndent = line.indent;
        // object data must have indent
        // otherwise stop reading
        if (objIndent <= prevIndent || !check.check(line)) {
            pushLine();
            return obj;
        }
        updater.update(obj, this);
        while (nextLine()) {
            if (isEnd())
                return obj;
            line = getLine();
            if (objIndent != line.indent || !check.check(line)) {
                pushLine();
                return obj;
            }
            updater.update(obj, this);
        }
        return obj;
    }

    <T extends YamlSerializable> T readObject(T obj) throws AndrolibException {
        return readObject(obj,
            line -> line.hasColon,
            YamlSerializable::readItem);
    }

    /**
     * Read list. Reader stand on the object name.
     * The list data should be placed on the next line.
     * Data should have same indent. May by same with name.
     */
    public <T> List<T> readList(List<T> list,
                                Updater<List<T>> updater) throws AndrolibException {
        if (isEnd())
            return list;
        int listIndent = getIndent();
        nextLine();
        int dataIndent = getIndent();
        while (true) {
            if (isEnd())
                return list;
            // check incorrect data indent
            if (dataIndent < listIndent) {
                pushLine();
                return list;
            }
            YamlLine line = getLine();
            if (dataIndent != line.indent || !line.isItem) {
                pushLine();
                return list;
            }
            updater.update(list, this);
            nextLine();
        }
    }

    public List<String> readStringList() throws AndrolibException {
        List<String> list = new ArrayList<>();
        return readList(list,
            (items, reader) -> {
                items.add(reader.getLine().getValueString());
            });
    };

    public List<Integer> readIntList() throws AndrolibException {
        List<Integer> list = new ArrayList<>();
        return readList(list,
            (items, reader) -> {
                items.add(reader.getLine().getValueInt());
            });
    };

    public Map<String, String> readMap() throws AndrolibException {
        Map<String, String> map = new LinkedHashMap<>();
        return readObject(map,
            line -> line.hasColon,
            (items, reader) -> {
                YamlLine line = reader.getLine();
                items.put(line.getKeyString(), line.getValueString());
            });
    };
}
