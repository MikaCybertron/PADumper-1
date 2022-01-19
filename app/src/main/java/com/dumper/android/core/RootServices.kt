package com.dumper.android.core

import android.content.Intent
import android.os.*
import android.util.Log
import com.dumper.android.dumper.Dumper
import com.dumper.android.process.Process
import com.dumper.android.utils.TAG
import com.topjohnwu.superuser.ipc.RootService


class RootServices : RootService(), Handler.Callback {
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
        val reply = Message.obtain()
        val data = Bundle()

        val reqType = requestData.getString(REQ_TYPE)

        if (reqType == IS_ALL_PROCESS){

            val process = Process(this).getAllProcess()
            data.putString(REQ_TYPE, IS_ALL_PROCESS)
            data.putParcelableArrayList(LIST_ALL_PROCESS, process)

        } else if (reqType == IS_DUMP) {

            val logOutput = StringBuilder()
            val process = requestData.getString(PROCESS_NAME)
            val fileName = requestData.getString(FILE_NAME, "libil2cpp.so")
            val isAutoFix = requestData.getBoolean(IS_FIX_NAME, false)
            val isDumpMetadata = requestData.getBoolean(IS_METADATA_NAME, false)
            val nativeDir = requestData.getString(LIBRARY_DIR_NAME)
            if (process != null) {
                val dumper = Dumper(process)
                dumper.file = fileName
                logOutput.appendLine(dumper.dumpFile(isAutoFix, nativeDir))

                if (isDumpMetadata) {
                    dumper.file = "global-metadata.dat"
                    logOutput.appendLine(dumper.dumpFile(false, null))
                }

                data.putString(DUMP_LOG, logOutput.toString())
            } else {
                data.putString(DUMP_LOG, "Data Error!")
            }

            data.putString(REQ_TYPE, IS_DUMP)
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
        const val REQ_TYPE = "REQ_TYPE"
        const val IS_DUMP = "IS_DUMP"
        const val IS_ALL_PROCESS = "IS_ALL_PROCESS"
        const val LIST_ALL_PROCESS = "LIST_ALL_PROCESS"
        const val PROCESS_NAME = "PROCESS"
        const val FILE_NAME = "FILE"
        const val IS_FIX_NAME = "FIXME"
        const val IS_METADATA_NAME = "DUMP_METADATA"
        const val LIBRARY_DIR_NAME = "NATIVE_DIR"
    }
}