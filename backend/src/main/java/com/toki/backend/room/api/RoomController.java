package com.toki.backend.room.api;


import com.toki.backend.auth.service.CustomOAuth2User;
import com.toki.backend.common.dto.response.CommonResponseDto;
import com.toki.backend.room.dto.RoomInfoDto;
import com.toki.backend.room.dto.request.CreateRoomRequestDto;
import com.toki.backend.room.dto.request.JoinRoomRequestDto;
import com.toki.backend.room.dto.response.CreateRoomResponseDto;
import com.toki.backend.room.dto.response.RoomListByCategoryDto;
import com.toki.backend.room.entity.Room;
import com.toki.backend.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // 화상 채팅방 페이지 별 목록 간단 조회
    @GetMapping
    public ResponseEntity<CommonResponseDto<Object>> findRoomListByPage(@RequestParam(required = false, defaultValue = "0") int page ) {

        Page<RoomInfoDto> rooms = roomService.getRoomList(page);

        return ResponseEntity.ok(CommonResponseDto.builder()
                .resultCode(200)
                .resultMessage("방 목록 조회에 성공했습니다.")
                .data(rooms)
                .build());
    }

    // 검색어가 포함된 해시태그를 가지고 있는 화상 채팅방 조회
    @GetMapping("/search/tag")
    public ResponseEntity<CommonResponseDto<Object>> findRoomListByHashTag(@RequestParam String tagName) {

        Stream<RoomInfoDto> rooms = roomService.getRoomListByTag(tagName);

        return ResponseEntity.ok(CommonResponseDto.builder()
                .resultCode(200)
                .resultMessage("방 목록 조회에 성공했습니다.")
                .data(rooms)
                .build());
    }

    // 카테고리 기반 화상 채팅방 조회
    @GetMapping("/search/category")
    public ResponseEntity<CommonResponseDto<Object>> findRoomListByCategory(@RequestParam int categoryPk,
                                                                            @RequestParam(required = false, defaultValue = "0") int page) {
        Page<RoomInfoDto> rooms = roomService.getRoomListByCategoryPk(categoryPk, page);

        return ResponseEntity.ok(CommonResponseDto.builder()
                .resultCode(200)
                .resultMessage("방 목록 조회에 성공했습니다.")
                .data(rooms)
                .build());}

    // 화상 채팅방 생성
    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody CreateRoomRequestDto createRoomRequestDto,
                                        @AuthenticationPrincipal CustomOAuth2User userPrincipal) {
        log.debug("요청 도달! {}",createRoomRequestDto.toString());
        CreateRoomResponseDto room = roomService.saveRoom(createRoomRequestDto, userPrincipal.getName());

        return ResponseEntity.ok(CommonResponseDto.builder()
                .resultCode(201)
                .resultMessage("방 생성에 성공하였습니다.")
                .data(room)
                .build());
    }

    // 타 이용자가 화상 채팅방 접속
    @PostMapping("/{roomId}")
    public ResponseEntity<?> joinRoom(@RequestBody JoinRoomRequestDto joinRoomRequestDto,
                                      @AuthenticationPrincipal CustomOAuth2User userPrincipal,
                                      @PathVariable String roomId) {

        Room room = roomService.getRoomByRoomPk(roomId);

        // 없는 방의 Id로 참가하려 한 경우
        if (room == null) {
            return ResponseEntity.ok(CommonResponseDto.builder()
                    .resultCode(404)
                    .resultMessage("없는 방입니다.")
                    .build());
        }

        // 이미 참여하고 있는 경우
        else if (
                roomService.getRoomMembersByRoomPk(room.getRoomPk()).getMembers()
                        .contains(userPrincipal.getName())
        ) {
            return ResponseEntity.ok(CommonResponseDto.builder()
                    .resultCode(403)
                    .resultMessage("이미 참여하고 있습니다.")
                    .build());
        }

        // 비밀번호나 세션Id가 일치하지 않는 경우
        else if (
                !Objects.equals(joinRoomRequestDto.getRoomPassword(), room.getPassword())
                        ||
                        !Objects.equals(joinRoomRequestDto.getSessionId(), room.getSessionId())) {
            return ResponseEntity.ok(CommonResponseDto.builder()
                    .resultCode(401)
                    .resultMessage("세션 Id 또는 비밀번호가 일치하지 않습니다.")
                    .build());
        }


        else {
            roomService.addRoomMember(roomId, userPrincipal.getName());
            return ResponseEntity.ok(CommonResponseDto.builder()
                    .resultCode(200)
                    .resultMessage("참여하였습니다.")
                    .build());
        }
    }


    // 태그 수정

    // 화상 채팅방 나가기

    // 화상 채팅방 태그 등록, 수정 및 삭제

    //
}
