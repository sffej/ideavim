/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType

interface VimVisualMotionGroup {
  val exclusiveSelection: Boolean
  val selectionAdj: Int

  /**
   * This function toggles visual mode.
   *
   * * If visual mode is disabled, enable it
   * * If visual mode is enabled, but [selectionType] differs, update visual according to new [selectionType]
   * * If visual mode is enabled with the same [selectionType], disable it
   */
  fun toggleVisual(editor: VimEditor, count: Int, rawCount: Int, selectionType: SelectionType, returnTo: Mode? = null): Boolean

  /**
   * Enter Select mode with the given selection type
   *
   * When used from Normal, Insert or Replace modes, it will enter Select mode using the current mode as the "return to"
   * mode. I.e., if entered from Normal, will return to Normal. If entered from Insert or Replace (via shifted keys)
   * will return to Insert or Replace (aka "Insert Select" mode).
   *
   * While it will toggle between Visual and Select modes, it doesn't update the character positions correctly. IdeaVim
   * treats Select mode as exclusive and adjusts the character position when toggling modes.
   */
  fun enterSelectMode(editor: VimEditor, selectionType: SelectionType): Boolean

  /**
   * Enters visual mode based on current editor state.
   *
   * If [subMode] is null, subMode will be detected automatically
   *
   * it:
   * - Updates command state
   * - Updates [VimCaret.vimSelectionStart] property
   * - Updates caret colors
   * - Updates care shape
   *
   * - DOES NOT change selection
   * - DOES NOT move caret
   * - DOES NOT check if carets actually have any selection
   */
  fun enterVisualMode(editor: VimEditor, subMode: SelectionType? = null): Boolean
  fun autodetectVisualSubmode(editor: VimEditor): SelectionType
}
