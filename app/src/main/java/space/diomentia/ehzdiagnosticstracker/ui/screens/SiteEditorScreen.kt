package space.diomentia.ehzdiagnosticstracker.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import space.diomentia.ehzdiagnosticstracker.BuildConfig
import space.diomentia.ehzdiagnosticstracker.R
import space.diomentia.ehzdiagnosticstracker.ScanIdQrActivity
import space.diomentia.ehzdiagnosticstracker.askCameraPermissions
import space.diomentia.ehzdiagnosticstracker.checkCameraPermissions
import space.diomentia.ehzdiagnosticstracker.data.AppState
import space.diomentia.ehzdiagnosticstracker.data.Coordinates
import space.diomentia.ehzdiagnosticstracker.data.FieldSpec
import space.diomentia.ehzdiagnosticstracker.data.FieldType
import space.diomentia.ehzdiagnosticstracker.data.LocalCoroutineScope
import space.diomentia.ehzdiagnosticstracker.data.LocalSessionManager
import space.diomentia.ehzdiagnosticstracker.data.LocalSnackbarHostState
import space.diomentia.ehzdiagnosticstracker.data.ReportDatabase
import space.diomentia.ehzdiagnosticstracker.data.ReportField
import space.diomentia.ehzdiagnosticstracker.data.ReportFieldValue
import space.diomentia.ehzdiagnosticstracker.data.ReportTemplate
import space.diomentia.ehzdiagnosticstracker.data.SessionManager
import space.diomentia.ehzdiagnosticstracker.data.Site
import space.diomentia.ehzdiagnosticstracker.data.SiteWithFields
import space.diomentia.ehzdiagnosticstracker.data.distanceKmTo
import space.diomentia.ehzdiagnosticstracker.ui.ActionButton
import space.diomentia.ehzdiagnosticstracker.ui.ActionCancelDialog
import space.diomentia.ehzdiagnosticstracker.ui.DeleteDialog
import space.diomentia.ehzdiagnosticstracker.ui.FieldSpecChooserDialog
import space.diomentia.ehzdiagnosticstracker.ui.ImageWithFullscreenView
import space.diomentia.ehzdiagnosticstracker.ui.LocationField
import space.diomentia.ehzdiagnosticstracker.ui.LocationIconButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmBottomBar
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmFilledIconButton
import space.diomentia.ehzdiagnosticstracker.ui.PtmSnackbarHost
import space.diomentia.ehzdiagnosticstracker.ui.PtmTopBar
import space.diomentia.ehzdiagnosticstracker.ui.ScrollableColumn
import space.diomentia.ehzdiagnosticstracker.ui.TemplateChooserDialog
import space.diomentia.ehzdiagnosticstracker.ui.rememberSnacker
import space.diomentia.ehzdiagnosticstracker.ui.theme.PtmTheme
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.DecimalFormat

@Composable
fun SiteEditorScreen(
    siteId: String,
    initialEditMode: Boolean = false,
    onBack: () -> Unit
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val reportDao = ReportDatabase.getInstance().reportDao()
    rememberSnacker()

    // TODO: maybe just set draft mark when a site is edited?
    var editMode by remember { mutableStateOf(initialEditMode) }
    val currentSite = remember(siteId, editMode) { reportDao.getSiteByIdOnce(siteId) }
    if (currentSite == null) {
        onBack()
        return
    }

    val reportSites = remember(currentSite) {
        currentSite.reportId.let { reportDao.getSitesForReportOnce(it) }
            .filter { it.id != siteId }
    }
    val previousSite = reportSites.lastOrNull()

    val siteWithFields by reportDao.getSiteWithFields(siteId).collectAsStateWithLifecycle(null)

    val editState = rememberSiteEditState(currentSite)
    var manageFields by remember { mutableStateOf(true) }
    val editLocation = rememberSaveable(siteId) { editState.isDraft || editState.location == null }

    var showExitDialog by remember { mutableStateOf(false) }
    if (showExitDialog) {
        ActionCancelDialog(
            label = {
                Text(
                    stringResource(
                        if (currentSite.isDraft)
                            R.string.site_draft_not_saved
                        else R.string.site_edits_not_saved
                    )
                )
            },
            actionLabel = { Text(stringResource(R.string.save)) },
            onAction = {
                onBack()
                editState.saveAndConfirm()
            },
            actionEnabled = editState.canSave,
            secondaryActionLabel = {
                Text(
                    stringResource(
                        if (currentSite.isDraft)
                            R.string.delete
                        else R.string.rollback
                    )
                )
            },
            onSecondaryAction = {
                onBack()
                if (currentSite.isDraft) {
                    reportDao.deleteSiteById(siteId)
                }
            },
            onCancel = { showExitDialog = false }
        ) {
            Column {
                Text(
                    stringResource(
                        if (currentSite.isDraft)
                            R.string.site_draft_not_saved_dialog
                        else R.string.site_edits_not_saved_dialog
                    )
                )

                if (!editState.canSave) {
                    Text(
                        stringResource(
                            if (currentSite.isDraft)
                                R.string.site_draft_cannot_be_saved
                            else R.string.site_edits_cannot_be_saved
                        ),
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }

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
                    ) { if (editMode) showExitDialog = true else onBack() }
                },
                actions = if (editMode) { ->
                    ActionButton(
                        icon = if (manageFields)
                            painterResource(R.drawable.ic_edit_square)
                        else painterResource(R.drawable.ic_contract_edit),
                        label = stringResource(R.string.manage_report_fields)
                    ) { manageFields = !manageFields }
                } else null
            )
        },
        snackbarHost = { PtmSnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        ScrollableColumn(
            innerPadding,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            if (editMode) {
                SiteEditContent(
                    editState = editState,
                    previousSite = previousSite,
                    manageFields = manageFields,
                    editLocation = editLocation,
                    onToggleManageFields = { manageFields = it }
                ) { editMode = false }
            } else siteWithFields?.let { swf ->
                SitePreviewContent(swf) { editMode = true }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// --- Preview Mode ---

@Composable
private fun SitePreviewContent(
    siteWithFields: SiteWithFields,
    onEdit: () -> Unit
) {
    val site = siteWithFields.info

    // Position
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column {
            Text(
                "${stringResource(R.string.position)}: ${
                    site.positionKm?.let { km -> "%.2f".format(km) } ?: "??"
                } ${
                    stringResource(
                        R.string.km
                    )
                }",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Validation
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
                color = if (site.validationId != null)
                    MaterialTheme.colorScheme.primary
                else Color.Gray,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        LocationIconButton(
            site.location,
            modifier = Modifier.size(52.dp)
        )
    }

    Spacer(Modifier.height(24.dp))

    // Fields
    if (siteWithFields.fields.isNotEmpty()) {
        siteWithFields.fields.forEach { field ->
            val fieldType = if (field.value.isBlank())
                FieldType.Raw
            else FieldType.parse(field.type) ?: FieldType.Raw
            val value = field.value
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    field.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .padding(end = 6.dp)
                )
                when (fieldType) {
                    FieldType.Text, FieldType.Bool, FieldType.Integer, FieldType.Decimal, FieldType.Raw, FieldType.Enum -> {
                        var displayValue = displayValue(field)
                        Text(
                            displayValue,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .widthIn(max = 200.dp)
                        )
                    }

                    FieldType.Photo -> {
                        val pic: Bitmap = remember(value) {
                            Base64.decode(value, Base64.DEFAULT).let {
                                BitmapFactory.decodeByteArray(it, 0, it.size)
                            }
                        }
                        ImageWithFullscreenView(
                            pic.asImageBitmap(),
                            modifier = Modifier.fillMaxWidth(.3f)
                        )
                    }
                }
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
}

@Composable
private fun displayValue(field: ReportField, fieldValue: String = field.value): String =
    if (fieldValue.isBlank())
        "\u2014"
    else when (FieldType.parse(field.type)) {
        null, FieldType.Raw, FieldType.Text, FieldType.Enum -> fieldValue
        FieldType.Integer -> fieldValue
        FieldType.Decimal -> DecimalFormat("#.##").format(fieldValue.toDouble())
        FieldType.Bool -> when (ReportFieldValue.Bool.fromString(fieldValue)) {
            true -> stringResource(R.string.is_true)
            false -> stringResource(R.string.is_false)
            else -> stringResource(R.string.unknown)
        }

        FieldType.Photo -> stringResource(R.string.image)
    } + (field.unit?.let { " $it" } ?: "")

// --- Edit Mode ---
@Composable
private fun SiteEditContent(
    editState: SiteEditState,
    previousSite: Site?,
    manageFields: Boolean,
    editLocation: Boolean,
    onToggleManageFields: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    // Position (editable)
    Text(
        stringResource(R.string.position),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    FieldTextField(
        value = editState.positionKmText,
        unit = stringResource(R.string.km),
        keyboardType = KeyboardType.Decimal
    ) { editState.positionKmText = it }

    Spacer(Modifier.height(16.dp))

    // GPS
    Text(
        stringResource(R.string.coordinates),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    LocationField(
        location = editState.location,
        modifiable = editLocation
    ) {
        val loc = Coordinates(lat = it.latitude, lng = it.longitude)
        editState.location = loc
        if (editState.positionKmText.isBlank()) {
            editState.positionKmText = (
                    previousSite?.run {
                        if (location != null && positionKm != null) {
                            positionKm + location.distanceKmTo(loc)
                        } else null
                    }
                    )?.let { DecimalFormat("#.##").format(it) } ?: ""
        }
    }

    Spacer(Modifier.height(16.dp))

    // QR Scan
    VerificationSection(editState)

    Spacer(Modifier.height(24.dp))

    // Insert template / Add field buttons
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showFieldSpecDialog by remember { mutableStateOf(false) }
    var deleteFieldRequest by remember { mutableStateOf<ReportField?>(null) }

    val config = AppState.reportConfig.value

    if (showTemplateDialog && config != null) {
        TemplateChooserDialog(
            templates = config.templates,
            onTemplateChosen = { template ->
                editState.applyTemplate(template)
                onToggleManageFields(false)
            },
            onCancel = { showTemplateDialog = false }
        )
    }
    if (showFieldSpecDialog && config != null) {
        FieldSpecChooserDialog(
            allSpecs = config.fieldSpecs,
            addedSpecIds = editState.fieldSpecs.map { it.specId }.toSet(),
            onSpecsChosen = { specs ->
                specs.forEach { editState.addField(it) }
                onToggleManageFields(false)
            },
            onCancel = { showFieldSpecDialog = false }
        )
    }
    deleteFieldRequest?.let { field ->
        DeleteDialog(
            onDelete = { editState.removeField(field.specId) },
            label = { Text(stringResource(R.string.field_value_will_be_deleted)) },
            name = field.name,
            details = editState.fieldValues[field.specId]!!,
            onCancel = { deleteFieldRequest = null }
        )
    }

    editState.fieldSpecs.forEach { field ->
        val fieldType = FieldType.parse(field.type)
        if (fieldType != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (!manageFields) {
                        EditableField(
                            field = field,
                            fieldType = fieldType,
                            value = editState.fieldValues[field.specId] ?: "",
                            onValueChange = { editState.fieldValues[field.specId] = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            field.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (editState.fieldValues[field.specId]!!.isNotBlank()) {
                            Text(
                                stringResource(R.string.value_will_be_deleted),
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .alpha(.6f)
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                }
                if (manageFields) {
                    PtmFilledIconButton(
                        onClick = {
                            if (editState.fieldValues[field.specId]!!.isNotBlank())
                                deleteFieldRequest = field
                            else editState.removeField(field.specId)
                        },
                        modifier = Modifier
                            .width(48.dp)
                            .aspectRatio(1f)
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
    if (!manageFields) {
        PtmFilledButton(
            onClick = {
                editState.saveAndConfirm()
                onSave()
            },
            enabled = editState.canSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painterResource(R.drawable.ic_save),
                contentDescription = stringResource(R.string.save),
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(stringResource(R.string.save))
        }
    } else {
        PtmFilledButton(
            onClick = { showFieldSpecDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painterResource(R.drawable.ic_variable_add),
                contentDescription = stringResource(R.string.add_field),
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(stringResource(R.string.add_field))
        }
        PtmFilledButton(
            onClick = { showTemplateDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painterResource(R.drawable.ic_list_alt),
                contentDescription = stringResource(R.string.insert_template),
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(stringResource(R.string.insert_template))
        }
    }
}

@Composable
private fun VerificationSection(editState: SiteEditState) {
    val context = LocalContext.current
    val snacker = rememberSnacker()

    val qrLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data?.getStringExtra(ScanIdQrActivity.QR_DATA)
        editState.validationId = data
    }

    val validated = editState.validationId?.isNotBlank() == true

    Text(
        stringResource(R.string.validation_status),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 4.dp)
    )

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
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxHeight()
        ) {
            Icon(
                painterResource(R.drawable.ic_qr_code),
                contentDescription = stringResource(R.string.scan_id),
                modifier = Modifier
                    .padding(4.dp)
                    .aspectRatio(1f)
            )
            Text(stringResource(R.string.QR))
        }
        PtmFilledButton(
            onClick = {
                // TODO: NFC functionality
            },
            enabled = false,
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxHeight()
                .padding(start = 8.dp)
        ) {
            Icon(
                painterResource(R.drawable.ic_nfc),
                contentDescription = stringResource(R.string.scan_id),
                modifier = Modifier
                    .padding(4.dp)
                    .aspectRatio(1f)
            )
            Text(stringResource(R.string.NFC))
        }
        Surface(
            color = if (validated) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(48.dp)
                .clickable {
                    snacker.showMessage(
                        context.getString(
                            if (validated)
                                R.string.report_verified
                            else R.string.report_not_verified
                        )
                    )
                }
        ) {
            Icon(
                if (validated) Icons.Outlined.Check else Icons.Outlined.Close,
                tint = MaterialTheme.colorScheme.background,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
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
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                BoolToggle(value, onValueChange)
            }
        }

        when (fieldType) {
            FieldType.Text, FieldType.Raw -> {
                FieldTextField(
                    value = value,
                    unit = field.unit,
                    onValueChange = onValueChange
                )
            }

            FieldType.Integer -> {
                FieldTextField(
                    value = value,
                    unit = field.unit,
                    keyboardType = KeyboardType.Number,
                    onValueChange = { input ->
                        onValueChange(input.filter { it.isDigit() || it == '-' })
                    }
                )
            }

            FieldType.Decimal -> {
                FieldTextField(
                    value = value,
                    unit = field.unit,
                    keyboardType = KeyboardType.Decimal,
                    onValueChange = { input ->
                        onValueChange(
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
                PhotoField(value, onValueChange)
            }

            FieldType.Bool -> Unit

            // TODO: dedicated picker UI; falls back to a free-text editor for now
            FieldType.Enum -> {
                FieldTextField(
                    value = value,
                    unit = field.unit,
                    onValueChange = onValueChange
                )
            }
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
                    false -> ""
                    else -> "true"
                }
            )
        },
        shape = RectangleShape,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
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
        OutlinedTextField(
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
    value: String,
    onValueChange: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = LocalCoroutineScope.current
    val snackbarHostState = LocalSnackbarHostState.current

    var cameraPermission: Boolean? by remember { mutableStateOf(context.checkCameraPermissions()) }
    if (cameraPermission == false)
        cameraPermission = askCameraPermissions {
            cameraPermission = null
            coroutineScope?.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(context.getString(R.string.unable_access_camera))
            }
        }

    val pic: Bitmap? = remember(value) {
        if (value.isBlank()) null
        else Base64.decode(value, Base64.DEFAULT).let {
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
            File.createTempFile("FIELD_PHOTO", ".png", picStorage)
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
                onValueChange(Base64.encodeToString(it.toByteArray(), Base64.DEFAULT))
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

    var deletePhotoDialog by remember(pic) { mutableStateOf(false) }
    if (deletePhotoDialog && pic != null) {
        ActionCancelDialog(
            onAction = { onValueChange("") },
            actionLabel = { Text(stringResource(R.string.delete)) },
            onCancel = { deletePhotoDialog = false },
            label = { Text(stringResource(R.string.delete_photo)) }
        ) {
            ImageWithFullscreenView(
                pic.asImageBitmap(),
                modifier = Modifier.height(150.dp)
            )
        }
    }

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
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxHeight()
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
                color = if (value.isNotBlank()) MaterialTheme.colorScheme.primary
                else Color.Gray,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = 8.dp)
            ) {
                Icon(
                    if (value.isNotBlank()) Icons.Outlined.Check
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
                ImageWithFullscreenView(
                    it.asImageBitmap(),
                    onLongClick = { deletePhotoDialog = true },
                    modifier = Modifier
                        .height(150.dp)
                )
            }
        }
    }
}

// --- State Holder ---

@OptIn(FlowPreview::class)
private class SiteEditState(
    site: Site,
    fields: List<ReportField>,
    private val sessionManager: SessionManager,
    scope: CoroutineScope
) {

    val isDraft = site.isDraft
    val siteId = site.id

    var location by mutableStateOf(site.location)
    var validationId by mutableStateOf(site.validationId)

    private var _positionKmText = mutableStateOf(site.positionKm?.let { "%.2f".format(it) } ?: "")
    var positionKmText: String
        get() = _positionKmText.value
        set(value) {
            _positionKmText.value = value.filter { it.isDigit() || it == '.' || it == ',' }
        }

    val positionKm: Double?
        get() = positionKmText.replace(',', '.').toDoubleOrNull()

    val fieldValues = mutableStateMapOf<String, String>().apply {
        fields.forEach { put(it.specId, it.value) }
    }

    var fieldSpecs by mutableStateOf(fields)
        private set

    val canSave: Boolean
        get() = positionKm?.let { km -> km != null && km >= 0.0 } ?: false

    init {
        if (site.isDraft) {
            scope.launch(Dispatchers.IO) {
                snapshotFlow { Triple(positionKm, location, validationId) }
                    .debounce(500)
                    .collectLatest { (km, coords, valId) ->
                        km?.let { sessionManager.updateSitePosition(siteId, it) }
                        if (coords != null) sessionManager.updateSiteLocation(siteId, coords)
                        sessionManager.setSiteValidation(siteId, valId)
                    }
            }
            scope.launch(Dispatchers.IO) {
                snapshotFlow { fieldValues.toMap() }
                    .debounce(500)
                    .collectLatest { values ->
                        values.forEach { (specId, value) ->
                            sessionManager.updateFieldValue(siteId, specId, value)
                        }
                    }
            }
        }
    }

    fun addField(spec: FieldSpec) {
        sessionManager.addFieldToSite(siteId, spec)
        if (fieldValues.containsKey(spec.id)) return
        fieldSpecs =
            fieldSpecs + ReportField(siteId, spec.id, spec.name, spec.type.datatype, spec.unit)
        fieldValues[spec.id] = ""
    }

    fun applyTemplate(template: ReportTemplate) {
        sessionManager.applyTemplateToSite(siteId, template)
        val fresh = sessionManager.getFieldsForSite(siteId)
        fieldSpecs = fresh
        fresh.forEach { fieldValues.putIfAbsent(it.specId, it.value) }
    }

    fun removeField(specId: String) {
        sessionManager.removeFieldFromSite(siteId, specId)
        fieldSpecs = fieldSpecs.filter { it.specId != specId }
        fieldValues.remove(specId)
    }

    fun saveAndConfirm() {
        positionKm?.let { sessionManager.updateSitePosition(siteId, it) }
        location?.let { loc ->
            sessionManager.updateSiteLocation(siteId, loc)
        }
        sessionManager.setSiteValidation(siteId, validationId)
        fieldValues.forEach { (specId, value) ->
            sessionManager.updateFieldValue(siteId, specId, value)
        }
        sessionManager.confirmSite(siteId)
    }
}

@Composable
private fun rememberSiteEditState(site: Site): SiteEditState {
    val sessionManager = LocalSessionManager.current
    val scope = rememberCoroutineScope()
    return remember(site) {
        val fields = sessionManager.getFieldsForSite(site.id)
        SiteEditState(site, fields, sessionManager, scope)
    }
}

// --- Previews ---

private val previewSite = Site(
    id = "preview",
    reportId = "r1",
    positionKm = 12.345,
    location = Coordinates(lat = 55.751244, lng = 37.618423),
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
private fun SitePreviewUnverifiedPreview() = PtmTheme {
    val unverifiedSite = previewSite.copy(validationId = null)
    val siteWithFields = SiteWithFields(unverifiedSite, previewFields.take(2))
    Column(Modifier.padding(16.dp)) {
        SitePreviewContent(siteWithFields) {}
    }
}

@Preview(showBackground = true)
@Composable
private fun SiteEditModePreview() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionManager = SessionManager(
        ReportDatabase.getInstance(context).reportDao(),
        null
    )
    val editState = SiteEditState(previewSite, previewFields, sessionManager, coroutineScope)
    CompositionLocalProvider(LocalSessionManager provides sessionManager) {
        PtmTheme {
            Column(Modifier.padding(16.dp)) {
                SiteEditContent(editState, null, false, true, {}) {}
            }
        }
    }
}
