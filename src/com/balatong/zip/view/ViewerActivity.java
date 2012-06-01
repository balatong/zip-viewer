package com.balatong.zip.view;

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
import com.balatong.zip.service.UnzipperService;
//import com.balatong.zip.service.ZipperService;

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

	public static final int ZIP_FILE_TO_OPEN = 0;

	public static final String STATUS_TEXT = "STATUS_TEXT";
	public static final String TITLE_TEXT = "TITLE_TEXT";
	
	public static final String VA_SET_STATUS_TEXT = "com.balatong.zip.viewer.VA_SET_STATUS_TEXT";
	public static final String VA_REFRESH_CONTENTS = "com.balatong.zip.viewer.VA_REFRESH_CONTENTS";
	public static final String VA_START_FILE_READ = "com.balatong.zip.viewer.VA_START_FILE_READ";
	public static final String VA_END_FILE_READ = 	"com.balatong.zip.viewer.VA_END_FILE_READ";
	public static final String VA_START_PROCESS_CONTENT = "com.balatong.zip.viewer.VA_START_PROCESS_CONTENT";
	public static final String VA_END_PROCESS_CONTENT =   "com.balatong.zip.viewer.VA_END_PROCESS_CONTENT";
	public static final String VA_SHOW_FILE_CHECKSUMS = "com.balatong.zip.viewer.VA_SHOW_FILE_CHECKSUMS";
	public static final String VA_SHOW_NEW_PROGRESS_INFO = "com.balatong.zip.viewer.VA_SHOW_NEW_PROGRESS_INFO";
	public static final String VA_SHOW_UPDATE_PROGRESS_INFO = "com.balatong.zip.viewer.VA_SHOW_UPDATE_PROGRESS_INFO";
	
	private ContentsAdapter zipContentsAdapter;

	private ProgressBar activityBar;
	private TextView statusBar;
	private ListView directoryContents;
	private View infoView;
	private View progressView;
	
	private ServiceConnection unzipperServiceConnection;
	private UnzipperService.Unzipper unzipper;
		
	private boolean receiversRegistered;
	private boolean isUnzipperBound;
	
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.zip_viewer_activity);
		
		activityBar = (ProgressBar)findViewById(R.id.pbar_status_activity);
		statusBar = (TextView)findViewById(R.id.txt_status_message);
		directoryContents = (ListView)findViewById(R.id.list_directory_contents);
		zipContentsAdapter = new ContentsAdapter(this, directoryContents);

		initializeMenus();
		initializeServices();
		
		ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
		menuOpen.setEnabled(true);		
		statusBar.setText(R.string.select_file_to_load);
	}
		
	private void initializeServices() {
		unzipperServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceDisconnected(ComponentName name) {
				unzipper.closeZipFile();
				isUnzipperBound = false;
			}
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				unzipper = (UnzipperService.Unzipper)service;
				isUnzipperBound = true;
				ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
				menuOpen.setEnabled(true);
			}
		};		
		Intent unzipperIntent = new Intent(ViewerActivity.this, UnzipperService.class);
		startService(unzipperIntent);
		if (getIntent().getData() != null) { 
			unzipperIntent.setData(getIntent().getData());
		}
		bindService(unzipperIntent, unzipperServiceConnection, Context.BIND_AUTO_CREATE);		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, final Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		switch (requestCode) {
		case ZIP_FILE_TO_OPEN:
			if (resultCode == RESULT_OK) {
				ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
				menuOpen.setEnabled(false);
				toggleMenus(false);
				unzipper.readZipFile(intent);
				zipContentsAdapter.setCurrentDirectory("");
			}
			else {
				AlertDialog.Builder builder = new AlertDialog.Builder(ViewerActivity.this);
				builder.setTitle(getResources().getText(R.string.title_open_google_play));
				builder.setMessage(getResources().getText(R.string.info_no_file_manager_installed));
				builder.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case AlertDialog.BUTTON_POSITIVE:
							String fileManagerUri = "market://details?id=org.openintents.filemanager";
							Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileManagerUri));
							startActivity(marketIntent);
							break;
						default:
							break;
						}
					}
				});
				builder.setNeutralButton(android.R.string.cancel, null);
				builder.create().show();
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
		            startActivityForResult(Intent.createChooser(intent, null), ZIP_FILE_TO_OPEN);
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
				
				AlertDialog.Builder builder = new AlertDialog.Builder(ViewerActivity.this);
				builder.setTitle(getResources().getText(R.string.title_extract_files));
				builder.setView(viewExtractPath);
				builder.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case AlertDialog.BUTTON_POSITIVE:
							String extractPath = ((EditText)viewExtractPath.findViewById(R.id.txt_extract_path)).getText().toString();
							unzipper.extractZipEntries(zipContentsAdapter.getCheckedItems(), extractPath);
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
				AlertDialog.Builder builder = new AlertDialog.Builder(ViewerActivity.this);
				builder.setTitle(getResources().getText(R.string.title_check_files));
				builder.setMessage(R.string.confirm_file_check);
				builder.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case AlertDialog.BUTTON_POSITIVE:
							unzipper.crcCheckZipFile();
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
		
		ImageButton menuAdd = (ImageButton)findViewById(R.id.img_btn_menu_add);
		menuAdd.setEnabled(false);
		menuAdd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LayoutInflater inflater = getLayoutInflater();
				final View viewAddPath = inflater.inflate(R.layout.add_path_dialog, null);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(ViewerActivity.this);
				builder.setTitle(getResources().getText(R.string.title_add_files));
				builder.setView(viewAddPath);
				builder.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case AlertDialog.BUTTON_POSITIVE:
							String addPath = ((EditText)viewAddPath.findViewById(R.id.txt_add_path)).getText().toString();
							unzipper.addZipEntries(zipContentsAdapter.getCurrentDirectory(), addPath);
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
		
		ImageButton menuDelete = (ImageButton)findViewById(R.id.img_btn_menu_delete);
		menuDelete.setEnabled(false);
		menuDelete.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(ViewerActivity.this);
				builder.setTitle(getResources().getText(R.string.title_delete_files));
				builder.setMessage(R.string.confirm_file_delete);
				builder.setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case AlertDialog.BUTTON_POSITIVE:
							unzipper.deleteZipEntries(zipContentsAdapter.getCheckedItems());
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
		
		ImageButton menuInfo = (ImageButton)findViewById(R.id.img_btn_menu_info);
		menuInfo.setEnabled(false);
		menuInfo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LayoutInflater inflater = getLayoutInflater();
				infoView = inflater.inflate(R.layout.file_info_dialog, null);
				
				File file = unzipper.getFile();
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
				
				pBar = new ProgressBar(ViewerActivity.this);
				pBar.setIndeterminate(true);
				view = (LinearLayout)infoView.findViewById(R.id.txt_crc);
				view.removeAllViews();
				view.addView(pBar);
				
				AlertDialog.Builder builder = new AlertDialog.Builder(ViewerActivity.this);
				builder.setTitle(getResources().getText(R.string.title_file_info));
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
			if (VA_SET_STATUS_TEXT.equals(action)) {
				String statusText = intent.getExtras().getString(STATUS_TEXT);
				statusBar.setText(statusText);
				logger.debug("Setting status with message: " + statusText);
			}
			else if (VA_REFRESH_CONTENTS.equals(action)) {
				unzipper.readZipFile(intent);
				logger.debug("Refresh contents: " + intent.getData().toString());
			}
			else if (VA_START_FILE_READ.equals(action)) {
				String statusText = intent.getExtras().getString(STATUS_TEXT);

				ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
				menuOpen.setEnabled(false);
				toggleMenus(false);
				
				activityBar.setIndeterminate(false);
				activityBar.setVisibility(ProgressBar.VISIBLE);
				statusBar.setText(statusText);
			}
			else if (VA_END_FILE_READ.equals(action)) {
				String statusText = intent.getExtras().getString(STATUS_TEXT);

				ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
				menuOpen.setEnabled(true);
				toggleMenus(true);

				activityBar.setVisibility(ProgressBar.INVISIBLE);
				statusBar.setText(statusText);

				zipContentsAdapter.setSource(unzipper.getResult());
				directoryContents.setAdapter(zipContentsAdapter);
				
			}
			else if (VA_START_PROCESS_CONTENT.equals(action)) {
				String statusText = intent.getExtras().getString(STATUS_TEXT);

				ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
				menuOpen.setEnabled(false);
				toggleMenus(false);

				activityBar.setIndeterminate(true);
				activityBar.setVisibility(ProgressBar.VISIBLE);
				statusBar.setText(statusText);
				
				if (progressView == null) {
					LayoutInflater inflater = getLayoutInflater();
					progressView = inflater.inflate(R.layout.process_entries_dialog, null);
					
					ProgressBar pbarTotalEntries = (ProgressBar)progressView.findViewById(R.id.pbar_processing_total);
					pbarTotalEntries.setMax(intent.getExtras().getInt(ContentsExtractor.TOTAL_FILES));
					pbarTotalEntries.setProgress(0);
					
					AlertDialog.Builder builder = new AlertDialog.Builder(ViewerActivity.this);
					builder.setTitle(intent.getExtras().getString(TITLE_TEXT));
					builder.setView(progressView);
					builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent();
							intent.setData(Uri.parse(unzipper.getFile().toString()));
							unzipper.readZipFile(intent);
							zipContentsAdapter.setCurrentDirectory("");
						}
					});
					builder.create().show();
				}
			}
			else if (VA_END_PROCESS_CONTENT.equals(action)) {
				String statusText = intent.getExtras().getString(STATUS_TEXT);

				ImageButton menuOpen = (ImageButton)findViewById(R.id.img_btn_menu_open);
				menuOpen.setEnabled(true);
				toggleMenus(true);
				
				activityBar.setVisibility(ProgressBar.INVISIBLE);
				statusBar.setText(statusText);
				
				zipContentsAdapter.uncheckItems();

				if (progressView != null) {
					ProgressBar pbarTotalEntries = (ProgressBar)progressView.findViewById(R.id.pbar_processing_total);
					pbarTotalEntries.incrementProgressBy(1);
	
					ProgressBar pbarEntry = (ProgressBar)progressView.findViewById(R.id.pbar_processing_entry);
					pbarEntry.setMax(1);
					pbarEntry.setProgress(1);
					progressView = null;
				}
			}
			else if (VA_SHOW_NEW_PROGRESS_INFO.equals(action)) {
				String statusText = intent.getExtras().getString(STATUS_TEXT);
				if (progressView == null) 
					return;

				ProgressBar pbarTotalEntries = (ProgressBar)progressView.findViewById(R.id.pbar_processing_total);
				pbarTotalEntries.incrementProgressBy(1);
				
				TextView processingTotalText = (TextView)progressView.findViewById(R.id.txt_processing_total);
				processingTotalText.setText(progressView.getResources().getString(R.string.num_out_of_processed, 
						pbarTotalEntries.getProgress(), pbarTotalEntries.getMax()));

				ProgressBar pbarEntry = (ProgressBar)progressView.findViewById(R.id.pbar_processing_entry);
				pbarEntry.setMax(intent.getExtras().getInt(ContentsExtractor.ENTRY_SIZE));
				pbarEntry.setProgress(0);
				
				TextView processingEntryText = (TextView)progressView.findViewById(R.id.txt_processing_entry);
				processingEntryText.setText(statusText);
			}
			else if (VA_SHOW_UPDATE_PROGRESS_INFO.equals(action)) {
				if (progressView == null) 
					return;

				ProgressBar pbarEntry = (ProgressBar)progressView.findViewById(R.id.pbar_processing_entry);
				pbarEntry.incrementProgressBy(intent.getExtras().getInt(ContentsExtractor.BYTES_READ));				
			}
			else if (VA_SHOW_FILE_CHECKSUMS.equals(action)) {
				String md5 = intent.getExtras().getString("MD5");
				if (infoView == null) 
					return;
				
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
				
				String crc = intent.getExtras().getString("CRC");
				view = (LinearLayout)infoView.findViewById(R.id.txt_crc);
				TextView crcView = new TextView(ViewerActivity.this);
				crcView.setText(crc);
				view.removeAllViews();
				view.addView(crcView); 
			}
		}
	};
	
	@Override
	public void onResume() {
		super.onResume();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(VA_SET_STATUS_TEXT);
		intentFilter.addAction(VA_REFRESH_CONTENTS);
		intentFilter.addAction(VA_START_FILE_READ);
		intentFilter.addAction(VA_END_FILE_READ);
		intentFilter.addAction(VA_START_PROCESS_CONTENT);
		intentFilter.addAction(VA_END_PROCESS_CONTENT);
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
		
		if (isUnzipperBound) {
			unbindService(unzipperServiceConnection);
		}
		Intent unzipperIntent = new Intent(ViewerActivity.this, UnzipperService.class);
		stopService(unzipperIntent);
	} 

}