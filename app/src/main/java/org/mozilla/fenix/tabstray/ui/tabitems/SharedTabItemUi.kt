/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.tabstray.ui.tabitems

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Indication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.RadioCheckmark
import mozilla.components.compose.base.RadioCheckmarkColors
import mozilla.components.compose.base.button.IconButton
import mozilla.components.compose.base.menu.DropdownMenu
import mozilla.components.compose.base.menu.MenuItem
import mozilla.components.compose.base.modifier.thenConditional
import mozilla.components.compose.base.text.Text
import mozilla.components.compose.base.theme.AcornCorners
import mozilla.components.compose.base.theme.layout.AcornLayout
import mozilla.components.support.utils.ext.isLandscape
import mozilla.components.ui.colors.NovaColors
import org.mozilla.fenix.R
import org.mozilla.fenix.tabstray.LocalTabManagementFeatureHelper
import org.mozilla.fenix.tabstray.TabsTrayTestTag
import org.mozilla.fenix.tabstray.browser.compose.TabItemInteractionState
import org.mozilla.fenix.tabstray.data.TabsTrayItem
import org.mozilla.fenix.theme.FirefoxTheme
import kotlin.math.abs
import mozilla.components.ui.icons.R as iconsR

// Rounded corner shape used by all tab items
val tabContentCardShape: CornerBasedShape
    @Composable
    get() = MaterialTheme.shapes.large

// The corner radius of a tab card's top outer edge
val tabCardTopCornerRadius: CornerSize
    @Composable
    get() = MaterialTheme.shapes.extraSmall.topStart

// The corner radius of a tab card's bottom outer edge
val tabCardBottomCornerRadius: CornerSize
    @Composable
    get() = MaterialTheme.shapes.medium.bottomStart

// Rounded shape used for tab thumbnails
val thumbnailShape: Shape
    @Composable
    get() = RoundedCornerShape(
        topStart = tabCardTopCornerRadius,
        topEnd = tabCardTopCornerRadius,
        bottomStart = tabCardBottomCornerRadius,
        bottomEnd = tabCardBottomCornerRadius,
    )

// The touch target size of a tab's header icon
val TabHeaderIconTouchTargetSize = 40.dp

val TabListFirstItemShape: Shape
    @Composable
    get() = MaterialTheme.shapes.medium.copy(
        bottomStart = CornerSize(0.dp),
        bottomEnd = CornerSize(0.dp),
    )

val TabListLastItemShape: Shape
    @Composable
    get() = MaterialTheme.shapes.medium.copy(
        topStart = CornerSize(0.dp),
        topEnd = CornerSize(0.dp),
    )

val TabListSingleItemShape: Shape
    @Composable
    get() = MaterialTheme.shapes.medium

val TabListBorderMiddleItemShape: Shape
    @Composable
    get() = RectangleShape

/**
 * Border drawn around a tab list item's thumbnail.
 */
val tablistItemThumbnailBorder: BorderStroke
    @Composable
    @ReadOnlyComposable
    get() = BorderStroke(
        width = AcornLayout.AcornBorder.default,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

/**
 * Shape information for a tab item displayed in a list.
 *
 * @property borderShape: The [Shape] representing the item's border.
 * @property clipTabToFit Whether the item content should be clipped to [borderShape].
 */
data class TabListShapeInfo(
    val borderShape: Shape,
    val clipTabToFit: Boolean,
)

/**
 * @param isSelected: Whether the tab is selected in multiselect mode
 * @param uncheckedBorderColor: The border color to display when the item is unchecked
 */
@Composable
fun MultiSelectTabButton(
    isSelected: Boolean,
    uncheckedBorderColor: Color = RadioCheckmarkColors.default().borderColor,
) {
    Box(
        modifier = Modifier.size(TabHeaderIconTouchTargetSize),
        contentAlignment = Alignment.Center,
    ) {
        RadioCheckmark(
            isSelected = isSelected,
            colors = RadioCheckmarkColors.default(borderColor = uncheckedBorderColor),
        )
    }
}

/**
 * The clickable modifier for tab items.
 * @param clickHandler: ClickHandler object that responds to click events
 * @param clickedItem: The generic TabTray item that is being interacted with
 */
@Composable
fun Modifier.tabItemClickable(
    clickHandler: TabsTrayItemClickHandler,
    clickedItem: TabsTrayItem,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }

    if (clickHandler.onLongClick == null) {
        Modifier.clickable(
            enabled = clickHandler.enabled,
            interactionSource = interactionSource,
            indication = clickRipple,
            onClick = { clickHandler.onClick(clickedItem) },
        )
    } else {
        Modifier.combinedClickable(
            enabled = clickHandler.enabled,
            interactionSource = interactionSource,
            indication = clickRipple,
            onLongClick = { clickHandler.onLongClick(clickedItem) },
            onClick = { clickHandler.onClick(clickedItem) },
        )
    }
}

private val clickRipple: Indication
    @Composable get() = ripple(
        color = when (isSystemInDarkTheme()) {
            true -> NovaColors.White
            false -> NovaColors.Black
        },
    )

/**
 * The width to height ratio of the tab grid item. In landscape mode, the width to height ratio is
 * 1:1 and in portrait mode, the width to height ratio is 4:5.
 */
val gridItemAspectRatio: Float
    @Composable
    @ReadOnlyComposable
    get() = if (LocalContext.current.isLandscape()) {
        1f
    } else {
        0.8f
    }

/**
 * Renders the three dot button and its menu items for [TabsTrayItem.TabGroup] views.
 * @param modifier: The Modifier parameter
 * @param includeCloseOption: Whether to include the "Close" dropdown item in the menu item list.
 * @param includeUngroupOption: Whether this surface wants the "Ungroup" dropdown item. The item is only
 * shown when [TabManagementFeatureHelper.ungroupTabGroupEnabled] is also true.
 * @param onDeleteTabGroupClick Invoked when the user clicks on delete tab group.
 * @param onEditTabGroupClick Invoked when the user clicks to edit the selected tab group.
 * @param onCloseTabGroupClick Invoked when the user clicks to close the tab group.
 * @param onShareTabGroupClick Invoked when the user clicks to share the tab group.
 * @param onUngroupTabGroupClick Invoked when the user clicks to ungroup the tab group.
 */
@Composable
fun TabGroupMenuButton(
    modifier: Modifier = Modifier,
    includeCloseOption: Boolean = false,
    includeUngroupOption: Boolean = false,
    onDeleteTabGroupClick: () -> Unit,
    onEditTabGroupClick: () -> Unit,
    onCloseTabGroupClick: () -> Unit,
    onShareTabGroupClick: () -> Unit,
    onUngroupTabGroupClick: () -> Unit,
) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    IconButton(
        onClick = {
            showDropdownMenu = true
        },
        contentDescription = stringResource(R.string.tab_group_three_dot_button_content_description),
        modifier = modifier
            .testTag(TabsTrayTestTag.TAB_GROUP_THREE_DOT_BUTTON),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = LocalContentColor.current,
        ),
    ) {
        Icon(
            painter = painterResource(id = iconsR.drawable.mozac_ic_ellipsis_vertical_24),
            contentDescription = null,
        )

        DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false },
            menuItems = generateTabGroupMenuItems(
                includeCloseOption = includeCloseOption,
                editTabGroup = onEditTabGroupClick,
                closeTabGroup = onCloseTabGroupClick,
                shareTabGroup = onShareTabGroupClick,
                deleteTabGroup = onDeleteTabGroupClick,
                includeUngroupOption = includeUngroupOption &&
                    LocalTabManagementFeatureHelper.current.ungroupTabGroupEnabled,
                ungroupTabGroup = onUngroupTabGroupClick,
            ),
        )
    }
}

/**
 * The trailing dismiss/close button in the list presentation of tab tray items.
 *
 * @param contentDescription Accessibility label describing the dismiss action.
 * @param modifier The [Modifier] applied to the button.
 * @param onClick Invoked when the dismiss button is clicked.
 */
@Composable
fun ListItemDismissButton(
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        contentDescription = contentDescription,
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            painter = painterResource(id = iconsR.drawable.mozac_ic_cross_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun generateTabGroupMenuItems(
    includeCloseOption: Boolean = false,
    includeUngroupOption: Boolean = false,
    editTabGroup: () -> Unit,
    closeTabGroup: () -> Unit,
    shareTabGroup: () -> Unit,
    deleteTabGroup: () -> Unit,
    ungroupTabGroup: () -> Unit,
): List<MenuItem> {
    val editItem = MenuItem.IconItem(
        text = Text.Resource(R.string.tab_group_three_dot_menu_edit),
        drawableRes = iconsR.drawable.mozac_ic_edit_24,
        testTag = TabsTrayTestTag.EDIT_TAB_GROUP,
        onClick = editTabGroup,
    )
    val closeItem = MenuItem.IconItem(
        text = Text.Resource(R.string.tab_group_three_dot_menu_close),
        drawableRes = iconsR.drawable.mozac_ic_tab_group_close_24,
        testTag = TabsTrayTestTag.CLOSE_TAB_GROUP,
        onClick = closeTabGroup,
    )
    val ungroupItem = MenuItem.IconItem(
        text = Text.Resource(R.string.tab_group_three_dot_menu_ungroup),
        drawableRes = iconsR.drawable.mozac_ic_tab_ungroup_24,
        testTag = TabsTrayTestTag.UNGROUP_TAB_GROUP,
        onClick = ungroupTabGroup,
    )
    val shareItem = MenuItem.IconItem(
        text = Text.Resource(R.string.tab_group_three_dot_menu_share),
        drawableRes = iconsR.drawable.mozac_ic_share_android_24,
        testTag = TabsTrayTestTag.SHARE_TAB_GROUP,
        onClick = shareTabGroup,
    )
    val deleteItem = MenuItem.IconItem(
        text = Text.Resource(R.string.tab_group_three_dot_menu_delete),
        drawableRes = iconsR.drawable.mozac_ic_delete_24,
        testTag = TabsTrayTestTag.DELETE_TAB_GROUP,
        onClick = deleteTabGroup,
        level = MenuItem.FixedItem.Level.Critical,
    )
    return buildList {
        add(editItem)
        if (includeCloseOption) {
            add(closeItem)
        }
        add(shareItem)
        if (includeUngroupOption) {
            add(ungroupItem)
        }
        add(deleteItem)
    }
}

// Long text string for verifying that tab items handle long titles with appropriate truncation.
const val LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
    "eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis " +
    "nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute " +
    "irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla " +
    "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia " +
    "deserunt mollit anim id est laborum."

/**
 * Renders a border around a [TabsTrayItem] to signify that it is in focus.
 * When the tab is not in focus, its BorderStroke will be null.
 */
@Composable
@ReadOnlyComposable
fun tabItemConditionalBorder(selectionState: TabsTrayItemSelectionState): BorderStroke? {
    return if (selectionState.isFocused && selectionState.focusEnabled) {
        tabItemBorderFocused()
    } else {
        null
    }
}

/**
 * Renders a border around a [TabsTrayItem] to signify that it is in focus.
 */
@Composable
@ReadOnlyComposable
fun tabItemBorderFocused(): BorderStroke {
    return BorderStroke(width = FirefoxTheme.layout.border.heaviest, brush = FirefoxTheme.gradients.tabOutline.brush)
}

/**
 * Applies tab list item styling, provided the shape information.
 *
 * @param tabShapeInfo The list item shape and clipping behavior.
 * @param selectionState the selection state of the item in the tabstray.
 */
@Composable
fun Modifier.tabListItemShapeStyling(
    tabShapeInfo: TabListShapeInfo,
    selectionState: TabsTrayItemSelectionState,
): Modifier {
    return this
        .thenConditional(
            Modifier.clip(tabShapeInfo.borderShape),
            { tabShapeInfo.clipTabToFit },
        )
        .thenConditional(
            modifier = Modifier.border(
                border = tabItemBorderFocused(),
                shape = tabShapeInfo.borderShape,
            ),
            { (selectionState.isFocused && selectionState.focusEnabled) },
        )
}

/**
 * Returns the container color used by tab grid items.
 */
@Composable
fun tabGridItemContainerColor(selectionState: TabsTrayItemSelectionState): Color {
    return if (selectionState.isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceBright
    }
}

/**
 * Object holding alpha values for tab items
 */
object Alpha {
    const val TAB_ITEM_DRAGGED = 0.7f
    const val TAB_ITEM_NO_INTERACTION = 1f
    const val TAB_ITEM_MIN_SWIPE_FADE = 0.1f
}

/**
 * Animates the tab item's alpha value to be slightly transparent when it is dragged.
 */
@Composable
private fun tabGridItemAnimatedAlpha(interactionState: TabItemInteractionState): State<Float> {
    return animateFloatAsState(
        targetValue = if (interactionState.isDragged) {
            Alpha.TAB_ITEM_DRAGGED
        } else {
            Alpha.TAB_ITEM_NO_INTERACTION
        },
        label = "TabGridItemAlpha",
    )
}

/**
 * Animates the tab item's alpha value to be slightly transparent when it is dragged, after being moved.
 */
@Composable
private fun tabListItemAnimatedAlpha(interactionState: TabItemInteractionState): State<Float> {
    return animateFloatAsState(
        targetValue = if (interactionState.isDragged && !interactionState.isHeld) {
            Alpha.TAB_ITEM_DRAGGED
        } else {
            Alpha.TAB_ITEM_NO_INTERACTION
        },
        label = "TabListItemAlpha",
    )
}

/**
 * Animates the tab item's size to be slightly reduced when it is dragged.
 */
@Composable
private fun tabGridItemAnimatedScale(interactionState: TabItemInteractionState): State<Float> {
    val targetValue = when {
        interactionState.isDragged -> Scale.DRAG_ACTIVE
        interactionState.isHoveredByItem -> Scale.HOVER_ACTIVE
        else -> Scale.NO_INTERACTION
    }
    return animateFloatAsState(
        targetValue = targetValue,
        label = "TabGridItemScale",
    )
}

/**
 * Animates the tab item's size to be slightly reduced when it is dragged, after being moved.
 */
@Composable
private fun tabListItemAnimatedScale(interactionState: TabItemInteractionState): State<Float> {
    val targetValue = when {
        interactionState.isHeld -> Scale.NO_INTERACTION
        interactionState.isDragged -> Scale.DRAG_ACTIVE
        interactionState.isHoveredByItem -> Scale.HOVER_ACTIVE_LIST
        else -> Scale.NO_INTERACTION
    }
    return animateFloatAsState(
        targetValue = targetValue,
        label = "TabListItemScale",
    )
}

/**
 * Renders an animated scale and alpha transition for the tab grid item based on its interaction state.
 * This happens at the graphics layer to avoid recomposition of the item.
 * The semantics properties are provided so that the state can be evaluated, as evaluating the composable will not
 * return the correct result, since these graphical animations occur at draw time.
 * The list and grid animations differ slightly in terms of scale and corner radius.
 * @param interactionState: State holding the hovered and dragged statuses.
 */
@Composable
fun Modifier.tabItemGridInteractionAnimation(interactionState: TabItemInteractionState): Modifier {
    return this.tabItemInteractionAnimation(
        tabItemScaleState = tabGridItemAnimatedScale(interactionState),
        tabItemAlphaState = tabGridItemAnimatedAlpha(interactionState),
        cornerSize = AcornCorners.large,
        interactionState = interactionState,
    )
}

/**
 * Renders an animated scale and alpha transition for the tab list group item based on its interaction state.
 * This happens at the graphics layer to avoid recomposition of the item.
 * The semantics properties are provided so that the state can be evaluated, as evaluating the composable will not
 * return the correct result, since these graphical animations occur at draw time.
 * The list and grid animations differ slightly in terms of scale and corner radius.
 * @param interactionState: State holding the hovered and dragged statuses.
 * @param key The item's key
 * @param onGroupEntranceAnimationPlayed Invoked when the group's entrance animation is played.
 */
@Composable
fun Modifier.tabItemGroupListInteractionAnimation(
    interactionState: TabItemInteractionState,
    key: String? = null,
    onGroupEntranceAnimationPlayed: () -> Unit = {},
): Modifier {
    val interactionScale = tabListItemAnimatedScale(interactionState)
    val entranceScale = tabGroupAppearanceScale(
        interactionState = interactionState,
        key = key,
        onGroupEntranceAnimationPlayed = onGroupEntranceAnimationPlayed,
    )
    val combinedScale = remember { derivedStateOf { interactionScale.value * entranceScale.value } }
    return this.tabItemInteractionAnimation(
        tabItemScaleState = combinedScale,
        tabItemAlphaState = tabListItemAnimatedAlpha(interactionState),
        cornerSize = AcornCorners.medium,
        interactionState = interactionState,
    )
}

/**
 * Renders an animated scale and alpha transition for the tab list item based on its interaction state.
 * This happens at the graphics layer to avoid recomposition of the item.
 * The semantics properties are provided so that the state can be evaluated, as evaluating the composable will not
 * return the correct result, since these graphical animations occur at draw time.
 * The list and grid animations differ slightly in terms of scale and corner radius.
 * @param interactionState: State holding the hovered and dragged statuses.
 */
@Composable
fun Modifier.tabItemListInteractionAnimation(interactionState: TabItemInteractionState): Modifier {
    return this.tabItemInteractionAnimation(
        tabItemScaleState = tabListItemAnimatedScale(interactionState),
        tabItemAlphaState = tabListItemAnimatedAlpha(interactionState),
        cornerSize = AcornCorners.medium,
        interactionState = interactionState,
    )
}

/**
 * Provides the alpha state for the tab group appearance animation.
 * @param interactionState The tab interaction state
 * @param key The tab item's key
 */
@Composable
private fun tabGroupAppearanceAlpha(
    interactionState: TabItemInteractionState,
    key: String?,
): State<Float> {
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(key, interactionState.isEnteringGroup) {
        if (interactionState.isEnteringGroup) {
            alpha.snapTo(targetValue = 0f)
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
            )
        }
    }
    return alpha.asState()
}

/**
 * Provides the scale state for the tab group appearance animation.
 * @param interactionState The tab interaction state
 * @param key The tab item's key
 * @param onGroupEntranceAnimationPlayed Invoked when the animation is finished playing
 */
@Composable
private fun tabGroupAppearanceScale(
    interactionState: TabItemInteractionState,
    key: String?,
    onGroupEntranceAnimationPlayed: () -> Unit,
): State<Float> {
    // This must be the default, else all group items will be incorrectly scaled
    val scale = remember { Animatable(Scale.NO_INTERACTION) }
    LaunchedEffect(key, interactionState.isEnteringGroup) {
        if (interactionState.isEnteringGroup) {
            scale.snapTo(targetValue = Scale.NEW_GROUP_ENTRANCE_START)
            scale.animateTo(
                targetValue = Scale.NEW_GROUP_ENTRANCE_PEAK,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            scale.animateTo(
                targetValue = Scale.NO_INTERACTION,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            onGroupEntranceAnimationPlayed()
        }
    }
    return scale.asState()
}

/**
 * Plays the entrance animation for a tab group.
 * @param interactionState The tab item's interaction state
 * @param key The tab item's key
 * @param onGroupEntranceAnimationPlayed Invoked when the animation is finished playing.
 */
@Composable
internal fun Modifier.tabGroupEntranceAnimation(
    interactionState: TabItemInteractionState,
    key: String?,
    onGroupEntranceAnimationPlayed: () -> Unit,
): Modifier {
    if (!interactionState.isEnteringGroup) return this
    val entranceScale = tabGroupAppearanceScale(
        interactionState = interactionState,
        key = key,
        onGroupEntranceAnimationPlayed = onGroupEntranceAnimationPlayed,
    )
    val entranceAlpha = tabGroupAppearanceAlpha(interactionState = interactionState, key = key)
    return this.graphicsLayer {
        scaleX = entranceScale.value
        scaleY = entranceScale.value
        alpha = entranceAlpha.value
    }
}

/**
 * Renders an animated scale and alpha transition for the tab item based on its interaction state.
 * This happens at the graphics layer to avoid recomposition of the item.
 * The semantics properties are provided so that the state can be evaluated, as evaluating the composable will not
 * return the correct result, since these graphical animations occur at draw time.
 */
@Composable
private fun Modifier.tabItemInteractionAnimation(
    tabItemAlphaState: State<Float>,
    tabItemScaleState: State<Float>,
    cornerSize: Dp,
    interactionState: TabItemInteractionState,
): Modifier {
    val backdropColor = MaterialTheme.colorScheme.secondaryContainer
    val backdropBorder = MaterialTheme.colorScheme.tertiary
    val borderSize = FirefoxTheme.layout.border.heaviest
    return this
        .thenConditional(
            Modifier.drawBehind(
                {
                    // A Stroke rect is centered on the shape edge, which will spill outside the drawing area
                    // To make the border match other tabs, we must inset by half the stroke width, and adjust the size
                    val inset = borderSize.toPx() / 2
                    val shapeOffset = Offset(x = inset, y = inset)
                    val shapeSize = Size(this.size.width - shapeOffset.x * 2, this.size.height - shapeOffset.y * 2)
                    drawRoundRect(
                        color = backdropColor,
                        topLeft = shapeOffset,
                        size = shapeSize,
                        cornerRadius = CornerRadius(cornerSize.toPx()),
                    )
                    drawRoundRect(
                        color = backdropBorder,
                        topLeft = shapeOffset,
                        size = shapeSize,
                        cornerRadius = CornerRadius(cornerSize.toPx()),
                        style = Stroke(width = borderSize.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                },
            ),
            { interactionState.isHoveredByItem },
        )
        .graphicsLayer {
            alpha = tabItemAlphaState.value
            scaleX = tabItemScaleState.value
            scaleY = tabItemScaleState.value
        }
        .semantics {
            scale = tabItemScaleState.value
            alpha = tabItemAlphaState.value
        }
}

/**
 * The default animations for a tab GridItem.
 * @param lazyGridItemScope The [LazyGridItemScope] (needed to define animateItem())
 * @param enteringGroupId The id of the group entering composition, if any.  Can be null.
 */
@Composable
fun Modifier.defaultGridItemAnimation(
    lazyGridItemScope: LazyGridItemScope,
    enteringGroupId: String?,
): Modifier = with(lazyGridItemScope) {
    /*
     * We need to explicitly set each of the LazyGrid animations to NULL to prevent some defaults
     * from occurring while the group entrance animation is playing.  Items are by default
     * clipped to their bounds while fade in/out animations are playing, and the group animation
     * scales to overshoot its bounds.
     *
     * Additionally, per the spec, we don't want to see 'ghost' items of the tabs that are being
     * combined to show the group, and the group should start at its placed position.
     */
    this@defaultGridItemAnimation.animateItem(
        fadeOutSpec = if (enteringGroupId != null) {
            null
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        },
        placementSpec = if (enteringGroupId != null) {
            null
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        },
        fadeInSpec = if (enteringGroupId != null) {
            null
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        },
    )
}

/**
 * The default animations for a tab ListItem.
 * @param lazyListItemScope The [LazyItemScope] (needed to define animateItem())
 * @param enteringGroupId The id of the group entering composition, if any.  Can be null.
 */
@Composable
fun Modifier.defaultListItemAnimation(
    lazyListItemScope: LazyItemScope,
    enteringGroupId: String?,
): Modifier = with(lazyListItemScope) {
    this@defaultListItemAnimation.animateItem(
        // When the group entrance animation is playing, all fade-out animations should be suppressed.
        // You should not see the exiting tabs fade out that are becoming a group.
        fadeOutSpec = if (enteringGroupId != null) {
            null
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        },
        // When the group entrance animation is playing, all grid shuffle animations should be suppressed.
        // The group should appear to enter at the place it was dropped (without translating up/down/left/right).
        // Nearby tabs should not appear to shuffle to make room for the group.
        placementSpec = if (enteringGroupId != null) {
            null
        } else {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        },
        fadeInSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
    )
}

/**
 * Creates a [SwipeToDismissBoxState] for the tab item identified by [tabId].
 *
 * Deliberately not [androidx.compose.material3.rememberSwipeToDismissBoxState], which saves its
 * current value: a lazy layout keeps an item's saved state around after the item leaves the list,
 * so a tab restored through the undo snackbar would return as swiped away
 * and be dismissed again on its first composition.
 *
 * @param tabId The id of the tab the state belongs to.
 */
@Composable
fun rememberTabSwipeToDismissBoxState(tabId: String): SwipeToDismissBoxState {
    val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold

    return remember(tabId) {
        SwipeToDismissBoxState(
            initialValue = SwipeToDismissBoxValue.Settled,
            positionalThreshold = positionalThreshold,
        )
    }
}

/**
 * Custom modifier that fades an item to transparent as it is swiped to dismiss.
 * The minimum alpha is 10%, so the item will at least be 10% visible.
 */
fun Modifier.fadeOnSwipeToDismiss(state: SwipeToDismissBoxState) = graphicsLayer {
    // state.progress is tied to targetValue which has a fixed threshold,
    // so we need to pull the offset to get a linear fade animation.
    val offset = try {
        if (state.dismissDirection == SwipeToDismissBoxValue.Settled) {
        0f
        } else {
            state.requireOffset()
        }
    } catch (e: IllegalStateException) {
        e.printStackTrace()
        // It should be safe to call requireOffset() here, but there's no
        // reason to risk a crash for a fade animation.
        0f
    }
    alpha = swipeFadeAlpha(offset = offset, width = size.width)
}

internal fun swipeFadeAlpha(offset: Float, width: Float): Float {
    return if (width <= 0f || offset.isNaN()) {
        Alpha.TAB_ITEM_NO_INTERACTION
    } else {
        maxOf(
            Alpha.TAB_ITEM_MIN_SWIPE_FADE,
            Alpha.TAB_ITEM_NO_INTERACTION - (abs(offset) / width).coerceIn(0f, 1f),
        )
    }
}

/**
 * Semantic property for accessing a Composable item's current graphical scale property.
 * This is intended to be applied evenly across X and Y and set and fetched as needed for verification.
 */
internal val ScaleKey = SemanticsPropertyKey<Float>("Scale")
internal var SemanticsPropertyReceiver.scale by ScaleKey

/**
 * Semantic property for accessing a Composable item's alpha property.
 * This is intended to be set and fetched as needed for verification.
 */
internal val AlphaKey = SemanticsPropertyKey<Float>("Alpha")
internal var SemanticsPropertyReceiver.alpha by AlphaKey

/**
 * Semantic property for accessing a tab grid's column count.
 */
internal val TabGridColumnCountKey = SemanticsPropertyKey<Int>("TabGridColumnCount")
internal var SemanticsPropertyReceiver.tabGridColumnCount by TabGridColumnCountKey

/**
 * Elevation parameters for interactable tab items.
 */
object Elevation {
    const val SWIPE_ACTIVE = 10f
    const val ENTERING_ITEM = 2f
    const val DRAGGED_ITEM = 1f
    const val NO_INTERACTION = 0f
}

/**
 * Scale parameters for interactable tab items.
 */
object Scale {
    const val DRAG_ACTIVE = 0.75f
    const val HOVER_ACTIVE = 0.75f
    const val HOVER_ACTIVE_LIST = 0.90f
    const val NO_INTERACTION = 1f
    const val NEW_GROUP_ENTRANCE_START = 0.92f
    const val NEW_GROUP_ENTRANCE_PEAK = 1.06f
}
