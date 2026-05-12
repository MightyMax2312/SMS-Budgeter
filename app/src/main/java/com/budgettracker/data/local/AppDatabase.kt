package com.budgettracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.budgettracker.data.local.dao.TransactionDao
import com.budgettracker.data.local.entity.TransactionEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DATABASE_NAME = "budget_tracker.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                SQLiteDatabase.loadLibs(context)

                val factory = SupportFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        fun getInstanceWithoutEncryption(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}