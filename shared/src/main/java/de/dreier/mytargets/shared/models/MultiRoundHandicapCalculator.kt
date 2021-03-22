package de.dreier.mytargets.shared.models

import de.dreier.mytargets.shared.models.db.Round
import java.math.BigDecimal
import java.math.RoundingMode

class MultiRoundHandicapCalculator{
    var rounds: List<Round>
        private set
    var roundHandicapScoreLists = ArrayList<List<BigDecimal>>()
        private set
    var totalScore: Int = 0
        private set
    var reachedScore: Int = 0
        private set

    constructor(roundList: List<Round>) {
        this.rounds = roundList
        for (round: Round in rounds) {
            totalScore += round.score.totalPoints
            reachedScore += round.score.reachedPoints
        }
        for (round: Round in rounds) {
            roundHandicapScoreLists.add(HandicapCalculator(round).handicapScoresList(false))
        }
    }

    fun handicapScoresList(): List<BigDecimal> {
        var handicapList = ArrayList<BigDecimal>()
        for (handicap: Int in  HandicapCalculator.handicapLowerBound()..HandicapCalculator.handicapUpperBound()) {
            var totalForHandicap: BigDecimal = BigDecimal("0")
            for (roundHandicapScoreList: List<BigDecimal> in roundHandicapScoreLists) {
                totalForHandicap += roundHandicapScoreList.get(handicap)
            }
            handicapList.add(totalForHandicap.setScale(0, RoundingMode.DOWN))
        }
        return  handicapList
    }

    fun getHandicapForScore(totalScore: Int): Int {
        val score = BigDecimal(totalScore.toString())
        var scoreList = handicapScoresList()
        for (handicap: Int in  HandicapCalculator.handicapLowerBound()..HandicapCalculator.handicapUpperBound()) {
            if (score >= scoreList.get(handicap)) {
                return  handicap
            }
        }
        return 101
    }

    fun getHandicap(): Int {
        return getHandicapForScore(reachedScore)
    }

}
