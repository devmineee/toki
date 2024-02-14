package com.toki.backend.roomchat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toki.backend.common.utils.TokenProvider;
import com.toki.backend.member.entity.User;
import com.toki.backend.member.repository.UserRepository;
import com.toki.backend.room.entity.Room;
import com.toki.backend.roomchat.dto.RoomChatType;
import com.toki.backend.roomchat.dto.request.RoomChatRequestMessageDto;
import com.toki.backend.roomchat.dto.response.RoomChatResponseMessageDto;
import com.toki.backend.roomchat.entity.RoomChatLog;
import com.toki.backend.roomchat.entity.RoomChatMessage;
import com.toki.backend.roomchat.repository.RoomChatLogRepository;
import com.toki.backend.roomchat.repository.RoomChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
@Slf4j
public class RoomChatService {
    private final RoomChatMessageRepository roomChatMessageRepository;
    private final RoomChatLogRepository roomChatLogRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final ChannelTopic channelTopic;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public void sendMessage(RoomChatRequestMessageDto roomChatRequestDto, String userPk,String userName) throws JsonProcessingException {

        String roomPk=roomChatRequestDto.getRoomChatPk();
        // 프론트에서 인증토큰을 잘 보내줄 수 있는 때가 온다면
//        User user =userRepository.findById(userPk).orElseThrow(()->
//                new RuntimeException("채팅 유저 조회오류")
//        );
        //임시
        User user= User.builder()
                .userNickName(userName)
                .build();

        //채팅 생성 및 저장
        RoomChatMessage roomChatMessage = RoomChatMessage.builder()
                .roomChatPk(roomPk)
                .chatType(roomChatRequestDto.getChatType())
                .fromUserNickName(user.getUserNickName())
                .fromUserPk(userPk)
                .content(roomChatRequestDto.getContent())
                .sendTo(roomChatRequestDto.getSendTo())
                .roomChatIdx(Objects.requireNonNull(redisTemplate.opsForSet().size("roomchatmessage:roomChatPk:" + roomPk))+1)
                .crateAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")).toString())
                        .build();
        log.debug("채팅 엔티티 {}",roomChatMessage.toString());
        roomChatMessageRepository.save(roomChatMessage);
        String topic = channelTopic.getTopic();


        switch (roomChatRequestDto.getChatType()) {
            case ENTER -> {
            }
            case GAMECOMMAND -> {
            }
            case COMMONCHAT -> {
            RoomChatResponseMessageDto roomChatResponseMessageDto = RoomChatResponseMessageDto.builder()
                    .roomChatPk(roomPk)
                    .fromUserNickName(userName)
                    .content(roomChatRequestDto.getContent())
                    .build();
            sendCommonChat(topic,roomChatResponseMessageDto);
                }
            case WHISPER -> {
            }
            case EXIT -> {
            }
            case SYSTEM -> {
                RoomChatResponseMessageDto roomChatResponseMessageDto = RoomChatResponseMessageDto.builder()
                        .roomChatPk(roomPk)
                        .fromUserNickName(userName)
                        .content(roomChatRequestDto.getContent())
                        .build();
                sendCommonChat(topic,roomChatResponseMessageDto);
            }
            case UNKNOWN -> {
            }
        }
        if(roomChatMessage.getRoomChatIdx()>80){
        deleteAndUpdateRoomChatLog(roomPk);
        }
    }


    public void sendCommonChat(String topic, RoomChatResponseMessageDto roomChatResponseMessageDto) throws JsonProcessingException {
        redisTemplate.convertAndSend(topic, objectMapper.writeValueAsString(roomChatResponseMessageDto));
    }
    public void sendSystemChat(String topic, RoomChatResponseMessageDto roomChatResponseMessageDto) throws JsonProcessingException {
        redisTemplate.convertAndSend(topic, objectMapper.writeValueAsString(roomChatResponseMessageDto));
    }
    public List<RoomChatMessage> getLastRoomChatMessage(String roomPk) {
        List<RoomChatMessage> roomChatMessageList = new java.util.ArrayList<>(roomChatMessageRepository.findAllByRoomChatPk(roomPk).stream().filter((v) -> v.getChatType().equals(RoomChatType.COMMONCHAT)).toList());
        if (roomChatMessageList.size() < 10) {
            RoomChatLog lastRoomChatLog = roomChatLogRepository.findTopByRoomChatPkOrderByRoomChatSessionNumber(roomPk).orElseThrow(() -> new RuntimeException("이전 기록없음"));
            try {
                List<RoomChatMessage> lastRoomChatMessageList = (List<RoomChatMessage>) objectMapper
                        .readValue(lastRoomChatLog.getRoomChatLogContent(), new TypeReference<Collection<? extends RoomChatMessage>>(){});


                    roomChatMessageList.addAll(lastRoomChatMessageList.stream().filter((v)->v.getChatType().equals(RoomChatType.COMMONCHAT)).sorted((a,b)->(int)(b.getRoomChatIdx()-a.getRoomChatIdx())).limit(20)
                            .toList());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }
        return roomChatMessageList;
    }

    public void deleteAndUpdateRoomChatLog(String roomChatPk) throws JsonProcessingException {

        List<RoomChatMessage> roomChatList=roomChatMessageRepository.findAllByRoomChatPk(roomChatPk);
        String roomChatLogText=objectMapper.writeValueAsString(roomChatList);
        roomChatLogRepository.save(RoomChatLog.builder()
                .roomChatPk(roomChatPk)
                .roomChatSessionNumber(roomChatLogRepository.countByRoomChatPk(roomChatPk)+1)
                .roomChatLogContent(roomChatLogText)
                .build()
        );
        roomChatMessageRepository.deleteAll(roomChatMessageRepository.findAllByRoomChatPk(roomChatPk));


    }
}
