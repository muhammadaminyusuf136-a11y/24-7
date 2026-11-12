package uz.reja24.app.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.reja24.app.R
import uz.reja24.app.core.util.TimeFormat
import uz.reja24.app.presentation.components.LockBanner
import uz.reja24.app.core.util.InstalledApp
import uz.reja24.app.presentation.theme.RejaTheme

@Composable
fun AppPickerScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SubScreenTopBar(title = stringResource(R.string.apps_title), onBack = onBack)
        AppPickerContent(modifier = Modifier.fillMaxSize())
    }
}

/** Qidiruv + ilovalar ro'yxati (belgilash bilan). Tanishuv ekranida ham ishlatiladi. */
@Composable
fun AppPickerContent(
    modifier: Modifier = Modifier,
    viewModel: AppPickerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.queryText.collectAsStateWithLifecycle()
    val colors = RejaTheme.colors

    Column(modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text(stringResource(R.string.apps_search)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.accent,
                cursorColor = colors.accent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )
        Text(
            text = stringResource(R.string.apps_selected_count, state.blocked.size.toString()),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textMuted,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        state.lockedUntil?.let { until ->
            LockBanner(
                text = stringResource(R.string.apps_locked_banner, TimeFormat.time(until)),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.apps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        checked = app.packageName in state.blocked,
                        loadIcon = viewModel::icon,
                        onToggle = { viewModel.toggle(app) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun AppRow(
    app: InstalledApp,
    checked: Boolean,
    loadIcon: suspend (String, Int) -> ImageBitmap?,
    onToggle: () -> Unit
) {
    val colors = RejaTheme.colors
    val iconSizePx = with(LocalDensity.current) { 36.dp.roundToPx() }
    val icon by produceState<ImageBitmap?>(initialValue = null, app.packageName) {
        value = loadIcon(app.packageName, iconSizePx)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(colors.surface, RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap = icon
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(36.dp))
        } else {
            Icon(Icons.Outlined.Apps, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = colors.accent)
        )
    }
}
