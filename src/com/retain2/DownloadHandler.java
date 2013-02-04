package com.retain2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import com.retain2.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;

public class DownloadHandler implements Handler.Callback{

	private static final String LOG_TAG = "DownloadHandler";
	private static final long[] NO_FEEDBACK = new long[] {0L, 0L, 0L, 0L};

	public static final  String DATA_DIR = Environment.getDataDirectory() + "/data/com.retain2";
    private static final String WEB_DIR =  DATA_DIR + "/webdata";
    
    public static final  String RESOURCES_DIR =  DATA_DIR + "/resources";
    public static final  String RETAIN_COLORS_CSS = RESOURCES_DIR + "/retaincolors.css";    

    // remove undesirable stuff
    private static final String[] undesirables = {
	  	  	"<\\s?script.*</\\s?script\\s?>",
			"user-scalable\\s?=\\s?no", 
			"user-scalable\\s?=\\s?0", 
			"maximum-scale\\s?=\\s?([0-9\\,\\.\\+\\-]+)"
    };
    
	private Context mContext;
    private final Handler mHandler;
	private Random mGenerator;
	NotificationManager mNotificationManager;
	
    static private DownloadHandler mInstance = null;

    public static synchronized DownloadHandler getInstance(Context context) {
        if (mInstance == null) {

    		AppUtils.createDir( WEB_DIR + "/" );   	  // web file storage
    		
            mInstance = new DownloadHandler(context);
        }
        return mInstance;
    }
    
    private DownloadHandler(Context context)
    {
    	mContext = context;
    	mHandler = new Handler(this);
		mGenerator = new Random();
		mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }
    
    private void postToast( String msg )
    {
    	Bundle b = new Bundle();
    	b.putString("msg", msg);
    	Message m = new Message();
    	m.setData(b);
    	mHandler.sendMessage( m );
    }
    
    @Override 
    public boolean handleMessage(Message msg) {
    	
    		Bundle b = msg.getData();
    		String m = b.getString("msg");
    		if( m == null )
    			return false;
    		
      	  	AppUtils.showToastLong(mContext, m);
      	  	return true;
    }

    public void download(final String url, final long deleteRowId)
    {
    	new Thread(new Runnable() {
        public void run() {

		Log.d(LOG_TAG, "Fetching " + url);

		WebDbAdapter dbHelper = new WebDbAdapter(mContext);
        dbHelper.open();  
		
		boolean errorOccurred = true;
		int notifyId = 0;
		String host = AppUtils.getHostFromUrl(url);
		boolean ioError = false;
    	try
    	{    
    		if( host == null )
    		{
    			Log.d(LOG_TAG, "Bad url " + url);
    			errorOccurred = true;
    		}
    		else
    		{
    			notifyId = showNotification( "Fetching " + host, "Fetching " + host, android.R.drawable.stat_sys_download, 0 );
		    	
		    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
				String userAgent = sp.getString(mContext.getString(R.string.pref_key_user_agent), 
												mContext.getString(R.string.default_user_agent));
				
				Log.d(LOG_TAG, "Using user agent=" + userAgent);
		
		    	AndroidHttpClient ahc = AndroidHttpClient.newInstance(mContext, url, userAgent);

		    	URI uri = new URI(url);		
		    	
		    	// remove the fragment from the original url
		    	URI norm = new URI(uri.getScheme().toLowerCase(),
		    			uri.getUserInfo(), uri.getHost().toLowerCase(), uri.getPort(),
		    			uri.getPath(), uri.getQuery(), null);
		    	norm = norm.normalize();
		    	
		    	HttpUriRequest get = new HttpGet(norm);	
		    	HttpResponse response = ahc.execute(get);
	
		    	if (response.getStatusLine().getStatusCode() == 200) {
		    	
		    	  HttpEntity entity = response.getEntity();
		    	  ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    	  entity.writeTo(baos);
		    	  String data = baos.toString();

		    	  // remove bad stuff
		    	  for(int i = 0; i < undesirables.length; i++)
		    	  {
			    	  Pattern p = Pattern.compile(undesirables[i], Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
			  	  	  data = data.replaceAll(p.pattern(), "");
		    	  }
	
		    	  long sysMillis = System.currentTimeMillis();
		    	  String newFileName = getPath( sysMillis, sp.getBoolean(mContext.getString(R.string.pref_key_store_sdcard), false) );
		    	  FileOutputStream strm = new FileOutputStream(newFileName);
		    	  
		    	  Log.d(LOG_TAG, "Writing to " + newFileName + " for url " + url);
		    	  String jsData = AppUtils.fromRawResourceFile(R.raw.retain_loadcolors, mContext);
		    	  jsData = jsData.replaceAll( "@css_file", RETAIN_COLORS_CSS );

		    	  strm.write(jsData.getBytes());
		          strm.write(data.getBytes());
		    	  strm.write(jsData.getBytes());
		          strm.flush();
		          strm.close();
		          
		          String entryName = getTitle( newFileName, url );
		          
		          // add new file to the db
	              long newRowId = dbHelper.createEntry(entryName, newFileName, url, sysMillis);
	              postToast("Downloaded \"" + entryName + "\"");
	              
	              errorOccurred = false;
		              
	      		  if( deleteRowId != 0)
	    		  {
					Log.d(LOG_TAG, "Deleting rowId=" + deleteRowId);
					dbHelper.deleteEntry(deleteRowId);

					// if only a rowId was specified and no url, show a Delete msg
					if( url == null)
						postToast("Item Deleted");
						
			    	mContext.startActivity(new Intent(mContext, RetainActivity.class));    			
	    		 	
	    		  }
	    		  else
	    		  {
	    			showNotification( "Download Complete", entryName, android.R.drawable.stat_sys_download_done, newRowId );
	    		  }
              
		        } 
		    	else {
		    		Log.e(LOG_TAG, "Response code=" + String.valueOf(response.getStatusLine().getStatusCode()));
		        }
    		}
    	}
    	catch(IOException ioe)
    	{
    		ioError = true;
    		Log.e(LOG_TAG, "RETAIN IOException: " + ioe.getMessage());
    	}
    	catch( URISyntaxException u)
    	{
    		Log.e(LOG_TAG, "RETAIN URISyntaxException: " + u.getMessage());
    	}
    	catch(OutOfMemoryError oome )
    	{
    		Log.e(LOG_TAG, "RETAIN OutOfMemoryError: " + oome.getMessage());
    	}
    	catch(Exception e )
    	{
    		Log.e(LOG_TAG, "RETAIN Exception: " + e.getMessage());
    	}
    	
		hideNotification(notifyId);
    	if( errorOccurred && host != null)
    	{
    		String msg = "Error fetching ";
    		if( ioError )
    			msg = "Filesystem error while fetching ";
    		
    		showNotification( msg, host, android.R.drawable.stat_notify_error, 0 );
    		postToast(msg + host);
        }
    		
    	dbHelper.close();

        }}).start();
    }

    private String getPath(long sysMillis, boolean useExternal)
    {
    	String path = null;
    	
    	if( useExternal )
    		path = getExternalPath(sysMillis);
    	
    	if( path == null )
        	path = WEB_DIR;
    	
    	return (path + "/" + String.valueOf(sysMillis) + ".html");
    }
    
    private String getExternalPath(long sysMillis)
    {
    	String state = Environment.getExternalStorageState();
    	if( Environment.MEDIA_MOUNTED.equals(state) )
    	{
    		File root = new File(Environment.getExternalStorageDirectory().getPath());
    		if( root == null || !root.canWrite())
    		{
    			postToast("Cannot write to external storage. Storing internally");
    			Log.e(LOG_TAG, "Cannot get external root directory");
    			return null;
    		}
    		
    		String strMillis = String.valueOf(sysMillis);
        	String path = root.getAbsolutePath() + "/" + mContext.getString(R.string.app_name) + "/";
    		path +=  strMillis;

        	File fpath = new File(path);
    		if( !fpath.mkdirs() )
    		{
    			postToast("Cannot write to external storage. Storing internally");
    			return null;
    		}
    		
    		return path;
    	}
    	
    	return null;
    }	    
    
    private String getTitle( String filename, String url ) throws IOException
    {
        Pattern p = Pattern.compile("<\\s?title\\s?>(.*)</\\s?title\\s?>", Pattern.DOTALL);
        Matcher matcher = p.matcher( AppUtils.fromFile(filename));

        if( matcher.find() )
        {
            String match = matcher.group(1); 
            String title = Html.fromHtml(match).toString();
            if( title != null)
            {
            	title = title.trim();
                if( title.length() > 75 )
                	title = title.substring(0,75) + "...";                 	

            	return title;
            }
        }
        return url;
    }
    
	private int showNotification( CharSequence contentTitle, 
								   CharSequence contentText, 
								   int icon, long rowId )
	{
		Intent i = null;
		if( rowId > 0 )
		{
			i = new Intent( mContext, WebViewActivity.class);
			i.putExtra(WebDbAdapter.KEY_ROWID, rowId);
		}
		else
			i = new Intent( mContext, RetainActivity.class);

		// makes this intent unique
		int uniqueId = mGenerator.nextInt();		
		Uri uri = Uri.parse("retain://" + String.valueOf(uniqueId));
		i.setData(uri);
		
		PendingIntent contentIntent = PendingIntent.getActivity( mContext, 0, i, 0);

		Notification notification = new Notification(icon, contentText, System.currentTimeMillis());
		notification.setLatestEventInfo(mContext, contentTitle, contentText, contentIntent);
		notification.defaults &= ~Notification.DEFAULT_VIBRATE;
		notification.vibrate = NO_FEEDBACK;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		mNotificationManager.notify( uniqueId, notification);
		
		return uniqueId;
	}
	
	private void hideNotification(int notifyId)
	{
		NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(notifyId);		
	}

    
}