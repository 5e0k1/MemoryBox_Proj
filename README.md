# MemoryBox Project

Spring Boot + JSP + MyBatis 기반의 모바일 우선 사진/영상 공유 서비스 프로젝트입니다.

## 현재 구현 범위
- MariaDB(local docker-compose) 연동
- DB의 기존 `USER`, `LOGIN_HISTORY` 테이블을 사용하는 세션 기반 로그인/로그아웃
- 로그인 성공 시 `USER.last_login_at` 업데이트
- 로그인 성공/실패 이력 `LOGIN_HISTORY` 저장
- 로그인 사용자만 `/feed`, `/feed/{id}`, `/upload` 접근 가능

## 실행
```bash
cd MemoryBox
./mvnw spring-boot:run
```

브라우저 접속:
- http://localhost:8080/login

## 기본 로컬 DB 설정
`src/main/resources/application.properties`
- host: `localhost`
- port: `3306`
- database: `memorybox_local`
- username: `memorybox`
- password: `mb1234`

## 테스트용 SQL 예시
- `MemoryBox/docs/sql/login-test-data.sql`
