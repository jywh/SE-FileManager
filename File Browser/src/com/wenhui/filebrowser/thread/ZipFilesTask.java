package com.wenhui.filebrowser.thread;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.wenhui.filebrowser.App;
import com.wenhui.filebrowser.R;
import com.wenhui.filebrowser.generic.FileUtils;

public class ZipFilesTask extends AsyncTask< File, String, Boolean > {

//	private static final String TAG = "ZipFileTask";
	
	private final static int BUFFER = 2048;
	
	private Dialog mDialog;
	private ProgressBar mProgressbar;
	private TextView mText;
	private File mDstDir;
	private int mTotalSize = 0;
	private int mNumFilesZipSoFar = 0;
	private boolean mShowing = true;
	private String mZipFileName;
	private OnAfterFilesZippedListener mListener;
	
	public ZipFilesTask( Context context, File dstDir, String fileName ) {
		this.mDstDir = dstDir;
		this.mZipFileName = fileName;
		createProgressDialog( context );
	}

	private void createProgressDialog( Context context ) {
		AlertDialog.Builder builder = new AlertDialog.Builder( context );
		LayoutInflater inflater = LayoutInflater.from( context );

		View view = inflater.inflate( R.layout.copy_progress, null );
		mProgressbar = ( ProgressBar ) view.findViewById( R.id.progressBar1 );
		mText = ( TextView ) view.findViewById( R.id.textView_cpy_file );
		mDialog = builder.setTitle( R.string.compressing ).setView( view ).setCancelable( true ).setPositiveButton( R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick( DialogInterface dialog, int which ) {
				cancel( true );
			}
		} ).setNegativeButton( R.string.hide, new DialogInterface.OnClickListener() {

			@Override
			public void onClick( DialogInterface dialog, int which ) {
				mShowing = false;
				dialog.cancel();
			}
		} )
		.setCancelable( false )
		.create();
	}
	
	@Override
	protected void onCancelled() {
		super.onCancelled();
		mDialog.cancel();
		if( mListener != null ){
			mListener.onAfterFilesZipped();
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mProgressbar.setProgress( 0 );
		mDialog.show();
	}

	@Override
	protected Boolean doInBackground( File... files ) {
		if ( files.length == 0 ){
			return false;
		}
		// Indicate system media cache need to be refreshed.
		App.instance().setRefreshCache( true );
		mTotalSize = FileUtils.countFile( files[0] );
		mProgressbar.setMax( mTotalSize );
		
		return createZipFile( files[0], mDstDir );

	}

	@Override
	protected void onProgressUpdate( String... values ) {
		super.onProgressUpdate( values );
		mText.setText( values[0] );
		try {
			mProgressbar.setProgress( Integer.parseInt( values[1] ) );
		} catch ( NumberFormatException e ) {

		}
	}

	@Override
	protected void onPostExecute( Boolean result ) {
		super.onPostExecute( result );
		mDialog.cancel();
		String msg = "";
		if ( result ){
			msg = App.instance().getString( R.string.done );
		} else {
			msg = App.instance().getString( R.string.fail );
		}
		Toast.makeText( App.instance(), msg, Toast.LENGTH_SHORT ).show();
		if( mListener != null ){
			mListener.onAfterFilesZipped();
		}
	}
	
	private boolean createZipFile( File inFile, File curDir ) {

		if ( !inFile.canRead() || !inFile.canWrite() ){
			return false;
		}
		
		String name = inFile.getName();
		
		File outFile = new File( curDir, mZipFileName );
		try {

			FileOutputStream dest = new FileOutputStream( outFile );
			ZipOutputStream out = new ZipOutputStream( new BufferedOutputStream( dest ) );
			if ( inFile.isFile() ) {
				zipFile( inFile, out, "" );
			} else {
				File files[] = inFile.listFiles();

				for ( File f : files ) {
					zipFile( f, out, name + "/" );
				}
			}
			out.flush();
			out.close();
		} catch ( Exception e ) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void zipFile( File file, ZipOutputStream out, String name ) throws IOException {

		// If cancel button is clicked, cancel the process.
		if ( isCancelled() ) {
			return;
		}
		byte data[] = new byte[BUFFER];
		name = name + file.getName();
		if ( file.isFile() ) {
			FileInputStream fi = new FileInputStream( file );
			BufferedInputStream origin = new BufferedInputStream( fi, BUFFER );
			ZipEntry entry = new ZipEntry( name );
			entry.setMethod( ZipOutputStream.DEFLATED );
			entry.setSize( file.length() );
			out.putNextEntry( entry );
			int count;

			while ( ( count = origin.read( data, 0, BUFFER ) ) != -1 ) {
				out.write( data, 0, count );
			}

			if ( mShowing ) {
				mNumFilesZipSoFar++;
				String[] updateMsg = new String[] { file.getName(), Integer.toString( mNumFilesZipSoFar ) };
				publishProgress( updateMsg );
			}
			origin.close();
		} else {
			// If file is directory, zip file recursively.
			name = name + "/";
			if ( file.canRead() ) {
				File[] files = file.listFiles();
				for ( File f : files ) {
					zipFile( f, out, name );
				}
			}
		}
	}

	public void setOnAfterFilesZippedListener( OnAfterFilesZippedListener listener ){
		this.mListener = listener;
	}
	
	public static interface OnAfterFilesZippedListener {
		/**
		 * This method is attempted to do task in UI thread.
		 */
		public void onAfterFilesZipped();
	}

}
