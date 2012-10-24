package com.wenhui.filebrowser;

import java.io.File;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class DeviceInfo {

	private int mScreenDensity;
	private File mHomeDir;
	
	public DeviceInfo( Context context ){
		init( context );
	}
	
	private void init( Context context ) {
		WindowManager wm = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
		Display display = wm.getDefaultDisplay(); 
		DisplayMetrics displaymetrics = new DisplayMetrics();
		display.getMetrics(displaymetrics);
		mScreenDensity = displaymetrics.densityDpi;
	}
	
	public int getScreenDensity(){
		return mScreenDensity;
	}
	
	public File homeDirectory() {
		if( mHomeDir == null ){
			mHomeDir = Environment.getExternalStorageDirectory();
		}
		return mHomeDir;
	}
	
	public File rootDirectory(){
		return new File("/");
	}
	
	public boolean isExternalStorageAval() {
		return Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED );
	}
	
    public static boolean hasFroyo() {
        // Can use static final constants like FROYO, declared in later versions
        // of the OS since they are inlined at compile time. This is guaranteed behavior.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasIceCreamSandwish() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }
    
    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }
    
    public boolean isAsusTransformer() {
    	Log.d("DeviceInfo", "Build manufacture: " + Build.MANUFACTURER );
    	Log.d("Device", "Build model: " + Build.MODEL ); 
    	return Build.MANUFACTURER.equalsIgnoreCase( "Asus" );
    }
	
}
