# Task Execution Framework

## ApplicationTaskExecutorBuilder

The `ApplicationTaskExecutorBuilder` provides a standardized approach for accessing the `applicationTaskExecutor` bean
across different Spring integrations. This helps solve the inconsistency issues with different Spring components
requiring different executor types while all relying on the same underlying bean.

### Key Features

1. **Type Flexibility**: Provides methods to access the executor as various types:
   - `getExecutor()` - As a basic `Executor` (suitable for GraphQL)
   - `getAsyncTaskExecutor()` - As an `AsyncTaskExecutor` (suitable for MVC, WebFlux, WebSocket)
   - `getTaskExecutor()` - As a `TaskExecutor` (suitable for other integrations)

2. **Consistent Access Pattern**: All Spring Boot auto-configurations follow the same pattern for accessing the executor.

3. **Error Handling**: Provides clear error messages when the executor doesn't exist or isn't of the required type.

### Integration Guidelines

Spring component authors should:

1. Use `ApplicationTaskExecutorBuilder` rather than directly looking up the bean
2. Use the most specific type needed for your component
3. Handle cases where the executor isn't available

Example:

```java
@Autowired
private ApplicationTaskExecutorBuilder executorBuilder;

// In configuration method
try {
    AsyncTaskExecutor executor = executorBuilder.getAsyncTaskExecutor();
    // Configure with executor
} catch (IllegalStateException ex) {
    // Handle no executor case
}
```

This approach creates a more consistent experience across Spring Boot integrations and makes it easier to switch between
executor implementations like virtual threads vs platform threads. 