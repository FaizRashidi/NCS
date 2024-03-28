/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.util;

import com.tms.hrdc.dao.APIManager;
import static com.tms.hrdc.defaultPluginTool.RegisterEmpAPS.B2C_DOMAIN_TEST;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import org.joget.commons.util.LogUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class B2CUtil {
    
    HttpUtil http;
    DBHandler db;
    APIManager mgr;
    HashMap header;
    
    public B2CUtil(DBHandler db, HttpUtil http, APIManager mgr){
        this.db = db;
        this.mgr = mgr;
        this.http = http;
        
        header = new HashMap();
        header.put("api_id", mgr.getApi_id());
        header.put("api_key", mgr.getApi_key());
    }
    
    public void createB2CUser(JSONObject body) throws JSONException, IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException{
        
        if(body == null){
            LogUtil.info(this.getClass().toString(), "Deleting B2C User Error - Empty JSON Body");
            return;
        }
        
        http.setHeader(header);
        http.setBody(body);
        http.sendPostRequest(Constants.B2C.USERAPI);

        JSONObject resp_data = http.getJSONResponse();
        int statusCode = http.getStatusCode();

        LogUtil.info("DATA SENT TO B2C", 
                "Status Code :"+Integer.toString(statusCode)
                + ", Response: "+resp_data.toString());
    }
    
    public void updateB2CUser(String username, JSONObject body) throws JSONException, IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException{
        
        if(username.isEmpty()){
            LogUtil.info(this.getClass().toString(), "Deleting B2C User Error - Empty username parameter");
            return;
        }
        
        if(username.contains("@")){
            username = username.split("@")[0];
        }
        
        String b2cId = db.selectOneValueFromTable(
                "SELECT id FROM app_fd_stp_b2c_users WHERE c_user_principal_name = ?",
                new String[]{username+"@"+B2C_DOMAIN_TEST}
        );
        
        if(!b2cId.isEmpty()){
            String url = Constants.B2C.USERUPDATE + "?b2cUserId=" + b2cId;

            http = new HttpUtil();
            http.setHeader(header);
            http.setBody(body);
            http.sendPostRequest(url);

            JSONObject resp_data = http.getJSONResponse();
            int statusCode = http.getStatusCode();

            LogUtil.info("DATA DELETE SENT TO B2C", 
                    "Status Code :"+Integer.toString(statusCode)
                    + ", Response: "+resp_data.toString());
        }
    }
    
    public void deleteB2CUser(String username) throws JSONException, IOException, KeyManagementException, KeyStoreException, NoSuchAlgorithmException{
        
        if(username.isEmpty()){
            LogUtil.info(this.getClass().toString(), "Deleting B2C User Error - Empty username parameter");
            return;
        }
        
        if(username.contains("@")){
            username = username.split("@")[0];
        }
        
        String b2cId = db.selectOneValueFromTable(
                "SELECT id FROM app_fd_stp_b2c_users WHERE c_user_principal_name = ?",
                new String[]{username+"@"+B2C_DOMAIN_TEST}
        );

        if(!b2cId.isEmpty()){
            String url = Constants.B2C.USERDELETE + "?b2cUserId=" + b2cId;

            http = new HttpUtil();
            http.setHeader(header);
            http.sendDeleteRequest(url);

            JSONObject resp_data = http.getJSONResponse();
            int statusCode = http.getStatusCode();

            LogUtil.info("DATA DELETE SENT TO B2C", 
                    "Status Code :"+Integer.toString(statusCode)
                    + ", Response: "+resp_data.toString());
        }
        
    }
    
}
