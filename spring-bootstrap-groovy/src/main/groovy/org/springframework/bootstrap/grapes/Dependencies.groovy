package org.springframework.bootstrap.grapes

import org.springframework.core.type.StandardAnnotationMetadata
import org.springframework.util.ClassUtils
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory
import groovy.util.logging.Log

@GrabResolver(name='spring-snapshot', root='http://repo.springframework.org/snapshot')
@GrabConfig(systemClassLoader=true)
@Grab("org.springframework:spring-core:4.0.0.BOOTSTRAP-SNAPSHOT")
@GrabExclude("commons-logging:commons-logging")
@Grab("org.slf4j:jcl-over-slf4j:1.6.1")
@Grab("org.slf4j:slf4j-jdk14:1.6.1")
@Log
class Dependencies { 

  static List<String> defaults() { 
    return ["org.springframework.bootstrap.grapes.BootstrapGrapes"]
  }

  static List<String> dependencies(Collection<String> configs) { 

    def result = []
    if (isWeb(configs)) { 
      log.info("Adding web dependencies.")
      result.addAll(web())
    }

    if (isBatch(configs)) {
      log.info("Adding batch dependencies.")
      result.addAll(batch())
      result << "org.springframework.bootstrap.grapes.BatchCommand"
      result << "org.springframework.bootstrap.grapes.BatchInitializationGrapes"
    }

    if (isHadoop(configs)) {
      log.info("Adding info dependencies.")
      result.addAll(hadoop())
      result << "org.springframework.bootstrap.grapes.HadoopContext"
    }

    return result

  }

  static String[] web() { 
    def result = []
    result << "org.springframework.bootstrap.grapes.WebGrapes"
    if (!isEmbeddedServerAvailable()) { result << "org.springframework.bootstrap.grapes.TomcatGrapes" }
    return result
  }

  static String[] batch() { 
    def result = []
    result << "org.springframework.bootstrap.grapes.BatchGrapes"
    return result
  }

  static String[] hadoop() { 
    def result = []
    result << "org.springframework.bootstrap.grapes.HadoopGrapes"
    return result
  }

  static boolean isWeb(Collection<String> configs) {
    SimpleMetadataReaderFactory factory = new SimpleMetadataReaderFactory()
    return configs.any { config ->
      def meta = factory.getMetadataReader(config).getAnnotationMetadata()
      meta.hasAnnotation("org.springframework.stereotype.Controller") || meta.hasAnnotation("org.springframework.web.servlet.config.annotation.EnableWebMvc")
    }
  }

  static boolean isHadoop(Collection<String> configs) { 
    SimpleMetadataReaderFactory factory = new SimpleMetadataReaderFactory()
    return configs.any { config ->
      config.contains("Hadoop")
    }
  }

  static boolean isBatch(Collection<String> configs) { 
    SimpleMetadataReaderFactory factory = new SimpleMetadataReaderFactory()
    return configs.any { config ->
      def meta = factory.getMetadataReader(config).getAnnotationMetadata()
      meta.hasAnnotation("org.springframework.batch.core.configuration.annotation.EnableBatchProcessing")
    }
  }

  static boolean isEmbeddedServerAvailable() { 
    return ClassUtils.isPresent("org.apache.catalina.startup.Tomcat") || ClassUtils.isPresent("org.mortbay.jetty.Server")
  }

}