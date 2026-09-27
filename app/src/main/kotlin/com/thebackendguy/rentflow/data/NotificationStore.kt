package com.thebackendguy.rentflow.data

import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.Network
import com.thebackendguy.rentflow.data.remote.NotificationDto
import com.thebackendguy.rentflow.data.remote.apiCall
import com.thebackendguy.rentflow.data.session.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The notification list in the bell's sheet, one page of 20 at a time. */
data class NotificationFeed(
    val items: List<NotificationDto> = emptyList(),
    val total: Int = 0,
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val moreError: String? = null
) {
    val hasMore: Boolean get() = loaded && items.size < total
    val newCount: Int get() = items.count { !it.isRead }
}

/**
 * The bell's unread count and the notification list. Opening the list marks
 * everything read on the server, so the list is only fetched when the sheet opens.
 */
object NotificationStore {
    const val PAGE_SIZE = 20

    private val api get() = Network.notifications
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _unread = MutableStateFlow(0)
    private val _feed = MutableStateFlow(NotificationFeed())
    private var countJob: Job? = null
    private var feedJob: Job? = null

    val unread: StateFlow<Int> = _unread.asStateFlow()
    val feed: StateFlow<NotificationFeed> = _feed.asStateFlow()

    /** Reloads the bell's count; a failure keeps the last one. */
    fun refreshCount() {
        if (SessionStore.session.value == null || _feed.value.loading) return
        countJob?.cancel()
        countJob = scope.launch {
            val result = apiCall { api.unreadCount() }
            if (result is ApiResult.Ok) _unread.value = result.value.data?.count ?: 0
        }
    }

    /** Loads the first page for the sheet; the server then counts them all as read. */
    fun open() {
        countJob?.cancel()
        feedJob?.cancel()
        _feed.value = NotificationFeed(loading = true)
        feedJob = scope.launch {
            _feed.value = when (val result = apiCall { api.list(1, PAGE_SIZE) }) {
                is ApiResult.Ok -> {
                    _unread.value = 0
                    val items = result.value.data.orEmpty()
                    NotificationFeed(items = items, total = result.value.count ?: items.size, loaded = true)
                }
                is ApiResult.Fail -> NotificationFeed(error = result.message)
            }
        }
    }

    fun loadMore() {
        val current = _feed.value
        if (!current.hasMore || current.loadingMore) return
        _feed.value = current.copy(loadingMore = true, moreError = null)
        feedJob = scope.launch {
            val page = current.items.size / PAGE_SIZE + 1
            val result = apiCall { api.list(page, PAGE_SIZE) }
            _feed.update {
                when (result) {
                    // New notifications shift the pages, so one can come twice
                    is ApiResult.Ok -> it.copy(
                        items = (it.items + result.value.data.orEmpty()).distinctBy(NotificationDto::id),
                        total = result.value.count ?: it.total,
                        loadingMore = false
                    )
                    is ApiResult.Fail -> it.copy(loadingMore = false, moreError = result.message)
                }
            }
        }
    }

    /** Forgets everything, for sign-out. */
    fun clear() {
        countJob?.cancel()
        feedJob?.cancel()
        _unread.value = 0
        _feed.value = NotificationFeed()
    }
}
