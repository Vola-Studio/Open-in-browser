package xyz.vola.openinbrowser

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

const val OPEN_DIALOG = "xyz.vola.openinbrowser.TEST_OPEN_DIALOG"
class DialogContainer : AppCompatActivity(), BrowsersListDialogFragment.Listener {
    /** 明确已知不支持的包 */
    private lateinit var unsupportedPackages: List<Pair<String, Long>>
    /** 在非 CT 中打开体验更好的包 */
    private lateinit var betterInStandalone: Array<String>
    private lateinit var url: Uri
    private val tag = "Container"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tag, "onCreate")
        setContentView(R.layout.activity_dialog_container)
        ContextCompat.startForegroundService(this, Intent(this, ClipboardListener::class.java))
        OpenInBrowserNotification.cancel(this)

        unsupportedPackages = resources.getStringArray(R.array.unsupported_packages).map {
            val (name, version) = it.split(",")
            return@map Pair(name, version.toLong())
        }
        betterInStandalone = resources.getStringArray(R.array.better_in_standalone_packages)
        if (intent.getBooleanExtra(OPEN_DIALOG, false)) {
            url = intent.data!!
            BrowsersListDialogFragment.newInstance(getCustomTabsPackages(this), getBetterInStandalonePackages(this))
                .show(supportFragmentManager, "choose_browser")
        } else {
            finish()
        }
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(tag, "onRestart")
        finish()
    }

    override fun onPause() {
        super.onPause()
        Log.d(tag, "onPause")
        finish()
    }

    override fun onDialogDismissed() {
        finish()
    }

    override fun onBrowsersClicked(packageName: String) {
        if (betterInStandalone.contains(packageName)) {
            /**  如果直接启动体验更好的话*/
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = url
                `package` = packageName
            })
        } else {
            /** 打开 Custom tab，并尝试使用 Incognito 模式 */
            CustomTabsIntent.Builder().build().apply {
                resources.getStringArray(R.array.incognito_flags).forEach { x -> intent.putExtra(x, true) }
                intent.`package` = packageName
            }.launchUrl(this, url)
        }
    }

    override fun isCustomTabBrowserSupportIncognito(
        packageManager: PackageManager,
        packageName: String
    ): BrowsersListDialogFragment.Listener.SupportingTypeOfICT {
        if (betterInStandalone.contains(packageName)) {
            return BrowsersListDialogFragment.Listener.SupportingTypeOfICT.Ignore
        }
        val version = packageManager.getPackageInfo(packageName, 0).longVersionCode
        return if (unsupportedPackages.any { it.first == packageName && it.second >= version })
            BrowsersListDialogFragment.Listener.SupportingTypeOfICT.NotSupport
        else BrowsersListDialogFragment.Listener.SupportingTypeOfICT.Unknown
    }

    private val mCTIntent = "android.support.customtabs.action.CustomTabsService"
    /**
     * 支持 CT 的浏览器
     */
    private fun getCustomTabsPackages(context: Context): List<ResolveInfo> {
        val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))
        val ctIntent = Intent().apply {
            action = mCTIntent
        }
        val resolvedActivityList =
            context.packageManager.queryIntentActivities(activityIntent, PackageManager.MATCH_ALL)
        return resolvedActivityList.filter {
            ctIntent.setPackage(it.activityInfo.packageName)
            return@filter context.packageManager.resolveService(ctIntent, 0) != null
        }.filter {
            !betterInStandalone.contains(it.activityInfo.packageName)
        }
    }

    /**
     * 直接打开体验更好的浏览器
     */
    private fun getBetterInStandalonePackages(context: Context): List<ResolveInfo> {
        val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))
        val resolvedActivityList =
            context.packageManager.queryIntentActivities(activityIntent, PackageManager.MATCH_ALL)
        return resolvedActivityList.filter { x -> betterInStandalone.any { y -> x.activityInfo.packageName == y } }
    }
}
