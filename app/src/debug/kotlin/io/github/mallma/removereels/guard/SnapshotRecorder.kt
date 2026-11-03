package io.github.mallma.removereels.guard

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import io.github.mallma.removereels.detection.ScreenSnapshot
import io.github.mallma.removereels.detection.SnapshotNode
import java.io.File

/**
 * Debug builds only: saves a [ScreenSnapshot] of the current screen when adb asks for one, to record test fixtures.
 *
 * ```
 * adb shell am broadcast -a io.github.mallma.removereels.SNAPSHOT --es name reels_tab
 * adb exec-out run-as io.github.mallma.removereels cat files/snapshots/reels_tab.json
 * ```
 *
 * Only the shell and the system hold the DUMP permission, so no other app can trigger it.
 */
internal class SnapshotRecorder(
    private val service: AccessibilityService,
) {
    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
                val root = service.rootInActiveWindow
                when {
                    !name.matches(SAFE_NAME) -> Log.w(TAG, "Snapshot name must match $SAFE_NAME: '$name'")
                    root == null -> Log.w(TAG, "No active window to record")
                    else -> save(name, root)
                }
            }
        }

    fun start() {
        service.registerReceiver(
            receiver,
            IntentFilter(ACTION),
            Manifest.permission.DUMP,
            null,
            Context.RECEIVER_EXPORTED,
        )
    }

    fun stop() {
        service.unregisterReceiver(receiver)
    }

    private fun save(
        name: String,
        root: AccessibilityNodeInfo,
    ) {
        val packageName = root.packageName.toString()
        val nodes = mutableListOf<SnapshotNode>()
        collect(root, nodes)
        val snapshot =
            ScreenSnapshot(
                packageName = packageName,
                appVersion =
                    service.packageManager
                        .getPackageInfo(packageName, 0)
                        .versionName
                        .orEmpty(),
                screen = root.boundsInScreen(),
                nodes = nodes,
            )
        val file = File(service.filesDir, "snapshots/$name.json")
        file.parentFile?.mkdirs()
        file.writeText(snapshot.toJson())
        Log.i(TAG, "Saved ${nodes.size} views of $packageName ${snapshot.appVersion} to $file")
    }

    /** Depth first, with each view's parent; text and descriptions are never read. */
    private fun collect(
        node: AccessibilityNodeInfo,
        into: MutableList<SnapshotNode>,
        parent: Int? = null,
    ) {
        val index = into.size
        into +=
            SnapshotNode(
                viewId = node.viewIdResourceName,
                className = node.className?.toString().orEmpty(),
                bounds = node.boundsInScreen(),
                visible = node.isVisibleToUser,
                selected = node.isSelected,
                parent = parent,
            )
        for (child in 0 until node.childCount) {
            node.getChild(child)?.let { collect(it, into, index) }
        }
    }

    private companion object {
        const val TAG = "SnapshotRecorder"
        const val ACTION = "io.github.mallma.removereels.SNAPSHOT"
        const val EXTRA_NAME = "name"
        val SAFE_NAME = Regex("[a-z0-9_]+")
    }
}
