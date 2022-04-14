package com.dumper.android.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.dumper.android.R
import com.dumper.android.core.MainActivity
import com.dumper.android.databinding.FragmentMemoryBinding
import com.dumper.android.process.ProcessData
import com.dumper.android.utils.allApps
import com.dumper.android.utils.console
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MemoryFragment : Fragment() {
    companion object {
        val instance by lazy { MemoryFragment() }
    }

    private lateinit var memBinding: FragmentMemoryBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        memBinding = FragmentMemoryBinding.inflate(inflater, container, false)
        return memBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        memBinding.apply {
            dumpButton.setOnClickListener {
                val process = processText.editText!!.text.toString()
                if (process.isNotBlank()) {
                    console.value = "=========================="
                    console.value = "Process : $process"

                    val listDump = mutableListOf(libName.editText!!.text.toString())
                    if (metadata.isChecked)
                        listDump.add("global-metadata.dat")

                    getMainActivity().sendRequestDump(
                        process,
                        listDump.toTypedArray(),
                        autoFix.isChecked
                    )
                } else {
                    console.value = "put pkg name!"
                }
            }

            selectApps.setOnClickListener {
                getMainActivity().sendRequestAllProcess()
            }
        }
    }

    fun showProcess(list: ArrayList<ProcessData>) {
        list.sortBy { lists -> lists.appName }

        val appNames = list.map { processData ->
            val processName = processData.processName
            if (processName.contains(":"))
                "${processData.appName} (${processName.substringAfter(":")})"
            else
                processData.appName
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select process")
            .setSingleChoiceItems(appNames.toTypedArray(), -1) { dialog, which ->
                memBinding.processText.editText!!.setText(list[which].processName)
                dialog.dismiss()
            }
            .show()

    }

    private fun getMainActivity() = requireActivity() as MainActivity
}