package br.com.taina.constantia.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.taina.constantia.core.database.BodyMetricEntity
import br.com.taina.constantia.core.database.FoodEntity
import br.com.taina.constantia.core.repository.DayNutrition
import br.com.taina.constantia.core.repository.NutritionRepository
import br.com.taina.constantia.engine.MealParseResult
import br.com.taina.constantia.engine.NutritionTotals
import br.com.taina.constantia.engine.SmartMealParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class NutritionViewModel(
    private val repository: NutritionRepository,
    private val mealParser: SmartMealParser = SmartMealParser()
) : ViewModel() {
    private val today = LocalDate.now()

    val foods: StateFlow<List<FoodEntity>> = repository.observeFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val day: StateFlow<DayNutrition> = repository.observeDay(today)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DayNutrition(emptyList(), emptyList(), emptyList(), NutritionTotals(), null)
        )

    val weights: StateFlow<List<BodyMetricEntity>> = repository.observeBodyMetrics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _sevenDayAverage = MutableStateFlow<Double?>(null)
    val sevenDayAverage: StateFlow<Double?> = _sevenDayAverage.asStateFlow()

    private val _mealDraft = MutableStateFlow<MealParseResult?>(null)
    val mealDraft: StateFlow<MealParseResult?> = _mealDraft.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureReady()
            refreshWeightAverage()
        }
    }

    fun addFood(food: FoodEntity, mealType: String, amount: Double, unitMode: String, notes: String = "") {
        if (amount <= 0.0) return
        viewModelScope.launch { repository.addFoodToMeal(today, mealType, food, amount, unitMode, notes) }
    }

    fun parseMealDescription(description: String) {
        _mealDraft.value = mealParser.parse(description, foods.value)
    }

    fun clearMealDraft() { _mealDraft.value = null }

    fun saveMealDraft(mealType: String) {
        val draft = _mealDraft.value ?: return
        viewModelScope.launch {
            draft.items.forEach { item ->
                repository.addFoodToMeal(
                    date = today,
                    mealType = mealType,
                    food = item.food,
                    amount = item.amount,
                    unitMode = item.unitMode,
                    notes = "Descrição interpretada localmente pelo Constantia"
                )
            }
            _mealDraft.value = null
        }
    }

    fun addCustomFood(
        name: String,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        measureName: String,
        measureGrams: Double
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCustomFood(name, kcal, protein, carbs, fat, measureName, measureGrams)
        }
    }

    fun saveGoal(calories: Int?, protein: Int?, carbs: Int?, fat: Int?) {
        viewModelScope.launch { repository.setGoal(calories, protein, carbs, fat) }
    }

    fun addWeight(weightKg: Double) {
        if (weightKg <= 0.0) return
        viewModelScope.launch {
            repository.addWeight(weightKg)
            refreshWeightAverage()
        }
    }

    private suspend fun refreshWeightAverage() {
        _sevenDayAverage.value = repository.getWeightTrend().sevenDayAverageKg
    }

    class Factory(private val repository: NutritionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = NutritionViewModel(repository) as T
    }
}
