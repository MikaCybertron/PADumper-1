package com.dumper.android.dumper

import com.dumper.android.utils.DEFAULT_DIR
import com.dumper.android.utils.Memory
import com.dumper.android.utils.longToHex
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.ByteBuffer

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
                throw IndexOutOfBoundsException("Failed parsing startAddress & endAddress")
            }

            mem.size = mem.eAddress - mem.sAddress
            log.appendLine("FILE : $file")
            log.appendLine("Start Address : ${mem.sAddress.longToHex()}")
            log.appendLine("End Address : ${mem.eAddress.longToHex()}")
            log.appendLine("Size Memory : ${mem.size.longToHex()}")

            if (mem.sAddress > 1L && mem.eAddress > 1L) {
                val path = File("$DEFAULT_DIR/$pkg")
                if (!path.exists()) path.mkdirs()

                val pathOut = File("${path.absolutePath}/${mem.sAddress.longToHex()}-$file")
                val outputStream = pathOut.outputStream()

                val inputAccess = RandomAccessFile("/proc/${mem.pid}/mem", "r")
                inputAccess.channel.run {
                    val buffer = ByteBuffer.allocate(mem.size.toInt())
                    read(buffer, mem.sAddress)
                    outputStream.write(buffer.array())
                    close()
                }

                outputStream.flush()
                inputAccess.close()
                outputStream.close()

                if (autoFix) {
                    log.appendLine("Fixing...")
                    log.appendLine(Fixer(nativeDir!!, pathOut, mem.sAddress.longToHex()).fixDump())
                }
                log.appendLine("Done: ${pathOut.absolutePath}")
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
            val startAddr = lines.find {
                if (file == "global-metadata.dat")
                    it.contains(file)
                else
                    //libil2cpp startAddress must r-xp
                    it.contains("r-xp") && it.contains(file)
            }
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

