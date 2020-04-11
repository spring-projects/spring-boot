package org.springframework.boot.elasticsearch.ais.staticString;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.elasticsearch.ais.utils.StringUtils;
import java.util.List;
import java.util.Map;

/**
 * bulk operation.
 *
 * @author lihang
 * @email 631533483@qq.com
 * @result Script of character type
 */
public class Bulk {

  /**
   * Insert a piece of data.
   *
   * @param <T>    data type
   * @param tclass data
   * @return String script
   */
  public static <T> String insert(T tclass) {
    String jsonObject = JSONObject.toJSONString(tclass);
    return jsonObject;
  }

  /**
   * Insert  data.
   *
   * @param <T>       data type
   * @param classList Data array
   * @return String script
   */
  public static <T> String insert(List<T> classList) {
    String script = "";
    for (T entity : classList) {
      script = script + "{\"index\":{}}" + "\n";
      script = script + JSON.toJSONString(entity) + "\n";
    }
    return script;
  }

  /**
   * Insert data.
   *
   * @param <K> data ID
   * @param <V> data
   * @return String script
   */
  public static <K, V> String insert(Map<K, V> map) {
    String script = "";
    for (Map.Entry<K, V> m : map.entrySet()) {
      script = script + "{\"index\":{\"_id\": " + JSON.toJSONString(m.getKey()) + "}}" + "\n";
      script = script + JSON.toJSONString(m.getValue()) + "\n";
    }
    return script;
  }

  /**
   * update  data.
   *
   * @param <I> data ID
   * @param <K> data Field name
   * @param <V> data Field value
   * @return String script
   */
  public static <I, K, V> String update(Map<I, Map<K, V>> map) {
    String script = "";
    for (Map.Entry<I, Map<K, V>> m : map.entrySet()) {
      if (StringUtils.isNotEmpty(m)) {
        script = script + "{\"update\":{\"_id\":" + JSON.toJSONString(m.getKey()) + "}}\n";
        script = script + "{\"doc\":{";
        for (Map.Entry<K, V> o : m.getValue().entrySet()) {
          if (StringUtils.isNotEmpty(o.getKey())) {
            script = script + JSON.toJSONString(o.getKey()) + ':' + JSON.toJSONString(o.getValue())
                + ',';
          }
        }
        script = script.substring(0, script.length() - 1) + "}}\n";
      }
    }
    return script;
  }

  /**
   * update  data.
   *
   * @param <K> data Field name
   * @param <V> data Field value
   * @return String script
   */
  public static <K, V> String update(Object id, Map<K, V> map) {
    String script = "";
    if (StringUtils.isNotEmpty(id)) {
      script = script + "{\"update\":{\"_id\":" + JSON.toJSONString(id) + "}}\n";
      script = script + "{\"doc\":{";
      for (Map.Entry<K, V> o : map.entrySet()) {
        if (StringUtils.isNotEmpty(o.getKey())) {
          script =
              script + JSON.toJSONString(o.getKey()) + ':' + JSON.toJSONString(o.getValue()) + ',';
        }
      }
      script = script.substring(0, script.length() - 1) + "}}\n";
      return script;
    }
    return null;

  }

  /**
   * delete  data.
   *
   * @param ids one or more ids
   * @return String script
   */
  public static String delete(Object... ids) {
    String script = "";
    for (Object i : ids) {
      script = script + "{\"delete\":{\"_id\":" + JSON.toJSONString(i) + "}}\n";
    }
    return script;
  }

  /**
   * Update query data.
   *
   * @param <T> The type of the corresponding field data needs to be modified
   * @param map The name and type of the corresponding field data need to be modified
   * @return String script
   */
  public static <T> String update_by_query(Map<String, T> map) {
    String script = "";
    String source = "";
    for (Map.Entry<String, T> o : map.entrySet()) {
      source =
          source + "ctx._source['" + o.getKey() + "']=" + JSON.toJSONString(o.getValue()) + ";";
    }
    source = source.replaceAll("\"", "'");
    script = "  \"script\": {\n"
        + "    \"source\": \"" + source + "\" \n"
        + "  }";
    return script;
  }


}
