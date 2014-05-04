package dyatel.terracontrol.level.generation;

import dyatel.terracontrol.level.CellMaster;
import dyatel.terracontrol.level.Level;

import java.util.ArrayList;

public interface GeneratableLevel extends Level {

    public ArrayList<CellMaster> getMasters();

    public void onLevelGenerated();

}
