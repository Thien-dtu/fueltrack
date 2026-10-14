package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.database.entity.MaintenanceTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceTemplateDao {
    @Query("SELECT * FROM maintenance_templates WHERE isArchived = 0 ORDER BY isBuiltIn DESC, name ASC")
    fun observeActiveTemplates(): Flow<List<MaintenanceTemplateEntity>>

    @Query("SELECT * FROM maintenance_templates ORDER BY isBuiltIn DESC, name ASC")
    fun observeAllTemplates(): Flow<List<MaintenanceTemplateEntity>>

    @Query("SELECT * FROM maintenance_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: Long): MaintenanceTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MaintenanceTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(templates: List<MaintenanceTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: MaintenanceTemplateEntity)

    @Query("UPDATE maintenance_templates SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id AND isBuiltIn = 0")
    suspend fun archiveTemplate(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE maintenance_templates SET isArchived = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun unarchiveTemplate(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM maintenance_templates WHERE name = :name LIMIT 1")
    suspend fun getTemplateByName(name: String): MaintenanceTemplateEntity?

    @Query("SELECT COUNT(*) FROM maintenance_templates")
    suspend fun getTemplateCount(): Int
}
