package org.springframework.boot.elasticsearch.ais.config;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * scan Elasticsearch annotation.
 *
 * @author lihang
 * @email 631533483@qq.com
 */

public class Scanner {


  private Set<Class<?>> classes = new HashSet<>();
  private Set<String> properties = new HashSet<>();
  private Logger log = LoggerFactory.getLogger(Scanner.class);

  /**
   * 从包package中获取所有的Class Get class of annotation class.
   *
   * @param packageName packageName
   * @return Classes with annotations
   */
  public Set<Class<?>> getClasses(String packageName) throws Exception {

    // 第一个class类的集合
    //List<Class<?>> classes = new ArrayList<Class<?>>();
    // 是否循环迭代
    boolean recursive = true;
    // 获取包的名字 并进行替换
    String packageDirName = packageName.replace('.', '/');
    // 定义一个枚举的集合 并进行循环来处理这个目录下的things
    Enumeration<URL> dirs;
    try {
      dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
      // 循环迭代下去
      while (dirs.hasMoreElements()) {
        // 获取下一个元素
        URL url = dirs.nextElement();
        // 得到协议的名称
        String protocol = url.getProtocol();
        // 如果是以文件的形式保存在服务器上
        if ("file".equals(protocol)) {
          // 获取包的物理路径
          String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
          // 以文件的方式扫描整个包下的文件 并添加到集合中
          addClass(classes, filePath, packageName);
        } else if ("jar".equals(protocol)) {
          // 如果是jar包文件
          // 定义一个JarFile
          JarFile jar;
          try {
            // 获取jar
            jar = ((JarURLConnection) url.openConnection()).getJarFile();
            // 从此jar包 得到一个枚举类
            Enumeration<JarEntry> entries = jar.entries();
            // 同样的进行循环迭代
            while (entries.hasMoreElements()) {
              // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
              JarEntry entry = entries.nextElement();
              String name = entry.getName();
              // 如果是以/开头的
              if (name.charAt(0) == '/') {
                // 获取后面的字符串
                name = name.substring(1);
              }
              // 如果前半部分和定义的包名相同
              if (name.startsWith(packageDirName)) {
                int idx = name.lastIndexOf('/');
                // 如果以"/"结尾 是一个包
                if (idx != -1) {
                  // 获取包名 把"/"替换成"."
                  packageName = name.substring(0, idx).replace('/', '.');
                }
                // 如果可以迭代下去 并且是一个包
                if ((idx != -1) || recursive) {
                  // 如果是一个.class文件 而且不是目录
                  if (name.endsWith(".class") && !entry.isDirectory()) {
                    // 去掉后面的".class" 获取真正的类名
                    String className = name.substring(packageName.length() + 1, name.length() - 6);
                    try {
                      // 添加到classes
                      classes.add(Class.forName(packageName + '.' + className));
                    } catch (ClassNotFoundException e) {
                      log.warn("A class was not found" + e);
                    } catch (NoClassDefFoundError error) {
                      log.warn("An instance of a class failed to load" + error);
                    } catch (Exception e) {
                      log.warn("An unknown error occurred");
                    }
                  }

                }
              }
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return classes;
  }

  /**
   * . 判断是否是注解 judge annotation
   */
  public void addClass(Set<Class<?>> classes, String filePath, String packageName)
      throws Exception {
    File[] files = new File(filePath).listFiles(
        file -> (file.isFile() && file.getName().endsWith(".class")) || file.isDirectory());
    assert files != null;
    for (File file : files) {
      String fileName = file.getName();
      if (file.isFile()) {
        String classsName = fileName.substring(0, fileName.lastIndexOf("."));
        if (!packageName.isEmpty()) {
          classsName = packageName + "." + classsName;
        }
        doAddClass(classes, classsName);
      } else if (file.isDirectory()) {
        String filePathTemPorary = packageName + "." + fileName;
        getClasses(filePathTemPorary.replace("/", "."));
      }

    }
  }

  /**
   * . 将class添加map中 Add class to map
   **/
  public void doAddClass(Set<Class<?>> classes, final String classsName) throws Exception {
    ClassLoader classLoader = new ClassLoader() {
      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(name);
      }
    };
    classes.add(classLoader.loadClass(classsName));
  }

  /**
   * . 寻找注解类 find annotation
   */
  public <A extends Annotation> Set<Class<?>> getAnnotationClasses(String packageName,
      Class<A> annotationClass) throws Exception {

    //找用了annotationClass注解的类
    Set<Class<?>> controllers = new HashSet<>();
    Set<Class<?>> clsList = getClasses(packageName);
    if (clsList != null && clsList.size() > 0) {
      for (Class<?> cls : clsList) {
        if (cls.getAnnotation(annotationClass) != null) {
          controllers.add(cls);
        }
      }
    }
    return controllers;
  }


}