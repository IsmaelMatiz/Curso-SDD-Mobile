package com.aristidevs.cursopremiumandroid.presentation.list

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.aristidevs.cursopremiumandroid.R
import com.aristidevs.cursopremiumandroid.domain.model.Dog
import com.aristidevs.cursopremiumandroid.ui.theme.BackgroundApp
import com.aristidevs.cursopremiumandroid.ui.theme.BackgroundComponent
import com.aristidevs.cursopremiumandroid.ui.theme.ControlColor
import com.aristidevs.cursopremiumandroid.ui.theme.PrimaryButton
import com.aristidevs.cursopremiumandroid.ui.theme.SecondaryText


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DogsScreen(
    onDogClicked: (Long) -> Unit,
    onAddDogClicked: () -> Unit,
    viewModel: DogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DogContent(
        uiState = uiState,
        onQueryChange = { viewModel.onQueryChange(it) },
        onDogClicked = onDogClicked,
        onAddDogClicked = onAddDogClicked,
        onRetryRefresh = { viewModel.retryRefresh() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DogContent(
    uiState: DogsUiState,
    onQueryChange: (String) -> Unit,
    onDogClicked: (Long) -> Unit,
    onAddDogClicked: () -> Unit,
    onRetryRefresh: () -> Unit
) {
    Scaffold(
        containerColor = BackgroundApp,
        topBar = {
            TopAppBar(
                title = { Text("Busca tu chucho") }, colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundApp,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            val addDogDescription = stringResource(R.string.dogs_add_fab_description)
            FloatingActionButton(
                onClick = onAddDogClicked,
                containerColor = PrimaryButton,
                modifier = Modifier.semantics { contentDescription = addDogDescription }
            ) {
                Text("+", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            DogSearchBar(uiState.query, onQueryChange)

            if (uiState.refreshError != null) {
                Spacer(Modifier.height(16.dp))
                RefreshErrorBanner(uiState.refreshError, onRetryRefresh)
            }

            Spacer(Modifier.height(32.dp))

            when {
                uiState.isInitialLoading -> {
                    LoadingDogState()
                }

                uiState.totalDogsCount == 0 -> {
                    EmptyNoDogsState(onAddDogClicked)
                }

                uiState.dogs.isEmpty() -> {
                    EmptyNoSearchResultsState()
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uiState.dogs, key = { dog -> dog.id }) { dog ->
                            DogItem(dog, onDogClicked)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DogSearchBar(query: String, onValueChanged: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onValueChanged,
        label = { Text(stringResource(R.string.dogs_search_field_label)) },
        placeholder = { Text("Buscar perro...") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SecondaryText,
            unfocusedTextColor = SecondaryText,
            cursorColor = ControlColor,
        )
    )
}

@Composable
fun DogItem(dog: Dog, onDogClicked: (Long) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDogClicked(dog.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundComponent)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {

            AsyncImage(
                model = dog.image,
                contentDescription = dog.name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dog.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (dog.isOwn) {
                        Spacer(Modifier.width(8.dp))
                        OwnDogLabel()
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(dog.breed, fontSize = 14.sp, color = PrimaryButton)
                Spacer(Modifier.height(4.dp))
                Text("${dog.age} años", fontSize = 14.sp, color = SecondaryText)
                Spacer(Modifier.height(8.dp))
                Text(dog.description, fontSize = 14.sp, color = Color.White)
            }

        }
    }
}

/** Text label, not just color, so a screen reader also announces it (RF-03, RF-13). */
@Composable
fun OwnDogLabel() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color = PrimaryButton)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            stringResource(R.string.dogs_own_label),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun LoadingDogState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ControlColor)
    }
}

@Composable
fun EmptyNoDogsState(onAddDogClicked: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.dogs_empty_no_dogs_title),
                color = SecondaryText,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAddDogClicked) {
                Text(stringResource(R.string.dogs_empty_no_dogs_action))
            }
        }
    }
}

@Composable
fun EmptyNoSearchResultsState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.dogs_empty_no_search_results),
            color = SecondaryText,
            fontSize = 16.sp
        )
    }
}

@Composable
fun RefreshErrorBanner(error: CatalogRefreshError, onRetry: () -> Unit) {
    val message = if (error == CatalogRefreshError.NO_CONNECTION) {
        stringResource(R.string.dogs_refresh_error_no_connection)
    } else {
        stringResource(R.string.dogs_refresh_error_unexpected)
    }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundComponent)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.dogs_refresh_retry))
            }
        }
    }
}
