package dyatel.terracontrol.level.button;

import dyatel.terracontrol.level.TurnableLevel;

public class TurnButton extends Button {

    protected TurnableLevel level;

    public TurnButton(int x, int y, int color, ButtonController controller, TurnableLevel level) {
        super(x, y, color, controller);

        this.level = level;
    }

    protected void update() {
        active = level.isTurnAvailable(color);
    }

    public void setHovering(boolean hovering) {
        this.hovering = hovering;
        if (hovering && active) level.highlightTurn(color);
    }

}
