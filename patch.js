const { readFileSync, writeFileSync } = require("fs");

console.log("Patching NX...");
/* 
console.log(
  require
    .resolve("nx/src/tasks-runner/forked-process-task-runner.js")
    .toString()
);

console.log(
  readFileSync(
    require.resolve("nx/src/tasks-runner/forked-process-task-runner.js")
  ).toString()
);

console.log(
  readFileSync(
    require("path").join(__dirname, "replacement-fork.js")
  ).toString()
);

writeFileSync(
  require.resolve("nx/src/tasks-runner/forked-process-task-runner.js"),
  readFileSync(require("path").join(__dirname, "replacement-fork.js"))
);

/*
writeFileSync(require.resolve('nx/src/tasks-runner/batch/run-batch.js'), readFileSync(
    require('path').join(__dirname, 'replacement.js')
));
*/

writeFileSync('node_modules/@nx/gradle/src/executors/gradle/gradle-batch.impl.js', readFileSync(
    require('path').join(__dirname, 'replacement-gradle-batch.js')
));
 
writeFileSync('node_modules/@nx/gradle/src/executors/gradle/get-exclude-task.js', readFileSync(
  require('path').join(__dirname, 'replacement-get-exclude-task.js')
));
