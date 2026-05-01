import groovy.json.JsonSlurper

def jsonToHtml(String jsonPath, String htmlPath) {
    if (!fileExists(jsonPath)) {
        error "JSON file not found: ${jsonPath}"
    }

    def jsonContent
    try {
        jsonContent = readFile(jsonPath)
    } catch (Exception e) {
        error "Cannot read file: ${jsonPath}"
    }

    def data
    try {
        data = new JsonSlurper().parseText(jsonContent)
    } catch (Exception e) {
        error "Invalid JSON format: ${jsonPath}"
    }

    if (!data || data.size() == 0) {
        writeFile file: htmlPath, text: "<p>No secrets detected</p>"
        echo "No secrets found, HTML created."
        return
    }

    // Build HTML rows
    def rows = data.collect { item
        return """
        <tr>
            <td>${item.File ?: ''}</td>
            <td>${item.RuleID ?: ''}</td>
            <td>${item.Secret ?: ''}</td>
            <td>${item.StartLine ?: ''}</td>
        </tr>
        """
    }.join("\n")

    def html = """
    <html>
    <head>
        <style>
            body { font-family: Arial; padding: 20px; }
            h2 { margin-bottom: 20px; }
            table { border-collapse: collapse; width: 100%; }
            th, td {
                border: 1px solid #ddd;
                padding: 10px;
                text-align: left;
            }
            th { background-color: #f4f4f4; }
            tr:nth-child(even) { background-color: #fafafa; }
        </style>
    </head>
    <body>

    <h2>Gitleaks Report</h2>

    <table>
        <thead>
            <tr>
                <th>File</th>
                <th>RuleID</th>
                <th>Secret</th>
                <th>StartLine</th>
            </tr>
        </thead>
        <tbody>
            ${rows}
        </tbody>
    </table>

    </body>
    </html>
    """

    writeFile file: htmlPath, text: html

    echo "HTML report generated at: ${htmlPath}"
}

return this
