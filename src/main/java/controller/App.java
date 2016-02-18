package controller;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;

public class App {

    public final Path SOURCE_DIR_PATH;
    public final Path TARGET_DIR_PATH;
    public final String MAX_HEAP_SIZE;
    public final String JAR_NAME;


    public App(String sourceDir, String targetDir, String MAX_HEAP_SIZE, String jarName) {
        this.MAX_HEAP_SIZE = MAX_HEAP_SIZE;
        this.JAR_NAME = jarName;
        this.SOURCE_DIR_PATH = Paths.get(sourceDir);
        this.TARGET_DIR_PATH = Paths.get(targetDir);
    }


    public static void main(String... args) throws IOException {
        final String fromPath = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        final String toPath;
        final String maxHeapSize;
        final String jarName;
        URL workingDirectory = App.class.getProtectionDomain().getCodeSource().getLocation();

        if (args.length == 3) {
            toPath = args[0];
            maxHeapSize = args[1];
            jarName = args[2];
        } else if (Files.exists(Paths.get(workingDirectory + "MCpal_config.cfg"))) {
            final List<String> arguments = Files.readAllLines(Paths.get(workingDirectory + "MCpal_config.cfg"));
            if (arguments.size() != 3) throw new RuntimeException("Invalid input parameters");
            Files.delete(Paths.get(workingDirectory + "MCpal_config.cfg"));
            toPath = arguments.get(0);
            maxHeapSize = arguments.get(1);
            jarName = arguments.get(2);
        } else {
            throw new IllegalStateException("Invalid Input Parameters. Please start this App file like this:\n" +
                    "java -jar MCpal.jar PATH_TO_BACKUP_FOLDER MAX_RAM_YOU_WANNA_SPEND NAME_OF_MINECRAFT_SERVER_JAR\n" +
                    "Example: java -jar MCpal.jar \"C:\\Users\\Rudolf Ramses\\Minecraft\" 1024 minecraft_server.jar");
        }

        Backup backupTask = new Backup(fromPath, toPath);

        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(fromPath));
        processBuilder.command("java", "-jar", "server.jar", "nogui");
        Process process = processBuilder.start();
        BufferedWriter consoleWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        consoleWriter.write("stop");
        process.destroy();

        MinecraftConsole consoleMonitor = new MinecraftConsole(process.getInputStream());
        new Thread(consoleMonitor).start();

        DailyBackupTask dailyTask = new DailyBackupTask(backupTask, consoleWriter);

        Timer timer = new Timer();
        Calendar date = Calendar.getInstance();
        date.set(
                Calendar.DAY_OF_WEEK,
                Calendar.SUNDAY
        );
        date.set(Calendar.HOUR, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        // Schedule to run every Sunday in midnight
        timer.schedule(
                dailyTask,
                date.getTime(),
                1000 * 60 * 60 * 24 * 7
        );
    }
}