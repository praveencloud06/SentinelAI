import boto3
import json
import os
from .base import LLMProvider

class BedrockProvider(LLMProvider):
    def __init__(self, model=None):
        # Load config from the common providers_config.json file
        config_path = os.path.join(os.path.dirname(__file__), 'providers_config.json')
        with open(config_path, 'r') as f:
            config = json.load(f)
        bedrock_config = config.get('bedrock', {})
        aws_access_key_id = bedrock_config.get('aws_access_key_id')
        aws_secret_access_key = bedrock_config.get('aws_secret_access_key')
        region = bedrock_config.get('region', 'us-east-1')
        model_name = model or bedrock_config.get('model', 'anthropic.claude-v2')
        self.model = model_name
        self.client = boto3.client(
            'bedrock-runtime',
            aws_access_key_id=aws_access_key_id,
            aws_secret_access_key=aws_secret_access_key,
            region_name=region
        )

    def analyze(self, logs: str) -> dict:
        prompt = f"Analyze these logs and provide root cause: {logs}"
        body = {
            "modelId": self.model,
            "contentType": "application/json",
            "accept": "application/json",
            "inputText": prompt
        }
        response = self.client.invoke_model(
            modelId=self.model,
            contentType="application/json",
            accept="application/json",
            body=json.dumps({"prompt": prompt})
        )
        result = json.loads(response['body'].read())
        return {"provider": "bedrock", "result": result.get("completion", "")}
