package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.adapters.EmployeesGridAdapter
import com.rajatt7z.retailx.databinding.FragmentEmployeeGridBinding
import com.rajatt7z.retailx.utils.Resource
import com.rajatt7z.retailx.viewmodel.AuthViewModel

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmployeeGridFragment : Fragment() {

    private var _binding: FragmentEmployeeGridBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var employeesAdapter: EmployeesGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmployeeGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.fabAddEmployee.setOnClickListener {
             findNavController().navigate(R.id.action_employeeGridFragment_to_addEmployeeFragment)
        }

        setupRecyclerView()
        setupSwipeRefresh()
        
        // Configure empty state
        binding.emptyState.tvEmptyTitle.text = "No Employees"
        binding.emptyState.tvEmptySubtitle.text = "Add employees to manage your store team"
        
        // Show shimmer and fetch
        showShimmer()
        viewModel.fetchEmployees()
        observeData()
    }

    private fun setupRecyclerView() {
        employeesAdapter = EmployeesGridAdapter(emptyList()) { employee ->
            Toast.makeText(requireContext(), "Selected: ${employee.name}", Toast.LENGTH_SHORT).show()
        }
        binding.rvEmployees.apply {
            adapter = employeesAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchEmployees()
        }
    }

    private fun showShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
        binding.rvEmployees.visibility = View.GONE
        binding.emptyState.emptyStateContainer.visibility = View.GONE
    }

    private fun hideShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
    }

    private fun observeData() {
        viewModel.employees.observe(viewLifecycleOwner) { resource ->
             when (resource) {
                is Resource.Success -> {
                    hideShimmer()
                    binding.swipeRefreshLayout.isRefreshing = false
                    val list = resource.data ?: emptyList()
                    employeesAdapter.updateList(list)
                    
                    if (list.isEmpty()) {
                        binding.rvEmployees.visibility = View.GONE
                        binding.emptyState.emptyStateContainer.visibility = View.VISIBLE
                    } else {
                        binding.rvEmployees.visibility = View.VISIBLE
                        binding.emptyState.emptyStateContainer.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    hideShimmer()
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading -> {
                     // Loading handled by shimmer
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
