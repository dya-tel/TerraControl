package dyatel.terracontrol.level;

import dyatel.terracontrol.util.Debug;
import dyatel.terracontrol.window.Screen;

public interface Level {

    public void update();

    public void render(Screen screen);

    public boolean canSetCell(int x, int y);

    public void setCell(Cell cell);

    public Cell getCell(int x, int y);

    public CellMaster getMaster(int x, int y);

    public void add(CellMaster u);

    public void needUpdate(Updatable u);

    public int getCellSize();

    public int getWidth();

    public int getHeight();

    public int[] getColors();

    public void changeZoom(int n);

    public Debug getDebug();

}
