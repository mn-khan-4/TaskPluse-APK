package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPulseItem
import com.example.ui.theme.CategoryBills
import com.example.ui.theme.CategoryBillsBg
import com.example.ui.theme.CategoryHealth
import com.example.ui.theme.CategoryHealthBg
import com.example.ui.theme.CategoryOther
import com.example.ui.theme.CategoryOtherBg
import com.example.ui.theme.CategoryPersonal
import com.example.ui.theme.CategoryPersonalBg
import com.example.ui.theme.CategoryShopping
import com.example.ui.theme.CategoryShoppingBg
import com.example.ui.theme.CategoryUrgent
import com.example.ui.theme.CategoryUrgentBg
import com.example.ui.theme.CategoryWork
import com.example.ui.theme.CategoryWorkBg
import com.example.ui.theme.LightCategoryBills
import com.example.ui.theme.LightCategoryBillsBg
import com.example.ui.theme.LightCategoryHealth
import com.example.ui.theme.LightCategoryHealthBg
import com.example.ui.theme.LightCategoryOther
import com.example.ui.theme.LightCategoryOtherBg
import com.example.ui.theme.LightCategoryPersonal
import com.example.ui.theme.LightCategoryPersonalBg
import com.example.ui.theme.LightCategoryShopping
import com.example.ui.theme.LightCategoryShoppingBg
import com.example.ui.theme.LightCategoryUrgent
import com.example.ui.theme.LightCategoryUrgentBg
import com.example.ui.theme.LightCategoryWork
import com.example.ui.theme.LightCategoryWorkBg

@Composable
fun getCategoryColor(category: TaskCategory): Color {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        when (category) {
            TaskCategory.WORK -> CategoryWork
            TaskCategory.PERSONAL -> CategoryPersonal
            TaskCategory.URGENT -> CategoryUrgent
            TaskCategory.BILLS -> CategoryBills
            TaskCategory.SHOPPING -> CategoryShopping
            TaskCategory.HEALTH -> CategoryHealth
            TaskCategory.OTHER -> CategoryOther
        }
    } else {
        when (category) {
            TaskCategory.WORK -> LightCategoryWork
            TaskCategory.PERSONAL -> LightCategoryPersonal
            TaskCategory.URGENT -> LightCategoryUrgent
            TaskCategory.BILLS -> LightCategoryBills
            TaskCategory.SHOPPING -> LightCategoryShopping
            TaskCategory.HEALTH -> LightCategoryHealth
            TaskCategory.OTHER -> LightCategoryOther
        }
    }
}

@Composable
fun getCategoryBgColor(category: TaskCategory): Color {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        when (category) {
            TaskCategory.WORK -> CategoryWorkBg
            TaskCategory.PERSONAL -> CategoryPersonalBg
            TaskCategory.URGENT -> CategoryUrgentBg
            TaskCategory.BILLS -> CategoryBillsBg
            TaskCategory.SHOPPING -> CategoryShoppingBg
            TaskCategory.HEALTH -> CategoryHealthBg
            TaskCategory.OTHER -> CategoryOtherBg
        }
    } else {
        when (category) {
            TaskCategory.WORK -> LightCategoryWorkBg
            TaskCategory.PERSONAL -> LightCategoryPersonalBg
            TaskCategory.URGENT -> LightCategoryUrgentBg
            TaskCategory.BILLS -> LightCategoryBillsBg
            TaskCategory.SHOPPING -> LightCategoryShoppingBg
            TaskCategory.HEALTH -> LightCategoryHealthBg
            TaskCategory.OTHER -> LightCategoryOtherBg
        }
    }
}

fun getCategoryIcon(category: TaskCategory): ImageVector {
    return when (category) {
        TaskCategory.WORK -> Icons.Default.Work
        TaskCategory.PERSONAL -> Icons.Default.Person
        TaskCategory.URGENT -> Icons.Default.Warning
        TaskCategory.BILLS -> Icons.AutoMirrored.Filled.ReceiptLong
        TaskCategory.SHOPPING -> Icons.Default.ShoppingCart
        TaskCategory.HEALTH -> Icons.Default.Favorite
        TaskCategory.OTHER -> Icons.Default.Category
    }
}

@Composable
fun CategoryFilterBar(
    selectedCategory: TaskCategory?,
    items: List<TaskPulseItem>,
    onCategorySelected: (TaskCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "All" Pill
        val isAllSelected = selectedCategory == null
        val allBgColor by animateColorAsState(
            targetValue = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "all_bg"
        )
        val allTextColor by animateColorAsState(
            targetValue = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            label = "all_text"
        )

        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { onCategorySelected(null) }
                .testTag("filter_category_all"),
            shape = RoundedCornerShape(16.dp),
            color = allBgColor,
            border = BorderStroke(
                1.dp,
                if (isAllSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isAllSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = allTextColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "All",
                    style = MaterialTheme.typography.labelLarge,
                    color = allTextColor,
                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isAllSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = items.size.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = allTextColor
                    )
                }
            }
        }

        // Each Category Pill
        TaskCategory.entries.forEach { category ->
            val isSelected = selectedCategory == category
            val catColor = getCategoryColor(category)
            val catBg = getCategoryBgColor(category)
            val count = items.count { it.taskCategory == category }

            val itemBgColor by animateColorAsState(
                targetValue = if (isSelected) catBg else MaterialTheme.colorScheme.surface,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "cat_bg_${category.name}"
            )
            val itemTextColor by animateColorAsState(
                targetValue = if (isSelected) catColor else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "cat_text_${category.name}"
            )

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onCategorySelected(category) }
                    .testTag("filter_category_${category.name.lowercase()}"),
                shape = RoundedCornerShape(16.dp),
                color = itemBgColor,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) catColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = null,
                        tint = itemTextColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = itemTextColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) catColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = count.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = itemTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}
