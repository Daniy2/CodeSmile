package util

import com.opencsv.CSVReader
import java.io.File
import java.io.FileReader

fun formatCsvToReadableText(csvPath: String?, projectPath: String?): String {
    val formattedText = StringBuilder()

    try {
        if (csvPath != null) {
            // Ottieni il percorso della root del progetto
            val projectRoot = File(".").absoluteFile.parent  // Ottieni la root directory del progetto

            // Calcola il percorso relativo rispetto alla root del progetto
            val relativePath = File(csvPath).absolutePath.replace(projectRoot, "").replace("\\", "/")  // Normalizza il percorso
            println("Percorso relativo: $relativePath")

            // Usa try-with-resources per chiudere automaticamente il reader
            FileReader(File(csvPath)).use { fileReader ->
                CSVReader(fileReader).use { reader ->
                    var line: Array<String>?

                    // Salta la prima riga (nomi delle colonne)
                    reader.readNext()

                    // Leggi ogni riga del CSV
                    while (reader.readNext().also { line = it } != null) {
                        val filename = line!![0]
                        val functionName = line!![1]
                        val smellName = line!![2]
                        val lineNum = line!![3]
                        val description = line!![4]

                        // Mantieni la barra rovesciata nel percorso, e sostituiscila con il percorso relativo
                        val relativeFilename = filename.replace(projectRoot, "").replace("\\", "/")  // Percorso relativo rispetto alla root del progetto

                        // Formatta l'output per l'utente
                        formattedText.append("Rilevato code smell di tipo \"$smellName\" nel file \"$relativeFilename\" nella funzione \"$functionName\" alla linea $lineNum. Descrizione: $description\n")
                    }
                }
            }
        } else {
            formattedText.append("No smells found\n")
        }

        // Ottieni la directory del progetto e aggiungi la cartella OUTPUT
         // Ottieni la root directory del progetto
        val outputDirectory = File(projectPath, "OUTPUT")  // Combina il percorso del progetto con OUTPUT
        val outputFolder = outputDirectory

        println("Percorso della cartella OUTPUT: ${outputFolder.absolutePath}")

        if (outputFolder.exists() && outputFolder.isDirectory) {
            Thread.sleep(500)  // Attendi 500ms per rilasciare le risorse
            if (deleteFileOrFolder(outputFolder)) {
                println("Cartella OUTPUT eliminata con successo.")
            } else {
                println("Impossibile eliminare la cartella OUTPUT.")
            }
        } else {
            println("La cartella OUTPUT non esiste o non è una directory valida.")
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }

    return formattedText.toString()
}



// Funzione ricorsiva per eliminare una cartella e i suoi contenuti
private fun deleteFileOrFolder(file: File): Boolean {
    if (file.isDirectory) {
        // Elimina ricorsivamente i contenuti della cartella
        file.listFiles()?.forEach { child ->
            if (!deleteFileOrFolder(child)) {
                println("Impossibile eliminare: ${child.path}")
                return false
            }
        }
    }
    // Tenta di eliminare il file o la cartella
    return if (file.delete()) {
        true
    } else {
        println("Impossibile eliminare: ${file.path}")
        false
    }
}