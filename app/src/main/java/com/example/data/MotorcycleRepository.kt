package com.example.data

import kotlinx.coroutines.flow.Flow

class MotorcycleRepository(private val dao: MotorcycleDao) {
    val allBikes: Flow<List<Bike>> = dao.getAllBikes()
    val allServiceRecords: Flow<List<ServiceRecord>> = dao.getAllServiceRecords()
    val allAppointments: Flow<List<Appointment>> = dao.getAllAppointments()

    // Service with parts records
    val allServiceWithPartsRecords: Flow<List<ServiceWithPartsRecord>> = dao.getAllServiceWithPartsRecords()
    
    suspend fun insertServiceWithPartsRecord(record: ServiceWithPartsRecord) = dao.insertServiceWithPartsRecord(record)
    suspend fun insertServiceWithPartsRecords(records: List<ServiceWithPartsRecord>) = dao.insertServiceWithPartsRecords(records)
    suspend fun deleteServiceWithPartsRecord(record: ServiceWithPartsRecord) = dao.deleteServiceWithPartsRecord(record)
    suspend fun clearServiceWithPartsRecords() = dao.clearServiceWithPartsRecords()

    // Oil history records
    val allOilHistoryRecords: Flow<List<OilHistoryRecord>> = dao.getAllOilHistoryRecords()
    
    suspend fun insertOilHistoryRecord(record: OilHistoryRecord) = dao.insertOilHistoryRecord(record)
    suspend fun insertOilHistoryRecords(records: List<OilHistoryRecord>) = dao.insertOilHistoryRecords(records)
    suspend fun deleteOilHistoryRecord(record: OilHistoryRecord) = dao.deleteOilHistoryRecord(record)
    suspend fun clearOilHistoryRecords() = dao.clearOilHistoryRecords()

    // Service without parts records
    val allServiceWithoutPartsRecords: Flow<List<ServiceWithoutPartsRecord>> = dao.getAllServiceWithoutPartsRecords()
    
    suspend fun insertServiceWithoutPartsRecord(record: ServiceWithoutPartsRecord) = dao.insertServiceWithoutPartsRecord(record)
    suspend fun insertServiceWithoutPartsRecords(records: List<ServiceWithoutPartsRecord>) = dao.insertServiceWithoutPartsRecords(records)
    suspend fun deleteServiceWithoutPartsRecord(record: ServiceWithoutPartsRecord) = dao.deleteServiceWithoutPartsRecord(record)
    suspend fun clearServiceWithoutPartsRecords() = dao.clearServiceWithoutPartsRecords()

    // Staff Members
    val allStaffMembers: Flow<List<StaffMember>> = dao.getAllStaffMembers()
    
    suspend fun insertStaffMember(staff: StaffMember) = dao.insertStaffMember(staff)
    suspend fun deleteStaffMember(staff: StaffMember) = dao.deleteStaffMember(staff)

    suspend fun getBikeByPlate(plate: String): Bike? = dao.getBikeByPlate(plate)

    suspend fun insertBike(bike: Bike) = dao.insertBike(bike)

    fun getServiceRecordsForBike(plate: String): Flow<List<ServiceRecord>> {
        return dao.getServiceRecordsForBike(plate)
    }

    suspend fun insertServiceRecord(record: ServiceRecord) = dao.insertServiceRecord(record)

    suspend fun deleteServiceRecord(record: ServiceRecord) = dao.deleteServiceRecord(record)

    fun getAppointmentsForBike(plate: String): Flow<List<Appointment>> {
        return dao.getAppointmentsForBike(plate)
    }

    suspend fun insertAppointment(appointment: Appointment) = dao.insertAppointment(appointment)

    suspend fun updateAppointmentStatus(id: Long, status: String) {
        dao.updateAppointmentStatus(id, status)
    }

    val allBikeRiderMappings: Flow<List<BikeRiderMapping>> = dao.getAllBikeRiderMappings()
    suspend fun getMappingForBike(plate: String): BikeRiderMapping? = dao.getMappingForBike(plate)
    suspend fun getMappingForRider(riderId: String): BikeRiderMapping? = dao.getMappingForRider(riderId)
    suspend fun insertBikeRiderMapping(mapping: BikeRiderMapping) = dao.insertBikeRiderMapping(mapping)
    suspend fun insertBikeRiderMappings(mappings: List<BikeRiderMapping>) = dao.insertBikeRiderMappings(mappings)
    suspend fun clearBikeRiderMappings() = dao.clearBikeRiderMappings()

    // Rider Photo Uploads
    fun getPhotoUploadsForBikeAndMonth(plate: String, month: String): Flow<List<RiderPhotoUpload>> = 
        dao.getPhotoUploadsForBikeAndMonth(plate, month)
    fun getPhotoUploadsForBike(plate: String): Flow<List<RiderPhotoUpload>> =
        dao.getPhotoUploadsForBike(plate)
    suspend fun insertPhotoUpload(upload: RiderPhotoUpload) = dao.insertPhotoUpload(upload)
    suspend fun deletePhotoUpload(upload: RiderPhotoUpload) = dao.deletePhotoUpload(upload)

    // Exposes the live database search filter stream
    fun searchRecords(query: String): Flow<List<MotorcycleEntity>> {
        val formattedQuery = "%$query%"
        return dao.searchServiceHistory(formattedQuery)
    }
}
