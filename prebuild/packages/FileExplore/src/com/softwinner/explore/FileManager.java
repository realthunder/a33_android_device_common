/*
    Open Manager, an open source file manager for the Android system
    Copyright (C) 2009, 2010, 2011  Joe Berria <nexesdevelopment@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.softwinner.explore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Stack;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;

/**
 * This class is completely modular, which is to say that it has
 * no reference to the any GUI activity. This class could be taken
 * and placed into in other java (not just Android) project and work.
 * <br>
 * <br>
 * This class handles all file and folder operations on the system.
 * This class dictates how files and folders are copied/pasted, (un)zipped
 * renamed and searched. The EventHandler class will generally call these
 * methods and have them performed in a background thread. Threading is not
 * done in this class.  
 * 
 * @author Joe Berria
 *
 */
public class FileManager {
	private static final String TAG = FileManager.class.getSimpleName();
	public  static final int ROOT_FLASH = 0;
	public  static final int ROOT_SDCARD = 1;
	public  static final int ROOT_USBHOST = 2;
	public	static final int ROOT_UNKNOWN = 3;
	
	private static final int BUFFER = 		2048;
	private static final int SORT_NONE = 	0;
	private static final int SORT_ALPHA = 	1;
	private static final int SORT_TYPE = 	2;
	
	private ArrayList<String> flashPath;
	private ArrayList<String> sdcardPath;
	private ArrayList<String> usbPath;
	private DevicePath mDevices;
	
	private boolean mShowHiddenFiles = false;
	private int mSortType = SORT_ALPHA;
	private long mDirSize = 0;
	private Stack<String> mPathStack;
	private ArrayList<String> mDirContent;
	
	public String flashList = "Sdcard";
	public String sdcardList = "External Sdcard";
	public String usbhostList = "Usb";
	
	private Context mContext ;
	/**
	 * Constructs an object of the class
	 * <br>
	 * this class uses a stack to handle the navigation of directories.
	 */
	public FileManager(Context context) {
		mDirContent = new ArrayList<String>();
		mPathStack = new Stack<String>();
		mContext = context;
		
		flashList = mContext.getResources().getString(R.string.flash);
		sdcardList = mContext.getResources().getString(R.string.extsd);
		usbhostList = mContext.getResources().getString(R.string.usbhost);
		
		mDevices = new DevicePath(context);
		flashPath = mDevices.getInterStoragePath();
		sdcardPath = mDevices.getSdStoragePath();
		usbPath = mDevices.getUsbStoragePath();
		mPathStack.push("/");
		mPathStack.push(flashList);
	}
	
	/**
	 * This will return a string of the current directory path
	 * @return the current directory
	 */
	public String getCurrentDir() {
		return mPathStack.peek();
	}
	
	/**
	 * This will return a string of the current home path.
	 * @return	the home directory
	 */
	public ArrayList<String> getHomeDir(int root_type) {
		//This will eventually be placed as a settings item
		mPathStack.clear();
		mPathStack.push("/");
		switch(root_type)
		{
			case ROOT_SDCARD:
				mPathStack.push(sdcardList);
				return mDevices.getMountedPath(sdcardPath);
				
			case ROOT_USBHOST:
				mPathStack.push(usbhostList);
				return mDevices.getMountedPath(usbPath);
				
			case ROOT_FLASH:
			default:
				mPathStack.push(flashList);
				return mDevices.getMountedPath(flashPath);
				
		}
	}
	
	/**
	 * This will tell if current path is root
	 * @return	is root?
	 */
	public boolean isRoot() {
		//This will eventually be placed as a settings item
		String tmp = mPathStack.peek();
		
		if(tmp.equals(sdcardList) ||
			tmp.equals(usbhostList) ||
			tmp.equals(flashList))
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * This will tell which root we are in;
	 * @return	which root?
	 */
	public int whichRoot() {
		//This will eventually be placed as a settings item
		String tmp = mPathStack.peek();
		if(tmp.equals(flashList)){
			return ROOT_FLASH;
		}else if(tmp.equals(sdcardList)){
			return ROOT_SDCARD;
		}else if(tmp.equals(usbhostList)){
			return ROOT_USBHOST;
		}else{
			for(String st:sdcardPath){
				if(tmp.startsWith(st)){
					return ROOT_SDCARD;
				}
			}
			for(String st:flashPath){
				if(tmp.startsWith(st)){
					return ROOT_FLASH;
				}
			}
			for(String st:usbPath){
				if(tmp.startsWith(st)){
					return ROOT_USBHOST;
				}
			}
		}
		return ROOT_UNKNOWN;
	}
	
	/**
	 * This will determine if hidden files and folders will be visible to the
	 * user.
	 * @param choice	true if user is veiwing hidden files, false otherwise
	 */
	public void setShowHiddenFiles(boolean choice) {
		mShowHiddenFiles = choice;
	}
	
	/**
	 * 
	 * @param type
	 */
	public void setSortType(int type) {
		mSortType = type;
	}
	
	/**
	 * This will return a string that represents the path of the previous path
	 * @return	returns the previous path
	 */
	public ArrayList<String> getPreviousDir() {
		int size = mPathStack.size();
		
		if (size >= 2)
			mPathStack.pop();
		
		else if(size == 0)
			mPathStack.push(flashList);
		
		String st = mPathStack.peek();
		if(st.equals(flashList)){
			return mDevices.getMountedPath(flashPath);
		}else if(st.equals(sdcardList)){
			return mDevices.getMountedPath(sdcardPath);
		}else if(st.equals(usbhostList)){
			return mDevices.getMountedPath(usbPath);
		}else{
			return populate_list();
		}
	}
	
	/**
	 * 
	 * @param path
	 * @param isFullPath
	 * @return
	 */
	public ArrayList<String> getNextDir(String path) {
		if(!path.equals(mPathStack.peek())) {
			mPathStack.push(path);
		}
		if(flashList.equals(path)){
			return mDevices.getMountedPath(flashPath);
		}else if(sdcardList.equals(path)){
			return mDevices.getMountedPath(sdcardPath);
		}else if(usbhostList.equals(path)){
			return mDevices.getMountedPath(usbPath);
		}else{
			return populate_list();
		}
	}

	/**
	 * @param oldPath the file to be copied
	 * @param newPath the directory to move the file to
	 * @param handler callback handler
	 * @return
	 */
	public int copyToDirectory(String oldPath, String newPath, Handler handler) {
		if (newPath.startsWith(oldPath)) {
			Log.e(TAG, "newPath(" + newPath + ") startsWith oldPath(" + oldPath + ")");
			return -1;
		}
		if (handler != null) {
			Message msg = new Message();
			msg.obj = oldPath;
			handler.sendMessage(msg);
		}
		File oldFile = new File(oldPath);
		File newFile = new File(newPath);
		if (newFile.isDirectory() && newFile.canRead() && newFile.canWrite()) {
			return realCopyToDirectory(oldFile, newFile, handler);
		}
		return -1;
	}

	private int realCopyToDirectory(File oldFile, File newFile, Handler handler) {
		String copyName = mContext.getResources().getString(R.string.Copied);
		byte[] data = new byte[BUFFER];
		int read = 0;
		File distFile = null;
		if (oldFile.getParent().equals(newFile.getAbsolutePath())) {
			String fileName = oldFile.getName();
			distFile = new File(newFile, copyName + fileName);
			int n = 2;
			while (distFile.exists()) {
				distFile = new File(newFile, copyName + n++ + "_" + fileName);
			}
		} else {
			distFile = new File(newFile, oldFile.getName());
		}
		if (oldFile.isFile()) {
			try {
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(distFile));
				BufferedInputStream bis = new BufferedInputStream(new FileInputStream(oldFile));
				while ((read = bis.read(data, 0, BUFFER)) != -1) {
					bos.write(data, 0, read);
					if (handler != null) {
						Message msg = new Message();
						msg.arg1 = read;
						handler.sendMessage(msg);
					}
				}
				bos.flush();
				bos.close();
				bis.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return -1;

			} catch (IOException e) {
				e.printStackTrace();
				return -1;
			}
		} else if (oldFile.isDirectory()) {
			if (!distFile.exists())
				if (!distFile.mkdir())
					return -1;
			for (File file : oldFile.listFiles())
				realCopyToDirectory(file, distFile, handler);
		}
		RefreshMedia mRefresh = new RefreshMedia(mContext);
		mRefresh.notifyMediaAdd(distFile);
		return 0;
	}
	
	/**
	 * 
	 * @param toDir
	 * @param fromDir
	 */
	public void extractZipFilesFromDir(String zipName, String toDir, String fromDir) {
		/* create new directory for zip file */
		String path = fromDir + "/" + zipName;
		String zipDir = toDir + zipName.substring(0, zipName.lastIndexOf(".")) + "/";
		unZipFiles(path, zipDir);
	}

	/**
	 * 
	 * @param zip_file
	 * @param directory
	 */
	public void extractZipFiles(String zip_file, String directory) {
		/* create new directory for zip file */
		String path = zip_file;
		String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
		String zipDir = path.substring(0, path.lastIndexOf("/") + 1) + name + "/";
		unZipFiles(path, zipDir);
	}

	private void unZipFiles(String zipPath, String zipDir) {
		byte[] data = new byte[BUFFER];
		java.util.zip.ZipEntry entry;
		java.util.zip.ZipFile zf;
		Enumeration<? extends java.util.zip.ZipEntry> enumeration;

		File zipFile = new File(zipDir);
		zipFile.mkdir();
		try {
			zf = new java.util.zip.ZipFile(zipPath);
			enumeration = zf.entries();
			while (enumeration.hasMoreElements()) {
				entry = enumeration.nextElement();
				String path = zipDir + entry.getName();
				File pathFile = new File(path);
				if (!pathFile.getParentFile().exists())
					pathFile.getParentFile().mkdirs();
				if (entry.isDirectory()) {
					pathFile.mkdir();
				} else {
					int read = 0;
					FileOutputStream out = new FileOutputStream(pathFile);
					InputStream zipstream = zf.getInputStream(entry);
					while ((read = zipstream.read(data, 0, BUFFER)) != -1)
						out.write(data, 0, read);
					zipstream.close();
					out.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		RefreshMedia mRefresh = new RefreshMedia(mContext);
		mRefresh.notifyMediaAdd(zipFile);
	}

	/**
	 * 
	 * @param path
	 */
	public void createZipFile(String path) {
		File dir = new File(path);
		if (!dir.canRead() || !dir.canWrite())
			return;

		final String name = path.substring(path.lastIndexOf(File.separator) + 1, path.length()) + ".zip";
		String pullPath = path + File.separator + name;
		String[] dirList = dir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String filename) {
				return !name.equals(filename);
			}
		});
		try {
			java.util.zip.ZipOutputStream zout = new java.util.zip.ZipOutputStream(new BufferedOutputStream(
					new FileOutputStream(pullPath)));
			for (String item : dirList)
				zipEntry(path, item, zout);
			zout.close();
			RefreshMedia mRefresh = new RefreshMedia(mContext);
			mRefresh.notifyMediaAdd(pullPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void zipEntry(String dir, String entryName, java.util.zip.ZipOutputStream zout) {
		byte[] data = new byte[BUFFER];
		int read;
		File file = new File(dir + File.separator + entryName);
		try {
			if (file.isFile()) {
				java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryName);
				zout.putNextEntry(entry);
				BufferedInputStream instream;
				instream = new BufferedInputStream(new FileInputStream(file));
				while ((read = instream.read(data, 0, BUFFER)) != -1)
					zout.write(data, 0, read);
				zout.closeEntry();
				instream.close();
			} else if (file.isDirectory()) {
				java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(entryName + File.separator);
				zout.putNextEntry(entry);
				zout.closeEntry();
				String fileList[] = file.list();
				for (String item : fileList)
					zipEntry(dir, entryName + File.separator + item, zout);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param filePath
	 * @param newName
	 * @return -1:newName file is exist; -2:rename fail; 0:rename success;
	 */
	public int renameTarget(String filePath, String newName) {
		File src = new File(filePath);
		String ext = "";
		File dest;
				
		if(src.isFile() && filePath.lastIndexOf(".") > 0)
			/*get file extension*/
			ext = filePath.substring(filePath.lastIndexOf("."), filePath.length());
		
		if(newName.length() < 1)
			return -2;
	
		String temp = filePath.substring(0, filePath.lastIndexOf("/"));
		
		String destPath = temp + "/" + newName + ext;
		dest = new File(destPath);
		
		//add to make sure destPath is not exists ! 
		//Beacuse FS can not exists the same file name in the same diretory !
		if(dest.exists()){
			return -1;
		}
		
		if(src.renameTo(dest))
		{
			RefreshMedia mRefresh = new RefreshMedia(mContext);
			mRefresh.notifyMediaAdd(destPath);
			mRefresh.notifyMediaDelete(filePath);
			return 0;
		}
		else
			return -2;
	}
	
	/**
	 * 
	 * @param path
	 * @param name
	 * @return
	 */
	public int createDir(String path, String name) {
		if (!path.endsWith("/"))
			path += "/";
		String dirPath = path + name;
		if (new File(dirPath).mkdir()) {
			RefreshMedia mRefresh = new RefreshMedia(mContext);
			mRefresh.notifyMediaAdd(dirPath);
			return 0;
		}
		return -1;
	}
	
	/**
	 * The full path name of the file to delete.
	 * @param path
	 */
	public void deleteTarget(String path) {
		File target = new File(path);
		realDeleteTarget(target);
		RefreshMedia mRefresh = new RefreshMedia(mContext);
		mRefresh.notifyMediaDelete(path);
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void realDeleteTarget(File target) {
		if (target.exists() && target.canWrite()) {
			if (target.isFile()) {
				target.delete();
			} else if (target.isDirectory()) {
				File[] fileList = target.listFiles();
				if (fileList != null && fileList.length > 0) {
					for (File file : fileList) {
						realDeleteTarget(file);
					}
				}
				target.delete();
			}
		}
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	public boolean isDirectory(String name) {
		return new File(mPathStack.peek() + "/" + name).isDirectory();
	}
		
	/**
	 * converts integer from wifi manager to an IP address. 
	 * 
	 * @param des
	 * @return
	 */
	public static String integerToIPAddress(int ip) {
		String ascii_address = "";
		int[] num = new int[4];
		
		num[0] = (ip & 0xff000000) >> 24;
		num[1] = (ip & 0x00ff0000) >> 16;
		num[2] = (ip & 0x0000ff00) >> 8;
		num[3] = ip & 0x000000ff;
		 
		ascii_address = num[0] + "." + num[1] + "." + num[2] + "." + num[3];
		 
		return ascii_address;
	 }
	
	/**
	 * 
	 * @param dir
	 * @param pathName
	 * @return
	 */
	public ArrayList<String> searchInDirectory(String dir, String pathName) {
		ArrayList<String> names = new ArrayList<String>();
		search_file(dir, pathName, names);

		return names;
	}

	/**
	 * 
	 * @param path
	 * @return
	 */
	public long getFileSize(File path) {
		long size = 0;
		if (!path.canRead()) {
			size = 0;
		} else if (path.isFile()) {
			size += path.length();
		} else if (path.isDirectory()) {
			File[] list = path.listFiles();
			if(list != null) {
				int len = list.length;
				for (int i = 0; i < len; i++) {
					size += getFileSize(list[i]);
				}
			}
		}
		return size;
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 */
	public long getDirSize(String path) {
		get_dir_size(new File(path));

		return mDirSize;
	}
	
	
	private static final Comparator alph = new Comparator<String>() {
		@Override
		public int compare(String arg0, String arg1) {
			return arg0.toLowerCase().compareTo(arg1.toLowerCase());
		}
	};
	
	private static final Comparator type = new Comparator<String>() {
		@Override
		public int compare(String arg0, String arg1) {
			String ext = null;
			String ext2 = null;
			
			try {
				ext = arg0.substring(arg0.lastIndexOf(".") + 1, arg0.length());
				ext2 = arg1.substring(arg1.lastIndexOf(".") + 1, arg1.length());
				
			} catch (IndexOutOfBoundsException e) {
				return 0;
			}
			
			return ext.compareTo(ext2);
		}
	};
	
	/* (non-Javadoc)
	 * this function will take the string from the top of the directory stack
	 * and list all files/folders that are in it and return that list so 
	 * it can be displayed. Since this function is called every time we need
	 * to update the the list of files to be shown to the user, this is where 
	 * we do our sorting (by type, alphabetical, etc).
	 * 
	 * @return
	 */
	private ArrayList<String> populate_list() {

		if(!mDirContent.isEmpty())
			mDirContent.clear();
		try
		{
			String path = mPathStack.peek();
			File file = new File(path); 
		
			if(file.exists() && file.canRead() && file.isDirectory()) {
//				String[] list = file.list();
				File[] fList = file.listFiles();
				boolean isPartition = false;
				if(mDevices.hasMultiplePartition(path)){
					if (Log.isLoggable("fileManager", Log.DEBUG))
						Log.d("chen",path + " has multi partition");
					isPartition = true;
				}
				if(fList != null)
				{
					int len = fList.length;
					
					/* add files/folder to arraylist depending on hidden status */
					for (int i = 0; i < len; i++) {
						if(isPartition){
							try{
								StatFs statFs = new StatFs(fList[i].getAbsolutePath());
								int count = statFs.getBlockCount();
								if (Log.isLoggable("fileManager", Log.DEBUG))
									Log.d("chen",fList[i].getName() + "  " + count);
								if(count == 0){
									continue;
								}
							}catch(Exception e){
								Log.e(TAG,fList[i].getName() + "  exception");
								continue;
							}
						}
						String name = fList[i].getName();
						if(!mShowHiddenFiles) {
							if(name.charAt(0) != '.')
								mDirContent.add(name);
					
						} else {
							mDirContent.add(name);
						}
					}
			
					/* sort the arraylist that was made from above for loop */
					switch(mSortType) 
					{
						case SORT_NONE:
							//no sorting needed
							break;
					
						case SORT_ALPHA:
							Object[] tt = mDirContent.toArray();
							mDirContent.clear();
					
							Arrays.sort(tt, alph);
					
							for (Object a : tt){
								mDirContent.add((String)a);
							}
							break;
					
						case SORT_TYPE:
							Object[] t = mDirContent.toArray();
							String dir = mPathStack.peek();
					
							Arrays.sort(t, type);
							mDirContent.clear();
					
							for (Object a : t){
								if(new File(dir + "/" + (String)a).isDirectory())
									mDirContent.add(0, (String)a);
								else
									mDirContent.add((String)a);
							}
							break;
					}
				}	
			} 
		}catch(Exception e)
		{
			/* clear any operate made above */
			Log.e("FileManager", "unknow exception");
			mDirContent.clear();
		}
		return mDirContent;
	}
	
	/*
	 * This function will be rewritten as there is a problem getting
	 * the directory size in certain folders from root. ex /sys, /proc.
	 * The app will continue until a stack overflow. get size is fine uder the 
	 * sdcard folder.
	 * 
	 * @param path
	 */
	private void get_dir_size(File path) {
		if(path.isFile()&& path.canRead()){
			mDirSize += path.length();
		}else{
			File[] list = path.listFiles();
			int len;
			
			if(list != null) {
				len = list.length;

				for (int i = 0; i < len; i++) {
					if(list[i].isFile() && list[i].canRead()) {
						mDirSize += list[i].length();
					} else if(list[i].isDirectory() && list[i].canRead()) { 
						get_dir_size(list[i]);
					}
				}
			}
		}
	}

	/*
	 * (non-JavaDoc)
	 * I dont like this method, it needs to be rewritten. Its hacky in that
	 * if you are searching in the root dir (/) then it is not going to be treated
	 * as a recursive method so the user dosen't have to sit forever and wait.
	 * 
	 * I will rewrite this ugly method.
	 * 
	 * @param dir		directory to search in
	 * @param fileName	filename that is being searched for
	 * @param n			ArrayList to populate results
	 */
	private void search_file(String dir, String fileName, ArrayList<String> n) {
		File root_dir = new File(dir);
		String[] list = root_dir.list();
		
		if(list != null && root_dir.canRead()) {
			int len = list.length;
			
			for (int i = 0; i < len; i++) {
				File check = new File(dir + "/" + list[i]);
				String name = check.getName();
					
				if(check.isFile() && name.toLowerCase().
										contains(fileName.toLowerCase())) {
					n.add(check.getPath());
				}
				else if(check.isDirectory()) {
					if(name.toLowerCase().contains(fileName.toLowerCase()))
						n.add(check.getPath());
					
					else if(check.canRead() && !dir.equals("/"))
						search_file(check.getAbsolutePath(), fileName, n);
				}
			}
		}
	}
}
