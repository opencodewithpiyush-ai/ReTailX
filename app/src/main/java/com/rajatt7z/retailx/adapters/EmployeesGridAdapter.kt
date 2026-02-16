package com.rajatt7z.retailx.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.databinding.ItemEmployeeGridCardBinding
import com.rajatt7z.retailx.models.Employee

class EmployeesGridAdapter(
    private var employees: List<Employee>,
    private val onEmployeeClick: (Employee) -> Unit
) : RecyclerView.Adapter<EmployeesGridAdapter.EmployeeViewHolder>() {

    inner class EmployeeViewHolder(private val binding: ItemEmployeeGridCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(employee: Employee) {
            binding.tvEmployeeName.text = employee.name
            binding.tvEmployeeRole.text = employee.role
            
            if (employee.profileImageUrl.isNotEmpty()) {
                binding.imgEmployee.load(employee.profileImageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.baseline_person_24)
                    error(R.drawable.baseline_person_24)
                }
            } else {
                binding.imgEmployee.setImageResource(R.drawable.baseline_person_24)
            }

            binding.btnDetails.setOnClickListener {
                onEmployeeClick(employee)
            }
             binding.root.setOnClickListener {
                onEmployeeClick(employee)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val binding = ItemEmployeeGridCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EmployeeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        holder.bind(employees[position])
    }

    override fun getItemCount(): Int = employees.size

    fun updateList(newEmployees: List<Employee>) {
        employees = newEmployees
        notifyDataSetChanged()
    }
}
