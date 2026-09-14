package com.aristidevs.cursopremiumandroid.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Version 1. No [androidx.room.RoomDatabase.Callback] destructive fallback is
 * configured on purpose: local dogs only exist on the device, so a destructive
 * migration would delete them (PLAN.md, "Compatibilidad y migraciones").
 */
@Database(entities = [DogEntity::class], version = 1, exportSchema = true)
@TypeConverters(DogSourceConverter::class)
abstract class DogDatabase : RoomDatabase() {
    abstract fun dogDao(): DogDao
}
