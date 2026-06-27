package com.example.edu.sports_predict_live.user.repository;

import com.example.edu.sports_predict_live.user.entity.Provider;
import com.example.edu.sports_predict_live.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByLoginId(String loginId);
    boolean existsByLoginIdAndDeletedAtIsNull(String loginId);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<User> findByLoginIdAndDeletedAtIsNull(String loginId);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findBySocialIdAndProviderAndDeletedAtIsNull(String socialId, Provider provider);
}