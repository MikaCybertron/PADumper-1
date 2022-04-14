package com.dumper.android.dumper

import android.content.Context
import android.system.Os.chmod
import com.topjohnwu.superuser.Shell
import java.io.File

class Fixer(
    private val nativeDir: String,
    private val dumpFile: File,
    private val startAddress: String
) {

    companion object {
        fun extractLibs(ctx: Context) {
            val libs = ctx.assets.list("SoFixer")
            libs?.forEach { lib ->
                ctx.assets.open("SoFixer/$lib").use { input ->
                    File(ctx.filesDir, lib).outputStream().use { output ->
                        input.copyTo(output)
                        chmod(ctx.filesDir.absolutePath + "/" + lib,755)
                    }
                }
            }
        }
    }

    fun fixDump(is32: Boolean): Array<List<String>> {
        val outList = mutableListOf<String>()
        val errList = mutableListOf<String>()

        Shell.cmd("'$nativeDir/${if (is32) "SoFixer32" else "SoFixer64"}' '${dumpFile.path} '$startAddress' '${dumpFile.parent}/${dumpFile.nameWithoutExtension}_fix.${dumpFile.extension}'")
            .to(outList)
        return arrayOf(outList, errList)
    }
}