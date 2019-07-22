package net.tnemc.dbupdater.core.providers.impl;

import net.tnemc.dbupdater.core.providers.FormatProvider;

public class H2Format implements FormatProvider {
  @Override
  public String name() {
    return "H2";
  }

  @Override
  public boolean supportsDefaultCollation() {
    return false;
  }
}