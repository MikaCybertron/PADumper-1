package com.dumper.android.utils

import androidx.lifecycle.MutableLiveData
import com.dumper.android.process.ProcessData

const val TAG = "PADumper"
const val DEFAULT_DIR = "/sdcard/PADumper"
val allApps = MutableLiveData<ArrayList<ProcessData>>()
val console = MutableLiveData<String>()

fun Long.toHex(): String {
    return this.toString(16)
}

fun Long.toMB(): Long {
    return this * 1024 * 1024
}