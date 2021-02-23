package de.dreier.mytargets.shared.models

import java.math.BigDecimal
import kotlin.math.pow

class HandicapCalculator {
    private lateinit var targetDistance: Dimension
    private lateinit var metricDistance: BigDecimal
    private val yards2metres = 0.9144
    private val inch2centimetre = 2.54

    private var handicap: Int = 0
    private var handicapCoefficient: Double = 0.0
    private var targetDistanceMetres: Double = 0.0


    fun metricDistance(): BigDecimal {
        return metricDistance
    }

    fun targetDistance(): Dimension {
        return targetDistance
    }

    fun handicap(): Int {
        return handicap
    }

    fun handicapCoefficient(): Double {
        return handicapCoefficient
    }

    fun yardsToMetres(yards: Int): Double {
        return yards * yards2metres
    }

    fun setTargetDistance(distanceDimension: Dimension) {
        targetDistance = distanceDimension
        this.metricDistance = BigDecimal.valueOf(distanceDimension.convertTo(Dimension.Unit.METER).value.toDouble())
    }

    fun setHandicap(handicap: Int) {
        this.handicap = handicap
        this.handicapCoefficient = handicapCoefficient(handicap)
    }

    fun handicapCoefficient(handicap: Int) :Double {
        //K=1.429*10^-6 * 1.07^(handicap+4.3)
        return 1.429*(10.00.pow(-6))*1.07.pow(handicap+4.3)
    }

    fun dispersionFactor(handicap: Int, distance: BigDecimal): BigDecimal {
        //F=1 + 1.429*10^-6 * 1.07^(handicap+4.3) * distance_in_metres^2
        return BigDecimal.valueOf(1 + 1.429 * (10.0.pow(-6)) * 1.07.pow(handicap+4.3) * distance.toDouble().pow(2))
    }

    fun angularDeviation(): BigDecimal {
       return  BigDecimal.valueOf((1.036.pow(handicap+12.9))*5*(10.0.pow(-4))*180/Math.PI)
    }

    fun groupRadius(): BigDecimal {
    //sigma=groupRadiusCm==100*distance_in_metres*(1.036^(handicap+12.9))*5*(10^-4)*Dispersion_Factor
        return BigDecimal(100* metricDistance.toDouble() * 1.036.pow(handicap+12.9) * 5 * 10.0.pow(-4)) * dispersionFactor(handicap, metricDistance)
    }


}
