#!/bin/bash

# Configuration
API_URL="http://localhost:8080"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Starting Timeline Service End-to-End Test ===${NC}\n"

# Helper function to extract user_id from JWT token
get_user_id_from_token() {
    local token=$1
    # Split the JWT into its 3 parts, take the payload (2nd part), base64 decode it, and extract user_id
    echo $token | cut -d'.' -f2 | base64 -d 2>/dev/null | jq -r '.user_id'
}

# 1. Create Users
echo "1. Creating users (1 Author, 3 Followers)..."

AUTHOR_RES=$(curl -s -X POST "$API_URL/auth/signup" -H "Content-Type: application/json" -d "{\"username\": \"author_user_$RANDOM\", \"email\": \"author_$RANDOM@example.com\", \"name\": \"Author\", \"password\": \"password123\"}")
AUTHOR_TOKEN=$(echo $AUTHOR_RES | jq -r '.data.token')
AUTHOR_ID=$(get_user_id_from_token $AUTHOR_TOKEN)
echo -e "${GREEN}Author created with ID: $AUTHOR_ID${NC}"

F1_RES=$(curl -s -X POST "$API_URL/auth/signup" -H "Content-Type: application/json" -d "{\"username\": \"follower_1_$RANDOM\", \"email\": \"follower1_$RANDOM@example.com\", \"name\": \"Follower 1\", \"password\": \"password123\"}")
F1_TOKEN=$(echo $F1_RES | jq -r '.data.token')
F1_ID=$(get_user_id_from_token $F1_TOKEN)
echo -e "${GREEN}Follower 1 created with ID: $F1_ID${NC}"

F2_RES=$(curl -s -X POST "$API_URL/auth/signup" -H "Content-Type: application/json" -d "{\"username\": \"follower_2_$RANDOM\", \"email\": \"follower2_$RANDOM@example.com\", \"name\": \"Follower 2\", \"password\": \"password123\"}")
F2_TOKEN=$(echo $F2_RES | jq -r '.data.token')
F2_ID=$(get_user_id_from_token $F2_TOKEN)
echo -e "${GREEN}Follower 2 created with ID: $F2_ID${NC}"

F3_RES=$(curl -s -X POST "$API_URL/auth/signup" -H "Content-Type: application/json" -d "{\"username\": \"follower_3_$RANDOM\", \"email\": \"follower3_$RANDOM@example.com\", \"name\": \"Follower 3\", \"password\": \"password123\"}")
F3_TOKEN=$(echo $F3_RES | jq -r '.data.token')
F3_ID=$(get_user_id_from_token $F3_TOKEN)
echo -e "${GREEN}Follower 3 created with ID: $F3_ID${NC}\n"

# 2. Followers Follow the Author
echo "2. Followers are following the Author..."
curl -s -X GET "$API_URL/user/$AUTHOR_ID/follow" -H "Authorization: Bearer $F1_TOKEN" -H "X-User-Id: $F1_ID" > /dev/null
echo -e "${GREEN}Follower 1 followed Author${NC}"

curl -s -X GET "$API_URL/user/$AUTHOR_ID/follow" -H "Authorization: Bearer $F2_TOKEN" -H "X-User-Id: $F2_ID" > /dev/null
echo -e "${GREEN}Follower 2 followed Author${NC}"

curl -s -X GET "$API_URL/user/$AUTHOR_ID/follow" -H "Authorization: Bearer $F3_TOKEN" -H "X-User-Id: $F3_ID" > /dev/null
echo -e "${GREEN}Follower 3 followed Author${NC}\n"

# Give Identity Service a moment to process follows (in case of any latency)
sleep 1

# 3. Author Creates a Thought
echo "3. Author is posting a new thought..."
THOUGHT_RES=$(curl -s -X POST "$API_URL/thoughts/" -H "Authorization: Bearer $AUTHOR_TOKEN" -H "Content-Type: application/json" -H "X-User-Id: $AUTHOR_ID" -d '{"content": "This is a brand new thought for my followers to see!", "parentThoughtId": null}')
THOUGHT_ID=$(echo $THOUGHT_RES | jq -r '.data.thoughtId')
echo -e "${GREEN}Thought created with ID: $THOUGHT_ID${NC}\n"

# Give Timeline Service a moment to consume Kafka event and update Redis
echo "Waiting for Kafka event to be processed by Timeline Service (2 seconds)..."
sleep 2
echo ""

# 4. Check Feeds
echo "4. Checking Follower Feeds..."

echo -e "${BLUE}Follower 1 Feed:${NC}"
curl -s -X GET "$API_URL/feed" -H "Authorization: Bearer $F1_TOKEN" -H "X-User-Id: $F1_ID" | jq '.data'

echo -e "${BLUE}Follower 2 Feed:${NC}"
curl -s -X GET "$API_URL/feed" -H "Authorization: Bearer $F2_TOKEN" -H "X-User-Id: $F2_ID" | jq '.data'

echo -e "${BLUE}Follower 3 Feed:${NC}"
curl -s -X GET "$API_URL/feed" -H "Authorization: Bearer $F3_TOKEN" -H "X-User-Id: $F3_ID" | jq '.data'

echo -e "\n${BLUE}=== Test Complete ===${NC}"
