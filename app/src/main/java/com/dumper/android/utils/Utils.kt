package com.dumper.android.utils

import android.content.pm.ApplicationInfo
import android.content.pm.ApplicationInfo.FLAG_STOPPED
import android.content.pm.ApplicationInfo.FLAG_SYSTEM
import android.content.pm.PackageManager
import com.topjohnwu.superuser.ShellUtils

const val TAG = "PADumper"
const val DEFAULT_DIR = "/sdcard/PADumper"

fun Long.longToHex(): String {
    return this.toString(16)
}