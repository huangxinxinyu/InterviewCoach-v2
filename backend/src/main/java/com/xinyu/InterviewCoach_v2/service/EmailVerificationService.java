package com.xinyu.InterviewCoach_v2.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class EmailVerificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${email.verification.expiration-minutes:10}")
    private int expirationMinutes;

    private static final String REDIS_KEY_PREFIX = "email_code:";

    public void sendVerificationCode(String email) {
        String code = generateCode();

        // 存储到Redis，自动过期
        String key = REDIS_KEY_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(expirationMinutes));

        // 发送邮件
        sendEmail(email, code);
    }

    public boolean verifyCode(String email, String code) {
        String key = REDIS_KEY_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode != null && storedCode.equals(code)) {
            redisTemplate.delete(key); // 验证成功后删除
            return true;
        }

        return false;
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    private void sendEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("面试教练 - 邮箱验证码");
        message.setText("您的验证码是：" + code + "\n\n验证码有效期为" + expirationMinutes + "分钟，请尽快使用。");

        mailSender.send(message);
    }
}