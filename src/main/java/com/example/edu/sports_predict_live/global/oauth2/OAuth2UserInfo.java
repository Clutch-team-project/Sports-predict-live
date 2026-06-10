package com.example.edu.sports_predict_live.global.oauth2;

// 소셜 제공자별 사용자 정보 추출 공통 인터페이스
public interface OAuth2UserInfo {
    String getSocialId();
    String getEmail();
    String getName();
    String getProfileImage();
}
