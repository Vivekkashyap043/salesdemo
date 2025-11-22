# Stage 1: build with Maven (uses mvnw)
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
# copy maven wrapper first to enable fast rebuilds
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src ./src
RUN chmod +x mvnw
RUN ./mvnw -B -DskipTests package

# Stage 2: runtime jre
FROM eclipse-temurin:17-jre-jammy
ARG JAR_FILE=target/*.jar
COPY --from=builder /app/${JAR_FILE} /app/app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
