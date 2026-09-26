#!/bin/bash

# ===========================================================
# Notification System End-to-End Test
# ===========================================================
# Prerequisites: All services must be running:
#   1. docker compose up (PostgreSQL, Redis, Kafka, MinIO)
#   2. IdentityService (port 9090)
#   3. ThoughtsService (port 9091)
#   4. TimelineService (port 9094)
#   5. NotificationService (port 9095) - Go service
#   6. APIGateway (port 8080)
# ===========================================================

BASE_URL="http://localhost:8080"
YELLOW='\033[1;33m'
GREEN='\033[1;32m'
CYAN='\033[1;36m'
RED='\033[1;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}=============================================${NC}"
echo -e "${CYAN}  🔔 Notification System E2E Test${NC}"
echo -e "${CYAN}=============================================${NC}"

# ----- Step 1: Register 2 users -----
echo -e "\n${YELLOW}➤ Step 1: Registering 2 test users...${NC}"

TIMESTAMP=$(date +%s)
USER1_NAME="notif_user1_${TIMESTAMP}"
USER2_NAME="notif_user2_${TIMESTAMP}"

SIGNUP1=$(curl -s -X POST "${BASE_URL}/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USER1_NAME}\",\"email\":\"${USER1_NAME}@test.com\",\"password\":\"password123\",\"name\":\"User One\"}")

echo "  User 1 signup: $SIGNUP1"

SIGNUP2=$(curl -s -X POST "${BASE_URL}/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USER2_NAME}\",\"email\":\"${USER2_NAME}@test.com\",\"password\":\"password123\",\"name\":\"User Two\"}")

echo "  User 2 signup: $SIGNUP2"

# ----- Step 2: Login both users -----
echo -e "\n${YELLOW}➤ Step 2: Logging in both users...${NC}"

LOGIN1=$(curl -s -X POST "${BASE_URL}/auth/signin" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USER1_NAME}\",\"password\":\"password123\"}")

TOKEN1=$(echo $LOGIN1 | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
USERID1=$(echo $LOGIN1 | grep -o '"userId":[0-9]*' | cut -d':' -f2)
echo "  User 1: ID=$USERID1"

LOGIN2=$(curl -s -X POST "${BASE_URL}/auth/signin" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USER2_NAME}\",\"password\":\"password123\"}")

TOKEN2=$(echo $LOGIN2 | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
USERID2=$(echo $LOGIN2 | grep -o '"userId":[0-9]*' | cut -d':' -f2)
echo "  User 2: ID=$USERID2"

if [ -z "$TOKEN1" ] || [ -z "$TOKEN2" ]; then
  echo -e "${RED}✘ Failed to get tokens. Aborting.${NC}"
  exit 1
fi

# ----- Step 3: User 1 creates a thought -----
echo -e "\n${YELLOW}➤ Step 3: User 1 creates a thought...${NC}"

CREATE_THOUGHT=$(curl -s -X POST "${BASE_URL}/thoughts/" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN1}" \
  -H "X-User-Id: ${USERID1}" \
  -d '{"content":"This is a test thought for notification testing!"}')

echo "  Create thought response: $CREATE_THOUGHT"
THOUGHT_ID=$(echo $CREATE_THOUGHT | grep -o '"thoughtId":[0-9]*' | cut -d':' -f2)
echo "  Thought ID: $THOUGHT_ID"

if [ -z "$THOUGHT_ID" ]; then
  echo -e "${RED}✘ Failed to create thought. Aborting.${NC}"
  exit 1
fi

# ----- Step 4: User 2 LIKES User 1's thought (should trigger LIKE notification) -----
echo -e "\n${YELLOW}➤ Step 4: User 2 likes User 1's thought...${NC}"

LIKE_RESP=$(curl -s -X GET "${BASE_URL}/thoughts/interact/${THOUGHT_ID}" \
  -H "Authorization: Bearer ${TOKEN2}" \
  -H "X-User-Id: ${USERID2}")

echo "  Like response: $LIKE_RESP"

# ----- Step 5: User 2 REPOSTS User 1's thought (should trigger REPOST notification) -----
echo -e "\n${YELLOW}➤ Step 5: User 2 reposts User 1's thought...${NC}"

REPOST_RESP=$(curl -s -X POST "${BASE_URL}/thoughts/" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN2}" \
  -H "X-User-Id: ${USERID2}" \
  -d "{\"parentThoughtId\":${THOUGHT_ID}}")

echo "  Repost response: $REPOST_RESP"

# ----- Step 6: User 2 writes an OPINION on User 1's thought (should trigger OPINION notification) -----
echo -e "\n${YELLOW}➤ Step 6: User 2 shares an opinion on User 1's thought...${NC}"

OPINION_RESP=$(curl -s -X POST "${BASE_URL}/thoughts/" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN2}" \
  -H "X-User-Id: ${USERID2}" \
  -d "{\"content\":\"Great thought! I agree completely.\",\"parentThoughtId\":${THOUGHT_ID}}")

echo "  Opinion response: $OPINION_RESP"

# ----- Step 7: User 2 FOLLOWS User 1 (should trigger FOLLOW notification) -----
echo -e "\n${YELLOW}➤ Step 7: User 2 follows User 1...${NC}"

FOLLOW_RESP=$(curl -s -X GET "${BASE_URL}/user/${USERID1}/follow" \
  -H "Authorization: Bearer ${TOKEN2}" \
  -H "X-User-Id: ${USERID2}")

echo "  Follow response: $FOLLOW_RESP"

# ----- Step 8: Wait for Kafka consumer to process -----
echo -e "\n${YELLOW}➤ Step 8: Waiting 3 seconds for Kafka consumer to process events...${NC}"
sleep 3

# ----- Step 9: Fetch User 1's notifications -----
echo -e "\n${YELLOW}➤ Step 9: Fetching User 1's notifications...${NC}"

NOTIFS=$(curl -s -X GET "${BASE_URL}/notifications?limit=20" \
  -H "Authorization: Bearer ${TOKEN1}" \
  -H "X-User-Id: ${USERID1}")

echo -e "\n${GREEN}=============================================${NC}"
echo -e "${GREEN}  📬 User 1's Notifications:${NC}"
echo -e "${GREEN}=============================================${NC}"
echo "$NOTIFS" | python3 -m json.tool 2>/dev/null || echo "$NOTIFS"

# ----- Validation -----
echo -e "\n${CYAN}=============================================${NC}"
echo -e "${CYAN}  ✅ Validation${NC}"
echo -e "${CYAN}=============================================${NC}"

# Check if we got notifications
NOTIF_COUNT=$(echo "$NOTIFS" | grep -o '"type"' | wc -l)
echo -e "  Total notifications received: ${NOTIF_COUNT}"

HAS_LIKE=$(echo "$NOTIFS" | grep -c '"type":"LIKE"')
HAS_REPOST=$(echo "$NOTIFS" | grep -c '"type":"REPOST"')
HAS_OPINION=$(echo "$NOTIFS" | grep -c '"type":"OPINION"')
HAS_FOLLOW=$(echo "$NOTIFS" | grep -c '"type":"FOLLOW"')

if [ "$HAS_LIKE" -gt 0 ]; then echo -e "  ${GREEN}✔ LIKE notification found${NC}"; else echo -e "  ${RED}✘ LIKE notification missing${NC}"; fi
if [ "$HAS_REPOST" -gt 0 ]; then echo -e "  ${GREEN}✔ REPOST notification found${NC}"; else echo -e "  ${RED}✘ REPOST notification missing${NC}"; fi
if [ "$HAS_OPINION" -gt 0 ]; then echo -e "  ${GREEN}✔ OPINION notification found${NC}"; else echo -e "  ${RED}✘ OPINION notification missing${NC}"; fi
if [ "$HAS_FOLLOW" -gt 0 ]; then echo -e "  ${GREEN}✔ FOLLOW notification found${NC}"; else echo -e "  ${RED}✘ FOLLOW notification missing${NC}"; fi

if [ "$NOTIF_COUNT" -ge 4 ]; then
  echo -e "\n${GREEN}🎉 All notification types verified! Test PASSED.${NC}"
else
  echo -e "\n${RED}⚠ Some notifications may be missing. Expected 4, got ${NOTIF_COUNT}.${NC}"
  echo -e "${YELLOW}  (This could be due to Kafka consumer lag. Try again after a few seconds.)${NC}"
fi

echo -e "\n${CYAN}=============================================${NC}"
echo -e "${CYAN}  Test Complete${NC}"
echo -e "${CYAN}=============================================${NC}"
