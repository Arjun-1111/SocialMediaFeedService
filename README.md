Social Media Feed Service — Complete Project README
Markdown

# Social Media Feed Service

A production-style Java Spring Boot application implementing a scalable
social media feed system. Built with read-heavy optimization strategies,
Redis-based feed storage, async fanout processing, and cursor-based pagination.

---

## Table of Contents

1. Project Overview
2. Learning Goals
3. Tech Stack
4. Architecture Overview
5. Project Structure
6. Domain Design
7. Data Flow
8. API Endpoints
9. Key Design Decisions
10. What Is Implemented
11. What Is Deferred / Not Yet Implemented
12. Known Limitations and Future Optimizations
13. How to Run Locally
14. Environment Configuration
15. Important Implementation Notes

---

## 1. Project Overview

A backend service that handles:
- User registration and profile management
- Follow/unfollow relationships between users
- Post creation with automatic feed distribution
- Feed reading with cursor-based pagination
- Redis-backed feed storage with cache-aside pattern
- Fanout-on-write for regular users
- Fanout-on-read for celebrity users (10K+ followers)

---

## 2. Learning Goals

| Goal | Status |
|------|--------|
| N+1 Query Problem and batch fetching | ✅ Implemented |
| Cache-Aside Pattern | ✅ Implemented |
| Cursor Pagination | ✅ Implemented |
| Fanout-on-Write | ✅ Implemented |
| Fanout-on-Read | ✅ Implemented |
| Redis Sorted Sets for feeds | ✅ Implemented |
| Async Processing with thread pool | ✅ Implemented |
| Denormalization for read performance | ✅ Implemented |
| Atomic counter updates | ✅ Implemented |
| Feed rebuild on cache expiry | ✅ Implemented |
| Celebrity/regular user split | ✅ Implemented |
| JWT Authentication | ❌ Deferred |
| Database Migrations | ❌ Deferred |
| Unit and Integration Tests | ❌ Deferred |

---

## 3. Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Language |
| Spring Boot | 4.x | Framework |
| Spring Data JPA | - | PostgreSQL ORM |
| Spring Data Redis | - | Redis client |
| Spring Security | - | Security (stateless, JWT deferred) |
| PostgreSQL | 15 | Primary database |
| Redis | 7 | Feed cache and post cache |
| Lettuce | - | Redis connection pool |
| HikariCP | - | PostgreSQL connection pool |
| Lombok | - | Boilerplate reduction |
| Jackson 3 | - | JSON serialization |
| Docker Compose | - | Local infrastructure |
| Maven | - | Build tool |

---

## 4. Architecture Overview
Client
│
▼
Controller Layer (HTTP)
│
▼
Service Layer (Business Logic)
│
├──── PostgreSQL (via JPA Repository)
│
└──── Redis (via FeedRedisRepository)
├── Sorted Sets (feeds)
└── Strings (post cache)

text


### Feed Write Flow (Fanout on Write)
POST /api/v1/posts
│
▼
PostServiceImpl.createPost()
│
├── Save post to PostgreSQL
│
└── FanoutService.fanoutToFollowers() [ASYNC - background thread]
│
├── Check followerCount
│ ├── >= 10,000 → skip (celebrity, fanout on read)
│ └── < 10,000 → proceed
│
├── Get all follower IDs from follows table
│
└── For each follower:
addToFeed(followerId, postId, score=timestamp)
→ Redis ZADD feed:user:{followerId}

text


### Feed Read Flow
GET /api/v1/feed?userId=X&cursor=Y&limit=20
│
▼
FeedServiceImpl.getFeed()
│
├── Get following IDs
│
├── Split into regular and celebrity
│
├── Regular posts:
│ ├── Redis feed exists?
│ │ YES → ZREVRANGEBYSCORE feed:user:{userId}
│ │ → Cache-aside for each postId
│ │ HIT → serve from Redis post cache
│ │ MISS → batch fetch from PostgreSQL
│ │ → store in Redis post cache
│ └── NO → Rebuild from PostgreSQL
│ → Write postIds back to Redis
│
├── Celebrity posts:
│ └── Always fetch from PostgreSQL directly
│ First page → findRecentPostsByUserIds
│ With cursor → findRecentPostsByUserIdsAndBefore
│
├── Merge regular + celebrity posts
│
├── Sort by createdAt descending
│
├── Trim to limit, determine hasMore
│
└── Return FeedResponse with nextCursor

text


---

## 5. Project Structure
src/main/java/com/project/SocialMediaFeedService/
│
├── common/
│ ├── config/
│ │ ├── AsyncConfig.java
│ │ ├── PasswordEncoderConfig.java
│ │ ├── RedisConfig.java
│ │ └── SecurityConfig.java
│ ├── entity/
│ │ └── BaseEntity.java
│ ├── exception/
│ │ ├── DuplicateResourceException.java
│ │ ├── GlobalExceptionHandler.java
│ │ ├── ResourceNotFoundException.java
│ │ └── SelfFollowNotAllowedException.java
│ └── response/
│ └── ApiResponse.java
│
├── user/
│ ├── controller/
│ │ └── UserController.java
│ ├── dto/
│ │ ├── request/
│ │ │ ├── RegisterRequest.java
│ │ │ └── UpdateUserRequest.java
│ │ └── response/
│ │ └── UserResponse.java
│ ├── entity/
│ │ └── User.java
│ ├── mapper/
│ │ └── UserMapper.java
│ ├── repository/
│ │ └── UserRepository.java
│ └── service/
│ ├── UserService.java
│ └── impl/
│ └── UserServiceImpl.java
│
├── follow/
│ ├── controller/
│ │ └── FollowController.java
│ ├── dto/
│ │ └── response/
│ │ └── FollowResponse.java
│ ├── entity/
│ │ ├── Follow.java
│ │ └── FollowId.java
│ ├── repository/
│ │ └── FollowRepository.java
│ └── service/
│ ├── FollowService.java
│ └── impl/
│ └── FollowServiceImpl.java
│
├── post/
│ ├── controller/
│ │ └── PostController.java
│ ├── dto/
│ │ ├── request/
│ │ │ └── CreatePostRequest.java
│ │ └── response/
│ │ └── PostResponse.java
│ ├── entity/
│ │ └── Post.java
│ ├── mapper/
│ │ └── PostMapper.java
│ ├── repository/
│ │ └── PostRepository.java
│ └── service/
│ ├── PostService.java
│ └── impl/
│ └── PostServiceImpl.java
│
└── feed/
├── controller/
│ └── FeedController.java
├── dto/
│ └── response/
│ └── FeedResponse.java
├── repository/
│ └── FeedRedisRepository.java
└── service/
├── FanoutService.java
├── FeedService.java
└── impl/
├── FanoutServiceImpl.java
└── FeedServiceImpl.java

text


---

## 6. Domain Design

### User Entity
users table:
id BIGINT PK AUTO_INCREMENT
username VARCHAR(50) NOT NULL UNIQUE
email VARCHAR(100) NOT NULL UNIQUE
password VARCHAR(100) NOT NULL (BCrypt hashed)
bio VARCHAR(500)
avatar_url VARCHAR(255)
follower_count BIGINT NOT NULL DEFAULT 0
following_count BIGINT NOT NULL DEFAULT 0
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL

text


### Follow Entity
follows table:
follower_id BIGINT NOT NULL (FK → users.id)
following_id BIGINT NOT NULL (FK → users.id)
created_at TIMESTAMP NOT NULL

PRIMARY KEY (follower_id, following_id)
UNIQUE CONSTRAINT on (follower_id, following_id)

text


Composite primary key via `@EmbeddedId` with `FollowId` class.

### Post Entity
posts table:
id BIGINT PK AUTO_INCREMENT
content VARCHAR(3000) NOT NULL
user_id BIGINT NOT NULL
username VARCHAR(50) NOT NULL ← denormalized
avatar_url VARCHAR(255) ← denormalized
like_count BIGINT NOT NULL DEFAULT 0
comment_count BIGINT NOT NULL DEFAULT 0
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL

text


`username` and `avatar_url` are denormalized from the users table.
This avoids N+1 queries when rendering feed posts.

### Feed Storage (Redis)
feed:user:{userId} → Sorted Set
Member: postId (String)
Score: createdAt epoch milliseconds (Double)
TTL: 5 minutes (sliding window)

post:cache:{postId} → String
Value: JSON of PostResponse
TTL: 5 minutes

text


---

## 7. Data Flow

### User Registration Flow
POST /api/v1/users
│
├── Check username not taken (existsByUsername)
├── Check email not taken (existsByEmail)
├── Hash password with BCrypt
├── Map RegisterRequest → User entity
├── Save to PostgreSQL
└── Return UserResponse (no password)

text


### Follow Flow
POST /api/v1/follows/{followingId}?followerId={id}
│
├── Check not following self
├── Check both users exist
├── Check not already following
├── Save Follow entity
├── incrementFollowingCount(followerId) ← atomic JPQL UPDATE
├── incrementFollowerCount(followingId) ← atomic JPQL UPDATE
└── Return FollowResponse with updated counts

text


### Post Creation Flow
POST /api/v1/posts?userId={id}
│
├── Fetch user from DB
├── Map CreatePostRequest + User → Post entity (denormalize username/avatarUrl)
├── Save post to PostgreSQL
├── Calculate score = createdAt.toEpochMilli()
├── Call fanoutService.fanoutToFollowers(postId, userId, score) [ASYNC]
│ └── Runs in fanout-thread-{n} background thread
│ ├── Fetch author — check followerCount
│ ├── >= 10,000 → log and return (celebrity)
│ ├── < 10,000 → get all follower IDs
│ └── For each follower → ZADD feed:user:{followerId}
└── Return PostResponse immediately (not waiting for fanout)

text


### Feed Read Flow
GET /api/v1/feed?userId={id}&cursor={cursor}&limit={limit}
│
├── Parse cursor → score (Double) + cursorTime (LocalDateTime)
├── Get followingIds from follows table
├── Fetch all followed users in ONE query (no N+1)
├── Split into regularIds and celebrityIds
│
├── Regular posts:
│ ├── feedExists(userId)?
│ │ YES:
│ │ ZREVRANGEBYSCORE feed:user:{userId} 0 (score-1) LIMIT limit+1
│ │ For each postId:
│ │ GET post:cache:{postId}
│ │ HIT → deserialize JSON → PostResponse
│ │ MISS → add to missedIds
│ │ findAllById(missedIds) → ONE batch query
│ │ For each fetched post:
│ │ serialize → SET post:cache:{postId} TTL 5min
│ │ NO (Redis expired):
│ │ findRecentPostsByUserIds OR findRecentPostsByUserIdsAndBefore
│ │ Write postIds back to Redis sorted set
│ └── Return PostResponse list
│
├── Celebrity posts:
│ cursor null → findRecentPostsByUserIds(celebrityIds, limit+1)
│ cursor set → findRecentPostsByUserIdsAndBefore(celebrityIds, before, limit+1)
│ └── Return PostResponse list
│
├── Merge regularPosts + celebrityPosts
├── Sort by createdAt DESC
├── merged.size() > limit → hasMore=true, trim to limit
├── nextCursor = last item createdAt.toEpochMilli() as String
└── Return FeedResponse

text


---

## 8. API Endpoints

### User
POST /api/v1/users
Body: { username, email, password }
Returns: 201 + UserResponse

GET /api/v1/users/{username}
Returns: 200 + UserResponse

PATCH /api/v1/users/{userId}
Body: { username?, bio?, avatarUrl? } (all optional)
Returns: 200 + UserResponse

text


### Follow
POST /api/v1/follows/{followingId}?followerId={id}
Returns: 201 + FollowResponse
Errors: 400 self-follow, 409 already following, 404 user not found

DELETE /api/v1/follows/{followingId}?followerId={id}
Returns: 200 + success message
Errors: 404 follow relationship not found

text


### Post
POST /api/v1/posts?userId={id}
Body: { content }
Returns: 201 + PostResponse

GET /api/v1/posts/{postId}
Returns: 200 + PostResponse

GET /api/v1/posts/user/{userId}
Returns: 200 + List<PostResponse>

text


### Feed
GET /api/v1/feed?userId={id}&cursor={cursor}&limit={limit}
cursor → optional, null for first page
limit → optional, default 20
Returns: 200 + FeedResponse {
posts: List<PostResponse>,
nextCursor: String | null,
hasMore: boolean
}

text


---

## 9. Key Design Decisions

### Fanout Strategy

| User Type | Strategy | Why |
|-----------|----------|-----|
| Regular (< 10K followers) | Fanout on Write | Fast reads, manageable write cost |
| Celebrity (>= 10K followers) | Fanout on Read | Write amplification too expensive |

Celebrity threshold: `10,000 followers`

### Cursor Pagination

Uses timestamp epoch milliseconds as cursor score.
First page: score = Double.MAX_VALUE
Next pages: score = lastItem.createdAt.toEpochMilli() - 1

text


Subtracting 1 makes cursor exclusive — prevents duplicate items.

Advantages over OFFSET:
- Consistent results when new posts arrive
- No performance degradation at large offsets
- Works naturally with Redis sorted sets

### Atomic Counter Updates

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE User u SET u.followerCount = u.followerCount + 1 WHERE u.id = :userId")
void incrementFollowerCount(@Param("userId") Long userId);
Why: prevents lost update race condition under concurrent follows.
Never do: load entity → modify count → save entity (race condition).

Denormalization
username and avatarUrl stored directly on Post entity.

Why:

Every feed post needs to show author name and avatar
Without denormalization → N+1 or expensive JOIN on every feed read
Trade-off: stale data if user renames → accepted as known limitation
Cache-Aside Pattern
text

Check Redis first
  HIT  → return cached data
  MISS → fetch from PostgreSQL
       → store in Redis with TTL
       → return data
Cache failure is graceful — falls back to PostgreSQL silently.
Feed never fails because of cache errors.

Async Fanout
Custom thread pool configuration:

Java

corePoolSize  = 5
maxPoolSize   = 20
queueCapacity = 100
threadPrefix  = "fanout-thread-"
Main request thread returns immediately after saving post.
Fanout runs in background thread pool.

10. What Is Implemented
Common Layer
✅ BaseEntity with auto-managed timestamps
✅ ApiResponse<T> standard response wrapper
✅ GlobalExceptionHandler with handlers for all common errors
✅ ResourceNotFoundException (404)
✅ DuplicateResourceException (409)
✅ SelfFollowNotAllowedException (400)
✅ PasswordEncoderConfig (BCrypt)
✅ SecurityConfig (stateless, all endpoints open — JWT deferred)
✅ AsyncConfig (custom thread pool for fanout)
✅ RedisConfig (StringRedisSerializer + ObjectMapper)
User Domain
✅ User entity with constraints and defaults
✅ UserRepository with custom queries and atomic updates
✅ RegisterRequest, UpdateUserRequest, UserResponse DTOs
✅ UserMapper
✅ UserService + UserServiceImpl
✅ UserController
Follow Domain
✅ Follow entity with composite primary key
✅ FollowId embedded ID class
✅ FollowRepository with JPQL follower/following queries
✅ FollowService + FollowServiceImpl
✅ FollowController
✅ FollowResponse DTO
Post Domain
✅ Post entity with denormalized fields
✅ PostRepository with batch and cursor queries
✅ CreatePostRequest, PostResponse DTOs
✅ PostMapper (accepts User for denormalization)
✅ PostService + PostServiceImpl (triggers fanout on create)
✅ PostController
Feed Domain
✅ FeedRedisRepository (sorted sets + post cache)
✅ FanoutService + FanoutServiceImpl (async, celebrity check)
✅ FeedService + FeedServiceImpl (full merged feed)
✅ FeedController
✅ FeedResponse DTO
11. What Is Deferred / Not Yet Implemented
High Priority
JWT Authentication

Spring Security filter chain
Login endpoint with LoginRequest DTO
JWT token generation on login
JWT validation filter
Extract userId from token (replace @RequestParam userId everywhere)
Protected vs public route configuration
Database Migrations

Replace ddl-auto: update with Flyway or Liquibase
Version-controlled schema files
Safe production deployments
Tests

Unit tests for all service layer methods
Repository integration tests
MockMvc controller tests
Redis integration tests
Medium Priority
Delete Post

Remove from PostgreSQL
Remove postId from all Redis feeds that contain it
Invalidate post:cache:{postId}
Decrement counts if needed
Update Post

Update content in PostgreSQL
Invalidate post:cache:{postId} to force refresh
Handle stale denormalized data in other places
Like/Unlike Post

Like entity or likes table
Atomic likeCount increment/decrement on Post (same pattern as followerCount)
Duplicate like prevention
Comment on Post

Comment entity with postId foreign key
Atomic commentCount increment/decrement on Post
Paginated comment retrieval
Update Profile → Propagate Denormalized Data

When user changes username or avatarUrl
Update all their posts' denormalized fields
Options: background job, accept stale data, or synchronous update
Low Priority / Future Optimizations
Celebrity Post Redis Cache

text

celebrity:posts:{celebrityId} → sorted set of recent post IDs
TTL: 1-2 minutes
All followers share one cached set instead of each hitting PostgreSQL
Feed Pre-warming on Login

When inactive user logs in
Rebuild their Redis feed proactively
Better first-load experience
Read Replicas

Route feed generation queries to read replica
Primary PostgreSQL only handles writes
Discuss in architecture context
Soft Delete

Add deleted_at column
Filter deleted posts from all queries
Preserve data for audit
Rate Limiting

Prevent abuse of post creation
Prevent feed scraping
Spring Cloud Gateway or Bucket4j
API Documentation

Springdoc OpenAPI
Swagger UI
Auto-generated from annotations
Monitoring and Observability

Micrometer metrics
Prometheus + Grafana
Custom metrics: fanout duration, cache hit rate, feed rebuild frequency
12. Known Limitations
Limitation	Description	Mitigation
Stale denormalized data	Username/avatarUrl on posts not updated when user profile changes	Accept as tradeoff — old posts show old username
Celebrity feed latency	Celebrity posts always hit PostgreSQL	Add celebrity post Redis cache (deferred)
Feed gap after TTL	If user inactive > 5 min and no new posts, feed rebuilds from DB	Rebuild implemented — slight latency on first read
No auth	userId passed as request param — anyone can read anyone's feed	JWT implementation deferred
followerCount accuracy	Under extreme concurrent follows, count might drift slightly	Atomic updates reduce risk significantly
Cursor precision	Two posts in same millisecond → same score → potential ordering issue	Extremely rare — acceptable trade-off
13. How to Run Locally
Prerequisites
Docker and Docker Compose
Java 21
Maven
Start Infrastructure
Bash

docker-compose up -d
This starts:

PostgreSQL on port 5432
Redis on port 6379
Run Application
Bash

./mvnw spring-boot:run -Dspring.profiles.active=dev
Or set active profile in application.yml:

YAML

spring:
  profiles:
    active: dev
Verify Startup
Look for these log lines:

text

The following 1 profile is active: "dev"
HikariPool-1 - Start completed
Tomcat started on port 8080
14. Environment Configuration
application.yml (shared base)
YAML

spring:
  application:
    name: social-media-feed-service
  jpa:
    open-in-view: false
application-dev.yml
YAML

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/feedservice
    username: feeduser
    password: feedpass
  data:
    redis:
      host: localhost
      port: 6379
      password: redispass
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
application-prod.yml
YAML

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST}
      password: ${REDIS_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
15. Important Implementation Notes
@Modifying Queries Must Have clearAutomatically = true
Java

@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE User u SET u.followerCount = u.followerCount + 1 WHERE u.id = :userId")
void incrementFollowerCount(@Param("userId") Long userId);
Without clearAutomatically — Hibernate returns stale cached count after update.

@Async Only Works Cross-Bean
Java

// WRONG — calling async method from same class
this.fanoutToFollowers(...)  // @Async ignored

// CORRECT — injected bean calls async method
fanoutService.fanoutToFollowers(...)  // @Async works through proxy
@Builder.Default Required for Field Defaults
Java

// WRONG — Lombok builder ignores field default
Long followerCount = 0L;

// CORRECT — builder respects this default
@Builder.Default
Long followerCount = 0L;
JPQL Uses Entity Names Not Table Names
Java

// WRONG
@Query("UPDATE users u SET ...")

// CORRECT
@Query("UPDATE User u SET ...")
Redis Cursor Is Exclusive
Java

// Subtract 1 to exclude the cursor item itself
double exclusiveMax = maxScore == Double.MAX_VALUE ? maxScore : maxScore - 1;
redisTemplate.opsForZSet().reverseRangeByScore(key, 0, exclusiveMax, 0, limit);
Boolean.TRUE.equals for Safe Redis hasKey
Java

// WRONG — can throw NPE if hasKey returns null
return redisTemplate.hasKey(key);

// CORRECT — null-safe
return Boolean.TRUE.equals(redisTemplate.hasKey(key));
Cache Failure Must Never Break Feed
Java

try {
    feedRedisRepository.cachePost(post.getId(), json);
} catch (JacksonException e) {
    log.warn("Failed to cache postId={}", post.getId());
    // do NOT rethrow — graceful degradation
}
freshPosts.add(postToBeCached); // always add to result regardless of cache
Service Beans Are Singletons — Never Use Instance State for Request Data
Java

// WRONG — shared across all requests
private final List<Long> cacheMissIds = new ArrayList<>();

// CORRECT — local to each request
List<Long> cacheMissIds = new ArrayList<>();
Redis Key Reference
Key Pattern	Type	TTL	Purpose
feed:user:{userId}	Sorted Set	5 min sliding	User's feed post IDs ordered by timestamp
post:cache:{postId}	String	5 min	Cached PostResponse JSON
Standard Response Format
All endpoints return:

JSON

{
  "success": true,
  "message": "descriptive message",
  "data": { ... }
}
Error responses:

JSON

{
  "success": false,
  "message": "error description",
  "data": null
}
Feed Response Format
JSON

{
  "success": true,
  "message": "User feed fetched Successfully !",
  "data": {
    "posts": [
      {
        "id": 10,
        "content": "post content",
        "userId": 7,
        "username": "bob",
        "avatarUrl": null,
        "likeCount": 0,
        "commentCount": 0,
        "createdAt": "2026-08-17T14:54:32.976",
        "updatedAt": "2026-08-17T14:54:32.976"
      }
    ],
    "nextCursor": "1786875388842",
    "hasMore": true
  }
}
nextCursor is epoch milliseconds as String.
Send as ?cursor={nextCursor} for next page.
null when no more pages exist. 
```
Project built as a learning exercise focusing on production-style patterns,
read-heavy optimization, and distributed system design concepts.






