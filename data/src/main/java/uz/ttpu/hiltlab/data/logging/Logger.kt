package uz.ttpu.hiltlab.data.logging

import android.util.Log
import javax.inject.Inject

interface Logger {
    fun log(message: String)
}

class LogcatLogger @Inject constructor() : Logger {
    override fun log(message: String) {
        Log.d(TAG, message)
    }

    private companion object {
        const val TAG = "HiltLab"
    }
}

/** Alternative implementation used to show one-line @Binds switching. */
class FileLogger @Inject constructor() : Logger {
    override fun log(message: String) {
        println("[file] $message")
    }
}
