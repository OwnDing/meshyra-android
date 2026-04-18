// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.tailscale.ipn.R

@Composable
fun TailscaleLogoView(
    animated: Boolean = false,
    usesOnBackgroundColors: Boolean = false,
    modifier: Modifier
) {
  val animatedModifier =
      if (animated) {
        val transition = rememberInfiniteTransition(label = "tailscaleLogoBreathing")
        val breathingScale by
            transition.animateFloat(
                initialValue = 0.88f,
                targetValue = 1.14f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 1050),
                        repeatMode = RepeatMode.Reverse),
                label = "tailscaleLogoScale",
            )
        val breathingAlpha by
            transition.animateFloat(
                initialValue = 0.72f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 1050),
                        repeatMode = RepeatMode.Reverse),
                label = "tailscaleLogoAlpha",
            )
        modifier.graphicsLayer {
          scaleX = breathingScale
          scaleY = breathingScale
        }.alpha(breathingAlpha)
      } else {
        modifier
      }

  Image(
      painter = painterResource(R.drawable.larktun_logo),
      contentDescription = stringResource(R.string.app_name),
      contentScale = ContentScale.Fit,
      modifier = animatedModifier,
  )
}
