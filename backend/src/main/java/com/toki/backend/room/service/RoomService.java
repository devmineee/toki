package com.toki.backend.room.service;


import com.toki.backend.room.dto.RoomInfoDto;
import com.toki.backend.room.dto.request.CreateRoomRequestDto;
import com.toki.backend.room.dto.response.CreateRoomResponseDto;
import com.toki.backend.room.dto.response.RoomTagDto;
import com.toki.backend.room.entity.Category;
import com.toki.backend.room.entity.Room;
import com.toki.backend.room.entity.RoomMember;
import com.toki.backend.room.entity.RoomTag;
import com.toki.backend.room.repository.CategoryRepository;
import com.toki.backend.room.repository.RoomMemberRepository;
import com.toki.backend.room.repository.RoomRepository;
import com.toki.backend.room.repository.RoomTagRepository;
import com.toki.backend.webrtc.dto.KurentoRoom;
import com.toki.backend.webrtc.dto.TokiRoomInstanceMap;
import com.toki.backend.webrtc.service.TokiRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final CategoryRepository categoryRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomTagRepository roomTagRepository;
    private final RedisTemplate<String,String> redisTemplate;
    private final TokiRoomService tokiRoomService;
    private final ConcurrentMap<String, KurentoRoom> rooms = TokiRoomInstanceMap.getInstance().getTokiRooms();

    public Page<RoomInfoDto> getRoomList(int page) {
        PageRequest pageRequest = PageRequest.of(page, 12);
        Page<Room> roomPage = roomRepository.findAll(pageRequest);

        return getRoomInfoDtos(roomPage);
    }


    public Page<RoomInfoDto> getRoomListByCategoryPk(int categoryPk, int page) {
        PageRequest pageRequest = PageRequest.of(page, 12);
        Category category = categoryRepository.findById(categoryPk).get();
        Page<Room> roomPage = roomRepository.findAllByCategory(pageRequest, category);

        return getRoomInfoDtos(roomPage);
    }

//    @Transactional
    public Stream<RoomInfoDto> getRoomListByTag(String tagName) {
        List<RoomTag> roomTags = roomTagRepository.findAllByTags(tagName);
        List<String> roomTagsPk = roomTags.stream().map(RoomTag::getRoomPk).toList();
        List<Room> rooms = roomRepository.findAllById(roomTagsPk);

        return rooms.stream().map(room -> {
            Integer currentCount = roomMemberCount(room);
            List<String> tags = roomTagRepository.findAllByRoomPk(room.getRoomPk()).stream().map(RoomTag::getTags).toList();

            return RoomInfoDto.builder()
                    .roomName(room.getTitle())
                    .categoryPk(room.getCategory().getCategoryPk())
                    .categoryName(room.getCategory().getName())
                    .isPrivate(room.getIsPrivate())
                    .currentCount(currentCount)
                    .tags(tags)
                    .build();
        });
    }

    private Page<RoomInfoDto> getRoomInfoDtos(Page<Room> roomPage) {
        return roomPage.map(room -> {
            Integer currentCount = roomMemberCount(room);

            List<String> tags = roomTagRepository.findAllByRoomPk(room.getRoomPk()).stream().map(RoomTag::getTags).toList();


            return RoomInfoDto.builder()
                    .roomName(room.getTitle())
                    .categoryPk(room.getCategory().getCategoryPk())
                    .categoryName(room.getCategory().getName())
                    .isPrivate(room.getIsPrivate())
                    .currentCount(currentCount)
                    .tags(tags)
                    .build();
        });
    }

    private int roomMemberCount(Room room) {
        return Math.toIntExact(redisTemplate.opsForSet().size("room_member:roomPk:" + room.getRoomPk()));
    }


    public Room getRoomByRoomPk(String roomPk) {
        return roomRepository.findById(roomPk).orElse(null);
    }

    public RoomMember getRoomMembersByRoomPk(String roomPk) {
        return roomMemberRepository.findById(roomPk).orElse(null);
    }

    public RoomTag getRoomTagByRoomPk(String roomPk) {
        return roomTagRepository.findById(roomPk).orElse(null);
    }

    public void addRoomMember(String roomPk, String userPk) {
        RoomMember room = getRoomMembersByRoomPk(roomPk);

        roomMemberRepository.save(room);
    }

    public void deleteRoomMember(String roomPk, String userPk) {
        RoomMember room = getRoomMembersByRoomPk(roomPk);
        redisTemplate.opsForSet().remove("room_member:roomPk:" + room.getRoomPk(),userPk);
    }

    public void updateRoomTag(String roomPk, String tags) {
        RoomTag room = getRoomTagByRoomPk(roomPk);
        redisTemplate.opsForSet().add("room_hashtag:roomPk:" + room.getRoomPk(),tags);
        roomTagRepository.save(room);
    }


//    @Transactional
    public CreateRoomResponseDto saveRoom(CreateRoomRequestDto createRoomRequestDto, String hostUserPk) {
        Room room=null;
        if(createRoomRequestDto.getParentRoomId()==null||createRoomRequestDto.getParentRoomId().isBlank()){
            room = Room.builder()
                    .title(createRoomRequestDto.getRoomName())
                    .category(categoryRepository.findById(createRoomRequestDto.getCategoryPk()).orElse(Category.builder().categoryPk(1).build()))
                    .isPrivate(createRoomRequestDto.getIsPrivate())
                    .password(createRoomRequestDto.getRoomPassword())
                    .build();
        }
        if(createRoomRequestDto.getParentRoomId()!=null&&!createRoomRequestDto.getParentRoomId().isBlank()){
            Room pRoom=roomRepository.findById(createRoomRequestDto.getParentRoomId()).get();
            room = Room.builder()
                    .parent(pRoom)
                    .title(createRoomRequestDto.getRoomName())
                    .category(categoryRepository.findById(createRoomRequestDto.getCategoryPk()).orElse(Category.builder().categoryPk(1).build()))
                    .isPrivate(createRoomRequestDto.getIsPrivate())
                    .password(createRoomRequestDto.getRoomPassword())
                    .build();
        }


        roomRepository.save(room);

//        rooms.put(room.getRoomPk(),tokiRoomService.createTokiRoom(room.getRoomPk()));

        RoomMember roomMember = RoomMember.builder()
                        .roomPk(room.getRoomPk())
                                .members(hostUserPk)
                                        .build();

        roomMemberRepository.save(roomMember);

        List<String> tags = createRoomRequestDto.getTags();
        for(String tag : tags){

        RoomTag roomTag = RoomTag.builder()
                .roomPk(room.getRoomPk())
                .tags(tag)
                .build();
        roomTagRepository.save(roomTag);
        }


        return CreateRoomResponseDto.builder()
                .roomId(room.getRoomPk())
                .roomName(room.getTitle())
                .sessionId(room.getSessionId())
                .build();

    }
}
