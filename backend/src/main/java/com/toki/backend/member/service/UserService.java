package com.toki.backend.member.service;


import com.toki.backend.badge.dto.BadgeDTO;
import com.toki.backend.badge.service.BadgeService;
import com.toki.backend.common.utils.ConvertUserTag;
import com.toki.backend.member.dto.OtherUserDTO;
import com.toki.backend.member.dto.UserDTO;
import com.toki.backend.member.dto.UpdateUserRequestDTO;
import com.toki.backend.member.entity.User;
import com.toki.backend.member.repository.UserRepository;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BadgeService badgeService ;


    //상세조회? : 닉네임, 유저태그, sns타입, 생성일자, 프로필 이미지 URL, 자기소개, 배지 (7개)

    public UserDTO getUserDetailInfo(int  userTag) {
        // userTag로 회원 정보 조회
        User user =  userRepository.findByUserTag(userTag)
                .orElseThrow(() -> new EntityNotFoundException("해당하는 회원이 없습니다 : " + userTag));

        // 배지 정보 가져오기 -> 배지인덱스, 이름, 이미지URL을 반환.
        List<BadgeDTO> badges = badgeService.getBadgesByUserTag(userTag);

        // Member 엔티티를 MemberDTO로 변환
        return convertEntityToDTO(user, badges);
    }

    // 유저 정보 간단 조회(닉네임, 고유번호, 프로필 사진)
    public UserDTO getUserSimpleInfo(int userTag) {
        // userTag로 회원 정보 조회
        User user = userRepository.findByUserTag(userTag)
                .orElseThrow(() -> new EntityNotFoundException("UserTag에 해당하는 회원이 없습니다 :" + userTag));

        // 배지 정보 가져오기 -> 배지인덱스, 이름, 이미지URL을 반환.
        List<BadgeDTO> badges = badgeService.getBadgesByUserTag(userTag);

        // Member 엔티티를 MemberDTO로 변환
        return convertEntityToSimpleDTO(user);
    }

    // 다른 사용자의 정보 조회
    public OtherUserDTO getOtherUserInfo(int userTag) {
        Optional<User> userOptional = userRepository.findByUserTag(userTag);
        if (!userOptional.isPresent()) {
            throw new EntityNotFoundException("UserPk에 해당하는 회원이 없습니다. " + userTag);
        }

        User user = userOptional.get();

        // 필요한 필드만 OtherUserDTO 객체에 설정하여 반환
        return OtherUserDTO.builder()
                .userRole(String.valueOf(user.getUserRole())) //오류 발생하여 valueOf로 감쌌습니다.
                .userNickName(user.getUserNickName())
                .userTag(user.getUserTag())
                .selfInfo(user.getSelfInfo()) // 자기소개 정보 추가
                .build();
    }

    private UserDTO convertEntityToSimpleDTO(User user) {
        return UserDTO.builder()
                .userNickName(user.getUserNickName())
                .userTag(ConvertUserTag.convertUserTag(user.getUserTag()))
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    private UserDTO convertEntityToDTO(User user, List<BadgeDTO> badges) {
        return UserDTO.builder()
                .userPk(user.getUserPk())
                .userId(user.getUserId())
                .userRole(user.getUserRole().toString()) // <- 이 부분이 오류발생하여 toString추가하였습니다.
                .userName(user.getUserName())
                .userNickName(user.getUserNickName())
                .userTag(ConvertUserTag.convertUserTag(user.getUserTag()))
                .createAt(user.getCreateAt())
                .updateAt(user.getUpdateAt())
                .deleteAt(user.getDeleteAt())
                .isDelete(user.getIsDelete())
                .snsType(user.getSnsType())
                .profileImageUrl(user.getProfileImageUrl())
                .birthYear(user.getBirthYear())
                .selfInfo(user.getSelfInfo()) // 자기소개 정보 추가
                .badges(badges) // 배지 정보 추가
                .build();
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////
    // 멤버 정보 수정
    public UserDTO updateUser(int userTag, UpdateUserRequestDTO request) {
        User user = userRepository.findByUserTag(userTag) //userTag에 해당하는 회원을 찾는다.
                .orElseThrow(() -> new EntityNotFoundException("UserTag에 맞는 회원이 없습니다 : " + userTag));

        // 업데이트할 정보 설정(닉네임,프로필URL,E-mail대신에 셀프인포(자기소개)로 대체)
        //사용자가 존재하는 경우, request에 있는 정보를 사용하여 사용자의 닉네임, 프로필 이미지 URL, 자기소개 정보를 업데이트
        user.setUserNickName(request.getUserNickname());
        user.setProfileImageUrl(request.getProfileImageUrl());
        user.setSelfInfo(request.getSelfInfo()); // 자기소개 정보 추가

        // 엔티티 저장
        //업데이트된 사용자 정보를 데이터베이스에 저장
        User updatedUser = userRepository.save(user);

        // 업데이트된 정보를 DTO로 변환하여 반환. //배지 정보도 함께 가져온다.
        return convertEntityToDTO(updatedUser, badgeService.getBadgesByUserTag(user.getUserTag()));
    }
}

















