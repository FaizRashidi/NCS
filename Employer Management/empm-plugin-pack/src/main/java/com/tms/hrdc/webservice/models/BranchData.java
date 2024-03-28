/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.webservice.models;

import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.HttpUtil;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.joget.commons.util.LogUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author kahying
 */
public class BranchData {
    
    HttpServletRequest request;
    DBHandler db;
    HttpUtil http;
    
    public BranchData(DBHandler d, HttpServletRequest req) throws SQLException {
        db = d;
        request = req;      
    }
    public JSONObject updateBranchDetails() throws JSONException, IOException, ParseException{
        JSONObject json = new JSONObject();

        JSONObject reqBody = HttpUtil.getRequestBody(request);
        String branchId = reqBody.optString("branchId", "0");
        String name = reqBody.optString("name", "");
        String address = reqBody.optString("address", "");
        String postcode = reqBody.optString("postcode", "");
        String country = reqBody.optString("country", "");
        String state = reqBody.optString("state", "");
        String city = reqBody.optString("city", "");
        String country_code = reqBody.optString("country_code", "");
        String empl_amount = reqBody.optString("empl_amount", "");
        String branch_status = reqBody.optString("branch_status", "");
        String tel_no = reqBody.optString("tel_no", "");
        String email_general = reqBody.optString("email_general", "");
        String email_alt = reqBody.optString("email_alt", "");
        
        if (branchId.equals("0")){
            json.put("Status", "NOTHING TO UPDATE!");
            return json;

        }
        else{
           
            HashMap hm = new HashMap();
            hm.put("name", name);
            hm.put("address", address);
            hm.put("postcode", postcode);
            hm.put("country", country);
            hm.put("state", state);
            hm.put("city", city);
            hm.put("country_code", country_code);
            hm.put("empl_amount", empl_amount);
            hm.put("branch_status", branch_status);
            hm.put("tel_no", tel_no);
            hm.put("email_general", email_general);
            hm.put("email_alt", email_alt);
            int upd = db.update("UPDATE app_fd_empm_branch SET c_name = ?, "
                    + "c_address = ?, c_postcode = ?, c_country = ?, c_state = ?, c_city = ?, "
                    + "c_country_code = ?, c_empl_amount = ?, c_branch_status = ?, c_tel_no = ?,"
                    + "c_email_general = ?, c_email_alt = ?"
                    + " WHERE id = ?",
            new String[]{name, address, postcode, country, state, city, country_code, empl_amount, branch_status,
                         tel_no, email_general, email_alt },
            new String[]{branchId});
  
            json.put("Status", "Branch Updated");
        }

        return json;
    }
    
    
}
