package dyatel.terracontrol.level.button;

import dyatel.terracontrol.util.Color;
import dyatel.terracontrol.window.Screen;

public abstract class Button {

    protected int x, y; // Our coordinates

    protected static final int size = 16, disabledSize = 12, hoveringSize = 20; // Side length in pixels

    protected int color; // Cell color

    protected boolean active = true; // Is button enabled
    protected boolean hovering = false; // Is mouse placed on button

    public Button(int x, int y, int color, ButtonController controller) {
        this.x = x;
        this.y = y;

        this.color = color;

        controller.add(this); // Adding us to controller
    }

    protected abstract void update();

    public void render(Screen screen) {
        int s; // Button size
        if (!active) {
            s = disabledSize;
        } else {
            s = hovering ? hoveringSize : size;
        }
        int c = active ? color : Color.subtract(color, 0xaa, 0xaa, 0xaa); // Button color

        screen.render(x - s / 2, y - s / 2, x + s / 2, y + s / 2, c, false); // Rendering
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public static int getSize() {
        return size;
    }

    public static int getHoveringSize() {
        return hoveringSize;
    }

    public void setHovering(boolean hovering) {
        this.hovering = hovering;
    }

}
