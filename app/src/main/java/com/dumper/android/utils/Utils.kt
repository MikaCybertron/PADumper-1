package com.dumper.android.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ApplicationInfo.FLAG_STOPPED
import android.content.pm.ApplicationInfo.FLAG_SYSTEM
import android.content.pm.PackageManager
import java.io.File


const val TAG = "PADumper"

fun Long.longToHex(): String {
    return Integer.toHexString(this.toInt())
}

fun File.getOriginalName(): String {
    var fileName = ""
    try {
        if (exists()) {
            fileName = name.replaceFirst("[.][^.]+$".toRegex(), "")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        fileName = ""
    }
    return fileName
}

fun File.getFileExtension(): String {
    val lastIndexOf = name.lastIndexOf(".")
    return if (lastIndexOf == -1) {
        "" // empty extension
    } else name.substring(lastIndexOf)
}

fun Context.getRunningApps(): Map<String, String> {
    val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
    val appFinal = apps.filter { !it.isInvalid() }
    return appFinal.associate { Pair(packageManager.getApplicationLabel(it).toString(), it.packageName) }
}

fun ApplicationInfo.isInvalid(): Boolean {
    return (flags and FLAG_STOPPED != 0) || (flags and FLAG_SYSTEM != 0)
}
