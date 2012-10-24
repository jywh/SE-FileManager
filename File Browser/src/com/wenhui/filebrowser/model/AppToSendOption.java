package com.wenhui.filebrowser.model;

import android.graphics.drawable.Drawable;

public class AppToSendOption {

	Drawable icon;
	String appname;
	String packagename;
	String classname;

	public AppToSendOption( Drawable icon, String appname, String packagename, String classname ) {
		this.icon = icon;
		this.appname = appname;
		this.packagename = packagename;
		this.classname = classname;
	}

	public Drawable getIcon() {
		return icon;
	}

	public String getAppname() {
		return appname;
	}

	public String getPackagename() {
		return packagename;
	}

	public String getClassname() {
		return classname;
	}
}
