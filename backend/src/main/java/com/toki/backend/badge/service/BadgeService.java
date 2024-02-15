package com.toki.backend.badge.service;


import com.toki.backend.badge.dto.BadgeDTO;
import com.toki.backend.badge.entity.Badge;
import com.toki.backend.badge.repository.BadgeRepository;
import com.toki.backend.common.utils.ConvertUserTag;
import com.toki.backend.member.dto.UserDTO;
import com.toki.backend.member.entity.User;
import com.toki.backend.member.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserRepository userRepository;


    // 1) 모든 배지 조회
    public List<BadgeDTO> getAllBadges() {
        List<Badge> badges = badgeRepository.findAll();
        return badges.stream()
                .map(this::convertEntityToDTO)
                .collect(Collectors.toList());
    }

    // 2) 배지 추가
    public BadgeDTO saveBadge(BadgeDTO badgeDTO) {
        Badge newBadge = Badge.builder()
//                .idx(badgeDTO.getIdx())
                .name(badgeDTO.getName())
                .imageUrl(badgeDTO.getImageUrl())
                .build();
        Badge savedBadge = badgeRepository.save(newBadge);
        return convertEntityToDTO(savedBadge);
    }

    // 3) 배지 삭제
    public void deleteBadge(int idx) {
        badgeRepository.deleteById(idx);
    }

    // 4) 특정 회원이 가지고 있는 배지를 조회
    public BadgeDTO getBadgeByIdx(int userTag) {
        Badge badge = badgeRepository.findById(userTag).orElse(null);
        return badge != null ? convertEntityToDTO(badge) : null;
    }

    // 5) 사용자에게 배지 추가
    public void addBadgeToUser(int badgeIdx, int userTag) {
        Badge badge = badgeRepository.findById(badgeIdx)
                .orElseThrow(() -> new EntityNotFoundException(badgeIdx + " 인덱스를 찾을 수 없습니다."));
        User user = userRepository.findByUserTag(userTag)
                .orElseThrow(() -> new EntityNotFoundException(userTag + " 유저태그를 찾을 수 없습니다."));

        List<Badge> userBadges = user.getBadges();
        userBadges.add(badge);
        user.setBadges(userBadges);
        userRepository.save(user);
    }

    // 6) 사용자로부터 배지 제거
    public void deleteBadgeFromUser(int badgeIdx, int userTag) {
        Badge badge = badgeRepository.findById(badgeIdx)
                .orElseThrow(() -> new EntityNotFoundException(badgeIdx + " 인덱스를 찾을 수 없습니다."));
        User user = userRepository.findByUserTag(userTag)
                .orElseThrow(() -> new EntityNotFoundException(userTag + " 유저태그를 찾을 수 없습니다."));

        List<Badge> userBadges = user.getBadges();
        userBadges.removeIf(b -> b.getIdx() == badgeIdx);
        user.setBadges(userBadges);
        userRepository.save(user);
    }

    // 7) 특정 배지를 가진 모든 사용자 조회
    public List<UserDTO> getUsersByBadge(int badgeIdx) {
        Badge badge = badgeRepository.findById(badgeIdx)
                .orElseThrow(() -> new EntityNotFoundException(badgeIdx + " : 배지 인덱스를 찾을 수 없습니다."));
        List<User> users = userRepository.findByBadges(badge);
        return users.stream()
                .map(this::convertUserToDTO)
                .collect(Collectors.toList());
    }


    public List<BadgeDTO> getBadgesByUserPk(String userPk) {

        User user = userRepository.findByUserPk(userPk)
                .orElseThrow(() -> new EntityNotFoundException(userPk + " 를 찾을 수 없습니다."));

        List<Badge> userBadges = user.getBadges();
        return userBadges.stream()
                .map(this::convertEntityToDTO)
                .collect(Collectors.toList());
    }
    // 사용자 엔티티를 DTO로 변환
    private UserDTO convertUserToDTO(User user) {
        return UserDTO.builder()
                .userTag(ConvertUserTag.convertUserTag(user.getUserTag()))
                .userName(user.getUserName())
                .userEmail(user.getUserEmail())
                .badges(user.getBadges().stream()
                        .map(this::convertEntityToDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    // 배지 엔티티를 DTO로 변환
    private BadgeDTO convertEntityToDTO(Badge badge) {
        return BadgeDTO.builder()
                .idx(badge.getIdx())
                .name(badge.getName())
                .imageUrl(badge.getImageUrl())
                .build();
    }

    public List<BadgeDTO> getBadgesByUserTag(int userTag) {
        BadgeDTO badge1 = BadgeDTO.builder()
                .idx(1) //배지 인덱스
                .name("활동왕") //배지 이름
                .imageUrl("사진1") //사진URL
                .build();

        BadgeDTO badge2 = BadgeDTO.builder()
                .idx(2)
                .name("수다쟁이")
                .imageUrl("사진2")
                .build();

        BadgeDTO badge3 = BadgeDTO.builder()
                .idx(3)
                .name("척척박사")
                .imageUrl("사진3")  // 예시 지정해주어야 함
                .build();
        return Arrays.asList(badge1, badge2, badge3);
    }
}









