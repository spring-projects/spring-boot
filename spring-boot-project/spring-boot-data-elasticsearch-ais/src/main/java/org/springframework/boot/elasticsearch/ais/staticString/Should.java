package org.springframework.boot.elasticsearch.ais.staticString;

/**
 * query should.
 *
 * @author lihang
 * @email 631533483@qq.com
 */
public class Should {

  /**
   * query should.
   *
   * @param script query script
   * @return String script
   */
  public static String should(String... script) {
    String should_ = String.join(",", script);
    String should = "{\n"
        + "  \"query\": {\n"
        + "    \"bool\": {\n"
        + "      \"should\": [" + should_ + "]\n"
        + "    }\n"
        + "  }\n"
        + "}";
    return null;
  }

  /**
   * query should.
   *
   * @param script               query script
   * @param minimum_should_match Minimum quantity satisfied
   * @return String script
   */
  public static String should(int minimum_should_match, String... script) {
    String should_ = String.join(",", script);
    String should = "{\n"
        + "  \"query\": {\n"
        + "    \"bool\": {\n"
        + "      \"should\": [" + should_ + "],\n"
        + "      \"minimum_should_match\": " + minimum_should_match + "\n"
        + "    }\n"
        + "  }\n"
        + "}";
    return null;
  }

}
