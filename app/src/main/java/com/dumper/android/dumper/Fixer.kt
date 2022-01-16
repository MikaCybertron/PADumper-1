package com.dumper.android.dumper

import java.io.File

class Fixer(private val nativeDir:String, private val files: File, private val startAddress: String) {

    fun fixDump(): String {
        val param = arrayOf(
            "${nativeDir}/libfixer.so",
            files.absolutePath,
            startAddress,
            "${files.parent}/${files.nameWithoutExtension}_fix.${files.extension}"
        )
        val proc = Runtime.getRuntime().exec(param)
        proc.waitFor()
        val res = proc.inputStream.bufferedReader().readLines()
        proc.destroy()
        return res.joinToString("\n")
    }
}