package com.retain;

import android.content.ContentValues;

public class PageInfo {

	public long getRowId() {
		return rowId;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	public String getFilePath() {
		return filePath;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public float getScroll() {
		return scroll;
	}
	
	public int getViewOption() {
		return landscape;
	}
	
	public void setViewOption(int newViewOption) 
	{
		landscape = newViewOption;
		ContentValues args = new ContentValues();
        args.put(WebDbAdapter.KEY_VIEW_OPTION, newViewOption);

        dbAdapter.updateField( rowId, args );
	}
	
	public void setTitle( String newTitle )
	{
		title = newTitle;
        ContentValues args = new ContentValues();
        args.put(WebDbAdapter.KEY_TITLE, newTitle);

        dbAdapter.updateField( rowId, args);
	}
	
    public void setScrollBy(float newScrollBy)
    {
    	scroll = newScrollBy;
        ContentValues args = new ContentValues();
        args.put(WebDbAdapter.KEY_SCROLL, newScrollBy);

        dbAdapter.updateField( rowId, args);
    }
    	

	private long rowId;
	private String title;
	private String url;
	private String filePath;
	private long timestamp;
	private float scroll;
	private int landscape;
	
	private WebDbAdapter dbAdapter;
	
	public PageInfo( WebDbAdapter dbAdapter, long rowId, String title, String url, String filePath, long timestamp, float scroll, int landscape)
	{
		this.dbAdapter = dbAdapter;
		this.rowId = rowId;
		this.title = title;
		this.url   = url;
		this.filePath = filePath;
		this.timestamp = timestamp;
		this.scroll = scroll;
		this.landscape = landscape;
	}
	
}
