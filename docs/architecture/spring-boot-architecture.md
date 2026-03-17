# Spring Boot 框架架构深度解析

> 基于 Spring Boot 最新源码分析，帮助你快速理解框架核心设计

---

## 目录

1. [框架总览](#1-框架总览)
2. [项目模块结构](#2-项目模块结构)
3. [核心模块详解](#3-核心模块详解)
4. [启动流程详解](#4-启动流程详解)
5. [自动配置机制](#5-自动配置机制)
6. [条件注解体系](#6-条件注解体系)
7. [Starter 机制](#7-starter-机制)
8. [内嵌容器机制](#8-内嵌容器机制)
9. [外部化配置](#9-外部化配置)
10. [Actuator 监控](#10-actuator-监控)
11. [关键设计模式](#11-关键设计模式)
12. [架构图索引](#12-架构图索引)

---

## 1. 框架总览

Spring Boot 是基于 Spring Framework 的快速开发框架，核心理念是 **"约定优于配置"（Convention over Configuration）**。

### 核心价值

| 特性 | 说明 |
|------|------|
| **自动配置** | 根据 classpath 中的依赖自动配置 Spring Bean |
| **Starter 依赖** | 一站式依赖管理，引入一个 starter 即获得完整功能栈 |
| **内嵌容器** | 内嵌 Tomcat/Jetty/Netty，无需部署 WAR 文件 |
| **零 XML 配置** | 全注解驱动，告别繁琐的 XML 配置 |
| **生产就绪** | 内置健康检查、指标监控、外部化配置等生产级特性 |

### Spring 生态层次关系

```
┌─────────────────────────────────────────────────┐
│              你的应用代码                         │
├─────────────────────────────────────────────────┤
│           Spring Boot (自动配置 + Starter)        │
├─────────────────────────────────────────────────┤
│        Spring Framework (IoC, AOP, MVC...)       │
├─────────────────────────────────────────────────┤
│          JDK + 第三方库 (Tomcat, Jackson...)      │
└─────────────────────────────────────────────────┘
```

---

## 2. 项目模块结构

Spring Boot 源码采用 Gradle 多模块构建，组织清晰：

```
spring-boot/
├── core/                          # 核心模块 (最重要)
│   ├── spring-boot                # 核心引导类 (SpringApplication)
│   ├── spring-boot-autoconfigure  # 自动配置基础设施
│   ├── spring-boot-test           # 测试支持
│   ├── spring-boot-test-autoconfigure  # 测试自动配置
│   ├── spring-boot-docker-compose # Docker Compose 集成
│   └── spring-boot-testcontainers # Testcontainers 集成
│
├── module/                        # 功能模块 (125+ 个)
│   ├── spring-boot-actuator       # 监控端点
│   ├── spring-boot-webmvc         # Spring MVC 支持
│   ├── spring-boot-webflux        # WebFlux 响应式支持
│   ├── spring-boot-data-jpa       # JPA 数据访问
│   ├── spring-boot-security       # 安全框架
│   ├── spring-boot-jdbc           # JDBC 支持
│   ├── spring-boot-kafka          # Kafka 消息队列
│   ├── spring-boot-redis          # Redis 缓存
│   └── ...                        # 更多技术集成模块
│
├── starter/                       # Starter 依赖聚合 (100+ 个)
│   ├── spring-boot-starter        # 基础 starter
│   ├── spring-boot-starter-web    # Web 开发 starter
│   ├── spring-boot-starter-data-jpa  # JPA starter
│   └── ...
│
├── loader/                        # 可执行 JAR 加载器
│   ├── spring-boot-loader         # Fat JAR 启动器
│   ├── spring-boot-loader-tools   # 打包工具
│   └── spring-boot-jarmode-tools  # JAR 模式工具
│
├── build-plugin/                  # 构建插件
│   ├── spring-boot-gradle-plugin  # Gradle 插件
│   ├── spring-boot-maven-plugin   # Maven 插件
│   └── spring-boot-antlib         # Ant 支持
│
├── buildpack/                     # 云原生 Buildpack
├── cli/                           # 命令行工具
├── platform/                      # 依赖版本管理 (BOM)
│   ├── spring-boot-dependencies   # 第三方依赖版本
│   └── spring-boot-internal-dependencies
│
├── configuration-metadata/        # 配置元数据处理
├── documentation/                 # 文档
└── test-support/                  # 测试基础设施
```

---

## 3. 核心模块详解

### 3.1 spring-boot (核心引导模块)

**位置**: `core/spring-boot`

这是整个框架的心脏，包含：

| 包/类 | 职责 |
|-------|------|
| `SpringApplication` | **应用启动引导类**，整个启动流程的入口 |
| `SpringBootConfiguration` | 标记 Spring Boot 配置类 |
| `WebApplicationType` | 判断 Web 应用类型 (NONE/SERVLET/REACTIVE) |
| `ApplicationRunner` / `CommandLineRunner` | 启动后回调接口 |
| `Banner` | 启动 Banner 打印 |
| `context/` | 应用上下文相关工具 |
| `env/` | 环境与配置属性处理 |
| `logging/` | 日志系统抽象 (Logback/Log4j2) |
| `web/` | 内嵌 Web 容器管理 |
| `ssl/` | SSL/TLS 配置 |
| `json/` | JSON 序列化支持 |

### 3.2 spring-boot-autoconfigure (自动配置基础)

**位置**: `core/spring-boot-autoconfigure`

| 类/注解 | 职责 |
|---------|------|
| `@SpringBootApplication` | **复合注解** = `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan` |
| `@EnableAutoConfiguration` | 启用自动配置，通过 `@Import` 导入 `AutoConfigurationImportSelector` |
| `@AutoConfiguration` | 标记自动配置类 (替代旧的 `@Configuration`) |
| `AutoConfigurationImportSelector` | **核心选择器**，从 `META-INF/spring/` 加载自动配置类列表 |
| `condition/` | 条件注解体系 (`@ConditionalOnClass`, `@ConditionalOnMissingBean` 等) |

### 3.3 module (功能模块群)

每个 module 封装了一个特定技术领域的自动配置：

```
module/
├── spring-boot-webmvc           → Spring MVC 自动配置
├── spring-boot-webflux          → WebFlux 响应式 Web
├── spring-boot-data-jpa         → JPA + Hibernate
├── spring-boot-jdbc             → DataSource + JdbcTemplate
├── spring-boot-security         → Spring Security
├── spring-boot-actuator         → 健康检查 + 监控端点
├── spring-boot-kafka            → Kafka 生产者/消费者
├── spring-boot-data-redis       → Redis 连接与操作
├── spring-boot-cache            → 缓存抽象
├── spring-boot-validation       → Bean Validation
├── spring-boot-mail             → 邮件发送
├── spring-boot-flyway           → 数据库迁移
└── ... (125+ 模块)
```

### 3.4 loader (可执行 JAR 加载器)

Spring Boot 的 "Fat JAR" 特性依赖此模块：

| 类 | 职责 |
|----|------|
| `JarLauncher` | 以 JAR 形式启动应用 |
| `WarLauncher` | 以 WAR 形式启动应用 |
| `PropertiesLauncher` | 可配置的高级启动器 |
| `LaunchedClassLoader` | 自定义类加载器，能加载嵌套 JAR |

---

## 4. 启动流程详解

### 4.1 启动入口

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

### 4.2 SpringApplication.run() 核心流程

以下是 `SpringApplication.run()` 方法的完整执行链路（源码位于 `SpringApplication.java:304-342`）：

```
SpringApplication.run(MyApplication.class, args)
        │
        ▼
┌─ new SpringApplication(primarySources) ──────────────────────┐
│  1. 记录主配置类 (primarySources)                               │
│  2. 推断 Web 应用类型: WebApplicationType.deduce()              │
│     → SERVLET / REACTIVE / NONE                               │
│  3. 加载 BootstrapRegistryInitializer (SpringFactoriesLoader) │
│  4. 加载 ApplicationContextInitializer                        │
│  5. 加载 ApplicationListener                                  │
│  6. 推断主类: deduceMainApplicationClass()                     │
└──────────────────────────────────────────────────────────────┘
        │
        ▼
┌─ run(args) ──────────────────────────────────────────────────┐
│                                                              │
│  ① Startup.create()                                         │
│     → 记录启动时间                                             │
│                                                              │
│  ② createBootstrapContext()                                  │
│     → 创建引导上下文                                            │
│                                                              │
│  ③ getRunListeners(args)                                     │
│     → 获取 SpringApplicationRunListener 列表                  │
│     → 通过 SpringFactoriesLoader 加载                         │
│                                                              │
│  ④ listeners.starting()                                      │
│     → 发布 ApplicationStartingEvent                           │
│                                                              │
│  ⑤ prepareEnvironment()                                     │
│     → 创建 ConfigurableEnvironment                            │
│     → 配置 PropertySources (命令行参数、配置文件等)                │
│     → 发布 ApplicationEnvironmentPreparedEvent                │
│                                                              │
│  ⑥ printBanner()                                            │
│     → 打印 Spring Boot Banner                                │
│                                                              │
│  ⑦ createApplicationContext()                               │
│     → 根据 WebApplicationType 创建对应的 ApplicationContext    │
│     → SERVLET → AnnotationConfigServletWebServerAppCtx       │
│     → REACTIVE → AnnotationConfigReactiveWebServerAppCtx     │
│     → NONE → AnnotationConfigApplicationContext              │
│                                                              │
│  ⑧ prepareContext()                                         │
│     → 设置 Environment                                       │
│     → 执行 ApplicationContextInitializer                     │
│     → 发布 ApplicationContextInitializedEvent                │
│     → 注册主配置类为 BeanDefinition                             │
│     → 发布 ApplicationPreparedEvent                           │
│                                                              │
│  ⑨ refreshContext()                                         │
│     → 调用 AbstractApplicationContext.refresh()               │
│     → ★ 这里触发所有 Bean 的创建和自动配置 ★                      │
│     → 启动内嵌 Web 服务器                                      │
│                                                              │
│  ⑩ afterRefresh()                                           │
│     → 预留扩展点 (当前为空实现)                                   │
│                                                              │
│  ⑪ listeners.started()                                      │
│     → 发布 ApplicationStartedEvent                            │
│                                                              │
│  ⑫ callRunners()                                            │
│     → 执行 ApplicationRunner 和 CommandLineRunner             │
│                                                              │
│  ⑬ listeners.ready()                                        │
│     → 发布 ApplicationReadyEvent                              │
│     → 应用完全就绪！                                            │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

### 4.3 启动事件时间线

```
时间线 ──────────────────────────────────────────────────────────────▶

ApplicationStartingEvent        应用刚开始启动
       │
ApplicationEnvironmentPreparedEvent    环境准备完毕
       │
ApplicationContextInitializedEvent     上下文初始化完毕
       │
ApplicationPreparedEvent               上下文准备完毕，即将刷新
       │
    ┌──┴── refresh() ───┐
    │  Bean 创建         │
    │  自动配置生效       │
    │  Web 服务器启动     │
    └──────────────────┘
       │
ContextRefreshedEvent                  上下文刷新完毕
       │
ApplicationStartedEvent                应用已启动
       │
ApplicationReadyEvent                  应用完全就绪，可以服务请求
```

---

## 5. 自动配置机制

### 5.1 @SpringBootApplication 注解拆解

```
@SpringBootApplication
     │
     ├── @SpringBootConfiguration
     │        └── @Configuration          → 标记为 Spring 配置类
     │
     ├── @EnableAutoConfiguration
     │        ├── @AutoConfigurationPackage  → 注册基础包路径
     │        └── @Import(AutoConfigurationImportSelector.class)
     │              → ★ 自动配置的核心入口 ★
     │
     └── @ComponentScan                   → 扫描当前包及子包的组件
```

### 5.2 自动配置加载流程

```
@EnableAutoConfiguration
        │
        ▼
AutoConfigurationImportSelector.getAutoConfigurationEntry()
        │
        ├── 1. getCandidateConfigurations()
        │      → ImportCandidates.load(AutoConfiguration.class)
        │      → 读取所有 JAR 中的:
        │         META-INF/spring/
        │           org.springframework.boot.autoconfigure.AutoConfiguration.imports
        │      → 得到候选自动配置类列表 (通常 100+ 个)
        │
        ├── 2. removeDuplicates()
        │      → 去重
        │
        ├── 3. getExclusions()
        │      → 处理 @SpringBootApplication(exclude=...) 排除项
        │      → 处理 spring.autoconfigure.exclude 属性
        │
        ├── 4. getConfigurationClassFilter().filter()
        │      → ★ 条件过滤 (这是性能关键！) ★
        │      → 通过 AutoConfigurationImportFilter 快速过滤
        │      → OnBeanCondition
        │      → OnClassCondition (检查类是否在 classpath)
        │      → OnWebApplicationCondition
        │      → 过滤掉大量不满足条件的配置类
        │
        └── 5. fireAutoConfigurationImportEvents()
               → 通知监听器，用于条件评估报告
               → 最终仅保留十几到几十个配置类

        结果: 只有满足条件的自动配置类会被加载
```

### 5.3 自动配置类示例

以 `DataSourceAutoConfiguration` 为例理解自动配置的工作方式：

```java
@AutoConfiguration(before = SqlInitializationAutoConfiguration.class)
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@ConditionalOnMissingBean(type = "io.r2dbc.spi.ConnectionFactory")
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceProperties properties) {
        // 只有当用户没有自定义 DataSource 时才创建
        return properties.initializeDataSourceBuilder().build();
    }
}
```

**工作原理**:
1. `@ConditionalOnClass(DataSource.class)` → 只有 classpath 中有 JDBC 驱动才生效
2. `@ConditionalOnMissingBean` → 用户如果自己定义了 DataSource Bean，自动配置退让
3. `@EnableConfigurationProperties` → 绑定 `spring.datasource.*` 配置属性

### 5.4 新版 vs 旧版自动配置注册

```
旧版 (Spring Boot 2.6 之前):
    META-INF/spring.factories
    └── org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
          com.example.FooAutoConfiguration,\
          com.example.BarAutoConfiguration

新版 (Spring Boot 2.7+, 推荐):
    META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── 每行一个全限定类名:
          com.example.FooAutoConfiguration
          com.example.BarAutoConfiguration
```

---

## 6. 条件注解体系

条件注解是自动配置的基石，决定了配置类/Bean 是否生效：

### 6.1 常用条件注解速查表

| 注解 | 条件 | 典型用途 |
|------|------|----------|
| `@ConditionalOnClass` | classpath 中存在指定类 | 检测第三方库是否引入 |
| `@ConditionalOnMissingClass` | classpath 中不存在指定类 | 排除特定库的场景 |
| `@ConditionalOnBean` | 容器中存在指定 Bean | 依赖其他配置 |
| `@ConditionalOnMissingBean` | 容器中不存在指定 Bean | **核心! 用户自定义优先** |
| `@ConditionalOnProperty` | 配置属性满足条件 | 功能开关 |
| `@ConditionalOnResource` | classpath 中存在指定资源 | 检测配置文件 |
| `@ConditionalOnWebApplication` | 是 Web 应用 | Web 特有配置 |
| `@ConditionalOnNotWebApplication` | 不是 Web 应用 | 非 Web 配置 |
| `@ConditionalOnExpression` | SpEL 表达式为 true | 复杂条件 |
| `@ConditionalOnJava` | JDK 版本满足要求 | 版本兼容 |
| `@ConditionalOnCloudPlatform` | 运行在指定云平台 | 云特有配置 |
| `@ConditionalOnSingleCandidate` | 容器中只有一个候选 Bean | 避免歧义 |

### 6.2 条件评估流程

```
自动配置类加载
      │
      ▼
  AutoConfigurationImportFilter (快速过滤阶段)
      │  → OnClassCondition: 检查 @ConditionalOnClass
      │  → OnBeanCondition: 检查 @ConditionalOnBean
      │  → OnWebApplicationCondition: 检查 Web 类型
      │  (这个阶段不加载类，只读 metadata，非常快)
      │
      ▼
  ConfigurationClassParser (精确评估阶段)
      │  → 逐个评估每个 @Conditional 注解
      │  → 决定是否注册 BeanDefinition
      │
      ▼
  BeanFactory (Bean 创建阶段)
      │  → 方法级 @ConditionalOnMissingBean 等
      │  → 决定是否创建具体的 Bean
      │
      ▼
  最终的 Bean 集合
```

---

## 7. Starter 机制

### 7.1 Starter 的本质

**Starter 本身不包含代码**，它只是一个 **依赖聚合** 的 POM/Gradle 文件：

```
spring-boot-starter-web (Starter)
    │
    ├── spring-boot-starter-jackson    → JSON 处理
    ├── spring-boot-starter-tomcat     → 内嵌 Tomcat
    ├── spring-boot-http-converter     → HTTP 消息转换
    └── spring-boot-webmvc            → Spring MVC 自动配置
         │
         └── 包含 AutoConfiguration 类
             → 根据条件自动配置 DispatcherServlet、ViewResolver 等
```

### 7.2 Starter 与自动配置的关系

```
引入 Starter 依赖
      │
      ▼
  相关 JAR 加入 classpath
      │
      ▼
  JAR 中的 META-INF/spring/...imports 被读取
      │
      ▼
  AutoConfiguration 类被加载
      │
      ▼
  @ConditionalOnClass 检测到相关类存在
      │
      ▼
  自动配置生效，创建所需 Bean
      │
      ▼
  开箱即用！
```

### 7.3 常用 Starter 一览

| Starter | 功能 | 自动配置内容 |
|---------|------|-------------|
| `spring-boot-starter` | 核心基础 | 日志、YAML、自动配置 |
| `spring-boot-starter-web` | Web 开发 | Tomcat、Spring MVC、Jackson |
| `spring-boot-starter-webflux` | 响应式 Web | Netty、WebFlux |
| `spring-boot-starter-data-jpa` | JPA 数据访问 | Hibernate、DataSource、事务 |
| `spring-boot-starter-data-redis` | Redis | Lettuce 客户端、RedisTemplate |
| `spring-boot-starter-security` | 安全 | Spring Security、登录页、CSRF |
| `spring-boot-starter-test` | 测试 | JUnit5、Mockito、AssertJ |
| `spring-boot-starter-actuator` | 监控 | 健康检查、指标、审计 |
| `spring-boot-starter-validation` | 参数校验 | Hibernate Validator |
| `spring-boot-starter-cache` | 缓存 | CacheManager |
| `spring-boot-starter-kafka` | 消息队列 | KafkaTemplate、Consumer |

---

## 8. 内嵌容器机制

### 8.1 Web 服务器自动配置

```
WebApplicationType.deduce()
      │
      ├── SERVLET (找到 Servlet + DispatcherServlet)
      │     │
      │     ▼
      │   ServletWebServerFactory
      │     ├── TomcatServletWebServerFactory (默认)
      │     ├── JettyServletWebServerFactory
      │     └── UndertowServletWebServerFactory
      │
      ├── REACTIVE (找到 ReactiveWeb 相关类)
      │     │
      │     ▼
      │   ReactiveWebServerFactory
      │     ├── NettyReactiveWebServerFactory (默认)
      │     ├── TomcatReactiveWebServerFactory
      │     └── JettyReactiveWebServerFactory
      │
      └── NONE (都不满足)
            → 不启动 Web 服务器
```

### 8.2 Fat JAR 启动流程

```
java -jar myapp.jar
      │
      ▼
  MANIFEST.MF
    Main-Class: org.springframework.boot.loader.launch.JarLauncher
    Start-Class: com.example.MyApplication
      │
      ▼
  JarLauncher.main()
    → 创建 LaunchedClassLoader
    → 将嵌套 JAR (BOOT-INF/lib/*.jar) 加入 classpath
    → 调用 Start-Class 的 main 方法
      │
      ▼
  MyApplication.main()
    → SpringApplication.run()
```

**Fat JAR 结构**:
```
myapp.jar
├── META-INF/
│   └── MANIFEST.MF
├── BOOT-INF/
│   ├── classes/         ← 你的编译代码
│   │   └── com/example/MyApplication.class
│   ├── lib/             ← 所有依赖 JAR
│   │   ├── spring-boot-3.x.jar
│   │   ├── spring-webmvc-6.x.jar
│   │   ├── tomcat-embed-core-10.x.jar
│   │   └── ...
│   ├── classpath.idx    ← classpath 索引
│   └── layers.idx       ← 分层索引 (用于 Docker 优化)
└── org/springframework/boot/loader/  ← Loader 类
```

---

## 9. 外部化配置

### 9.1 配置属性加载优先级（从高到低）

```
优先级从高到低:

 1. 命令行参数              --server.port=9090
 2. SPRING_APPLICATION_JSON  环境变量中的 JSON
 3. ServletConfig 参数
 4. ServletContext 参数
 5. JNDI 属性
 6. Java System Properties   -Dserver.port=9090
 7. OS 环境变量              SERVER_PORT=9090
 8. random.* 属性
 9. application-{profile}.yml   特定 Profile 配置
10. application.yml             默认配置文件
11. @PropertySource 注解
12. 默认属性 (SpringApplication.setDefaultProperties)

   ★ 高优先级覆盖低优先级 ★
```

### 9.2 配置绑定方式

```java
// 方式 1: @Value 注入
@Value("${server.port:8080}")
private int port;

// 方式 2: @ConfigurationProperties 类型安全绑定 (推荐)
@ConfigurationProperties(prefix = "app.datasource")
public class DataSourceConfig {
    private String url;
    private String username;
    private String password;
    private Pool pool = new Pool();  // 支持嵌套对象

    public static class Pool {
        private int maxSize = 10;
        private Duration timeout = Duration.ofSeconds(30);
    }
}

// 方式 3: Environment 编程式访问
@Autowired
private Environment env;
String port = env.getProperty("server.port");
```

---

## 10. Actuator 监控

### 10.1 Actuator 端点体系

```
/actuator
  ├── /health          → 健康检查 (UP/DOWN)
  │     ├── /health/db       → 数据库健康
  │     ├── /health/redis    → Redis 健康
  │     └── /health/disk     → 磁盘空间
  ├── /info            → 应用信息
  ├── /metrics          → 性能指标
  │     ├── /metrics/jvm.memory.used
  │     ├── /metrics/http.server.requests
  │     └── /metrics/system.cpu.usage
  ├── /env             → 环境属性
  ├── /beans           → 所有 Bean 列表
  ├── /mappings        → URL 映射
  ├── /configprops     → 配置属性
  ├── /loggers         → 日志级别 (可动态修改!)
  ├── /threaddump      → 线程快照
  ├── /heapdump        → 堆内存快照
  ├── /scheduledtasks  → 定时任务
  └── /startup         → 启动步骤分析
```

Actuator 源码位于 `module/spring-boot-actuator/`，分为：
- `endpoint/` — 端点定义
- `health/` — 健康指示器
- `metrics/` — 指标收集
- `web/` — Web 暴露
- `security/` — 安全控制

---

## 11. 关键设计模式

### 11.1 SPI 机制: SpringFactoriesLoader

```
Spring Boot 大量使用 SPI（Service Provider Interface）模式:

META-INF/spring.factories 或 META-INF/spring/xxx.imports
      │
      ▼
SpringFactoriesLoader.loadFactories(type, classLoader)
      │
      ▼
返回所有 JAR 中注册的实现类实例

用途:
  - 加载 SpringApplicationRunListener
  - 加载 ApplicationContextInitializer
  - 加载 ApplicationListener
  - 加载 AutoConfigurationImportFilter
  - 加载 EnvironmentPostProcessor
```

### 11.2 核心设计模式总结

| 模式 | 应用场景 | 示例 |
|------|----------|------|
| **工厂方法** | 创建 ApplicationContext | `ApplicationContextFactory` |
| **策略模式** | 条件判断 | `SpringBootCondition` 及其子类 |
| **观察者模式** | 生命周期事件 | `ApplicationListener` + `ApplicationEvent` |
| **模板方法** | 启动流程扩展 | `SpringApplication.run()` 中的各 hook |
| **建造者模式** | 流式 API 构建 | `SpringApplicationBuilder` |
| **SPI 扩展** | 可插拔组件发现 | `SpringFactoriesLoader` |
| **装饰器模式** | 属性源包装 | `ConfigurationPropertySources` |

---

## 12. 架构图索引

以下是 Mermaid 格式的架构图，可在任何 Mermaid 渲染器（如 GitHub、VS Code、[mermaid.live](https://mermaid.live)）中查看。

### 图1: 模块依赖关系图

见文件: `spring-boot-module-diagram.mmd`

### 图2: 启动流程时序图

见文件: `spring-boot-startup-sequence.mmd`

### 图3: 自动配置流程图

见文件: `spring-boot-autoconfiguration-flow.mmd`

### 图4: 核心类关系图

见文件: `spring-boot-class-diagram.mmd`

---

## 快速上手建议

1. **先跑起来**: 用 [Spring Initializr](https://start.spring.io) 创建项目
2. **理解入口**: 从 `@SpringBootApplication` + `SpringApplication.run()` 开始读源码
3. **理解自动配置**: 在 `application.yml` 中添加 `debug: true`，启动时会打印完整的条件评估报告
4. **阅读条件报告**: 查看哪些自动配置被激活/跳过，理解为什么
5. **自定义覆盖**: 通过定义自己的 `@Bean` 来覆盖自动配置（`@ConditionalOnMissingBean` 会退让）
6. **看 Starter 源码**: 选一个 Starter（如 `spring-boot-starter-web`），追踪它的依赖链

---

*文档基于 Spring Boot 最新源码 (main 分支) 分析生成*
