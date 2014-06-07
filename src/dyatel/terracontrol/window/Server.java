package dyatel.terracontrol.window;

import dyatel.terracontrol.level.ServerLevel;
import dyatel.terracontrol.network.ServerConnection;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.Debug;

public class Server extends GameWindow {

    public Server(int width, int height, DataArray data) {
        super(width, height, " server", data, Debug.serverDebug);
    }

    protected void start(DataArray data) throws Exception {
        debug.println("Starting server...");

        // Initialization goes here
        screen = new Screen(width, height);
        level = new ServerLevel(data, this);
        connection = new ServerConnection(data.getInteger("port"), this);

        // Starting main loop
        thread = new Thread(this, "Server");
        running = true;
    }

    protected void update() {
        level.update();
    }

    public ServerLevel getLevel() {
        return (ServerLevel) level;
    }

    public ServerConnection getConnection() {
        return (ServerConnection) connection;
    }

}
