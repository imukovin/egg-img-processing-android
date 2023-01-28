package com.imukstudio.eggimgprocessing.domain.model

/**
 * Contains physical parameters of egg.
 *
 * [width] physical width.
 * [height] physical height.
 * [volume] physical volume.
 * [square] physical surface area.
 * [mass] physical mass.
 * [rationAreaToVolume] area ration to volume.
 * [shellMass] egg shell mass (it's in a range [10%-12%] of main mass)
 * [yolkMass] egg yolk mass (it's in a range [30%-32%] of main mass)
 * [proteinMass] egg protein mass (it's in a range [55%-57%] of main mass)
 */
data class EggParameters(
    val width: Double,
    val height: Double,
    val volume: Double,
    val square: Double,
    val mass: Double,
    val rationAreaToVolume: Double,
    val shellMass: Double,
    val yolkMass: Double,
    val proteinMass: Double
)
