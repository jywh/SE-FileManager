package com.wenhui.filebrowser.generic;

import java.util.HashMap;

public class MIMEType {

	private MIMEType(){}
	
	private static HashMap<String, String> mimeMap = new HashMap<String, String>(50, 1);
	
	public static void init(){
		mimeMap.put(".ppt", "application/vnd.ms-powerpoint");
		mimeMap.put(".pptx", "application/vnd.ms-powerpoint");

		mimeMap.put(".doc", "application/msword");
		mimeMap.put(".docx", "application/msword");

		mimeMap.put(".xls", "application/vnd.ms-excel");
		mimeMap.put(".xlsx", "application/vnd.ms-excel");

		mimeMap.put(".mp3", "audio/mpeg3");
		mimeMap.put(".wma", "audio/x-ms-wma");
		mimeMap.put(".aif", "audio/x-aiff");
		mimeMap.put(".aiff", "audio/x-aiff");
		mimeMap.put(".mav", "audio/mav");
		mimeMap.put(".mid", "audio/mid");
		mimeMap.put(".midi", "audio/midi");
		mimeMap.put(".aac", "audio/aac");
		mimeMap.put(".ogg", "audio/x-ogg");
		
		mimeMap.put(".flv", "video/x-flv");
		mimeMap.put(".mp4", "video/mpeg");
		mimeMap.put(".rmvb", "application/vnd.rn-realmedia-vbr");
		mimeMap.put(".rm", "video/vnd.rn-realmedia-vbr");
		mimeMap.put(".wmv", "video/x-ms-wmv");
		mimeMap.put(".avi", "video/x-msvideo");
		mimeMap.put(".mov", "video/quicktime");
		mimeMap.put(".mpg", "video/mpeg");

		mimeMap.put(".bmp", "image/bmp");
		mimeMap.put(".gif", "image/gif");
		mimeMap.put(".jpg", "image/jpeg");
		mimeMap.put(".png", "image/x-png");
		mimeMap.put(".jpeg", "image/jpeg");
		mimeMap.put(".tiff", "image/tiff");

		mimeMap.put(".apk","application/vnd.android.package-archive");
		
		mimeMap.put(".gz", "application/x-gzip");
		mimeMap.put(".jar", "application/java-archive");
		
		mimeMap.put(".txt", "text/plain");
		mimeMap.put(".html", "text/html");
		mimeMap.put(".htm", "text/html");
		mimeMap.put(".php", "text/php");
		mimeMap.put(".xml", "text/xml");
		
		mimeMap.put(".pdf", "application/pdf");
	}
	
	public static String get(String extension){
		String mimeType = mimeMap.get(extension);
		if(mimeType == null ){
			mimeType = "*/*";
		}
		return mimeType;
	}
	
	
}
