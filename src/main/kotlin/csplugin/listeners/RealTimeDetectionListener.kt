package csplugin.listeners

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import csplugin.ui.ToolWindowMenu
import java.io.File
import java.nio.file.*
import javax.swing.JButton
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import csplugin.util.sendAnalysisRequest

class RealTimeDetectionListener(
    private val project: Project,
    private val resultsArea: JTextArea,
    private val realTimeButton: JButton,
    private val toolWindowMenu: ToolWindowMenu  // Passiamo il riferimento al ToolWindowMenu
) {

    internal var isRealTimeActive = false
    private lateinit var watchService: WatchService
    private lateinit var watchThread: Thread
    private var lastModifiedTime: Long = 0  // Variabile per tenere traccia dell'ultimo evento di modifica

    fun startRealTimeDetection() {
        if (isRealTimeActive) return // Se la modalità è già attiva, non fare nulla

        // Cambia il colore del pulsante per indicare che la modalità è attiva
        realTimeButton.text = "Stop real time detection"
        realTimeButton.background = JBColor.RED
        realTimeButton.foreground = JBColor.WHITE  // Imposta il colore del testo

        // Modifica il bordo del pulsante per colorarlo completamente
        realTimeButton.border = javax.swing.BorderFactory.createLineBorder(JBColor.RED, 2)

        // Disabilita gli altri pulsanti dal ToolWindowMenu.kt
        toolWindowMenu.disableOtherButtons()

        // Avvia la modalità di detection real-time
        monitorRealTimeChanges()

        isRealTimeActive = true
    }

    fun stopRealTimeDetection() {
        if (!isRealTimeActive) return // Se la modalità non è attiva, non fare nulla

        // Ripristina il pulsante al colore originale
        realTimeButton.text = "Start real time detection"
        realTimeButton.background = JBColor.GREEN
        realTimeButton.foreground = JBColor.BLACK  // Imposta il colore del testo

        // Modifica il bordo del pulsante per colorarlo completamente
        realTimeButton.border = javax.swing.BorderFactory.createLineBorder(JBColor.GREEN, 2)

        // Riabilita gli altri pulsanti dal ToolWindowMenu.kt
        toolWindowMenu.enableOtherButtons()

        // Ferma il monitoraggio in real-time
        stopMonitoringRealTimeChanges()

        isRealTimeActive = false
    }

    private fun monitorRealTimeChanges() {
        // Avvia un nuovo thread per monitorare i file in tempo reale
        watchThread = Thread {
            try {
                // Inizializza il WatchService per monitorare i file
                watchService = FileSystems.getDefault().newWatchService()
                val projectDir = File(project.basePath ?: return@Thread)

                // Aggiunge la directory del progetto al WatchService
                projectDir.toPath().register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE
                )

                // Inizia a monitorare
                while (isRealTimeActive) {
                    val key = watchService.take() // Blocca finché non viene rilevato un evento
                    for (event in key.pollEvents()) {
                        val kind = event.kind()
                        val filePath = event.context() as Path
                        val fullPath = projectDir.toPath().resolve(filePath)

                        // Controlla se il file modificato è un file .py
                        if (fullPath.toString().endsWith(".py") && kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            // Aggiungi un controllo di debouncing per evitare eventi ripetuti in tempi brevi
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastModifiedTime > 300) { // Ridotto da 500 ms a 300 ms
                                println("File modificato: $fullPath")
                                sendRealTimeAnalysisRequest(fullPath.toString())
                                lastModifiedTime = currentTime
                            }
                        }
                    }
                    key.reset() // Resetta la chiave per continuare a monitorare
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        watchThread.start() // Avvia il thread per monitorare i file
    }

    private fun stopMonitoringRealTimeChanges() {
        // Interrompe il monitoraggio dei file
        watchThread.interrupt()
        println("Real-time detection stopped.")
    }

    private fun sendRealTimeAnalysisRequest(filePath: String) {
        // Aggiungi un messaggio di avvio nell'area dei risultati
        SwingUtilities.invokeLater {
            println("Aggiornamento dell'interfaccia utente...")
            resultsArea.text = ""
            resultsArea.append("Running real time analysis...")
        }

        // Invia la richiesta di analisi
        try {
            println("Invio della richiesta di analisi...")
            sendAnalysisRequest(project.basePath, resultsArea, true)
            println("Richiesta di analisi inviata.")
        } catch (e: Exception) {
            println("Errore durante l'invio della richiesta: ${e.message}")
            SwingUtilities.invokeLater {
                resultsArea.append("Errore durante l'analisi: ${e.message}\n")
            }
        }
    }
}