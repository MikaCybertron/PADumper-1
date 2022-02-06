package com.dumper.android.core

import android.content.Intent
import android.os.*
import android.util.Log
import com.afollestad.materialdialogs.utils.MDUtil.getStringArray
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
        val reply = Message.obtain()
        val data = Bundle()

        when (msg.what) {
            MSG_GET_PROCESS_LIST -> {
                val process = Process(this).getAllProcess()
                reply.what = MSG_GET_PROCESS_LIST
                data.putParcelableArrayList(LIST_ALL_PROCESS, process)
            }

            MSG_DUMP_PROCESS -> {
                val requestData = msg.data
                reply.what = MSG_DUMP_PROCESS
                val logOutput = StringBuilder()
                val process = requestData.getString(PROCESS_NAME)
                val fileName = requestData.getStringArray(LIST_FILE)
                val isAutoFix = requestData.getBoolean(IS_FIX_NAME, false)
                val nativeDir = requestData.getString(LIBRARY_DIR_NAME)
                if (process != null && fileName != null) {
                    val dumper = Dumper(process)
                    for (file in fileName.iterator()) {
                        dumper.file = file
                        logOutput.appendLine(dumper.dumpFile(isAutoFix, nativeDir))
                    }

                    data.putString(DUMP_LOG, logOutput.toString())
                } else {
                    data.putString(DUMP_LOG, "Data Error!")
                }
            }
            else -> {
                data.putString("result", "Unknown command")
            }
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
        const val MSG_DUMP_PROCESS = 1
        const val MSG_GET_PROCESS_LIST = 2
        const val DUMP_LOG = "DUMP_LOG"
        const val LIST_ALL_PROCESS = "LIST_ALL_PROCESS"
        const val PROCESS_NAME = "PROCESS"
        const val LIST_FILE = "LIST_FILE"
        const val IS_FIX_NAME = "FIX_ELF"
        const val LIBRARY_DIR_NAME = "NATIVE_DIR"
    }
}