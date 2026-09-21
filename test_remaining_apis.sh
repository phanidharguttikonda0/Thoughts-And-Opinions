#!/bin/bash
HOST="http://localhost:8080"
# Login as User 1
RESPONSE=$(curl -s -X POST "${HOST}/auth/signin" -H "Content-Type: application/json" -d "{\"username\": \"test_user_v4_1\", \"password\": \"Password123!\"}")
TOKEN=$(echo "$RESPONSE" | jq -r '.data.token')

# Login as User 2
RESPONSE2=$(curl -s -X POST "${HOST}/auth/signin" -H "Content-Type: application/json" -d "{\"username\": \"test_user_v4_2\", \"password\": \"Password123!\"}")
TOKEN2=$(echo "$RESPONSE2" | jq -r '.data.token')

# Get Profile User 1
echo "1. GET /profile/search/test_user_v4_1"
curl -s -X GET "${HOST}/profile/search/test_user_v4_1" -H "Authorization: Bearer ${TOKEN}" | jq -c '.success, .message'

# The ID of User 1
USER_ID_1=$(curl -s -X GET "${HOST}/profile/search/test_user_v4_1" -H "Authorization: Bearer ${TOKEN}" | jq -r '.data.users[0].userId')
USER_ID_2=$(curl -s -X GET "${HOST}/profile/search/test_user_v4_2" -H "Authorization: Bearer ${TOKEN}" | jq -r '.data.users[0].userId')

echo "2. GET /profile/${USER_ID_1}"
curl -s -X GET "${HOST}/profile/${USER_ID_1}" -H "Authorization: Bearer ${TOKEN}" | jq -c '.success, .message'

echo "3. GET /profile/${USER_ID_1}/feed"
curl -s -X GET "${HOST}/profile/${USER_ID_1}/feed?limit=10" -H "Authorization: Bearer ${TOKEN}" | jq -c '.success, .message'

# Thoughts remaining endpoints
# User 1 creates a new thought to delete it and unlike it
THOUGHT_ID=$(curl -s -X POST "${HOST}/thoughts/" -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" -d "{\"content\": \"Thought to delete\"}" | jq -r '.data.thoughtId')

echo "4. DELETE /thoughts/interact/${THOUGHT_ID} (Unlike)"
# Like it first
curl -s -X GET "${HOST}/thoughts/interact/${THOUGHT_ID}" -H "Authorization: Bearer ${TOKEN}" > /dev/null
# Unlike it
curl -s -X DELETE "${HOST}/thoughts/interact/${THOUGHT_ID}" -H "Authorization: Bearer ${TOKEN}" | jq -c '.success, .message'

echo "5. GET /thoughts/reposts/${THOUGHT_ID}"
curl -s -X GET "${HOST}/thoughts/reposts/${THOUGHT_ID}?limit=10" -H "Authorization: Bearer ${TOKEN}" | jq -c '.success, .message'

echo "6. DELETE /thoughts/${THOUGHT_ID}"
curl -s -X DELETE "${HOST}/thoughts/${THOUGHT_ID}" -H "Authorization: Bearer ${TOKEN}" | jq -c '.success, .message'

# Follow remaining endpoints
echo "7. DELETE /user/${USER_ID_2}/follow (Unfollow User 2)"
# Follow first
curl -s -X GET "${HOST}/user/${USER_ID_2}/follow" -H "Authorization: Bearer ${TOKEN}" > /dev/null
# Unfollow
curl -s -X DELETE "${HOST}/user/${USER_ID_2}/follow" -H "Authorization: Bearer ${TOKEN}" | jq -c '.success, .message'

echo "8. GET /user/${USER_ID_1}/followinglist"
curl -s -X GET "${HOST}/user/${USER_ID_1}/followinglist?limit=10" -H "Authorization: Bearer ${TOKEN}" | jq -c '.success, .message'

echo "All remaining APIs tested."
