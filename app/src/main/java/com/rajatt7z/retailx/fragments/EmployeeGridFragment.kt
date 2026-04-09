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
import coil.load
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
            showEmployeeDetailsDialog(employee)
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

    private fun showEmployeeDetailsDialog(employee: com.rajatt7z.retailx.models.Employee) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_employee_details, null)
        
        val imgProfile = dialogView.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.imgProfile)
        val tvName = dialogView.findViewById<android.widget.TextView>(R.id.tvName)
        val chipRole = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipRole)
        val tvEmail = dialogView.findViewById<android.widget.TextView>(R.id.tvEmail)
        val tvPhone = dialogView.findViewById<android.widget.TextView>(R.id.tvPhone)
        val tvPermissions = dialogView.findViewById<android.widget.TextView>(R.id.tvPermissions)
        val tvJoinDate = dialogView.findViewById<android.widget.TextView>(R.id.tvJoinDate)

        tvName.text = employee.name
        chipRole.text = employee.role
        tvEmail.text = employee.email.ifEmpty { "No Email" }
        tvPhone.text = employee.phone.ifEmpty { "No Phone" }
        tvPermissions.text = "${employee.permissions} Permissions"
        
        val sdf = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.getDefault())
        tvJoinDate.text = "Joined ${sdf.format(java.util.Date(employee.createdAt))}"

        if (employee.profileImageUrl.isNotEmpty()) {
            imgProfile.load(employee.profileImageUrl) {
                crossfade(true)
                placeholder(R.drawable.round_account_circle_24)
                error(R.drawable.round_account_circle_24)
            }
        } else {
            imgProfile.setImageResource(R.drawable.round_account_circle_24)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
