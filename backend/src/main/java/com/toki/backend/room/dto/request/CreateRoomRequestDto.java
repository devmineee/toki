package com.toki.backend.room.dto.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@Getter
@ToString
public class CreateRoomRequestDto {

    String roomName;
    Integer categoryPk;
    List<String> tags;
    Boolean isPrivate;
    String roomPassword;
    String parentRoomId;
    RoomOption roomOption;
    @Getter
    @Builder
    @ToString
    public static class RoomOption{
        String ageLimit;

        String genderCatch;
    }

}