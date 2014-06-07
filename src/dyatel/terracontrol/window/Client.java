package dyatel.terracontrol.window;

import dyatel.terracontrol.level.ClientLevel;
import dyatel.terracontrol.network.ClientConnection;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.Debug;

public class Client extends GameWindow {

    public Client(int width, int height, DataArray data) {
        super(width, height, " client", data, Debug.clientDebug);
    }

    protected void start(DataArray data) throws Exception {
        debug.println("Starting client...");

        // Initialization goes here
        screen = new Screen(width, height);
        level = new ClientLevel(this);
        connection = new ClientConnection(data.getString("address"), data.getInteger("port"), this);

        // Creating main loop
        thread = new Thread(this, "Client");
        running = true;
    }

    protected void update() {
        level.update();
    }

    public ClientLevel getLevel() {
        return (ClientLevel) level;
    }

    public ClientConnection getConnection() {
        return (ClientConnection) connection;
    }

}