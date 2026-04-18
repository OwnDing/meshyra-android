// Copyright (c) Tailscale Inc & AUTHORS
// SPDX-License-Identifier: BSD-3-Clause

package com.tailscale.ipn.ui.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TailcfgNodeDisplayNameTest {

  @Test
  fun aliasNameReadsFirstCapMapString() {
    val node =
        testNode(
            capMap =
                mapOf(
                    "https://meshyra.example/cap/node-alias" to
                        Json.parseToJsonElement("""["Office Mac"]""")))

    assertEquals("Office Mac", node.aliasName)
    assertEquals("Office Mac", node.displayName)
  }

  @Test
  fun displayNamePrefersHostnameWhenAliasMissing() {
    val node = testNode(hostName = "macbook-host", computedName = "dns-label")

    assertEquals("macbook-host", node.displayName)
  }

  @Test
  fun displayNameFallsBackToComputedNameThenName() {
    val computedNode = testNode(hostName = null, computedName = "dns-label")
    val rawNameNode = testNode(hostName = null, computedName = null, name = "raw-name.")

    assertEquals("dns-label", computedNode.displayName)
    assertEquals("raw-name", rawNameNode.displayName)
  }

  @Test
  fun aliasNameIgnoresInvalidOrBlankPayloads() {
    val objectPayloadNode =
        testNode(
            capMap =
                mapOf(
                    "https://meshyra.example/cap/node-alias" to
                        Json.parseToJsonElement("""[{"alias":"Office Mac"}]""")))
    val blankPayloadNode =
        testNode(
            capMap =
                mapOf(
                    "https://meshyra.example/cap/node-alias" to
                        Json.parseToJsonElement("""["   "]""")))

    assertNull(objectPayloadNode.aliasName)
    assertNull(blankPayloadNode.aliasName)
    assertEquals("macbook-host", objectPayloadNode.displayName)
    assertEquals("macbook-host", blankPayloadNode.displayName)
  }

  private fun testNode(
      hostName: String? = "macbook-host",
      computedName: String? = "dns-label",
      name: String = "dns-label.ts.net.",
      capMap: Map<String, JsonElement?>? = null,
  ): Tailcfg.Node {
    return Tailcfg.Node(
        ID = 1L,
        StableID = "node-1",
        Name = name,
        User = 1L,
        Key = "nodekey:123",
        KeyExpiry = GoZeroTimeString,
        Machine = "machine-key",
        Hostinfo = Tailcfg.Hostinfo(Hostname = hostName),
        Created = GoZeroTimeString,
        CapMap = capMap,
        ComputedName = computedName,
        ComputedNameWithHost = computedName)
  }
}
