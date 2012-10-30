package com.retain;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.ToggleButton;

public class RetainActivity extends Activity {

    private static final String LOG_TAG = "RetainActivity";

	private ViewController mViewController;
	private ToggleButton mOrderByDateButton;
	private ToggleButton mOrderBySiteButton;
	public static final int RETAIN_NOTIFICATION_ID = 1;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.main_title);
        
		mViewController = new ViewController( this );
        
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		int orderBy = sp.getInt(getString(R.string.pref_key_order_by), SettingsManager.ORDER_BY_TIME);		
		
		mOrderByDateButton = (ToggleButton) findViewById(R.id.Button01);
		mOrderByDateButton.setChecked( orderBy == SettingsManager.ORDER_BY_TIME );
		mOrderByDateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mOrderByDateButton.setChecked(true);
				mOrderBySiteButton.setChecked(false);
				setOrderBy(SettingsManager.ORDER_BY_TIME);
				mViewController.setViewType(SettingsManager.ORDER_BY_TIME);
			}
		});
		
		mOrderBySiteButton = (ToggleButton) findViewById(R.id.Button02);
		mOrderBySiteButton.setChecked( orderBy == SettingsManager.ORDER_BY_SITE);
		mOrderBySiteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mOrderBySiteButton.setChecked(true);
				mOrderByDateButton.setChecked(false);
				setOrderBy(SettingsManager.ORDER_BY_SITE);
				mViewController.setViewType(SettingsManager.ORDER_BY_SITE);
			}
		});
    }
    
	private void setOrderBy( int orderBy)
	{
		Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		prefEditor.putInt( getString(R.string.pref_key_order_by), orderBy);
		prefEditor.commit();		
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
				
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		int orderBy = sp.getInt(getString(R.string.pref_key_order_by), SettingsManager.ORDER_BY_TIME);
		mViewController.refreshListViewData();
		mViewController.setViewType( orderBy );
	}
	
	@Override
	public void onDestroy()
	{
		super.onResume();
		
		Log.d(LOG_TAG, "onDestroy called");
		mViewController.onDestroyCalled();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		super.onCreateOptionsMenu(menu);
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}
		
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId())
		{
		case R.id.mainMenuSettings:
	    	this.startActivity(new Intent(this, SettingsManager.class));		
			break;
			
		case R.id.mainMenuExport:
			mViewController.export();
			break;
		case R.id.mainMenuExit:
			finish();
			break;
			
		case R.id.mainMenuHelp:
			this.startActivity(new Intent(this, HelpActivity.class));
			break;
			
		case R.id.mainMenuTest:
			Intent i = new Intent(this, DownloaderActivity.class);
			i.putExtra( Intent.EXTRA_TEXT, "http://mashable.com/2010/09/16/diaspora-source/#4192Diaspora-Developer-Opinions");
			this.startActivity(i);
			
			i = new Intent(this, DownloaderActivity.class);
			i.putExtra(Intent.EXTRA_TEXT, "http://www.nytimes.com/2010/12/05/realestate/05canal.html?hp");
			this.startActivity(i);
			break;			
			
			
		}	
		
		return true;
	}
    	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		String title = (String) item.getTitle();
		return mViewController.onContextItemSelected( title );
		
	}
	
}


