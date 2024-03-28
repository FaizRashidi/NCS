package com.tms.hrdc.datalistAction;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.KeywordDictionary;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import javax.swing.text.AbstractDocument.Content;

import org.joget.apps.app.dao.PluginDefaultPropertiesDao;
import org.joget.apps.app.lib.EmailTool;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.PluginDefaultProperties;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.property.service.PropertyUtil;

public class SentEmailFromForm extends DataListActionDefault {

    public String getName(){
        return this.getClass().toString();
    }

    public String getVersion(){
        return "1.0";
    }
    
    public String getDescription(){
        return "To send Email to list of Potential Employers";
    }

    public String getLinkLabel() {
        String label = getPropertyString("label");
        if (label == null || label.isEmpty()) {
            label = "Sent Email";
        }
        return label;
    }

    @Override
    public String getHref() {
        return getPropertyString("href"); 
    }

    @Override
    public String getTarget() {
        return "post";
    }

    @Override
    public String getHrefParam() {
        return getPropertyString("hrefParam"); 
    }

    @Override
    public String getHrefColumn() {
        return getPropertyString("hrefColumn");
    }

    public String getConfirmation(){
        String confirm =getPropertyString("confrimation");
        if (confirm == null || confirm.isEmpty()){
            confirm= "Send Email?";
        }
        return confirm;
    }

    public String getLabel() {
        return "HRDC - EMPM - Send Email to PE Button";
    }

    public String getClassName() {
        return this.getClass().getName();
    }

    public String getPropertyOptions() {
        String json = "[{\n"
                + "    title : 'Application & Sent Email',\n"
                + "    properties : [{\n"
                + "        label : 'Label',\n"
                + "        name : 'label',\n"
                + "        type : 'textfield',\n"
                + "        description : 'Sent Email to User',\n"
                + "        value : 'Sent Email & Record'\n"
                + "    },{\n" 
                + "        name: 'template_id', " 
                + "        label: 'Template ID', " 
                + "        type: 'textfield', " 
                + "        required : 'true', " 
                + "        description : 'ID of Email Template',\n"
                + "    }]\n"
                + "}]";
        return json;
    }    

    private HashMap getTemplateData(DBHandler db, String email_type) {
        String query = "SELECT " +
                        "	mt.id, CONCAT(mt.c_moduleType, ' - ',mt.c_emailType) , " +
                        "	s.c_template_subject, s.c_template_content " +
                        "FROM app_fd_empm_email_stp mt " +
                        "INNER JOIN app_fd_empm_template_stp s ON mt.c_mailTemplate = s.id " + 
                        "AND mt.id = '"+email_type+"'";        
        return db.selectOneRecord(query);        
    }

    @Override
    public DataListActionResult executeAction(DataList dataList, String[] rowKeys) {
        
        String message = "";   
        String email_type = (String) getPropertyString("template_id");
        DBHandler db = new DBHandler();        
        
        int count = 0;
        try{
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
            
            KeywordDictionary kwd = new KeywordDictionary(db);
            String query = "SELECT c_pe_myCoID, c_pe_email, c_pe_compName "
                            + "FROM app_fd_empm_pe_pot_empl where id = ?" ;
            
            HashMap templateHm = getTemplateData(db, email_type);
            
            if(rowKeys.length != 0){

                for(String id:rowKeys){
                    //Setiap rowKeys ni ID of Potential Empl

                    //nk dapatkan email, nama etc:
                    HashMap dataHm = db.selectOneRecord(query, new String[]{id});
                    
                    String name = dataHm.get("c_pe_compName").toString();
                    String email = dataHm.get("c_pe_email").toString();
                    String mycoid = dataHm.get("c_pe_myCoID").toString();
                    
                    String subject = (String) templateHm.get("c_template_subject");
                    String content = (String) templateHm.get("c_template_content");
                    
//                    LogUtil.info("SENDING EMAIL ", "content "+content);
                    //querying..
                    subject = kwd.buildContent(db, subject, id, "");
                    content = kwd.buildContent(db, content, id, "");
                    
//                    LogUtil.info("SENDING EMAIL ", "to "+name+" "+email+" - "+mycoid+" - "+subject+" |||| "+content);
                    sendEmail("ncs.admin@hrdc.com.my", email, "", "", subject, content);
                }
                 
            }else{
                message = "No Data Selected";
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }
        
        message += "Email sent: "+Integer.toString(count);
        
        DataListActionResult result = new DataListActionResult();
        result.setType(DataListActionResult.TYPE_REDIRECT);
        result.setUrl("REFERER");
            
        if(!message.isEmpty()){
            result.setMessage("Email sent: "+Integer.toString(count));
        }
            
        return result;     
    }

    private String getUserMycoid(DBHandler db, String id) {
        String query = "SELECT c_pe_myCoID FROM app_fd_empm_pe_pot_empl WHERE id = ?"; 
        String[] cond = {id};
        HashMap hm = db.selectOneRecord(query, cond);
         
        if(hm!=null){
            return hm.get("c_pe_myCoID").toString();
        }
        
        return "";
    }



    private void sendEmail(String from, String receiver,String cc, String bcc, String subject,String message){
        
        EmailTool et = new EmailTool();
        Map properties = getEmailDefaultSettings();
        
        properties.put("toSpecific", receiver);
        properties.put("cc", cc);
        properties.put("from", from);
        properties.put("subject", subject);
        properties.put("message", message);
        et.execute(properties);
    }

    public static Map getEmailDefaultSettings() {
        PluginDefaultPropertiesDao dao = (PluginDefaultPropertiesDao) AppUtil.getApplicationContext().getBean("pluginDefaultPropertiesDao");
        PluginDefaultProperties pluginDefaultProperties = (PluginDefaultProperties) dao.loadById("org.joget.apps.app.lib.EmailTool", AppUtil.getCurrentAppDefinition());
        Map properties = PropertyUtil.getPropertiesValueFromJson(pluginDefaultProperties.getPluginProperties());
    
        return properties;
    }

}
