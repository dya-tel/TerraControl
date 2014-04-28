package dyatel.terracontrol.util;

import dyatel.terracontrol.Launcher;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ErrorLogger {

    private static boolean canAdd = true; // We can add more exceptions if true

    private static ArrayList<Exception> errors = new ArrayList<Exception>();

    public synchronized static void add(Exception e) {
        if (canAdd) {
            // Checking if already have same exception
            for (Exception err : errors) if (err.toString().equals(e.toString())) return;

            // Creating error dialog
            String message = "Something went wrong!\nClose your launcher and check \"errors\" directory!";
            JOptionPane.showMessageDialog(Launcher.getLauncher(), message, "TerraControl Error", JOptionPane.ERROR_MESSAGE);

            // Adding exception to log
            errors.add(e);
        }
    }

    public static void close() {
        if (errors.size() == 0) return;

        canAdd = false; // Locking log
        try {
            String date = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
            File log;

            // Making directory and deciding where to put log
            File dir = new File("errors");
            if (!dir.exists() && !dir.mkdir()) {
                System.err.println("Can`t create \"errors\" directory!");
                log = new File("errorlog_" + date + ".txt");
            } else {
                log = new File("errors/errorlog_" + date + ".txt");
            }

            // Opening
            PrintStream out = new PrintStream(new FileOutputStream(log));

            // Printing error log
            out.println("# Hello there!\n# Unfortunately, something went wrong.\n# Please, create new issue on my GitHub: https://github.com/dya-tel/TerraControl/issues\n# Put everything from this file into description.\n# Thank you.");
            for (int i = 0; i < errors.size(); i++) {
                out.println("\n" + (i + 1) + ":");
                errors.get(i).printStackTrace(out);
            }
            out.close();
        } catch (Exception e) {
            System.err.println("Failed to fill error log!");
            e.printStackTrace();
        }
    }

}
