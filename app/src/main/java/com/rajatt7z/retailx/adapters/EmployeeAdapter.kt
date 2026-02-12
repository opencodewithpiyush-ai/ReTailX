package com.rajatt7z.retailx.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rajatt7z.retailx.databinding.ItemEmployeeBinding
import com.rajatt7z.retailx.models.Employee

class EmployeeAdapter(
    private val onEmployeeClick: (Employee) -> Unit,
    private val onDeleteClick: (Employee) -> Unit
) : RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder>() {

    inner class EmployeeViewHolder(val binding: ItemEmployeeBinding) : RecyclerView.ViewHolder(binding.root)

    private val diffCallback = object : DiffUtil.ItemCallback<Employee>() {
        override fun areItemsTheSame(oldItem: Employee, newItem: Employee): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: Employee, newItem: Employee): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, diffCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val binding = ItemEmployeeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EmployeeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        val employee = differ.currentList[position]
        holder.binding.apply {
            tvEmployeeName.text = employee.name
            tvEmployeeRole.text = employee.role
            tvEmployeePhone.text = employee.phone
            
            // Clear tint to show actual image colors
            ivAvatar.imageTintList = null
            
            if (employee.profileImageUrl.isNotEmpty()) {
                coil.ImageLoader(root.context).enqueue(
                    coil.request.ImageRequest.Builder(root.context)
                        .data(employee.profileImageUrl)
                        .target(ivAvatar)
                        .placeholder(com.rajatt7z.retailx.R.drawable.baseline_person_24)
                        .error(com.rajatt7z.retailx.R.drawable.baseline_person_24)
                        .build()
                )
            } else {
                 ivAvatar.setImageResource(com.rajatt7z.retailx.R.drawable.baseline_person_24)
                 // Re-apply tint for default icon if desired, or just leave it
                 ivAvatar.setColorFilter(android.graphics.Color.GRAY, android.graphics.PorterDuff.Mode.SRC_IN)
            }
            
            root.setOnClickListener {
                onEmployeeClick(employee)
            }
            
            btnDelete.setOnClickListener {
                onDeleteClick(employee)
            }
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}
