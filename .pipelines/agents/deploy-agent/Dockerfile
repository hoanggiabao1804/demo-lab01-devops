FROM maven:3.9.14-eclipse-temurin-25

RUN apt-get update -qq && \
    apt-get install -y -qq --no-install-recommends \
        wget \
        ca-certificates \
        gnupg

# Install yq
RUN wget -qO /usr/local/bin/yq \
    https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64 && \
    chmod +x /usr/local/bin/yq

# Install Node.js 20
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get install -y -qq --no-install-recommends nodejs

# Install Docker CLI
RUN apt-get update -qq && \
    apt-get install -y -qq --no-install-recommends docker.io

# Cleanup
RUN rm -rf /var/lib/apt/lists/*

# Verify tools
RUN node -v && npm -v && \
    yq --version && \
    docker --version
