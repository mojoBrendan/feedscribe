package net.oddsoftware.android.utils;

import java.util.concurrent.LinkedBlockingQueue;

import android.os.Handler;
import android.util.Log;

import net.oddsoftware.android.feedscribe.*;

public class WorkerThread extends Thread {

    public static class Task
    {
        public void doInBackground()
        {
            if( Globals.LOGGING ) Log.e(Globals.LOG_TAG, "WorkerThread.Task.doInBackground");
        }
        
        public void onPostExecute()
        {
            if( Globals.LOGGING ) Log.e(Globals.LOG_TAG, "WorkerThread.Task.onPostExecute");
        }
    }
    
    protected LinkedBlockingQueue<Task> mBackgroundQueue;
    
    protected Handler mHandler;
    
    protected volatile boolean mRunning;
    
    public WorkerThread()
    {
        mBackgroundQueue = new LinkedBlockingQueue<Task>();
        mHandler = new Handler();
        setPriority(MIN_PRIORITY);
        mRunning = true;
    }
    
    
    @Override
    public void run()
    {
        while( mRunning )
        {
            try
            {
                final Task t = mBackgroundQueue.take();
                
                // run task in background
                t.doInBackground();
                
                // post to foreground to complete
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        t.onPostExecute();
                    }
                });
            }
            catch( InterruptedException exc )
            {
                if( Globals.LOGGING) Log.e(Globals.LOG_TAG, "WorkerThread.run - interrupted", exc);
                return;
            }
            catch( Exception exc )
            {
                if( Globals.LOGGING) Log.e(Globals.LOG_TAG, "WorkerThread.run - unhandled exception", exc);
            }
        }
    }
    
    public void addTask(Task t)
    {
        try {
            mBackgroundQueue.put(t);
        } catch (InterruptedException exc) {
            if( Globals.LOGGING) Log.e(Globals.LOG_TAG, "WorkerThread.addTask - interrupted", exc);
        }
    }
    
    public void kill()
    {
        mRunning = false;
        interrupt();
    }
    
    public boolean checkRunning()
    {
        return mRunning;
    }
}
