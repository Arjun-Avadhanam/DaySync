package com.daysync.app.feature.expenses.model

enum class ExpenseCategory(
    val displayName: String,
    val subcategories: List<String>,
    val icon: String,
) {
    FOOD("Food", listOf("Dining Out", "Groceries", "Snacks", "Beverages", "Delivery"), "restaurant"),
    TRAVEL("Travel", listOf("Metro", "Auto/Cab", "Fuel", "Parking", "Bus", "Train", "Plane"), "directions_car"),
    PERSONAL_CARE("Personal Care", emptyList(), "spa"),
    HEALTH("Health", listOf("Medicine", "Doctor", "Gym"), "local_hospital"),
    BILLS("Bills", listOf("Phone", "Internet", "Electricity", "Subscriptions", "Rent"), "receipt_long"),
    SHOPPING("Shopping", listOf("Clothing", "Electronics", "Home"), "shopping_bag"),
    ENTERTAINMENT("Entertainment", emptyList(), "movie"),
    EDUCATION("Education", emptyList(), "school"),
    GIFTS("Gifts", emptyList(), "card_giftcard"),
    OTHER("Other", emptyList(), "more_horiz");

    companion object {
        private val PACKAGE_CATEGORY_MAP = mapOf(
            "in.swiggy.android" to "Food > Delivery",
            "com.grofers.customerapp" to "Food > Groceries",
            "in.amazon.mShop.android.shopping" to "Shopping",
            "com.dreamplug.androidapp" to "Bills",
        )

        private val MERCHANT_RULES = listOf(
            MerchantRule({ it.contains("swiggy", ignoreCase = true) }, "Food > Delivery"),
            MerchantRule({ it.contains("zomato", ignoreCase = true) }, "Food > Delivery"),
            MerchantRule({ it.contains("blinkit", ignoreCase = true) }, "Food > Groceries"),
            MerchantRule({ it.contains("bigbasket", ignoreCase = true) }, "Food > Groceries"),
            MerchantRule({ it.contains("zepto", ignoreCase = true) }, "Food > Groceries"),
            MerchantRule({ it.contains("uber", ignoreCase = true) }, "Travel > Auto/Cab"),
            MerchantRule({ it.contains("ola", ignoreCase = true) }, "Travel > Auto/Cab"),
            MerchantRule({ it.contains("rapido", ignoreCase = true) }, "Travel > Auto/Cab"),
            MerchantRule({ it.contains("metro", ignoreCase = true) }, "Travel > Metro"),
            MerchantRule({ it.contains("irctc", ignoreCase = true) }, "Travel > Train"),
            MerchantRule({ it.contains("indigo", ignoreCase = true) }, "Travel > Plane"),
            MerchantRule({ it.contains("spicejet", ignoreCase = true) }, "Travel > Plane"),
            MerchantRule({ it.contains("airindia", ignoreCase = true) }, "Travel > Plane"),
            MerchantRule({ it.contains("netflix", ignoreCase = true) }, "Bills > Subscriptions"),
            MerchantRule({ it.contains("spotify", ignoreCase = true) }, "Bills > Subscriptions"),
            MerchantRule({ it.contains("hotstar", ignoreCase = true) }, "Bills > Subscriptions"),
            MerchantRule({ it.contains("youtube", ignoreCase = true) }, "Bills > Subscriptions"),
            MerchantRule({ it.contains("amazon prime", ignoreCase = true) }, "Bills > Subscriptions"),
            MerchantRule({ it.contains("amazon", ignoreCase = true) }, "Shopping"),
            MerchantRule({ it.contains("flipkart", ignoreCase = true) }, "Shopping"),
            MerchantRule({ it.contains("myntra", ignoreCase = true) }, "Shopping > Clothing"),
            MerchantRule({ it.contains("pharma", ignoreCase = true) }, "Health > Medicine"),
            MerchantRule({ it.contains("1mg", ignoreCase = true) }, "Health > Medicine"),
            MerchantRule({ it.contains("apollo", ignoreCase = true) }, "Health"),
            MerchantRule({ it.contains("bookmyshow", ignoreCase = true) }, "Entertainment"),
        )

        fun suggestFromMerchant(merchantName: String?, packageName: String?): String? {
            if (packageName != null) {
                PACKAGE_CATEGORY_MAP[packageName]?.let { return it }
            }
            if (merchantName != null) {
                MERCHANT_RULES.firstOrNull { it.matcher(merchantName) }?.let { return it.category }
            }
            return null
        }

        fun fromCategoryString(value: String): Pair<ExpenseCategory, String?>? {
            val parts = value.split(" > ", limit = 2)
            val topLevel = parts[0].trim()
            val sub = parts.getOrNull(1)?.trim()
            val category = entries.find {
                it.displayName.equals(topLevel, ignoreCase = true)
            } ?: return null
            return category to sub
        }

        fun allCategoryStrings(): List<String> {
            return entries.flatMap { cat ->
                if (cat.subcategories.isEmpty()) {
                    listOf(cat.displayName)
                } else {
                    cat.subcategories.map { sub -> "${cat.displayName} > $sub" }
                }
            }
        }
    }
}

private data class MerchantRule(
    val matcher: (String) -> Boolean,
    val category: String,
)
