package space.diomentia.ehzdiagnosticstracker.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.preferences: DataStore<Preferences> by preferencesDataStore(name = "preferences")

object AppPreferences {
    class ApplicationPreferencesException : IllegalArgumentException()

    val KEY_ADMIN_HASHWORD = stringPreferencesKey("hashword_admin")

    const val DEFAULT_ADMIN_HASHWORD =
        "9a3a1db1fc6019de41f996a01493f5341ef743a765de87c13ea70b6c618cae7e30bf338953cc481a78050b93bafefd02264bd7639f18a00d62c22f9b5fdb9194"

    @Composable
    fun rememberAdminHashword() = rememberPreference(
        key = KEY_ADMIN_HASHWORD,
        defaultValue = DEFAULT_ADMIN_HASHWORD,
        encoder = {
            if (AppState.adminSession)
                it.hash
            else
                throw SecurityException("shouldn't be able to change admin password")
        },
        decoder = { PasswordHash.fromHash(it) }
    )

    suspend fun <T> Context.getPreference(
        key: Preferences.Key<T>
    ) = preferences.data.first()[key]

    suspend fun <T> Context.getPreference(
        key: Preferences.Key<T>,
        defaultValue: T
    ) = getPreference(key) ?: defaultValue

    @OptIn(DelicateCoroutinesApi::class)
    @Composable
    fun <P, T> rememberPreference(
        key: Preferences.Key<P>,
        decoder: (P) -> T,
        encoder: (T) -> P,
        defaultValue: P
    ): MutableState<T> {
        val context = LocalContext.current
        val state by remember {
            context.preferences.data
                .map {
                    it[key] ?: defaultValue
                }
        }.collectAsState(initial = defaultValue)
        return remember {
            object : MutableState<T> {
                override var value: T
                    get() = decoder(state)
                    set(value) {
                        GlobalScope.launch {
                            context.preferences.edit {
                                it[key] = encoder(value)
                            }
                        }
                    }

                override fun component1() = value
                override fun component2(): (T) -> Unit = { value = it }
            }
        }
    }

    @Composable
    fun <T> rememberPreference(
        key: Preferences.Key<T>,
        defaultValue: T
    ) = rememberPreference(key, { it }, { it }, defaultValue)
}