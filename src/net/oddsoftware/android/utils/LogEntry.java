package net.oddsoftware.android.utils;

public class LogEntry
{
    public long mTimestamp;
    public int mLevel;
    public String mMessage;
    public Throwable mException;
    
    public LogEntry(long timestamp, int level, String message, Throwable exc)
    {
        mTimestamp = timestamp;
        mLevel = level;
        mMessage = message;
        mException = exc;
    }
    
    public String toString()
    {
        return mMessage;
    }
 
}
