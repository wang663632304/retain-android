package com.retain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.retain.dialog.DeleteDialog;
import com.retain.dialog.RenameDialog;

public class ViewController
{
	
	private int mCurrentViewType; // as defined in SettingsManager
	private WebDbAdapter mDbAdapter;
	private Activity mActivity;
	
	private ListView mListView;
	private ExpandableListView mExpandableList;
	private MyExpandableListAdapter mExpandableListAdapter;	
	
	private final Cursor mDataCursor;
	
	private static final String LOG_TAG = "ViewController";
	

	private long mSelectedItemId = 0;
	
	public ViewController( Activity activity )
	{
		mDbAdapter = new WebDbAdapter(activity);
		mDbAdapter.open();
		
		mActivity = activity;
		mCurrentViewType = SettingsManager.ORDER_BY_NOTSET;
		
		mDataCursor = mDbAdapter.fetchAllEntries();
		mActivity.startManagingCursor(mDataCursor);
	}
	
	public void setViewType( int viewType )
	{

        if( mDataCursor.getCount() == 0)
        {
        	TextView tv = new TextView(mActivity);
        	tv.setText(mActivity.getString(com.retain.R.string.no_items));
        	
        	showView( tv );
        	return;
        }		
		
		// don't need to rebuild the listview type
		if( mCurrentViewType == viewType)
		{
			Log.d(LOG_TAG, "Already viewing view type=" + viewType);
			return;
		}
        
		Log.d(LOG_TAG, "Setting view type " + viewType );
		
		mCurrentViewType = viewType;
		
		switch( viewType )
		{
		case SettingsManager.ORDER_BY_SITE:
			buildExpandableListView();
			
			break;
			
		case SettingsManager.ORDER_BY_TIME:
			
			if( mListView == null )
				mListView = buildListView();	

	        String[] from = new String[] { WebDbAdapter.KEY_TITLE, WebDbAdapter.KEY_URL, WebDbAdapter.KEY_TIMESTAMP };
	        int[] to = new int[] { R.id.rowTitle, R.id.rowSiteTime, R.id.rowSiteTime };
	        
	        // Now create an array adapter and set it to display using our row
	        SimpleCursorAdapter entries =
	            new SimpleCursorAdapter(mActivity, R.layout.entry_row, mDataCursor, from, to);
	        
	        entries.setViewBinder( new DateViewCursorBinder() );
	        
	        mListView.setAdapter(entries);			
			showView(mListView);
			mListView.requestLayout();
			
			break;
			
		default:
			Log.e(LOG_TAG, "Unknown view type " + viewType);
		}
	}
	
	public void refreshListViewData()
	{
		if( mDataCursor == null || mDataCursor.isClosed() )
			return;
		else
			mDataCursor.requery();
		
		if( mCurrentViewType == SettingsManager.ORDER_BY_SITE)
			buildExpandableListView();
	}
	
	public void onDestroyCalled()
	{
		if( mDataCursor != null )
		{
			mActivity.stopManagingCursor(mDataCursor);
			mDataCursor.close();
		}
		if( mDbAdapter != null )
			mDbAdapter.close();
	}
	
	private void showView(View view)
	{
		removeViews();
		
		LinearLayout ll = (LinearLayout) mActivity.findViewById(com.retain.R.id.mainLinearLayout);
		ll.addView( view );			
	}

	
	public void deleteAll()
	{
		mDbAdapter.deleteAll();
		refreshListViewData();
	}
	
	private void buildExpandableListView()
	{
		mExpandableList = new ExpandableListView(mActivity);
		mExpandableList.setId(SettingsManager.ORDER_BY_SITE);
		mExpandableList.setVisibility(ExpandableListView.VISIBLE);
		mExpandableList.setEnabled(true);
		mExpandableList.setLongClickable(true);			
		
		mExpandableListAdapter = new MyExpandableListAdapter(mActivity, mExpandableList, mDataCursor );
		mExpandableListAdapter.refreshData();
		mExpandableList.setAdapter(mExpandableListAdapter);
		
		mExpandableList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
		    @Override
		    public boolean onChildClick(ExpandableListView parent,
		            View v, int groupPosition, int childPosition, long id) {
		        if (groupPosition >= 0 && childPosition >= 0) {
		        	try
		        	{
		        		showContent( mExpandableListAdapter.getRowIdForGroupAndChild( groupPosition, childPosition ) );
		        	}
		        	catch(Exception e)
		        	{
		        		Log.e(LOG_TAG, "Exception " + e.getMessage());
		        		AppUtils.showToastLong(mActivity, "Unable to load item. Please reload Retain and try again");
		        	}

	        	
		        	return true;
		        } 
		        return false;
		    }
		    });		
	
		mExpandableList.setOnItemLongClickListener( new MyOnItemLongClickListener() );
		mExpandableList.setOnCreateContextMenuListener( new MyOnCreateContextMenuListener() );
	
		showView(mExpandableList);
		mExpandableList.requestLayout();
	}
	
	public boolean onContextItemSelected(String title )
	{
		
		if( mSelectedItemId == 0 )
			return false;
		
		PageInfo info = mDbAdapter.fetchEntry(mSelectedItemId);
		if( info == null)
			return false;
		
		String wTitle = info.getTitle();
		String wUrl   = info.getUrl();
	    
		if( title == mActivity.getString(R.string.web_open_url))
		{
			final Intent viewIntent = new Intent("android.intent.action.VIEW", Uri.parse(wUrl));  
			mActivity.startActivity(viewIntent);
		}
		else if( title == mActivity.getString(R.string.web_delete))
		{
			handleDeleteRequest(mSelectedItemId);
		}
		else if( title == mActivity.getString(R.string.web_rename))
		{
			PageInfo pageInfo = mDbAdapter.fetchEntry(mSelectedItemId);
			RenameDialog rd = new RenameDialog( mActivity,
												pageInfo,
												mDbAdapter,
												new RenameHandlerInterface.OnRenameItemListener() {	
													@Override
													public void onRenameItem(String title) {
														refreshListViewData();
														AppUtils.showToastShort(mActivity, mActivity.getString(R.string.renamedto) + " " + title);
													}
												}									
			
			);
			rd.show();

		}		
		else if( title == mActivity.getString(R.string.web_share))
		{
			final Intent shareIntent = new Intent( android.content.Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, wUrl);
			shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, wTitle);
			
			mActivity.startActivity(Intent.createChooser(shareIntent, wTitle));
		}	
		mSelectedItemId = 0;
		return true;
	}
	
	public void export()
	{
		Cursor c = mDataCursor;
		c.moveToFirst();
		
		String output = mActivity.getString(R.string.exported_msg) + "\n";
        while(!c.isAfterLast())
        {
        	String title = c.getString(WebDbAdapter.COL_TITLE);
        	String url   = c.getString(WebDbAdapter.COL_URL);
        
        	output += title + "\n" + url + "\n\n";
        	c.moveToNext();
        	
        }
        
		final Intent shareIntent = new Intent( android.content.Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, output);
		
		mActivity.startActivity(Intent.createChooser(shareIntent, "Export"));        
	}
	
	private ListView buildListView()
	{
		if( mListView == null )
		{
	        mListView = new ListView( mActivity );
	        mListView.setId(SettingsManager.ORDER_BY_TIME);
	        mListView.setLongClickable( true );
	        mListView.setOnItemClickListener( new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
						long arg3) {

					try
					{
						showContent( (int) arg3 );
					}
		        	catch(Exception e)
		        	{
		        		Log.e(LOG_TAG, "Exception " + e.getMessage());
		        		AppUtils.showToastLong(mActivity, "Unable to load item. Please reload Retain and try again");
		        	}					
				}
	        }
	        );
	        
	        mListView.setOnCreateContextMenuListener(new MyOnCreateContextMenuListener());
	        mListView.setOnItemLongClickListener( new MyOnItemLongClickListener());
		}
		
		return mListView;
	}
	

    private void showContent( long rowId )
    {
    	Intent i = new Intent(mActivity, WebViewActivity.class);
    	i.putExtra(WebDbAdapter.KEY_ROWID, rowId);
        
    	mActivity.startActivity(i);
    }	
	
	public void handleDeleteRequest(long rowId)
	{
		// should pass in cursor here. since viewcontroller should have the only WebDbAdapter
		// for this instance
		PageInfo info = mDbAdapter.fetchEntry(rowId);
		DeleteDialog dialog = 
			new DeleteDialog( mActivity,
							  info,
							  mDbAdapter,
							  new DeleteHandlerInterface.OnDeleteItemListener() {	
								@Override
								public void onDeleteItem(long rowId) {
									refreshListViewData();
								}
							});
	    dialog.prompt();
	}    

	private void removeViews()
	{
		LinearLayout ll = (LinearLayout) mActivity.findViewById(com.retain.R.id.mainLinearLayout);
		ll.removeAllViews();
	}
	
	class MyOnItemLongClickListener implements OnItemLongClickListener
	{
		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {	

			int id = (int) arg1.getId();
			if( mCurrentViewType == SettingsManager.ORDER_BY_SITE )
			{ 
				if( id < 0)
					return true;
				
				mSelectedItemId = id;
			}
			else
				mSelectedItemId = arg3;
			
			return false; // return false to let the context menu do its work
		}	
	}
	
	class MyOnCreateContextMenuListener implements OnCreateContextMenuListener
	{
		
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo) {

			PageInfo info = mDbAdapter.fetchEntry(mSelectedItemId);
			if( info == null )
				return;

			menu.setHeaderTitle(info.getTitle());
			menu.add(R.string.web_share);
			menu.add(R.string.web_delete);
			menu.add(R.string.web_rename);
		}
	}
	
}


class MyExpandableListAdapter extends BaseExpandableListAdapter {
	private Activity mActivity;
	private LayoutInflater mInflater;
	
	private ArrayList<String> groups;
	private HashMap<String, LinkedList<WebItem> > data;
	private Cursor c;
	
	static private final String LOG_TAG = "MyExpandableListAdapter";

	// dip size for left-pad in group rows
	private static final float ROW_LEFT_PADDING = 34;	
	private int mLeftGroupPad;
	
	private class WebItem
	{
		public int       id;
		public String title;
		public long time;
		
		public WebItem( String title, int id, long time)
		{
			this.id = id; this.title = title; this.time = time;
		}
	}
	
	public MyExpandableListAdapter(Activity activity, ExpandableListView elv, Cursor c){
		
		this.mActivity = activity;
		this.c = c;

    	// Convert the dips to pixels
    	final float scale = mActivity.getResources().getDisplayMetrics().density;
    	mLeftGroupPad = (int) (ROW_LEFT_PADDING * scale + 0.5f);		
				
        this.mInflater = LayoutInflater.from(mActivity);
	}
	

	public int getRowIdForGroupAndChild( int groupPosition, int childPosition )
	{
		return data.get(groups.get(groupPosition)).get(childPosition).id;
	}
	
	public void refreshData()
	{
        data = new HashMap<String, LinkedList<WebItem>>();
        groups = new ArrayList<String>();
        
        if( c.moveToFirst() && c.getCount() > 0 )
        {
        	
	        while(!c.isAfterLast())
	        {
	        	String title = c.getString(WebDbAdapter.COL_TITLE);
	        	String url   = c.getString(WebDbAdapter.COL_URL);
	        	int id = c.getInt(WebDbAdapter.COL_ROWID);
	        	
	        	String host = AppUtils.getHostFromUrl(url);	        	
	        	WebItem ri = new WebItem(title, id, c.getLong(WebDbAdapter.COL_TIMESTAMP));
	        	
	        	if( data.containsKey( host ))
	        		data.get(host).add(ri);
	        	else
	        	{
	        		LinkedList<WebItem> ll = new LinkedList<WebItem>();
	        		ll.add(ri);
	        		data.put( host, ll);
	        		groups.add( host );
	        	}
	        	c.moveToNext();
	        }
        }
        
        Collections.sort( groups );
	}
	
	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return data.get(groups.get(groupPosition)).get(childPosition).title;
	}

	@Override
    public long getChildId(int groupPosition, int childPosition) {
		int id = data.get(groups.get(groupPosition)).get(childPosition).id;
		return id;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {

		if( groupPosition > groups.size() - 1 || childPosition > data.get(groups.get(groupPosition)).size() - 1)
		{
			Log.d(LOG_TAG, "out of range");
			return null;
		}
        LinearLayout row= (LinearLayout) mInflater.inflate(R.layout.entry_row, null);
        WebItem wi = data.get(groups.get(groupPosition)).get(childPosition);
        
        TextView tv = (TextView) row.findViewById(R.id.rowTitle);
        tv.setText( wi.title );
        TextView tv2 = (TextView) row.findViewById(R.id.rowSiteTime);
        tv2.setText( AppUtils.getDateStr( wi.time, new Date()));
        row.setId(wi.id);
        
        return row;	
	}

	@Override
	public int getChildrenCount(int groupPosition) {
        try {
        	return data.get(groups.get(groupPosition)).size();
        	
        } catch (Exception e) {
        	Log.d(LOG_TAG, "Exception: " + e.getMessage());
        }

        return 0;
	}

	@Override
	public Object getGroup(int groupPosition) {
		if( groupPosition > groups.size() - 1)
			return null;
		
		return groups.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return groups.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		
		TextView textView = getGenericView();
        textView.setText(getGroup(groupPosition).toString());
        return textView;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
	
	public TextView getGenericView() {
        // Layout parameters for the ExpandableListView
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT, 60);

        TextView textView = new TextView(mActivity);
        textView.setLayoutParams(lp);
        // Center the text vertically
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        
        textView.setTextAppearance(mActivity, R.style.groupRow);
        // Set the text starting position
        textView.setPadding(mLeftGroupPad, 0, 0, 0);
        return textView;
    }	
	
}
