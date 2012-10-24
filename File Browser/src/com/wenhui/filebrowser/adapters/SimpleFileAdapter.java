package com.wenhui.filebrowser.adapters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.ImageView.ScaleType;

import com.wenhui.filebrowser.App;
import com.wenhui.filebrowser.R;
import com.wenhui.filebrowser.generic.FileUtils;
import com.wenhui.filebrowser.model.ListItemsContainer;
import com.wenhui.filebrowser.thread.ImageLoader;

public class SimpleFileAdapter extends BaseAdapter {

	static class ViewHolder {
		LinearLayout layout;
		ImageView icon;
		ImageView playOverlay;
		TextView text;
		TextView fileDetail;
	}

	private List< ListItemsContainer > mDir;
	private LayoutInflater mInflater;
	private ImageLoader mImageLoader;
	
	public SimpleFileAdapter( Context context, List< ListItemsContainer > files ) {
		mInflater = LayoutInflater.from( context );
		this.mDir = files;
		mImageLoader = new ImageLoader( context, ImageLoader.IMAGE_MAX_SIZE );
	}

	public void setData( List<ListItemsContainer> files){
		this.mDir = files;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return mDir.size();
	}

	@Override
	public ListItemsContainer getItem( int position ) {
		return mDir.get( position );
	}

	@Override
	public long getItemId( int position ) {
		return position;
	}

	private int getLayoutResId() {
		switch ( App.instance().getListOption() ) {
		case App.LIST_VIEW:
		default:
			return R.layout.file_list_item;
		case App.GRID_VIEW:
			return R.layout.file_grid_item;
		case App.LIST_WITH_DETAIL:
			return R.layout.file_list_with_detail_item;
		}
	}

	@Override
	public View getView( int position, View convertView, ViewGroup parent ) {
		ViewHolder holder = null;

		if ( convertView == null ) {
			int resId = getLayoutResId();
			convertView = mInflater.inflate( resId, null );
			holder = new ViewHolder();
			holder.layout = ( LinearLayout ) convertView.findViewById( R.id.layout_list );
			holder.text = ( TextView ) convertView.findViewById( R.id.textView_file );
			holder.icon = ( ImageView ) convertView.findViewById( R.id.imageView_file );
			holder.icon.setAdjustViewBounds( true );
			holder.icon.setScaleType( ScaleType.CENTER_CROP );
			holder.playOverlay = ( ImageView ) convertView.findViewById( R.id.imageView_play_overlay );
			holder.fileDetail = ( TextView ) convertView.findViewById( R.id.textView_detail );
			convertView.setTag( holder );
		} else {
			holder = ( ViewHolder ) convertView.getTag();
		}

		ListItemsContainer item = mDir.get( position );
		File file = item.getFile();

		if ( file.isDirectory() ) {
			holder.icon.setImageResource( R.drawable.folder );
		} else if ( FileUtils.hasImage( file ) ) {
			mImageLoader.loadImage( file, holder.icon );
		} else {
			holder.icon.setImageResource( item.getResId() );
		}

		String ext = item.getFileExtension();
		if ( !FileUtils.isVideoFile( ext ) ) {
			holder.playOverlay.setVisibility( View.GONE );
		} else {
			holder.playOverlay.setVisibility( View.VISIBLE );
		}

		holder.text.setText( file.getName() );
		
		if( isListWithDetail() ){
			holder.fileDetail.setVisibility( View.VISIBLE );
			holder.fileDetail.setText( getFileDetailInfo( file ) );
		}
		
		if( App.instance().isMultiSelectMode() ){
			setChecked( holder.layout, item.getIsChecked() );
		}
		return convertView;
	}

	private boolean isListWithDetail() {
		return App.instance().getListOption() == App.LIST_WITH_DETAIL;
	}

	private String getFileDetailInfo( File file ) {
		String lastModiDate = ( String ) DateFormat.format( "MM/dd/yyyy kk:mm", file.lastModified() );
		if( file.isDirectory() ){
			return folderSize(file) +  " "+ App.instance().getString(R.string.items) +" | " + lastModiDate;
		} else {
			return FileUtils.toDetail(file) + " | "+lastModiDate;
		}
	}
	
	private String folderSize(File dir){
		try{
			String[] files = dir.list();
			return Integer.toString(files.length);
		}catch(Exception e){
			return "";
		}
	}
	
	public ArrayList< File > getCheckecItems() {
		ArrayList< File > checkedFiles = new ArrayList< File >();
		for ( ListItemsContainer fi : mDir ) {
			if ( fi.getIsChecked() ) {
				checkedFiles.add( fi.getFile() );
			}
		}
		return checkedFiles;
	}

	/**
	 * Change layout background color depend on if it is listView or gridView.
	 * @param layout
	 * @param isChecked
	 */
	public void setChecked( View layout, boolean isChecked ){
		if(isChecked){
			layout.setBackgroundResource(R.color.dark_gray);
		} else {
			int resId = ( App.instance().isListMode() ) ? android.R.color.transparent : R.drawable.border;
			layout.setBackgroundResource( resId );
		}
	}
}
