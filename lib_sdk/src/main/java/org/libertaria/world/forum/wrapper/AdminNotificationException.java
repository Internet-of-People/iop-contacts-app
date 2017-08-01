package org.libertaria.world.forum.wrapper;

/**
 * Created by mati on 30/01/17.
 */

public class AdminNotificationException extends Exception {

    int adminNotificationType;
    String message;

    public AdminNotificationException(int adminNotificationType,String message) {
        super("Admin notification exception, please cast me..");
        this.adminNotificationType = adminNotificationType;
        this.message = message;
    }

    public int getAdminNotificationType() {
        return adminNotificationType;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
