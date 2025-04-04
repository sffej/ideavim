/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.intellij.idea.TestFor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.commands.UndoCommand
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class UndoCommandTest : VimTestCase() {
  @Test
  fun `command parsing`() {
    val command = injector.vimscriptParser.parseCommand("undo")
    assertTrue(command is UndoCommand)
  }

  @Test
  @TestFor(issues = ["VIM-3671"])
  fun `test undo scrolls caret to reset scrolloff`() {
    configureByLines(200, "lorem ipsum dolor sit amet")
    enterCommand("set scrolloff=10")
    typeText("50G", "dd", "G")
    enterCommand("undo")
    assertPosition(49, 0)
    assertVisibleArea(39, 73)
  }
}
