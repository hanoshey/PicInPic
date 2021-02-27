package com.zynastor.picinpic.db

import androidx.room.*

@Dao
interface MainAndUserDao:MainListDao,UserDao {
    @Transaction
    @Query("SELECT * FROM mainlist order by mainId desc")
    fun getAllMainAndUser(): List<MainAndUser>
}