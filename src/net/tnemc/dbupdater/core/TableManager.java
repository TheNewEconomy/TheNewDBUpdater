package net.tnemc.dbupdater.core;

import net.tnemc.dbupdater.core.data.ColumnData;
import net.tnemc.dbupdater.core.data.TableData;
import net.tnemc.dbupdater.core.providers.FormatProvider;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TableManager {

  Map<String, TableData> configurationTables = new HashMap<>();
  Map<String, TableData> dataBase = new HashMap<>();

  private LinkedList<String> queries = new LinkedList<>();

  private List<String> prefixes = new ArrayList<>();

  private Map<String, FormatProvider> providers = new HashMap<>();

  private String format = "H2";

  public TableManager(String format) {
    this.format = format;
  }

  public void addFormat(FormatProvider provider) {
    providers.put(provider.name(), provider);
  }

  public FormatProvider provider() {
    return providers.get(format);
  }

  public void generateQueriesAndRun(Connection connection) {
    generateQueries();
    runQueries(connection);
  }

  public void runQueries(Connection connection) {
    for(String query : queries) {
      try(Statement statement = connection.createStatement()) {
        statement.executeUpdate(query);
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void generateQueries() {
    List<String> tablesCreateName = new ArrayList<>();

    for(Map.Entry<String, TableData> entry : configurationTables.entrySet()) {
      if(!dataBase.containsKey(entry.getKey())) {
        queries.add(provider().generateTableCreate(entry.getValue()));
        tablesCreateName.add(entry.getKey());
      }
    }

    for(Map.Entry<String, TableData> entry : configurationTables.entrySet()) {
      if(tablesCreateName.contains(entry.getKey())) continue;

      for(Map.Entry<String, ColumnData> colEntry : entry.getValue().getColumns().entrySet()) {
        if(!dataBase.get(entry.getKey()).getColumns().containsKey(colEntry.getKey())) {
          queries.add(provider().generateAddColumn(entry.getKey(), Collections.singletonList(colEntry.getValue())));
          continue;
        }

        if(!dataBase.get(entry.getKey()).getColumns().get(colEntry.getKey()).toString().equalsIgnoreCase(colEntry.getValue().toString())) {
          queries.add(provider().generateAlterColumn(entry.getKey(), colEntry.getValue()));
        }
      }

      for(Map.Entry<String, ColumnData> colEntry : dataBase.get(entry.getKey()).getColumns().entrySet()) {
        if(!entry.getValue().getColumns().containsKey(colEntry.getKey())) {
          queries.add(provider().generateDropColumn(entry.getKey(), Collections.singletonList(colEntry.getKey())));
        }
      }
    }
  }

  public void generateConfigurationTables() {

  }

  public void generateDataBaseTables(Connection connection) {
    dataBase = provider().getTableData(connection, prefixes);
  }
}