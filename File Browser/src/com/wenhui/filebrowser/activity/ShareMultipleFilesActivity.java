package com.wenhui.filebrowser.activity;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;

import com.wenhui.filebrowser.App;
import com.wenhui.filebrowser.R;

public class ShareMultipleFilesActivity extends Activity{

	public static final String APP_TO_SEND_PACKAGENAME = "app_to_send_packagname";
	public static final String APP_TO_SEND_CLASSNAME = "app_to_send_classname";
	private String packagename;
	private String classname;
	private String curPath;
	private File curDir;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		packagename = (savedInstanceState != null)? savedInstanceState.getString(APP_TO_SEND_PACKAGENAME):null;
		classname = (savedInstanceState != null)? savedInstanceState.getString(APP_TO_SEND_CLASSNAME):null;

		if(packagename == null || classname == null && getIntent().getExtras() != null){
			Bundle extra = getIntent().getExtras();
			packagename = (extra != null)? extra.getString(APP_TO_SEND_PACKAGENAME):null;
			classname = (extra != null)? extra.getString(APP_TO_SEND_CLASSNAME):null;
		}
		
		if(packagename == null || classname == null){
			finish();
			return;
		}
		
		setContentView(R.layout.share_multiple_files);
		
	}
	
}
