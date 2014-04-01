package dyatel.terracontrol.util;

public class Debug {

    public static Debug launcherDebug = new Debug("[Launcher]");
    public static Debug clientDebug = new Debug("[Client]");
    public static Debug serverDebug = new Debug("[Server]");

    private String prefix;

    private Debug(String prefix) {
        this.prefix = prefix;
    }

    public void print(String s) {
        System.out.print(prefix + " > " + s);
    }

    public void println() {
        System.out.println();
    }

    public void println(String s) {
        System.out.println(prefix + " > " + s);
    }

    public void println(int n) {
        System.out.println(String.valueOf(n));
    }

}
