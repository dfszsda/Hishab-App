package com.example.hisabapp.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.hisabapp.R
import com.example.hisabapp.ui.TransactionAdapter
import com.example.hisabapp.ui.TransactionDiffCallback
import com.example.hisabapp.databinding.ItemTransactionBinding
import com.example.hisabapp.data.model.Transaction
import java.util.Locale
import java.text.NumberFormat

@Suppress("DEPRECATION")
class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(
    TransactionDiffCallback()
) {

    class ViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transaction) {
            binding.nameText.text = transaction.name ?: "Unknown"
            binding.mobileText.text = transaction.mobileNumber ?: "N/A"
            binding.amountText.text = String.format(Locale.US,"$%.2f", transaction.amount)
            binding.categoryText.text = transaction.category
            binding.dateText.text = transaction.date
            binding.typeText.text = transaction.type

            val indianLocale = Locale("en", "IN") // Locale for English in India
            val currencyFormatter = NumberFormat.getCurrencyInstance(indianLocale)
            binding.amountText.text = currencyFormatter.format(transaction.amount)

            binding.categoryText.text = transaction.category
            binding.dateText.text = transaction.date
            binding.typeText.text = transaction.type

            // Set type color
            when (transaction.type) {
                "Income" -> {
                    binding.typeText.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.income_color))
                    binding.amountText.setTextColor(ContextCompat.getColor(binding.root.context, R.color.income_color))
                }
                "Expense" -> {
                    binding.typeText.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.expense_color))
                    binding.amountText.setTextColor(ContextCompat.getColor(binding.root.context, R.color.expense_color))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
    override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem == newItem
    }
}