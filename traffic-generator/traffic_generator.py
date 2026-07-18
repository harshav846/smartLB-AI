import time
import requests
import random
import sys

TARGET_URL = "http://localhost/api/traffic"

def load_sim():
    print("Starting traffic load simulation...")
    while True:
        try:
            # Randomize traffic rate & payload
            delay = random.uniform(0.1, 1.5)
            headers = {
                "User-Agent": "SmartLB-Traffic-Sim/1.0",
                "X-Tenant-ID": f"tenant-{random.randint(1, 10)}",
                "X-Client-Location": random.choice(["US-East", "EU-West", "AP-South", "US-West"])
            }
            # Simulating GET to local API gateway
            print(f"Sending client request with headers (Tenant: {headers['X-Tenant-ID']})...")
            # In real execution, target might not be immediately responsive, catch connections
            try:
                r = requests.get("http://localhost:8080/health", headers=headers, timeout=2.0)
                print(f"Response: {r.status_code}")
            except requests.RequestException:
                pass
            time.sleep(delay)
        except KeyboardInterrupt:
            print("System interrupt, halting simulator.")
            sys.exit(0)

if __name__ == "__main__":
    load_sim()