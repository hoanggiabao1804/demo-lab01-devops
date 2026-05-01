import groovy.json.JsonSlurper

def jsonToHtml(String jsonPath, String htmlPath) {

    if (!fileExists(jsonPath)) {
        error "Snyk JSON file not found: ${jsonPath}"
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

    def vulnerabilities = data?.vulnerabilities ?: []

    if (vulnerabilities.isEmpty()) {
        writeFile file: htmlPath, text: "<p>No vulnerabilities</p>"
        echo "No vulnerabilities found."
        return
    }

    def rows = vulnerabilities.collect { v ->
        def fixedIn = (v.fixedIn && v.fixedIn.size() > 0)
                ? v.fixedIn.join(", ")
                : "N/A"

        return """
        <tr>
            <td>${v.severity ?: ''}</td>
            <td>${v.packageName ?: ''}</td>
            <td>${v.version ?: ''}</td>
            <td>${v.title ?: ''}</td>
            <td>${fixedIn}</td>
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

    <h2>Snyk Vulnerability Report</h2>

    <table>
        <thead>
            <tr>
                <th>Severity</th>
                <th>Package</th>
                <th>Version</th>
                <th>Title</th>
                <th>Fixed In</th>
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

    echo "Snyk HTML report generated: ${htmlPath}"
}