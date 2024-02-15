package com.toki.backend.room.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@ToString
@Getter
public class RoomGameResponseDto {
    String title;
    String roomId;
}
