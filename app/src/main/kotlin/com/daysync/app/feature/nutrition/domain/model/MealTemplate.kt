package com.daysync.app.feature.nutrition.domain.model

data class MealTemplate(
    val id: String,
    val name: String,
    val description: String?,
)

data class MealTemplateItem(
    val id: String,
    val templateId: String,
    val foodId: String,
    val defaultAmount: Double,
)

data class MealTemplateItemWithFood(
    val item: MealTemplateItem,
    val food: FoodItem,
)

data class MealTemplateWithItems(
    val template: MealTemplate,
    val items: List<MealTemplateItemWithFood>,
)

data class MealTemplateInput(
    val name: String,
    val description: String? = null,
    val items: List<MealTemplateItemInput>,
)

data class MealTemplateItemInput(
    val foodId: String,
    val defaultAmount: Double,
)
