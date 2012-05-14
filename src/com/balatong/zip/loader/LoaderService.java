package com.balatong.zip.loader;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import com.balatong.BaseService;
import com.balatong.logger.Logger;
import com.balatong.zip.R;
import com.balatong.zip.io.FileReader;
import com.balatong.zip.viewer.ViewerActivity;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.ProgressBar;

public class LoaderService extends BaseService {

	private static Logger logger = Logger.getLogger(LoaderService.class.getName());
	
	private File file;
	private FileReader reader;
	private LoaderBinder fBinder = new LoaderBinder();
	
	public class LoaderBinder extends Binder {
		public File readZipFile(Intent intent) {
			return readFile(intent);
		}
		public void closeZipFile() {
			reader.closeFile();
		}
		public Map<String, Object> getResult() {
			return reader.getEntries();
		}
		public File getFile() {
			return file;
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int sticky = super.onStartCommand(intent, flags, startId);
		readFile(intent);
		return sticky;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return fBinder;
	}
	
	private File readFile(Intent intent) {
		if (intent.getData() == null)
			return null;

		file = new File(intent.getData().getPath());
		if (!file.isFile()) {
			logger.debug("Path " + file.getAbsolutePath()
					+ " is not a valid file.");
			LocalBroadcastManager.getInstance(LoaderService.this).sendBroadcastSync(wrapIntent(
					ViewerActivity.VA_SET_STATUS_TEXT, 
					ViewerActivity.STATUS_TEXT, getString(R.string.err_not_valid_zip_file, file.getAbsolutePath())));
			return null;
		}
		reader = new FileReader(LoaderService.this);
		reader.execute(file);

		return file;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (reader != null)
			reader.closeFile();
	}

	private Intent wrapIntent(String action, Object... extras) {
		Intent intent = new Intent(LoaderService.this, ViewerActivity.class);
		intent.setAction(action);
		for (int i=0; i<(extras.length/2); i++) {
			String key = (String)extras[(2*i)];
			Object value = extras[(2*i)+1];
			if (value instanceof String) 
				intent.putExtra(key, (String)value);
			else if (value instanceof Integer)
				intent.putExtra(key, (Integer)value);
			else if (value instanceof Long)
				intent.putExtra(key, (Long)value);
			else if (value instanceof Boolean)
				intent.putExtra(key, (Boolean)value);
		}
		return intent;
	}

}
