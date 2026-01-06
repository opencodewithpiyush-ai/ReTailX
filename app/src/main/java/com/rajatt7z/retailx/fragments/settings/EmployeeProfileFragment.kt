package com.rajatt7z.retailx.fragments.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.auth.MainActivity

class EmployeeProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_employee_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val etName = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etName)
        val etEmail = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etEmail)
        val etPhone = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPhone)
        val chipRole = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipRole)
        val chipPermission = view.findViewById<com.google.android.material.chip.Chip>(R.id.chipPermission)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        toolbar.setNavigationOnClickListener {
             findNavController().navigateUp()
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val email = document.getString("email") ?: ""
                        val name = document.getString("name") ?: ""
                        val phone = document.getString("phone") ?: ""
                        // Fetch generic role (Store Manager, etc.) and permission (Viewer, Editor)
                        val role = document.getString("role")
                        val permission = document.getString("permissions")

                        etName.setText(name)
                        etEmail.setText(email)
                        etPhone.setText(phone)
                        
                        // Set text or hide if empty
                        if (!role.isNullOrEmpty()) {
                            chipRole.text = role
                            chipRole.visibility = View.VISIBLE
                        } else {
                            chipRole.visibility = View.GONE
                        }

                        if (!permission.isNullOrEmpty()) {
                            chipPermission.text = permission
                            chipPermission.visibility = View.VISIBLE
                        } else {
                            chipPermission.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener {
                   // Handle error if needed, maybe show a toast or a snackbar
                   etName.setText("Error loading details")
                }
        }

        btnLogout.setOnClickListener {
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
}
