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

    println("Modalità $singleFileMode")
    // Crea la richiesta JSON con il percorso del progetto
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

    println("Sending analysis request: $json")

    val body = json
        .toRequestBody("application/json".toMediaTypeOrNull())

    // Crea la richiesta HTTP
    val request = Request.Builder()
        .url("http://localhost:5000/analyze")
        .post(body)
        .build()

    // Esegue richiesta in modo asincrono
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace() // Gestione degli errori
            resultsArea.append("Errore nella richiesta: ${e.message}\n")
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val responseBody = response.body?.string()

                //Gson per deserializzare la risposta JSON
                val apiResponse = Gson().fromJson(responseBody, ApiResponse::class.java)

                // Mostra il messaggio e il percorso del file
//                when(singleFileMode) {
//                    true -> resultsArea.append("Risultati dell'analisi sul file corrente:\n")
//                    else -> resultsArea.append("Risultati dell'analisi sul progetto:\n")
//                }


                println("Api response : $apiResponse")
                println("Api response message : ${apiResponse.message}")
                println("Api response status : ${apiResponse.status}")
                println("Api response output path : ${apiResponse.output_path}")


                val csvFilePath = apiResponse.output_path // Percorso completo al file overview.csv
                println("Csv file path: $csvFilePath")

                when (apiResponse.message) {
                    "Analysis completed successfully, but no code smells were found. No CSV file generated." -> {
                        resultsArea.append(formatCsvToReadableText(csvFilePath, projectPath,singleFileMode))

                    }
                    "Analysis completed successfully" -> {
                        resultsArea.append(formatCsvToReadableText(csvFilePath, projectPath,singleFileMode))
                    }
                    else -> {
                        resultsArea.append("Errore nella risposta del server.\n")
                    }
                }
            }
        }
    })
}


