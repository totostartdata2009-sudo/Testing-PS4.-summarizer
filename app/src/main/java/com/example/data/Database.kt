package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history_items")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val time: String,
    val summary: String,
    val hasVoice: Boolean,
    val audioUri: String? = null
)

@Entity(tableName = "reminder_items")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val time: String,
    val isDone: Boolean = false,
    val summaryContext: String = "",
    val timestamp: Long = 0L
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_items ORDER BY id DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem)

    @Delete
    suspend fun deleteHistory(item: HistoryItem)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder_items ORDER BY timestamp DESC")
    fun getAllReminders(): Flow<List<ReminderItem>>

    @Query("SELECT * FROM reminder_items WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: Int): ReminderItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(item: ReminderItem): Long
    
    @Update
    suspend fun updateReminder(item: ReminderItem)

    @Delete
    suspend fun deleteReminder(item: ReminderItem)
}

@Database(entities = [HistoryItem::class, ReminderItem::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
