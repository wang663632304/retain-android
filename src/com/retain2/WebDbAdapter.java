package com.retain2;

import java.io.File;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/*
 * Modified by Ryan Aviles 2010.
 * Copyright (C) 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


public class WebDbAdapter {

    public static final String KEY_TITLE = "title";
    public static final String KEY_FILENAME = "filename";
    public static final String KEY_URL = "url";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_SCROLL = "scroll";
    public static final String KEY_VIEW_OPTION = "landscape";

    public static final int COL_ROWID 		= 0;
    public static final int COL_TITLE 		= 1;
    public static final int COL_FILENAME 	= 2;
    public static final int COL_URL 		= 3;
    public static final int COL_TIMESTAMP 	= 4;
    public static final int COL_SCROLL  	= 5;
    public static final int COL_VIEW_OPTION   = 6;
    public static final String KEY_ROWID = "_id";

    private static final String TAG = "WebDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    
    /**
     * Database creation sql statement
     */
    private static final String DATABASE_CREATE =
            "create table entries (_id integer primary key autoincrement, "
                    + "title text not null, filename text not null, url text not null, timestamp long not null, scroll real DEFAULT 0, landscape int DEFAULT 2);";

    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "entries";
    private static final int DATABASE_VERSION = 3;

    private final Context mCtx;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        	if( oldVersion == newVersion )
        	{
        		Log.d(TAG, "Old version " + oldVersion + " == New version " + newVersion + ". Not upgrading");
        		return;
        	}
        	
        	Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        	
            if( oldVersion == 1 )
            {
            	db.execSQL("ALTER TABLE " + DATABASE_TABLE + " ADD " + KEY_SCROLL + " REAL DEFAULT 0");
            	oldVersion = 2;
            }
            
            if( oldVersion == 2 )
            	db.execSQL("ALTER TABLE " + DATABASE_TABLE + " ADD " + KEY_VIEW_OPTION + " INT DEFAULT 2");
            
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public WebDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    public boolean isOpen()
    {
    	return this.mDb.isOpen();
    }
    
    /**
     * Open the database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public WebDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }
    
    public void close() {
        mDbHelper.close();
    }


    /**
     * Create a new entry. If it is
     * successfully created return the new rowId, otherwise return
     * a -1 to indicate failure.
     * 
     * @param title the title of the entry
     * @param fileName file name on the device
     * @return rowId or -1 if failed
     */
    public long createEntry(String title, String fileName, String url, long timestamp) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_FILENAME, fileName);
        initialValues.put(KEY_URL, url);
        initialValues.put(KEY_TIMESTAMP, new Long(timestamp));
        initialValues.put(KEY_SCROLL, 0F);
        initialValues.put(KEY_VIEW_OPTION, 2);

         long l = mDb.insert(DATABASE_TABLE, null, initialValues);
         return l;
    }

    /**
     * Delete the entry with the given rowId
     * 
     * @param rowId id of the entry to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteEntry(long rowId) {

    	// get the filename for the entry and check if it exists and delete it
    	PageInfo pi = fetchEntry( rowId );
    	return deleteEntry( pi );
    }
    
    private boolean deleteEntry( PageInfo info )
    {
    	try
    	{
	    	if( info != null)
	    	{
	    		String fileName = info.getFilePath();
	    		String url      = info.getUrl();
	    		long   ts       = info.getTimestamp();
	    		Log.d(TAG, "Deleting file: " + fileName + ", url:" + url);
	    		
	    		File f = new File( fileName );
	    		if( f.exists() && f.isFile() )
	    		{
	    			File parent = f.getParentFile();
	    			String tsStr  = String.valueOf(ts);
	
	        		// check if this is an external directory
	    			if( tsStr != null && tsStr.length() > 0 && parent.getName().equals(tsStr) )
	    				deleteDirectory( parent );
	    			else
	    				f.delete();
	    		}

		    	return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + info.getRowId(), null) > 0;
	    	}
    	}
    	catch(Exception e )
    	{
    		Log.d(TAG, "Exception deleting file: " + e.getMessage());
    	}
    	return false;
	    	   	
    }

    public int deleteAll()
    {
    	Cursor c = fetchAllEntries();
    	
    	int numDeleted = 0;
    	
    	while(c.moveToNext())
    	{
    		deleteEntry(c.getLong(COL_ROWID));
    	}
    	c.close();
    	return numDeleted;
    }
    
    /**
     * Updates an item in the database generically. This should probably be tempalted instead
     * @param rowId id of the item in the database
     * @param value field value
     */
    public void updateField( long rowId, ContentValues value)
    {
        mDb.update(DATABASE_TABLE, value, KEY_ROWID + "=" + rowId, null);
    }
    
    /**
     * Return a Cursor over the list of all entries in the database
     * 
     * @return Cursor over all entries
     */
    public Cursor fetchAllEntries() {

        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE,
                KEY_FILENAME, KEY_URL, KEY_TIMESTAMP, KEY_VIEW_OPTION}, null, null, null, null, KEY_TIMESTAMP + " DESC");
    }

    /**
     * Return a Cursor positioned at the entry that matches the given rowId
     * 
     * @param rowId id of entry to retrieve
     * @return Cursor positioned to matching entry, if found
     * @throws SQLException if entry could not be found/retrieved
     */
    public PageInfo fetchEntry(long rowId) throws SQLException {

        Cursor c =

                mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                        KEY_TITLE, KEY_FILENAME, KEY_URL, KEY_TIMESTAMP, KEY_SCROLL, KEY_VIEW_OPTION}, KEY_ROWID + "=" + rowId, null,
                        null, null, null, null);
        if( c == null || c.isAfterLast())
        	return null;
        
        c.moveToFirst();
        
        PageInfo info = new PageInfo(
        						this,
						        c.getLong(WebDbAdapter.COL_ROWID),
						        c.getString(WebDbAdapter.COL_TITLE),
						        c.getString(WebDbAdapter.COL_URL),
						        c.getString(WebDbAdapter.COL_FILENAME),
						        c.getLong(WebDbAdapter.COL_TIMESTAMP),
						        c.getFloat(WebDbAdapter.COL_SCROLL),
						        c.getInt(WebDbAdapter.COL_VIEW_OPTION));
        
        c.close();
        return info;

    }

    static public boolean deleteDirectory(File path) {
        if( path.exists() ) {
          File[] files = path.listFiles();
          for(int i=0; i<files.length; i++) {
             if(files[i].isDirectory()) {
               deleteDirectory(files[i]);
             }
             else {
               files[i].delete();
             }
          }
        }
        return( path.delete() );
      }    
    
}
