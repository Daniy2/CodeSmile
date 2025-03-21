package csplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import csplugin.util.getCurrentFilePath
import csplugin.util.sendAnalysisRequest
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import csplugin.listeners.RealTimeDetectionListener

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

    private val allButtons = mutableListOf<JButton>()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Avvia il server Flask automaticamente all'inizio
        killPreviousProcesses()
        startFlaskServer() // Avvia il server al caricamento del plugin

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        // Creazione dell'area di testo per mostrare i risultati
        val resultsArea = JTextArea(10, 40)
        resultsArea.isEditable = false

        val scrollPane = JBScrollPane(resultsArea)

        // Creazione del pulsante per attivare/disattivare la modalità di detection real-time
        val realTimeButton = JButton("Attiva modalità di detection real-time")
        val realTimeDetectionListener = RealTimeDetectionListener(project, resultsArea, realTimeButton, this) // Passiamo il riferimento al ToolWindowMenu

        realTimeButton.addActionListener {
            if (realTimeDetectionListener.isRealTimeActive) {
                // Se la modalità è attiva, ferma il monitoraggio
                realTimeDetectionListener.stopRealTimeDetection()
            } else {
                // Se la modalità non è attiva, avvia il monitoraggio
                realTimeDetectionListener.startRealTimeDetection()
            }
        }

        // Aggiungi il pulsante al gruppo di pulsanti


        // Creazione dei pulsanti per le altre funzionalità
        val analyzeButton = JButton("Analizza progetto")
        analyzeButton.addActionListener {
            // Chiamata alla funzione per inviare la richiesta di analisi
            resultsArea.append("Analisi sul progetto:\n")
            sendAnalysisRequest(project.basePath, resultsArea, false)  // Chiamata alla funzione di util
        }

        val currentFileButton = JButton("Analizza file corrente")
        currentFileButton.addActionListener {
            // Funzione per analizzare il file corrente
            val currentFile = getCurrentFilePath(project)
            if (currentFile != null) {
                resultsArea.append("Analisi sul file corrente:\n")
                sendAnalysisRequest(currentFile, resultsArea, true)
            } else {
                resultsArea.append("No file corrente yet\n")
            }
        }

        val multipleFilesButton = JButton("Analizza più file")
        multipleFilesButton.addActionListener {
            // Funzione per analizzare più file
            val projectBasePath = project.basePath ?: return@addActionListener
            val fileChooser = JFileChooser(projectBasePath)
            fileChooser.dialogTitle = "Seleziona file Python"
            fileChooser.fileFilter = FileNameExtensionFilter("File Python", "py")
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY)
            fileChooser.isMultiSelectionEnabled = true
            val returnValue = fileChooser.showOpenDialog(null)

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                val selectedFiles = fileChooser.selectedFiles
                val filesList = selectedFiles.map { it.absolutePath }.toList()

                val message = "Sei sicuro di voler analizzare i seguenti file?\n" + filesList.joinToString("\n")
                val response = JOptionPane.showConfirmDialog(null, message, "Conferma Analisi", JOptionPane.YES_NO_OPTION)

                if (response == JOptionPane.YES_OPTION) {
                    val tempDir = File(project.basePath, "temp_files")
                    if (!tempDir.exists()) {
                        tempDir.mkdir()
                    }
                    filesList.forEach { filePath ->
                        val file = File(filePath)
                        val destination = File(tempDir, file.name)
                        file.copyTo(destination, overwrite = true)
                    }
                    val formattedTempDirPath = tempDir.absolutePath.replace(File.separator, "\\\\")
                    val formattedOutputDirPath = File(tempDir, "OUTPUT").absolutePath.replace(File.separator, "\\\\")
                    sendAnalysisRequest(formattedTempDirPath, resultsArea, false)
                    val scheduler = Executors.newSingleThreadScheduledExecutor()
                    scheduler.schedule({
                        if (tempDir.exists()) {
                            tempDir.deleteRecursively()
                        }
                    }, 5, TimeUnit.SECONDS)
                }
            }
        }

        // Aggiungi i pulsanti al pannello
        allButtons.add(analyzeButton)
        allButtons.add(currentFileButton)
        allButtons.add(multipleFilesButton)

        panel.add(realTimeButton)
        panel.add(analyzeButton)
        panel.add(currentFileButton)
        panel.add(multipleFilesButton)
        panel.add(scrollPane)

        // Aggiungi il pannello al ToolWindow
        toolWindow.contentManager.removeAllContents(true)
        toolWindow.contentManager.addContent(
            ContentFactory.getInstance().createContent(panel, "", false)
        )
    }

    fun disableOtherButtons() {
        allButtons.forEach { it.isEnabled = false }
    }

    fun enableOtherButtons() {
        allButtons.forEach { it.isEnabled = true }
    }
}
