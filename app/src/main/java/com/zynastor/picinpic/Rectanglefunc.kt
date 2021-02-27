package com.zynastor.picinpic

import android.os.Parcel
import android.os.Parcelable

data class Rectanglefunc(val startX: Int = 0, val startY: Int = 0, val endX: Int = 0, val endY: Int = 0) :
    Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(startX)
        parcel.writeInt(startY)
        parcel.writeInt(endX)
        parcel.writeInt(endY)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Rectanglefunc> {
        override fun createFromParcel(parcel: Parcel): Rectanglefunc {
            return Rectanglefunc(parcel)
        }

        override fun newArray(size: Int): Array<Rectanglefunc?> {
            return arrayOfNulls(size)
        }
    }
}