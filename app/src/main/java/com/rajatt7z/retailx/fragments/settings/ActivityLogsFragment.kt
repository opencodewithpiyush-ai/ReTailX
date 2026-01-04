package com.rajatt7z.retailx.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rajatt7z.retailx.databinding.FragmentActivityLogsBinding

class ActivityLogsFragment : Fragment() {

    private var _binding: FragmentActivityLogsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: com.rajatt7z.retailx.viewmodel.LogsViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[com.rajatt7z.retailx.viewmodel.LogsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        
        val adapter = com.rajatt7z.retailx.adapters.LoginLogsAdapter()
        binding.rvLogs.adapter = adapter
        binding.rvLogs.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        
        viewModel.fetchLoginLogs()
        
        viewModel.logs.observe(viewLifecycleOwner) { result ->
            when (result) {
                is com.rajatt7z.retailx.utils.Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is com.rajatt7z.retailx.utils.Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    result.data?.let { logs ->
                        adapter.submitList(logs)
                    }
                }
                is com.rajatt7z.retailx.utils.Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    android.widget.Toast.makeText(requireContext(), result.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
