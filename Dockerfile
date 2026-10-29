# Imagem de execucao do back-end MediStock.
# O .jar e produzido pelo job "build" do pipeline e baixado em target/ antes
# da construcao da imagem (.github/workflows/ci-cd.yml).
FROM eclipse-temurin:25-jre

WORKDIR /app
RUN useradd --system --create-home --shell /usr/sbin/nologin medistock
COPY --chown=medistock:medistock target/*.jar /app/medistock.jar
USER medistock

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=oracle
ENTRYPOINT ["java", "-jar", "/app/medistock.jar"]
