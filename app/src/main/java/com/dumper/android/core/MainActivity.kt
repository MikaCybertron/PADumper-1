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
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.dumper.android.R
import com.dumper.android.core.DumperService.Companion.FILE_NAME
import com.dumper.android.core.DumperService.Companion.IS_FIX_NAME
import com.dumper.android.core.DumperService.Companion.IS_METADATA_NAME
import com.dumper.android.core.DumperService.Companion.LIBRARY_DIR_NAME
import com.dumper.android.core.DumperService.Companion.MSG_GETINFO
import com.dumper.android.core.DumperService.Companion.PROCESS_NAME
import com.dumper.android.databinding.ActivityMainBinding
import com.dumper.android.utils.*
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService

class MainActivity : AppCompatActivity(), Handler.Callback {
    private lateinit var mainBind: ActivityMainBinding
    private lateinit var allApps: Map<String, String>
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

        allApps = packageManager.getRunningApps()

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
                    runService(process, libName.text.toString(), autoFix.isChecked)
                } else {
                    consoleList.add("put pkg name!")
                }
            }

            selectApps.setOnClickListener {
                MaterialDialog(this@MainActivity).show {
                    title(text = "Select Your Apps")
                    listItemsSingleChoice(items = allApps.keys.toList(), waitForPositiveButton = false) { _, index, _ ->
                        processText.setText(allApps.values.toList()[index])
                        dismiss()
                    }

                    negativeButton(text = "Refresh") {
                        allApps = packageManager.getRunningApps()
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
                val intent = Intent(this, DumperService::class.java)
                RootService.bind(intent, conn)
                return true
            }
        }
        return false
    }

    private fun runService(process: String, dump_file: String?, autoFix: Boolean) {
        val message: Message = Message.obtain(null, MSG_GETINFO)

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
        val output = p0.data.getString(DumperService.DUMP_LOG, "")
        consoleList.add(output)
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        RootService.unbind(conn)
    }
}