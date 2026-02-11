package com.example.hisabapp.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.hisabapp.databinding.ActivityReportBinding
import com.example.hisabapp.viewmodel.TransactionViewModel
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate

class ReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportBinding
    private val viewModel: TransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.getTotalByType("Income").observe(this) { income ->
            viewModel.getTotalByType("Expense").observe(this) { expense ->
                val entries = listOf(
                    BarEntry(1f, income?.toFloat() ?: 0f),
                    BarEntry(2f, expense?.toFloat() ?: 0f)
                )
                val dataSet = BarDataSet(entries, "Financials")
                dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
                val barData = BarData(dataSet)
                binding.barChart.data = barData
                binding.barChart.invalidate()
            }
        }
    }
}