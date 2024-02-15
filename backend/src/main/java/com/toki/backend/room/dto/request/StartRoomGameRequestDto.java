package com.toki.backend.room.dto.request;

import com.toki.backend.room.dto.GameOption;
import com.toki.backend.room.dto.GameType;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class StartRoomGameRequestDto {
    private String roomPk;
    private GameType gameType;
    private GameOption option;
    
}
