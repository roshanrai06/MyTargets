package de.dreier.mytargets.shared.models

import de.dreier.mytargets.shared.models.db.Round

class MultiRoundHandicapCalculator{
    private lateinit var rounds: List<Round>

    constructor(roundList: List<Round>) {
       this.rounds = roundList
    }

    fun handicapScoresList(): List<Int> {
        var handicapList = ArrayList<Int>()
        var roundHandicapScoreLists = ArrayList<List<Int>>()
        for (round: Round in rounds) {
            roundHandicapScoreLists.add(HandicapCalculator(round).handicapScoresList())
        }
        for (handicap: Int in  HandicapCalculator.handicapLowerBound()..HandicapCalculator.handicapUpperBound()) {
            var totalForHandicap: Int = 0
            for (roundHandicapScoreList: List<Int> in roundHandicapScoreLists) {
                totalForHandicap += roundHandicapScoreList.get(handicap)
            }
            handicapList.add(totalForHandicap)
        }
        return  handicapList
    }

    fun getHandicapForScore(score: Int): Int {
        var scoreList = handicapScoresList()
        for (handicap: Int in  HandicapCalculator.handicapLowerBound()..HandicapCalculator.handicapUpperBound()) {
            if (score >= scoreList.get(handicap)) {
                return  handicap
            }
        }
        return 101
    }

    fun getHandicap(): Int {
        var totalScore: Int = 0
        for (round: Round in rounds) {
           totalScore += round.score.totalPoints
        }
        return getHandicapForScore(totalScore)
    }

}
