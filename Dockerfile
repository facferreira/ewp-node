# Build and package
FROM maven:3-openjdk-11-slim as builder

WORKDIR /build

# Compile and package
COPY . .
RUN mvn -B package

# Deploy
FROM bellsoft/liberica-openjre-debian:11

RUN apt-get update && apt-get install -y --no-install-recommends wget

# Install mermaid-cli for diagrams generation (requires node.js)
ENV CHROME_BIN="/usr/bin/chromium-browser" \
    PUPPETEER_SKIP_CHROMIUM_DOWNLOAD="true"
RUN apt-get update && apt-get install -y --no-install-recommends chromium && ln -T /usr/bin/chromium /usr/bin/chromium-browser
RUN wget -O - https://deb.nodesource.com/setup_18.x | bash -
RUN apt-get update && apt-get install -y --no-install-recommends nodejs
RUN npm install -g @mermaid-js/mermaid-cli
COPY puppeteer-config.json /opt/puppeteer-config.json

COPY --from=builder /build/target/*.jar /opt/app.jar

ENV SPRING_CONFIG_LOCATION=/config/application.yml
VOLUME /config

VOLUME /logs

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom", "-jar","/opt/app.jar"]
