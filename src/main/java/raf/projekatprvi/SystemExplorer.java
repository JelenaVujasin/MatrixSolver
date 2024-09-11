package main.java.raf.projekatprvi;

import main.java.raf.projekatprvi.task.Task;
import main.java.raf.projekatprvi.task.TaskQueue;
import main.java.raf.projekatprvi.task.TaskType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SystemExplorer implements Runnable{
    private String baseDir;
    private long sleepTime;
    private TaskQueue taskQueue;

    private volatile boolean running = true;


    @Override
    public void run() {
        while (running) {
            try {
                scanForMatrixFiles();
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {

                break;
            }
        }
    }

    public void stop() {
        running = false;
    }
    public void scanForMatrixFiles(String... additionalDirectories) throws InterruptedException, IOException {
        List<String> directories = new ArrayList<>();
        directories.add(baseDir);
        directories.addAll(Arrays.asList(additionalDirectories));

        for (String directory : directories) {
            File dir = new File(directory);
            if (dir.isDirectory()) {
                processDirectory(dir);
            }
        }
    }

    public void processDirectory(File directory) throws InterruptedException, IOException {
        for (File file : directory.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".rix")) {
                processFile(file);
            } else if (file.isDirectory()) {
                processDirectory(file);
            }
        }
    }

    private Map<String, Long> lastModifiedMap = new ConcurrentHashMap<>();

    private void processFile(File file) throws InterruptedException, IOException {
        String filePath = file.getAbsolutePath();
        long lastModified = file.lastModified();
        if (!lastModifiedMap.containsKey(filePath)) {
            Task task = new Task(TaskType.CREATE, file);
            taskQueue.addTask(task);
            lastModifiedMap.put(filePath, lastModified);
        } else if(lastModifiedMap.containsKey(filePath) && lastModifiedMap.get(filePath) != lastModified){
            System.out.println("Fajl " + file.getName() + " promenjen");
            Task task = new Task(TaskType.MODIFY,file);
            taskQueue.addTask(task);
        }
        lastModifiedMap.put(filePath, lastModified);
    }





    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public long getSleepTime() {
        return sleepTime;
    }

    public void setSleepTime(long sleepTime) {
        this.sleepTime = sleepTime;
    }

    public TaskQueue getTaskQueue() {
        return taskQueue;
    }

    public void setTaskQueue(TaskQueue taskQueue) {
        this.taskQueue = taskQueue;
    }

    public  Map<String, Long> getLastModifiedMap() {
        return lastModifiedMap;
    }

    public  void setLastModifiedMap(Map<String, Long> lastModifiedMap) {
        this.lastModifiedMap = lastModifiedMap;
    }


}
