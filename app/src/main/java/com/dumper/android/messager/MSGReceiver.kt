package com.dumper.android.messager


import android.os.*
import android.widget.Toast
import com.dumper.android.core.MainActivity
import com.dumper.android.core.RootServices
import com.dumper.android.process.ProcessData
import com.dumper.android.ui.ConsoleFragment
import com.dumper.android.ui.MemoryFragment

class MSGReceiver(private val activity: MainActivity) : Handler.Callback {
    override fun handleMessage(message: Message): Boolean {
        message.data.classLoader = activity.classLoader

        when (message.what) {
            RootServices.MSG_GET_PROCESS_LIST -> {
                val allProcess =
                    message.data.getParcelableArrayList<ProcessData>(RootServices.LIST_ALL_PROCESS)
                if (allProcess != null) {
                    MemoryFragment.instance.showProcess(allProcess)
                }
            }
            RootServices.MSG_DUMP_PROCESS -> {
                message.data.getString(RootServices.DUMP_LOG)?.let {
                    activity.console.append(it)
                    Toast.makeText(activity, "Dump Complete!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return false
    }
}