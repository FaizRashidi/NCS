/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import static com.tms.hrdc.defaultPluginTool.EmailTemplateTool.getEmailTemplate;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.KeywordDictionary;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;

/**
 *
 * @author Raja Sulaiman <sulaiman.razali@tmsasia.com>
 */
public class SchedulerNotification extends DefaultApplicationPlugin {
    
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
        return "Scheduler for Notification";
    }
    
    @Override
    public String getLabel() {
        return "HRDC - EMPM - Scheduler For Task Notification";
    }
    
    @Override
    public String getClassName() {
        return this.getClass().toString();
    }
    
    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/email_template_tool.json", null, true, null);
    }
    
    @Override
    public Object execute(Map props) {
        
        DBHandler db = new DBHandler();
        
        String mailId = "";
        String subject = "";
        String message = "";
        String regId = "";
        String originProcessId = "";
        String receiver = "";
        String toEmail = "";
        String query = "";
        String activityId = "";
        
        String mail_template = getPropertyString("mail_template");
        
        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
            
            if (mail_template.equalsIgnoreCase("ET025")) {
                activityId = "verification";
            }
            else if (mail_template.equalsIgnoreCase("ET019")) {
                activityId = "approval";
            }
            else if (mail_template.equalsIgnoreCase("ET011")){
                activityId = "query";
            }
            
            query = "SELECT shk.*, w.originProcessId, r.id AS regId "
                    + "FROM SHKAssignmentsTable shk " 
                    + "INNER JOIN wf_process_link w ON w.processId = shk.ActivityProcessId " 
                    + "INNER JOIN app_fd_empm_regAppl a ON a.id = w.originProcessId " 
                    + "INNER JOIN app_fd_empm_reg r ON r.id = a.c_empl_fk "
                    + "WHERE ActivityId LIKE '%empm%' AND ActivityId LIKE '%"+ activityId +"%' ";
            
            ArrayList<HashMap<String, String>> data = db.select(query);
            
            KeywordDictionary kwd = new KeywordDictionary(db);
            
            
            HashMap templHm;
            if (mail_template.equalsIgnoreCase("ET011")){
                templHm = getEmailTemplate(db, "ET011");
            }
            else{
                templHm = getEmailTemplate(db, mail_template);
            }

            for(HashMap hm : data) {
                originProcessId = hm.get("originProcessId").toString();
                LogUtil.info("OriginProcessId", originProcessId);
                regId = hm.get("regId").toString();
                receiver = hm.get("ResourceId").toString();
                kwd.setRecordId(regId);
                subject = kwd.buildContent(db, templHm.get("c_template_subject").toString(), regId, "");
                message = kwd.buildContent(db, templHm.get("c_template_content").toString(), regId, "");
                message = "<span style='font-family:Segoe UI,Roboto,Helvetica Neue,sans-serif'>"+message+"</span>";
                
                toEmail = getEmail(db, receiver);
 
                if(toEmail != null && !toEmail.equals("")) {
                    CommonUtils.sendEmail(toEmail, "", subject, message, "", null);
                } else {
                    LogUtil.info("Email empty", "dir_user email empty");
                } 
            }              
        } catch(Exception e) {
            Logger.getLogger(this.getClassName()).log(Level.SEVERE, null, e);
        } finally {
            db.closeConnection();
        }
        return null;
    }
    
    private String getEmail(DBHandler db, String id) {
        String email = "";
        String query = "SELECT * "
                + "FROM dir_user "
                + "WHERE id = ? ";
        
        HashMap hm = db.selectOneRecord(query, new String[]{id});
        if(hm != null) {
            email = (String) hm.get("email").toString() != null ? (String) hm.get("email").toString() : "";
        }
        
        
        return email;

    }

}
