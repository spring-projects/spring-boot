package org.springframework.boot.elasticsearch.ais.utils;

/**
 * is null? .
 *
 * @author lihang
 * @email 631533483@qq.com
 */
public class StringUtils {


  /**
   * is null? .
   */
  public static boolean isBlank(CharSequence cs) {
    int length;
    if ((cs == null) || (length = cs.length()) == 0) {
      return true;
    }
    for (int i = 0; i < length; i++) {
      if (!Character.isWhitespace(cs.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * is not null? .
   */
  public static boolean isNotBlank(CharSequence cs) {
    return !isBlank(cs);
  }

  /**
   * is null? .
   */
  public static boolean isEmpty(Object object) {
    if (object != null) {
      return isBlank(object.toString());
    }
    return true;
  }

  /**
   * is not null? .
   */
  public static boolean isNotEmpty(Object object) {
    return !isEmpty(object);
  }
}
