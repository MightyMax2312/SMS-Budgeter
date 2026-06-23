package com.budgettracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.budgettracker.data.local.dao.TransactionDao
import com.budgettracker.data.local.entity.TransactionEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [TransactionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DATABASE_NAME = "budget_tracker.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN smsId INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN smsThreadId INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN smsAddress TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN smsDate INTEGER")
                db.execSQL("ALTER TABLE transactions ADD COLUMN transactionFingerprint TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_smsId ON transactions(smsId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_transactionFingerprint ON transactions(transactionFingerprint)")
            }
        }

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
                    .addMigrations(MIGRATION_1_2)
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
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
