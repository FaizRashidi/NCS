/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.webservice.models;

import com.tms.hrdc.util.DBHandler;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class EmpSetup {
    HttpServletRequest request;
    DBHandler db;
    
    public EmpSetup(DBHandler d, HttpServletRequest req) throws SQLException {
        db = d;
        request = req;        
    }
    
    public JSONObject getEmplDeregBypassSetup() throws JSONException{
        
        String query = "SELECT  c_dereg_no_check as f4, "
                + "             c_dereg_bhlf_no_check as f4_ob, "
                + "             c_f4a_dereg_no_check as f4a, "
                + "             c_f4a_dereg_bhlf_no_check as f4a_ob, "
                + "             c_f5_dereg_no_check as f5 "
                + "     FROM    app_fd_empm_reg_stp WHERE id = ?";
        
        ArrayList<HashMap<String,String>> result = db.select(query, new String[]{"epm_stp"});
        
        JSONArray shell = new JSONArray();
        JSONObject data = new JSONObject(); 
        
        for(HashMap hm:result){
            data = new JSONObject(hm);
            shell.put(data);
        }
        return new JSONObject().put("special", shell);
    }
    
    public JSONObject getDeregDummy() throws JSONException{
        
        String empId = request.getParameter("empId");
        
        String query = "select * from app_fd_empm_levy_dummy e WHERE c_empId = ? ";
        
        ArrayList<HashMap<String,String>> result = db.select(query, new String[]{empId});
        
        JSONArray shell = new JSONArray();
        JSONObject obj = new JSONObject(); 
        
        for(HashMap data:result){
            obj.put("enable_app_dummy", (data.get("c_enable_app_dummy")==null)?"":data.get("c_enable_app_dummy").toString());
            obj.put("app_refno", data.get("c_app_refno").toString());
            obj.put("status", data.get("c_status").toString());
            obj.put("enable_pay_dummy", (data.get("c_enable_app_dummy")==null)?"":data.get("c_enable_pay_dummy").toString());
            obj.put("month_unpaid", data.get("c_levy_month_unpaid").toString());
            obj.put("amount", data.get("c_levy_amount").toString());
            
            shell.put(obj);
        }
        return new JSONObject().put("special", shell);
    }
}
