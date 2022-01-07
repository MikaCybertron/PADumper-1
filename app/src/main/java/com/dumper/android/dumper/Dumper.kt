package com.dumper.android.dumper

import com.dumper.android.utils.Memory
import com.dumper.android.utils.longToHex
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.lang.StringBuilder
import java.nio.ByteBuffer
import java.nio.charset.Charset

/*
   An Modified Tools.kt from "https://github.com/BryanGIG/KMrite"
*/

class Dumper(private val pkg: String) {
    private val mem = Memory(pkg)
    var file: String = ""

    fun dumpFile(autoFix: Boolean, nativeDir: String?): String {
        val log = StringBuilder()
        try {
            getProcessID()
            log.appendLine("PID : ${mem.pid}")
            parseMap()
            if (mem.eAddress < mem.sAddress) {
                throw NegativeArraySizeException("Failed parsing startAddress & endAddress")
            }
            mem.size = mem.eAddress - mem.sAddress
            log.appendLine("FILE : ${file}")
            log.appendLine("Start Address : ${mem.sAddress.longToHex()}")
            log.appendLine("End Address : ${mem.eAddress.longToHex()}")
            log.appendLine("Size Memory : ${mem.size}")

            if (mem.sAddress > 1L && mem.eAddress > 1L) {
                val path = File("/sdcard/Download/$pkg")
                if (!path.exists()) path.mkdirs()
                val pathOut = File("${path.absolutePath}/${mem.sAddress.longToHex()}-$file")

                val rAcess = RandomAccessFile("/proc/${mem.pid}/mem", "r")
                val filechannel = rAcess.channel

                log.appendLine("Dumping...")
                val buff = ByteBuffer.allocate(mem.size.toInt())
                filechannel.read(buff, mem.sAddress)

                val outputStream = pathOut.outputStream()
                outputStream.write(buff.array())
                outputStream.flush()
                buff.clear()
                outputStream.close()

                if (autoFix) {
                    log.appendLine("Fixing...")
                    log.appendLine(Fixer(nativeDir!!, pathOut, mem).fixDump())
                }
                log.appendLine("Done. Saved at ${pathOut.absolutePath}")
                filechannel.close()
                rAcess.close()
            }
        } catch (e: Exception) {
            log.appendLine(e.stackTraceToString())
            e.printStackTrace()
        }
        return log.toString()
    }

    private fun parseMap() {
        val files = File("/proc/${mem.pid}/maps")
        if (files.exists()) {
            val lines = files.readLines()
            val startAddr = lines.find { it.contains(file) }
            val endAddr = lines.findLast { it.contains(file) }
            val regex = "\\p{XDigit}+-\\p{XDigit}+".toRegex()
            if (startAddr == null || endAddr == null) {
                throw FileNotFoundException("$file not found in ${files.path}")
            } else {
                regex.find(startAddr)?.value?.run {
                    val result = split("-")
                    mem.sAddress = result[0].toLong(16)
                }

                regex.find(endAddr)?.value?.run {
                    val result = split("-")
                    mem.eAddress = result[1].toLong(16)
                }
            }
        } else {
            throw FileNotFoundException("Failed To Open : ${files.path}")
        }
    }

    private fun getProcessID() {
        val process = Runtime.getRuntime().exec(arrayOf("pidof", mem.pkg))
        val reader = process.inputStream.bufferedReader()
        val buff = reader.readLines().joinToString("\n")
        reader.close()
        process.waitFor()
        process.destroy()
        if (buff.isNotBlank())
            mem.pid = buff.toInt()
        else
            throw IllegalArgumentException("Make sure your proccess package is running !\n")
    }
}

