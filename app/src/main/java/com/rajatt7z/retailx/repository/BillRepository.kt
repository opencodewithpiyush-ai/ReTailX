package com.rajatt7z.retailx.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.rajatt7z.retailx.models.Bill
import com.rajatt7z.retailx.models.Order
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BillRepository {

    private val db = FirebaseFirestore.getInstance()
    private val billsCollection = db.collection("bills")
    private val ordersCollection = db.collection("orders")
    
    suspend fun saveBill(bill: Bill): Boolean {
        return try {
            val billId = bill.id.ifEmpty { UUID.randomUUID().toString() }
            val timestamp = System.currentTimeMillis()
            val newBill = bill.copy(id = billId, timestamp = timestamp)
            
            // Save Bill
            billsCollection.document(billId).set(newBill).await()
            
            // Save each item as an Order for Inventory Manager visibility
            bill.products.forEach { item ->
                val order = Order(
                    id = UUID.randomUUID().toString(),
                    productId = item.productId,
                    productName = item.productName,
                    quantity = item.quantity,
                    totalPrice = item.totalPrice,
                    timestamp = timestamp,
                    status = "Pending",
                    soldBy = bill.generatedBy,
                    customerName = bill.customerName,
                    customerPhone = bill.customerPhone
                )
                ordersCollection.document(order.id).set(order).await()
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    suspend fun getBillsByEmployee(employeeId: String): List<Bill> {
        return try {
            val snapshot = billsCollection
                .whereEqualTo("generatedBy", employeeId)
                .get()
                .await()
            
            snapshot.toObjects(Bill::class.java).sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
