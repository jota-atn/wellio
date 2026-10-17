package com.projeto.wellio.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.projeto.wellio.domain.model.ActivityLevel
import com.projeto.wellio.domain.model.Profile
import com.projeto.wellio.domain.model.Sex
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

class ProfileStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.store() = ProfileStore(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { folder.newFile("test.preferences_pb") }
    )

    @Test
    fun `sem nada salvo o perfil vem vazio e sem onboarding`() = runTest {
        assertEquals(Profile(), store().profile.first())
    }

    @Test
    fun `guarda e le o perfil completo`() = runTest {
        val store = store()
        val profile = Profile(
            name = "João",
            photoFile = "profile_1.jpg",
            birthDate = LocalDate.of(1999, 3, 14),
            sex = Sex.Male,
            heightCm = 178,
            activity = ActivityLevel.Moderate,
            onboardingDone = true
        )

        store.save(profile)

        assertEquals(profile, store.profile.first())
    }

    @Test
    fun `campo apagado volta a ficar vazio`() = runTest {
        val store = store()
        store.save(Profile(name = "João", heightCm = 178, sex = Sex.Female))

        store.save(Profile(name = "João"))

        assertEquals(Profile(name = "João"), store.profile.first())
    }
}

class ProfilePhotoStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `foto nova ganha nome novo e apaga a antiga`() = runTest {
        var clock = 1L
        val store = ProfilePhotoStore(folder.root) { clock }

        val first = store.save { it.write(byteArrayOf(1)) }
        clock = 2L
        val second = store.save { it.write(byteArrayOf(2)) }

        assertFalse(first == second)
        assertFalse(store.file(first).exists())
        assertTrue(store.file(second).readBytes().contentEquals(byteArrayOf(2)))
    }

    @Test
    fun `remover apaga a foto`() = runTest {
        val store = ProfilePhotoStore(folder.root) { 1L }
        val name = store.save { it.write(byteArrayOf(1)) }

        store.clear()

        assertFalse(store.file(name).exists())
    }
}
