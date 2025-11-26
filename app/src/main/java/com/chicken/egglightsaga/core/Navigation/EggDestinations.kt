package com.chicken.egglightsaga.core.Navigation

object EggRoutes {
    const val MAIN_MENU = "main_menu"
    const val GAME = "game"
    const val SPELLBOOK = "spellbook"
    const val SPELLBOOK_SHOW_CAST_PARAM = "showCastButton"
    const val SPELLBOOK_ROUTE = "$SPELLBOOK?$SPELLBOOK_SHOW_CAST_PARAM={$SPELLBOOK_SHOW_CAST_PARAM}"
}
