package com.balatong.zip.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.zip.CRC32;

import com.balatong.logger.Logger;
import com.balatong.zip.view.ViewerActivity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

public class DigestExtractor extends AsyncTask<File, Void, HashMap<String, String>> {

	private Logger logger = Logger.getLogger(DigestExtractor.class.getName());
	
	private Context context;
	final public static String MD5 = "MD5";
	final public static String SHA1 = "SHA1";
	final public static String CRC = "CRC";
	
	public DigestExtractor(Context context) {
		this.context = context;
	}
	
	@Override
	protected HashMap<String, String> doInBackground(File... params) {
		HashMap<String, String> digest = new HashMap<String, String>();
		
		File file = params[0];
		MessageDigest md5 = null;
		MessageDigest sha1 = null;
		CRC32 crc32 = null;
		InputStream is = null;
		
		try {
			md5 = MessageDigest.getInstance("MD5");
			sha1 = MessageDigest.getInstance("SHA1");
			crc32 = new CRC32();
			is = new FileInputStream(file);
			
			int count = 0;
			byte buffer[] = new byte[24 * 1024];
			while ((count = is.read(buffer)) > 0) {
				md5.update(buffer, 0, count);
				sha1.update(buffer, 0, count);
				crc32.update(buffer, 0, count);
			}
			
			StringBuilder md5Builder = new StringBuilder();
			for (byte b : md5.digest()) {
				String hex = Integer.toHexString(0xFF & b);
				if (hex.length()==2) 
					md5Builder.append(hex);
				else 
					md5Builder.append("0" + hex);
				logger.debug(hex);
			}
			digest.put("MD5", md5Builder.toString());

			StringBuilder sha1Builder = new StringBuilder();
			for (byte b : sha1.digest()) {
				String hex = Integer.toHexString(0xFF & b);
				if (hex.length()==2) 
					sha1Builder.append(hex);
				else 
					sha1Builder.append("0" + hex);
				logger.debug(hex);
			}
			digest.put("SHA1", sha1Builder.toString());
			
			digest.put("CRC", Long.toString(crc32.getValue()));
			
			return digest;
		}
		catch (Exception e) {
			digest.put("MD5", "Unable to read md5sum.");
			digest.put("SHA1", "Unable to read sha1sum.");
			digest.put("CRC", "Unable to read sha1sum.");
			return digest;
		}
		finally {
			try {
				is.close();
			}
			catch (Exception e) {
			}
		}
	}
	@Override
	protected void onPostExecute(HashMap<String, String> result) {
		LocalBroadcastManager.getInstance(context).sendBroadcastSync(wrapIntent(
				ViewerActivity.VA_SHOW_FILE_CHECKSUMS, 
				MD5, result.get(MD5),
				SHA1, result.get(SHA1),
				CRC, result.get(CRC)
		));			
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
