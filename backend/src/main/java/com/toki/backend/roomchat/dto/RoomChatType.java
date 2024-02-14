package com.toki.backend.roomchat.dto;

import org.springframework.messaging.simp.stomp.StompCommand;


public enum RoomChatType {
    ENTER,
    GAMECOMMAND,
    COMMONCHAT,
    WHISPER,
    EXIT,
    SYSTEM,
    UNKNOWN

}
