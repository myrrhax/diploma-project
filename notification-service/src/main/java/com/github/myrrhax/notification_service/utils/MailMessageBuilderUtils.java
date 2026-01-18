package com.github.myrrhax.notification_service.utils;

public class MailMessageBuilderUtils {
    public static String buildHtmlList(Object... args) {
        StringBuilder sb = new StringBuilder();
        for (Object arg : args) {
            sb.append("<li>");
            sb.append(arg);
            sb.append("</li>");
            sb.append('\n');
        }

        return sb.toString();
    }
}
