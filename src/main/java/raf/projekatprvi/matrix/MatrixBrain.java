package main.java.raf.projekatprvi.matrix;

import main.java.raf.projekatprvi.Main;
import main.java.raf.projekatprvi.task.Task;
import main.java.raf.projekatprvi.task.TaskQueue;
import main.java.raf.projekatprvi.task.TaskType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MatrixBrain {
    private ExecutorService threadPool;
    private Map<String, Matrix> matrixCache;

    private Map<String,Matrix> resultMatrix;



    private TaskQueue taskQueue;

    public MatrixBrain() {
        this.threadPool = Executors.newCachedThreadPool();
        this.resultMatrix = new HashMap<>();
        this.matrixCache = new HashMap<>();
        taskQueue = new TaskQueue();

    }

    public void stop(){
        threadPool.shutdown();
    }

    public void printAll(){
        for(Matrix matrix: matrixCache.values()){
            System.out.println(matrix.getName());
        }
    }


    public String getInfo(String matrixName) {
        Matrix matrix = matrixCache.get(matrixName);
        if (matrix != null) {
            return matrix.getInfo();
        } else {
            return "Matrica sa imenom '" + matrixName + "' nije pronadjena";
        }
    }

    public void receiveMatrix(Matrix matrix){
        if(matrix.getFile() != null && matrix.getFile().getName().contains("result")){
            resultMatrix.put(matrix.getName(),matrix);
        }else if(matrix.getName() != null || matrix.getFile() != null){
            matrixCache.put(matrix.getName(),matrix);
        }
    }

    public Future<?> multiplyMatrices(String  matrixA, String matrixB, boolean async, String resultName) throws ExecutionException, InterruptedException {
        Matrix matrix = matrixCache.get(matrixA);
        Matrix matrix1 = matrixCache.get(matrixB);
        if (async) {
            return multiplyMatricesAsync(matrix, matrix1, resultName);
        } else {
            return multiplyMatricesSync(matrix, matrix1, resultName);
        }
    }

    private Future<?> multiplyMatricesSync(Matrix matrixA, Matrix matrixB, String resultName) {
        return threadPool.submit(() -> {
            Task task = null;
            try {
                task = new Task(TaskType.MULTIPLY);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            task.setMatrixOne(matrixA);
            task.setMatrixTwo(matrixB);
            task.setResultName(resultName);

            try {
                taskQueue.addTask(task);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Future<?> matrix = task.initiate();
            Matrix matrix1 = (Matrix) matrix.get();

            return matrix1;
        });
    }


    private Future<?> multiplyMatricesAsync(Matrix matrixA, Matrix matrixB, String resultName) throws ExecutionException, InterruptedException {
        return threadPool.submit(() ->{

            Task task = null;
            try {
                task = new Task(TaskType.MULTIPLY);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            task.setMatrixOne(matrixA);
            task.setMatrixTwo(matrixB);
            task.setResultName(resultName);
            try {
                taskQueue.addTask(task);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return true;
        });
    }



    public void clear(String arg) throws IOException, InterruptedException {
        File file = new File(arg);

        if (file.exists()) {
            clearMatricesFromFile(file);
        } else {
            clearMatrix(arg);
        }
    }


    private void clearMatrix(String matrixName) throws IOException, InterruptedException {
        boolean found = false;
        Iterator<Map.Entry<String, Matrix>> iterator = matrixCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Matrix> entry = iterator.next();
            String resultName = entry.getKey();
            if (resultName.contains(matrixName)) {
                if(matrixCache.get(resultName).getFile() !=null){
                    makeTask(matrixName);
                }
                if(matrixCache.get(resultName).getMultMatrix()!= null){
                    makeMultiplyTask(resultName);
                }else if(ifIncludedInMult(resultName)){
                    makeMultiplyTask(takeName(resultName));
                }
                System.out.println("Obrisana matrica " + resultName + " i svi njeni rezultati");
                iterator.remove();
                found = true;


            }
        }
        if (!found) {
            System.out.println("Matrica '" + matrixName + "' nije pronađena.");
        }
    }

    private String takeName(String name){
        for(Matrix matrix : matrixCache.values()){
            if(matrix.getMultMatrix() != null && matrix.getMultMatrix().contains(name)){
                return matrix.getName();
            }
        }
        return null;
    }

    private boolean ifIncludedInMult(String name){
        for(Matrix matrix : matrixCache.values()){
            if(matrix.getMultMatrix() != null && matrix.getMultMatrix().contains(name)){
                return true;
            }
        }
        return false;
    }

    private void makeMultiplyTask(String resultName) throws IOException, InterruptedException {
        //System.out.println("PRAVI MULTIPLY TASK ZA " + resultName);
        Task task = new Task(TaskType.MULTIPLY);
        Matrix matrix = matrixCache.get(resultName);
        String matrix1 = matrix.getMultMatrix().split("_")[0];
        String matrix2 = matrix.getMultMatrix().split("_")[1];
        Matrix matrix3 = matrixCache.get(matrix1);
        Matrix matrix4 = matrixCache.get(matrix2);
        task.setMatrixOne(matrix3);
        task.setMatrixTwo(matrix4);
        task.setResultName(resultName);
        taskQueue.addTask(task);
    }

    private void makeTask(String matrixName) throws IOException, InterruptedException {
        //System.out.println("PRAVI TASK ZA " + matrixName);
        Task task = new Task(TaskType.CREATE,matrixCache.get(matrixName).getFile());
        taskQueue.addTask(task);
    }

    public boolean containsMatrixWithName(String matrixName) {
        for (String resultName : matrixCache.keySet()) {
            if (resultName.contains(matrixName)) {
                return true;
            }
        }
        return false;
    }




    private void clearMatricesFromFile(File file) throws IOException, InterruptedException {
        List<String> lines = Files.readAllLines(file.toPath());
        List<String> matrixNames = new ArrayList<>();


        for (String line : lines) {
            if (line.startsWith("matrix_name=")) {
                String[] parts = line.split(",");
                String matrixName = parts[0].substring("matrix_name=".length());
                matrixNames.add(matrixName);
            }
        }


        for (String matrixName : matrixNames) {
            clearMatrix(matrixName);
        }

        System.out.println("Svi rezultati za matrice iz fajla '" + file.getName() + "' su uspešno obrisani.");
    }


    public void deleteMatrix(String matrixName) {
        matrixCache.remove(matrixName);
    }


    public void clearCache() {
        matrixCache.clear();
    }

    public TaskQueue getTaskQueue() {
        return taskQueue;
    }

    public void setTaskQueue(TaskQueue taskQueue) {
        this.taskQueue = taskQueue;
    }

    public Map<String, Matrix> getmatrixCache() {
        return matrixCache;
    }



    public Map<String, Matrix> getMatrixCache() {
        return matrixCache;
    }

    public void setMatrixCache(Map<String, Matrix> matrixCache) {
        this.matrixCache = matrixCache;
    }


    public Map<String, Matrix> getResultMatrix() {
        return resultMatrix;
    }
}
