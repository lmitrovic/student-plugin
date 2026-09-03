package com.github.lmitrovic.studenttestingintellijplugin.tracking

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.project.Project

/**
 * Čitanje grešaka (ERROR highlight-a) iz otvorenog dokumenta preko markup modela.
 * Zamena za `DaemonCodeAnalyzerImpl.getHighlights(...)` koji je interni API i
 * lako se lomi između verzija platforme.
 */
internal object EditorErrors {

    fun errorHighlights(project: Project, document: Document): List<HighlightInfo> {
        val model = DocumentMarkupModel.forDocument(document, project, false)
        return model.allHighlighters
            .mapNotNull { it.errorStripeTooltip as? HighlightInfo }
            .filter { it.severity === HighlightSeverity.ERROR }
    }
}
