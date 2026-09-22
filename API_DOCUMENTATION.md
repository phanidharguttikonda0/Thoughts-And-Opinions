# Thoughts & Opinions API Documentation

Welcome to the API Documentation for the Thoughts & Opinions Social Media Application.
This document contains the complete and detailed specification of all APIs available in the system, designed to facilitate the creation of a "Twitter-like" frontend application.

The API Gateway is built on **Spring WebFlux** and runs on port `8080` (`http://localhost:8080`).

## Global Guidelines for Frontend Integration
1. **Authentication:** All protected endpoints MUST include the header `Authorization: Bearer <token>`.
2. **User Identity:** Some endpoints require the `X-User-Id` header (typically injected internally by the API Gateway after JWT validation, but may be required during testing/development).
3. **Pagination:** Endpoints returning lists use cursor-based pagination. You pass `limit` (default 10 or 20) and a `cursor` (derived from the `nextCursor` property of the previous response). If `nextCursor` is empty/null, you've reached the end of the list.

---

## 1. Authentication (`/auth`)
*Used for onboarding and authenticating users.*

### 1.1 Sign Up
- **Endpoint:** `POST /auth/signup`
- **Description:** Register a new user in the system.
- **Request Body:**
  ```json
  {
    "email": "johndoe@example.com",
    "name": "John Doe",
    "username": "johndoe",
    "password": "SecurePassword123"
  }
  ```
- **Success Response (201 Created):**
  ```json
  {
    "success": true,
    "message": "User signed up successfully",
    "data": {
      "token": "eyJhbGciOiJIUzUxMiJ9..."
    }
  }
  ```
- **Error Responses:**
  - `400 Bad Request`: Validation failed (e.g., blank fields, weak password).
  - `409 Conflict`: Username or email is already taken.

### 1.2 Sign In
- **Endpoint:** `POST /auth/signin`
- **Description:** Authenticate an existing user and receive a JWT.
- **Request Body:**
  ```json
  {
    "username": "johndoe",
    "password": "SecurePassword123"
  }
  ```
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "User signed in successfully",
    "data": {
      "token": "eyJhbGciOiJIUzUxMiJ9..."
    }
  }
  ```
- **Error Responses:**
  - `403 Forbidden`: Invalid credentials passed.
  - `404 Not Found`: User does not exist.

---

## 2. Profile (`/profile`)
*Used for fetching user profiles, updating details, and searching.*

### 2.1 Get Profile
- **Endpoint:** `GET /profile/{id}`
- **Description:** Get public profile information for a given user ID.
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "successfully fetched profile data",
    "data": {
      "userId": 1,
      "name": "John Doe",
      "username": "johndoe",
      "bio": "Software Engineer",
      "profilePic": "http://localhost:9000/profile-pictures/uuid.jpg",
      "followersCount": 100,
      "followingCount": 50,
      "joinedAt": "2026-09-21T10:00:00Z"
    }
  }
  ```
- **Error Responses:**
  - `404 Not Found`: If the requested user ID does not exist.

### 2.2 Get Profile Feed
- **Endpoint:** `GET /profile/{id}/feed?limit=10&cursor=`
- **Description:** Fetch the paginated feed of thoughts (tweets) specifically posted by this user (used for the User Profile Page).
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "successfully got profile feed",
    "data": {
      "thoughts": [
        {
          "thoughtId": 10,
          "user": {
            "userId": 1,
            "name": "John Doe",
            "username": "johndoe",
            "profilePicUrl": "..."
          },
          "content": "Hello world!",
          "likesCount": 5,
          "opinionsCount": 2,
          "repostsCount": 1,
          "parentThoughtId": null,
          "createdAt": "2026-09-21T10:00:00Z"
        }
      ],
      "nextCursor": "cursor_string_here"
    }
  }
  ```

### 2.3 Update Profile
- **Endpoint:** `PATCH /profile/`
- **Description:** Update current user's profile details or profile picture.
- **Headers:** `Content-Type: multipart/form-data`
- **Request Parts:**
  - `data` (optional): `application/json` containing `{"name": "...", "username": "...", "bio": "..."}`
  - `file` (optional): Image file (`.jpg`, `.png`).
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Profile updated successfully",
    "data": null
  }
  ```
- **Error Responses:**
  - `409 Conflict`: If the requested new username is already taken.

### 2.4 Search Profiles
- **Endpoint:** `GET /profile/search/{usernamePrefix}`
- **Description:** Search for users by a prefix of their username (used for the search bar / explore page).
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "here are searched users",
    "data": {
      "users": [
        {
          "userId": 1,
          "name": "John Doe",
          "username": "johndoe",
          "profilePicUrl": "..."
        }
      ],
      "nextCursor": null
    }
  }
  ```

---

## 3. Social Graph (`/user`)
*Used for managing followers and followings.*

### 3.1 Follow User
- **Endpoint:** `GET /user/{id}/follow`
- **Description:** Follow the user specified by `{id}`.
- **Success Response (200 OK):**
  ```json
  { "success": true, "message": "sucessfully followed", "data": null }
  ```
- **Error Responses:**
  - `409 Conflict`: Already following this user.
  - `404 Not Found`: Target user does not exist.

### 3.2 Unfollow User
- **Endpoint:** `DELETE /user/{id}/follow`
- **Description:** Unfollow the user specified by `{id}`.
- **Success Response (200 OK):**
  ```json
  { "success": true, "message": "successfully unfollowed", "data": null }
  ```
- **Error Responses:**
  - `404 Not Found`: Target user does not exist, or you aren't currently following them.

### 3.3 Check Following Status
- **Endpoint:** `GET /user/{id}/is-following`
- **Description:** Check if the currently authenticated user is following the target user `{id}` (useful for rendering the "Follow/Unfollow" button state).
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "successfully checked following status",
    "data": true
  }
  ```

### 3.4 Get Followers
- **Endpoint:** `GET /user/{id}/followerslist?limit=20&cursor=`
- **Description:** Get a paginated list of users following the target user `{id}`.
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "here are followers",
    "data": {
      "users": [...],
      "nextCursor": "..."
    }
  }
  ```

### 3.5 Get Following
- **Endpoint:** `GET /user/{id}/followinglist?limit=20&cursor=`
- **Description:** Get a paginated list of users that the target user `{id}` is following.
- **Success Response (200 OK):** Similar to Get Followers.

---

## 4. Thoughts & Opinions (`/thoughts`)
*Used for core interactions with Thoughts (Tweets), Opinions (Replies), Likes, and Reposts.*

### 4.1 Create Thought
- **Endpoint:** `POST /thoughts/`
- **Description:** Publish a new Thought. Can also be used to post an Opinion (Reply) by providing a `parentThoughtId`.
- **Request Body:**
  ```json
  {
    "content": "This is a new thought!",
    "parentThoughtId": null 
  }
  ```
  *(Pass a valid ID for `parentThoughtId` if this is a reply)*
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "successfully created thought",
    "data": {
      "thoughtId": 15,
      "createdAt": "2026-09-21T10:05:00Z"
    }
  }
  ```

### 4.2 Delete Thought
- **Endpoint:** `DELETE /thoughts/{thoughtId}`
- **Description:** Delete a specific thought (requires ownership).
- **Success Response (200 OK):**
  ```json
  { "success": true, "message": "successfully deleted thought", "data": null }
  ```
- **Error Responses:**
  - `404 Not Found`: Thought does not exist.
  - `403 Forbidden`: You do not own this thought.

### 4.3 Get Single Thought
- **Endpoint:** `GET /thoughts/{thoughtId}`
- **Description:** Fetch detailed information for a specific thought (useful for the "Thought Detail" view).
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "got the thought",
    "data": {
      "thoughtId": 15,
      "user": { ... },
      "content": "...",
      "likesCount": 10,
      "opinionsCount": 5,
      "repostsCount": 2,
      "parentThoughtId": null,
      "createdAt": "..."
    }
  }
  ```

### 4.4 Get Opinions (Replies)
- **Endpoint:** `GET /thoughts/opinions/{thoughtId}?limit=20&cursor=`
- **Description:** Get a paginated list of thoughts that are replies to `{thoughtId}`.
- **Success Response (200 OK):** Returns a `ThoughtsFeedDTO` (same structure as Get Profile Feed).

### 4.5 Like Thought
- **Endpoint:** `GET /thoughts/interact/{thoughtId}`
- **Description:** Like a specific thought.
- **Success Response (200 OK):**
  ```json
  { "success": true, "message": "liked thought", "data": null }
  ```
- **Error Responses:**
  - `409 Conflict`: Already liked.

### 4.6 Unlike Thought
- **Endpoint:** `DELETE /thoughts/interact/{thoughtId}`
- **Description:** Remove a like from a specific thought.
- **Success Response (200 OK):**
  ```json
  { "success": true, "message": "unliked thought", "data": null }
  ```

### 4.7 Get Likes List
- **Endpoint:** `GET /thoughts/likes/{thoughtId}?limit=20&cursor=`
- **Description:** Returns paginated list of users who liked the thought.
- **Success Response (200 OK):** Returns a `UsersFeedDTO` object.

### 4.8 Get Reposts List
- **Endpoint:** `GET /thoughts/reposts/{thoughtId}?limit=20&cursor=`
- **Description:** Returns paginated list of users who reposted the thought.
- **Success Response (200 OK):** Returns a `UsersFeedDTO` object.

---

## 5. Home Feed (`/feed`)
*Used to power the user's primary timeline (Home Page).*

### 5.1 Get Timeline Feed
- **Endpoint:** `GET /feed?limit=10&cursor=`
- **Description:** Fetch the personalized, chronological home timeline for the currently authenticated user. This feed contains the latest thoughts from all the users they follow, powered by the backend Timeline Service (fan-out on write architecture via Redis and Kafka).
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Feed retrieved successfully",
    "data": {
      "thoughts": [
        {
          "thoughtId": 10,
          "user": {
            "userId": 2,
            "name": "Followed User",
            "username": "followed",
            "profilePicUrl": "..."
          },
          "content": "This is a thought on your timeline!",
          "likesCount": 0,
          "opinionsCount": 0,
          "repostsCount": 0,
          "parentThoughtId": null,
          "createdAt": "2026-09-22T10:00:00Z"
        }
      ],
      "nextCursor": "cursor_string_here"
    }
  }
  ```
