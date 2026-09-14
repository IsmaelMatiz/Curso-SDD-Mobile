package com.aristidevs.cursopremiumandroid.presentation.create

import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.aristidevs.cursopremiumandroid.R
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationError
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationField
import com.aristidevs.cursopremiumandroid.domain.model.DogValidationReason
import com.aristidevs.cursopremiumandroid.ui.theme.BackgroundApp
import com.aristidevs.cursopremiumandroid.ui.theme.ControlColor
import com.aristidevs.cursopremiumandroid.ui.theme.SecondaryText

@Composable
fun AddDogScreen(onDogSaved: () -> Unit, viewModel: AddDogViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) onDogSaved()
    }

    BackHandler(onBack = viewModel::onBackRequested)

    AddDogContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onBreedChange = viewModel::onBreedChange,
        onAgeChange = viewModel::onAgeChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onWeightChange = viewModel::onWeightChange,
        onOriginChange = viewModel::onOriginChange,
        onTemperamentChange = viewModel::onTemperamentChange,
        onPhotoPicked = viewModel::onPhotoPicked,
        onBackSelected = viewModel::onBackRequested,
        onSaveClicked = viewModel::onSaveClicked,
        onDiscardConfirmed = viewModel::onDiscardConfirmed,
        onDiscardDismissed = viewModel::onDiscardDismissed
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDogContent(
    uiState: AddDogUiState,
    onNameChange: (String) -> Unit,
    onBreedChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onOriginChange: (String) -> Unit,
    onTemperamentChange: (String) -> Unit,
    onPhotoPicked: (String?) -> Unit,
    onBackSelected: () -> Unit,
    onSaveClicked: () -> Unit,
    onDiscardConfirmed: () -> Unit,
    onDiscardDismissed: () -> Unit
) {
    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onPhotoPicked(it.toString()) } }

    if (uiState.showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = onDiscardDismissed,
            title = { Text(stringResource(R.string.add_dog_discard_title)) },
            text = { Text(stringResource(R.string.add_dog_discard_message)) },
            confirmButton = {
                TextButton(onClick = onDiscardConfirmed) {
                    Text(stringResource(R.string.add_dog_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDiscardDismissed) {
                    Text(stringResource(R.string.add_dog_discard_cancel))
                }
            }
        )
    }

    Scaffold(
        containerColor = BackgroundApp,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_dog_title)) },
                navigationIcon = {
                    IconButton(onBackSelected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow),
                            contentDescription = stringResource(R.string.detail_back_description)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundApp,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AddDogTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.add_dog_name_label),
                errorText = fieldError(DogValidationField.NAME, uiState.errors)
            )
            AddDogTextField(
                value = uiState.breed,
                onValueChange = onBreedChange,
                label = stringResource(R.string.add_dog_breed_label),
                errorText = fieldError(DogValidationField.BREED, uiState.errors)
            )
            AddDogTextField(
                value = uiState.ageText,
                onValueChange = onAgeChange,
                label = stringResource(R.string.add_dog_age_label),
                errorText = fieldError(DogValidationField.AGE, uiState.errors),
                keyboardType = KeyboardType.Number
            )
            AddDogTextField(
                value = uiState.description,
                onValueChange = onDescriptionChange,
                label = stringResource(R.string.add_dog_description_label),
                errorText = fieldError(DogValidationField.DESCRIPTION, uiState.errors),
                singleLine = false
            )
            AddDogTextField(
                value = uiState.weight,
                onValueChange = onWeightChange,
                label = stringResource(R.string.add_dog_weight_label),
                errorText = fieldError(DogValidationField.WEIGHT, uiState.errors)
            )
            AddDogTextField(
                value = uiState.origin,
                onValueChange = onOriginChange,
                label = stringResource(R.string.add_dog_origin_label),
                errorText = fieldError(DogValidationField.ORIGIN, uiState.errors)
            )
            AddDogTextField(
                value = uiState.temperament,
                onValueChange = onTemperamentChange,
                label = stringResource(R.string.add_dog_temperament_label),
                errorText = fieldError(DogValidationField.TEMPERAMENT, uiState.errors)
            )

            PhotoPickerField(
                photoUri = uiState.photoUri,
                errorText = fieldError(DogValidationField.PHOTO, uiState.errors),
                onPickPhoto = {
                    photoPicker.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            )

            Button(
                onClick = onSaveClicked,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Color.White)
                } else {
                    Text(stringResource(R.string.add_dog_confirm))
                }
            }
        }
    }
}

@Composable
private fun AddDogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorText: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = errorText != null,
        supportingText = errorText?.let { { Text(it) } },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (errorText != null) error(errorText) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = SecondaryText,
            unfocusedTextColor = SecondaryText,
            cursorColor = ControlColor
        )
    )
}

@Composable
private fun PhotoPickerField(photoUri: String?, errorText: String?, onPickPhoto: () -> Unit) {
    Column(modifier = Modifier.semantics { if (errorText != null) error(errorText) }) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = stringResource(R.string.add_dog_photo_selected_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }
        OutlinedButton(onClick = onPickPhoto, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.add_dog_photo_cta))
        }
        if (errorText != null) {
            Text(errorText, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun fieldError(field: DogValidationField, errors: Set<DogValidationError>): String? {
    val error = errors.firstOrNull { it.field == field } ?: return null
    return when {
        field == DogValidationField.AGE && error.reason == DogValidationReason.OUT_OF_RANGE ->
            stringResource(R.string.add_dog_error_age_range)

        field == DogValidationField.PHOTO ->
            stringResource(R.string.add_dog_error_photo_required)

        error.reason == DogValidationReason.TOO_LONG ->
            stringResource(R.string.add_dog_error_too_long)

        else -> stringResource(R.string.add_dog_error_required)
    }
}
