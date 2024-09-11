package main.java.raf.projekatprvi.task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TaskQueue {

    private final BlockingQueue<Task> tasks;
    public TaskQueue() {
        this.tasks = new LinkedBlockingQueue<>();
    }

    public void addTask(Task task) throws InterruptedException {
        tasks.put(task);
    }

    public Task takeTask() throws InterruptedException {
        return tasks.take();
    }

    public BlockingQueue<Task> getTasks() {
        return tasks;
    }
}
