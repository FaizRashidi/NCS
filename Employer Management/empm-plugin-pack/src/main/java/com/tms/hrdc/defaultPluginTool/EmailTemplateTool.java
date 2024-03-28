/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.KeywordDictionary;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.joget.apps.app.dao.PluginDefaultPropertiesDao;
import org.joget.apps.app.lib.EmailTool;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.PluginDefaultProperties;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PluginThread;
import org.joget.commons.util.SetupManager;
import org.joget.commons.util.StringUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.property.service.PropertyUtil;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;

/**
 *
 * @author faizr
 */
public class EmailTemplateTool extends DefaultApplicationPlugin{

    @Override
    public String getName() {
        return this.getClass().toString();
    }

    @Override
    public String getVersion() {
        return "2.0.0";
    }

    @Override
    public String getDescription() {
        return "Used in EMPM process as a mapper for the email tool and its respective setup";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Email-Template Mapper Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/email_template_tool.json", null, true, null);
    }
    
    private void msg(String msg){
        LogUtil.info(this.getClass().toString(), msg);
    }
    
    @Override
    public Object execute(Map props) {
                      
        WorkflowAssignment wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");     
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        
        PluginManager pm =  (PluginManager)props.get("pluginManager");
        WorkflowManager wm = (WorkflowManager) pm.getBean("workflowManager");
        String parsed_template_id = wm.getProcessVariable(wfAssignment.getProcessId(), "mail_id");
        
        Thread checkingThread = new PluginThread(new Runnable(){
            @Override
            public void run() {
                
                try
                {
                    Thread.sleep(900);
                }
                catch(InterruptedException ex)
                {
                    Thread.currentThread().interrupt();
                    msg("Custom Email Thread "+ex.getMessage());
                }
                
                String id = appService.getOriginProcessId(wfAssignment.getProcessId());
                String from = getPropertyString("from");        
                String to = getPropertyString("to");
                String to_pt_id = getPropertyString("to_pt_id");
                String cc = getPropertyString("cc");
                String cc_pt_id = getPropertyString("cc_pt_id");
                String bcc = getPropertyString("bcc");
                String bcc_pt_id = getPropertyString("bcc_pt_id");        

                String non_template_setup = getPropertyString("non_template_setup");
                String mail_template = getPropertyString("mail_template");
                
                msg("Non Template Setup? "+non_template_setup+", Mail ID -> "+parsed_template_id+", Template Type -> "+mail_template);

                String receiver = "";
                String cc_receiver = "";

                AppDefinition appDef = (AppDefinition) props.get("appDef");
                
                if ((to_pt_id != null && to_pt_id.trim().length() != 0) || (to != null && to.trim().length() != 0)) {
                    Collection<String> tss = AppUtil.getEmailList(to_pt_id, to, wfAssignment, appDef);
                    for (String address : tss) {
                        receiver += StringUtil.encodeEmail(address)+",";  
                    }
                } else {
                    msg("no email specified");
                }

                if(receiver.isEmpty()){
                    receiver = to;
                }

                //for cc        
                if ((cc_pt_id != null && cc_pt_id.trim().length() != 0) || (cc != null && cc.trim().length() != 0)) {
                    Collection<String> tss = AppUtil.getEmailList(cc_pt_id, cc, wfAssignment, appDef);
                    for (String address : tss) {
                        cc_receiver += StringUtil.encodeEmail(address)+",";  
                    }
                } else {
//                    LogUtil.info("Custom Email tool","no cc email specified");
                }

                if(cc_receiver.isEmpty()){
                    cc_receiver = to_pt_id;
                }
                
                DBHandler db = new DBHandler();  
                try{
                    
                    Object attachments[] = null;
                    
                    db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
                    String emp_id = getEmpID(db, props, id);

                    String subject = "";
                    String message = "";
                    String mailId = "";
                    String attachmentName = "";

                    HashMap templHm = new HashMap();
                    HashMap mail_preview_hm = new HashMap();
                    
                    EmpmObj eo = new EmpmObj(db, EmpmObj.BY_ID, emp_id);

                    KeywordDictionary kwd = new KeywordDictionary(db);
                    kwd.setRecordId(id);
                    //means use from setup template
                    if(non_template_setup.equals("true")){
//                        templHm = getLatestUsrTemplate(db, emp_id, mail_template);
                        templHm = getLatestUsrTemplate(db, emp_id, parsed_template_id,  mail_template);
                        
                        mailId = templHm.get("id")==null?"":templHm.get("id").toString();
                        subject = templHm.get("c_template_subject").toString();
                        message = templHm.get("c_template_content").toString();
                        subject = kwd.buildContent(db, templHm.get("c_template_subject").toString(), emp_id, mailId);
                        message = kwd.buildContent(db, message + Constants.EMAIL_TRACK, emp_id, mailId);                
                        message = "<span style='font-family:Segoe UI,Roboto,Helvetica Neue,sans-serif'>"+message+"</span>";            
                        
                        attachmentName = templHm.getOrDefault("c_mail_attachment", "").toString();
                        
                        if(!attachmentName.isEmpty()){
                            attachments = setAttachmentFile(mailId, attachmentName);
                        }
                        
                        mail_preview_hm.put("mail_type", mail_template);
                        mail_preview_hm.put("mail_fk", emp_id);
                        mail_preview_hm.put("mail_to", receiver);
                        mail_preview_hm.put("comp_name", eo.getCompName());

                        mailId = CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                                    Constants.FORM_ID.MAIL_PREVIEW, 
                                                    mailId, mail_preview_hm);
                    }else{                          
                        templHm = getEmailTemplate(db, mail_template);
                        
                        if(templHm == null){
                            throw new Exception("No template data set");  
                        }

                        String app_type = props.get("app_type")==null?"":props.get("app_type").toString();
                        
                        if (app_type.equals("cksp_complaint")){
                            receiver = eo.getPrimaryEmail();
                        }

                        mail_preview_hm.put("mail_type", mail_template);
                        mail_preview_hm.put("mail_fk", emp_id);
                        mail_preview_hm.put("mail_to", receiver);
                        mail_preview_hm.put("comp_name", eo.getCompName());

                        mailId = CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                                    Constants.FORM_ID.MAIL_PREVIEW, 
                                                    "", mail_preview_hm);
                        
                        if(mail_template.equals("ET017")){
                           attachments = getBorang1Jasper(id, mailId, eo);
                           
                           if(attachments.length>0){
                               HashMap hm = (HashMap) attachments[0];
                               attachmentName = hm.getOrDefault("fileName", "ERROR_FILENAME").toString();
                           }
                        }

                        subject = kwd.buildContent(db, templHm.get("c_template_subject").toString(), emp_id, mailId);
                        message = kwd.buildContent(db, templHm.get("c_template_content").toString()+ Constants.EMAIL_TRACK, emp_id, mailId);
                        message = "<span style='font-family:Segoe UI,Roboto,Helvetica Neue,sans-serif'>"+message+"</span>";            

                        mail_preview_hm.put("mail_subject", subject);
                        mail_preview_hm.put("mail_content", message);    
                        
                        mailId = CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                                    Constants.FORM_ID.MAIL_PREVIEW, 
                                                    mailId, mail_preview_hm);
                    }

                    if(!attachmentName.isEmpty()){
                        db.update("UPDATE "+Constants.TABLE.EMAIL+" "
                                + "SET c_mail_subject = ?, c_mail_content = ?,"
                                + (attachmentName.isEmpty()?"":" c_mail_attachment =  '"+attachmentName+"', ")
                                + "c_mail_to = ? WHERE id = ?",
                                new String[]{subject, message, receiver},
                                new String[]{mailId});
                    }
                    
                    msg( "SENDING EMAIL TO: "+receiver
                            +", CC: "+cc_receiver
                            +", SUBJECT: "+subject
                            +", ATTACHMENTS: "+Boolean.toString(attachments!=null)+" - "+attachmentName);

                    CommonUtils.sendEmail(receiver, cc_receiver, subject, message, null, attachments);

                  }catch(Exception e){
                      e.printStackTrace();
                  }finally{
                      db.closeConnection();
                  }
            }

        });        
        checkingThread.setDaemon(true);
        checkingThread.start();
        return null;
    }

    public static HashMap getEmailTemplate(DBHandler db, String email_type) {
        String query = "SELECT \n" +
                        "	mt.id, CONCAT(mt.c_moduleType, ' - ',mt.c_emailType) ,\n" +
                        "	s.c_template_subject, s.c_template_content\n" +
                        "FROM app_fd_empm_email_stp mt\n" +
                        "INNER JOIN app_fd_empm_template_stp s ON mt.id = s.c_email_fk " + 
                        "AND mt.id = ? limit 1 ";
        return db.selectOneRecord(query, new String[]{email_type});        
    }
    
    public static HashMap getEmailTemplateNEW(DBHandler db, String email_type) {
        String query = "SELECT s.c_template_subject, s.c_template_content " +
                        "FROM app_fd_empm_template_stp s WHERE s.id = ? ";
        return db.selectOneRecord(query, new String[]{email_type});        
    }

    private HashMap getLatestUsrTemplate(DBHandler db, String emp_id, String parsed_template_id, String defTemplId) {
        
        String sql = "SELECT id, c_mail_subject as c_template_subject, "
                + "c_mail_content as c_template_content, c_mail_attachment "
                + "FROM app_fd_empm_usr_mail m WHERE c_mail_fk = ? "
                + "AND c_mail_type = ? "
//                + "AND id = ? "
                + "ORDER BY dateCreated DESC LIMIT 1";
        HashMap hm = db.selectOneRecord(sql, new String[]{emp_id, defTemplId});
        
        if(hm==null){
            LogUtil.info("get email template "+defTemplId+ ": ","failed");
            String sql1 = "SELECT id, c_mail_subject as c_template_subject, "
                + "c_mail_content as c_template_content, c_mail_attachment "
                + "FROM app_fd_empm_usr_mail m WHERE c_mail_fk = ? "
                + "ORDER BY dateCreated DESC LIMIT 1";
            HashMap hm1 = db.selectOneRecord(sql1, new String[]{emp_id});
            if (!(hm1 == null)){
                LogUtil.info("can get by ", "emp_id");
            }
            else{
                LogUtil.info("can't get by ", "emp_id");

            }
            
            String sql2 = "SELECT id, c_mail_subject as c_template_subject, "
            + "c_mail_content as c_template_content, c_mail_attachment "
            + "FROM app_fd_empm_usr_mail m WHERE c_mail_type = ?"
            + "ORDER BY dateCreated DESC LIMIT 1";
            HashMap hm2 = db.selectOneRecord(sql2, new String[]{defTemplId});
            if (!(hm2 == null)){
                LogUtil.info("can get by ", "defTemplId");
            }
            else{
                LogUtil.info("can't get by ", "defTemplId");

            }
            
            hm = getEmailTemplate(db, defTemplId);
        }
        
        return hm;
    }

    private String getEmpID(DBHandler db, Map props, String id) {
        String app_type = props.get("app_type")==null?"":props.get("app_type").toString();
        switch(app_type){
            case "req_change":
                id = CommonUtils.getEmpId_reqChange(db, id);
            break;
            case "empl_dereg":
                id = CommonUtils.getEmpId_empDereg(db, id);
            break;
            case "empl_dereg_f5":
                id = CommonUtils.getEmpId_empDeregF5(db, id);
            break;
            case "empl_dereg_wd":
                String temp_id = CommonUtils.getEmplId_DeregWD(db, id);
                if(temp_id.isEmpty()){
                    id = CommonUtils.getEmplId_DeregWD_F5(db, id);
                }else{
                    id=temp_id;
                }
            break;
            case "pe_write_off":
                id = CommonUtils.getEmplId_WriteOff(db, id);
            break;
            case "cksp_complaint":
                id = CommonUtils.getEmplId_CKSPComplaint(db, id);
            break;
            default:
                id = CommonUtils.getEmpId_empReg(db, id);
        }

        return id;
    }
    
    private Object[] getBorang1Jasper(String regId, String mailId, EmpmObj emp) throws IOException {
        String doc_name = "Borang_1_"+CommonUtils.get_DT_CurrentDateTime("yyyyMMdd") + "_" + emp.getMycoid() + ".pdf";  
                
        String receiptPdfURL = Constants.EMPM_JASPER_PDF_PATH// JASPER_URL
                + "&menuId=" + Constants.URL.JASPER_FORM1_URL
                + "&id=" + regId;                

        String fileResult = CommonUtils.generateAndSavePDF(receiptPdfURL,doc_name, mailId, "empm_usr_mail");

        HashMap map = new HashMap();
        map.put("fileName", doc_name);
        map.put("type", "system");
        map.put("path", fileResult);

        Object[] fileObject = {map};
        
        return fileObject;
    }
    
    private Object[] setAttachmentFile(String mailId, String fileName) throws IOException {
        
        String file_dir = SetupManager.getBaseDirectory() + "app_formuploads" + File.separator 
                            + "empm_usr_mail" + File.separator + mailId + File.separator 
                            + fileName;

        HashMap map = new HashMap();
        map.put("fileName", fileName);
        map.put("type", "system");
        map.put("path", file_dir);

        Object[] fileObject = {map};
        
        return fileObject;
    }

    
}
