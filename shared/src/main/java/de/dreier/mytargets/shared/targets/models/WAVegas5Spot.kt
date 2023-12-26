package de.dreier.mytargets.shared.targets.models

import android.graphics.PointF
import de.dreier.mytargets.shared.R
import de.dreier.mytargets.shared.models.Dimension
import de.dreier.mytargets.shared.targets.decoration.CenterMarkDecorator
import de.dreier.mytargets.shared.targets.scoringstyle.ScoringStyle
import de.dreier.mytargets.shared.targets.zone.CircularZone
import de.dreier.mytargets.shared.utils.Color


class WAVegas5Spot : TargetModelBase(
    id = ID,
    nameRes = R.string.vegas_5_spot,


    zones = listOf(
        CircularZone(0.20f, Color.LEMON_YELLOW, Color.DARK_GRAY, 2),
        CircularZone(0.4f, Color.LEMON_YELLOW, Color.DARK_GRAY, 2),
        CircularZone(0.6f, Color.LEMON_YELLOW, Color.DARK_GRAY, 2),
        CircularZone(0.8f, Color.FLAMINGO_RED, Color.DARK_GRAY, 2),
        CircularZone(1.0f, Color.FLAMINGO_RED, Color.DARK_GRAY, 2)

    ),
    scoringStyles = listOf(
        ScoringStyle(R.string.recurve_style_x_6, true, 10, 10, 9, 8, 7, 6),
        ScoringStyle(R.string.recurve_style_10_6, false, 10, 10, 9, 8, 7, 6),
        ScoringStyle(R.string.compound_style, false, 10, 9, 9, 8, 7, 6),
        ScoringStyle(false, 11, 10, 9, 8, 7, 6),
        ScoringStyle(true, 5, 5, 5, 4, 4, 3),
        ScoringStyle(false, 9, 9, 9, 7, 7, 5)
    ),
    diameters = listOf(Dimension(40f, Dimension.Unit.CENTIMETER))
) {
    init {
        decorator = CenterMarkDecorator(Color.DARK_GRAY, 25f, 9, true)
        facePositions = listOf(
            PointF(-0.6f, -0.6f),
            PointF(0.6f, -0.6f),
            PointF(0.0f, 0.0f),
            PointF(-0.6f, 0.6f),
            PointF(0.6f, 0.6f)
        )
        faceRadius = 0.4f
    }

    companion object {
        private const val ID = 6L
    }
}
