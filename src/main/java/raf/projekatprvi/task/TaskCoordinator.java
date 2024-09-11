package main.java.raf.projekatprvi.task;

import main.java.raf.projekatprvi.matrix.Matrix;
import main.java.raf.projekatprvi.matrix.MatrixBrain;
import main.java.raf.projekatprvi.matrix.MatrixExtractor;
import main.java.raf.projekatprvi.matrix.MatrixMultiplier;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class TaskCoordinator implements Runnable {

    private final TaskQueue taskQueue;

    private final MatrixBrain matrixBrain;

    private final MatrixMultiplier matrixMultiplier;
    private final MatrixExtractor matrixExtractor;

    private boolean run = true;

    public TaskCoordinator(TaskQueue taskQueue, MatrixBrain matrixBrain,MatrixExtractor matrixExtractor,MatrixMultiplier matrixMultiplier) {
        this.taskQueue = taskQueue;
        this.matrixBrain = matrixBrain;
        this.matrixExtractor =  matrixExtractor;
        this.matrixMultiplier = matrixMultiplier;
    }

    @Override
    public void run() {
        while (run) {
            try {
                Task task = taskQueue.takeTask();
                task.setMatrixExtractor(matrixExtractor);
                task.setMatrixMultiplier(matrixMultiplier);
                Future<Matrix> matrixFuture = task.initiate();
                if(matrixFuture == null && task.getTaskType() == TaskType.STOP){
                    run = false;
                }else if (matrixFuture!=null) {
                    Matrix matrix = matrixFuture.get();
                    matrixBrain.receiveMatrix(matrix);
                }else{
                    System.out.println("Matrica nije dobra");
                }

            } catch (InterruptedException e) {
                break;
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }
}