package com.wenhui.filebrowser.activity;

import java.io.File;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.wenhui.filebrowser.App;
import com.wenhui.filebrowser.DeviceInfo;
import com.wenhui.filebrowser.R;

public class PrefsActivity extends PreferenceActivity {

	private int RESULT = RESULT_CANCELED;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setTitle( R.string.setting );
		
		addPreferencesFromResource( R.xml.setting );
		final CheckBoxPreference hidden = ( CheckBoxPreference ) getPreferenceManager().findPreference( App.PREFS_KEY_HIDDEN );
		final CheckBoxPreference saveState = ( CheckBoxPreference ) getPreferenceManager().findPreference( App.PREFS_KEY_SAVE_PREV_STATE );
		final Preference feedback = ( Preference ) findPreference(App.PREFS_KEY_FEEDBACK);
		
		
		hidden.setOnPreferenceChangeListener( new OnHiddenPreferenceChangeListener() );

		saveState.setOnPreferenceChangeListener( new OnSaveStatePreferenceChangeListener() );

		feedback.setOnPreferenceClickListener( new OnFeedBackClickListener() );
		
		if( DeviceInfo.hasHoneycomb() ){
			final Preference removable = ( Preference ) findPreference( App.PREFS_KEY_REMOVABLE );
			removable.setOnPreferenceClickListener( new OnRemovablePathPreferenceClickListener() );
		}
	}

	@Override
	public void finish() {
		super.finish();
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {

		switch ( item.getItemId() ) {
		case android.R.id.home:
			finish();
			break;
		}
		
		return true;
	}
	
	private class OnHiddenPreferenceChangeListener implements OnPreferenceChangeListener {
		@Override
		public boolean onPreferenceChange( Preference preference, Object newValue ) {
			RESULT = RESULT_OK;
			App.instance().setShowHiddenFiles( ( Boolean ) newValue );
			return true;
		}
	}
	
	private class OnSaveStatePreferenceChangeListener implements OnPreferenceChangeListener {
		@Override
		public boolean onPreferenceChange( Preference preference, Object newValue ) {
			App.instance().setSavePrevState( ( Boolean ) newValue );
			return true;
		}
	}
	
	private class OnFeedBackClickListener implements OnPreferenceClickListener {

		@Override
		public boolean onPreferenceClick( Preference preference ) {
			Intent intent_mail = new Intent( android.content.Intent.ACTION_SEND );
			intent_mail.setType( "plain/text" );
			intent_mail.putExtra( Intent.EXTRA_EMAIL, new String[] { "jywh842005@gmail.com" } );
			try {
				startActivity( Intent.createChooser( intent_mail, PrefsActivity.this.getString( R.string.share_action ) ) );
				return true;
			} catch ( android.content.ActivityNotFoundException ex ) {
			}
			return false;
		}
	}
	
	private class OnRemovablePathPreferenceClickListener implements OnPreferenceClickListener {

		@Override
		public boolean onPreferenceClick( Preference preference ) {
			
			AlertDialog.Builder builder = new AlertDialog.Builder( PrefsActivity.this );
			builder.setTitle( R.string.set_removable_path );
			
			LayoutInflater inflater = LayoutInflater.from( PrefsActivity.this );
			View view = inflater.inflate( R.layout.entry, null );
			builder.setView( view );
			
			final TextView warning = (TextView) view.findViewById( R.id.textView_entry );
			final EditText edit = ( EditText)view.findViewById( R.id.editText_add_folder );
			warning.setText( "" );
			edit.addTextChangedListener( new CheckFileExistWatcher(warning) );
			builder.setPositiveButton( R.string.ok, new OnClickListener() {
				@Override
				public void onClick( DialogInterface dialog, int which ) {
					String path = (String)edit.getText().toString();
					if( !path.isEmpty() ){
						File file = new File(path);
						if( file.exists() && file.isDirectory() && file.canRead() ){
							App.instance().setRemovableDir( path );
						}
					}
				}
			} );
			builder.setNegativeButton( R.string.cancel, new OnClickListener() {
				
				@Override
				public void onClick( DialogInterface dialog, int which ) {
				}
			} ).show();
			return true;
		}
		
		
	}
	
	private class CheckFileExistWatcher implements TextWatcher {

		private static final int PATH_EXIST_MSG = R.string.path_exist_msg;
		private static final int PATH_NOT_EXIST_MSG = R.string.path_not_exiost_msg;
		private static final int PATH_NOT_ACCESSABLE = R.string.path_not_accssable;
		
		private TextView tv;
		
		public CheckFileExistWatcher( TextView tv ){
			this.tv = tv;
		}
		
		@Override
		public void afterTextChanged( Editable s ) {
			if( s.length() == 0 ){
				tv.setText( PATH_NOT_EXIST_MSG );
				return;
			}
			File file = new File(s.toString());
			if( !file.exists() || !file.isDirectory() ){
				tv.setText( PATH_NOT_EXIST_MSG );
				return;
			}
			
			if( !file.canRead() ){
				tv.setText( PATH_NOT_ACCESSABLE );
				return;
			}
			
			tv.setText( PATH_EXIST_MSG );
		}

		@Override
		public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
		}

		@Override
		public void onTextChanged( CharSequence s, int start, int before, int count ) {
			
		}
		
	}
}
