package ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
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




private fun startFlaskServer() {
    try {
        // Ottieni il percorso della root del progetto principale
        val projectRootPath = System.getenv("PROJECT_ROOT") ?: File(".").absolutePath
        println("Percorso della root del progetto principale: $projectRootPath")

        // Verifica che il percorso sia valido
        if (!File(projectRootPath).exists()) {
            throw IOException("Il percorso del progetto principale non esiste: $projectRootPath")
        }

        // Combina il percorso del progetto con la cartella 'server' e 'server.py'
        val serverScriptPath = Paths.get(projectRootPath, "server", "server.py").toString()
        println("Percorso del server: $serverScriptPath")

        // Ottieni il percorso dell'interprete Python dall'ambiente virtuale
        val pythonInterpreterPath = getPythonInterpreterPathFromVenv(projectRootPath)
        println("Percorso dell'interprete Python: $pythonInterpreterPath")

        // Costruisci il comando per eseguire Flask usando l'interprete specifico
        val command = listOf(pythonInterpreterPath, serverScriptPath)

        println("Command: ${command.joinToString(" ")}")

        // Avvia il processo
        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(true)  // Unifica gli stream di errore e uscita

        // Imposta la working directory del processo sulla root del progetto principale
        processBuilder.directory(File(projectRootPath))

        // Avvia il processo
        val process = processBuilder.start()
        println("Server Flask avviato...")

        // Stampa l'output del server per debugging
        Executors.newSingleThreadExecutor().submit {
            val reader = process.inputStream.bufferedReader()
            reader.forEachLine { println(it) }
        }

    } catch (e: IOException) {
        e.printStackTrace()
        println("Errore nell'avvio del server Flask.")
    }
}


class ToolWindowMenu : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Avvia il server Flask automaticamente all'inizio
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
            sendAnalysisRequest(project.basePath, resultsArea)  // Chiamata alla funzione di util
        }

        // Aggiungi il pulsante e l'area di risultati al pannello
        panel.add(analyzeButton)
        panel.add(scrollPane)

        // Aggiungi il pannello al ToolWindow
        toolWindow.contentManager.removeAllContents(true)
        toolWindow.contentManager.addContent(
            ContentFactory.getInstance().createContent(panel, "", false)
        )
    }
}