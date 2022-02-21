/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.editor.Caret
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.common.EditorLine
import com.maddyhome.idea.vim.common.Offset
import com.maddyhome.idea.vim.common.offset
import com.maddyhome.idea.vim.group.MotionGroup

class IjVimCaret(val caret: Caret) : VimCaret {
  override val editor: VimEditor
    get() = IjVimEditor(caret.editor)
  override val offset: Offset
    get() = caret.offset.offset

  override fun moveToOffset(offset: Int) {
    // TODO: 17.12.2021 Unpack internal actions
    MotionGroup.moveCaret(caret.editor, caret, offset)
  }

  override fun offsetForLineStartSkipLeading(line: Int): Int {
    return VimPlugin.getMotion().moveCaretToLineStartSkipLeading((editor as IjVimEditor).editor, line)
  }

  override fun getLine(): EditorLine.Pointer {
    return EditorLine.Pointer.init(caret.logicalPosition.line, editor)
  }

  override fun hasSelection(): Boolean {
    return caret.hasSelection()
  }

  override fun equals(other: Any?): Boolean = this.caret == (other as? IjVimCaret)?.caret

  override fun hashCode(): Int = this.caret.hashCode()
}

val VimCaret.ij: Caret
  get() = (this as IjVimCaret).caret

val Caret.vim: VimCaret
  get() = IjVimCaret(this)