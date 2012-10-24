package com.wenhui.filebrowser.thread;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.wenhui.filebrowser.App;
import com.wenhui.filebrowser.R;
import com.wenhui.filebrowser.activity.RootActivity;
import com.wenhui.filebrowser.generic.FileUtils;

public class UnzipThread {

	private static final int MESSAGE_FILE_EXIST = 100;
	private static final int MESSAGE_PROGRESS = 101;
	private static final int MESSAGE_FINISH = 102;

	private final static int BUFFER = 2048;

	private Handler mHandler;
	private Dialog mDialog;
	private Dialog mWaringExistDialog;
	private ProgressBar mProgressbar;
	private TextView mText;
	private TextView mTextExist;
	private CheckBox mCheckbox;
	private volatile int mTotalSize = 0;
	private volatile int mNumFilesExtractSoFar = 0;
	private boolean mDoTheFollowing = false;
	private UnzipFilesThread mThread;
	private File mToDir;
	private File mFileToUnzip;

	private Handler progressHandler = new Handler() {
		@Override
		public void handleMessage( Message msg ) {
			switch( msg.what ){
			case MESSAGE_PROGRESS:
				int percentage = msg.arg1;
				String message1 = ( String ) msg.obj;
				mText.setText( message1 );
				mProgressbar.setProgress( percentage );
				break;
			case MESSAGE_FILE_EXIST:
				String fileExistMsg = ( String ) msg.obj;
				if ( mDialog.isShowing() ){ mDialog.dismiss(); }
				mTextExist.setText( fileExistMsg );
				mWaringExistDialog.show();
				break;
			case MESSAGE_FINISH:
				Toast.makeText( App.instance(), R.string.done, Toast.LENGTH_SHORT ).show();
				Message message2 = mHandler.obtainMessage( RootActivity.MESSAGE_REFRESH_LIST );
				mHandler.sendMessage( message2 );
				mDialog.cancel();
				break;
			}
		}
	};

	public UnzipThread( Context context, Handler handler ) {
		this.mHandler = handler;
		setupDialog( context );
	}

	public void setupDialog( Context context ) {
		LayoutInflater inflater = LayoutInflater.from( context );
		createProgressDialog( context, inflater );
		createExistDialog( context, inflater );
	}

	public void start( File toDir, File toUnzip ) {
		// Indicate system media cache need to be refreshed.
		App.instance().setRefreshCache( true );
		this.mFileToUnzip = toUnzip;
		this.mToDir = toDir;
		mDialog.show();
		mTotalSize = calculateTotalSize( toUnzip );
		mProgressbar.setMax( mTotalSize );
		mThread = new UnzipFilesThread();
		mThread.start();

	}

	private void createProgressDialog( Context context, LayoutInflater inflater ) {
		AlertDialog.Builder builder = new AlertDialog.Builder( context );

		View view = inflater.inflate( R.layout.copy_progress, null );
		mProgressbar = ( ProgressBar ) view.findViewById( R.id.progressBar1 );
		mText = ( TextView ) view.findViewById( R.id.textView_cpy_file );

		mDialog = builder.setTitle( R.string.extracting ).setView( view ).setCancelable( true ).setPositiveButton( R.string.cancel, new OnClickListener() {
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
					mThread.toSkip = true;
					mThread.pleaseWait = false;
					mThread.notify();
				}
				dialog.cancel();
			}
		} )
		.setCancelable( false )
		.create();

	}

	private void createExistDialog( Context context, LayoutInflater inflater ) {
		View view1 = inflater.inflate( R.layout.file_exist_warning, null );
		AlertDialog.Builder builder1 = new AlertDialog.Builder( context );
		builder1.setTitle( R.string.file_exist_warning ).setIcon( R.drawable.warning ).setView( view1 );
		mTextExist = ( TextView ) view1.findViewById( R.id.textView_file_exist );
		mCheckbox = ( CheckBox ) view1.findViewById( R.id.checkBox_do_following );

		mCheckbox.setOnCheckedChangeListener( new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
				mDoTheFollowing = isChecked;
			}
		} );
		mWaringExistDialog = builder1.setPositiveButton( R.string.skip, new OnClickListener() {

			@Override
			public void onClick( DialogInterface dialog, int which ) {
				synchronized ( mThread ) {
					mThread.toSkip = true;
					mThread.toDoFollowing = mDoTheFollowing;
					mThread.pleaseWait = false;
					mThread.notify();
				}
				if ( !UnzipThread.this.mDialog.isShowing() )
					UnzipThread.this.mDialog.show();
			}
		} ).setNeutralButton( R.string.overwrite, new OnClickListener() {
			@Override
			public void onClick( DialogInterface dialog, int which ) {
				synchronized ( mThread ) {
					mThread.toSkip = false;
					mThread.toDoFollowing = mDoTheFollowing;
					mThread.pleaseWait = false;
					mThread.notify();
				}
				if ( !UnzipThread.this.mDialog.isShowing() )
					UnzipThread.this.mDialog.show();

			}
		} ).setNegativeButton( R.string.cancel, new OnClickListener() {

			@Override
			public void onClick( DialogInterface dialog, int which ) {
				UnzipThread.this.mDialog.cancel();
				synchronized ( mThread ) {
					mThread.toCancel = true;
					mThread.pleaseWait = false;
					mThread.notify();
				}
			}
		} ).create();
	}

	private int calculateTotalSize( File zipFile ) {
		int size = 0;
		try {
			ZipFile zipFiles = new ZipFile( zipFile );

			@SuppressWarnings("rawtypes")
			Enumeration entries = zipFiles.entries();

			while ( entries.hasMoreElements() ) {
				size++;
				entries.nextElement();
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		return size;
	}

	private class UnzipFilesThread extends Thread {
		boolean pleaseWait = false;
		boolean toDoFollowing = false;
		boolean toCancel = false;
		boolean toShow = true;
		boolean toSkip = false;

		@Override
		public void run() {
			if ( unzipFileToDir( mFileToUnzip, mToDir ) ) {
				Message msg = progressHandler.obtainMessage( MESSAGE_FINISH );
				progressHandler.sendMessage( msg );
			}
		}

		/**
		 * 
		 * @param fromFile
		 *            is a zip file
		 * @param toDir
		 *            to the directory
		 * @return
		 */
		private boolean unzipFileToDir( File zipFile, File toDir ) {
			try {
				String zipFolderName = FileUtils.createZipFolderName( zipFile );
				File zipFolder = new File( toDir, zipFolderName );
				if ( !zipFolder.exists() ) {
					zipFolder.mkdirs();
				}
				BufferedOutputStream dest = null;
				FileInputStream fis = new FileInputStream( zipFile );
				ZipInputStream zis = new ZipInputStream( new BufferedInputStream( fis ) );
				ZipEntry entry;
				while ( ( entry = zis.getNextEntry() ) != null ) {
					if ( toCancel ) {
						break;
					}
					String entryName = entry.getName();
					File outputFile = new File( zipFolder, entryName );

					File parent = outputFile.getParentFile();
					if ( !parent.exists() ) {
						parent.mkdirs();
					}
					if ( entry.isDirectory() ) {
						if ( !outputFile.exists() )
							outputFile.mkdirs();
					} else {

						if ( outputFile.exists() && !toDoFollowing ) {
							pleaseWait = true;
							Message msg = progressHandler.obtainMessage();
							msg.what = MESSAGE_FILE_EXIST;
							msg.obj = outputFile.getPath();
							progressHandler.sendMessage( msg );
							pleaseWait = true;
						}

						while ( pleaseWait ) {
							try {
								wait();
							} catch ( Exception e ) {

							}
						}

						if ( toSkip ) {
							if ( !toDoFollowing ) {
								toSkip = false;
							}

						} else {
							int count;
							byte data[] = new byte[BUFFER];
							FileOutputStream fos = new FileOutputStream( outputFile );
							dest = new BufferedOutputStream( fos, BUFFER );
							while ( ( count = zis.read( data, 0, BUFFER ) ) != -1 ) {
								dest.write( data, 0, count );
							}
							dest.flush();
							dest.close();
						}

					}
					if ( toShow ) {
						mNumFilesExtractSoFar++;
						String fileMsg = outputFile.getName() + " => " + outputFile.getParent() + File.separator;

						Message msg = progressHandler.obtainMessage( MESSAGE_PROGRESS, mNumFilesExtractSoFar, 0, fileMsg );
						progressHandler.sendMessage( msg );
					}

				}
				zis.close();
			} catch ( Exception e ) {
				e.printStackTrace();
			}
			return true;
		}
	}
	
}
