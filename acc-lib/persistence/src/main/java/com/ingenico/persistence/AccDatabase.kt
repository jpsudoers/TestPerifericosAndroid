package com.ingenico.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ingenico.persistence.dao.*
import com.ingenico.persistence.entity.*

@Database(entities = [
    EmvAidEntity::class,
    EmvCommonEntity::class,
    EmvCAKeyEntity::class,
],
    version = DatabaseConstants.DB_VERSION)
abstract class AccDatabase : RoomDatabase() {

    abstract fun emvCommonDao() : EmvCommonDao
    abstract fun emvAidDao() : EmvAidDao
    abstract fun emvCAKeyDao() : EmvCAkeyDao

    companion object {

        private var INSTANCE: AccDatabase? = null

        fun getDatabase(context: Context): AccDatabase {
            INSTANCE = INSTANCE?: Room.databaseBuilder(
                    context.applicationContext,
                    AccDatabase::class.java,
                    DatabaseConstants.DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()

            return INSTANCE!!
        }
    }
}
