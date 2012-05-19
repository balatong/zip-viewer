package com.balatong.zip.view;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

import com.balatong.logger.Logger;
import com.balatong.zip.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ContentsAdapter extends ArrayAdapter<String> implements AdapterView.OnItemClickListener {

	private Logger logger = Logger.getLogger(ContentsAdapter.class.getName());

	private List<String> orderedList = new ArrayList<String>();
	private Map<String, Object> source;
	private ListView listView;
	private String currentDirectory = "";
	private List<Integer> checkedItems = new ArrayList<Integer>();
	
	public ContentsAdapter(Context context, ListView listView) {
		super(context, 0);
		this.listView = listView;
		this.listView.setOnItemClickListener(this);
	}
	
	public void setSource(final Map<String, Object> source) {
		super.clear();

		this.source = source;
		
		checkedItems.clear();
		orderedList.clear();
		orderedList.addAll(source.keySet());
		Collections.sort(orderedList, new Comparator<String>() {
			public int compare(String lhs, String rhs) {
				if (lhs.equals(".."))
					return -1;
				if (rhs.equals(".."))
					return +1;
				if (source.get(lhs) instanceof Map && source.get(rhs) instanceof Map)
					return lhs.compareTo(rhs);
				if (source.get(lhs) instanceof Map && !(source.get(rhs) instanceof Map))
					return -1;
				if (!(source.get(lhs) instanceof Map) && source.get(rhs) instanceof Map)
					return +1;
				if (!(source.get(lhs) instanceof Map) && !(source.get(rhs) instanceof Map))
					return lhs.compareTo(rhs);
						
				return 0;
			}
		});
		
		for	(String key : orderedList)
			super.add(key);
		
		logger.debug("Source size: " + orderedList.size());
		logger.debug("View size: " + getCount());
		logger.debug("List view size: " + listView.getChildCount());

	}
	
	public Map<String, Object> getSource() {
		return source;
	}
	
	public View getView(final int position, View view, ViewGroup parent) {
//		View view = convertView;
		if (view == null) {
			LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = vi.inflate(R.layout.zip_entry_item, null);
		}
		String key = orderedList.get(position);
		Object obj = source.get(key);
		
		CheckBox chkBox = (CheckBox)view.findViewById(R.id.chk_item_unzip);
		chkBox.setFocusable(false);
		chkBox.setEnabled(true);
		chkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked && !checkedItems.contains(position))
					checkedItems.add(position);
				else if (!isChecked && checkedItems.contains(position))
					checkedItems.remove(Integer.valueOf(position));
			}
		});
		if (checkedItems.contains(position))
			chkBox.setChecked(true);
		else
			chkBox.setChecked(false);
		if ("..".equals(key)) {
			chkBox.setEnabled(false);
			logger.debug("Disabled check box for key: " + key);
		}
				
//		logger.debug("getView() for " + key + " at position " + position + "."); 
		
		if (obj != null && obj instanceof ZipEntry) {
			ZipEntry zipEntry = (ZipEntry)obj;
			ImageView image = (ImageView)view.findViewById(R.id.img_item_icon);
			image.setImageResource(R.drawable.ic_menu_file);
			TextView entry = (TextView)view.findViewById(R.id.txt_item_zip_entry);
			entry.setText(key);
			TextView desc = (TextView)view.findViewById(R.id.txt_item_zip_desc);
			Calendar modified = Calendar.getInstance();
			modified.setTimeInMillis(zipEntry.getTime());
			desc.setText(
					"Size: " + ((zipEntry.getSize() == -1) ? "" : (zipEntry.getSize() / 1024) + " KB ") + 
					"Modified: " + ((zipEntry.getTime() == 0) ? "" : new SimpleDateFormat().format(modified.getTime())) 
					);
		}
		else if (obj != null && obj instanceof Map) {
			ImageView image = (ImageView)view.findViewById(R.id.img_item_icon);
			image.setImageResource(R.drawable.ic_menu_folder_close);
			TextView entry = (TextView)view.findViewById(R.id.txt_item_zip_entry);
			entry.setText(key);
			TextView desc = (TextView)view.findViewById(R.id.txt_item_zip_desc);
			desc.setText("");
		}
		else {
			IllegalStateException e = new IllegalStateException("There should have been an object at position " + position + "."); 
			logger.error(e.getMessage(), e);
		}

		return view;
	}
	
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		String key = orderedList.get(position);
		Object value = source.get(key); 
		logger.debug("Clicked on " + key);
		
		if (value instanceof Map) {
			source = (Map<String, Object>)value;
			TextView statusView = (TextView)view.getRootView().findViewById(R.id.txt_status_message);
			if ("..".equals(key)) {
				currentDirectory = currentDirectory.substring(0, currentDirectory.lastIndexOf("/"));
			}
			else {
				currentDirectory = currentDirectory + "/" + key;
			}
			statusView.setText(currentDirectory);
			this.setSource(source);
			listView.setAdapter(this);
		}
		else { // ZipEntry
			ZipEntry zipEntry = (ZipEntry)value;
			logger.debug("Extract: " + zipEntry.getName());
			
			// extract
		}
	}

	public Map<String, Object> getCheckedItems() {
		Map<String, Object> items = new HashMap<String, Object>();
		for (Integer position : checkedItems) {
			String key = orderedList.get(position);
			Object value = source.get(key);
			items.put(key, value);
		}
		if (items.size() == 0) {
			logger.debug("Unzipping all files in source.");
			items = source;
		}
		return items;
	}

	public void uncheckItems() {
		checkedItems.clear();
	}

	public String getCurrentDirectory() {
		return currentDirectory;
	}

	public void setCurrentDirectory(String currentDirectory) {
		this.currentDirectory = currentDirectory;
	}

}
