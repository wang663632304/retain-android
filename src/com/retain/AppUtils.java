package com.retain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * AppUtils is a helper class that makes it easy to perform frequently used tasks in Android development.
 *
 * @author Nazmul Idris
 * @version 1.0
 * @since Jul 8, 2008, 2:35:39 PM
 */
public class AppUtils {

/** 127.0.0.1 in the emulator points back to itself. Use this if you want to access your host OS */
public static String EmulatorLocalhost = "10.0.2.2";

/** shows a short message on top of your app... it goes away automatically after a short delay */
public static void showToastShort(Context a, String msg) {
  Toast.makeText(a,
                 msg,
                 Toast.LENGTH_SHORT).show();
}

/** shows a short message on top of your app... it goes away automatically after a long delay */
public static void showToastLong(Context a, String msg) {
	  Toast.makeText(a,
	                 msg,
	                 Toast.LENGTH_LONG).show();
	}

public static void showToast(Context a, String msg, int length) {
	  Toast.makeText(a,
	                 msg,
	                 length).show();
	}

/**
 * create an image view, given a drawable. you can set the max size of this imageview as well.
 *
 * @param iconWidth  -1 means dont set this
 * @param iconHeight -1 means dont set this
 * @param imageRes   -1 means dont set this
 */
public static ImageView createImageView(Context activity, int iconWidth, int iconHeight, int imageRes) {
  ImageView icon = new ImageView(activity);
  icon.setAdjustViewBounds(true);
  icon.setScaleType(ImageView.ScaleType.FIT_CENTER);

  if (iconHeight != -1) icon.setMaxHeight(iconHeight);
  if (iconWidth != -1) icon.setMaxWidth(iconWidth);

  if (imageRes != -1) icon.setImageResource(imageRes);
  return icon;
}

/** simply resizes a given drawable resource to the given width and height */
public static Drawable resizeImage(Context ctx,
                                   int resId,
                                   int iconWidth,
                                   int iconHeight)
{

  // load the origial Bitmap
  Bitmap BitmapOrg = BitmapFactory.decodeResource(ctx.getResources(),
                                                  resId);

  int width = BitmapOrg.getWidth();
  int height = BitmapOrg.getHeight();
  int newWidth = iconWidth;
  int newHeight = iconHeight;

  // calculate the scale
  float scaleWidth = ((float) newWidth) / width;
  float scaleHeight = ((float) newHeight) / height;

  // create a matrix for the manipulation
  Matrix matrix = new Matrix();
  // resize the Bitmap
  matrix.postScale(scaleWidth, scaleHeight);

  // if you want to rotate the Bitmap
  // matrix.postRotate(45);

  // recreate the new Bitmap
  Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0,
                                             width, height, matrix, true);

  // make a Drawable from Bitmap to allow to set the Bitmap
  // to the ImageView, ImageButton or what ever
  return new BitmapDrawable(resizedBitmap);

}


public static void createDir( String dirPath )
{
	File dir = new File(dirPath);
	if( !dir.exists())
	{
		Log.d("createDir", dir.getPath() + " does not exist. Creating");
		if( !dir.mkdir() )
		{
			Log.e("createDir", "Unable to create " + dir.getPath());
		}  
	}     	
	
}

public static String fromRawResourceFile(int id, Context ctx)
{
	String data = "";
	try
	{
	   InputStream is = ctx.getResources().openRawResource(id);
	   InputStreamReader isr = new InputStreamReader(is);
	   
	   BufferedReader br = new BufferedReader(isr);
	   String thisLine;
	   while( (thisLine = br.readLine()) != null)
	   {
		   data += thisLine;
	   }
	   is.close();
	   
   }
   catch(Exception e)
   {
	   Log.e("fromRawResourceFile", "Error: " + e.getMessage() + "," + e.toString());	   
   }			

   return data;
}

public static CharSequence fromFile(String filename) throws IOException {
    FileInputStream fis = new FileInputStream(filename);
    FileChannel fc = fis.getChannel();

    // Create a read-only CharBuffer on the file
    ByteBuffer bbuf = fc.map(FileChannel.MapMode.READ_ONLY, 0, (int)fc.size());
    CharBuffer cbuf = Charset.forName("8859_1").newDecoder().decode(bbuf);
    return cbuf;
}

private static final String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
static public String getDateStr(long milliseconds, Date currentDate)
{
	// seconds diff
	long diff = (long) (currentDate.getTime() - milliseconds) / 1000;
	if( diff < 3600 )
	{
		long minutesAgo = (long) diff/60;
		if( minutesAgo < 1 )
			return "seconds ago";
		else if( minutesAgo == 1 )
			return "1 minute ago";
		else
			return (String.valueOf(minutesAgo) + " minutes ago");		
	}
	else if( diff < 7200 )
		return "1 hour ago";
	else if( diff < 86400 )
		return (String.valueOf((int) diff/3600) + " hours ago");
	else
	{
		Date d = new Date(milliseconds);
		return months[d.getMonth()] + " " + d.getDate();
	}
}

static public String getHostFromUrl( String url )
{
	try {
    	URI uri = new URI( url );
		String host = uri.getHost();
		if( host == null )
			return null;
		
		host = host.replaceAll("^www\\.", "");
		
		int i = host.lastIndexOf('.');
    	if( i >= 0 )
    	{
			int j = host.lastIndexOf('.', i-1);
			if( j >= 0)
				host = host.substring(j+1, host.length());
    	}
    	return host;
	}
	catch(URISyntaxException urie)
	{
		Log.e("getHostFromUrl", urie.getMessage());
	}
	
	return url;
	
}

}//end class AppUtils
