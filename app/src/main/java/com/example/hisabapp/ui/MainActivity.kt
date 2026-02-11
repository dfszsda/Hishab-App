package com.example.hisabapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hisabapp.ui.TransactionAdapter
import com.example.hisabapp.databinding.ActivityMainBinding
import com.example.hisabapp.viewmodel.TransactionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: TransactionViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = TransactionAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        CoroutineScope(Dispatchers.Main).launch {
            viewModel.allTransactions.collectLatest { transactions ->
                adapter.submitList(transactions)
            }
        }

        binding.addButton.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        binding.reportButton.setOnClickListener {
            startActivity(Intent(this, ReportActivity::class.java))
        }
    }
}