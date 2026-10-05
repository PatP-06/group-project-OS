# Concert Reservation

> **Operating Systems Group Project**  
> ระบบจำลองการจองตั๋วคอนเสิร์ต/ทรัพยากรจำนวน 50 ที่นั่ง (`Ticket 1-50`) ที่รองรับผู้ใช้งานหลายราย (Clients 1–5) เข้ามาทำรายการพร้อมกัน โดยมีแม่ข่าย (Server) ทำหน้าที่ประมวลผลคำขอแบบขนานด้วย **Worker Thread Pool (3 Threads)**

---

## สารบัญ (Table of Contents)

- [โครงสร้างโปรเจกต์ (Project Structure)](#โครงสร้างโปรเจกต์-project-structure)
- [คู่มือการใช้งานระบบและ Docker ตามข้อกำหนด (System & Docker Guide)](#คู่มือการใช้งานระบบและ-docker-ตามข้อกำหนด-system--docker-guide)
  - [1. วิธี Build Docker Image](#1-วิธี-build-docker-image)
  - [2. วิธี Run Container](#2-วิธี-run-container)
  - [3. วิธีเปิด Server](#3-วิธีเปิด-server)
  - [4. วิธีเปิด Client หลายตัว](#4-วิธีเปิด-client-หลายตัว)
  - [5. รูปแบบ Message Queue ที่ใช้](#5-รูปแบบ-message-queue-ที่ใช้)
  - [6. คำสั่งที่ Client รองรับ](#6-คำสั่งที่-client-รองรับ)
  - [7. วิธีทดลอง Race Condition](#7-วิธีทดลอง-race-condition)
  - [8. วิธีเปิด/ปิด Synchronization](#8-วิธีเปิดปิด-synchronization)
- [วิธีรันบนเครื่องโดยตรง (Local Terminal Execution)](#วิธีรันบนเครื่องโดยตรง-local-terminal-execution)
- [ระบบบันทึกส่วนกลาง (Centralized Logging)](#ระบบบันทึกส่วนกลาง-centralized-logging)
- [ผู้จัดทำ (Producer)](#ผู้จัดทำ-producer)
---

## โครงสร้างโปรเจกต์ (Project Structure)

| ไฟล์ | หน้าที่และความรับผิดชอบ |
| :--- | :--- |
| `Server.java` | แม่ข่ายหลัก เริ่มการทำงานของ 3 Worker Threads และทำหน้าที่เป็น Dispatcher คัดแยกคำขอ |
| `Client.java` | ลูกข่าย จัดการคำสั่งจากผู้ใช้, ส่งคำขอผ่าน IPC, รับผลลัพธ์ และรองรับระบบ Stress Test |
| `MessageQueue.java` | ตัวกลาง IPC สื่อสารผ่านไฟล์ (Request/Response) และคิว Monitor ภายในสำหรับ Workers |
| `Worker.java` | Thread ประมวลผลคำขอ `RESERVE` และ `CANCEL` ผ่าน ReservationManager |
| `ReservationManager.java` | ตัวควบคุม Concurrency: จัดการ Mutex Lock (`synchronized`), สิทธิ์ความเป็นเจ้าของ, และ `randomDelay()` |
| `SeatManager.java` | จัดการตารางข้อมูลตั๋ว 50 ที่นั่ง (`Seat[] seats`) การสร้างผัง และตรวจสอบสถานะ |
| `Seat.java` | Model ข้อมูลตั๋วแต่ละใบ (หมายเลข, สถานะ `AVAILABLE`/`RESERVE`, ชื่อเจ้าของ `owner`) |
| `Request.java` | Data Transfer Object (DTO) บรรจุคำขอ (คำสั่ง, หมายเลขตั๋ว, Client ID) |
| `ServerLogger.java` | Centralized Thread-Safe Logger จัดการพิมพ์ Log แบบไม่ให้ข้อความซ้อนทับกัน |
| `TUI.java` | Text User Interface แสดงผลเมนู, กล่องตารางผังตั๋ว 50 ใบ และผลลัพธ์สีสันสวยงาม |
| `Dockerfile` | Image configuration สำหรับคอมไพล์และรันระบบบน Linux container (JDK 17) |
| `docker-compose.yml` | Container Orchestration สำหรับรัน Server และ Mount โค้ดลงใน Container |

---

## คู่มือการใช้งานระบบและ Docker ตามข้อกำหนด (System & Docker Guide)

ระบบนี้รองรับการทำงานบน Container ด้วย Docker และจำลองสภาวะแวดล้อมระบบปฏิบัติการ (OS) ผ่านหัวข้อสำคัญ 8 ประการ ดังนี้:

### 1. วิธี Build Docker Image
สามารถเลือก Build ได้ 2 วิธีตามเครื่องมือที่มีในเครื่อง:
* **วิธีที่ 1 (ผ่าน Dockerfile โดยตรง):**
  เปิด Terminal ในโฟลเดอร์โปรเจกต์ แล้วรันคำสั่ง:
  ```bash
  docker build -t os-server .
  ```
* **วิธีที่ 2 (ผ่าน Docker Compose):**
  ```bash
  docker compose build
  ```

---

### 2. วิธี Run Container
* **วิธีที่ 1 (ผ่าน Docker CLI):**
  รัน Container ในพื้นหลัง (Detach mode) พร้อมทำการ Mount ไดเรกทอรีโปรเจกต์เข้าไปที่ `/workspace`:
  * สำหรับ PowerShell / macOS / Linux:
    ```bash
    docker run -d --name os-server -v "${PWD}:/workspace" os-server
    ```
  * สำหรับ Windows Command Prompt (cmd):
    ```cmd
    docker run -d --name os-server -v "%cd%:/workspace" os-server
    ```
* **วิธีที่ 2 (ผ่าน Docker Compose - แนะนำ):**
  ```bash
  docker compose up -d
  ```
* **ตรวจสอบสถานะการทำงานของ Container:**
  ```bash
  docker ps
  ```

---

### 3. วิธีเปิด Server
ระบบถูกตั้งค่าให้อ่านและคอมไพล์โค้ดภาษา Java พร้อมเริ่มการทำงานของ `Server` อัตโนมัติทันทีที่ Container เริ่มทำงาน (`javac *.java && java Server`)
* **ตรวจสอบการทำงานและหน้าจอแสดงผลของ Server ได้ผ่านทาง Log:**
  ```bash
  docker logs -f os-server
  ```
* **กรณีที่ต้องการสั่งรัน Server ใหม่อีกครั้งด้วยตนเองภายใน Container:**
  ```bash
  docker exec -it os-server java Server
  ```

---

### 4. วิธีเปิด Client หลายตัว
ให้เปิดหน้าต่าง Terminal ใหม่แยกกันตามจำนวน Client ที่ต้องการจำลอง (เช่น Client 1 ถึง Client 5) แล้วรันคำสั่ง Execute เข้าสู่ Container ดังนี้:
* **Terminal หน้าต่างที่ 1 (จำลอง Client 1):**
  ```bash
  docker exec -it os-server java Client 1
  ```
* **Terminal หน้าต่างที่ 2 (จำลอง Client 2):**
  ```bash
  docker exec -it os-server java Client 2
  ```
* **Terminal หน้าต่างที่ 3 (จำลอง Client 3):**
  ```bash
  docker exec -it os-server java Client 3
  ```
* **Terminal หน้าต่างที่ 4 (จำลอง Client 4):**
  ```bash
  docker exec -it os-server java Client 4
  ```
* **Terminal หน้าต่างที่ 5 (จำลอง Client 5):**
  ```bash
  docker exec -it os-server java Client 5
  ```

---

### 5. รูปแบบ Message Queue ที่ใช้
ระบบใช้การสื่อสารระหว่างโพรเซส (Inter-Process Communication: IPC) ผ่าน **Shared File Queue / Shared Volume Directory** แทนการใช้ Network Socket เพื่อเป็นไปตามหลักการจัดการทรัพยากรของระบบปฏิบัติการ:
* **โครงสร้างคิวในระดับ File System:**
  * `ipc_queue/requests/` : ที่เก็บไฟล์คำขอจาก Client มีรูปแบบชื่อไฟล์ `timestamp_seq_clientId.req`
  * `ipc_queue/responses/` : ที่เก็บไฟล์ผลลัพธ์จาก Server ส่งกลับให้ Client มีรูปแบบชื่อไฟล์ `timestamp_seq_clientId.res`
* **กลไกความปลอดภัยและความถูกต้อง (Concurrency & Atomicity):**
  * **Atomic Write**: Client จะเขียนคำขอลงในไฟล์นามสกุล `.tmp` ก่อน และใช้คำสั่ง `renameTo()` เปลี่ยนเป็น `.req` เพื่อป้องกันไม่ให้ Server อ่านไฟล์ในขณะที่เนื้อหายังเขียนไม่สมบูรณ์
  * **File Lock Protection**: Worker จะใช้ไฟล์ `.lock` ขณะหยิบคำขอ เพื่อป้องกันการชนกัน (Race Condition) ระหว่าง Worker ที่จะแย่งหยิบไฟล์เดียวกัน
  * **FIFO Ordering**: คัดแยกและหยิบไฟล์คำขอขึ้นมาทำงานตามลำดับเวลา (Timestamp)
  * **Worker Memory Queue**: ภายใน Server จัดการคิวงานของ Worker Thread Pool ทั้ง 3 ตัวผ่าน `LinkedList` ควบคุมด้วย Monitor Mechanism (`wait()` และ `notifyAll()`)

---

### 6. คำสั่งที่ Client รองรับ
Client สามารถสั่งการได้ทั้งการพิมพ์ตัวเลขเมนูหรือพิมพ์ชื่อคำสั่งภาษาอังกฤษ:
* `LIST` (หรือกด `1`): แสดงผังที่นั่งคอนเสิร์ตทั้งหมด 50 ที่นั่ง (Ticket 1-50) พร้อมสถานะว่างหรือถูกจอง
* `STATUS <ticket_id>` (หรือกด `2`): ตรวจสอบสถานะและผู้ถือครองตั๋วใบที่ระบุ เช่น `STATUS 10`
* `RESERVE <ticket_id>` (หรือกด `3`): ทำการจองตั๋วใบที่ระบุ เช่น `RESERVE 10`
* `CANCEL <ticket_id>` (หรือกด `4`): ยกเลิกการจองตั๋ว (เฉพาะ Client ที่เป็นเจ้าของตั๋วจริงเท่านั้นจึงจะมีสิทธิ์ยกเลิกได้)
* `SHOOT` (หรือกด `5`): รัน Concurrency Stress Test ส่ง 250 คำขอยิงแย่งตั๋วพร้อมกัน (5 Clients แย่งตั๋ว 50 ใบพร้อมกันในเสี้ยววินาที)
* `QUIT` (หรือกด `0`): ปิดโปรแกรม Client

---

### 7. วิธีทดลอง Race Condition
เพื่อสังเกตปัญหาทาง Concurrency เมื่อไม่มีการควบคุมการเข้าถึง Critical Section:
1. ปิดระบบ Synchronization ในโค้ด (ดูวิธีในข้อ 8)
2. สตาร์ต Server บน Docker Container
3. เปิด Client อย่างน้อย 1 ตัว แล้วพิมพ์คำสั่ง `SHOOT` (หรือกด `5`)
4. สังเกตผลลัพธ์:
   * เกิดปัญหา **Overbooking (Check-Then-Act)**: จำนวนคำขอที่ได้รับผลตอบรับว่า "จองสำเร็จ" รวมกันจะเกิน 50 ครั้ง (เช่น สำเร็จ 65-70 ครั้ง จากตั๋วที่มีเพียง 50 ใบ)
   * เกิดปัญหา **Lost Update**: ข้อมูลเจ้าของตั๋วถูกเขียนทับสลับไปมา เนื่องจาก Worker หลายตัวอ่านสถานะว่าว่างพร้อมกันในช่วงที่มี Delay แล้วเขียนทับข้อมูลกัน

---

### 8. วิธีเปิด/ปิด Synchronization
สามารถสลับโหมดการทำงานได้ที่ไฟล์ `Server.java` (บรรทัดที่ 40):
* **กรณีเปิด Synchronization (Default - ระบบถูกต้อง 100%):**
  ```java
  ReservationManager reservationManager = new ReservationManager(true, bridge);
  ```
  * ผลลัพธ์: มีการใช้ `synchronized (this)` ควบคุม Critical Section ตั๋วแต่ละใบจะถูกประมวลผลทีละ 1 Worker แบบ Atomic Transaction ยอดจองสำเร็จรวมจะเท่ากับ 50 ใบถ้วนเสมอ ไม่มีการจองซ้ำ
* **กรณีปิด Synchronization (เพื่อทดลอง Race Condition):**
  ```java
  ReservationManager reservationManager = new ReservationManager(false, bridge);
  ```
  * ผลลัพธ์: ปิดกั้น Lock ทำให้เกิดการแย่งชิงทรัพยากรและเกิดปัญหา Overbooking

---

## วิธีรันบนเครื่องโดยตรง (Local Terminal Execution)

กรณีไม่ต้องการรันผ่าน Docker สามารถรันบนเครื่องโดยตรงด้วยขั้นตอนดังนี้:

### ขั้นตอนที่ 1: คอมไพล์โค้ด
เปิด Terminal ที่โฟลเดอร์โปรเจกต์ แล้วรันคำสั่ง:
```bash
javac *.java
```

### ขั้นตอนที่ 2: รัน Server (Terminal ที่ 1)
```bash
java Server
```
*ระบบจะล้างคิวเก่าอัตโนมัติ และแสดงสถานะ `Server is READY!`*

### ขั้นตอนที่ 3: รัน Client (Terminal ที่ 2 ถึง 6)
เปิด Terminal ใหม่เพื่อรัน Client แต่ละตัว (จำลองลูกค้าหลายคน):
```bash
# Terminal 2 (Client 1)
java Client 1

# Terminal 3 (Client 2)
java Client 2

# Terminal 4 (Client 3)
java Client 3
```

---

## ระบบบันทึกส่วนกลาง (Centralized Logging)

ทุกเหตุการณ์ที่เกิดขึ้นใน Server จะถูกบันทึกผ่าน `ServerLogger` ในรูปแบบมาตรฐาน:
```text
[Timestamp]           [Worker/Entity] [Action]   [Message]
[2026-10-03 17:55:58] [SERVER    ]    [START   ] Concurrent Reservation Server started
[2026-10-03 17:56:02] [Worker-1  ]    [LOCK    ] Entering critical section
[2026-10-03 17:56:02] [Worker-1  ]    [RESERVE ] SUCCESS: Reserved ticket 10 by Client-1
[2026-10-03 17:56:02] [Worker-1  ]    [UNLOCK  ] Leaving critical section
[2026-10-03 17:56:05] [Worker-2  ]    [CANCEL  ] SUCCESS: Cancelled ticket 10 by Client-1
```
*มีการใช้ `synchronized` ครอบการพิมพ์ Log เพื่อป้องกันข้อความทับซ้อนกันจากหลาย Thread (Interleaving)*

---

## ผู้จัดทำ (Producer)

* นางสาวฐิตารีย์ ล่ำสัน รหัสนักศึกษา 68090500408
* นางสาวธนัญญา ลิมป์จันทรา รหัสนักศึกษา 68090500412
* นางสาวพรรธนพร เหมวัตร รหัสนักศึกษา 68090500413
* นายพีรภาส รุ่งวัฒนไพบูลย์ รหัสนักศึกษา 68090500415
* นายณธภัค ภัทรพงศ์วัฒนา รหัสนักศึกษา 68090500450
