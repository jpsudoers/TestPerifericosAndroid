package com.vigatec.testaplicaciones.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.vigatec.testaplicaciones.R
import com.vigatec.testaplicaciones.databinding.FragmentTestDisplayYellowBinding

class  FragmentTestDisplayYellow : Fragment()

{
    private val TAG = "FragmentDisplayYellow"
    private var _binding: FragmentTestDisplayYellowBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            {

            _binding = FragmentTestDisplayYellowBinding.inflate(inflater, container, false)
                return binding.root
            }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
            {
            super.onViewCreated(view, savedInstanceState)
            binding.btnTestDisplay3Yellow.setOnClickListener{

                    val action = FragmentTestDisplayYellowDirections.actionFragmentTestDisplayYellowToFragmentTestDisplayKey()
                findNavController().navigate(action)
                    }

            }
}