FROM eclipse-temurin:25-jre
ARG PORT=8096
EXPOSE ${PORT}
WORKDIR /app
COPY svc-openfinance/target/*.jar app.jar
ENV SERVER_PORT=${PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]

