package com.toki.backend.friend.api;


import com.toki.backend.auth.service.CustomOAuth2User;
import com.toki.backend.common.dto.response.CommonResponseDto;
import com.toki.backend.friend.dto.request.CommonFriendDto;
import com.toki.backend.friend.dto.request.FriendRequestDto;
import com.toki.backend.friend.dto.request.FriendRequestProcessDto;
import com.toki.backend.friend.entity.Friend;
import com.toki.backend.friend.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    // 친구 목록 조회 | 받은 친구 요청 조회
    @GetMapping("/")
    public ResponseEntity<CommonResponseDto<Object>> findFriends(@AuthenticationPrincipal CustomOAuth2User userPrincipal,
                                                    @RequestParam Boolean isFriend) {
        List<Friend> data;

        if (isFriend) {
            FriendRequestDto requestDto = FriendRequestDto.builder()
                    .fromUserPk(userPrincipal.getName())
                    .build();
            data = friendService.getFriendListByFromUserAndIsFriend(requestDto);
        }
        else {
            FriendRequestDto requestDto = FriendRequestDto.builder()
                    .toUserPk(userPrincipal.getName())
                    .build();
            data = friendService.getFriendListByToUserAndNotIsFriend(requestDto);
        }

        CommonResponseDto<Object> responseDto = CommonResponseDto.builder()
                .resultCode(200)
                .resultMessage("조회에 성공했습니다.")
                .data(data)
                .build();
        return ResponseEntity.ok(responseDto);
    }


    // 친구 요청
    @PostMapping("/")
    public ResponseEntity<CommonResponseDto<Object>> requestFriend(@RequestBody FriendRequestDto requestDto,
                                           @AuthenticationPrincipal CustomOAuth2User userPrincipal) {

        requestDto.setFromUserPk(userPrincipal.getName());
        friendService.saveFriendByNotIsFriend(requestDto);

        CommonResponseDto<Object> responseDto = CommonResponseDto.builder()
                .resultCode(200)
                .resultMessage("친구 요청에 성공했습니다.")
                .build();
        return ResponseEntity.ok(responseDto);
    }


    // 친구 요청 수락 또는 거절
    @PutMapping("/")
    public ResponseEntity<CommonResponseDto<Object>> requestFriendProcess(@RequestBody FriendRequestProcessDto requestDto,
                                                                          @AuthenticationPrincipal CustomOAuth2User userPrincipal) {

        CommonFriendDto commonFriendDto = CommonFriendDto.builder()
                .fromUserPk(userPrincipal.getName())
                .toUserPk(requestDto.getToUserPk())
                .build();

        if (requestDto.getAcceptFriend()) {
            friendService.updateFriendByIsFriend(commonFriendDto);
            CommonFriendDto saveDto = CommonFriendDto.builder()
                    .fromUserPk(requestDto.getToUserPk())
                    .toUserPk(userPrincipal.getName())
                    .build();
            friendService.saveFriendByIsFriend(saveDto);

            CommonResponseDto<Object> responseDto = CommonResponseDto.builder()
                    .resultCode(200)
                    .resultMessage("친구 수락에 성공했습니다.")
                    .build();
            return ResponseEntity.ok(responseDto);


        } else {
            friendService.deleteFriend(commonFriendDto);
            CommonResponseDto<Object> responseDto = CommonResponseDto.builder()
                    .resultCode(200)
                    .resultMessage("친구 거절에 성공했습니다.")
                    .build();
            return ResponseEntity.ok(responseDto);
        }



    }

    // 친구 삭제
    @DeleteMapping("/{friendPk}")
    public ResponseEntity<?> deleteFriend(@PathVariable String friendPk,
                                          @AuthenticationPrincipal CustomOAuth2User userPrincipal) {
        CommonFriendDto myRelationDto = CommonFriendDto.builder()
                .fromUserPk(userPrincipal.getName())
                .toUserPk(friendPk)
                .build();

        friendService.deleteFriend(myRelationDto);

        CommonFriendDto friendRelationDto = CommonFriendDto.builder()
                .fromUserPk(friendPk)
                .toUserPk(userPrincipal.getName())
                .build();

        friendService.deleteFriend(friendRelationDto);

        CommonResponseDto<Object> responseDto = CommonResponseDto.builder()
                .resultCode(200)
                .resultMessage("친구 삭제에 성공했습니다.")
                .build();
        return ResponseEntity.ok(responseDto);

    }
}