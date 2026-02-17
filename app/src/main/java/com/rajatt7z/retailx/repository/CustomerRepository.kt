package com.rajatt7z.retailx.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.rajatt7z.retailx.models.Customer
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CustomerRepository {

    private val db = FirebaseFirestore.getInstance()
    private val customersCollection = db.collection("customers")

    suspend fun addCustomer(customer: Customer): String {
        val id = customer.id.ifEmpty { UUID.randomUUID().toString() }
        val newCustomer = customer.copy(id = id, createdAt = System.currentTimeMillis())
        customersCollection.document(id).set(newCustomer).await()
        return id
    }

    suspend fun updateCustomer(customer: Customer) {
        customersCollection.document(customer.id).set(customer).await()
    }

    suspend fun deleteCustomer(customerId: String) {
        customersCollection.document(customerId).delete().await()
    }

    suspend fun getCustomer(customerId: String): Customer? {
        return customersCollection.document(customerId).get().await().toObject(Customer::class.java)
    }

    suspend fun getAllCustomers(): List<Customer> {
        return try {
            customersCollection.orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await().toObjects(Customer::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchByPhone(phone: String): List<Customer> {
        return try {
            customersCollection
                .whereGreaterThanOrEqualTo("phone", phone)
                .whereLessThanOrEqualTo("phone", phone + "\uf8ff")
                .get().await().toObjects(Customer::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchByName(name: String): List<Customer> {
        return try {
            val lowerName = name.lowercase()
            val allCustomers = getAllCustomers()
            allCustomers.filter { it.name.lowercase().contains(lowerName) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun incrementStats(customerId: String, amount: Double) {
        if (customerId.isEmpty()) return
        try {
            customersCollection.document(customerId).update(
                mapOf(
                    "totalOrders" to com.google.firebase.firestore.FieldValue.increment(1),
                    "totalSpent" to com.google.firebase.firestore.FieldValue.increment(amount)
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
