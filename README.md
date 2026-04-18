#  SQLCoach — SQL 쿼리 최적화 코칭 플랫폼

> SQLP 자격증을 준비하면서, 단순히 시험 공부에 그치지 않고  
> **배운 개념을 직접 서비스로 만들어 검증하는 프로젝트**입니다.

---

##  왜 이 프로젝트를 만들었나

SQLP를 공부하면서 느낀 점이 하나 있습니다.

**"더 나은 쿼리를 짜는 법을 알려주는 서비스가 거의 없다."**

실행계획 읽는 법, 인덱스 설계 전략, 쿼리 튜닝 기법 — 이런 내용은 책이나 블로그에 흩어져 있지만, 내가 작성한 쿼리에 대해 **즉각적인 피드백**을 받을 수 있는 곳은 찾기 어려웠습니다.

그래서 직접 만들기로 했습니다.

---

##  주요 기능

###  쿼리 분석 & 피드백
- SQL 쿼리를 입력하면 LLM이 분석하여 **최적화 포인트**를 알려줍니다
- 인덱스 활용 여부, SELECT * 남용, 서브쿼리 비효율 등을 진단
- **개선된 쿼리 제안**과 함께 SQLP 관련 학습 포인트 제공

###  SQL 퀴즈 & 챌린지
- 인덱스, 조인, 실행계획, 트랜잭션 등 카테고리별 문제 풀이
- 난이도별 (입문 / 중급 / 고급) 문제 구성
- 문제마다 **상세한 해설**과 SQLP 연관 개념 제공

### 학습 통계
- 카테고리별 정답률 추적
- 취약 영역 자동 분석

---

##  기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Spring Boot 3.5, Java 17 |
| Database | MySQL 8.0 |
| ORM | Spring Data JPA |
| AI | LLM API (쿼리 분석) |
| Frontend | React |

---

##  프로젝트 구조

```
sqlcoach-api/
├── src/main/java/com/sqlcoach/
│   ├── domain/
│   │   ├── query/          # 쿼리 분석 도메인
│   │   ├── quiz/           # 퀴즈 도메인
│   │   └── experiment/     # 실험 노트 도메인
│   ├── global/
│   │   ├── config/         # CORS, JPA 등 설정
│   │   └── error/          # 공통 에러 처리
│   └── SqlcoachApplication.java
├── src/main/resources/
│   ├── application.properties
│   └── application-local.properties  (gitignore)
└── build.gradle
```

---

##  로컬 실행 방법

### 1. MySQL 데이터베이스 생성

```sql
CREATE DATABASE sqlcoach DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 환경 설정

`application-local.properties` 파일 생성:

```properties
spring.datasource.username=root
spring.datasource.password=본인_비밀번호
```

### 3. 서버 실행

```bash
./gradlew bootRun
```

---

##  개발 로드맵

- [x] 프로젝트 초기 설정 (Spring Boot + MySQL)
- [ ] 쿼리 분석 API (POST /api/v1/queries/analyze)
- [ ] 퀴즈 목록/상세 API
- [ ] 퀴즈 답안 제출 & 채점
- [ ] 학습 통계 API
- [ ] LLM 연동 (쿼리 분석 엔진)
- [ ] 실험 노트 CRUD
- [ ] 프론트엔드 연동
- [ ] 배포

---

## SQLP 학습 기록

이 프로젝트를 만들면서 직접 실험하고 학습한 내용을 기록합니다.

| # | 주제 | 핵심 내용 | 날짜 |
|---|------|----------|------|
| 1 | — | 작성 예정 | — |

> 학습 기록은 프로젝트를 진행하면서 계속 업데이트됩니다.

---

##  API 명세

자세한 API 명세는 [API_SPEC.md](./API_SPEC.md)를 참고해주세요.

---

<p align="center">
  <sub>SQLP 준비 + 포트폴리오 프로젝트 | 2026</sub>
</p>