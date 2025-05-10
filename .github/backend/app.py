from flask import Flask, request, jsonify
import jwt
import time
import os
from cryptography.hazmat.primitives import serialization

app = Flask(__name__)

@app.route('/')
def root():
    return '🧠 GitHub AI App is Running', 200

@app.route('/webhook', methods=['POST'])
def webhook():
    payload = request.json
    event = request.headers.get('X-GitHub-Event', 'ping')
    
    if event == 'ping':
        return jsonify({'msg': 'pong'})
    
    # Example: Auto-comment or log merged PR
    if event == 'pull_request' and payload['action'] == 'closed' and payload['pull_request']['merged']:
        title = payload['pull_request']['title']
        print(f"🧠 Learning from merged PR: {title}")
        # Here you'd call your AI learning module
        return jsonify({'msg': f'learned from {title}'}), 200

    return jsonify({'msg': 'event ignored'}), 200

def generate_jwt(app_id, private_key_path):
    with open(private_key_path, 'rb') as key_file:
        private_key = serialization.load_pem_private_key(
            key_file.read(),
            password=None
        )
    payload = {
        'iat': int(time.time()),
        'exp': int(time.time()) + (10 * 60),
        'iss': app_id
    }
    encoded_jwt = jwt.encode(payload, private_key, algorithm='RS256')
    return encoded_jwt

if __name__ == '__main__':
    app.run(debug=True, port=5000)
