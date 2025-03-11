package ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import util.getCurrentFilePath
import util.sendAnalysisRequest
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.Executors
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea

private fun getPythonInterpreterPathFromVenv(projectRootPath: String): String {
    // Cerca l'interprete Python nell'ambiente virtuale
    val venvPath = Paths.get(projectRootPath, ".venv").toString()
    val interpreterPath = when {
        File("$venvPath/Scripts/python.exe").exists() -> "$venvPath/Scripts/python.exe" // Windows
        File("$venvPath/bin/python").exists() -> "$venvPath/bin/python" // Linux/Mac
        else -> throw IOException("Ambiente virtuale non trovato o interprete Python non disponibile.")
    }
    return interpreterPath
}

fun killPreviousProcesses() {
    try {
        // Esegui il comando taskkill per terminare il processo python che usa la porta 5000
        val processBuilder = ProcessBuilder("taskkill", "/F", "/IM", "python.exe")
        processBuilder.start().waitFor()  // Aspetta che il processo venga terminato
        println("Previous Python processes terminated.")
    } catch (e: Exception) {
        println("Error killing previous processes: ${e.message}")
    }
}


fun startFlaskServer() {
    try {
        val projectRootPath = System.getenv("PROJECT_ROOT") ?: File(".").absolutePath
        println("Percorso root: $projectRootPath")

        val serverScriptPath = Paths.get(projectRootPath, "server", "server.py").toString()
        if (!File(serverScriptPath).exists()) {
            throw IOException("Server script non trovato: $serverScriptPath")
        }

        val pythonInterpreterPath = getPythonInterpreterPathFromVenv(projectRootPath)
        println("Python interpreter: $pythonInterpreterPath")

        // Aggiungi "-u" per il flushing immediato
        val command = listOf(pythonInterpreterPath, "-u", serverScriptPath)
        println("Comando: ${command.joinToString(" ")}")

        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(true)
        processBuilder.directory(File(projectRootPath))

        val process = processBuilder.start()
        println("Server avviato...")

        // Stampa log con prefisso
        Executors.newSingleThreadExecutor().submit {
            process.inputStream.bufferedReader().forEachLine { line ->
                println("[SERVER] $line")
            }
        }

    } catch (e: IOException) {
        println("ERRORE: ${e.message}")
        e.printStackTrace()
    }
}



class ToolWindowMenu : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Avvia il server Flask automaticamente all'inizio
        killPreviousProcesses()
        startFlaskServer() // Avvia il server al caricamento del plugin

        // Creazione del pannello principale
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        // Creazione dell'area di testo per mostrare i risultati
        val resultsArea = JTextArea(10, 40)
        resultsArea.isEditable = false
        val scrollPane = JBScrollPane(resultsArea)

        // Creazione del pulsante per avviare l'analisi
        val analyzeButton = JButton("Analizza progetto")
        analyzeButton.addActionListener {
            // Chiamata alla funzione per inviare la richiesta di analisi
            sendAnalysisRequest(project.basePath, resultsArea, false)  // Chiamata alla funzione di util
        }

        val currentFileButton = JButton("Analizza file corrente")
        currentFileButton.addActionListener {

            val currentFile = getCurrentFilePath(project)
            if(currentFile != null) {
                println("Percorso file corrente $currentFile")
                sendAnalysisRequest(currentFile, resultsArea, true)

            }else{
                resultsArea.append("No file corrente yet")
            }

        }

        // Aggiungi il pulsante e l'area di risultati al pannello
        panel.add(analyzeButton)
        panel.add(currentFileButton)
        panel.add(scrollPane)

        // Aggiungi il pannello al ToolWindow
        toolWindow.contentManager.removeAllContents(true)
        toolWindow.contentManager.addContent(
            ContentFactory.getInstance().createContent(panel, "", false)
        )
    }
}