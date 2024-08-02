#!/bin/bash

# Define the API key and endpoint
OPENAI_API_KEY="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"
ENDPOINT="https://api.openai.com/v1/chat/completions"

# Perform the API request
curl $ENDPOINT\
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $OPENAI_API_KEY" \
  -d '{
    "model": "gpt-4o-mini",
    "messages": [
      {
        "role": "user",
        "content": [
          {
            "type": "text",
            "text": "Extract Amount, currency, purpose/place, category, and date if present, no additional comments please"
          },
          {
            "type": "image_url",
            "image_url": {
              "url": "https://volee-avatars-dev-us.s3.amazonaws.com/ai-counting/Check.jpg",
              "detail": "auto"
            }
          }
        ]
      }
    ],
    "max_tokens": 300
  }'