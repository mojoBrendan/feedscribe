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
