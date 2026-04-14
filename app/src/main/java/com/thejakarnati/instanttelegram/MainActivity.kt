package com.thejakarnati.instanttelegram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import coil.compose.AsyncImage
import com.thejakarnati.instanttelegram.data.local.AppDatabase
import com.thejakarnati.instanttelegram.data.remote.BridgeApi
import com.thejakarnati.instanttelegram.data.remote.InstagramApi
import com.thejakarnati.instanttelegram.data.repository.CreatorRepository
import com.thejakarnati.instanttelegram.domain.CreatorProfile
import com.thejakarnati.instanttelegram.domain.FeedItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "instanttelegram.db").build()
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.instagram.com/")
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        val bridgeApi = BuildConfig.BRIDGE_BASE_URL.takeIf { it.isNotBlank() }?.let { baseUrl ->
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(BridgeApi::class.java)
        }
        val repository = CreatorRepository(
            bridgeApi = bridgeApi,
            api = retrofit.create(InstagramApi::class.java),
            favoritesDao = db.favoriteProfileDao(),
            httpClient = httpClient
        )

        val vmFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository) as T
            }
        }

        setContent {
            val viewModel: MainViewModel = ViewModelProvider(this, vmFactory)[MainViewModel::class.java]
            MaterialTheme {
                App(viewModel)
            }
        }
    }
}

data class UiState(
    val query: String = "",
    val loading: Boolean = false,
    val message: String = "Search an Instagram username to preview public posts.",
    val selectedProfile: CreatorProfile? = null,
    val previewItems: List<FeedItem> = emptyList(),
    val favoriteProfiles: List<CreatorProfile> = emptyList(),
    val favoriteFeed: List<FeedItem> = emptyList()
)

class MainViewModel(
    private val repository: CreatorRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeFavoriteProfiles().collect { favorites ->
                _uiState.value = _uiState.value.copy(favoriteProfiles = favorites)
                refreshFavoriteFeed(favorites)
            }
        }
    }

    fun updateQuery(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
    }

    fun search() {
        val username = _uiState.value.query.trim()
        if (username.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, message = "Fetching public profile…")
            val result = repository.searchProfile(username)
            result.onSuccess { (profile, feed) ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    selectedProfile = profile,
                    previewItems = feed,
                    message = "Loaded ${feed.size} recent public posts."
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    message = "Could not load profile preview. For reliable previews, configure BRIDGE_BASE_URL to your own backend bridge."
                )
            }
        }
    }

    fun addFavorite() {
        val profile = _uiState.value.selectedProfile
        if (profile == null) {
            val username = _uiState.value.query.trim()
            if (username.isBlank()) {
                _uiState.value = _uiState.value.copy(message = "Type a username first.")
                return
            }
            viewModelScope.launch {
                repository.addFavoriteByUsername(username)
                _uiState.value = _uiState.value.copy(
                    message = "Added @$username to favorites without preview. Search again later to refresh public posts."
                )
            }
            return
        }
        viewModelScope.launch {
            repository.addFavorite(profile)
            _uiState.value = _uiState.value.copy(message = "Added @${profile.username} to favorites.")
        }
    }

    fun removeFavorite(username: String) {
        viewModelScope.launch {
            repository.removeFavorite(username)
            _uiState.value = _uiState.value.copy(message = "Removed @$username from favorites.")
        }
    }

    private fun refreshFavoriteFeed(favorites: List<CreatorProfile>) {
        viewModelScope.launch {
            val collected = mutableListOf<FeedItem>()
            favorites.forEach { profile ->
                repository.searchProfile(profile.username).getOrNull()?.second?.let { items ->
                    collected += items.take(6)
                }
            }
            _uiState.value = _uiState.value.copy(
                favoriteFeed = collected.take(50),
                message = if (favorites.isEmpty()) "Add favorites to build your low-distraction feed." else _uiState.value.message
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("InstantTelegram", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "No login required. Favorites are stored only on this device.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::updateQuery,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Instagram username") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = viewModel::search) { Text("Preview") }
                    Button(onClick = viewModel::addFavorite) { Text("Add favorite") }
                }
                if (state.loading) {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator()
                }
                Spacer(Modifier.height(8.dp))
                Text(state.message)
            }

            item {
                Text("Favorites", style = MaterialTheme.typography.titleLarge)
            }
            items(state.favoriteProfiles, key = { it.username }) { profile ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("@${profile.username}")
                        TextButton(onClick = { viewModel.removeFavorite(profile.username) }) {
                            Text("Remove")
                        }
                    }
                }
            }

            item {
                Text("Favorite feed", style = MaterialTheme.typography.titleLarge)
            }
            items(state.favoriteFeed, key = { it.id }) { item ->
                FeedCard(item)
            }

            item {
                if (state.previewItems.isNotEmpty()) {
                    Text("Search preview", style = MaterialTheme.typography.titleLarge)
                }
            }
            items(state.previewItems, key = { it.id + "preview" }) { item ->
                FeedCard(item)
            }
        }
    }
}

@Composable
private fun FeedCard(item: FeedItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("@${item.username}", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            AsyncImage(
                model = item.thumbnailUrl.ifBlank { item.mediaUrl },
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Text(if (item.isVideo) "Video" else "Image", style = MaterialTheme.typography.labelMedium)
            if (item.caption.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(item.caption, maxLines = 3)
            }
            Spacer(Modifier.height(4.dp))
            Text(item.permalink, style = MaterialTheme.typography.bodySmall)
        }
    }
}
