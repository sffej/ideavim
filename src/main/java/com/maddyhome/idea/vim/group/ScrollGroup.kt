/*
 * Copyright 2003-2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.VisualPosition
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimScrollGroup
import com.maddyhome.idea.vim.api.getVisualLineCount
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.normalizeLine
import com.maddyhome.idea.vim.api.normalizeVisualColumn
import com.maddyhome.idea.vim.api.normalizeVisualLine
import com.maddyhome.idea.vim.api.visualLineToBufferLine
import com.maddyhome.idea.vim.helper.EditorHelper.getNonNormalizedVisualLineAtBottomOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.getVisibleArea
import com.maddyhome.idea.vim.helper.EditorHelper.getVisualColumnAtLeftOfDisplay
import com.maddyhome.idea.vim.helper.EditorHelper.getVisualColumnAtRightOfDisplay
import com.maddyhome.idea.vim.helper.EditorHelper.getVisualLineAtBottomOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.getVisualLineAtTopOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.scrollColumnToLeftOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.scrollColumnToRightOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.scrollFullPageDown
import com.maddyhome.idea.vim.helper.EditorHelper.scrollFullPageUp
import com.maddyhome.idea.vim.helper.EditorHelper.scrollVisualLineToBottomOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.scrollVisualLineToCaretLocation
import com.maddyhome.idea.vim.helper.EditorHelper.scrollVisualLineToMiddleOfScreen
import com.maddyhome.idea.vim.helper.EditorHelper.scrollVisualLineToTopOfScreen
import com.maddyhome.idea.vim.helper.ScrollViewHelper.scrollCaretIntoView
import com.maddyhome.idea.vim.helper.getNormalizedScrollOffset
import com.maddyhome.idea.vim.helper.getNormalizedSideScrollOffset
import com.maddyhome.idea.vim.helper.localEditors
import com.maddyhome.idea.vim.helper.vimEditorGroup
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.LocalOptionChangeListener
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.options.OptionScope
import com.maddyhome.idea.vim.options.helpers.StrictMode.assert
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import kotlin.math.abs
import kotlin.math.max

class ScrollGroup : VimScrollGroup {
  override fun scrollCaretIntoView(editor: VimEditor) {
    scrollCaretIntoView(editor.ij)
  }

  override fun scrollFullPage(editor: VimEditor, caret: VimCaret, pages: Int): Boolean {
    assert(pages != 0, "pages != 0")
    return if (pages > 0) scrollFullPageDown(editor, caret, pages) else scrollFullPageUp(editor, caret, abs(pages))
  }

  private fun scrollFullPageDown(editor: VimEditor, caret: VimCaret, pages: Int): Boolean {
    val ijEditor = editor.ij
    val result = scrollFullPageDown(ijEditor, pages)
    val scrollOffset = getNormalizedScrollOffset(ijEditor)
    val topVisualLine = getVisualLineAtTopOfScreen(ijEditor)
    var caretVisualLine = result.second
    if (caretVisualLine < topVisualLine + scrollOffset) {
      caretVisualLine = editor.normalizeVisualLine(caretVisualLine + scrollOffset)
    }
    if (caretVisualLine != caret.ij.visualPosition.line) {
      val offset = injector.motion.moveCaretToLineWithStartOfLineOption(
        editor,
        editor.visualLineToBufferLine(caretVisualLine),
        caret
      )
      caret.moveToOffset(offset)
      return result.first
    }
    return false
  }

  private fun scrollFullPageUp(editor: VimEditor, caret: VimCaret, pages: Int): Boolean {
    val ijEditor = editor.ij
    val result = scrollFullPageUp(ijEditor, pages)
    val scrollOffset = getNormalizedScrollOffset(ijEditor)
    val bottomVisualLine = getVisualLineAtBottomOfScreen(ijEditor)
    var caretVisualLine = result.second
    if (caretVisualLine > bottomVisualLine - scrollOffset) {
      caretVisualLine = editor.normalizeVisualLine(caretVisualLine - scrollOffset)
    }
    if (caretVisualLine != caret.ij.visualPosition.line && caretVisualLine != -1) {
      val offset = injector.motion.moveCaretToLineWithStartOfLineOption(
        editor,
        editor.visualLineToBufferLine(caretVisualLine),
        caret
      )
      caret.moveToOffset(offset)
      return result.first
    }

    // We normally report an error if we didn't move the caret, but we have a special case for a page showing only the
    // last two lines of the file and virtual space. Vim normally scrolls window height minus two, but when the caret is
    // on last line minus one, this becomes window height minus one, meaning the top line of the current page becomes
    // the bottom line of the new page, and the caret doesn't move. Make sure we don't beep in this scenario.
    return caretVisualLine == editor.getVisualLineCount() - 2
  }

  override fun scrollHalfPage(editor: VimEditor, caret: VimCaret, rawCount: Int, down: Boolean): Boolean {
    val ijEditor = editor.ij
    val caretModel = ijEditor.caretModel
    val currentLogicalLine = caretModel.logicalPosition.line
    if (!down && currentLogicalLine <= 0 || down && currentLogicalLine >= editor.lineCount() - 1) {
      return false
    }
    val visibleArea = getVisibleArea(ijEditor)

    // We want to scroll the screen and keep the caret in the same screen-relative position. Calculate which line will
    // be at the current caret line and work the offsets out from that
    var targetCaretVisualLine = getScrollScreenTargetCaretVisualLine(ijEditor, rawCount, down)

    // Scroll at most one screen height
    val yInitialCaret = ijEditor.visualLineToY(caretModel.visualPosition.line)
    val yTargetVisualLine = ijEditor.visualLineToY(targetCaretVisualLine)
    if (abs(yTargetVisualLine - yInitialCaret) > visibleArea.height) {
      val yPrevious = visibleArea.y
      val moved: Boolean
      if (down) {
        targetCaretVisualLine = getVisualLineAtBottomOfScreen(ijEditor) + 1
        moved = scrollVisualLineToTopOfScreen(ijEditor, targetCaretVisualLine)
      } else {
        targetCaretVisualLine = getVisualLineAtTopOfScreen(ijEditor) - 1
        moved = scrollVisualLineToBottomOfScreen(ijEditor, targetCaretVisualLine)
      }
      if (moved) {
        // We'll keep the caret at the same position, although that might not be the same line offset as previously
        targetCaretVisualLine = ijEditor.yToVisualLine(yInitialCaret + getVisibleArea(ijEditor).y - yPrevious)
      }
    } else {
      scrollVisualLineToCaretLocation(ijEditor, targetCaretVisualLine)
      val scrollOffset = getNormalizedScrollOffset(ijEditor)
      val visualTop = getVisualLineAtTopOfScreen(ijEditor) + if (down) scrollOffset else 0
      val visualBottom = getVisualLineAtBottomOfScreen(ijEditor) - if (down) 0 else scrollOffset
      targetCaretVisualLine = targetCaretVisualLine.coerceIn(visualTop, visualBottom)
    }
    val logicalLine = editor.visualLineToBufferLine(targetCaretVisualLine)
    val caretOffset = injector.motion.moveCaretToLineWithStartOfLineOption(editor, logicalLine, caret)
    caret.moveToOffset(caretOffset)
    return true
  }

  override fun scrollLines(editor: VimEditor, lines: Int): Boolean {
    assert(lines != 0) { "lines cannot be 0" }
    val ijEditor = editor.ij
    if (lines > 0) {
      val visualLine = getVisualLineAtTopOfScreen(ijEditor)
      scrollVisualLineToTopOfScreen(ijEditor, visualLine + lines)
    } else {
      val visualLine = getNonNormalizedVisualLineAtBottomOfScreen(ijEditor)
      scrollVisualLineToBottomOfScreen(ijEditor, visualLine + lines)
    }
    MotionGroup.moveCaretToView(ijEditor)
    return true
  }

  override fun scrollCurrentLineToDisplayTop(editor: VimEditor, rawCount: Int, start: Boolean): Boolean {
    scrollLineToScreenLocation(editor, ScreenLocation.TOP, rawCount, start)
    return true
  }

  override fun scrollCurrentLineToDisplayMiddle(editor: VimEditor, rawCount: Int, start: Boolean): Boolean {
    scrollLineToScreenLocation(editor, ScreenLocation.MIDDLE, rawCount, start)
    return true
  }

  override fun scrollCurrentLineToDisplayBottom(editor: VimEditor, rawCount: Int, start: Boolean): Boolean {
    scrollLineToScreenLocation(editor, ScreenLocation.BOTTOM, rawCount, start)
    return true
  }

  // Scrolls current or [count] line to given screen location
  // In Vim, [count] refers to a file line, so it's a one-based logical line
  private fun scrollLineToScreenLocation(editor: VimEditor,
    screenLocation: ScreenLocation,
    rawCount: Int,
    start: Boolean
  ) {
    val scrollOffset = getNormalizedScrollOffset(editor.ij)
    val visualLine = if (rawCount == 0) {
      editor.primaryCaret().getVisualPosition().line
    } else {
      editor.bufferLineToVisualLine(editor.normalizeLine(rawCount - 1))
    }
    when (screenLocation) {
      ScreenLocation.TOP -> scrollVisualLineToTopOfScreen(editor.ij, visualLine - scrollOffset)
      ScreenLocation.MIDDLE -> scrollVisualLineToMiddleOfScreen(editor.ij, visualLine, true)
      ScreenLocation.BOTTOM -> {
        // Make sure we scroll to an actual line, not virtual space
        scrollVisualLineToBottomOfScreen(
          editor.ij,
          editor.normalizeVisualLine(visualLine + scrollOffset)
        )
      }
    }
    if (visualLine != editor.primaryCaret().getVisualPosition().line || start) {
      val offset = injector.motion.run {
        if (start) {
          moveCaretToLineStartSkipLeading(editor, editor.visualLineToBufferLine(visualLine))
        } else {
          moveCaretToLineWithSameColumn(editor, editor.visualLineToBufferLine(visualLine), editor.primaryCaret())
        }
      }
      editor.primaryCaret().moveToOffset(offset)
    }
  }

  override fun scrollColumns(editor: VimEditor, columns: Int): Boolean {
    val ijEditor = editor.ij
    val caretVisualPosition = ijEditor.caretModel.visualPosition
    if (columns > 0) {
      // TODO: Don't add columns to visual position. This includes inlays and folds
      var visualColumn = editor.normalizeVisualColumn(
        caretVisualPosition.line,
        getVisualColumnAtLeftOfDisplay(ijEditor, caretVisualPosition.line) +
          columns,
        false
      )

      // If the target column has an inlay preceding it, move passed it. This inlay will have been (incorrectly)
      // included in the simple visual position, so it's ok to step over. If we don't do this, scrollColumnToLeftOfScreen
      // can get stuck trying to make sure the inlay is visible.
      // A better solution is to not use VisualPosition everywhere, especially for arithmetic
      val inlay =
        ijEditor.inlayModel.getInlineElementAt(VisualPosition(caretVisualPosition.line, visualColumn - 1))
      if (inlay != null && !inlay.isRelatedToPrecedingText) {
        visualColumn++
      }
      scrollColumnToLeftOfScreen(ijEditor, caretVisualPosition.line, visualColumn)
    } else {
      // Don't normalise the rightmost column, or we break virtual space
      val visualColumn =
        getVisualColumnAtRightOfDisplay(ijEditor, caretVisualPosition.line) + columns
      scrollColumnToRightOfScreen(ijEditor, caretVisualPosition.line, visualColumn)
    }
    MotionGroup.moveCaretToView(ijEditor)
    return true
  }

  override fun scrollCaretColumnToDisplayLeftEdge(vimEditor: VimEditor): Boolean {
    val editor = vimEditor.ij
    val caretVisualPosition = editor.caretModel.visualPosition
    val scrollOffset = getNormalizedSideScrollOffset(editor)
    // TODO: Should the offset be applied to visual columns? This includes inline inlays and folds
    val column = max(0, caretVisualPosition.column - scrollOffset)
    scrollColumnToLeftOfScreen(editor, caretVisualPosition.line, column)
    return true
  }

  override fun scrollCaretColumnToDisplayRightEdge(editor: VimEditor): Boolean {
    val ijEditor = editor.ij
    val caretVisualPosition = ijEditor.caretModel.visualPosition
    val scrollOffset = getNormalizedSideScrollOffset(ijEditor)
    // TODO: Should the offset be applied to visual columns? This includes inline inlays and folds
    val column =
      editor.normalizeVisualColumn(caretVisualPosition.line, caretVisualPosition.column + scrollOffset, false)
    scrollColumnToRightOfScreen(ijEditor, caretVisualPosition.line, column)
    return true
  }

  private enum class ScreenLocation {
    TOP, MIDDLE, BOTTOM
  }

  object ScrollOptionsChangeListener : LocalOptionChangeListener<VimDataType> {
    override fun processGlobalValueChange(oldValue: VimDataType?) {
      for (editor in localEditors()) {
        if (editor.vimEditorGroup) {
          scrollCaretIntoView(editor)
        }
      }
    }

    override fun processLocalValueChange(oldValue: VimDataType?, editor: VimEditor) {
      editor.ij.apply {
        if (vimEditorGroup) {
          scrollCaretIntoView(this)
        }
      }
    }
  }

  companion object {
    // Get the visual line that will be in the same screen relative location as the current caret line, after the screen
    // has been scrolled
    private fun getScrollScreenTargetCaretVisualLine(editor: Editor, rawCount: Int, down: Boolean): Int {
      val visibleArea = getVisibleArea(editor)
      val caretVisualLine = editor.caretModel.visualPosition.line
      val scrollOption = getScrollOption(rawCount)
      val targetCaretVisualLine = if (scrollOption == 0) {
        // Scroll up/down half window size by default. We can't use line count here because of block inlays
        val offset = if (down) visibleArea.height / 2 else editor.lineHeight - visibleArea.height / 2
        editor.yToVisualLine(editor.visualLineToY(caretVisualLine) + offset)
      } else {
        if (down) caretVisualLine + scrollOption else caretVisualLine - scrollOption
      }
      return editor.vim.normalizeVisualLine(targetCaretVisualLine)
    }

    private fun getScrollOption(rawCount: Int): Int {
      if (rawCount == 0) {
        return (VimPlugin.getOptionService().getOptionValue(
          OptionScope.GLOBAL,
          OptionConstants.scrollName,
          OptionConstants.scrollName
        ) as VimInt).value
      }
      // TODO: This needs to be reset whenever the window size changes
      VimPlugin.getOptionService().setOptionValue(
        OptionScope.GLOBAL,
        OptionConstants.scrollName,
        VimInt(rawCount),
        OptionConstants.scrollName
      )
      return rawCount
    }
  }
}
