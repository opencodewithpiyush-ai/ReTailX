package com.rajatt7z.retailx.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.rajatt7z.retailx.fragments.SalesChartPageFragment
import com.rajatt7z.retailx.models.Order

class SalesChartPagerAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {

    private val fragments = mutableMapOf<Int, SalesChartPageFragment>()
    private var orders: List<Order> = emptyList()

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        val type = when (position) {
            0 -> "Today"
            1 -> "This Week"
            else -> "This Month"
        }
        val fragment = SalesChartPageFragment.newInstance(type)
        fragment.updateData(orders) // Initial data if available
        fragments[position] = fragment
        return fragment
    }
    
    fun updateData(newOrders: List<Order>) {
        this.orders = newOrders
        fragments.values.forEach { it.updateData(newOrders) }
    }
}
