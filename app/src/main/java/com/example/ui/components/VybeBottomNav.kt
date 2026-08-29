package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalVybeColors

enum class VybeTab(
  val title: String,
  val activeIcon: ImageVector,
  val inactiveIcon: ImageVector
) {
  HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
  SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search),
  LIBRARY("Library", Icons.Outlined.LibraryMusic, Icons.Outlined.LibraryMusic),
  SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun VybeBottomNav(
  currentTab: VybeTab,
  onTabSelected: (VybeTab) -> Unit,
  modifier: Modifier = Modifier
) {
  val accent = LocalVybeColors.current.accent

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.background)
  ) {
    HorizontalDivider(
      thickness = 0.5.dp,
      color = MaterialTheme.colorScheme.outlineVariant
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(60.dp)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      VybeTab.values().forEach { tab ->
        val isSelected = tab == currentTab
        val color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier
            .weight(1f)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) {
              onTabSelected(tab)
            }
            .padding(vertical = 4.dp)
            .testTag("nav_tab_${tab.title.lowercase()}")
        ) {
          Icon(
            imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
            contentDescription = tab.title,
            tint = color,
            modifier = Modifier.size(24.dp)
          )
          Text(
            text = tab.title,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(top = 3.dp)
          )
        }
      }
    }
  }
}
