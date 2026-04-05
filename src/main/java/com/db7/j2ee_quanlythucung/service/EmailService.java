package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.Appointment;
import com.db7.j2ee_quanlythucung.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@petclinic.com}")
    private String fromEmail;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy", new java.util.Locale("vi", "VN"));

    @Async
    public void sendAppointmentConfirmation(Appointment appointment) {
        if (!emailEnabled) {
            log.info("Email disabled. Skipping confirmation email for appointment ID: {}", appointment.getId());
            return;
        }

        User owner = appointment.getPet().getOwner();
        if (owner.getEmail() == null || owner.getEmail().isBlank()) {
            log.warn("No email found for owner. Skipping confirmation email for appointment ID: {}", appointment.getId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(owner.getEmail());
            helper.setSubject("Xác nhận đặt lịch khám thú cưng - Pet Clinic");
            helper.setText(buildHtmlContent(appointment, owner), true);

            mailSender.send(message);
            log.info("Confirmation email sent successfully to: {} for appointment ID: {}", owner.getEmail(), appointment.getId());
        } catch (MessagingException e) {
            log.error("Failed to send confirmation email to: {} for appointment ID: {}", owner.getEmail(), appointment.getId(), e);
        }
    }

    @Async
    public void sendAppointmentReminder(Appointment appointment) {
        if (!emailEnabled) {
            log.info("Email disabled. Skipping reminder email for appointment ID: {}", appointment.getId());
            return;
        }

        User owner = appointment.getPet().getOwner();
        if (owner.getEmail() == null || owner.getEmail().isBlank()) {
            log.warn("No email found for owner. Skipping reminder email for appointment ID: {}", appointment.getId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(owner.getEmail());
            helper.setSubject("Nhắc nhở lịch khám thú cưng ngày mai - Pet Clinic");
            helper.setText(buildReminderContent(appointment, owner), true);

            mailSender.send(message);
            log.info("Reminder email sent successfully to: {} for appointment ID: {}", owner.getEmail(), appointment.getId());
        } catch (MessagingException e) {
            log.error("Failed to send reminder email to: {} for appointment ID: {}", owner.getEmail(), appointment.getId(), e);
        }
    }

    @Async
    public void sendAppointmentCancellation(Appointment appointment) {
        if (!emailEnabled) {
            log.info("Email disabled. Skipping cancellation email for appointment ID: {}", appointment.getId());
            return;
        }

        User owner = appointment.getPet().getOwner();
        if (owner.getEmail() == null || owner.getEmail().isBlank()) {
            log.warn("No email found for owner. Skipping cancellation email for appointment ID: {}", appointment.getId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(owner.getEmail());
            helper.setSubject("Thông báo hủy lịch khám - Pet Clinic");
            helper.setText(buildCancellationContent(appointment, owner), true);

            mailSender.send(message);
            log.info("Cancellation email sent successfully to: {} for appointment ID: {}", owner.getEmail(), appointment.getId());
        } catch (MessagingException e) {
            log.error("Failed to send cancellation email to: {} for appointment ID: {}", owner.getEmail(), appointment.getId(), e);
        }
    }

    private String buildHtmlContent(Appointment appointment, User owner) {
        String appointmentType = getVietnameseAppointmentType(appointment.getType());
        String status = getVietnameseStatus(appointment.getStatus());
        String dateStr = appointment.getAppointmentDate().format(DATE_FORMATTER);
        String timeStr = appointment.getAppointmentDate().format(TIME_FORMATTER);
        String fullDateStr = appointment.getAppointmentDate().format(FULL_DATE_FORMATTER);

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Xác nhận đặt lịch</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #4CAF50 0%%, #2E7D32 100%%); padding: 30px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 600;">Xác nhận đặt lịch khám</h1>
                            <p style="color: #e8f5e9; margin: 10px 0 0; font-size: 14px;">Pet Clinic - Chăm sóc thú cưng tận tâm</p>
                        </td>
                    </tr>
                    
                    <!-- Success Icon -->
                    <tr>
                        <td style="text-align: center; padding: 30px 30px 10px;">
                            <div style="width: 80px; height: 80px; background-color: #E8F5E9; border-radius: 50%%; display: inline-flex; align-items: center; justify-content: center;">
                                <span style="font-size: 40px; color: #4CAF50;">&#10003;</span>
                            </div>
                        </td>
                    </tr>
                    
                    <!-- Greeting -->
                    <tr>
                        <td style="padding: 0 30px; text-align: center;">
                            <h2 style="color: #333333; margin: 0 0 10px; font-size: 20px;">Cảm ơn quý khách!</h2>
                            <p style="color: #666666; margin: 0; font-size: 15px;">Kính gửi <strong>%s</strong>,</p>
                            <p style="color: #666666; margin: 10px 0 0; font-size: 15px;">Lịch khám của quý khách đã được đặt thành công.</p>
                        </td>
                    </tr>
                    
                    <!-- Appointment Details Card -->
                    <tr>
                        <td style="padding: 30px;">
                            <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #f8f9fa; border-radius: 8px; border: 1px solid #e0e0e0;">
                                <tr>
                                    <td colspan="2" style="padding: 15px 20px; background-color: #E8F5E9; border-bottom: 1px solid #e0e0e0;">
                                        <h3 style="margin: 0; color: #2E7D32; font-size: 16px;">&#128196; Thông tin lịch khám</h3>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 12px 20px; border-bottom: 1px solid #e0e0e0; width: 40%%;">
                                        <span style="color: #888888; font-size: 13px;">Mã lịch hẹn</span>
                                    </td>
                                    <td style="padding: 12px 20px; border-bottom: 1px solid #e0e0e0;">
                                        <strong style="color: #333333; font-size: 14px;">#%d</strong>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 12px 20px; border-bottom: 1px solid #e0e0e0;">
                                        <span style="color: #888888; font-size: 13px;">Thú cưng</span>
                                    </td>
                                    <td style="padding: 12px 20px; border-bottom: 1px solid #e0e0e0;">
                                        <strong style="color: #333333; font-size: 14px;">%s</strong>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 12px 20px; border-bottom: 1px solid #e0e0e0;">
                                        <span style="color: #888888; font-size: 13px;">Loại dịch vụ</span>
                                    </td>
                                    <td style="padding: 12px 20px; border-bottom: 1px solid #e0e0e0;">
                                        <span style="color: #333333; font-size: 14px;">%s</span>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 12px 20px; border-bottom: 1px solid #e0e0e0;">
                                        <span style="color: #888888; font-size: 13px;">Ngày khám</span>
                                    </td>
                                    <td style="padding: 12px 20px; border-bottom: 1px solid #e0e0e0;">
                                        <strong style="color: #333333; font-size: 14px;">%s</strong>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 12px 20px; border-bottom: 1px solid #e0e0e0;">
                                        <span style="color: #888888; font-size: 13px;">Giờ khám</span>
                                    </td>
                                    <td style="padding: 12px 20px; border-bottom: 1px solid #e0e0e0;">
                                        <strong style="color: #4CAF50; font-size: 16px;">%s</strong>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 12px 20px; border-bottom: 1px solid #e0e0e0;">
                                        <span style="color: #888888; font-size: 13px;">Lý do</span>
                                    </td>
                                    <td style="padding: 12px 20px; border-bottom: 1px solid #e0e0e0;">
                                        <span style="color: #333333; font-size: 14px;">%s</span>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 12px 20px;">
                                        <span style="color: #888888; font-size: 13px;">Trạng thái</span>
                                    </td>
                                    <td style="padding: 12px 20px;">
                                        <span style="background-color: #FFF3E0; color: #E65100; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600;">%s</span>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    
                    <!-- Important Notice -->
                    <tr>
                        <td style="padding: 0 30px;">
                            <div style="background-color: #FFF8E1; border-left: 4px solid #FFC107; padding: 15px 20px; border-radius: 4px;">
                                <p style="margin: 0; color: #F57F17; font-size: 14px;"><strong>&#9888; Lưu ý quan trọng:</strong></p>
                                <ul style="margin: 10px 0 0; padding-left: 20px; color: #5D4037; font-size: 13px;">
                                    <li>Vui lòng đến đúng giờ hoặc sớm 15 phút</li>
                                    <li>Mang theo sổ tiêm phòng của thú cưng (nếu có)</li>
                                    <li>Liên hệ hotline <strong>1900 xxxx</strong> nếu cần thay đổi lịch</li>
                                </ul>
                            </div>
                        </td>
                    </tr>
                    
                    <!-- Footer -->
                    <tr>
                        <td style="padding: 30px; text-align: center; background-color: #f5f5f5; border-top: 1px solid #e0e0e0;">
                            <p style="margin: 0 0 10px; color: #888888; font-size: 13px;">Pet Clinic - Hệ thống chăm sóc thú cưng</p>
                            <p style="margin: 0; color: #aaaaaa; font-size: 12px;">Email này được gửi tự động. Vui lòng không reply.</p>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(
                owner.getFullName(),
                appointment.getId(),
                appointment.getPet().getName(),
                appointmentType,
                fullDateStr,
                timeStr,
                appointment.getReason() != null ? appointment.getReason() : "Không có",
                status
        );
    }

    private String buildReminderContent(Appointment appointment, User owner) {
        String dateStr = appointment.getAppointmentDate().format(FULL_DATE_FORMATTER);
        String timeStr = appointment.getAppointmentDate().format(TIME_FORMATTER);

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Nhắc nhở lịch khám</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', sans-serif; background-color: #f4f4f4;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1);">
                    <tr>
                        <td style="background: linear-gradient(135deg, #FF9800 0%%, #F57C00 100%%); padding: 30px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 24px;">&#128276; Nhắc nhở lịch khám</h1>
                            <p style="color: #fff3e0; margin: 10px 0 0; font-size: 14px;">Pet Clinic</p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 30px; text-align: center;">
                            <p style="color: #333333; margin: 0 0 20px; font-size: 16px;">Kính gửi <strong>%s</strong>,</p>
                            <div style="background-color: #fff3e0; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
                                <p style="margin: 0; color: #E65100; font-size: 18px;"><strong>Ngày mai bạn có lịch khám cho thú cưng!</strong></p>
                            </div>
                            <p style="color: #666666; font-size: 15px;">Thú cưng: <strong>%s</strong></p>
                            <p style="color: #666666; font-size: 15px;">Ngày: <strong>%s</strong></p>
                            <p style="color: #4CAF50; font-size: 20px;"><strong>Giờ: %s</strong></p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 0 30px 30px; text-align: center;">
                            <p style="color: #888888; font-size: 13px; margin: 0;">Hãy nhớ đến đúng giờ và mang theo sổ tiêm phòng nhé!</p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 20px; text-align: center; background-color: #f5f5f5;">
                            <p style="margin: 0; color: #aaaaaa; font-size: 12px;">Pet Clinic - Chăm sóc thú cưng tận tâm</p>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(owner.getFullName(), appointment.getPet().getName(), dateStr, timeStr);
    }

    private String buildCancellationContent(Appointment appointment, User owner) {
        String dateStr = appointment.getAppointmentDate().format(FULL_DATE_FORMATTER);
        String timeStr = appointment.getAppointmentDate().format(TIME_FORMATTER);

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Thông báo hủy lịch khám</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', sans-serif; background-color: #f4f4f4;">
                <table width="100%%" cellpadding="0" cellspacing="0" style="max-width: 600px; margin: 30px auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1);">
                    <tr>
                        <td style="background: linear-gradient(135deg, #f44336 0%%, #c62828 100%%); padding: 30px; text-align: center;">
                            <h1 style="color: #ffffff; margin: 0; font-size: 24px;">Thông báo hủy lịch khám</h1>
                            <p style="color: #ffcdd2; margin: 10px 0 0; font-size: 14px;">Pet Clinic</p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 30px; text-align: center;">
                            <p style="color: #333333; margin: 0 0 20px; font-size: 16px;">Kính gửi <strong>%s</strong>,</p>
                            <p style="color: #666666; font-size: 15px;">Lịch khám sau đã bị hủy:</p>
                            <div style="background-color: #ffebee; padding: 15px; border-radius: 8px; margin: 20px 0;">
                                <p style="margin: 5px 0; color: #333333;">Thú cưng: <strong>%s</strong></p>
                                <p style="margin: 5px 0; color: #333333;">Ngày: %s</p>
                                <p style="margin: 5px 0; color: #333333;">Giờ: %s</p>
                            </div>
                            <p style="color: #666666; font-size: 14px;">Nếu bạn cần đặt lịch mới, vui lòng truy cập website của chúng tôi.</p>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding: 20px; text-align: center; background-color: #f5f5f5;">
                            <p style="margin: 0; color: #aaaaaa; font-size: 12px;">Pet Clinic - Chăm sóc thú cưng tận tâm</p>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(owner.getFullName(), appointment.getPet().getName(), dateStr, timeStr);
    }

    private String getVietnameseAppointmentType(Appointment.AppointmentType type) {
        return switch (type) {
            case CHECKUP -> "Khám tổng quát";
            case VACCINATION -> "Tiêm phòng";
            case GROOMING -> "Chăm sóc lông";
            case SURGERY -> "Phẫu thuật";
            case DENTAL -> "Chăm sóc răng";
            case OTHER -> "Khác";
        };
    }

    private String getVietnameseStatus(Appointment.AppointmentStatus status) {
        return switch (status) {
            case SCHEDULED -> "Đã đặt lịch";
            case CONFIRMED -> "Đã xác nhận";
            case IN_PROGRESS -> "Đang thực hiện";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
            case NO_SHOW -> "Không đến";
        };
    }
}
