package util

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project


fun getCurrentFilePath(project: Project): String? {
    // Ottieni il FileEditorManager del progetto
    val editorManager = FileEditorManager.getInstance(project)

    // Recupera l'editor selezionato (il file attualmente aperto)
    val currentEditor = editorManager.selectedEditor

    // Restituisci il percorso del file selezionato se esiste
    return currentEditor?.file?.path
}
