package de.dreier.mytargets.shared.models

import java.math.BigDecimal
import kotlin.math.pow

class HandicapCalculator {
    private val yards2metres = 0.9144
    private val inch2centimetre = 2.54

    private var handicap: Int = 0
    private var handicapCoefficient: Double = 0.0
    private var targetDistanceYards: Int = 0
    private var targetDistanceMetres: Double = 0.0
    private var isMetric: Boolean = true


    fun targetDistanceMetres(): Any {
        return targetDistanceMetres
    }

    fun targetDistanceYards(): Any {
        return targetDistanceYards
    }

    fun handicap(): Int {
        return handicap
    }

    fun handicapCoefficient(): Double {
        return handicapCoefficient
    }

    fun setTargetDistanceYards(yards: Int) {
        this.targetDistanceYards = yards
        this.targetDistanceMetres = yardsToMetres(yards)
        this.isMetric = false
    }

    fun yardsToMetres(yards: Int): Double {
        return yards * yards2metres
    }

    fun setTargetDistanceMetres(metres: Int) {
        this.targetDistanceMetres = metres.toDouble()
    }

    fun setHandicap(handicap: Int) {
        this.handicap = handicap
        this.handicapCoefficient = handicapCoefficient(handicap)
    }

    fun handicapCoefficient(handicap: Int) :Double {
        //K=1.429*10^-6 * 1.07^(handicap+4.3)
        return 1.429*(10.00.pow(-6))*1.07.pow(handicap+4.3)
    }

    fun dispersionFactor(handicap: Int, distance: Double): Double {
        //F=1 + 1.429*10^-6 * 1.07^(handicap+4.3) * distance_in_metres^2
        return 1+1.429*(10.0.pow(-6)) * 1.07.pow(handicap+4.3) * distance.pow(2)
    }

    fun dispersionFactorYards(handicap: Int, yards: Int): Double {
        return dispersionFactor(handicap, yardsToMetres(yards))
    }

    fun isMetric(): Boolean {
        return isMetric

    }

    fun angularDeviation(): BigDecimal {
       return  BigDecimal.valueOf((1.036.pow(handicap+12.9))*5*(10.0.pow(-4))*180/Math.PI)
    }


}
