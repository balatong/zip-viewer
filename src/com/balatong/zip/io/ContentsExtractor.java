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

public class ContentsExtractor extends AsyncTask<Object, String, Integer> {

	private Logger logger = Logger.getLogger(ContentsExtractor.class.getName());
	final private static int MAX_BYTES = 24 * 1024;

	private Context context;
	private File file;

	private int numToBeExtracted = 0;
	private long totalSizeToBeExtracted = 0;	
	
	public ContentsExtractor(Context context) {
		this.context = context;
	}
	
	@Override
	protected Integer doInBackground(Object... params) {
		long current = System.currentTimeMillis();
		Integer extracted = unzipContents((Map<String, Object>)params[0], (String)params[1]);
		logger.debug("Elapsed: " + (System.currentTimeMillis() - current) / 1000 + " secs.");
		return extracted;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		LocalBroadcastManager.getInstance(context).sendBroadcast(wrapIntent(
				ViewerActivity.VA_START_CONTENT_EXTRACT, 
				"data", context.getResources().getString(R.string.extracting_files)));			
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		super.onPostExecute(result);
		LocalBroadcastManager.getInstance(context).sendBroadcast(wrapIntent(
				ViewerActivity.VA_END_CONTENT_EXTRACT, 
				"data", context.getResources().getString(R.string.extracted_num_files, result)));			
	}
	
	@Override
	protected void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);
		LocalBroadcastManager.getInstance(context).sendBroadcast(wrapIntent(
				"NEW_ENTRY".equals(values[0]) ? ViewerActivity.VA_SHOW_NEW_PROGRESS_INFO :
												ViewerActivity.VA_SHOW_PROGRESS_INFO, 
				"name", values[1],
				"entrySize", values[2],
				"entryExtracted", values[3],
				"totalFiles", values[4],
				"totalSize", values[5]));			
	}


	public Integer unzipContents(Map<String, Object> zipEntries, String extractPath) {
		logger.debug("Retrieving stats.");
		retrieveStats(zipEntries);
		
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

	private void retrieveStats(Map<String, Object> zipEntries) {
		for (Map.Entry<String, Object> entry : zipEntries.entrySet()) {
			if(!"..".equals(entry.getKey()) && entry.getValue() instanceof Map )
				retrieveStats((Map<String, Object>)entry.getValue());
			else if (entry.getValue() instanceof ZipEntry){
				ZipEntry zipEntry = (ZipEntry)entry.getValue();
				numToBeExtracted++;
				totalSizeToBeExtracted += zipEntry.getSize();
			}
		}
		
	}

	private Integer extractContents(Map<String, Object> zipEntries, ZipFile zipFile, String extractPath) throws IOException {
		File path = new File(extractPath);
		if (!path.exists())
			if (!path.mkdirs()) {
				String message = "Unable to create directory " + extractPath + ".";
				IOException e = new IOException(message);
				logger.error(message, e);
				throw e;
			}
		int extracted = 0;
		for (Map.Entry<String, Object> entry : zipEntries.entrySet()) {
			if (entry.getValue() instanceof ZipEntry) {
				
				publishProgress("NEW_ENTRY", 
						entry.getKey(), 
						((ZipEntry)entry.getValue()).getSize() + "", "0",
						getNumToBeExtracted() + "", getTotalSizeToBeExtracted() + "" );
				
				InputStream is = zipFile.getInputStream((ZipEntry)entry.getValue());
				writeFile(entry.getKey(), (ZipEntry)entry.getValue(), is, extractPath);
				is.close();
				extracted++;
			}
			else { // instance of Map
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
		int extracted = 0;
		while ((count = is.read(buffer, 0, MAX_BYTES)) > 0) {
			fos.write(buffer, 0, count);
			extracted += count;
			
			publishProgress("OLD_ENTRY", 
					key, 
					entry.getSize() + "", extracted + "", 
					getNumToBeExtracted() + "", getTotalSizeToBeExtracted() + "" );
		}
		fos.close();
		logger.debug("Written to file: " + extractPath + "/" + key + ".");
	}

	public void setFile(File file) {
		this.file = file;
	}
	
	public int getNumToBeExtracted() {
		return numToBeExtracted;
	}

	public long getTotalSizeToBeExtracted() {
		return totalSizeToBeExtracted;
	}

	private Intent wrapIntent(String action, String... keyVals) {
		Intent intent = new Intent(context, ViewerActivity.class);
		intent.setAction(action);
		for (int i=0; i<(keyVals.length/2); i++) 
			intent.putExtra(keyVals[(2*i)], keyVals[(2*i)+1]);
		return intent;
	}

	
}
