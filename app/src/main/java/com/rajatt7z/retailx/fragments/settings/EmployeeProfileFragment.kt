package com.rajatt7z.retailx.fragments.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rajatt7z.retailx.auth.MainActivity
import com.rajatt7z.retailx.databinding.FragmentEmployeeProfileBinding

class EmployeeProfileFragment : Fragment() {

    private var _binding: FragmentEmployeeProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmployeeProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (_binding == null) return@addOnSuccessListener
                    if (document != null && document.exists()) {
                        val email = document.getString("email") ?: ""
                        val name = document.getString("name") ?: ""
                        val phone = document.getString("phone") ?: ""
                        val role = document.getString("role")
                        val permission = document.getString("permissions")

                        binding.etName.setText(name)
                        binding.etEmail.setText(email)
                        binding.etPhone.setText(phone)
                        
                        if (!role.isNullOrEmpty()) {
                            binding.chipRole.text = role
                            binding.chipRole.visibility = View.VISIBLE
                        } else {
                            binding.chipRole.visibility = View.GONE
                        }

                        if (!permission.isNullOrEmpty()) {
                            binding.chipPermission.text = permission
                            binding.chipPermission.visibility = View.VISIBLE
                        } else {
                            binding.chipPermission.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener {
                    if (_binding != null) {
                        binding.etName.setText("Error loading details")
                    }
                }
        }

        binding.btnLogout.setOnClickListener {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
