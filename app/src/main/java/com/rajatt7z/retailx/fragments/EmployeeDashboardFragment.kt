package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rajatt7z.retailx.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmployeeDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_employee_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val ivProfile = view.findViewById<View>(R.id.ivProfile)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: "Employee"
                        
                        val calendar = java.util.Calendar.getInstance()
                        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                        
                        val greeting = when (hour) {
                            in 6..11 -> "Good Morning"
                            in 12..17 -> "Good Afternoon"
                            in 18..23 -> "Good Evening"
                            else -> "Good Night"
                        }
                        
                        tvWelcome.text = "$greeting\n$name !"
                    }
                }
        }

        ivProfile.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_profile)
        }
    }
}
