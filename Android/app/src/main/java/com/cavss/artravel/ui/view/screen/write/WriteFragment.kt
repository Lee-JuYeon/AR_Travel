package com.cavss.artravel.ui.view.screen.write

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cavss.artravel.databinding.FragmentTravelBinding
import com.cavss.artravel.databinding.FragmentWriteBinding

class WriteFragment : Fragment() {
    private lateinit var binding : FragmentWriteBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentWriteBinding.inflate(inflater, container, false)
        binding.run {

        }
        return binding.root
    }
}