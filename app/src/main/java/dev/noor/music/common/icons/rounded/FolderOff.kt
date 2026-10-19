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

public val Icons.Rounded.FolderOff: ImageVector
    get() {
        if (_folderOff != null) {
            return _folderOff!!
        }
        _folderOff =
            materialIcon(name = "Rounded.FolderOff") {
                materialPath {
                    moveTo(2.81f, 2.81f)
                    curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                    curveTo(1.0f, 3.2f, 1.0f, 3.83f, 1.39f, 4.22f)
                    lineToRelative(0.85f, 0.85f)
                    curveTo(2.1f, 5.35f, 2.01f, 5.66f, 2.01f, 6.0f)
                    lineTo(2.0f, 18.0f)
                    curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                    horizontalLineToRelative(13.17f)
                    lineToRelative(2.61f, 2.61f)
                    curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                    curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                    lineTo(2.81f, 2.81f)
                    close()
                }
                materialPath {
                    moveTo(20.0f, 6.0f)
                    horizontalLineToRelative(-8.0f)
                    lineToRelative(-1.41f, -1.41f)
                    curveTo(10.21f, 4.21f, 9.7f, 4.0f, 9.17f, 4.0f)
                    horizontalLineTo(6.83f)
                    lineToRelative(14.93f, 14.93f)
                    curveTo(21.91f, 18.65f, 22.0f, 18.34f, 22.0f, 18.0f)
                    verticalLineTo(8.0f)
                    curveTo(22.0f, 6.9f, 21.1f, 6.0f, 20.0f, 6.0f)
                    close()
                }
            }
        return _folderOff!!
    }

private var _folderOff: ImageVector? = null
