package com.dumper.android.dumper


import android.os.Environment
import com.dumper.android.utils.*
import java.io.File

class Fixer(private val nativeDir:String, private val files: File, private val memory: Memory) {

    fun fixDump(): String {
        val param = arrayOf(
            "${nativeDir}${File.separator}libfixer.so",
            files.absolutePath,
            memory.sAddress.longToHex(),
            "${Environment.getExternalStorageDirectory().path}/Download/${files.getOriginalName()}_fix${files.getFileExtension()}"
        )
        val proc = Runtime.getRuntime().exec(param)
        proc.waitFor()
        val res = proc.inputStream.bufferedReader().readLines()
        proc.destroy()
        return res.joinToString("\n")
    }
}