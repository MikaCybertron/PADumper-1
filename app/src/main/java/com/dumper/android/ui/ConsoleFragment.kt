package com.dumper.android.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import com.dumper.android.databinding.FragmentConsoleBinding
import com.dumper.android.ui.viewmodel.ConsoleViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConsoleFragment : Fragment() {
    companion object {
        val instance by lazy { ConsoleFragment() }
    }

    private lateinit var consoleBind: FragmentConsoleBinding
    private val vm: ConsoleViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        consoleBind = FragmentConsoleBinding.inflate(layoutInflater, container, false)
        return consoleBind.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.console.observe(viewLifecycleOwner) {
            consoleBind.console.append("$it\n")
            vm.viewModelScope.launch {
                delay(10)
                consoleBind.scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }
    }
}
