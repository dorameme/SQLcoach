# SQLCoach API 명세서

> SQL 쿼리 최적화 코칭 & 학습 플랫폼  
> Backend: Spring Boot (Java) + MySQL

---

## 기본 정보

| 항목 | 값 |
|------|-----|
| Base URL | `http://localhost:8080/api/v1` |
| Content-Type | `application/json` |
| 에러 형식 | `{ "code": "ERROR_CODE", "message": "설명" }` |

---

## 공통 에러 응답

```json
{
  "code": "INVALID_REQUEST",
  "message": "쿼리를 입력해주세요."
}
```

| HTTP Status | code | 설명 |
|-------------|------|------|
| 400 | INVALID_REQUEST | 잘못된 요청 파라미터 |
| 404 | NOT_FOUND | 리소스를 찾을 수 없음 |
| 429 | RATE_LIMITED | 요청 횟수 초과 |
| 500 | INTERNAL_ERROR | 서버 내부 오류 |

---

## 1. 쿼리 분석 (Query Analysis)

### 1-1. 쿼리 분석 요청

사용자가 입력한 SQL 쿼리를 LLM이 분석하여 최적화 피드백을 제공합니다.

**POST** `/queries/analyze`

#### Request Body

```json
{
  "query": "SELECT * FROM users WHERE name LIKE '%kim%' ORDER BY created_at",
  "dialect": "mysql",
  "context": {
    "tableSchema": "CREATE TABLE users (id BIGINT PK, name VARCHAR(100), email VARCHAR(255), created_at DATETIME, INDEX idx_created_at(created_at))",
    "dataVolume": "약 100만 건",
    "description": "이름에 'kim'이 포함된 유저를 최신순으로 조회"
  }
}
```

| 필드 | 타입 | 필수  | 설명 |
|------|------|-----|------|
| query | String | O   | 분석할 SQL 쿼리 |
| dialect | String | X   | SQL 방언 (기본값: "mysql") |
| context.tableSchema | String | X   | 테이블 DDL (제공 시 더 정확한 분석) |
| context.dataVolume | String | X   | 예상 데이터 규모 |
| context.description | String | X   | 쿼리의 목적 설명 |

#### Response `200 OK`

```json
{
  "analysisId": "ana_a1b2c3d4",
  "originalQuery": "SELECT * FROM users WHERE name LIKE '%kim%' ORDER BY created_at",
  "overallScore": 35,
  "summary": "전체 테이블 스캔이 발생하며, 인덱스를 활용하지 못하는 쿼리입니다.",
  "issues": [
    {
      "severity": "CRITICAL",
      "category": "INDEX_USAGE",
      "title": "LIKE '%keyword%' 패턴은 인덱스를 사용할 수 없습니다",
      "description": "앞에 와일드카드(%)가 있으면 B-Tree 인덱스의 정렬 순서를 활용할 수 없어 Full Table Scan이 발생합니다.",
      "suggestion": "Full-Text Index 사용을 고려하거나, 검색 요구사항에 따라 LIKE 'kim%' (전방 일치)로 변경할 수 있는지 검토하세요.",
      "relatedConcept": "B-Tree 인덱스 구조와 범위 스캔"
    },
    {
      "severity": "WARNING",
      "category": "SELECT_STAR",
      "title": "SELECT * 사용을 지양하세요",
      "description": "필요한 컬럼만 명시하면 네트워크 전송량이 줄고, 커버링 인덱스 활용 가능성이 높아집니다.",
      "suggestion": "SELECT id, name, email FROM users ...",
      "relatedConcept": "커버링 인덱스 (Covering Index)"
    }
  ],
  "optimizedQuery": "SELECT id, name, email FROM users WHERE MATCH(name) AGAINST('kim' IN BOOLEAN MODE) ORDER BY created_at DESC",
  "explanationPlan": {
    "before": "type: ALL, rows: 1000000, Extra: Using filesort",
    "after": "type: fulltext, rows: 150, Extra: Using where"
  },
  "learningPoints": [
    {
      "concept": "B-Tree 인덱스와 LIKE 패턴",
      "sqlpChapter": "인덱스 기본 원리",
      "briefExplanation": "B-Tree 인덱스는 값의 앞부분부터 정렬되어 있어, LIKE 'abc%'는 범위 스캔이 가능하지만 '%abc%'는 불가능합니다."
    }
  ],
  "createdAt": "2025-04-18T10:30:00"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| analysisId | String | 분석 결과 고유 ID |
| overallScore | Integer (0-100) | 쿼리 품질 점수 |
| issues[].severity | Enum | CRITICAL / WARNING / INFO |
| issues[].category | String | 이슈 카테고리 |
| issues[].relatedConcept | String | SQLP 관련 개념 |
| optimizedQuery | String | 개선된 쿼리 제안 |
| learningPoints | Array | SQLP 학습 포인트 |

---

### 1-2. 분석 히스토리 조회

최근 분석한 쿼리 목록을 조회합니다 (브라우저 세션 기반).

**GET** `/queries/history?page=0&size=10`

#### Query Parameters

| 파라미터 | 타입 | 필수  | 설명 |
|----------|------|-----|------|
| page | Integer | X   | 페이지 번호 (기본값: 0) |
| size | Integer | X   | 페이지 크기 (기본값: 10, 최대: 50) |

#### Response `200 OK`

```json
{
  "content": [
    {
      "analysisId": "ana_a1b2c3d4",
      "query": "SELECT * FROM users WHERE name LIKE '%kim%'",
      "overallScore": 35,
      "issueCount": 2,
      "createdAt": "2025-04-18T10:30:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 23,
  "totalPages": 3
}
```

---

### 1-3. 분석 상세 조회

**GET** `/queries/{analysisId}`

#### Response `200 OK`

1-1의 Response와 동일한 구조

---

## 2. SQL 퀴즈 / 챌린지 (Quiz & Challenge)

### 2-1. 퀴즈 목록 조회

**GET** `/quizzes?category={category}&difficulty={difficulty}&page=0&size=10`

#### Query Parameters

| 파라미터 | 타입 | 필수  | 설명 |
|----------|------|-----|------|
| category | String | X   | 카테고리 필터 |
| difficulty | Enum | X   | BEGINNER / INTERMEDIATE / ADVANCED |
| page | Integer | X   | 페이지 번호 (기본값: 0) |
| size | Integer | X   | 페이지 크기 (기본값: 10) |

#### 카테고리 목록

| category | 설명 | SQLP 연관 |
|----------|------|-----------|
| INDEX_OPTIMIZATION | 인덱스 최적화 | 인덱스 설계 및 활용 |
| JOIN_OPTIMIZATION | 조인 최적화 | 조인 방식과 성능 |
| SUBQUERY | 서브쿼리 튜닝 | 서브쿼리 vs 조인 |
| EXECUTION_PLAN | 실행계획 분석 | 실행계획 해석 |
| TRANSACTION | 트랜잭션과 락 | 동시성 제어 |
| DATA_MODELING | 데이터 모델링 | 정규화/반정규화 |
| AGGREGATION | 집계 쿼리 최적화 | GROUP BY, 윈도우 함수 |
| QUERY_REWRITE | 쿼리 변환 | SQL 옵티마이저 원리 |

#### Response `200 OK`

```json
{
  "content": [
    {
      "quizId": "quiz_001",
      "title": "인덱스를 타지 않는 쿼리 찾기",
      "category": "INDEX_OPTIMIZATION",
      "difficulty": "INTERMEDIATE",
      "description": "다음 중 주어진 인덱스를 활용하지 못하는 쿼리를 고르세요.",
      "type": "MULTIPLE_CHOICE",
      "solvedCount": 342,
      "correctRate": 0.58
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 48,
  "totalPages": 5
}
```

---

### 2-2. 퀴즈 상세 조회 (문제 풀기)

**GET** `/quizzes/{quizId}`

#### Response `200 OK`

```json
{
  "quizId": "quiz_001",
  "title": "인덱스를 타지 않는 쿼리 찾기",
  "category": "INDEX_OPTIMIZATION",
  "difficulty": "INTERMEDIATE",
  "type": "MULTIPLE_CHOICE",
  "timeLimit": 120,
  "problem": {
    "description": "다음 테이블과 인덱스가 주어졌을 때, 인덱스를 활용하지 못하는 쿼리를 모두 고르세요.",
    "tableSchema": "CREATE TABLE orders (\n  id BIGINT PRIMARY KEY,\n  user_id BIGINT,\n  product_id BIGINT,\n  amount DECIMAL(10,2),\n  status VARCHAR(20),\n  created_at DATETIME,\n  INDEX idx_user_status (user_id, status),\n  INDEX idx_created (created_at)\n);",
    "choices": [
      {
        "id": "A",
        "query": "SELECT * FROM orders WHERE user_id = 100 AND status = 'PAID'"
      },
      {
        "id": "B",
        "query": "SELECT * FROM orders WHERE status = 'PAID'"
      },
      {
        "id": "C",
        "query": "SELECT * FROM orders WHERE user_id = 100 ORDER BY created_at"
      },
      {
        "id": "D",
        "query": "SELECT * FROM orders WHERE YEAR(created_at) = 2024"
      }
    ]
  }
}
```

| type | 설명 |
|------|------|
| MULTIPLE_CHOICE | 객관식 (단일/다중 선택) |
| QUERY_WRITE | 쿼리 직접 작성 |
| QUERY_FIX | 주어진 쿼리 개선 |
| EXPLAIN_ANALYZE | 실행계획 해석 |

---

### 2-3. 퀴즈 답안 제출

**POST** `/quizzes/{quizId}/submit`

#### Request Body

```json
{
  "answer": ["B", "D"],
  "timeTaken": 45
}
```

| 필드 | 타입 | 필수  | 설명 |
|------|------|-----|------|
| answer | Array/String | O   | 선택한 답 또는 작성한 쿼리 |
| timeTaken | Integer | X   | 풀이 소요 시간 (초) |

#### Response `200 OK`

```json
{
  "correct": true,
  "correctAnswer": ["B", "D"],
  "explanation": "B: 복합 인덱스 idx_user_status(user_id, status)에서 선행 컬럼 user_id 없이 status만으로 조회하면 인덱스를 탈 수 없습니다 (인덱스 스킵 스캔 제외).\n\nD: WHERE YEAR(created_at) = 2024는 컬럼에 함수를 적용하여 인덱스를 사용할 수 없습니다. WHERE created_at >= '2024-01-01' AND created_at < '2025-01-01'로 변환해야 합니다.",
  "detailedExplanation": {
    "A": "O user_id, status 모두 인덱스 선행 컬럼 순서대로 사용 → 인덱스 활용 가능",
    "B": "X 복합 인덱스의 두 번째 컬럼만 단독 사용 → Full Table Scan",
    "C": "O user_id로 idx_user_status 인덱스 사용 가능 (status 없어도 선행 컬럼이므로 OK)",
    "D": "X 컬럼에 함수 적용 → 인덱스 무효화"
  },
  "relatedConcepts": [
    "복합 인덱스의 컬럼 순서와 선행 컬럼 규칙",
    "인덱스 컬럼의 변형 (함수 적용) 금지"
  ],
  "nextRecommended": "quiz_005"
}
```

---

### 2-4. 퀴즈 통계 조회

**GET** `/quizzes/stats`

#### Response `200 OK`

```json
{
  "totalSolved": 15,
  "correctCount": 10,
  "correctRate": 0.67,
  "byCategory": [
    {
      "category": "INDEX_OPTIMIZATION",
      "solved": 5,
      "correct": 4,
      "correctRate": 0.80
    },
    {
      "category": "JOIN_OPTIMIZATION",
      "solved": 3,
      "correct": 1,
      "correctRate": 0.33
    }
  ],
  "byDifficulty": [
    {
      "difficulty": "BEGINNER",
      "solved": 8,
      "correct": 7
    },
    {
      "difficulty": "INTERMEDIATE",
      "solved": 5,
      "correct": 3
    },
    {
      "difficulty": "ADVANCED",
      "solved": 2,
      "correct": 0
    }
  ],
  "weakCategories": ["JOIN_OPTIMIZATION", "TRANSACTION"],
  "streakDays": 3
}
```

---

## 3. 데이터베이스 스키마 (참고용)

백엔드 구현 시 참고할 테이블 설계입니다.

### ERD 개요

```
query_analyses       quizzes              quiz_submissions
├── id (PK)          ├── id (PK)          ├── id (PK)
├── session_id       ├── title            ├── session_id
├── original_query   ├── category         ├── quiz_id (FK)
├── dialect          ├── difficulty       ├── answer
├── context_json     ├── type             ├── correct
├── score            ├── problem_json     ├── time_taken
├── result_json      ├── answer_json      └── created_at
└── created_at       ├── explanation
                     ├── solved_count
                     └── correct_rate
```

### DDL

```sql
-- 쿼리 분석 기록
CREATE TABLE query_analyses (
    id VARCHAR(20) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    original_query TEXT NOT NULL,
    dialect VARCHAR(20) DEFAULT 'mysql',
    context_json JSON,
    score INT,
    result_json JSON NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_created (session_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 퀴즈 문제
CREATE TABLE quizzes (
    id VARCHAR(20) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    difficulty ENUM('BEGINNER','INTERMEDIATE','ADVANCED') NOT NULL,
    type ENUM('MULTIPLE_CHOICE','QUERY_WRITE','QUERY_FIX','EXPLAIN_ANALYZE') NOT NULL,
    time_limit INT DEFAULT 120,
    problem_json JSON NOT NULL,
    answer_json JSON NOT NULL,
    explanation TEXT,
    detailed_explanation_json JSON,
    related_concepts_json JSON,
    solved_count INT DEFAULT 0,
    correct_rate DECIMAL(5,4) DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category_difficulty (category, difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 퀴즈 풀이 기록
CREATE TABLE quiz_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    quiz_id VARCHAR(20) NOT NULL,
    answer JSON NOT NULL,
    correct BOOLEAN NOT NULL,
    time_taken INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (quiz_id) REFERENCES quizzes(id),
    INDEX idx_session_quiz (session_id, quiz_id),
    INDEX idx_session_created (session_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 4. SQLP 실험 노트용 API (보너스)

SQLP 공부하면서 실험한 내용을 기록하는 기능입니다. 포트폴리오에서 `/experiments` 페이지로 보여줄 수 있습니다.

### 4-1. 실험 노트 저장

**POST** `/experiments`

#### Request Body

```json
{
  "title": "LIKE '%keyword%' vs Full-Text Index 성능 비교",
  "category": "INDEX_OPTIMIZATION",
  "environment": {
    "mysqlVersion": "8.0.35",
    "dataSize": "1,000,000 rows",
    "tableDDL": "CREATE TABLE users (...)"
  },
  "hypothesis": "Full-Text Index를 사용하면 LIKE '%keyword%' 대비 10배 이상 빠를 것이다",
  "experiments": [
    {
      "label": "LIKE 패턴 (Full Scan)",
      "query": "SELECT * FROM users WHERE name LIKE '%kim%'",
      "explainResult": "type: ALL, rows: 1000000, Extra: Using where",
      "executionTime": 1230,
      "notes": "전체 테이블 스캔 발생"
    },
    {
      "label": "Full-Text Index",
      "query": "SELECT * FROM users WHERE MATCH(name) AGAINST('kim' IN BOOLEAN MODE)",
      "explainResult": "type: fulltext, rows: 1, Extra: Using where",
      "executionTime": 12,
      "notes": "Full-Text 인덱스 활용, 약 100배 성능 향상"
    }
  ],
  "conclusion": "Full-Text Index 사용 시 약 100배의 성능 향상을 확인. 단, 한글 검색 시 ngram 파서 설정이 필요하며, InnoDB에서는 MySQL 5.6부터 지원.",
  "sqlpRelation": "SQLP 과목2 - 인덱스 활용 > 특수 인덱스"
}
```

#### Response `201 Created`

```json
{
  "experimentId": "exp_001",
  "createdAt": "2025-04-18T14:00:00"
}
```

### 4-2. 실험 노트 목록 조회

**GET** `/experiments?category={category}&page=0&size=10`

### 4-3. 실험 노트 상세 조회

**GET** `/experiments/{experimentId}`

### 4-4. 실험 노트 수정

**PUT** `/experiments/{experimentId}`

### 4-5. 실험 노트 삭제

**DELETE** `/experiments/{experimentId}`

---

## 5. 구현 우선순위

| 우선순위 | 기능 | 난이도 | SQLP 학습 연관도 |
|----------|------|--------|-----------------|
|  1순위 | 쿼리 분석 (1-1) | 중 (LLM 연동) | ⭐⭐⭐⭐⭐ |
|  2순위 | 퀴즈 목록/상세 (2-1, 2-2) | 하 | ⭐⭐⭐⭐ |
|  3순위 | 퀴즈 제출/통계 (2-3, 2-4) | 중 | ⭐⭐⭐⭐ |
| 4순위 | 분석 히스토리 (1-2, 1-3) | 하 | ⭐⭐⭐ |
| 5순위 | 실험 노트 (4-*) | 하 | ⭐⭐⭐⭐⭐ |

---

## 6. LLM 연동 가이드 (쿼리 분석)

쿼리 분석 시 LLM API 호출에 사용할 프롬프트 구조 예시:

```
System: 당신은 MySQL 쿼리 최적화 전문가입니다.
사용자가 제출한 SQL 쿼리를 분석하고, 아래 JSON 형식으로 응답하세요.

분석 기준:
1. 인덱스 활용 여부
2. SELECT * 사용
3. 불필요한 서브쿼리
4. 조인 방식 적절성
5. WHERE 절 컬럼 변형
6. ORDER BY / GROUP BY 최적화
7. 데이터 타입 적절성

응답 형식: { "overallScore": ..., "issues": [...], ... }

User: {사용자 입력 쿼리 + 컨텍스트}
```

---

## 7. CORS 설정

프론트엔드 개발 시 CORS 설정이 필요합니다:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000", "http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowCredentials(true);
    }
}
```