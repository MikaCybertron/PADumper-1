package com.dumper.android.utils

const val TAG = "PADumper"
const val DEFAULT_DIR = "/sdcard/PADumper"

fun Long.longToHex(): String {
    return this.toString(16)
}