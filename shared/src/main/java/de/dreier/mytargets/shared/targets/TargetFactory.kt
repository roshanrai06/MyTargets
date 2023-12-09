/*
 * Copyright (C) 2018 Florian Dreier
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
package de.dreier.mytargets.shared.targets

import de.dreier.mytargets.shared.models.Target
import de.dreier.mytargets.shared.targets.models.ASA3D
import de.dreier.mytargets.shared.targets.models.ASA3D14
import de.dreier.mytargets.shared.targets.models.Beursault
import de.dreier.mytargets.shared.targets.models.DAIR3D
import de.dreier.mytargets.shared.targets.models.DBSCBlowpipe
import de.dreier.mytargets.shared.targets.models.HitOrMiss
import de.dreier.mytargets.shared.targets.models.IBO3D
import de.dreier.mytargets.shared.targets.models.IFAAAnimal
import de.dreier.mytargets.shared.targets.models.NFAAAnimal
import de.dreier.mytargets.shared.targets.models.NFAAExpertField
import de.dreier.mytargets.shared.targets.models.NFAAField
import de.dreier.mytargets.shared.targets.models.NFAAHunter
import de.dreier.mytargets.shared.targets.models.NFAAIndoor
import de.dreier.mytargets.shared.targets.models.NFAAIndoor5Spot
import de.dreier.mytargets.shared.targets.models.NFAS3D
import de.dreier.mytargets.shared.targets.models.NFASField
import de.dreier.mytargets.shared.targets.models.SCAPeriod
import de.dreier.mytargets.shared.targets.models.TargetModelBase
import de.dreier.mytargets.shared.targets.models.WA3Ring
import de.dreier.mytargets.shared.targets.models.WA3Ring3Spot
import de.dreier.mytargets.shared.targets.models.WA5Ring
import de.dreier.mytargets.shared.targets.models.WA6Ring
import de.dreier.mytargets.shared.targets.models.WADanage3Spot
import de.dreier.mytargets.shared.targets.models.WADanage6Spot
import de.dreier.mytargets.shared.targets.models.WAField
import de.dreier.mytargets.shared.targets.models.WAField3Spot
import de.dreier.mytargets.shared.targets.models.WAFull
import de.dreier.mytargets.shared.targets.models.WAVegas3Spot
import de.dreier.mytargets.shared.targets.models.WAVertical3Spot
import de.dreier.mytargets.shared.targets.models.Worcester

object TargetFactory {

    private val list: MutableList<TargetModelBase>

    private var idIndexLookup = mutableMapOf<Long, Int>()

    val comparator: Comparator<Target>
        get() = compareBy { idIndexLookup[it.id]!! }

    init {
        list = ArrayList()
        list.add(WAFull())
        list.add(WA6Ring())
        list.add(WA5Ring())
        list.add(WA3Ring())
        list.add(WAVertical3Spot())
        list.add(WAVegas3Spot())
        list.add(WA3Ring3Spot())
        list.add(WADanage3Spot())
        list.add(WADanage6Spot())
        list.add(NFAAIndoor())
        list.add(NFAAIndoor5Spot())
        list.add(HitOrMiss())
        list.add(Beursault())
        list.add(SCAPeriod())
        list.add(Worcester())
        list.add(DBSCBlowpipe())
        list.add(WAField())
        list.add(WAField3Spot())
        list.add(NFAAField())
        list.add(NFAAExpertField())
        list.add(NFAAHunter())
        list.add(IFAAAnimal())
        list.add(NFAAAnimal())
        list.add(NFASField())
        list.add(ASA3D())
        list.add(ASA3D14())
        list.add(IBO3D())
        list.add(NFAS3D())
        list.add(DAIR3D())
    }

    init {
        for (i in list.indices) {
            idIndexLookup[list[i].id] = i
        }
    }

    fun getList(): List<TargetModelBase> {
        return list
    }

    fun getList(target: Target): List<TargetModelBase> {
        val out = ArrayList<TargetModelBase>()
        if (target.id < 7L) {
            val til = if (target.diameter.value <= 60) 7L else 4L
            for (i in 0 until til) {
                out.add(list[i.toInt()])
            }
        } else if (target.id == 10L || target.id == 11L) {
            out.add(NFAAIndoor())
            out.add(NFAAIndoor5Spot())
        } else {
            out.add(list[idIndexLookup[target.id]!!])
        }
        return out
    }

    fun getTarget(id: Long): TargetModelBase {
        return list[idIndexLookup[id]!!]
    }
}
