"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const batch_messages_1 = require("./batch-messages");
const workspace_root_1 = require("../../utils/workspace-root");
const params_1 = require("../../utils/params");
const project_graph_1 = require("../../project-graph/project-graph");
const configuration_1 = require("../../config/configuration");
const async_iterator_1 = require("../../utils/async-iterator");
const executor_utils_1 = require("../../command-line/run/executor-utils");
function getBatchExecutor(executorName, projects) {
    const [nodeModule, exportName] = executorName.split(':');
    return (0, executor_utils_1.getExecutorInformation)(nodeModule, exportName, workspace_root_1.workspaceRoot, projects);
}
async function runTasks(executorName, projectGraph, batchTaskGraph, fullTaskGraph) {
    const input = {};
    const projectsConfigurations = (0, project_graph_1.readProjectsConfigurationFromProjectGraph)(projectGraph);
    const nxJsonConfiguration = (0, configuration_1.readNxJson)();
    const batchExecutor = getBatchExecutor(executorName, projectsConfigurations.projects);
    const tasks = Object.values(batchTaskGraph.tasks);
    const context = {
        root: workspace_root_1.workspaceRoot,
        cwd: process.cwd(),
        projectsConfigurations,
        nxJsonConfiguration,
        isVerbose: false,
        projectGraph,
        taskGraph: fullTaskGraph,
    };
    for (const task of tasks) {
        const projectConfiguration = projectsConfigurations.projects[task.target.project];
        const targetConfiguration = projectConfiguration.targets[task.target.target];
        input[task.id] = (0, params_1.combineOptionsForExecutor)(task.overrides, task.target.configuration, targetConfiguration, batchExecutor.schema, null, process.cwd());
    }
    const batchStart = performance.mark('run-batch:start');
    try {
        const results = await batchExecutor.batchImplementationFactory()(batchTaskGraph, input, tasks[0].overrides, context);
        if (typeof results !== 'object') {
            throw new Error(`"${executorName} returned invalid results: ${results}`);
        }
        if ((0, async_iterator_1.isAsyncIterator)(results)) {
            const batchResults = {};
            do {
                const current = await results.next();
                if (!current.done) {
                    batchResults[current.value.task] = current.value.result;
                    process.send({
                        type: batch_messages_1.BatchMessageType.CompleteTask,
                        task: current.value.task,
                        result: current.value.result,
                    });
                }
                else {
                    break;
                }
            } while (true);
            return batchResults;
        }
        return results;
    }
    catch (e) {
        const isVerbose = tasks[0].overrides.verbose;
        console.error(isVerbose ? e : e.message);
        process.exit(1);
    }
    finally {
        const batchEnd = performance.mark('run-batch:end');
        const duration = performance.measure('run-batch', batchStart.name, batchEnd.name);
        if (process.env.NX_PERF_LOGGING === 'true') {
            console.log(`Time for 'run-batch'`, duration.duration);
        }
    }
}
process.on('message', async (message) => {
    switch (message.type) {
        case batch_messages_1.BatchMessageType.RunTasks: {
            const results = await runTasks(message.executorName, message.projectGraph, message.batchTaskGraph, message.fullTaskGraph);
            console.log("Send results to parent process");
            process.send({
                type: batch_messages_1.BatchMessageType.CompleteBatchExecution,
                results,
            }, (error) => {
                if (error) {
                    console.error('Error sending message:', error);
                    process.exit(1);
                }
                console.log('exiting run batch');
                process.exit(0);
            });
        }
    }
});
