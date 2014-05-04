package dyatel.terracontrol.level.generation;

import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.util.Util;

import java.util.ArrayList;
import java.util.Random;

public abstract class Generator {

    protected Random random = Util.getRandom();

    protected GeneratableLevel level;

    protected long genStart = -1; // System time at the moment of first generate() call

    public Generator(GeneratableLevel level) {
        this.level = level;
    }

    public abstract void generate(Cell[] cells);

    protected void onLevelGenerated() {
        ArrayList<CellMaster> masters = level.getMasters();
        for (int i = 0; i < masters.size(); i++) masters.get(i).setID(i);
        level.onLevelGenerated();
    }

    public int getGeneratedPercent() {
        int generated = 0;
        for (CellMaster master : level.getMasters()) generated += master.getCells().size();
        return generated * 100 / (level.getWidth() * level.getHeight());
    }

    public boolean isGenerated() {
        int width = level.getWidth();
        for (int i = 0; i < width * level.getHeight(); i++) {
            if (level.getCell(i % width, i / width) == null) return false;
        }
        return true;
    }

    public boolean check() {
        int tempC = 0;
        for (CellMaster master : level.getMasters()) {
            tempC += master.getCells().size();
        }
        return tempC == level.getWidth() * level.getHeight();
    }

}
