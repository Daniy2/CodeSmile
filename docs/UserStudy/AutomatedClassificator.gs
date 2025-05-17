function onFormSubmit(e) {
  const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  const lastRow = sheet.getLastRow();
  const headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
  const row = sheet.getRange(lastRow, 1, 1, sheet.getLastColumn()).getValues()[0];

  Logger.log("🟡 Nuovo invio rilevato. Riga: " + lastRow);

  const scale = {
    "Per nulla familiare": 1,
    "Poco familiare": 2,
    "Moderatamente familiare": 3,
    "Familiare": 4,
    "Molto familiare": 5
  };

  const fileMeta = {
    "testA": { smells: ["CIDX"], libs: ["pandas"] },
    "testB": { smells: ["CDE"], libs: ["pandas"] },
    "testC": { smells: ["GNC"], libs: ["pytorch"] },
    "testD": { smells: ["PC"], libs: ["pytorch"] },
    "testE": { smells: ["IPA"], libs: ["pandas"] },
    "testF": { smells: ["TA"], libs: ["tensorflow"] }
  };

  const fileLinks = {
    "testA_manual.py": "https://drive.google.com/uc?export=download&id=15YYmgmLFakK6bVDQOOcgl8QXU2xULhKe",
    "testA_tool.py": "https://drive.google.com/uc?export=download&id=1BPlBTbtDCsv-CoPr6CKIQbh8_gNuqr9Z",
    "testB_manual.py": "https://drive.google.com/uc?export=download&id=1Fb-_mFHlYfdu_YQqAYGCkfV6yzYroz9F",
    "testB_tool.py": "https://drive.google.com/uc?export=download&id=12tDNCZT9Byf2_r7nhjsVWD0zFT-Uv8dq",
    "testC_manual.py": "https://drive.google.com/uc?export=download&id=1LIfwcqfnCBJrVba1EY_oMmghkgEkyC27",
    "testC_tool.py": "https://drive.google.com/uc?export=download&id=15FZ8ssdDc1EKfmEQpTGVdrANIap0wPTN",
    "testD_manual.py": "https://drive.google.com/uc?export=download&id=1wQ7rNogrgRGNLjK4m0KZLw0m3x-hGerD",
    "testD_tool.py": "https://drive.google.com/uc?export=download&id=1jAYc8e4kZXRM1v6DfHgEppFk5N6dSf8Z",
    "testE_manual.py": "https://drive.google.com/uc?export=download&id=1_23EgigS16eXVat3cT_G_yWacZgGcfEW",
    "testE_tool.py": "https://drive.google.com/uc?export=download&id=1mOaqMyQOqkHluBzGZK9APoGLfkXs2_8r",
    "testF_manual.py": "https://drive.google.com/uc?export=download&id=1Bj021yMCwMM63JA7OjyW9bC5tF1_fv1f",
    "testF_tool.py": "https://drive.google.com/uc?export=download&id=1Drzr4ofNfd1Nds23g2SGyKkqSJ6TipzC"
  };

  const evalFormManualFirst = "https://tally.so/r/wQXvPk";
  const evalFormToolFirst = "https://tally.so/r/mBggYA";

  const getVal = (fragment) => {
    for (let i = 0; i < headers.length; i++) {
      if (headers[i].includes(fragment)) {
        const risposta = row[i];
        Logger.log("📥 Risposta per " + fragment + ": " + risposta);
        return risposta;
      }
    }
    Logger.log("⚠️ Nessun valore trovato per: " + fragment);
    return "";
  };

  const familiarity = {
    pandas: scale[getVal("[Pandas]")] || 0,
    tensorflow: scale[getVal("[TensorFlow]")] || 0,
    pytorch: scale[getVal("[PyTorch]")] || 0
  };

  Logger.log("📊 Familiarità librerie: " + JSON.stringify(familiarity));

  // Candidati iniziali
  const manualCandidates = [];
  const toolCandidates = [];

  for (let f in fileMeta) {
    const libs = fileMeta[f].libs;
    const isManual = libs.some(lib => familiarity[lib] >= 4);
    const isTool = libs.every(lib => familiarity[lib] <= 2);
    if (isManual) manualCandidates.push(f);
    if (isTool) toolCandidates.push(f);
  }

  Logger.log("🧾 Candidati Manuale iniziali: " + manualCandidates.join(", "));
  Logger.log("🛠️ Candidati Tool iniziali: " + toolCandidates.join(", "));

  // Funzione di random
  const pickRandom = (arr, num) => {
    const copy = [...arr];
    const picked = [];
    while (picked.length < num && copy.length > 0) {
      const index = Math.floor(Math.random() * copy.length);
      picked.push(copy.splice(index, 1)[0]);
    }
    return picked;
  };

  const manualFinal = pickRandom(manualCandidates, 2);
  const remainingForTool = manualCandidates.filter(f => !manualFinal.includes(f)).concat(toolCandidates);
  const toolFinal = pickRandom(remainingForTool, 2);

  Logger.log("📄 File Manuale finali scelti: " + manualFinal.join(", "));
  Logger.log("🛠️ File Tool finali scelti: " + toolFinal.join(", "));

  const m1 = manualFinal[0], m2 = manualFinal[1], t1 = toolFinal[0], t2 = toolFinal[1];

  const getSmells = f => f && fileMeta[f] ? fileMeta[f].smells.join(", ") : "";
  const getLink = (f, type) => fileLinks[`${f}_${type}.py`] || "";

  const order = ((lastRow - 2) % 2 === 0) ? "Manuale → Tool" : "Tool → Manuale";
  const evalLink = order === "Manuale → Tool" ? evalFormManualFirst : evalFormToolFirst;

  sheet.getRange(lastRow, 17, 1, 9).setValues([[
    m1, m2, t1, t2,
    getSmells(m1), getSmells(m2),
    getSmells(t1), getSmells(t2),
    order
  ]]);

  Logger.log("✅ Riga aggiornata nel foglio. Ordine: " + order);

  // Email
  const email = row[headers.indexOf("Indirizzo email")];
  const nome = email ? email.split("@")[0] : "utente";
  const manualNames = `- ${m1}.py → ${getLink(m1, "manual")}\n- ${m2}.py → ${getLink(m2, "manual")}`;
  const toolNames = `- ${t1}.py → ${getLink(t1, "tool")}\n- ${t2}.py → ${getLink(t2, "tool")}`;

  const repolink = "https://github.com/Daniy2/CodeSmile";

  const bodyHtml = `Ciao ${nome},<br><br>

Sei stato assegnato alla seguente sequenza per il test <b>CodeSmile</b>:<br><br>

🔁 <b>Ordine delle fasi:</b> ${order}<br><br>

📄 <b>FASE MANUALE:</b><br>
${manualNames.replaceAll('\n', '<br>')}<br><br>

⚙️ <b>FASE TOOL:</b><br>
${toolNames.replaceAll('\n', '<br>')}<br><br>

<b>📌 Cosa devi fare:</b><br>
1. Scarica i file indicati per ciascuna fase<br>
2. Analizza i file manualmente e con il tool seguendo l’ordine indicato<br>
3. Prendi nota dei code smells che riesci a identificare<br>
<b>4. IMPORTANTE!! Munisciti di un cronometro per misurare il tempo che impieghi per risolvere ogni task sul file.</b><br><br>

🔗 Il tool è disponibile al seguente link:<br>
<a href="${repolink}">${repolink}</a><br><br>

✅ Una volta completato il test, clicca qui per compilare il questionario di valutazione:<br>
👉 <a href="${evalLink}">${evalLink}</a><br><br>

Grazie per il tuo contributo!<br>
— Il team CodeSmile`;

  Logger.log("📤 Email preparata per: " + email);

  if (email) {
    MailApp.sendEmail({
      to: email,
      subject: "CodeSmile – File assegnati per il test",
      htmlBody: bodyHtml
    });
    Logger.log("✅ Email inviata a: " + email);
  } else {
    Logger.log("⚠️ Nessuna email trovata.");
  }
}