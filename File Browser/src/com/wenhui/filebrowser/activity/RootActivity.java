package com.wenhui.filebrowser.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wenhui.filebrowser.App;
import com.wenhui.filebrowser.DeviceInfo;
import com.wenhui.filebrowser.R;
import com.wenhui.filebrowser.adapters.SimpleFileAdapter;
import com.wenhui.filebrowser.generic.FileUtils;
import com.wenhui.filebrowser.generic.IconPicker;
import com.wenhui.filebrowser.model.ListItemsContainer;

/**
 * Subclass this class must add 
 * <b> 
 * <intent-filter> &lt;action android:name="android.intent.action.MEDIA_MOUNTED" /> &lt;data
 * android:scheme="file" /> &lt;/intent-filter>
 *  </b> 
 *  to its <activity> tag.
 */
public abstract class RootActivity extends ActionBarActivity {

	private static final String TAG = "RootActivity";

	public static final int MESSAGE_REFRESH_LIST = 101;

	private final static int DIALOG_PROCESS = 5;

	protected File mCurrentDir;
	protected ListView mListView;
	protected GridView mGridView;

	protected SimpleFileAdapter mAdapter;
	protected ArrayList< ListItemsContainer > mListItems;
	protected ArrayList< String > mDirStack;
	protected ArrayAdapter< String > mDirAdapter;

	private HashMap< String, Selection > mSelectionCache;

	static class Selection {
		int select;
		int top;

		public Selection( int select, int top ) {
			this.select = select;
			this.top = top;
		}
	}

	protected abstract void onListItemClick( AdapterView< ? > arg0, View v, int position, long id );

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		mListItems = new ArrayList< ListItemsContainer >();
		mSelectionCache = new HashMap< String, Selection >();
		
		mDirStack = new ArrayList< String >();
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		App.instance().setRefreshCache( false );
	}

	@Override
	protected void onUserLeaveHint() {
		super.onUserLeaveHint();
		if ( App.instance().refreshCache() ) {
			// Refresh media cache, so gallery can pick up file changes.
			sendBroadcast( new Intent( Intent.ACTION_MEDIA_MOUNTED, Uri.parse( "file://" + Environment.getExternalStorageDirectory() ) ) );
		}
	}

	@Override
	protected Dialog onCreateDialog( int id ) {
		switch ( id ) {
		case DIALOG_PROCESS:
			ProgressDialog dialog = new ProgressDialog( this ) {
				@Override
				public boolean onSearchRequested() {
					return false;
				}
			};
			dialog.setMessage( getString( R.string.processing ) );
			dialog.setCancelable( false );
			return dialog;
		}

		return null;
	}

	@Override
	public void onConfigurationChanged( Configuration newConfig ) {
		super.onConfigurationChanged( newConfig );
		if ( !App.instance().isListMode() ) {
			setGridColumnNumber( mGridView );
		}
	}

	// ////////////////////////////////////
	// UI
	// ////////////////////////////////////

	protected abstract int getResId();
	
	protected void initView() {

		mCurrentDir = App.instance().getCurrentPath();

		setContentView( getResId() );

		mListView = ( ListView ) findViewById( R.id.myList );
		mGridView = ( GridView ) findViewById( R.id.myGrid );

		mListView.setOnCreateContextMenuListener( this );
		mGridView.setOnCreateContextMenuListener( this );
		setGridColumnNumber( mGridView );

		mGridView.setOnItemClickListener( mListItemListener );
		mListView.setOnItemClickListener( mListItemListener );

		if ( App.instance().isListMode() ) {
			mGridView.setVisibility( View.GONE );
		} else {
			mListView.setVisibility( View.GONE );
		}


	}

	private OnItemClickListener mListItemListener = new OnItemClickListener() {

		@Override
		public void onItemClick( AdapterView< ? > arg0, View v, int position, long id ) {
			onListItemClick( arg0, v, position, id );
		}
	};

	protected void refreshList( boolean layoutChange ) {

		if ( layoutChange ) {
			mSelectionCache.clear();
		}

		savePrevPosition();
		App.instance().setCurrentPath( mCurrentDir );
		if ( mCurrentDir.isDirectory() ) {
			String path = mCurrentDir.getPath();
			if (!App.instance().isMultiSelectMode() ) {
				new PopulateDirectoryNavigationListTask().execute( path );
			}
			new PopulateAdapterDataTask( layoutChange ).execute();
		}
	}

	private void savePrevPosition() {
		if ( !App.instance().getSavePrveSaveState() ) {
			return;
		}
		File prevDir = App.instance().getCurrentPath();
		int index, top;
		if ( App.instance().isListMode() ) {
			index = mListView.getFirstVisiblePosition();
			View view = mListView.getChildAt( 0 );
			// this is the y position relative to the first child
			top = ( view == null ) ? 0 : view.getTop();
		} else {
			index = mGridView.getFirstVisiblePosition();
			Log.d( TAG, "grid view first view position: " + index );
			top = 0;
		}
		mSelectionCache.put( prevDir.getPath(), new Selection( index, top ) );
	}

	private void restorePrevSelection( File curPath ) {
		int index = 0;
		int top = 0;
		if ( App.instance().getSavePrveSaveState() ) {
			Selection selection = mSelectionCache.get( curPath.getPath() );
			if ( selection != null ) {
				index = selection.select;
				top = selection.top;
			}
		}
		if ( App.instance().isListMode() ) {
			mListView.setSelectionFromTop( index, top );
		} else {
			mGridView.setSelection( index );
		}
	}

	private void updateContentView( boolean layoutChange ) {
		View emptyView = findViewById( R.id.empty_folder );
		int option = App.instance().getListOption();
		if ( mListItems.isEmpty() ) {
			emptyView.setVisibility( View.VISIBLE );
			mListView.setVisibility( View.GONE );
			mGridView.setVisibility( View.GONE );
			return;
		} else if ( emptyView.isShown() ) {
			emptyView.setVisibility( View.GONE );
			if ( App.instance().isListMode() ) {
				if ( !mListView.isShown() ) {
					mListView.setVisibility( View.VISIBLE );
				}
			} else {
				if ( !mGridView.isShown() ) {
					mGridView.setVisibility( View.VISIBLE );
				}
			}
		}

		if ( !layoutChange ) {
			mAdapter.setData( mListItems );
			restorePrevSelection( mCurrentDir );
			return;
		}
		// Empty selection cache when selection change

		Log.d( TAG, "Update current laytout" );

		switch ( option ) {
		case App.LIST_VIEW:
		case App.LIST_WITH_DETAIL:
		default:
			if ( mGridView.isShown() ) {
				mGridView.setVisibility( View.GONE );
				mGridView.setAdapter( null );
			}

			if ( !mListView.isShown() ) {
				mListView.setVisibility( View.VISIBLE );
			}
			mListView.setAdapter( null );
			mAdapter = new SimpleFileAdapter( this, mListItems );
			mListView.setAdapter( mAdapter );
			break;
		case App.GRID_VIEW:
			Log.d( TAG, "Update grid view" );
			if ( mListView.isShown() ) {
				mListView.setVisibility( View.GONE );
				mListView.setAdapter( null );
			}

			if ( !mGridView.isShown() ) {
				mGridView.setVisibility( View.VISIBLE );
			}
			mGridView.setAdapter( null );
			mAdapter = new SimpleFileAdapter( this, mListItems );
			mGridView.setAdapter( mAdapter );
			break;
		}
	}

	private class PopulateDirectoryNavigationListTask extends AsyncTask< String, Void, ArrayList< String >> {

		@Override
		protected ArrayList< String > doInBackground( String... params ) {
			mDirStack.clear();
			String curPath = params[0];
			if ( curPath.length() == 0 ) {
				return null;
			}
			// replace the first file seperator
			curPath = curPath.substring( 1 );
			String[] pathes = curPath.split( File.separator );
			for ( String path : pathes ) {
				mDirStack.add( File.separator + path );
			}
			return null;
		}

		@Override
		protected void onPostExecute( ArrayList< String > result ) {
			super.onPostExecute( result );
			if ( !App.instance().isMultiSelectMode() ) {
				mDirAdapter.notifyDataSetChanged();
			}
			getSupportActionBar().setSelectedNavigationItem( mDirAdapter.getCount() - 1 );
		}

	}

	private class PopulateAdapterDataTask extends AsyncTask< Void, Void, ArrayList< ListItemsContainer > > {

		private boolean layoutChange;

		public PopulateAdapterDataTask( boolean layoutChange ) {
			this.layoutChange = layoutChange;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showProgress();
		}

		@Override
		protected ArrayList< ListItemsContainer > doInBackground( Void... params ) {
			File[] allFiles = FileUtils.getFiles( App.instance().getCurrentListBy(), mCurrentDir );
			ArrayList< ListItemsContainer > list = new ArrayList< ListItemsContainer >();
			for ( File file : allFiles ) {
				String ext = FileUtils.getFileExtension( file );
				int resId = IconPicker.getIcon( ext );
				list.add( new ListItemsContainer( file, ext, resId ) );
			}
			return list;
		}

		@Override
		protected void onPostExecute( ArrayList< ListItemsContainer > result ) {
			super.onPostExecute( result );
			mListItems.clear();
			mListItems = result;
			updateContentView( layoutChange );
			dismissProgress( );
		}

	}

	protected void showProgress() {
		setProgressBarIndeterminateVisibility(true);
	}

	protected void dismissProgress( ) {
		setProgressBarIndeterminateVisibility(false);
	}

	protected void showAddFolderDialog() {
		AlertDialog.Builder b = new AlertDialog.Builder( this );
		LayoutInflater inflator = LayoutInflater.from( this );
		View view = inflator.inflate( R.layout.entry, null );
		TextView tv = ( TextView ) view.findViewById( R.id.textView_entry );

		b.setTitle( R.string.create_folder ).setView( view );
		final EditText edit = ( EditText ) view.findViewById( R.id.editText_add_folder );
		edit.addTextChangedListener( new CheckFileExistListener( tv ) );

		b.setPositiveButton( R.string.create, new DialogInterface.OnClickListener() {
			@Override
			public void onClick( DialogInterface dialog, int which ) {
				String folderName = edit.getText().toString();
				File dir = new File( App.instance().getCurrentPath(), folderName );
				if ( !dir.exists() ) {
					dir.mkdirs();
					refreshList( false );
				} else {
					Toast.makeText( App.instance(), R.string.folder_exist, Toast.LENGTH_LONG ).show();
				}
				edit.setText( "" );
				hideSoftKeyboard( edit );
			}
		} ).setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick( DialogInterface dialog, int which ) {
				edit.setText( "" );
				dialog.cancel();
				hideSoftKeyboard( edit );
			}
		} );

		Dialog dialog = b.create();
		showSoftKeyboardForDialog( dialog );
		dialog.show();
	}

	protected void setGridColumnNumber( GridView gridView ) {
		int num;
		if ( getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ) {
			num = getResources().getInteger( R.integer.grid_view_column_num_portrait );
		} else {
			num = getResources().getInteger( R.integer.grid_view_column_num_landscape );
		}
		gridView.setNumColumns( num );
	}

	protected boolean isHomeDirectory( File file ) {
		return file.compareTo( App.instance().deviceInfo().homeDirectory() ) == 0;
	}

	protected boolean isRootDirectory( File file ) {
		return file.compareTo( App.instance().deviceInfo().rootDirectory() ) == 0;
	}

	protected boolean writableDirectory( File directory ) {
		return directory.canWrite();
	}

	protected boolean sameDirectory( File file, File toPasteDir ) {
		return toPasteDir.compareTo( file.getParentFile() ) == 0;
	}

	protected void showSoftKeyboardForDialog( Dialog dialog ) {
		dialog.getWindow().setSoftInputMode( WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE );
	}

	protected void hideSoftKeyboard( EditText edit ) {
		try {
			InputMethodManager imm = ( InputMethodManager ) getSystemService( INPUT_METHOD_SERVICE );
			imm.hideSoftInputFromWindow( edit.getWindowToken(), 0 );
		} catch ( Exception ne ) {
			ne.printStackTrace();
		}
	}

	protected File createFileForCurrentDir( String fileName ) {
		return new File( mCurrentDir, fileName );
	}

	// /////////////////////
	// Inner classes
	// /////////////////////

	protected class CheckFileExistListener implements TextWatcher {

		private static final String EXIST_MSG = "Invalid name, folder/file already exists";
		private static final String NOT_EXIST_MSG = "Valid folder/file name";

		private TextView tv;

		public CheckFileExistListener( TextView tv ) {
			this.tv = tv;
		}

		@Override
		public void afterTextChanged( Editable s ) {
			File file = createFileForCurrentDir( s.toString() );
			if ( file.exists() ) {
				tv.setTextColor( Color.RED );
				tv.setText( EXIST_MSG );
			} else {
				tv.setTextColor( Color.BLACK );
				tv.setText( NOT_EXIST_MSG );
			}
		}

		@Override
		public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
		}

		@Override
		public void onTextChanged( CharSequence s, int start, int before, int count ) {
		}

	}
}
