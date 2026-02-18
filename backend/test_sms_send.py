#!/usr/bin/env python3
"""Test script to send SMS via the gateway backend."""

import httpx
import sys

BASE_URL = "http://localhost:7777"

async def send_sms(phone_number: str, message: str, max_retries: int = 3):
    """Send an SMS via the gateway."""
    async with httpx.AsyncClient() as client:
        try:
            response = await client.post(
                f"{BASE_URL}/api/v1/sms/send",
                json={
                    "to": phone_number,
                    "body": message,
                    "max_retries": max_retries,
                },
                timeout=10,
            )

            print(f"Status Code: {response.status_code}")
            print(f"Response: {response.json()}")

            if response.status_code == 202:
                job = response.json()
                print(f"\n✅ SMS Job Created!")
                print(f"   Job ID: {job['job_id']}")
                print(f"   To: {job['to']}")
                print(f"   Status: {job['status']}")
                return True
            else:
                print(f"\n❌ Failed to send SMS")
                return False

        except Exception as e:
            print(f"❌ Error: {e}")
            return False

if __name__ == "__main__":
    import asyncio

    # Test phone number and message
    if len(sys.argv) > 2:
        phone = sys.argv[1]
        msg = " ".join(sys.argv[2:])
    else:
        # Default test
        phone = "+1234567890"
        msg = "Hello from SMS Gateway! 🚀"

    print(f"📱 Sending SMS to {phone}")
    print(f"📝 Message: {msg}")
    print(f"🌐 Backend: {BASE_URL}\n")

    result = asyncio.run(send_sms(phone, msg))
    sys.exit(0 if result else 1)
