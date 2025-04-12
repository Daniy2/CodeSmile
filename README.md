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

