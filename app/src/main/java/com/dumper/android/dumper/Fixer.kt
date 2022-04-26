package com.dumper.android.dumper

import android.content.Context
import android.system.Os.chmod
import com.topjohnwu.superuser.Shell
import java.io.File

object Fixer {


    fun extractLibs(ctx: Context) {
        val libs = ctx.assets.list("SoFixer")
        libs?.forEach { lib ->
            ctx.assets.open("SoFixer/$lib").use { input ->
                File(ctx.filesDir, lib).outputStream().use { output ->
                    input.copyTo(output)
                    Shell.cmd("chmod 755 ${ctx.filesDir.absolutePath}/$lib").exec()
                }
            }
        }
    }


    fun fixDump(
        nativeDir: String, dumpFile: File,
        startAddress: String, is32: Boolean
    ): Array<List<String>> {
        val outList = mutableListOf<String>()
        val errList = mutableListOf<String>()
        Shell.cmd("'$nativeDir/${if (is32) "SoFixer32" else "SoFixer64"}' '${dumpFile.path} '$startAddress' '${dumpFile.parent}/${dumpFile.nameWithoutExtension}_fix.${dumpFile.extension}'")
            .to(outList, errList)
        return arrayOf(outList, errList)
    }
}