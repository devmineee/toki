/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.toki.backend.webrtc.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.Continuation;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.3.1
 */
@Getter
@Slf4j
@Setter
public class KurentoRoom implements Closeable {



  private ConcurrentMap<String, UserRoomSession> participants = new ConcurrentHashMap<>();
  private MediaPipeline pipeline;

  private KurentoClient kurento;
  private String roomId;
  private String title;
  private Boolean isPlayGame;

  @Builder
  public KurentoRoom(String roomName) {

    this.roomId = roomName;

  }

  public void setTokiRooms(String roomId,String title,KurentoClient kurento) {
    this.kurento=kurento;
    this.roomId=roomId;
    this.title=title;
    this.isPlayGame=false;
  }

  public void createPipline(){
    this.pipeline = this.kurento.createMediaPipeline();
//    log.info("pipline : {} ",this.pipeline);
  }

  public void setParticipants(ConcurrentMap<String, UserRoomSession> participants) {
    this.participants = participants;
  }

  @PreDestroy
  private void shutdown() {
    this.close();
  }

  public UserRoomSession join(String userName, WebSocketSession session) throws IOException {
    log.info("ROOM {}: adding participant {}", this.roomId, userName);
    final UserRoomSession participant = new UserRoomSession(userName, this.roomId, session, this.pipeline);
    joinRoom(participant);
    participants.put(participant.getName(), participant);
    sendParticipantNames(participant);
    return participant;
  }

  public Collection<UserRoomSession> getParticipants() {
    return participants.values();
  }


  public void leave(UserRoomSession user) throws IOException {
    log.debug("PARTICIPANT {}: Leaving room {}", user.getName(), this.roomId);
    this.removeParticipant(user.getName());
    user.close();
  }

  private Collection<String> joinRoom(UserRoomSession newParticipant) throws IOException {
    final JsonObject newParticipantMsg = new JsonObject();
    newParticipantMsg.addProperty("id", "newParticipantArrived");
    newParticipantMsg.addProperty("name", newParticipant.getName());

    final List<String> participantsList = new ArrayList<>(participants.values().size());
    log.debug("ROOM {}: notifying other participants of new participant {}", roomId,
        newParticipant.getName());

    for (final UserRoomSession participant : participants.values()) {
      try {
        participant.sendMessage(newParticipantMsg);
      } catch (final IOException e) {
        log.debug("ROOM {}: participant {} could not be notified", roomId, participant.getName(), e);
      }
      participantsList.add(participant.getName());
    }

    return participantsList;
  }

  private void removeParticipant(String name) throws IOException {
    participants.remove(name);

    log.debug("ROOM {}: notifying all users that {} is leaving the room", this.roomId, name);

    final List<String> unnotifiedParticipants = new ArrayList<>();
    final JsonObject participantLeftJson = new JsonObject();
    participantLeftJson.addProperty("id", "participantLeft");
    participantLeftJson.addProperty("name", name);
    for (final UserRoomSession participant : participants.values()) {
      try {
        participant.cancelVideoFrom(name);
        participant.sendMessage(participantLeftJson);
      } catch (final IOException e) {
        unnotifiedParticipants.add(participant.getName());
      }
    }

    if (!unnotifiedParticipants.isEmpty()) {
      log.debug("ROOM {}: The users {} could not be notified that {} left the room", this.roomId,
          unnotifiedParticipants, name);
    }

  }

  public void sendParticipantNames(UserRoomSession user) throws IOException {

    final JsonArray participantsArray = new JsonArray();
    for (final UserRoomSession participant : this.getParticipants()) {
      if (!participant.equals(user)) {
        final JsonElement participantName = new JsonPrimitive(participant.getName());
        participantsArray.add(participantName);
      }
    }

    final JsonObject existingParticipantsMsg = new JsonObject();
    existingParticipantsMsg.addProperty("id", "existingParticipants");
    existingParticipantsMsg.add("data", participantsArray);
    log.debug("PARTICIPANT {}: sending a list of {} participants", user.getName(),
        participantsArray.size());
    user.sendMessage(existingParticipantsMsg);
  }

  public UserRoomSession getParticipant(String name) {
    return participants.get(name);
  }

  @Override
  public void close() {
    for (final UserRoomSession user : participants.values()) {
      try {
        user.close();
      } catch (IOException e) {
        log.debug("ROOM {}: Could not invoke close on participant {}", this.roomId, user.getName(),
            e);
      }
    }

    participants.clear();

    pipeline.release(new Continuation<Void>() {

      @Override
      public void onSuccess(Void result) throws Exception {
        log.trace("ROOM {}: Released Pipeline", KurentoRoom.this.roomId);
      }

      @Override
      public void onError(Throwable cause) throws Exception {
        log.warn("PARTICIPANT {}: Could not release Pipeline", KurentoRoom.this.roomId);
      }
    });

    log.debug("Room {} closed", this.roomId);
  }

}
