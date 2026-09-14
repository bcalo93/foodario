package com.foodario.core.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import foodario.shared.generated.resources.Res
import foodario.shared.generated.resources.caveat_bold
import foodario.shared.generated.resources.caveat_medium
import foodario.shared.generated.resources.caveat_semibold
import foodario.shared.generated.resources.nunito_medium
import foodario.shared.generated.resources.nunito_regular
import org.jetbrains.compose.resources.Font

val CaveatFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.caveat_medium, FontWeight.Medium),
        Font(Res.font.caveat_semibold, FontWeight.SemiBold),
        Font(Res.font.caveat_bold, FontWeight.Bold),
    )

val NunitoFamily: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.nunito_regular, FontWeight.Normal),
        Font(Res.font.nunito_medium, FontWeight.Medium),
    )

data class FoodarioTypography(
    val displayHand: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
    ),
    val titleHand: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
    ),
    val quantityHand: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
    ),
    val labelHand: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
    ),
    val body: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    val label: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
)

@Composable
fun foodarioTypography(): FoodarioTypography {
    val base = FoodarioTypography()
    return base.copy(
        displayHand = base.displayHand.copy(fontFamily = CaveatFamily),
        titleHand = base.titleHand.copy(fontFamily = CaveatFamily),
        quantityHand = base.quantityHand.copy(fontFamily = CaveatFamily),
        labelHand = base.labelHand.copy(fontFamily = CaveatFamily),
        body = base.body.copy(fontFamily = NunitoFamily),
        label = base.label.copy(fontFamily = NunitoFamily),
        caption = base.caption.copy(fontFamily = NunitoFamily),
    )
}

@Composable
fun FoodarioTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = displayHand,
    headlineMedium = titleHand,
    bodyLarge = body,
    labelLarge = label,
    bodySmall = caption,
)
