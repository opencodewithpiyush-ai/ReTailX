package com.rajatt7z.retailx.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rajatt7z.retailx.models.Refund
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RefundRepository {

    private val db = FirebaseFirestore.getInstance()
    private val refundsCollection = db.collection("refunds")

    suspend fun saveRefund(refund: Refund): Boolean {
        return try {
            val refundId = refund.id.ifEmpty { UUID.randomUUID().toString() }
            val newRefund = refund.copy(id = refundId, timestamp = System.currentTimeMillis())
            refundsCollection.document(refundId).set(newRefund).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getRefundsByBill(billId: String): List<Refund> {
        return try {
            val snapshot = refundsCollection
                .whereEqualTo("originalBillId", billId)
                .get()
                .await()
            snapshot.toObjects(Refund::class.java).sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllRefunds(): List<Refund> {
        return try {
            val snapshot = refundsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            snapshot.toObjects(Refund::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
