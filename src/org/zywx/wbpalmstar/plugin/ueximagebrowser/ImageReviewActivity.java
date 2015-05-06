package org.zywx.wbpalmstar.plugin.ueximagebrowser;

import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.ResoureFinder;
import org.zywx.wbpalmstar.base.cache.MyAsyncTask;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class ImageReviewActivity extends Activity implements OnClickListener {

	private ImageView imageView;
	private Button btnRePick;
	private Button btnConfirm;
	private String path;
	private boolean isPickSuccess = false;
	private ResoureFinder finder;
	public static final String INTENT_KEY_PICK_IMAGE_RETURN_PATH = "mediaImagePick";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		finder = ResoureFinder.getInstance();
		startPickPhoto();
	}

	private void initViews() {
		setContentView(finder.getLayoutId("plugin_imagebrowser_review_layout"));
		getWindow().getDecorView().setBackgroundDrawable(null);
		imageView = (ImageView) findViewById(finder
				.getId("plugin_image_review_photo"));
		btnConfirm = (Button) findViewById(finder
				.getId("plugin_image_review_btn_confirm"));
		btnRePick = (Button) findViewById(finder
				.getId("plugin_image_review_btn_repick"));
		btnConfirm.setOnClickListener(this);
		btnRePick.setOnClickListener(this);
	}

	private void startPickPhoto() {
		isPickSuccess = false;
		Intent intent = null;
		if (Build.VERSION.SDK_INT == 16) {
			intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("image/*");
			intent.putExtra("return-data", true);
		} else {
			intent = new Intent(Intent.ACTION_PICK);
			intent.setType("image/*");
		}
		try {
			startActivityForResult(
					intent,
					EUExImageBrowser.F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_PICK);
		} catch (ActivityNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			this.finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == EUExImageBrowser.F_ACT_REQ_CODE_UEX_MEDIA_LIBRARY_IMAGE_PICK) {
			if (resultCode == Activity.RESULT_OK) {
				BDebug.d("ImageReviewActivity", "data:" + data.getDataString());
				isPickSuccess = true;
				String url = data.getDataString();// "content://u5 903sfdj"
				if (url == null) {
					Toast.makeText(
							this,
							finder.getString("plugin_image_browser_system_have_not_return_image_path"),
							Toast.LENGTH_SHORT).show();
					return;
				}
				if (URLUtil.isFileUrl(url)) {
					path = url.replace("file://", "");
				} else {
					Cursor c = managedQuery(data.getData(), null, null, null,
							null);
					boolean isExist = c.moveToFirst();
					if (isExist) {
						path = c.getString(c
								.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
					}
				}
				initViews();
				BDebug.d("ImageReviewActivity", "path:" + path);
				final int maxSize = ImageUtility.getPictrueSourceMaxSize(this);
				new MyAsyncTask() {
					private BitmapFactory.Options options = new BitmapFactory.Options();

					@Override
					protected Object doInBackground(Object... params) {
						Bitmap bitmap = null;
						try {
							bitmap = ImageUtility.decodeSourceBitmapByPath(
									path, options, maxSize);
						} catch (OutOfMemoryError e) {
							LogUtils.e("ImageReviewActivity",
									"OutOfMemoryError: " + e.getMessage());
						}
						return bitmap;
					}

					public void handleOnCompleted(MyAsyncTask task,
							Object result) {
						if (result == null) {
							Toast.makeText(
									ImageReviewActivity.this,
									finder.getString("plugin_image_browser_load_image_fail"),
									Toast.LENGTH_SHORT).show();
						} else {
							Bitmap bitmap = (Bitmap) result;
							imageView.setImageBitmap(bitmap);
						}

					};
				}.execute(new Object[] {});
			} else {
				finish();
			}
		}
	}

	@Override
	public void onClick(View v) {
		if (v == btnRePick) {
			startPickPhoto();
		} else if (v == btnConfirm) {
			if (isPickSuccess) {
				Intent intent = new Intent();
				intent.putExtra(INTENT_KEY_PICK_IMAGE_RETURN_PATH, path);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

}