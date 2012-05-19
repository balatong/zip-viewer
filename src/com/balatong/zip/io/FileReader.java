
package com.balatong.zip.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.balatong.logger.Logger;
import com.balatong.zip.R;
import com.balatong.zip.service.UnzipperService;
import com.balatong.zip.view.ViewerActivity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

public class FileReader extends AsyncTask<File, Object, Map<String, Object>>{
		
	private static Logger logger = Logger.getLogger(FileReader.class.getName());

	private Context context;
	private File file;
	private Map<String, Object> zipEntries = new HashMap<String, Object>();

	public FileReader(Context ctx) {
		this.context = ctx;
	}
	
	@Override
	protected Map<String, Object> doInBackground(File... file) { 
		int numFiles = readFile(file[0]);
		return zipEntries;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		LocalBroadcastManager.getInstance(context).sendBroadcastSync(wrapIntent(
				ViewerActivity.VA_START_FILE_READ, 
				ViewerActivity.STATUS_TEXT, ""));			
	}
	
	@Override
	protected void onPostExecute(Map<String, Object> result) {
		super.onPostExecute(result);
		LocalBroadcastManager.getInstance(context).sendBroadcastSync(wrapIntent(
				ViewerActivity.VA_END_FILE_READ, 
				ViewerActivity.STATUS_TEXT, ""));			
	}	
	
	@Override
	protected void onProgressUpdate(Object... progress) {
		super.onProgressUpdate(progress);
		LocalBroadcastManager.getInstance(context).sendBroadcastSync(wrapIntent(
				ViewerActivity.VA_START_FILE_READ, 
				ViewerActivity.STATUS_TEXT, context.getResources().getString(R.string.reading_file, progress[0])));			
	}
	
	public Map<String, Object> getEntries() {
		return zipEntries;
	}

	private Integer readFile(File file) {
		this.file = file;
		ZipFile zipFile = null;;
		try {
			logger.info("Opening up " + file.getName() + ".");
			zipFile = new ZipFile(file);
		}
		catch (IOException e) {
			logger.error("Unable to open " + file.getName() + ".", e);
			LocalBroadcastManager.getInstance(context).sendBroadcast(wrapIntent(
					ViewerActivity.VA_SET_STATUS_TEXT, 
					ViewerActivity.STATUS_TEXT, context.getResources().getString(R.string.err_not_valid_zip_file, file.getAbsolutePath())));			
			return 0;
		}
		
		try {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			int numFiles = 0;
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();				
				buildStructure(entry);
				publishProgress(numFiles++);
			}
			logger.info("Read " + numFiles + " entries.");
			postBuildStructure();
			return numFiles;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			LocalBroadcastManager.getInstance(context).sendBroadcast(wrapIntent(
					ViewerActivity.VA_SET_STATUS_TEXT, 
					ViewerActivity.STATUS_TEXT, e.getMessage()));			
			return 0;
		}
		
		finally {
			logger.info("Closing " + zipFile.getName() + ".");
			try {
				if (zipFile != null)
					zipFile.close();
			}
			catch (IOException e) {
			}
		}
	}

	private void postBuildStructure() {
		Map<String, Object> parent = zipEntries;
		amendStructure(parent);
	}

	private void amendStructure(Map<String, Object> parent) {
		for (Map.Entry<String, Object> entry : parent.entrySet()) {
			if (entry.getValue() instanceof Map) {
				amendStructure((Map<String, Object>)entry.getValue());
				((Map<String, Object>)entry.getValue()).put("..", parent);
			}
		}
	}

	private void buildStructure(ZipEntry entry) {
		String name = entry.getName();
//		logger.debug("Reading N:" + name + " S:" + entry.getSize() + " CS:" + entry.getCompressedSize() + " T:" + entry.getTime());
		boolean isFile = true;
		if (name.endsWith("/"))
			isFile = false;
		String[] paths = name.split("/");
		
		Map<String, Object> parent = zipEntries;
		for (int i=0; i<paths.length-1; i++) {
			if (parent.containsKey(paths[i])) {
				parent = (Map<String, Object>)parent.get(paths[i]); 
			}
			else {
				Map<String, Object> child = new HashMap<String, Object>();
				parent.put(paths[i], child);
				parent = child;
			}
		}
		if (isFile)
			parent.put(paths[paths.length-1], entry);
	}

	private Intent wrapIntent(String action, Object... extras) {
		Intent intent = new Intent(context, ViewerActivity.class);
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

	public void closeFile() {
	}

	

}
