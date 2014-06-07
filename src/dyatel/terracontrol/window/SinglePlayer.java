package dyatel.terracontrol.window;

import dyatel.terracontrol.level.SPLevel;
import dyatel.terracontrol.network.Connection;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.Debug;

public class SinglePlayer extends GameWindow {

    public SinglePlayer(int width, int height, DataArray data) {
        super(width, height, "", data, Debug.spDebug);
    }

    protected void start(DataArray data) throws Exception {
        debug.println("Starting...");

        // Initialization goes here
        screen = new Screen(width, height);
        level = new SPLevel(data, this);

        // Creating main loop
        thread = new Thread(this, "TerraControl");
        running = true;
    }

    protected void update() {
        level.update();
    }

    public SPLevel getLevel() {
        return (SPLevel) level;
    }

    public Connection getConnection() {
        return null;
    }

}
