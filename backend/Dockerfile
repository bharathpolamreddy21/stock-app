FROM maven:3.9.12-eclipse-temurin-17 AS build

WORKDIR /build

COPY . .

RUN mvn clean package

FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=build /build/target/*jar backend.jar

CMD ["java","-jar","backend.jar"]


