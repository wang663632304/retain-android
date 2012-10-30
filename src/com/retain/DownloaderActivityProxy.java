package com.retain;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class DownloaderActivityProxy   extends Activity {


	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        

        // handle a 'Share' intent from outside
		Intent intent = getIntent();
		String data = intent.getDataString();
		
		if( data != null )
		{
			Intent i = new Intent(this, DownloaderActivity.class);
			
			i.putExtra( Intent.EXTRA_TEXT, data);
	    	this.startActivity(i);			
		}


		intent.replaceExtras( new Bundle()); 
		setResult(RESULT_CANCELED, null);		
		
		finish();
	}
}
