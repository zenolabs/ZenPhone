package app.baldphone.neo.launcher.apps

import android.widget.ImageView

import coil3.dispose
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.fallback
import coil3.request.placeholder

import app.baldphone.neo.launcher.apps.data.PredefinedApps
import app.baldphone.neo.launcher.apps.data.db.AppEntry
import app.baldphone.neo.launcher.apps.sync.AppIconStorage

import com.bald.uriah.baldphone.R

/**
 * Handles binding app icons to ImageViews using Coil.
 */
object AppIconBinder {
    @JvmStatic
    fun loadPic(app: AppEntry?, imageView: ImageView) {
        val icon: Any? =
            when {
                app == null -> {
                    null
                }

                app.isPredefined -> {
                    PredefinedApps.getIconResId(app.componentName)
                }

                else -> {
                    AppIconStorage.getCachedIconFile(
                        imageView.context.applicationContext,
                        app.componentName,
                        app.userId
                    )
                }
            }

        imageView.load(icon) {
            placeholder(R.drawable.ic_default_app_icon)
            error(android.R.drawable.sym_def_app_icon)
            fallback(R.drawable.ic_default_app_icon)
            crossfade(false)
        }
    }

    /**
     * Drops any load still in flight on this view.
     *
     * Needed wherever an ImageView is reused: loading is asynchronous, so a request started for
     * one app will happily deliver its icon to whatever the view has become in the meantime,
     * overwriting an icon set since. Harmless to call when nothing is pending.
     */
    @JvmStatic
    fun cancel(imageView: ImageView) {
        imageView.dispose()
    }
}
