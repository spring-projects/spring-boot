package org.springframework.boot.elasticsearch.ais.utils;

import com.alibaba.fastjson.JSON;
import java.util.ArrayList;
import java.util.List;

/**
 * collect is null? or? .
 *
 * @author lihang
 * @email 631533483@qq.com
 */
public class CollectUtil {

  /**
   * is empty? .
   */
  public static boolean isEmpty(Object... object) {
    if (object.length > 0) {
      return false;
    }
    return true;
  }

  /**
   * is not empty? .
   */
  public static boolean isNotEmpty(Object... object) {
    return !isEmpty(object);
  }


  /**
   * iSeparate data by commas.
   */
  public static String commaSplit(Object... objects) {
    if (objects.length > 0) {
      String re = "";
      for (Object o : objects) {
        re = re + JSON.toJSONString(o) + ",";
      }
      return re.substring(0, re.length() - 1);
    }
    return null;
  }

  /**
   * Return sort statement.
   */
  public static String brackersSplit(Object... objects) {
    if (objects.length == 1) {
      return null;
    }
    int length = objects.length;
    if (objects.length % 2 == 1) {
      length = length - 1;
    }
    String script;
    List<String> list = new ArrayList<String>();
    for (int i = 0; i < length; i = i + 2) {
      String tem = "        {\n"
          + "            \"" + objects[i] + "\": {\n"
          + "            \"order\": \"" + objects[i + 1] + "\"\n"
          + "          }\n"
          + "        }";
      list.add(tem);
    }
    script = String.join(",", list);
    return script;
  }

}
