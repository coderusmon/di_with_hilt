package uz.ttpu.hiltlab

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uz.ttpu.hiltlab.data.scopes.AppSession
import uz.ttpu.hiltlab.data.scopes.ScreenState
import javax.inject.Inject
import javax.inject.Provider

/** Q16: @Singleton returns the same instance, an unscoped class does not. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ScopeTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var appSession: AppSession
    @Inject lateinit var appSessionProvider: Provider<AppSession>
    @Inject lateinit var screenStateProvider: Provider<ScreenState>

    @Before
    fun inject() = hiltRule.inject()

    @Test
    fun singletonIsShared() {
        assertSame(appSession, appSessionProvider.get())
    }

    @Test
    fun unscopedObjectIsCreatedEveryTime() {
        assertNotSame(screenStateProvider.get(), screenStateProvider.get())
    }
}
