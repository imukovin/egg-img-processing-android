package com.imukstudio.eggimgprocessing.domain.model

/**
 * Contains physical parameters of egg.
 *
 * [width] physical width.
 * [height] physical height.
 * [volume] physical volume.
 * [square] physical surface area.
 * [mass] physical mass.
 */
data class EggParameters(
    val width: Double,
    val height: Double,
    val volume: Double,
    val square: Double,
    val mass: Double,
)
