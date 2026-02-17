package com.rajatt7z.retailx.fragments.settings

import  android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rajatt7z.retailx.databinding.FragmentChangePasswordBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import androidx.fragment.app.viewModels
import com.rajatt7z.retailx.viewmodel.AuthViewModel
import com.rajatt7z.retailx.utils.Resource

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangePasswordFragment : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        
        // Check for Reset Mode
        if (arguments?.getBoolean("IS_RESET_MODE") == true) {
            binding.etCurrentPassword.visibility = View.GONE
            binding.etCurrentPassword.isEnabled = false
            binding.toolbar.title = "Reset Password"
            binding.btnUpdatePassword.text = "Reset Password"
        }
        
        binding.btnUpdatePassword.setOnClickListener {
            updatePassword()
        }
    }

    private fun updatePassword() {
        val isResetMode = arguments?.getBoolean("IS_RESET_MODE") == true
        val currentPw = binding.etCurrentPassword.text.toString().trim()
        val newPw = binding.etNewPassword.text.toString().trim()
        val confirmPw = binding.etConfirmPassword.text.toString().trim()

        if (!isResetMode && currentPw.isEmpty()) {
            Toast.makeText(context, "Please enter current password", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPw.isEmpty() || confirmPw.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPw != confirmPw) {
            Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (newPw.length < 6) {
             Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
             return
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            if (isResetMode) {
                // Directly update password without re-authentication for reset mode
                // Note: This requires a recent login session. If the session is old, it will fail.
                user.updatePassword(newPw).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        Toast.makeText(context, "Password reset successfully", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        val exception = updateTask.exception
                        if (exception is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                            Toast.makeText(context, "Please logout and login again to reset password", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Reset failed: ${exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                // Standard flow with re-authentication
                if (user.email != null) {
                    val credential = EmailAuthProvider.getCredential(user.email!!, currentPw)
                    
                    user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            user.updatePassword(newPw).addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                                    findNavController().navigateUp()
                                } else {
                                    Toast.makeText(context, "Update failed: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Authentication failed: Incorrect current password", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
