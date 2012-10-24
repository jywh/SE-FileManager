package com.wenhui.filebrowser.generic;

import java.io.File;
import java.util.HashMap;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import com.wenhui.filebrowser.R;

/**
 * This class handle all the file intents
 * 
 * @author Wenhui
 * 
 */
public class IntentManager {

	Context context;

	public IntentManager( Context context ) {
		this.context = context;
		init();
	}

	public boolean executeIntent( File file ) {
		String ext = FileUtils.getFileExtension( file );
		return getIntent( ext ).execute( file );
	}

	private HashMap< String, IntentsHandle > intentMap = new HashMap< String, IntentsHandle >();

	private void init() {

		intentMap.put( ".ppt", new PptHandle( context ) );
		intentMap.put( ".pptx", new PptHandle( context ) );

		intentMap.put( ".doc", new DocHandle( context ) );
		intentMap.put( ".docx", new DocHandle( context ) );

		intentMap.put( ".xls", new XlsHandle( context ) );
		intentMap.put( ".xlsx", new XlsHandle( context ) );

		intentMap.put( ".mp3", new AudioHandle( context ) );
		intentMap.put( ".wma", new AudioHandle( context ) );
		intentMap.put( ".aif", new AudioHandle( context ) );
		intentMap.put( ".m4a", new AudioHandle( context ) );
		intentMap.put( ".m4p", new AudioHandle( context ) );
		intentMap.put( ".ogg", new AudioHandle( context ) );
		intentMap.put( ".mid", new AudioHandle( context ) );
		intentMap.put( ".midi", new AudioHandle( context ) );
		intentMap.put( ".aac", new AudioHandle( context ) );

		intentMap.put( ".flv", new VideoHandle( context ) );
		intentMap.put( ".mp4", new VideoHandle( context ) );
		intentMap.put( ".rmvb", new VideoHandle( context ) );
		intentMap.put( ".rm", new VideoHandle( context ) );
		intentMap.put( ".wmv", new VideoHandle( context ) );
		intentMap.put( ".avi", new VideoHandle( context ) );
		intentMap.put( ".mov", new VideoHandle( context ) );
		intentMap.put( ".mpg", new VideoHandle( context ) );

		intentMap.put( ".bmp", new ImageHandle( context ) );
		intentMap.put( ".gif", new ImageHandle( context ) );
		intentMap.put( ".jpg", new ImageHandle( context ) );
		intentMap.put( ".png", new ImageHandle( context ) );
		intentMap.put( ".thm", new ImageHandle( context ) );
		intentMap.put( ".jpeg", new ImageHandle( context ) );
		intentMap.put( ".tiff", new ImageHandle( context ) );

		intentMap.put( ".apk", new ApkHandle( context ) );

		intentMap.put( ".jar", new GenericHandle( context ) );
		intentMap.put( ".7z", new GenericHandle( context ) );
		intentMap.put( ".rar", new GenericHandle( context ) );
		intentMap.put( ".gz", new GenericHandle( context ) );
		intentMap.put( ".deb", new GenericHandle( context ) );

		intentMap.put( ".txt", new TxtHandle( context ) );

		intentMap.put( ".pdf", new PdfHandle( context ) );
	}

	public IntentsHandle getIntent( String ext ) {
		IntentsHandle handle = intentMap.get( ext );
		if ( handle != null ) {
			return handle;
		} else {
			return new GenericHandle( context );
		}
	}
}

abstract class IntentsHandle {
	Context context;

	public IntentsHandle( Context context ) {
		this.context = context;
	}

	public abstract boolean execute( final File file );
}

/**
 * Handle .jpeg, .png, .jpg, .tiff, .gif image files
 * 
 * @author Wenhui
 * 
 */
class ImageHandle extends IntentsHandle {

	public ImageHandle( Context context ) {
		super( context );
	}

	@Override
	public boolean execute( final File file ) {
		Intent picIntent = new Intent();
		picIntent.setAction( android.content.Intent.ACTION_VIEW );
		picIntent.setDataAndType( Uri.fromFile( file ), "image/*" );
		try {
			context.startActivity( picIntent );
		} catch ( ActivityNotFoundException e ) {
			return false;
		}
		return true;
	}
}

/**
 * Handle .mp3, .m4a, .m4p, .wma audio files, may be more...
 * 
 * @author Wenhui
 * 
 */
class AudioHandle extends IntentsHandle {

	public AudioHandle( Context context ) {
		super( context );
	}

	@Override
	public boolean execute( final File file ) {
		Intent i = new Intent();
		i.setAction( android.content.Intent.ACTION_VIEW );
		i.setDataAndType( Uri.fromFile( file ), "audio/*" );
		try {
			context.startActivity( i );
		} catch ( ActivityNotFoundException e ) {
			return false;
		}
		return true;
	}
}

/**
 * Handle .mp4, .flv, .rmvb, .rm, .wmv, .avi, .ogg, .mov, .mpg
 * 
 * @author Wenhui
 * 
 */
class VideoHandle extends IntentsHandle {

	public VideoHandle( Context context ) {
		super( context );
	}

	@Override
	public boolean execute( final File file ) {
		Intent movieIntent = new Intent();
		movieIntent.setAction( android.content.Intent.ACTION_VIEW );
		movieIntent.setDataAndType( Uri.fromFile( file ), "video/*" );
		try {
			context.startActivity( movieIntent );
		} catch ( ActivityNotFoundException e ) {
			return false;
		}
		return true;
	}

}

/**
 * Handle .apk file
 * 
 * @author Wenhui
 * 
 */
class ApkHandle extends IntentsHandle {
	public ApkHandle( Context context ) {
		super( context );
	}

	@Override
	public boolean execute( final File file ) {
		Intent apkIntent = new Intent();
		apkIntent.setAction( android.content.Intent.ACTION_VIEW );
		apkIntent.setDataAndType( Uri.fromFile( file ), "application/vnd.android.package-archive" );
		try {
			context.startActivity( apkIntent );
		} catch ( ActivityNotFoundException e ) {
			return false;
		}
		return true;
	}
}

class PdfHandle extends IntentsHandle {

	public PdfHandle( Context context ) {
		super( context );
	}

	@Override
	public boolean execute( final File file ) {
		Intent pdfIntent = new Intent();
		pdfIntent.setAction( android.content.Intent.ACTION_VIEW );
		pdfIntent.setDataAndType( Uri.fromFile( file ), "application/pdf" );

		try {
			context.startActivity( pdfIntent );
		} catch ( ActivityNotFoundException e ) {
			return false;
		}
		return true;
	}
}

/**
 * Handle .html file
 * 
 * @author Wenhui
 * 
 */
class HtmlHandle extends IntentsHandle {

	public HtmlHandle( Context context ) {
		super( context );
	}

	@Override
	public boolean execute( final File file ) {
		Intent htmlIntent = new Intent();
		htmlIntent.setAction( android.content.Intent.ACTION_VIEW );
		htmlIntent.setDataAndType( Uri.fromFile( file ), "text/html" );

		try {
			context.startActivity( htmlIntent );
		} catch ( ActivityNotFoundException e ) {
			return false;
		}
		return true;
	}
}

/**
 * Handle .txt file
 * 
 * @author Wenhui
 * 
 */
class TxtHandle extends IntentsHandle {

	public TxtHandle( Context context ) {
		super( context );
	}

	@Override
	public boolean execute( final File file ) {

		Intent txtIntent = new Intent();
		txtIntent.setAction( android.content.Intent.ACTION_VIEW );
		txtIntent.setDataAndType( Uri.fromFile( file ), "text/plain" );
		try {
			context.startActivity( txtIntent );
		} catch ( ActivityNotFoundException e ) {
			txtIntent.setType( "text/*" );
			context.startActivity( txtIntent );
		} catch ( Exception e ) {
			return false;
		}
		return true;
	}

}

class DocHandle extends IntentsHandle {

	public DocHandle( Context context ) {
		super( context );
	}

	@Override
	public boolean execute( File file ) {
		Intent mswordIntent = new Intent();
		mswordIntent.setAction( android.content.Intent.ACTION_VIEW );
		mswordIntent.setDataAndType( Uri.fromFile( file ), "application/msword" );

		try {
			context.startActivity( mswordIntent );
		} catch ( ActivityNotFoundException e ) {
			return false;
		}
		return true;
	}
}

class PptHandle extends IntentsHandle {

	public PptHandle( Context context ) {
		super( context );
	}

	@Override
	public boolean execute( File file ) {
		Intent pptIntent = new Intent();
		pptIntent.setAction( android.content.Intent.ACTION_VIEW );
		pptIntent.setDataAndType( Uri.fromFile( file ), "application/vnd.ms-powerpoint" );

		try {
			context.startActivity( pptIntent );
		} catch ( ActivityNotFoundException e ) {
			pptIntent.setType( "application/powerpoint" );
			context.startActivity( pptIntent );
		} catch ( Exception e ) {
			return false;
		}
		return true;
	}
}

class XlsHandle extends IntentsHandle {

	public XlsHandle( Context context ) {
		super( context );
	}

	@Override
	public boolean execute( File file ) {
		Intent xlsIntent = new Intent();
		xlsIntent.setAction( android.content.Intent.ACTION_VIEW );
		xlsIntent.setDataAndType( Uri.fromFile( file ), "application/vnd.ms-excel" );

		try {
			context.startActivity( xlsIntent );
		} catch ( ActivityNotFoundException e ) {
			return false;
		}
		return true;

	}

}

/**
 * Handle the unknown file,
 * 
 * @author Wenhui
 * 
 */
class GenericHandle extends IntentsHandle {

	public GenericHandle( Context context ) {
		super( context );
	}

	@Override
	public boolean execute( final File file ) {

		final Intent generic = new Intent();
		generic.setAction( android.content.Intent.ACTION_VIEW );
		AlertDialog.Builder builder = new AlertDialog.Builder( context );
		builder.setTitle( R.string.open_as ).setItems( R.array.unknown_options, new DialogInterface.OnClickListener() {

			@Override
			public void onClick( DialogInterface dialog, int which ) {
				switch ( which ) {
				case 0:
					generic.setDataAndType( Uri.fromFile( file ), "audio/*" );
					break;
				case 1:
					generic.setDataAndType( Uri.fromFile( file ), "video/*" );
					break;
				case 2:
					generic.setDataAndType( Uri.fromFile( file ), "text/plain" );
					break;
				case 3:
					generic.setDataAndType( Uri.fromFile( file ), "image/*" );
					break;
				case 4:
				default:
					generic.setDataAndType( Uri.fromFile( file ), "image/*" );
					generic.setDataAndType( Uri.fromFile( file ), "application/*" );
				}

				try {
					context.startActivity( generic );
				} catch ( ActivityNotFoundException e ) {
				}
			}
		} ).show();

		return true;
	}
}