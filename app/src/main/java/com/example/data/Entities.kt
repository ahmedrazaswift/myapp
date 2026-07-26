package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bikes")
data class Bike(
    @PrimaryKey val licensePlate: String, // e.g. "ABC-1234" (always uppercase)
    val model: String,                    // e.g. "Yamaha YZF-R1"
    val ownerName: String,
    val ownerPhone: String,
    val year: Int = 2024,
    val currentMileage: Int = 0
)

@Entity(tableName = "service_records")
data class ServiceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bikePlate: String,                 // references Bike.licensePlate
    val type: String,                      // "SERVICE_WITH_PARTS", "OIL_CHANGE", "SERVICE_WITHOUT_PARTS"
    val partsDetails: String?,             // list of parts used
    val cost: Double,
    val odometer: Int,
    val notes: String,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bikePlate: String,
    val riderName: String,
    val riderPhone: String,
    val serviceType: String,               // "SERVICE_WITH_PARTS", "OIL_CHANGE", "SERVICE_WITHOUT_PARTS"
    val preferredDate: Long,
    val notes: String,
    val status: String = "PENDING",        // "PENDING", "CONFIRMED", "CANCELLED", "COMPLETED"
    val bookingType: String = "STANDARD",   // "STANDARD", "MANUAL"
    val appointmentNumber: String = ""
)

@Entity(tableName = "service_with_parts_records")
data class ServiceWithPartsRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serviceType: String = "",
    val sku: String = "",
    val vchNo: String = "",
    val vchDate: String = "",
    val partNo: String = "",
    val itemName: String = "",
    val voucherType: String = "",
    val garage: String = "",
    val bikeNo: String = "",
    val division: String = "",
    val mechanic: String = "",
    val quantity: Double = 0.0
)

@Entity(tableName = "oil_history_records")
data class OilHistoryRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: String = "",
    val bikeNumber: String = "",
    val kilometer: Int = 0,
    val nextService: String = "",
    val company: String = "",
    val remarks: String = "",
    val blankColumn: String = "",
    val mechanic: String = ""
)

@Entity(tableName = "service_without_parts_records")
data class ServiceWithoutPartsRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String = "",
    val talabatId: String = "",
    val bikeNo: String = "",
    val kmRun: Int = 0,
    val jobCard: String = "",
    val jobDone: String = "",
    val serviceType: String = "",
    val mechanic: String = "",
    val company: String = ""
)

@Entity(tableName = "staff_members")
data class StaffMember(
    @PrimaryKey val name: String,
    val shift: String = "Shift A", // "Shift A" or "Shift B"
    val designation: String = "Mechanic", // "Mechanic", "Supervisor", "Storekeeper", "Admin", "Cleaner"
    val weeklyOff: String = "None" // "None", "Sunday", "Monday", etc.
)

@Entity(tableName = "bike_rider_mappings")
data class BikeRiderMapping(
    @PrimaryKey val bikePlate: String, // e.g. "MH-12-AB-1234" (always uppercase)
    val riderId: String,               // e.g. "R-101"
    val riderName: String              // e.g. "Alex Mercer"
)

@Entity(tableName = "rider_photo_uploads")
data class RiderPhotoUpload(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bikePlate: String,
    val uploadMonth: String, // format "YYYY-MM"
    val photoUri: String,
    val uploadTimestamp: Long,
    val rating: Double? = null,
    val assessmentSummary: String? = null
)

data class FirebaseEngineOilRecord(
    val dateOfService: String = "",
    val bikeNumber: String = "",
    val kilometer: String = "",
    val nextService: String = "",
    val remarks: String = ""
)

data class FirebaseBikeVisitRecord(
    val date: String = "",
    val talabatId: String = "",
    val bikeNo: String = "",
    val kmRun: String = "",
    val jobCard: String = "",
    val jobDone: String = "",
    val serviceType: String = "",
    val mechanic: String = ""
)

data class FirebaseExportedReportRecord(
    val type: String = "",
    val vchNo: String = "",
    val vchDate: String = "",
    val itemName: String = "",
    val bikeNo: String = "",
    val mechanic: String = ""
)

data class FirebaseBikeData(
    val bikeNumber: String,
    val engineOilList: List<FirebaseEngineOilRecord> = emptyList(),
    val bikeVisitsList: List<FirebaseBikeVisitRecord> = emptyList(),
    val exportedReportsList: List<FirebaseExportedReportRecord> = emptyList()
)

data class ServiceQueueItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bikeNumber: String = "",
    val entryTimeMillis: Long = System.currentTimeMillis(),
    val status: String = "QUEUED", // "QUEUED", "READY", "COMPLETED"
    val readyTimeMillis: Long? = null,
    val completionTimeMillis: Long? = null
)


