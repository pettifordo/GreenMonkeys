package com.strive4it.greenmonkeys.capture

import android.content.Context
import java.io.File
import java.util.UUID

/**
 * Owns the on-disk video files. Videos are radioactive (SPEC §5): app-private
 * storage under filesDir (inaccessible to other apps), backup is disabled
 * app-wide (`allowBackup="false"`), and nothing is ever exported or shared.
 * Ported from iOS `VideoStore`.
 */
class VideoStore(context: Context) {

    private val directory: File = File(context.filesDir, "videos").apply { mkdirs() }

    fun file(fileName: String): File = File(directory, fileName)

    /** Moves a freshly recorded temp file into private storage. Returns the stored file name. */
    fun store(temporary: File): String {
        val fileName = UUID.randomUUID().toString() + ".mp4"
        val destination = file(fileName)
        if (!temporary.renameTo(destination)) {
            temporary.copyTo(destination, overwrite = false)
            temporary.delete()
        }
        return fileName
    }

    /** A scratch file for CameraX to record into before storing. */
    fun newTempFile(): File = File(directory, "recording-${UUID.randomUUID()}.tmp")

    /** Deletion only ever happens on explicit user action (hard rule 3). */
    fun delete(fileName: String) {
        file(fileName).delete()
    }

    fun exists(fileName: String): Boolean = file(fileName).exists()
}
