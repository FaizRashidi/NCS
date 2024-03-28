/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.webservice.models;

import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.HttpUtil;
import java.sql.SQLException;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.joget.commons.util.LogUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class ReregButtons {
    
    HttpServletRequest request;
    DBHandler db;
    HttpUtil http;
    
    public ReregButtons(DBHandler d, HttpServletRequest req) throws SQLException {
        db = d;
        request = req;      
    }
    
    /*
    API
    */
    public JSONObject setReregButton() throws JSONException{
        String recId = request.getParameter("recId");        
        String usrId = request.getParameter("usrId");        
        
        String aTag = "";
        
        HashMap hm = db.selectOneRecord(
                "SELECT r.id as empId, ra.id, ra.c_flow_status, r.c_req_email FROM "+Constants.TABLE.EMPREG_APPL+" ra "
                        + " INNER JOIN "+Constants.TABLE.EMPREG+" r ON r.id = ra.c_empl_fk "
                        + "WHERE ra.id = ?",
                new String[]{recId}
        );
        
        if(hm==null){
            return new JSONObject();
        }
        
        String regStatus = hm.get("c_flow_status").toString().toUpperCase();
        String reqEmail = hm.get("c_req_email").toString();
        String empId = hm.get("empId").toString();
        
        if(regStatus.equals("REJECTED") && reqEmail.equals(usrId)){
            return new JSONObject().put("value", "<br /><a class='action_btn' href='REGISTER_FORM1_REJECTED?empl_fk="+empId+"'>Resubmit Form 1 Registration</a>");
        }else{
            return new JSONObject();
        }
    }
    
}
