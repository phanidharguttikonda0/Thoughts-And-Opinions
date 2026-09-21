#!/bin/bash

# Configuration
HOST="http://localhost:8080"
USER_PREFIX="test_user_v4"
PASSWORD="Password123!"

echo "====================================="
echo "  Starting Social Media Simulation   "
echo "====================================="

# Arrays to store generated data
declare -a TOKENS
declare -a USER_IDS

# 1. Register and Login 10 Users
echo -e "\n[1] Registering and Logging in 10 Users..."
for i in {1..10}; do
    USERNAME="${USER_PREFIX}_${i}"
    EMAIL="${USERNAME}@example.com"
    
    # Register
    curl -s -X POST "${HOST}/auth/signup" \
         -H "Content-Type: application/json" \
         -d "{\"email\": \"${EMAIL}\", \"username\": \"${USERNAME}\", \"password\": \"${PASSWORD}\"}" > /dev/null

    # Login and extract token
    RESPONSE=$(curl -s -X POST "${HOST}/auth/signin" \
         -H "Content-Type: application/json" \
         -d "{\"username\": \"${USERNAME}\", \"password\": \"${PASSWORD}\"}")
         
    TOKEN=$(echo "$RESPONSE" | jq -r '.data.token')
    
    if [ "$TOKEN" != "null" ] && [ -n "$TOKEN" ]; then
        TOKENS[$i]=$TOKEN
        
        # Get Profile to extract USER ID
        PROFILE_RESPONSE=$(curl -s -X GET "${HOST}/profile/search/${USERNAME}" -H "Authorization: Bearer ${TOKEN}")
        USER_ID=$(echo "$PROFILE_RESPONSE" | jq -r '.data.users[0].userId')
        USER_IDS[$i]=$USER_ID
        
        echo "✅ Registered User $i (ID: $USER_ID) - ${USERNAME}"
    else
        echo "❌ Failed to register User $i"
    fi
done

# Check if User 1 succeeded
if [ -z "${TOKENS[1]}" ]; then
    echo "User 1 failed to login, aborting simulation."
    exit 1
fi

# 2. User 1 Creates a Thought
echo -e "\n[2] User 1 is creating a Thought..."
THOUGHT_RESPONSE=$(curl -s -X POST "${HOST}/thoughts/" \
     -H "Authorization: Bearer ${TOKENS[1]}" \
     -H "Content-Type: application/json" \
     -d "{\"content\": \"This is my first viral thought on the platform! Welcome everyone!\"}")

THOUGHT_ID=$(echo "$THOUGHT_RESPONSE" | jq -r '.data.thoughtId')

if [ "$THOUGHT_ID" == "null" ] || [ -z "$THOUGHT_ID" ]; then
    echo "❌ Failed to create thought. Response: $THOUGHT_RESPONSE"
    exit 1
fi
echo "✅ Thought created! (ID: ${THOUGHT_ID})"

# 3. Users 2-10 Like the Thought
echo -e "\n[3] Users 2 to 10 are Liking the thought..."
for i in {2..10}; do
    curl -s -X GET "${HOST}/thoughts/interact/${THOUGHT_ID}" \
         -H "Authorization: Bearer ${TOKENS[$i]}" > /dev/null
    echo "   ❤️ User $i liked the thought."
done

# 4. Users 2-5 Write Opinions (Replies)
echo -e "\n[4] Users 2 to 5 are writing Opinions..."
for i in {2..5}; do
    curl -s -X POST "${HOST}/thoughts/" \
         -H "Authorization: Bearer ${TOKENS[$i]}" \
         -H "Content-Type: application/json" \
         -d "{\"content\": \"Great thought! I totally agree with you User 1! - From User $i\", \"parentThoughtId\": ${THOUGHT_ID}}" > /dev/null
    echo "   💬 User $i replied to the thought."
done

# 5. Users 6-10 Follow User 1
echo -e "\n[5] Users 6 to 10 are following User 1..."
for i in {6..10}; do
    TARGET_ID=${USER_IDS[1]}
    curl -s -X GET "${HOST}/user/${TARGET_ID}/follow" \
         -H "Authorization: Bearer ${TOKENS[$i]}" > /dev/null
    echo "   🤝 User $i started following User 1."
done

# 6. Fetch Feed & Results
echo -e "\n[6] Fetching Final Results..."

LIKES=$(curl -s -X GET "${HOST}/thoughts/likes/${THOUGHT_ID}?limit=10" -H "Authorization: Bearer ${TOKENS[1]}")
OPINIONS=$(curl -s -X GET "${HOST}/thoughts/opinions/${THOUGHT_ID}?limit=10" -H "Authorization: Bearer ${TOKENS[1]}")
FOLLOWERS=$(curl -s -X GET "${HOST}/user/${USER_IDS[1]}/followerslist?limit=10" -H "Authorization: Bearer ${TOKENS[1]}")

echo -e "\n--- Likes on Thought ${THOUGHT_ID} ---"
echo "$LIKES" | jq -r '.data.users[].username'

echo -e "\n--- Opinions (Replies) on Thought ${THOUGHT_ID} ---"
echo "$OPINIONS" | jq -r '.data.thoughts[].content'

echo -e "\n--- Followers of User 1 ---"
echo "$FOLLOWERS" | jq -r '.data.users[].username'

echo -e "\n====================================="
echo "  Simulation Completed Successfully!   "
echo "====================================="
