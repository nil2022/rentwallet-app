package com.thebackendguy.rentflow.ui.screens.landlord

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.thebackendguy.rentflow.data.LandlordStore
import com.thebackendguy.rentflow.data.Photo
import com.thebackendguy.rentflow.data.PropertyTypes
import com.thebackendguy.rentflow.data.remote.ApiResult
import com.thebackendguy.rentflow.data.remote.PropertyBody
import com.thebackendguy.rentflow.data.remote.PropertyDto
import com.thebackendguy.rentflow.ui.components.AppPage
import com.thebackendguy.rentflow.ui.components.ChoiceChips
import com.thebackendguy.rentflow.ui.components.CountStepper
import com.thebackendguy.rentflow.ui.components.FormBar
import com.thebackendguy.rentflow.ui.components.FormSection
import com.thebackendguy.rentflow.ui.components.PhotoGrid
import com.thebackendguy.rentflow.ui.components.RfTextField
import com.thebackendguy.rentflow.ui.components.RowDivider
import com.thebackendguy.rentflow.ui.components.SwitchRow
import com.thebackendguy.rentflow.ui.components.TextArea
import com.thebackendguy.rentflow.ui.components.Toasts
import com.thebackendguy.rentflow.ui.icons.Lucide
import com.thebackendguy.rentflow.ui.screens.Shell
import kotlinx.coroutines.launch

/**
 * Add a property, or edit one when [propertyId] is set. The same fields as the
 * web form, grouped into short sections.
 */
@Composable
fun PropertyFormScreen(propertyId: String?, shell: Shell, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val state = portfolioState()
    val existing = propertyId?.let { state.data?.property(it) }
    if (propertyId != null && existing == null) {
        AppPage(topBar = { shell.TopBar("Edit Property", onBack = onBack) }) {
            PortfolioContent(state) { NotFound("Property", onBack) }
        }
        return
    }
    PropertyForm(existing, shell, onBack, onSaved)
}

@Composable
private fun PropertyForm(existing: PropertyDto?, shell: Shell, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by rememberSaveable { mutableStateOf(existing?.propertyName.orEmpty()) }
    var type by rememberSaveable { mutableIntStateOf(PropertyTypes.indexOfFirst { it.first == existing?.propertyType }) }
    var address by rememberSaveable { mutableStateOf(existing?.address.orEmpty()) }
    var city by rememberSaveable { mutableStateOf(existing?.city.orEmpty()) }
    var region by rememberSaveable { mutableStateOf(existing?.state.orEmpty()) }
    var country by rememberSaveable { mutableStateOf(existing?.country ?: "India") }
    var pincode by rememberSaveable { mutableStateOf(existing?.pincode.orEmpty()) }
    var floors by rememberSaveable { mutableIntStateOf(existing?.floorCount?.coerceAtLeast(1) ?: 1) }
    var roomCount by rememberSaveable { mutableIntStateOf(existing?.roomCount?.coerceAtLeast(1) ?: 1) }
    var parking by rememberSaveable { mutableStateOf(existing?.hasParking ?: false) }
    var lift by rememberSaveable { mutableStateOf(existing?.hasLift ?: false) }
    var description by rememberSaveable { mutableStateOf(existing?.description.orEmpty()) }
    var photos by remember { mutableStateOf<List<Photo>>(existing?.images.orEmpty().map { Photo.Stored(it.key, it.url) }) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    fun required(value: String, what: String) = if (submitted && value.isBlank()) "$what is required" else null
    val nameErr = required(name, "Property name")
    val typeErr = if (submitted && type < 0) "Choose a property type" else null
    val addressErr = required(address, "Address")
    val cityErr = required(city, "City")
    val regionErr = required(region, "State")
    val countryErr = required(country, "Country")
    val pincodeErr = if (submitted && !(pincode.length == 6 && pincode.first() != '0')) "Enter a 6-digit pincode" else null

    fun save() {
        submitted = true
        val valid = listOf(name, address, city, region, country).all(String::isNotBlank) && type >= 0 &&
            pincode.length == 6 && pincode.first() != '0'
        if (!valid) {
            Toasts.error("Some details are missing. Check the fields marked in red.")
            return
        }
        if (saving) return
        saving = true
        scope.launch {
            val body = PropertyBody(
                propertyName = name.trim(), address = address.trim(), city = city.trim(), state = region.trim(),
                country = country.trim(), pincode = pincode, propertyType = PropertyTypes[type].first,
                floorCount = floors, roomCount = roomCount, hasParking = parking, hasLift = lift,
                description = description.trim(), images = emptyList()
            )
            val result = LandlordStore.saveProperty(context, existing?.id, body, photos)
            saving = false
            if (result.announce(if (existing == null) "Property added" else "Property saved")) {
                onSaved((result as ApiResult.Ok).value)
            }
        }
    }

    AppPage(
        topBar = { shell.TopBar(if (existing == null) "Add Property" else "Edit Property", onBack = onBack) },
        bottomBar = { FormBar("Save property", ::save, loading = saving, secondary = "Cancel", onSecondary = onBack) }
    ) {
        FormSection(Lucide.Building2, "Basic details") {
            RfTextField(
                label = "Property name", value = name, onValueChange = { name = it }, icon = Lucide.Building2,
                placeholder = "e.g. Green View Residency", capitalization = KeyboardCapitalization.Words, error = nameErr
            )
            ChoiceChips("Property type", PropertyTypes.map { it.second }, type, { type = it }, required = true, error = typeErr)
        }

        FormSection(Lucide.MapPin, "Address") {
            RfTextField(
                label = "Property address", value = address, onValueChange = { address = it }, icon = Lucide.MapPin,
                placeholder = "House, street and area", capitalization = KeyboardCapitalization.Words, error = addressErr
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RfTextField(
                    label = "City", value = city, onValueChange = { city = it }, icon = Lucide.MapPin, placeholder = "City",
                    capitalization = KeyboardCapitalization.Words, error = cityErr, modifier = Modifier.weight(1f)
                )
                RfTextField(
                    label = "State", value = region, onValueChange = { region = it }, icon = Lucide.MapPin, placeholder = "State",
                    capitalization = KeyboardCapitalization.Words, error = regionErr, modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RfTextField(
                    label = "Country", value = country, onValueChange = { country = it }, icon = Lucide.MapPin, placeholder = "Country",
                    capitalization = KeyboardCapitalization.Words, error = countryErr, modifier = Modifier.weight(1f)
                )
                RfTextField(
                    label = "Pincode", value = pincode, onValueChange = { pincode = it.filter(Char::isDigit).take(6) },
                    icon = Lucide.MapPin, placeholder = "700091", keyboardType = KeyboardType.Number,
                    error = pincodeErr, modifier = Modifier.weight(1f)
                )
            }
        }

        FormSection(Lucide.Layers, "Building") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CountStepper("Total floors", floors, { floors = it }, Modifier.weight(1f))
                CountStepper("Total rooms", roomCount, { roomCount = it }, Modifier.weight(1f))
            }
            Column {
                SwitchRow(Lucide.Car, "Parking", parking, { parking = it })
                RowDivider()
                SwitchRow(Lucide.ArrowUpDown, "Lift", lift, { lift = it })
            }
        }

        FormSection(Lucide.Image, "Photos", caption = "Up to 10") {
            PhotoGrid(photos, onChange = { photos = it })
        }

        FormSection(Lucide.FileText, "Description", caption = "Optional") {
            TextArea("About the property", description, { description = it }, "What should tenants know about this place?")
        }
    }
}
