/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BackgroundWorker {
    private final ExecutorService mExecutor;
    private final List<Future<?>> mWorkerFutures;
    private volatile boolean mSubmitAllowed;

    public BackgroundWorker(int threads) {
        mExecutor = Executors.newFixedThreadPool(threads);
        mWorkerFutures = new ArrayList<>();
        mSubmitAllowed = true;
    }

    public void waitForFinish() {
        checkState();
        mSubmitAllowed = false;
        for (Future<?> future : mWorkerFutures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
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
