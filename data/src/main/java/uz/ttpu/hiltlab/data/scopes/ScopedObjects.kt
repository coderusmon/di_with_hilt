package uz.ttpu.hiltlab.data.scopes

import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.android.scopes.ViewModelScoped
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

private object Counters {
    val app = AtomicInteger()
    val activity = AtomicInteger()
    val screen = AtomicInteger()
    val viewModel = AtomicInteger()
}

/** One instance for the whole process. */
@Singleton
class AppSession @Inject constructor() {
    val number = Counters.app.incrementAndGet()
    init { println("created AppSession #$number") }
}

/** One instance per Activity, survives configuration changes. */
@ActivityRetainedScoped
class ActivitySession @Inject constructor() {
    val number = Counters.activity.incrementAndGet()
    init { println("created ActivitySession #$number") }
}

/** No scope: a new instance for every injection. */
class ScreenState @Inject constructor() {
    val number = Counters.screen.incrementAndGet()
    init { println("created ScreenState #$number") }
}

/** One instance per ViewModel. */
@ViewModelScoped
class ViewModelTracker @Inject constructor() {
    val number = Counters.viewModel.incrementAndGet()
    init { println("created ViewModelTracker #$number") }
}

/** Cheap stateless helper: unscoped on purpose (Q20). */
class DateFormatter @Inject constructor() {
    fun format(epochMillis: Long): String =
        java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(epochMillis))
}

/** Login state lives as long as the app, but changes at runtime. */
@Singleton
class AuthToken @Inject constructor() {
    @Volatile
    var value: String? = null
}
