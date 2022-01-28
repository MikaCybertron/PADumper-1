package com.dumper.android.core

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.ServiceConnection
import android.net.Uri
import android.os.*
import android.util.Log
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.dumper.android.core.RootServices.Companion.FILE_NAME
import com.dumper.android.core.RootServices.Companion.IS_ALL_PROCESS
import com.dumper.android.core.RootServices.Companion.IS_DUMP
import com.dumper.android.core.RootServices.Companion.IS_FIX_NAME
import com.dumper.android.core.RootServices.Companion.IS_METADATA_NAME
import com.dumper.android.core.RootServices.Companion.LIBRARY_DIR_NAME
import com.dumper.android.core.RootServices.Companion.MSG_GETINFO
import com.dumper.android.core.RootServices.Companion.PROCESS_NAME
import com.dumper.android.core.RootServices.Companion.REQ_TYPE
import com.dumper.android.databinding.ActivityMainBinding
import com.dumper.android.process.ProcessData
import com.dumper.android.utils.*
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService

class MainActivity : AppCompatActivity(), Handler.Callback {
    private lateinit var mainBind: ActivityMainBinding
    private val allApps = MutableLiveData<ArrayList<ProcessData>>()
    private var remoteMessenger: Messenger? = null
    private val myMessenger = Messenger(Handler(Looper.getMainLooper(), this))
    private val conn = MSGConnection()
    private var serviceQueued = false

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBind = ActivityMainBinding.inflate(layoutInflater)

        if (!initRoot()) {
            Toast.makeText(this, "Please Grant Permission Root!", Toast.LENGTH_SHORT).show()
            return
        }

        with(mainBind) {
            setContentView(root)
            github.setOnClickListener {
                val intent = Intent(ACTION_VIEW, Uri.parse("https://github.com/BryanGIG/PADumper"))
                startActivity(intent)
            }

            dumpButton.setOnClickListener {
                initRoot()
                val process = processText.text.toString()
                if (process.isNotBlank()) {
                    consoleList.add("Process : $process")
                    sendRequestDump(process, libName.text.toString(), autoFix.isChecked)
                } else {
                    consoleList.add("put pkg name!")
                }
            }

            selectApps.setOnClickListener {
                initRoot()
                sendRequestAllProcess()
            }

            allApps.observe(this@MainActivity) {
                it.sortBy { list -> list.appName }

                val appNames = it.map { processData ->
                    if (processData.processName.contains(":")) {
                        "${processData.appName} (${processData.processName.substringAfter(":")})"
                    } else
                        processData.appName
                }

                MaterialDialog(this@MainActivity).show {
                    title(text = "Select Your Apps")
                    listItemsSingleChoice(
                        items = appNames,
                        waitForPositiveButton = false
                    ) { _, index, _ ->
                        processText.setText(it[index].processName)
                        dismiss()
                    }

                    negativeButton(text = "Refresh") {
                        sendRequestAllProcess()
                        dismiss()
                        selectApps.performClick()
                    }
                }
            }
        }
    }

    private fun initRoot(): Boolean {
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

    private fun sendRequestAllProcess() {
        val message: Message = Message.obtain(null, MSG_GETINFO)

        message.data.putString(REQ_TYPE, IS_ALL_PROCESS)
        message.replyTo = myMessenger
        try {
            remoteMessenger?.send(message)
        } catch (e: RemoteException) {
            Log.e(TAG, "Remote error", e)
        }
    }

    private fun sendRequestDump(process: String, dump_file: String?, autoFix: Boolean) {
        val message: Message = Message.obtain(null, MSG_GETINFO)

        message.data.putString(REQ_TYPE, IS_DUMP)
        message.data.putString(PROCESS_NAME, process)
        message.data.putString(FILE_NAME, dump_file)
        message.data.putBoolean(IS_METADATA_NAME, mainBind.metadata.isChecked)

        if (autoFix) {
            message.data.putBoolean(IS_FIX_NAME, true)
            message.data.putString(LIBRARY_DIR_NAME, applicationInfo.nativeLibraryDir)
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
            Log.d(TAG, "service onServiceConnected")
            remoteMessenger = Messenger(service)
            if (serviceQueued) {
                serviceQueued = false
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.d(TAG, "service onServiceDisconnected")
            consoleList.add("Root Service Disconnected!")
            remoteMessenger = null
        }
    }

    private var consoleList = object : CallbackList<String>() {
        override fun onAddElement(s: String) {
            mainBind.console.append(s)
            mainBind.console.append("\n")
            mainBind.sv.postDelayed({ mainBind.sv.fullScroll(ScrollView.FOCUS_DOWN) }, 10)
        }
    }

    override fun handleMessage(p0: Message): Boolean {
        p0.data.classLoader = this@MainActivity.classLoader
        if (p0.data.getString(REQ_TYPE) == IS_DUMP)
            consoleList.add(p0.data.getString(RootServices.DUMP_LOG, ""))
        else if (p0.data.getString(REQ_TYPE) == IS_ALL_PROCESS) {
            allApps.value = p0.data.getParcelableArrayList(RootServices.LIST_ALL_PROCESS)
        }

        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        RootService.unbind(conn)
    }
}