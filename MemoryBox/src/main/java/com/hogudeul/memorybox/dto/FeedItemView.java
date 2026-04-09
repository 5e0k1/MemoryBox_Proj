package com.hogudeul.memorybox.dto;

/**
 * 메인 피드 정적 목업 렌더링용 DTO.
 * 추후 Entity/Service 계층 연결 전까지 View Model 역할만 수행한다.
 */
public record FeedItemView(
        Long id,
        String mediaType,
        String thumbnailUrl,
        String title,
        String author,
        int shotYear,
        String uploadedAt,
        int likeCount,
        int commentCount,
        String[] tags
) {
}
