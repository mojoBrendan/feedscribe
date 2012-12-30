package net.oddsoftware.android.feedscribe;

import net.oddsoftware.android.utils.Logger;
import net.oddsoftware.android.utils.LoggerFactory;

public class Globals
{
    public static final int VERSION_CODE = 18;
    
    public static final int PREVIOUS_VERSION_CODE = 14; // if <= then show changelog
    
    public static final boolean LOGGING = false;
    
    public static final String LOG_TAG = "feedscribe";
    
    public static final Logger LOG = LoggerFactory.getLogger(LOG_TAG);

    public static final int NOTIFICATION_SYNCING = 1;
    public static final int NOTIFICATION_NEW_ITEMS = 2;
    public static final int NOTIFICATION_PLAYING = 22;    
    
    static
    {
        LOG.setLevel(Logger.VERBOSE);
    }
}
