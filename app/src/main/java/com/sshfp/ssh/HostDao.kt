package com.sshfp.ssh

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sshfp.model.Host
import kotlinx.coroutines.flow.Flow

/**
 * 主机数据访问对象
 */
@Dao
interface HostDao {

    @Query("SELECT * FROM hosts ORDER BY sortOrder, name")
    fun getAllHosts(): Flow<List<Host>>

    @Query("SELECT * FROM hosts ORDER BY sortOrder, name")
    suspend fun getAllHostsList(): List<Host>

    @Query("SELECT * FROM hosts WHERE id = :id")
    suspend fun getHostById(id: Long): Host?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHost(host: Host): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHosts(hosts: List<Host>)

    @Update
    suspend fun updateHost(host: Host)

    @Delete
    suspend fun deleteHost(host: Host)

    @Query("DELETE FROM hosts WHERE id = :id")
    suspend fun deleteHostById(id: Long)

    @Query("DELETE FROM hosts")
    suspend fun deleteAllHosts()

    @Query("UPDATE hosts SET lastConnectedAt = :timestamp WHERE id = :hostId")
    suspend fun updateLastConnected(hostId: Long, timestamp: Long)

    @Query("UPDATE hosts SET sortOrder = :order WHERE id = :hostId")
    suspend fun updateSortOrder(hostId: Long, order: Int)
}
