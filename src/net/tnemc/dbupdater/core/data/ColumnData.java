package net.tnemc.dbupdater.core.data;

public class ColumnData {


  //Identifying
  private String name;
  private String type;
  private boolean primary = false;
  private boolean unique = false;

  //Length
  private long length = -1;
  private long precision = -1;
  private long scale = -1;

  //Defaults
  private String defaultValue = null;
  private String characterSet = "";
  private String collate = "";

  //Extra
  private boolean nullable = false;
  private boolean increment = false;

  public ColumnData(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isPrimary() {
    return primary;
  }

  public void setPrimary(boolean primary) {
    this.primary = primary;
  }

  public boolean isUnique() {
    return unique;
  }

  public void setUnique(boolean unique) {
    this.unique = unique;
  }

  public long getLength() {
    return length;
  }

  public void setLength(long length) {
    this.length = length;
  }

  public long getPrecision() {
    return precision;
  }

  public void setPrecision(long precision) {
    this.precision = precision;
  }

  public long getScale() {
    return scale;
  }

  public void setScale(long scale) {
    this.scale = scale;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public String getCharacterSet() {
    return characterSet;
  }

  public void setCharacterSet(String characterSet) {
    this.characterSet = characterSet;
  }

  public String getCollate() {
    return collate;
  }

  public void setCollate(String collate) {
    this.collate = collate;
  }

  public boolean isNullable() {
    return nullable;
  }

  public void setNullable(boolean nullable) {
    this.nullable = nullable;
  }

  public boolean isIncrement() {
    return increment;
  }

  public void setIncrement(boolean increment) {
    this.increment = increment;
  }
}