package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.Role;
import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.repository.RoleRepository;
import com.db7.j2ee_quanlythucung.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Page<User> search(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return userRepository.findAll(pageable);
        }
        return userRepository.findByFullNameContainingIgnoreCase(keyword, pageable);
    }

    @Transactional
    public User create(User user, String... roleNames) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            roleRepository.findByName(Role.RoleType.valueOf(roleName))
                    .ifPresent(roles::add);
        }
        if (!roles.isEmpty()) {
            user.setRoles(roles);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User update(User user) {
        User existing = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        existing.setFullName(user.getFullName());
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        existing.setAddress(user.getAddress());
        existing.setAvatarUrl(user.getAvatarUrl());
        existing.setEnabled(user.getEnabled());
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(existing);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public void resetPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        user.setPassword(passwordEncoder.encode("123456"));
        userRepository.save(user);
    }

    @Transactional
    public void toggleStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        user.setEnabled(!Boolean.TRUE.equals(user.getEnabled()));
        userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * Đăng ký làm bác sĩ: gửi yêu cầu chờ admin duyệt.
     * Đặt isVetPending = true, chưa gán ROLE_VET hay isVet = true.
     */
    @Transactional
    public User registerAsVet(Long userId,
                              String clinicName,
                              String specialization,
                              String province,
                              String city,
                              String clinicAddress,
                              String bio,
                              String licenseNumber,
                              Integer yearsExperience) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        if (Boolean.TRUE.equals(user.getVetRegistrationBlocked())) {
            throw new IllegalStateException("Tài khoản bị chặn, không thể đăng ký làm bác sĩ thú y");
        }
        if (Boolean.TRUE.equals(user.getIsVet())) {
            throw new IllegalStateException("Tài khoản đã đăng ký làm bác sĩ thú y");
        }
        if (Boolean.TRUE.equals(user.getIsVetPending())) {
            throw new IllegalStateException("Yêu cầu đăng ký bác sĩ đang chờ duyệt, không cần gửi lại");
        }
        user.setClinicName(clinicName.trim());
        user.setSpecialization(specialization.trim());
        user.setProvince(province.trim());
        user.setCity(city != null && !city.isBlank() ? city.trim() : null);
        user.setClinicAddress(clinicAddress != null && !clinicAddress.isBlank() ? clinicAddress.trim() : null);
        user.setBio(bio != null && !bio.isBlank() ? bio.trim() : null);
        user.setLicenseNumber(licenseNumber != null && !licenseNumber.isBlank() ? licenseNumber.trim() : null);
        user.setYearsExperience(yearsExperience != null && yearsExperience >= 0 ? yearsExperience : 0);
        user.setIsVetPending(true);
        user.setVetRequestNote(null);
        return userRepository.save(user);
    }

    /**
     * Hủy yêu cầu đăng ký bác sĩ (chờ duyệt) của chính user.
     */
    @Transactional
    public void cancelPendingVetRequest(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        if (!Boolean.TRUE.equals(user.getIsVetPending())) {
            throw new IllegalStateException("Không có yêu cầu đăng ký bác sĩ nào đang chờ duyệt");
        }
        user.setIsVetPending(false);
        user.setVetRequestNote(null);
        userRepository.save(user);
    }

    /**
     * Hủy đăng ký bác sĩ: xóa ROLE_VET khỏi danh sách vai trò và đặt isVet = false.
     */
    @Transactional
    public void unregisterAsVet(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        if (!Boolean.TRUE.equals(user.getIsVet())) {
            throw new IllegalStateException("Tài khoản chưa đăng ký làm bác sĩ thú y");
        }
        user.setIsVet(false);
        user.setVetRegistrationBlocked(false);
        user.setVetEverApproved(false);
        user.getRoles().removeIf(r -> r.getName() == Role.RoleType.ROLE_VET);
        userRepository.save(user);
    }

    /* ===== Phê duyệt bác sĩ ===== */

    @Transactional(readOnly = true)
    public List<User> findPendingVetRequests() {
        return userRepository.findByIsVetPendingTrue();
    }

    @Transactional(readOnly = true)
    public List<User> findApprovedVets() {
        return userRepository.findAdminVetManagementList();
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail);
    }

    /**
     * Admin duyệt yêu cầu đăng ký bác sĩ: xác nhận isVet = true, gán ROLE_VET.
     */
    @Transactional
    public User approveVetRequest(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        if (!Boolean.TRUE.equals(user.getIsVetPending())) {
            throw new IllegalStateException("Tài khoản không có yêu cầu chờ duyệt");
        }
        if (Boolean.TRUE.equals(user.getIsVet())) {
            throw new IllegalStateException("Tài khoản đã là bác sĩ");
        }
        user.setIsVetPending(false);
        user.setIsVet(true);
        user.setVetRequestNote(null);
        user.setVetRegistrationBlocked(false);
        user.setVetEverApproved(true);
        Role vetRole = roleRepository.findByName(Role.RoleType.ROLE_VET)
                .orElseThrow(() -> new RuntimeException("Vai trò bác sĩ chưa được cấu hình"));
        user.getRoles().add(vetRole);
        return userRepository.save(user);
    }

    /**
     * Admin từ chối yêu cầu đăng ký bác sĩ: xóa trạng thái chờ, ghi chú, chặn đăng ký lại.
     */
    @Transactional
    public void rejectVetRequest(Long userId, String note) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        if (!Boolean.TRUE.equals(user.getIsVetPending())) {
            throw new IllegalStateException("Tài khoản không có yêu cầu chờ duyệt");
        }
        user.setIsVetPending(false);
        user.setVetRequestNote(note != null ? note.trim() : null);
        user.setVetRegistrationBlocked(true);
        userRepository.save(user);
    }

    /**
     * Admin thêm bác sĩ trực tiếp: tìm tài khoản bằng username hoặc email, gán ROLE_VET + isVet = true.
     */
    @Transactional
    public User addVetByAdmin(String usernameOrEmail) {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail.trim())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        if (Boolean.TRUE.equals(user.getIsVet())) {
            throw new IllegalStateException("Tài khoản đã là bác sĩ");
        }
        if (Boolean.TRUE.equals(user.getVetRegistrationBlocked())) {
            throw new IllegalStateException("Tài khoản bị chặn đăng ký làm bác sĩ");
        }
        user.setIsVetPending(false);
        user.setIsVet(true);
        user.setVetRequestNote(null);
        user.setVetRegistrationBlocked(false);
        user.setVetEverApproved(true);
        Role vetRole = roleRepository.findByName(Role.RoleType.ROLE_VET)
                .orElseThrow(() -> new RuntimeException("Vai trò bác sĩ chưa được cấu hình"));
        user.getRoles().add(vetRole);
        return userRepository.save(user);
    }

    /**
     * Admin hủy bác sĩ: xóa ROLE_VET, đặt isVet = false, vetRemoved = true.
     * Giữ lại vetEverApproved = true để hiển thị trong tab "Bị hủy".
     */
    @Transactional
    public void removeVet(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        user.setIsVet(false);
        user.setIsVetPending(false);
        user.setVetRequestNote(null);
        user.setVetRegistrationBlocked(false);
        user.setVetRemoved(true);
        user.getRoles().removeIf(r -> r.getName() == Role.RoleType.ROLE_VET);
        userRepository.save(user);
    }

    /** Danh sách tài khoản đã bị hủy bác sĩ */
    @Transactional(readOnly = true)
    public List<User> findRemovedVets() {
        return userRepository.findByVetRemovedTrue();
    }

    /**
     * Admin thêm lại bác sĩ đã từng bị hủy: gán ROLE_VET, isVet = true.
     */
    @Transactional
    public User restoreVet(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        if (!Boolean.TRUE.equals(user.getVetRemoved())) {
            throw new IllegalStateException("Tài khoản không nằm trong danh sách bị hủy");
        }
        if (Boolean.TRUE.equals(user.getIsVet())) {
            throw new IllegalStateException("Tài khoản đã là bác sĩ");
        }
        if (Boolean.TRUE.equals(user.getVetRegistrationBlocked())) {
            throw new IllegalStateException("Tài khoản đang bị chặn đăng ký bác sĩ");
        }
        user.setIsVet(true);
        user.setVetRemoved(false);
        user.setVetRequestNote(null);
        Role vetRole = roleRepository.findByName(Role.RoleType.ROLE_VET)
                .orElseThrow(() -> new RuntimeException("Vai trò bác sĩ chưa được cấu hình"));
        user.getRoles().add(vetRole);
        return userRepository.save(user);
    }

    /**
     * Admin chặn tài khoản không được đăng ký làm bác sĩ: gỡ vai trò bác sĩ (nếu có) và đặt cờ chặn.
     */
    @Transactional
    public void blockVetRegistration(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        user.setIsVet(false);
        user.setIsVetPending(false);
        user.getRoles().removeIf(r -> r.getName() == Role.RoleType.ROLE_VET);
        user.setVetRegistrationBlocked(true);
        user.setVetEverApproved(true);
        userRepository.save(user);
    }

    /**
     * Admin mở chặn: cho phép tài khoản đăng ký làm bác sĩ trở lại (không tự gán lại vai trò bác sĩ).
     */
    @Transactional
    public void unblockVetRegistration(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        if (!Boolean.TRUE.equals(user.getVetRegistrationBlocked())) {
            throw new IllegalStateException("Tài khoản không đang bị chặn đăng ký bác sĩ");
        }
        user.setVetRegistrationBlocked(false);
        user.setVetRequestNote(null);
        userRepository.save(user);
    }

    /** Danh sách tài khoản đang bị chặn đăng ký bác sĩ */
    @Transactional(readOnly = true)
    public List<User> findBlockedVets() {
        return userRepository.findByVetRegistrationBlockedTrue();
    }

    /**
     * Cập nhật hồ sơ bác sĩ đã đăng ký.
     */
    @Transactional
    public User updateVetProfile(Long userId,
                                 String clinicName,
                                 String specialization,
                                 String province,
                                 String city,
                                 String clinicAddress,
                                 String bio,
                                 String licenseNumber,
                                 Integer yearsExperience) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        if (!Boolean.TRUE.equals(user.getIsVet())) {
            throw new IllegalStateException("Tài khoản chưa đăng ký làm bác sĩ thú y");
        }
        user.setClinicName(clinicName.trim());
        user.setSpecialization(specialization.trim());
        user.setProvince(province.trim());
        user.setCity(city != null && !city.isBlank() ? city.trim() : null);
        user.setClinicAddress(clinicAddress != null && !clinicAddress.isBlank() ? clinicAddress.trim() : null);
        user.setBio(bio != null && !bio.isBlank() ? bio.trim() : null);
        user.setLicenseNumber(licenseNumber != null && !licenseNumber.isBlank() ? licenseNumber.trim() : null);
        user.setYearsExperience(yearsExperience != null && yearsExperience >= 0 ? yearsExperience : 0);
        return userRepository.save(user);
    }
}
