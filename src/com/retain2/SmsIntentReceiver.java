package com.retain2;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;

import com.retain2.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsIntentReceiver extends BroadcastReceiver {
	private static final String LOG_TAG = "SmsIntentReceiver";
	private final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent.getAction().equals(ACTION) ) {
			

			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
			boolean useSms = sp.getBoolean( context.getString(R.string.pref_key_use_sms), false);
			
			if( useSms )
			{
	            SmsMessage[] messages = getMessagesFromIntent(intent);
	            
	            for(SmsMessage currentMessage : messages) {

	                    String body = currentMessage.getDisplayMessageBody();
	                    
	                    body = body.replaceAll("\n", " ");
	                    String[] parts = body.trim().split(" ");
	                    for(String url : parts )
	                    {
	                    	try {
	                    		
		                    	URL u = new URL(url); // this will throw for non urls
		                    	
		                    	Log.d(LOG_TAG, "Processing url " + url); // non urls should not reach here
		                    	 
			            		final Intent i = new Intent( context, DownloaderActivity.class);
			                	i.putExtra( Intent.EXTRA_TEXT, u.toString());
			                	i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			            		context.startActivity(i);
	                    	}

	                    	catch( IllegalArgumentException e )
	                    	{	
	                    	}
	                    	catch( MalformedURLException e )
	                    	{
	                    	}
	                    	
	                    }
	            }
			}
			else
				Log.d(LOG_TAG, "Not processesing SMS. use_sms=false");
		}		
	}	
		
	private SmsMessage[]
	                   getMessagesFromIntent(Intent intent)
	                {
	                   SmsMessage retMsgs[] = null;
	                   Bundle bdl = intent.getExtras();
	                   try{
	                      Object pdus[] = (Object [])bdl.get("pdus");
	                      retMsgs = new SmsMessage[pdus.length];
	                      for(int n=0; n < pdus.length; n++)
	                      {
	                         byte[] byteData = (byte[])pdus[n];
	                         retMsgs[n] =
	                           SmsMessage.createFromPdu(byteData);
	                      }        
	                   }
	                   catch(Exception e)
	                   {
	                      Log.e("GetMessages", "fail", e);
	                   }
	                   return retMsgs;
	                }	
}
