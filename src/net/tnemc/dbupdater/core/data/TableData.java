package net.tnemc.dbupdater.core.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class TableData {
  LinkedHashMap<String, ColumnData> columns = new LinkedHashMap<>();

  private String name;
  private String characterSet = "";
  private String collate = "";
  private String engine = "";

  public TableData(String name) {
    this.name = name;
  }

  public List<String> primaryKeys() {
    List<String> keys = new ArrayList<>();

    for(ColumnData data : columns.values()) {
      if(data.isPrimary()) keys.add(data.getName());
    }
    return keys;
  }

  public void addColumn(ColumnData data) {
    columns.put(data.getName(), data);
  }

  public LinkedHashMap<String, ColumnData> getColumns() {
    return columns;
  }

  public void setColumns(LinkedHashMap<String, ColumnData> columns) {
    this.columns = columns;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public String getEngine() {
    return engine;
  }

  public void setEngine(String engine) {
    this.engine = engine;
  }
}