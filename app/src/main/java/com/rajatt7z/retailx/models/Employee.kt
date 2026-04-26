package com.rajatt7z.retailx.models

data class Employee(
    val uid: String = "",
    val name: String = "", // Full Name
    val email: String = "", // Optional
    val phone: String = "",
    val role: String = "", // Designation
    val department: String = "",
    val userType: String = "employee",
    val profileImageUrl: String = "", // Base64 or URL
    val createdAt: Long = 0L,
    val retailxId: String = "",
    val uan: String = "",
    val password: String = "",
    val isTempPsswd: Boolean = true,
    
    // Additional Personal Details
    val panCard: String = "",
    val dob: String = "",
    val gender: String = "",
    val doj: String = "", // Date of Joining
    
    // Address Details
    val address: EmployeeAddress = EmployeeAddress(),
    
    // Bank Details
    val bankAccNo: String = "",
    val ifscCode: String = "",
    
    // Optional Details
    val emergencyContact: String = "",
    val bloodGroup: String = "",
    val nomineeDetails: String = ""
)

data class EmployeeAddress(
    val houseNo: String = "",
    val street: String = "",
    val landmark: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = ""
)
