"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.batchRunnerPath = void 0;
exports.default = gradlewBatch;
const devkit_1 = require("@nx/devkit");
const run_commands_impl_1 = require("nx/src/executors/run-commands/run-commands.impl");
const exec_gradle_1 = require("../../utils/exec-gradle");
const path_1 = require("path");
const child_process_1 = require("child_process");
exports.batchRunnerPath = (0, path_1.join)(__dirname, '../../../batch-runner/build/libs/nx-batch-runner.jar');
async function gradlewBatch(taskGraph, inputs, overrides, context) {
    const gradlewTaskNameToNxTaskId = new Map();
    const gradlewTaskNames = Object.keys(taskGraph.tasks).map((taskId) => {
        const gradlewTaskName = inputs[taskId].taskName;
        gradlewTaskNameToNxTaskId.set(gradlewTaskName, taskId);
        return gradlewTaskName;
    });
    try {
        const projectName = taskGraph.tasks[taskGraph.roots[0]]?.target?.project;
        let projectRoot = context.projectGraph.nodes[projectName]?.data?.root ?? '';
        const gradlewPath = (0, exec_gradle_1.findGradlewFile)((0, path_1.join)(projectRoot, 'project.json')); // find gradlew near project root
        const root = (0, path_1.join)(context.root, (0, path_1.dirname)(gradlewPath));
        const input = inputs[taskGraph.roots[0]];
        const args = typeof input.args === 'string'
            ? input.args.trim()
            : Array.isArray(input.args)
                ? input.args.join(' ')
                : '';
        const gradlewBatchStart = performance.mark(`gradlew-batch:start`);
        const batchResults = (0, child_process_1.execSync)(`java -jar ${exports.batchRunnerPath} --taskNames=${gradlewTaskNames.join(',')} --workspaceRoot=${root} ${args ? '--args=' + args : ''} ${process.env.NX_VERBOSE_LOGGING === 'true' ? '' : '--quiet'}`, {
            windowsHide: true,
            env: process.env,
            maxBuffer: run_commands_impl_1.LARGE_BUFFER,
        });
        const gradlewBatchEnd = performance.mark(`gradlew-batch:end`);
        performance.measure(`gradlew-batch`, gradlewBatchStart.name, gradlewBatchEnd.name);
        const gradlewBatchResults = JSON.parse(batchResults.toString());
        // Replace keys in gradlewBatchResults with gradlewTaskNameToNxTaskId values
        const mappedResults = Object.keys(gradlewBatchResults).reduce((acc, key) => {
            const nxTaskId = gradlewTaskNameToNxTaskId.get(key);
            if (nxTaskId) {
                acc[nxTaskId] = gradlewBatchResults[key];
            }
            return acc;
        }, {});
        return mappedResults;
    }
    catch (e) {
        devkit_1.logger.error(e);
        return taskGraph.roots.reduce((acc, key) => {
            acc[key] = { success: false, terminalOutput: e.toString() };
            return acc;
        }, {});
    }
}
