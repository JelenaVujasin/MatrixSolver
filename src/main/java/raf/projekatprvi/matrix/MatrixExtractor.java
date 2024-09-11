package main.java.raf.projekatprvi.matrix;

import main.java.raf.projekatprvi.task.Task;
import main.java.raf.projekatprvi.task.TaskQueue;
import main.java.raf.projekatprvi.task.TaskType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MatrixExtractor {
    private ExecutorService threadPool;
    private int maxSegmentSize;

    private TaskQueue taskQueue;

    public MatrixExtractor() {
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void stop(){
        threadPool.shutdown();
    }

    public Future<Matrix> extractMatrixFromFile(File file) {
        return threadPool.submit(() -> {
            Matrix matrix = new Matrix();
            matrix.setFile(file);
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                String header = raf.readLine();
                fillMatrix(header, matrix);

                long fileLength = raf.length();
                long cp = raf.getFilePointer();
                List<Future<?>> futures = new ArrayList<>();

                while (cp < fileLength) {
                    long segmentStart = cp;
                    long segmentEnd = Math.min(cp + maxSegmentSize, fileLength);
                    raf.seek(segmentEnd);

                    while (segmentEnd < fileLength) {
                        int b = raf.read();
                        if (b == '\n' || b == -1) break;
                        segmentEnd++;
                    }

                    raf.seek(segmentStart);
                    byte[] segmentBytes = new byte[(int)(segmentEnd - segmentStart)];
                    raf.readFully(segmentBytes);
                    String segment = new String(segmentBytes);

                    Future<?> future = threadPool.submit(new Runnable() {
                        @Override
                        public void run() {
                            processSegment(matrix, segment);
                        }
                    });
                    futures.add(future);

                    cp = segmentEnd;
                }


                for (Future<?> future : futures) {
                    future.get();
                }
                makeTaskForMatrix(matrix);
                //System.out.println("NAPRAVIO MATRICU " + matrix.getName());
                return matrix;
            } catch (FileNotFoundException e) {
                System.err.println("Fajl  " + file.getName() + " nije pronadjen.");
                return null;
            } catch (IOException e) {
                System.err.println("Greska u obradi fajla " + file.getName() + ": " + e.getMessage());
                return null;
            }catch (NullPointerException e){
                System.err.println("Ime fajla ne postoji.");
                return null;
            }
        });
    }


    private void fillMatrix(String line, Matrix matrix){
        String[] headerParts = line.split(",");
        String matrixName = null;
        int rows = 0;
        int cols = 0;
        for (String part : headerParts) {
            String[] keyValue = part.split("=");
            if (keyValue[0].trim().equals("matrix_name")) {
                matrixName = keyValue[1].trim();
            } else if (keyValue[0].trim().equals("rows")) {
                rows = Integer.parseInt(keyValue[1].trim());
            } else if (keyValue[0].trim().equals("cols")) {
                cols = Integer.parseInt(keyValue[1].trim());
            }
        }
        matrix.setName(matrixName);
        matrix.setRows(rows);
        matrix.setColumns(cols);
        matrix.fillWithZeros(rows,cols);
    }

    private void makeTaskForMatrix(Matrix matrix) throws IOException, InterruptedException {
        if(isSquareMatrix(matrix)) {
            Task task = new Task(TaskType.MULTIPLY, matrix.getFile());
            task.setMatrixOne(matrix);
            task.setMatrixTwo(matrix);
            taskQueue.addTask(task);
        }

    }

    public boolean isSquareMatrix(Matrix matrix) {
        return matrix.getRows() == matrix.getColumns();
    }

    private void processSegment(Matrix matrix, String segment) {
        String[] lines = segment.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty() || line.contains("matrix_name")) continue;

            String[] parts = line.split("=");
            if (parts.length < 2 || parts[1].trim().isEmpty()) {
                System.out.println("Losa linija: " + line);
                continue;
            }

            try {
                String[] indices = parts[0].trim().split(",");
                int row = Integer.parseInt(indices[0].trim());
                int col = Integer.parseInt(indices[1].trim());
                BigInteger value = BigInteger.valueOf(Integer.parseInt(parts[1].trim()));
                if(value == null){
                    value = BigInteger.ZERO;
                }
                matrix.getData()[row][col] = value;
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.out.println("Greška na liniji: " + line + " Greška: " + e.getMessage());
            }
        }
    }

    public int getMaxSegmentSize() {
        return maxSegmentSize;
    }

    public void setMaxSegmentSize(int maxSegmentSize) {
        this.maxSegmentSize = maxSegmentSize;
    }

    public TaskQueue getTaskQueue() {
        return taskQueue;
    }

    public void setTaskQueue(TaskQueue taskQueue) {
        this.taskQueue = taskQueue;
    }
}
