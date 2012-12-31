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
package net.oddsoftware.android.html;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProxySelector;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

import net.oddsoftware.android.feedscribe.Globals;
import net.oddsoftware.android.feedscribe.data.FeedManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;



public class HttpCache
{
    
    private File mCacheDirectory;
    private ContentResolver mContentResolver;
    
    public static final int MAX_ETAG_LENGTH = 200;
    
    // default to 6 hour cache time
    protected static final int DEFAULT_EXPIRES = 6 * 60 * 60 * 1000;
    
    // if it hasn't been accessed for 24 hours
    protected static final int MAX_CACHE_IDLE_TIME = 24 * 60 * 60 * 1000;
    
    // don't cache anything bigger than this (?)
    protected static final int MAX_CONTENT_LENGTH = 300 * 1024;
    
    protected static final boolean ALWAYS_PREFER_CACHE = true;

    
    public HttpCache(Context context)
    {
        mCacheDirectory = context.getCacheDir();
        mContentResolver = context.getContentResolver();
    }
    
    
    public InputStream getResource(String url, boolean forceDownload) throws IOException
    {
        CacheItem cacheItem = CacheItem.getByURL(mContentResolver, url);
        
        if( cacheItem == null )
        {
            cacheItem = new CacheItem(url);
            
            download(cacheItem);
        }
        else
        {
            cacheItem.mHitTime = new Date().getTime();
            cacheItem.update(mContentResolver);
        }
        
        /*// for now we will always use the cache if it's there
        if( (! ALWAYS_PREFER_CACHE) && cacheItem.mExpiresAt < new Date().getTime() )
        {
            download(cacheItem);
        }
        */
        
        File cacheFile = new File( cacheItem.mFilename );
        
        if(! forceDownload )
        {
            if( cacheFile.exists() )
            {
                return new GZIPInputStream( new FileInputStream(cacheFile) );
            }
        }
        
        download(cacheItem);
        
        if( cacheFile.exists() )
        {
            return new GZIPInputStream( new FileInputStream(cacheFile) );
        }
        
        return null;
    }
    
    public String getLastUrl(String url)
    {
        CacheItem cacheItem = CacheItem.getByURL(mContentResolver, url);
        
        if( cacheItem == null )
        {
            return null;
        }
        else
        {
            return cacheItem.mLastUrl;
        }
    }
    
    private void download(CacheItem cacheItem)
    {
        try
        {
            // check to see if file exist, if so check etag and last-modified
            if( cacheItem.mFilename.length() > 0 )
            {
                File f = new File(cacheItem.mFilename);
                
                try
                {
                    InputStream is = new FileInputStream(f);
                    is.close();
                }
                catch( IOException exc )
                {
                    // no file, nuke the cache stats
                    cacheItem.mETag = "";
                    cacheItem.mLastModified = 0;
                }
            }
            else
            {
                cacheItem.mFilename = mCacheDirectory  + File.separator + UUID.randomUUID().toString() + ".html.gz";
            }
            
            
    	    HttpContext httpContext = new BasicHttpContext();
            HttpClient client = createHttpClient();
            HttpUriRequest request = createHttpRequest(cacheItem.mUrl, cacheItem.mETag, cacheItem.mLastModified);
            
            if( request == null || request.getURI() == null || request.getURI().getHost() == null || request.getURI().getHost().length() == 0)
            {
                if (Globals.LOGGING) Log.e(Globals.LOG_TAG, "unable to create http request for url " + cacheItem.mUrl);
                return; // sadness
            }
            
            HttpResponse response = client.execute(request, httpContext);
            
            StatusLine status = response.getStatusLine();
            HttpEntity entity = response.getEntity();
            
            if (status.getStatusCode() == 304)
            {
                if (Globals.LOGGING) Log.d(Globals.LOG_TAG, "received 304 not modified");
                
                cacheItem.mHitTime = new Date().getTime();
                
                cacheItem.update(mContentResolver);
                
                return;
            }
            
            if( status.getStatusCode() == 200 )
            {
                InputStream inputStream = null;
                
                if(entity != null)
                {
                    inputStream = entity.getContent();
                }
                else
                {
                    return;
                }
                
                long contentLength = entity.getContentLength();
                
                if(contentLength > MAX_CONTENT_LENGTH)
                {
                    if(Globals.LOGGING) Log.w(Globals.LOG_TAG, "HttpCache.download item " + cacheItem.mUrl + " content length is too big " + contentLength);
                    return;
                }
                
                Header encodingHeader = entity.getContentEncoding();
                boolean encoded = false;
                
                if (encodingHeader != null)
                {
                    if (encodingHeader.getValue().equalsIgnoreCase("gzip"))
                    {
                        inputStream = new GZIPInputStream(inputStream);
                        encoded = true;
                    }
                    else if (encodingHeader.getValue().equalsIgnoreCase("deflate"))
                    {
                        inputStream = new InflaterInputStream(inputStream);
                        encoded = true;
                    }
                }
                
                File tmpFile = File.createTempFile("httpcache", ".html.gz.tmp", mCacheDirectory);
                OutputStream os = new GZIPOutputStream( new FileOutputStream( tmpFile ) );
                
                byte[] buffer = new byte[4096];
                int count = 0;
                long fileSize = 0;
                while( ( count = inputStream.read(buffer)) != -1 )
                {
                    os.write(buffer, 0, count);
                    fileSize += count;
                }
                inputStream.close();
                os.close();
                
                if( !encoded && contentLength > 0 && fileSize != contentLength )
                {
                    Log.e(Globals.LOG_TAG, "HttpCache.download: content-length: " + contentLength + " but file size: " + fileSize + " aborting");
                    tmpFile.delete();
                    return;
                }
                
                tmpFile.renameTo( new File( cacheItem.mFilename ) );
                
            
                // if the parse was ok, update these attributes
                // ETag: "6050003-78e5-4981d775e87c0"
                Header etagHeader = response.getFirstHeader("ETag");
                if (etagHeader != null)
                {
                    if (etagHeader.getValue().length() < MAX_ETAG_LENGTH)
                    {
                        cacheItem.mETag = etagHeader.getValue();
                    }
                    else
                    {
                        if (Globals.LOGGING) Log.e(Globals.LOG_TAG, "etag length was too big: " + etagHeader.getValue().length());
                    }
                }
                
                // Last-Modified: Fri, 24 Dec 2010 00:57:11 GMT
                Header lastModifiedHeader = response.getFirstHeader("Last-Modified");
                if (lastModifiedHeader != null)
                {
                    try
                    {
                        cacheItem.mLastModified = FeedManager.parseRFC822Date(lastModifiedHeader.getValue()).getTime();
                    }
                    catch(ParseException exc)
                    {
                        if (Globals.LOGGING) Log.e(Globals.LOG_TAG, "unable to parse date", exc);
                    }
                }
                
                // Expires: Thu, 01 Dec 1994 16:00:00 GMT
                Header expiresHeader = response.getFirstHeader("Expires");
                if (expiresHeader != null)
                {
                    try
                    {
                        cacheItem.mExpiresAt = FeedManager.parseRFC822Date(expiresHeader.getValue()).getTime();
                    }
                    catch(ParseException exc)
                    {
                        if (Globals.LOGGING) Log.e(Globals.LOG_TAG, "unable to parse expires", exc);
                    }
                }
                
                long now = new Date().getTime() + DEFAULT_EXPIRES;
                if( cacheItem.mExpiresAt < now )
                {
                    cacheItem.mExpiresAt = now;
                }
                
                HttpUriRequest currentReq = (HttpUriRequest) httpContext.getAttribute( ExecutionContext.HTTP_REQUEST );
                HttpHost currentHost = (HttpHost)  httpContext.getAttribute( ExecutionContext.HTTP_TARGET_HOST );
                String currentUrl = currentHost.toURI() + currentReq.getURI();
                
                if( Globals.LOGGING ) Log.w(Globals.LOG_TAG, "loaded redirect from " + request.getURI().toString() + " to " + currentUrl );

                cacheItem.mLastUrl = currentUrl;
                
                cacheItem.mHitTime = new Date().getTime();
                
                cacheItem.update(mContentResolver);
            }
        }
        catch(IOException exc)
        {
            if( Globals.LOGGING )
            {
                Log.e(Globals.LOG_TAG, "error downloading file to cache", exc );
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
    
    private HttpUriRequest createHttpRequest(String url, String eTag, long lastModified)
    {
        HttpGet request = null;
        
        try
        {
            request = new HttpGet(url);
        }
        catch(IllegalArgumentException exc)
        {
            if (Globals.LOGGING) Log.e(Globals.LOG_TAG, "createHttpRequest: error creating http get", exc);
            return null;
        }
    
        request.setHeader("User-Agent", FeedManager.USER_AGENT);
	    request.setHeader("Accept-Encoding", "gzip,deflate");
    
        // send etag if we have it
        if (eTag != null && eTag.length() > 0)
        {
            request.setHeader("If-None-Match", eTag);
        }
    
        // send If-Modified-Since if we have it
        if( lastModified > 0 )
        {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' GMT'", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            String formattedTime = dateFormat.format(lastModified);
            // If-Modified-Since: Sat, 29 Oct 1994 19:43:31 GMT
            request.setHeader("If-Modified-Since", formattedTime);
        }
        
        return request;
    }
    
    @SuppressWarnings("unused")
    private HttpUriRequest createHttpRequest(String url, long resumeFrom)
    {
        HttpGet request = new HttpGet(url);
    
        request.setHeader("User-Agent", FeedManager.USER_AGENT);
	    request.setHeader("Accept-Encoding", "gzip,deflate");
    
        if( resumeFrom > 0 )
        {
            request.setHeader("Range", "bytes=" + resumeFrom + "-");
        }
        
        
        return request;
    }


    public void seed(String url)
    {
        CacheItem cacheItem = CacheItem.getByURL(mContentResolver, url);
        
        if( cacheItem == null )
        {
            if(Globals.LOGGING) Log.d(Globals.LOG_TAG, "HttpCache.seed: seeding" + url);
            
            cacheItem = new CacheItem(url);
            
            download(cacheItem);
        }
        else
        {
            cacheItem.mHitTime = new Date().getTime();
            cacheItem.update(mContentResolver);
        }
    }
    
    public void maintainCache()
    {
        ArrayList<CacheItem> allItems = CacheItem.getAllItems(mContentResolver);
        long now = new Date().getTime();
        
        @SuppressWarnings("unused")
        long cacheSize = 0;
        
        for(CacheItem item: allItems)
        {
            if( item.mHitTime + MAX_CACHE_IDLE_TIME  < now )
            {
                if(Globals.LOGGING)
                {
                    Log.e(Globals.LOG_TAG, 
                            "HttpCache: maintainCache - removing item " + item.mUrl + 
                            " that hasn't been accessed for " + ((now - item.mHitTime) / 1000) + " seconds "
                            );
                }
                
                try
                {
                    File f = new File(item.mFilename);
                    f.delete();
                    item.delete(mContentResolver);
                }
                catch(SecurityException exc)
                {
                    if(Globals.LOGGING) Log.e(Globals.LOG_TAG, "HttpCache: maintainCache", exc);
                }
            }
            
            try
            {
                File f = new File(item.mFilename);
                cacheSize += f.length();
            }
            catch(SecurityException exc)
            {
                if(Globals.LOGGING) Log.e(Globals.LOG_TAG, "HttpCache: maintainCache", exc);
            }
        }
        
        if(Globals.LOGGING) Log.d(Globals.LOG_TAG, "HttpCache.maintainCache - cache size is " + (cacheSize/1024) + " kilobytes");
    }
    

}
