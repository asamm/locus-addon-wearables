package com.asamm.locus.addon.wear;

import android.content.Context;

import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import locus.api.utils.Logger;

/**
 * Created by Milan Cejnar on 23.11.2017.
 * Asamm Software, s.r.o.
 */

public class AppStorageManager {
	private static final String TAG = "AppStorageManager";
	private static final String ICON_PREFIX = "icon";

	public static boolean isIconCached(Context ctx, long profileId) {
		File f = new File(ctx.getCacheDir(), ICON_PREFIX + profileId);
		return f.exists() && f.isFile();
	}

	public static void persistIcon(Context ctx, TrackProfileIconValue value) {
		if (value == null || value.getIcon() == null)
			return;
		try {
			FileOutputStream fos = new FileOutputStream(new File(ctx.getCacheDir(), ICON_PREFIX + value.getId()));
			fos.write(value.getIcon());
		} catch (IOException e) {
			Logger.logE(TAG, "Cache write failed", e);
		}
	}

	public static TrackProfileIconValue getIcon(Context ctx, long profileId) {
		File f = new File(ctx.getCacheDir(), ICON_PREFIX + profileId);
		if (f.exists() && f.isFile()) {
			try {
				FileInputStream fis = new FileInputStream(f);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int nRead;
				byte[] buffer = new byte[1024];
				while ((nRead = fis.read(buffer, 0, buffer.length)) != -1) {
					baos.write(buffer, 0, nRead);
				}
				return new TrackProfileIconValue(profileId, baos.toByteArray());
			} catch (IOException e) {
				Logger.logE(TAG, "Cache write failed", e);
			}
		}
		return null;
	}
}
