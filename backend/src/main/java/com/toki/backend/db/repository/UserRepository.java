package com.toki.backend.db.repository;

import com.toki.backend.db.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * member 테이블 관련 Jpa 인터페이스
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

	Optional<User> findByUserId(String user);
	Optional<User> findByUserTag(User user);

    Optional<User> findByUserIdAndSnsType(String userId, int snsType);
}
