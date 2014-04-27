package dyatel.terracontrol.level;

import dyatel.terracontrol.Screen;

public class Cell {

    private int x, y;

    private CellMaster master;

    private Level level;

    public Cell(int x, int y, CellMaster master) {
        this.x = x;
        this.y = y;

        this.master = master;

        level = master.getLevel();
        level.setCell(this);

        master.addCell(this);
    }

    public void render(Screen screen, int color) {
        int cellSize = level.getCellSize();
        int xp = x * (cellSize + 1);
        int yp = y * (cellSize + 1);
        int width = cellSize;
        int height = cellSize;

        if (master.getOwner() != null) {
            if (level.getMaster(x + 1, y) == master) width++;
            if (level.getMaster(x, y + 1) == master) height++;
        }

        screen.render(xp, yp, xp + width, yp + height, color, true);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setMaster(CellMaster master) {
        this.master = master;
    }

    public CellMaster getMaster() {
        return master;
    }

}
