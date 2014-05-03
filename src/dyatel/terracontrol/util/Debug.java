package dyatel.terracontrol.util;

public class Debug {

    public static Debug launcherDebug = new Debug("[Launcher]");
    public static Debug spDebug = new Debug("[TerraControl]");
    public static Debug clientDebug = new Debug("[Client]");
    public static Debug serverDebug = new Debug("[Server]");

    private String prefix;

    private Debug(String prefix) {
        this.prefix = prefix;
    }

    public void println(String s) {
        System.out.println(prefix + " > " + s);
    }

}
