package com.wenhui.filebrowser.thread;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wenhui.filebrowser.App;
import com.wenhui.filebrowser.R;
import com.wenhui.filebrowser.activity.RootActivity;
import com.wenhui.filebrowser.generic.FileUtils;

public class DeleteThread {

	private static final int MESSAGE_PROGRESS = 101;
	private static final int MESSAGE_FINISH = 102;
	
	private Handler mHandler;
	private Dialog mDialog;
	private ProgressBar mProgressBar;
	private TextView mText;
	private volatile int mTotalSize = 0;
	private volatile int mNumFilesDeleteSoFar = 0;
	private DeleteFilesThread mThread;
	
	private volatile File[] mFilesToDelete;

	private Handler mProgressHandler = new Handler() {
		@Override
		public void handleMessage( Message msg ) {
			switch( msg.what ){
			case MESSAGE_PROGRESS:
				int percentage = msg.arg1;
				String message = ( String ) msg.obj;
				mText.setText( message );
				mProgressBar.setProgress( percentage );
				break;
			case MESSAGE_FINISH:
				Toast.makeText( App.instance(), R.string.done, Toast.LENGTH_SHORT ).show();
				Message text = mHandler.obtainMessage( RootActivity.MESSAGE_REFRESH_LIST );
				mHandler.sendMessage( text );
				mDialog.cancel();
				break;
			}
		}
	};
	
	public DeleteThread( Context context, Handler handler ) {
		this.mHandler = handler;
		setupDialog( context );
	}

	public void setupDialog( Context context ) {
		AlertDialog.Builder builder = new AlertDialog.Builder( context );
		LayoutInflater inflater = LayoutInflater.from( context );

		View view = inflater.inflate( R.layout.copy_progress, null );
		mProgressBar = ( ProgressBar ) view.findViewById( R.id.progressBar1 );
		mText = ( TextView ) view.findViewById( R.id.textView_cpy_file );

		mDialog = builder.setTitle( R.string.delete ).setView( view ).setCancelable( true ).setPositiveButton( R.string.cancel, new OnClickListener() {
			@Override
			public void onClick( DialogInterface dialog, int which ) {
				synchronized ( mThread ) {
					mThread.toCancel = true;
				}
			}
		} ).setNegativeButton( R.string.hide, new OnClickListener() {

			@Override
			public void onClick( DialogInterface dialog, int which ) {
				synchronized ( mThread ) {
					mThread.toShow = false;
				}
				dialog.cancel();
			}
		} )
		.setCancelable( false )
		.create();
		
	}

	public void start( File[] toDelete ) {
		// Indicate system media cache need to be refreshed.
		App.instance().setRefreshCache( true );
		mFilesToDelete = toDelete;
		mDialog.show();
		mTotalSize = calculateMultipleFileSize();
		mProgressBar.setMax( mTotalSize );
		mThread = new DeleteFilesThread();
		mThread.start();

	}

	private int calculateMultipleFileSize() {
		int size = 0;
		for ( File file : mFilesToDelete ) {
			size += FileUtils.countFile( file );
		}
		return size;
	}

	private class DeleteFilesThread extends Thread {

		boolean toCancel = false;
		boolean toShow = true;

		@Override
		public void run() {

			for ( File file : mFilesToDelete ) {
				deleteFiles( file );
			}
			Message msg = mProgressHandler.obtainMessage( MESSAGE_FINISH );
			mProgressHandler.sendMessage( msg );

		}

		@SuppressLint("HandlerLeak")
		private boolean deleteFiles( File file ) {
			if ( toCancel ) {
				return true;
			}
			if ( file.isFile() ) {
				String name = file.getName();
				file.delete();
				if ( toShow ) {
					mNumFilesDeleteSoFar++;
					Message msg = mProgressHandler.obtainMessage( MESSAGE_PROGRESS, mNumFilesDeleteSoFar, 0, name );
					mProgressHandler.sendMessage( msg );
				}

			} else {
				try {
					File[] files = file.listFiles();
					for ( File f : files ) {
						deleteFiles( f );
					}
					file.delete();
				} catch ( Exception e ) {

				}
			}
			return true;
		}
	}

}
