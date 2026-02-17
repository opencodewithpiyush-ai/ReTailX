package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rajatt7z.retailx.adapters.DetailedOrderAdapter
import com.rajatt7z.retailx.databinding.FragmentInventoryManagerBinding
import com.rajatt7z.retailx.repository.OrderRepository
import com.rajatt7z.retailx.repository.AuthRepository
import kotlinx.coroutines.launch

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InventoryManagerFragment : Fragment() {

    private var _binding: FragmentInventoryManagerBinding? = null
    private val binding get() = _binding!!
    
    @Inject
    lateinit var repository: OrderRepository
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    private lateinit var adapter: DetailedOrderAdapter

    private var employeeMap = mapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupHeader()
        setupRecyclerView()
        setupSwipeRefresh()
        
        binding.btnSmartReorder.setOnClickListener {
            findNavController().navigate(com.rajatt7z.retailx.R.id.action_inventoryManagerFragment_to_smartReorderFragment)
        }
        
        // Configure empty state
        binding.emptyState.tvEmptyTitle.text = "No Orders to Process"
        binding.emptyState.tvEmptySubtitle.text = "Orders requiring processing will appear here"
        
        showShimmer()
        loadOrders()
    }

    private fun setupHeader() {
        val headerView = binding.tvHeader
        val profileView = binding.ivProfile
        val roleTitle = "Inventory Manager"

        // Greeting Logic
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
        headerView.text = "$greeting $roleTitle"

        // Profile Click
        profileView.setOnClickListener {
            findNavController().navigate(com.rajatt7z.retailx.R.id.action_inventoryManagerFragment_to_profile)
        }

        // Fetch Name
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: roleTitle
                        headerView.text = "$greeting\n$name"
                        
                        val profileImageUrl = document.getString("profileImageUrl")
                        if (!profileImageUrl.isNullOrEmpty()) {
                            coil.ImageLoader(requireContext()).enqueue(
                                coil.request.ImageRequest.Builder(requireContext())
                                    .data(profileImageUrl)
                                    .target(profileView)
                                    .placeholder(com.rajatt7z.retailx.R.drawable.round_account_circle_24)
                                    .error(com.rajatt7z.retailx.R.drawable.round_account_circle_24)
                                    .build()
                            )
                        }
                    }
                }
        }
    }

    private fun setupRecyclerView() {
        adapter = DetailedOrderAdapter(emptyList(), emptyMap(), 
            onActionClick = { order ->
                markOrderProcessed(order)
            },
            onDownloadClick = { order ->
                val employeeName = employeeMap[order.soldBy] ?: "Store Employee"
                com.rajatt7z.retailx.utils.PdfGenerator.generateOrderPdf(requireContext(), order, employeeName)
            }
        )
        binding.rvOrders.layoutManager = LinearLayoutManager(context)
        binding.rvOrders.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        val typedValue = android.util.TypedValue()
        val theme = requireContext().theme
        
        // Resolve colorPrimary for the progress spinner
        theme.resolveAttribute(com.google.android.material.R.attr.colorTertiary, typedValue, true)
        val primaryColor = typedValue.data
        
        // Resolve colorSurfaceContainer for the background (fallback to colorSurface)
        // Note: colorSurfaceContainer is available in newer Material versions, checking existence or fallback
        val backgroundAttr = com.google.android.material.R.attr.colorSurfaceContainer
        val hasSurfaceContainer = theme.resolveAttribute(backgroundAttr, typedValue, true)
        
        if (!hasSurfaceContainer) {
             theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        }
        val backgroundColor = typedValue.data

        binding.swipeRefreshLayout.setProgressBackgroundColorSchemeColor(backgroundColor)
        binding.swipeRefreshLayout.setColorSchemeColors(primaryColor)

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadOrders()
        }
    }

    private fun showShimmer() {
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
        binding.rvOrders.visibility = View.GONE
        binding.emptyState.emptyStateContainer.visibility = View.GONE
    }

    private fun hideShimmer() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
    }

    private fun loadOrders() {
        lifecycleScope.launch {
            try {
                val orders = repository.getAllOrders()
                val employeesResult = authRepository.getEmployees()
                
                if (employeesResult is com.rajatt7z.retailx.utils.Resource.Success) {
                    employeeMap = employeesResult.data?.associate { it.uid to it.name } ?: emptyMap()
                    adapter.updateEmployeeMap(employeeMap)
                }
                
                if (_binding != null) {
                    hideShimmer()
                    binding.swipeRefreshLayout.isRefreshing = false
                    adapter.updateList(orders)
                    
                    if (orders.isEmpty()) {
                        binding.rvOrders.visibility = View.GONE
                        binding.emptyState.emptyStateContainer.visibility = View.VISIBLE
                    } else {
                        binding.rvOrders.visibility = View.VISIBLE
                        binding.emptyState.emptyStateContainer.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    hideShimmer()
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                Toast.makeText(context, "Failed to load orders", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun markOrderProcessed(order: com.rajatt7z.retailx.models.Order) {
         lifecycleScope.launch {
            val success = repository.updateOrderStatus(order.id, "Processed")
            if (success) {
                Toast.makeText(context, "Order Processed", Toast.LENGTH_SHORT).show()
                loadOrders()
            } else {
                Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
