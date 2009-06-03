package com.nevilon.bigplanet;

import java.util.List;

import com.nevilon.bigplanet.core.db.DAO;
import com.nevilon.bigplanet.core.db.GeoBookmark;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AllGeoBookmarks extends ListActivity {

	private List<GeoBookmark> geoBookmarks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setData();
	}
	
	private void setData(){
		DAO dao = new DAO(this);
		geoBookmarks = dao.getBookmarks();
		setListAdapter(new SpeechListAdapter(this));
	}

	/**
	 * Создает элементы меню
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, 0, "Edit");
		menu.add(0, 1, 0, "Delete");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:

			break;

		case 1:
			final int bookmarkId = geoBookmarks.get((int) this.getSelectedItemId())
					.getId();
			new AlertDialog.Builder(this).setTitle("Bookmark removing")
					.setMessage("Are you really want to remove this bookmark?")
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									DAO dao = new DAO(AllGeoBookmarks.this);
									dao.removeGeoBookmark(bookmarkId);
									setData();
								}
							}).setNegativeButton("No", null).show();
			break;
		default:
			break;
		}

		return true;
	}

	private class SpeechListAdapter extends BaseAdapter {

		public SpeechListAdapter(Context context) {
			mContext = context;
		}

		public int getCount() {
			return geoBookmarks.size();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			SpeechView sv;
			GeoBookmark bookmark = geoBookmarks.get(position);
			if (convertView == null) {

				sv = new SpeechView(mContext, bookmark.getName(), bookmark
						.getDescription());
			} else {
				sv = (SpeechView) convertView;
				sv.setName(bookmark.getName());
				sv.setDescription(bookmark.getDescription());
				sv.id = bookmark.getId();
			}

			return sv;
		}

		private Context mContext;

	}

	private class SpeechView extends LinearLayout {
		public SpeechView(Context context, String name, String description) {
			super(context);
			View v = View.inflate(AllGeoBookmarks.this, R.layout.geobookmark,
					null);
			nameLabel = (TextView) v.findViewById(android.R.id.text1);
			nameLabel.setText(name);

			descriptionLabel = (TextView) v.findViewById(android.R.id.text2);
			descriptionLabel.setText(description);
			addView(v);
		}

		public void setName(String name) {
			descriptionLabel.setText(name);
		}

		public void setDescription(String description) {
			descriptionLabel.setText(description);
		}

		protected long id;

		private TextView nameLabel;
		private TextView descriptionLabel;
	}

}
