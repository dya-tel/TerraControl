package dyatel.terracontrol.level;

import dyatel.terracontrol.Screen;
import dyatel.terracontrol.window.GameWindow;

public class AILevel extends ClientLevel {

    public AILevel(int cellSize, GameWindow window) {
        super(cellSize, window);
    }

    protected void sideUpdate() {
        // Making a turn if needed
        if (needToMakeATurn) {
            // Choosing best available
            int max = -1;
            int turn = -1;
            for (int i = 0; i < colors.length; i++) {
                int willAdd = willCapture(owner, i);
                if (willAdd > max) {
                    max = willAdd;
                    turn = i;
                }
            }

            // Making turn
            owner.addTurn(turn);
            needToMakeATurn = false;
        }
    }

    public void render(Screen screen) {
        // This level should not be rendered
    }

}
