package de.unisb.cs.depend.ccs_sem.evalutators.executors;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


/**
 * This executor tries to keep the assignment of jobs to working threads
 * more statically in such a way that a working thread that submits a new
 * job will most probably be the thread that later executes this job.
 * If a working thread has no more jobs, it takes one of the other's jobs, of
 * course.
 *
 * @author Clemens Hammacher
 */
public class ThreadBasedExecutor extends AbstractExecutorService {

    private final ThreadFactory threadFactory;
    protected volatile boolean isShutdown = false;
    protected Map<Thread, Queue<Runnable>> threadJobs =
        new HashMap<Thread, Queue<Runnable>>();
    protected volatile boolean forcedStop = false;

    // object for synchronization
    protected Object newJobs = new Object();

    public ThreadBasedExecutor(int poolSize,
            ThreadFactory myThreadFactory) {
        super();
        if (poolSize < 1)
            throw new IllegalArgumentException("Poolsize must be > 0");
        this.threadFactory = myThreadFactory;
        initialize(poolSize);
    }

    private void initialize(int poolSize) {
        for (int i = 0; i < poolSize; ++i) {
            final Thread newThread = threadFactory.newThread(new Worker());
            newThread.start();
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public boolean isTerminated() {
        if (!isShutdown)
            return false;

        return threadJobs == null;
    }

    public void shutdown() {
        synchronized (newJobs) {
            isShutdown = true;
            newJobs.notifyAll();
        }
    }

    public List<Runnable> shutdownNow() {
        synchronized (newJobs) {
            forcedStop = true;
            isShutdown = true;
            newJobs.notifyAll();
        }
        Thread.yield();
        for (final Thread thread: threadJobs.keySet()) {
            thread.interrupt();
        }

        for (final Thread thread: threadJobs.keySet()) {
            while (true) {
                try {
                    thread.join();
                } catch (final InterruptedException e) {
                    // ignore and try again
                }
            }
        }
        final List<Runnable> list = new ArrayList<Runnable>();
        for (final Queue<Runnable> q: threadJobs.values())
            list.addAll(q);

        return list;
    }

    public void execute(Runnable command) {
        if (isShutdown)
            throw new RejectedExecutionException("Already shutdown.");

        final Thread thread = Thread.currentThread();
        Queue<Runnable> jobs = threadJobs.get(thread);
        if (jobs == null) {
            synchronized (threadJobs) {
                while (threadJobs.size() == 0)
                    try {
                        threadJobs.wait();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                // add to the first queue
                jobs = threadJobs.values().iterator().next();
            }
        }
        if (jobs == null)
            throw new RejectedExecutionException("No running thread found ?!?");
        jobs.add(command);
        synchronized (newJobs) {
            newJobs.notify();
        }
    }

    private class Worker implements Runnable {

        private Queue<Runnable> myJobs;
        private Thread myThread;

        public Worker() {
            // nothing
        }

        public void run() {
            myThread = Thread.currentThread();
            synchronized (threadJobs) {
                myJobs = threadJobs.get(myThread);
                if (myJobs == null) {
                    threadJobs.put(myThread, myJobs = new ArrayDeque<Runnable>());
                    threadJobs.notifyAll();
                }
            }
            while (!forcedStop) {
                Runnable nextJob = getNextJob();
                if (nextJob == null) {
                    if (isShutdown && !forcedStop) {
                        // look a last time for a new job
                        nextJob = getNextJob();
                        if (nextJob == null)
                            break;
                    }
                }
                if (nextJob != null)
                    nextJob.run();
            }
        }

        private Runnable getNextJob() {
            Runnable nextJob = null;
            synchronized (myJobs) {
                nextJob = myJobs.poll();
            }
            if (nextJob != null)
                return nextJob;

            Queue<Runnable> preferredQueue = null;
            int maxSize = 0;
            for (final Queue<Runnable> q: threadJobs.values()) {
                synchronized (q) {
                    if (q.size() > maxSize) {
                        maxSize = q.size();
                        preferredQueue = q;
                    }
                }
            }

            if (preferredQueue != null) {
                synchronized (preferredQueue) {
                    nextJob = preferredQueue.poll();
                }
            }

            if (nextJob == null) {
                synchronized (newJobs) {
                    if (myJobs.isEmpty() && !isShutdown) {
                        try {
                            newJobs.wait();
                        } catch (final InterruptedException e) {
                            // hm, then go on...
                        }
                    }
                }
            }

            return nextJob;
        }

    }
}