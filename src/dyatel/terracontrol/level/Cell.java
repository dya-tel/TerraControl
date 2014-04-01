package dyatel.terracontrol.level;

import dyatel.terracontrol.Screen;
import dyatel.terracontrol.Server;

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
        int x = this.x * (cellSize + 1);
        int y = this.y * (cellSize + 1);
        int width = cellSize;
        int height = cellSize;

        if (master.getOwner() != null) {
            Cell cell = level.getCell(this.x + 1, this.y);
            if (cell != null && cell.getMaster() == master) {
                width++;
            }
            cell = level.getCell(this.x, this.y + 1);
            if (cell != null && cell.getMaster() == master) {
                height++;
            }
        }

        screen.render(x, y, x + width, y + height, color, true);
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
