package com.chicken.egglightsaga.ui.screens.game

import com.chicken.egglightsaga.ui.model.RuneTile

sealed class GameAction {
    data class Swap(val from: Int, val to: Int) : GameAction()
    data class BounceBack(val index: Int) : GameAction()
    data class Match(val indices: List<Int>) : GameAction()
    data class Fall(val indices: List<Int>, val distance: Int) : GameAction()
    data class Refill(val tiles: List<RefillRune>) : GameAction() {
        data class RefillRune(val index: Int, val tile: RuneTile)
    }
}
