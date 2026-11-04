package com.tuki.flipslop.core

import android.os.Build
import com.tuki.flipslop.fold.PanelSize

/** Which side of the cover's bottom edge the camera block sits on. */
enum class CameraSide { LEFT, RIGHT }

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
    /** Width of the camera block inside that bottom strip; 0 when there is none. */
    val coverCameraWidthDp: Int,
    val coverCameraSide: CameraSide,
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
         *
         * The camera block (flash + two ~90dp lenses) sits in the bottom-right
         * corner and is about 264dp wide, measured from a photo of the device;
         * the tree has no cutout for it. The user can adjust it in settings.
         */
        val ARCFOX = DeviceProfile(
            device = "arcfox",
            coverPanel = PanelSize(1080, 1272),
            innerPanel = PanelSize(1080, 2640),
            coverBottomInsetDp = 143,
            coverCameraWidthDp = 264,
            coverCameraSide = CameraSide.RIGHT,
        )

        val GENERIC = DeviceProfile(
            device = "generic",
            coverPanel = null,
            innerPanel = null,
            coverBottomInsetDp = 0,
            coverCameraWidthDp = 0,
            coverCameraSide = CameraSide.LEFT,
        )

        fun forDevice(device: String): DeviceProfile = when (device) {
            "arcfox" -> ARCFOX
            else -> GENERIC
        }

        val current: DeviceProfile by lazy { forDevice(Build.DEVICE) }
    }
}
