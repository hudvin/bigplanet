package com.nevilon.bigplanet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.util.Linkify;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.nevilon.bigplanet.core.BigPlanetApp;
import com.nevilon.bigplanet.core.MarkerManager;
import com.nevilon.bigplanet.core.Place;
import com.nevilon.bigplanet.core.Preferences;
import com.nevilon.bigplanet.core.RawTile;
import com.nevilon.bigplanet.core.db.DAO;
import com.nevilon.bigplanet.core.db.GeoBookmark;
import com.nevilon.bigplanet.core.geoutils.GeoUtils;
import com.nevilon.bigplanet.core.providers.MapStrategyFactory;
import com.nevilon.bigplanet.core.tools.savemap.MapSaverUI;
import com.nevilon.bigplanet.core.ui.AddBookmarkDialog;
import com.nevilon.bigplanet.core.ui.MapControl;
import com.nevilon.bigplanet.core.ui.OnDialogClickListener;
import com.nevilon.bigplanet.core.ui.OnMapLongClickListener;

public class BigPlanet extends Activity {

	public static final int GO_TO_LOCATION = 20;

	private static final String BOOKMARK_DATA = "bookmark";

	private static int MY_LOCATION_ZOOM = 1;

	private static int SEARCH_ZOOM = 5;

	private Toast textMessage;

	/*
	 * Графический движок, реализующий карту
	 */
	private MapControl mapControl;

	private MarkerManager mm;

	private LocationManager locationManager;

	private boolean inHome = false;

	private MyIntentReceiver intentReceiver;

	/**
	 * Конструктор
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// проверка на доступность sd
		String status = Environment.getExternalStorageState();
		if (!status.equals(Environment.MEDIA_MOUNTED)) {

			new AlertDialog.Builder(this)

			.setMessage("Please, insert sd card and restart application")
					.setCancelable(false).setNeutralButton("Ok",
							new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface arg0,
										int arg1) {
									finish();
								}

							}).show();
		} else {

		
			
		

			intentReceiver = new MyIntentReceiver();

			IntentFilter intentFilter = new IntentFilter(
					"com.nevilon.bigplanet.INTENTS.GOTO");

			registerReceiver(intentReceiver, intentFilter);

			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			// создание карты
			mm = new MarkerManager(getResources());
			RawTile savedTile = Preferences.getTile();
			//savedTile.s = 0;
			configMapControl(savedTile);
			// использовать ли сеть
			boolean useNet = Preferences.getUseNet();
			mapControl.getPhysicalMap().getTileResolver().setUseNet(useNet);
			// источник карты
			int mapSourceId = Preferences.getSourceId();
			mapControl.getPhysicalMap().getTileResolver().setMapSource(
					mapSourceId);
			mapControl.getPhysicalMap().getDefaultTile().s = mapSourceId;
			// величина отступа
			Point globalOffset = Preferences.getOffset();
			//globalOffset.x = 0;
			//globalOffset.y = -32;
			System.out.println("offset " + globalOffset + " " + savedTile);
			mapControl.getPhysicalMap().setGlobalOffset(globalOffset);
			mapControl.getPhysicalMap().reloadTiles();
			if (BigPlanetApp.isDemo) {
				showTrialDialog(R.string.this_is_demo_title,
						R.string.this_is_demo_message);
			}
			setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
		}
	}

	public class MyIntentReceiver extends BroadcastReceiver {

		/**
		 * 
		 * @see adroid.content.BroadcastReceiver#onReceive(android.content.Context,
		 *      android.content.Intent)
		 */

		@Override
		public void onReceive(Context context, Intent intent) {
			int z = SEARCH_ZOOM;
			Place place = (Place) intent.getSerializableExtra("place");
			mm.addMarker(place, z, false, MarkerManager.SEARCH_MARKER);
			com.nevilon.bigplanet.core.geoutils.Point p = GeoUtils.toTileXY(
					place.getLat(), place.getLon(), z);
			com.nevilon.bigplanet.core.geoutils.Point off = GeoUtils
					.getPixelOffsetInTile(place.getLat(), place.getLon(), z);
			mapControl.goTo((int) p.x, (int) p.y, z, (int) off.x, (int) off.y);
		}

	}
	
	

	@Override
	public boolean onSearchRequested() {
		startSearch("", false, null, false);
		return true;
	}

	/**
	 * Обрабатывает поворот телефона
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		System.gc();
		configMapControl(mapControl.getPhysicalMap().getDefaultTile());
	}

	/**
	 * Запоминает текущий тайл и отступ при выгрузке приложения
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(intentReceiver!=null){
			unregisterReceiver(intentReceiver);
			Preferences.putTile(mapControl.getPhysicalMap().getDefaultTile());
			Preferences.putOffset(mapControl.getPhysicalMap().getGlobalOffset());
		}
		if (textMessage != null) {
			textMessage.cancel();
		}	
		System.gc();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		case RESULT_OK:
			GeoBookmark bookmark = (GeoBookmark) data
					.getSerializableExtra(BOOKMARK_DATA);
			mapControl.getPhysicalMap().setDefTile(bookmark.getTile());

			Point offset = new Point();
			offset.set(bookmark.getOffsetX(), bookmark.getOffsetY());
			Preferences.putSourceId(bookmark.getTile().s);
			mapControl.getPhysicalMap().setGlobalOffset(offset);
			mapControl.getPhysicalMap().reloadTiles();
			mapControl.setMapSource(bookmark.getTile().s);
		default:
			break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent ev) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			/**
			 * если текущий режим SELECT_MODE - изменить на ZOOM_MODE если
			 * текущий режим ZOOM_MODE - делегировать обработку
			 */
			if (mapControl.getMapMode() == MapControl.SELECT_MODE) {
				mapControl.setMapMode(MapControl.ZOOM_MODE);
				return true;
			}
		default:
			return super.onKeyDown(keyCode, ev);
		}
	}

	/**
	 * Создает элементы меню
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, 7, 0, "My location").setIcon(R.drawable.home);

		SubMenu sub = menu.addSubMenu(0, 6, 0, R.string.BOOKMARKS_MENU)
				.setIcon(R.drawable.bookmark);
		sub.add(0, 61, 1, R.string.BOOKMARKS_VIEW_MENU);
		sub.add(0, 62, 0, R.string.BOOKMARK_ADD_MENU);

		// add tools menu
		sub = menu.addSubMenu(0, 1, 0, R.string.TOOLS_MENU).setIcon(
				R.drawable.tools);
		sub.add(2, 11, 1, R.string.CACHE_MAP_MENU);
		sub.add(2, 13, 10, R.string.ABOUT_MENU);
		sub.add(2, 14, 1, R.string.MAP_SOURCE_MENU);
		sub.add(2, 15, 0, R.string.NETWORK_MODE_MENU);

		menu.add(3, 3, 0, R.string.SEARCH_MENU).setIcon(R.drawable.search);

		return true;
	}

	/**
	 * Устанавливает статус(активен/неактивен) пунктов меню
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean useNet = Preferences.getUseNet();
		menu.findItem(11).setEnabled(useNet);
		return true;
	}

	/**
	 * Устанавливает размеры карты и др. свойства
	 */
	private void configMapControl(RawTile tile) {
		WindowManager wm = this.getWindowManager();
		Display display = wm.getDefaultDisplay();
		int height = display.getHeight();
		int width = display.getWidth();
		if(height==480){
			height =430;
		}
		if(height==320){
			height = 270;
		}
		
		if (mapControl == null) {
			mapControl = new MapControl(this, width, height, tile, mm);
			mapControl.setOnMapLongClickListener(new OnMapLongClickListener() {

				@Override
				public void onMapLongClick(int x, int y) {
					hideMessage();
					final GeoBookmark newGeoBookmark = new GeoBookmark();
					newGeoBookmark.setOffsetX(mapControl.getPhysicalMap()
							.getGlobalOffset().x);
					newGeoBookmark.setOffsetY(mapControl.getPhysicalMap()
							.getGlobalOffset().y);

					newGeoBookmark.setTile(mapControl.getPhysicalMap()
							.getDefaultTile());
					newGeoBookmark.getTile().s = mapControl.getPhysicalMap()
							.getTileResolver().getMapSourceId();

					AddBookmarkDialog.show(BigPlanet.this, newGeoBookmark,
							new OnDialogClickListener() {

								@Override
								public void onCancelClick() {
									// TODO Auto-generated method stub

								}

								@Override
								public void onOkClick(Object obj) {
									GeoBookmark geoBookmark = (GeoBookmark) obj;
									DAO d = new DAO(BigPlanet.this);
									d.saveGeoBookmark(geoBookmark);
									mapControl.setMapMode(MapControl.ZOOM_MODE);

								}

							});
				}

			});
		} else {
			mapControl.setSize(width, height);
		}
		mapControl.updateZoomControls();
		setContentView(mapControl, new ViewGroup.LayoutParams(width, height));
	}

	/**
	 * Создает радиокнопку с заданными параметрами
	 * 
	 * @param label
	 * @param id
	 * @return
	 */
	private RadioButton buildRadioButton(String label, int id) {
		RadioButton btn = new RadioButton(this);
		btn.setText(label);
		btn.setId(id);
		return btn;
	}

	/**
	 * Обрабатывает нажатие на меню
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		hideMessage();
		switch (item.getItemId()) {
		case 3:
			showSearch();
			break;
		case 7:
			showMyLocation();
			break;
		case 11:
			if (BigPlanetApp.isDemo
					&& mapControl.getPhysicalMap().getZoomLevel() <= 6) {
				showTrialDialog(R.string.try_demo_title,
						R.string.try_demo_message);
			} else {
				showMapSaver();
			}
			break;
		case 14:
			selectMapSource();
			break;
		case 15:
			selectNetworkMode();
			break;
		case 12:
			 showSearch();
			break;
		case 13:
			showAbout();
			break;
		case 62:
			switchToBookmarkMode();
			break;
		case 61:
			showAllGeoBookmarks();
			break;
		}
		return false;

	}

	private void showTrialDialog(int title, int message) {
		final Dialog paramsDialog = new Dialog(this);

		final View v = View.inflate(this, R.layout.demodialog, null);

		final TextView messageValue = (TextView) v.findViewById(R.id.message);
		messageValue.setText(message);
		final Button okBtn = (Button) v.findViewById(R.id.okBtn);
		okBtn.setEnabled(false);
		okBtn.setClickable(false);
		okBtn.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				paramsDialog.dismiss();
			}

		});
		paramsDialog.setTitle(title);
		paramsDialog.setCanceledOnTouchOutside(false);
		paramsDialog.setCancelable(false);
		paramsDialog.setContentView(v);

		paramsDialog.show();

		final Handler handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				int okValue = (Integer) msg.what;

				if (okValue == 0) {
					okBtn.setText(R.string.OK_LABEL);
					okBtn.setEnabled(true);
				} else {
					okBtn.setText(String.valueOf(okValue));
				}

			}
		};

		new Thread() {

			int count = 5;

			boolean exec = true;

			@Override
			public void run() {
				while (exec) {
					try {
						Thread.sleep(1000);
						count--;
						if (count == 0) {
							exec = false;
						}
						Message message = handler.obtainMessage(count);
						handler.sendMessage(message);

					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}

		}.start();
	}

	private void showMyLocation() {
		inHome = false;
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setSpeedRequired(false);
		String provider = locationManager.getBestProvider(criteria, true);
		if (provider != null) {
			locationManager.requestLocationUpdates(provider, 1, 1,
					new LocationListener() {

						public void onLocationChanged(Location location) {
							locationManager.removeUpdates(this);
							if (!inHome) {
								inHome = true;
								goToMyLocation(location, MY_LOCATION_ZOOM);
							}
						}

						public void onProviderDisabled(String arg0) {
						}

						public void onProviderEnabled(String arg0) {
						}

						public void onStatusChanged(String arg0, int arg1,
								Bundle arg2) {
						}

					});
			if (!inHome) {
				Location tmpLocation = locationManager
						.getLastKnownLocation(provider);
				if (tmpLocation != null) {
					goToMyLocation(tmpLocation, MY_LOCATION_ZOOM);
				} else {
					Toast
							.makeText(this, "Unable to get current location",
									3000).show();
				}
				inHome = true;
			}
		} else {
			Toast.makeText(this, "Unable to get current location", 3000).show();
		}
	}

	private void goToMyLocation(Location location, int zoom) {
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		com.nevilon.bigplanet.core.geoutils.Point p = GeoUtils.toTileXY(lat,
				lon, zoom);
		com.nevilon.bigplanet.core.geoutils.Point off = GeoUtils
				.getPixelOffsetInTile(lat, lon, zoom);
		mapControl.goTo((int) p.x, (int) p.y, zoom, (int) off.x, (int) off.y);

		Place place = new Place();
		place.setLat(lat);
		place.setLon(lon);
		mm.addMarker(place, zoom, true, MarkerManager.MY_LOCATION_MARKER);
	}

	private void showSearch() {
		//onSearchRequested();
		 Intent i = new Intent();
		 i.setClass(this, FindPlace.class);
		 startActivityForResult(i,0);
	}

	private void showAbout() {
		TextView tv = new TextView(this);
		tv.setLinksClickable(true);
		tv.setAutoLinkMask(Linkify.EMAIL_ADDRESSES);
		tv.setGravity(Gravity.CENTER);
		tv.setText(R.string.ABOUT_MESSAGE);
		tv.setTextSize(12f);
		new AlertDialog.Builder(this).setTitle(R.string.ABOUT_TITLE)
				.setView(tv).setIcon(R.drawable.globe).setPositiveButton(
						R.string.OK_LABEL,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog,
									int whichButton) {

							}

						}).show();
	}

	private void showAllGeoBookmarks() {
		Intent intent = new Intent();
		intent.setClass(this, AllGeoBookmarks.class);
		startActivityForResult(intent, 0);
	}

	private void switchToBookmarkMode() {
		if (mapControl.getMapMode() != MapControl.SELECT_MODE) {
			mapControl.setMapMode(MapControl.SELECT_MODE);
			showMessage();
		}
	}

	private void showMessage() {
		textMessage = Toast.makeText(this, R.string.SELECT_OBJECT_MESSAGE,
				Toast.LENGTH_LONG);
		textMessage.show();
	}

	private void hideMessage() {
		if (textMessage != null) {
			textMessage.cancel();
		}
	}

	/**
	 * Отображает диалоги для кеширования карты в заданном радиусе
	 */
	private void showMapSaver() {
		MapSaverUI mapSaverUI = new MapSaverUI(this, mapControl
				.getPhysicalMap().getZoomLevel(), mapControl.getPhysicalMap()
				.getAbsoluteCenter(), mapControl.getPhysicalMap()
				.getTileResolver().getMapSourceId());
		mapSaverUI.show();
	}

	/**
	 * Создает диалог для выбора режима работы(оффлайн, онлайн)
	 */
	private void selectNetworkMode() {
		final Dialog networkModeDialog;
		networkModeDialog = new Dialog(this);
		networkModeDialog.setCanceledOnTouchOutside(true);
		networkModeDialog.setCancelable(true);
		networkModeDialog.setTitle(R.string.SELECT_NETWORK_MODE_LABEL);

		final LinearLayout mainPanel = new LinearLayout(this);
		mainPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mainPanel.setOrientation(LinearLayout.VERTICAL);

		RadioGroup modesRadioGroup = new RadioGroup(this);

		LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(
				RadioGroup.LayoutParams.WRAP_CONTENT,
				RadioGroup.LayoutParams.WRAP_CONTENT);

		modesRadioGroup.addView(buildRadioButton(getResources().getString(
				(R.string.OFFLINE_MODE_LABEL)), 0), 0, layoutParams);

		modesRadioGroup.addView(buildRadioButton(getResources().getString(
				R.string.ONLINE_MODE_LABEL), 1), 0, layoutParams);

		boolean useNet = Preferences.getUseNet();
		int checked = 0;
		if (useNet) {
			checked = 1;
		}
		modesRadioGroup.check(checked);

		modesRadioGroup
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						boolean useNet = checkedId == 1;
						mapControl.getPhysicalMap().getTileResolver()
								.setUseNet(useNet);
						Preferences.putUseNet(useNet);
						networkModeDialog.hide();
					}

				});

		mainPanel.addView(modesRadioGroup);
		networkModeDialog.setContentView(mainPanel);
		networkModeDialog.show();

	}

	/**
	 * Создает диалог для выбора источника карт
	 */
	private void selectMapSource() {
		final Dialog mapSourceDialog;
		mapSourceDialog = new Dialog(this);
		mapSourceDialog.setCanceledOnTouchOutside(true);
		mapSourceDialog.setCancelable(true);
		mapSourceDialog.setTitle(R.string.SELECT_MAP_SOURCE_TITLE);

		ScrollView scrollPanel = new ScrollView(this);
		scrollPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		final LinearLayout mainPanel = new LinearLayout(this);
		mainPanel.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));
		mainPanel.setOrientation(LinearLayout.VERTICAL);

		RadioGroup sourcesRadioGroup = new RadioGroup(this);

		LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(
				RadioGroup.LayoutParams.WRAP_CONTENT,
				RadioGroup.LayoutParams.WRAP_CONTENT);

		for (Integer id : MapStrategyFactory.strategies.keySet()) {
			sourcesRadioGroup.addView(
					buildRadioButton(MapStrategyFactory.strategies.get(id)
							.getDescription(), id), 0, layoutParams);
		}

		sourcesRadioGroup.check(Preferences.getSourceId());

		sourcesRadioGroup
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						Preferences.putSourceId(checkedId);
						mapControl.setMapSource(checkedId);
						mapSourceDialog.hide();
					}

				});

		mainPanel.addView(sourcesRadioGroup);
		scrollPanel.addView(mainPanel);
		mapSourceDialog.setContentView(scrollPanel);
		mapSourceDialog.show();

	}

}