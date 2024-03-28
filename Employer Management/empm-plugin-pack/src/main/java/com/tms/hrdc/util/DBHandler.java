/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.util;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ListIterator;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;

/**
 *
 * @author faizr
 */
public class DBHandler {
    private Connection con;
    
    public void openConnection() throws SQLException {
        
        DataSource ds = (DataSource)AppUtil.getApplicationContext().getBean("setupDataSource");
        if(getConnection() == null){
            this.con = ds.getConnection();
        }        
    }

    public void openConnection(DataSource ds) throws SQLException {
        if(getConnection() == null){
            this.con = ds.getConnection();
        }        
    }
    
    public void openConnection(Connection con) throws SQLException {
        this.con = con;
    }

    public void closeConnection() {
        try {
            if(this.con != null) {
                this.con.close();
            }
        } catch(SQLException e) {
            LogUtil.error(getClassName(), e, e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return this.con;
    }

    public boolean isRecordAvailable(String table, String whereColumn, String whereValue) {
        boolean result = false;

        ResultSet rSet = null;
        PreparedStatement stmt = null;

        try {
            table = formatTableName(table);
            whereColumn = formatColumnName(whereColumn);

            String sql = "SELECT id FROM " + table + " WHERE " + whereColumn + " = ? ";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, whereValue);
            rSet = stmt.executeQuery();
            if (rSet.next()) { //if got data
                result = true;
            }
            rSet.close();
            stmt.close();
        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(rSet != null) {
                    rSet.close();
                }
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    public boolean isRecordAvailable(String sql) {
        ArrayList<String> conditions = new ArrayList<>();

        return isRecordAvailable(sql, conditions);
    }

    public boolean isRecordAvailable(String sql, String[] conditions) {
        ArrayList<String> conditionsList = new ArrayList<String>(Arrays.asList(conditions));

        return isRecordAvailable(sql, conditionsList);
    }

    public boolean isRecordAvailable(String sql, ArrayList<String> conditions) {
        boolean result = false;

        ResultSet rSet = null;
        PreparedStatement stmt = null;

        try {
            stmt = con.prepareStatement(sql);
            if (conditions != null) {
                int condCtr = 1;
                for (String cond : conditions) {
                    stmt.setString(condCtr, cond);

                    condCtr = condCtr + 1;
                }
            }
            rSet = stmt.executeQuery();
            if (rSet.next()) { //if got data
                result = true;
            }
            rSet.close();
            stmt.close();
        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(rSet != null) {
                    rSet.close();
                }
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    public String selectFoundRows() {
        String result = "0";

        ResultSet rSet = null;
        PreparedStatement stmt = null;

        try {
            String sql = "SELECT FOUND_ROWS() AS total ";
            stmt = con.prepareStatement(sql);
            rSet = stmt.executeQuery();

            while (rSet.next()) {
                result = rSet.getObject(1) != null ? rSet.getString(1) : "0";
            }
            rSet.close();
            stmt.close();
        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(rSet != null) {
                    rSet.close();
                }
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    public ArrayList<HashMap<String, String>> select(String sql) {
        
        ArrayList<String> conditions = new ArrayList<>();

        return select(sql, conditions);
    }

    public ArrayList<HashMap<String, String>> select(String sql, String[] conditions) {
        ArrayList<String> conditionsList = new ArrayList<String>(Arrays.asList(conditions));
        return select(sql, conditionsList);
    }

    public ArrayList<HashMap<String, String>> select(String sql, ArrayList<String> conditions) {
        ArrayList<HashMap<String, String>> result = new ArrayList<>();

        ResultSet rSet = null;
        PreparedStatement stmt = null;

        try {
            stmt = con.prepareStatement(sql);
            
            if (conditions != null) {
                int condCtr = 1;
                for (String cond : conditions) {
                    stmt.setString(condCtr, cond);

                    condCtr = condCtr + 1;
                }
            }
            rSet = stmt.executeQuery();
            ResultSetMetaData rSetMetaData = rSet.getMetaData();

            ArrayList<String> columnList = new ArrayList<>();
            for (int i = 1; i <= rSetMetaData.getColumnCount(); i++) {
                String column = rSetMetaData.getColumnLabel(i);
                columnList.add(column);
            }
            
            while (rSet.next()) {
                HashMap<String, String> row = new HashMap<>();
                ListIterator<String> columnListItr = columnList.listIterator();
                while(columnListItr.hasNext()){
                    String column = columnListItr.next();
                    String value = rSet.getObject(column) != null ? rSet.getString(column) : "";
                    row.put(column, value);
                }

                result.add(row);
            }
            rSet.close();
            stmt.close();
        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(rSet != null) {
                    rSet.close();
                }
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    public HashMap<String, String> selectOneRecord(String sql) {
        HashMap<String, String> result = null;

        ArrayList<HashMap<String, String>> res = select(sql);
        if (res.size() > 0) {
            result = res.get(0);
        }

        return result;
    }

    public HashMap<String, String> selectOneRecord(String sql, String[] conditions) {
        HashMap<String, String> result = null;

        ArrayList<String> conditionsList = new ArrayList<String>(Arrays.asList(conditions));
        ArrayList<HashMap<String, String>> res = select(sql, conditionsList);
        if (res.size() > 0) {
            result = res.get(0);
        }

        return result;
    }

    public String selectOneValueFromId(String table, String column, String id) {
        return selectOneValueFromTable(table, column, "id", id);
    }

    public String selectOneValueFromTable(String table, String column, String whereColumn, String whereValue) {
        String result = "";

        ResultSet rSet = null;
        PreparedStatement stmt = null;

        try {
            table = formatTableName(table);
            column = formatColumnName(column);
            whereColumn = formatColumnName(whereColumn);

            String sql = "SELECT " + column + " FROM " + table + " WHERE " + whereColumn + " = ? "
                    + "ORDER BY dateModified DESC "
                    + "LIMIT 1 ";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, whereValue);
            rSet = stmt.executeQuery();

            while (rSet.next()) {
                result = rSet.getObject(column) != null ? rSet.getString(column) : "";
            }
            rSet.close();
            stmt.close();
        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(rSet != null) {
                    rSet.close();
                }
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    public String selectOneValueFromTable(String table, String column, HashMap<String, String> conditions) {
        String result = "";

        ResultSet rSet = null;
        PreparedStatement stmt = null;

        try {
            table = formatTableName(table);
            column = formatColumnName(column);

            String sql = "SELECT " + column + " FROM " + table + " ";
            if (conditions != null) {
                boolean firstCond = true;
                for (HashMap.Entry<String, String> cond : conditions.entrySet()) {
                    String whereColumn = formatColumnName(cond.getKey());

                    if (firstCond) {
                        firstCond = false;
                        sql = sql + "WHERE " + whereColumn + " = ? ";
                    }
                    else {
                        sql = sql + "AND " + whereColumn + " = ? ";
                    }
                }
            }
            sql = sql + "ORDER BY dateModified DESC "
                    + "LIMIT 1 ";

            stmt = con.prepareStatement(sql);
            int ctr = 1;
            if (conditions != null) {
                for (HashMap.Entry<String, String> cond : conditions.entrySet()) {
                    stmt.setString(ctr, cond.getValue());

                    ctr = ctr + 1;
                }
            }
            rSet = stmt.executeQuery();

            while (rSet.next()) {
                result = rSet.getObject(column) != null ? rSet.getString(column) : "";
            }
            rSet.close();
            stmt.close();
        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(rSet != null) {
                    rSet.close();
                }
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    public String selectOneValueFromTable(String sql, String[] conditions) {
        ArrayList<String> conditionsList = new ArrayList<String>(Arrays.asList(conditions));

        return selectOneValueFromTable(sql, conditionsList);
    }

    public String selectOneValueFromTable(String sql, ArrayList<String> conditions) {
        String result = "";

        ResultSet rSet = null;
        PreparedStatement stmt = null;

        try {
            stmt = con.prepareStatement(sql);
            if (conditions != null) {
                int condCtr = 1;
                for (String cond : conditions) {
                    stmt.setString(condCtr, cond);

                    condCtr = condCtr + 1;
                }
            }
            rSet = stmt.executeQuery();

            while (rSet.next()) {
                result = rSet.getObject(1) != null ? rSet.getString(1) : "";
            }
            rSet.close();
            stmt.close();
        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(rSet != null) {
                    rSet.close();
                }
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    public int update(String sql) {
        int result = 0;

        PreparedStatement stmt = null;

        try {
            stmt = con.prepareStatement(sql);
            result = stmt.executeUpdate();
            stmt.close();
        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    //public int update(String sql, ArrayList<String> values, ArrayList<String> conditions) {
    public int update(String sql, String[] values, String[] conditions) {
        int result = 0;

        PreparedStatement stmt = null;

        try {
            stmt = con.prepareStatement(sql);

            int ctr = 1;
            if (values != null) {
                for (String value : values) {
                    stmt.setString(ctr, value);

                    ctr = ctr + 1;
                }
            }
            if (conditions != null) {
                for (String cond : conditions) {
                    stmt.setString(ctr, cond);

                    ctr = ctr + 1;
                }
            }
            result = stmt.executeUpdate();
            stmt.close();
        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    public int update(String table, HashMap<String, String> values, HashMap<String, String> conditions) {
        int result = 0;

        PreparedStatement stmt = null;

        try {
            if (!StringUtils.isBlank(table)) {
                table = formatTableName(table);

                String sql = "UPDATE " + table + " SET dateModified = NOW() ";
                if (values != null) {
                    for (HashMap.Entry<String, String> value : values.entrySet()) {
                        String column = formatColumnName(value.getKey());

                        sql = sql + ", " + column + " = ? ";
                    }
                }
                if (conditions != null) {
                    boolean firstCond = true;
                    for (HashMap.Entry<String, String> cond : conditions.entrySet()) {
                        String column = formatColumnName(cond.getKey());

                        if (firstCond) {
                            firstCond = false;
                            sql = sql + "WHERE " + column + " = ? ";
                        }
                        else {
                            sql = sql + "AND " + column + " = ? ";
                        }
                    }
                }
                stmt = con.prepareStatement(sql);

                int ctr = 1;
                if (values != null) {
                    for (HashMap.Entry<String, String> value : values.entrySet()) {
                        stmt.setString(ctr, value.getValue());

                        ctr = ctr + 1;
                    }
                }
                if (conditions != null) {
                    for (HashMap.Entry<String, String> cond : conditions.entrySet()) {
                        stmt.setString(ctr, cond.getValue());

                        ctr = ctr + 1;
                    }
                }
                result = stmt.executeUpdate();
                stmt.close();
            }

        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    public int delete(String sql) {
        int result = 0;

        PreparedStatement stmt = null;

        try {
            stmt = con.prepareStatement(sql);
            result = stmt.executeUpdate();
            stmt.close();
        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    public int delete(String sql, String[] conditions) {
        int result = 0;

        PreparedStatement stmt = null;

        try {
            stmt = con.prepareStatement(sql);

            int ctr = 1;
            if (conditions != null) {
                for (String cond : conditions) {
                    stmt.setString(ctr, cond);

                    ctr = ctr + 1;
                }
            }
            result = stmt.executeUpdate();
            stmt.close();
        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    public int delete(String table, HashMap<String, String> conditions) {
        int result = 0;

        PreparedStatement stmt = null;

        try {
            if (!StringUtils.isBlank(table)) {
                table = formatTableName(table);

                String sql = "";
                if (conditions != null) {
                    sql = "DELETE FROM " + table + " ";

                    boolean firstCond = true;
                    for (HashMap.Entry<String, String> cond : conditions.entrySet()) {
                        String column = formatColumnName(cond.getKey());

                        if (firstCond) {
                            firstCond = false;
                            sql = sql + "WHERE " + column + " = ? ";
                        }
                        else {
                            sql = sql + "AND " + column + " = ? ";
                        }
                    }

                    stmt = con.prepareStatement(sql);

                    int ctr = 1;
                    if (conditions != null) {
                        for (HashMap.Entry<String, String> cond : conditions.entrySet()) {
                            stmt.setString(ctr, cond.getValue());

                            ctr = ctr + 1;
                        }
                    }
                    result = stmt.executeUpdate();
                    stmt.close();
                }
            }

        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    public int insert(String sql, String[] values) {
        int result = 0;

        PreparedStatement stmt = null;

        try {
            stmt = con.prepareStatement(sql);

            int ctr = 1;
            if (values != null) {
                for (String value : values) {
                    stmt.setString(ctr, value);

                    ctr = ctr + 1;
                }
            }
            result = stmt.executeUpdate();
            stmt.close();

        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    public int insert(String table, HashMap<String, String> values) {
        int result = 0;

        PreparedStatement stmt = null;

        try {
            if (!StringUtils.isBlank(table) && values != null) {
                if (values.size() > 0) {
                    table = formatTableName(table);

                    String columnSql = "";
                    String columnSqlValues = "";
                    for (HashMap.Entry<String, String> value : values.entrySet()) {
                        String column = formatColumnName(value.getKey());

                        if (StringUtils.isBlank(columnSql)) {
                            columnSql = column;
                            columnSqlValues = "?";
                        }
                        else {
                            columnSql = columnSql + ", " + column;
                            columnSqlValues = columnSqlValues + ", ?";
                        }
                    }
                    String sql = "INSERT INTO " + table + " (" + columnSql + ") "
                            + "VALUES (" + columnSqlValues + ") ";
                    stmt = con.prepareStatement(sql);

                    int ctr = 1;
                    for (HashMap.Entry<String, String> value : values.entrySet()) {
                        stmt.setString(ctr, value.getValue());

                        ctr = ctr + 1;
                    }
                    result = stmt.executeUpdate();
                    stmt.close();
                }
            }

        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        } finally {
            try {
                if(stmt != null) {
                    stmt.close();
                }
            } catch(SQLException e) {
                LogUtil.error(getClassName(), e, e.getMessage());
            }
        }

        return result;
    }

    public void insertAuditTrailEntry(String auditTable, String appId, String recordId, String loggedBy, String logStatus, String logRemarks) {
        PreparedStatement stmt = null;

        try {
            auditTable = formatAuditTableName(auditTable);

            String uuid = UuidGenerator.getInstance().getUuid();
            String sql = "INSERT INTO " + auditTable + " "
                    + "(id, appDefId, rowId, loggedBy, loggedOn, logStatus, logRemarks) "
                    + "VALUES(?, ?, ?, ?, NOW(), ?, ?)";

            stmt = con.prepareStatement(sql);
            stmt.setString(1, uuid);
            stmt.setString(2, appId);
            stmt.setString(3, recordId);
            stmt.setString(4, loggedBy);
            stmt.setString(5, logStatus);
            stmt.setString(6, logRemarks);
            stmt.executeUpdate();
            stmt.close();
        }
        catch (Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        }
    }

    private String formatAuditTableName(String table) {
        String result = table;

        try {
            if (table.length() >= 10) {
                if (!table.substring(0, 10).equalsIgnoreCase("app_audit_")) {
                    table = "app_audit_" + table;
                }
                if (!table.contains("app_audit_")) {
                    table = "app_audit_" + table;
                }
            }
            else {
                table = "app_audit_" + table;
            }

            result = table;
        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        }

        return result;
    }

    private String formatTableName(String table) {
        String result = table;

        try {
            if (table.length() >= 10) {
                if (!table.substring(0, 10).equalsIgnoreCase("app_audit_")) {
                    if (!table.substring(0, 7).equalsIgnoreCase("app_fd_")) {
                        table = "app_fd_" + table;
                    }
                    if (!table.contains("app_fd_")) {
                        table = "app_fd_" + table;
                    }
                }
            }
            else if (table.length() >= 7) {
                if (!table.substring(0, 7).equalsIgnoreCase("app_fd_")) {
                    table = "app_fd_" + table;
                }
                if (!table.contains("app_fd_")) {
                    table = "app_fd_" + table;
                }
            }
            else {
                table = "app_fd_" + table;
            }

            result = table;
        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        }

        return result;
    }

    private String formatColumnName(String column) {
        String result = column;

        try {
            if (!column.equalsIgnoreCase("id") && !column.equalsIgnoreCase("dateCreated") && !column.equalsIgnoreCase("dateModified")) {
                if (column.length() >= 2) {
                    if (!column.substring(0, 2).equalsIgnoreCase("c_")) {
                        column = "c_" + column;
                    }
                    if (!column.contains("c_")) {
                        column = "c_" + column;
                    }
                }
                else {
                    column = "c_" + column;
                }
            }

            result = column;
        } catch(Exception e) {
            LogUtil.error(getClassName(), e, e.getMessage());
        }

        return result;
    }

    private String getClassName() {
        return getClass().getName();
    }

}
