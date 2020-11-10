# spring-boot 学习笔记

## 模块解析

### 1、spring-boot-parent

- 这个模块没有代码，是 spring-boot 模块的父项目，被其他子模块继承

### 2、spring-boot

> 项目的核心，基础核心功能都在这里实现。为 spring-boot 提供了支持

- `SpringApplication` 这个类，是 SpringBoot 的启动类，提供了一个静态的 run 方法启动程序，创建并刷新 spring 容器 `ApplicationContext`

- 支持选择不同的容器比如 Tomcat,Jetty 等来作为应用的嵌入容器，这个是 SpringBoot 的新特性之一

- 外部配置支持，这个指的是我们执行java -jar xxx.jar命令时可以带一些参数

- 内置了一些SpringBoot启动时的生命周期事件和一些容器初始化器(`ApplicationContext initializers`)

### 3、 spring-boot-autoconfigure

> 自动配置模块，关键注解 `@EnableAutoConfiguration`

### 4、spring-boot-starters

> SpringBoot 通过提供众多起步依赖降低项目依赖的复杂度

### 5、spring-boot-cli

> Spring Boot CLI是一个命令行工具

### 6、spring-boot-actuator

> 提供开箱即用的监控端点，`HealthEndpoint`, `EnvironmentEndpoint` 和 `BeansEndpoint` 等端点

### 7、spring-boot-actuator-autoconfigure

> 检控官模块的自动配置

### 8、spring-boot-test

> 测试模块，包含了一些帮助我们测试的核心类和注解（比如 `@SpringBootTest` ）

### 9、spring-boot-dependencies

> 定义 SpringBoot 以来及其版本

### 10、spring-boot-devtools

> 热部署模块，即修改代码后无需重启应用即生效

### 11、spring-boot-docs

> 文档模块

### 12、spring-boot-properties-migrator

> 项目迁移相关

### 13、spring-boot-test-autoconfigure

> 测试自动配置模块

### 14、spring-boot-tools

> 工具模块，加载，插件,metadata和后置处理相关的支持