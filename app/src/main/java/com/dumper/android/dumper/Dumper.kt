package com.dumper.android.dumper

import androidx.core.text.isDigitsOnly
import com.dumper.android.utils.DEFAULT_DIR
import com.dumper.android.utils.Memory
import com.dumper.android.utils.toHex
import com.dumper.android.utils.toMB
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.ByteBuffer

class Dumper(private val pkg: String) {
    private val mem = Memory(pkg)
    var file: String = ""

    /**
     * Dump the memory to a file
     *
     * @param autoFix if true, the dumped file will be fixed after dumping
     * @param nativeDir require if autoFix is true, the native library directory
     * @return log of the dump
     */
    fun dumpFile(autoFix: Boolean, nativeDir: String?): String {
        val log = StringBuilder()
        try {
            getProcessID()

            log.appendLine("PID : ${mem.pid}")
            parseMap()
            if (mem.eAddress < mem.sAddress) {
                throw Exception("Failed parsing startAddress & endAddress")
            }

            mem.size = mem.eAddress - mem.sAddress
            log.appendLine("FILE : $file")
            log.appendLine("Start Address : ${mem.sAddress.toHex()}")
            log.appendLine("End Address : ${mem.eAddress.toHex()}")
            log.appendLine("Size Memory : ${mem.size.toHex()}")

            if (mem.sAddress > 1L && mem.eAddress > 1L) {
                val path = File("$DEFAULT_DIR/$pkg")
                if (!path.exists()) path.mkdirs()

                val pathOut = File("${path.absolutePath}/${mem.sAddress.toHex()}-$file")
                val outputStream = pathOut.outputStream()

                val inputAccess = RandomAccessFile("/proc/${mem.pid}/mem", "r")
                inputAccess.channel.run {
                    // Check if mem.size under 500MB
                    if (mem.size < 500L.toMB()) {
                        val buffer = ByteBuffer.allocate(mem.size.toInt())
                        read(buffer, mem.sAddress)
                        outputStream.write(buffer.array())
                        close()
                    } else {
                        throw Exception("Size of memory is too big")
                    }
                }

                outputStream.flush()
                inputAccess.close()
                outputStream.close()

                if (!file.contains(".dat") && autoFix) {
                    log.appendLine("Fixing...")
                    val is32bit = mem.sAddress.toHex().length == 8
                    val fixer = Fixer.fixDump(nativeDir!!, pathOut, mem.sAddress.toHex(), is32bit)
                    // Check output fixer and error fixer
                    if (fixer[0].isNotEmpty()) {
                        log.appendLine("Fixer output : \n${fixer[0].joinToString("\n")}")
                    }
                    if (fixer[1].isNotEmpty()) {
                        log.appendLine("Fixer error : \n${fixer[1].joinToString("\n")}")
                    }
                }
                log.appendLine("Done: ${pathOut.absolutePath}")
            }
        } catch (e: Exception) {
            log.appendLine(e.stackTraceToString())
            e.printStackTrace()
        }
        return log.toString()
    }

    /**
     * Parsing the memory map
     *
     * @throws FileNotFoundException if required file is not found in memory map
     */
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

            if (startAddr == null) {
                throw Exception("Start Address not found")
            } else if (endAddr == null) {
                throw Exception("End Address not found")
            } else {
                regex.find(startAddr)!!.value.run {
                    val result = split("-")
                    mem.sAddress = result[0].toLong(16)
                }
                regex.find(endAddr)!!.value.run {
                    val result = split("-")
                    mem.eAddress = result[1].toLong(16)
                }
            }
        } else {
            throw FileNotFoundException("Failed To Open : ${files.path}")
        }
    }

    /**
     * Get the process ID
     *
     * @throws Exception if failed to get the process ID
     * @throws FileNotFoundException if "/proc" failed to open
     */
    private fun getProcessID() {
        val dProc = File("/proc")
        if (dProc.exists()) {
            val dPID = dProc.listFiles()
            if (dPID.isNullOrEmpty()) {
                throw Exception("Failed To Open : ${dProc.path}")
            }
            for (line in dPID) {
                if (line.name.isDigitsOnly()) {
                    val cmdline = File("${line.path}/cmdline")
                    if (cmdline.exists()) {
                        val textCmd = cmdline.readText()
                        if (textCmd.contains(pkg)) {
                            mem.pid = line.name.toInt()
                            break
                        }
                    }
                }
            }
        } else {
            throw FileNotFoundException("Failed To Open : ${dProc.path}")
        }
    }
}

