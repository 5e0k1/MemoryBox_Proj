# MemoryBox 1.0.0 배포/실행 가이드

## 1) 프로파일 구조
- `application.yml`: 공통 설정 + 기본 프로파일(`local`)
- `application-local.yml`: 로컬 개발(Windows 경로 기본값, mapper DEBUG)
- `application-dev.yml`: 베타/ngrok 테스트(환경변수 우선)
- `application-prod.yml`: 운영(AWS/Linux 기준, `remember-secure=true`, INFO 로그)

## 2) 실행 방법
### local
```bash
cd MemoryBox
./mvnw spring-boot:run
```
(기본 active profile: `local`)

### dev
```bash
cd MemoryBox
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### prod
```bash
cd MemoryBox
java -jar target/MemoryBox-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### prod (외부 secret 파일 자동 로드)
`application-prod.yml`은 기본적으로 아래 경로를 자동 import 합니다.
- `/etc/memorybox/application-secret.yml`

경로를 바꾸고 싶으면 `APP_SECRET_CONFIG` 환경변수를 지정합니다.
```bash
export APP_SECRET_CONFIG=/opt/memorybox/config/application-secret.yml
java -jar target/MemoryBox-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 3) 환경변수 목록
### 필수(권장)
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

### 선택(프로파일/환경별)
- `APP_SECRET_CONFIG` (prod 외부 secret 파일 경로)
- `SERVER_PORT`
- `FEED_NEW_THRESHOLD_HOURS`
- `FFMPEG_COMMAND`
- `VIDEO_MAX_FILE_SIZE_BYTES`
- `REMEMBER_DAYS`
- `REMEMBER_COOKIE_NAME`
- `REMEMBER_SECURE`
- `LAST_ACCESS_UPDATE_MINUTES`
- `STORAGE_TYPE`
- `STORAGE_LOCAL_ROOT`
- `STORAGE_PUBLIC_BASE_URL`
- `S3_BUCKET`
- `S3_REGION`
- `S3_PREFIX`
- `CLOUDFRONT_DOMAIN`
- `CALENDAR_ENABLED`
- `CALENDAR_PERSONAL_URL`
- `CALENDAR_HOLIDAY_URL`
- `MAPPER_LOG_LEVEL` (dev)

## 4) 빌드
```bash
cd MemoryBox
./mvnw clean package
```

## 5) 실행 명령어 예시
```bash
java -jar MemoryBox.jar --spring.profiles.active=prod
```

## 6) 주의사항
- `application-secret.yml`, `application-private.yml`, `.env`는 Git에 커밋하지 않습니다.
- GitHub에 노출된 기존 자격증명/개인 URL은 반드시 폐기하고 새 값으로 교체하세요.
- 현재 구현된 저장소는 `local`이며, `s3/cloudfront` 키는 전환 준비용 설정입니다.
