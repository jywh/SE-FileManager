package com.wenhui.filebrowser;

import java.io.File;
import java.io.IOException;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.wenhui.filebrowser.generic.IconPicker;
import com.wenhui.filebrowser.thread.AsyncTask;

public class App extends Application {

	public static String PREFS_PRIVATE = "private_prefs";
	
	public static String PREFS_KEY_HIDDEN = "prefs_show_hidden";
	public static String PREFS_KEY_SAVE_PREV_STATE = "prefs_save_prev_state";
	public static String PREFS_KEY_FEEDBACK = "prefs_feed_back";
	public static String PREFS_KEY_REMOVABLE= "prefs_removable_path";
	
	public static String PREFS_KEY_LIST_BY = "prefs_list_by";
	public static String PREFS_KEY_LIST_OPTION = "prefs_list_options";

	public static final int LIST_VIEW = 0;
	public static final int GRID_VIEW = 1;
	public static final int LIST_WITH_DETAIL = 2;
	
	private static App sInstance;

	private DeviceInfo mDeviceInfo;
	private File mCurrentPath;
	private boolean mShowHidden = false;
	private SharedPreferences mPrefs;
	private SharedPreferences mPrivatePrefs;
	private int mCurrentListBy;
	private int mListOption;
	private boolean mSavePrevState;
	private boolean mMultiSelectMode;
	private boolean mRefreshCache = false;
	private File mRemovable;
	
	public static App instance() {
		return sInstance;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		sInstance = this;
		mDeviceInfo = new DeviceInfo( this );
		mCurrentPath = mDeviceInfo.homeDirectory();
		mPrefs = PreferenceManager.getDefaultSharedPreferences( this );
		mPrivatePrefs = getSharedPreferences( PREFS_PRIVATE, Context.MODE_PRIVATE );
		initSharedPreference();
		IconPicker.init();
	}

	public DeviceInfo deviceInfo() {
		return mDeviceInfo;
	}

	public void setCurrentPath( File file ) {
		mCurrentPath = file;
	}

	public File getCurrentPath() {
		return mCurrentPath;
	}

	public void setMultiSelectMode( boolean b ) {
		this.mMultiSelectMode = b;
	}

	public boolean isMultiSelectMode() {
		return this.mMultiSelectMode;
	}

	public void setRefreshCache( boolean b) {
		this.mRefreshCache = b;
	}
	
	public boolean refreshCache(){
		return this.mRefreshCache;
	}
	
	public boolean isListMode() {
		return mListOption != GRID_VIEW;
	}
	
	// /////////////////////////////////////////////////
	// Preference
	// /////////////////////////////////////////////////

	public SharedPreferences preference() {
		return mPrefs;
	}

	public void initSharedPreference() {
		mShowHidden = mPrefs.getBoolean( PREFS_KEY_HIDDEN, false );
		mSavePrevState = mPrefs.getBoolean( PREFS_KEY_SAVE_PREV_STATE, true );

		mCurrentListBy = mPrivatePrefs.getInt( PREFS_KEY_LIST_BY, 0 );
		mListOption = mPrivatePrefs.getInt( PREFS_KEY_LIST_OPTION, 0 );
		String path = mPrivatePrefs.getString( PREFS_KEY_REMOVABLE, null );
		if( path != null ){ mRemovable = new File( path ); }
	}

	/**
	 * 0: name 1: time 2: size 3: type
	 * 
	 * @return
	 */
	public void setCurrentListBy( int listBy ) {
		mCurrentListBy = listBy;
		editPrivateSharedPreferenceAsync( PREFS_KEY_LIST_BY, listBy );
	}

	/**
	 * 0: name 1: time 2: size 3: type
	 * 
	 * @return
	 */
	public int getCurrentListBy() {
		return mCurrentListBy;
	}

	public void setListOption( int listOption ) {
		mListOption = listOption;
		editPrivateSharedPreferenceAsync( PREFS_KEY_LIST_OPTION, mListOption );
	}

	public int getListOption() {
		return mListOption;
	}

	public void setSavePrevState( boolean b ) {
		mSavePrevState = b;
	}

	public boolean getSavePrveSaveState() {
		return mSavePrevState;
	}

	public void setShowHiddenFiles( boolean b ) {
		this.mShowHidden = b;
	}

	public boolean showHiddenFiles() {
		return mShowHidden;
	}

	public void setRemovableDir( String path ){
		File f = new File(path);
		this.mRemovable = f;
		editPrivateSharedPreferenceAsync( PREFS_KEY_REMOVABLE, path );
	}
	
	public File getRemovableDir() {
		if( mRemovable != null ){
			return mRemovable;
		}
		
		if( !DeviceInfo.hasHoneycomb() ){ return mRemovable; }
		
		mRemovable = new File( mDeviceInfo.rootDirectory(), "Removable" );
		if( !mRemovable.exists() ){
			mRemovable = null;
		}
		return this.mRemovable;
	}
	
	public void editSharePreference( String key, int value ) {
		Editor editor = mPrefs.edit();
		editor.putInt( key, value );
		editor.commit();
	}

	public void editPrivateSharedPreferenceAsync( String key, int value ) {
		WritePrivatePrefsTask task = new WritePrivatePrefsTask( key, value );
		WritePrefsExecutor executor = new WritePrefsExecutor() {
			@Override
			public void execute( Editor editor, String key, Object object ) {
				editor.putInt( key, ( Integer ) object );
			}
		};
		task.executeOnExecutor( AsyncTask.DUAL_THREAD_EXECUTOR, executor );
	}

	public void editPrivateSharedPreferenceAsync( String key, boolean value ) {
		WritePrivatePrefsTask task = new WritePrivatePrefsTask( key, value );
		WritePrefsExecutor executor = new WritePrefsExecutor() {
			@Override
			public void execute( Editor editor, String key, Object object ) {
				editor.putBoolean( key, ( Boolean ) object );
			}
		};
		task.executeOnExecutor( AsyncTask.DUAL_THREAD_EXECUTOR, executor );
	}

	public void editPrivateSharedPreferenceAsync( String key, String value ){
		WritePrivatePrefsTask task = new WritePrivatePrefsTask( key, value );
		WritePrefsExecutor executor = new WritePrefsExecutor() {
			@Override
			public void execute( Editor editor, String key, Object object ) {
				editor.putString( key, ( String ) object );
			}
		};
		task.executeOnExecutor( AsyncTask.DUAL_THREAD_EXECUTOR, executor );
	}
	
	private class WritePrivatePrefsTask extends AsyncTask< WritePrefsExecutor, Void, Void > {

		private Object obj;
		private String key;

		public WritePrivatePrefsTask( String key, Object obj ) {
			this.key = key;
			this.obj = obj;
		}

		@Override
		protected Void doInBackground( WritePrefsExecutor... params ) {
			if ( params.length == 0 ) {
				return null;
			}
			Editor editor = mPrivatePrefs.edit();
			params[0].execute( editor, key, obj );
			editor.commit();
			return null;
		}
	}

	private interface WritePrefsExecutor {
		public void execute( Editor editor, String key, Object object );
	}

}
