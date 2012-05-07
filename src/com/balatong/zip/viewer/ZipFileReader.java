
package com.balatong.zip.viewer;

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.balatong.logger.Logger;

import android.os.AsyncTask;

public class ZipFileReader extends AsyncTask<File, Integer, Map<String, Object>>{
		
	private static Logger logger = Logger.getLogger(ZipFileReader.class.getName());
	private File file;
	private Map<String, Object> zipStructure = new HashMap<String, Object>();
	
	@Override
	protected Map<String, Object> doInBackground(File... file) {
		int numFiles = readFile(file[0]);
		return zipStructure;
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
		Map<String, Object> parent = zipStructure;
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
		logger.debug("Reading entry: " + name);
		boolean isFile = true;
		if (name.endsWith("/"))
			isFile = false;
		String[] paths = name.split("/");
		
		Map<String, Object> parent = zipStructure;
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

	public void closeFile() {
	}

	public boolean unzipFile(Map<String, Object> zipEntries, String extractPath) {
		logger.debug("Extracting to: " + extractPath);		
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			extractFiles(zipEntries, zipFile, extractPath);			
			return true;
		}
		catch (Exception e) {
			logger.error("Unable to extract files." + e.getMessage(), e);
			return false;
		}
		finally {
			try {
				zipFile.close();
			}
			catch (Exception e) {
			}
		}
	}

	private void extractFiles(Map<String, Object> zipEntries, ZipFile zipFile, String extractPath) throws IOException {
		File path = new File(extractPath);
		if (!path.exists())
			if (!path.mkdirs()) {
				String message = "Unable to create directory " + extractPath + ".";
				IOException e = new IOException(message);
				logger.error(message, e);
				throw e;
			}
		for (Map.Entry<String, Object> entry : zipEntries.entrySet()) {
			if (entry.getValue() instanceof ZipEntry) {
				InputStream is = zipFile.getInputStream((ZipEntry)entry.getValue());
				writeFile(entry.getKey(), is, extractPath);
				is.close();
			}
			else { // instance of Map
				if (!"..".equals(entry.getKey()))
					extractFiles((Map<String, Object>)entry.getValue(), zipFile, extractPath + "/" + entry.getKey());
			}
		}
	}

	private void writeFile(String key, InputStream is, String extractPath) throws IOException {
		FileOutputStream fos = new FileOutputStream(extractPath + "/" + key);
		int MAX_BYTES = 1024;
		byte[] buffer = new byte[MAX_BYTES];
		int offset = 0;
		int count = is.read(buffer, 0, MAX_BYTES);
		while (count > 0) {
			fos.write(buffer, 0, count);
			offset += count;
			count = is.read(buffer, 0, MAX_BYTES);
		}
		fos.close();
		logger.debug("Written to file: " + extractPath + "/" + key + ".");
	}
}
