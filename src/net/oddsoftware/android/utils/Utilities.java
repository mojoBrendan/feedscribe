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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.oddsoftware.android.feedscribe.Globals;
import android.util.Log;

public class Utilities
{
    
    private static DecimalFormat TWO_DECIMAL_PLACES = new DecimalFormat("0.00");
    
    private static DateFormat mDateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

    
    /**
     * @param str "194" or "0:3:14"
     * @return duration in seconds from string
     */
    public static int parseDuration(String str)
    {
        int duration = 0;
        
        Pattern p = Pattern.compile("[\\d:]+");
        Matcher m = p.matcher(str);
        
        if( m.find() )
        {
            String[] parts = m.group().split(":");
            
            for( int i = 0; i < parts.length; ++i )
            {
                try
                {
                    String number = parts[i];
                    
                    duration = duration * 60;
                    
                    if( number.length() > 0 )
                    {
                        duration += Integer.parseInt(parts[i]);
                    }
                }
                catch(NumberFormatException exc)
                {
                    if(Globals.LOGGING) Log.e(Globals.LOG_TAG, "Error parsing duration from " + str, exc);
                    break;
                }
            }
        }
        else
        {
            if(Globals.LOGGING) Log.e(Globals.LOG_TAG, "Error parsing duration from " + str );
        }
        
        return duration;
    }
    
    public static String formatDuration(int seconds)
    {
        int minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        
        int hours = minutes / 60;
        minutes = minutes - hours * 60;
        
        StringBuilder builder = new StringBuilder(16);
        
        if( hours > 0 )
        {
            builder.append(hours);
            builder.append(':');
        }
        
        if( minutes < 10 && hours > 0)
        {
            builder.append('0');
        }
        
        builder.append(minutes);
        builder.append(':');
        
        if( seconds < 10)
        {
            builder.append('0');
        }
        
        builder.append(seconds);
        
        return builder.toString();
    }
    
    public static String formatFileSize(long bytes)
    {
        StringBuffer stringBuffer = new StringBuffer(10);
        
        if( bytes < 1000 )
        {
            stringBuffer.append(bytes);
            stringBuffer.append(" B");
        }
        else if ( bytes < (1000*1024) )
        {
            double kBytes = bytes / 1024.0;
            stringBuffer.append( TWO_DECIMAL_PLACES.format(kBytes) );
            stringBuffer.append(" kB");
        }
        else if( bytes < (10*1024*1024) )
        {
            double mBytes = bytes / (1024*1024.0);
            
            stringBuffer.append( TWO_DECIMAL_PLACES.format(mBytes) );
            stringBuffer.append(" MB");
        }
        else
        {
            double mBytes = Math.round( bytes / (1024*1024.0) );
            
            stringBuffer.append( (long) mBytes );
            stringBuffer.append(" MB");
        }
        
        return stringBuffer.toString();
    }
    
    
    public static String formatDate(Date pubTime)
    {
        long now = new Date().getTime();
        
        long diff = (now - pubTime.getTime()) / 60000;
        
        if( diff <= 1 )
        {
            return "1 min";
        }
        else if( diff < 60 )
        {
            return diff + " mins";
        }
        else if( diff < (24*60) )
        {
            long hours = diff / 60;
            if( hours == 1 )
            {
                return "1 hour";
            }
            else
            {
                return hours + " hours";
            }
        }
        else if( diff < (7 * 24 * 60) )
        {
            long days = diff / (24 * 60);
            if( days == 1 )
            {
                return "1 day";
            }
            else
            {
                return days + " days";
            }
        }
        else
        {
            return mDateFormat.format( pubTime );
        }
    }

}
