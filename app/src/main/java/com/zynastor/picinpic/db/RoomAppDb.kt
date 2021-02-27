package com.zynastor.picinpic.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserEntry::class, MainList::class], version = 1)
abstract class RoomAppDb : RoomDatabase() {
    abstract fun getMainAndUserDao():MainAndUserDao
    companion object {
        private var INSTANCE: RoomAppDb? = null
        /*val migration_1_2=object :Migration(1,2){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("")
            }
        }*/
        fun getAppDatabase(context: Context): RoomAppDb {
            if (INSTANCE == null) {
                synchronized(RoomDatabase::class) {
                    INSTANCE = Room.databaseBuilder(
                        context, RoomAppDb::class.java, "AppDB.db"
                    ).build()
                }
            }
            return INSTANCE!!
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}