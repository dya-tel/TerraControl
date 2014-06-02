package dyatel.terracontrol.level.button;

import dyatel.terracontrol.window.Screen;

import java.util.ArrayList;

public class ButtonController {

    private ArrayList<Button> buttons;

    public ButtonController() {
        buttons = new ArrayList<Button>();
    }

    public void update(int x, int y) {
        for (Button button : buttons) {
            button.update(); // Updating button

            // Checking if mouse is on button
            int bx = button.getX();
            int by = button.getY();
            int size = Button.getSize();
            button.setHovering(x > bx - size / 2 && x < bx + size / 2 && y > by - size / 2 && y < by + size / 2);
        }
    }

    public void add(Button button) {
        buttons.add(button);
    }

    public void render(Screen screen) {
        for (Button button : buttons) button.render(screen);
    }

}
