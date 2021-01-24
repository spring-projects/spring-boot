



```git
git clone https://github.com/mjf1310/spring-boot.git
Cloning into 'spring-boot'...
remote: Enumerating objects: 11, done.
remote: Counting objects: 100% (11/11), done.
remote: Compressing objects: 100% (10/10), done.
remote: Total 626468 (delta 3), reused 1 (delta 1), pack-reused 626457 eceiving objects:  99% (626463/626468), 126.60 MiB | 100.00 KiB/s
Receiving objects: 100% (626468/626468), 126.60 MiB | 100.00 KiB/s, done.
Resolving deltas: 100% (277734/277734), done.
error: unable to create file spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin/src/test/resources/org/springframework/boot/gradle/plugin/ApplicationPluginActionIntegrationTests-applicationNameCanBeUsedToCustomizeDistributionName.gradle: Filename too long
error: unable to create file spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin/src/test/resources/org/springframework/boot/gradle/plugin/DependencyManagementPluginActionIntegrationTests-helpfulErrorWhenVersionlessDependencyFailsToResolve.gradle: Filename too long
error: unable to create file spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin/src/test/resources/org/springframework/boot/gradle/plugin/JavaPluginActionIntegrationTests-additionalMetadataLocationsConfiguredWhenProcessorIsPresent.gradle: Filename too long
error: unable to create file spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin/src/test/resources/org/springframework/boot/gradle/plugin/JavaPluginActionIntegrationTests-additionalMetadataLocationsNotConfiguredWhenProcessorIsAbsent.gradle: Filename too long
error: unable to create file spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin/src/test/resources/org/springframework/boot/gradle/plugin/SpringBootPluginIntegrationTests-unresolvedDependenciesAreAnalyzedWhenDependencyResolutionFails.gradle: Filename too long
error: unable to create file spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin/src/test/resources/org/springframework/boot/gradle/tasks/buildinfo/BuildInfoIntegrationTests-notUpToDateWhenExecutedTwiceWithFixedTimeAndChangedGradlePropertiesProjectVersion.gradle: Filename too long
error: unable to create file spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin/src/test/resources/org/springframework/boot/gradle/tasks/buildinfo/BuildInfoIntegrationTests-notUpToDateWhenExecutedTwiceWithFixedTimeAndChangedProjectVersion.gradle: Filename too long
error: unable to create file spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin/src/test/resources/org/springframework/boot/gradle/tasks/bundling/BootJarIntegrationTests-developmentOnlyDependenciesAreNotIncludedInTheArchiveByDefault.gradle: Filename too long
error: unable to create file spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin/src/test/resources/org/springframework/boot/gradle/tasks/bundling/BootJarIntegrationTests-upToDateWhenBuiltWithDefaultLayeredAndThenWithExplicitLayered.gradle: Filename too long
error: unable to create file spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin/src/test/resources/org/springframework/boot/gradle/tasks/bundling/BootJarIntegrationTests-whenAResolvableCopyOfAnUnresolvableConfigurationIsResolvedThenResolutionSucceeds.gradle: Filename too long
error: unable to create file spring-boot-project/spring-boot-tools/spring-boot-gradle-plugin/src/test/resources/org/springframework/boot/gradle/tasks/bundling/BootWarIntegrationTests-developmentOnlyDependenciesAreNotIncludedInTheArchiveByDefault.gradle: Filename too long
Checking out files: 100% (7121/7121), done.
fatal: unable to checkout working tree
warning: Clone succeeded, but checkout failed.
You can inspect what was checked out with 'git status'
and retry the checkout with 'git checkout -f HEAD'


E:\workspace_2021\java\dev0>
```









# git for windows下的Filename too long

![img](https://csdnimg.cn/release/blogv2/dist/pc/img/reprint.png)

[赶路人儿](https://blog.csdn.net/liuxiao723846) 2017-10-24 14:27:48 ![img](https://csdnimg.cn/release/blogv2/dist/pc/img/articleReadEyes.png) 18983 ![img](https://csdnimg.cn/release/blogv2/dist/pc/img/tobarCollect.png) 收藏 17

分类专栏： [git工具](https://blog.csdn.net/liuxiao723846/category_3169839.html) 文章标签： [git](https://www.csdn.net/tags/MtzaYgwsMzEzMy1ibG9n.html)

从github克隆一个项目下发出现了错误：

![img](https://img-blog.csdn.net/20171024142806622?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvbGl1eGlhbzcyMzg0Ng==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/Center)



git有可以创建4096长度的文件名，然而在windows最多是260，因为git用了旧版本的windows api，为此踩了个坑。



## 解决

打开git命令行：

```html
git config --global core.longpaths true
```

























































