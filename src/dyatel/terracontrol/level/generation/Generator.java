package dyatel.terracontrol.level.generation;

import dyatel.terracontrol.level.Cell;
import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.util.Util;

import java.util.ArrayList;
import java.util.Random;

public abstract class Generator {

    protected Random random = Util.getRandom(); // Randomizer

    protected GeneratableLevel level; // Level that we are generating

    protected long genStart = -1; // System time at the moment of first generate() call

    public static final String[] types = {"Fill", "Point"}; // All available generators

    public Generator(GeneratableLevel level) {
        this.level = level;
        level.getDebug().println("Using \"" + getName() + "\" generator");
    }

    public abstract void generate(Cell[] cells);

    protected void onLevelGenerated() {
        ArrayList<CellMaster> masters = level.getMasters();
        for (int i = 0; i < masters.size(); i++) masters.get(i).setID(i); // Setting IDs to masters
        level.onLevelGenerated(); // Saying level that we are done
    }

    public int getGeneratedPercent() {
        int generated = 0;
        for (CellMaster master : level.getMasters()) generated += master.getCells().size();
        return generated * 100 / (level.getWidth() * level.getHeight());
    }

    public boolean isGenerated() {
        int width = level.getWidth();
        for (int i = 0; i < width * level.getHeight(); i++) {
            if (level.getCell(i % width, i / width) == null) return false; // Not generated if there is null cell
        }
        return true;
    }

    public abstract String getName();

    public static Generator parseGenerator(String string, GeneratableLevel level) {
        // Finding generator
        if (string.equals("Fill")) return new FillGenerator(level);
        if (string.equals("Point")) return new PointGenerator(level);
        return null; // If found nothing
    }

}
