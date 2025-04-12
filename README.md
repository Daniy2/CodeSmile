# 🧠 CodeSmile – A Code Smell Detection Tool for ML-enabled Systems

CodeSmile is a plugin and CLI tool for detecting **Machine Learning-specific code smells (ML-CSs)** in Python projects.  
It statically analyzes code to identify implementation patterns that may lead to performance, maintainability or correctness issues in ML pipelines.

> This project is the outcome of a research-based internship and thesis project at the University of Salerno.

---

## 🚀 Features

- ✅ Detection of ML-specific code smells (based on research literature)
- ✅ CLI-based and REST API execution
- ✅ IDE integration-ready (for JetBrains PyCharm)
- ✅ Real-time analysis support
- ✅ JSON and CSV output for integration & visualization

---

## 🎛️ Detection Modes

CodeSmile offers four detection modes, depending on your workflow or use case:

- 📄 **Single File Detection** – Analyze a specific Python file
- 🗂️ **Multiple Files Detection** – Analyze a selected set of files in a batch
- 🏗️ **Full Project Detection** – Scan an entire directory/project recursively
- ⚡ **Real-Time Detection** – Live detection during coding via the IDE plugin (PyCharm)


## 🔍 Supported Code Smells

CodeSmile currently detects the following ML-CSs:

- Chain Indexing  
- DataFrame Conversion API Misused  
- Columns and Dtype Not Explicitly Set
- Gradients Not Cleared Before Backward Propagation
- Matrix Multiplication API Misused  
- Merge API Parameter Not Explicitly Set  
- In-Place APIs Misused  
- NaN Equivalence Comparison Misused  
- Memory Not Freed Before Backward Propagation (TensorFlow)  
- PyTorch `forward()` Method Misused  
- TensorArray Not Used
- Unnecessary Iteration (non-vectorized Pandas usage)

---

## 🛠 Installation

1. Clone the repository:

```bash
git clone https://github.com/Daniy2/CodeSmile.git
cd CodeSmile
```
2. (Optional) Create a virtual environment:
   
```bash
python -m venv .venv
source .venv/bin/activate  # on Windows: .venv\Scripts\activate
```
3. Install dependencies:
```bash
pip install -r requirements.txt
```
⚙️ Usage 

### 🧪 Running the Plugin from IntelliJ (Gradle)

To launch CodeSmile as a JetBrains plugin:

1. Make sure your `Gradle` task is configured to run `runIde`
2. Set the environment variable `PROJECT_ROOT` to your project root (usually auto-filled)
3. You can use the provided run configuration file, or set it up manually like this:

![Gradle run configuration](./gradle_conf.png)



Once configured, hit ▶️ **Run Plugin** to launch the IDE with the plugin installed.

### 📂 Run the plugin on your project

1. Open your projects in the IDE
2. Click the tool window menu in Pycharm called "Code Smile" (to help you to search, see the photo below):
3. Select the detection modes you prefer and enjoy

![CS-Tool Window](./CsToolWindow.png)





