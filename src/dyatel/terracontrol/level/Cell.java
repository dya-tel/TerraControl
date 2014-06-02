package dyatel.terracontrol.level;

import dyatel.terracontrol.window.Screen;

public class Cell {

    private int x, y; // Coordinates on level

    private CellMaster master; // Cell`s master

    private Level level; // Cell`s level

    public Cell(int x, int y, CellMaster master) {
        this.x = x;
        this.y = y;

        this.master = master;

        level = master.getLevel(); // Getting level
        level.setCell(this); // Adding us to level

        master.addCell(this); // Adding us to master`s list
    }

    public void render(Screen screen, int color) {
        int cellSize = level.getCellSize();
        int xp = x * (cellSize + 1); // Where to draw
        int yp = y * (cellSize + 1);
        int width = cellSize;
        int height = cellSize;

        if (master.getOwner() != null) {
            // Connecting with right and bottom cells if we have same owner
            if (level.getMaster(x + 1, y) == master) width++;
            if (level.getMaster(x, y + 1) == master) height++;
        }

        screen.render(xp, yp, xp + width, yp + height, color, true); // Rendering
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
