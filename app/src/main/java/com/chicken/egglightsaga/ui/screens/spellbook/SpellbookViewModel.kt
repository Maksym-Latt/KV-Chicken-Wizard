package com.chicken.egglightsaga.ui.screens.spellbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chicken.egglightsaga.ui.model.RuneColor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val ENERGY_COST = 1f
private const val AWAKENING_GOAL = 10

@HiltViewModel
class SpellbookViewModel @Inject constructor(
    private val repository: SpellbookRepository
) : ViewModel() {

    private val spellDefinitions = listOf(
        SpellDefinition(
            id = SpellId.FREEZE_TIME,
            title = "Frozen Hourglass",
            color = RuneColor.BLUE,
            awakeningsToUnlock = 0,
            energyCost = ENERGY_COST
        ),
        SpellDefinition(
            id = SpellId.FIRE_EGG,
            title = "Shattering Chant",
            color = RuneColor.YELLOW,
            awakeningsToUnlock = 3,
            energyCost = ENERGY_COST
        ),
        SpellDefinition(
            id = SpellId.TELEPORT,
            title = "Storm Surge",
            color = RuneColor.PINK,
            awakeningsToUnlock = 5,
            energyCost = ENERGY_COST
        )
    )
    private val _actionMessages = MutableStateFlow<String?>(null)
    val actionMessages: StateFlow<String?> = _actionMessages

    val uiState: StateFlow<SpellbookUiState> = repository.progress
        .map { progress ->
            val spells = spellDefinitions.map { definition ->
                val currentEnergy = progress.energyByColor[definition.color] ?: 0f
                val isUnlocked = progress.awakeningsCompleted >= definition.awakeningsToUnlock
                val unlockProgress = if (definition.awakeningsToUnlock == 0) {
                    1f
                } else {
                    (progress.awakeningsCompleted.toFloat() / definition.awakeningsToUnlock)
                        .coerceIn(0f, 1f)
                }
                SpellState(
                    definition = definition,
                    isUnlocked = isUnlocked,
                    unlockProgress = unlockProgress,
                    awakeningsCompleted = progress.awakeningsCompleted,
                    currentEnergy = currentEnergy,
                    energyProgress = (currentEnergy / definition.energyCost).coerceIn(0f, 1f),
                    isActive = isUnlocked && currentEnergy >= definition.energyCost
                )
            }
            SpellbookUiState(
                spells = spells,
                awakeningsCompleted = progress.awakeningsCompleted,
                awakeningsGoal = AWAKENING_GOAL
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SpellbookUiState(
                spells = spellDefinitions.map { definition ->
                    SpellState(
                        definition = definition,
                        isUnlocked = definition.awakeningsToUnlock == 0,
                        unlockProgress = if (definition.awakeningsToUnlock == 0) 1f else 0f,
                        awakeningsCompleted = 0,
                        currentEnergy = 0f,
                        energyProgress = 0f,
                        isActive = false
                    )
                },
                awakeningsCompleted = 0,
                awakeningsGoal = AWAKENING_GOAL
            )
        )

    fun onCastSpell(spellId: SpellId): Boolean {
        val definition = spellDefinitions.firstOrNull { it.id == spellId } ?: return false
        val spellState = uiState.value.spells.firstOrNull { it.definition.id == spellId } ?: return false
        if (!spellState.isUnlocked) {
            sendMessage("The spell is still locked")
            return false
        }
        if (spellState.currentEnergy < definition.energyCost) {
            sendMessage("Not enough energy")
            return false
        }
        repository.consumeEnergy(definition.color)
        repository.notifySpellCast(definition.id)
        return true
    }

    fun consumeMessage() {
        _actionMessages.value = null
    }

    private fun sendMessage(message: String) {
        viewModelScope.launch {
            _actionMessages.emit(message)
        }
    }
}

data class SpellbookUiState(
    val spells: List<SpellState>,
    val awakeningsCompleted: Int,
    val awakeningsGoal: Int
)

data class SpellState(
    val definition: SpellDefinition,
    val isUnlocked: Boolean,
    val unlockProgress: Float,
    val awakeningsCompleted: Int,
    val currentEnergy: Float,
    val energyProgress: Float,
    val isActive: Boolean
) {
    val remainingAwakenings: Int = (definition.awakeningsToUnlock - awakeningsCompleted).coerceAtLeast(0)
}

data class SpellDefinition(
    val id: SpellId,
    val title: String,
    val color: RuneColor,
    val awakeningsToUnlock: Int,
    val energyCost: Float
)

enum class SpellId {
    FREEZE_TIME,
    FIRE_EGG,
    TELEPORT
}
