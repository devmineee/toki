package com.toki.backend.member;

import com.toki.backend.common.dto.response.CommonResponseDto;
import com.toki.backend.common.utils.ConvertUserTag;
import com.toki.backend.member.dto.OtherUserDTO;
import com.toki.backend.member.dto.UpdateUserRequestDTO;
import com.toki.backend.member.dto.UserDTO;
import com.toki.backend.member.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
//2월15일 수정 : userTag를 String타입으로 고치고, converUserTag메서드를 이용하도록 수정하였습니다.(Util)

//    2024,02,08 유저정보조회 수정하였음 -> 다른 사람 정보를 조회시, 민감정보는 제외하고 DTO에 담는다.
    private final UserService userService;



//     유저 정보 조회
//     userTag 조회할 유저의 태그
//     infoType 조회할 정보의 타입 (상세 또는 간단)
//     principal 현재 사용자의 Principal 객체
//     return 조회된 유저 정보

    @GetMapping("/{userTag}")
    public ResponseEntity<CommonResponseDto<UserDTO>> getUserInfo(
            @RequestParam(name = "info_type", defaultValue = "detail") String infoType,
            Principal principal
    ) {
        try {
            String currentUserTag = principal.getName();

            UserDTO result;
            if ("simple".equals(infoType)) {
                // 간단한 정보 조회 요청일 경우
                result = userService.getUserSimpleInfo(currentUserTag);
            } else {
                // 상세 정보 조회 요청일 경우
                result = userService.getUserDetailInfo(currentUserTag);
            }

            return ResponseEntity.ok(CommonResponseDto.<UserDTO>builder()
                    .resultCode(200)
                    .resultMessage("유저 정보를 조회합니다.")
                    .data(result)
                    .build());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    // 요청한 유저를 찾을 수 없는 경우
                    .body(CommonResponseDto.<UserDTO>builder()
                            .resultCode(404)
                            .resultMessage("유저를 찾을 수 없습니다.")
                            .data(null)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    // 서버 에러 발생 시
                    .body(CommonResponseDto.<UserDTO>builder()
                            .resultCode(500)
                            .resultMessage("서버 에러: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }






//     다른 사용자 정보 조회
//     userTag 조회할 사용자의 태그
//     return 조회된 사용자 정보

    @GetMapping("/others/{userTag}")
    public ResponseEntity<CommonResponseDto<OtherUserDTO>> getOtherUserInfo(@PathVariable String userTag) {
        //@PathVariable: URL 경로에 있는 변수 값을 메소드의 매개변수로 받아올 때 사용
        // int타입에서 String타입으로 수정하였습니다.
        try {
            OtherUserDTO result = userService.getOtherUserInfo(ConvertUserTag.convertUserTag(userTag)); //수정한 부분
            return ResponseEntity.ok(CommonResponseDto.<OtherUserDTO>builder()
                    .resultCode(200)
                    .resultMessage("다른 사용자 정보를 조회합니다.")
                    .data(result)
                    .build());
        } catch (EntityNotFoundException e) {
            // 요청한 유저를 찾을 수 없는 경우
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponseDto.<OtherUserDTO>builder()
                            .resultCode(404)
                            .resultMessage("유저를 찾을 수 없습니다.")
                            .data(null)
                            .build());
        } catch (Exception e) {
            // 서버 에러 발생 시
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponseDto.<OtherUserDTO>builder()
                            .resultCode(500)
                            .resultMessage("서버 에러: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }


//     유저 정보 수정
//     userTag 수정할 유저의 태그
//     request 수정할 정보를 담은 DTO 객체
//     return 수정된 유저 정보
//
    @PutMapping("/{userTag}")
    public ResponseEntity<CommonResponseDto<UserDTO>> updateUser(@PathVariable String userTag, @RequestBody UpdateUserRequestDTO request) {
        // int타입에서 String타입으로 수정하였습니다.
        //@PathVariable: URL 경로에 있는 변수 값을 메소드의 매개변수로 받아올 때 사용
        //@RequestBody: HTTP 요청의 본문에 있는 데이터를 메소드의 매개변수로 받아올 때 사용.
        try {
            UserDTO result = userService.updateUser(ConvertUserTag.convertUserTag(userTag), request);//수정한 부분
            return ResponseEntity.ok(CommonResponseDto.<UserDTO>builder()
                    .resultCode(200)
                    .resultMessage("유저 정보를 수정합니다.")
                    .data(result)
                    .build());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponseDto.<UserDTO>builder()
                            .resultCode(404)
                            .resultMessage("유저를 찾을 수 없습니다.")
                            .data(null)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponseDto.<UserDTO>builder()
                            .resultCode(500)
                            .resultMessage("서버 에러: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }
}
