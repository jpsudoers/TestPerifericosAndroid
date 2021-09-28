package com.vigatec.testaplicaciones.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.vigatec.testaplicaciones.databinding.FragmentInicioBinding


class FragmentInicio : Fragment()
{

    private val TAG = "FragmentInicio"
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?
            {

            _binding = FragmentInicioBinding.inflate(inflater, container, false)
            return binding.root

            }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
            {
                super.onViewCreated(view, savedInstanceState)
                binding.Btnstart.setOnClickListener{
                val action = FragmentInicioDirections.actionFragmentInicioToFragmentTestDisplayCyan()

            //Used for through arguments
            findNavController().navigate(action)

            //Used without arguments
            //findNavController().navigate(R.id.action_fragmentInicio_to_fragmentTestDisplayCyan)

                                                    }
            }
}
