package dyatel.terracontrol.window;

import dyatel.terracontrol.level.ClientLevel;
import dyatel.terracontrol.network.ClientConnection;
import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.Debug;

import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

public class Client extends GameWindow {

    public Client(int width, int height, DataArray data) {
        super(width, height, " client", data, Debug.clientDebug);
    }

    protected void start(DataArray data) throws Exception {
        debug.println("Starting client...");

        // Initialization goes here
        screen = new Screen(width, height);
        level = new ClientLevel(data.getInteger("cellSize"), this);
        connection = new ClientConnection(data.getString("address"), data.getInteger("port"), this);

        // Creating main loop
        thread = new Thread(this, "Client");
        running = true;
    }

    protected void update() {
        level.update();
    }

    protected void render() {
        BufferStrategy bs = getBufferStrategy();
        if (bs == null) {
            createBufferStrategy(3);
            return;
        }

        Graphics g = bs.getDrawGraphics();

        // Draw stuff here
        level.render(screen);
        screen.draw(g);

        g.setColor(Color.BLACK);
        g.setFont(font);
        String tx = "Tx: " + connection.getTransmitted();
        String rx = "Rx: " + connection.getReceived();
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        FontMetrics fm = image.getGraphics().getFontMetrics(font);
        g.drawString(statusBar[0], 2, height - 28);
        g.drawString(statusBar[1], (width - fm.stringWidth(statusBar[1])) / 2, height - 28);
        g.drawString(statusBar[2], width - fm.stringWidth(statusBar[2]) - 2, height - 28);
        g.drawString(tx, 2, height - 8);
        g.drawString(statusBar[4], (width - fm.stringWidth(statusBar[4])) / 2, height - 8);
        g.drawString(rx, width - fm.stringWidth(rx) - 2, height - 8);

        g.dispose();
        bs.show();
    }

    public ClientLevel getLevel() {
        return (ClientLevel) level;
    }

    public ClientConnection getConnection() {
        return (ClientConnection) connection;
    }

}