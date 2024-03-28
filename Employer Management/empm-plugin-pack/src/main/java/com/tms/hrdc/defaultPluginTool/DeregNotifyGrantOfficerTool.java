/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.dao.APIManager;
import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.HttpUtil;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.service.FileUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PluginThread;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class DeregNotifyGrantOfficerTool extends DefaultApplicationPlugin{

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
        return "To notify Grant Officers on Deregistration submission";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Grant Dereg. Notification Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return  "";
    }
    
    PluginManager pm = null;
    WorkflowManager wm = null;
    WorkflowAssignment wfAssignment = null;

    @Override
    public Object execute(Map props) {
        
        wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");     
        pm =  (PluginManager)props.get("pluginManager");
        wm = (WorkflowManager) pm.getBean("workflowManager");
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        
        String id = appService.getOriginProcessId(wfAssignment.getProcessId());        
        
        DBHandler db = new DBHandler();
        
        try{
            db.openConnection();
            String empId = CommonUtils.getEmpId_empDereg(db, id);            
            EmpmObj emp = new EmpmObj(db, EmpmObj.BY_ID, empId);
            
            id = emp.getHrdcNo();
        }catch(Exception e){
            
        }finally{
            db.closeConnection();
        }
        
        final String hrdcNo = id;
        final APIManager apiMgr = new APIManager(APIManager.APIType.GRANT);
        final String url = Constants.URL.GRANT_DEREG_NOTI;
        
        Thread checkingThread = new PluginThread(new Runnable() {
            @Override
            public void run() {
                HttpUtil http = new HttpUtil();                
                
                http.setAPIMethod("POST");
                JSONObject body = new JSONObject();
                try {
                    body.put("psmb_no", hrdcNo);
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                
                HashMap header = new HashMap();
                header.put("api_id", apiMgr.getApi_id());
                header.put("api_key", apiMgr.getApi_key());
                
                http.setHeader(header);                
                http.setBody(body);
                
                try {
                    http.sendPostRequest(url);
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (NoSuchAlgorithmException ex) {
                    ex.printStackTrace();
                } catch (KeyManagementException ex) {
                    ex.printStackTrace();
                } catch (KeyStoreException ex) {
                    ex.printStackTrace();
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
        });

        checkingThread.setDaemon(true);
        checkingThread.start();
        
        return null;
    }
}
