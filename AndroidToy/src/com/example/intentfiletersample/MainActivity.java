package com.example.intentfiletersample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends Activity implements OnItemClickListener {
	ListView mList;

	String[] mListContent = { 
			"SwipableList",
			"AnimatedList"
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mList = (ListView) findViewById(R.id.listView1);
		ArrayAdapter<String> adapter = 
				new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1, mListContent);
		mList.setAdapter(adapter);
		mList.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long id) {

		switch (position) {
		case 0:
			Intent i0 = new Intent( this, SwipableListActivity.class );
			startActivity( i0 );
			break;
		case 1:
			Intent i1 = new Intent( this, AnimatedListActivity.class);
			startActivity( i1 );
			break;
		}

	}


}
