package com.wenhui.filebrowser.adapters;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.wenhui.filebrowser.R;
import com.wenhui.filebrowser.model.AppToSendOption;

public class SendOptionsAdapter extends ArrayAdapter< AppToSendOption > {
	private List< AppToSendOption > mApps;
	private LayoutInflater mInflater;
	private static final int RESOURCE = R.layout.send_option_dialog;

	class ViewHolder {
		TextView text;
		ImageView icon;
	}

	public SendOptionsAdapter( Context context, List< AppToSendOption > objects ) {
		super( context, RESOURCE, objects );
		mInflater = LayoutInflater.from( context );
		mApps = objects;
	}

	@Override
	public View getView( int position, View convertView, ViewGroup parent ) {
		ViewHolder holder;
		if ( convertView == null ) {
			holder = new ViewHolder();
			convertView = mInflater.inflate( RESOURCE, null );
			holder.text = ( TextView ) convertView.findViewById( R.id.textView_appname );
			holder.text.setTextColor( Color.BLACK );
			holder.icon = ( ImageView ) convertView.findViewById( R.id.imageView_appicon );
			holder.icon.setAdjustViewBounds( true );
			holder.icon.setScaleType( ScaleType.CENTER_INSIDE );
			convertView.setTag( holder );
		} else {
			holder = ( ViewHolder ) convertView.getTag();
		}
		holder.icon.setImageDrawable( mApps.get( position ).getIcon() );
		holder.text.setText( mApps.get( position ).getAppname() );

		return convertView;
	}
	
}
