package com.rajatt7z.retailx.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.rajatt7z.retailx.databinding.FragmentSalesChartPageBinding
import com.rajatt7z.retailx.models.Order
import java.util.Calendar
import java.util.Locale

class SalesChartPageFragment : Fragment() {

    private var _binding: FragmentSalesChartPageBinding? = null
    private val binding get() = _binding!!
    private var chartType: String = "Today"
    private var orders: List<Order> = emptyList()

    companion object {
        private const val ARG_CHART_TYPE = "chart_type"
        
        fun newInstance(chartType: String): SalesChartPageFragment {
            val fragment = SalesChartPageFragment()
            val args = Bundle()
            args.putString(ARG_CHART_TYPE, chartType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chartType = arguments?.getString(ARG_CHART_TYPE) ?: "Today"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSalesChartPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
    }

    fun updateData(newOrders: List<Order>) {
        this.orders = newOrders
        if (_binding != null) {
            processAndDisplayData()
        }
    }

    private fun setupChart() {
        binding.barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(false)
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.GRAY
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                textColor = Color.GRAY
            }
            
            axisRight.isEnabled = false
        }
        
        if (orders.isNotEmpty()) {
            processAndDisplayData()
        }
    }

    private fun processAndDisplayData() {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        when (chartType) {
            "Today" -> processTodayData(entries, labels)
            "This Week" -> processWeekData(entries, labels)
            "This Month" -> processMonthData(entries, labels)
        }

        if (entries.isEmpty()) {
             binding.barChart.clear()
             binding.barChart.setNoDataText("No sales data for $chartType")
             return
        }

        val dataSet = BarDataSet(entries, "Sales")
        dataSet.color = Color.parseColor("#4A90E2") // Or use theme color if possible
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 10f

        val barData = BarData(dataSet)
        binding.barChart.data = barData
        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.barChart.invalidate()
        binding.barChart.animateY(1000)
    }

    private fun processTodayData(entries: ArrayList<BarEntry>, labels: ArrayList<String>) {
        // Group by Hour (0-23)
        val today = Calendar.getInstance()
        val salesByHour = FloatArray(24) { 0f }
        
        orders.forEach { order ->
            val orderDate = Calendar.getInstance().apply { timeInMillis = order.timestamp }
            if (orderDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                orderDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                
                val hour = orderDate.get(Calendar.HOUR_OF_DAY)
                salesByHour[hour] += order.totalPrice.toFloat()
            }
        }

        val currentHour = today.get(Calendar.HOUR_OF_DAY)
        // Show last 6 hours or full day? key hours? 
        // Let's show all hours with data or ranges.
        // For simplicity, let's show data if > 0
        
        var index = 0f
        for (h in 0..23) {
            if (salesByHour[h] > 0 || (h in 9..20)) { // Show business hours range generally
                entries.add(BarEntry(index, salesByHour[h]))
                labels.add(String.format("%02d:00", h))
                index++
            }
        }
    }

    private fun processWeekData(entries: ArrayList<BarEntry>, labels: ArrayList<String>) {
        // Last 7 days including today
        val today = Calendar.getInstance()
        // Reset to end of today
        
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        
        // We want to show Mon-Sun or Last 7 Days
        // Let's go with Mon-Sun of current week
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) // Start of current week?
        if (canBeFuture(cal, today)) {
             cal.add(Calendar.WEEK_OF_YEAR, -1) // Go back if Monday is in future (e.g. today is Sunday and Monday is "next" on some Locales)
        }
        
        // Actually simpler: Get start of this week.
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        
        val salesByDay = HashMap<Int, Float>() // DayOfYear -> Sales
        orders.forEach { order ->
             val orderDate = Calendar.getInstance().apply { timeInMillis = order.timestamp }
             // Filter if it's in this week loop logic
             val dayOfYear = orderDate.get(Calendar.DAY_OF_YEAR)
             val currentSales = salesByDay[dayOfYear] ?: 0f
             salesByDay[dayOfYear] = currentSales + order.totalPrice.toFloat()
        }

        // Logic for "This Week" - show 7 days starting from Mon (or First day)
        val viewCal = Calendar.getInstance()
        viewCal.set(Calendar.DAY_OF_WEEK, viewCal.firstDayOfWeek)
        
        for (i in 0..6) {
             val dayOfYear = viewCal.get(Calendar.DAY_OF_YEAR)
             val sales = salesByDay[dayOfYear] ?: 0f
             
             entries.add(BarEntry(i.toFloat(), sales))
             labels.add(days[viewCal.get(Calendar.DAY_OF_WEEK) - 1])
             
             viewCal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    
    private fun canBeFuture(cal: Calendar, today: Calendar): Boolean {
         return cal.get(Calendar.YEAR) > today.get(Calendar.YEAR) || 
               (cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) > today.get(Calendar.DAY_OF_YEAR))
    }
    
     private val Calendar.dayOfYear: Int
        get() = get(Calendar.DAY_OF_YEAR)

    private fun processMonthData(entries: ArrayList<BarEntry>, labels: ArrayList<String>) {
         // Show by Weeks (Week 1, Week 2...) or 5-day intervals
         val today = Calendar.getInstance()
         val month = today.get(Calendar.MONTH)
         val year = today.get(Calendar.YEAR)
         
         val salesByDay = FloatArray(32) { 0f } // 1-31
         
         orders.forEach { order ->
             val orderDate = Calendar.getInstance().apply { timeInMillis = order.timestamp }
             if (orderDate.get(Calendar.MONTH) == month && orderDate.get(Calendar.YEAR) == year) {
                 val day = orderDate.get(Calendar.DAY_OF_MONTH)
                 salesByDay[day] += order.totalPrice.toFloat()
             }
         }
         
         // Aggregate by 5 days for clarity? Or show all days? 30 bars is tight but doable
         // Let's do every 5 days + last day
         var index = 0f
         var sum = 0f
         var count = 0
         
         for (d in 1..today.getActualMaximum(Calendar.DAY_OF_MONTH)) {
              sum += salesByDay[d]
              count++
              
              if (count == 5 || d == today.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                   entries.add(BarEntry(index, sum))
                   labels.add("${d-count+1}-$d")
                   index++
                   sum = 0f
                   count = 0
              }
         }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
