package space.diomentia.ehzdiagnosticstracker.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.text.DecimalFormatSymbols
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.BuildConfig
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.ScanIdQrActivity
import space.diomentia.ehzdiagnosticstracker.askCameraPermissions
import space.diomentia.ehzdiagnosticstracker.checkCameraPermissions
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.FieldSpec
import space.diomentia.ehzdiagnosticstracker.data.FieldType
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalLocationProvider
import space.diomentia.ehzdiagnosticstracker.data.LocalSessionManager
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportField
import space.diomentia.ehzdiagnosticstracker.data.ReportFieldValue
import space.diomentia.ehzdiagnosticstracker.data.SiteWithFields
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.FieldSpecChooserDialog
import space.diomentia.ehzdiagnosticstracker.ui.FullScreenImageDialog
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledIconButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.TemplateChooserDialog
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.data.SessionManager
import space.diomentia.ehzdiagnosticstracker.data.Site
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

@Composable
fun SiteEditorScreen(
    siteId: String,
    initialEditMode: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    val sessionManager = LocalSessionManager.current

    val fullReport by sessionManager.fullReport.collectAsStateWithLifecycle(null)
    val siteWithFields = remember(fullReport, siteId) {
        fullReport?.sites?.find { it.site.id == siteId }
    }

    var editMode by remember { mutableStateOf(initialEditMode) }

    if (siteWithFields == null) return

    Scaffold(
        topBar = {
            PtmTopBar(title = {
                Text(stringResource(R.string.measurement_site))
            })
        },
        bottomBar = {
            PtmBottomBar(
                navigation = {
                    ActionButton(
                        icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.ArrowBack),
                        label = stringResource(R.string.back)
                    ) { onBack() }
                }
            )
        },
        snackbarHost = { PtmSnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        ScrollableColumn(
            innerPadding,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            if (editMode) {
                SiteEditContent(siteWithFields!!, onBack)
            } else {
                SitePreviewContent(siteWithFields!!) { editMode = true }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// --- Preview Mode ---

@Composable
private fun SitePreviewContent(
    siteWithFields: SiteWithFields,
    onEdit: () -> Unit
) {
    val site = siteWithFields.site

    // Position
    site.positionKm?.let { km ->
        Text(
            "${stringResource(R.string.position_km)}: ${"%.2f".format(km)}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    // Coordinates
    if (site.latitude != null && site.longitude != null) {
        Text(
            "${stringResource(R.string.latitude)}: ${"%.6f".format(site.latitude)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "${stringResource(R.string.longitude)}: ${"%.6f".format(site.longitude)}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }

    // Validation
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Text(
            "${stringResource(R.string.validation_status)}: ",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            stringResource(
                if (site.validationId != null) R.string.verified
                else R.string.not_verified
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (site.validationId != null) MaterialTheme.colorScheme.primary
            else Color.Gray
        )
    }

    // Fields
    if (siteWithFields.fields.isNotEmpty()) {
        siteWithFields.fields.forEach { field ->
            val displayValue = formatFieldValue(field)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    field.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    displayValue,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // Edit button
    PtmFilledButton(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Outlined.Edit,
            contentDescription = null,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(stringResource(R.string.edit))
    }

    // TODO: implement export functionality
    PtmFilledButton(
        onClick = { /* TODO */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Icon(
            painterResource(R.drawable.ic_file_export),
            contentDescription = stringResource(R.string.export),
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(stringResource(R.string.export_report))
    }
}

// TODO: display images in preview mode

@Composable
private fun formatFieldValue(field: ReportField): String {
    if (field.value.isBlank()) return stringResource(R.string.not_set)
    val fieldType = FieldType.parse(field.type)
    return when (fieldType) {
        FieldType.Bool -> when (ReportFieldValue.Bool.fromString(field.value)) {
            true -> stringResource(R.string.is_true)
            false -> stringResource(R.string.is_false)
            else -> stringResource(R.string.unknown)
        }
        FieldType.Photo -> stringResource(R.string.image)
        else -> field.value + (field.unit?.let { " $it" } ?: "")
    }
}

// --- Edit Mode ---

@Composable
private fun SiteEditContent(
    siteWithFields: SiteWithFields,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    val sessionManager = LocalSessionManager.current
    val site = siteWithFields.site

    // Position (editable)
    var positionKmText by remember(site.positionKm) {
        mutableStateOf(site.positionKm?.let { "%.2f".format(it) } ?: "")
    }
    Text(
        stringResource(R.string.position_km),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    TextField(
        value = positionKmText,
        onValueChange = { input ->
            positionKmText = input.filter { it.isDigit() || it == '.' || it == ',' }
            val parsed = positionKmText.replace(',', '.').toDoubleOrNull()
            if (parsed != null && parsed >= 0.0) {
                sessionManager.updateSitePosition(site.id, parsed)
            }
        },
        placeholder = {
            Text(
                stringResource(R.string.input_value),
                fontStyle = FontStyle.Italic,
                modifier = Modifier.alpha(.5f)
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )

    // GPS
    GpsSection(site.id)

    Spacer(Modifier.height(16.dp))

    // QR Scan
    QrScanSection(site.id)

    Spacer(Modifier.height(16.dp))

    // TODO: better add fields/templates and manage fields buttons — turn some to FABs
    // Insert template / Add field buttons
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showFieldSpecDialog by remember { mutableStateOf(false) }

    val config = AppState.reportConfig.value
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        PtmFilledButton(
            onClick = { showTemplateDialog = true },
            enabled = config != null && config.templates.isNotEmpty(),
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.insert_template))
        }
        PtmFilledButton(
            onClick = { showFieldSpecDialog = true },
            enabled = config != null && config.fieldSpecs.isNotEmpty(),
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.add_field))
        }
    }

    if (showTemplateDialog && config != null) {
        TemplateChooserDialog(
            templates = config.templates,
            onTemplateChosen = { template ->
                sessionManager.applyTemplateToSite(site.id, template)
            },
            onCancel = { showTemplateDialog = false }
        )
    }
    if (showFieldSpecDialog && config != null) {
        FieldSpecChooserDialog(
            allSpecs = config.fieldSpecs,
            addedSpecIds = siteWithFields.fields.map { it.specId }.toSet(),
            onSpecsChosen = { specs ->
                specs.forEach { sessionManager.addFieldToSite(site.id, it) }
            },
            onCancel = { showFieldSpecDialog = false }
        )
    }

    Spacer(Modifier.height(16.dp))

    // Fields editor
    var manageFields by remember { mutableStateOf(false) }
    if (siteWithFields.fields.isNotEmpty()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { manageFields = !manageFields }
                .padding(vertical = 8.dp)
        ) {
            Text(
                stringResource(R.string.manage_fields),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (manageFields) Icons.Outlined.Close else Icons.Outlined.Edit,
                contentDescription = null
            )
        }
    }

    siteWithFields.fields.forEach { field ->
        val fieldType = FieldType.parse(field.type)
        if (fieldType != null) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    EditableField(
                        field = field,
                        fieldType = fieldType,
                        siteId = site.id,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
                AnimatedVisibility(manageFields) {
                    PtmFilledIconButton(
                        onClick = {
                            sessionManager.removeFieldFromSite(site.id, field.specId)
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.delete)
                        )
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Save button
    PtmFilledButton(
        onClick = {
            sessionManager.confirmSite(site.id)
            onBack()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            painterResource(R.drawable.ic_save),
            contentDescription = stringResource(R.string.save),
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(stringResource(R.string.save))
    }

    // TODO: implement export functionality
    PtmFilledButton(
        onClick = { /* TODO */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Icon(
            painterResource(R.drawable.ic_file_export),
            contentDescription = stringResource(R.string.export),
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(stringResource(R.string.export_report))
    }
}

@Composable
private fun GpsSection(siteId: String) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    val sessionManager = LocalSessionManager.current
    val locationProvider = LocalLocationProvider.current
        ?: LocationServices.getFusedLocationProviderClient(context)

    var isSyncingLocation by remember { mutableStateOf(false) }
    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        isSyncingLocation = true
    }

    LaunchedEffect(isSyncingLocation) {
        if (!isSyncingLocation) return@LaunchedEffect
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
            isSyncingLocation = false
            return@LaunchedEffect
        }
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setMaxUpdateAgeMillis(15000L)
            .build()
        locationProvider.getCurrentLocation(request, null)
            .addOnSuccessListener {
                sessionManager.updateSiteCoordinates(siteId, it.latitude, it.longitude)
            }
            .addOnFailureListener {
                coroutineScope?.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        "${context.getString(R.string.location)}: ${
                            context.getString(R.string.something_wrong)
                        }..."
                    )
                }
            }
            .addOnCompleteListener { isSyncingLocation = false }
    }

    Text(
        stringResource(R.string.coordinates),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        PtmFilledButton(
            onClick = { isSyncingLocation = true },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(stringResource(R.string.refresh_location))
        }
    }
}

@Composable
private fun QrScanSection(siteId: String) {
    val context = LocalContext.current
    val sessionManager = LocalSessionManager.current

    val qrLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data?.getStringExtra(ScanIdQrActivity.QR_DATA)
        sessionManager.setSiteValidation(siteId, data)
    }

    val fullReport by sessionManager.fullReport.collectAsStateWithLifecycle(null)
    val site = fullReport?.sites?.find { it.site.id == siteId }?.site
    val validated = site?.validationId?.isNotBlank() == true

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        PtmFilledButton(
            onClick = {
                qrLauncher.launch(Intent(context, ScanIdQrActivity::class.java))
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painterResource(R.drawable.ic_qr_code),
                contentDescription = stringResource(R.string.scan_id),
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(stringResource(R.string.scan_id))
        }
        Surface(
            color = if (validated) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 16.dp)
        ) {
            Icon(
                if (validated) Icons.Outlined.Check else Icons.Outlined.Close,
                tint = MaterialTheme.colorScheme.background,
                contentDescription = null,
                modifier = Modifier
                    .aspectRatio(1f)
                    .padding(8.dp)
            )
        }
    }
}

// --- Field Editors ---

@Composable
private fun EditableField(
    field: ReportField,
    fieldType: FieldType,
    siteId: String,
    modifier: Modifier = Modifier
) {
    val sessionManager = LocalSessionManager.current

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                field.name,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
            )
            if (fieldType == FieldType.Bool) {
                BoolToggle(field.value) { newValue ->
                    sessionManager.updateFieldValue(siteId, field.specId, newValue)
                }
            }
        }

        when (fieldType) {
            FieldType.Text, FieldType.Raw -> {
                FieldTextField(
                    value = field.value,
                    unit = field.unit,
                    onValueChange = {
                        sessionManager.updateFieldValue(siteId, field.specId, it)
                    }
                )
            }
            FieldType.Integer -> {
                FieldTextField(
                    value = field.value,
                    unit = field.unit,
                    keyboardType = KeyboardType.Number,
                    onValueChange = { input ->
                        sessionManager.updateFieldValue(
                            siteId, field.specId,
                            input.filter { it.isDigit() || it == '-' }
                        )
                    }
                )
            }
            FieldType.Decimal -> {
                FieldTextField(
                    value = field.value,
                    unit = field.unit,
                    keyboardType = KeyboardType.Decimal,
                    onValueChange = { input ->
                        sessionManager.updateFieldValue(
                            siteId, field.specId,
                            input.filter {
                                it.isDigit()
                                        || it == '.' || it == ','
                                        || it == '-'
                            }
                        )
                    }
                )
            }
            FieldType.Photo -> {
                PhotoField(siteId, field)
            }
            FieldType.Bool -> Unit
        }
    }
}

@Composable
private fun BoolToggle(
    value: String,
    onToggle: (String) -> Unit
) {
    FilledTonalIconButton(
        onClick = {
            onToggle(
                when (ReportFieldValue.Bool.fromString(value)) {
                    true -> "false"
                    false -> "true"
                    else -> "true"
                }
            )
        },
        shape = RectangleShape,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .border(BorderStroke(6.dp, MaterialTheme.colorScheme.primary))
    ) {
        Icon(
            if (ReportFieldValue.Bool.fromString(value) == true) Icons.Outlined.Check
            else Icons.Outlined.Close,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(.8f)
                .aspectRatio(1f)
                .alpha(if (value != "") 1f else 0f)
        )
    }
}

@Composable
private fun FieldTextField(
    value: String,
    unit: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp)
    ) {
        TextField(
            value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    stringResource(R.string.input_value),
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    modifier = Modifier.alpha(.5f)
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.weight(1f)
        )
        if (unit != null) {
            Text(
                unit,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun PhotoField(
    siteId: String,
    field: ReportField
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current
    val sessionManager = LocalSessionManager.current

    var cameraPermission: Boolean? by remember { mutableStateOf(context.checkCameraPermissions()) }
    if (cameraPermission == false)
        cameraPermission = askCameraPermissions {
            cameraPermission = null
            coroutineScope?.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(context.getString(R.string.unable_access_camera))
            }
        }

    val pic: Bitmap? = remember(field.value) {
        if (field.value.isBlank()) null
        else Base64.decode(field.value, Base64.DEFAULT).let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    }
    val picStorage = remember {
        File(context.cacheDir, "images").also { if (!it.exists()) it.mkdirs() }
    }
    val picUri = remember {
        FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID,
            File.createTempFile("FIELD_${field.specId}", ".png", picStorage)
        )
    }
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) return@rememberLauncherForActivityResult
        try {
            @Suppress("DEPRECATION")
            val bitmap = MediaStore.Images.Media
                .getBitmap(context.contentResolver, picUri)
            ByteArrayOutputStream().use {
                bitmap.compress(Bitmap.CompressFormat.WEBP, 80, it)
                sessionManager.updateFieldValue(
                    siteId, field.specId,
                    Base64.encodeToString(it.toByteArray(), Base64.DEFAULT)
                )
            }
        } catch (e: Exception) {
            when (e) {
                is FileNotFoundException, is IOException -> coroutineScope?.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(context.getString(R.string.something_wrong))
                }
            }
        }
    }

    var fullView by remember { mutableStateOf(false) }
    if (fullView) pic?.asImageBitmap()?.let { FullScreenImageDialog(it) { fullView = false } }

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            PtmFilledButton(
                onClick = if (cameraPermission == true)
                    { -> photoLauncher.launch(picUri) }
                else
                    { -> cameraPermission = context.checkCameraPermissions() },
                modifier = Modifier.weight(1f)
            ) {
                val label = stringResource(
                    if (pic == null) R.string.make_photo else R.string.change_photo
                )
                Icon(
                    painterResource(R.drawable.ic_photo_camera),
                    contentDescription = label,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(label)
            }
            Surface(
                color = if (field.value.isNotBlank()) MaterialTheme.colorScheme.primary
                else Color.Gray,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 16.dp)
            ) {
                Icon(
                    if (field.value.isNotBlank()) Icons.Outlined.Check
                    else Icons.Outlined.Close,
                    tint = MaterialTheme.colorScheme.background,
                    contentDescription = null,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(8.dp)
                )
            }
        }
        pic?.let {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(8.dp)
            ) {
                @Suppress("DEPRECATION")
                Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "")
                Image(
                    it.asImageBitmap(),
                    contentDescription = "",
                    modifier = Modifier
                        .height(150.dp)
                        .clickable { fullView = true }
                )
            }
        }
    }
}

// --- Previews ---

private val previewSite = Site(
    id = "preview",
    reportId = "r1",
    positionKm = 12.345,
    latitude = 55.751244,
    longitude = 37.618423,
    timestamp = 0,
    validationId = "QR-123",
    locationOnly = false
)

private val previewFields = listOf(
    ReportField("preview", "voltage", "Напряжение", "decimal", "В", "1.25"),
    ReportField("preview", "current", "Ток", "decimal", "А", ""),
    ReportField("preview", "corrosion", "Наличие коррозии", "bool", null, "true"),
    ReportField("preview", "note", "Примечание", "text", null, "Без замечаний")
)

private val previewSiteWithFields = SiteWithFields(previewSite, previewFields)

@Preview(showBackground = true)
@Composable
private fun SitePreviewModePreview() = PtmTheme {
    Column(Modifier.padding(16.dp)) {
        SitePreviewContent(previewSiteWithFields) {}
    }
}

@Preview(showBackground = true)
@Composable
private fun SiteEditModePreview() {
    val context = LocalContext.current
    val sessionManager = SessionManager(
        ReportDatabase.getInstance(context).reportDao(),
        null
    )
    CompositionLocalProvider(LocalSessionManager provides sessionManager) {
        PtmTheme {
            Column(Modifier.padding(16.dp)) {
                SiteEditContent(previewSiteWithFields, onBack = {})
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SitePreviewUnverifiedPreview() = PtmTheme {
    val unverifiedSite = previewSite.copy(validationId = null)
    val siteWithFields = SiteWithFields(unverifiedSite, previewFields.take(2))
    Column(Modifier.padding(16.dp)) {
        SitePreviewContent(siteWithFields) {}
    }
}