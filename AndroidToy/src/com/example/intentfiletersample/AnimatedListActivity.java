package com.example.intentfiletersample;

import java.util.ArrayList;

import android.animation.*;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.*;

@SuppressLint("NewApi")
public class AnimatedListActivity extends Activity{

	private ListView mList;
	private ArrayList<String> mContent = new ArrayList<String>();
    Animator defaultAppearingAnim, defaultDisappearingAnim;
    Animator defaultChangingAppearingAnim, defaultChangingDisappearingAnim;
    Animator customAppearingAnim, customDisappearingAnim;
    Animator customChangingAppearingAnim, customChangingDisappearingAnim;
    Animator currentAppearingAnim, currentDisappearingAnim;
    Animator currentChangingAppearingAnim, currentChangingDisappearingAnim;
    LayoutTransition transistor;
    
	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		
		setContentView( R.layout.main );
		
		mList = (ListView)findViewById( R.id.listView1 );
		
		transistor = new LayoutTransition();
		mList.setLayoutTransition( transistor );
		createCustomAnimations( transistor );
		setupTransistor( transistor );
		fillListContent();
		mList.setAdapter( new ListAdapter( this ) );
		mList.setOnItemClickListener( itemClickListener );
		
	}

	private void fillListContent() {
		for ( int i = 0; i < 20; ++i ) {
			mContent.add( "Item " + i );
		}
	}
	
	private void setupTransistor(LayoutTransition transistion){
		transistion.setAnimator( LayoutTransition.APPEARING, customAppearingAnim );
		transistion.setAnimator( LayoutTransition.DISAPPEARING, customDisappearingAnim );
		transistion.setAnimator( LayoutTransition.CHANGE_APPEARING, customChangingAppearingAnim );
		transistion.setAnimator( LayoutTransition.CHANGE_DISAPPEARING, customChangingDisappearingAnim );
	}
	
    private void createCustomAnimations(LayoutTransition transition) {
        // Changing while Adding
        PropertyValuesHolder pvhLeft =
                PropertyValuesHolder.ofInt("left", 0, 1);
        PropertyValuesHolder pvhTop =
                PropertyValuesHolder.ofInt("top", 0, 1);
        PropertyValuesHolder pvhRight =
                PropertyValuesHolder.ofInt("right", 0, 1);
        PropertyValuesHolder pvhBottom =
                PropertyValuesHolder.ofInt("bottom", 0, 1);
        PropertyValuesHolder pvhScaleX =
                PropertyValuesHolder.ofFloat("scaleX", 1f, 0f, 1f);
        PropertyValuesHolder pvhScaleY =
                PropertyValuesHolder.ofFloat("scaleY", 1f, 0f, 1f);
        customChangingAppearingAnim = ObjectAnimator.ofPropertyValuesHolder(
                        this, pvhLeft, pvhTop, pvhRight, pvhBottom, pvhScaleX, pvhScaleY).
                setDuration(transition.getDuration(LayoutTransition.CHANGE_APPEARING));
        customChangingAppearingAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator anim) {
                View view = (View) ((ObjectAnimator) anim).getTarget();
                view.setScaleX(1f);
                view.setScaleY(1f);
            }
        });

        // Changing while Removing
        Keyframe kf0 = Keyframe.ofFloat(0f, 0f);
        Keyframe kf1 = Keyframe.ofFloat(.9999f, 360f);
        Keyframe kf2 = Keyframe.ofFloat(1f, 0f);
        PropertyValuesHolder pvhRotation =
                PropertyValuesHolder.ofKeyframe("rotation", kf0, kf1, kf2);
        customChangingDisappearingAnim = ObjectAnimator.ofPropertyValuesHolder(
                        this, pvhLeft, pvhTop, pvhRight, pvhBottom, pvhRotation).
                setDuration(transition.getDuration(LayoutTransition.CHANGE_DISAPPEARING));
        customChangingDisappearingAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator anim) {
                View view = (View) ((ObjectAnimator) anim).getTarget();
                view.setRotation(0f);
            }
        });

        // Adding
        customAppearingAnim = ObjectAnimator.ofFloat(null, "rotationY", 90f, 0f).
                setDuration(transition.getDuration(LayoutTransition.APPEARING));
        customAppearingAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator anim) {
                View view = (View) ((ObjectAnimator) anim).getTarget();
                view.setRotationY(0f);
            }
        });

        // Removing
        customDisappearingAnim = ObjectAnimator.ofFloat(null, "rotationX", 0f, 90f).
                setDuration(transition.getDuration(LayoutTransition.DISAPPEARING));
        customDisappearingAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator anim) {
                View view = (View) ((ObjectAnimator) anim).getTarget();
                view.setRotationX(0f);
            }
        });

    }
    
    private OnItemClickListener itemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick( AdapterView< ? > arg0, View arg1, int position, long arg3 ) {
			mContent.add( position + 1, "Add object" );
			setupTransistor( transistor );
			( (BaseAdapter)mList.getAdapter() ).notifyDataSetChanged();
		}
	};
    
    private class ListAdapter extends BaseAdapter {

		class ViewHolder {
			TextView text;
			View view;
		}

		private LayoutInflater inflater;

		public ListAdapter( Context ctx ) {
			inflater = LayoutInflater.from( ctx );
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
			return convertView;
		}

	}

}
