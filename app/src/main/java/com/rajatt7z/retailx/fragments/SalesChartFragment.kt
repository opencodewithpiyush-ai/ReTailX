package com.rajatt7z.retailx.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
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

    private fun loadChartData(count: Int) {
        // Mock Data for Bar Chart
        val barEntries = ArrayList<com.github.mikephil.charting.data.BarEntry>()
        for (i in 0 until 7) {
             val value = (Math.random() * 50).toFloat() + 10
             barEntries.add(com.github.mikephil.charting.data.BarEntry(i.toFloat(), value))
        }
        val barDataSet = com.github.mikephil.charting.data.BarDataSet(barEntries, "Daily Sales")
        barDataSet.colors = com.github.mikephil.charting.utils.ColorTemplate.MATERIAL_COLORS.toList()
        barDataSet.valueTextColor = Color.BLACK
        barDataSet.valueTextSize = 12f
        
        val barData = com.github.mikephil.charting.data.BarData(barDataSet)
        binding.barChart.data = barData
        binding.barChart.invalidate()
        binding.barChart.animateY(1000)

        // Mock Data for Pie Chart
        val pieEntries = ArrayList<com.github.mikephil.charting.data.PieEntry>()
        pieEntries.add(com.github.mikephil.charting.data.PieEntry(40f, "Electronics"))
        pieEntries.add(com.github.mikephil.charting.data.PieEntry(30f, "Clothing"))
        pieEntries.add(com.github.mikephil.charting.data.PieEntry(20f, "Groceries"))
        pieEntries.add(com.github.mikephil.charting.data.PieEntry(10f, "Others"))

        val pieDataSet = com.github.mikephil.charting.data.PieDataSet(pieEntries, "")
        pieDataSet.colors = com.github.mikephil.charting.utils.ColorTemplate.JOYFUL_COLORS.toList()
        pieDataSet.valueTextColor = Color.WHITE
        pieDataSet.valueTextSize = 14f

        val pieData = com.github.mikephil.charting.data.PieData(pieDataSet)
        binding.pieChart.data = pieData
        binding.pieChart.invalidate()
        binding.pieChart.animateXY(1000, 1000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
