package com.lifepad.app.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

private val iconMap: Map<String, ImageVector> = mapOf(
    "restaurant" to Icons.Filled.Restaurant,
    "fastfood" to Icons.Filled.Fastfood,
    "directions_car" to Icons.Filled.DirectionsCar,
    "commute" to Icons.Filled.Commute,
    "bolt" to Icons.Filled.Bolt,
    "movie" to Icons.Filled.Movie,
    "medical_services" to Icons.Filled.LocalHospital,
    "shopping_bag" to Icons.Filled.ShoppingBag,
    "local_grocery_store" to Icons.Filled.LocalGroceryStore,
    "work" to Icons.Filled.Work,
    "savings" to Icons.Filled.Savings,
    "subscriptions" to Icons.Filled.Subscriptions,
    "more_horiz" to Icons.Filled.Menu,
    "home" to Icons.Filled.Home,
    "phone_android" to Icons.Filled.PhoneAndroid,
    "wifi" to Icons.Filled.Wifi,
    "smart_toy" to Icons.Filled.SmartToy,
    "travel" to Icons.Filled.TravelExplore,
    "education" to Icons.Filled.School,
    "fitness" to Icons.Filled.FitnessCenter,
    "clothing" to Icons.Filled.Checkroom,
    "money" to Icons.Filled.AttachMoney,
    "bank" to Icons.Filled.AccountBalance
)

val categoryIconOptions: List<Pair<String, ImageVector>> = iconMap.entries.map { it.toPair() }

fun categoryIconForName(name: String?): ImageVector {
    return iconMap[name] ?: Icons.Filled.Category
}

@Composable
fun CategoryIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
    )
}
