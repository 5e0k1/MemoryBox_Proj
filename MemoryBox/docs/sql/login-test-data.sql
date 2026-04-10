-- 기존 스키마를 변경하지 않고 로그인 검증용으로만 사용하세요.
-- USER / LOGIN_HISTORY 테이블 생성 DDL은 포함하지 않습니다.

-- 1) BCrypt 해시 예시 (raw: Passw0rd!23)
-- $2a$10$2Y9yHWW7sGecMwG9A3wznu5PaKzDg9MZjE7AI6/U3xM6l1KQLW3m6

-- 2) 테스트 계정 insert 예시
INSERT INTO `USER` (
    user_id,
    login_id,
    password_hash,
    display_name,
    role,
    created_at,
    updated_at,
    last_login_at,
    del_yn,
    del_at
) VALUES (
    900001,
    'memorybox_admin',
    '$2a$10$2Y9yHWW7sGecMwG9A3wznu5PaKzDg9MZjE7AI6/U3xM6l1KQLW3m6',
    '관리자',
    'ADMIN',
    NOW(),
    NOW(),
    NULL,
    'N',
    NULL
);

-- 3) 로그인 검증용 조회
SELECT user_id, login_id, display_name, role, last_login_at, del_yn
FROM `USER`
WHERE login_id = 'memorybox_admin';

SELECT lh_id, user_id, login_id_input, login_at, success_yn
FROM LOGIN_HISTORY
WHERE login_id_input = 'memorybox_admin'
ORDER BY lh_id DESC
LIMIT 20;
