/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.functions.bitwiseFunctions

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class AndFunctionTest : VimTestCase() {
  @BeforeEach
  override fun setUp(testInfo: TestInfo) {
    super.setUp(testInfo)
    configureByText("\n")
  }

  @Test
  fun `test and function bitwise ANDs two numbers`() {
    assertCommandOutput("echo and(2, 6)", "2")
  }

  @Test
  fun `test and function bitwise ANDs two numbers with negative numbers`() {
    assertCommandOutput("echo and(-2, -6)", "-6")
  }

  @Test
  fun `test and function returns 0`() {
    assertCommandOutput("echo and(2, 4)", "0")
  }

  @Test
  fun `test and function coerces string to number`() {
    assertCommandOutput("echo and('0xFF', '35')", "35")
  }

  @Test
  fun `test and function with list causes error`() {
    enterCommand("echo and([1, 2, 3], [2, 3, 4])")
    assertNoExOutput()
    assertPluginError(true)
    assertPluginErrorMessageContains("E745: Using a List as a Number")
  }

  @Test
  fun `test and function with dict causes error`() {
    enterCommand("echo and({1: 2, 3: 4}, {3: 4, 5: 6})")
    assertNoExOutput()
    assertPluginError(true)
    assertPluginErrorMessageContains("E728: Using a Dictionary as a Number")
  }

  @Test
  fun `test and function with float causes error`() {
    enterCommand("echo and(1.5, 2.5)")
    assertNoExOutput()
    assertPluginError(true)
    assertPluginErrorMessageContains("E805: Using a Float as a Number")
  }
}
