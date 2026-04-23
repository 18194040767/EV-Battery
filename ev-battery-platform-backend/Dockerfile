FROM eclipse-temurin:8-jre
WORKDIR /app
COPY target/ev-battery-platform-backend-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
