package com.zynastor.picinpic.db

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import com.zynastor.picinpic.Rectanglefunc

@Entity(tableName = "userInfo", foreignKeys = [ForeignKey(
    entity = MainList::class,
    parentColumns = ["mainId"],
    childColumns = ["mainListId"],
    onDelete = CASCADE
)])
data class UserEntry(
    @PrimaryKey(autoGenerate = true) val userId: Long = 0,
    val layerLevel: Int,
    @Embedded val rect: Rectanglefunc,
    val fromId: Long,
    val destPhotoUri: String,
    val mainListId: Long,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readInt(),
        parcel.readParcelable(Rectanglefunc::class.java.classLoader)!!,
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readLong()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(userId)
        parcel.writeInt(layerLevel)
        parcel.writeParcelable(rect, flags)
        parcel.writeLong(fromId)
        parcel.writeString(destPhotoUri)
        parcel.writeLong(mainListId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UserEntry> {
        override fun createFromParcel(parcel: Parcel): UserEntry {
            return UserEntry(parcel)
        }

        override fun newArray(size: Int): Array<UserEntry?> {
            return arrayOfNulls(size)
        }
    }
}