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
package net.oddsoftware.android.feedscribe.service;

import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import net.oddsoftware.android.feedscribe.*;

import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.data.Downloader;
import net.oddsoftware.android.feedscribe.data.FeedManager;
import net.oddsoftware.android.feedscribe.data.FeedUpdateListener;
import net.oddsoftware.android.feedscribe.ui.FeedsActivity;
import net.oddsoftware.android.utils.WakefulIntentService;

public class FeedService extends WakefulIntentService implements FeedUpdateListener
{
    
    public static final String STATUS_UPDATE = "net.oddsoftware.android.cricket.CricketService.STATUS_UPDATE";
    
    public static final int CMD_NONE = 0;
    public static final int CMD_UPDATE_FEEDS = 1;
    public static final int CMD_DOWNLOAD_ADDED = 2;
    public static final int CMD_UPDATE_FEED = 3;
    public static final int CMD_CLEAR_NOTIFICATIONS = 4;
    
    public static final int STATUS_NONE = 0;
    public static final int STATUS_UPDATING = 1;
    public static final int STATUS_UPDATE_COMPLETE = 2;
    
    public static final int ERROR_NONE = 0;
    public static final int ERROR_NETWORK = 1;
    
    private NotificationManager mNotificationManager = null;
    

    
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_FORCE = "force";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_CMD = "cmd";
    public static final String EXTRA_FEED_ID = "feed_id";
    
    private boolean mNotificationsEnabled;
    
    public FeedService()
    {
        super("FeedService");
        
        // mWakeTimeout = 120 * 1000; // 2 minutes
        mWakeTimeout = 0;
        
        mNotificationsEnabled = false;
    }

    @Override
    protected void doWakefulWork(Intent intent)
    {
        if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "doWakefulWork begins at " + new Date().getTime() );
        
        if( mNotificationManager == null)
        {
            mNotificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
        }
        
        int cmd = intent.getIntExtra("cmd", CMD_NONE);
        
        switch( cmd )
        {
            case CMD_UPDATE_FEEDS:
            {
                boolean forceUpdate = intent.getBooleanExtra(EXTRA_FORCE, false);
                doUpdateFeeds(0, forceUpdate);
                break;
            }
            
            case CMD_UPDATE_FEED:
            {
                long feedId = intent.getLongExtra(EXTRA_FEED_ID, 0);
                doUpdateFeeds(feedId, true);
                break;
            }
            
            
            case CMD_DOWNLOAD_ADDED:
            {
                Downloader.getInstance(this).processDownloads();
                break;
            }
            
            case CMD_CLEAR_NOTIFICATIONS:
            {
                clearNewItemsNotification();
            }
        }
        
        if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "doWakefulWork ends at " + new Date().getTime() );
    }
    
    private void doUpdateFeeds(long feedId, boolean forceUpdate)
    {
        try
        {
            FeedManager feedManager = FeedManager.getInstance(this);
            
            mNotificationsEnabled = feedManager.getFeedConfig().getNotificationsEnabled();
            
            feedManager.setFeedUpdateListener(this);
            
            int minIntervalMinutes = 0;
            if( !forceUpdate )
            {
                minIntervalMinutes = 60;
            }
    
            boolean updateAttempted = feedManager.updateItems( feedId, forceUpdate, minIntervalMinutes );
            
            if( updateAttempted )
            {
                checkNetwork();
            }
        
            /*
            feedManager.deleteOldItems( feedManager.getKeepHours() );
            feedManager.deleteOldImages();
            */
        
            feedManager.getFeedConfig().syncComplete();
            
            if( feedManager.getFeedConfig().getNotificationsEnabled() && !forceUpdate)
            {
                int newItemCount = feedManager.getFeedConfig().getNewItemCount();
                if( newItemCount > 0)
                {
                    showNewItemsNotification( newItemCount );
                }
            }
        }
        finally
        {
            broadcastStatus(STATUS_UPDATE_COMPLETE, 100, ERROR_NONE);
        }
    }
    
    private void checkNetwork()
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
        if( netInfo == null || !netInfo.isConnected() )
        {
            if (Globals.LOGGING) Log.w(Globals.LOG_TAG, "doUpdateFeeds - no network available");
            broadcastStatus(STATUS_UPDATE_COMPLETE, 100, ERROR_NETWORK);
        }
    }
        
    
    private void broadcastStatus(int status, int progress, int errorCode)
    {
        Intent intent = new Intent(STATUS_UPDATE);
        intent.putExtra("status", status);
        intent.putExtra(EXTRA_PROGRESS, progress);
        intent.putExtra(EXTRA_ERROR, errorCode);
        sendBroadcast(intent);
        
        if( status == STATUS_UPDATING && mNotificationsEnabled)
        {
            showSyncingNotification();
        }
        else if( status == STATUS_UPDATE_COMPLETE )
        {
            closeSyncingNotification();
        }
            
    }
    
    private void showSyncingNotification()
    {
        String tickerText = getResources().getString(R.string.notification_syncing_ticker);
        String titleText = getResources().getString(R.string.notification_syncing_title);
        String text = getResources().getString(R.string.notification_syncing_text);
        
        Intent intent = new Intent(this, FeedsActivity.class);
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new Notification(R.drawable.feedscribe_status, tickerText, System.currentTimeMillis());
        notification.setLatestEventInfo(this, titleText, text, contentIntent);
        
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        
        mNotificationManager.notify(Globals.NOTIFICATION_SYNCING, notification);
    }
    
    private void closeSyncingNotification()
    {
        mNotificationManager.cancel(Globals.NOTIFICATION_SYNCING);
    }
    
    
    private void showNewItemsNotification(int newItems)
    {
        String tickerText = getResources().getString(R.string.notification_new_items_ticker, new Integer(newItems));
        String titleText = getResources().getString(R.string.notification_new_items_title);
        String text = getResources().getString(R.string.notification_new_items_text, new Integer(newItems));
        
        Intent intent = new Intent(this, FeedsActivity.class);
        intent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP );
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new Notification(R.drawable.feedscribe_status, tickerText, System.currentTimeMillis());
        notification.setLatestEventInfo(this, titleText, text, contentIntent);
        
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        
        mNotificationManager.notify(Globals.NOTIFICATION_NEW_ITEMS, notification);
    }
    
    private void clearNewItemsNotification()
    {
        mNotificationManager.cancel(Globals.NOTIFICATION_NEW_ITEMS);
    }
    
    public void feedUpdateProgress(int stage, int numStages)
    {
        if (Globals.LOGGING) Log.e(Globals.LOG_TAG, "feedUpdateProgress " + stage + " of " + numStages);
        
        int progress = 0;
        if( numStages > 0)
        {
            progress = stage * 100/ numStages;
        }
        broadcastStatus(STATUS_UPDATING, progress, ERROR_NONE);
    }
    
    public static void downloadAdded(Context ctx)
    {
        Intent i = new Intent(ctx, FeedService.class);
        i.putExtra(EXTRA_CMD, CMD_DOWNLOAD_ADDED);
        sendWakefulWork(ctx, i);
    }
    
    public static void updateFeeds(Context ctx, boolean forceUpdate)
    {
        Intent i = new Intent(ctx, FeedService.class);
        i.putExtra(EXTRA_CMD, CMD_UPDATE_FEEDS);
        i.putExtra(EXTRA_FORCE, forceUpdate);
        sendWakefulWork(ctx, i);
        
    }

    public static void updateFeed(Context ctx, long feedId)
    {
        Intent i = new Intent(ctx, FeedService.class);
        i.putExtra(EXTRA_CMD, CMD_UPDATE_FEED);
        i.putExtra(EXTRA_FEED_ID, feedId);
        sendWakefulWork(ctx, i);
    }
    
    public static void clearNotifications(Context ctx)
    {
        Intent i = new Intent(ctx, FeedService.class);
        i.putExtra(EXTRA_CMD, CMD_CLEAR_NOTIFICATIONS);
        sendWakefulWork(ctx, i);
    }
}

