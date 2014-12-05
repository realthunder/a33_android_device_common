/*
 * add by chenjd, chenjd@allwinnertech.com  20110919
 * when a file is created,modify or delete,it will used this class to notify the MediaScanner to refresh the media database
 */

package com.softwinner.explore;

import java.io.File;
import java.io.IOException;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;

public class RefreshMedia {
	private Context mContext;

	static final String EXTERNAL_VOLUME = "external";

	private static final String TAG = "RefreshMedia";

	public RefreshMedia(Context c) {
		mContext = c;
	}

	private void fileAdd(File file) {
		Uri mUri = Uri.fromFile(file);
		mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mUri));
	}

	private void folderAdd(File file) {
		File tempFile = new File(file, ".template");
		try {
			tempFile.createNewFile();
			Bundle args = new Bundle();
			args.putString("filepath", tempFile.getPath());
			args.putBinder("listener", mDeleteBinder);
			mContext.startService(new Intent("android.media.IMediaScannerService").putExtras(args));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	android.media.IMediaScannerListener.Stub mDeleteBinder = new android.media.IMediaScannerListener.Stub() {
		@Override
		public void scanCompleted(String path, Uri uri) {
			File tempFile = new File(path);
			if (tempFile.exists()) {
				tempFile.delete();
				notifyMediaDeleteForScanner(path);
			}
		}
	};

	public void notifyMediaAdd(File file) {
		if (!file.exists())
			return;
		if (file.isDirectory()) {
			folderAdd(file);
			File[] fileList = file.listFiles();
			for (File f : fileList) {
				notifyMediaAdd(f);
			}
		} else {
			fileAdd(file);
		}
	}

	public void notifyMediaAdd(String path) {
		File file = new File(path);
		notifyMediaAdd(file);
	}

	public void notifyMediaDelete(String file) {
		notifyMediaDeleteForScanner(file);
		notifyMediaDeleteFor4K(file);
	}

	private void notifyMediaDeleteForScanner(String file) {
		// for MediaScanner
		final int ID_COLUMN_INDEX = 0;
		final int PATH_COLUMN_INDEX = 1;
		String[][] PROJECTION = new String[][] { new String[] { Audio.Media._ID, Audio.Media.DATA },
				new String[] { Video.Media._ID, Video.Media.DATA },
				new String[] { Images.Media._ID, Images.Media.DATA },
				new String[] { Files.FileColumns._ID, Files.FileColumns.DATA } };
		Uri[] mediatypes = new Uri[] { Audio.Media.getContentUri(EXTERNAL_VOLUME),
				Video.Media.getContentUri(EXTERNAL_VOLUME), Images.Media.getContentUri(EXTERNAL_VOLUME),
				Files.getContentUri(EXTERNAL_VOLUME) };

		ContentResolver cr = mContext.getContentResolver();
		Cursor c = null;
		for (int i = 0; i < mediatypes.length; i++) {
			c = cr.query(mediatypes[i], PROJECTION[i], null, null, null);
			if (c != null) {
				try {
					while (c.moveToNext()) {
						long rowId = c.getLong(ID_COLUMN_INDEX);
						String path = c.getString(PATH_COLUMN_INDEX);
						if (path != null && path.startsWith(file)
								&& (path.equals(file) || path.charAt(file.length()) == '/')) {
							if (Log.isLoggable("fileManager", Log.DEBUG))
								Log.d(TAG, "delete row " + rowId + "in table " + mediatypes[i]);
							cr.delete(ContentUris.withAppendedId(mediatypes[i], rowId), null, null);
						}
					}
				} finally {
					c.close();
					c = null;
				}
			}
		}
	}

	private void notifyMediaDeleteFor4K(String file) {
		// for 4k player
		File mfile = new File(file);
		Uri mUri = Uri.fromFile(mfile);
		Intent mIntent = new Intent();
		mIntent.setData(mUri);
		mIntent.setAction("android.intent.action.softwinner.MEDIA_SCANNER_DELETE_FILE");
		mContext.sendBroadcast(mIntent);
	}
}