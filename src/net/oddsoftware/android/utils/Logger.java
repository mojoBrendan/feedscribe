package net.oddsoftware.android.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;

import net.oddsoftware.android.feedscribe.Globals;

import android.util.Log;


public class Logger
{
    public static final int VERBOSE = Log.VERBOSE;
    public static final int DEBUG   = Log.DEBUG;
    public static final int INFO    = Log.INFO;
    public static final int WARN    = Log.WARN;
    public static final int ERROR   = Log.ERROR;
    public static final int ASSERT  = Log.ASSERT;
    public String mTag;
    
    public boolean mAndroid; 
    
    public int mLevel;
    
    public LogEntry[] mEntries;
    int mSlot;
    int mMaxSize;
    
    public Logger(String tag)
    {
        mTag = tag;
        
        mLevel = INFO;
        mAndroid = true;
        
        mSlot = 0;
        mMaxSize = 1024;
        
        mEntries = new LogEntry[mMaxSize];
    }
    
    public void setLevel(int level)
    {
        mLevel = level;
    }
    
    public final void log(int level, String message, Throwable exc)
    {
        if (mAndroid && (Globals.LOGGING || level >= mLevel))
        {
            if (exc != null)
            {
                StringWriter sw = new StringWriter();
                
                exc.printStackTrace(new PrintWriter(sw));
                
                message = message + "\n" + sw;
            }
            Log.println(level, mTag, message);
        }
        
        if (level >= mLevel)
        {
            LogEntry entry = new LogEntry(System.currentTimeMillis(), level, message, exc);
            
            addEntry(entry);
        }
    }
    
    public final boolean v()
    {
        return mLevel <= VERBOSE;
    }
    
    public final void v(String message)
    {
        log(VERBOSE, message, null);
    }
    
    public final void v(String message, Throwable exc)
    {
        log(VERBOSE, message, exc);
    }
    
    public final boolean d()
    {
        return mLevel <= DEBUG;
    }
    
    public final void d(String message)
    {
        log(DEBUG, message, null);
    }
    
    public final void d(String message, Throwable exc)
    {
        log(DEBUG, message, exc);
    }
    
    public final boolean i()
    {
        return mLevel <= INFO;
    }
    
    public final void i(String message)
    {
        log(INFO, message, null);
    }
    
    public final void i(String message, Throwable exc)
    {
        log(INFO, message, exc);
    }
    
    public final boolean w()
    {
        return mLevel <= WARN;
    }
    
    public final void w(String message)
    {
        log(WARN, message, null);
    }
    
    public final void w(String message, Throwable exc)
    {
        log(WARN, message, exc);
    }
    
    public final boolean e()
    {
        return mLevel <= ERROR;
    }
    
    public final void e(String message)
    {
        log(ERROR, message, null);
    }
    
    public final void e(String message, Throwable exc)
    {
        log(ERROR, message, exc);
    }

    public final boolean a()
    {
        return mLevel <= ASSERT;
    }
    
    public final void a(String message)
    {
        log(ASSERT, message, null);
    }
    
    public final void a(String message, Throwable exc)
    {
        log(ASSERT, message, exc);
    }
    
    private synchronized void addEntry(LogEntry entry)
    {
        mEntries[mSlot] = entry;
        
        mSlot = (mSlot+1) % mMaxSize;
    }
    
    public synchronized void getEntries(Collection<LogEntry> out)
    {
        for(int i = mSlot; i < mMaxSize; ++ i)
        {
            if(mEntries[i] != null)
            {
                out.add(mEntries[i]);
            }
        }
        
        for(int i = 0; i < mSlot; ++i)
        {
            if(mEntries[i] != null)
            {
                out.add(mEntries[i]);
            }
        }
    }

}
