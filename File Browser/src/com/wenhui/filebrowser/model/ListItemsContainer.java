package com.wenhui.filebrowser.model;

import java.io.File;

public class ListItemsContainer {

	private File file;
	private boolean isChecked;
	private int resId;
	private String ext;
	
	public ListItemsContainer(File file, String ext, int resId ){
		this.file=file;
		this.isChecked=false;
		this.ext = ext;
		this.resId = resId;
	}
	
	public int getResId(){
		return this.resId;
	}
	
	public String getFileExtension() {
		return this.ext;
	}
	
	public void setFile(File file){
		this.file = file;
	}
	
	public void setIsChecked(boolean isChecked){
		this.isChecked = isChecked;
	}
	
	
	public File getFile(){
		return file;
	}
	
	public boolean getIsChecked(){
		return isChecked;
	}
}
