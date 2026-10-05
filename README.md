# Concert Reservation

> **Operating Systems Group Project**  
> ระบบจำลองการจองตั๋วคอนเสิร์ต/ทรัพยากรจำนวน 50 ที่นั่ง (`Ticket 1-50`) ที่รองรับผู้ใช้งานหลายราย (Clients 1–5) เข้ามาทำรายการพร้อมกัน โดยมีแม่ข่าย (Server) ทำหน้าที่ประมวลผลคำขอแบบขนานด้วย **Worker Thread Pool (3 Threads)**

---

## สารบัญ (Table of Contents)

- [โครงสร้างโปรเจกต์ (Project Structure)](#โครงสร้างโปรเจกต์-project-structure)
- [คำสั่งที่รองรับ (Supported Commands)](#คำสั่งที่รองรับ-supported-commands)
- [วิธีติดตั้งและรันระบบ (Getting Started)](#วิธีติดตั้งและรันระบบ-getting-started)
  - [วิธีที่ 1: รันบนเครื่องโดยตรง (Local Terminal)](#วิธีที่-1-รันบนเครื่องโดยตรง-local-terminal)
  - [วิธีที่ 2: รันผ่าน Docker & Docker Compose](#วิธีที่-2-รันผ่าน-docker--docker-compose)
- [การทดลอง Race Condition vs Synchronization](#การทดลอง-race-condition-vs-synchronization)
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

## คำสั่งที่รองรับ (Supported Commands)

| คำสั่ง | ตัวอย่างการใช้งาน | คำอธิบาย |
| :---: | :--- | :--- |
| `[1] LIST` | `LIST` หรือพิมพ์ `1` | แสดงผังตั๋วทั้งหมด 50 ใบ พร้อมระบุสถานะสีสันแบบเรียลไทม์ |
| `[2] STATUS` | `STATUS 10` หรือพิมพ์ `2` | ตรวจสอบสถานะและผู้ครอบครองตั๋วใบที่ระบุ |
| `[3] RESERVE` | `RESERVE 10` หรือพิมพ์ `3` | จองตั๋วใบที่ต้องการ (หากยังว่างอยู่) |
| `[4] CANCEL` | `CANCEL 10` หรือพิมพ์ `4` | ยกเลิกการจองตั๋ว **(ต้องเป็นเจ้าของตั๋วเท่านั้น จึงจะยกเลิกได้)** |
| `[5] SHOOT` | `SHOOT` หรือพิมพ์ `5` | รัน Stress Test: ส่ง 250 คำขอยิงแย่งตั๋วพร้อมกัน 5 คนต่อ 1 ตั๋ว |
| `[0] QUIT` | `QUIT` หรือพิมพ์ `0` | ปิดโปรแกรมและสิ้นสุดเซสชัน |

---

## วิธีติดตั้งและรันระบบ (Getting Started)

### ความต้องการของระบบ (Prerequisites)
* Java Development Kit (JDK 17 หรือใหม่กว่า)
* Docker & Docker Desktop *(หากต้องการรันบน Container)*

---

### วิธีที่ 1: รันบนเครื่องโดยตรง (Local Terminal)

#### ขั้นตอนที่ 1: คอมไพล์โค้ด
เปิด Terminal ที่โฟลเดอร์โปรเจกต์ แล้วรันคำสั่ง:
```bash
javac *.java
```

#### ขั้นตอนที่ 2: รัน Server (Terminal ที่ 1)
```bash
java Server
```
*ระบบจะล้างคิวเก่าอัตโนมัติ และแสดงสถานะ `Server is READY!`*

#### ขั้นตอนที่ 3: รัน Client (Terminal ที่ 2 ถึง 6)
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

### วิธีที่ 2: รันผ่าน Docker & Docker Compose

#### ขั้นตอนที่ 1: สตาร์ต Server Container
```bash
docker compose up --build -d
```

#### ขั้นตอนที่ 2: ตรวจสอบ Log ของ Server
```bash
docker logs -f os-server
```

#### ขั้นตอนที่ 3: เปิด Terminal จำลอง Client 1-5 เข้าไปใน Container
เปิด Terminal ใหม่แล้วสั่ง Execute เข้าไปยัง Container:
```bash
# Client 1
docker exec -it os-server java Client 1

# Client 2 (เปิด Terminal อีกหน้าต่าง)
docker exec -it os-server java Client 2

# Client 3 (เปิด Terminal อีกหน้าต่าง)
docker exec -it os-server java Client 3
```

---

## การทดลอง Race Condition vs Synchronization

ในคลาส `ReservationManager.java` สามารถปรับค่าสวิตช์ `useSynchronization` เพื่อเปรียบเทียบผลลัพธ์:

### 1. กรณีปิด Synchronization (`useSynchronization = false`):
* **พฤติกรรม**: หลาย Worker เข้าสู่ `processReservation` พร้อมกันโดยไม่มี Lock กั้น เกิดปัญหา **Check-Then-Act** ระหว่างช่วง `randomDelay()`
* **ผลลัพธ์**: เกิด **Race Condition (Overbooking)** จองสำเร็จเกิน 50 ใบ (เช่น ได้รับการตอบรับสำเร็จ 65 คำขอ จากตั๋ว 50 ใบ) และข้อมูลเจ้าของตั๋วถูกเขียนทับ (Lost Update)

### 2. กรณีเปิด Synchronization (`useSynchronization = true`):
* **พฤติกรรม**: ใช้ **`synchronized (this)`** ควบคุม Critical Section ทำให้ Worker เข้าไปตรวจสอบและบันทึกตั๋วได้ทีละ 1 ตัวแบบ **Atomic Transaction**
* **ผลลัพธ์**: ข้อมูลถูกต้อง 100% ยอดจองสำเร็จรวม **50 ใบถ้วนเสมอ** ไม่มีตั๋วถูกขายซ้ำ และรักษาสิทธิ์ของเจ้าของตั๋วได้สมบูรณ์

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
