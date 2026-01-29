package com.sshfp.ssh

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sshfp.model.Host

/**
 * 主机数据库
 */
@Database(entities = [Host::class], version = 1)
abstract class HostDatabase : RoomDatabase() {
    abstract fun hostDao(): HostDao

    companion object {
        @Volatile
        private var INSTANCE: HostDatabase? = null

        fun getInstance(context: Context): HostDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): HostDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                HostDatabase::class.java,
                "sshfp.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
