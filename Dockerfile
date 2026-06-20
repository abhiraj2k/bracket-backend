FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY common/pom.xml common/
COPY app/pom.xml app/
RUN mvn dependency:go-offline -pl common,app -q
COPY common/src common/src
COPY app/src app/src
RUN mvn -pl common,app -DskipTests clean package -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/app/target/app-*.jar app.jar
ENV JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseContainerSupport"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
