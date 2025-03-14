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
import java.util.concurrent.TimeUnit
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

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
            resultsArea.append("Analisi sul progetto:\n")
            sendAnalysisRequest(project.basePath, resultsArea, false)  // Chiamata alla funzione di util
        }

        val currentFileButton = JButton("Analizza file corrente")
        currentFileButton.addActionListener {

            val currentFile = getCurrentFilePath(project)
            if(currentFile != null) {
                println("Percorso file corrente $currentFile")
                resultsArea.append("Analisi sul file corrente:\n")
                sendAnalysisRequest(currentFile, resultsArea, true)

            }else{
                resultsArea.append("No file corrente yet")
            }

        }

        val multipleFilesButton = JButton("Analizza più file")
        multipleFilesButton.addActionListener {
            // Ottieni la cartella di base del progetto
            val projectBasePath = project.basePath ?: return@addActionListener

            // Crea un JFileChooser per permettere la selezione di file
            val fileChooser = JFileChooser(projectBasePath)
            fileChooser.dialogTitle = "Seleziona file Python"

            // Imposta il filtro per mostrare solo file .py
            fileChooser.fileFilter = FileNameExtensionFilter("File Python", "py")

            // Imposta la modalità di selezione su FILES_ONLY per permettere solo la selezione dei file
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY)

            // Abilita la selezione multipla
            fileChooser.isMultiSelectionEnabled = true

            // Apri la finestra di selezione file
            val returnValue = fileChooser.showOpenDialog(null)

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                // Ottieni i file selezionati
                val selectedFiles = fileChooser.selectedFiles
                val filesList = selectedFiles.map { it.absolutePath }.toList()

                // Mostra un messaggio di conferma con i file selezionati
                val message = "Sei sicuro di voler analizzare i seguenti file?\n" + filesList.joinToString("\n")
                val response = JOptionPane.showConfirmDialog(null, message, "Conferma Analisi", JOptionPane.YES_NO_OPTION)

                if (response == JOptionPane.YES_OPTION) {
                    // Crea una cartella temporanea per i file
                    val tempDir = File(project.basePath, "temp_files")
                    if (!tempDir.exists()) {
                        tempDir.mkdir()
                    }

                    // Copia i file selezionati nella cartella temporanea
                    filesList.forEach { filePath ->
                        val file = File(filePath)
                        val destination = File(tempDir, file.name)
                        file.copyTo(destination, overwrite = true)
                    }

                    // Formatta il percorso della directory temporanea
                    val formattedTempDirPath = tempDir.absolutePath.replace(File.separator, "\\\\")
                    val formattedOutputDirPath = File(tempDir, "OUTPUT").absolutePath.replace(File.separator, "\\\\")

                    // Invia la richiesta di analisi al server con il percorso della cartella temporanea
                    println("Sending analysis request: {")
                    println("  \"input_directory\": \"$formattedTempDirPath\",")
                    println("  \"output_directory\": \"$formattedOutputDirPath\"")
                    println("}")

                    // Chiama la funzione sendAnalysisRequest per analizzare i file selezionati (singleFileMode = false)
                    resultsArea.append("Analisi su più file:\n")
                    sendAnalysisRequest(formattedTempDirPath, resultsArea, false)

                    // Usa un ScheduledExecutorService per ritardare l'eliminazione della cartella temporanea
                    val scheduler = Executors.newSingleThreadScheduledExecutor()
                    scheduler.schedule({
                        // Dopo il ritardo, elimina la cartella temporanea
                        if (tempDir.exists()) {
                            tempDir.deleteRecursively()
                            println("Cartella temporanea 'temp_files' eliminata con successo.")
                        }
                    }, 5, TimeUnit.SECONDS) // Ritardo di 5 secondi
                }
            }
        }

        // Aggiungi il pulsante e l'area di risultati al pannello
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
}