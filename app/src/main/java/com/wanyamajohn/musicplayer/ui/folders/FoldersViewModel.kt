package com.wanyamajohn.musicplayer.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.wanyamajohn.musicplayer.data.prefs.PrefKeys
import com.wanyamajohn.musicplayer.data.prefs.PreferencesRepository
import com.wanyamajohn.musicplayer.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val player: PlayerConnection,
) : ViewModel() {
    val trees: StateFlow<Set<String>> = prefs.observe(PrefKeys.BROWSE_TREES, emptySet())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun addTree(uri: String) { viewModelScope.launch { prefs.set(PrefKeys.BROWSE_TREES, trees.value + uri) } }
    fun removeTree(uri: String) { viewModelScope.launch { prefs.set(PrefKeys.BROWSE_TREES, trees.value - uri) } }

    fun playFiles(files: List<FileEntry>, index: Int) {
        val items = files.map { f ->
            MediaItem.Builder()
                .setMediaId("doc:" + f.uri.hashCode())
                .setUri(f.uri)
                .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(f.uri).build())
                .setMediaMetadata(MediaMetadata.Builder().setTitle(f.name.substringBeforeLast('.')).build())
                .build()
        }
        player.playItems(items, index)
    }
}
