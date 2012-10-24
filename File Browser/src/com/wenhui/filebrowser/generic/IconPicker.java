package com.wenhui.filebrowser.generic;

import java.util.HashMap;

import com.wenhui.filebrowser.DeviceInfo;
import com.wenhui.filebrowser.R;

public class IconPicker {

	private final static HashMap< String, Integer > iconPick = new HashMap< String, Integer >( 30, 0.9f );

	public static final int PPT = R.drawable.ppt;
	public static final int PDF = R.drawable.pdf;
	public static final int WORD = R.drawable.doc;
	public static final int XLS = R.drawable.xls;
	public static final int TXT = R.drawable.txt;
	public static final int AUDIO = R.drawable.audio;
	public static final int VIDEO = R.drawable.video;
	public static final int APK = R.drawable.appicon;
	public static final int IMG = R.drawable.image;
	public static final int UNKNOWN = R.drawable.unknown;
	public static final int FOLDER = R.drawable.folder;
	public static final int ZIP = R.drawable.zip;
	public static final int ARCHIVE = R.drawable.rar;
	public static final int FONT = R.drawable.font;
	public static final int PSD = R.drawable.psd;
	public static final int ODS = R.drawable.ods;
	public static final int ODT = R.drawable.odt;
	public static final int VIDEO_HOLDER = R.drawable.empty_photo;

	public static void init() {

		iconPick.put( ".psd", PSD );

		iconPick.put( ".ppt", PPT );
		iconPick.put( ".pptx", PPT );

		iconPick.put( ".doc", WORD );
		iconPick.put( ".docx", WORD );

		iconPick.put( ".xls", XLS );
		iconPick.put( ".xlsx", XLS );

		iconPick.put( ".ods", ODS );
		iconPick.put( ".odt", ODT );

		iconPick.put( ".mp3", AUDIO );
		iconPick.put( ".wma", AUDIO );
		iconPick.put( ".aif", AUDIO );
		iconPick.put( ".m4a", AUDIO );
		iconPick.put( ".m4p", AUDIO );
		iconPick.put( ".mid", AUDIO );
		iconPick.put( ".midi", AUDIO );
		iconPick.put( ".aac", AUDIO );
		iconPick.put( ".ogg", AUDIO );
		iconPick.put( ".wav", AUDIO );

		// The video file formats that will replace with thumbnail
		iconPick.put( ".mp4", VIDEO_HOLDER );
		iconPick.put( ".3gp", VIDEO_HOLDER );
		iconPick.put( ".webm", VIDEO_HOLDER );
		
		iconPick.put( ".flv", VIDEO );
		iconPick.put( ".rmvb", VIDEO );
		iconPick.put( ".rm", VIDEO );
		iconPick.put( ".wmv", VIDEO );
		iconPick.put( ".avi", VIDEO );
		iconPick.put( ".mov", VIDEO );
		iconPick.put( ".mpg", VIDEO );

		iconPick.put( ".bmp", IMG );
		iconPick.put( ".gif", IMG );
		iconPick.put( ".jpg", IMG );
		iconPick.put( ".png", IMG );
		iconPick.put( ".jpeg", IMG );
		iconPick.put( ".tiff", IMG );

		iconPick.put( ".apk", APK );

		iconPick.put( ".zip", ZIP );
		iconPick.put( ".gzip", ZIP );

		iconPick.put( ".jar", ARCHIVE );
		iconPick.put( ".7z", ARCHIVE );
		iconPick.put( ".rar", ARCHIVE );
		iconPick.put( ".gz", ARCHIVE );
		iconPick.put( ".deb", ARCHIVE );

		iconPick.put( ".txt", TXT );

		iconPick.put( ".pdf", PDF );

		iconPick.put( ".otf", FONT );
		iconPick.put( ".ttf", FONT );
		
		if( DeviceInfo.hasIceCreamSandwish() ){
			iconPick.put( ".webp", IMG );
			iconPick.put( ".mkv", VIDEO_HOLDER );
		}
	}

	public static int getIcon( String ext ) {
		Integer icon = iconPick.get( ext );
		if ( icon != null ) {
			return icon;
		} else {
			return UNKNOWN;
		}
	}

}
