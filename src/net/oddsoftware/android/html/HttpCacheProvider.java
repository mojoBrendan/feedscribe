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

import java.util.HashMap;

import net.oddsoftware.android.html.CacheItem.CacheItems;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class HttpCacheProvider extends ContentProvider
{
    public static final String AUTHORITY = "net.oddsoftware.android.feedscribe.html.HttpCacheProvider";
    
    private static final String DATABASE_NAME = "httpcache";
    
    private static final int DATABASE_VERSION = 1;
    
    private static final String CACHE_ITEMS_TABLE_NAME = "cache_items";
    
    private static final UriMatcher sUriMatcher;
    
    private static final int CACHE_ITEMS = 1;
    
    private static HashMap<String, String> cacheItemsProjectionMap;
    
    static
    {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, CACHE_ITEMS_TABLE_NAME, CACHE_ITEMS);
        
        cacheItemsProjectionMap = new HashMap<String, String>();
        cacheItemsProjectionMap.put(CacheItems.ITEM_ID, CacheItems.ITEM_ID);
        cacheItemsProjectionMap.put(CacheItems.URL, CacheItems.URL);
        cacheItemsProjectionMap.put(CacheItems.FILENAME, CacheItems.FILENAME);
        cacheItemsProjectionMap.put(CacheItems.ETAG, CacheItems.ETAG);
        cacheItemsProjectionMap.put(CacheItems.LAST_MODIFIED, CacheItems.LAST_MODIFIED);
        cacheItemsProjectionMap.put(CacheItems.HIT_TIME, CacheItems.HIT_TIME);
        cacheItemsProjectionMap.put(CacheItems.LAST_URL, CacheItems.LAST_URL);
        cacheItemsProjectionMap.put(CacheItems.EXPIRES_AT, CacheItems.EXPIRES_AT);
    }
    
    private static class DatabaseHelper extends SQLiteOpenHelper
    {
        DatabaseHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL("CREATE TABLE " + CACHE_ITEMS_TABLE_NAME + " (" +
            		CacheItems.ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            		CacheItems.URL + " STRING NOT NULL, " +
            		CacheItems.FILENAME + " STRING NOT NULL, " +
            		CacheItems.ETAG + " STRING NOT NULL, " +  
            		CacheItems.LAST_MODIFIED + " INTEGER NOT NULL, " +
            		CacheItems.HIT_TIME + " INTEGER NOT NULL, " +
            		CacheItems.LAST_URL + " STRING NOT NULL, " +
            		CacheItems.EXPIRES_AT + " INTEGER NOT NULL " +
            		")");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            db.execSQL("DROP TABLE IF EXISTS " + CACHE_ITEMS_TABLE_NAME );
            onCreate(db);
        }
    }
    
    private DatabaseHelper dbHelper;
    
    @Override
    public boolean onCreate()
    {
        dbHelper = new DatabaseHelper(getContext());
        return false;
    }


    @Override
    public int delete(Uri uri, String where, String[] whereArgs)
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri))
        {
            case CACHE_ITEMS:
                count = db.delete(CACHE_ITEMS_TABLE_NAME, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri)
    {
        switch (sUriMatcher.match(uri))
        {
            case CACHE_ITEMS:
                return CacheItems.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues)
    {
        if (sUriMatcher.match(uri) != CACHE_ITEMS)
        {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        
        ContentValues values;
        if (initialValues != null)
        {
            values = new ContentValues(initialValues);
        }
        else
        {
            values = new ContentValues();
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(CACHE_ITEMS_TABLE_NAME, CacheItems.FILENAME, values);
        if (rowId > 0)
        {
            Uri noteUri = ContentUris.withAppendedId(CacheItems.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri))
        {
            case CACHE_ITEMS:
                qb.setTables(CACHE_ITEMS_TABLE_NAME);
                qb.setProjectionMap(cacheItemsProjectionMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs)
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri))
        {
            case CACHE_ITEMS:
                count = db.update(CACHE_ITEMS_TABLE_NAME, values, where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

}
