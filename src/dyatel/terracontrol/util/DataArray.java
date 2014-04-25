package dyatel.terracontrol.util;

import java.util.HashMap;

public class DataArray {

    private HashMap<String, Boolean> booleans;
    private HashMap<String, Integer> integers;
    private HashMap<String, String> strings;

    public DataArray() {
        booleans = new HashMap<String, Boolean>();
        integers = new HashMap<String, Integer>();
        strings = new HashMap<String, String>();
    }

    public void fillBoolean(String key, boolean data) {
        booleans.put(key, data);
    }

    public void fillInteger(String key, int data) {
        integers.put(key, data);
    }

    public void fillInteger(String key, String data) throws NumberFormatException {
        integers.put(key, Integer.parseInt(data));
    }

    public void fillString(String key, String data) {
        strings.put(key, data);
    }

    public boolean getBoolean(String key) {
        return booleans.get(key);
    }

    public int getInteger(String key) {
        return integers.get(key);
    }

    public String getString(String key) {
        return strings.get(key);
    }

}
