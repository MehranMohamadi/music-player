/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.noor.music.common.icons.rounded

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Rounded.PlaylistRemove: ImageVector
    get() {
        if (_playlistRemove != null) {
            return _playlistRemove!!
        }
        _playlistRemove =
            materialIcon(name = "Rounded.PlaylistRemove") {
                materialPath {
                    moveTo(13.71f, 21.3f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    lineTo(17.0f, 19.41f)
                    lineToRelative(1.89f, 1.89f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    reflectiveCurveToRelative(0.39f, -1.02f, 0.0f, -1.41f)
                    lineTo(18.41f, 18.0f)
                    lineToRelative(1.89f, -1.89f)
                    curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                    reflectiveCurveToRelative(-1.02f, -0.39f, -1.41f, 0.0f)
                    lineTo(17.0f, 16.59f)
                    lineToRelative(-1.89f, -1.89f)
                    curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                    reflectiveCurveToRelative(-0.39f, 1.02f, 0.0f, 1.41f)
                    lineTo(15.59f, 18.0f)
                    lineToRelative(-1.89f, 1.89f)
                    curveTo(13.32f, 20.27f, 13.32f, 20.91f, 13.71f, 21.3f)
                    close()
                    moveTo(14.0f, 11.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineTo(4.0f)
                    curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                    reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(9.0f)
                    curveTo(13.55f, 12.0f, 14.0f, 11.55f, 14.0f, 11.0f)
                    close()
                    moveTo(14.0f, 7.0f)
                    curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineTo(4.0f)
                    curveTo(3.45f, 6.0f, 3.0f, 6.45f, 3.0f, 7.0f)
                    reflectiveCurveToRelative(0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(9.0f)
                    curveTo(13.55f, 8.0f, 14.0f, 7.55f, 14.0f, 7.0f)
                    close()
                    moveTo(3.0f, 15.0f)
                    curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                    horizontalLineToRelative(5.0f)
                    curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                    reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                    horizontalLineTo(4.0f)
                    curveTo(3.45f, 14.0f, 3.0f, 14.45f, 3.0f, 15.0f)
                    close()
                }
            }
        return _playlistRemove!!
    }

private var _playlistRemove: ImageVector? = null
