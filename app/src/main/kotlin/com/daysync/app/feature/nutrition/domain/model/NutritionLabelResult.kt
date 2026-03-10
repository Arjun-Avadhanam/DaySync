package com.daysync.app.feature.nutrition.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NutritionLabelResult(
    @SerialName("product_name") val productName: String = "",
    val category: String? = null,
    @SerialName("per_100") val per100: NutrientValues? = null,
    @SerialName("per_serving") val perServing: NutrientValues? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    @SerialName("serving_amount") val servingAmount: Double? = null,
    @SerialName("serving_unit") val servingUnit: String? = null,
    @SerialName("detected_unit") val detectedUnit: String? = null,
)

@Serializable
data class NutrientValues(
    val calories: Double = 0.0,
    @SerialName("protein_g") val proteinG: Double = 0.0,
    @SerialName("carbs_g") val carbsG: Double = 0.0,
    @SerialName("fat_g") val fatG: Double = 0.0,
    @SerialName("sugar_g") val sugarG: Double = 0.0,
)
