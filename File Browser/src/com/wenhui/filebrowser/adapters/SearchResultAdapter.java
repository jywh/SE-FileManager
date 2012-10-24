package com.wenhui.filebrowser.adapters;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wenhui.filebrowser.R;
import com.wenhui.filebrowser.generic.FileUtils;
import com.wenhui.filebrowser.generic.IconPicker;
import com.wenhui.filebrowser.thread.ImageLoader;

public class SearchResultAdapter extends ArrayAdapter< File > {

	private static final int RESOURCE = R.layout.dialog_panel;
	
	private List< File > mAllFiles;
	private LayoutInflater mInflater;
	private ImageLoader mImageLoader;

	static class ViewHolder {
		TextView text;
		ImageView icon;
	}

	public SearchResultAdapter( Context context, List< File > objects ) {
		super( context, RESOURCE, objects );
		mInflater = LayoutInflater.from( context );
		mAllFiles = objects;
		mImageLoader = new ImageLoader( context, ImageLoader.IMAGE_MAX_SIZE );
	}

	@Override
	public View getView( int position, View convertView, ViewGroup parent ) {
		ViewHolder holder;
		if ( convertView == null ) {
			holder = new ViewHolder();
			convertView = mInflater.inflate( RESOURCE, null );
			holder.text = ( TextView ) convertView.findViewById( R.id.textView_dialog_file );
			holder.icon = ( ImageView ) convertView.findViewById( R.id.imageView_dialog_file );
			holder.icon.setAdjustViewBounds( true );
			convertView.setTag( holder );
		} else {
			holder = ( ViewHolder ) convertView.getTag();
		}

		File file = mAllFiles.get( position );
		String filename = file.getPath();
		if ( filename != null ) {
			holder.text.setText( filename );
			if ( file.isDirectory() ) {
				holder.icon.setImageResource( R.drawable.folder );
			} else if ( FileUtils.hasImage( file ) ) {
				mImageLoader.loadImage( file, holder.icon );
			} else {
				String ext = FileUtils.getFileExtension( file );
				holder.icon.setImageResource( IconPicker.getIcon( ext ) );
			}
		}

		return convertView;
	}
}
