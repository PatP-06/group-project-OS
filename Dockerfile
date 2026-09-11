# Base Image: Eclipse Temurin JDK 17
FROM eclipse-temurin:17-jdk

WORKDIR /workspace

CMD ["sh", "-c", "javac Server.java Client.java && java Server"]
