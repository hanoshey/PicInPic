package com.zynastor.picinpic.util

import android.graphics.Bitmap
import android.view.View
import com.zynastor.picinpic.ProjectPt

object DrawFunctions{
    fun projectXY(iv: View, bm: Bitmap, x: Int, y: Int): ProjectPt? {
        return if (x >= (iv.width - bm.width) / 2 && y >= (iv.height - bm.height) / 2 && x <= ((iv.width - bm.width) / 2) + bm.width && y <= (iv.height - bm.height) / 2 + bm.height) {
            val projectedX =
                (x.toDouble() - ((iv.width.toDouble() - bm.width.toDouble()) / 2)).toInt()
            val projectedY =
                (y.toDouble() - ((iv.height.toDouble() - bm.height.toDouble()) / 2)).toInt()
            ProjectPt(projectedX, projectedY)
        } else {
            null
        }
    }
}