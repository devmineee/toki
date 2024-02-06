package com.toki.backend.member.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UpdateUserRequestDTO {

    private String userNickname; // 업데이트할 사용자 닉네임
    private String profileImageUrl;// 업데이트할 프로필 이미지 URL
    private String userEmail;// 업데이트할 사용자 이메일 주소
}