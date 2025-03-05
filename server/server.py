import sys

from flask import Flask, request, jsonify
import subprocess
import os

app = Flask(__name__)

# Correzione del percorso di cs-smile
base_path = os.path.dirname(os.path.abspath(__file__))  # Percorso della cartella corrente del server (server.py)

# Funzione per eseguire CodeSmile tramite CLI
def run_codesmile(input_directory, output_directory):
    input_directory = os.path.abspath(input_directory)
    output_directory = os.path.abspath(output_directory)  # Converte in percorso assoluto

    # Non creare la cartella 'output' manualmente, lascia che 'cs-smile' lo faccia
    if not os.path.exists(output_directory):
        print(f"Output directory does not exist. Creating {output_directory}")
        os.makedirs(output_directory)  # Crea la cartella se non esiste

    # Verifica i permessi di scrittura
    if not os.access(output_directory, os.W_OK):
        raise PermissionError(f"No write permission for the output directory: {output_directory}")

    # Aggiungi la cartella cs-smile al PYTHONPATH
    cs_smile_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'cs-smile'))
    sys.path.append(cs_smile_path)  # Aggiungi cs-smile al PYTHONPATH

    # Costruisci il comando CLI per CodeSmile
    command = f"python -m cli.cli_runner --input {input_directory} --output {output_directory}"

    print(f"Running command: {command}")  # Log del comando eseguito

    try:
        # Imposta la directory di lavoro su cs-smile e esegui il comando
        result = subprocess.run(command, shell=True, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=cs_smile_path)

        # Log dell'output del comando
        print(f"Command output: {result.stdout.decode('utf-8')}")
        print(f"Command error (if any): {result.stderr.decode('utf-8')}")

        # Verifica se CodeSmile ha trovato code smells
        if "No code smells found" in result.stdout.decode('utf-8'):
            print("No code smells found. Skipping the creation of overview.csv.")
            return None

        # Verifica se il file 'overview.csv' è stato creato
        overview_file_path = os.path.join(output_directory, 'output', 'overview.csv')
        if not os.path.exists(overview_file_path):
            print(f"Expected CSV file not found: {overview_file_path}")
            return None

        return result.stdout.decode('utf-8')

    except subprocess.CalledProcessError as e:
        print(f"Error during CodeSmile execution: {e.stderr.decode('utf-8')}")
        return f"Error: {e.stderr.decode('utf-8')}"





@app.route('/analyze', methods=['POST'])
def analyze():
    try:
        # Aggiungiamo log per vedere se la richiesta arriva al server
        print("Received request to analyze project...")  # Log della richiesta ricevuta
        data = request.json
        print(f"Request data: {data}")  # Log dei dati ricevuti nel corpo della richiesta

        input_directory = data.get('input_directory')
        output_directory = data.get('output_directory')

        if not input_directory or not output_directory:
            return jsonify({"error": "Both input_directory and output_directory are required"}), 400

        print(f"Input directory: {input_directory}")
        print(f"Output directory: {output_directory}")

        # Verifica se la directory di input esiste
        if not os.path.exists(input_directory):
            print(f"Input directory does not exist: {input_directory}")
            return jsonify({"error": "Input directory does not exist"}), 400

        # Esegui CodeSmile sul progetto
        output = run_codesmile(input_directory, output_directory)

        if output is None:
            return jsonify({
                "status": "success",
                "message": "Analysis completed successfully, but no code smells were found. No CSV file generated."
            })

        # Restituire l'output come JSON
        return jsonify({
            "status": "success",
            "message": "Analysis completed successfully",
            "output_path": os.path.join(output_directory, 'output', 'overview.csv')
        })

    except Exception as e:
        print(f"Error occurred: {str(e)}")
        return jsonify({"error": f"Internal Server Error: {str(e)}"}), 500


if __name__ == '__main__':
    app.run(host='127.0.0.1', port=5000)  # Cambia da 0.0.0.0 a 127.0.0.1
