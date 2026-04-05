package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.*;
import com.db7.j2ee_quanlythucung.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitService implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PetCategoryRepository petCategoryRepository;
    private final BreedRepository breedRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final PetRepository petRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final VaccinationRepository vaccinationRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final HealthMetricRepository healthMetricRepository;
    private final AppointmentRepository appointmentRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() > 0) return;

        // ==================== ROLES ====================
        Role adminRole = roleRepository.save(Role.builder().name(Role.RoleType.ROLE_ADMIN).description("Quản trị viên").build());
        Role managerRole = roleRepository.save(Role.builder().name(Role.RoleType.ROLE_MANAGER).description("Quản lý").build());
        Role staffRole = roleRepository.save(Role.builder().name(Role.RoleType.ROLE_STAFF).description("Nhân viên").build());
        Role vetRole = roleRepository.save(Role.builder().name(Role.RoleType.ROLE_VET).description("Bác sĩ thú y").build());
        Role customerRole = roleRepository.save(Role.builder().name(Role.RoleType.ROLE_CUSTOMER).description("Khách hàng").build());

        // ==================== USERS ====================
        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .fullName("Quản trị viên")
                .email("admin@petmgr.com")
                .phone("0901234567")
                .enabled(true)
                .roles(Set.of(adminRole))
                .build();
        admin = userRepository.save(admin);

        User manager = User.builder()
                .username("manager")
                .password(passwordEncoder.encode("manager123"))
                .fullName("Nguyễn Văn Quản lý")
                .email("manager@petmgr.com")
                .phone("0901234568")
                .enabled(true)
                .roles(Set.of(managerRole))
                .build();
        manager = userRepository.save(manager);

        User vet = User.builder()
                .username("vet")
                .password(passwordEncoder.encode("vet123"))
                .fullName("Dr. Nguyễn Thị Bác sĩ")
                .email("vet@petmgr.com")
                .phone("0901234570")
                .address("45 Lê Lợi, Quận 1, TP.HCM")
                .enabled(true)
                .isVet(true)
                .vetEverApproved(true)
                .specialization("Nội khoa thú nhỏ")
                .province("TP. Hồ Chí Minh")
                .city("Quận 1")
                .clinicName("Phòng khám Thú y PetCare Demo")
                .clinicAddress("45 Lê Lợi, Quận 1, TP.HCM")
                .bio("Bác sĩ thú y với hơn 10 năm kinh nghiệm khám và điều trị chó mèo.")
                .licenseNumber("BVT-DEMO-001")
                .yearsExperience(10)
                .roles(Set.of(vetRole))
                .build();
        vet = userRepository.save(vet);

        User customer = User.builder()
                .username("customer")
                .password(passwordEncoder.encode("customer123"))
                .fullName("Trần Thị Khách hàng")
                .email("customer@gmail.com")
                .phone("0901234569")
                .address("123 Đường ABC, Quận 1, TP.HCM")
                .enabled(true)
                .roles(Set.of(customerRole))
                .build();
        customer = userRepository.save(customer);

        // ==================== PET CATEGORIES ====================
        PetCategory dogCat = petCategoryRepository.save(PetCategory.builder()
                .code("DOG").name("Chó").description("Loài chó").build());
        PetCategory catCat = petCategoryRepository.save(PetCategory.builder()
                .code("CAT").name("Mèo").description("Loài mèo").build());

        // ==================== BREEDS ====================
        Breed phuQuocBreed = breedRepository.save(Breed.builder().name("Chó Phú Quốc").category(dogCat).build());
        Breed poodleBreed = breedRepository.save(Breed.builder().name("Chó Poodle").category(dogCat).build());
        Breed huskyBreed = breedRepository.save(Breed.builder().name("Chó Husky").category(dogCat).build());
        Breed britishShorthair = breedRepository.save(Breed.builder().name("Mèo Anh lông ngắn").category(catCat).build());
        Breed persianCat = breedRepository.save(Breed.builder().name("Mèo Ba Tư").category(catCat).build());

        // ==================== SAMPLE PETS ====================
        Pet pet1 = Pet.builder()
                .name("Buddy")
                .category(dogCat)
                .breed(poodleBreed)
                .owner(customer)
                .gender(Pet.Gender.MALE)
                .dateOfBirth(LocalDate.of(2022, 3, 15))
                .color("Trắng")
                .weight("8.5")
                .notes("Năng động, thân thiện")
                .status(Pet.PetStatus.ACTIVE)
                .build();
        pet1 = petRepository.save(pet1);

        Pet pet2 = Pet.builder()
                .name("Luna")
                .category(catCat)
                .breed(britishShorthair)
                .owner(customer)
                .gender(Pet.Gender.FEMALE)
                .dateOfBirth(LocalDate.of(2021, 7, 22))
                .color("Xám")
                .weight("4.2")
                .notes("Ít hoạt động, thích ngủ")
                .status(Pet.PetStatus.ACTIVE)
                .build();
        pet2 = petRepository.save(pet2);

        Pet pet3 = Pet.builder()
                .name("Max")
                .category(dogCat)
                .breed(huskyBreed)
                .owner(customer)
                .gender(Pet.Gender.MALE)
                .dateOfBirth(LocalDate.of(2020, 1, 10))
                .color("Nâu trắng")
                .weight("25.0")
                .notes("Năng động, cần nhiều vận động")
                .status(Pet.PetStatus.ACTIVE)
                .build();
        pet3 = petRepository.save(pet3);

        // ==================== SAMPLE MEDICAL RECORDS ====================
        // Pet 1 - Buddy
        medicalRecordRepository.save(MedicalRecord.builder()
                .pet(pet1)
                .veterinarian(vet)
                .recordDate(LocalDateTime.now().minusMonths(2))
                .diagnosis("Viêm da nhẹ")
                .symptoms("Ngứa, gãi nhiều, có nốt đỏ trên da")
                .treatment("Tắm bằng sữa tắm chuyên dụng, bôi thuốc kháng sinh")
                .prescription("Thuốc kháng histamine 5mg x 2 lần/ngày trong 7 ngày")
                .weight("8.2")
                .notes("Cần theo dõi sau 1 tuần")
                .build());

        medicalRecordRepository.save(MedicalRecord.builder()
                .pet(pet1)
                .veterinarian(vet)
                .recordDate(LocalDateTime.now().minusDays(15))
                .diagnosis("Kiểm tra sức khỏe định kỳ")
                .symptoms("Không có triệu chứng bất thường")
                .treatment("Khám tổng quát, tiêm vitamin")
                .prescription("Vitamin B-complex 1 viên/ngày trong 30 ngày")
                .weight("8.5")
                .notes("Sức khỏe tốt")
                .build());

        // Pet 2 - Luna
        medicalRecordRepository.save(MedicalRecord.builder()
                .pet(pet2)
                .veterinarian(vet)
                .recordDate(LocalDateTime.now().minusMonths(3))
                .diagnosis("Nhiễm trùng đường tiết niệu")
                .symptoms("Đi tiểu nhiều lần, có máu trong nước tiểu")
                .treatment("Kháng sinh, tăng cường uống nước")
                .prescription("Antibiotic A 250mg x 2 lần/ngày trong 14 ngày")
                .weight("4.0")
                .notes("Tái khám sau 2 tuần")
                .build());

        // Pet 3 - Max
        medicalRecordRepository.save(MedicalRecord.builder()
                .pet(pet3)
                .veterinarian(vet)
                .recordDate(LocalDateTime.now().minusMonths(1))
                .diagnosis("Loạn sản xương hông")
                .symptoms("Khó đứng dậy, đi lại khập khiễng")
                .treatment("Thuốc giảm đau, bổ sung glucosamine")
                .prescription("Glucosamine 500mg x 2 lần/ngày, NSAIDs khi cần")
                .weight("26.5")
                .notes("Cần kiểm tra X-quang sau 1 tháng")
                .build());

        // ==================== SAMPLE VACCINATIONS ====================
        // Pet 1 - Buddy
        vaccinationRepository.save(Vaccination.builder()
                .pet(pet1)
                .administeredBy(vet)
                .vaccineName("Vaccine dại")
                .vaccineType(Vaccination.VaccineType.RABIES)
                .vaccinationDate(LocalDate.now().minusMonths(6))
                .nextDueDate(LocalDate.now().plusMonths(6))
                .manufacturer("Zoetis")
                .batchNumber("LOT2024-001")
                .notes("Tiêm dưới da vùng cổ")
                .reminderSent(false)
                .build());

        vaccinationRepository.save(Vaccination.builder()
                .pet(pet1)
                .administeredBy(vet)
                .vaccineName("Vaccine 5 bệnh cho chó")
                .vaccineType(Vaccination.VaccineType.DHPP)
                .vaccinationDate(LocalDate.now().minusMonths(3))
                .nextDueDate(LocalDate.now().plusDays(3)) // Due soon for notification demo
                .manufacturer("Nobivac")
                .batchNumber("LOT2024-002")
                .notes("Tiêm phòng định kỳ")
                .reminderSent(false)
                .build());

        vaccinationRepository.save(Vaccination.builder()
                .pet(pet1)
                .administeredBy(vet)
                .vaccineName("Vaccine Bordetella")
                .vaccineType(Vaccination.VaccineType.BORDETELLA)
                .vaccinationDate(LocalDate.now().minusMonths(4))
                .nextDueDate(LocalDate.now().plusDays(10))
                .manufacturer("Zoetis")
                .notes("Phòng ho kennels")
                .reminderSent(false)
                .build());

        // Pet 2 - Luna
        vaccinationRepository.save(Vaccination.builder()
                .pet(pet2)
                .administeredBy(vet)
                .vaccineName("Vaccine dại")
                .vaccineType(Vaccination.VaccineType.RABIES)
                .vaccinationDate(LocalDate.now().minusMonths(6))
                .nextDueDate(LocalDate.now().minusDays(5)) // Overdue for notification demo
                .manufacturer("Merck")
                .batchNumber("LOT2024-003")
                .notes("Tiêm cho mèo")
                .reminderSent(false)
                .build());

        vaccinationRepository.save(Vaccination.builder()
                .pet(pet2)
                .administeredBy(vet)
                .vaccineName("Vaccine 4 bệnh cho mèo")
                .vaccineType(Vaccination.VaccineType.FVRCP)
                .vaccinationDate(LocalDate.now().minusMonths(2))
                .nextDueDate(LocalDate.now().plusMonths(10))
                .manufacturer("Zoetis")
                .batchNumber("LOT2024-004")
                .notes("FVRCP + FeLV")
                .reminderSent(false)
                .build());

        // Pet 3 - Max
        vaccinationRepository.save(Vaccination.builder()
                .pet(pet3)
                .administeredBy(vet)
                .vaccineName("Vaccine 5 bệnh cho chó")
                .vaccineType(Vaccination.VaccineType.DHPP)
                .vaccinationDate(LocalDate.now().minusMonths(2))
                .nextDueDate(LocalDate.now().plusMonths(10))
                .manufacturer("Nobivac")
                .batchNumber("LOT2024-005")
                .notes("DHPP + Leptospira")
                .reminderSent(false)
                .build());

        vaccinationRepository.save(Vaccination.builder()
                .pet(pet3)
                .administeredBy(vet)
                .vaccineName("Vaccine dại")
                .vaccineType(Vaccination.VaccineType.RABIES)
                .vaccinationDate(LocalDate.now().minusMonths(12))
                .nextDueDate(LocalDate.now().plusDays(1)) // Due tomorrow for notification demo
                .manufacturer("Zoetis")
                .batchNumber("LOT2024-006")
                .notes("Tiêm nhắc")
                .reminderSent(false)
                .build());

        // ==================== SAMPLE WEIGHT RECORDS ====================
        // Pet 1 - Buddy weight history
        weightRecordRepository.save(WeightRecord.builder()
                .pet(pet1)
                .recordDate(LocalDate.now().minusMonths(6))
                .weight("7.0")
                .unit("kg")
                .notes("Cân nặng ban đầu khi nhận nuôi")
                .recordedBy(vet)
                .build());

        weightRecordRepository.save(WeightRecord.builder()
                .pet(pet1)
                .recordDate(LocalDate.now().minusMonths(4))
                .weight("7.5")
                .unit("kg")
                .notes("Tăng cân tốt")
                .recordedBy(vet)
                .build());

        weightRecordRepository.save(WeightRecord.builder()
                .pet(pet1)
                .recordDate(LocalDate.now().minusMonths(2))
                .weight("8.0")
                .unit("kg")
                .notes("Cân nặng ổn định")
                .recordedBy(vet)
                .build());

        weightRecordRepository.save(WeightRecord.builder()
                .pet(pet1)
                .recordDate(LocalDate.now().minusDays(15))
                .weight("8.5")
                .unit("kg")
                .notes("Cân nặng lý tưởng")
                .recordedBy(vet)
                .build());

        // Chỉ số sức khỏe (dashboard / API xu hướng — mặc định 30 ngày, nên mốc thời gian trong ~30 ngày)
        healthMetricRepository.save(HealthMetric.builder()
                .pet(pet1)
                .metricType(HealthMetric.MetricType.WEIGHT)
                .value(7.8)
                .unit("kg")
                .recordedAt(LocalDateTime.now().minusDays(28))
                .recordedBy(vet)
                .notes("Dữ liệu mẫu")
                .build());
        healthMetricRepository.save(HealthMetric.builder()
                .pet(pet1)
                .metricType(HealthMetric.MetricType.WEIGHT)
                .value(8.0)
                .unit("kg")
                .recordedAt(LocalDateTime.now().minusDays(21))
                .recordedBy(vet)
                .notes("Dữ liệu mẫu")
                .build());
        healthMetricRepository.save(HealthMetric.builder()
                .pet(pet1)
                .metricType(HealthMetric.MetricType.WEIGHT)
                .value(8.2)
                .unit("kg")
                .recordedAt(LocalDateTime.now().minusDays(14))
                .recordedBy(vet)
                .notes("Dữ liệu mẫu")
                .build());
        healthMetricRepository.save(HealthMetric.builder()
                .pet(pet1)
                .metricType(HealthMetric.MetricType.WEIGHT)
                .value(8.5)
                .unit("kg")
                .recordedAt(LocalDateTime.now().minusDays(3))
                .recordedBy(vet)
                .notes("Dữ liệu mẫu")
                .build());

        // Pet 2 - Luna weight history
        weightRecordRepository.save(WeightRecord.builder()
                .pet(pet2)
                .recordDate(LocalDate.now().minusMonths(6))
                .weight("3.8")
                .unit("kg")
                .notes("Cân nặng ổn định")
                .recordedBy(vet)
                .build());

        weightRecordRepository.save(WeightRecord.builder()
                .pet(pet2)
                .recordDate(LocalDate.now().minusMonths(3))
                .weight("4.5")
                .unit("kg")
                .notes("Tăng cân sau khi điều trị")
                .recordedBy(vet)
                .build());

        weightRecordRepository.save(WeightRecord.builder()
                .pet(pet2)
                .recordDate(LocalDate.now().minusMonths(1))
                .weight("4.2")
                .unit("kg")
                .notes("Cân nặng lý tưởng")
                .recordedBy(vet)
                .build());

        // Pet 3 - Max weight history
        weightRecordRepository.save(WeightRecord.builder()
                .pet(pet3)
                .recordDate(LocalDate.now().minusMonths(6))
                .weight("24.0")
                .unit("kg")
                .notes("Cân nặng bình thường")
                .recordedBy(vet)
                .build());

        weightRecordRepository.save(WeightRecord.builder()
                .pet(pet3)
                .recordDate(LocalDate.now().minusMonths(3))
                .weight("25.5")
                .unit("kg")
                .notes("Tăng nhẹ cân")
                .recordedBy(vet)
                .build());

        weightRecordRepository.save(WeightRecord.builder()
                .pet(pet3)
                .recordDate(LocalDate.now().minusMonths(1))
                .weight("26.5")
                .unit("kg")
                .notes("Cần kiểm soát cân nặng")
                .recordedBy(vet)
                .build());

        // ==================== SAMPLE APPOINTMENTS ====================
        appointmentRepository.save(Appointment.builder()
                .pet(pet1)
                .staff(vet)
                .appointmentDate(LocalDateTime.now().plusDays(3).withHour(10).withMinute(0))
                .type(Appointment.AppointmentType.VACCINATION)
                .reason("Tiêm phòng định kỳ")
                .status(Appointment.AppointmentStatus.SCHEDULED)
                .build());

        appointmentRepository.save(Appointment.builder()
                .pet(pet2)
                .staff(vet)
                .appointmentDate(LocalDateTime.now().withHour(14).withMinute(30))
                .type(Appointment.AppointmentType.CHECKUP)
                .reason("Khám sức khỏe tổng quát")
                .status(Appointment.AppointmentStatus.SCHEDULED)
                .build());

        appointmentRepository.save(Appointment.builder()
                .pet(pet3)
                .staff(vet)
                .appointmentDate(LocalDateTime.now().minusDays(5).withHour(9).withMinute(0))
                .type(Appointment.AppointmentType.CHECKUP)
                .reason("Kiểm tra xương khớp")
                .status(Appointment.AppointmentStatus.COMPLETED)
                .notes("Tái khám sau 1 tháng")
                .build());

        // ==================== PRODUCTS ====================
        ProductCategory foodCat = productCategoryRepository.save(ProductCategory.builder()
                .code("FOOD").name("Thức ăn").description("Thức ăn cho thú cưng").build());
        ProductCategory toyCat = productCategoryRepository.save(ProductCategory.builder()
                .code("TOY").name("Đồ chơi").description("Đồ chơi cho thú cưng").build());

        productRepository.save(Product.builder()
                .sku("FOOD-001").name("Thức ăn Royal Canin").category(foodCat)
                .price(new BigDecimal("250000")).salePrice(new BigDecimal("220000"))
                .stockQuantity(100).unit("kg").status(Product.ProductStatus.ACTIVE).build());
        productRepository.save(Product.builder()
                .sku("TOY-001").name("Bóng cao su").category(toyCat)
                .price(new BigDecimal("50000")).stockQuantity(50).unit("cái")
                .status(Product.ProductStatus.ACTIVE).build());

        // ==================== SERVICES ====================
        serviceOfferingRepository.save(ServiceOffering.builder()
                .code("VAC").name("Tiêm phòng").description("Tiêm phòng đầy đủ")
                .price(new BigDecimal("150000")).duration("30 phút").active(true).build());
        serviceOfferingRepository.save(ServiceOffering.builder()
                .code("GROOM").name("Cắt tỉa lông").description("Cắt tỉa lông chuyên nghiệp")
                .price(new BigDecimal("200000")).duration("60 phút").active(true).build());

        System.out.println("=== Dữ liệu mẫu đã được khởi tạo ===");
        System.out.println("Đăng nhập: admin / admin123");
        System.out.println("Bác sĩ: vet / vet123");
        System.out.println("Khách hàng: customer / customer123");
        System.out.println("\nDemo thông báo:");
        System.out.println("- Luna: Vaccine dại đã QUÁ HẠN 5 ngày");
        System.out.println("- Max: Vaccine dại đến hạn NGÀY MAI");
        System.out.println("- Buddy: Vaccine 5 bệnh đến hạn sau 3 ngày");
    }
}
