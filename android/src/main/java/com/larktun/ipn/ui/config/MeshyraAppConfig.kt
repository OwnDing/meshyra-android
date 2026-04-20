// SPDX-License-Identifier: BSD-3-Clause

package com.larktun.ipn.ui.config

/** App-level configuration switches for Meshyra Client. */
object MeshyraAppConfig {
  /**
   * Tailscale's upstream Android app uses `BrowseToURL` to trigger opening a browser for login.
   *
   * Meshyra Client uses its own login flow, so we disable the browser-based login by default.
   */
  const val ENABLE_TAILSCALE_BROWSER_LOGIN = false
}

