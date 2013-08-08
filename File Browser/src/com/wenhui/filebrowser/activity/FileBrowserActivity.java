package com.wenhui.filebrowser.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.*;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.TextView.OnEditorActionListener;

import com.wenhui.filebrowser.App;
import com.wenhui.filebrowser.R;
import com.wenhui.filebrowser.adapters.SearchResultAdapter;
import com.wenhui.filebrowser.adapters.SendOptionsAdapter;
import com.wenhui.filebrowser.generic.FileManagerProvider;
import com.wenhui.filebrowser.generic.FileUtils;
import com.wenhui.filebrowser.generic.IntentManager;
import com.wenhui.filebrowser.generic.MIMEType;
import com.wenhui.filebrowser.model.AppToSendOption;
import com.wenhui.filebrowser.model.FileModel;
import com.wenhui.filebrowser.thread.*;
import com.wenhui.filebrowser.thread.SearchFileTask.OnAfterSearchFinishedListener;
import com.wenhui.filebrowser.thread.ZipFilesTask.OnAfterFilesZippedListener;

public class FileBrowserActivity extends RootActivity {

	private static final String TAG = "FileBrowserActivity";

	private final static int DIALOG_SORT_BY = 1;
	private final static int DIALOG_SEARCH = 3;
	private final static int DIALOG_OPEN_FAIL = 4;
	private final static int DIALOG_VIEW_BY = 7;
	private final static int DIALOG_SEND_OPTION = 8;

	private LinearLayout mLayoutPaste;
	private Button mButtonPaste;
	private Button mButtonCancel;

	private FileModel mFileToCopy;

	private volatile boolean mQuit = false;
	private ExitCountDownTimer mExitTimer;

	private IntentManager mIntentManager;

	private boolean mGetContentMode = false;

	private List<ResolveInfo> ris;
	private ArrayList<AppToSendOption> appsOptions = new ArrayList<AppToSendOption>();

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_REFRESH_LIST:
				refreshList(false);
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mIntentManager = new IntentManager(this);
		Intent intent = getIntent();
		if (intent != null && intent.getAction() != null
				&& intent.getAction().equals(Intent.ACTION_GET_CONTENT)) {
			mGetContentMode = true;
		}

		initView();
		initButtomBarUI();
	}

	protected void initView() {
		super.initView();

		// Show action bar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setTitle("");
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		mDirAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mDirStack);
		mDirAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		actionBar.setListNavigationCallbacks(mDirAdapter, new OnDirectoryNavigationItemSelectionListener());
	}

	public void initButtomBarUI() {
		mLayoutPaste = (LinearLayout) findViewById(R.id.linearLayout_paste);
		mLayoutPaste.setVisibility(View.GONE);
		mButtonPaste = (Button) findViewById(R.id.button_paste);
		mButtonCancel = (Button) findViewById(R.id.button_cancel);
		mButtonPaste.setOnClickListener(new OnPasteButtonClick());
		mButtonCancel.setOnClickListener(new OnCancelButtonClick());
	}

	@Override
	protected void onResume() {
		super.onResume();
		App.instance().setMultiSelectMode(false);
		if (!App.instance().deviceInfo().isExternalStorageAval()) {
			Toast.makeText(this, R.string.sdcard_not_avai, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		mCurrentDir = App.instance().getCurrentPath();
		refreshList(true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		cancelExitTimer();
	}

	@Override
	protected void onListItemClick(AdapterView<?> arg0, View v, int position, long id) {
		File file = mAdapter.getItem(position).getFile();
		openFile(file);
	}

	private void openFile(File file) {
		if (file == null)
			return;

		if (file.isDirectory()) {
			if (file.canRead()) {
				mCurrentDir = file;
				refreshList(false);
			} else {
				Toast.makeText(FileBrowserActivity.this, R.string.permission_denial, Toast.LENGTH_LONG)
						.show();
			}

			return;
		}

		// open from other app
		if (mGetContentMode) {
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW);

			Uri data = Uri.fromFile(file);
			String type = MIMEType.get(FileUtils.getFileExtension(file));
			intent.setDataAndType(data, type);
			// In that case, we should probably just return the requested data.
			intent.setData(Uri.parse(FileManagerProvider.FILE_PROVIDER_PREFIX + file));
			setResult(RESULT_OK, intent);
			finish();
			return;
		}

		if (FileUtils.getFileExtension(file).equals(".zip")
				|| FileUtils.getFileExtension(file).equals(".gzip")) {
			showUnzipOptionsDialog(file);
			return;
		}

		if (!mIntentManager.executeIntent(file)) {
			showDialog(DIALOG_OPEN_FAIL);
		}
	}

	private void backPress() {
		if (!isHomeDirectory(mCurrentDir) && !isRootDirectory(mCurrentDir)) {
			mCurrentDir = mCurrentDir.getParentFile();
			refreshList(false);
		} else if (mQuit) {
			finish();
		} else {
			cancelExitTimer();
			mQuit = true;
			mExitTimer = new ExitCountDownTimer();
			mExitTimer.start();
			Toast.makeText(FileBrowserActivity.this, R.string.press_again_to_exit, Toast.LENGTH_SHORT).show();

		}
	}

	private void cancelExitTimer() {
		if (mExitTimer != null) {
			mExitTimer.cancel();
			mExitTimer = null;
		}
	}

	private class ExitCountDownTimer extends CountDownTimer {

		public ExitCountDownTimer() {
			super(1000l, 1000l);
		}

		@Override
		public void onFinish() {
			mQuit = false;
		}

		@Override
		public void onTick(long millisUntilFinished) {
		}

	}

	// ///// Option Menu
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.file_browser, menu);
		if (mGetContentMode) {
			menu.findItem(R.id.add_folder).setEnabled(false);
			menu.findItem(R.id.multi_selection).setEnabled(false);
		}

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		setRemovableMenuItem(menu);
		return true;
	}

	private void setRemovableMenuItem(Menu menu) {
		if (App.instance().getRemovableDir() != null) {
			MenuItem item = menu.findItem(R.id.removable_drive);
			item.setVisible(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			backPress();
			break;
		case R.id.home:
			goHome();
			break;
		case R.id.search:
			showDialog(DIALOG_SEARCH);
			break;
		case R.id.add_folder:
			if (mCurrentDir != null) {
				showAddFolderDialog();
			}
			break;
		case R.id.multi_selection:
			Intent multiIntent = new Intent(FileBrowserActivity.this, MultiSelectionActivity.class);
			startActivity(multiIntent);
			break;
		case R.id.view_by:
			showDialog(DIALOG_VIEW_BY);
			break;
		case R.id.list_by:
			showDialog(DIALOG_SORT_BY);
			break;
		case R.id.removable_drive:
			mCurrentDir = App.instance().getRemovableDir();
			refreshList(false);
			break;
		case R.id.setting:
			startActivity(new Intent(FileBrowserActivity.this, PrefsActivity.class));
			break;
		case R.id.exit:
			finish();
			break;
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		backPress();
	}
	
	@Override
	protected int getResId() {
		return R.layout.file_browser;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);

		if (ris == null) {
			Intent sendOption = new Intent();
			sendOption.setType("application/*");
			sendOption.setAction(Intent.ACTION_SEND_MULTIPLE);
			ris = getPackageManager().queryIntentActivities(sendOption, 0);
		}
		if (appsOptions.isEmpty()) {
			for (ResolveInfo ri : ris) {
				Drawable icon = ri.loadIcon(getPackageManager());
				String appname = (String) ri.loadLabel(getPackageManager());
				String packagename = ri.activityInfo.packageName;
				String classname = ri.activityInfo.name;
				appsOptions.add(new AppToSendOption(icon, appname, packagename, classname));
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_SORT_BY:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			return builder
					.setTitle(R.string.list_by)
					.setSingleChoiceItems(R.array.list_by, App.instance().getCurrentListBy(),
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog, int which) {
									App.instance().setCurrentListBy(which);
									refreshList(false);
									dialog.cancel();
								}
							}).setNegativeButton(R.string.cancel, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					}).create();
		case DIALOG_SEARCH:
			return createSearchDialog();
		case DIALOG_OPEN_FAIL:
			AlertDialog.Builder builder4 = new AlertDialog.Builder(this);
			return builder4.setTitle(R.string.open_file).setMessage(R.string.open_file_msg)
					.setPositiveButton(R.string.ok, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					}).create();
		case DIALOG_VIEW_BY:
			AlertDialog.Builder b8 = new AlertDialog.Builder(this);
			String[] viewOptions = new String[] { getString(R.string.list), getString(R.string.thumbnail),
					getString(R.string.list_with_detail) };
			return b8.setTitle(R.string.view_by).setItems(viewOptions, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (App.instance().getListOption() == which) {
						return;
					}
					App.instance().setListOption(which);
					refreshList(true);
				}
			}).create();
		case DIALOG_SEND_OPTION:
			AlertDialog.Builder builder9 = new AlertDialog.Builder(this);
			ArrayAdapter<AppToSendOption> adapter01 = new SendOptionsAdapter(this, appsOptions);
			return builder9.setTitle(getString(R.string.option))
					.setSingleChoiceItems(adapter01, -1, new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							AppToSendOption app = appsOptions.get(which);
							String packagename = app.getPackagename();
							String classname = app.getClassname();
							Intent intentSend = new Intent(FileBrowserActivity.this,
									ShareMultipleFilesActivity.class);
							intentSend.putExtra(ShareMultipleFilesActivity.APP_TO_SEND_PACKAGENAME,
									packagename);
							intentSend.putExtra(ShareMultipleFilesActivity.APP_TO_SEND_CLASSNAME, classname);
							startActivity(intentSend);
							dialog.dismiss();
						}
					}).setNegativeButton(R.string.cancel, null).create();
		}
		return null;
	}

	private File getFileAtPosition(int position) {
		return mAdapter.getItem(position).getFile();
	}

	// ////// Context Menu
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		int position = ((AdapterContextMenuInfo) menuInfo).position;
		File file = getFileAtPosition(position);
		menu.setHeaderTitle(file.getName());
		if (file.isDirectory()) {
			getMenuInflater().inflate(R.menu.dir_context_menu, menu);
		} else {
			getMenuInflater().inflate(R.menu.file_context_menu, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final int position = info.position;
		File file = getFileAtPosition(position);
		switch (item.getItemId()) {
		case R.id.file_delete:
		case R.id.folder_delete:
			Log.i(TAG, "delete file");
			deleteFileConfirm(file);
			break;
		case R.id.file_rename:
		case R.id.folder_rename:
			rename(file);
			break;
		case R.id.file_copy:
		case R.id.folder_copy:
			FileModel copyModel = new FileModel(FileModel.TO_COPY, file);
			copyMoveUnzipFile(copyModel);
			break;
		case R.id.file_move:
		case R.id.folder_move:
			FileModel moveModel = new FileModel(FileModel.TO_MOVE, file);
			copyMoveUnzipFile(moveModel);
			break;
		case R.id.file_detail:
		case R.id.folder_detail:
			new DisplayDetailTask().execute(file);
			break;
		case R.id.file_share:
			share(file);
			break;
		case R.id.folder_zip:
		case R.id.file_zip:
			showBeforeZipDialog(file);
			break;
		}
		return true;
	}

	private void deleteFileConfirm(final File file) {
		AlertDialog.Builder builder = new AlertDialog.Builder(FileBrowserActivity.this);
		builder.setTitle(R.string.delete).setMessage(file.getName());
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				File[] files = new File[] { file };
				new DeleteThread(FileBrowserActivity.this, mHandler).start(files);

			}
		}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		}).create().show();
	}

	private void rename(final File originalFile) {
		AlertDialog.Builder builder = new AlertDialog.Builder(FileBrowserActivity.this);
		LayoutInflater inflator = LayoutInflater.from(FileBrowserActivity.this);
		View view = inflator.inflate(R.layout.entry, null);
		builder.setTitle(R.string.rename).setView(view);
		final EditText edit = (EditText) view.findViewById(R.id.editText_add_folder);
		final TextView text = (TextView) view.findViewById(R.id.textView_entry);
		String fileName = originalFile.getName();
		edit.setText(fileName);
		// if it is directory, highlight the whole file name, otherwise hight
		// light only the file name,
		// not the extension
		int lastDotIndex = !originalFile.isDirectory() ? fileName.lastIndexOf('.') : -1;
		int length = (lastDotIndex > 0) ? lastDotIndex : fileName.length();
		edit.setSelection(0, length);
		edit.addTextChangedListener(new CheckFileExistListener(text));

		text.setText(R.string.new_name);
		builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				renameFile(edit, originalFile);
			}
		}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		Dialog dialog = builder.create();
		edit.setOnEditorActionListener(new RenameDoneListener(originalFile, dialog));
		dialog.show();
		showSoftKeyboardForDialog(dialog);
	}

	private void copyMoveUnzipFile(FileModel model) {
		mFileToCopy = model;
		switch (mFileToCopy.code()) {
		case FileModel.TO_COPY:
		case FileModel.TO_MOVE:
			mButtonPaste.setText(R.string.paste_here);
			break;
		case FileModel.TO_UNZIP:
			mButtonPaste.setText(R.string.extract_here);
		}
		showBottomBar();
	}

	// //// display file/folder info
	private class DisplayDetailTask extends AsyncTask<File, Boolean, Boolean> {

		private String fileName = "";
		private String lastModiDate = "";
		private String size = "";
		private String folderNum = "0";
		private String fileNum = "0";
		private String permission = "";
		private boolean isDirectory = false;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showProgress();
		}

		@Override
		protected Boolean doInBackground(File... params) {
			File file = params[0];
			if (file == null)
				return false;
			fileName = file.getName();
			lastModiDate = (String) DateFormat.format("MM/dd/yyyy kk:mm", file.lastModified());
			size = FileUtils.toDetail(file);
			permission = FileUtils.getFilePermission(file);

			if (file.isDirectory()) {
				isDirectory = true;
				Long[] num = FileUtils.getFolderContents(file);
				folderNum = Long.toString(num[0]);
				fileNum = Long.toString(num[1]);
			} else {
				isDirectory = false;
			}
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			dismissProgress();
			if (result) {
				showDetailDialog(fileName, lastModiDate, size, permission, folderNum, fileNum, isDirectory);
			}
		}
	}

	private void showDetailDialog(String fileName, String lastModiDate, String size, String permission,
			String folderNum, String fileNum, boolean isDirectory) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = LayoutInflater.from(this);
		View v2 = inflater.inflate(R.layout.detail, null);
		final TextView textName = (TextView) v2.findViewById(R.id.textView_name);
		final TextView textLMD = (TextView) v2.findViewById(R.id.textView_last_modi);
		final TextView textSize = (TextView) v2.findViewById(R.id.textView_size);
		final TextView textFolder = (TextView) v2.findViewById(R.id.textView_folder);
		final TextView textContent = (TextView) v2.findViewById(R.id.textView_contents);
		final TextView textPerm = (TextView) v2.findViewById(R.id.textView_permission);

		textName.setText(fileName);
		textLMD.setText(lastModiDate);
		textSize.setText(size);
		textPerm.setText(permission);
		if (isDirectory) {
			textContent.setText("Folder " + folderNum + ", Files " + fileNum);
		} else {
			textFolder.setVisibility(View.GONE);
			textContent.setVisibility(View.GONE);
		}

		builder.setTitle(R.string.detail).setView(v2).setPositiveButton(R.string.ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).create().show();
	}

	// ///////////////////////////////////////////////////
	// Share
	// ///////////////////////////////////////////////////

	private void share(File file) {
		Intent mailIntent = new Intent();
		mailIntent.setAction(android.content.Intent.ACTION_SEND);
		mailIntent.setType("application/mail");
		mailIntent.putExtra(Intent.EXTRA_BCC, "");
		mailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
		startActivity(mailIntent);
	}

	// ///////////////////////////////////////////////////
	// Search
	// ///////////////////////////////////////////////////

	private Dialog createSearchDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflator2 = LayoutInflater.from(this);
		View view2 = inflator2.inflate(R.layout.search, null);
		builder.setTitle(R.string.search).setView(view2);
		final EditText edit2 = (EditText) view2.findViewById(R.id.editText_search);
		final CheckBox subDir = (CheckBox) view2.findViewById(R.id.checkBox_sub);
		edit2.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					hideSoftKeyboard(edit2);
					if (search(edit2, subDir)) {
						try {
							dismissDialog(DIALOG_SEARCH);
						} catch (IllegalArgumentException e) {

						}
					}
					return true;
				}
				return false;
			}
		});

		subDir.setChecked(true);
		return builder.setPositiveButton(R.string.search, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				search(edit2, subDir);
			}
		}).setNegativeButton(R.string.cancel, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				edit2.setText("");
				dialog.cancel();
			}
		}).create();
	}

	private boolean search(EditText edit, CheckBox subDir) {
		String query = edit.getText().toString();
		if (query.length() > 0) {
			File dir = subDir.isChecked() ? mCurrentDir : App.instance().deviceInfo().homeDirectory();
			SearchFileTask search = new SearchFileTask(FileBrowserActivity.this, dir);
			search.setOnAfterSearchFinishedListener(new OnSearchFinishedListener());
			search.execute(query);
			edit.setSelection(0, query.length());
			return true;
		}

		return false;
	}

	private class OnSearchFinishedListener implements OnAfterSearchFinishedListener {

		@Override
		public void onAfterSearchFinished(List<File> result, String searchQuery) {
			displaySearchResults(result, searchQuery);
		}

	}

	/**
	 * Display search results in a dialog
	 * 
	 * @param files
	 */
	private void displaySearchResults(final List<File> files, String query) {
		query = " \"" + query + "\"";
		if (files.isEmpty()) {
			Toast.makeText(FileBrowserActivity.this, getString(R.string.no_file_found) + query,
					Toast.LENGTH_LONG).show();
			return;
		}
		SearchResultAdapter adapter = new SearchResultAdapter(this, files);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String title = getString(R.string.search_result) + query;
		builder.setTitle(title);
		builder.setSingleChoiceItems(adapter, -1, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				openFile(files.get(which));
				dialog.dismiss();
			}
		}).setNegativeButton(R.string.cancel, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		}).create().show();
	}

	// //////// zip file
	private void showBeforeZipDialog(final File file) {

		final String filename = createZipFilename(file);

		File zipFile = new File(mCurrentDir, filename);
		if (!zipFile.exists()) {
			zipFile(filename, file);
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String msg = file.getName() + " " + getString(R.string.file_exist_msg);
		builder.setTitle(R.string.zip_file_exist_warning).setIcon(R.drawable.warning).setMessage(msg)
				.setPositiveButton(R.string.overwrite, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						zipFile(filename, file);
					}
				}).setNegativeButton(R.string.cancel, new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				}).create().show();

	}

	private void zipFile(final String dstFilename, final File file) {
		ZipFilesTask zip = new ZipFilesTask(FileBrowserActivity.this, mCurrentDir, dstFilename);
		zip.setOnAfterFilesZippedListener(new RefreshFileListener());
		zip.execute(file);
	}

	private class RefreshFileListener implements OnAfterFilesZippedListener {
		@Override
		public void onAfterFilesZipped() {
			refreshList(false);
		}

	}

	private String createZipFilename(File toZip) {
		String filename = toZip.getName();
		if (toZip.isFile()) {
			int dotPos = filename.lastIndexOf('.');
			if (dotPos > 0) {
				filename = filename.substring(0, dotPos);
			}
		}

		return filename + ".zip";
	}

	private void showBottomBar() {
		float height = mLayoutPaste.getHeight();
		mLayoutPaste.setVisibility(View.VISIBLE);
		TranslateAnimation slide = new TranslateAnimation(0, 0, height, 0);
		slide.setDuration(400);
		slide.setFillAfter(true);
		mLayoutPaste.startAnimation(slide);
	}

	private void hideBottomBar(boolean animate) {
		if (animate) {
			float height = mLayoutPaste.getHeight();
			TranslateAnimation slide = new TranslateAnimation(0, 0, 0, height);
			slide.setDuration(400);
			mLayoutPaste.startAnimation(slide);
		}
		mLayoutPaste.setVisibility(View.GONE);
	}

	public Handler getHandler() {
		return mHandler;
	}

	// //////////////////////////////////////////////////////////////////////
	// Rename files
	// //////////////////////////////////////////////////////////////////////

	private boolean renameFile(TextView tv, File fileToRename) {
		String newfilename = tv.getText().toString();
		File newFile = createFileForCurrentDir(newfilename);
		if (!newFile.exists()) {
			fileToRename.renameTo(newFile);
			refreshList(false);
			App.instance().setRefreshCache(true);
			return true;
		}
		Toast.makeText(App.instance(), "File exists", Toast.LENGTH_SHORT).show();
		return false;

	}

	private class RenameDoneListener implements EditText.OnEditorActionListener {

		private File fileToRename;
		private Dialog dialog;

		public RenameDoneListener(File fileToRename, Dialog dialog) {
			this.fileToRename = fileToRename;
			this.dialog = dialog;
		}

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				if (renameFile(v, fileToRename)) {
					dialog.cancel();
					return true;
				}
			}
			return false;
		}
	}

	// //////////////////////////////////////////////////////////////////////
	// Inner classes
	// //////////////////////////////////////////////////////////////////////

	private class OnDirectoryNavigationItemSelectionListener implements ActionBar.OnNavigationListener {

		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			Log.d(TAG, "Select item " + itemPosition);
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i <= itemPosition; ++i) {
				builder.append(mDirStack.get(i));
			}
			File file = new File(builder.toString());
			if (mCurrentDir.compareTo(file) != 0) {
				mCurrentDir = file;
				refreshList(false);
			}
			return true;
		}

	}

	private class OnBackButtonPress implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			backPress();
		}
	}

	private void goHome() {
		if (isHomeDirectory(mCurrentDir)) {
			Toast.makeText(FileBrowserActivity.this, R.string.already_home, Toast.LENGTH_SHORT).show();
			return;
		}
		mCurrentDir = App.instance().deviceInfo().homeDirectory();
		refreshList(false);
	}

	private class OnHomeButtonClick implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			goHome();
		}
	}

	private class OnPasteButtonClick implements View.OnClickListener {
		@Override
		public void onClick(View v) {

			File file = mFileToCopy.file();
			if (file == null) {
				Toast.makeText(App.instance(), "Empty file", Toast.LENGTH_SHORT).show();
				return;
			}

			switch (mFileToCopy.code()) {
			case FileModel.TO_COPY:
				if (!mCurrentDir.canWrite()) {
					Toast.makeText(App.instance(), "Permission denial.", Toast.LENGTH_SHORT).show();
					return;
				}

				if (FileUtils.checkIfParentFile(mCurrentDir, file)) {
					Toast.makeText(FileBrowserActivity.this, R.string.cannot_cpy_to_child, Toast.LENGTH_LONG)
							.show();
					return;
				}

				CopyThread copy1 = new CopyThread(FileBrowserActivity.this, mHandler, CopyThread.TO_COPY);

				File[] files1 = new File[] { file };
				copy1.start(mCurrentDir, files1);
				break;
			case FileModel.TO_MOVE:
				if (!mCurrentDir.canWrite()) {
					Toast.makeText(App.instance(), "Permission denial.", Toast.LENGTH_SHORT).show();
					return;
				}

				if (sameDirectory(file, mCurrentDir)) {
					Toast.makeText(App.instance(), "Same directory", Toast.LENGTH_LONG).show();
					return;
				}

				if (FileUtils.checkIfParentFile(mCurrentDir, file)) {
					Toast.makeText(FileBrowserActivity.this, R.string.cannot_cpy_to_child, Toast.LENGTH_LONG)
							.show();
					return;
				}
				CopyThread move = new CopyThread(FileBrowserActivity.this, mHandler, CopyThread.TO_MOVE);
				File[] files2 = new File[] { file };
				move.start(mCurrentDir, files2);
				break;
			case FileModel.TO_UNZIP:
				UnzipThread unzip = new UnzipThread(FileBrowserActivity.this, mHandler);
				unzip.start(mCurrentDir, file);

			}

			hideBottomBar(true);
		}
	}

	private class OnCancelButtonClick implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			hideBottomBar(true);
		}
	}

	// ////////////////////////////////////////
	// Dialogs
	// ////////////////////////////////////////

	private void showUnzipOptionsDialog(final File zipFile) {
		String[] options = new String[] { getString(R.string.extract_here), getString(R.string.extract_to) };
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.choose).setItems(options, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					new UnzipThread(FileBrowserActivity.this, mHandler).start(mCurrentDir, zipFile);
				} else {
					FileModel model = new FileModel(FileModel.TO_UNZIP, zipFile);
					copyMoveUnzipFile(model);
				}
				dialog.cancel();
			}
		}).show();
	}

}