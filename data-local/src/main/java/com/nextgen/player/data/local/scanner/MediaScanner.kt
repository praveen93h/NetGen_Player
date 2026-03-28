package com.nextgen.player.data.local.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.nextgen.player.data.local.entity.MediaEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SUPPORTED_MIME_TYPES = setOf(
            "video/mp4",
            "video/x-matroska",
            "video/avi",
            "video/x-msvideo",
            "video/quicktime",
            "video/x-flv",
            "video/mp2ts",
            "video/webm",
            "video/3gpp",
            "video/3gpp2",
            "video/x-ms-wmv",
            "video/x-ms-asf",
            "video/ogg",
            "video/mpeg"
        )
    }

    suspend fun scanMedia(): List<MediaEntity> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaEntity>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED
        )

        val hasRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        if (hasRelativePath) {
            projection.add(MediaStore.Video.Media.RELATIVE_PATH)
        }

        val selection = "${MediaStore.Video.Media.DURATION} > ? AND ${MediaStore.Video.Media.MIME_TYPE} IN (${SUPPORTED_MIME_TYPES.joinToString(",") { "?" }})"
        val selectionArgs = arrayOf("0") + SUPPORTED_MIME_TYPES.toTypedArray()
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val relativePathColumn = if (hasRelativePath) {
                cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
            } else -1

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val dataPath = cursor.getString(pathColumn)
                val displayName = cursor.getString(displayNameColumn) ?: "unknown"
                val mimeType = cursor.getString(mimeTypeColumn) ?: "video/mp4"

                // On scoped storage, DATA may be empty; construct path from RELATIVE_PATH + DISPLAY_NAME
                val path: String
                val folderPath: String
                val folderName: String
                if (hasRelativePath && relativePathColumn >= 0) {
                    val relativePath = cursor.getString(relativePathColumn) ?: ""
                    folderPath = relativePath.trimEnd('/')
                    folderName = folderPath.substringAfterLast('/').ifEmpty { folderPath }
                    path = dataPath ?: "/storage/emulated/0/$relativePath$displayName"
                } else {
                    path = dataPath ?: continue
                    val file = File(path)
                    folderPath = file.parent ?: ""
                    folderName = file.parentFile?.name ?: ""
                }

                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                mediaList.add(
                    MediaEntity(
                        id = id,
                        path = path,
                        title = cursor.getString(titleColumn) ?: File(path).nameWithoutExtension,
                        displayName = displayName,
                        mimeType = mimeType,
                        duration = cursor.getLong(durationColumn),
                        size = cursor.getLong(sizeColumn),
                        width = cursor.getInt(widthColumn),
                        height = cursor.getInt(heightColumn),
                        dateAdded = cursor.getLong(dateAddedColumn),
                        dateModified = cursor.getLong(dateModifiedColumn),
                        folderPath = folderPath,
                        folderName = folderName,
                        thumbnailPath = contentUri.toString()
                    )
                )
            }
        }

        mediaList
    }
}
