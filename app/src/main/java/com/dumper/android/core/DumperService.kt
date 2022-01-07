package com.dumper.android.core

import android.content.Intent
import android.os.*
import android.util.Log
import com.dumper.android.dumper.Dumper
import com.dumper.android.utils.*
import com.topjohnwu.superuser.ipc.RootService
import java.lang.StringBuilder

class DumperService : RootService(), Handler.Callback {
    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "MSGService: onBind")
        val h = Handler(Looper.getMainLooper(), this)
        val m = Messenger(h)
        return m.binder
    }

    override fun handleMessage(msg: Message): Boolean {
        if (msg.what == MSG_STOP) {
            stopSelf()
            return false
        }
        if (msg.what != MSG_GETINFO)
            return false

        val requestData = msg.data
        val logOutput = StringBuilder()
        val process = requestData.getString(PROCESS_NAME)
        val fileName = requestData.getString(FILE_NAME, "libil2cpp.so")
        val isAutoFix = requestData.getBoolean(IS_FIX_NAME, false)
        val isDumpMetadata = requestData.getBoolean(IS_METADATA_NAME, false)
        val nativeDir = requestData.getString(LIBRARY_DIR_NAME)

        val reply = Message.obtain()
        val data = Bundle()
        if (process != null) {
            val dumper = Dumper(process)
            dumper.file = fileName
            logOutput.appendLine(dumper.dumpFile(isAutoFix, nativeDir))
            if (isDumpMetadata) {
                dumper.file = "global-metadata.dat"
                logOutput.appendLine(dumper.dumpFile(isAutoFix, nativeDir))
            }
            data.putString(DUMP_LOG, logOutput.toString())
        }else{
            data.putString(DUMP_LOG, "Data Error!")
        }
        reply.data = data
        try {
            msg.replyTo.send(reply)
        } catch (e: RemoteException) {
            Log.e(TAG, "Remote error", e)
        }
        return false
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "MSGService: onUnbind, client process unbound")
        return false
    }

    companion object {
        const val MSG_GETINFO = 1
        const val MSG_STOP = 2
        const val DUMP_LOG = "DumpLog"
        const val PROCESS_NAME = "PROCESS"
        const val FILE_NAME = "FILE"
        const val IS_FIX_NAME = "FIXME"
        const val IS_METADATA_NAME = "DUMP_METADATA"
        const val LIBRARY_DIR_NAME = "NATIVE_DIR"
    }
}