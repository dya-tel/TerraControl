package dyatel.terracontrol.util;

public class Color {

    public static int subtract(int color, int red, int green, int blue) {
        int r = (color >> 16) & 255;
        int g = (color >> 8) & 255;
        int b = color & 255;

        r = Math.max(r - red, 0);
        g = Math.max(g - green, 0);
        b = Math.max(b - blue, 0);

        return (r << 16) + (g << 8) + b;
    }

}
