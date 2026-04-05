package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<User> findByFullNameContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u")
    long countAllUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true")
    long countActiveUsers();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") com.db7.j2ee_quanlythucung.entity.Role.RoleType roleName);

    /* ===== Bác sĩ ===== */
    List<User> findByIsVetPendingTrue();
    List<User> findByIsVetTrue();

    /** Tab admin: bác sĩ đang hoạt động + tài khoản đã chặn sau khi từng là bác sĩ */
    @Query("SELECT u FROM User u WHERE u.isVet = true OR (u.vetRegistrationBlocked = true AND u.vetEverApproved = true) ORDER BY u.id DESC")
    List<User> findAdminVetManagementList();

    long countByIsVetTrue();

    long countByIsVetPendingTrue();

    /** Tab admin: tài khoản đang bị chặn đăng ký bác sĩ (từng là bác sĩ hoặc từng bị từ chối) */
    List<User> findByVetRegistrationBlockedTrue();

    /** Tab admin: tài khoản đã bị hủy bác sĩ */
    List<User> findByVetRemovedTrue();

    @Query("SELECT u FROM User u WHERE u.username = :input OR u.email = :input")
    Optional<User> findByUsernameOrEmail(@Param("input") String input);
}
