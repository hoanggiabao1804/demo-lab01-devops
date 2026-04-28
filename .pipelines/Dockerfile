FROM maven:3.9.14-eclipse-temurin-25

RUN apt-get update -qq && \
    apt-get install -y -qq --no-install-recommends \
        jq \
        tar \
        ca-certificates \
        gnupg

# Install Node.js 20
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y -qq --no-install-recommends nodejs && \
    rm -rf /var/lib/apt/lists/*

# Install Gitleaks
RUN curl -fL https://github.com/gitleaks/gitleaks/releases/download/v8.30.1/gitleaks_8.30.1_linux_x64.tar.gz -o gitleaks.tar.gz && \
    tar -xzf gitleaks.tar.gz && \
    mv gitleaks /usr/local/bin/gitleaks && \
    chmod +x /usr/local/bin/gitleaks && \
    rm gitleaks.tar.gz

# Install Snyl CLI
RUN curl -Lo /usr/local/bin/snyk https://static.snyk.io/cli/latest/snyk-linux && \
    chmod +x /usr/local/bin/snyk

# Verify tools
RUN node -v && npm -v && \
    jq --version && \
    gitleaks version && \
    snyk --version
