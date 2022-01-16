package com.dumper.android.utils

import android.content.pm.ApplicationInfo
import android.content.pm.ApplicationInfo.FLAG_STOPPED
import android.content.pm.ApplicationInfo.FLAG_SYSTEM
import android.content.pm.PackageManager

const val TAG = "PADumper"

fun Long.longToHex(): String {
    return this.toString(16)
}

fun PackageManager.getRunningApps(): Map<String, String> {
    val apps = getInstalledApplications(PackageManager.GET_META_DATA)
    val appFinal = apps.filter { !it.isInvalid() }
    return appFinal.associate { Pair(getApplicationLabel(it).toString(), it.packageName) }.toSortedMap()
}

fun ApplicationInfo.isInvalid(): Boolean {
    return (flags and FLAG_STOPPED != 0) || (flags and FLAG_SYSTEM != 0)
}
