/*
 *  Copyright 2012 Brendan McCarthy (brendan@oddsoftware.net)
 *
 *  This file is part of Feedscribe.
 *
 *  Feedscribe is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 3 
 *  as published by the Free Software Foundation.
 *
 *  Feedscribe is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Feedscribe.  If not, see <http://www.gnu.org/licenses/>.
 */
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
