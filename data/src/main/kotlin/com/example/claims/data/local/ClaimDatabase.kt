package com.example.claims.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "claims")
data class ClaimEntity(
    @PrimaryKey val id: String,
    val number: String,
    val policyNumber: String,
    val type: String,
    val incidentEpochDay: Long,
    val lossGrosze: Long,
    val description: String,
    val status: String,
    val createdAtMillis: Long,
)

@Dao
interface ClaimDao {
    @Query("SELECT * FROM claims ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<ClaimEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(claim: ClaimEntity)
}

@Database(entities = [ClaimEntity::class], version = 1, exportSchema = true)
abstract class ClaimDatabase : RoomDatabase() {
    abstract fun claimDao(): ClaimDao
}
