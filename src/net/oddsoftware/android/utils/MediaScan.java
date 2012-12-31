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

import net.oddsoftware.android.feedscribe.Globals;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.util.Log;

public class MediaScan implements MediaScannerConnectionClient
{
    public static void ScanFile(Context context, String name, String contentType)
    {
        new MediaScan(context, name, contentType);
    }
    
    
    private String mName;
    private String mContentType;
    private MediaScannerConnection mConnection;
    
    private MediaScan(Context context, String name, String contentType)
    {
        mName = name;
        mContentType = contentType;
        mConnection = new MediaScannerConnection(context, this);
        mConnection.connect();
    }
    
    
    @Override
    public void onMediaScannerConnected()
    {
        try
        {
            if( Globals.LOGGING) Log.d(Globals.LOG_TAG, "media scanner connected for " + mName);
            mConnection.scanFile(mName, mContentType);
        }
        catch(IllegalStateException exc)
        {
            if( Globals.LOGGING) Log.e(Globals.LOG_TAG, "error notifying media scanner", exc);
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri)
    {
        
    }

}
