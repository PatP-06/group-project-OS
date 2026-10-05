# Base Image: Eclipse Temurin JDK 17
FROM eclipse-temurin:17-jdk

WORKDIR /workspace

COPY *.java ./

CMD ["sh", "-c", "javac *.java && java Server; tail -f /dev/null"]
