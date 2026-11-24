package com.example.claims.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.claims.domain.ClaimType
import com.example.claims.domain.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** DataStore trzyma wyłącznie dane niewrażliwe — numer polisy celowo NIE jest zapamiętywany. */
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    override val lastClaimType: Flow<ClaimType> = dataStore.data.map { prefs ->
        prefs[LAST_CLAIM_TYPE]?.let { runCatching { ClaimType.valueOf(it) }.getOrNull() } ?: ClaimType.AUTO
    }

    override suspend fun setLastClaimType(type: ClaimType) {
        dataStore.edit { it[LAST_CLAIM_TYPE] = type.name }
    }

    private companion object {
        val LAST_CLAIM_TYPE = stringPreferencesKey("last_claim_type")
    }
}
