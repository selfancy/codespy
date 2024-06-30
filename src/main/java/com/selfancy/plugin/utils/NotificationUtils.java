package com.selfancy.plugin.utils;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.selfancy.plugin.bundle.CodeSpyBundle;
import com.selfancy.plugin.bundle.Icons;
import lombok.experimental.UtilityClass;

/**
 * {@link NotificationUtils}
 * <p>
 * Created by mike on 2024/05/18
 */
@UtilityClass
public class NotificationUtils {

    private static final String NOTIFICATION_GROUP_ID = CodeSpyBundle.message("decompiler.notification.groupId");
    private static final String TITLE = CodeSpyBundle.message("decompiler.notification.title");

    public void sendInfo(String content) {
        send(content, NotificationType.INFORMATION);
    }

    public void sendError(String content) {
        send(content, NotificationType.ERROR);
    }

    public void sendWarning(String content) {
        send(content, NotificationType.WARNING);
    }

    private void send(String content, NotificationType notificationType) {
        Project defaultProject = ProjectManager.getInstance().getDefaultProject();
        NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP_ID)
                .createNotification(TITLE, "\n" + content, notificationType)
                .setIcon(Icons.PLUGIN_ICON)
                .notify(defaultProject);
    }
}
