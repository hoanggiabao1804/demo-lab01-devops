def jsonToHtml(String jsonPath, String htmlPath) {

    if (!fileExists(jsonPath)) {
        error "Snyk JSON file not found: ${jsonPath}"
    }

    sh """
    jq -r '
    if (.vulnerabilities | length) == 0 then
    "<p>No vulnerabilities</p>"
    else
    "<html>
    <head>
    <style>
    body { font-family: Arial; padding: 20px; }
    h2 { margin-bottom: 20px; }

    table {
    border-collapse: collapse;
    width: 100%;
    }

    th, td {
    border: 1px solid #ddd;
    padding: 10px;
    text-align: left;
    }

    th {
    background-color: #f4f4f4;
    }

    tr:nth-child(even) {
    background-color: #fafafa;
    }
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
    " +

    (
    [.vulnerabilities[] |
        "<tr>" +
        "<td>" + .severity + "</td>" +
        "<td>" + .packageName + "</td>" +
        "<td>" + .version + "</td>" +
        "<td>" + .title + "</td>" +
        "<td>" + (if .fixedIn then (.fixedIn | join(", ")) else "N/A" end) + "</td>" +
        "</tr>"
    ] | join("")
    )

    + "

    </tbody>
    </table>

    </body>
    </html>
    "
    end
    ' ${jsonPath} > ${htmlPath}
    """

    echo "Snyk HTML report generated: ${htmlPath}"
}

return this