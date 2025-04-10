"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ForkedProcessTaskRunner = void 0;
const fs_1 = require("fs");
const child_process_1 = require("child_process");
const chalk = require("chalk");
const output_1 = require("../utils/output");
const utils_1 = require("./utils");
const path_1 = require("path");
const batch_messages_1 = require("./batch/batch-messages");
const strip_indents_1 = require("../utils/strip-indents");
const stream_1 = require("stream");
const pseudo_terminal_1 = require("./pseudo-terminal");
const exit_codes_1 = require("../utils/exit-codes");
const forkScript = (0, path_1.join)(__dirname, './fork.js');
const workerPath = (0, path_1.join)(__dirname, './batch/run-batch.js');
class ForkedProcessTaskRunner {
    constructor(options) {
        this.options = options;
        this.cliPath = (0, utils_1.getCliPath)();
        this.verbose = process.env.NX_VERBOSE_LOGGING === 'true';
        this.processes = new Set();
        this.finishedBatchProcesses = new Set();
        this.pseudoTerminal = pseudo_terminal_1.PseudoTerminal.isSupported()
            ? (0, pseudo_terminal_1.getPseudoTerminal)()
            : null;
    }
    async init() {
        console.log('fork process task runner init');
        if (this.pseudoTerminal) {
            await this.pseudoTerminal.init();
        }
        this.setupProcessEventListeners();
    }
    // TODO: vsavkin delegate terminal output printing
    forkProcessForBatch({ executorName, taskGraph: batchTaskGraph }, projectGraph, fullTaskGraph, env) {
        return new Promise((res, rej) => {
            try {
                const count = Object.keys(batchTaskGraph.tasks).length;
                if (count > 1) {
                    output_1.output.logSingleLine(`Running ${output_1.output.bold(count)} ${output_1.output.bold('tasks')} with ${output_1.output.bold(executorName)}`);
                }
                else {
                    const args = (0, utils_1.getPrintableCommandArgsForTask)(Object.values(batchTaskGraph.tasks)[0]);
                    output_1.output.logCommand(args.join(' '));
                }
                const p = (0, child_process_1.fork)(workerPath, {
                    stdio: ['inherit', 'inherit', 'inherit', 'ipc'],
                    env,
                });
                this.processes.add(p);
                p.once('exit', (code, signal) => {
                    this.processes.delete(p);
                    if (code === null)
                        code = (0, exit_codes_1.signalToCode)(signal);
                    if (code !== 0) {
                        rej(new Error(`"${executorName}" exited unexpectedly with code: ${code}`));
                    }
                });
                p.on('message', (message) => {
                    console.log('batch process received message', message);
                    switch (message.type) {
                        case batch_messages_1.BatchMessageType.CompleteBatchExecution: {
                            res(message.results);
                            this.finishedBatchProcesses.add(p);
                            break;
                        }
                        case batch_messages_1.BatchMessageType.RunTasks: {
                            break;
                        }
                        case 'endCommand': {
                            // this is a message from the main process that the command has completed
                            // we need to exit the child process
                            console.log('endCommand message received');
                            p.kill();
                            break;
                        }
                        default: {
                            // Re-emit any non-batch messages from the task process
                            if (process.send) {
                                process.send(message);
                            }
                        }
                    }
                });
                // Start the tasks
                p.send({
                    type: batch_messages_1.BatchMessageType.RunTasks,
                    executorName,
                    projectGraph,
                    batchTaskGraph,
                    fullTaskGraph,
                });
            }
            catch (e) {
                rej(e);
            }
        });
    }
    cleanupBatchProcesses() {
        if (this.finishedBatchProcesses.size > 0) {
            this.finishedBatchProcesses.forEach((p) => {
                p.kill();
            });
            this.finishedBatchProcesses.clear();
        }
    }
    async forkProcessLegacy(task, { temporaryOutputPath, streamOutput, pipeOutput, taskGraph, env, }) {
        return pipeOutput
            ? await this.forkProcessPipeOutputCapture(task, {
                temporaryOutputPath,
                streamOutput,
                taskGraph,
                env,
            })
            : await this.forkProcessDirectOutputCapture(task, {
                temporaryOutputPath,
                streamOutput,
                taskGraph,
                env,
            });
    }
    async forkProcess(task, { temporaryOutputPath, streamOutput, taskGraph, env, disablePseudoTerminal, }) {
        const shouldPrefix = streamOutput && process.env.NX_PREFIX_OUTPUT === 'true';
        // streamOutput would be false if we are running multiple targets
        // there's no point in running the commands in a pty if we are not streaming the output
        if (!this.pseudoTerminal ||
            disablePseudoTerminal ||
            !streamOutput ||
            shouldPrefix) {
            return this.forkProcessWithPrefixAndNotTTY(task, {
                temporaryOutputPath,
                streamOutput,
                taskGraph,
                env,
            });
        }
        else {
            return this.forkProcessWithPseudoTerminal(task, {
                temporaryOutputPath,
                streamOutput,
                taskGraph,
                env,
            });
        }
    }
    async forkProcessWithPseudoTerminal(task, { temporaryOutputPath, streamOutput, taskGraph, env, }) {
        const args = (0, utils_1.getPrintableCommandArgsForTask)(task);
        if (streamOutput) {
            output_1.output.logCommand(args.join(' '));
        }
        const childId = task.id;
        const p = await this.pseudoTerminal.fork(childId, forkScript, {
            cwd: process.cwd(),
            execArgv: process.execArgv,
            jsEnv: env,
            quiet: !streamOutput,
        });
        p.send({
            targetDescription: task.target,
            overrides: task.overrides,
            taskGraph,
            isVerbose: this.verbose,
        });
        this.processes.add(p);
        let terminalOutput = '';
        p.onOutput((msg) => {
            terminalOutput += msg;
        });
        return new Promise((res) => {
            p.onExit((code) => {
                this.processes.delete(p);
                // If the exit code is greater than 128, it's a special exit code for a signal
                if (code >= 128) {
                    process.exit(code);
                }
                this.writeTerminalOutput(temporaryOutputPath, terminalOutput);
                res({
                    code,
                    terminalOutput,
                });
            });
        });
    }
    forkProcessPipeOutputCapture(task, { streamOutput, temporaryOutputPath, taskGraph, env, }) {
        return this.forkProcessWithPrefixAndNotTTY(task, {
            streamOutput,
            temporaryOutputPath,
            taskGraph,
            env,
        });
    }
    forkProcessWithPrefixAndNotTTY(task, { streamOutput, temporaryOutputPath, taskGraph, env, }) {
        return new Promise((res, rej) => {
            try {
                const args = (0, utils_1.getPrintableCommandArgsForTask)(task);
                if (streamOutput) {
                    output_1.output.logCommand(args.join(' '));
                }
                const p = (0, child_process_1.fork)(this.cliPath, {
                    stdio: ['inherit', 'pipe', 'pipe', 'ipc'],
                    env,
                });
                this.processes.add(p);
                // Re-emit any messages from the task process
                p.on('message', (message) => {
                    if (process.send) {
                        process.send(message);
                    }
                });
                // Send message to run the executor
                p.send({
                    targetDescription: task.target,
                    overrides: task.overrides,
                    taskGraph,
                    isVerbose: this.verbose,
                });
                if (streamOutput) {
                    if (process.env.NX_PREFIX_OUTPUT === 'true') {
                        const color = getColor(task.target.project);
                        const prefixText = `${task.target.project}:`;
                        p.stdout
                            .pipe(logClearLineToPrefixTransformer(color.bold(prefixText) + ' '))
                            .pipe(addPrefixTransformer(color.bold(prefixText)))
                            .pipe(process.stdout);
                        p.stderr
                            .pipe(logClearLineToPrefixTransformer(color(prefixText) + ' '))
                            .pipe(addPrefixTransformer(color(prefixText)))
                            .pipe(process.stderr);
                    }
                    else {
                        p.stdout.pipe(addPrefixTransformer()).pipe(process.stdout);
                        p.stderr.pipe(addPrefixTransformer()).pipe(process.stderr);
                    }
                }
                let outWithErr = [];
                p.stdout.on('data', (chunk) => {
                    outWithErr.push(chunk.toString());
                });
                p.stderr.on('data', (chunk) => {
                    outWithErr.push(chunk.toString());
                });
                p.on('exit', (code, signal) => {
                    this.processes.delete(p);
                    if (code === null)
                        code = (0, exit_codes_1.signalToCode)(signal);
                    // we didn't print any output as we were running the command
                    // print all the collected output|
                    const terminalOutput = outWithErr.join('');
                    if (!streamOutput) {
                        this.options.lifeCycle.printTaskTerminalOutput(task, code === 0 ? 'success' : 'failure', terminalOutput);
                    }
                    this.writeTerminalOutput(temporaryOutputPath, terminalOutput);
                    res({ code, terminalOutput });
                });
            }
            catch (e) {
                console.error(e);
                rej(e);
            }
        });
    }
    forkProcessDirectOutputCapture(task, { streamOutput, temporaryOutputPath, taskGraph, env, }) {
        return new Promise((res, rej) => {
            try {
                const args = (0, utils_1.getPrintableCommandArgsForTask)(task);
                if (streamOutput) {
                    output_1.output.logCommand(args.join(' '));
                }
                const p = (0, child_process_1.fork)(this.cliPath, {
                    stdio: ['inherit', 'inherit', 'inherit', 'ipc'],
                    env,
                });
                this.processes.add(p);
                // Re-emit any messages from the task process
                p.on('message', (message) => {
                    if (process.send) {
                        process.send(message);
                    }
                });
                // Send message to run the executor
                p.send({
                    targetDescription: task.target,
                    overrides: task.overrides,
                    taskGraph,
                    isVerbose: this.verbose,
                });
                p.once('exit', (code, signal) => {
                    this.processes.delete(p);
                    if (code === null)
                        code = (0, exit_codes_1.signalToCode)(signal);
                    // we didn't print any output as we were running the command
                    // print all the collected output
                    let terminalOutput = '';
                    try {
                        terminalOutput = this.readTerminalOutput(temporaryOutputPath);
                        if (!streamOutput) {
                            this.options.lifeCycle.printTaskTerminalOutput(task, code === 0 ? 'success' : 'failure', terminalOutput);
                        }
                    }
                    catch (e) {
                        console.log((0, strip_indents_1.stripIndents) `
              Unable to print terminal output for Task "${task.id}".
              Task failed with Exit Code ${code} and Signal "${signal}".

              Received error message:
              ${e.message}
            `);
                    }
                    res({
                        code,
                        terminalOutput,
                    });
                });
            }
            catch (e) {
                console.error(e);
                rej(e);
            }
        });
    }
    readTerminalOutput(outputPath) {
        return (0, fs_1.readFileSync)(outputPath).toString();
    }
    writeTerminalOutput(outputPath, content) {
        (0, fs_1.writeFileSync)(outputPath, content);
    }
    setupProcessEventListeners() {
        if (this.pseudoTerminal) {
            this.pseudoTerminal.onMessageFromChildren((message) => {
                process.send(message);
            });
        }
        const messageLisnter = (message) => {
            // this.publisher.publish(message.toString());
            console.log('message from parent', message);
            if (typeof message === 'object' &&
                message.type === 'endCommand') {
                this.processes.forEach((p) => {
                    p.kill();
                });
            }
            if (this.pseudoTerminal) {
                this.pseudoTerminal.sendMessageToChildren(message);
            }
            this.processes.forEach((p) => {
                if ('connected' in p) {
                    if (p.connected) {
                        p.send(message);
                    }
                    else {
                        p.kill();
                    }
                }
            });
        };
        // When the nx process gets a message, it will be sent into the task's process
        process.on('message', messageLisnter);
        const cleanupListeners = (signal) => {
            this.processes.forEach((p) => {
                if ('connected' in p ? p.connected : p.isAlive) {
                    p.kill(signal);
                }
            });
            process.off('message', messageLisnter);
        };
        // Terminate any task processes on exit
        process.once('exit', () => {
            cleanupListeners();
        });
        process.once('SIGINT', () => {
            cleanupListeners('SIGINT');
            // we exit here because we don't need to write anything to cache.
            process.exit((0, exit_codes_1.signalToCode)('SIGINT'));
        });
        process.once('SIGTERM', () => {
            cleanupListeners('SIGTERM');
            // no exit here because we expect child processes to terminate which
            // will store results to the cache and will terminate this process
        });
        process.once('SIGHUP', () => {
            cleanupListeners('SIGHUP');
            // no exit here because we expect child processes to terminate which
            // will store results to the cache and will terminate this process
        });
    }
}
exports.ForkedProcessTaskRunner = ForkedProcessTaskRunner;
const colors = [
    chalk.green,
    chalk.greenBright,
    chalk.red,
    chalk.redBright,
    chalk.cyan,
    chalk.cyanBright,
    chalk.yellow,
    chalk.yellowBright,
    chalk.magenta,
    chalk.magentaBright,
];
function getColor(projectName) {
    let code = 0;
    for (let i = 0; i < projectName.length; ++i) {
        code += projectName.charCodeAt(i);
    }
    const colorIndex = code % colors.length;
    return colors[colorIndex];
}
/**
 * Prevents terminal escape sequence from clearing line prefix.
 */
function logClearLineToPrefixTransformer(prefix) {
    let prevChunk = null;
    return new stream_1.Transform({
        transform(chunk, _encoding, callback) {
            if (prevChunk && prevChunk.toString() === '\x1b[2K') {
                chunk = chunk.toString().replace(/\x1b\[1G/g, (m) => m + prefix);
            }
            this.push(chunk);
            prevChunk = chunk;
            callback();
        },
    });
}
function addPrefixTransformer(prefix) {
    const newLineSeparator = process.platform.startsWith('win') ? '\r\n' : '\n';
    return new stream_1.Transform({
        transform(chunk, _encoding, callback) {
            const list = chunk.toString().split(/\r\n|[\n\v\f\r\x85\u2028\u2029]/g);
            list
                .filter(Boolean)
                .forEach((m) => this.push(prefix ? prefix + ' ' + m + newLineSeparator : m + newLineSeparator));
            callback();
        },
    });
}
