package com.dumper.android.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.dumper.android.databinding.FragmentConsoleBinding
import com.dumper.android.utils.console

class ConsoleFragment : Fragment() {
    lateinit var consoleBind: FragmentConsoleBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        consoleBind = FragmentConsoleBinding.inflate(layoutInflater, container, true)
        return consoleBind.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        console.observe(viewLifecycleOwner) {
            consoleBind.console.append("$it\n")
            Handler(Looper.getMainLooper()).postDelayed({
                consoleBind.scrollView.fullScroll(View.FOCUS_DOWN) }, 10)

        }
    }
}
