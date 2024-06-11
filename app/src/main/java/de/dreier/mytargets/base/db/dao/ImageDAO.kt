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

package de.dreier.mytargets.base.db.dao

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import java.io.File

@Dao
interface ImageDAO {
    /**
     * Returns a list of file names, which are implicitly placed in the ../files/ folder of the app.
     */
    @Query(
        "SELECT `fileName` FROM `BowImage` " +
                "UNION SELECT `fileName` FROM `EndImage` " +
                "UNION SELECT `fileName` FROM `ArrowImage`"
    )
    fun loadAllFileNames(): List<String>

    @Transaction
    fun removeAllPhotos(context: Context) {
        try {
            // 1. Delete entries from the database tables
            deleteBowImages()
            deleteEndImages()
            deleteArrowImages()
        } catch (e: SQLiteException) {
            // Handle exceptions if tables don't exist (e.g., log a warning)
        }

        // 2. Delete the corresponding files from storage
        val fileNames = try {
            loadAllFileNames()
        } catch (e: SQLiteException) {
            // Handle exceptions if the database hasn't been initialized (e.g., return an empty list)
            emptyList()
        }

        for (fileName in fileNames) {
            val file = File(context.filesDir, fileName)
            file.delete()
        }
    }

    @Query("DELETE FROM BowImage")
    fun deleteBowImages()

    @Query("DELETE FROM EndImage")
    fun deleteEndImages()

    @Query("DELETE FROM ArrowImage")
    fun deleteArrowImages()
}
