package net.tnemc.dbupdater.core.providers.impl;

import net.tnemc.dbupdater.core.providers.FormatProvider;

public class MySQLFormat implements FormatProvider {
  @Override
  public String name() {
    return "mysql";
  }
}