package com.pbk.flowlens.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/**
 * An IDE action that triggers FlowLens analysis for the current project.
 *
 * This class is registered in plugin.xml and appears under Tools > Run FlowLens Analysis.
 * It demonstrates how to:
 * - obtain the current Project from the action event
 * - guard against null projects (e.g., welcome screen)
 * - perform side effects (show a dialog / log messages)
 */
class RunFlowLensAnalysisAction : AnAction() {

    /**
     * Called when the user invokes the action (e.g., clicks the menu item or uses a shortcut).
     *
     * Contract:
     * - Input: [AnActionEvent] providing context such as the current project.
     * - Behavior: if a project is available, start the FlowLens analysis workflow (placeholder)
     *   and provide feedback via a message dialog and INFO log. If no project is present, inform the user.
     * - Errors: none are thrown; the action is a no-op when no project is open.
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project? = e.project ?: e.getData(CommonDataKeys.PROJECT)
        if (project == null) {
            // Inform the user that this action requires a project context.
            Messages.showWarningDialog(
                /* project = */ null,
                /* message = */ "FlowLens analysis requires an open project.",
                /* title = */ "FlowLens"
            )
            thisLogger().info("RunFlowLensAnalysisAction invoked without an open project")
            return
        }

        // Placeholder for the actual analysis logic. Replace with your orchestration entry point.
        thisLogger().info("Starting FlowLens analysis for project: ${'$'}{project.name}")

        // Provide immediate user feedback. You can replace this with a Notification if preferred.
        Messages.showInfoMessage(
            project,
            "FlowLens analysis started for project: ${'$'}{project.name}",
            "FlowLens"
        )

        // ... invoke your analysis services here and report results ...
    }

    /**
     * Called by the IDE to update the action's presentation (enabled/visible state) before rendering.
     *
     * Behavior:
     * - Enabled and visible only when a project is open to avoid confusing the user on the Welcome screen.
     */
    override fun update(e: AnActionEvent) {
        val hasProject = e.project != null || e.getData(CommonDataKeys.PROJECT) != null
        e.presentation.isEnabledAndVisible = hasProject
    }
}

