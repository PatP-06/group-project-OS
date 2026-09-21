# Base Image: Eclipse Temurin JDK 17
FROM eclipse-temurin:17-jdk

WORKDIR /workspace

# คัดลอกไฟล์ทั้งหมดเข้าไปใน container เพื่อให้รันได้แม้ไม่มี volume
COPY . /workspace

# คอมไพล์ไฟล์ .java ทั้งหมด และสั่งรัน Server
# CMD ["sh", "-c", "javac *.java && java Server"]
CMD ["sh", "-c", "javac *.java; java Server; tail -f /dev/null"]
