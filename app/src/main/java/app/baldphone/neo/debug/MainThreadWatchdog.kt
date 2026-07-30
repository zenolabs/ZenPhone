/*
 * Copyright 2026 Zenolabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.baldphone.neo.debug

import android.os.Handler
import android.os.Looper
import android.util.Log

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reports where the main thread is stuck, for debug builds only.
 *
 * A blocked main thread produces no exception and no crash, so nothing reaches the log by
 * itself; the system writes its own trace to a directory an unrooted device will not hand over.
 * This asks the main thread to answer a message, and if it does not, prints its stack.
 *
 * Logged at error level on purpose: some vendor builds, Nothing OS among them, drop anything
 * quieter coming from a third-party app.
 */
object MainThreadWatchdog {
    private const val TAG = "ZenWatchdog"

    /** How long the main thread may take to answer before it counts as stuck. */
    private const val TIMEOUT_MS = 4_000L

    /** Frames printed per report. Deep enough for a runaway measure pass to be recognisable. */
    private const val MAX_FRAMES = 60

    fun start() {
        val mainThread = Looper.getMainLooper().thread
        val handler = Handler(Looper.getMainLooper())

        val worker =
            Thread({
                var reported = false
                while (!Thread.currentThread().isInterrupted) {
                    val answered = AtomicBoolean(false)
                    handler.post { answered.set(true) }

                    Thread.sleep(TIMEOUT_MS)

                    if (answered.get()) {
                        if (reported) {
                            Log.e(TAG, "main thread is answering again")
                            reported = false
                        }
                    } else {
                        reported = true
                        dump(mainThread)
                    }
                }
            }, "zen-watchdog")

        worker.isDaemon = true
        worker.start()
    }

    private fun dump(mainThread: Thread) {
        Log.e(TAG, "main thread silent for ${TIMEOUT_MS}ms - stack follows")
        val stack = mainThread.stackTrace
        if (stack.isEmpty()) {
            Log.e(TAG, "  (no stack available)")
            return
        }
        // One line per frame: a single multi-line message is truncated by the log buffer.
        stack.take(MAX_FRAMES).forEachIndexed { i, frame ->
            Log.e(TAG, "  #%02d %s".format(i, frame))
        }
        Log.e(TAG, "  end of stack")
    }
}
