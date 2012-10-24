package com.wenhui.filebrowser.model;

import java.io.File;

/**
 * Hold the file to be move, copy or unzip
 * 
 */
public class FileModel {

	public static final int TO_MOVE  = 0;
	public static final int TO_COPY  = 1;
	public static final int TO_UNZIP = 2;
	
	private int mCode;
	private File mFile;
	
	public FileModel( int code, File file ){
		this.mCode = code;
		this.mFile = file;
	}
	
	public int code(){
		return mCode;
	}
	
	public File file() {
		return mFile;
	}
	
}
