package com.rajatt7z.retailx.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.rajatt7z.retailx.models.Order
import kotlinx.coroutines.tasks.await
import java.util.UUID

import javax.inject.Inject

class OrderRepository @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()
    private val ordersCollection = db.collection("orders")

    suspend fun createOrder(order: Order): Boolean {
        return try {
            val orderId = order.id.ifEmpty { UUID.randomUUID().toString() }
            val newOrder = order.copy(id = orderId, timestamp = System.currentTimeMillis())
            ordersCollection.document(orderId).set(newOrder).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getOrdersByEmployee(employeeId: String): List<Order> {
        return try {
            val snapshot = ordersCollection
                .whereEqualTo("soldBy", employeeId)
                .get()
                .await()
            snapshot.toObjects(Order::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllOrders(): List<Order> {
        return try {
            val snapshot = ordersCollection
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.toObjects(Order::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun updateOrderStatus(orderId: String, status: String): Boolean {
        return try {
            ordersCollection.document(orderId).update("status", status).await()
            true
        } catch (e: Exception) {
             false
        }
    }
}
