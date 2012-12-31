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
package net.oddsoftware.android.feedscribe.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.utils.MediaScan;

public class Downloader extends Object
{
    
    private Context mContext;
    
    ExecutorService mThreadPool;
    
    Thread mAwakeChecker;
    
    long mAwakeInterval = 5000;
    
    int mDownloadCount;
    
    FeedManager mFeedManager;
    
    WakeLock mWakeLock;
    
    // TODO - should check connection time
    
    private static Downloader mInstance = null;
    public static synchronized Downloader getInstance(Context ctx)
    {
        if( mInstance == null )
        {
            mInstance = new Downloader(ctx);
        }
        return mInstance;
    }
    
    private Downloader(Context ctx)
    {
        mContext = ctx;
        
        mThreadPool = Executors.newFixedThreadPool(4);
        
        mFeedManager = FeedManager.getInstance(mContext);
        
        mDownloadCount = 0;
        
        mWakeLock = ((PowerManager) ctx.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FeedScribe.Downloader");
        mWakeLock.setReferenceCounted(false);
        
        mAwakeChecker = new Thread() 
        {
            @Override
            public void run() {
                try
                {
                    while( true )
                    {
                        Thread.sleep(mAwakeInterval);
                        checkAwake();
                    }
                }
                catch(InterruptedException exc)
                {
                    return;
                }
            }
        };
        
        mAwakeChecker.start();
    }
    
    private void checkAwake()
    {
        processDownloads();
        
        Globals.LOG.v("Downloader.checkAwake number of downloads: " + mDownloadCount );
        if( mDownloadCount > 0 )
        {
            mWakeLock.acquire(mAwakeInterval * 5); // timed here to release it if something goes horribly wrong
            Globals.LOG.v("Downloader.checkAwake aquiring wake lock: " + mWakeLock.isHeld() );
        }
        else
        {
            mWakeLock.release();
            Globals.LOG.v("Downloader.checkAwake releasing wake lock: " + mWakeLock.isHeld() );
        }
    }
    
    public void processDownloads()
    {
        ArrayList<Download> downloads = mFeedManager.getDownloads();
        
        int count = 0;
        for(Download download: downloads)
        {
            if( ! download.mEnqueued )
            {
                download.mEnqueued = true;
                
                Enclosure enclosure = mFeedManager.getEnclosure(download.mEnclosureId);
                
                if(enclosure != null)
                {
                    mThreadPool.execute( new DownloadThread( download, enclosure ) );
                }
                else
                {
                    Globals.LOG.w("Downloader.processDownloads - enclosure " + download.mEnclosureId + " is null");
                    download.mCancelled = true;
                }
            }
            
            if( ! (download.mPaused || download.mCancelled) )
            {
                count++;
            }
        }
        
        mDownloadCount = count;
        
    }
    
    private class DownloadThread implements Runnable
    {
        Download mDownload;
        Enclosure mEnclosure;
        
        public DownloadThread(Download download, Enclosure enclosure)
        {
            mDownload = download;
            mEnclosure = enclosure;
        }
        
        public void run()
        {
            if( mDownload.mPaused )
            {
                return;
            }
            
            // do download in background
            performDownload();
            
            // only do one of these at a time
            synchronized (mFeedManager)
            {
                if( mDownload.mCancelled )
                {
                    boolean deleted = mFeedManager.deleteDownload( mDownload, mEnclosure );
                    
                    Globals.LOG.i("Downloader - deleting cancelled download " + mEnclosure.mDownloadPath + " success " + deleted);
                }
                else if( mDownload.mSuccess )
                {
                    MediaPlayer mp = MediaPlayer.create(mContext, Uri.fromFile( new File(mEnclosure.mDownloadPath)));
                    
                    if(mp != null)
                    {
                        mEnclosure.mDuration = mp.getDuration();
                        mp.release();
                    }
                    else
                    {
                        Globals.LOG.w("unable to determine duration for '" + mEnclosure.mDownloadPath + "'");
                    }
                    
                    mFeedManager.downloadComplete(mDownload);
                }
                else
                {
                    // download stopped for some reason ....
                }
                
                mFeedManager.updateEnclosure(mEnclosure);
            }
        }
        
        private void performDownload()
        {
            mDownload.mSuccess = false;
            
            if( mEnclosure.mDownloadPath.length() == 0 )
            {
                // this should be safe in the background thread
                if( ! mFeedManager.createFile(mEnclosure) )
                {
                    mDownload.mSuccess = false;
                    return;
                }
                else
                {
                    mFeedManager.updateEnclosure(mEnclosure);
                }
            }
            
            // enclosure file is set, start the download
            try
            {
                mDownload.mInProgress = true;
                
                File outputFile = new File( mEnclosure.mDownloadPath );
                
                long existingFileSize = outputFile.length();
                
                
                HttpClient client = createHttpClient();
                HttpUriRequest request = createHttpRequest(mEnclosure.mURL, existingFileSize, null, null);
                HttpResponse response = client.execute(request);
                
                StatusLine status = response.getStatusLine();
                HttpEntity entity = response.getEntity();
                
                Globals.LOG.d("http request: " + mEnclosure.mURL);
                Globals.LOG.d("http response code: " + status);
    
                
                if( entity == null )
                {
                    return;
                }
                
                InputStream is = entity.getContent();
                OutputStream os = null;
                
                long contentLength = 0;
                
                boolean fileComplete = false;
                
                // partial content - append file
                if( status.getStatusCode() == 206 )
                {
                    os = new FileOutputStream( outputFile, true);
                    
                    contentLength = existingFileSize + entity.getContentLength();
                }
                else if( status.getStatusCode() == 200 )
                {
                    os = new FileOutputStream( outputFile );
                    contentLength = entity.getContentLength();
                    existingFileSize = 0; // reset
                }
                else if( status.getStatusCode() == 416 ) // requested range not satisfiable = file complete
                {
                    contentLength = 0;
                    fileComplete = true;
                }
                else
                {
                    // abort
                    Globals.LOG.e("Error downloading url: " + mEnclosure.mURL + "response " + status);
                    return;
                }
                
                mDownload.mSize = contentLength;
                mDownload.mDownloaded = existingFileSize;
                long size = existingFileSize;
                
                byte[] buffer = new byte[4096];
                int bytes;
                boolean stopped = false;
                
                try
                {
                    while( ( bytes = is.read(buffer) ) != -1  && ! stopped && ! fileComplete)
                    {
                        size += bytes;
                    
                        mDownload.mDownloaded = size;
                        if( size > mDownload.mSize )
                        {
                            mDownload.mSize = size;
                        }
                    
                        os.write(buffer, 0, bytes);
                    
                        stopped = mDownload.mPaused || mDownload.mCancelled;
                    }
                }
                finally
                {
                    // close streams even if there was an exception
                    is.close();
                
                    if( os != null )
                    {
                        os.close();
                    }
                }
                
                if( ! stopped )
                {
                    mEnclosure.mLength = size;
                    mEnclosure.mDownloadTime = new Date().getTime();
                    
                    MediaScan.ScanFile(mContext, mEnclosure.mDownloadPath, mEnclosure.mContentType);

                    mDownload.mSuccess = true;
                }
                
                mDownload.mInProgress = false;
                
                return;
            }
            catch( MalformedURLException exc )
            {
                Globals.LOG.e("error parsing download url", exc);
                mDownload.mSuccess = false;
            }
            catch( IOException exc )
            {
                Globals.LOG.e("error downloading enclosure", exc);
                // if this was the only error, reschedule for downloading
                mDownload.mSuccess = false;
                mDownload.mEnqueued = false;
            }
        }
    }
        
    private HttpClient createHttpClient()
    {
        // use apache http client lib to set parameters from feedStatus
        DefaultHttpClient client = new DefaultHttpClient();
    
        // set up proxy handler
        ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(
                client.getConnectionManager().getSchemeRegistry(), 
                ProxySelector.getDefault());  
        client.setRoutePlanner(routePlanner);
        
        return client;
    }
    
    private HttpUriRequest createHttpRequest(String url, long resumeFrom, String eTag, Date lastModified)
    {
        HttpGet request = new HttpGet(url);
    
        request.setHeader("User-Agent", FeedManager.USER_AGENT);
    
        if( resumeFrom > 0 )
        {
            request.setHeader("Range", "bytes=" + resumeFrom + "-");
        }
        
        // send etag if we have it
        if (eTag != null && eTag.length() > 0)
        {
            request.setHeader("If-None-Match", eTag);
        }
    
        // send If-Modified-Since if we have it
        if( lastModified != null && lastModified.getTime() > 0 )
        {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' GMT'", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            String formattedTime = dateFormat.format(lastModified);
            // If-Modified-Since: Sat, 29 Oct 1994 19:43:31 GMT
            request.setHeader("If-Modified-Since", formattedTime);
        }
        
        return request;
    }
    
}
