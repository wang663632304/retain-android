package com.retain2.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;

import com.retain2.R;
import com.retain2.AppUtils;
import com.retain2.DeleteHandlerInterface;
import com.retain2.PageInfo;
import com.retain2.WebDbAdapter;

public class DeleteDialog  {

	private Activity mActivity;
	private AlertDialog mAlertDialog;
	private PageInfo mPageInfo;
	private WebDbAdapter mDbAdapter;
	private DeleteHandlerInterface.OnDeleteItemListener mDeleteListener;
	
	public DeleteDialog( Activity activity,
					     PageInfo pageInfo,  
					     WebDbAdapter dbAdapter,
					     DeleteHandlerInterface.OnDeleteItemListener deleteListener)
	{
		mActivity = activity;
		mPageInfo = pageInfo;
		mDeleteListener = deleteListener;
		mDbAdapter = dbAdapter;
		
		mAlertDialog = new AlertDialog.Builder(mActivity).create();
	    mAlertDialog.setCancelable(true);
	    mAlertDialog.setTitle(mActivity.getString(R.string.confirm));
	    mAlertDialog.setMessage(mActivity.getString(R.string.delete) + " \"" + pageInfo.getTitle() + "\"?");
		
		mAlertDialog.setButton(mActivity.getString(R.string.ok), new DialogInterface.OnClickListener() {
		      @Override
		      public void onClick(DialogInterface dialog, int which) {
				mDbAdapter.deleteEntry(mPageInfo.getRowId());
				
				AppUtils.showToastShort( mActivity, mActivity.getString(R.string.deleted) + " " + mPageInfo.getTitle());
				
				mDeleteListener.onDeleteItem(mPageInfo.getRowId());
		        return;
		    } });
		mAlertDialog.setButton2(mActivity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
	}
	
	public void prompt()
	{
		mAlertDialog.show();

	}
	
	protected void finalize() throws Throwable
	{
	} 
}
