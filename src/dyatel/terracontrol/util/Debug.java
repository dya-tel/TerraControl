package dyatel.terracontrol.util;

public class Debug {

    public static final Debug launcherDebug = new Debug("[Launcher]");
    public static final Debug spDebug = new Debug("[TerraControl]");
    public static final Debug clientDebug = new Debug("[Client]");
    public static final Debug serverDebug = new Debug("[Server]");

    private String prefix;

    private Debug(String prefix) {
        this.prefix = prefix;
    }

    public void println(String s) {
        System.out.println(prefix + " > " + s);
    }

}
