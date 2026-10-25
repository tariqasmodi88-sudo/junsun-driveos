package it.driveos.launcher.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Icone non presenti nel set "core" di Material, disegnate qui (forme standard). */
val PauseIcon: ImageVector = ImageVector.Builder("Pausa", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(6f, 19f); horizontalLineTo(10f); verticalLineTo(5f); horizontalLineTo(6f); close()
        moveTo(14f, 5f); verticalLineTo(19f); horizontalLineTo(18f); verticalLineTo(5f); close()
    }
}.build()

val RadioIcon: ImageVector = ImageVector.Builder("Radio", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        // antenna + corpo radio con manopola
        moveTo(3.24f, 6.15f); curveTo(2.51f, 6.43f, 2.12f, 7.24f, 2.4f, 7.97f)
        curveTo(2.68f, 8.7f, 3.49f, 9.09f, 4.22f, 8.81f)
        lineTo(18.6f, 3.36f); curveTo(19.09f, 3.17f, 19.4f, 2.69f, 19.4f, 2.17f)
        curveTo(19.4f, 1.5f, 18.85f, 0.95f, 18.18f, 0.95f)
        curveTo(18.03f, 0.95f, 17.88f, 0.98f, 17.74f, 1.03f)
        lineTo(3.24f, 6.15f); close()
        moveTo(7f, 20f); horizontalLineTo(5f); verticalLineTo(22f); horizontalLineTo(7f); close()
        moveTo(20f, 10f); horizontalLineTo(5f); curveTo(3.9f, 10f, 3f, 10.9f, 3f, 12f)
        verticalLineTo(18f); curveTo(3f, 19.1f, 3.9f, 20f, 5f, 20f)
        horizontalLineTo(20f); curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
        verticalLineTo(12f); curveTo(22f, 10.9f, 21.1f, 10f, 20f, 10f); close()
        moveTo(8f, 18f); curveTo(6.34f, 18f, 5f, 16.66f, 5f, 15f)
        curveTo(5f, 13.34f, 6.34f, 12f, 8f, 12f); curveTo(9.66f, 12f, 11f, 13.34f, 11f, 15f)
        curveTo(11f, 16.66f, 9.66f, 18f, 8f, 18f); close()
        moveTo(18f, 17f); horizontalLineTo(14f); verticalLineTo(15f); horizontalLineTo(18f); close()
        moveTo(18f, 14f); horizontalLineTo(14f); verticalLineTo(12f); horizontalLineTo(18f); close()
    }
}.build()

val MicIcon: ImageVector = ImageVector.Builder("Microfono", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 14f)
        curveTo(13.66f, 14f, 15f, 12.66f, 15f, 11f)
        verticalLineTo(5f)
        curveTo(15f, 3.34f, 13.66f, 2f, 12f, 2f)
        curveTo(10.34f, 2f, 9f, 3.34f, 9f, 5f)
        verticalLineTo(11f)
        curveTo(9f, 12.66f, 10.34f, 14f, 12f, 14f)
        close()
        moveTo(17.3f, 11f)
        curveTo(17.3f, 14f, 14.76f, 16.1f, 12f, 16.1f)
        curveTo(9.24f, 16.1f, 6.7f, 14f, 6.7f, 11f)
        horizontalLineTo(5f)
        curveTo(5f, 14.41f, 7.72f, 17.23f, 11f, 17.72f)
        verticalLineTo(21f)
        horizontalLineTo(13f)
        verticalLineTo(17.72f)
        curveTo(16.28f, 17.24f, 19f, 14.42f, 19f, 11f)
        horizontalLineTo(17.3f)
        close()
    }
}.build()
