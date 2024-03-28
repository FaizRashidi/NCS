/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.joget.apps.app.dao.PluginDefaultPropertiesDao;
import org.joget.apps.app.lib.EmailTool;
import org.joget.apps.app.model.PluginDefaultProperties;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.property.service.PropertyUtil;

/**
 *
 * @author faizr
 */
public class PotEmpEmailBinder extends WorkflowFormBinder{
    
    @Override
    public String getName() {
        return "EmailBinder";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "To parse and load email data for previw";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Pot. Emp. Mail Binder";
    }
    
    String unsuccessfulReason = "</ul>";
    int unsuccessfulRowsUploaded = 0; 
    int successfulRowsUploaded = 0;
    int totalRows = 0;
        
    public void msg(String msg){
        LogUtil.info(getName(), msg);
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
    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {
        super.store(element, rows, formData);
        DBHandler db = new DBHandler();
        
        String formId = super.getFormId();
 
        try {
            db.openConnection();        
          
            FormRow row = rows.get(0);
            String id = row.getId();
            String query = "SELECT b.c_empl_email_pri, c_mail_subject, c_mail_content, c_mail_to, a.id, b.id as empId "
                    + "FROM app_fd_empm_usr_mail a "
                    + "INNER JOIN "+Constants.TABLE.EMPREG+" b "
                    + "ON a.c_mail_fk = b.id "
                    + "WHERE a.c_list_fk='"+id+"'";

            ArrayList<HashMap<String, String>> email_message_list = db.select(query);

            for (HashMap<String, String> email_message: email_message_list){

                String mailId = email_message.get("id").toString();
                String empId = email_message.get("empId").toString();
                String emp_email = email_message.get("c_mail_to").toString();
                String emp_subject = email_message.get("c_mail_subject").toString();
                String emp_message = email_message.get("c_mail_content").toString();

                sendEmail("hrdc@no-reply.com",emp_email,"","",emp_subject,emp_message);

                query = "UPDATE app_fd_empm_usr_mail SET c_is_seen = ? WHERE id = ?";
                db.update(query, new String[]{Constants.STATUS.EMAIL.SENT}, new String[]{mailId});

                query = "UPDATE "+Constants.TABLE.EMPREG+" SET c_last_move = ? WHERE id = ?";
                db.update(query, new String[]{Constants.LAST_MOVEMENT.LETTER_SENT}, new String[]{empId});
            }
        
        } catch (SQLException ex) {
            ex.printStackTrace();
            db.closeConnection();
        } finally{
            db.closeConnection();
        }
        
        return rows;
        
    }
    
     @Override
    public FormRowSet load(Element element, String id, FormData formData) {
        
        LogUtil.info("PotEmpMailPrev", "loading "+id);
//        check whether request parameter id is empty 
        String currentUserUsername = AppUtil.processHashVariable("#currentUser.username#", null, null, null);
        WorkflowFormBinder binder = new WorkflowFormBinder();
        FormRowSet rows = binder.load(element, id, formData);
        
        return rows;
    }
    
    private void sendEmail(String from, String receiver,String cc, String bcc, String subject,String message){
        
        EmailTool et = new EmailTool();
        Map properties = getEmailDefaultSettings();
        
        properties.put("toSpecific", receiver);
        properties.put("cc", cc);
        properties.put("from", from);
        properties.put("subject", subject);
        properties.put("message", message);
        properties.put("isHtml", "true");
        et.execute(properties);
    }

    public static Map getEmailDefaultSettings() {
        PluginDefaultPropertiesDao dao = (PluginDefaultPropertiesDao) AppUtil.getApplicationContext().getBean("pluginDefaultPropertiesDao");
        PluginDefaultProperties pluginDefaultProperties = (PluginDefaultProperties) dao.loadById("org.joget.apps.app.lib.EmailTool", AppUtil.getCurrentAppDefinition());
        Map properties = PropertyUtil.getPropertiesValueFromJson(pluginDefaultProperties.getPluginProperties());
    
        return properties;
    }
}
