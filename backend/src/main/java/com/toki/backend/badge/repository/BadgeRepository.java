package com.toki.backend.badge.repository;

import com.toki.backend.badge.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Integer> {

    Optional<Badge> findByIdx(int idx);



    List<Badge> findByMemberUserPk(String userPk);



}
