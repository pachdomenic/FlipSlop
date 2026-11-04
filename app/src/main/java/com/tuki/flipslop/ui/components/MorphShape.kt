package com.tuki.flipslop.ui.components

import androidx.compose.material3.toPath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph

/** A [Shape] for one frame of a [Morph] between two Material shapes. */
class MorphShape(
    private val morph: Morph,
    private val progress: Float,
    private val rotation: Float = 0f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path: Path = morph.toPath(progress)
        val matrix = Matrix().apply {
            scale(size.width, size.height)
            if (rotation != 0f) {
                translate(0.5f, 0.5f)
                rotateZ(rotation)
                translate(-0.5f, -0.5f)
            }
        }
        path.transform(matrix)
        return Outline.Generic(path)
    }
}
