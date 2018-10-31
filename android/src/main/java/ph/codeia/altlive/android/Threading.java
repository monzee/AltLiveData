package ph.codeia.altlive.android;

/*
 * This file is a part of the AltLiveData project.
 */

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;

public enum Threading implements Executor {
    ANYWHERE {
        @Override
        public void execute(@NonNull Runnable runnable) {
            runnable.run();
        }
    },

    CPU_BOUND {
        @Override
        public void execute(@NonNull Runnable runnable) {
            AsyncTask.THREAD_POOL_EXECUTOR.execute(runnable);
        }
    },

    IO_BOUND {
        private final Executor pool = Executors.newCachedThreadPool();

        @Override
        public void execute(@NonNull Runnable runnable) {
            pool.execute(runnable);
        }
    },

    UI_POST_ALWAYS {
        @Override
        public void execute(@NonNull Runnable runnable) {
            Singleton.HANDLER.post(runnable);
        }
    },

    UI_POST_AS_NEEDED {
        @Override
        public void execute(@NonNull Runnable runnable) {
            if (Thread.currentThread() == Singleton.MAIN_THREAD) {
                runnable.run();
            }
            else {
                Singleton.HANDLER.post(runnable);
            }
        }
    };

    private static class Singleton {
        static final Handler HANDLER;
        static final Thread MAIN_THREAD;
        static {
            Looper mainLooper = Looper.getMainLooper();
            MAIN_THREAD = mainLooper.getThread();
            HANDLER = new Handler(mainLooper);
        }
    }
}

