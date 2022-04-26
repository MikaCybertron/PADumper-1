package com.dumper.android.messager

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Messenger
import com.dumper.android.core.MainActivity
import com.dumper.android.ui.ConsoleFragment
import com.dumper.android.ui.MemoryFragment

class MSGConnection(private val activity: MainActivity) : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        activity.console.appendInfo("RootService: Successfully connected")
        activity.remoteMessenger = Messenger(service)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        activity.console.appendInfo("RootService: Service disconnected")
        activity.remoteMessenger = null
    }
}