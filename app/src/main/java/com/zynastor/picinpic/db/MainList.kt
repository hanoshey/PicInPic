package com.zynastor.picinpic.db

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*

@Entity(tableName = "mainlist")
data class MainList(
    @PrimaryKey(autoGenerate = true) val mainId: Long = 0,
    val name: String,
    val photoData: String,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readString()!!) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(mainId)
        parcel.writeString(name)
        parcel.writeString(photoData)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MainList> {
        override fun createFromParcel(parcel: Parcel): MainList {
            return MainList(parcel)
        }

        override fun newArray(size: Int): Array<MainList?> {
            return arrayOfNulls(size)
        }
    }
}
