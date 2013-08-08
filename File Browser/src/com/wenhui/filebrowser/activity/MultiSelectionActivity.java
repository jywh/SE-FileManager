package com.wenhui.filebrowser.activity;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Toast;

import com.wenhui.filebrowser.App;
import com.wenhui.filebrowser.DeviceInfo;
import com.wenhui.filebrowser.R;
import com.wenhui.filebrowser.generic.FileUtils;
import com.wenhui.filebrowser.model.ListItemsContainer;
import com.wenhui.filebrowser.thread.CopyThread;
import com.wenhui.filebrowser.thread.DeleteThread;

public class MultiSelectionActivity extends RootActivity {

	static final String TAG = "MultiSelectionActivity";

	private int mAction = -1;
	private Button mButtonCopy; // also cancel button
	private Button mButtonMove; // also create button
	private Button mButtonDelete; // also paste button

	private MenuItem mMenuItem;
	private boolean mSelectMode = true;
	private ArrayList<File> mCheckedItems = new ArrayList<File>();
	private boolean mAllSelected = false;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MESSAGE_REFRESH_LIST) {
				finish();
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Retrieve row id for database if it is on edit mode
		mCurrentDir = App.instance().getCurrentPath();

		if (!mCurrentDir.isDirectory()) {
			finish();
			return;
		}
		initView();

	}

	@Override
	protected int getResId() {
		return R.layout.multi_file_browser;
	}

	@Override
	protected void initView() {
		super.initView();

		// Show action bar
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setTitle(R.string.multi_selection);
		
		mButtonCopy = (Button) findViewById(R.id.button_back);
		mButtonMove = (Button) findViewById(R.id.button_home);
		mButtonDelete = (Button) findViewById(R.id.button_search);
		setTextAndDrawable(mButtonCopy, R.drawable.ic_menu_copy, R.string.copy);
		setTextAndDrawable(mButtonMove, R.drawable.ic_menu_cut, R.string.cut);
		setTextAndDrawable(mButtonDelete, R.drawable.ic_menu_delete, R.string.delete);

		mButtonCopy.setOnClickListener(new OnButtonCopyClick());
		mButtonMove.setOnClickListener(new OnButtonMoveClick());
		mButtonDelete.setOnClickListener(new OnButtonDeleteClick());
	}

	private void setTextAndDrawable(Button button, int resId, int textId) {
		button.setText(textId);
		button.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0);
	}

	@Override
	protected void onResume() {
		super.onResume();
		App.instance().setMultiSelectMode(true);
		refreshList(true);
	}

	private boolean isArrayEmpty() {
		mCheckedItems.clear();
		mCheckedItems = mAdapter.getCheckecItems();
		return mCheckedItems.isEmpty();
	}

	@Override
	protected void onListItemClick(AdapterView<?> arg0, View v, int position, long id) {
		if (mSelectMode) {
			boolean b = !mListItems.get(position).getIsChecked();
			mListItems.get(position).setIsChecked(b);
			View layout = v.findViewById(R.id.layout_list);
			mAdapter.setChecked(layout, b);

		} else {
			File file = mListItems.get(position).getFile();
			if (file.isDirectory()) {
				if (file.canRead()) {
					mCurrentDir = file;
					refreshList(false);
				} else {
					Toast.makeText(MultiSelectionActivity.this, R.string.permission_denial, Toast.LENGTH_LONG)
							.show();
				}
			}
		}
	}

	@Override
	public void finish() {
		super.finish();
	}

	/**
	 * The list is refreshed after user press copy or move button
	 */
	private void refreshListAfterCopyMoveButtonClick() {
		for (ListItemsContainer fi : mListItems) {
			fi.setIsChecked(false);
		}
		mSelectMode = false;
		// back button
		setTextAndDrawable(mButtonCopy, R.drawable.ic_menu_revert, R.string.back);
		// cancel button
		setTextAndDrawable(mButtonMove, R.drawable.ic_menu_close_clear_cancel, R.string.cancel);
		// paste button
		setTextAndDrawable(mButtonDelete, R.drawable.ic_menu_paste, R.string.paste_here);
		mAdapter.notifyDataSetChanged();
		if (DeviceInfo.hasHoneycomb()) {
			resetMenuItem(mMenuItem);
		}
	}

	@Override
	public void onBackPressed() {
		if (mSelectMode) {
			finish();
		} else {
			backOneLevel();
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (!DeviceInfo.hasHoneycomb()) {
			MenuItem item = menu.getItem(0);
			resetMenuItem(item);
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.multi_select, menu);
		mMenuItem = menu.findItem(R.id.add_folder);
		int resId = (DeviceInfo.hasHoneycomb()) ? R.drawable.ic_menu_selectall_holo_light
				: R.drawable.ic_menu_selectall_holo_dark;
		mMenuItem.setIcon(resId);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		case R.id.add_folder:
			if (mSelectMode) {
				mAllSelected = !mAllSelected;
				markAll(mAllSelected);
			} else {
				showAddFolderDialog();
			}
			if (DeviceInfo.hasHoneycomb()) {
				resetMenuItem(mMenuItem);
			}
			break;
		}
		return true;
	}

	private void resetMenuItem(MenuItem item) {

		if (item == null) {
			return;
		}

		if (mSelectMode) {
			if (!mAllSelected) {
				item.setTitle(R.string.select_all);
			} else {
				item.setTitle(R.string.deselect_all);
			}
			int resId = (DeviceInfo.hasHoneycomb()) ? R.drawable.ic_menu_selectall_holo_light
					: R.drawable.ic_menu_selectall_holo_dark;
			item.setIcon(resId);
		} else {
			item.setTitle(R.string.add_folder);
			int resId = (DeviceInfo.hasHoneycomb()) ? R.drawable.ic_action_add_folder
					: R.drawable.ic_action_add_folder_holo_dark;
			item.setIcon(resId);
		}
	}

	private void markAll(boolean selected) {
		for (ListItemsContainer item : mListItems) {
			item.setIsChecked(selected);
		}
		mAdapter.notifyDataSetChanged();
	}

	private void backOneLevel() {
		if (!isHomeDirectory(mCurrentDir)) {
			mCurrentDir = mCurrentDir.getParentFile();
			refreshList(false);
		} else {
			Toast.makeText(this, R.string.already_home, Toast.LENGTH_SHORT).show();
		}
	}

	private String createMsg(ArrayList<File> files) {
		StringBuilder str = new StringBuilder();
		for (File f : files) {
			str.append("* ").append(f.getName()).append("\n");
		}
		return str.toString();
	}

	/**
	 * check if the toCopy files have the parent directory of the curDir
	 * 
	 * @return false if it has no
	 */
	private boolean checkIsParentDir() {
		for (File file : mCheckedItems) {
			if (FileUtils.checkIfParentFile(mCurrentDir, file)) {
				Toast.makeText(this, R.string.cannot_cpy_to_child, Toast.LENGTH_LONG).show();
				return true;
			}
		}
		return false;
	}

	private void copyMoveFile() {
		if (!checkIsParentDir()) {
			File[] files = mCheckedItems.toArray(new File[mCheckedItems.size()]);
			new CopyThread(this, mHandler, mAction).start(mCurrentDir, files);
		}
	}

	private void showDeleteFileDialog(String confirmMsg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.confirm)).setMessage(confirmMsg)
				.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						File[] files = mCheckedItems.toArray(new File[mCheckedItems.size()]);
						new DeleteThread(MultiSelectionActivity.this, mHandler).start(files);
					}
				}).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();
	}

	// ////////////////////////////////////////////////////
	// Inner classes
	// ////////////////////////////////////////////////////

	private class OnButtonCopyClick implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mSelectMode) {
				if (isArrayEmpty()) {
					Toast.makeText(App.instance(), R.string.empty_selection, Toast.LENGTH_SHORT).show();
					return;
				}
				mAction = CopyThread.TO_COPY;
				refreshListAfterCopyMoveButtonClick();
			} else {
				// this use as back button
				backOneLevel();

			}
		}
	}

	private class OnButtonDeleteClick implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mSelectMode) {
				if (isArrayEmpty()) {
					Toast.makeText(App.instance(), R.string.empty_selection, Toast.LENGTH_SHORT).show();
					return;
				}
				String confirmMsg = getString(R.string.delete_message) + createMsg(mCheckedItems);
				showDeleteFileDialog(confirmMsg);
			} else {
				// this use as paste button
				copyMoveFile();
			}

		}
	}

	private class OnButtonMoveClick implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mSelectMode) {
				if (isArrayEmpty()) {
					Toast.makeText(App.instance(), R.string.empty_selection, Toast.LENGTH_SHORT).show();
					return;
				}
				mAction = CopyThread.TO_MOVE;
				refreshListAfterCopyMoveButtonClick();
			} else {// this work as create cancel button
				finish();
			}
		}
	}

}
