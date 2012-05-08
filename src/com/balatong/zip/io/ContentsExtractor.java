package com.balatong.zip.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.balatong.logger.Logger;

import android.os.AsyncTask;

public class ContentsExtractor extends AsyncTask<Object, String, Integer> {

	private Logger logger = Logger.getLogger(ContentsExtractor.class.getName());
	final private static int MAX_BYTES = 24 * 1024;
	
	private File file;
	
	public void setFile(File file) {
		this.file = file;
	}
	
	@Override
	protected Integer doInBackground(Object... params) {
		long current = System.currentTimeMillis();
		Integer extracted = unzipContents((Map<String, Object>)params[0], (String)params[1]);
		logger.debug("Elapsed: " + (System.currentTimeMillis() - current) / 1000 + " secs.");
		return extracted;
	}

	public Integer unzipContents(Map<String, Object> zipEntries, String extractPath) {
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
				publishProgress(entry.getKey());
				InputStream is = zipFile.getInputStream((ZipEntry)entry.getValue());
				writeFile(entry.getKey(), is, extractPath);
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

	private void writeFile(String key, InputStream is, String extractPath) throws IOException {
		//BufferedInputStream bis = new BufferedInputStream(is);
		FileOutputStream fos = new FileOutputStream(extractPath + "/" + key);
		//BufferedOutputStream bos = new BufferedOutputStream(fos);
		byte[] buffer = new byte[MAX_BYTES];
		int count = 0;
		while ((count = is.read(buffer, 0, MAX_BYTES)) > 0) {
			fos.write(buffer, 0, count);
		}
		fos.close();
		//bos.close();
		//bis.close();
		logger.debug("Written to file: " + extractPath + "/" + key + ".");
	}

}
