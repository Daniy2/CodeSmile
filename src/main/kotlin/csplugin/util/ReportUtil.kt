package csplugin.util

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import java.io.File

fun formatCsvToReadableText(csvPath: String?, projectPath: String?, singleFileMode: Boolean): String {
    if (csvPath.isNullOrEmpty() || projectPath.isNullOrEmpty()) {
        cleanupOutputDirectory(File(projectPath?.let { File(it).parentFile }, "OUTPUT"))
        return "Nessun code smell \n"
    }

    val csvFile = File(csvPath)
    if (!csvFile.exists()) {
        return "File CSV non trovato: $csvPath"
    }

    val formattedText = StringBuilder()
    val projectRoot = File(projectPath).absolutePath

    try {
        // Usa kotlin-csv per leggere il file
        val rows = csvReader().readAll(csvFile)

        // Salta la riga di intestazione
        for (row in rows.drop(1)) {
            val rawFilename = row[0]
            println("Raw filename from CSV: $rawFilename") // Debug: Verifica il valore grezzo

            val normalizedFilename = rawFilename.replace("\\", "/")
            println("Normalized filename: $normalizedFilename") // Debug: Verifica il valore normalizzato

            val functionName = row[1]
            val smellName = row[2]
            val lineNum = row[3]
            val description = row[4]

            // Calcola il percorso relativo
            val relativePath = normalizedFilename.replace(projectRoot, "")
                .replace(""""\\"""", "/")
                .removePrefix("/") // Rimuove slash iniziale

            formattedText.append(
                "Rilevato: $smellName\n" +
                        "File: $relativePath\n" +
                        "Funzione: $functionName\n" +
                        "Linea: $lineNum\n" +
                        "Descrizione: $description\n\n"
            )
        }

        // Pulizia directory OUTPUT
        val outputFolder = if (!singleFileMode) {
            File(projectPath, "OUTPUT")
        } else {
            File(File(projectPath).parentFile, "OUTPUT")
        }

        println("Output folder: $outputFolder")
        cleanupOutputDirectory(outputFolder)

        return formattedText.toString()

    } catch (e: Exception) {
        return "Errore durante la lettura del CSV: ${e.message}"
    }
}


private fun cleanupOutputDirectory(folder: File) {
    if (folder.exists()) {
        folder.deleteRecursively()
        println("Cartella ${folder.path} eliminata")
    }
}