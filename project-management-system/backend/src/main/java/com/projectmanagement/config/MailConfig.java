package com.projectmanagement.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 邮件配置类
 * 确保总是有JavaMailSender可用，优先使用真实邮件发送
 */
@Configuration
public class MailConfig {

    @Value("${spring.mail.test-connection:false}")
    private boolean testConnection;
    
    @Value("${spring.mail.host:}")
    private String mailHost;
    
    @Value("${spring.mail.port:25}")
    private int mailPort;
    
    @Value("${spring.mail.username:}")
    private String mailUsername;
    
    @Value("${spring.mail.password:}")
    private String mailPassword;
    
    @Value("${spring.mail.protocol:smtp}")
    private String mailProtocol;

    /**
     * 自定义JavaMailSender配置
     * 确保总是有可用的JavaMailSender Bean
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = "mailSender")
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // 基本配置
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setProtocol(mailProtocol);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);
        mailSender.setDefaultEncoding("UTF-8");
        
        // SMTP属性配置
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", mailProtocol);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "false");
        props.put("mail.smtp.ssl.enable", "false");
        props.put("mail.debug", "true");
        
        System.out.println("📧 创建自定义JavaMailSender: " + mailHost + ":" + mailPort);
        
        return mailSender;
    }

    /**
     * 邮件配置验证器
     * 在应用启动时验证邮件配置并显示状态
     */
    @Bean
    public CommandLineRunner mailConnectionValidator(@Autowired JavaMailSender mailSender) {
        return args -> {
            String longSeparator = "================================================================================";
            System.out.println("\n" + longSeparator);
            System.out.println("📧 邮件配置验证与初始化");
            System.out.println(longSeparator);
            
            // 检查邮件配置是否完整
            boolean configComplete = isMailConfigurationComplete();
            
            if (!configComplete) {
                System.out.println("⚠️  邮件配置不完整，但JavaMailSender已创建");
                System.out.println("🔧 请完善以下配置以确保邮件正常发送:");
                System.out.println("   - spring.mail.host: " + (mailHost.isEmpty() ? "❌ 未配置" : "✅ " + mailHost));
                System.out.println("   - spring.mail.username: " + (mailUsername.isEmpty() ? "❌ 未配置" : "✅ 已配置"));
                System.out.println("   - spring.mail.password: " + (mailPassword.isEmpty() ? "❌ 未配置" : "✅ 已配置"));
                System.out.println("📧 邮件发送器类型: " + mailSender.getClass().getSimpleName());
                
            } else {
                System.out.println("✅ 邮件配置完整，使用真实邮件发送器");
                System.out.println("📡 SMTP服务器: " + mailHost + ":" + mailPort);
                System.out.println("👤 用户名: " + mailUsername);
                System.out.println("📧 邮件发送器类型: " + mailSender.getClass().getSimpleName());
                
                // 检查是否启用了邮件连接测试
                if (testConnection && mailSender instanceof JavaMailSenderImpl) {
                    JavaMailSenderImpl realMailSender = (JavaMailSenderImpl) mailSender;
                    try {
                        System.out.println("🔍 正在测试邮件服务器连接...");
                        
                        // 测试连接
                        realMailSender.testConnection();
                        
                        System.out.println("✅ 邮件服务器连接成功！");
                        System.out.println("📧 真实邮件发送功能已启用");
                        
                    } catch (Exception e) {
                        System.out.println("❌ 邮件服务器连接失败:");
                        System.out.println("   错误信息: " + e.getMessage());
                        System.out.println("⚠️  邮件发送可能失败，请检查以下配置:");
                        System.out.println("   - SMTP服务器地址是否正确");
                        System.out.println("   - 用户名和密码是否正确");
                        System.out.println("   - 网络连接是否正常");
                        System.out.println("   - 邮箱是否启用了SMTP服务");
                        System.out.println("   - 防火墙是否阻止了SMTP连接");
                    }
                } else if (!testConnection) {
                    System.out.println("ℹ️  邮件连接测试已禁用 (spring.mail.test-connection=false)");
                    System.out.println("📧 真实邮件发送功能已启用（未测试连接）");
                } else {
                    System.out.println("📧 真实邮件发送功能已启用");
                }
            }
            
            System.out.println(longSeparator + "\n");
        };
    }
    
    /**
     * 检查邮件配置是否完整
     */
    private boolean isMailConfigurationComplete() {
        return mailHost != null && !mailHost.trim().isEmpty() && 
               !mailHost.contains("xxx") &&
               mailUsername != null && !mailUsername.trim().isEmpty() && 
               !mailUsername.contains("xxx") &&
               mailPassword != null && !mailPassword.trim().isEmpty() &&
               !mailPassword.contains("xxx");
    }
}