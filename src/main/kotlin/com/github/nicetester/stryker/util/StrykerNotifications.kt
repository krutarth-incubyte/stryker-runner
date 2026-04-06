package com.github.nicetester.stryker.util

import com.github.nicetester.stryker.StrykerBundle
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object StrykerNotifications {

    fun notify(project: Project, title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(StrykerBundle.message("notification.group.id"))
            .createNotification(title, content, type)
            .notify(project)
    }

    fun notifyInfo(project: Project, title: String, content: String) =
        notify(project, title, content, NotificationType.INFORMATION)

    fun notifyWarning(project: Project, title: String, content: String) =
        notify(project, title, content, NotificationType.WARNING)

    fun notifyError(project: Project, title: String, content: String) =
        notify(project, title, content, NotificationType.ERROR)
}
