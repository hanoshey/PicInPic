package com.zynastor.picinpic.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation

@Entity(tableName = "mainanduser")
data class MainAndUser(
    @Embedded val mainList: MainList,
    @Relation(parentColumn = "mainId", entityColumn = "mainListId")
    val users: List<UserEntry>,
)