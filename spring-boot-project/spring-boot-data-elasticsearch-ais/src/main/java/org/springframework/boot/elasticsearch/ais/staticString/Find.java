package org.springframework.boot.elasticsearch.ais.staticString;


import com.alibaba.fastjson.JSON;
import org.springframework.boot.elasticsearch.ais.utils.CollectUtil;
import org.springframework.boot.elasticsearch.ais.utils.StringUtils;


/**
 * . find operation
 *
 * @author lihang
 * @email 631533483@qq.com
 * @return String script
 */
public class Find {

  /**
   * 查询所有 find all.
   *
   * @return String script
   */
  public static String findAll() {
    String query = "{\n"
        + "    \"match_all\": {}\n"
        + "  }";
    return query;
  }

  /**
   * 精确查询 find term.
   *
   * @param key   field name
   * @param value field value
   * @return String script
   */
  public static String term(String key, Object value) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "    \"term\": {\n"
          + "      \"" + key + "\": {\n"
          + "        \"value\": " + JSON.toJSONString(value) + "\n"
          + "      }\n"
          + "    }\n"
          + "  }";
      return query;
    }
    return null;
  }

  /**
   * 精确查询 find term.
   *
   * @param key   field name
   * @param value field value
   * @param boost Query ranking score
   * @return String script
   */
  public static String term(String key, Object value, double boost) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "    \"term\": {\n"
          + "      \"" + key + "\": {\n"
          + "        \"value\": " + JSON.toJSONString(value) + ", \n"
          + "        \"boost\": " + boost + "\n"
          + "      }\n"
          + "    }\n"
          + "  }";
      return query;
    }
    return null;
  }

  /**
   * 分词查询 find match.
   *
   * @param key   field name
   * @param value field value
   * @return String script
   */
  public static String match(String key, Object value) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "   \"match\": {\n"
          + "     \"" + key + "\": \"" + value.toString().trim() + "\"\n"
          + "   }\n"
          + "  }";
      return query;
    }
    return null;
  }

  /**
   * 分词查询 find match.
   *
   * @param key   field name
   * @param value field value
   * @param boost Query ranking score
   * @return String script
   */
  public static String match(String key, Object value, double boost) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = " {\n"
          + "    \"match\": {\n"
          + "      \"" + key + "\": {\n"
          + "        \"boost\": " + boost + ",\n"
          + "        \"query\": \"" + value.toString().trim() + "\"\n"
          + "      }\n"
          + "    }\n"
          + "  } ";
      return query;
    }
    return null;
  }

  /**
   * 模糊查询 find wildcard,Left and right have been added* .
   *
   * @param key   field name
   * @param value field value
   * @return String script
   */
  public static String wildcard(String key, Object value) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "   \"wildcard\": {\n"
          + "     \"" + key + "\": {\n"
          + "       \"value\": \"*" + value.toString().trim() + "*\"\n"
          + "     }\n"
          + "   }\n"
          + "  }";
      return query;
    }
    return null;
  }

  /**
   * 模糊查询 find wildcard，Left and right have been added * .
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   * @return String script
   */
  public static String wildcard(String key, Object value, double boost) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "   \"wildcard\": {\n"
          + "     \"" + key + "\": {\n"
          + "       \"value\": \"*" + value.toString().trim() + "*\",\n"
          + "       \"boost\": " + boost + "\n"
          + "     }\n"
          + "   }\n"
          + "  } ";
      return query;
    }
    return null;
  }

  /**
   * 模糊查询 find wildcard,Left  have been added * .
   *
   * @param key   field name
   * @param value field value
   * @return String script
   */
  public static String wildcardLeft(String key, Object value) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "   \"wildcard\": {\n"
          + "     \"" + key + "\": {\n"
          + "       \"value\": \"*" + value.toString().trim() + "\"\n"
          + "     }\n"
          + "   }\n"
          + "  }";
      return query;
    }
    return null;
  }

  /**
   * 模糊查询 find wildcard，Left have been added * .
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   * @return String script
   */
  public static String wildcardLeft(String key, Object value, double boost) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "   \"wildcard\": {\n"
          + "     \"" + key + "\": {\n"
          + "       \"value\": \"*" + value.toString().trim() + "\",\n"
          + "       \"boost\": " + boost + "\n"
          + "     }\n"
          + "   }\n"
          + "  } ";
      return query;
    }
    return null;
  }

  /**
   * 模糊查询 find wildcard，Right have been added * .
   *
   * @param key   field name
   * @param value field value
   * @return String script
   */
  public static String wildcardRight(String key, Object value) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "   \"wildcard\": {\n"
          + "     \"" + key + "\": {\n"
          + "       \"value\": \"" + value.toString().trim() + "*\"\n"
          + "     }\n"
          + "   }\n"
          + "  }";
      return query;
    }
    return null;
  }

  /**
   * 模糊查询 find wildcard，Right have been added * .
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   * @return String script
   */
  public static String wildcardRight(String key, Object value, double boost) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "   \"wildcard\": {\n"
          + "     \"" + key + "\": {\n"
          + "       \"value\": \"" + value.toString().trim() + "*\",\n"
          + "       \"boost\": " + boost + "\n"
          + "     }\n"
          + "   }\n"
          + "  } ";
      return query;
    }
    return null;
  }

  /**
   * 精确数组查询 find terms .
   *
   * @param key   field name
   * @param value field value
   * @return String script
   */
  public static String trems(String key, Object... value) {
    if (StringUtils.isNotBlank(key) && CollectUtil.isNotEmpty(value)) {
      String query = "{\n"
          + "   \"terms\": {\n"
          + "     \"" + key + "\":[" + CollectUtil.commaSplit(value) + "]\n"
          + "   }\n"
          + "  }";
      return query;
    }
    return null;
  }

  /**
   * 模糊查询 Left and right not added * .
   *
   * @param key   field name
   * @param value field value
   * @return String script
   */
  public static String wildcardFree(String key, Object value) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "   \"wildcard\": {\n"
          + "     \"" + key + "\": {\n"
          + "       \"value\": \"" + value.toString().trim() + "\"\n"
          + "     }\n"
          + "   }\n"
          + "  }";
      return query;
    }
    return null;
  }

  /**
   * 模糊查询 Left and right not added * .
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   * @return String script
   */
  public static String wildcardFree(String key, Object value, double boost) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "   \"wildcard\": {\n"
          + "     \"" + key + "\": {\n"
          + "       \"value\": \"" + value.toString().trim() + "\",\n"
          + "       \"boost\": " + boost + "\n"
          + "     }\n"
          + "   }\n"
          + "  } ";
      return query;
    }
    return null;
  }

  /**
   * 短语前缀匹配查询.
   *
   * <p>Returns documents that contain the words of a provided text,
   * in the same order as provided. The last term of the provided text is treated as a prefix,
   * matching any words that begin with that term.<p/>
   *
   * @param key   field name
   * @param value field value
   * @param slop  slop(Optional, integer) Maximum number of positions allowed between matching
   *              tokens. Defaults to 0. Transposed terms have a slop of 2.
   * @return String script
   */
  public static String match_phrase_prefix(String key, Object value, int slop) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "          \"match_phrase_prefix\": {\n"
          + "            \"" + key + "\": {\n"
          + "              \"query\": \"" + value.toString().trim() + "\",\n"
          + "              \"max_expansions\": " + slop + "\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 短语前缀匹配查询.
   * <p>Returns documents that contain the words of a provided text,
   * in the same order as provided.  The last term of the provided text is treated as a prefix,
   * matching any words that begin with that term.</p>
   *
   * @param key            field name
   * @param value          field value
   * @param slop           slop(Optional, integer) Maximum number of positions allowed between
   *                       matching tokens. Defaults to 0. Transposed terms have a slop of 2.
   * @param max_expansions Maximum number of terms to which the last provided term of the query
   *                       value will expand.
   * @return String script
   */
  public static String match_phrase_prefix(String key, Object value, int max_expansions, int slop) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "          \"match_phrase_prefix\": {\n"
          + "            \"" + key + "\": {\n"
          + "              \"query\": \"" + value.toString().trim() + "\",\n"
          + "              \"max_expansions\": " + max_expansions + ",\n"
          + "              \"slop\":" + slop + "\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }


  /**
   * 短语匹配查询.
   * <p>The match_phrase query analyzes the text and creates a phrase query out of the analyzed
   * text.</p>
   *
   * @param key   field name
   * @param value field value
   * @param slop  slop(Optional, integer) Maximum number of positions allowed between matching
   *              tokens. Defaults to 0. Transposed terms have a slop of 2.
   * @return String script
   */
  public static String match_phrase(String key, Object value, int slop) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "          \"match_phrase\": {\n"
          + "            \"" + key + "\": {\n"
          + "              \"query\": \"" + value.toString().trim() + "\",\n"
          + "              \"slop\": " + slop + "\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 排除高频率词查询查询.
   * <p>The common terms query is a modern alternative to stopwords which improves the precision
   * and recall of search results (by taking stopwords into account), without sacrificing
   * performance.</p>
   *
   * @param key              field name
   * @param value            field value
   * @param cutoff_frequency Terms are allocated to the high or low frequency groups based on the
   *                         cutoff_frequency, which can be specified as an absolute frequency (>=1)
   *                         or as a relative frequency (0.0 .. 1.0).
   * @return String script
   */
  public static String common(String key, Object value, double cutoff_frequency) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = " {\n"
          + "          \"common\": {\n"
          + "            \"" + key + "\": {\n"
          + "              \"query\": \"" + value.toString().trim() + "\",\n"
          + "              \"cutoff_frequency\": " + cutoff_frequency + "\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 判断字段是否存在.
   *
   * <p>exits field<p/>
   *
   * @param key field name
   * @return String script
   */
  public static String exits(String key) {
    if (StringUtils.isNotBlank(key)) {
      String query = " {\n"
          + "          \"exists\": {\n"
          + "            \"field\": \"" + key + "\"\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 相似查询，可以允许错误 Allow bad queries.
   *
   * @param key           field name
   * @param value         field value
   * @param boost         boost Query ranking score
   * @param prefix_length Number of beginning characters left unchanged when creating expansions.
   * @param fuzziness     Maximum edit distance allowed for matching. See Fuzziness for valid values
   *                      and more information
   * @return String script
   */
  public static String fuzzy(String key, Object value, int fuzziness, int prefix_length,
      double boost) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "          \"fuzzy\": {\n"
          + "            \"" + key + "\": {\n"
          + "              \"value\": \"" + value.toString().trim() + "\",\n"
          + "              \"fuzziness\": " + fuzziness + ",\n"
          + "              \"prefix_length\": " + prefix_length + ",\n"
          + "              \"boost\": " + boost + "\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 相似查询，可以允许错误 Allow bad queries.
   *
   * @param key           field name
   * @param value         field value
   * @param prefix_length Number of beginning characters left unchanged when creating expansions.
   * @param fuzziness     Maximum edit distance allowed for matching. See Fuzziness for valid values
   *                      and more information
   * @return String script
   */
  public static String fuzzy(String key, Object value, int fuzziness, int prefix_length) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "          \"fuzzy\": {\n"
          + "            \"" + key + "\": {\n"
          + "              \"value\": \"" + value.toString().trim() + "\",\n"
          + "              \"fuzziness\": " + fuzziness + ",\n"
          + "              \"prefix_length\": " + prefix_length + "\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 地理位置查询.
   *
   * <p>The geo_shape query uses the same grid square representation as the geo_shape mapping to
   * find documents that have a shape that intersects with the query shape. It will also use the
   * same Prefix Tree configuration as defined for the field mapping</p>
   *
   * @param type        field name
   * @param coordinates coordinate
   * @param relation    Query mode
   * @return String script
   */
  public static String geo_shape(String type, Object coordinates, Object relation) {
    if (StringUtils.isNotBlank(type) && StringUtils.isNotEmpty(coordinates)) {
      String query = "{\n"
          + "          \"geo_shape\": {\n"
          + "            \"location\": {\n"
          + "              \"shape\": {\n"
          + "                \"type\": \" " + type + "\",\n"
          + "                \"coordinates\": [" + coordinates.toString().trim() + "]\n"
          + "              }\n"
          + "            },\n"
          + "            \"relation\": \"" + relation + "\"\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * id查询.
   *
   * <p>Returns documents based on their IDs. This query uses document IDs stored in the _id field.
   * <p/>
   *
   * @param values ids valus
   * @return String script
   */
  public static String ids(Object... values) {
    if (CollectUtil.isNotEmpty(values)) {
      String query = " {\n"
          + "          \"ids\": {\n"
          + "            \"values\": [" + CollectUtil.commaSplit(values) + "]\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }


  /**
   * 多字段分词查询.
   * <p>
   * The multi_match query builds on the match query to allow multi-field queries
   * </p>
   *
   * @param value field value
   * @param keys  field name
   * @return String script
   */
  public static String multi_match(Object value, Object... keys) {
    if (StringUtils.isNotEmpty(value) && CollectUtil.isNotEmpty(keys)) {
      String query = " {\n"
          + "          \"multi_match\": {\n"
          + "            \"query\": \"" + value + "\",\n"
          + "            \"fields\": [" + CollectUtil.commaSplit(keys) + "]\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }


  /**
   * 必须包含词查询，要求字段中必须包含某个词.
   *
   * <p>The More Like This Query finds documents that are "like" a given set of documents. In order
   * to do so, MLT selects a set of representative terms of these input documents, forms a query
   * using these terms, executes the query and returns the results. The user controls the input
   * documents, how the terms should be selected and how the query is formed.<p/>
   *
   * @param min_term_freq   The minimum term frequency below which the terms will be ignored from
   *                        the input document. Defaults to 2.
   * @param max_query_terms The maximum number of query terms that will be selected. Increasing this
   *                        value gives greater accuracy at the expense of query execution speed.
   *                        Defaults to 25
   * @param value           field value
   * @param keys            field name
   * @return String script
   */
  public static String more_like_this(int min_term_freq, int max_query_terms, Object value,
      Object... keys) {
    if (StringUtils.isNotEmpty(value) && CollectUtil.isNotEmpty(keys)) {
      String query = "{\n"
          + "          \"more_like_this\": {\n"
          + "            \"fields\": [" + CollectUtil.commaSplit(keys) + "],\n"
          + "            \"like\": \"" + value.toString().trim() + "\",\n"
          + "            \"min_term_freq\": " + min_term_freq + ",\n"
          + "            \"max_query_terms\": " + max_query_terms + "\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 字段属性查询.
   *
   * <p>The percolate query can be used to match queries stored in an index. The percolate query
   * itself contains the document that will be used as query to match with the stored queries.</p>
   *
   * @param key   field name
   * @param value field value
   * @param field The field of type percolator that holds the indexed queries. This is a required
   *              parameter.
   * @return String script
   */
  public static String percolate(String key, Object value, String field) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value) && StringUtils
        .isNotBlank(field)) {
      String query = "        {\n"
          + "          \"percolate\": {\n"
          + "            \"field\": \"" + field + "\",\n"
          + "            \"document\": {\n"
          + "              \"" + key + "\": \"" + value.toString().trim() + "\"\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 左模糊查询.
   *
   * <p>Returns documents that contain a specific prefix in a provided field.</p>
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   * @return String script
   */
  public static String prefix(String key, Object value, double boost) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = " {\n"
          + "          \"prefix\": {\n"
          + "            \"" + key + "\": {\n"
          + "              \"value\": \"" + value.toString().trim() + "\",\n"
          + "              \"boost\": " + boost + "\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 左模糊查询.
   *
   * <p>Returns documents that contain a specific prefix in a provided field.</p>
   *
   * @param key   field name
   * @param value field value
   * @return String script
   */
  public static String prefix(String key, Object value) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = " {\n"
          + "          \"prefix\": {\n"
          + "            \"" + key + "\": {\n"
          + "              \"value\": \"" + value.toString().trim() + "\"\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 较为严格的字符串查询（可以进行和或非等操作，但如果查询中包含无效内容，则查询失败）.
   *
   * <p>Returns documents based on a provided query string, using a parser with a strict syntax.
   * <p>
   * This query uses a syntax to parse and split the provided query string based on operators, such
   * as AND or NOT. The query then analyzes each split text independently before returning matching
   * documents.</p>
   *
   * @param default_field field name
   * @param query_        query script
   * @return String script
   */
  public static String query_string(String default_field, Object query_) {
    if (StringUtils.isNotBlank(default_field) && StringUtils.isNotEmpty(query_)) {
      String query = " {\n"
          + "          \"query_string\": {\n"
          + "            \"default_field\": \"" + default_field + "\",\n"
          + "            \"query\": \"" + query_.toString().trim() + "\"\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 范围查询 Returns documents that contain terms within a provided range.
   *
   * @param key field name
   * @param gte Greater than or equal to
   * @param lte Less than or equal to.
   * @return String script
   */
  public static String range(String key, Object gte, Object lte) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(gte) && StringUtils.isNotEmpty(lte)) {
      String query = "{\n"
          + "          \"range\": {\n"
          + "            \"" + key + "\": {\n"
          + "              \"gte\": " + JSON.toJSONString(gte) + ",\n"
          + "              \"lte\": " + JSON.toJSONString(lte) + "\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 范围查询 Returns documents that contain terms within a provided range.
   *
   * @param key field name
   * @param lte Less than or equal to.
   * @return String script
   */
  public static String rangelte(String key, Object lte) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(lte)) {
      String query = "{\n"
          + "          \"range\": {\n"
          + "            \"" + key + "\": {\n"
          + "              \"lte\": " + JSON.toJSONString(lte) + "\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 范围查询 Returns documents that contain terms within a provided range.
   *
   * @param key field name
   * @param gte Greater than or equal to
   * @return String script
   */
  public static String rangegte(String key, Object gte) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(gte)) {
      String query = "{\n"
          + "          \"range\": {\n"
          + "            \"" + key + "\": {\n"
          + "              \"gte\": " + JSON.toJSONString(gte) + "\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }


  /**
   * 正则查询.
   *
   * <p>Returns documents that contain terms matching a regular expression.
   * A regular expression is a way to match patterns in data using placeholder characters, called
   * operators. For a list of operators supported by the regexp query, see Regular expression
   * syntax.<p/>
   *
   * @param key                     field name
   * @param value                   field value
   * @param max_determinized_states Maximum number of automaton states required for the query.
   *                                Default is 10000. Elasticsearch uses Apache Lucene internally to
   *                                parse regular expressions. Lucene converts each regular
   *                                expression to a finite automaton containing a number of
   *                                determinized states. You can use this parameter to prevent that
   *                                conversion from unintentionally consuming too many resources.
   *                                You may need to increase this limit to run complex regular
   *                                expressions.
   * @return String script
   */
  public static String regexp(String key, Object value, int max_determinized_states) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = " {\n"
          + "          \"regexp\": {\n"
          + "            \"" + key + "\": {\n"
          + "              \"value\": \"" + value.toString().trim() + "\",\n"
          + "              \"max_determinized_states\": " + max_determinized_states + "\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 正则查询.
   *
   * <p>Returns documents that contain terms matching a regular expression.
   * A regular expression is a way to match patterns in data using placeholder characters, called
   * operators. For a list of operators supported by the regexp query, see Regular expression
   * syntax.<p/>
   *
   * @param key   field name
   * @param value field value
   * @return String script
   */
  public static String regexp(String key, String value) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value)) {
      String query = "{\n"
          + "          \"regexp\": {\n"
          + "            \"" + key + "\": {\n"
          + "              \"value\": \"" + value.trim() + "\"\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 正则查询.
   *
   * <p>Returns documents that contain terms matching a regular expression.
   * A regular expression is a way to match patterns in data using placeholder characters, called
   * operators. For a list of operators supported by the regexp query, see Regular expression
   * syntax.<p/>
   *
   * @param key                     field name
   * @param value                   field value
   * @param max_determinized_states Maximum number of automaton states required for the query.
   *                                Default is 10000. Elasticsearch uses Apache Lucene internally to
   *                                parse regular expressions. Lucene converts each regular
   *                                expression to a finite automaton containing a number of
   *                                determinized states. You can use this parameter to prevent that
   *                                conversion from unintentionally consuming too many resources.
   *                                You may need to increase this limit to run complex regular
   *                                expressions.
   * @param flags                   Enables optional operators for the regular expression. For valid
   *                                values and more information, see Regular expression syntax
   * @return String script
   */
  public static String regexp(String key, String value, int max_determinized_states, String flags) {
    if (StringUtils.isNotBlank(key) && StringUtils.isNotEmpty(value) && StringUtils
        .isNotBlank(flags)) {
      String query = " {\n"
          + "          \"regexp\": {\n"
          + "            \"" + key + "\": {\n"
          + "              \"value\": \"" + value.trim() + "\",\n"
          + "              \"max_determinized_states\": " + max_determinized_states + ",\n"
          + "               \"flags\": \"" + flags + "\"\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return query;
    }
    return null;
  }

  /**
   * 脚本查询 Filters documents based on a provided script. The script query is typically used in a
   * filter context.
   *
   * @param source Contains a script to run as a query. This script must return a boolean value,
   *               true or false.
   * @return String script
   */
  public static String script(String source) {
    if (StringUtils.isNotBlank(source)) {
      String script = "{\n"
          + "          \"script\": {\n"
          + "            \"script\": {\n"
          + "              \"source\": \"" + source + "\"\n"
          + "            }\n"
          + "          }\n"
          + "        }";
      return script;
    }
    return null;
  }

  /**
   * 脚本查询 Filters documents based on a provided script. The script query is typically used in a
   * filter context.
   *
   * @param source Contains a script to run as a query. This script must return a boolean value,
   *               true or false.
   * @param lang   scripting language
   * @return String script
   */
  public static String script(String source, String lang) {
    if (StringUtils.isNotBlank(source) && StringUtils.isNotBlank(lang)) {
      String script = "  {\n"
          + "          \"script\": {\n"
          + "            \"script\": \"" + source + "\",\n"
          + "            \"lang\": \"" + lang + "\"\n"
          + "          }";
      return script;
    }
    return null;
  }

  /**
   * 脚本查询 Filters documents based on a provided script. The script query is typically used in a
   * filter context.
   *
   * @param source Contains a script to run as a query. This script must return a boolean value,
   *               true or false.
   * @param lang   scripting language
   * @param params variable
   * @return String script
   */
  public static String script(String source, String lang, String params) {
    if (StringUtils.isNotBlank(source) && StringUtils.isNotBlank(lang)) {
      String script = "{\n"
          + "        \"script\": {\n"
          + "          \"script\": {\n"
          + "            \"source\": \"" + source + "\",\n"
          + "            \"lang\": \"" + lang + "\",\n"
          + "            \"params\": {" + params + "}\n"
          + "          }\n"
          + "        }\n"
          + "      }";
      return script;
    }
    return null;
  }

  /**
   * 使用较为简单的符号操作进行查询（如+-|等），查询更为严格.
   *
   * <p>Returns documents based on a provided query string, using a parser with a limited but
   * fault-tolerant syntax.
   * <p>
   * This query uses a simple syntax to parse and split the provided query string into terms based
   * on special operators. The query then analyzes each term independently before returning matching
   * documents.</p>
   *
   * @param query query script
   * @param filed field name
   * @return String script
   */
  public static String simple_query_string(String query, Object... filed) {
    if (StringUtils.isNotBlank(query) && CollectUtil.isNotEmpty(filed)) {
      String script = "{\n"
          + "          \"simple_query_string\": {\n"
          + "            \"query\": \"" + query + "\",\n"
          + "            \"fields\": [" + CollectUtil.commaSplit(filed) + "]\n"
          + "          }\n"
          + "        }";
      return script;
    }
    return null;
  }

  /**
   * A query that accepts any other query as base64 encoded string.
   *
   * @param query Base64 encoded string
   * @return String script
   */
  public static String wripper(String query) {
    if (StringUtils.isNotBlank(query)) {
      String script = "{\n"
          + "          \"wrapper\": {\n"
          + "            \"query\": \"" + query + "\"\n"
          + "          }\n"
          + "        }";
      return script;
    }
    return null;
  }


}
