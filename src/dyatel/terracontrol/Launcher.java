package dyatel.terracontrol;

import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.Debug;
import dyatel.terracontrol.util.ErrorLogger;
import dyatel.terracontrol.window.Client;
import dyatel.terracontrol.window.GameWindow;
import dyatel.terracontrol.window.Server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;

public class Launcher extends JFrame {

    private static Launcher launcher = null;

    Launcher() {
        final Debug debug = Debug.launcherDebug;

        setResizable(false);

        setTitle("TerraControl");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new GridLayout(0, 3));

        final JTextField widthField = new JTextField("500");
        final JTextField heightField = new JTextField("300");

        final JTextField levelWidthField = new JTextField("55");
        final JTextField levelHeightField = new JTextField("28");

        final JTextField addressField = new JTextField("localhost");
        final JTextField portField = new JTextField("8192");

        final JTextField cellSizeField = new JTextField("8");

        final JCheckBox fastGenerationCheck = new JCheckBox();
        fastGenerationCheck.setHorizontalAlignment(JCheckBox.CENTER);
        fastGenerationCheck.setSelected(true);

        final JCheckBox endAt50Check = new JCheckBox();
        endAt50Check.setHorizontalAlignment(JCheckBox.CENTER);
        endAt50Check.setSelected(true);

        final JTextField colorsField = new JTextField("ff0000 00ff00 0000ff");

        final JButton single = new JButton("Single player");
        final JButton client = new JButton("Client");
        final JButton server = new JButton("Server");

        add(new JLabel("Window size"));
        add(widthField);
        add(heightField);
        add(new JLabel("Level size"));
        add(levelWidthField);
        add(levelHeightField);
        add(new JLabel("Address"));
        add(addressField);
        add(portField);

        add(new JLabel("Cell size"));
        add(new JLabel());
        add(cellSizeField);

        add(new JLabel("Fast generation"));
        add(new JLabel());
        add(fastGenerationCheck);

        add(new JLabel("End if captured 50%"));
        add(new JLabel());
        add(endAt50Check);

        add(new JLabel("Colors"));
        add(new JLabel());
        add(colorsField);

        add(single);
        add(client);
        add(server);
        pack();

        setLocationRelativeTo(null);

        single.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int width = Integer.parseInt(widthField.getText());
                    int height = Integer.parseInt(heightField.getText());
                    if (width <= 0 || height <= 0) throw new NumberFormatException();

                    // Parsing AI client data
                    DataArray AIData = new DataArray();
                    AIData.fillString("address", "localhost");
                    AIData.fillInteger("port", portField.getText());
                    AIData.fillInteger("cellSize", cellSizeField.getText());
                    AIData.fillBoolean("isAI", true);
                    AIData.fillBoolean("noGUI", true);

                    // Parsing client data
                    DataArray clientData = new DataArray();
                    clientData.fillString("address", "localhost");
                    clientData.fillInteger("port", portField.getText());
                    clientData.fillInteger("cellSize", cellSizeField.getText());
                    clientData.fillBoolean("isAI", false);
                    clientData.fillBoolean("noGUI", false);

                    // Parsing server data
                    DataArray serverData = new DataArray();
                    serverData.fillInteger("port", portField.getText());
                    serverData.fillInteger("levelWidth", levelWidthField.getText());
                    serverData.fillInteger("levelHeight", levelHeightField.getText());
                    serverData.fillInteger("cellSize", cellSizeField.getText());
                    serverData.fillBoolean("fastGeneration", fastGenerationCheck.isSelected());
                    serverData.fillBoolean("endAt50", endAt50Check.isSelected());
                    serverData.fillBoolean("noGUI", true);

                    String[] colorsR = colorsField.getText().split(" ");
                    serverData.fillInteger("colors", colorsR.length);
                    for (int i = 0; i < colorsR.length; i++) {
                        serverData.fillInteger("color" + i, Integer.parseInt(colorsR[i], 16));
                    }

                    // Creating AI client, player client and server and binding them
                    GameWindow AIWindow = new Client(width, height, AIData, null);
                    GameWindow serverWindow = new Server(width, height, serverData, AIWindow);
                    new Client(width, height, clientData, serverWindow);
                } catch (Exception ex) {
                    debug.println("Error: wrong input!");
                }
            }
        });

        client.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int width = Integer.parseInt(widthField.getText());
                    int height = Integer.parseInt(heightField.getText());
                    if (width <= 0 || height <= 0) throw new NumberFormatException();

                    // Checking if address can be parsed
                    InetAddress.getByName(addressField.getText());

                    // Putting data into data wrapper
                    DataArray data = new DataArray();
                    data.fillString("address", addressField.getText());
                    data.fillInteger("port", portField.getText());
                    data.fillInteger("cellSize", cellSizeField.getText());
                    data.fillBoolean("isAI", false);
                    data.fillBoolean("noGUI", false);

                    new Client(width, height, data, null);
                } catch (Exception ex) {
                    debug.println("Error: wrong input!");
                }
            }

        });

        server.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int width = Integer.parseInt(widthField.getText());
                    int height = Integer.parseInt(heightField.getText());
                    if (width <= 0 || height <= 0) throw new NumberFormatException();

                    // Putting data into data wrapper
                    DataArray data = new DataArray();
                    data.fillInteger("port", portField.getText());
                    data.fillInteger("levelWidth", levelWidthField.getText());
                    data.fillInteger("levelHeight", levelHeightField.getText());
                    data.fillInteger("cellSize", cellSizeField.getText());
                    data.fillBoolean("fastGeneration", fastGenerationCheck.isSelected());
                    data.fillBoolean("endAt50", endAt50Check.isSelected());
                    data.fillBoolean("noGUI", false);

                    String[] colorsR = colorsField.getText().split(" ");
                    data.fillInteger("colors", colorsR.length);
                    for (int i = 0; i < colorsR.length; i++) {
                        data.fillInteger("color" + i, Integer.parseInt(colorsR[i], 16));
                    }

                    new Server(width, height, data, null);
                } catch (Exception ex) {
                    debug.println("Error: wrong input!");
                }
            }
        });

        launcher = this;

        // Things to do on exit
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                ErrorLogger.close(); // Filling error log
            }
        });

        setVisible(true);
    }

    public static Launcher getLauncher() {
        return launcher;
    }

    public static void main(String[] args) {
        new Launcher();
    }

}
