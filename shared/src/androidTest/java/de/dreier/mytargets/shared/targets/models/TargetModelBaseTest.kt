/*
 * Copyright (C) 2017 Florian Dreier
 *
 * This file is part of MyTargets.
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

package de.dreier.mytargets.shared.targets.models

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import de.dreier.mytargets.shared.SharedApplicationInstance
import de.dreier.mytargets.shared.models.Dimension
import de.dreier.mytargets.shared.targets.TargetFactory
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TargetModelBaseTest {

    @Before
    fun setUp() {
        SharedApplicationInstance.context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    @Throws(Exception::class)
    fun trivialTargetRealSize() {
        for (id in NFAAField.ID..WAField3Spot.ID) {
            val target = TargetFactory.getTarget(id)
            val diameter = Dimension(40f, Dimension.Unit.CENTIMETER)
            val realSize = target.getRealSize(diameter)
            Assert.assertEquals("Real size $realSize for target id $id does not match with expected value 40cm",
                    realSize, Dimension(40f, Dimension.Unit.CENTIMETER))
        }
    }

    @Test
    @Throws(Exception::class)
    fun waFieldRealSize() {
        val target = TargetFactory.getTarget(WAField.ID)
        val diameter = Dimension(40f, Dimension.Unit.CENTIMETER)
        val realSize = target.getRealSize(diameter)
        Assert.assertEquals(realSize, Dimension(40f, Dimension.Unit.CENTIMETER))
    }

    @Test
    @Throws(Exception::class)
    fun waVertical3SpotRealSize() {
        val target = TargetFactory.getTarget(WAVertical3Spot.ID)
        val diameter = Dimension(40f, Dimension.Unit.CENTIMETER)
        val realSize = target.getRealSize(diameter)
        Assert.assertEquals(realSize, Dimension(20f, Dimension.Unit.CENTIMETER))
    }

    @Test
    @Throws(Exception::class)
    fun waFullRealSize() {
        val target = TargetFactory.getTarget(WAFull.ID)
        val diameter = Dimension(40f, Dimension.Unit.CENTIMETER)
        val realSize = target.getRealSize(diameter)
        Assert.assertEquals(realSize, Dimension(40f, Dimension.Unit.CENTIMETER))
    }

    @Test
    @Throws(Exception::class)
    fun wa6RingRealSize() {
        val target = TargetFactory.getTarget(WA6Ring.ID)
        val diameter = Dimension(40f, Dimension.Unit.CENTIMETER)
        val realSize = target.getRealSize(diameter)
        Assert.assertEquals(realSize, Dimension(24f, Dimension.Unit.CENTIMETER))
    }

    @Test
    @Throws(Exception::class)
    fun wa5RingRealSize() {
        val target = TargetFactory.getTarget(WA5Ring.ID)
        val diameter = Dimension(40f, Dimension.Unit.CENTIMETER)
        val realSize = target.getRealSize(diameter)
        Assert.assertEquals(realSize, Dimension(20f, Dimension.Unit.CENTIMETER))
    }

    @Test
    @Throws(Exception::class)
    fun wa3RingRealSize() {
        val target = TargetFactory.getTarget(WA3Ring.ID)
        val diameter = Dimension(40f, Dimension.Unit.CENTIMETER)
        val realSize = target.getRealSize(diameter)
        Assert.assertEquals(realSize, Dimension(12f, Dimension.Unit.CENTIMETER))
    }


    @Test
    fun get_scoring_zone_sizes_simple_10_zone() {
        val unit = TargetFactory.getTarget(WAFull.ID)
//        var targetSize = Dimension(122f, Dimension.Unit.CENTIMETER)
//        var scoringStyle = ScoringStyle(R.string.recurve_style_x_1, true, 10, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1)

        var zoneMap = unit.getZoneSizeMapByIndex(0, 4)
        Assert.assertEquals(10, zoneMap.size)
        Assert.assertEquals(6.6f, zoneMap.get(10))
        Assert.assertEquals(33f, zoneMap.get(5))
        Assert.assertEquals(66f, zoneMap.get(1))
    }

}
