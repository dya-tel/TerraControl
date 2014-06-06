package dyatel.terracontrol.level.generation;

import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.util.Debug;

import java.util.ArrayList;

public class PointGenerator extends Generator {

    public PointGenerator(GeneratableLevel level) {
        super(level);

        Debug debug = level.getDebug();

        // Adding masters
        int width = level.getWidth();
        int height = level.getHeight();
        int minMasters = width * height * 2 / 5;
        int maxMasters = width * height * 4 / 5;
        int masters = random.nextInt(maxMasters - minMasters + 1) + minMasters;
        debug.println("Going to add " + masters + " masters");
        for (int i = 0; i < masters; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            if (level.canSetCell(x, y)) {
                new Cell(x, y, new CellMaster(level));
            }
        }
        debug.println("Added " + level.getMasters().size() + " masters");
    }

    protected void gen() {
        // Generating every master
        for (CellMaster master : level.getMasters()) {
            ArrayList<Cell> borders = master.getBorderCells();
            for (Cell cell : borders) {
                // Determining where to try putting new cell
                int x = cell.getX() + random.nextInt(3) - 1; // 0,1,2 - 1 = -1, 0, 1
                int y = cell.getY() + random.nextInt(3) - 1;
                if (x != cell.getX() && y != cell.getY()) continue; // Preventing diagonal generation
                if (level.canSetCell(x, y)) new Cell(x, y, master);
            }
        }
    }

    public String getName() {
        return "Point";
    }

}
