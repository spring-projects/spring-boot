"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getExcludeTasksArgs = getExcludeTasksArgs;
/**
 * Returns Gradle CLI arguments to exclude dependent tasks
 * that are not part of the current execution set.
 *
 * For example, if a project defines `dependsOn: ['lint']` for the `test` target,
 * and only `test` is running, this will return: ['--exclude-task', 'lint'].
 */
function getExcludeTasksArgs(projectGraph, targets, runningTaskIds = []) {
    const excludes = new Set();
    for (const { project, target } of targets) {
        const taskDeps = projectGraph.nodes[project]?.data?.targets?.[target]?.dependsOn ?? [];
        for (const dep of taskDeps) {
            const taskId = typeof dep === 'string' ? dep : dep?.target;
            if (taskId && !runningTaskIds.includes(taskId)) {
                excludes.add(taskId);
            }
        }
    }
    const args = [];
    for (const taskId of excludes) {
        const [projectName, targetName] = taskId.split(':');
        const taskName = projectGraph.nodes[projectName]?.data?.targets?.[targetName]?.options
            ?.taskName;
        if (taskName) {
            args.push('--exclude-task', taskName);
        }
    }
    return args;
}
