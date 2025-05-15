package csplugin.util

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import javax.swing.JTextArea

data class ApiResponse(val status: String, val message: String, val output_path: String)

fun sendAnalysisRequest(projectPath: String?, resultsArea: JTextArea, singleFileMode: Boolean) {
    val client = OkHttpClient()

    // Registra il tempo di inizio per la detection
    val startTime = System.currentTimeMillis()

    // Creazione della richiesta JSON con il percorso del progetto
    val outputDirectory = if (singleFileMode) {
        File(File(projectPath).parentFile, "OUTPUT").toString().replace("\\", "\\\\")
    } else {
        "$projectPath\\\\OUTPUT"
    }

    val json = """
        {
            "input_directory": "$projectPath",
            "output_directory": "$outputDirectory"
        }
    """.trimIndent()

    val body = json
        .toRequestBody("application/json".toMediaTypeOrNull())

    val request = Request.Builder()
        .url("http://localhost:5000/analyze")
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace() // Gestione degli errori
            resultsArea.append("Errore nella richiesta: ${e.message}\n")
        }

        override fun onResponse(call: Call, response: Response) {
            // Registra il tempo di fine per la detection
            val endTime = System.currentTimeMillis()
            val elapsedTime = endTime - startTime  // Tempo in millisecondi
            val elapsedTimeInSeconds = elapsedTime / 1000.0  // Converti in secondi

            // Calcola il tempo impiegato per la detection
            println("Tempo impiegato per la detection: ${"%.2f".format(elapsedTimeInSeconds)} secondi")

            if (response.isSuccessful) {
                val responseBody = response.body?.string()

                val apiResponse = Gson().fromJson(responseBody, ApiResponse::class.java)

                println("Api response : $apiResponse")
                println("Api response message : ${apiResponse.message}")
                println("Api response status : ${apiResponse.status}")
                println("Api response output path : ${apiResponse.output_path}")

                val csvFilePath = apiResponse.output_path // Percorso completo al file overview.csv
                println("Csv file path: $csvFilePath")

                when (apiResponse.message) {
                    "Analysis completed successfully, but no code smells were found. No CSV file generated." -> {
                        resultsArea.append(formatCsvToReadableText(csvFilePath, projectPath, singleFileMode))
                    }
                    "Analysis completed successfully" -> {
                        resultsArea.append(formatCsvToReadableText(csvFilePath, projectPath, singleFileMode))
                    }
                    else -> {
                        resultsArea.append("Errore nella risposta del server.\n")
                    }
                }

                // Aggiungi il tempo impiegato anche nei risultati visualizzati
                resultsArea.append("\nDetection lasted : ${"%.2f".format(elapsedTimeInSeconds)} mins\n")
            }
        }
    })
}


