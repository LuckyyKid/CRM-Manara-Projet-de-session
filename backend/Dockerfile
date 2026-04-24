# Étape 1 : build avec Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests && cp target/*jar target/app.jar

# Étape 2 : run avec Java
FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/target/app.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]