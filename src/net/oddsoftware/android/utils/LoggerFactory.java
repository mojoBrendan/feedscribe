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
