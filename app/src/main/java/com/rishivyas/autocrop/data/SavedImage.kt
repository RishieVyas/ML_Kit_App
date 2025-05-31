package com.rishivyas.autocrop.data

import android.graphics.Bitmap
import java.io.File
import java.util.Date

/**
 * Data class representing a saved image in the history
 */
data class SavedImage(
    val file: File,
    val name: String = file.name,
    val dateCreated: Date = Date(file.lastModified()),
    val thumbnail: Bitmap? = null
) 