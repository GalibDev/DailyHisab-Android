package com.dailyhisab.android.feature.category

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyhisab.android.data.local.DailyHisabDatabase
import com.dailyhisab.android.data.repository.RoomFinanceRepository
import com.dailyhisab.android.domain.model.Category
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database = DailyHisabDatabase.getInstance(application)
    private val repository = RoomFinanceRepository(database)
    val categories = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (database.financeDao().categoryCount() == 0) {
                defaultCategories.forEach { repository.saveCategory(it) }
            }
        }
    }

    fun save(name: String, editing: Category?) = viewModelScope.launch {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return@launch
        repository.saveCategory(
            editing?.copy(name = cleanName) ?: Category(
                name = cleanName,
                iconKey = "category",
                colorArgb = 0xFF7146E8,
                position = categories.value.size,
            ),
        )
    }

    fun delete(category: Category) = viewModelScope.launch {
        if (!category.isDefault) repository.deleteCategory(category.id)
    }

    fun move(category: Category, offset: Int) = viewModelScope.launch {
        val list = categories.value.toMutableList()
        val from = list.indexOfFirst { it.id == category.id }
        if (from < 0) return@launch
        val to = (from + offset).coerceIn(0, list.lastIndex)
        if (from != to) {
            list.add(to, list.removeAt(from))
            repository.updateCategoryOrder(list.map(Category::id))
        }
    }

    private val defaultCategories = listOf(
        Category(name = "Breakfast", iconKey = "restaurant", colorArgb = 0xFFFF9D00, position = 0, isDefault = true),
        Category(name = "Transport", iconKey = "transport", colorArgb = 0xFF0875D1, position = 1, isDefault = true),
        Category(name = "Lunch", iconKey = "restaurant", colorArgb = 0xFFFF7314, position = 2, isDefault = true),
        Category(name = "Shopping", iconKey = "shopping", colorArgb = 0xFF079669, position = 3, isDefault = true),
        Category(name = "Other", iconKey = "category", colorArgb = 0xFF7146E8, position = 4, isDefault = true),
    )
}

