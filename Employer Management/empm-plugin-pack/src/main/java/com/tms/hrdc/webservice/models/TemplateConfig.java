/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.webservice.models;

import com.tms.hrdc.util.DBHandler;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class TemplateConfig {
    
    HttpServletRequest request;
    DBHandler db;
    
    public TemplateConfig(DBHandler d, HttpServletRequest req) throws SQLException {
        db = d;
        request = req;        
    }
    
    public JSONObject getEmplEmailTemplate() throws JSONException{
        
        String query = "SELECT id as 'value', \n" +
                        "CONCAT('(',id,') ',c_moduleType, ' - ', \n" +
                        "        (SELECT c_email_type FROM app_fd_empm_email_type_stp s \n" +
                        "         WHERE s.id = e.c_emailType limit 1) \n" +
                        "        ) as label \n" +
                        "FROM app_fd_empm_email_stp e order by c_moduleType asc";
        
        ArrayList<HashMap<String,String>> result = db.select(query);
        
        JSONArray shell = new JSONArray();
        JSONObject data = new JSONObject();
        
        for(HashMap hm:result){
            data = new JSONObject(hm);
            shell.put(data);
        }
        return new JSONObject().put("special", shell);
    }
    
    public JSONObject getKeywords() throws JSONException{
        
        String group = request.getParameter("group")==null?"":request.getParameter("group");
        
        String query = "SELECT c_columnID as pholder, c_columnName as descr"
                + " FROM app_fd_empm_keywords WHERE c_group_as = ?";
        
        ArrayList<HashMap<String,String>> result = db.select(query, new String[]{group});
        
        JSONArray shell = new JSONArray();
        JSONObject data = new JSONObject();
        
        for(HashMap hm:result){
            data = new JSONObject(hm);
            shell.put(data);
        }
        return new JSONObject().put("special", shell);
    }

    public JSONObject getCountryCode() throws JSONException {       
        
        String countryId = request.getParameter("countryId")==null?"":
                request.getParameter("countryId");
        
        String query = "SELECT distinct cc.c_iso_code, c_iso_code\n" +
                        "FROM app_fd_stp_country cc   \n" +
                        "WHERE id = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{countryId});
        
        JSONArray shell = new JSONArray();
        JSONObject data = hm==null?new JSONObject():new JSONObject(hm);
        
        shell.put(data);
        return new JSONObject().put("special", shell);
    }
}
