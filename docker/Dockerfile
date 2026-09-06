FROM maven:3.9.11-eclipse-temurin-21 AS builder
WORKDIR /build
COPY app/pom.xml .
RUN mvn -B dependency:go-offline
COPY app/src ./src
RUN mvn -B clean verify

# Runtime stage: WAR -> Tomcat
FROM tomcat:10.1-jdk21-temurin

# Remove default applications to reduce attack surface.
RUN rm -rf /usr/local/tomcat/webapps/*

COPY --from=builder /build/target/secure-cicd-demo.war /usr/local/tomcat/webapps/secure-cicd-demo.war
USER 1001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3   CMD curl --fail --silent http://127.0.0.1:8080/secure-cicd-demo/api/health || exit 1
CMD ["catalina.sh","run"]
