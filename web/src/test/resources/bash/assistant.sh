#!/bin/bash

# Define the API key and endpoint
OPENAI_API_KEY="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"
ENDPOINT="https://api.openai.com/v1/threads/runs"

# Perform the API request
curl $ENDPOINT\
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -H "Content-Type: application/json" \
  -H "OpenAI-Beta: assistants=v2" \
  -d '{
    "assistant_id": "asst_Y7NTF6GZ906pAsqh9t9Aac6G",
    "thread": {
        "messages": [
        {
            "role": "user",
            "content": [
            {
                "type": "text",
                "text": "Analyse the image according to your instructions"
            },
            {
                "type": "image_url",
                "image_url": {
                "url": "https://volee-avatars-dev-us.s3.amazonaws.com/ai-counting/Check.jpg",
                "detail": "auto"
                }
            }]
        }]
    }
  }'