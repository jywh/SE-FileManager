package com.wenhui.filebrowser.thread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.wenhui.filebrowser.App;
import com.wenhui.filebrowser.R;
import com.wenhui.filebrowser.activity.RootActivity;
import com.wenhui.filebrowser.generic.FileUtils;

/**
 * Background thread used to copy/move files.
 * 
 */
public class CopyThread {

	public static final int TO_COPY = 0;
	public static final int TO_MOVE = 1;

	private static final int MESSAGE_FILE_EXIST = 100;
	private static final int MESSAGE_PROGRESS = 101;
	private static final int MESSAGE_FINISH = 102;

	private Handler mHandler;
	private Dialog mProgressDialog;
	private Dialog mExistWarningDialog;
	private ProgressBar mProgressBar;
	private TextView mText;
	private TextView mTextExistWaring;
	private CheckBox mCheckbox;
	private int mMoveFile = 0; // 0 is copy, 1 is move
	private volatile int mTotalSize = 0;
	private volatile int mNumFilesCopySoFar = 0;
	private boolean mDoTheFollowing = false;
	private CopyFilesThread mThread;

	private volatile File mDstDir;
	private volatile File[] mFilesToCopy;

	private Handler mProgressHandler = new Handler() {
		@Override
		public void handleMessage( Message msg ) {
			
			switch( msg.what ){
			case MESSAGE_PROGRESS:
				int percentage = msg.arg1;
				String text = ( String ) msg.obj;
				mText.setText( text );
				mProgressBar.setProgress( percentage );
				break;
			case MESSAGE_FILE_EXIST:
				String fileExistMsg = ( String ) msg.obj;
				if ( mProgressDialog.isShowing() ){ mProgressDialog.dismiss(); }
				mTextExistWaring.setText( fileExistMsg );
				mExistWarningDialog.show();
				break;
			case MESSAGE_FINISH:
				Toast.makeText( App.instance(), R.string.done, Toast.LENGTH_SHORT ).show();
				Message message = mHandler.obtainMessage( RootActivity.MESSAGE_REFRESH_LIST );
				mHandler.sendMessage( message );
				mProgressDialog.cancel();
				break;
			}
		}
	};

	public CopyThread( Context context, Handler handler, int flag ) {
		this.mHandler = handler;
		this.mMoveFile = flag;
		setupDialog( context );
	}

	public void setupDialog( Context context ) {
		LayoutInflater inflater = LayoutInflater.from( context );
		createProgressDialog( context, inflater );
		createExistDialog( context, inflater );
	}

	/**
	 * Create a progress dialog display file operation progress.
	 * @param context
	 * @param inflater
	 */
	private void createProgressDialog( Context context, LayoutInflater inflater ) {
		AlertDialog.Builder builder = new AlertDialog.Builder( context );
		View view = inflater.inflate( R.layout.copy_progress, null );
		mProgressBar = ( ProgressBar ) view.findViewById( R.id.progressBar1 );
		mText = ( TextView ) view.findViewById( R.id.textView_cpy_file );

		String title = ( mMoveFile == 0 ) ? context.getString( R.string.copying_file ) : context.getString( R.string.moving );
		mProgressDialog = builder.setTitle( title ).setView( view ).setCancelable( true ).setPositiveButton( R.string.cancel, new OnClickListener() {
			@Override
			public void onClick( DialogInterface dialog, int which ) {
				synchronized ( mThread ) {
					mThread.toCancel = true;
					mThread.pleaseWait = false;
					mThread.notify();
				}
			}
		} ).setNegativeButton( R.string.hide, new OnClickListener() {

			@Override
			public void onClick( DialogInterface dialog, int which ) {
				synchronized ( mThread ) {
					mThread.toShow = false;
					mThread.pleaseWait = false;
					mThread.notify();
				}
				dialog.cancel();
			}
		} ).setCancelable( false ).create();
	}

	/**
	 * Create a dialog for warning file is already exists.
	 * @param context
	 * @param inflater
	 */
	private void createExistDialog( Context context, LayoutInflater inflater ) {
		View view1 = inflater.inflate( R.layout.file_exist_warning, null );
		AlertDialog.Builder builder1 = new AlertDialog.Builder( context );
		builder1.setTitle( R.string.file_exist_warning ).setIcon( R.drawable.warning ).setView( view1 );
		mTextExistWaring = ( TextView ) view1.findViewById( R.id.textView_file_exist );
		mCheckbox = ( CheckBox ) view1.findViewById( R.id.checkBox_do_following );

		mCheckbox.setOnCheckedChangeListener( new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
				mDoTheFollowing = isChecked;
			}
		} );
		mExistWarningDialog = builder1.setPositiveButton( R.string.skip, new OnClickListener() {

			@Override
			public void onClick( DialogInterface dialog, int which ) {
				synchronized ( mThread ) {
					mThread.toSkip = true;
					mThread.toDoFollowing = mDoTheFollowing;
					mThread.pleaseWait = false;
					mThread.notify();
				}
				if ( !CopyThread.this.mProgressDialog.isShowing() )
					CopyThread.this.mProgressDialog.show();
			}
		} )
		.setNeutralButton( R.string.overwrite, new OnClickListener() {
			@Override
			public void onClick( DialogInterface dialog, int which ) {
				synchronized ( mThread ) {
					mThread.toSkip = false;
					mThread.toDoFollowing = mDoTheFollowing;
					mThread.pleaseWait = false;
					mThread.notify();
				}
				if ( !CopyThread.this.mProgressDialog.isShowing() )
					CopyThread.this.mProgressDialog.show();

			}
		} )
		.setNegativeButton( R.string.cancel, new OnClickListener() {

			@Override
			public void onClick( DialogInterface dialog, int which ) {
				CopyThread.this.mProgressDialog.cancel();
				synchronized ( mThread ) {
					mThread.toCancel = true;
					mThread.pleaseWait = false;
					mThread.toSkip = true;
					mThread.notify();
				}
			}
		} )
		.setCancelable( false )
		.create();
	}

	/**
	 * Start copying files
	 * @param curDir
	 * @param toCopy
	 */
	public void start( File curDir, File[] toCopy ) {
		// Indicate system media cache need to be refreshed.
		App.instance().setRefreshCache( true );
		mFilesToCopy = toCopy;
		mDstDir = curDir;
		mProgressDialog.show();
		mTotalSize = calculateTotalSize();
		Log.i( "total size:", Integer.toString( mTotalSize ) );
		mProgressBar.setMax( mTotalSize );
		mThread = new CopyFilesThread();
		mThread.start();

	}

	/**
	 * Return the number of files in the diretory
	 * @return
	 */
	private int calculateTotalSize() {
		int size = 0;
		for ( File file : mFilesToCopy ) {
			size += FileUtils.countFile( file );
		}
		return size;
	}

	private class CopyFilesThread extends Thread {

		boolean pleaseWait = false;
		boolean toDoFollowing = false;
		boolean toCancel = false;
		boolean toShow = true;
		boolean toSkip = false;

		@Override
		public void run() {
			for ( File file : mFilesToCopy ) {
				String filename = file.getName();
				File dstFile = new File( mDstDir, filename );
				copyFiles( file, dstFile );
			}
			Message msg = mProgressHandler.obtainMessage( MESSAGE_FINISH );
			mProgressHandler.sendMessage( msg );

		}

		private boolean copyFiles( File src, File dst ) {
			if ( toCancel ) {
				return true;
			}
			if ( src.isDirectory() ) {
				copyDirectory( src, dst );
			} else if ( src.canRead() ) {
				copySingleFile( src, dst );
			}
			return true;
		}

		private void copyDirectory( File src, File dst ) {
			if ( !dst.exists() ) {
				// dst directory not exists, create it.
				dst.mkdirs();
			}
			
			try {
				File[] allFiles = src.listFiles();
				for ( File f : allFiles ) {
					String filename = f.getName();
					File newSrc = new File( src, filename );
					File newDst = new File( dst, filename );
					copyFiles( newSrc, newDst );
				}
				
				if ( mMoveFile == TO_MOVE ) {
					// This will try to delete an empty directory,
					// if the directory is not empty, this will not take effect.
					src.delete();
				}

			} catch ( SecurityException ignore ) {
			}
		}

		private void copySingleFile( File src, File dst ) {
			try {
				if ( dst.exists() && !toDoFollowing ) {
					// When file exists, wait for user's response
					pleaseWait = true;
					Message msg = mProgressHandler.obtainMessage();
					msg.what = MESSAGE_FILE_EXIST;
					msg.obj = dst.getPath();
					mProgressHandler.sendMessage( msg );
				}

				while ( pleaseWait ) {
					try {
						wait();
					} catch ( Exception e ) {

					}
				}
				String parent = src.getParent();
				if ( toSkip ) {
					if ( !toDoFollowing )
						toSkip = false;
				} else {
					copyFile( src, dst );
					if ( mMoveFile == TO_MOVE ) {// to move file, delete the source file
						src.delete();
					}
				}
				if ( toShow ) {
					mNumFilesCopySoFar++;
					String fileMsg = src.getName() + " => " + parent + File.separator;
					Message msg = mProgressHandler.obtainMessage( MESSAGE_PROGRESS, mNumFilesCopySoFar, 0, fileMsg );
					mProgressHandler.sendMessage( msg );
				}
			} catch ( Exception e ) {
			}
		}

		private void copyFile( File src, File dst ) throws IOException {
			FileChannel inChannel = new FileInputStream( src ).getChannel();
			FileChannel outChannel = new FileOutputStream( dst ).getChannel();
			try {
				inChannel.transferTo( 0, inChannel.size(), outChannel );
			} finally {
				if ( inChannel != null )
					inChannel.close();
				if ( outChannel != null )
					outChannel.close();
			}
		}

	}

}
