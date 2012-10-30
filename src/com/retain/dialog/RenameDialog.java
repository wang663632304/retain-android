package com.retain.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;

import com.retain.PageInfo;
import com.retain.R;
import com.retain.WebDbAdapter;
import com.retain.RenameHandlerInterface.OnRenameItemListener;

public class RenameDialog extends AlertDialog {

	private EditText mET;
	private WebDbAdapter mDbAdapter;
	private OnRenameItemListener mRenameListener;
	private PageInfo mPageInfo;
	public RenameDialog(Activity activity, PageInfo pageInfo, WebDbAdapter dbAdapter, OnRenameItemListener renameListener) {
		super(activity);
		
		mET = new EditText( activity );
		mET.setText( pageInfo.getTitle() );
		mDbAdapter = dbAdapter;
		mPageInfo = pageInfo;
		mRenameListener = renameListener;
		
		setView( mET );
		setCancelable(true);
		setTitle(activity.getString(R.string.web_rename));			

		setButton( activity.getString(R.string.ok), new DialogInterface.OnClickListener() {
		      @Override
		      public void onClick(DialogInterface dialog, int which) {
		    	String title = mET.getText().toString();

		    	mPageInfo.setTitle( title );
				mRenameListener.onRenameItem(title);
				
		        return;
		    } });
		setButton2(activity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});				
		
	}

	
	
	
	
}
