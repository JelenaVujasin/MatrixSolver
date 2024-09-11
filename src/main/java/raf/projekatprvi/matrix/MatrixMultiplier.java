package main.java.raf.projekatprvi.matrix;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MatrixMultiplier {
    private ExecutorService threadPool;
    private int minRowsPerTask;


    public MatrixMultiplier() {
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void stop(){
        threadPool.shutdown();
    }


    public Future<Matrix> multiplyMatrices(Matrix matrixA, Matrix matrixB, String resultName) {
        if(matrixA == null && matrixB == null && resultName == null){
            stop();
        }

        if (matrixA.getRows() <= minRowsPerTask || matrixB.getColumns() <= minRowsPerTask) {

            return threadPool.submit(() -> multiply(matrixA, matrixB,resultName));
        } else {

            return threadPool.submit(() -> multiplyInSegments(matrixA, matrixB,resultName));
        }
    }


    private Matrix multiply(Matrix matrixA, Matrix matrixB,String resultName) {
        //System.out.println("METODA U MULTIPLY " + matrixA.getInfo()  + " " + matrixB.getInfo());
        int rowsA = matrixA.getRows();
        int colsA = matrixA.getColumns();
        int colsB = matrixB.getColumns();
        Matrix resultMatrix = new Matrix(resultName, rowsA,colsB);


        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                BigInteger sum = BigInteger.ZERO;
                for (int k = 0; k < colsA; k++) {
                    sum = sum.add(matrixA.getValueAt(i, k).multiply(matrixB.getValueAt(k, j)));
                }
                resultMatrix.set(i, j, sum);
            }
        }

        return resultMatrix;
    }


    private Matrix multiplyInSegments(Matrix matrixA, Matrix matrixB,String resultName) {
        //System.out.println("Mnozenje matrica " + matrixA.getName() + " i " + matrixB.getName());
        int rowsA = matrixA.getRows();
        int colsB = matrixB.getColumns();
        Matrix resultMatrix = new Matrix(resultName, rowsA,colsB);
        resultMatrix.setMultMatrix(matrixA.getName().concat("_").concat(matrixB.getName()));

        int numSegments = Math.max(rowsA / minRowsPerTask, 1);


        List<Future<Void>> segmentFutures = new ArrayList<>();


        for (int i = 0; i < numSegments; i++) {
            final int segmentStart = i * minRowsPerTask;
            final int segmentEnd = (i == numSegments - 1) ? rowsA : (i + 1) * minRowsPerTask;


            Callable<Void> segmentTask = () -> {
                for (int row = segmentStart; row < segmentEnd; row++) {
                    for (int col = 0; col < colsB; col++) {
                        BigInteger sum = BigInteger.ZERO;
                        for (int k = 0; k < matrixA.getColumns(); k++) {
                            sum = sum.add(matrixA.getValueAt(row, k).multiply(matrixB.getValueAt(k, col)));
                        }
                        resultMatrix.set(row, col, sum);
                    }
                }
                return null;
            };


            segmentFutures.add(threadPool.submit(segmentTask));
        }


        for (Future<Void> future : segmentFutures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        return resultMatrix;
    }


    public int getMinRowsPerTask() {
        return minRowsPerTask;
    }

    public void setMinRowsPerTask(int minRowsPerTask) {
        this.minRowsPerTask = minRowsPerTask;
    }
}
