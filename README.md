# MemoryBox Project

Spring Boot + JSP 기반의 모바일 우선 사진/영상 공유 서비스 목업 프로젝트입니다.

## 현재 구현 범위 (UI 초안)
- 로그인 페이지 (`/`, `/login`)
- 메인 피드 페이지 (`/feed`)
- 업로드/상세보기는 링크 구조 확인용 placeholder 페이지 제공

## 폴더 구조 (초안)
```text
MemoryBox/
 └─ src/
    └─ main/
       ├─ java/com/hogudeul/memorybox/
       │  ├─ MemoryBoxApplication.java
       │  ├─ controller/PageController.java
       │  └─ dto/FeedItemView.java
       ├─ resources/
       │  ├─ application.properties
       │  └─ static/
       │     ├─ css/
       │     │  ├─ common.css
       │     │  ├─ login.css
       │     │  └─ feed.css
       │     └─ js/
       │        └─ feed.js
       └─ webapp/WEB-INF/views/
          ├─ login.jsp
          ├─ feed.jsp
          ├─ upload-placeholder.jsp
          └─ detail-placeholder.jsp
```

## 실행
```bash
cd MemoryBox
./mvnw spring-boot:run
```

브라우저 접속:
- http://localhost:8080/
- http://localhost:8080/login
- http://localhost:8080/feed
