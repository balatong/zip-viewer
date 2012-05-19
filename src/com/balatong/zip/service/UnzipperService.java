package com.balatong.zip.service;

import java.io.File;
import java.util.Map;

import com.balatong.BaseService;
import com.balatong.logger.Logger;
import com.balatong.zip.R;
import com.balatong.zip.io.ContentsExtractor;
import com.balatong.zip.io.ContentsDeleter;
import com.balatong.zip.io.ContentsInserter;
import com.balatong.zip.io.CrcValidator;
import com.balatong.zip.io.FileReader;
import com.balatong.zip.view.ViewerActivity;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

public class UnzipperService extends BaseService {

	private static Logger logger = Logger.getLogger(UnzipperService.class.getName());
	
	private File file;
	private FileReader reader;
//	private CrcValidator crcValidator;
	private Unzipper unzipper;
	
	public class Unzipper extends Binder {
		private Unzipper(){
		}
		public File getFile() {
			return file;
		}
		public File readZipFile(Intent intent) {
			return readFile(intent);
		}
		public void crcCheckZipFile() {
			crcCheck();
		}
		public void closeZipFile() {
			reader.closeFile();
		}
		public Map<String, Object> getResult() {
			return reader.getEntries();
		}
		public void extractZipEntries(Map<String, Object> checkedZipEntries, String extractPath) {
			extractEntries(checkedZipEntries, extractPath);
		}
		public void deleteZipEntries(Map<String, Object> checkedZipEntries) {
			deleteEntries(checkedZipEntries);
		}
		public void addZipEntries(String currentDirectory, String addPath) {
			addEntries(currentDirectory, addPath);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		if (unzipper == null) {
			unzipper = new Unzipper();
		}
		if (intent != null && intent.getData() != null) { 
			unzipper.readZipFile(intent);
		}
		return unzipper;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		if (intent != null && intent.getData() != null) { 
			unzipper.readZipFile(intent);
		}
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		return true;
	}
	
	private File readFile(Intent intent) {
		if (intent.getData() == null)
			return null;

		file = new File(intent.getData().getPath());
		if (!file.isFile()) {
			logger.debug("Path " + file.getAbsolutePath()
					+ " is not a valid file.");
			LocalBroadcastManager.getInstance(UnzipperService.this).sendBroadcastSync(wrapIntent(
					ViewerActivity.VA_SET_STATUS_TEXT, 
					ViewerActivity.STATUS_TEXT, getString(R.string.err_not_valid_zip_file, file.getAbsolutePath())));
			return null;
		}
		
		reader = new FileReader(UnzipperService.this);
		reader.execute(file);
		
		return file;
	}

	private void crcCheck() {
		CrcValidator crcValidator = new CrcValidator(file, UnzipperService.this);
		crcValidator.execute(reader.getEntries());
	}

	private void extractEntries(Map<String, Object> checkedZipEntries, String extractPath) {
		ContentsExtractor extractor = new ContentsExtractor(file, UnzipperService.this);
		extractor.execute(checkedZipEntries, extractPath);			
	}

	private void addEntries(String currentDirectory, String addPath) {
		ContentsInserter updater = new ContentsInserter(file, UnzipperService.this);
		updater.execute(currentDirectory, addPath);
	}
	
	private void deleteEntries(Map<String, Object> checkedZipEntries) {
		ContentsDeleter updater = new ContentsDeleter(file, UnzipperService.this);
		updater.execute(checkedZipEntries);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (reader != null)
			reader.closeFile();
	}

	private Intent wrapIntent(String action, Object... extras) {
		Intent intent = new Intent(UnzipperService.this, ViewerActivity.class);
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
