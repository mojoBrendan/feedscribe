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

import java.util.HashMap;

import net.oddsoftware.android.feedscribe.Globals;
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
import android.text.TextUtils;
import android.util.Log;

public class PlaylistProvider extends ContentProvider
{
    
    public static final String PLAYLIST_NAME_NAME = "name";
    public static final String PLAYLIST_ID_NAME = "playlist_id";
    public static final String PLAYLIST_TYPE_NAME = "playlist_type";
    public static final String PLAYLIST_ORDER_NAME = "playlist_order";
    public static final String ITEM_ID_NAME = "entry_item_id";
    public static final String ITEM_URI_NAME = "entry_item_uri";
    
    public static final String PLAYLISTS_TABLE_NAME = "playlists";
    public static final String PLAYLIST_ENTRIES_TABLE_NAME = "playlist_entries";
    
    public static final int PLAYLIST_TYPE_AUDIO = 1;
    
    public static final String DATABASE_NAME = "playlists";
    public static final int DATABASE_VERSION = 1;
    
    private static final int PLAYLISTS = 1;
    private static final int PLAYLIST_ID = 2;
    private static final int PLAYLIST_ENTRIES = 3;
    private static final int PLAYLIST_ENTRY_ID = 4;
    
    private static final String AUTHORITY = "net.oddsoftware.android.feedscribe";
    
    private static final UriMatcher sUriMatcher;
    private static HashMap<String, String> sPlaylistsProjectionMap;
    private static HashMap<String, String> sPlaylistEntriesProjectionMap;
    
    public static final String PLAYLIST_CONTENT_TYPE = "vnd.android.cursor.dir/net.oddsoftware.android.feedscribe.playlist";
    public static final String PLAYLIST_ITEM_CONTENT_TYPE = "vnd.android.cursor.item/net.oddsoftware.android.feedscribe.playlist";
    
    public static final String PLAYLIST_ENTRY_CONTENT_TYPE = "vnd.android.cursor.dir/net.oddsoftware.android.feedscribe.playlist_entry";
    public static final String PLAYLIST_ENTRY_ITEM_CONTENT_TYPE = "vnd.android.cursor.item/net.oddsoftware.android.feedscribe.playlist_entry";
    
    public static final Uri PLAYLISTS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/playlists");
    public static final Uri PLAYLIST_ENTRIES_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/playlist_entries");
    
    static
    {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "playlists", PLAYLISTS);
        sUriMatcher.addURI(AUTHORITY, "playlists/#", PLAYLIST_ID);
        sUriMatcher.addURI(AUTHORITY, "playlist_entries/#", PLAYLIST_ENTRIES);
        sUriMatcher.addURI(AUTHORITY, "playlist_entries/#", PLAYLIST_ENTRY_ID);
        
        sPlaylistsProjectionMap = new HashMap<String, String>();
        sPlaylistsProjectionMap.put("_id", "_id");
        sPlaylistsProjectionMap.put(PLAYLIST_NAME_NAME, PLAYLIST_NAME_NAME);
        sPlaylistsProjectionMap.put(PLAYLIST_TYPE_NAME, PLAYLIST_TYPE_NAME);
        
        sPlaylistEntriesProjectionMap = new HashMap<String, String>();
        sPlaylistEntriesProjectionMap.put("_id", "_id");
        sPlaylistEntriesProjectionMap.put(PLAYLIST_ID_NAME, PLAYLIST_ID_NAME);
        sPlaylistEntriesProjectionMap.put(PLAYLIST_ORDER_NAME, PLAYLIST_ORDER_NAME);
        sPlaylistEntriesProjectionMap.put(ITEM_ID_NAME, ITEM_ID_NAME);
        sPlaylistEntriesProjectionMap.put(ITEM_URI_NAME, ITEM_URI_NAME);
    }
    
    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + PLAYLISTS_TABLE_NAME + " ("
                    + "_id INTEGER PRIMARY KEY,"
                    + PLAYLIST_NAME_NAME + " TEXT NOT NULL,"
                    + PLAYLIST_TYPE_NAME + " INTEGER NOT NULL,"
                    + ");");
            
            db.execSQL("CREATE TABLE " + PLAYLIST_ENTRIES_TABLE_NAME + " ("
                    + "_id INTEGER PRIMARY KEY,"
                    + PLAYLIST_ID_NAME + " INTEGER NOT NULL,"
                    + PLAYLIST_ORDER_NAME + " INTEGER NOT NULL,"
                    + ITEM_ID_NAME + " INTEGER NOT NULL,"
                    + ITEM_URI_NAME + " TEXT NOT NULL"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(Globals.LOG_TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + PLAYLISTS_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + PLAYLIST_ENTRIES_TABLE_NAME);
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }
    
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder)
    {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        
        switch( sUriMatcher.match(uri) )
        {
            case PLAYLISTS:
                qb.setProjectionMap(sPlaylistsProjectionMap);
                break;
                
            case PLAYLIST_ID:
                qb.setProjectionMap(sPlaylistsProjectionMap);
                qb.appendWhere("_id=" + uri.getPathSegments().get(1));
                break;
                
            case PLAYLIST_ENTRIES:
                qb.setProjectionMap(sPlaylistEntriesProjectionMap);
                qb.appendWhere("playlist_id=" + uri.getPathSegments().get(1));
                break;
                
            case PLAYLIST_ENTRY_ID:
                qb.setProjectionMap(sPlaylistEntriesProjectionMap);
                qb.appendWhere("_id=" + uri.getPathSegments().get(1));
                break;
                
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        
        String orderBy;
        if( TextUtils.isEmpty(sortOrder) )
        {
            orderBy = "_id ASC";
        }
        else
        {
            orderBy = sortOrder;
        }
        
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        
        return c;
    }
    

    @Override
    public String getType(Uri uri)
    {
        switch( sUriMatcher.match(uri) )
        {
            case PLAYLISTS:
                return PLAYLIST_CONTENT_TYPE;
            case PLAYLIST_ID:
                return PLAYLIST_ITEM_CONTENT_TYPE;
            case PLAYLIST_ENTRIES:
                return PLAYLIST_ENTRY_CONTENT_TYPE;
            case PLAYLIST_ENTRY_ID:
                return PLAYLIST_ENTRY_ITEM_CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }


    @Override
    public Uri insert(Uri uri, ContentValues initialValues)
    {
        int type = sUriMatcher.match(uri);
        
        ContentValues values;
        if (initialValues != null)
        {
            values = new ContentValues(initialValues);
        }
        else
        {
            values = new ContentValues();
        }
        
        if( type == PLAYLISTS )
        {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            long rowId = db.insert(PLAYLISTS_TABLE_NAME, null, values);
            if (rowId > 0)
            {
                Uri notifyUri = ContentUris.withAppendedId(PLAYLISTS_CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(notifyUri, null);
                return notifyUri;
            }
            throw new SQLException("Failed to insert row into " + uri);
        }
        else if ( type == PLAYLIST_ENTRIES)
        {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            long rowId = db.insert(PLAYLIST_ENTRIES_TABLE_NAME, null, values);
            if (rowId > 0)
            {
                Uri notifyUri = ContentUris.withAppendedId(PLAYLIST_ENTRIES_CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(notifyUri, null);
                return notifyUri;
            }
            throw new SQLException("Failed to insert row into " + uri);
        }
        else
        {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs)
    {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri))
        {
            case PLAYLISTS:
                count = db.delete(PLAYLISTS_TABLE_NAME, where, whereArgs);
                break;

            case PLAYLIST_ID:
                String playlistId = uri.getPathSegments().get(1);
                count = db.delete(PLAYLISTS_TABLE_NAME, "_id=" + playlistId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
                
            case PLAYLIST_ENTRIES:
                count = db.delete(PLAYLISTS_TABLE_NAME, where, whereArgs);
                break;

            case PLAYLIST_ENTRY_ID:
                String entryId = uri.getPathSegments().get(1);
                count = db.delete(PLAYLIST_ENTRIES_TABLE_NAME, "_id=" + entryId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs)
    {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri))
        {
            case PLAYLISTS:
                count = db.update(PLAYLISTS_TABLE_NAME, values, where, whereArgs);
                break;

            case PLAYLIST_ID:
                String playlistId = uri.getPathSegments().get(1);
                count = db.update(PLAYLISTS_TABLE_NAME, values, "_id=" + playlistId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;
                
            case PLAYLIST_ENTRIES:
                count = db.update(PLAYLIST_ENTRIES_TABLE_NAME, values, where, whereArgs);
                break;

            case PLAYLIST_ENTRY_ID:
                String entryId = uri.getPathSegments().get(1);
                count = db.update(PLAYLISTS_TABLE_NAME, values, "_id=" + entryId
                        + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                break;

        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

}
