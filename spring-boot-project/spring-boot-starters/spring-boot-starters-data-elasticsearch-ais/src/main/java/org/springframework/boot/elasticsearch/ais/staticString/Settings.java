package org.springframework.boot.elasticsearch.ais.staticString;


import org.springframework.boot.elasticsearch.ais.utils.CollectUtil;
import org.springframework.boot.elasticsearch.ais.utils.StringUtils;

/**
 * Query settings.
 *
 * @author lihang
 * @email 631533483@qq.com
 */

public class Settings {

  /**
   * field filter.
   *
   * @param fields fields name
   * @return String script
   */
  public static String source(String... fields) {
    if (CollectUtil.isNotEmpty(fields)) {
      String script = "\"_source\": [" + CollectUtil.commaSplit(fields) + "]";
    }
    return null;
  }

  /**
   * version.
   *
   * @param type true flase
   * @return String script
   */
  public static String version(boolean type) {
    String script = "\"version\": " + type;
    return script;
  }

  /**
   * timeout.
   *
   * @param i timeout
   * @return String script
   */
  public static String timeout(int i) {
    String script = "\"timeout\": \"" + i + "s\"";
    return script;
  }

  /**
   * field filter.
   *
   * @param fields fields name
   * @return String script
   */
  public static String stored_fields(String... fields) {
    if (CollectUtil.isNotEmpty(fields)) {
      String script = "\"stored_fields\": [" + CollectUtil.commaSplit(fields) + "]";
      return script;
    }
    return null;
  }

  /**
   * Statistical results.
   *
   * @param fields fields name
   * @return String script
   */
  public static String stats(String... fields) {
    if (CollectUtil.isNotEmpty(fields)) {
      String script = "\"stats\": [" + CollectUtil.commaSplit(fields) + "]";
      return script;
    }
    return null;
  }

  /**
   * sort order.
   *
   * @param field field name
   * @param order desc asc
   * @return String script
   */
  public static String sort(String field, String order) {
    if (StringUtils.isNotBlank(order) && StringUtils.isNotBlank(field)) {
      String script = "      \"sort\": [\n"
          + "        {\n"
          + "            \"" + field + "\": {\n"
          + "            \"order\": \"" + order + "\"\n"
          + "          }\n"
          + "        }\n"
          + "      ]";
      return script;
    }
    return null;
  }

  /**
   * sort order.
   *
   * @param fieldorder field name and sort,such as ["name","asc","age","desc"]
   * @return String script
   */
  public static String sort(String... fieldorder) {
    if (CollectUtil.isNotEmpty(fieldorder)) {
      String script = "      \"sort\": [" + CollectUtil.brackersSplit(fieldorder) + "]";

      return script;
    }
    return null;
  }

  /**
   * size.
   *
   * @param s size num
   * @return String script
   */
  public static String size(int s) {
    String script = "\"size\": " + s;
    return script;
  }

  /**
   * script.
   *
   * @param source script String
   * @return String script
   */
  public static String script_fields(String source) {
    if (StringUtils.isNotBlank(source)) {
      String script = "      \"script_fields\": {\n"
          + "        \"script_field\": {\n"
          + "          \"script\": {\"source\": \"" + source + "\"}\n"
          + "        }\n"
          + "      }";
      return script;
    }
    return null;
  }


  /**
   * polymerization process.
   *
   * @param b true,false
   * @return String script
   */
  public static String profile(boolean b) {
    String script = "\"profile\": \"" + b + "\"  ";
    return script;
  }

  /**
   * field filter.
   *
   * @param include include field name
   * @param exclude exclude field name
   * @return String script
   */
  public static String partial_fields(String[] include, String[] exclude) {
    if (CollectUtil.isNotEmpty(include) || CollectUtil.isNotEmpty(exclude)) {
      String script = "      \"partial_fields\": {\n"
          + "        \"pattial\": {\n"
          + "          \"include\": [" + CollectUtil.commaSplit(include) + "],\n"
          + "          \"exclude\": [" + CollectUtil.commaSplit(exclude) + "]\n"
          + "        }\n"
          + "      }";
      return script;
    }
    return null;
  }

  /**
   * Correlation control.
   *
   * @param index field name
   * @param n     field num
   * @return String script
   */
  public static String indices_boost(String[] index, int[] n) {
    if (CollectUtil.isNotEmpty(index) && CollectUtil.isNotEmpty(n) && index.length == n.length) {
      String[] script_ = new String[index.length];
      for (int i = 0; i < index.length; i++) {
        script_[i] = '"' + index[i] + '"' + ':' + n[i];
      }
      String script = "\"indices_boost\": [\n" + String.join(",", script_) + "  ]";
      return script;
    }
    return null;
  }

  /**
   * highlight field.
   *
   * @param type field name
   * @return String script
   */
  public static String highlight(String type) {
    if (StringUtils.isNotBlank(type)) {
      String script = "\"highlight\": {  \"fields\": {\"" + type + "\": {}}}";
      return script;
    }
    return null;
  }

  /**
   * highlight field.
   *
   * @param type      field name
   * @param pre_tags  Prefix symbol
   * @param post_tags Suffix symbol
   * @return String script
   */
  public static String highlight(String type, String pre_tags, String post_tags) {
    if (StringUtils.isNotBlank(type) && StringUtils.isNotBlank(post_tags) && StringUtils
        .isNotBlank(pre_tags)) {
      String script = "  \"highlight\": {\n"
          + "    \"fields\": {\n"
          + "      \"" + type + "\": {}\n"
          + "    },\n"
          + "    \"post_tags\": \"" + post_tags + "\",\n"
          + "    \"pre_tags\": \"" + pre_tags + "\"\n"
          + "  }";
      return script;
    }
    return null;
  }

  /**
   * from.
   *
   * @param i from num
   * @return String script
   */
  public static String from(int i) {
    String script = " \"from\": " + i;
    return script;
  }

  /**
   * explain.
   *
   * @param b true,false
   * @return String script
   */
  public static String explain(boolean b) {
    String script = "\"explain\": " + b;
    return script;
  }


  /**
   * Another query .
   *
   * @param fields fields name
   * @return String script
   */
  public static String docvalue_fields(String... fields) {
    if (CollectUtil.isNotEmpty(fields)) {
      String script = "\"docvalue_fields\": [" + CollectUtil.commaSplit(fields) + "]";
      return script;

    }
    return null;
  }

  /**
   * field collapse .
   *
   * @param field field name
   * @return String script
   */
  public static String collapse(String field) {
    if (StringUtils.isNotBlank(field)) {
      String script = "\"collapse\": {\n"
          + "    \"field\": \"" + field + "\"\n"
          + "  }";
      return script;
    }
    return null;
  }


}
