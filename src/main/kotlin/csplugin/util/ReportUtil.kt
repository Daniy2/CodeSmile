package csplugin.util

import com.opencsv.CSVReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.nio.charset.Charset

fun formatCsvToReadableText(csvPath: String?, projectPath: String?, singleFileMode: Boolean): String {
    if (csvPath.isNullOrEmpty() || projectPath.isNullOrEmpty()) {
        cleanupOutputDirectory(File(projectPath?.let { File(it).parentFile }, "OUTPUT"))
        return  "Nessun code smell \n"
    }

    val csvFile = File(csvPath)
    if (!csvFile.exists()) {
        return "File CSV non trovato: $csvPath"
    }

    val formattedText = StringBuilder()
    val projectRoot = File(projectPath).absolutePath

    try {
        FileReader(csvFile, Charset.forName("UTF-8")).use { fileReader ->
            CSVReader(fileReader).use { reader ->
                reader.readNext() // Salta intestazione

                var line: Array<String>?
                while (reader.readNext().also { line = it } != null) {
                    line?.let {
                        val filename = it[0]
                        val functionName = it[1]
                        val smellName = it[2]
                        val lineNum = it[3]
                        val description = it[4]

                        val relativePath = filename.replace(projectRoot, "")
                            .replace("\\", "/")
                            .removePrefix("/") // Rimuove slash iniziale

                        formattedText.append(
                            "Rilevato: $smellName\n" +
                                    "File: $relativePath\n" +
                                    "Funzione: $functionName\n" +
                                    "Linea: $lineNum\n" +
                                    "Descrizione: $description\n\n"
                        )
                    }
                }
            }
        }

        // Pulizia directory OUTPUT

        val outputFolder = if(!singleFileMode){

            File(projectPath, "OUTPUT")
        }else{
            File(File(projectPath).parentFile, "OUTPUT")
        }

        println("Output folder: $outputFolder")
        cleanupOutputDirectory(outputFolder)

        return formattedText.toString()

    } catch (e: FileNotFoundException) {
        return "Errore: File CSV non trovato"
    } catch (e: IOException) {
        return "Errore durante la lettura del CSV: ${e.message}"
    }
}

private fun cleanupOutputDirectory(folder: File) {
    if (folder.exists()) {
        folder.deleteRecursively()
        println("Cartella ${folder.path} eliminata")
    }
}