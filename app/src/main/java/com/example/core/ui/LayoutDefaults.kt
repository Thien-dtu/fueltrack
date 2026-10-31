package com.example.core.ui

import androidx.compose.ui.unit.dp

/**
 * Space reserved at the end of scrolling content so the last item is not hidden
 * behind the floating action button.
 *
 * The FAB floats above the NavHost and therefore does not contribute to the
 * Scaffold's inner padding. A fixed 80.dp Spacer was previously appended as the
 * last list item, which both duplicated this constant across five screens and
 * ignored the device's navigation-bar inset.
 */
val FabClearance = 88.dp
