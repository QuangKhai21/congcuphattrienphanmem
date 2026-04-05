package com.db7.j2ee_quanlythucung;

import com.db7.j2ee_quanlythucung.entity.Pet;
import com.db7.j2ee_quanlythucung.entity.PetCategory;
import com.db7.j2ee_quanlythucung.entity.Role;
import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.repository.PetCategoryRepository;
import com.db7.j2ee_quanlythucung.repository.PetRepository;
import com.db7.j2ee_quanlythucung.repository.RoleRepository;
import com.db7.j2ee_quanlythucung.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WeightRecordPostIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PetRepository petRepository;
    @Autowired private PetCategoryRepository categoryRepository;
    @Autowired private RoleRepository roleRepository;

    private User customerUser;
    private Pet testPet;

    @BeforeEach
    void setUp() {
        // DataInitService bị @Profile("!test") skip → tạo user/pet trực tiếp trong test
        Role customerRole = roleRepository.save(
                Role.builder().name(Role.RoleType.ROLE_CUSTOMER).build());

        customerUser = userRepository.save(User.builder()
                .username("customer")
                .password("$2a$10$dummy")   // not used — @WithMockUser handles auth
                .fullName("Test Customer")
                .email("test@test.com")
                .enabled(true)
                .roles(Set.of(customerRole))
                .build());

        PetCategory dogCat = categoryRepository.save(
                PetCategory.builder().code("DOG").name("Chó").build());

        testPet = petRepository.save(Pet.builder()
                .name("Buddy")
                .category(dogCat)
                .owner(customerUser)
                .status(Pet.PetStatus.ACTIVE)
                .build());
    }

    @Test
    @WithMockUser(username = "customer")
    void postWeightRecord_savesAndRedirectsToWeightPage() throws Exception {
        mockMvc.perform(post("/pets/{petId}/weight", testPet.getId())
                        .with(csrf())
                        .param("weight", "5.2")
                        .param("recordDate", "2026-04-03")
                        .param("unit", "kg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pets/" + testPet.getId() + "/weight"))
                .andExpect(flash().attribute("success",
                        org.hamcrest.Matchers.containsString("cân nặng")));
    }
}
