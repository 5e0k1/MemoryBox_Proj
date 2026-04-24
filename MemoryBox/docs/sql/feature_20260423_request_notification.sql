-- 요청 게시글
CREATE TABLE REQUEST_POST (
    request_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    del_at DATETIME NULL,
    PRIMARY KEY (request_id),
    CONSTRAINT fk_request_post_user FOREIGN KEY (user_id) REFERENCES USER(user_id)
);

-- 요청 댓글
CREATE TABLE REQUEST_COMMENT (
    request_comment_id BIGINT NOT NULL AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    del_yn CHAR(1) NOT NULL DEFAULT 'N',
    del_at DATETIME NULL,
    PRIMARY KEY (request_comment_id),
    CONSTRAINT fk_request_comment_post FOREIGN KEY (request_id) REFERENCES REQUEST_POST(request_id),
    CONSTRAINT fk_request_comment_user FOREIGN KEY (user_id) REFERENCES USER(user_id)
);

-- 알림
CREATE TABLE NOTIFICATION (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    receiver_user_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    notification_type VARCHAR(30) NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    message VARCHAR(500) NOT NULL,
    is_read CHAR(1) NOT NULL DEFAULT 'N',
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_user_id) REFERENCES USER(user_id),
    CONSTRAINT fk_notification_actor FOREIGN KEY (actor_user_id) REFERENCES USER(user_id)
);

CREATE INDEX idx_request_post_created_at ON REQUEST_POST(created_at);
CREATE INDEX idx_request_comment_request_id ON REQUEST_COMMENT(request_id);
CREATE INDEX idx_notification_receiver_read_created ON NOTIFICATION(receiver_user_id, is_read, created_at);
CREATE INDEX idx_notification_target ON NOTIFICATION(target_type, target_id);
