# Thoughts & Opinions API Documentation

Welcome to the API Documentation for the Thoughts & Opinions Social Media Application.

The API Gateway is built on **Spring WebFlux** and runs on port `8080`.
All requests to protected endpoints MUST include the `Authorization: Bearer <token>` header.
Some endpoints (like profile updates) will also automatically receive the `X-User-Id` header internally after JWT validation.

---

## 1. Authentication (`/auth`)

### 1.1 Sign Up
- **Endpoint:** `POST /auth/signup`
- **Description:** Register a new user in the system.
- **Request Body:**
  ```json
  {
    "email": "johndoe@example.com",
    "username": "johndoe",
    "password": "SecurePassword123"
  }
  ```
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "User registered successfully",
    "data": {
      "token": "eyJhbGciOiJIUzUxMiJ9..."
    }
  }
  ```
- **Error Consequences:**
  - `409 Conflict`: If the username is already taken. (Mapped from `Status.ALREADY_EXISTS`)

### 1.2 Sign In
- **Endpoint:** `POST /auth/signin`
- **Description:** Authenticate and receive a JWT.
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
    "message": "Login successful",
    "data": {
      "token": "eyJhbGciOiJIUzUxMiJ9..."
    }
  }
  ```
- **Error Consequences:**
  - `403 Forbidden`: Invalid credentials passed. (Mapped from `Status.PERMISSION_DENIED`)
  - `404 Not Found`: User does not exist. (Mapped from `Status.NOT_FOUND`)

---

## 2. Profile (`/profile`)

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
- **Error Consequences:**
  - `404 Not Found`: If the requested user ID does not exist.

### 2.2 Get Profile Feed
- **Endpoint:** `GET /profile/{id}/feed?limit=10&cursor=`
- **Description:** Fetch the paginated feed of thoughts specifically posted by this user.
- **Query Params:**
  - `limit` (optional): Defaults to 10.
  - `cursor` (optional): Pass the `nextCursor` from a previous response to paginate.
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "successfully got profile feed",
    "data": {
      "thoughts": [
        {
          "thoughtId": 10,
          "user": null, // Null in profile feed because context is already known
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
- **Headers:** `Authorization: Bearer <token>`, `Content-Type: multipart/form-data`
- **Request Parts:**
  - `data` (optional): `application/json` containing `{"name": "...", "username": "...", "bio": "..."}`
  - `file` (optional): Image file (`.jpg`, `.png`).
- **Logic Flow & Cleanup:**
  - Old image is fetched. New image is uploaded to MinIO. Profile is updated via gRPC. 
  - If successful, the old MinIO image is deleted. If failed, the new MinIO image is deleted to prevent orphans.
- **Success Response (200 OK):**
  ```json
  {
    "success": true,
    "message": "Profile updated successfully",
    "data": null
  }
  ```
- **Error Consequences:**
  - `409 Conflict`: If the requested new username is already taken.

### 2.4 Search Profiles
- **Endpoint:** `GET /profile/search/{usernamePrefix}`
- **Description:** Search for users by username prefix.
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

### 3.1 Follow User
- **Endpoint:** `GET /user/{id}/follow`
- **Description:** Follow the user specified by `{id}`.
- **Error Consequences:**
  - `409 Conflict`: Already following this user.
  - `404 Not Found`: Target user does not exist.

### 3.2 Unfollow User
- **Endpoint:** `DELETE /user/{id}/follow`
- **Description:** Unfollow the user specified by `{id}`.
- **Error Consequences:**
  - `404 Not Found`: Target user does not exist, or you aren't currently following them.

### 3.3 Get Followers / Following
- **Endpoints:** 
  - `GET /user/{id}/followerslist?limit=20&cursor=`
  - `GET /user/{id}/followinglist?limit=20&cursor=`
- **Description:** Get a paginated list of users following or followed by the user `{id}`.
- **Response Format:** Returns a `UsersFeedDTO` containing a `users` array and a `nextCursor`.

---

## 4. Thoughts & Opinions (`/thoughts`)

### 4.1 Create Thought
- **Endpoint:** `POST /thoughts/`
- **Request Body:**
  ```json
  {
    "content": "This is a new thought!",
    "parentThoughtId": null 
  }
  ```
  *(Pass a valid ID for `parentThoughtId` if this is an opinion/reply)*
- **Success Response:**
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
- **Error Consequences:**
  - `404 Not Found`: Thought does not exist.
  - `403 Forbidden`: You do not own this thought.

### 4.3 Get Single Thought
- **Endpoint:** `GET /thoughts/{thoughtId}`
- **Response:** Returns `ThoughtDetailsDTO` (including populated `UserDTO`).

### 4.4 Get Opinions (Replies)
- **Endpoint:** `GET /thoughts/opinions/{thoughtId}?limit=20&cursor=`
- **Description:** Paginated list of opinions (replies) replying to `{thoughtId}`.
- **Response:** Returns `ThoughtsFeedDTO`.

### 4.5 Interact (Like / Unlike)
- **Endpoints:**
  - `GET /thoughts/interact/{thoughtId}` (Like)
  - `DELETE /thoughts/interact/{thoughtId}` (Unlike)
- **Error Consequences:**
  - `409 Conflict`: Already liked (on Like)
  - `404 Not Found`: Not liked yet (on Unlike) or Thought doesn't exist.

### 4.6 Get Likes / Reposts Lists
- **Endpoints:**
  - `GET /thoughts/likes/{thoughtId}?limit=20&cursor=`
  - `GET /thoughts/reposts/{thoughtId}?limit=20&cursor=`
- **Description:** Returns paginated `UsersFeedDTO` of people who liked/reposted the thought.
