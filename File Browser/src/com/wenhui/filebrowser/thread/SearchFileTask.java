package com.wenhui.filebrowser.thread;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.wenhui.filebrowser.R;

public class SearchFileTask extends AsyncTask<String, String, Boolean> {
	
	private Dialog mDialog;
	private TextView mTextNum;
	private TextView mTextDir;
	private File mSearchDir;
	private boolean mIsShowing = true;
	private Matcher mMatcher;
	private int mResults = 0;
	private Pattern mPattern;
	private ArrayList<File> mSearchResults;
	private String mSearchQuery;
	private OnAfterSearchFinishedListener mListener;
	
	public SearchFileTask(Context context, File searchDir) {
		this.mSearchDir = searchDir;
		this.mSearchResults = new ArrayList<File>();
		createDialog( context );
	}

	private void createDialog( Context context ){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		LayoutInflater inflater = LayoutInflater.from(context);

		View view = inflater.inflate(R.layout.search_process, null);
		mTextNum = (TextView) view.findViewById(R.id.textView_num);
		mTextDir = (TextView) view.findViewById(R.id.textView_dir);
		mDialog = builder
				.setTitle(R.string.searching)
				.setView(view)
				.setCancelable(true)
				.setPositiveButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								cancel(true);
							}
						})
				.setNegativeButton(R.string.hide,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mIsShowing = false;
								dialog.cancel();
							}
						}).create();
	}
	
	@Override
	protected void onCancelled() {
		super.onCancelled();
		mDialog.cancel();
		if( mListener != null ){
			mListener.onAfterSearchFinished( mSearchResults, mSearchQuery );
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mDialog.show();
	}

	@Override
	protected Boolean doInBackground(String... files) {
		if (files.length == 0)
			return true;
		mSearchQuery = files[0];
		mPattern = createPattern(mSearchQuery);
		SearchFilter filter = new SearchFilter();
		mSearchResults.clear();
		searchFiles(mSearchDir, filter);
		return true;
	}

	@Override
	protected void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);
		try {
			mTextNum.setText(values[0]);
			mTextDir.setText(values[1]);
		} catch (NullPointerException ignore) {
		}
	}

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		mDialog.cancel();
		if( mListener != null ){
			mListener.onAfterSearchFinished( mSearchResults, mSearchQuery );
		}
	}
	
	private void searchFiles(File file, SearchFilter filter)
			throws NullPointerException {
		if (isCancelled()) {
			return;
		}
		if (file.isDirectory()) {
			if (file.canRead()) {
				mMatcher = mPattern.matcher(file.getName());
				if (mMatcher.find()) {
					mSearchResults.add(file);
				}
				File[] files = file.listFiles(filter);
				for (File f : files) {
					searchFiles(f, filter);
				}
			}
		} else {
			if (mIsShowing) {
				mResults++;
				String[] values = new String[] { Integer.toString(mResults),
						file.getName() };
				publishProgress(values);
			}
			mSearchResults.add(file);
		}
	}

	private class SearchFilter implements FileFilter {

		@Override
		public boolean accept(File pathname) {
			if (pathname.isDirectory() && !pathname.isHidden())
				return true;
			mMatcher = mPattern.matcher(pathname.getName());
			return mMatcher.find();
		}
	}

	/**
	 * Create search pattern.
	 * 
	 * @param query
	 * @return
	 */
	private Pattern createPattern(String query) {
		// Replace special character with "\" in front of it.
		query = query.replace( "*", "\\*" );
		query = query.replace( "^", "\\^" );
		query = query.replace( "$", "\\$" );
		String regx = "^";
		int dotPos = query.lastIndexOf(".");
		if (dotPos >= 0) {
			String prefix = query.substring(0, dotPos + 1);
			String suf = query.substring(dotPos);
			regx = regx + prefix + "*" + suf + "$";
		} else {
			regx = regx + query + "*";
		}
		return Pattern.compile(regx, Pattern.CASE_INSENSITIVE);
	}

	public void setOnAfterSearchFinishedListener( OnAfterSearchFinishedListener listener ){
		mListener = listener;
	}
	
	public interface OnAfterSearchFinishedListener {
		public void onAfterSearchFinished( List<File> result, String searchQuery );
	}
	
}
