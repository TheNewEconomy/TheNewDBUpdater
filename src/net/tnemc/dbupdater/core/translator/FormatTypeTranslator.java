package net.tnemc.dbupdater.core.translator;

import java.util.Arrays;
import java.util.List;

public interface FormatTypeTranslator {

  /**
   * @param type The type to translate.
   * @return A friendly version of the specified type supported by the FormatProvider using this Translator.
   */
  String translate(String type);

  /**
   * @return A list of data types that are numeric in nature. Example: INTEGER, BIGINT, INT, etc
   */
  default List<String> numericTypes() {
    return Arrays.asList("TINYINT", "SMALLINT", "INT", "INTEGER", "BIGINT", "DECIMAL",
                         "NUMERIC", "FLOAT", "REAL", "DOUBLE", "B");
  }

  /**
   * @return A list of data types that support the additional "scale" value for length.
   * Example: MySQL's DECIMAL(40, 4)
   */
  default List<String> scaleTypes() {
    return Arrays.asList("DECIMAL", "NUMERIC");
  }
}
