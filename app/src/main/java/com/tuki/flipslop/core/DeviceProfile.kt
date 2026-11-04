package com.tuki.flipslop.core

import android.os.Build
import com.tuki.flipslop.fold.PanelSize

/**
 * Known-good defaults for devices we have reference configs for. Everything here
 * is only a starting point: panel sizes are learned at runtime and the bottom
 * inset is user adjustable.
 */
data class DeviceProfile(
    val device: String,
    /** Cover panel in its natural orientation. */
    val coverPanel: PanelSize?,
    /** Inner panel in its natural orientation. */
    val innerPanel: PanelSize?,
    /**
     * Space to keep free at the bottom of the cover screen, for camera holes the
     * framework does not model as a cutout.
     */
    val coverBottomInsetDp: Int,
) {
    val knownPanels: Set<PanelSize> get() = setOfNotNull(coverPanel, innerPanel)

    companion object {
        /**
         * Motorola razr 50 Ultra / razr+ 2024, from the LineageOS arcfox tree:
         *  - configs/displayconfig/display_id_4630947043778501764.xml: "Outer Display",
         *    1080x1272 @ 360 dpi
         *  - TARGET_SCREEN_WIDTH/HEIGHT: inner panel 1080x2640
         *  - SystemUIResArcfox values-sw430dp: keyguard_indication_margin_bottom = 143dp,
         *    which keeps the lock screen text above the camera block on the cover
         */
        val ARCFOX = DeviceProfile(
            device = "arcfox",
            coverPanel = PanelSize(1080, 1272),
            innerPanel = PanelSize(1080, 2640),
            coverBottomInsetDp = 143,
        )

        val GENERIC = DeviceProfile(
            device = "generic",
            coverPanel = null,
            innerPanel = null,
            coverBottomInsetDp = 0,
        )

        fun forDevice(device: String): DeviceProfile = when (device) {
            "arcfox" -> ARCFOX
            else -> GENERIC
        }

        val current: DeviceProfile by lazy { forDevice(Build.DEVICE) }
    }
}
