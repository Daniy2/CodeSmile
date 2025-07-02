function onFormSubmit(e) {
  const sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  const lastRow = sheet.getLastRow();
  const headers = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
  const row = sheet.getRange(lastRow, 1, 1, sheet.getLastColumn()).getValues()[0];

  const email = row[headers.indexOf("Indirizzo email")];
  const nome = email ? email.split("@")[0] : "utente";

  const f1Col = headers.indexOf("Fase 1") + 1;
  const f2Col = headers.indexOf("Fase 2") + 1;

  // Conta le righe con Fase 1 già assegnata
  const assignedCount = sheet.getRange(2, f1Col, lastRow - 1).getValues()
      .filter(row => row[0] !== "").length;

  const participantIndex = assignedCount % 4;

  // Rotazione fissa
  const assignment = [
    { order: "Tool → Manual", toolSet: "T1", manualSet: "M2" },
    { order: "Manual → Tool", toolSet: "T2", manualSet: "M1" },
    { order: "Tool → Manual", toolSet: "T2", manualSet: "M1" },
    { order: "Manual → Tool", toolSet: "T1", manualSet: "M2" }
  ];

  const zipLinks = {

    "T1": "https://drive.google.com/uc?export=download&id=1RGQpSAIWXimd3Hj1blq1h_EPeUHo7mb3",
    "T2": "https://drive.google.com/uc?export=download&id=1DnsWq3PO3p2z5HIGw1P55lwwEgQ8xK30",
    "M1": "https://drive.google.com/uc?export=download&id=1jVRgF5ZonzEk1SoiVYzvbWl56eIK57kV",
    "M2": "https://drive.google.com/uc?export=download&id=1LISbkXTyrF7rkLhDpjC5InplE9poo7Xb"
  };

  const evaluationLinksByAssignment = {
    0: "https://docs.google.com/forms/d/e/1FAIpQLSchRp-R1IBtY6poJUQxWlGKtIDzNeFY6Je5pkuFDQRbhgoHFQ/viewform?usp=dialog",  // P1: T1 → M2
    1: "https://docs.google.com/forms/d/e/1FAIpQLScUZK_C3LL5KFqu6ljs-ggwFHk9i5n6_iE5FksC1ZiembaUvQ/viewform?usp=dialog",  // P2: M1 → T2
    2: "https://docs.google.com/forms/d/e/1FAIpQLSc0FzQRj-3TbEGGULnhm8ZWJEDJlLZB5adVx_Yp4h3XHnRvGw/viewform?usp=dialog",  // P3: T2 → M1
    3: "https://docs.google.com/forms/d/e/1FAIpQLSeFEMrD1-ubW9sl1RlI2uxY4TfoDXnPFkEqZLOHQJhtjac34g/viewform?usp=dialog"   // P4: M2 → T1
  };

  const { order, toolSet, manualSet } = assignment[participantIndex];
  const repoLink = "https://github.com/Daniy2/CodeSmile";
  const evalLink = evaluationLinksByAssignment[participantIndex];

  const toolLink = zipLinks[toolSet];
  const manualLink = zipLinks[manualSet];

  // Scrivi nel foglio
  if (order === "Tool → Manual") {
    sheet.getRange(lastRow, f1Col).setValue(toolSet);
    sheet.getRange(lastRow, f2Col).setValue(manualSet);
  } else {
    sheet.getRange(lastRow, f1Col).setValue(manualSet);
    sheet.getRange(lastRow, f2Col).setValue(toolSet);
  }

  // Email body
  const bodyHtml = `Ciao ${nome},<br><br>
Sei stato assegnato alla seguente sequenza per il test <b>CodeSmile</b>:<br><br>

🔁 <b>Ordine delle fasi:</b> ${order}<br><br>

📄 <b>FASE MANUALE (${manualSet}):</b><br>
<a href="${manualLink}">Scarica i file manuali (m1–m6)</a><br><br>

⚙️ <b>FASE TOOL (${toolSet}):</b><br>
<a href="${toolLink}">Scarica i file con tool (s1–s6)</a><br><br>

<b>📌 Istruzioni:</b><br>
1. Scarica i file e svolgi l'analisi secondo l'ordine indicato<br>
2. Identifica i code smells manualmente o con il tool<br>
3. <b>Registra il tempo</b> per ciascun file<br><br>

🔧 Il tool è disponibile al link:<br>
<a href="${repoLink}">${repoLink}</a><br><br>

✅ Una volta completato, compila il questionario:<br>
<a href="${evalLink}">${evalLink}</a><br><br>

Grazie per il tuo contributo!<br>
— Il team CodeSmile`;

  Logger.log("📤 Email per: " + email);

  if (email) {
    MailApp.sendEmail({
      to: email,
      subject: "CodeSmile – File assegnati per il test",
      htmlBody: bodyHtml
    });
    Logger.log("✅ Email inviata");
  } else {
    Logger.log("⚠️ Nessun indirizzo email valido trovato.");
  }
}
