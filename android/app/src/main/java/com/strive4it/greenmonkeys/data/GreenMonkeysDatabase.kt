package com.strive4it.greenmonkeys.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        SessionPlanEntity::class,
        CommitmentEntity::class,
        SessionVideoEntity::class,
        VerdictEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class GreenMonkeysDatabase : RoomDatabase() {
    abstract fun planDao(): PlanDao

    companion object {
        @Volatile
        private var instance: GreenMonkeysDatabase? = null

        fun get(context: Context): GreenMonkeysDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    GreenMonkeysDatabase::class.java,
                    "greenmonkeys.db",
                ).build().also { instance = it }
            }
    }
}
