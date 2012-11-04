package com.example.intentfiletersample;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

public class CustomImageButton extends ImageButton {

	@Override
	protected void drawableStateChanged() {
		Log.d("Button", "isPressed: " + isPressed() );
		if( isPressed() ){
			setBackgroundResource( android.R.color.holo_blue_dark );
		} else {
			setBackgroundResource( android.R.color.transparent );
		}
		super.drawableStateChanged();
		
	}

	public CustomImageButton( Context context ) {
		super( context );
	}

	public CustomImageButton( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	public CustomImageButton( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
		// TODO Auto-generated constructor stub
	}

	
	private class AdapterExample extends CursorAdapter {

		public AdapterExample( Context context, Cursor c, int flags ) {
			super( context, c, flags );
		}

		@Override
		public void bindView( View arg0, Context arg1, Cursor arg2 ) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public View newView( Context arg0, Cursor arg1, ViewGroup arg2 ) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	
}
