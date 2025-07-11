# 🩺Secure IoMT Solution for Healthcare

**Master's Degree Project**

---
This project presents a secure and standards-compliant IoT architecture designed for real-time medical monitoring. It demonstrates how physiological data collected from medical sensors can be securely transmitted, processed, stored, and visualized using a multi-layered service-oriented architecture.

---
⚠️ **Note**: This prototype is intended strictly for academic and experimental use. It is **not suitable for clinical deployment**.

---

## Key Features

- 🌡️ Data collection via Raspberry Pi and medical sensors (ECG, pulse oximeter, IR temperature)
- 🔒 End-to-end encryption with **AES-256**, **CBOR + COSE Encrypt**, and **mutual TLS**
- 🧵 Secure message passing via **MQTT** and **RabbitMQ**
- 🏥 Compliance with healthcare standards: **SDC**, **SDPi**, **HL7 FHIR**, **IEEE 11073**
- 📱 Real-time data streaming to an **Android application**
- 🗃️ MongoDB for structured **FHIR** resource storage
- 🐳 Docker-based deployment 

---

## System Architecture

### 1. Data Collection

- **Raspberry Pi** equipped with:
  - MLX90614 (temperature sensor)
  - AD8232 (ECG sensor)
  - MAX30102 (pulse oximeter)

- Data flow:
  - Sensor data is read via Python script.
  - Encoded with **CBOR**, encrypted using **COSE Encrypt + AES-256**.
  - Published to **Mosquitto (MQTT)** over **mutual TLS (mTLS)**.

### 2. Secure Messaging and Transformation

- **MQTT broker** receives encrypted data.
- An **SDC adapter**:
  - Decrypts and deserializes messages.
  - Converts them into **FHIR Observation** resources.
- Transformed data is published to **RabbitMQ** queues.

### 3. Storage & Backend

- FHIR resources are stored in **MongoDB**.
- A **Micronaut-based backend** exposes REST APIs.

### 4. Android Application

The Android app acts as a **RabbitMQ subscriber** and provides a secure, user-friendly interface for clinicians. The app also supports PDF report generation based on historical data saved in the database, for a selected time interval.

---
## Highlights

- **Mutual TLS (mTLS)** for all MQTT and RabbitMQ communication
- **AES-256 encryption** using **COSE Encrypt**
- Efficient data serialization via **CBOR**
- **X.509 certificate-based device whitelisting**
- Compliance with healthcare interoperability standards:
  - 🧬 **HL7 FHIR**
  - 📡 **IEEE 11073 SDC**
  - 🔗 **SDPi** (Service-oriented Device Point-of-care Interoperability, by HL7 & IHE)
---

## 🐳 Deployment

To deploy the full infrastructure using Docker:

```bash
# Stop any existing deployment
docker stack rm secure-stack

# Build updated Docker images
docker build -t sdc-adapter:latest ./sdc-adapter
docker build -t fhir-backend:latest ./fhir-backend

# Launch the full stack
docker stack deploy -c docker-compose.yml secure-stack
