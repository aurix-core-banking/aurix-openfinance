FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY svc-openfinance/pom.xml svc-openfinance/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -pl svc-openfinance dependency:go-offline -B
COPY svc-openfinance/src svc-openfinance/src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -pl svc-openfinance package -DskipTests -B

FROM eclipse-temurin:25-jre
ARG PORT=8096
EXPOSE ${PORT}
WORKDIR /app
COPY --from=build /app/svc-openfinance/target/*.jar app.jar
ENV SERVER_PORT=${PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]
