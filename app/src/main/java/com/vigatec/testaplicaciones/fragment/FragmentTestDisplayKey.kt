package com.vigatec.testaplicaciones.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.vigatec.testaplicaciones.R
import com.vigatec.testaplicaciones.databinding.FragmentTestDisplayKeyBinding


class FragmentTestDisplayKey : Fragment()
{
    private val TAG = "FragmentDisplayKey"
    private var _binding: FragmentTestDisplayKeyBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            {

                _binding = FragmentTestDisplayKeyBinding.inflate(inflater, container, false)
                return binding.root

            }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)

            {
                super.onViewCreated(view, savedInstanceState)
                binding.btmTestDisplay4Key.setOnClickListener{

                        val action = FragmentTestDisplayKeyDirections.actionFragmentTestDisplayKeyToFragmentTestMagStripe()
                    findNavController().navigate(action)

                        }
            }
}