package net.tnemc.dbupdater.core.translator.impl;

import net.tnemc.dbupdater.core.translator.FormatTypeTranslator;

import java.util.HashMap;
import java.util.Map;

public class BasicTypeTranslator implements FormatTypeTranslator {

  private Map<String, String> translations = new HashMap<>();

  public BasicTypeTranslator() {
    translations.put("TI", "TINYINT");
    translations.put("SI", "SMALLINT");
    translations.put("I", "INTEGER");
    translations.put("INT", "INTEGER");
    translations.put("BI", "BIGINT");
    translations.put("BOOLEAN", "TINYINT");
  }

  /**
   * @param type The type to translate.
   *
   * @return A friendly version of the specified type supported by the FormatProvider using this
   * Translator.
   */
  @Override
  public String translate(String type) {
    return translations.getOrDefault(type, type);
  }
}