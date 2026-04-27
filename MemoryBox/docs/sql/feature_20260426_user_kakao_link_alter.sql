-- USER_KAKAO_LINK 확장 DDL (MariaDB)
-- 운영 반영 전 동일 스키마/데이터 백업 후 적용하세요.

ALTER TABLE USER_KAKAO_LINK
    ADD COLUMN IF NOT EXISTS kakao_nickname VARCHAR(100) NULL AFTER use_kakao_notify,
    ADD COLUMN IF NOT EXISTS link_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER kakao_nickname,
    ADD COLUMN IF NOT EXISTS last_login_at DATETIME NULL AFTER link_status,
    ADD COLUMN IF NOT EXISTS token_updated_at DATETIME NULL AFTER last_login_at;

-- 기존 데이터 정합성 확인용(중복 건이 있으면 UNIQUE 추가 전에 정리 필요)
-- SELECT user_id, COUNT(*) cnt FROM USER_KAKAO_LINK GROUP BY user_id HAVING cnt > 1;
-- SELECT kakao_user_id, COUNT(*) cnt FROM USER_KAKAO_LINK GROUP BY kakao_user_id HAVING cnt > 1;

ALTER TABLE USER_KAKAO_LINK
    ADD UNIQUE KEY uk_user_kakao_link_user_id (user_id);

ALTER TABLE USER_KAKAO_LINK
    ADD UNIQUE KEY uk_user_kakao_link_kakao_user_id (kakao_user_id);
