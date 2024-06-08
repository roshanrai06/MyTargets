/*
 * MyTargets Project Copyright (C) 2018 Florian Dreier
 *
 * This file is (c) 2021 Jez McKinley - Canford Magna Bowmen
 *
* Calculations used in this file are courtesy of Jack Atkinson - see:
 * https://www.jackatkinson.net/post/archery_handicap/
 * derived from David Lane's Handicap Calcs for Toxophilus 1979
 *
 * MyTargets is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
 *
 * MyTargets is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package de.dreier.mytargets.shared.models

import de.dreier.mytargets.shared.models.db.Round
import de.dreier.mytargets.shared.targets.models.TargetModelBase
import de.dreier.mytargets.shared.targets.scoringstyle.ScoringStyle
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

class HandicapCalculator {
    constructor(round: Round) {
        this.arrowCount = round.score.shotCount
        this.targetModel = round.target.model
        this.scoringStyle = round.target.getScoringStyle()
        this.targetSize = round.target.diameter
        this.maxScore = round.score.totalPoints
        this.reachedScore = round.score.reachedPoints
        setTargetDistance(round.distance)
    }

    constructor()

    var reachedScore: Int = 0
        private set
    var maxScore: Int = 0
        private set
    var arrowCount: Int = 1
        private set
    lateinit var targetModel: TargetModelBase
        private set
    var targetSize: Dimension = Dimension.UNKNOWN
        private set
    lateinit var scoringStyle: ScoringStyle
        private set
    lateinit var targetDistance: Dimension
        private set
    lateinit var metricDistance: BigDecimal
        private set
    var arrowRadius = BigDecimal("0.357")
        private set

    companion object {
        // Australian handicap range: 0 to 100
        @JvmStatic
        fun handicapLowerBound(): Int = 0

        @JvmStatic
        fun handicapUpperBound(): Int = 100
    }


    private fun setTargetDistance(distanceDimension: Dimension) {
        targetDistance = distanceDimension
        this.metricDistance =
            BigDecimal.valueOf(distanceDimension.convertTo(Dimension.Unit.METER).value.toDouble())
    }


    fun setArrowRadius(arrowRadius: BigDecimal) {
        this.arrowRadius = arrowRadius
    }


    private fun dispersionFactor(handicap: Int): BigDecimal {
        val australianK = BigDecimal("1.234e-6")
        val australianA = BigDecimal("1.075")
        return BigDecimal.valueOf(1) + australianK.multiply(australianA.pow((handicap + 4.3).toInt()))
            .multiply(metricDistance.pow(2))
    }

    private fun groupRadius(handicap: Int): BigDecimal {
        val australianConstant = BigDecimal("0.048") // Replace with actual Australian value

        // Define australianA within the function's scope (or move to companion object)
        val australianA = BigDecimal("1.075") // Replace with actual Australian value

        return australianConstant * metricDistance * australianA.pow((handicap + 12.9).toInt()) * dispersionFactor(
            handicap
        )
    }

    private fun averageArrowScoreForHandicap(handicap: Int): BigDecimal {
        val groupRadiusSquared = groupRadius(handicap).pow(2)
        val zoneMap = targetModel.getZoneSizeMap(
            scoringStyle,
            targetSize
        ) // Assuming zoneMap is accurate for Australian targets

        return calculateAustralianAverageArrowScore(zoneMap, groupRadiusSquared, handicap)
    }

    private fun calculateAustralianAverageArrowScore(
        zoneMap: Map<Int, BigDecimal>,
        groupRadiusSquared: BigDecimal,
        handicap: Int
    ): BigDecimal {
        // Implementation based on Australian handicap system tables and formulas
        var expectedScore = BigDecimal.ZERO
        val theta = calculateTheta(handicap) // Use your existing theta calculation
        val xValues = calculateXValues(zoneMap)  // Convert zone radii to x values (see below)

        for (i in xValues.indices) {
            val zoneScore =
                BigDecimal(zoneMap.keys.elementAt(i)) // Assuming scores are keys in zoneMap
            val hitProbability = calculateHitProbability(
                xValues[i],
                theta
            ) // Calculate hit probability for this zone
            expectedScore += zoneScore * hitProbability
        }

        return expectedScore
    }

    private fun calculateTheta(handicap: Int): Double {
        // This is a rough approximation based on the relationship between handicap and theta described in the article.
        // It assumes a linear relationship, which may not be accurate for the actual Australian system.
        val baseTheta = 0.05 // Example starting point for theta at handicap 0
        val thetaIncrementPerHandicap = 0.005 // Example increment (needs to be adjusted)
        return baseTheta + (handicap * thetaIncrementPerHandicap)
    }

    private fun calculateXValues(zoneMap: Map<Int, BigDecimal>): List<Double> {
        // Convert zone radii to x values based on target face size and arrow radius
        return zoneMap.values.map { radius ->
            (radius + arrowRadius).toDouble() / (targetSize.value.toDouble() / 2)
        }
    }

    private fun calculateHitProbability(xValue: Double, theta: Double): BigDecimal {
        // Calculate the probability of hitting a zone using the normal distribution approximation
        val zScore = xValue / theta
        val probability =
            BigDecimal(0.5 * (1 + erf(zScore / sqrt(2.0)))) // Using error function (erf)
        return probability
    }


    fun handicapScoresList(rounded: Boolean = true): List<BigDecimal> {
        val decimalPlaces: Int = if (rounded) 0 else 2
        val handicapList = ArrayList<BigDecimal>()
        for (handicap: Int in handicapLowerBound()..handicapUpperBound()) {
//            handicapList.add((BigDecimal(arrowCount) * averageArrowScoreForHandicap(handicap).setScale(2, RoundingMode.HALF_UP)).setScale(0, RoundingMode.UP).toInt())
            val average = averageArrowScoreForHandicap(handicap)
            val roundScore = (BigDecimal(arrowCount) * average)
            handicapList.add(roundScore.setScale(decimalPlaces, RoundingMode.HALF_UP))
        }
        return handicapList
    }

    private fun erf(x: Double): Double {
        // This is a basic approximation of the error function
        // You can find more accurate implementations online
        val a1 = 0.254829592
        val a2 = -0.284496736
        val a3 = 1.421413741
        val a4 = -1.453152027
        val a5 = 1.061405429
        val p = 0.3275911

        val sign = if (x < 0) -1 else 1
        val xAbs = abs(x)

        val t = 1.0 / (1.0 + p * xAbs)
        val y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * exp(-xAbs * xAbs)
        return sign * y
    }

}
