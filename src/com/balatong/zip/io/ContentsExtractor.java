package com.balatong.zip.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.balatong.logger.Logger;
import com.balatong.zip.R;
import com.balatong.zip.viewer.ViewerActivity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

public class ContentsExtractor extends AsyncTask<String, Object, Integer> {

	private Logger logger = Logger.getLogger(ContentsExtractor.class.getName());
	
	final public static String NEW_ENTRY = "NEW_ENTRY";
	final public static String OLD_ENTRY = "OLD_ENTRY";
	final public static String ENTRY_NAME = "ENTRY_NAME";
	final public static String ENTRY_SIZE = "ENTRY_SIZE";
	final public static String BYTES_READ = "BYTES_READ";
	final public static String TOTAL_FILES = "TOTAL_FILES";
	final public static String TOTAL_SIZE = "TOTAL_SIZE";
	
	final private static int MAX_BYTES = 24 * 1024;

	private Context context;
	private Map<String, Object> zipEntries;
	private File file;

	private int numToBeExtracted = 0;
	private long totalSizeToBeExtracted = 0;	
	
	public ContentsExtractor(File file, Map<String, Object> zipEntries, Context context) {
		this.file = file;
		this.zipEntries = zipEntries;
		this.context = context;
	}
	
	@Override
	protected Integer doInBackground(String... params) {
		long current = System.currentTimeMillis();
		Integer extracted = unzipContents(params[0]);
		logger.debug("Elapsed: " + (System.currentTimeMillis() - current) / 1000 + " secs.");
		return extracted;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		logger.debug("Retrieving stats.");
		retrieveStats(zipEntries);
		LocalBroadcastManager.getInstance(context).sendBroadcast(wrapIntent(
				ViewerActivity.VA_START_CONTENT_EXTRACT, 
				ViewerActivity.STATUS_TEXT, context.getResources().getString(R.string.extracting_files),
				TOTAL_FILES, getNumToBeExtracted(),
				TOTAL_SIZE, getTotalSizeToBeExtracted()));			
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		LocalBroadcastManager.getInstance(context).sendBroadcast(wrapIntent(
				ViewerActivity.VA_END_CONTENT_EXTRACT, 
				ViewerActivity.STATUS_TEXT, context.getResources().getString(R.string.extracted_num_files, result)));			
	}
	
	@Override
	protected void onProgressUpdate(Object... values) {
		super.onProgressUpdate(values);
		
		if (NEW_ENTRY.equals(values[0])) {
			LocalBroadcastManager.getInstance(context).sendBroadcast(wrapIntent(
					ViewerActivity.VA_SHOW_NEW_PROGRESS_INFO, 
					ViewerActivity.STATUS_TEXT, context.getResources().getString(R.string.extracting_file, values[1]),
					ENTRY_SIZE, values[2]
			));			
		}
		else {
			LocalBroadcastManager.getInstance(context).sendBroadcast(wrapIntent(
					ViewerActivity.VA_SHOW_UPDATE_PROGRESS_INFO, 
					BYTES_READ, values[1]
			));			
		}
	}
	
	public Integer unzipContents(String extractPath) {
		logger.debug("Extracting to: " + extractPath);
		ZipFile zipFile = null;
		Integer extracted = 0;
		try {
			zipFile = new ZipFile(file);
			extracted = extractContents(zipEntries, zipFile, extractPath);			
			return extracted;
		}
		catch (Exception e) {
			logger.error("Unable to extract files." + e.getMessage(), e);
			return extracted;
		}
		finally {
			try {
				zipFile.close();
			}
			catch (Exception e) {
			}
		}
	}
	
	private int getNumToBeExtracted() {
		return numToBeExtracted;
	}

	private long getTotalSizeToBeExtracted() {
		return totalSizeToBeExtracted;
	}

	private void retrieveStats(Map<String, Object> entries) {
		for (Map.Entry<String, Object> entry : entries.entrySet()) {
			if (entry.getValue() instanceof ZipEntry) {
				ZipEntry zipEntry = (ZipEntry)entry.getValue();
				numToBeExtracted++;
				totalSizeToBeExtracted += zipEntry.getSize();
			}
			else {
				if (!"..".equals(entry.getKey())) {
					retrieveStats((Map<String, Object>)entry.getValue());
				}
			}
		}
	}
	
	private Integer extractContents(Map<String, Object> entries, ZipFile zipFile, String extractPath) throws IOException {
		File path = new File(extractPath);
		if (!path.exists())
			if (!path.mkdirs()) {
				String message = "Unable to create directory " + extractPath + ".";
				IOException e = new IOException(message);
				logger.error(message, e);
				throw e;
			}
		int extracted = 0;
		for (Map.Entry<String, Object> entry : entries.entrySet()) {
			if (entry.getValue() instanceof ZipEntry) {
				
				publishProgress(NEW_ENTRY, entry.getKey(), (int)((ZipEntry)entry.getValue()).getSize());
				InputStream is = zipFile.getInputStream((ZipEntry)entry.getValue());
				writeFile(entry.getKey(), (ZipEntry)entry.getValue(), is, extractPath);
				is.close();
				extracted++;
			}
			else { 
				if (!"..".equals(entry.getKey())) {
					extracted += extractContents((Map<String, Object>)entry.getValue(), zipFile, extractPath + "/" + entry.getKey());
				}
			}
		}
		return extracted;
	}
	
	private void writeFile(String key, ZipEntry entry, InputStream is, String extractPath) throws IOException {
		FileOutputStream fos = new FileOutputStream(extractPath + "/" + key);
		byte[] buffer = new byte[MAX_BYTES];
		int count = 0;
		while ((count = is.read(buffer, 0, MAX_BYTES)) > 0) {
			fos.write(buffer, 0, count);
			publishProgress(OLD_ENTRY, count);
		}
		fos.close();
		logger.debug("Written to file: " + extractPath + "/" + key + ".");
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

	
}
