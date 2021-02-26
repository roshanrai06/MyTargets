package de.dreier.mytargets.shared.models

import de.dreier.mytargets.shared.targets.models.TargetModelBase
import java.lang.Math.exp
import java.math.BigDecimal
import kotlin.math.pow

class HandicapCalculator {
    private lateinit var targetModel: TargetModelBase
    private var targetSizeIndex: Int = 0
    private var scoringStyleIndex: Int = 0
    private lateinit var targetDistance: Dimension
    private lateinit var metricDistance: BigDecimal
    private val yards2metres = 0.9144
    private val inch2centimetre = 2.54

    private var handicap: Int = 0
    private var arrowRadius = BigDecimal("0.357")


    fun metricDistance(): BigDecimal {
        return metricDistance
    }

    fun targetDistance(): Dimension {
        return targetDistance
    }

    fun handicap(): Int {
        return handicap
    }

    fun yardsToMetres(yards: Int): Double {
        return yards * yards2metres
    }

    fun setTargetDistance(distanceDimension: Dimension) {
        targetDistance = distanceDimension
        this.metricDistance = BigDecimal.valueOf(distanceDimension.convertTo(Dimension.Unit.METER).value.toDouble())
    }

    fun handicapCoefficient(handicap: Int) :Double {
        //K=1.429*10^-6 * 1.07^(handicap+4.3)
        return 1.429*(10.00.pow(-6))*1.07.pow(handicap+4.3)
    }

    fun dispersionFactor(handicap: Int): BigDecimal {
        //F=1 + 1.429*10^-6 * 1.07^(handicap+4.3) * distance_in_metres^2
        return BigDecimal.valueOf(1 + 1.429 * (10.0.pow(-6)) * 1.07.pow(handicap+4.3) * metricDistance.toDouble().pow(2))
    }

    fun angularDeviation(handicap: Int): BigDecimal {
       return  BigDecimal.valueOf((1.036.pow(handicap+12.9))*5*(10.0.pow(-4))*180/Math.PI)
    }

    fun groupRadius(handicap: Int): BigDecimal {
        //sigma=groupRadiusCm==100*distance_in_metres*(1.036^(handicap+12.9))*5*(10^-4)*Dispersion_Factor
        return BigDecimal("0.05") * metricDistance * BigDecimal.valueOf(1.036.pow(handicap+12.9)) * dispersionFactor(handicap)
    }

    fun setScoringStyleIndex(scoringStyleIndex: Int) {
        this.scoringStyleIndex = scoringStyleIndex
    }

    fun setTargetSizeIndex(targetSizeIndex: Int) {
        this.targetSizeIndex = targetSizeIndex
    }

    fun setTargetModel(targetModel: TargetModelBase) {
        this.targetModel = targetModel
    }

    fun averageArrowScoreForHandicap(handicap: Int): BigDecimal {
    //10-zone-Average_Arrow_Score=10 - (EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((7*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((8*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((9*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((10*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2))
        val groupRadiusSquared = groupRadius(handicap).pow(2)
        val zoneMap = targetModel.getZoneSizeMapFromProperties(scoringStyleIndex, targetSizeIndex)
        val bestArrowScore = BigDecimal(zoneMap.keys.max().toString())

        var zoneScoreStep = (zoneMap.keys.elementAt(0) - zoneMap.keys.elementAt(1))
        if (zoneScoreStep == 2) {
            return imperialCalc(zoneMap, groupRadiusSquared, bestArrowScore, zoneScoreStep)
        } else {
            return metricCalc(zoneMap, groupRadiusSquared, bestArrowScore)
        }
    }

    private fun metricCalc(zoneMap: Map<Int, BigDecimal>, groupRadiusSquared: BigDecimal, bestArrowScore: BigDecimal): BigDecimal {
        var exponentTotals = BigDecimal(0)
        for ((index, radius) in zoneMap.iterator()) {
            var zoneRadiusSquared = (radius + arrowRadius).pow(2)
            exponentTotals += BigDecimal.valueOf(exp(-(zoneRadiusSquared / groupRadiusSquared).toDouble()))
        }
        return bestArrowScore - exponentTotals
    }

    private fun imperialCalc(zoneMap: Map<Int, BigDecimal>, groupRadiusSquared: BigDecimal, bestArrowScore: BigDecimal, zoneScoreStep: Int): BigDecimal {
        var exponentTotals = BigDecimal(0)
        var lastEntry = zoneMap.entries.last()
        for ((index, radius) in zoneMap.iterator()) {
            var zoneRadiusSquared = (radius + arrowRadius).pow(2)
            if (radius == lastEntry.value) {
                exponentTotals -= BigDecimal.valueOf(exp(-(zoneRadiusSquared / groupRadiusSquared).toDouble()))
            } else {
                exponentTotals += BigDecimal.valueOf(zoneScoreStep * exp(-(zoneRadiusSquared / groupRadiusSquared).toDouble()))
            }
        }
        return bestArrowScore - exponentTotals
    }

    fun setArrowRadius(newArrowRadius: BigDecimal) {
        this.arrowRadius = newArrowRadius
    }
//arrowDiameterCm=arrow_diameter_cm  (0.357cm) 18/64
//targetSizeCm=target_size_cm (122)
//targetDistanceMetres=distance_m (70)
//

//15y=13.716m

//SizeOfZone = 1*targetSizeCm/20.....

    //10-zone-Average_Arrow_Score=10 - (
    // EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((7*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((8*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((9*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) +
    // EXP(-(((10*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2))

//5-zone-avg-arrow=          =9 -
// 2*(
// EXP(-(((1*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) +
// EXP(-(((2*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) +
// EXP(-(((3*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) +
// EXP(-(((4*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2)) -
// EXP(-(((5*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2)

//10-zone-compound-wa-pmoth  =10 - (EXP(-(((1*targetSizeCm/40)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((7*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((8*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((9*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((10*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2))
//10-zone-Average_Arrow_Score=10 - (EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((7*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((8*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((9*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((10*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2))
//10-zone-compound-wa-pmoth  =10 - (EXP(-(((1*targetSizeCm/40)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((7*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((8*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((9*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((10*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2))
//5-zone-avg-arrow=          =9 - 2*(EXP(-(((1*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2)) - EXP(-(((5*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2)
//10-zone-trispot==10 - (EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2)) - 6* EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2)
//6-zone-fita=10 - (EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2)) - 5* EXP(-(((6*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2)
//worcester=5 - (EXP(-(((1*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((2*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2))
//wa-field=6 - EXP(-(((1*targetSizeCm/20)+arrowDiameterCm)^2)/groupRadiusCm^2) - (EXP(-(((2*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((3*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((4*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2) + EXP(-(((5*targetSizeCm/10)+arrowDiameterCm)^2)/groupRadiusCm^2))
//beiter-hit-miss=1 - EXP(-(((1*targetSizeCm/2)+arrowDiameterCm)^2)/groupRadiusCm^2)
}
