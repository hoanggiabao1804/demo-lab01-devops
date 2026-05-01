def jsonToHtml(String jsonPath, String htmlPath) {
    
    if (!fileExists(jsonPath)) {
        error "JSON file not found: ${jsonPath}"
    }

    sh """
    jq -r '
    if length == 0 then
    "<p>No secrets detected</p>"
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
    " +

    (
    [.[] |
        "<tr>" +
        "<td>" + .File + "</td>" +
        "<td>" + .RuleID + "</td>" +
        "<td>" + .Secret + "</td>" +
        "<td>" + (.StartLine | tostring) + "</td>" +
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

    echo "HTML report generated at: ${htmlPath}"
}

return this
