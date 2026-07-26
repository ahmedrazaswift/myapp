package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Bike::class,
        ServiceRecord::class,
        Appointment::class,
        ServiceWithPartsRecord::class,
        OilHistoryRecord::class,
        ServiceWithoutPartsRecord::class,
        StaffMember::class,
        BikeRiderMapping::class,
        RiderPhotoUpload::class,
        MotorcycleEntity::class // Added our new sync database table cleanly here
    ],
    version = 10, // Incremented to 10 for Bike currentMileage field and service overdue system
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun motorcycleDao(): MotorcycleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "advance_auto_motor_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.motorcycleDao())
                }
            }
        }

        suspend fun populateDatabase(dao: MotorcycleDao) {
            // Prepopulate some bikes with custom currentMileage to demonstrate the service alerts system
            val bike1 = Bike("MH-12-AB-1234", "Yamaha YZF-R3", "Alex Mercer", "+1 555-0192", 2022, currentMileage = 7500)
            val bike2 = Bike("DL-3S-CD-5678", "Honda CBR600RR", "Marcus Fenix", "+1 555-0143", 2021, currentMileage = 18500)
            val bike3 = Bike("CA-99-EF-9012", "Kawasaki Ninja 400", "Elena Fisher", "+1 555-0187", 2023, currentMileage = 4200)
            val bike4 = Bike("NY-23-GH-4567", "Suzuki Hayabusa", "Dom Santiago", "+1 555-0111", 2020, currentMileage = 8500)

            dao.insertBike(bike1)
            dao.insertBike(bike2)
            dao.insertBike(bike3)
            dao.insertBike(bike4)

            // Prepopulate service records for Yamaha YZF-R3 (MH-12-AB-1234)
            dao.insertServiceRecord(ServiceRecord(
                bikePlate = "MH-12-AB-1234",
                type = "OIL_CHANGE",
                partsDetails = "Motul 10W40 Full Synthetic Oil, Yamaha OEM Oil Filter",
                cost = 85.00,
                odometer = 3500,
                notes = "Scheduled oil and filter change. Chain cleaned and lubricated.",
                date = System.currentTimeMillis() - (60 * 24 * 60 * 60 * 1000L)
            ))
            dao.insertServiceRecord(ServiceRecord(
                bikePlate = "MH-12-AB-1234",
                type = "OIL_CHANGE",
                partsDetails = "Motul 10W40 Oil",
                cost = 65.00,
                odometer = 7200,
                notes = "Routine mid-season oil swap.",
                date = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L)
            ))

            dao.insertServiceRecord(ServiceRecord(
                bikePlate = "MH-12-AB-1234",
                type = "SERVICE_WITH_PARTS",
                partsDetails = "EBC Double-H Sintered Front Brake Pads, NGK Iridium Spark Plugs",
                cost = 220.00,
                odometer = 5000,
                notes = "Major tune-up. Replaced worn front brake pads and installed performance spark plugs. Idle throttle adjusted.",
                date = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            ))

            dao.insertServiceRecord(ServiceRecord(
                bikePlate = "MH-12-AB-1234",
                type = "SERVICE_WITHOUT_PARTS",
                partsDetails = null,
                cost = 50.00,
                odometer = 6200,
                notes = "Full chain tension adjustment, clutch play adjustment, and tire pressure check. Lubricated control cables.",
                date = System.currentTimeMillis() - (15 * 24 * 60 * 60 * 1000L)
            ))

            // Prepopulate service records for Honda CBR600RR (DL-3S-CD-5678)
            dao.insertServiceRecord(ServiceRecord(
                bikePlate = "DL-3S-CD-5678",
                type = "OIL_CHANGE",
                partsDetails = "Honda GN4 10W30 Mineral Oil, K&N Oil Filter",
                cost = 75.00,
                odometer = 12000,
                notes = "Oil filter swap, checked air filter cleanliness.",
                date = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L)
            ))
            dao.insertServiceRecord(ServiceRecord(
                bikePlate = "DL-3S-CD-5678",
                type = "SERVICE_WITH_PARTS",
                partsDetails = "Pirelli Diablo Rosso IV Rear Tire, Heavy Duty Tube",
                cost = 340.00,
                odometer = 14500,
                notes = "Replaced rear tire due to puncture. Balanced wheel and adjusted chain alignment.",
                date = System.currentTimeMillis() - (20 * 24 * 60 * 60 * 1000L)
            ))

            // Prepopulate appointments
            dao.insertAppointment(Appointment(
                bikePlate = "MH-12-AB-1234",
                riderName = "Alex Mercer",
                riderPhone = "+1 555-0192",
                serviceType = "OIL_CHANGE",
                preferredDate = System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000L),
                notes = "Bike is running a bit hot. Standard oil check and change.",
                status = "CONFIRMED",
                appointmentNumber = "00001"
            ))

            dao.insertAppointment(Appointment(
                bikePlate = "CA-99-EF-9012",
                riderName = "Elena Fisher",
                riderPhone = "+1 555-0187",
                serviceType = "SERVICE_WITH_PARTS",
                preferredDate = System.currentTimeMillis() + (4 * 24 * 60 * 60 * 1000L),
                notes = "Hearing squeaking noises from the front fork. Might need oil seals replacement.",
                status = "PENDING",
                appointmentNumber = "00002"
            ))

            // Seed Staff Members
            val teamAStaff = listOf(
                "316-MOHAMMAD AFTAB ALAM",
                "321 - ALJAY PRADES FORMALEJO",
                "323-IMSATH ALI MOHAMADU THAMBY",
                "329-PRAMOD RAJBANSHI",
                "338-MD RIYAD HOSSAIN",
                "341-Jumary Bermejo",
                "351-M Zuber",
                "355-Hannan Khan",
                "366 - Md Shahid"
            )
            val teamBStaff = listOf(
                "319-KAUSHAL KUMAR",
                "335 - ABDUL HAEE RAHMAT MANSOORI",
                "340-Md Sahabul Islam",
                "348-HAJRAT MANSUR",
                "349-NURUDIN HUSEN",
                "352-JHANGIR HOSSAIN POLASH",
                "357-Sabir Dhobi",
                "359-M Sameer M Azeez",
                "G364-M Hajrat Ali Ansari"
            )
            teamAStaff.forEach { name ->
                dao.insertStaffMember(StaffMember(name = name, shift = "Shift A", designation = "Mechanic"))
            }
            teamBStaff.forEach { name ->
                dao.insertStaffMember(StaffMember(name = name, shift = "Shift B", designation = "Mechanic"))
            }

            // Seed Service with Parts Records
            dao.insertServiceWithPartsRecord(ServiceWithPartsRecord(
                serviceType = "Routine", sku = "SKU101", vchNo = "V-1001", vchDate = "15/06/2026",
                partNo = "P-01", itemName = "Spark Plug BKR5E", voucherType = "Sales",
                garage = "Garage A", bikeNo = "DX-101", division = "Div 1", mechanic = "John Doe", quantity = 1.0
            ))
            dao.insertServiceWithPartsRecord(ServiceWithPartsRecord(
                serviceType = "Routine", sku = "SKU102", vchNo = "V-1001", vchDate = "15/06/2026",
                partNo = "P-02", itemName = "Air Filter OEM", voucherType = "Sales",
                garage = "Garage A", bikeNo = "DX-101", division = "Div 1", mechanic = "John Doe", quantity = 1.0
            ))
            dao.insertServiceWithPartsRecord(ServiceWithPartsRecord(
                serviceType = "Oil Swap", sku = "SKU201", vchNo = "V-1002", vchDate = "18/06/2026",
                partNo = "P-03", itemName = "Engine Oil", voucherType = "Sales",
                garage = "Garage A", bikeNo = "DX-102", division = "Div 1", mechanic = "John Doe", quantity = 1.0
            ))
            dao.insertServiceWithPartsRecord(ServiceWithPartsRecord(
                serviceType = "Oil Swap Extra", sku = "SKU202", vchNo = "V-1003", vchDate = "19/06/2026",
                partNo = "P-04", itemName = "Engine Oil 2L", voucherType = "Sales",
                garage = "Garage A", bikeNo = "DX-103", division = "Div 1", mechanic = "John Doe", quantity = 2.0
            ))
            dao.insertServiceWithPartsRecord(ServiceWithPartsRecord(
                serviceType = "Engine Repair", sku = "SKU301", vchNo = "V-1004", vchDate = "20/06/2026",
                partNo = "P-05", itemName = "Piston Rings", voucherType = "Sales",
                garage = "Garage A", bikeNo = "DX-104", division = "Div 1", mechanic = "John Doe", quantity = 1.0
            ))
            dao.insertServiceWithPartsRecord(ServiceWithPartsRecord(
                serviceType = "Accident Repair", sku = "SKU401", vchNo = "V-1005", vchDate = "22/06/2026",
                partNo = "P-06", itemName = "Front Handlebar", voucherType = "Sales",
                garage = "Garage A", bikeNo = "DX-105", division = "Div 1", mechanic = "John Doe", quantity = 1.0
            ))

            // Seed Oil History Records
            dao.insertOilHistoryRecord(OilHistoryRecord(
                month = "June 2026", bikeNumber = "DX-201", kilometer = 15400, nextService = "18400",
                company = "Talabat", remarks = "Regular oil replacement", blankColumn = "", mechanic = "Mike Ross"
            ))

            // Seed Service Without Parts Records
            dao.insertServiceWithoutPartsRecord(ServiceWithoutPartsRecord(
                date = "22/06/2026", talabatId = "T-1001", bikeNo = "DX-301", kmRun = 9500,
                jobCard = "JC-2001", jobDone = "Chain lubrication and brake tight",
                serviceType = "Routine Maintenance", mechanic = "John Doe", company = "Talabat"
            ))
            dao.insertServiceWithoutPartsRecord(ServiceWithoutPartsRecord(
                date = "22/06/2026", talabatId = "T-1002", bikeNo = "DX-302", kmRun = 11200,
                jobCard = "JC-2002", jobDone = "Brake shoe cleaning",
                serviceType = "Brake Service", mechanic = "John Doe", company = "Talabat"
            ))

            // Seed BikeRiderMappings
            dao.insertBikeRiderMapping(BikeRiderMapping("MH-12-AB-1234", "R-101", "Alex Mercer"))
            dao.insertBikeRiderMapping(BikeRiderMapping("DL-3S-CD-5678", "R-102", "Marcus Fenix"))
        }
    }
}