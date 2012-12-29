
/***
Copyright (c) 2009 CommonsWare, LLC
Licensed under the Apache License, Version 2.0 (the "License"); you may
not use this file except in compliance with the License. You may obtain
a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package net.oddsoftware.android.utils;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.util.Log;

import net.oddsoftware.android.feedscribe.*;

abstract public class WakefulIntentService extends IntentService {
    abstract protected void doWakefulWork(Intent intent);

    private static final String LOCK_NAME_STATIC="net.oddsoftware.android.cricket.WakefulIntentService";
    private static PowerManager.WakeLock lockStatic=null;
    
    protected static int mWakeTimeout = 0;
    
    synchronized private static PowerManager.WakeLock getLock(Context context) {
        if (lockStatic==null) {
            PowerManager mgr=(PowerManager)context.getSystemService(Context.POWER_SERVICE);

            lockStatic=mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    LOCK_NAME_STATIC);
            
            //lockStatic.setReferenceCounted(true);
            lockStatic.setReferenceCounted(false);  // work around under-locked problem in framework
                                                    // where if we explicitly release this lock the framework
                                                    // still tries to unlock it later
        }

        return(lockStatic);
    }

    public static void sendWakefulWork(Context ctxt, Intent i) {
        
        if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "WakefulIntentService sendWakefulWork");
        if (PackageManager.PERMISSION_DENIED==ctxt
                .getPackageManager()
                .checkPermission("android.permission.WAKE_LOCK",
                        ctxt.getPackageName())) {
            throw new RuntimeException("Application requires the WAKE_LOCK permission!");
        }
        if( mWakeTimeout == 0 )
        {
            getLock(ctxt).acquire();
        }
        else
        {
            getLock(ctxt).acquire(mWakeTimeout);
        }
        
        if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "WakefulIntentService startingService");
        ctxt.startService(i);
    }
    
    @SuppressWarnings("rawtypes") 
    public static void sendWakefulWork(Context ctxt, Class clsService) {
        if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "WakefulIntentService sendWakefulWork");
        sendWakefulWork(ctxt, new Intent(ctxt, clsService));
    }

    public WakefulIntentService(String name) {
        super(name);
    }
    
    
    // returns START_STICKY or START_STICKY_COMPATIBILITY in api level 5 or higher
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "WakefulIntentService onStart");
        handleOnStart(intent, startId);
    }

    /*
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleOnStart(intent, startId);
        return(START_NOT_STICKY);
    }
    */
    
    protected void handleOnStart(Intent intent, int startId)
    {
        if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "WakefulIntentService.handleOnStart, lock is held: " + getLock(this).isHeld());
        if (!getLock(this).isHeld())
        { 
            // fail-safe for crash restart
            if( mWakeTimeout == 0 )
            {
                getLock(this).acquire();
            }
            else
            {
                getLock(this).acquire(mWakeTimeout);
            }
        }
    }

    @Override
    final protected void onHandleIntent(Intent intent) {
        if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "WakefulIntentService onHandleIntent");
        try {
            doWakefulWork(intent);
        }
        finally
        {
            if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "WakefulIntentService releasing wake lock");
            
            try
            {
                getLock(this).release();
            }
            catch( RuntimeException exc )
            {
                if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "WakefulIntentService releasing wake lock", exc);
            }
        }
    }
}

