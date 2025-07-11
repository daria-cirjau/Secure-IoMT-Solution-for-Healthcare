import ssl
import time
import board
import busio
import json
import random
import paho.mqtt.client as mqtt
import sys
import os

from pycose.messages import Enc0Message
from pycose.keys import CoseKey
from pycose.algorithms import A256GCM
import cbor2

from pycose.headers import Algorithm, IV
from pycose.keys.keyparam import KpKty, SymKpK, KpKeyOps
from pycose.keys.keytype import KtySymmetric
from pycose.keys.keyops import EncryptOp, DecryptOp

from Crypto.Cipher import AES
from Crypto.Util.Padding import pad
from Crypto.Random import get_random_bytes

from adafruit_ads1x15.ads1015 import ADS1015
from adafruit_ads1x15.analog_in import AnalogIn
import adafruit_mlx90614

sys.path.append(os.path.join(os.path.dirname(__file__), 'max30102'))
from heartrate_monitor import HeartRateMonitor

hrm = HeartRateMonitor(print_raw=False, print_result=False)
hrm.start_sensor()

CA_CERT = "/mqtt_certs/certificate-authority.crt"
CLIENT_CERT = "/mqtt_certs/publisher.crt"
CLIENT_KEY = "/mqtt_certs/publisher.key"
KEY_PATH = "/mqtt_certs/aes.key"


def load_aes_key():
    with open(KEY_PATH, "rb") as f:
        return f.read()


AES_KEY = load_aes_key()
client = None


def encrypt_cbor(payload_dict):
    cbor_data = cbor2.dumps(payload_dict)
    iv = get_random_bytes(12)

    msg = Enc0Message(
        phdr={Algorithm: A256GCM},
        uhdr={IV: iv},
        payload=cbor_data
    )

    cose_key = {
        KpKty: KtySymmetric,
        SymKpK: AES_KEY,
        KpKeyOps: [EncryptOp, DecryptOp]
    }

    cose_key = CoseKey.from_dict(cose_key)
    msg.key = cose_key
    encoded = msg.encode()

    return encoded


def connect_to_mqtt_broker():
    global client
    broker = "daria-laptop.local"
    port = 8883

    client = mqtt.Client()
    client.tls_set(
        ca_certs=CA_CERT,
        certfile=CLIENT_CERT,
        keyfile=CLIENT_KEY,
       )
    client.enable_logger()
    client.connect(broker, port)


def record_temperature():
    try:
        i2c = busio.I2C(board.SCL, board.SDA)
        mlx = adafruit_mlx90614.MLX90614(i2c)
        target_temp = round(mlx.object_temperature, 2)
    except Exception as e:
        target_temp = 0
        print(f"Error in recording temperature: {e}")

    payload = {
        "device_oid": "1.3.6.1.4.1.99999.1.2",
        "metrics": {
            "temperature": target_temp
        }
    }
    encrypted = encrypt_cbor(payload)
    client.publish("sensors/temperature", encrypted, qos=1)
    print(f"Target Temperature: {target_temp} °C")


def record_ekg():
    try:
        i2c = busio.I2C(board.SCL, board.SDA)
        ads = ADS1015(i2c)
        ads.gain = 1
        ekg = AnalogIn(ads, 0)
        ekg_voltage = round(ekg.voltage, 4)
    except Exception as e:
        ekg_voltage = 0
        print(f"Error in recording EKG: {e}")

    payload = {
        "device_oid": "1.3.6.1.4.1.99999.1.1",
        "metrics": {
            "ekg": ekg_voltage
        }
    }
    encrypted = encrypt_cbor(payload)
    client.publish("sensors/ekg", encrypted, qos=1)
    print(f"EKG Voltage: {ekg_voltage} V")


def record_pulse_oximetry():
    try:
        hrm.start_sensor()
        time.sleep(3)
        hrm.stop_sensor()

        hr = int(hrm.bpm)
        spo2 = float(hrm.spo2)

    except Exception as e:
        hr = 0
        spo2 = 0
        print(f"Error in recording pulse oximetry: {e}")

    payload = {
        "device_oid": "1.3.6.1.4.1.99999.1.3",
        "metrics": {
            "pulse": hr,
            "oxygen": spo2
        }
    }
    encrypted = encrypt_cbor(payload)
    client.publish("sensors/pulse_oximetry", encrypted, qos=1)
    print(f"Heart Rate: {hr} BPM. SpO2: {spo2}%")


def main():
    connect_to_mqtt_broker()
    hrm = HeartRateMonitor(print_raw=False, print_result=False)
    hrm.start_sensor()
    time.sleep(2)

    try:
        while True:
            record_temperature()
            time.sleep(1)
            record_ekg()
            time.sleep(1)
            record_pulse_oximetry()
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nStopped.")
        client.disconnect()
        hrm.stop_sensor()


main()
