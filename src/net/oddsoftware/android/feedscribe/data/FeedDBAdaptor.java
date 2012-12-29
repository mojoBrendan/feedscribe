package net.oddsoftware.android.feedscribe.data;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import net.oddsoftware.android.feedscribe.Globals;

public class FeedDBAdaptor
{

    public static final String KEY_FEED_ID       = "feed_id";
    public static final String KEY_FEED_URL      = "url";
    public static final String KEY_FEED_LINK     = "link";
    public static final String KEY_FEED_NAME     = "name";
    public static final String KEY_FEED_TYPE     = "type";
    
    public static final String KEY_FEED_TITLE       = "title";
    public static final String KEY_FEED_DESCRIPTION = "description";
    public static final String KEY_FEED_IMAGE_URL   = "image_url";
    
    public static final String KEY_LAST_HIT      = "last_hit";
    public static final String KEY_TTL           = "ttl";
    public static final String KEY_ETAG          = "etag";
    public static final String KEY_LAST_MODIFIED = "last_modified";
    public static final String KEY_LAST_URL      = "last_url";
    
    public static final String KEY_FEED_ITEM_ID = "item_id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_LINK = "link";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_ENCLOSURE = "enclosure";
    public static final String KEY_GUID = "guid";
    public static final String KEY_PUB_DATE = "pub_date";
    public static final String KEY_FLAGS = "flags";
    public static final String KEY_ORIGINAL_LINK = "original_link";
    public static final String KEY_CLEAN_DESCRIPTION = "clean_description";
    public static final String KEY_CLEAN_TITLE = "clean_title";
    public static final String KEY_IMAGE_URL = "image_url";
    public static final String KEY_FEED_ITEM_POSITION = "position";

    public static final String KEY_ENCLOSURE_ID               = "enclosure_id";
    public static final String KEY_ENCLOSURE_URL              = "url";
    public static final String KEY_ENCLOSURE_LENGTH           = "length";
    public static final String KEY_ENCLOSURE_CONTENT_TYPE     = "content_type";
    public static final String KEY_ENCLOSURE_FILE_PATH        = "file_path";
    public static final String KEY_ENCLOSURE_DOWNLOAD_TIME    = "download_time";
    public static final String KEY_ENCLOSURE_DURATION         = "duration";
    public static final String KEY_ENCLOSURE_POSITION         = "position";
    
    
    public static final String KEY_IMAGE_ID = "image_id";
    public static final String KEY_IMAGE_DATA = "data";
    public static final String KEY_IMAGE_TIMESTAMP = "timestamp";
    public static final String KEY_IMAGE_PERSISTENT = "persistent";
    
    public static final String KEY_UPDATE_AUTOMATICALLY = "update_automatically";
    public static final String KEY_DISPLAY_FULL_ARTICLE = "display_full_article";
    public static final String KEY_CACHE_FULL_ARTICLE = "cache_full_article";
    public static final String KEY_CACHE_IMAGES = "cache_images";
    public static final String KEY_TEXTIFY = "textify";
    
    public static final String KEY_DOWNLOAD_ID               = "download_id";
    

    private DatabaseHelper mDbHelper;
    SQLiteDatabase mDb;


    private static final String[] FEED_ITEM_QUERY = 
                new String[]
                    {
                        KEY_FEED_ITEM_ID, KEY_FEED_ID, KEY_TITLE, KEY_LINK, 
                        KEY_DESCRIPTION, KEY_AUTHOR, KEY_ENCLOSURE, KEY_GUID,
                        KEY_PUB_DATE, KEY_FLAGS, KEY_ORIGINAL_LINK, KEY_CLEAN_DESCRIPTION, KEY_CLEAN_TITLE, KEY_IMAGE_URL,
                        KEY_FEED_ITEM_POSITION
                    };
    
    private static final String[] SHORT_FEED_ITEM_QUERY = 
                new String[]
                    {
                        KEY_FEED_ITEM_ID, KEY_LINK, KEY_PUB_DATE, KEY_CLEAN_TITLE, KEY_ENCLOSURE, KEY_GUID, KEY_FLAGS
                    };
    
    private static final String[] FEED_QUERY = 
                new String[]
                    {
                        KEY_FEED_ID, KEY_FEED_TYPE, KEY_FEED_URL, KEY_FEED_NAME, KEY_FEED_LINK, KEY_FEED_DESCRIPTION, KEY_FEED_IMAGE_URL,
                    };
    
    
    private static final String[] ENCLOSURE_QUERY = 
                new String[]
                    {
                        KEY_ENCLOSURE_ID, KEY_FEED_ITEM_ID, KEY_ENCLOSURE_URL, KEY_ENCLOSURE_LENGTH, KEY_ENCLOSURE_CONTENT_TYPE, 
                        KEY_ENCLOSURE_FILE_PATH, KEY_ENCLOSURE_DOWNLOAD_TIME, KEY_ENCLOSURE_DURATION, KEY_ENCLOSURE_POSITION
                    };
    
    private static final String DATABASE_NAME = "feeds";
    private static final String FEEDS_TABLE = "feeds";
    private static final String FEED_STATUS_TABLE = "feed_status";
    private static final String FEED_ITEMS_TABLE = "feed_items";
    private static final String IMAGES_TABLE = "images";
    private static final String ENCLOSURES_TABLE = "enclosures";
    private static final String DOWNLOADS_TABLE = "downloads";
    private static final String FEED_SETTINGS_TABLE = "feed_settings";
    private static final int DATABASE_VERSION = 4;

    private final Context mCtx;
    
    public static final String CREATE_FEEDS =
                    "CREATE TABLE feeds (" +
                    "   feed_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "   type INTEGER NOT NULL," +
                    "   url STRING NOT NULL," +
                    "   name NOT NULL," +
                    "   link STRING NOT NULL," +
                    "   description NOT NULL," +
                    "   image_url NOT NULL" +
                    ");";

    private static final String CREATE_FEED_STATUS = 
                    "CREATE TABLE feed_status (" +
                    "   feed_id INTEGER NOT NULL," +
                    "   last_hit INTEGER NOT NULL," +
                    "   ttl INTEGER NOT NULL," +
                    "   etag STRING NOT NULL," +
                    "   last_modified INTEGER NOT NULL," +
                    "   last_url INTEGER NOT NULL" +
                    ");";

    private static final String CREATE_FEED_ITEMS = 
                    "CREATE TABLE feed_items (" +
                    "   item_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "   feed_id INTEGER NOT NULL," +
                    "   title STRING NOT NULL," +
                    "   link STRING NOT NULL," +
                    "   description STRING NOT NULL," +
                    "   author STRING NOT NULL," +
                    "   guid STRING NOT NULL," +
                    "   pub_date STRING NOT NULL," +  // TODO - this should be integer
                    "   flags INTEGER NOT NULL," +
                    "   original_link STRING NOT NULL," +
                    "   clean_description STRING NOT NULL," +
                    "   clean_title STRING NOT NULL," +
                    "   image_url STRING NOT NULL," +
                    "   enclosure STRING NOT NULL," +
                    "   position NOT NULL DEFAULT -1" +
                    ");";
    
    private static final String CREATE_FEED_ITEMS_FEED_ID_INDEX = 
                    "CREATE INDEX IF NOT EXISTS " +
                    "    feed_items_feed_id_index ON feed_items " +
                    "    ( feed_id ); "; 
    
    private static final String CREATE_FEED_ITEMS_FEED_ID_FLAGS_INDEX = 
                    "CREATE INDEX IF NOT EXISTS " +
                    "    feed_items_feed_id_flags_index ON feed_items " +
                    "    ( feed_id, flags ); "; 

    private static final String CREATE_IMAGES =
                    "CREATE TABLE images (" +
                    "   image_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "   image_url STRING NOT NULL," +
                    "   timestamp INTEGER NOT NULL," +
                    "   persistent INTEGER NOT NULL," +
                    "   data blob NOT NULL" +
                    ");";
    
    private static final String CREATE_ENCLOSURES =
                    "CREATE TABLE enclosures (" +
                    "   enclosure_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "   item_id INTEGER NOT NULL," +
                    "   url STRING NOT NULL," +
                    "   length INTEGER NOT NULL," +
                    "   content_type STRING NOT NULL," +
                    "   file_path STRING NOT NULL," +
                    "   download_time INTEGER NOT NULL," +
                    "   duration INTEGER NOT NULL," +
                    "   position INTEGER NOT NULL" +
                    ");";
    
    private static final String CREATE_ENCLOSURES_ITEM_ID_INDEX = 
                    "CREATE INDEX IF NOT EXISTS " +
                    "    enclosures_item_id_index ON enclosures " +
                    "    ( item_id ); "; 
    
    private static final String CREATE_DOWNLOADS =
                    "CREATE TABLE downloads (" +
                    "   download_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "   enclosure_id INTEGER NOT NULL" +
                    ");";
    
    
    private static final String CREATE_FEED_SETTINGS =
                    "CREATE TABLE feed_settings (" +
                    "   feed_id INTEGER PRIMARY KEY," +
                    "   update_automatically INTEGER NOT NULL," +
                    "   display_full_article INTEGER NOT NULL," +
                    "   cache_full_article INTEGER NOT NULL," +
                    "   cache_images INTEGER NOT NULL," +
                    "   textify INTEGER NOT NULL" +
                    ");";
                    

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL( CREATE_FEEDS );
            db.execSQL( CREATE_FEED_STATUS );
            db.execSQL( CREATE_FEED_ITEMS );
            db.execSQL( CREATE_IMAGES );
            db.execSQL( CREATE_ENCLOSURES );
            db.execSQL( CREATE_DOWNLOADS );
            db.execSQL( CREATE_FEED_SETTINGS );
            db.execSQL( CREATE_FEED_ITEMS_FEED_ID_INDEX );
            db.execSQL( CREATE_FEED_ITEMS_FEED_ID_FLAGS_INDEX );
            db.execSQL( CREATE_ENCLOSURES_ITEM_ID_INDEX );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            /*
            db.execSQL("ALTER TABLE feed_content ADD COLUMN image_url STRING NOT NULL DEFAULT ''");
            db.execSQL( CREATE_IMAGES );
            */
            if (Globals.LOGGING) Log.w(Globals.LOG_TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            
            if( oldVersion <= 1 )
            {
                db.execSQL( CREATE_FEED_SETTINGS );
            }
            
            if( oldVersion <= 2 )
            {
                db.execSQL( CREATE_FEED_ITEMS_FEED_ID_INDEX );
                db.execSQL( CREATE_FEED_ITEMS_FEED_ID_FLAGS_INDEX );
                db.execSQL( CREATE_ENCLOSURES_ITEM_ID_INDEX );
            }
            
            if( oldVersion <= 3 )
            {
                db.execSQL( "ALTER TABLE " + FEED_ITEMS_TABLE + " ADD COLUMN " + KEY_FEED_ITEM_POSITION + " INTEGER NOT NULL DEFAULT -1");
            }
            
            // resetDB(db);
        }
        
        public void resetDB(SQLiteDatabase db)
        {
            db.execSQL("DROP TABLE IF EXISTS feeds");
            db.execSQL("DROP TABLE IF EXISTS images");
            db.execSQL("DROP TABLE IF EXISTS enclosures");
            db.execSQL("DROP TABLE IF EXISTS feed_status");
            db.execSQL("DROP TABLE IF EXISTS feed_items");
            db.execSQL("DROP TABLE IF EXISTS downloads");
            db.execSQL("DROP TABLE IF EXISTS feed_settings");
            onCreate(db);
        }
    }
    
    public void resetDB()
    {
        mDbHelper.resetDB(mDb);
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public FeedDBAdaptor(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialisation call)
     * @throws SQLException if the database could be neither opened or created
     */
    public synchronized FeedDBAdaptor open() throws SQLException
    {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        
        return this;
    }

    public synchronized void close() {
        mDbHelper.close();
    }


    public synchronized boolean updateFeedStatus(FeedStatus status)
    {
        ContentValues values = new ContentValues();
        values.put(KEY_LAST_HIT, status.mLastHit.getTime());
        values.put(KEY_TTL, status.mTTL);
        values.put(KEY_ETAG, status.mETag);
        values.put(KEY_LAST_MODIFIED, status.mLastModified.getTime());
        values.put(KEY_LAST_URL, status.mLastURL);
            
        if (mDb.update(FEED_STATUS_TABLE, values, KEY_FEED_ID + "=" + status.mFeedId, null) > 0)
        {
            return true;
        }
        else
        {
            values.put(KEY_FEED_ID, status.mFeedId);
            
            return mDb.insert(FEED_STATUS_TABLE, null, values) >= 0;
        }
    }
    
    public synchronized boolean updateFeedImageURL(Feed feed)
    {
        ContentValues values = new ContentValues();
        values.put(KEY_FEED_IMAGE_URL, feed.mImageURL);
            
        return mDb.update(
                        FEEDS_TABLE,
                        values,
                        KEY_FEED_ID + "=?",
                        new String[]{ "" + feed.mId }
                        ) >0;
    }
    
    public synchronized FeedStatus getFeedStatus(long feedId)
    {
        Cursor c = mDb.query(FEED_STATUS_TABLE,
                new String[] { KEY_LAST_HIT, KEY_TTL, KEY_ETAG, KEY_LAST_MODIFIED, KEY_LAST_URL },
                KEY_FEED_ID + "=" + feedId,
                null, null, null, null
                );
        
        if (c.getCount() == 0)
        {
            c.close();
            return null;
        }
        else
        {
            c.moveToFirst();
            Date lastHit = new Date();
            Date lastModified = new Date();
            
            lastHit.setTime( c.getLong(0));
            int ttl = c.getInt(1);
            String etag = c.getString(2);
            lastModified.setTime( c.getLong(3));
            String lastURL = c.getString(4);
            
            c.close();
            
            return new FeedStatus(feedId, lastHit, ttl, etag, lastModified, lastURL);
        }
    }
    
    
    public synchronized FeedItem getFeedItem(long itemId)
    {
        Cursor c = mDb.query(FEED_ITEMS_TABLE,
                FEED_ITEM_QUERY, KEY_FEED_ITEM_ID + "=?",
                new String[] { "" + itemId }, null, null, null
                );
        
        if (c.getCount() == 0)
        {
            c.close();
            return null;
        }
        else
        {
            c.moveToFirst();
            
            FeedItem item = parseFeedItem(c, 0);
            c.close();
            
            return item;
        }
    }
    
    public synchronized ArrayList<FeedItem> getFeedItems(long feedId)
    {
        Cursor c = mDb.query(FEED_ITEMS_TABLE,
                FEED_ITEM_QUERY, KEY_FEED_ID + "=" + feedId,
                null, null, null, null
                );
        
        ArrayList<FeedItem> items = new ArrayList<FeedItem>();
        
        while(c.moveToNext())
        {
            FeedItem item = parseFeedItem(c, 0);
            if (item != null)
            {
                items.add(item);
            }
        }
        c.close();
        
        return items;
    }
    
    
    public synchronized ArrayList<FeedItem> getFeedItems(int feedId, String guid, String link)
    {
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        
        if (guid != null)
        {
            builder.appendWhere(KEY_GUID + "=");
            builder.appendWhereEscapeString(guid);
        }
        
        if (link != null)
        {
            if (guid != null)
            {
                builder.appendWhere(" AND ");
            }
            
            builder.appendWhere(KEY_LINK + "=");
            builder.appendWhereEscapeString(link);
        }
        
        builder.setTables(FEED_ITEMS_TABLE);
        Cursor c = builder.query(mDb, FEED_ITEM_QUERY, KEY_FEED_ID + "=" + feedId,
                null, null, null, null
                );
        
        ArrayList<FeedItem> items = new ArrayList<FeedItem>();
        
        while(c.moveToNext())
        {
            FeedItem item = parseFeedItem(c, 0);
            if (item != null)
            {
                items.add(item);
            }
        }
        c.close();
        
        return items;
    }
    
    
    
    public synchronized boolean updateFeedItem(FeedItem item)
    {
        ContentValues values = new ContentValues();
        values.put(KEY_FEED_ID, item.mFeedId);
        values.put(KEY_TITLE, item.mTitle);
        values.put(KEY_LINK, item.mLink);
        values.put(KEY_DESCRIPTION, item.mDescription);
        values.put(KEY_AUTHOR, item.mAuthor);
        values.put(KEY_ENCLOSURE, item.mEnclosureURL);
        values.put(KEY_GUID, item.mGUID);
        values.put(KEY_PUB_DATE, item.mPubDate.getTime());
        values.put(KEY_FLAGS, item.mFlags);
        values.put(KEY_ORIGINAL_LINK, item.mOriginalLink);
        values.put(KEY_CLEAN_DESCRIPTION, item.mCleanDescription);
        values.put(KEY_CLEAN_TITLE, item.mCleanTitle);
        values.put(KEY_IMAGE_URL, item.mImageURL);
        values.put(KEY_FEED_ITEM_POSITION, item.mPosition);
            
        if (mDb.update(FEED_ITEMS_TABLE, values, KEY_FEED_ITEM_ID + "=" + item.mId, null) > 0)
        {
            return true;
        }
        else
        {
            if (item.mId > 0)
            {
                if (Globals.LOGGING) Log.e(Globals.LOG_TAG, "updateFeedItem item id is " + item.mId + " but we are about to insert");
            }
            
            long id = mDb.insert(FEED_ITEMS_TABLE, null, values);
            
            if (id > 0)
            {
                item.mId = id;
                return true;
            }
            else
            {
                return false;
            }
        }
    }
    
    public synchronized boolean updateFeedItemFlags(FeedItem item)
    {
        ContentValues values = new ContentValues();
        values.put(KEY_FLAGS, item.mFlags);
            
        if (mDb.update(FEED_ITEMS_TABLE, values, KEY_FEED_ITEM_ID + "=" + item.mId, null) > 0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    public synchronized boolean deleteFeedItem(long itemId)
    {
        return mDb.delete(FEED_ITEMS_TABLE, KEY_FEED_ITEM_ID + "=?", new String[]{ "" + itemId } ) > 0;
    }
    
    
    private FeedItem parseFeedItem(Cursor c, int startIndex)
    {
        Date pubDate = new Date();
        
        long itemId = c.getLong(startIndex + 0);
        int feedId = c.getInt(startIndex + 1);
        String title = c.getString(startIndex + 2);
        String link = c.getString(startIndex + 3);
        String description = c.getString(startIndex + 4);
        String author = c.getString(startIndex + 5);
        String enclosure = c.getString(startIndex + 6);
        String guid = c.getString(startIndex + 7);
        
        pubDate.setTime(c.getLong(startIndex + 8));
        
        long flags = c.getLong(startIndex + 9);
        
        String originalLink = c.getString(startIndex + 10);
        
        String cleanDescription = c.getString(startIndex + 11);
        
        String cleanTitle = c.getString(startIndex + 12);
        
        String imageURL = c.getString(startIndex + 13);
        
        long position = c.getLong(startIndex + 14);
        
        return new FeedItem(itemId, feedId, title, cleanTitle, link, originalLink, description, cleanDescription, author, enclosure, guid, pubDate, flags, imageURL, position);
    }
    
    
    private ShortFeedItem parseShortFeedItem(Cursor c, int startIndex)
    {
        long itemId = c.getLong(startIndex + 0);
        String link = c.getString(startIndex + 1);
        long pubDate = c.getLong(startIndex + 2);
        String title = c.getString(startIndex + 3);
        String enclosure = c.getString(startIndex + 4);
        String guid = c.getString(startIndex + 5);
        int flags = c.getInt(startIndex + 6);
        
        return new ShortFeedItem(itemId, link, pubDate, title, enclosure, guid, flags);
    }
    
    public synchronized ArrayList<ShortFeedItem> getShortFeedItems(long feedId, String[] searchTerms, boolean includeDeleted)
    {
        ArrayList<String> whereArgs = new ArrayList<String>();
        
        String whereClause = KEY_FEED_ID + "=?";
        whereArgs.add("" + feedId);
        
        if( ! includeDeleted )
        {
             whereClause += " AND (" + KEY_FLAGS + "&" + FeedItem.FLAG_DELETED + ")=0 ";
        }
        
        if( searchTerms != null )
        {
            for(int i = 0; i < searchTerms.length; ++i )
            {
                whereClause += " AND ( " + KEY_CLEAN_TITLE + " LIKE ? OR " + KEY_CLEAN_DESCRIPTION + " LIKE ? ) ";
                String term = "%" + searchTerms[i] + "%";
                whereArgs.add( term );
                whereArgs.add( term );
            }
        }
        
        Cursor c = mDb.query(FEED_ITEMS_TABLE,
                SHORT_FEED_ITEM_QUERY, whereClause,
                whereArgs != null ? whereArgs.toArray(new String[]{}) : null, null, null, null
                );
        
        ArrayList<ShortFeedItem> items = new ArrayList<ShortFeedItem>();
        
        while(c.moveToNext())
        {
            ShortFeedItem item = parseShortFeedItem(c, 0);
            item.mFeedId = feedId;
            if (item != null)
            {
                items.add(item);
            }
        }
        c.close();
        
        return items;
    }
    
    public synchronized void updateImageTime(String url, long timestamp)
    {
        ContentValues values = new ContentValues();
        values.put(KEY_IMAGE_TIMESTAMP, timestamp);
        
        mDb.update(IMAGES_TABLE, values, KEY_IMAGE_URL + "= ?", new String[]{ url } );
    }
    
    public synchronized void insertImage(String url, long timestamp, boolean persistent, byte[] data)
    {
        ContentValues values = new ContentValues();
        values.put(KEY_IMAGE_URL, url);
        values.put(KEY_IMAGE_TIMESTAMP, timestamp);
        values.put(KEY_IMAGE_PERSISTENT, persistent);
        values.put(KEY_IMAGE_DATA, data);
        
        long id = mDb.insert(IMAGES_TABLE, null, values);
        
        if( id <= 0 )
        {
            if( Globals.LOGGING ) Log.e(Globals.LOG_TAG, "error inserting image " + url);
        }
    }
    
    public synchronized boolean hasImage(String url)
    {
        Cursor c = mDb.query(IMAGES_TABLE, new String[]{ KEY_IMAGE_ID },
                KEY_IMAGE_URL + "= ?", new String[]{ url },
                null, null, null
                );
        
        boolean answer = c.getCount() != 0;
        c.close();
        
        return answer;
    }
    
    public synchronized byte[] getImage(String url)
    {
        Cursor c = mDb.query(IMAGES_TABLE, new String[]{ KEY_IMAGE_DATA },
                KEY_IMAGE_URL + "= ?", new String[]{ url },
                null, null, null, null
                );
        
        byte[] answer = null;
        if( c.moveToFirst() )
        {
            answer = c.getBlob(0);
        }
        c.close();
        
        return answer;
    }
    
    public synchronized void deleteOlderImages(long timestamp)
    {
        mDb.delete(
                IMAGES_TABLE,
                KEY_IMAGE_TIMESTAMP + " < ? AND " + KEY_IMAGE_PERSISTENT + "=0",
                new String[]{ "" + timestamp}
                );
    }
    
    public synchronized boolean addFeed(Feed feed)
    {
        ContentValues values = new ContentValues();
        values.put(KEY_FEED_TYPE, feed.mType );
        values.put(KEY_FEED_URL, feed.mURL );
        values.put(KEY_FEED_NAME, feed.mName );
        values.put(KEY_FEED_LINK, feed.mLink );
        values.put(KEY_FEED_DESCRIPTION, feed.mDescription );
        values.put(KEY_FEED_IMAGE_URL, feed.mImageURL );
        
        long id = mDb.insert(FEEDS_TABLE, null, values);
        
        if( id > 0 )
        {
            feed.mId = id;
            return true;
        }
        else
        {
            return false;
        }
    }
    
    public synchronized boolean setFeedName(long feedId, String newName)
    {
        ContentValues values = new ContentValues();
        values.put(KEY_FEED_NAME, newName);
        
        String feedIdString = new String();
        feedIdString += feedId;
        
        int count = mDb.update(FEEDS_TABLE, values, "feed_id=?", new String[]{ feedIdString } );
        
        return count > 0;
    }
    
    public synchronized Feed getFeed(long id)
    {
        Cursor c = mDb.query(FEEDS_TABLE,
                FEED_QUERY, KEY_FEED_ID + " = " + id, 
                null, null, null, null
                );
        
        Feed feed = null;
        if(c.moveToNext())
        {
            feed = parseFeed(c, 0);
        }
        c.close();
        
        return feed;
    }
    
    public synchronized Feed getFeedByItemId(long itemId)
    {
        StringBuilder sql = new StringBuilder(500);
        
        sql.append("SELECT ");
        
        for(String s: FEED_QUERY)
        {
            sql.append(" feeds.");
            sql.append(s);
            sql.append(",");
        }
        sql.append("1 ");
        
        sql.append( " FROM feeds,feed_items " +
        		" WHERE feeds.feed_id=feed_items.feed_id " +
        		" AND feed_items.item_id="
        		);
        sql.append(itemId);
        
        Cursor c = mDb.rawQuery(sql.toString(), null);
        
        Feed feed = null;
        if( c.moveToNext() )
        {
            feed = parseFeed(c, 0);
        }
        c.close();
        
        return feed;
    }
    
    
    public synchronized ArrayList<Feed> getFeeds()
    {
        Cursor c = mDb.query(FEEDS_TABLE, FEED_QUERY,
                null, null, null, null, null
                );
        
        ArrayList<Feed> feeds = new ArrayList<Feed>();
        
        while (c.moveToNext())
        {
            Feed feed = parseFeed(c, 0);
            if( feed != null )
            {
                feeds.add(feed);
            }
        }
        c.close();
        
        return feeds;
    }
    
    public synchronized ArrayList<Feed> getFeeds(int feedTypes)
    {
        Cursor c = mDb.query(FEEDS_TABLE, FEED_QUERY, "( " + KEY_FEED_TYPE + " & " + feedTypes + " ) > 0 ",
                null, null, null, null, null
                );
        
        ArrayList<Feed> feeds = new ArrayList<Feed>();
        
        while (c.moveToNext())
        {
            Feed feed = parseFeed(c, 0);
            if( feed != null )
            {
                feeds.add(feed);
            }
        }
        c.close();
        
        return feeds;
    }
    
    public synchronized Feed getFeedByURL(String url)
    {
        Cursor c = mDb.query(FEEDS_TABLE,
                FEED_QUERY, KEY_FEED_URL + "=? ",
                new String[] { url },
                null, null, null
                );
        
        Feed feed = null;
        if(c.moveToNext())
        {
            feed = parseFeed(c, 0);
        }
        c.close();
        
        return feed;
    }
    
    private Feed parseFeed(Cursor c, int startIndex)
    {
        long feedId = c.getLong(startIndex + 0);
        int feedType = c.getInt(startIndex + 1);
        String feedURL = c.getString(startIndex + 2);
        String feedName = c.getString(startIndex + 3);
        String feedLink = c.getString(startIndex + 4);
        String feedDescription = c.getString(startIndex + 5);
        String feedImageURL = c.getString(startIndex + 6);
        
        return new Feed(feedId, feedType, feedURL, feedName, feedLink, feedDescription, feedImageURL );
    }
    
    
    public synchronized boolean updateEnclosure(Enclosure enclosure)
    {
        ContentValues values = new ContentValues();
        
        values.put(KEY_FEED_ITEM_ID, enclosure.mItemId);
        values.put(KEY_ENCLOSURE_URL, enclosure.mURL);
        values.put(KEY_ENCLOSURE_LENGTH, enclosure.mLength);
        values.put(KEY_ENCLOSURE_CONTENT_TYPE, enclosure.mContentType);
        values.put(KEY_ENCLOSURE_FILE_PATH, enclosure.mDownloadPath);
        values.put(KEY_ENCLOSURE_DOWNLOAD_TIME, enclosure.mDownloadTime);
        values.put(KEY_ENCLOSURE_DURATION, enclosure.mDuration);
        values.put(KEY_ENCLOSURE_POSITION, enclosure.mPosition);
        
        boolean success = false;
        if( enclosure.mId <= 0 )
        {
            enclosure.mId = mDb.insert(ENCLOSURES_TABLE, null, values);
            success = (enclosure.mId >= 0);
        }
        else
        {
            values.put(KEY_ENCLOSURE_ID, enclosure.mId);
            success = mDb.update(ENCLOSURES_TABLE, values, KEY_ENCLOSURE_ID + "=" + enclosure.mId, null) > 0;
        }
        
        return success;
    }
    
    public synchronized Enclosure getEnclosure(long enclosureId)
    {
        Cursor c = mDb.query(
                ENCLOSURES_TABLE, ENCLOSURE_QUERY, KEY_ENCLOSURE_ID + "=" + enclosureId,
                null, null, null, null
                );
        
        Enclosure enclosure = null;
        if( c.moveToNext() )
        {
            enclosure = parseEnclosure(c, 0);
        }
        c.close();
        
        return enclosure;
    }
    
    public synchronized Enclosure getEnclosure(String url)
    {
        Cursor c = mDb.query(
                ENCLOSURES_TABLE, ENCLOSURE_QUERY, KEY_ENCLOSURE_URL + "=?", new String[] { url },
                null, null, null
                );
        
        Enclosure enclosure = null;
        if( c.moveToNext() )
        {
            enclosure = parseEnclosure(c, 0);
        }
        c.close();
        
        return enclosure;
    }
    
    public synchronized Enclosure getEnclosureFromItemId(long itemId)
    {
        Cursor c = mDb.query(
                ENCLOSURES_TABLE, ENCLOSURE_QUERY, KEY_FEED_ITEM_ID + "=" + itemId,
                null, null, null, null
                );
        
        Enclosure enclosure = null;
        if( c.moveToNext() )
        {
            enclosure = parseEnclosure(c, 0);
        }
        c.close();
        
        return enclosure;
    }
    
    private Enclosure parseEnclosure(Cursor c, int startIndex)
    {
        long enclosureId = c.getLong(startIndex + 0);
        long itemId = c.getLong(startIndex + 1);
        String url = c.getString(startIndex + 2);
        long length = c.getLong(startIndex + 3);
        String contentType = c.getString(startIndex + 4);
        String path = c.getString(startIndex + 5);
        long downloadTime = c.getLong(startIndex + 6);
        long duration = c.getLong(startIndex + 7);
        long position = c.getLong(startIndex + 8);
        
        return new Enclosure(enclosureId, itemId, url, length, contentType, path, downloadTime, duration, position);
    }
    
    public synchronized HashMap<Long, FeedEnclosureInfo> getFeedEnclosureInfo(String enclosureType)
    {
        HashMap<Long, FeedEnclosureInfo> infos = new HashMap<Long, FeedEnclosureInfo>();
        
        String sql =  "SELECT feed_items.flags, enclosures.download_time, feed_items.feed_id " +
        		"FROM feed_items, enclosures " +
        		"WHERE feed_items.item_id=enclosures.item_id " +
        		" AND (feed_items.flags & " + FeedItem.FLAG_DELETED + ")=0 " +
        		" AND enclosures.content_type LIKE ?";
        Cursor c = mDb.rawQuery(sql, new String[] { enclosureType + "%" } );
        
        while( c.moveToNext() )
        {
            int itemFlags = c.getInt(0);
            long downloadTime = c.getLong(1);
            long feedId = c.getLong(2);
            
            FeedEnclosureInfo info = infos.get(feedId);
            if( info == null )
            {
                info = new FeedEnclosureInfo();
                info.mFeedId = feedId;
                infos.put(feedId, info);
            }
            
            info.mItemCount++;
            
            
            if( (itemFlags & FeedItem.FLAG_READ) == 0)
            {
                info.mNewCount++;
            
                if( downloadTime > 0 )
                {
                    info.mUnplayedDownloadCount++;
                }
            }
            
            if( downloadTime > 0 )
            {
                info.mDownloadedCount++;
            }
        }
        
        c.close();
        
        return infos;
    }
    
    public synchronized HashMap<Long, FeedEnclosureInfo> getFeedsWithoutEnclosuresInfo()
    {
        HashMap<Long, FeedEnclosureInfo> infos = new HashMap<Long, FeedEnclosureInfo>();
        
        String sql =  "SELECT feed_items.flags, feed_items.feed_id " +
        		"FROM feed_items " +
        		"WHERE (feed_items.flags & " + FeedItem.FLAG_DELETED + ")=0 " +
        		" AND (SELECT COUNT(*) FROM enclosures WHERE enclosures.item_id==feed_items.item_id) == 0"
        		;
        Cursor c = mDb.rawQuery(sql, null );
        
        while( c.moveToNext() )
        {
            int itemFlags = c.getInt(0);
            long feedId = c.getLong(1);
            
            FeedEnclosureInfo info = infos.get(feedId);
            if( info == null )
            {
                info = new FeedEnclosureInfo();
                info.mFeedId = feedId;
                infos.put(feedId, info);
            }
            info.mItemCount++;
            
            
            if( (itemFlags & FeedItem.FLAG_READ) == 0)
            {
                info.mNewCount++;
            }
        }
        
        c.close();
        
        return infos;
    }
    
    
    public synchronized ArrayList<FeedItemEnclosureInfo> getFeedItemEnclosureInfo(long feedId, String enclosureType)
    {
        ArrayList<FeedItemEnclosureInfo> infos = new ArrayList<FeedItemEnclosureInfo>();
        
        String sql =  "SELECT feed_items.flags, enclosures.download_time, feed_items.item_id, " +
        		" enclosures.enclosure_id, feed_items.clean_title, feed_items.clean_description, " +
        		" enclosures.duration, enclosures.position " +
        		" FROM feed_items, enclosures WHERE feed_items.item_id=enclosures.item_id " +
        		" AND enclosures.content_type LIKE ? AND feed_items.feed_id=? " +
        		" AND (feed_items.flags & " + FeedItem.FLAG_DELETED + ")=0 " +
        		" ORDER BY feed_items.pub_date DESC ";
        Cursor c = mDb.rawQuery(sql, new String[] { enclosureType + "%", "" + feedId } );
        
        while( c.moveToNext() )
        {
            FeedItemEnclosureInfo info = new FeedItemEnclosureInfo();
            
            info.mFeedId = feedId;
            info.mItemFlags = c.getInt(0);
            long downloadTime = c.getLong(1);
            info.mItemId = c.getLong(2);
            info.mEnclosureId = c.getLong(3);
            info.mCleanTitle = c.getString(4);
            info.mCleanDescription = c.getString(5);
            info.mDuration = c.getLong(6);
            info.mSeekPosition = c.getLong(7);
            info.mDownloaded = (downloadTime > 0);
            
            infos.add(info);
        }
        
        c.close();
        
        return infos;
    }
    
    public synchronized void deleteFeed(long feedId)
    {
        mDb.beginTransaction();
        try
        {
            mDb.delete(FEED_ITEMS_TABLE, KEY_FEED_ID + "=" + feedId, null);
            mDb.delete(FEEDS_TABLE, KEY_FEED_ID + "=" + feedId, null);
            mDb.setTransactionSuccessful();
        }
        finally
        {
            mDb.endTransaction();
        }
    }
    
    public synchronized void deleteEnclosure(long enclosureId)
    {
        mDb.delete(ENCLOSURES_TABLE, KEY_ENCLOSURE_ID + "=" + enclosureId, null);
    }
    
    public synchronized long addDownload(long enclosureId)
    {
        ContentValues values = new ContentValues();
        values.put(KEY_ENCLOSURE_ID, enclosureId);
        return mDb.insert(DOWNLOADS_TABLE, null, values);
    }
    
    public synchronized ArrayList<Download> getAllDownloads()
    {
        Cursor c = mDb.query(DOWNLOADS_TABLE, new String[] { KEY_DOWNLOAD_ID, KEY_ENCLOSURE_ID },
                null, null, null, null, null);
        
        ArrayList<Download> downloads = new ArrayList<Download>();
        while( c.moveToNext() )
        {
            Download download = new Download();
            download.mId = c.getLong(0);
            download.mEnclosureId = c.getLong(1);
            downloads.add(download);
        }
        c.close();
        
        return downloads;
    }
    
    public synchronized void deleteDownload(long downloadId)
    {
        mDb.delete(DOWNLOADS_TABLE, KEY_DOWNLOAD_ID + "=" + downloadId, null);
    }
    
    public synchronized void deleteDownloadByEnclosure(long enclosureId)
    {
        mDb.delete(DOWNLOADS_TABLE, KEY_ENCLOSURE_ID + "=" + enclosureId, null);
    }

    public synchronized FeedSettings getFeedSettings(long feedId)
    {
        Cursor c = mDb.query(FEED_SETTINGS_TABLE,
                new String[] {
                    KEY_UPDATE_AUTOMATICALLY, KEY_DISPLAY_FULL_ARTICLE, KEY_CACHE_FULL_ARTICLE,
                    KEY_CACHE_IMAGES, KEY_TEXTIFY
                },
                "feed_id=" + feedId,
                null, null, null, null );
        
        FeedSettings feedSettings = null;
        
        if(c.moveToNext())
        {
            feedSettings = new FeedSettings();
            feedSettings.mFeedId = feedId;
            feedSettings.mUpdateAutomatically = c.getInt(0) != 0;
            feedSettings.mDisplayFullArticle  = c.getInt(1) != 0;
            feedSettings.mCacheFullArticle    = c.getInt(2) != 0;
            feedSettings.mCacheImages         = c.getInt(3) != 0;
            feedSettings.mTextify             = c.getInt(4) != 0;
            
        }
        
        c.close();
       
        return feedSettings;
    }
    
    public synchronized void updateFeedSettings(FeedSettings feedSettings)
    {
        
        ContentValues values = new ContentValues();
        values.put(KEY_FEED_ID, feedSettings.mFeedId);
        values.put(KEY_UPDATE_AUTOMATICALLY, feedSettings.mUpdateAutomatically);
        values.put(KEY_DISPLAY_FULL_ARTICLE, feedSettings.mDisplayFullArticle);
        values.put(KEY_CACHE_FULL_ARTICLE, feedSettings.mCacheFullArticle);
        values.put(KEY_CACHE_IMAGES, feedSettings.mCacheImages);
        values.put(KEY_TEXTIFY, feedSettings.mTextify);
            
        if (mDb.update(FEED_SETTINGS_TABLE, values, KEY_FEED_ID + "=" + feedSettings.mFeedId, null) > 0)
        {
            // yay
        }
        else
        {
            mDb.insert(FEED_SETTINGS_TABLE, null, values);
        }
    }

    public Context getContext()
    {
        return mCtx;
    }

    public synchronized void setFeedItemsRead(long feedId)
    {
        mDb.execSQL(
                "UPDATE feed_items SET flags=(flags|" + FeedItem.FLAG_READ + ") " +
                " WHERE feed_id=" + feedId + 
                " AND (flags&" + FeedItem.FLAG_READ + ")=0"
                );
    }
    
    /*
        return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
        return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
    */
}
