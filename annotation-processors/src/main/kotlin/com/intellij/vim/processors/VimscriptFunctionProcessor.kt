/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.processors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.intellij.vim.FileWriter
import com.intellij.vim.annotations.VimscriptFunction

class VimscriptFunctionProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
  private val visitor = VimscriptFunctionVisitor()
  private val writer = FileWriter()
  private val nameToFunction = mutableMapOf<String, KSClassDeclaration>()


  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver.getAllFiles().forEach { it.accept(visitor, Unit) }
    writer.generateResourceFile("VimscriptFunctions.yaml", generateFunctionDict(), environment)
    return emptyList()
  }

  private fun generateFunctionDict(): String {
    val mapper = YAMLMapper()
    val dictToWrite: Map<String, String> = nameToFunction
      .map { it.key to it.value.qualifiedName!!.asString() }
      .toMap()
    return mapper.writeValueAsString(dictToWrite)
  }

  // todo inspection that annotation is properly used on proper classes
  private inner class VimscriptFunctionVisitor : KSVisitorVoid() {
    @OptIn(KspExperimental::class)
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      val vimscriptFunctionAnnotation = classDeclaration.getAnnotationsByType(VimscriptFunction::class).firstOrNull() ?: return
      val functionName = vimscriptFunctionAnnotation.name
      nameToFunction[functionName] = classDeclaration
    }

    override fun visitFile(file: KSFile, data: Unit) {
      file.declarations.forEach { it.accept(this, Unit) }
    }
  }
}
