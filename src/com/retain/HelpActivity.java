package com.retain;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.LinearLayout;

public class HelpActivity  extends Activity {

	private static final String LOG_TAG = "HelpActivity";

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        
		setContentView(R.layout.help); 
		
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.main_title);
		
		LinearLayout ll = (LinearLayout) findViewById(R.id.OrderButtons);
		ll.setVisibility(View.INVISIBLE);
		
		String helpData = AppUtils.fromRawResourceFile(R.raw.retain_help, this);
		WebView wv = (WebView) findViewById(R.id.webviewHelp);
	   
		wv.setBackgroundColor(-11119018);
	    wv.loadData( helpData, "text/html", "utf-8");
	    wv.setVisibility(View.VISIBLE);
        
    }

	@Override protected void onDestroy() 
	{
	  super.onDestroy();
	  
	}
  
	@Override protected void onResume()
	{
		super.onResume();
		

	}
    
}
