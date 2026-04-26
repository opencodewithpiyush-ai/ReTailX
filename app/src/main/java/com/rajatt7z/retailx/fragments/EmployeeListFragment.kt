package com.rajatt7z.retailx.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.adapters.EmployeeAdapter
import com.rajatt7z.retailx.databinding.FragmentEmployeeListBinding
import com.rajatt7z.retailx.models.Employee
import com.rajatt7z.retailx.utils.Resource
import com.rajatt7z.retailx.viewmodel.AuthViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmployeeListFragment : Fragment() {

    private var _binding: FragmentEmployeeListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private lateinit var employeeAdapter: EmployeeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmployeeListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        setupSwipeRefresh()
        setupObservers()
        
        // Configure empty state
        binding.emptyState.tvEmptyTitle.text = "No Employees"
        binding.emptyState.tvEmptySubtitle.text = "Tap + to add your first employee"
        
        // Show shimmer while loading
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.shimmerViewContainer.startShimmer()
        binding.rvEmployeeList.visibility = View.GONE
        binding.emptyState.emptyStateContainer.visibility = View.GONE
        
        viewModel.fetchEmployees()
    }

    private fun setupRecyclerView() {
        employeeAdapter = EmployeeAdapter(
            onEmployeeClick = { employee ->
                showEditEmployeeDialog(employee)
            },
            onDeleteClick = { employee ->
                showDeleteConfirmationDialog(employee)
            }
        )
        binding.rvEmployeeList.apply {
            adapter = employeeAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun showDeleteConfirmationDialog(employee: Employee) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Employee")
            .setMessage("Are you sure you want to delete ${employee.name}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteEmployee(employee.uid)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupListeners() {
        binding.fabAddEmployee.setOnClickListener {
            showAddEmployeeDialog()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchEmployees()
        }
    }

    private fun setupObservers() {
        viewModel.employees.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Show shimmer
                    binding.shimmerViewContainer.visibility = View.VISIBLE
                    binding.shimmerViewContainer.startShimmer()
                    binding.rvEmployeeList.visibility = View.GONE
                    binding.emptyState.emptyStateContainer.visibility = View.GONE
                }
                is Resource.Success -> {
                    // Hide shimmer
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false

                    val list = resource.data ?: emptyList()
                    employeeAdapter.differ.submitList(list)
                    
                    if (list.isEmpty()) {
                        binding.rvEmployeeList.visibility = View.GONE
                        binding.emptyState.emptyStateContainer.visibility = View.VISIBLE
                    } else {
                        binding.rvEmployeeList.visibility = View.VISIBLE
                        binding.emptyState.emptyStateContainer.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    // Hide shimmer
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    
                    Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        viewModel.authStatus.observe(viewLifecycleOwner) { resource ->
             when (resource) {
                is Resource.Success -> {
                    Toast.makeText(context, resource.data, Toast.LENGTH_SHORT).show()
                    viewModel.fetchEmployees() // Refresh list
                }
                is Resource.Error -> {
                     val msg = resource.message ?: "Error"
                     if (!msg.contains("Login", true)) {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                     }
                }
                else -> {}
             }
        }
    }

    private lateinit var imageUploadHelper: com.rajatt7z.retailx.utils.ImageUploadHelper
    private var currentDialogImageView: android.widget.ImageView? = null
    private var currentUploadedImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageUploadHelper = com.rajatt7z.retailx.utils.ImageUploadHelper(this, lifecycleScope) { url ->
            currentUploadedImageUrl = url
            currentDialogImageView?.let { imageView ->
                coil.ImageLoader(requireContext()).enqueue(
                    coil.request.ImageRequest.Builder(requireContext())
                        .data(url)
                        .target(imageView)
                        .placeholder(com.rajatt7z.retailx.R.drawable.round_account_circle_24)
                        .error(com.rajatt7z.retailx.R.drawable.round_account_circle_24)
                        .build()
                )
            }
        }
    }

    private fun showAddEmployeeDialog() {
        val dialog = android.app.Dialog(requireContext(), R.style.Theme_ReTailX_FullScreenDialog)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val dialogBinding = com.rajatt7z.retailx.databinding.DialogAddEmployeeBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        dialogBinding.toolbar.title = "Add New Employee"
        dialogBinding.toolbar.setNavigationOnClickListener { dialog.dismiss() }

        // Setup Spinners
        val genders = arrayOf("Male", "Female", "Other")
        val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genders)
        dialogBinding.spinnerGender.setAdapter(genderAdapter)

        val roles = arrayOf("Store Manager", "Inventory Manager", "Sales Executive", "Accountant", "Delivery Partner")
        val roleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, roles)
        dialogBinding.spinnerRole.setAdapter(roleAdapter)

        // Setup Date Pickers
        setupDatePicker(dialogBinding.etEmployeeDob)
        setupDatePicker(dialogBinding.etDoj)

        // Reset state for new employee
        currentUploadedImageUrl = null
        currentDialogImageView = dialogBinding.ivEmployeeProfile
        dialogBinding.ivEmployeeProfile.setOnClickListener { imageUploadHelper.showImagePicker() }

        dialogBinding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save -> {
                    val name = dialogBinding.etEmployeeName.text.toString().trim()
                    val phone = dialogBinding.etEmployeePhone.text.toString().trim()
                    val email = dialogBinding.etEmployeeEmail.text.toString().trim()
                    val dob = dialogBinding.etEmployeeDob.text.toString()
                    val gender = dialogBinding.spinnerGender.text.toString()
                    val pan = dialogBinding.etPanCard.text.toString().trim()
                    
                    val houseNo = dialogBinding.etHouseNo.text.toString().trim()
                    val street = dialogBinding.etStreet.text.toString().trim()
                    val landmark = dialogBinding.etLandmark.text.toString().trim()
                    val city = dialogBinding.etCity.text.toString().trim()
                    val state = dialogBinding.etState.text.toString().trim()
                    val pincode = dialogBinding.etPincode.text.toString().trim()

                    val role = dialogBinding.spinnerRole.text.toString()
                    val dept = dialogBinding.etDepartment.text.toString().trim()
                    val doj = dialogBinding.etDoj.text.toString()

                    val bankAcc = dialogBinding.etBankAccNo.text.toString().trim()
                    val ifsc = dialogBinding.etIfscCode.text.toString().trim()
                    
                    val emergency = dialogBinding.etEmergencyContact.text.toString().trim()
                    val blood = dialogBinding.etBloodGroup.text.toString().trim()
                    val nominee = dialogBinding.etNomineeDetails.text.toString().trim()
                    
                    val password = generateRandomPassword()
                    val retailxId = "RX_" + (1000..9999).random().toString() + "_" + name.take(2).uppercase()
                    val uan = (100000000000..999999999999).random().toString()

                    if (validateFields(name, phone, dob, gender, pan, houseNo, street, city, state, pincode, role, dept, doj, bankAcc, ifsc)) {
                        val address = com.rajatt7z.retailx.models.EmployeeAddress(houseNo, street, landmark, city, state, pincode)
                        val userMap: HashMap<String, Any> = hashMapOf(
                            "name" to name,
                            "phone" to phone,
                            "email" to email,
                            "dob" to dob,
                            "gender" to gender,
                            "panCard" to pan,
                            "address" to address,
                            "role" to role,
                            "department" to dept,
                            "doj" to doj,
                            "bankAccNo" to bankAcc,
                            "ifscCode" to ifsc,
                            "emergencyContact" to emergency,
                            "bloodGroup" to blood,
                            "nomineeDetails" to nominee,
                            "userType" to "employee",
                            "retailxId" to retailxId,
                            "uan" to uan,
                            "password" to password, 
                            "isTempPsswd" to true,
                            "createdAt" to System.currentTimeMillis(),
                            "profileImageUrl" to (currentUploadedImageUrl ?: "")
                        )

                        viewModel.createEmployee(email.ifEmpty { "${phone}@retailx.com" }, password, userMap)
                        
                        // Send Professional Notification via Render API
                        sendProfessionalWelcomeSMS(name, phone, retailxId, password)
                        
                        dialog.dismiss()
                    }
                    true
                }
                else -> false
            }
        }
        dialog.show()
    }

    private fun generateRandomPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#%"
        return (1..8).map { chars.random() }.joinToString("")
    }

    private fun sendProfessionalWelcomeSMS(name: String, phone: String, id: String, pass: String) {
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://retailxdev.onrender.com/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
        val service = retrofit.create(com.rajatt7z.retailx.network.NotificationService::class.java)

        val professionalMessage = """
            Dear $name,
            
            Welcome to the ReTailX family! Your professional account has been successfully set up.
            
            Access Credentials:
            --------------------------
            ReTailX ID : $id
            Temp Password: $pass
            --------------------------
            
            Security Notice:
            For your protection, this is a temporary password. You are required to reset it during your first login. Please maintain the confidentiality of your account by not sharing these credentials with anyone.
            
            Best Regards,
            Administration | Team ReTailX
        """.trimIndent()

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                service.sendNotification(com.rajatt7z.retailx.network.NotificationRequest(phone, professionalMessage))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupDatePicker(editText: android.widget.EditText) {
        editText.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(requireContext(), { _, year, month, day ->
                editText.setText("$day/${month + 1}/$year")
            }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun validateFields(vararg fields: String): Boolean {
        for (field in fields) {
            if (field.isEmpty()) return false
        }
        return true
    }

    private fun sendWelcomeNotification(name: String, phone: String) {
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://retailxdev.onrender.com/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
        val service = retrofit.create(com.rajatt7z.retailx.network.NotificationService::class.java)

        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val message = "Welcome $name to ReTailX! Your account has been created successfully."
                service.sendNotification(com.rajatt7z.retailx.network.NotificationRequest(name, message))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showEditEmployeeDialog(employee: Employee) {
        val dialog = android.app.Dialog(requireContext(), R.style.Theme_ReTailX_FullScreenDialog)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val dialogBinding = com.rajatt7z.retailx.databinding.DialogAddEmployeeBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        dialogBinding.toolbar.title = "Edit Employee"
        dialogBinding.toolbar.setNavigationOnClickListener { dialog.dismiss() }
        
        // Setup Spinners
        val genders = arrayOf("Male", "Female", "Other")
        val genderAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genders)
        dialogBinding.spinnerGender.setAdapter(genderAdapter)

        val roles = arrayOf("Store Manager", "Inventory Manager", "Sales Executive", "Accountant", "Delivery Partner")
        val roleAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, roles)
        dialogBinding.spinnerRole.setAdapter(roleAdapter)

        // Setup Date Pickers
        setupDatePicker(dialogBinding.etEmployeeDob)
        setupDatePicker(dialogBinding.etDoj)

        // Pre-fill existing data
        dialogBinding.etEmployeeName.setText(employee.name)
        dialogBinding.etEmployeePhone.setText(employee.phone)
        dialogBinding.etEmployeeEmail.setText(employee.email)
        dialogBinding.etEmployeeDob.setText(employee.dob)
        dialogBinding.spinnerGender.setText(employee.gender, false)
        dialogBinding.etPanCard.setText(employee.panCard)
        
        dialogBinding.etHouseNo.setText(employee.address.houseNo)
        dialogBinding.etStreet.setText(employee.address.street)
        dialogBinding.etLandmark.setText(employee.address.landmark)
        dialogBinding.etCity.setText(employee.address.city)
        dialogBinding.etState.setText(employee.address.state)
        dialogBinding.etPincode.setText(employee.address.pincode)

        dialogBinding.spinnerRole.setText(employee.role, false)
        dialogBinding.etDepartment.setText(employee.department)
        dialogBinding.etDoj.setText(employee.doj)

        dialogBinding.etBankAccNo.setText(employee.bankAccNo)
        dialogBinding.etIfscCode.setText(employee.ifscCode)
        
        dialogBinding.etEmergencyContact.setText(employee.emergencyContact)
        dialogBinding.etBloodGroup.setText(employee.bloodGroup)
        dialogBinding.etNomineeDetails.setText(employee.nomineeDetails)

        // SETUP state for profile image
        currentUploadedImageUrl = employee.profileImageUrl
        currentDialogImageView = dialogBinding.ivEmployeeProfile
        
        if (!currentUploadedImageUrl.isNullOrEmpty()) {
             coil.ImageLoader(requireContext()).enqueue(
                coil.request.ImageRequest.Builder(requireContext())
                    .data(currentUploadedImageUrl)
                    .target(dialogBinding.ivEmployeeProfile)
                    .placeholder(com.rajatt7z.retailx.R.drawable.round_account_circle_24)
                    .error(com.rajatt7z.retailx.R.drawable.round_account_circle_24)
                    .build()
            )
        }
        
        dialogBinding.ivEmployeeProfile.setOnClickListener { imageUploadHelper.showImagePicker() }

        dialogBinding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_save -> {
                    val name = dialogBinding.etEmployeeName.text.toString().trim()
                    val phone = dialogBinding.etEmployeePhone.text.toString().trim()
                    val email = dialogBinding.etEmployeeEmail.text.toString().trim()
                    val dob = dialogBinding.etEmployeeDob.text.toString()
                    val gender = dialogBinding.spinnerGender.text.toString()
                    val pan = dialogBinding.etPanCard.text.toString().trim()
                    
                    val houseNo = dialogBinding.etHouseNo.text.toString().trim()
                    val street = dialogBinding.etStreet.text.toString().trim()
                    val landmark = dialogBinding.etLandmark.text.toString().trim()
                    val city = dialogBinding.etCity.text.toString().trim()
                    val state = dialogBinding.etState.text.toString().trim()
                    val pincode = dialogBinding.etPincode.text.toString().trim()

                    val role = dialogBinding.spinnerRole.text.toString()
                    val dept = dialogBinding.etDepartment.text.toString().trim()
                    val doj = dialogBinding.etDoj.text.toString()

                    val bankAcc = dialogBinding.etBankAccNo.text.toString().trim()
                    val ifsc = dialogBinding.etIfscCode.text.toString().trim()
                    
                    val emergency = dialogBinding.etEmergencyContact.text.toString().trim()
                    val blood = dialogBinding.etBloodGroup.text.toString().trim()
                    val nominee = dialogBinding.etNomineeDetails.text.toString().trim()

                    if (validateFields(name, phone, dob, gender, pan, houseNo, street, city, state, pincode, role, dept, doj, bankAcc, ifsc)) {
                        val address = com.rajatt7z.retailx.models.EmployeeAddress(houseNo, street, landmark, city, state, pincode)
                        val updates = hashMapOf<String, Any>(
                            "name" to name,
                            "phone" to phone,
                            "email" to email,
                            "dob" to dob,
                            "gender" to gender,
                            "panCard" to pan,
                            "address" to address,
                            "role" to role,
                            "department" to dept,
                            "doj" to doj,
                            "bankAccNo" to bankAcc,
                            "ifscCode" to ifsc,
                            "emergencyContact" to emergency,
                            "bloodGroup" to blood,
                            "nomineeDetails" to nominee
                        )
                        
                        if (currentUploadedImageUrl != null) {
                            updates["profileImageUrl"] = currentUploadedImageUrl!!
                        }

                        viewModel.updateEmployee(employee.uid, updates)
                        dialog.dismiss()
                    } else {
                        Toast.makeText(context, "Please check all required fields", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
