package edge.verdant.service;

import jakarta.mail.MessagingException;

public interface EmailService {
    /**
     * 发送纯文本邮件
     * @param to  收件人地址
     * @param subject 邮件标题
     * @param content 邮件正文
     * */
     void sendSimpleMail(String to, String subject, String content);

    /** 发送 HTML / 带附件邮件 */
     void sendHtmlMail(String to, String subject, String htmlContent)throws MessagingException;
}
