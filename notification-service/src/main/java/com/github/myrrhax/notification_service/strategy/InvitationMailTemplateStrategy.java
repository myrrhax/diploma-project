package com.github.myrrhax.notification_service.strategy;

import com.github.myrrhax.notification_service.utils.MailMessageBuilderUtils;
import com.github.myrrhax.shared.model.MailType;
import com.github.myrrhax.shared.payload.MailPayload;
import com.github.myrrhax.shared.payload.SchemeInvitationMailPayload;
import org.springframework.stereotype.Component;

@Component
public class InvitationMailTemplateStrategy implements MailTemplateStrategy {
    private static final String INVITATION_TEMPLATE = """
        <!DOCTYPE html>
        <html lang="ru">
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                .container { width: 100%%; max-width: 600px; margin: 20px auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; }
                .header { background-color: #2c3e50; color: #ffffff; padding: 20px; text-align: center; }
                .content { padding: 30px; background-color: #ffffff; }
                .footer { background-color: #f9f9f9; color: #7f8c8d; padding: 15px; text-align: center; font-size: 12px; }
                .btn { display: inline-block; padding: 12px 24px; color: #ffffff !important; background-color: #3498db; text-decoration: none; border-radius: 5px; font-weight: bold; margin-top: 20px; }
                .permissions-box { background-color: #f0f4f8; border-left: 4px solid #3498db; padding: 15px; margin: 20px 0; }
                ul { padding-left: 20px; margin: 5px 0; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h2>Приглашение к сотрудничеству</h2>
                </div>
                <div class="content">
                    <p>Здравствуйте!</p>
                    <p>Пользователь <strong>%s</strong> пригласил вас совместно работать над схемой базы данных %s.</p>
                    
                    <div class="permissions-box">
                        <strong>Вам будут предоставлены следующие права:</strong>
                        <ul>
                            %s
                        </ul>
                    </div>
                    
                    <p>Нажмите на кнопку ниже, чтобы принять приглашение и приступить к работе:</p>
                    
                    <div style="text-align: center;">
                        <a href="%s" class="btn">Присоединиться к проекту</a>
                    </div>
                    
                    <p style="font-size: 13px; color: #95a5a6; margin-top: 30px;">
                        Если кнопка не работает, скопируйте эту ссылку в браузер:<br>
                        %4$s
                    </p>
                </div>
                <div class="footer">
                    Это автоматическое уведомление. Пожалуйста, не отвечайте на него.
                </div>
            </div>
        </body>
        </html>
        """;

    @Override
    public MailType getSupportedType() {
        return MailType.SCHEME_INVITATION;
    }

    @Override
    public String buildMessage(String to, MailPayload payload) {
        if (payload instanceof SchemeInvitationMailPayload(
                String schemeName, String inviterEmail, String[] authorities, String url
        )) {
            return String.format(INVITATION_TEMPLATE,
                    inviterEmail,
                    schemeName,
                    MailMessageBuilderUtils.buildHtmlList(authorities),
                    url);
        }

        throw new RuntimeException("Unsupported payload type");
    }
}
