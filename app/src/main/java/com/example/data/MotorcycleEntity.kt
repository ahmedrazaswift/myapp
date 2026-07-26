package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_history_table")
data class MotorcycleEntity(
    @PrimaryKey(autoGenerate = true) val localId: Int = 0,
    val service_type: String,
    val sku_number: String,
    val voucher_number: String,
    val voucher_date: String,
    val part_number: String,
    val item_name: String,
    val voucher_type: String,
    val garage: String,
    val bike_number: String,
    val division: String,
    val mechanic_name: String,
    val quantity: String
)