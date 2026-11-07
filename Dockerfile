FROM eclipse-temurin:17
WORKDIR /app
COPY ./target/breakalert-0.0.1-SNAPSHOT.jar /app
CMD ["java", "-jar", "breakalert-0.0.1-SNAPSHOT.jar"]