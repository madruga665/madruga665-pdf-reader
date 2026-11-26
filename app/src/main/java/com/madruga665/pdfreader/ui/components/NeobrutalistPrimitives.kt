package com.madruga665.pdfreader.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.madruga665.pdfreader.ui.theme.NeobrutalismMetrics
import com.madruga665.pdfreader.ui.theme.NeobrutalismTheme
import com.madruga665.pdfreader.ui.theme.neobrutalistShadow

@Composable
fun NeobrutalistButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NeobrutalismTheme.colors.accentYellow,
    contentColor: Color = NeobrutalismTheme.colors.onAccent,
    borderColor: Color = NeobrutalismTheme.colors.border,
    shadowColor: Color = NeobrutalismTheme.colors.shadow,
    shape: Shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusMedium),
    borderWidth: Dp = NeobrutalismMetrics.BorderWidth,
    shadowOffset: Dp = NeobrutalismMetrics.ShadowOffset,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    enabled: Boolean = true
) {
    NeobrutalistButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        borderColor = borderColor,
        shadowColor = shadowColor,
        shape = shape,
        borderWidth = borderWidth,
        shadowOffset = shadowOffset,
        contentPadding = contentPadding,
        enabled = enabled
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun NeobrutalistButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NeobrutalismTheme.colors.accentYellow,
    contentColor: Color = NeobrutalismTheme.colors.onAccent,
    borderColor: Color = NeobrutalismTheme.colors.border,
    shadowColor: Color = NeobrutalismTheme.colors.shadow,
    shape: Shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusMedium),
    borderWidth: Dp = NeobrutalismMetrics.BorderWidth,
    shadowOffset: Dp = NeobrutalismMetrics.ShadowOffset,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressedOffset = NeobrutalismMetrics.PressedShadowOffset.coerceAtMost(shadowOffset)
    val targetShadow = when {
        !enabled -> 0.dp
        isPressed -> pressedOffset
        else -> shadowOffset
    }
    val targetTranslation = when {
        !enabled -> 0.dp
        isPressed -> (shadowOffset - pressedOffset).coerceAtLeast(0.dp)
        else -> 0.dp
    }

    val animatedShadow by animateDpAsState(
        targetValue = targetShadow,
        animationSpec = tween(durationMillis = 75),
        label = "neoButtonShadow"
    )
    val animatedTranslation by animateDpAsState(
        targetValue = targetTranslation,
        animationSpec = tween(durationMillis = 75),
        label = "neoButtonOffset"
    )

    val effectiveContainer = if (enabled) containerColor else NeobrutalismTheme.colors.progressTrack
    val effectiveContent = if (enabled) contentColor else NeobrutalismTheme.colors.subtext

    Box(
        modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset),
        propagateMinConstraints = true
    ) {
        Row(
            modifier = Modifier
                .offset {
                    val px = animatedTranslation.roundToPx()
                    IntOffset(px, px)
                }
                .neobrutalistShadow(
                    shadowColor = shadowColor,
                    borderColor = borderColor,
                    borderWidth = borderWidth,
                    shadowOffset = animatedShadow,
                    shape = shape
                )
                .background(effectiveContainer, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides effectiveContent) {
                content()
            }
        }
    }
}

@Composable
fun NeobrutalistIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NeobrutalismTheme.colors.surface,
    contentColor: Color = NeobrutalismTheme.colors.onSurface,
    borderColor: Color = NeobrutalismTheme.colors.border,
    shadowColor: Color = NeobrutalismTheme.colors.shadow,
    size: Dp = 40.dp,
    borderWidth: Dp = NeobrutalismMetrics.BorderWidthCompact,
    shadowOffset: Dp = NeobrutalismMetrics.ShadowOffsetCompact,
    shape: Shape = RoundedCornerShape(10.dp),
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressedOffset = NeobrutalismMetrics.PressedShadowOffset.coerceAtMost(shadowOffset)
    val targetShadow = when {
        !enabled -> 0.dp
        isPressed -> pressedOffset
        else -> shadowOffset
    }
    val targetTranslation = when {
        !enabled -> 0.dp
        isPressed -> (shadowOffset - pressedOffset).coerceAtLeast(0.dp)
        else -> 0.dp
    }

    val animatedShadow by animateDpAsState(
        targetValue = targetShadow,
        animationSpec = tween(durationMillis = 75),
        label = "neoIconBtnShadow"
    )
    val animatedTranslation by animateDpAsState(
        targetValue = targetTranslation,
        animationSpec = tween(durationMillis = 75),
        label = "neoIconBtnOffset"
    )

    val effectiveContainer = if (enabled) containerColor else NeobrutalismTheme.colors.progressTrack
    val effectiveContent = if (enabled) contentColor else NeobrutalismTheme.colors.subtext

    Box(
        modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .offset {
                    val px = animatedTranslation.roundToPx()
                    IntOffset(px, px)
                }
                .neobrutalistShadow(
                    shadowColor = shadowColor,
                    borderColor = borderColor,
                    borderWidth = borderWidth,
                    shadowOffset = animatedShadow,
                    shape = shape
                )
                .background(effectiveContainer, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides effectiveContent) {
                content()
            }
        }
    }
}

@Composable
fun NeobrutalistBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = NeobrutalismTheme.colors.accentBlue,
    contentColor: Color = NeobrutalismTheme.colors.onAccent,
    borderColor: Color = NeobrutalismTheme.colors.border,
    shadowColor: Color = NeobrutalismTheme.colors.shadow,
    borderWidth: Dp = NeobrutalismMetrics.BorderWidthCompact,
    shadowOffset: Dp = NeobrutalismMetrics.ShadowOffsetCompact,
    shape: Shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusSmall),
    leadingIcon: ImageVector? = null
) {
    Box(
        modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset)
    ) {
        Row(
            modifier = Modifier
                .neobrutalistShadow(
                    shadowColor = shadowColor,
                    borderColor = borderColor,
                    borderWidth = borderWidth,
                    shadowOffset = shadowOffset,
                    shape = shape
                )
                .background(containerColor, shape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NeobrutalistCard(
    modifier: Modifier = Modifier,
    containerColor: Color = NeobrutalismTheme.colors.surface,
    borderColor: Color = NeobrutalismTheme.colors.border,
    shadowColor: Color = NeobrutalismTheme.colors.shadow,
    borderWidth: Dp = NeobrutalismMetrics.BorderWidth,
    shadowOffset: Dp = NeobrutalismMetrics.ShadowOffset,
    shape: Shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusLarge),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressedOffset = NeobrutalismMetrics.PressedShadowOffset.coerceAtMost(shadowOffset)
    val animatedShadow by animateDpAsState(
        targetValue = if (onClick != null && isPressed) pressedOffset else shadowOffset,
        animationSpec = tween(durationMillis = 75),
        label = "neoCardShadow"
    )
    val animatedTranslation by animateDpAsState(
        targetValue = if (onClick != null && isPressed) {
            (shadowOffset - pressedOffset).coerceAtLeast(0.dp)
        } else {
            0.dp
        },
        animationSpec = tween(durationMillis = 75),
        label = "neoCardOffset"
    )

    Box(
        modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    val px = animatedTranslation.roundToPx()
                    IntOffset(px, px)
                }
                .neobrutalistShadow(
                    shadowColor = shadowColor,
                    borderColor = borderColor,
                    borderWidth = borderWidth,
                    shadowOffset = animatedShadow,
                    shape = shape
                )
                .background(containerColor, shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else {
                        Modifier
                    }
                ),
            content = content
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NeobrutalistDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = NeobrutalismTheme.colors

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            NeobrutalistCard(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                containerColor = colors.surface,
                borderColor = colors.border,
                shadowColor = colors.shadow,
                borderWidth = NeobrutalismMetrics.BorderWidth,
                shadowOffset = NeobrutalismMetrics.ShadowOffset,
                shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusLarge)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (icon != null) {
                            icon()
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(
                        thickness = NeobrutalismMetrics.BorderWidthCompact,
                        color = colors.border
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    content()

                    Spacer(modifier = Modifier.height(18.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        actions()
                    }
                }
            }
        }
    }
}

@Composable
fun NeobrutalistProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    fillColor: Color = NeobrutalismTheme.colors.accentGreen,
    trackColor: Color = NeobrutalismTheme.colors.progressTrack,
    borderColor: Color = NeobrutalismTheme.colors.border
) {
    val clamped = progress.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(trackColor, shape)
            .border(NeobrutalismMetrics.BorderWidthCompact, borderColor, shape)
            .padding(2.dp)
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .background(fillColor, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun NeobrutalistSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    isError: Boolean = true
) {
    val colors = NeobrutalismTheme.colors
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    ) { snackbarData ->
        val badgeColor = if (isError) colors.accentRed else colors.accentYellow
        val shape = RoundedCornerShape(NeobrutalismMetrics.CornerRadiusMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = NeobrutalismMetrics.ShadowOffset, bottom = NeobrutalismMetrics.ShadowOffset)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .neobrutalistShadow(
                        shadowColor = colors.shadow,
                        borderColor = colors.border,
                        borderWidth = NeobrutalismMetrics.BorderWidth,
                        shadowOffset = NeobrutalismMetrics.ShadowOffset,
                        shape = shape
                    )
                    .background(colors.surface, shape)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(badgeColor, RoundedCornerShape(NeobrutalismMetrics.CornerRadiusSmall))
                        .border(
                            NeobrutalismMetrics.BorderWidthCompact,
                            colors.border,
                            RoundedCornerShape(NeobrutalismMetrics.CornerRadiusSmall)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = colors.onAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = snackbarData.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
