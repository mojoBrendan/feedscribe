package net.oddsoftware.android.utils;

import java.util.HashMap;

public class LoggerFactory
{
    public static HashMap<String, Logger> mLoggers = new HashMap<String, Logger>();
    
    
    public synchronized static Logger getLogger(String name)
    {
        Logger logger = mLoggers.get(name);
        
        if (logger == null)
        {
            logger = new Logger(name);
            
            mLoggers.put(name, logger);
        }
        
        return logger;
    }
    
}
