package os.kei.mcp.bridge

import os.kei.feature.home.mcp.McpHomeBaSnapshot
import os.kei.feature.home.mcp.McpHomeBaSnapshotProvider
import os.kei.feature.home.model.isHomeBaActivated
import os.kei.ui.page.main.ba.support.BASettingsStore
import os.kei.ui.page.main.ba.support.cafeStorageCap
import os.kei.ui.page.main.ba.support.displayAp

internal data object AppMcpHomeBaSnapshotProvider : McpHomeBaSnapshotProvider {
    override fun loadSnapshot(): McpHomeBaSnapshot {
        val snapshot = BASettingsStore.loadSnapshot()
        val cafeLevel = snapshot.cafeLevel.coerceIn(1, 10)
        val cafeCap = cafeStorageCap(cafeLevel)
        val cafeStored = snapshot.cafeStoredAp.coerceIn(0.0, cafeCap)
        return McpHomeBaSnapshot(
            activated = isHomeBaActivated(snapshot.idFriendCode),
            apCurrent = displayAp(snapshot.apCurrent),
            apLimit = snapshot.apLimit,
            cafeLevel = cafeLevel,
            cafeStored = cafeStored.toInt(),
            cafeCap = cafeCap.toInt(),
        )
    }
}
