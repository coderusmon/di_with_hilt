package uz.ttpu.hiltlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import uz.ttpu.hiltlab.data.logging.Logger
import uz.ttpu.hiltlab.data.scopes.ActivitySession
import uz.ttpu.hiltlab.data.scopes.AppSession
import uz.ttpu.hiltlab.data.scopes.ScreenState
import uz.ttpu.hiltlab.ui.AppNav
import uz.ttpu.hiltlab.ui.theme.HiltLabTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var logger: Logger
    @Inject lateinit var appSession: AppSession
    @Inject lateinit var activitySession: ActivitySession
    @Inject lateinit var screenState: ScreenState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.log("MainActivity created: app=${appSession.number}, activity=${activitySession.number}, screen=${screenState.number}")

        setContent {
            HiltLabTheme {
                AppNav()
            }
        }
    }
}
