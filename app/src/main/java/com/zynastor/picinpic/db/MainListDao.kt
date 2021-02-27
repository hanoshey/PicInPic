package com.zynastor.picinpic.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MainListDao {

    @Query("SELECT * FROM mainlist ORDER BY mainId DESC")
    suspend fun getAllMainListInfo():List<MainList>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMainList(main: MainList?): Long?

    @Delete
    suspend fun deleteMainList(main: MainList?)

    @Update
    suspend fun updateMainList(main: MainList?)

    /* @Query("SELECT * FROM mainlist WHERE main_id = :mainId")
     fun getMainList(mainId: Long)*/
}