package main.java.raf.projekatprvi.task;

import main.java.raf.projekatprvi.matrix.Matrix;
import main.java.raf.projekatprvi.matrix.MatrixExtractor;
import main.java.raf.projekatprvi.matrix.MatrixMultiplier;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

public class Task {

    private TaskType taskType;
    private Matrix matrixOne;
    private Matrix matrixTwo;
    private File matrixFile;
    private MatrixExtractor matrixExtractor;
    private MatrixMultiplier matrixMultiplier;
    private boolean stop =  false;
    private long rowsSize;
    private String resultName;

    public Task(File file) {
        this.matrixFile = file;

    }

    public Task(TaskType taskType, File matrixFile) {
        this.taskType = taskType;
        this.matrixFile = matrixFile;
    }

    public Task(TaskType taskType) throws IOException {

        this.taskType = taskType;
    }

    public Future<Matrix> initiate(){
        switch (taskType){
            case TaskType.CREATE:
                return matrixExtractor.extractMatrixFromFile(matrixFile);
            case TaskType.MODIFY:
                return matrixExtractor.extractMatrixFromFile(matrixFile);
            case TaskType.MULTIPLY:
                return matrixMultiplier.multiplyMatrices(matrixOne,matrixTwo,resultName);
            case TaskType.STOP:
                matrixMultiplier.stop();
                matrixExtractor.stop();
                return null;
            default:
                System.out.println("Nije dobar tip taska");
                return null;
        }
    }


    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public Matrix getMatrixOne() {
        return matrixOne;
    }

    public void setMatrixOne(Matrix matrixOne) {
        this.matrixOne = matrixOne;
    }

    public Matrix getMatrixTwo() {
        return matrixTwo;
    }

    public void setMatrixTwo(Matrix matrixTwo) {
        this.matrixTwo = matrixTwo;
    }

    public File getMatrixFile() {
        return matrixFile;
    }

    public void setMatrixFile(File matrixFile) {
        this.matrixFile = matrixFile;
    }

    public MatrixExtractor getMatrixExtractor() {
        return matrixExtractor;
    }

    public void setMatrixExtractor(MatrixExtractor matrixExtractor) {
        this.matrixExtractor = matrixExtractor;
    }

    public MatrixMultiplier getMatrixMultiplier() {
        return matrixMultiplier;
    }

    public void setMatrixMultiplier(MatrixMultiplier matrixMultiplier) {
        this.matrixMultiplier = matrixMultiplier;
    }



    public String getResultName() {
        return resultName;
    }

    public void setResultName(String resultName) {
        this.resultName = resultName;
    }
}