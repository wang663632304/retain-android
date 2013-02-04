package com.retain2;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class DateViewCursorBinder implements SimpleCursorAdapter.ViewBinder {

	private static final String LOG_TAG = "WebListAdapter";
	private Date mCurrentDate;
	DateViewCursorBinder()
	{
		mCurrentDate = new Date();
	}
	
	public boolean setViewValue (View view, Cursor cursor, int columnIndex)
	{

		switch( columnIndex )
		{
		case WebDbAdapter.COL_TITLE:
			{
				String str = cursor.getString(columnIndex);
				((TextView) view).setText(str);
				return true;
			}	

		case WebDbAdapter.COL_URL:
			try 
			{
				String str = cursor.getString(columnIndex);
				URI uri = new URI( str );
				String host = uri.getHost();
				host = host.replaceAll("^www\\.", "");
				((TextView) view).setText(host);
			}
			catch(URISyntaxException urie)
			{
				Log.e(LOG_TAG, urie.getMessage());
			}
			
			return true;
			

		case WebDbAdapter.COL_TIMESTAMP:
			{
				long l = cursor.getLong(columnIndex);
				String str = AppUtils.getDateStr(l, mCurrentDate);
				
				TextView tv = (TextView) view;
				tv.setText( tv.getText() + " - " + str);
				return true;			
			}	
		}
		
		return false;
	}

}

