/**
 * Created by Milan Cejnar on 23.11.2017.
 * Asamm Software, s.r.o.
 */
package com.asamm.locus.addon.wear

import android.content.Context
import com.asamm.locus.addon.wear.common.communication.containers.trackrecording.TrackProfileIconValue
import locus.api.utils.Logger.logE
import java.io.*

/**
 * Manager for handling of cache in device internal storage
 */
object AppStorageManager {

    private const val TAG = "AppStorageManager"

    private const val ICON_PREFIX = "icon"

    fun isIconCached(ctx: Context, profileId: Long): Boolean {
        val f = File(ctx.cacheDir, ICON_PREFIX + profileId)
        return f.exists() && f.isFile
    }

    fun persistIcon(ctx: Context, value: TrackProfileIconValue?) {
        if (value == null || value.icon == null) return
        try {
            FileOutputStream(File(ctx.cacheDir, ICON_PREFIX + value.id)).use { fos ->
                fos.write(
                        value.icon
                )
            }
        } catch (e: IOException) {
            logE(TAG, "Cache write failed", e)
        }
    }

    @JvmStatic
    fun getIcon(ctx: Context, profileId: Long): TrackProfileIconValue? {
        val f = File(ctx.cacheDir, ICON_PREFIX + profileId)
        if (f.exists() && f.isFile) {
            try {
                FileInputStream(f).use { fis ->
                    ByteArrayOutputStream().use { baos ->
                        var nRead: Int
                        val buffer = ByteArray(1024)
                        while (fis.read(buffer, 0, buffer.size).also { nRead = it } != -1) {
                            baos.write(buffer, 0, nRead)
                        }
                        return TrackProfileIconValue(profileId, baos.toByteArray())
                    }
                }
            } catch (e: IOException) {
                logE(TAG, "Cache write failed", e)
            }
        }
        return null
    }

    /**
     * Deletes all the cache files, for debug only
     */
    fun debugTrimCache(context: Context) {
        try {
            val dir = context.cacheDir
            if (dir != null && dir.isDirectory) {
                deleteDir(dir)
            }
        } catch (e: Exception) {
            // TODO: handle exception
        }
    }

    fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
        }
        // The directory is now empty so delete it
        return dir!!.delete()
    }
}