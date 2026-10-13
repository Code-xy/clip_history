package com.seagull.clip.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seagull.clip.data.Clip
import com.seagull.clip.data.ClipRepo
import com.seagull.clip.data.Filter
import com.seagull.clip.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ClipViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(Filter.ALL)

    private val _items = MutableStateFlow<List<Clip>>(emptyList())
    val items: StateFlow<List<Clip>> = _items.asStateFlow()

    val count: StateFlow<Int> = ClipRepo.count

    init {
        ClipRepo.init(ctx)
        Prefs.init(ctx)
        viewModelScope.launch {
            combine(query, filter, ClipRepo.rev) { q, f, _ -> q to f }
                .collectLatest { (q, f) ->
                    _items.value = ClipRepo.list(q, f)
                }
        }
        ClipRepo.refreshCount()
    }

    fun setQuery(v: String) { query.value = v }
    fun setFilter(v: Filter) { filter.value = v }

    fun copy(clip: Clip) = ClipRepo.copyToClipboard(ctx, clip.text)
    fun pin(clip: Clip) = ClipRepo.togglePin(clip.id, !clip.pinned)
    fun fav(clip: Clip) = ClipRepo.toggleFav(clip.id, !clip.favorite)
    fun remove(clip: Clip) = ClipRepo.delete(clip.id)
    fun saveText(clip: Clip, text: String) = ClipRepo.updateText(clip.id, text)
    fun clearAll() = ClipRepo.clearAll()
    fun clearKeepStarred() = ClipRepo.clearKeepStarred()
    fun refresh() = ClipRepo.refreshCount()
}
