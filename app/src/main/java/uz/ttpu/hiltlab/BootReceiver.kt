package uz.ttpu.hiltlab

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import uz.ttpu.hiltlab.data.logging.Logger

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var logger: Logger

    override fun onReceive(context: Context, intent: Intent) {
        // Hilt injects fields in the generated Hilt_BootReceiver before this method runs.
        logger.log("BootReceiver received: ${intent.action}")
    }
}
