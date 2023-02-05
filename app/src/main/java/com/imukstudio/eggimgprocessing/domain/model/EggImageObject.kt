package com.imukstudio.eggimgprocessing.domain.model

import org.opencv.core.Point
import org.opencv.core.Rect

/**
 * Parameters in px for egg from image.
 * [boundingBox] bounding box for found ellipse.
 * [equatorialRadius] equatorial radius.
 * [polarRadius] polar radius.
 */
class EggImageObject(
    val boundingBox: Rect,
    val equatorialRadius: Double,
    val polarRadius: Double
)
