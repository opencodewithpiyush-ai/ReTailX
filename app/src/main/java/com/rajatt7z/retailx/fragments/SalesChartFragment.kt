package com.rajatt7z.retailx.fragments


import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rajatt7z.retailx.databinding.FragmentSalesChartBinding

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
        loadChartData(7) // Default to weekly (7 days)
    }

    private fun setupChart() {
        // Bar Chart Setup
        binding.barChart.description.isEnabled = false
        binding.barChart.setTouchEnabled(true)
        binding.barChart.legend.isEnabled = true
        
        // Pie Chart Setup
        binding.pieChart.description.isEnabled = false
        binding.pieChart.isDrawHoleEnabled = true
        binding.pieChart.setHoleColor(Color.WHITE)
        binding.pieChart.transparentCircleRadius = 61f
    }

    private fun setupToggleListeners() {
        // Keeping this if we want to switch timeframes for BarChart in future
        // For now, it just reloads default data
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                loadChartData(10) // Just reload random data
            }
        }
    }

    private fun loadChartData(days: Int) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val repository = com.rajatt7z.retailx.repository.OrderRepository()
        
        lifecycleScope.launchWhenResumed {
            val orders = repository.getOrdersByEmployee(userId)
            // Group orders by day vs amount
            // This requires some date manipulation. 
            // For simplicity, let's just group by day of month for the last 7 days? 
            // Or just map all existing orders to dates.
            
            val salesMap = java.util.TreeMap<String, Float>()
            val dateFormat = java.text.SimpleDateFormat("dd", java.util.Locale.getDefault())

            orders.forEach { order ->
                val day = dateFormat.format(java.util.Date(order.timestamp))
                val current = salesMap.getOrDefault(day, 0f)
                salesMap[day] = current + order.totalPrice.toFloat()
            }
            
            val barEntries = ArrayList<com.github.mikephil.charting.data.BarEntry>()
            var index = 0f
            salesMap.forEach { (_, value) ->
                barEntries.add(com.github.mikephil.charting.data.BarEntry(index, value))
                index++
            }
            
             // Bar Chart Update
            if (barEntries.isNotEmpty()) {
                val barDataSet = com.github.mikephil.charting.data.BarDataSet(barEntries, "Daily Sales")
                barDataSet.colors = com.github.mikephil.charting.utils.ColorTemplate.MATERIAL_COLORS.toList()
                barDataSet.valueTextColor = Color.BLACK
                barDataSet.valueTextSize = 12f
                
                val barData = com.github.mikephil.charting.data.BarData(barDataSet)
                binding.barChart.data = barData
                binding.barChart.invalidate()
                binding.barChart.animateY(1000)
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
                pieDataSet.valueTextColor = Color.WHITE
                pieDataSet.valueTextSize = 14f
    
                val pieData = com.github.mikephil.charting.data.PieData(pieDataSet)
                binding.pieChart.data = pieData
                binding.pieChart.invalidate()
                binding.pieChart.animateXY(1000, 1000)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
