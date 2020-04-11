package org.springframework.boot.elasticsearch.ais;

import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.elasticsearch.ais.restClient.RestPerform;
import org.springframework.boot.elasticsearch.ais.staticString.Bulk;
import org.springframework.boot.elasticsearch.ais.staticString.Find;
import org.springframework.boot.elasticsearch.ais.staticString.Script;
import org.springframework.boot.elasticsearch.ais.staticString.Settings;
import org.springframework.boot.elasticsearch.ais.staticString.Should;
import org.springframework.boot.elasticsearch.ais.utils.StringUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/*
 *@author:lihang
 *@email:631533483@qq.com
 */
@Component
public class Estemplate {

  @Autowired
  private RestPerform restPerform;

  private List must = new ArrayList<String>();
  private List must_not = new ArrayList<String>();
  private List filter_must = new ArrayList<String>();
  private List filter_must_not = new ArrayList<String>();
  private List set = new ArrayList<String>();
  private String request = "get";

  //private RestPerform restPerform=new RestPerformIpml(new Config());

  /**
   * 查询所有 find all.
   */
  public void findAll() {
    must.add(Find.findAll());
  }

  /**
   * set request (post,get,put,delete).
   */
  public void setRequest(String method) {
    this.request = method;
  }

  /**
   * add String script.
   *
   * @param query String script
   */
  public void add(String query) {
    if (query != null) {
      must.add(query);
    }
  }

  /**
   * add String script.
   *
   * @param query String,script
   * @param type  must,must_not,filter_must,filter_must_not
   */
  public void add(String query, String type) {
    if (query != null) {
      if (type.equals("must_not")) {
        must_not.add(query);
      } else if (type.equals("filter_must")) {
        filter_must.add(query);
      } else if (type.equals("filter_must_not")) {
        filter_must_not.add(query);
      } else {
        must.add(query);
      }
    }
  }

  /**
   * add String script.
   *
   * @param query String set script
   */
  public void setadd(String query) {
    if (query != null) {
      set.add(query);
    }
  }

  /**
   * 精确查询 find term.
   *
   * @param key   field name
   * @param value field value
   */
  public void term(String key, Object value) {
    String query = Find.term(key.trim(), value);
    add(query);
  }

  /**
   * 精确查询 find term.
   *
   * @param key   field name
   * @param value field value
   * @param boost Query ranking score
   */
  public void term(String key, Object value, double boost) {
    String query = Find.term(key.trim(), value, boost);
    add(query);
  }

  /**
   * 精确查询 find term.
   *
   * @param key   field name
   * @param value field value
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void term(String type, String key, Object value) {
    String query = Find.term(key.trim(), value);
    add(query, type);
  }

  /**
   * 精确查询 find term.
   *
   * @param key   field name
   * @param value field value
   * @param type  must,must_not,filter_must,filter_nust_not
   * @param boost Query ranking score
   */
  public void term(String type, String key, Object value, double boost) {
    String query = Find.term(key.trim(), value, boost);
    add(query, type);

  }

  /**
   * Custom query statement, parentheses must be aligned.
   *
   * @param query Custom query statement
   */
  public void findFree(String query) {
    if (StringUtils.isNotBlank(query)) {
      add(query);
    }
  }

  /**
   * Custom query statement, parentheses must be aligned.
   *
   * @param type  must,must_not,filter_must,filter_nust_not
   * @param query Custom query statement
   */
  public void findFree(String type, String query) {
    if (StringUtils.isNotBlank(query)) {
      add(query, type);
    }
  }

  /**
   * 分词查询 find match.
   *
   * @param key   field name
   * @param value field value
   */
  public void match(String key, Object value) {
    String query = Find.match(key.trim(), value);
    add(query);
  }

  /**
   * 分词查询 find match.
   *
   * @param key   field name
   * @param value field value
   * @param boost Query ranking score
   */
  public void match(String key, Object value, double boost) {
    String query = Find.match(key.trim(), value, boost);
    add(query);
  }

  /**
   * 分词查询 find match.
   *
   * @param key   field name
   * @param value field value
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void match(String type, String key, Object value) {
    String query = Find.match(key.trim(), value);
    add(query, type);
  }

  /**
   * 分词查询 find match.
   *
   * @param key   field name
   * @param value field value
   * @param boost Query ranking score
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void match(String type, String key, Object value, double boost) {
    String query = Find.match(key.trim(), value, boost);
    add(query, type);
  }

  /**
   * 模糊查询 find wildcard，Left and right have been added * .
   *
   * @param key   field name
   * @param value field value
   */
  public void wildcard(String key, Object value) {
    String query = Find.wildcard(key.trim(), value);
    add(query);
  }

  /**
   * 模糊查询 find wildcard，Left and right have been added * .
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   */
  public void wildcard(String key, Object value, double boost) {
    String query = Find.wildcard(key.trim(), value, boost);
    add(query);
  }

  /**
   * 模糊查询 find wildcard，Left and right have been added * .
   *
   * @param key   field name
   * @param value field value
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void wildcard(String type, String key, Object value) {
    String query = Find.wildcard(key.trim(), value);
    add(query, type);
  }

  /**
   * 模糊查询 find wildcard，Left and right have been added * .
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void wildcard(String type, String key, Object value, double boost) {
    String query = Find.wildcard(key.trim(), value, boost);
    add(query, type);
  }

  /**
   * 模糊查询 find wildcard，Left have been added * .
   *
   * @param key   field name
   * @param value field value
   */
  public void wildcardLeft(String key, Object value) {
    String query = Find.wildcardLeft(key.trim(), value);
    add(query);
  }

  /**
   * 模糊查询 find wildcard，Left have been added * .
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   */
  public void wildcardLeft(String key, Object value, double boost) {
    String query = Find.wildcardLeft(key.trim(), value, boost);
    add(query);
  }

  /**
   * 模糊查询 find wildcard，Left have been added * .
   *
   * @param key   field name
   * @param value field value
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void wildcardLeft(String type, String key, Object value) {
    String query = Find.wildcardLeft(key.trim(), value);
    add(query, type);
  }

  /**
   * 模糊查询 find wildcard，Left have been added * .
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void wildcardLeft(String type, String key, Object value, double boost) {
    String query = Find.wildcardLeft(key.trim(), value, boost);
    add(query, type);
  }


  /**
   * 模糊查询 find wildcard，Right have been added * .
   *
   * @param key   field name
   * @param value field value
   */
  public void wildcardRight(String key, Object value) {
    String query = Find.wildcard(key.trim(), value);
    add(query);
  }


  /**
   * 模糊查询 find wildcard，Right have been added * .
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   */
  public void wildcardRight(String key, Object value, double boost) {
    String query = Find.wildcard(key.trim(), value, boost);
    add(query);
  }


  /**
   * 模糊查询 find wildcard，Right have been added * .
   *
   * @param key   field name
   * @param value field value
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void wildcardRight(String type, String key, Object value) {
    String query = Find.wildcard(key.trim(), value);
    add(query, type);
  }

  /**
   * 模糊查询 find wildcard，Right have been added * .
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void wildcardRight(String type, String key, Object value, double boost) {
    String query = Find.wildcard(key.trim(), value, boost);
    add(query, type);
  }

  /**
   * 精确数组查询 find terms .
   *
   * @param key   field name
   * @param value field value
   */
  public void terms(String key, Object... value) {
    String query = Find.trems(key.trim(), value);
    add(query);
  }

  /**
   * 模糊查询 Left and right not added * .
   *
   * @param key   field name
   * @param value field value
   */
  public void wildcardFree(String key, Object value) {
    String query = Find.wildcardFree(key.trim(), value);
    add(query);
  }

  /**
   * 模糊查询 Left and right not added * .
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   */
  public void wildcardFree(String key, Object value, double boost) {
    String query = Find.wildcardFree(key.trim(), value, boost);
    add(query);
  }

  /**
   * 模糊查询 Left and right not added * .
   *
   * @param key   field name
   * @param value field value
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void wildcardFree(String type, String key, Object value) {
    String query = Find.wildcardFree(key.trim(), value);
    add(query, type);
  }

  /**
   * 模糊查询 Left and right not added * .
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void wildcardFree(String type, String key, Object value, double boost) {
    String query = Find.wildcardFree(key.trim(), value, boost);
    add(query, type);
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
   */
  public void match_phrase_prefix(String key, Object value) {
    String query = Find.match_phrase_prefix(key.trim(), value, 2);
    add(query);
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
   */
  public void match_phrase_prefix(String key, Object value, int slop) {
    String query = Find.match_phrase_prefix(key.trim(), value, slop);
    add(query);
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
   */
  public void match_phrase_prefix(String key, Object value, int slop, int max_expansions) {
    String query = Find.match_phrase_prefix(key.trim(), value, max_expansions, slop);
    add(query);
  }

  /**
   * 短语前缀匹配查询.
   *
   * <p>Returns documents that contain the words of a provided text,
   * in the same order as provided. The last term of the provided text is treated as a prefix,
   * matching any words that begin with that term.<p/>
   *
   * @param type  must,must_not,filter_must,filter_nust_not
   * @param key   field name
   * @param value field value
   */
  public void match_phrase_prefix(String type, String key, Object value) {
    String query = Find.match_phrase_prefix(key.trim(), value, 2);
    add(query, type);
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
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void match_phrase_prefix(String type, String key, Object value, int slop) {
    String query = Find.match_phrase_prefix(key.trim(), value, slop);
    add(query, type);
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
   * @param type           must,must_not,filter_must,filter_nust_not
   */
  public void match_phrase_prefix(String type, String key, Object value, int slop,
      int max_expansions) {
    String query = Find.match_phrase_prefix(key.trim(), value, max_expansions, slop);
    add(query, type);
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
   */
  public void match_phrase(String key, Object value, int slop) {
    String query = Find.match_phrase(key.trim(), value, slop);
    add(query);
  }

  /**
   * 短语匹配查询.
   * <p>The match_phrase query analyzes the text and creates a phrase query out of the analyzed
   * text.</p>
   *
   * @param key   field name
   * @param value field value
   */
  public void match_phrase(String key, Object value) {
    String query = Find.match_phrase(key.trim(), value, 2);
    add(query);
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
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void match_phrase(String type, String key, Object value, int slop) {
    String query = Find.match_phrase(key.trim(), value, slop);
    add(query, type);
  }

  /**
   * 短语匹配查询.
   * <p>The match_phrase query analyzes the text and creates a phrase query out of the analyzed
   * text.</p>
   *
   * @param key   field name
   * @param value field value
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void match_phrase(String type, String key, Object value) {
    String query = Find.match_phrase(key.trim(), value, 2);
    add(query);
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
   */
  public void common(String key, Object value, double cutoff_frequency) {
    String query = Find.common(key.trim(), value, cutoff_frequency);
    add(query);
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
   * @param type             must,must_not,filter_must,filter_nust_not
   */
  public void common(String type, String key, Object value, double cutoff_frequency) {
    String query = Find.common(key.trim(), value, cutoff_frequency);
    add(query, type);
  }

  /**
   * 判断字段是否存在.
   *
   * <p>exits field<p/>
   *
   * @param key field name
   */
  public void exits(String key) {
    String query = Find.exits(key.trim());
    add(query);
  }

  /**
   * 判断字段是否存在.
   *
   * <p>exits field<p/>
   *
   * @param key  field name
   * @param type must,must_not,filter_must,filter_nust_not
   */
  public void exits(String type, String key) {
    String query = Find.exits(key.trim());
    add(query, type);
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
   */
  public void fuzzy(String key, Object value, int fuzziness, int prefix_length, double boost) {
    String query = Find.fuzzy(key.trim(), value, fuzziness, prefix_length, boost);
    add(query);
  }

  /**
   * 相似查询，可以允许错误 Allow bad queries.
   *
   * @param key           field name
   * @param value         field value
   * @param prefix_length Number of beginning characters left unchanged when creating expansions.
   * @param fuzziness     Maximum edit distance allowed for matching. See Fuzziness for valid values
   *                      and more information
   */
  public void fuzzy(String key, Object value, int fuzziness, int prefix_length) {
    String query = Find.fuzzy(key.trim(), value, fuzziness, prefix_length);
    add(query);
  }

  /**
   * 相似查询，可以允许错误 Allow bad queries.
   *
   * @param key       field name
   * @param value     field value
   * @param fuzziness Maximum edit distance allowed for matching. See Fuzziness for valid values and
   *                  more information
   */
  public void fuzzy(String key, Object value, int fuzziness) {
    String query = Find.fuzzy(key.trim(), value, fuzziness, 0);
    add(query);
  }

  /**
   * 相似查询，可以允许错误 Allow bad queries.
   *
   * @param key   field name
   * @param value field value
   */
  public void fuzzy(String key, Object value) {
    String query = Find.fuzzy(key.trim(), value, 2, 0);
    add(query);
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
   * @param type          must,must_not,filter_must,filter_nust_not
   */
  public void fuzzy(String type, String key, Object value, int fuzziness, int prefix_length,
      double boost) {
    String query = Find.fuzzy(key.trim(), value, fuzziness, prefix_length, boost);
    add(query, type);
  }

  /**
   * 相似查询，可以允许错误 Allow bad queries.
   *
   * @param key           field name
   * @param value         field value
   * @param prefix_length Number of beginning characters left unchanged when creating expansions.
   * @param fuzziness     Maximum edit distance allowed for matching. See Fuzziness for valid values
   *                      and more information
   * @param type          must,must_not,filter_must,filter_nust_not
   */
  public void fuzzy(String type, String key, Object value, int fuzziness, int prefix_length) {
    String query = Find.fuzzy(key.trim(), value, fuzziness, prefix_length);
    add(query, type);
  }

  /**
   * 地理位置查询.
   *
   * <p>The geo_shape query uses the same grid square representation as the geo_shape mapping to
   * find documents that have a shape that intersects with the query shape. It will also use the
   * same Prefix Tree configuration as defined for the field mapping</p>
   *
   * @param key         field name
   * @param coordinates coordinate
   * @param relation    Query mode
   */
  public void geo_shape(String key, Object coordinates, Object relation) {
    String query = Find.geo_shape(key.trim(), coordinates, relation);
    add(query);
  }

  /**
   * 地理位置查询.
   *
   * <p>The geo_shape query uses the same grid square representation as the geo_shape mapping to
   * find documents that have a shape that intersects with the query shape. It will also use the
   * same Prefix Tree configuration as defined for the field mapping</p>
   *
   * @param type        must,must_not,filter_must,filter_nust_not
   * @param coordinates coordinate
   * @param relation    Query mode
   * @param key         field name
   */
  public void geo_shape(String type, String key, Object coordinates, Object relation) {
    String query = Find.geo_shape(key.trim(), coordinates, relation);
    add(query, type);
  }


  /**
   * id查询.
   *
   * <p>Returns documents based on their IDs. This query uses document IDs stored in the _id field.
   * <p/>
   *
   * @param values ids valus
   */
  public void ids(Object... values) {
    String query = Find.ids(values);
    add(query);
  }

  /**
   * id查询.
   *
   * <p>Returns documents based on their IDs. This query uses document IDs stored in the _id field.
   * <p/>
   *
   * @param values ids valus
   * @param type   must,must_not,filter_must,filter_nust_not
   */
  public void ids(String type, Object... values) {
    String query = Find.ids(values);
    add(query, type);
  }


  /**
   * 多字段分词查询.
   * <p>
   * The multi_match query builds on the match query to allow multi-field queries
   * </p>
   *
   * @param value field value
   * @param keys  field name
   */
  public void multi_match(String value, String... keys) {
    String query = Find.multi_match(value, keys);
    add(query);
  }

  /**
   * 多字段分词查询.
   * <p>
   * The multi_match query builds on the match query to allow multi-field queries
   * </p>
   *
   * @param value field value
   * @param keys  field name
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void multi_match(String type, String value, Object... keys) {
    String query = Find.multi_match(value, keys);
    add(query, type);
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
   */
  public void more_like_this(int min_term_freq, int max_query_terms, String value, String... keys) {
    String query = Find.more_like_this(min_term_freq, max_query_terms, value, keys);
    add(query);
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
   * @param type            must,must_not,filter_must,filter_nust_not
   */
  public void more_like_this(String type, int min_term_freq, int max_query_terms, String value,
      String... keys) {
    String query = Find.more_like_this(min_term_freq, max_query_terms, value, keys);
    add(query, type);
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
   */
  public void percolate(String key, Object value, String field) {
    String query = Find.percolate(key.trim(), value, field.trim());
    add(query);
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
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void percolate(String type, String key, Object value, String field) {
    String query = Find.percolate(key.trim(), value, field.trim());
    add(query, type);
    ;
  }

  /**
   * 左模糊查询.
   *
   * <p>Returns documents that contain a specific prefix in a provided field.</p>
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   */
  public void prefix(String key, Object value, double boost) {
    String query = Find.prefix(key.trim(), value, boost);
    add(query);
  }

  /**
   * 左模糊查询.
   *
   * <p>Returns documents that contain a specific prefix in a provided field.</p>
   *
   * @param key   field name
   * @param value field value
   */
  public void prefix(String key, Object value) {
    String query = Find.prefix(key.trim(), value);
    add(query);
  }

  /**
   * 左模糊查询.
   *
   * <p>Returns documents that contain a specific prefix in a provided field.</p>
   *
   * @param key   field name
   * @param value field value
   * @param boost boost Query ranking score
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void prefix(String type, String key, Object value, double boost) {
    String query = Find.prefix(key.trim(), value, boost);
    add(query, type);
  }

  /**
   * 左模糊查询.
   *
   * <p>Returns documents that contain a specific prefix in a provided field.</p>
   *
   * @param key   field name
   * @param value field value
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void prefix(String type, String key, Object value) {
    String query = Find.prefix(key.trim(), value);
    add(query, type);
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
   */
  public void query_string(String default_field, Object query_) {
    String query = Find.query_string(default_field.trim(), query_);
    add(query);
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
   * @param type          must,must_not,filter_must,filter_nust_not
   */
  public void query_string(String type, String default_field, Object query_) {
    String query = Find.query_string(default_field.trim(), query_);
    add(query, type);
  }

  /**
   * 范围查询 Returns documents that contain terms within a provided range.
   *
   * @param key field name
   * @param gte Greater than or equal to
   * @param lte Less than or equal to.
   */
  public void range(String key, Object gte, Object lte) {
    String query = Find.range(key.trim(), gte, lte);
    add(query);
  }

  /**
   * 范围查询 Returns documents that contain terms within a provided range.
   *
   * @param key  field name
   * @param gte  Greater than or equal to
   * @param lte  Less than or equal to.
   * @param type must,must_not,filter_must,filter_nust_not
   */
  public void range(String type, String key, Object gte, Object lte) {
    String query = Find.range(key.trim(), gte, lte);
    add(query, type);
  }

  /**
   * 范围查询 Returns documents that contain terms within a provided range.
   *
   * @param key field name
   * @param lte Less than or equal to.
   */
  public void rangelte(String key, Object lte) {
    String query = Find.rangelte(key.trim(), lte);
    add(query);
  }

  /**
   * 范围查询 Returns documents that contain terms within a provided range.
   *
   * @param key  field name
   * @param lte  Less than or equal to.
   * @param type must,must_not,filter_must,filter_nust_not
   */
  public void rangelte(String type, String key, Object lte) {
    String query = Find.rangelte(key.trim(), lte);
    add(query, type);
  }

  /**
   * 范围查询 Returns documents that contain terms within a provided range.
   *
   * @param key field name
   * @param gte Greater than or equal to
   */
  public void rangegte(String key, Object gte) {
    String query = Find.rangegte(key.trim(), gte);
    add(query);
  }

  /**
   * 范围查询 Returns documents that contain terms within a provided range.
   *
   * @param key  field name
   * @param gte  Greater than or equal to
   * @param type must,must_not,filter_must,filter_nust_not
   */
  public void rangegte(String type, String key, Object gte) {
    String query = Find.rangegte(key.trim(), gte);
    add(query, type);
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
   */
  public void regexp(String key, Object value, int max_determinized_states) {
    String query = Find.regexp(key.trim(), value, max_determinized_states);
    add(query);
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
   */
  public void regexp(String key, String value) {
    String query = Find.regexp(key.trim(), value);
    add(query);
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
   */
  public void regexp(String key, String value, int max_determinized_states, String flags) {
    String query = Find.regexp(key.trim(), value, max_determinized_states, flags.trim());
    add(query);
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
   * @param type                    must,must_not,filter_must,filter_nust_not
   */
  public void regexp(String type, String key, Object value, int max_determinized_states) {
    String query = Find.regexp(key.trim(), value, max_determinized_states);
    add(query, type);
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
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void regexp(String type, String key, String value) {
    String query = Find.regexp(key.trim(), value);
    add(query, type);
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
   * @param type                    must,must_not,filter_must,filter_nust_not
   */
  public void regexp(String type, String key, String value, int max_determinized_states,
      String flags) {
    String query = Find.regexp(key.trim(), value, max_determinized_states, flags.trim());
    add(query, type);
  }

  /**
   * 脚本查询 Filters documents based on a provided script. The script query is typically used in a
   * filter context.
   *
   * @param source Contains a script to run as a query. This script must return a boolean value,
   *               true or false.
   */
  public void script(String source) {
    String query = Find.script(source.trim());
    add(query, "filter_must");
  }

  /**
   * 脚本查询 Filters documents based on a provided script. The script query is typically used in a
   * filter context.
   *
   * @param source Contains a script to run as a query. This script must return a boolean value,
   *               true or false.
   * @param lang   scripting language
   */
  public void script(String source, String lang) {
    String query = Find.script(source.trim(), lang.trim());
    add(query, "filter_must");
  }

  /**
   * 脚本查询 Filters documents based on a provided script. The script query is typically used in a
   * filter context.
   *
   * @param source Contains a script to run as a query. This script must return a boolean value,
   *               true or false.
   * @param lang   scripting language
   * @param params variable
   */
  public void script(String source, String lang, String params) {
    String query = Find.script(source.trim(), lang.trim(), params.trim());
    add(query, "filter_must");
  }

  /**
   * 脚本查询 Filters documents based on a provided script. The script query is typically used in a
   * filter context.
   *
   * @param source Contains a script to run as a query. This script must return a boolean value,
   *               true or false.
   * @param lang   scripting language
   * @param params variable
   * @param type   must,must_not,filter_must,filter_nust_not
   */
  public void script(String type, String source, String lang, String params) {
    String query = Find.script(source.trim(), lang.trim(), params.trim());
    add(query, type);
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
   * @param value query script
   * @param filed field name
   */
  public void simple_query_string(String value, Object... filed) {
    String query_ = Find.simple_query_string(value.trim(), filed);
    add(query_);
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
   * @param value query script
   * @param filed field name
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void simple_query_string(String type, String value, Object... filed) {
    String query_ = Find.simple_query_string(value.trim(), filed);
    add(query_, type);
  }

  /**
   * A query that accepts any other query as base64 encoded string.
   *
   * @param query Base64 encoded string
   */
  public void wripper(String query) {
    String query_ = Find.wripper(query.trim());
    add(query_);
  }

  /**
   * A query that accepts any other query as base64 encoded string.
   *
   * @param query Base64 encoded string
   * @param type  must,must_not,filter_must,filter_nust_not
   */
  public void wripper(String type, String query) {
    String query_ = Find.wripper(query.trim());
    add(query_, type);
  }


  /**
   * query should.
   *
   * @param script query script
   */
  public void should(String... script) {
    String query = Should.should(script);
    add(query);
  }

  /**
   * query should.
   *
   * @param script               query script
   * @param minimum_should_match Minimum quantity satisfied
   */
  public void should(int minimum_should_match, String... script) {
    String query = Should.should(minimum_should_match, script);
    add(query);
  }

  /**
   * 精确数组查询 find not terms .
   *
   * @param key   field name
   * @param value field value
   */
  public void terms_not(String key, Object... value) {
    String query = Find.trems(key.trim(), value);
    add(query, "must_not");
  }


  /**
   * field filter.
   *
   * @param fields fields name
   */
  public void source(String... fields) {
    String script = Settings.source(fields);
    setadd(script);
  }

  /**
   * version.
   *
   * @param type true flase
   */
  public void version(boolean type) {
    String script = Settings.version(type);
    setadd(script);
  }

  /**
   * timeout.
   *
   * @param i timeout
   */
  public void timeout(int i) {
    String script = Settings.timeout(i);
    setadd(script);
  }

  /**
   * field filter.
   *
   * @param fields fields name
   */
  public void stored_fields(String... fields) {
    String script = Settings.stored_fields(fields);
    setadd(script);
  }

  /**
   * Statistical results.
   *
   * @param fields fields name
   */
  public void stats(String... fields) {
    String script = Settings.stats(fields);
    set.add(fields);
  }

  /**
   * sort order.
   *
   * @param field field name
   * @param order desc asc
   */
  public void sort(String field, String order) {
    String script = Settings.sort(field, order);
    setadd(script);
  }

  /**
   * sort order.
   *
   * @param fieldorder field name and sort,such as ["name","asc","age","desc"]
   * @return String script
   */
  public void sort(String... fieldorder) {
    String script = Settings.sort(fieldorder);
    setadd(script);
  }

  /**
   * size.
   *
   * @param s size num
   * @return String script
   */
  public void size(Integer s) {
    if (s != null) {
      String script = Settings.size(s);
      setadd(script);
    }
  }

  /**
   * script.
   *
   * @param source script String
   */
  public void script_fields(String source) {
    String script = Settings.script_fields(source);
    setadd(script);
  }

  /**
   * polymerization process.
   *
   * @param b true,false
   */
  public void profile(boolean b) {
    String script = Settings.profile(b);
    setadd(script);
  }

  /**
   * field filter.
   *
   * @param include include field name
   * @param exclude exclude field name
   */
  public void partial_fields(String[] include, String[] exclude) {
    String script = Settings.partial_fields(include, exclude);
    setadd(script);
  }

  /**
   * Correlation control.
   *
   * @param index field name
   * @param boost field num
   */
  public void indices_boost(String[] index, int[] boost) {
    String script = Settings.indices_boost(index, boost);
    setadd(script);
  }

  /**
   * highlight field.
   */
  public void highlight(String key) {
    String script = Settings.highlight(key);
    setadd(script);
  }

  /**
   * highlight field.
   *
   * @param pre_tags  Prefix symbol
   * @param post_tags Suffix symbol
   */
  public void highlight(String key, String pre_tags, String post_tags) {
    String script = Settings.highlight(key, pre_tags, post_tags);
    setadd(script);
  }

  /**
   * from.
   *
   * @param i from num
   */
  public void from(Integer i) {
    if (i != null) {
      String script = Settings.from(i);
      setadd(script);
    }
  }

  /**
   * explain.
   *
   * @param b true,false
   */
  public void explain(boolean b) {
    String script = Settings.explain(b);
    setadd(script);
  }

  /**
   * Another query .
   *
   * @param fields fields name
   */
  public void docvalue_fields(String... fields) {
    String script = Settings.docvalue_fields(fields);
    setadd(script);
  }

  /**
   * field collapse .
   *
   * @param fields field name
   */
  public void collapse(String fields) {
    String script = Settings.collapse(fields);
    setadd(script);
  }


  /**
   * Custom settings, symbols must be standardized.
   *
   * @param settings set script String
   */
  public void setSettings(String settings) {
    setadd(settings);
  }

  /**
   * Assembly code.
   */
  private String scriptJoin() {
    String must_ = String.join(",", must);
    String mustNot_ = String.join(",", must_not);
    String filter_must_ = String.join(",", filter_must);
    String filter_must_not_ = String.join(",", filter_must_not);
    String set_ = String.join(",", set);
    String script;
    if (StringUtils.isNotBlank(must_) || StringUtils.isNotBlank(mustNot_) && StringUtils
        .isNotBlank(filter_must_) && StringUtils.isNotBlank(filter_must_not_)) {
      if (StringUtils.isNotBlank(set_)) {
        script = Script.getScript(must_, mustNot_, filter_must_, filter_must_not_, "," + set_);
      } else {
        script = Script.getScript(must_, mustNot_, filter_must_, filter_must_not_, "");
      }
      must.clear();
      must_not.clear();
      filter_must.clear();
      filter_must_not.clear();
      set.clear();
      return script;
    }
    return null;
  }

  /**
   * Insert a piece of data.
   *
   * @param <T>    data type
   * @param tclass data
   * @param index  index
   */
  public <T> JSONObject insert(T tclass, String index) throws IOException {
    String script = Bulk.insert(tclass);
    return restPerform.executeJSON("POST", index + "/_doc", script);
  }

  /**
   * Insert  data.
   *
   * @param <T>       data type
   * @param classList Data array
   * @param index     index
   */
  public <T> JSONObject insert(List<T> classList, String index) throws IOException {
    String script = Bulk.insert(classList);
    return restPerform.executeJSON("POST", index + "/_bulk", script);
  }

  /**
   * Insert data.
   *
   * @param <K>   data ID
   * @param <V>   data
   * @param index index
   */
  public <K, V> JSONObject insert(Map<K, V> map, String index) throws IOException {
    String script = Bulk.insert(map);
    return restPerform.executeJSON("POST", index + "/_bulk", script);
  }

  /**
   * update  data.
   *
   * @param <I>   data ID
   * @param <K>   data Field name
   * @param <V>   data Field value
   * @param index index
   */
  public <I, K, V> JSONObject update(Map<I, Map<K, V>> map, String index) throws IOException {
    if (!map.isEmpty() && !map.values().isEmpty()) {
      String script = Bulk.update(map);
      return restPerform.executeJSON("POST", index + "/_bulk", script);
    }
    return null;
  }

  /**
   * update  data.
   *
   * @param <K>   data Field name
   * @param <V>   data Field value
   * @param index index
   */
  public <K, V> JSONObject update(Object id, Map<K, V> map, String index) throws IOException {
    if (!map.isEmpty()) {
      String script = Bulk.update(id, map);
      return restPerform.executeJSON("POST", index + "/_bulk", script);
    }
    return null;
  }

  /**
   * delete  data.
   *
   * @param ids one or more ids
   */
  public JSONObject delete(String index, Object... ids) throws IOException {
    String script = Bulk.delete(ids);
    if (StringUtils.isNotBlank(script)) {
      return restPerform.executeJSON("POST", index + "/_bulk", script);
    }
    return null;
  }

  /**
   * Delete the contents of the query.
   *
   * @param index index
   */
  public JSONObject delete_by_query(String index) throws IOException {
    String script = scriptJoin();
    if (StringUtils.isNotBlank(script)) {
      return restPerform.executeJSON("POST", index + "/_delete_by_query", script);
    }
    return null;
  }

  /**
   * Update query data.
   *
   * @param <T> The type of the corresponding field data needs to be modified
   * @param map The name and type of the corresponding field data need to be modified
   */
  public <T> JSONObject update_by_query(Map<String, T> map, String index) throws IOException {
    String script_ = Bulk.update_by_query(map);
    this.setSettings(script_);
    String script = scriptJoin();
    return restPerform.executeJSON("POST", index + "/_update_by_query", script);
  }

  /**
   * Executing scripts and parsing returned values.
   *
   * @param index  index
   * @param tClass data type
   * @param <T>    data type
   * @return data list
   */
  public <T> List<T> execute(String index, Class<T> tClass) throws IOException {
    String script = scriptJoin();
    return restPerform.execute(request, index + "/_search", script, tClass);
  }

  /**
   * Executing scripts and parsing returned values.
   *
   * @param requestMethod Request mode(get,put,post,delete)
   * @param index         index
   * @param tClass        data type
   * @param <T>           data type
   * @return data list
   */
  public <T> List<T> execute(String requestMethod, String index, Class<T> tClass)
      throws IOException {
    String script = scriptJoin();
    return restPerform.execute(requestMethod, index, script, tClass);
  }

  public <T> List<T> executeAsync(String index, Class<T> tClass)
      throws IOException, InterruptedException {
    String script = scriptJoin();
    return restPerform.executeAsync(request, index + "/_search", script, 1, tClass);
  }

  public <T> List<T> executeAsync(String requestMethod, String url, Class<T> tClass)
      throws IOException, InterruptedException {
    String script = scriptJoin();
    return restPerform.executeAsync(requestMethod, url, script, 1, tClass);
  }

  /**
   * Executing scripts and parsing returned values.
   *
   * @param index index
   * @return data JSON
   */
  public JSONObject executeJSON(String index) throws IOException {
    String script = scriptJoin();
    return restPerform.executeJSON(request, index + "/_search", script);
  }

  /**
   * Executing scripts and parsing returned values.
   *
   * @param requestMethod Request mode(get,put,post,delete)
   * @param index         index
   * @return data JSON
   */
  public JSONObject executeJSON(String requestMethod, String index) throws IOException {
    String script = scriptJoin();
    return restPerform.executeJSON(requestMethod, index, script);
  }

  public JSONObject executeJSONAsync(String index) throws IOException, InterruptedException {
    String script = scriptJoin();
    return restPerform.executeJSONAsync(request, index + "/_search", script, 1);
  }

  public JSONObject executeJSONAsync(String requestMethod, String url)
      throws IOException, InterruptedException {
    String script = scriptJoin();
    return restPerform.executeJSONAsync(requestMethod, url, script, 1);
  }

  /**
   * Executing scripts and parsing returned values.
   *
   * @param index  index
   * @param tClass data type
   * @param <T>    data type
   * @return data a
   */
  public <T> T executeOne(String index, Class<T> tClass) throws IOException {
    String script = scriptJoin();
    return restPerform.executeOne(request, index + "_search", script, tClass);
  }

  /**
   * Executing scripts and parsing returned values.
   *
   * @param requestMethod Request mode(get,put,post,delete)
   * @param index         index
   * @param tClass        data type
   * @param <T>           data type
   * @return data a
   */
  public <T> T executeOne(String requestMethod, String index, Class<T> tClass) throws IOException {
    String script = scriptJoin();
    return restPerform.executeOne(requestMethod, index, script, tClass);
  }

  public <T> T executeOneAsync(String index, Class<T> tClass)
      throws IOException, InterruptedException {
    String script = scriptJoin();
    return restPerform.executeOneAsync(request, index + "_search", script, 1, tClass);
  }


  public <T> T executeOneAsync(String requestMethod, String url, Class<T> tClass)
      throws IOException, InterruptedException {
    String script = scriptJoin();
    return restPerform.executeOneAsync(requestMethod, url, script, 1, tClass);
  }


}
