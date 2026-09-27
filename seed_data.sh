#!/bin/bash

# Configuration
API_URL="http://localhost:8080"
NUM_USERS=5
POSTS_PER_USER=10

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Starting Data Seeding Script ===${NC}\n"

# Helper function to extract user_id from JWT token
get_user_id_from_token() {
    local token=$1
    echo $token | cut -d'.' -f2 | base64 -d 2>/dev/null | jq -r '.user_id'
}

declare -a USER_TOKENS
declare -a USER_IDS
declare -a USERNAMES

echo "1. Creating ${NUM_USERS} users..."
for i in $(seq 1 $NUM_USERS); do
    USERNAME="user${i}_$RANDOM"
    EMAIL="user${i}_$RANDOM@example.com"
    NAME="User ${i}"
    PASSWORD="password123"
    
    echo "Creating $USERNAME..."
    RES=$(curl -s -X POST "$API_URL/auth/signup" -H "Content-Type: application/json" -d "{\"username\": \"$USERNAME\", \"email\": \"$EMAIL\", \"name\": \"$NAME\", \"password\": \"$PASSWORD\"}")
    
    TOKEN=$(echo $RES | jq -r '.data.token')
    ID=$(get_user_id_from_token $TOKEN)
    
    USER_TOKENS[$i]=$TOKEN
    USER_IDS[$i]=$ID
    USERNAMES[$i]=$USERNAME
    
    echo -e "${GREEN}Created ${USERNAME} (ID: $ID) with password: ${PASSWORD}${NC}"
done
echo ""

echo "2. Making all users follow each other..."
for i in $(seq 1 $NUM_USERS); do
    for j in $(seq 1 $NUM_USERS); do
        if [ $i -ne $j ]; then
            FOLLOWER_TOKEN=${USER_TOKENS[$i]}
            FOLLOWER_ID=${USER_IDS[$i]}
            FOLLOWEE_ID=${USER_IDS[$j]}
            FOLLOWEE_USERNAME=${USERNAMES[$j]}
            
            curl -s -X GET "$API_URL/user/$FOLLOWEE_ID/follow" -H "Authorization: Bearer $FOLLOWER_TOKEN" -H "X-User-Id: $FOLLOWER_ID" > /dev/null
            echo -e "${GREEN}${USERNAMES[$i]} is now following ${FOLLOWEE_USERNAME}${NC}"
        fi
    done
done
echo ""

# Give Identity Service a moment to process follows
sleep 2

echo "3. Creating ${POSTS_PER_USER} thoughts for each user..."
for i in $(seq 1 $NUM_USERS); do
    AUTHOR_TOKEN=${USER_TOKENS[$i]}
    AUTHOR_ID=${USER_IDS[$i]}
    AUTHOR_USERNAME=${USERNAMES[$i]}
    
    for p in $(seq 1 $POSTS_PER_USER); do
        THOUGHT_CONTENT="This is thought number ${p} from ${AUTHOR_USERNAME}! Learning so much building this platform."
        RES=$(curl -s -X POST "$API_URL/thoughts/" -H "Authorization: Bearer $AUTHOR_TOKEN" -H "Content-Type: application/json" -H "X-User-Id: $AUTHOR_ID" -d "{\"content\": \"$THOUGHT_CONTENT\", \"parentThoughtId\": null}")
        echo -e "${GREEN}${AUTHOR_USERNAME} posted a thought.${NC}"
        sleep 0.5 # Small delay to ensure sequence
    done
done
echo ""

echo -e "${BLUE}=== Data Seeding Complete ===${NC}"
echo "You can log in with any of the following users (Password: password123):"
for i in $(seq 1 $NUM_USERS); do
    echo "- ${USERNAMES[$i]}"
done
