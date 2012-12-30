package net.oddsoftware.android.feedscribe.data;

import java.util.Date;

import net.oddsoftware.android.feedscribe.Globals;
import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class FeedConfig {
    
    public static synchronized FeedConfig getInstance(Context ctx)
    {
        if( mInstance == null )
        {
            mInstance = new FeedConfig(ctx);
        }
        
        return mInstance;
    }
    
    private static FeedConfig mInstance = null;
    
    private static final String CONFIG_PREVIOUS_VERSION = "previousVersion";
    
    
    private SharedPreferences mSharedPreferences;
    
    private FeedConfig(Context ctx)
    {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
    }
    
    
    public int getPreviousPackageVersion(int defaultVersion)
    {
        try
        {
            return mSharedPreferences.getInt(CONFIG_PREVIOUS_VERSION, defaultVersion);
        }
        catch(ClassCastException exc)
        {
            if (Globals.LOGGING) Log.e(Globals.LOG_TAG, "error loading previous version from config", exc);
        }
        return defaultVersion;
    }
    
    public long getKeepHours()
    {
        try
        {
            return Long.parseLong( mSharedPreferences.getString("keepTime", "2") ) * 24;
        }
        catch( NumberFormatException exc )
        {
            if (Globals.LOGGING) Log.e(Globals.LOG_TAG, "error parsing keepTime preference", exc);
            return 48; // default to 2 days
        }
    }
    
    
    public boolean syncTimeExpired()
    {
        try
        {
            long now = new Date().getTime();
            
            long lastSync = mSharedPreferences.getLong("lastSync", 0);
            
            long interval = getSyncInterval();
            
            long nextSync = lastSync + interval - interval / 3;
            
            if( nextSync < now )
            {
                return true;
            }
            
            // if lastSync is more than a day in the future, schedule a sync
            if( lastSync > (now + 24 * 3600 * 1000) )
            {
                if (Globals.LOGGING) Log.e(Globals.LOG_TAG, "last sync time is too far in the future, scheduling");
                return true;
            }
            
            return false;
        }
        catch( ClassCastException exc )
        {
            if (Globals.LOGGING) Log.e(Globals.LOG_TAG, "error retreiving lastSync preference", exc);
            return true; // doing a sync should re-write the preference and make the happy
        }
    }
    
    public long getSyncInterval()
    {
        try
        {
            return Long.parseLong( mSharedPreferences.getString("syncTime", "1440") ) * 60 * 1000;
        }
        catch( NumberFormatException exc )
        {
            if (Globals.LOGGING) Log.e(Globals.LOG_TAG, "error parsing syncTime preference", exc);
            return 0;
        }
    }
    
    public long getInexactSyncInterval()
    {
        long interval = getSyncInterval();
        
        if( interval == 0)
        {
            return 0;
        }
        else if( interval <= 6 * 3600 * 1000)
        {
            return AlarmManager.INTERVAL_HALF_HOUR;
        }
        else if (interval <= 24 * 3600 * 1000)
        {
            return AlarmManager.INTERVAL_HALF_DAY;
        }
        else
        {
            return AlarmManager.INTERVAL_DAY;
        }
    }
    
    public boolean getNotificationsEnabled()
    {
        try
        {
            return mSharedPreferences.getBoolean("showNotifications", true);
        }
        catch( ClassCastException exc )
        {
            if (Globals.LOGGING) Log.e(Globals.LOG_TAG, "FeedManager.getNotificationsEnabled ", exc );
            return false;
        }
    }
    
    public boolean getTextifyEnabled()
    {
        try
        {
            return mSharedPreferences.getBoolean("textify_on", true);
        }
        catch( ClassCastException exc )
        {
            if (Globals.LOGGING) Log.e(Globals.LOG_TAG, "FeedManager.getTextifyEnabled", exc );
            return false;
        }
    }
    
    public void setPreviousPackageVersion(int packageVersion)
    {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        
        editor.putInt(CONFIG_PREVIOUS_VERSION, packageVersion);
        
        editor.commit();
    }
    
    public boolean getShowZoomControls()
    {
        try
        {
            return mSharedPreferences.getBoolean("show_zoom_controls", true);
        }
        catch(ClassCastException exc)
        {
            if(Globals.LOGGING) Log.e(Globals.LOG_TAG, "FeedConfig.getShowZoomControls", exc);
            return true;
        }
    }
    
    public void syncComplete()
    {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        
        long now = new Date().getTime();
        
        editor.putLong("lastSync", now);
        
        editor.commit();
    }
    
    public void clearNewItemCount()
    {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        
        editor.putInt("newItemCount", 0);
        editor.commit();
    }
    
    public void addNewItemCount(int newItemCount)
    {
        newItemCount += getNewItemCount();
        
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        
        editor.putInt("newItemCount", newItemCount);
        editor.commit();
    }
    
    public int getNewItemCount()
    {
        try
        {
            return mSharedPreferences.getInt("newItemCount", 0);            
        }
        catch(ClassCastException exc)
        {
            if(Globals.LOGGING) Log.e(Globals.LOG_TAG, "FeedConfig.getNewItemCount", exc);
        }
        return 0;
    }
    
}
