package com.rajatt7z.retailx.fragments


import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import com.rajatt7z.retailx.databinding.FragmentSalesChartBinding
import kotlinx.coroutines.launch

class SalesChartFragment : Fragment() {

    private var _binding: FragmentSalesChartBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSalesChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        setupToggleListeners()
        loadChartData(1) // Default to daily (matches checked btnDaily)
    }

    private fun setupChart() {
        // Bar Chart Setup
        binding.barChart.description.isEnabled = false
        binding.barChart.setTouchEnabled(true)
        binding.barChart.legend.isEnabled = true
        
        // Pie Chart Setup
        binding.pieChart.description.isEnabled = false
        binding.pieChart.isDrawHoleEnabled = true
        // binding.pieChart.setHoleColor(Color.WHITE) // Set dynamically in loadChartData
        binding.pieChart.transparentCircleRadius = 61f
    }

    private fun setupToggleListeners() {
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val days = when (checkedId) {
                    com.rajatt7z.retailx.R.id.btnDaily -> 1
                    com.rajatt7z.retailx.R.id.btnWeekly -> 7
                    com.rajatt7z.retailx.R.id.btnMonthly -> 30
                    com.rajatt7z.retailx.R.id.btnYearly -> 365
                    else -> 7
                }
                loadChartData(days)
            }
        }
    }

    private fun loadChartData(days: Int) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val repository = com.rajatt7z.retailx.repository.OrderRepository()
        val showAllOrders = arguments?.getBoolean("showAllOrders") ?: false
        
        lifecycleScope.launch {
            val allOrders = if (showAllOrders) {
                repository.getAllOrders()
            } else {
                repository.getOrdersByEmployee(userId)
            }
            
            // Filter orders within the selected time period
            val cutoffTime = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
            val orders = allOrders.filter { it.timestamp >= cutoffTime }
            
            // Choose date format based on period
            val datePattern = when {
                days <= 1 -> "HH:00"   // Hourly for daily
                days <= 7 -> "EEE"      // Day name for weekly
                days <= 31 -> "dd MMM"        // Day+month for monthly
                else -> "MMM yyyy"      // Month+year for yearly
            }
            val dateFormat = java.text.SimpleDateFormat(datePattern, java.util.Locale.getDefault())
            
            val salesMap = java.util.LinkedHashMap<String, Float>()
            orders.forEach { order ->
                val day = dateFormat.format(java.util.Date(order.timestamp))
                val current = salesMap.getOrDefault(day, 0f)
                salesMap[day] = current + order.totalPrice.toFloat()
            }
            
            val barEntries = ArrayList<com.github.mikephil.charting.data.BarEntry>()
            val labels = ArrayList<String>()
            var index = 0f
            salesMap.forEach { (key, value) ->
                barEntries.add(com.github.mikephil.charting.data.BarEntry(index, value))
                labels.add(key)
                index++
            }
            
            if (_binding == null) return@launch
            
            // Resolve Theme Colors
            val colorOnSurface = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface)
            val colorSurface = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurface)

            // Bar Chart Update
            if (barEntries.isNotEmpty()) {
                val periodLabel = when {
                    days <= 1 -> "Hourly Sales"
                    days <= 7 -> "Daily Sales"
                    days <= 31 -> "Sales by Date"
                    else -> "Monthly Sales"
                }
                val barDataSet = com.github.mikephil.charting.data.BarDataSet(barEntries, periodLabel)
                barDataSet.colors = com.github.mikephil.charting.utils.ColorTemplate.MATERIAL_COLORS.toList()
                barDataSet.valueTextColor = colorOnSurface
                barDataSet.valueTextSize = 12f
                
                binding.barChart.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
                binding.barChart.xAxis.granularity = 1f
                
                // Theme Styling for Bar Chart
                binding.barChart.xAxis.textColor = colorOnSurface
                binding.barChart.axisLeft.textColor = colorOnSurface
                binding.barChart.axisRight.textColor = colorOnSurface
                binding.barChart.legend.textColor = colorOnSurface
                binding.barChart.description.textColor = colorOnSurface
                
                val barData = com.github.mikephil.charting.data.BarData(barDataSet)
                binding.barChart.data = barData
                binding.barChart.invalidate()
                binding.barChart.animateY(1000)
            } else {
                binding.barChart.clear()
            }
            
            // Pie Chart - Sales by Product Name (Top 5)
            val productSales = HashMap<String, Float>()
            orders.forEach { 
                val current = productSales.getOrDefault(it.productName, 0f)
                productSales[it.productName] = current + it.totalPrice.toFloat()
            }
            
            val pieEntries = ArrayList<com.github.mikephil.charting.data.PieEntry>()
            productSales.entries.sortedByDescending { it.value }.take(5).forEach {
                pieEntries.add(com.github.mikephil.charting.data.PieEntry(it.value, it.key))
            }
            
            if (pieEntries.isNotEmpty()) {
                val pieDataSet = com.github.mikephil.charting.data.PieDataSet(pieEntries, "Top Products")
                pieDataSet.colors = com.github.mikephil.charting.utils.ColorTemplate.JOYFUL_COLORS.toList()
                pieDataSet.valueTextColor = Color.WHITE // Inside chart slices, white usually looks best with colors
                pieDataSet.valueTextSize = 14f
    
                // Theme Styling for Pie Chart
                binding.pieChart.setHoleColor(colorSurface)
                binding.pieChart.setEntryLabelColor(Color.WHITE) // Inside slices
                binding.pieChart.legend.textColor = colorOnSurface
                binding.pieChart.description.textColor = colorOnSurface

                val pieData = com.github.mikephil.charting.data.PieData(pieDataSet)
                binding.pieChart.data = pieData
                binding.pieChart.invalidate()
                binding.pieChart.animateXY(1000, 1000)
            } else {
                binding.pieChart.clear()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
