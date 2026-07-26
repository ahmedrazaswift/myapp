package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MotorcycleDao {
    // Bike queries
    @Query("SELECT * FROM bikes ORDER BY model ASC")
    fun getAllBikes(): Flow<List<Bike>>

    @Query("SELECT * FROM bikes WHERE UPPER(licensePlate) = UPPER(:plate) LIMIT 1")
    suspend fun getBikeByPlate(plate: String): Bike?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBike(bike: Bike)

    // Service Record queries
    @Query("SELECT * FROM service_records ORDER BY date DESC")
    fun getAllServiceRecords(): Flow<List<ServiceRecord>>

    @Query("SELECT * FROM service_records WHERE UPPER(bikePlate) = UPPER(:plate) ORDER BY date DESC")
    fun getServiceRecordsForBike(plate: String): Flow<List<ServiceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceRecord(record: ServiceRecord)

    @Delete
    suspend fun deleteServiceRecord(record: ServiceRecord)

    // Appointment queries
    @Query("SELECT * FROM appointments ORDER BY preferredDate ASC")
    fun getAllAppointments(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE UPPER(bikePlate) = UPPER(:plate) ORDER BY preferredDate DESC")
    fun getAppointmentsForBike(plate: String): Flow<List<Appointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment)

    @Query("UPDATE appointments SET status = :status WHERE id = :id")
    suspend fun updateAppointmentStatus(id: Long, status: String)

    // Service With Parts Records
    @Query("SELECT * FROM service_with_parts_records ORDER BY id DESC")
    fun getAllServiceWithPartsRecords(): Flow<List<ServiceWithPartsRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceWithPartsRecord(record: ServiceWithPartsRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceWithPartsRecords(records: List<ServiceWithPartsRecord>)

    @Delete
    suspend fun deleteServiceWithPartsRecord(record: ServiceWithPartsRecord)

    @Query("DELETE FROM service_with_parts_records")
    suspend fun clearServiceWithPartsRecords()

    // Oil History Records
    @Query("SELECT * FROM oil_history_records ORDER BY id DESC")
    fun getAllOilHistoryRecords(): Flow<List<OilHistoryRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOilHistoryRecord(record: OilHistoryRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOilHistoryRecords(records: List<OilHistoryRecord>)

    @Delete
    suspend fun deleteOilHistoryRecord(record: OilHistoryRecord)

    @Query("DELETE FROM oil_history_records")
    suspend fun clearOilHistoryRecords()

    // Service Without Parts Records
    @Query("SELECT * FROM service_without_parts_records ORDER BY id DESC")
    fun getAllServiceWithoutPartsRecords(): Flow<List<ServiceWithoutPartsRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceWithoutPartsRecord(record: ServiceWithoutPartsRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceWithoutPartsRecords(records: List<ServiceWithoutPartsRecord>)

    @Delete
    suspend fun deleteServiceWithoutPartsRecord(record: ServiceWithoutPartsRecord)

    @Query("DELETE FROM service_without_parts_records")
    suspend fun clearServiceWithoutPartsRecords()

    // Staff Members
    @Query("SELECT * FROM staff_members ORDER BY name ASC")
    fun getAllStaffMembers(): Flow<List<StaffMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffMember(staff: StaffMember)

    @Delete
    suspend fun deleteStaffMember(staff: StaffMember)

    // BikeRiderMapping
    @Query("SELECT * FROM bike_rider_mappings")
    fun getAllBikeRiderMappings(): Flow<List<BikeRiderMapping>>

    @Query("SELECT * FROM bike_rider_mappings WHERE UPPER(bikePlate) = UPPER(:plate) LIMIT 1")
    suspend fun getMappingForBike(plate: String): BikeRiderMapping?

    @Query("SELECT * FROM bike_rider_mappings WHERE UPPER(riderId) = UPPER(:riderId) LIMIT 1")
    suspend fun getMappingForRider(riderId: String): BikeRiderMapping?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBikeRiderMapping(mapping: BikeRiderMapping)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBikeRiderMappings(mappings: List<BikeRiderMapping>)

    @Query("DELETE FROM bike_rider_mappings")
    suspend fun clearBikeRiderMappings()

    // Rider Photo Uploads
    @Query("SELECT * FROM rider_photo_uploads WHERE UPPER(bikePlate) = UPPER(:plate) AND uploadMonth = :month")
    fun getPhotoUploadsForBikeAndMonth(plate: String, month: String): Flow<List<RiderPhotoUpload>>

    @Query("SELECT * FROM rider_photo_uploads WHERE UPPER(bikePlate) = UPPER(:plate) ORDER BY uploadTimestamp DESC")
    fun getPhotoUploadsForBike(plate: String): Flow<List<RiderPhotoUpload>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotoUpload(upload: RiderPhotoUpload)

    @Delete
    suspend fun deletePhotoUpload(upload: RiderPhotoUpload)
    // --- GOOGLE SHEETS SYNC & SEARCH FUNCTIONS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRecords(records: List<MotorcycleEntity>)

    @Query("DELETE FROM service_history_table")
    suspend fun clearDatabase()

    @Query("""
        SELECT * FROM service_history_table 
        WHERE bike_number LIKE :searchQuery 
        OR mechanic_name LIKE :searchQuery 
        OR voucher_number LIKE :searchQuery
        OR item_name LIKE :searchQuery
        ORDER BY voucher_date DESC
    """)
    fun searchServiceHistory(searchQuery: String): kotlinx.coroutines.flow.Flow<List<MotorcycleEntity>>
}
