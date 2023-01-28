package com.imukstudio.eggimgprocessing.domain.model

import com.imukstudio.eggimgprocessing.domain.utils.ReferenceObjectType
import org.opencv.core.Rect

/**
 * Parameters for reference object.
 * [boundingBox] bounding box for found ellipse.
 * [coefficient] Coefficient showing the ratio of the real size of the object to the size in the image.
 * [type] Type of current reference object.
 */
data class ReferenceObject(
    val boundingBox: Rect,
    val coefficient: Double,
    val type: ReferenceObjectType
)
