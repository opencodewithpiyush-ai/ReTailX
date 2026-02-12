package com.rajatt7z.retailx.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.rajatt7z.retailx.databinding.FragmentRbacBinding
import com.rajatt7z.retailx.utils.Resource
import com.rajatt7z.retailx.viewmodel.AuthViewModel

class RBACFragment : Fragment() {

    private var _binding: FragmentRbacBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRbacBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        loadRoleCounts()
    }

    private fun loadRoleCounts() {
        viewModel.fetchEmployees()
        viewModel.employees.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                val employees = resource.data ?: emptyList()
                val roleCounts = employees.groupBy { it.role }.mapValues { it.value.size }

                updateRoleCount("Store Manager", roleCounts.getOrDefault("Store Manager", 0))
                updateRoleCount("Inventory Manager", roleCounts.getOrDefault("Inventory Manager", 0))
                updateRoleCount("Sales Executive", roleCounts.getOrDefault("Sales Executive", 0))
            }
        }
    }

    private fun updateRoleCount(roleName: String, count: Int) {
        // Find the card that contains this role and update the subtitle
        // The layout has static cards, so we search through children
        val root = binding.root as ViewGroup
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is com.google.android.material.card.MaterialCardView) {
                val cardLayout = child.getChildAt(0) as? ViewGroup ?: continue
                val titleView = cardLayout.getChildAt(0) as? TextView ?: continue
                val subtitleView = cardLayout.getChildAt(1) as? TextView ?: continue
                
                if (titleView.text.toString() == roleName) {
                    val suffix = if (count == 1) "employee" else "employees"
                    subtitleView.text = "${subtitleView.text}\n$count $suffix assigned"
                    break
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
