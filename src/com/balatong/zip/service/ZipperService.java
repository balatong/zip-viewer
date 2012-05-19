package com.balatong.zip.service;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipFile;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.widget.ImageButton;

import com.balatong.BaseService;
import com.balatong.logger.Logger;
import com.balatong.zip.R;
import com.balatong.zip.io.CrcValidator;
import com.balatong.zip.io.FileReader;
import com.balatong.zip.service.UnzipperService.Unzipper;
import com.balatong.zip.view.ViewerActivity;

public class ZipperService extends BaseService {

	private Logger logger = Logger.getLogger(ZipperService.class.getName());
	
	private File file;
	private FileReader reader;
	private CrcValidator crcValidator;
	private Zipper zipper;

	private ServiceConnection unzipperServiceConnection;
	private UnzipperService.Unzipper unzipper;
		
	private boolean isUnzipperBound;

	public class Zipper extends Binder {
		private Zipper() {
		}
		public File getFile() {
			return file;
		}
		public File writeZipFile(Intent intent) {
			return writeFile(intent);
		}
		public void insertZipEntries(String currentDirectory, String addPath) {
			insertEntries(currentDirectory, addPath);
		}
		public void closeZipFile() {
			reader.closeFile();
		}
	}

	private File writeFile(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	private void insertEntries(String currentDirectory, String addPath) {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
		}
		catch (IOException e) {
			// TODO: handle exception
		}
		finally {
			try {
				zipFile.close();
			}
			catch (Exception e) {
			}
		}
	}
	
//	@Override
//	public void onCreate() {
//		super.onCreate();
//		unzipperServiceConnection = new ServiceConnection() {
//			@Override
//			public void onServiceDisconnected(ComponentName name) {
//				unzipper.closeZipFile();
//				isUnzipperBound = false;
//			}
//			@Override
//			public void onServiceConnected(ComponentName name, IBinder service) {
//				unzipper = (UnzipperService.Unzipper)service;
//				isUnzipperBound = true;
//			}
//		};
//		
//		Intent unzipperIntent = new Intent(ZipperService.this, UnzipperService.class);
//		startService(unzipperIntent);
//		bindService(unzipperIntent, unzipperServiceConnection, Context.BIND_AUTO_CREATE);		
//	}
//	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		if (zipper == null) {
			zipper = new Zipper();
		}
		return zipper;
	}

//	@Override
//	public void onRebind(Intent intent) {
//		super.onRebind(intent);
//		if (intent != null && intent.getData() != null) { 
//			unzipper.readZipFile(intent);
//		}
//	}
//	
//	@Override
//	public boolean onUnbind(Intent intent) {
//		return true;
//	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();		
	} 

	

}
