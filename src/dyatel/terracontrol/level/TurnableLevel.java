package dyatel.terracontrol.level;

public interface TurnableLevel {

    public boolean isTurnAvailable(int color);

    public void highlightTurn(int color);

}
