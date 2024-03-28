/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.formGridBinder;

import com.tms.hrdc.util.DBHandler;
import java.util.ArrayList;
import java.util.HashMap;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormLoadMultiRowElementBinder;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 *
 * @author faizr
 */
public class EngagementChangeListBinder extends WorkflowFormBinder implements FormLoadMultiRowElementBinder{

    @Override
    public String getName() {
        return this.getClassName();
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Formatter for uuid of changed values in EMPM Engagement flow";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - PE Eng. Value Changes Multirow Load Binder";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }
    
    @Override
    public FormRowSet load(Element elmnt, String primaryKey, FormData fd) {
        
        FormRowSet rows = new FormRowSet();
        rows.setMultiRow(true);
        
        DBHandler db = new DBHandler();
        
        try{
            db.openConnection();
            ArrayList<HashMap<String, String>> chgeList = getValChges(db, primaryKey);
            
            for(HashMap dataRow:chgeList){
                FormRow row = new FormRow();
                row.put("date", dataRow.get("dateCreated").toString());
                row.put("actionBy", dataRow.get("modifiedByName").toString());
                row.put("status", dataRow.get("c_status").toString());
                row.put("refField", dataRow.get("c_columnName").toString());
                
                String isLocationType = dataRow.get("c_isLocationField")==null?"":dataRow.get("c_isLocationField").toString();
                String isSectorType = dataRow.get("c_isSectorField")==null?"":dataRow.get("c_isSectorField").toString();
                String newValue = dataRow.get("c_curr_value")==null?"":dataRow.get("c_curr_value").toString();
                String currValue = dataRow.get("c_prev_value")==null?"":dataRow.get("c_prev_value").toString();
                
                row.put("currValue", formatVal(db, isLocationType, isSectorType, currValue));
                row.put("newValue", formatVal(db, isLocationType, isSectorType, newValue));
                rows.add(row);
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }
        
        return rows;
    }

    private ArrayList<HashMap<String, String>> getValChges(DBHandler db, String primaryKey) {
        String sql = "select\n" +
                    "s.dateCreated, " +
                    "t.modifiedByName,\n" +
                    "s.c_status,\n" +
                    "s.c_field_name,\n" +
                    "k.c_columnName,\n" +
                    "s.c_prev_value,\n" +
                    "s.c_curr_value,\n" +
                    "k.c_isLocationField,\n" +
                    "k.c_isSectorField\n" +
                    "from app_fd_empm_audit_sub s\n" +
                    "inner join app_fd_empm_pe_egmnt e on e.id = s.c_fk\n" +
                    "inner join app_fd_empm_reg_temp t on t.id = e.c_temp_emp_fk\n" +
                    "inner join app_fd_empm_keywords k on k.c_columnID = s.c_field_name "+
                    "WHERE e.id = ? "+
                    "ORDER BY s.dateCreated desc";
        
        ArrayList<HashMap<String, String>> chgeList = db.select(sql, new String[]{primaryKey});
        
        return chgeList;
    }
    
    public String formatVal(DBHandler db, String isLocType, String isSectType, String value){
        if(isSectType.equals("true")){
            return getSectLabel(db, value);
        }
        
        if(isLocType.equals("true")){
            return getLocLabel(db, value);
        }
        
        return value;
    }
    
    private String getSectLabel(DBHandler db, String id) {
        String query = "select data.id, data.i_value from (\n" +
                        "select id, concat(s.c_descr, '(',s.c_sub_sector_code,')') as i_value from app_fd_stp_sub_sector s\n" +
                        "UNION\n" +
                        "select id, concat(c_descr, '(',c_main_sector_code,')') from app_fd_stp_main_sector \n" +
                        "UNION\n" +
                        "select id, concat(a.c_descr, '(',a.c_div_code,')') from app_fd_stp_industry_div a\n" +
                        "UNION\n" +
                        "select id, concat(s.c_industry_sector, ' (', s.c_industry_sector_code, ')') from app_fd_stp_industry_sector s\n" +
                        ") data WHERE data.id = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{id});
        
        if(hm==null){
            return id;
        }
        
        return hm.get("i_value")==null?"":hm.get("i_value").toString();
    }

    private String getLocLabel(DBHandler db, String id) {
        String query = "select data.id, data.i_value from (\n" +
                    "select id, c_location as i_value from app_fd_stp_location\n" +
                    "union\n" +
                    "select id, c_city from app_fd_stp_city\n" +
                    "union\n" +
                    "select id, c_state from app_fd_stp_state\n" +
                    "union\n" +
                    "select id, c_country from app_fd_stp_country\n" +
                    ") data WHERE data.id = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{id});
        
        if(hm==null){
            return id;
        }
        
        return hm.get("i_value")==null?"":hm.get("i_value").toString();
    }
}
