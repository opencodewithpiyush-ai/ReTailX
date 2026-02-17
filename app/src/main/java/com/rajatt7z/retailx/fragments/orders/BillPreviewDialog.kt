package com.rajatt7z.retailx.fragments.orders

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.rajatt7z.retailx.R
import com.rajatt7z.retailx.models.Bill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BillPreviewDialog(
    private val bill: Bill,
    private val onGenerateClick: () -> Unit // Optional if we want to generate button inside dialog
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_bill_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup Header
        val tvDate = view.findViewById<TextView>(R.id.tvDate)
        val tvCustomer = view.findViewById<TextView>(R.id.tvCustomerName)
        val tvCustomerPhone = view.findViewById<TextView>(R.id.tvCustomerPhone)
        val tvEmployee = view.findViewById<TextView>(R.id.tvEmployeeName)
        
        val sdf = SimpleDateFormat("MMM dd, yyyy | hh:mm a", Locale.getDefault())
        tvDate.text = sdf.format(Date(bill.timestamp))
        tvCustomer.text = bill.customerName
        tvCustomerPhone.text = bill.customerPhone
        tvEmployee.text = "Served by: ${bill.generatedByName}"

        // Setup Items
        val itemsContainer = view.findViewById<LinearLayout>(R.id.llItemsContainer)
        val inflater = LayoutInflater.from(context)
        
        bill.products.forEach { item ->
            val itemView = inflater.inflate(R.layout.item_preview_line, itemsContainer, false)
            
            itemView.findViewById<TextView>(R.id.tvItemName).text = item.productName
            
            itemView.findViewById<TextView>(R.id.tvItemPrice).text = String.format("$%.2f", item.unitPrice)
            itemView.findViewById<TextView>(R.id.tvItemQty).text = item.quantity.toString()
            itemView.findViewById<TextView>(R.id.tvItemTotal).text = String.format("$%.2f", item.totalPrice)
            
            itemsContainer.addView(itemView)
        }

        // Subtotal
        view.findViewById<TextView>(R.id.tvSubtotal).text = String.format("$%.2f", bill.subtotal)

        // Discount
        if (bill.discountAmount > 0) {
            view.findViewById<View>(R.id.layoutPreviewDiscount).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvPreviewDiscount).text = String.format("-$%.2f", bill.discountAmount)
        }

        // Tax
        if (bill.taxRate > 0 && bill.taxAmount > 0) {
            view.findViewById<View>(R.id.layoutPreviewTax).visibility = View.VISIBLE
            view.findViewById<TextView>(R.id.tvPreviewTaxLabel).text = String.format("Tax (%.1f%%)", bill.taxRate)
            view.findViewById<TextView>(R.id.tvPreviewTaxAmount).text = String.format("$%.2f", bill.taxAmount)
        }

        // Grand Total
        view.findViewById<TextView>(R.id.tvGrandTotal).text = String.format("$%.2f", bill.totalAmount)

        // Close Button
        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
}
