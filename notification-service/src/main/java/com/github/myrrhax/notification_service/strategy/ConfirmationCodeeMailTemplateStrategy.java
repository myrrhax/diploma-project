package com.github.myrrhax.notification_service.strategy;

import com.github.myrrhax.shared.model.MailType;
import com.github.myrrhax.shared.payload.ConfirmationCodeEmailPayload;
import com.github.myrrhax.shared.payload.MailPayload;
import org.springframework.stereotype.Component;

@Component
public class ConfirmationCodeeMailTemplateStrategy implements MailTemplateStrategy {
    private static final String TEMPLATE = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Код подтверждения</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f4f4;">
                <table align="center" border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px; background-color: #ffffff; margin-top: 20px; border-radius: 8px; box-shadow: 0 4px 10px rgba(0,0,0,0.1);">
                    <tr>
                        <td align="center" style="padding: 40px 20px;">
                            <h1 style="color: #333333; margin-bottom: 20px;">Подтверждение входа</h1>
                            <p style="color: #666666; font-size: 16px; margin-bottom: 30px;">
                                Используйте этот код для завершения регистрации. Код действителен в течение 24 часов.
                            </p>
            
                            <table align="center" border="0" cellpadding="0" cellspacing="0">
                                <tr>
                                    <td align="center" style="background-color: #f0f8ff; border: 2px dashed #00FFFF; border-radius: 10px; padding: 20px 40px;">
                                        <span style="font-size: 48px; font-weight: bold; color: #00CED1; letter-spacing: 10px;">${code}</span>
                                    </td>
                                </tr>
                            </table>
            
                            <p style="color: #999999; font-size: 14px; margin-top: 40px;">
                                Если вы не запрашивали этот код, просто проигнорируйте это письмо.
                            </p>
                        </td>
                    </tr>
                    <tr>
                        <td align="center" style="padding: 20px; background-color: #333333; border-bottom-left-radius: 8px; border-bottom-right-radius: 8px;">
                            <p style="color: #ffffff; font-size: 12px; margin: 0;">&copy; 2026 MyrrErm. Все права защищены.</p>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """;
    @Override
    public MailType getSupportedType() {
        return MailType.ACCOUNT_CONFIRMATION;
    }

    @Override
    public String buildMessage(String to, MailPayload payload) {
        if (payload instanceof ConfirmationCodeEmailPayload(String code)) {
            return TEMPLATE.replace("${code}", code);
        }

        throw new RuntimeException("Invalid payload type");
    }

    @Override
    public String getSubject() {
        return "Подтвердите свой Email";
    }
}
