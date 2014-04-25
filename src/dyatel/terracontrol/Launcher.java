package dyatel.terracontrol;

import dyatel.terracontrol.util.DataArray;
import dyatel.terracontrol.util.Debug;
import dyatel.terracontrol.window.Client;
import dyatel.terracontrol.window.Server;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;

public class Launcher extends JFrame {

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

        final JTextField colorsField = new JTextField("ff0000 00ff00 0000ff");

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

        add(new JLabel("Colors"));
        add(new JLabel());
        add(colorsField);

        add(client);
        add(new JLabel());
        add(server);
        pack();

        client.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int width = Integer.parseInt(widthField.getText());
                    int height = Integer.parseInt(heightField.getText());

                    // Checking if address can be parsed
                    InetAddress.getByName(addressField.getText());

                    // Putting data into data wrapper
                    DataArray data = new DataArray();
                    data.fillString("address", addressField.getText());
                    data.fillInteger("port", portField.getText());
                    data.fillInteger("cellSize", cellSizeField.getText());

                    new Client(width, height, data);
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

                    // Putting data into data wrapper
                    DataArray data = new DataArray();
                    data.fillInteger("port", portField.getText());
                    data.fillInteger("levelWidth", levelWidthField.getText());
                    data.fillInteger("levelHeight", levelHeightField.getText());
                    data.fillInteger("cellSize", cellSizeField.getText());
                    data.fillBoolean("fastGeneration", fastGenerationCheck.isSelected());

                    String[] colorsR = colorsField.getText().split(" ");
                    data.fillInteger("colors", colorsR.length);
                    for (int i = 0; i < colorsR.length; i++) {
                        data.fillInteger("color" + i, Integer.parseInt(colorsR[i], 16));
                    }

                    new Server(width, height, data);
                } catch (NumberFormatException ex) {
                    debug.println("Error: wrong input!");
                    ex.printStackTrace();
                }
            }
        });

        setLocationRelativeTo(null);

        setVisible(true);
    }

    public static void main(String[] args) {
        new Launcher();
    }

}
