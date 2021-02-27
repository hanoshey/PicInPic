package com.zynastor.picinpic.db

import androidx.room.*

@Dao
interface UserDao {

    @Query("SELECT * FROM userinfo ORDER BY userId DESC")
    suspend fun getAllUserInfo(): List<UserEntry>

    @Query("SELECT * FROM userinfo WHERE userId = :userId")
    suspend fun getUser(userId: Long): UserEntry

    @Query("select * from userInfo where mainListId = :mainListId")
    suspend fun getUserByMainId(mainListId: Long): List<UserEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntry?): Long

    @Delete
    suspend fun deleteUser(user: UserEntry?)

    @Update
    suspend fun updateUser(user: UserEntry?)
}