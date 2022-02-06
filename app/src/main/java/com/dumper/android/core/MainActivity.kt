package com.dumper.android.core

import android.content.ComponentName
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.ServiceConnection
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.dumper.android.R
import com.dumper.android.core.RootServices.Companion.IS_FIX_NAME
import com.dumper.android.core.RootServices.Companion.LIBRARY_DIR_NAME
import com.dumper.android.core.RootServices.Companion.LIST_FILE
import com.dumper.android.core.RootServices.Companion.MSG_DUMP_PROCESS
import com.dumper.android.core.RootServices.Companion.MSG_GET_PROCESS_LIST
import com.dumper.android.core.RootServices.Companion.PROCESS_NAME
import com.dumper.android.databinding.ActivityMainBinding
import com.dumper.android.process.ProcessData
import com.dumper.android.ui.ConsoleFragment
import com.dumper.android.ui.MemoryFragment
import com.dumper.android.utils.TAG
import com.dumper.android.utils.allApps
import com.dumper.android.utils.console
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService

class MainActivity : AppCompatActivity(), Handler.Callback {
    private lateinit var mainBind: ActivityMainBinding
    private lateinit var consoleFragment: ConsoleFragment
    private lateinit var memoryFragment: MemoryFragment
    private var remoteMessenger: Messenger? = null
    private val myMessenger = Messenger(Handler(Looper.getMainLooper(), this))
    private val conn = MSGConnection()
    private var serviceQueued = false

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.appbar_menu, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBind = ActivityMainBinding.inflate(layoutInflater)

        if (!initRoot()) {
            Toast.makeText(this, "Please Grant Permission Root!", Toast.LENGTH_SHORT).show()
            return
        }

        mainBind.apply {
            setContentView(root)
            setSupportActionBar(toolbar)

            memoryFragment = MemoryFragment()
            consoleFragment = ConsoleFragment()

            if (savedInstanceState == null) {
                supportFragmentManager.commit {
                    add(R.id.contentContainer, consoleFragment)
                    add(R.id.contentContainer, memoryFragment)
                }
            }

            bottomBar.setOnItemSelectedListener {
                supportFragmentManager.commit {
                    hide(
                        when (it.itemId) {
                            R.id.action_memory -> consoleFragment
                            R.id.action_console -> memoryFragment
                            else -> throw Exception("Unknown item selected")
                        }
                    )
                    show(
                        when (it.itemId) {
                            R.id.action_memory -> memoryFragment
                            R.id.action_console -> consoleFragment
                            else -> throw Exception("Unknown item selected")
                        }
                    )
                }
                true
            }

            toolbar.setOnMenuItemClickListener {
                if (it.itemId == R.id.github) {
                    val intent =
                        Intent(ACTION_VIEW, Uri.parse("https://github.com/BryanGIG/PADumper"))
                    startActivity(intent)
                }
                true
            }
        }
    }

    fun initRoot(): Boolean {
        if (Shell.rootAccess()) {
            if (remoteMessenger == null) {
                serviceQueued = true
                val intent = Intent(this, RootServices::class.java)
                RootService.bind(intent, conn)
                return true
            }
        }
        return false
    }

    fun sendRequestAllProcess() {
        val message = Message.obtain(null, MSG_GET_PROCESS_LIST)
        message.replyTo = myMessenger
        try {
            remoteMessenger?.send(message)
        } catch (e: RemoteException) {
            Log.e(TAG, "Remote error", e)
        }
    }

    fun sendRequestDump(process: String, dump_file: Array<String>, autoFix: Boolean) {
        val message = Message.obtain(null, MSG_DUMP_PROCESS)

        message.data.apply {
            putString(PROCESS_NAME, process)
            putStringArray(LIST_FILE, dump_file)
            if (autoFix) {
                putBoolean(IS_FIX_NAME, true)
                putString(LIBRARY_DIR_NAME, applicationInfo.nativeLibraryDir)
            }
        }

        message.replyTo = myMessenger
        try {
            remoteMessenger?.send(message)
        } catch (e: RemoteException) {
            Log.e(TAG, "Remote error", e)
        }
    }


    inner class MSGConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            console.value = "rootService: connected"
            remoteMessenger = Messenger(service)
            if (serviceQueued) {
                serviceQueued = false
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            console.value = "rootService: disconnected"
            remoteMessenger = null
        }
    }

    override fun handleMessage(message: Message): Boolean {
        message.data.classLoader = this@MainActivity.classLoader

        when (message.what) {
            MSG_GET_PROCESS_LIST -> {
                val allProcess =
                    message.data.getParcelableArrayList<ProcessData>(RootServices.LIST_ALL_PROCESS)
                allApps.value = allProcess
            }
            MSG_DUMP_PROCESS -> {
                val dump = message.data.getString(RootServices.DUMP_LOG)
                console.value = dump
                console.value = "=========================="
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        RootService.unbind(conn)
    }
}