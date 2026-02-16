package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayoutMediator
import com.rajatt7z.retailx.adapters.SalesChartPagerAdapter
import com.rajatt7z.retailx.adapters.TransactionsAdapter
import com.rajatt7z.retailx.databinding.FragmentSalesOverviewBinding
import com.rajatt7z.retailx.utils.Resource
import com.rajatt7z.retailx.viewmodel.OrderViewModel
import java.util.Calendar

class SalesOverviewFragment : Fragment() {

    private var _binding: FragmentSalesOverviewBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OrderViewModel by viewModels()
    private lateinit var transactionsAdapter: TransactionsAdapter
    private lateinit var chartPagerAdapter: SalesChartPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSalesOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupChartPager()
        setupRecyclerView()
        observeData()
    }

    private fun setupChartPager() {
        chartPagerAdapter = SalesChartPagerAdapter(this)
        binding.viewPagerChart.adapter = chartPagerAdapter
        binding.viewPagerChart.offscreenPageLimit = 3 // Keep all charts in memory
        
        TabLayoutMediator(binding.tabLayout, binding.viewPagerChart) { tab, position ->
            tab.text = when (position) {
                0 -> "Today"
                1 -> "This Week"
                else -> "This Month"
            }
        }.attach()
    }

    private fun setupRecyclerView() {
        transactionsAdapter = TransactionsAdapter(emptyList())
        binding.rvRecentTransactions.apply {
            adapter = transactionsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeData() {
        viewModel.orders.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val orders = resource.data ?: emptyList()
                    
                    // Filter for Today
                    val today = Calendar.getInstance()
                    val todayOrders = orders.filter {
                        val orderDate = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                        orderDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        orderDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                    }
                    val salesToday = todayOrders.sumOf { it.totalPrice }
                    binding.tvSalesToday.text = String.format("$%,.2f", salesToday)

                    // Filter for This Week (Current Week starting Monday/First Day)
                    // We want strict "Current Week" logic 
                    val currentWeekStart = Calendar.getInstance().apply {
                         set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                         set(Calendar.HOUR_OF_DAY, 0)
                         set(Calendar.MINUTE, 0)
                         set(Calendar.SECOND, 0)
                         set(Calendar.MILLISECOND, 0)
                    }
                    // Handle edge case where today is Sunday and First Day is Monday (so start of week is tomorrow?? No, previous Monday)
                    if (currentWeekStart.after(Calendar.getInstance())) {
                         currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1)
                    }
                    
                    val weekOrders = orders.filter { 
                         it.timestamp >= currentWeekStart.timeInMillis
                    }
                    val salesWeek = weekOrders.sumOf { it.totalPrice }
                    binding.tvSalesWeek.text = String.format("$%,.2f", salesWeek)

                    // Recent Transactions (Last 5)
                    transactionsAdapter.updateList(orders.take(5))
                    
                    // Update Charts
                    chartPagerAdapter.updateData(orders)
                }
                is Resource.Error -> {
                    // Handle error
                }
                is Resource.Loading -> {
                    binding.tvSalesToday.text = "Loading..."
                    binding.tvSalesWeek.text = "Loading..."
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
