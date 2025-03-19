package listeners

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import javax.swing.JButton
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import ui.ToolWindowMenu

class RealTimeDetectionListener(
    private val project: Project,
    private val resultsArea: JTextArea,
    private val realTimeButton: JButton,
    private val toolWindowMenu: ToolWindowMenu  // Passiamo il riferimento al ToolWindowMenu
) {

    internal var isRealTimeActive = false

    fun startRealTimeDetection() {
        if (isRealTimeActive) return // Se la modalità è già attiva, non fare nulla

        // Cambia il colore del pulsante per indicare che la modalità è attiva
        realTimeButton.text = "Stop real time detection"
        realTimeButton.background = JBColor.RED

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
        realTimeButton.text = "Attiva modalità di detection real-time"
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
        // Logica per monitorare le modifiche ai file in tempo reale
        SwingUtilities.invokeLater {
            resultsArea.append("Real-time detection started...\n")
        }

        // Logica per il monitoraggio in tempo reale, ad esempio, chiamando una funzione di analisi
        val projectPath = project.basePath ?: return
        sendRealTimeAnalysisRequest(projectPath)
    }

    private fun stopMonitoringRealTimeChanges() {
        // Logica per fermare il monitoraggio delle modifiche
        SwingUtilities.invokeLater {
            resultsArea.append("Real-time detection stopped.\n")
        }
    }

    private fun sendRealTimeAnalysisRequest(projectPath: String) {
        // Qui possiamo inviare la richiesta di analisi in tempo reale (simile alla funzione sendAnalysisRequest)
        // Ad esempio, chiamando un'API o un altro metodo che esegue l'analisi
        println("Real-time analysis request for project at path: $projectPath")
        // Aggiungere qui la logica per inviare la richiesta al server per analizzare i file in tempo reale
    }
}