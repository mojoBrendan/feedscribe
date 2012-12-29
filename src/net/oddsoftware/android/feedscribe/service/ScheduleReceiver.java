package net.oddsoftware.android.feedscribe.service;

import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.data.FeedConfig;

public class ScheduleReceiver extends BroadcastReceiver
{

    public static final String EXTRA_CMD = "cmd";
    
    public static final int CMD_SYNC = 1;
    
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "ScheduleReceiver.onReceive");
        
        if( intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) )
        {
            if( Globals.LOGGING ) Log.e(Globals.LOG_TAG, "onReceive scheduling from boot");
            scheduleSync(context);
            doSync(context);
        }
        else
        {
            int cmd = intent.getIntExtra(EXTRA_CMD, 0);
            
            if( cmd == CMD_SYNC )
            {
                doSync(context);
            }
        }
    }
    
    
    public static void scheduleSync(Context ctx)
    {
        if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "CricketReceiver.scheduleSync from feed manager");
        
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = new Intent(ctx, ScheduleReceiver.class);
        intent.putExtra(EXTRA_CMD, CMD_SYNC);
        
        PendingIntent sender = PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        FeedConfig feedConfig = FeedConfig.getInstance(ctx);
        long interval = feedConfig.getSyncInterval();
        
        if( interval == 0 )
        {
            if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "CricketReceiver.scheduleSync from feed manager - cancelling");
            alarmManager.cancel(sender);
        }
        else
        {
            interval = feedConfig.getInexactSyncInterval();
            long next = new Date().getTime() + interval / 2;
            if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "CricketReceiver.scheduleSync from feed manager - scheduling for " + next + " interval " + interval);
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, next, interval, sender);
        }
    }
    
    
    private void doSync(Context ctx)
    {
        if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "CricketReceiver.doSync");
        
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if( connectivityManager.getBackgroundDataSetting() )
        {
            if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "CricketReceiver.doSync - updating feeds");
            
            FeedConfig feedConfig = FeedConfig.getInstance(ctx);
            
            if( feedConfig.syncTimeExpired() )
            {
                FeedService.updateFeeds(ctx, false);
            }
        }
    }
    
}
