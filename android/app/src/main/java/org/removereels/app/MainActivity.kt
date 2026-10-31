package org.removereels.app

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

/**
 * One WebView per platform, each loading the platform's own mobile website with the rule engine injected at document
 * start. A tab bar switches platforms; "Rules" shows each rule's switch and whether it still seems to match the site.
 */
class MainActivity : ComponentActivity() {
    private lateinit var platforms: List<Platform>
    private lateinit var store: RuleStore
    private lateinit var engineSource: String
    private lateinit var container: FrameLayout
    private val webViews = mutableMapOf<String, WebView>()
    private val tabButtons = mutableMapOf<String, Button>()
    private val scriptHandlers = mutableMapOf<String, ScriptHandler>()
    private var current: Platform? = null

    private var pendingFiles: ValueCallback<Array<Uri>>? = null
    private val fileChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        pendingFiles?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        pendingFiles = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        // Lets chrome://inspect attach to debug builds, which is how rule selectors get written and fixed.
        WebView.setWebContentsDebuggingEnabled(applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)

        platforms = Platform.loadAll(this)
        store = RuleStore(this)
        engineSource = assets.open("engine.js").bufferedReader().use { it.readText() }
        container = findViewById(R.id.web_container)

        val tabBar = findViewById<LinearLayout>(R.id.tab_bar)
        val tabParams = { LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f) }
        platforms.forEach { platform ->
            val button = Button(this, null, 0, R.style.TabButton).apply {
                text = platform.name
                setOnClickListener { show(platform) }
            }
            tabButtons[platform.id] = button
            tabBar.addView(button, tabParams())
        }
        tabBar.addView(Button(this, null, 0, R.style.TabButton).apply {
            setText(R.string.rules)
            setOnClickListener { current?.let(::showRules) }
        }, tabParams())
        tabBar.minimumHeight = (56 * resources.displayMetrics.density).toInt()

        onBackPressedDispatcher.addCallback(this) {
            val webView = current?.let { webViews[it.id] }
            if (webView?.canGoBack() == true) webView.goBack() else finish()
        }

        val lastId = getPreferences(MODE_PRIVATE).getString(LAST_PLATFORM, null)
        show(platforms.firstOrNull { it.id == lastId } ?: platforms.first())
    }

    private fun show(platform: Platform, url: String? = null) {
        val previous = current?.let { webViews[it.id] }
        val webView = webViews.getOrPut(platform.id) { createWebView(platform) }
        if (previous != null && previous !== webView) {
            previous.evaluateJavascript(PAUSE_MEDIA, null)
            previous.visibility = View.GONE
        }
        webView.visibility = View.VISIBLE
        if (url != null) webView.loadUrl(url)
        current = platform
        tabButtons.forEach { (id, button) -> button.alpha = if (id == platform.id) 1f else 0.55f }
        getPreferences(MODE_PRIVATE).edit().putString(LAST_PLATFORM, platform.id).apply()
    }

    private fun createWebView(platform: Platform): WebView = WebView(this).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // No autoplay: videos only start when tapped. Less pull to keep watching, and less mobile data.
        settings.mediaPlaybackRequiresUserGesture = true
        webViewClient = PlatformClient(platform)
        webChromeClient = ChromeClient()
        installEngine(this, platform)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(this, "rrNative", platform.origins) { _, message, _, isMainFrame, _ ->
                val data = message.data
                if (isMainFrame && data != null) store.record(platform, data)
            }
        }
        container.addView(this, FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        loadUrl(platform.startUrl)
    }

    /** (Re)installs the engine with the current rule switches; takes effect on the next page load. */
    private fun installEngine(webView: WebView, platform: Platform) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            scriptHandlers.remove(platform.id)?.remove()
            scriptHandlers[platform.id] =
                WebViewCompat.addDocumentStartJavaScript(webView, engineScript(platform), platform.origins)
        }
    }

    private fun engineScript(platform: Platform): String {
        val config = JSONObject()
            .put("platform", platform.id)
            .put("ruleSet", platform.json)
            .put("enabled", store.enabledMap(platform))
        return "(function(){\n$engineSource\nRemoveReelsEngine.start($config);\n})();"
    }

    private fun showRules(platform: Platform) {
        val rules = platform.rules
        val checked = BooleanArray(rules.size) { store.isEnabled(platform, rules[it]) }
        val items = rules.map { "${it.name}\n${statusLine(platform, it)}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.rules_title, platform.name, platform.version))
            .setMultiChoiceItems(items, checked) { _, which, isChecked -> checked[which] = isChecked }
            .setPositiveButton(R.string.apply) { _, _ ->
                rules.forEachIndexed { i, rule -> store.setEnabled(platform, rule, checked[i]) }
                webViews[platform.id]?.let { installEngine(it, platform); it.reload() }
            }
            .setNeutralButton(R.string.reset_stats) { _, _ -> store.resetStats(platform) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun statusLine(platform: Platform, rule: RuleInfo): String {
        if (!store.isEnabled(platform, rule)) return getString(R.string.status_off)
        val s = store.stats(platform, rule)
        return when {
            rule.type == "route" ->
                if (s.hits == 0) getString(R.string.status_route_idle) else getString(R.string.status_route_hits, s.hits)
            s.looksBroken -> getString(R.string.status_canary_broken, s.canaryMisses, s.canaryChecks)
            rule.canary && s.canaryChecks > 0 ->
                getString(R.string.status_canary_ok, s.canaryChecks - s.canaryMisses, s.canaryChecks)
            s.pageViews == 0 -> getString(R.string.status_no_data)
            else -> getString(R.string.status_hidden, s.hits, s.pageViews)
        }
    }

    private fun openExternally(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_app_for_link, Toast.LENGTH_SHORT).show()
        }
    }

    private inner class PlatformClient(private val platform: Platform) : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            val isWeb = uri.scheme == "https" || uri.scheme == "http"
            if (isWeb && platform.ownsHost(uri.host)) return false
            // A link to another supported platform opens in its tab, not in that platform's native app.
            val other = platforms.firstOrNull { isWeb && it.ownsHost(uri.host) }
            if (other != null) show(other, uri.toString()) else openExternally(uri)
            return true
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            // Old WebViews without document-start scripts get the engine as early as this callback allows.
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT) &&
                url != null && Uri.parse(url).let { "${it.scheme}://${it.host}" } in platform.origins
            ) {
                view.evaluateJavascript(engineScript(platform), null)
            }
        }
    }

    private inner class ChromeClient : WebChromeClient() {
        // Needed for posting: <input type="file"> does nothing in a WebView unless the app opens a picker.
        override fun onShowFileChooser(
            webView: WebView,
            callback: ValueCallback<Array<Uri>>,
            params: FileChooserParams,
        ): Boolean {
            pendingFiles?.onReceiveValue(null)
            pendingFiles = callback
            return try {
                fileChooser.launch(params.createIntent())
                true
            } catch (e: ActivityNotFoundException) {
                pendingFiles = null
                false
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        webViews.values.forEach { it.destroy() }
        super.onDestroy()
    }

    private companion object {
        const val LAST_PLATFORM = "last_platform"
        const val PAUSE_MEDIA = "document.querySelectorAll('video,audio').forEach(function(m){m.pause()})"
    }
}
