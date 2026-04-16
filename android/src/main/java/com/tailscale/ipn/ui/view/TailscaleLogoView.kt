// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
  Image(
      painter = painterResource(R.drawable.larktun_logo),
      contentDescription = stringResource(R.string.app_name),
      contentScale = ContentScale.Fit,
      modifier = modifier,
  )
}
