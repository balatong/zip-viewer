package com.balatong.zip.viewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

import com.balatong.BaseActivity;
import com.balatong.logger.Logger;
import com.balatong.zip.R;
import com.balatong.zip.io.ContentsExtractor;
import com.balatong.zip.io.DigestExtractor;
import com.balatong.zip.io.FileReader;
import com.balatong.zip.loader.LoaderService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RemoteViews.ActionException;
import android.widget.TextView;
import android.widget.Toast;

public class ViewerActivity extends BaseActivity {

	private static Logger logger = Logger.getLogger(ViewerActivity.class.getName());

	public static final String STATUS_TEXT = "STATUS_TEXT";
	
	public static final String VA_SET_STATUS_TEXT = "com.balatong.zip.viewer.VA_SET_STATUS_TEXT";
	public static final String VA_START_FILE_READ = "com.balatong.zip.viewer.VA_START_FILE_READ";
	public static final String VA_END_FILE_READ = 	"com.balatong.zip.viewer.VA_END_FILE_READ";
	public static final String VA_START_CONTENT_EXTRACT = "com.balatong.zip.viewer.VA_START_CONTENT_EXTRACT";
	public static final String VA_END_CONTENT_EXTRACT =   "com.balatong.zip.viewer.VA_END_CONTENT_EXTRACT";
	public static final String VA_SHOW_FILE_CHECKSUMS = "com.balatong.zip.viewer.VA_SHOW_FILE_CHECKSUMS";
	public static final String VA_SHOW_NEW_PROGRESS_INFO = "com.balatong.zip.viewer.VA_SHOW_NEW_PROGRESS_INFO";
	public static final String VA_SHOW_UPDATE_PROGRESS_INFO = "com.balatong.zip.viewer.VA_SHOW_UPDATE_PROGRESS_INFO";
	
	private ContentsAdapter zipContentsAdapter;

	private ProgressBar activityBar;
	private TextView statusBar;
	private ListView directoryContents;
	private View infoView;
	private View progressView;
	
	private ServiceConnection connection;
	private LoaderService.LoaderBinder loader;
	
	private boolean receiversRegistered;
	private boolean isBound;
	
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.zip_viewer_activity);
		
		activityBar = (ProgressBar)findViewById(R.id.pbar_status_activity);
		statusBar = (TextView)findViewById(R.id.txt_status_message);
		directoryContents = (ListView)findViewById(R.id.list_directory_contents);
		zipContentsAdapter = new ContentsAdapter(this, directoryContents);

		initializeMenus();
		
		connection = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
				loader.closeZipFile();
				isBound = false;
			}
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				loader = (LoaderService.LoaderBinder)service;
				isBound = true;
				ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
				menuOpen.setEnabled(true);
				statusBar.setText(R.string.select_file_to_load);
				logger.debug("Reading zip file.");
			}
		};
		
		Intent service = new Intent(ViewerActivity.this, LoaderService.class);
		if (getIntent().getData() != null) {
			service.setData(getIntent().getData());
			startService(service);
		}
		bindService(service, connection, Context.BIND_AUTO_CREATE);
		
		ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
		menuOpen.setEnabled(true);
		
	}
		
	@Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		switch (requestCode) {
		case 0:
			if (resultCode == RESULT_OK) {
				if (isBound) { 
					loader.readZipFile(intent);
					ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
					menuOpen.setEnabled(false);
					toggleMenus(false);
				}
			}
			break;
		}
	}
	
	private void initializeMenus() {
		ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
		menuOpen.setEnabled(false);
		menuOpen.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT); 
		        intent.setType("*/zip"); 
		        intent.setType("*/rar"); 
		        intent.setType("*/tar"); 
		        intent.setType("*/jar"); 
		        intent.setType("*/war"); 
		        intent.setType("*/ear"); 
		        intent.addCategory(Intent.CATEGORY_OPENABLE);
		        try {
		            startActivityForResult(Intent.createChooser(intent, null), 0);
		        } 
		        catch (android.content.ActivityNotFoundException ex) {
		            Toast.makeText(getParent(), 
		            		getResources().getText(R.string.info_install_file_manager), 
		                    Toast.LENGTH_SHORT).show();
		        }			
			}
		});
		
		ImageButton menuUnzip = (ImageButton)findViewById(R.id.img_btn_menu_unzip);
		menuUnzip.setEnabled(false);
		menuUnzip.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LayoutInflater inflater = getLayoutInflater();
				final View viewExtractPath = inflater.inflate(R.layout.extract_path_dialog, null);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
				builder.setTitle(v.getContext().getText(R.string.title_extract_files));
				builder.setView(viewExtractPath);
				builder.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case AlertDialog.BUTTON_POSITIVE:
							String extractPath = ((EditText)viewExtractPath.findViewById(R.id.txt_extract_path)).getText().toString();
							Map<String, Object> checkedZipEntries = zipContentsAdapter.getCheckedItems();
							ContentsExtractor extractor = new ContentsExtractor(
									loader.getFile(),
									checkedZipEntries,
									ViewerActivity.this);
							extractor.execute(extractPath);
							break;
						default:
							break;
						}
					}
				});
				builder.setNeutralButton(android.R.string.cancel, null);
				builder.create().show();
			}
		});
		
		ImageButton menuCheck = (ImageButton)findViewById(R.id.img_btn_menu_check);
		menuCheck.setEnabled(false);
		menuCheck.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
				builder.setTitle(v.getContext().getText(R.string.warn_not_yet_implemented));
				builder.setMessage(v.getContext().getText(R.string.info_available_in_future_version));
				builder.setPositiveButton(android.R.string.ok, null);
				builder.create().show();
//				reader.checkFile();
			}
		});
		
		ImageButton menuAdd = (ImageButton)findViewById(R.id.img_btn_menu_add);
		menuAdd.setEnabled(false);
		menuAdd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
				builder.setTitle(v.getContext().getText(R.string.warn_not_yet_implemented));
				builder.setMessage(v.getContext().getText(R.string.info_available_in_future_version));
				builder.setPositiveButton(android.R.string.ok, null);
				builder.create().show();
//				reader.addFile();
			}
		});
		
		ImageButton menuDelete = (ImageButton)findViewById(R.id.img_btn_menu_delete);
		menuDelete.setEnabled(false);
		menuDelete.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
				builder.setTitle(v.getContext().getText(R.string.warn_not_yet_implemented));
				builder.setMessage(v.getContext().getText(R.string.info_available_in_future_version));
				builder.setPositiveButton(android.R.string.ok, null);
				builder.create().show();
//				reader.deleteFile();
			}
		});
		
		ImageButton menuInfo = (ImageButton)findViewById(R.id.img_btn_menu_info);
		menuInfo.setEnabled(false);
		menuInfo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LayoutInflater inflater = getLayoutInflater();
				infoView = inflater.inflate(R.layout.file_info_dialog, null);
				
				File file = loader.getFile();
				TextView tView = (TextView)infoView.findViewById(R.id.txt_name);
				tView.setText(file.getName());
				tView = (TextView)infoView.findViewById(R.id.txt_path);
				tView.setText(file.getParent());
				tView = (TextView)infoView.findViewById(R.id.txt_size);
				tView.setText(file.length() / 1024 + " KB");
				tView = (TextView)infoView.findViewById(R.id.txt_modified);
				tView.setText(new SimpleDateFormat().format(file.lastModified()));
				
				ProgressBar pBar = new ProgressBar(ViewerActivity.this);
				pBar.setIndeterminate(true);
				LinearLayout view = (LinearLayout)infoView.findViewById(R.id.txt_md5);
				view.removeAllViews();
				view.addView(pBar);
				
				pBar = new ProgressBar(ViewerActivity.this);
				pBar.setIndeterminate(true);
				view = (LinearLayout)infoView.findViewById(R.id.txt_sha1);
				view.removeAllViews();
				view.addView(pBar);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
				builder.setTitle(v.getContext().getText(R.string.title_file_info));
				builder.setPositiveButton(android.R.string.ok, null);
				builder.setView(infoView);
				builder.create().show();
				
				DigestExtractor digest = new DigestExtractor(ViewerActivity.this);
				digest.execute(file);
			}
		});		
	}

	
	private void toggleMenus(Boolean enabled ) {
		ImageButton menuUnzip = (ImageButton)findViewById(R.id.img_btn_menu_unzip);
		menuUnzip.setEnabled(enabled);
		ImageButton menuCheck = (ImageButton)findViewById(R.id.img_btn_menu_check);
		menuCheck.setEnabled(enabled);
		ImageButton menuAdd = (ImageButton)findViewById(R.id.img_btn_menu_add);
		menuAdd.setEnabled(enabled);
		ImageButton menuDelete = (ImageButton)findViewById(R.id.img_btn_menu_delete);
		menuDelete.setEnabled(enabled);
		ImageButton menuInfo = (ImageButton)findViewById(R.id.img_btn_menu_info);
		menuInfo.setEnabled(enabled);
	}

	private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			String action = intent.getAction();
			String statusText = intent.getExtras().getString(STATUS_TEXT);
			if (VA_SET_STATUS_TEXT.equals(action)) {
				statusBar.setText(statusText);
			}
			else if (VA_START_FILE_READ.equals(action)) {
				ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
				menuOpen.setEnabled(false);
				toggleMenus(false);
				
				activityBar.setIndeterminate(false);
				activityBar.setVisibility(ProgressBar.VISIBLE);
				statusBar.setText(statusText);
			}
			else if (VA_END_FILE_READ.equals(action)) {
				ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
				menuOpen.setEnabled(true);
				toggleMenus(true);

				activityBar.setVisibility(ProgressBar.INVISIBLE);
				statusBar.setText(statusText);

				zipContentsAdapter.setSource(loader.getResult());
				directoryContents.setAdapter(zipContentsAdapter);
			}
			else if (VA_START_CONTENT_EXTRACT.equals(action)) {
				ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
				menuOpen.setEnabled(false);
				toggleMenus(false);

				activityBar.setIndeterminate(true);
				activityBar.setVisibility(ProgressBar.VISIBLE);
				statusBar.setText(statusText);
				
				if (progressView == null) {
					LayoutInflater inflater = getLayoutInflater();
					progressView = inflater.inflate(R.layout.extract_entries_dialog, null);

					ProgressBar pbarSelectedEntries = (ProgressBar)progressView.findViewById(R.id.pbar_selected_entries);
					pbarSelectedEntries.setMax(intent.getExtras().getInt(ContentsExtractor.TOTAL_FILES));
					pbarSelectedEntries.setProgress(0);
					
					AlertDialog.Builder builder = new AlertDialog.Builder(ViewerActivity.this);
					builder.setTitle(getText(R.string.title_extract_files));
					builder.setView(progressView);
					builder.setPositiveButton(android.R.string.ok, null);
					builder.create().show();
				}
			}
			else if (VA_SHOW_NEW_PROGRESS_INFO.equals(action)) {
				ProgressBar pbarSelectedEntries = (ProgressBar)progressView.findViewById(R.id.pbar_selected_entries);
				pbarSelectedEntries.incrementProgressBy(1);
				
				TextView extractingSelectedText = (TextView)progressView.findViewById(R.id.txt_extracting_selected);
				extractingSelectedText.setText(progressView.getResources().getString(R.string.num_out_of_extracted, 
						pbarSelectedEntries.getProgress(), pbarSelectedEntries.getMax()));

				ProgressBar pbarEntry = (ProgressBar)progressView.findViewById(R.id.pbar_zip_entry);
				pbarEntry.setMax(intent.getExtras().getInt(ContentsExtractor.ENTRY_SIZE));
				pbarEntry.setProgress(0);
				
				TextView extractingEntryText = (TextView)progressView.findViewById(R.id.txt_extracting_entry);
				extractingEntryText.setText(statusText);
			}
			else if (VA_SHOW_UPDATE_PROGRESS_INFO.equals(action)) {
				ProgressBar pbarEntry = (ProgressBar)progressView.findViewById(R.id.pbar_zip_entry);
				pbarEntry.incrementProgressBy(intent.getExtras().getInt(ContentsExtractor.BYTES_READ));				
			}
			else if (VA_END_CONTENT_EXTRACT.equals(action)) {
				ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
				menuOpen.setEnabled(true);
				toggleMenus(true);

				activityBar.setVisibility(ProgressBar.INVISIBLE);
				statusBar.setText(statusText);
				
				zipContentsAdapter.uncheckItems();

				ProgressBar pbarSelectedEntries = (ProgressBar)progressView.findViewById(R.id.pbar_selected_entries);
				pbarSelectedEntries.incrementProgressBy(1);
				
				progressView = null;
			}
			else if (VA_SHOW_FILE_CHECKSUMS.equals(action)) {
				String md5 = intent.getExtras().getString("MD5");
				LinearLayout view = (LinearLayout)infoView.findViewById(R.id.txt_md5);
				TextView md5View = new TextView(ViewerActivity.this);
				md5View.setText(md5);
				view.removeAllViews();
				view.addView(md5View);

				String sha1 = intent.getExtras().getString("SHA1");
				view = (LinearLayout)infoView.findViewById(R.id.txt_sha1);
				TextView sha1View = new TextView(ViewerActivity.this);
				sha1View.setText(sha1);
				view.removeAllViews();
				view.addView(sha1View); 
			}
			
		}
	};
	
	@Override
	public void onResume() {
		super.onResume();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(VA_SET_STATUS_TEXT);
		intentFilter.addAction(VA_START_FILE_READ);
		intentFilter.addAction(VA_END_FILE_READ);
		intentFilter.addAction(VA_START_CONTENT_EXTRACT);
		intentFilter.addAction(VA_END_CONTENT_EXTRACT);
		intentFilter.addAction(VA_SHOW_NEW_PROGRESS_INFO);
		intentFilter.addAction(VA_SHOW_UPDATE_PROGRESS_INFO);
		intentFilter.addAction(VA_SHOW_FILE_CHECKSUMS);
		LocalBroadcastManager.getInstance(ViewerActivity.this).registerReceiver(intentReceiver, intentFilter);
		receiversRegistered = true;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (receiversRegistered) {
			LocalBroadcastManager.getInstance(ViewerActivity.this).unregisterReceiver(intentReceiver);
			receiversRegistered = false;
		}
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (isBound) {
			Intent service = new Intent(ViewerActivity.this, LoaderService.class);
			unbindService(connection);
			stopService(service);
			isBound = false;
		}
	} 

}