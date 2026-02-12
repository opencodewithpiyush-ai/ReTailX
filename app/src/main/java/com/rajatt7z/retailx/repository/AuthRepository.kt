package com.rajatt7z.retailx.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rajatt7z.retailx.utils.Resource
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun registerUser(
        email: String,
        password: String,
        userMap: HashMap<String, Any>,
        userId: String? = null
    ): Resource<String> {
        return try {
            val uid = userId ?: auth.createUserWithEmailAndPassword(email, password).await().user?.uid
            if (uid != null) {
                userMap["uid"] = uid
                db.collection("users").document(uid).set(userMap).await()
                Resource.Success("Registration Successful")
            } else {
                Resource.Error("Failed to get User ID")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unknown error occurred")
        }
    }
    
    // Create employee without logging out the current admin
    suspend fun createEmployeeAccount(
        email: String, 
        password: String, 
        userMap: HashMap<String, Any>
    ): Resource<String> {
        return try {
            // 1. Initialize a secondary Firebase App
            val appOptions = com.google.firebase.FirebaseOptions.Builder()
                .setApiKey(auth.app.options.apiKey)
                .setApplicationId(auth.app.options.applicationId)
                .setProjectId(auth.app.options.projectId)
                .build()

            val secondaryApp = try {
                 com.google.firebase.FirebaseApp.getInstance("SecondaryApp")
            } catch (e: Exception) {
                 com.google.firebase.FirebaseApp.initializeApp(auth.app.applicationContext, appOptions, "SecondaryApp")
            }

            // 2. Get Auth instance for this secondary app
            val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

            // 3. Create the user on this secondary instance
            val result = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid

            if (uid != null) {
                // 4. Use the PRIMARY Firestore (auth as Admin) to write the data
                // We use 'db' which is the main instance. Admin should have write permission to "users" collection.
                // NOTE: Firestore rules must allow authenticated users to write to 'users' collection.
                userMap["uid"] = uid
                db.collection("users").document(uid).set(userMap).await()
                
                // 5. Sign out the secondary instance to be clean
                secondaryAuth.signOut()
                
                Resource.Success("Employee Added Successfully")
            } else {
                secondaryAuth.signOut()
                Resource.Error("Failed to generate Employee ID")
            }
        } catch (e: Exception) {
            Resource.Error("Failed to add employee: ${e.message}")
        }
    }

    suspend fun loginUser(email: String, password: String): Resource<String> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Check if user is disabled
                val userDetailsResult = getUserDetails(currentUser.uid)
                if (userDetailsResult is Resource.Success) {
                    val disabled = userDetailsResult.data?.get("disabled") as? Boolean ?: false
                    if (disabled) {
                        auth.signOut()
                        return Resource.Error("This account has been disabled. Contact your administrator.")
                    }
                    val userType = userDetailsResult.data?.get("userType") as? String ?: "Unknown"
                    val userEmail = userDetailsResult.data?.get("email") as? String ?: email
                    saveLoginLog(currentUser.uid, userType, userEmail)
                }
            }
            Resource.Success("Login Successful")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login Failed")
        }
    }
    
    private suspend fun saveLoginLog(userId: String, userType: String, email: String) {
        try {
            val logId = db.collection("login_logs").document().id
            val log = hashMapOf(
                "logId" to logId,
                "userId" to userId,
                "userType" to userType,
                "email" to email,
                "timestamp" to System.currentTimeMillis(),
                "deviceName" to android.os.Build.MODEL
            )
            db.collection("login_logs").document(logId).set(log).await()
        } catch (e: Exception) {
            e.printStackTrace() // Log silently, don't fail login
        }
    }

    suspend fun getLoginLogs(): Resource<List<com.rajatt7z.retailx.models.LoginLog>> {
        return try {
            val snapshot = db.collection("login_logs")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val logs = snapshot.toObjects(com.rajatt7z.retailx.models.LoginLog::class.java)
            Resource.Success(logs)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch logs")
        }
    }
    
    suspend fun getUserDetails(uid: String): Resource<Map<String, Any>> {
        return try {
            val document = db.collection("users").document(uid).get().await()
            if (document.exists()) {
                Resource.Success(document.data ?: emptyMap())
            } else {
                Resource.Error("User details not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch user details")
        }
    }

    fun getCurrentUser() = auth.currentUser

    suspend fun getEmployees(): Resource<List<com.rajatt7z.retailx.models.Employee>> {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("userType", "employee")
                .get()
                .await()
            val employees = snapshot.toObjects(com.rajatt7z.retailx.models.Employee::class.java)
            Resource.Success(employees)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch employees")
        }
    }

    suspend fun updateEmployee(uid: String, updates: Map<String, Any>): Resource<String> {
        return try {
            db.collection("users").document(uid).update(updates).await()
            Resource.Success("Employee updated successfully")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update employee")
        }
    }

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Resource<String> {
        return try {
            db.collection("users").document(uid).update(updates).await()
            Resource.Success("Profile Updated Successfully")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile")
        }
    }

    suspend fun deleteEmployee(uid: String): Resource<String> {
        return try {
            // Mark user as disabled before deletion so they can't log in
            // even if Firestore delete succeeds but Auth delete isn't possible from client
            db.collection("users").document(uid)
                .update("disabled", true).await()
            
            // Delete the Firestore user document
            db.collection("users").document(uid).delete().await()
            
            // NOTE: Firebase Auth account cannot be deleted from the client SDK
            // for another user. To fully remove the Auth account, deploy a
            // Cloud Function triggered on Firestore document deletion:
            //   exports.onUserDeleted = functions.firestore
            //     .document('users/{uid}')
            //     .onDelete((snap, context) => admin.auth().deleteUser(context.params.uid));
            
            Resource.Success("Employee deleted successfully")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete employee")
        }
    }

    suspend fun resetPassword(email: String): Resource<String> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Resource.Success("Password reset email sent")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send reset email")
        }
    }



    fun logout() = auth.signOut()
}
