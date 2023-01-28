package com.imukstudio.eggimgprocessing.domain.model

import org.opencv.core.Point
import org.opencv.core.Rect

/**
 * Parameters in px for egg from image.
 * [boundingBox] bounding box for found ellipse.
 * [a] equatorial radius.
 * [b] short polar radius.
 * [c] long polar radius.
 * [center] point of intersection equatorial and polar radius.
 */
class EggImageObject(
    val boundingBox: Rect,
    val a: Double,
    val b: Double,
    val c: Double,
    val center: Point
)
