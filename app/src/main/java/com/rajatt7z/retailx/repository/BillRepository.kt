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

    suspend fun getBillById(billId: String): Bill? {
        return try {
            val doc = billsCollection.document(billId).get().await()
            doc.toObject(Bill::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllBills(): List<Bill> {
        return try {
            val snapshot = billsCollection
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.toObjects(Bill::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchBills(query: String): List<Bill> {
        return try {
            val allBills = getAllBills()
            allBills.filter { bill ->
                bill.id.lowercase().contains(query.lowercase()) ||
                bill.customerPhone.contains(query) ||
                bill.customerName.lowercase().contains(query.lowercase())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateBillStatus(billId: String, status: String): Boolean {
        return try {
            billsCollection.document(billId).update("status", status).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
