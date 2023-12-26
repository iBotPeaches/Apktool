package brut.androlib;

import java.util.ArrayList;
import java.util.concurrent.*;

public class BackgroundWorker {

    private static final int THREADS_COUNT = Runtime.getRuntime().availableProcessors();
    private final ArrayList<Future<?>> mWorkerFutures = new ArrayList<>();
    private final ExecutorService mExecutor;
    private volatile boolean mSubmitAllowed = true;

    public BackgroundWorker() {
        this(THREADS_COUNT);
    }

    public BackgroundWorker(int threads) {
        mExecutor = Executors.newFixedThreadPool(threads);
    }

    public void waitForFinish() {
        checkState();
        mSubmitAllowed = false;
        for (Future<?> future : mWorkerFutures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        mWorkerFutures.clear();
        mSubmitAllowed = true;
    }

    public void clearFutures() {
        mWorkerFutures.clear();
    }

    private void checkState() {
        if (!mSubmitAllowed) {
            throw new IllegalStateException("BackgroundWorker is not ready");
        }
    }

    public void shutdownNow() {
        mSubmitAllowed = false;
        mExecutor.shutdownNow();
    }

    public ExecutorService getExecutor() {
        return mExecutor;
    }

    public void submit(Runnable task) {
        checkState();
        mWorkerFutures.add(mExecutor.submit(task));
    }

    public <T> Future<T> submit(Callable<T> task) {
        checkState();
        Future<T> future = mExecutor.submit(task);
        mWorkerFutures.add(future);
        return future;
    }
}
