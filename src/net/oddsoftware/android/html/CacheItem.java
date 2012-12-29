package net.oddsoftware.android.html;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

public class CacheItem
{
    
    public long mId;
    public String mUrl;
    public String mFilename;
    public String mETag;
    public long mLastModified;
    public long mHitTime;
    public String mLastUrl;
    public long mExpiresAt;
    
    public static final class CacheItems implements BaseColumns
    {
        private CacheItems()
        {
        }
        
        public static final Uri CONTENT_URI = Uri.parse("content://" + HttpCacheProvider.AUTHORITY + "/cache_items");
        
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/net.oddsoftware.android.html.httpcache.cacheitems";
        
        public static final String ITEM_ID = "_id";
        public static final String URL = "url";
        public static final String FILENAME = "filename";
        public static final String ETAG = "etag";
        public static final String LAST_MODIFIED = "last_modified";
        public static final String HIT_TIME =  "hit_time";
        public static final String LAST_URL =  "last_url";
        public static final String EXPIRES_AT =  "expires_at";
    }
    
    
    private static String[] projection = {
        CacheItems.ITEM_ID,
        CacheItems.URL,
        CacheItems.FILENAME,
        CacheItems.ETAG,
        CacheItems.LAST_MODIFIED,
        CacheItems.HIT_TIME,
        CacheItems.LAST_URL,
        CacheItems.EXPIRES_AT
    };
    
    public static CacheItem getByURL(ContentResolver contentResolver, String url)
    {
        Cursor c = contentResolver.query(
                CacheItems.CONTENT_URI,
                projection,
                CacheItems.URL + "=?",
                new String[] { url },
                null
                );
        
        CacheItem cacheItem = null;
        if( c.moveToFirst() )
        {
            cacheItem = new CacheItem(c);
        }
        
        c.close();
        
        return cacheItem;
    }
    
    public static ArrayList<CacheItem> getAllItems(ContentResolver contentResolver)
    {
        Cursor c = contentResolver.query(CacheItems.CONTENT_URI, projection, null, null, null);
        ArrayList<CacheItem> items = new ArrayList<CacheItem>(c.getCount());
        c.moveToPosition(-1);
        while( c.moveToNext() )
        {
            items.add( new CacheItem( c ) );
        }
        c.close();
        return items;
    }
    
    public CacheItem(String url)
    {
        mId = 0;
        mUrl = url;
        mFilename = "";
        mETag = "";
        mLastModified = 0; 
        mHitTime = 0;
        mLastUrl = "";
        mExpiresAt = 0;
    }
    
    private CacheItem(Cursor c)
    {
        mId = c.getLong( c.getColumnIndexOrThrow( CacheItems.ITEM_ID ));
        mUrl = c.getString( c.getColumnIndexOrThrow( CacheItems.URL ));
        mFilename = c.getString( c.getColumnIndexOrThrow( CacheItems.FILENAME ));
        mETag = c.getString( c.getColumnIndexOrThrow( CacheItems.ETAG ));
        mLastModified = c.getLong( c.getColumnIndexOrThrow( CacheItems.LAST_MODIFIED ));
        mHitTime = c.getLong( c.getColumnIndexOrThrow( CacheItems.HIT_TIME ));
        mLastUrl = c.getString( c.getColumnIndexOrThrow( CacheItems.LAST_URL ));
        mExpiresAt = c.getLong( c.getColumnIndexOrThrow( CacheItems.EXPIRES_AT ));
    }
    
    public void update(ContentResolver contentResolver)
    {
        ContentValues values = new ContentValues();
        values.put(CacheItems.URL, mUrl );
        values.put(CacheItems.FILENAME, mFilename);
        values.put(CacheItems.ETAG, mETag);
        values.put(CacheItems.LAST_MODIFIED, mLastModified);
        values.put(CacheItems.HIT_TIME, mHitTime);
        values.put(CacheItems.LAST_URL, mLastUrl);
        values.put(CacheItems.EXPIRES_AT, mExpiresAt);
        
        if( mId == 0 )
        {
            Uri uri = contentResolver.insert(CacheItems.CONTENT_URI, values);
            mId = Long.parseLong( uri.getLastPathSegment() );
        }
        else
        {
            contentResolver.update(CacheItems.CONTENT_URI, values, CacheItems.ITEM_ID + "=?", new String[]{"" + mId} );
        }
    }

    public void delete(ContentResolver contentResolver)
    {
        contentResolver.delete(CacheItems.CONTENT_URI, "_id=" + mId, null);
    }
}
