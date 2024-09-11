package main.java.raf.projekatprvi;

import main.java.raf.projekatprvi.matrix.Matrix;
import main.java.raf.projekatprvi.matrix.MatrixBrain;
import main.java.raf.projekatprvi.matrix.MatrixExtractor;
import main.java.raf.projekatprvi.matrix.MatrixMultiplier;
import main.java.raf.projekatprvi.task.Task;
import main.java.raf.projekatprvi.task.TaskCoordinator;
import main.java.raf.projekatprvi.task.TaskQueue;
import main.java.raf.projekatprvi.task.TaskType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Main {


    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        TaskQueue taskQueue = new TaskQueue();
        MatrixExtractor matrixExtractor = new MatrixExtractor();
        matrixExtractor.setTaskQueue(taskQueue);
        MatrixMultiplier matrixMultiplier = new MatrixMultiplier();
        MatrixBrain matrixBrain = new MatrixBrain();
        matrixBrain.setTaskQueue(taskQueue);
        SystemExplorer systemExplorer = new SystemExplorer();
        systemExplorer.setTaskQueue(taskQueue);
        TaskCoordinator taskCoordinator = new TaskCoordinator(taskQueue, matrixBrain, matrixExtractor, matrixMultiplier);
        loadConfig(systemExplorer, matrixExtractor, matrixMultiplier);
        startThreads(systemExplorer, taskCoordinator);
        cliLoop(systemExplorer, matrixBrain,taskQueue);

    }



    private static void loadConfig(SystemExplorer systemExplorer, MatrixExtractor matrixExtractor, MatrixMultiplier matrixMultiplier) throws IOException {
        Properties config = new Properties();
        try {
            config.load(new FileInputStream("app.properties"));
            long sleepTime = Long.parseLong(config.getProperty("sys_explorer_sleep_time"));
            long maxChunkSize = Long.parseLong(config.getProperty("maximum_file_chunk_size"));
            long maxRowsSize = Long.parseLong(config.getProperty("maximum_rows_size"));
            String dir = config.getProperty("start_dir");
            systemExplorer.setSleepTime(sleepTime);
            systemExplorer.setBaseDir(dir);
            matrixExtractor.setMaxSegmentSize((int) maxChunkSize);
            matrixMultiplier.setMinRowsPerTask((int) maxRowsSize);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void startThreads(SystemExplorer systemExplorer, TaskCoordinator taskCoordinator) {
        Thread sysExpThread = new Thread(systemExplorer);
        Thread taskThread = new Thread(taskCoordinator);
        sysExpThread.start();
        taskThread.start();
    }


    private static void cliLoop(SystemExplorer systemExplorer, MatrixBrain matrixBrain, TaskQueue taskQueue) throws IOException, InterruptedException, ExecutionException {
        Scanner scanner = new Scanner(System.in);
        while (true) {

            String command = scanner.nextLine();
            if (command.equalsIgnoreCase("stop")) {
                break;
            }
            processCommand(command, systemExplorer, matrixBrain);
            //System.out.print("> ");
        }
        scanner.close();

        stopThreads(systemExplorer,matrixBrain,taskQueue);
    }

    private static void processCommand(String command, SystemExplorer systemExplorer, MatrixBrain matrixBrain) throws IOException, InterruptedException, ExecutionException {
        String[] parts = command.split("\\s+");
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "dir":
                handleDirCommand(parts, systemExplorer);
                break;
            case "info":
                handleInfoCommand(parts, matrixBrain);
                break;
            case "multiply":
                handleMultiplyCommand(parts, matrixBrain);
                break;
            case "save":
                handleSaveCommand(parts, matrixBrain);
                break;
            case "clear":
                handleClearCommand(parts,matrixBrain);
                break;
            case "help":
                handleHelpCommand();
            default:
                System.out.println("Invalid command.");
        }
    }

    private static void handleHelpCommand() {
        Map<String, String> commands = getAllCommands();

        System.out.println("Supported Commands:");
        for (Map.Entry<String, String> entry : commands.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private static void handleDirCommand(String[] parts, SystemExplorer systemExplorer) throws IOException, InterruptedException {
        if (parts.length != 2) {
            System.out.println("Invalid usage: dir <dir_name>");
            return;
        }

        String dirName = parts[1];
        File dir = new File(dirName);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Invalid directory: " + dirName);
            return;
        }

        systemExplorer.processDirectory(dir);
        System.out.println("Directory added: " + dirName);
    }

    private static void handleInfoCommand(String[] parts, MatrixBrain matrixBrain) throws IOException {
        if (parts.length < 2) {
            System.out.println("Nedostaje naziv matrice.");
            return;
        }

        String command = parts[1];
        switch (command) {
            case "-all":
                printAllMatrices(matrixBrain);
                break;
            case "-asc":
                printSortedMatrices(matrixBrain, true);
                break;
            case "-desc":
                printSortedMatrices(matrixBrain, false);
                break;
            case "-s":
                if (parts.length < 3) {
                    System.out.println("Nedostaje broj matrica za prikaz.");
                    return;
                }
                int topN = Integer.parseInt(parts[2]);
                printTopNMatrices(matrixBrain, topN);
                break;
            case "-e":
                if (parts.length < 3) {
                    System.out.println("Nedostaje broj matrica za prikaz.");
                    return;
                }
                int endN = Integer.parseInt(parts[2]);
                printLastNMatrices(matrixBrain, endN);
                break;
            default:
                System.out.println(matrixBrain.getInfo(command));
                break;
        }
    }


    private static void handleMultiplyCommand(String[] parts, MatrixBrain matrixBrain) throws IOException, ExecutionException, InterruptedException {
        String resultName;
        String[] names = parts[1].split(",");
        if(names.length!=2){
            System.out.println("Matrice moraju biti razdvojene zarezom");
            return;
        }
        String name1 = names[0];
        String name2 = names[1];
        if (parts.length == 4 && parts[2].equals("-name")) {
            resultName = parts[3];
        } else {
            resultName = name1.concat(name2);
        }
        Matrix matrixx = matrixBrain.getMatrixCache().get(name1);
        Matrix matrix1 = matrixBrain.getMatrixCache().get(name2);
        if (matrixBrain.containsMatrixWithName(name1.concat(name2)) || matrixBrain.containsMatrixWithName(resultName)) {
            System.out.println("Mnozenje matrica " + name1 + " i " + name2 + " zavrseno");
        } else {
            if (canMultiplyMatrices(matrixx, matrix1)) {
                Future<?> multiplicationResultFuture = matrixBrain.multiplyMatrices(name1, name2, true, resultName);

                while (!multiplicationResultFuture.isDone()) {
                    System.out.println("Množenje matrica u toku...");
                    Thread.sleep(1000);
                }
                if (multiplicationResultFuture.get() != null) {
                    System.out.println("Množenje matrica završeno. Ime nove matrice:  " + resultName);
                } else {
                    System.out.println("Greška pri množenju matrica: rezultat je null.");
                }
            } else {
                System.out.println("Nije moguce pomnoziti matrice");
            }
        }

        /*for (String name : matrixBrain.getResultMatrix().keySet()) {
                    if(name.equals(resultName)){
                        Matrix matrix = matrixBrain.getMatrixCache().get(name);
                        Matrix matrix2 = matrixBrain.getResultMatrix().get(name);
                        //System.out.println("U PROVERI " + matrix.getInfo() + " " + matrix2.getInfo());
                        if(areMatricesEqual(matrix,matrix2)){
                            System.out.println("DOBROOOO");
                        }else {
                            System.out.println("NIJEEE");
                        }
                    }

        }*/
    }

    public static boolean areMatricesEqual(Matrix matrix1, Matrix matrix2) {
        if (matrix1.getRows() != matrix2.getRows() || matrix1.getColumns() != matrix2.getColumns()) {
            System.out.println("MATRICA 1 " + matrix1.getRows()  + " " + matrix1.getColumns() + "\n" + " MATRICA 2 "
                    + matrix2.getRows()  + " "+ matrix2.getColumns());

            System.out.println("NISU DOBRE DIMENZIJE");
            return false;
        }

        for (int i = 0; i < matrix1.getRows(); i++) {
            for (int j = 0; j < matrix1.getColumns(); j++) {
                BigInteger value1 = matrix1.getValueAt(i, j);
                BigInteger value2 = matrix2.getValueAt(i, j);


                if (!value1.equals(value2)) {
                    return false;
                }
            }
        }


        return true;
    }

    private static void handleSaveCommand(String[] parts, MatrixBrain matrixBrain) throws IOException {
        if (parts.length < 5 || !parts[1].equals("-name") || !parts[3].equals("-file")) {
            System.out.println("Neispravni parametri za komandu save.");
            return;
        }

        String matrixName = parts[2];
        String fileName = parts[4];

        Matrix matrix = matrixBrain.getMatrixCache().get(matrixName);
        matrix.setFile(new File(fileName));
        if (matrix == null) {
            System.out.println("Matrica '" + matrixName + "' ne postoji.");
            return;
        }

        saveMatrixToFile(matrix, fileName);
        System.out.println("Matrica '" + matrixName + "' je uspešno sačuvana u fajl '" + fileName + "'.");
    }

    private static void handleClearCommand(String[] parts,MatrixBrain matrixBrain) throws IOException, InterruptedException {
        if (parts.length != 2) {
            System.out.println("Neispravni parametri za komandu clear.");
            return;
        }

        String arg = parts[1];
        matrixBrain.clear(arg);

        //System.out.println("Matrica i svi njeni rezultati su izbrisani");
    }

    private static void stopThreads(SystemExplorer systemExplorer,MatrixBrain matrixBrain,TaskQueue taskQueue) throws IOException, InterruptedException {
        systemExplorer.stop();
        matrixBrain.stop();
        Task task = new Task(TaskType.STOP);
        taskQueue.addTask(task);
    }


    public static boolean canMultiplyMatrices(Matrix matrixA, Matrix matrixB) {
        return matrixA.getColumns() == matrixB.getRows();
    }


    private static void printAllMatrices(MatrixBrain matrixBrain) {
        Map<String,Matrix> mergedMap = mergeMaps(matrixBrain.getMatrixCache(),matrixBrain.getResultMatrix());
        for (Matrix matrix : mergedMap.values()) {
            System.out.println(matrix.getInfo());
        }
    }

    private static void printSortedMatrices(MatrixBrain matrixBrain, boolean ascending) {
        Map<String,Matrix> mergedMap = mergeMaps(matrixBrain.getMatrixCache(),matrixBrain.getResultMatrix());
        Matrix[] matrices = mergedMap.values().toArray(new Matrix[0]);
        Arrays.sort(matrices, (m1, m2) -> {
            if (ascending) {
                if (m1.getRows() != m2.getRows()) {
                    return m1.getRows() - m2.getRows();
                } else {
                    return m1.getColumns() - m2.getColumns();
                }
            } else {
                if (m1.getRows() != m2.getRows()) {
                    return m2.getRows() - m1.getRows();
                } else {
                    return m2.getColumns() - m1.getColumns();
                }
            }
        });

        for (Matrix matrix : matrices) {
            System.out.println(matrix.getInfo());
        }
    }

    private static void printTopNMatrices(MatrixBrain matrixBrain, int topN) {
        Map<String,Matrix> mergedMap = mergeMaps(matrixBrain.getMatrixCache(),matrixBrain.getResultMatrix());
        Matrix[] matrices = mergedMap.values().toArray(new Matrix[0]);
        Arrays.sort(matrices, (m1, m2) -> {
            if (m1.getRows() != m2.getRows()) {
                return m1.getRows() - m2.getRows();
            } else {
                return m1.getColumns() - m2.getColumns();
            }
        });

        for (int i = 0; i < Math.min(topN, matrices.length); i++) {
            System.out.println(matrices[i].getInfo());
        }
    }

    private static void printLastNMatrices(MatrixBrain matrixBrain, int endN) {
        Map<String,Matrix> mergedMap = mergeMaps(matrixBrain.getMatrixCache(),matrixBrain.getResultMatrix());
        Matrix[] matrices = mergedMap.values().toArray(new Matrix[0]);
        Arrays.sort(matrices, (m1, m2) -> {
            if (m1.getRows() != m2.getRows()) {
                return m1.getRows() - m2.getRows();
            } else {
                return m1.getColumns() - m2.getColumns();
            }
        });

        int startIndex = Math.max(0, matrices.length - endN);
        for (int i = startIndex; i < matrices.length; i++) {
            System.out.println(matrices[i].getInfo());
        }


    }

    public static Map<String, Matrix> mergeMaps(Map<String, Matrix> map1, Map<String, Matrix> map2) {
        Map<String, Matrix> mergedMap = new HashMap<>();

        for (Map.Entry<String, Matrix> entry : map1.entrySet()) {
            String key = entry.getKey();
            Matrix value = entry.getValue();
            mergedMap.put(key, value);
        }

        for (Map.Entry<String, Matrix> entry : map2.entrySet()) {
            String key = entry.getKey();
            Matrix value = entry.getValue();
            if (!mergedMap.containsKey(key)) {
                mergedMap.put(key, value);
            }
        }

        return mergedMap;
    }

    private static void saveMatrixToFile(Matrix matrix, String fileName) {
        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.println("matrix_name=" + matrix.getName() + ",rows=" + matrix.getRows() + ",cols=" + matrix.getColumns());
            for (int i = 0; i < matrix.getRows(); i++) {
                for (int j = 0; j < matrix.getColumns(); j++) {
                    BigInteger value = matrix.getValueAt(i, j);
                    writer.println(i + "," + j + " = " + value);
                }
            }
        }catch (IOException e) {
            System.err.println("Greška prilikom čuvanja matrice u fajl: " + e.getMessage());
        }
    }

    public static Map<String, String> getAllCommands() {
        Map<String, String> commands = new HashMap<>();

        // Dodavanje svih podržanih komandi sa opisima
        commands.put("dir dir_name", "Add directory for scanning.");
        commands.put("info matrix_name", "Retrieve information about a specific matrix or set of matrices.");
        commands.put("multiply mat1,mat2", "Multiply two matrices. Use -async for asynchronous execution.");
        commands.put("save -name mat_name -file file_name", "Save matrix to a file.");
        commands.put("clear mat_name / clear file_name", "Clear results associated with a matrix or file.");
        commands.put("stop", "Shutdown the application.");

        return commands;
    }
}
