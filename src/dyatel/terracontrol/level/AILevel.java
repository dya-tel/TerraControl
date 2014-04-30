package dyatel.terracontrol.level;

import dyatel.terracontrol.Screen;
import dyatel.terracontrol.util.Util;
import dyatel.terracontrol.window.GameWindow;

public class AILevel extends ClientLevel {

    public AILevel(int cellSize, GameWindow window) {
        super(cellSize, window);
    }

    protected void sideUpdate() {
        // Making a turn if needed
        if (needToMakeATurn) {
            int turn = Util.getRandom().nextInt(colors.length);
            owner.addTurn(turn);
            needToMakeATurn = false;
        }
    }

    public void render(Screen screen) {
        // This level should not be rendered
    }

}
