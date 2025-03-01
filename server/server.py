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
    # Verifica solo che la cartella di output esista
    if not os.path.exists(output_directory):
        print(f"Output directory does not exist. Creating {output_directory}")
        os.makedirs(output_directory)  # Crea la cartella se non esiste

    # Verifica se abbiamo i permessi di scrittura nella cartella di output
    if not os.access(output_directory, os.W_OK):
        raise PermissionError(f"No write permission for the output directory: {output_directory}")

    # Aggiungi la cartella cs-smile al PYTHONPATH
    cs_smile_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'cs-smile'))
    sys.path.append(cs_smile_path)  # Aggiungi cs-smile al PYTHONPATH

    # Costruisci il comando CLI per CodeSmile con il comando corretto
    command = f"python -m cli.cli_runner --input {input_directory} --output {output_directory}"

    print(f"Running command: {command}")  # Aggiungi il logging del comando eseguito

    try:
        # Imposta la directory di lavoro su cs-smile e esegui il comando
        result = subprocess.run(command, shell=True, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=cs_smile_path)
        print(f"Command output: {result.stdout.decode('utf-8')}")  # Log dell'output
        print(f"Command error (if any): {result.stderr.decode('utf-8')}")  # Log degli errori (se ci sono)
        return result.stdout.decode('utf-8')
    except subprocess.CalledProcessError as e:
        print(f"Error: {e.stderr.decode('utf-8')}")
        return f"Error: {e.stderr.decode('utf-8')}"




@app.route('/analyze', methods=['POST'])
def analyze():
    # Ricevi il percorso del progetto e la cartella di output dalla richiesta JSON
    data = request.json
    input_directory = data.get('input_directory')
    output_directory = data.get('output_directory')

    if not input_directory or not output_directory:
        return jsonify({"error": "Both input_directory and output_directory are required"}), 400

    # Verifica se la directory di input esiste
    if not os.path.exists(input_directory):
        return jsonify({"error": "Input directory does not exist"}), 400

    # Esegui CodeSmile sul progetto
    output = run_codesmile(input_directory, output_directory)

    # Restituire l'output come JSON
    return jsonify({
        "status": "success",
        "message": "Analysis completed successfully",
        "output_path": os.path.join(output_directory, 'output', 'overview.csv')
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
