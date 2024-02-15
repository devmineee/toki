package com.toki.backend.room.entity;


import com.toki.backend.member.entity.User;
import com.toki.backend.hashTag.entity.HashTag;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private String roomPk;

    @ManyToOne
    private Room parentRoomPk;

    @Column(nullable = false)
    private String title;

    @OneToOne
    private Category category;

    @Column(nullable = false)
    private Boolean isPrivate;

    @Column
    private String password;

    @Column
    private String sessionId;

}
