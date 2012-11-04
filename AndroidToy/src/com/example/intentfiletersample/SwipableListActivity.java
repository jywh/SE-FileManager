package com.example.intentfiletersample;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.view.View.MeasureSpec;
import android.view.View.OnTouchListener;
import android.view.animation.*;
import android.view.animation.Animation.AnimationListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

@SuppressLint("NewApi")
public class SwipableListActivity extends Activity implements OnItemClickListener {

	static final String TAG = "MainActivity";

	ListView mList;

	private ArrayList< String > mContent = new ArrayList< String >();

	private OnTouch mTouchListener;
	private ListAdapter mAdapter;

	@Override
	public void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );

		mList = ( ListView ) findViewById( R.id.myList );
		fillListContent();
		mAdapter = new ListAdapter( this );
		mList.setAdapter( mAdapter );
		mList.setOnTouchListener( mTouchListener );
//		 mList.setOnItemClickListener( this );
//		 mList.setOnItemLongClickListener( mLongListener );
		// mList.setOnItemLongClickListener( mLongListener );
	}

	private Animation createFadeInAnim() {
		return AnimationUtils.loadAnimation( this, R.anim.fade );
	}
	
	private void fillListContent() {
		for ( int i = 0; i < 20; ++i ) {
			mContent.add( "Item " + i );
		}
	}

	@Override
	public void onItemClick( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
		mContent.add( arg2+1, "Item add 1" );
		mContent.add( arg2+2, "Item add 2" );
		ArrayList<Integer> changes = new ArrayList<Integer>();
		changes.add( arg2+1 );
		changes.add( arg2+2 );
		mAdapter.setPositionChange( changes ); 
		mAdapter.notifyDataSetChanged();
		// mTouchListener.setPosition( arg2 );
//		Toast.makeText( getApplicationContext(), "Click " + mAdapter.getItem( arg2 ), Toast.LENGTH_SHORT ).show();

	}

	private OnItemLongClickListener mLongListener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick( AdapterView< ? > arg0, View arg1, int arg2, long arg3 ) {
			Animation anim = new CollapseAnimation( arg1, false );
			anim.setAnimationListener( new CollapseAnimationListener( arg2, arg1 ) );
			anim.setDuration( 500l );
			arg1.startAnimation( anim );
			return true;
		}
	};

	private static final float MIN_X_DISTANCE = 10f;
	private static final float REMOVE_DISTANCE = 150f;
	private static final float MIN_Y_DISTANCE = 20f;

	private class OnTouch implements OnTouchListener {

		private float startX = 0f;
		private float startY = 0f;

		private float mDeltaX = 0f;
		private float mDeltaY = 0f;
		private View mCurrentTouchView;
		private int mPosition = -1;

		public void setPosition( int position ) {
			mPosition = position;
		}

		@Override
		public boolean onTouch( View v, MotionEvent event ) {

			switch ( event.getAction() ) {
			case MotionEvent.ACTION_DOWN:
				startX = event.getX();
				Log.d( TAG, "X pos: " + startX );
				startY = event.getY();
				Log.d( TAG, "Y pos: " + startY );
				break;
			case MotionEvent.ACTION_MOVE:
				Log.d( TAG, "Move pos: " + startX );
				float nowX = event.getX();
				float nowY = event.getY();
				mDeltaX = nowX - startX;
				mDeltaY = nowY - startY;
				move();
				break;
			case MotionEvent.ACTION_UP:
				// if( ignoreMotion() && mCurrentTouchView != null ){
				// Toast.makeText(getApplicationContext(), "Long Click", Toast.LENGTH_SHORT ).show();
				// }
				removeItem();
			case MotionEvent.ACTION_CANCEL:
				removeItem();
				break;

			}

			Log.d( TAG, "delta: " + mDeltaX );

			if ( ignoreMotion() ) {
				Log.d( "MovableLinearLayout", "Not consume action" );
				return false;
			} else {
				return true;
			}

		}

		private boolean ignoreMotion() {
			return Math.abs( mDeltaX ) < MIN_X_DISTANCE || Math.abs( mDeltaY ) > MIN_Y_DISTANCE;
		}

		private void removeItem() {

			if ( mPosition >= 0 && needToBeRemoved() ) {
				startAnimation();
			} else {
				reset();
			}

		}

		private void reset() {
			mDeltaX = 0f;
			mDeltaY = 0f;
			mPosition = -1;
			move();
			// setCurrentTouchView( null );
		}

		@SuppressLint("NewApi")
		private void move() {
			Log.d( TAG, "Moviong" );
			if ( mCurrentTouchView != null ) {
				mCurrentTouchView.setX( mDeltaX );
				if ( needToBeRemoved() ) {
					mCurrentTouchView.findViewById( R.id.delete ).setVisibility( View.VISIBLE );
				} else {
					mCurrentTouchView.findViewById( R.id.delete ).setVisibility( View.GONE );
				}
			}
		}

		private boolean needToBeRemoved() {
			return Math.abs( mDeltaX ) >= REMOVE_DISTANCE;
		}

		private void startAnimation() {
			float left = mCurrentTouchView.getX();
			float width = mCurrentTouchView.getWidth();
			boolean toLeft = mDeltaX < 0;
			float toXDelta;
			if ( toLeft ) {
				toXDelta = -1 * ( width + left );
			} else {
				toXDelta = width - left;
			}
			Log.d( TAG, "toXDelta " + toXDelta );
			TranslateAnimation anim = new TranslateAnimation( 0f, toXDelta, 0f, 0f );
			anim.setInterpolator( new AccelerateInterpolator() );
			anim.setDuration( 200L );
			anim.setAnimationListener( new RemoveAnimationListener( toLeft ) );
			mCurrentTouchView.startAnimation( anim );
		}

		private class RemoveAnimationListener implements AnimationListener {

			public boolean toLeft = false;

			public RemoveAnimationListener( boolean toLeft ) {
				this.toLeft = toLeft;
			}

			@Override
			public void onAnimationEnd( Animation animation ) {
				int width = mCurrentTouchView.getWidth();
				if ( toLeft ) {
					width = -1 * width;
				}
				mCurrentTouchView.setX( width );
				Animation anim = new CollapseAnimation( mCurrentTouchView, false );
				anim.setDuration( 500L );
				anim.setAnimationListener( new CollapseAnimationListener() );
				anim.setFillAfter( true );
				mCurrentTouchView.startAnimation( anim );
			}

			@Override
			public void onAnimationRepeat( Animation animation ) {

			}

			@Override
			public void onAnimationStart( Animation animation ) {
			}

		}

		private class CollapseAnimationListener implements AnimationListener {

			@Override
			public void onAnimationEnd( Animation animation ) {
				mContent.remove( mPosition );
				mAdapter.notifyDataSetChanged();
				reset();
			}

			@Override
			public void onAnimationRepeat( Animation animation ) {

			}

			@Override
			public void onAnimationStart( Animation animation ) {

			}

		}
	}

	private class CollapseAnimationListener implements AnimationListener {

		int position;
		View view;
		public CollapseAnimationListener( int position, View view ){
			this.position = position;
			this.view = view;
		}
		
		@Override
		public void onAnimationEnd( Animation animation ) {
			view = null;
			mContent.remove( position );
			mAdapter.notifyDataSetChanged();
		}

		@Override
		public void onAnimationRepeat( Animation animation ) {

		}

		@Override
		public void onAnimationStart( Animation animation ) {

		}

	}
	
	private class ListAdapter extends BaseAdapter {

		class ViewHolder {
			TextView text;
			View view;
		}

		private LayoutInflater inflater;
		private ArrayList<Integer> posChanges;
		
		public ListAdapter( Context ctx ) {
			inflater = LayoutInflater.from( ctx );
			posChanges = new ArrayList<Integer>();
		}

		@Override
		public int getCount() {
			return mContent.size();
		}

		@Override
		public Object getItem( int position ) {
			return mContent.get( position );
		}

		@Override
		public long getItemId( int position ) {
			return 0;
		}

		public void setPositionChange( ArrayList<Integer> changes ){
			posChanges = changes;
		}
		
		@Override
		public View getView( int position, View convertView, ViewGroup parent ) {

			ViewHolder holder;
			if ( convertView == null || convertView.getAnimation() != null ) {
				convertView = inflater.inflate( R.layout.file_list_item, null );
				holder = new ViewHolder();
				holder.view = convertView.findViewById( R.id.layout_list );
				holder.text = ( TextView ) convertView.findViewById( R.id.textView_file );
				convertView.setTag( holder );
			} else {
				holder = ( ViewHolder ) convertView.getTag();
			}

			holder.text.setText( mContent.get( position ) );
			if( posChanges.contains( position )){
				Animation anim = new CollapseAnimation( convertView, true );
				anim.setDuration( 1000l );
				anim.setAnimationListener(new Listener( convertView ));
				convertView.startAnimation( anim );
				posChanges.remove( Integer.valueOf(position) );
			}
			return convertView;
		}

		private class Listener implements AnimationListener {
			
			View view;
			public Listener(View view){
				this.view = view;
			}
			
			@Override
			public void onAnimationStart( Animation animation ) {
				
			}
			
			@Override
			public void onAnimationRepeat( Animation animation ) {
				
			}
			
			@Override
			public void onAnimationEnd( Animation animation ) {
				view.setAnimation( null );
			}
		};

	}

	/**
	 * Original Post: http://stackoverflow.com/questions/4946295/android-expand-collapse-animation
	 */
	private class CollapseAnimation extends Animation {
		int targetHeight;
		View view;
		boolean expand;

		public CollapseAnimation( View view, boolean expand ) {
			this.view = view;
			this.targetHeight = this.view.getHeight();
			this.expand = expand;
		}

		@Override
		protected void applyTransformation( float interpolatedTime, Transformation t ) {
			int newHeight;
			if ( expand ) {
				newHeight = ( int ) ( targetHeight * interpolatedTime );
			} else {
				newHeight = ( int ) ( targetHeight * ( 1 - interpolatedTime ) );
			}
			view.getLayoutParams().height = newHeight;
			view.requestLayout();
		}

		@Override
		public void initialize( int width, int height, int parentWidth, int parentHeight ) {
			super.initialize( width, height, parentWidth, parentHeight );
		}

		@Override
		public boolean willChangeBounds() {
			return true;
		}
	}

}
