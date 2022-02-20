package com.dumper.android.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.dumper.android.core.MainActivity
import com.dumper.android.databinding.FragmentMemoryBinding
import com.dumper.android.utils.allApps
import com.dumper.android.utils.console

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

    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            memBinding.run {
                dumpButton.setOnClickListener {
                    getMainActivity().initRoot()
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
                    getMainActivity().initRoot()
                    getMainActivity().sendRequestAllProcess()
                }

                if (allApps.hasActiveObservers())
                    return

                allApps.observe(viewLifecycleOwner) {
                    it.sortBy { list -> list.appName }

                    val appNames = it.map { processData ->
                        val processName = processData.processName
                        if (processName.contains(":"))
                            "${processData.appName} (${processName.substringAfter(":")})"
                        else
                            processData.appName
                    }

                    MaterialDialog(requireContext()).show {
                        title(text = "Select Your Apps")

                        listItemsSingleChoice(
                            items = appNames,
                            initialSelection = 0,
                            waitForPositiveButton = false
                        ) { _, index, _ ->
                            processText.editText!!.setText(it[index].processName)
                            cancel()
                        }

                        positiveButton(text = "Refresh") {
                            getMainActivity().sendRequestAllProcess()
                            cancel()
                            selectApps.performClick()
                        }

                        negativeButton(text = "Cancel") {
                            cancel()
                        }
                    }
                }
            }
        }
    }

    private fun getMainActivity() = requireActivity() as MainActivity
}