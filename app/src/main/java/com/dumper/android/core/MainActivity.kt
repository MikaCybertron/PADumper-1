package com.dumper.android.core

import android.content.ComponentName
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.ServiceConnection
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.Menu
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
import com.dumper.android.dumper.Fixer
import com.dumper.android.process.ProcessData
import com.dumper.android.ui.ConsoleFragment
import com.dumper.android.ui.MemoryFragment
import com.dumper.android.utils.TAG
import com.dumper.android.utils.allApps
import com.dumper.android.utils.console
import com.topjohnwu.superuser.ipc.RootService

class MainActivity : AppCompatActivity(), Handler.Callback {
    private lateinit var mainBind: ActivityMainBinding
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

        initService()

        mainBind.apply {
            setContentView(root)
            setSupportActionBar(toolbar)

            supportFragmentManager.commit {
                replace(R.id.contentContainer, MemoryFragment.instance)
            }

            bottomBar.setOnItemSelectedListener {
                supportFragmentManager.commit {
                    setCustomAnimations(
                        R.anim.fade_in,
                        R.anim.fade_out,
                        R.anim.fade_in,
                        R.anim.fade_out
                    )
                    replace(R.id.contentContainer,
                        when (it.itemId) {
                            R.id.action_memory -> MemoryFragment.instance
                            R.id.action_console -> ConsoleFragment.instance
                            else -> throw IllegalArgumentException("Unknown item selected")
                        }
                    )
                    addToBackStack(null)
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

    private fun initService() {
        Fixer.extractLibs(this)
        if (remoteMessenger == null) {
            serviceQueued = true
            val intent = Intent(this, RootServices::class.java)
            RootService.bind(intent, conn)
        }
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
                putString(LIBRARY_DIR_NAME, "${filesDir.path}/SoFixer")
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
                if (allProcess != null) {
                    MemoryFragment.instance.showProcess(allProcess)
                }
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