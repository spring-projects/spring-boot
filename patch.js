const { readFileSync, writeFileSync } = require('fs');

console.log('Patching NX...');

/*
writeFileSync(require.resolve('nx/src/tasks-runner/forked-process-task-runner.js'), readFileSync(
    require('path').join(__dirname, 'replacement-fork.js')
));

writeFileSync(require.resolve('nx/src/tasks-runner/batch/run-batch.js'), readFileSync(
    require('path').join(__dirname, 'replacement.js')
));

writeFileSync('node_modules/@nx/gradle/src/executors/gradlew/gradlew-batch.impl.js', readFileSync(
    require('path').join(__dirname, 'replacement-batch.js')
));
*/