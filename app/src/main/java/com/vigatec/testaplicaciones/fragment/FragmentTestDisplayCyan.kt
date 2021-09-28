package com.vigatec.testaplicaciones.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.vigatec.testaplicaciones.databinding.FragmentTestDisplayCyanBinding

class FragmentTestDisplayCyan : Fragment()
{

    private val TAG = "FragmentTestDisplayCyan"
    private var _binding: FragmentTestDisplayCyanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentTestDisplayCyanBinding.inflate(inflater,container,false)
        return binding.root
             }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
                {
                        super.onViewCreated(view, savedInstanceState)
                        binding.btnTestDisplay1Cyan.setOnClickListener{
                        val action = FragmentTestDisplayCyanDirections.actionFragmentTestDisplayCyanToFragmentTestDisplayMagenta()
                            findNavController().navigate(action)

                    }

                }

}

