package csplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import csplugin.listeners.RealTimeDetectionListener
import csplugin.util.getCurrentFilePath
import csplugin.util.sendAnalysisRequest
import java.awt.*
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

private fun getPythonInterpreterPathFromVenv(projectRootPath: String): String {
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
        val processBuilder = ProcessBuilder("taskkill", "/F", "/IM", "python.exe")
        processBuilder.start().waitFor()
        println("Previous Python processes terminated.")
    } catch (e: Exception) {
        println("Error killing previous processes: ${e.message}")
    }
}

fun startFlaskServer() {
    try {
        val projectRootPath = System.getenv("PROJECT_ROOT") ?: File(".").absolutePath
        val serverScriptPath = Paths.get(projectRootPath, "server", "server.py").toString()
        if (!File(serverScriptPath).exists()) {
            throw IOException("Server script non trovato: $serverScriptPath")
        }

        val pythonInterpreterPath = getPythonInterpreterPathFromVenv(projectRootPath)
        val command = listOf(pythonInterpreterPath, "-u", serverScriptPath)

        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(true)
        processBuilder.directory(File(projectRootPath))

        val process = processBuilder.start()
        println("Server avviato...")

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

// Creazione dei pulsanti con stile
fun createStyledButton(text: String, color: JBColor): JButton {
    val button = JButton(text)
    button.background = color
    button.foreground = JBColor(Gray._240, Gray._240) // Testo leggermente meno brillante per migliorare il contrasto
    button.font = Font("Arial", Font.BOLD, 12)
    button.border = BorderFactory.createEmptyBorder(10, 20, 10, 20)

    // Imposta dimensioni fisse
    button.preferredSize = Dimension(250, 40)
    button.minimumSize = Dimension(250, 40)
    button.maximumSize = Dimension(250, 40)

    button.isFocusPainted = false
    button.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    return button
}

class ToolWindowMenu : ToolWindowFactory {

    private val allButtons = mutableListOf<JButton>()
    private val realTimeButton = createStyledButton(
        "Attiva modalità di detection real-time",
        JBColor(Color(76, 175, 80), Color(76, 175, 80)) // Verde per entrambi i temi
    )

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        killPreviousProcesses()
        startFlaskServer()

        // Pannello principale
        val panel = JPanel(BorderLayout())
        panel.background = JBColor(Color(46, 46, 46), Color(30, 30, 30)) // Sfondo grigio scuro compatibile con temi chiaro/scuro

        // Creazione dell'area di testo per mostrare i risultati
        val resultsArea = JTextArea(10, 40)
        resultsArea.isEditable = false
        resultsArea.font = Font("Consolas", Font.PLAIN, 13) // Font monospaced
        resultsArea.foreground = JBColor(Color(240, 240, 240), Color(240, 240, 240)) // Testo leggermente meno brillante
        resultsArea.background = JBColor(Color(46, 46, 46), Color(30, 30, 30)) // Sfondo scuro compatibile con temi chiaro/scuro

        val scrollPane = JBScrollPane(resultsArea)
        scrollPane.border = BorderFactory.createEmptyBorder(10, 10, 10, 10) // Padding interno
        scrollPane.background = JBColor(Color(46, 46, 46), Color(30, 30, 30)) // Sfondo scuro compatibile con temi chiaro/scuro

        // Pannello superiore per i pulsanti
        val buttonPanel = JPanel()
        buttonPanel.layout = FlowLayout(FlowLayout.LEFT, 10, 5) // Allineamento a sinistra, spaziatura di 10px tra i pulsanti
        buttonPanel.background = JBColor(Color(46, 46, 46), Color(30, 30, 30)) // Sfondo grigio scuro compatibile con temi chiaro/scuro

        val analyzeButton = createStyledButton(
            "Analizza progetto",
            JBColor(Color(33, 150, 243), Color(33, 150, 243)) // Blu per entrambi i temi
        )
        val currentFileButton = createStyledButton(
            "Analizza file corrente",
            JBColor(Color(255, 152, 0), Color(255, 152, 0)) // Arancione per entrambi i temi
        )
        val multipleFilesButton = createStyledButton(
            "Analizza più file",
            JBColor(Color(156, 39, 176), Color(156, 39, 176)) // Viola per entrambi i temi
        )

        // Aggiungi azioni ai pulsanti
        analyzeButton.addActionListener {
            resultsArea.text = ""
            resultsArea.append("Analisi sul progetto:\n")
            sendAnalysisRequest(project.basePath, resultsArea, false)
        }

        currentFileButton.addActionListener {
            resultsArea.text = ""
            val currentFile = getCurrentFilePath(project)
            if (currentFile != null) {
                resultsArea.append("Analisi sul file corrente:\n")
                sendAnalysisRequest(currentFile, resultsArea, true)
            } else {
                resultsArea.append("No file corrente yet\n")
            }
        }

        multipleFilesButton.addActionListener {
            val projectBasePath = project.basePath ?: return@addActionListener
            val fileChooser = JFileChooser(projectBasePath)
            fileChooser.dialogTitle = "Seleziona file Python"
            fileChooser.fileFilter = FileNameExtensionFilter("File Python", "py")
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY)
            fileChooser.isMultiSelectionEnabled = true
            val returnValue = fileChooser.showOpenDialog(null)

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                val selectedFiles = fileChooser.selectedFiles.map { it.absolutePath }.toList()
                val message = "Sei sicuro di voler analizzare i seguenti file?\n" + selectedFiles.joinToString("\n")
                val response = JOptionPane.showConfirmDialog(null, message, "Conferma Analisi", JOptionPane.YES_NO_OPTION)

                if (response == JOptionPane.YES_OPTION) {
                    val tempDir = File(project.basePath, "temp_files")
                    if (!tempDir.exists()) {
                        tempDir.mkdir()
                    }
                    selectedFiles.forEach { filePath ->
                        val file = File(filePath)
                        val destination = File(tempDir, file.name)
                        file.copyTo(destination, overwrite = true)
                    }
                    val formattedTempDirPath = tempDir.absolutePath.replace(File.separator, "\\\\")
                    val formattedOutputDirPath = File(tempDir, "OUTPUT").absolutePath.replace(File.separator, "\\\\")


                    resultsArea.text = ""
                    resultsArea.append("Analisi sui file selezionati:\n")
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

        val realTimeDetectionListener = RealTimeDetectionListener(project, resultsArea, realTimeButton, this)
        realTimeButton.addActionListener {

            if (realTimeDetectionListener.isRealTimeActive) {
                realTimeDetectionListener.stopRealTimeDetection()
            } else {
                resultsArea.text=""
                realTimeDetectionListener.startRealTimeDetection()
            }
        }

        // Aggiungi i pulsanti al pannello
        allButtons.addAll(listOf(analyzeButton, currentFileButton, multipleFilesButton, realTimeButton))
        buttonPanel.add(analyzeButton)
        buttonPanel.add(currentFileButton)
        buttonPanel.add(multipleFilesButton)
        buttonPanel.add(realTimeButton)

        // Aggiungi i componenti al pannello principale
        panel.add(buttonPanel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)

        // Aggiungi il pannello al ToolWindow
        toolWindow.contentManager.removeAllContents(true)
        toolWindow.contentManager.addContent(
            ContentFactory.getInstance().createContent(panel, "", false)
        )
    }

    fun disableOtherButtons() {
        allButtons.filter { it != realTimeButton }.forEach { it.isEnabled = false }
    }

    fun enableOtherButtons() {
        allButtons.forEach { it.isEnabled = true }
    }
}