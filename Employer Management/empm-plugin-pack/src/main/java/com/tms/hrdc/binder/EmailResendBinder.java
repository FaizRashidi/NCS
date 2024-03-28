/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.dao.CurrentUser;
import com.tms.hrdc.defaultPluginTool.EmailTemplateTool;
import static com.tms.hrdc.defaultPluginTool.EmailTemplateTool.getEmailTemplate;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.KeywordDictionary;
import java.io.File;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author faizr
 */
public class EmailResendBinder  extends WorkflowFormBinder {

    @Override
    public String getName() {
        return this.getClass().toString();
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Used in Employer Management to Resend Emails";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Email Resend Binder";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    public void msg(String msg) {
        LogUtil.info(getName(), msg);
    }

    // get resend to field 
    //
    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {
        
        rowSet = super.store(element, rowSet, formData);
        FormRow row = rowSet.get(0);

        String id = row.getId();
//        String mail_to =  row.get("mail")==null?"":row.get("value").toString();
        String resend_mail_to =  row.get("resend_mail_to")==null?"":row.get("resend_mail_to").toString();
        String mail_attachment =  row.get("mail_attachment")==null?"":row.get("mail_attachment").toString();
        String mail_content =  row.get("mail_content")==null?"":row.get("mail_content").toString();
        String mail_subject = row.get("mail_subject")==null?"":row.get("mail_subject").toString();        
        
        //resend
        
        String status = "SENT";
        String resend = "Yes";
        CurrentUser cu = new CurrentUser();
        
        String cu_id = cu.getId();
        String cu_name = cu.getFullName();

        DBHandler db = new DBHandler();
        try{
            db.openConnection();
            
            int i = db.update(
                    "UPDATE app_fd_empm_usr_mail SET c_resend = ?, c_resend_mail_to = ? WHERE id = ? ", 
                    new String[]{resend, resend_mail_to}, 
                    new String[]{id}
            );
            
            HashMap hm = db.selectOneRecord(
                    "SELECT * FROM "+Constants.TABLE.EMAIL+" WHERE id =? ",
                    new String[]{id}
            );
            
            String list_fk = hm.get("c_list_fk").toString();
            String letter_status = hm.getOrDefault("c_letter_status","").toString();
//            String mail_status = hm.get("c_mail_status").toString();
            String mail_type = hm.get("c_mail_type").toString();
            String sent_as = hm.get("c_sent_as").toString();
            String batch_id = hm.get("c_batch_id").toString();
            String mail_to = hm.get("c_mail_to").toString();
            String mail_fk = hm.get("c_mail_fk").toString();
            
            
            HashMap newMailHm = new HashMap();
            newMailHm.put("list_fk", list_fk);
            newMailHm.put("letter_status", letter_status);
            newMailHm.put("mail_status", status);
            newMailHm.put("mail_type", mail_type);
            newMailHm.put("sent_as", sent_as);
            newMailHm.put("batch_id", batch_id);
//            newMailHm.put("mail_to", status);
            newMailHm.put("createdBy", cu_id);
            newMailHm.put("createdByName", cu_name);
            newMailHm.put("mail_fk", mail_fk);
            
            newMailHm.put("mail_to", resend_mail_to);
            newMailHm.put("mail_attachment", mail_attachment);
            newMailHm.put("mail_content", mail_content);
            newMailHm.put("mail_subject", mail_subject);
            
            String newId = CommonUtils.saveUpdateForm2("", "mail_list_preview_view", "", newMailHm);
                        
            Object[] attachments = null;
            
            if(!StringUtils.isBlank(mail_attachment)){
                CommonUtils.duplicateDir(id, newId, Constants.TABLE.EMAIL.replace("app_fd_", ""));
                
                String duplDir = Constants.JOGET_BASE_UPL_PATH
                                +File.separator
                                +Constants.TABLE.EMAIL.replace("app_fd_","")
                                +File.separator
                                +newId
                                +File.separator
                                +mail_attachment;
                
                HashMap map = new HashMap();
                map.put("fileName", mail_attachment);
                map.put("type", "system");
                map.put("path", duplDir);

                
                attachments = new Object[1];
                attachments[0] = map;
            }
            
            msg("MAIL FK "+mail_fk+", "+newId);
            
            KeywordDictionary kwd = new KeywordDictionary(db);
            kwd.setRecordId(id);
            
            if(mail_type.equals(Constants.MAIL_TYPE.REG.REG_WELCOME_EMAIL)){
                HashMap empRegHm = db.selectOneRecord(
                        "SELECT * FROM app_fd_empm_regAppl ra "
                                + "INNER JOIN app_fd_empm_reg r ON r.id = ra.c_empl_fk "
                                + "WHERE r.id =? ",
                        new String[]{mail_fk}
                );
                
                String regId = empRegHm.getOrDefault("id", "").toString();
                kwd.setRecordId(regId);
                
                HashMap templHm = EmailTemplateTool.getEmailTemplate(db, Constants.MAIL_TYPE.REG.REG_WELCOME_EMAIL);
                
                mail_subject = kwd.buildContent(db, templHm.get("c_template_subject").toString(), mail_fk, newId);
                mail_content = kwd.buildContent(db, templHm.get("c_template_content").toString()+ Constants.EMAIL_TRACK, mail_fk, newId);
                mail_content = "<span style='font-family:Segoe UI,Roboto,Helvetica Neue,sans-serif'>"+mail_content+"</span>";      

                newMailHm.put("mail_content", mail_content);
                newMailHm.put("mail_subject", mail_subject);

                CommonUtils.saveUpdateForm2("", "mail_list_preview_view", newId, newMailHm);                
            }else{
                mail_content = kwd.buildContent(db, mail_content+ Constants.EMAIL_TRACK, mail_fk, newId);
            }
            
            LogUtil.info("EMPM MODULE - EMail Tool", "SENDING EMAIL TO: "+resend_mail_to
                            +", CC: "+mail_to
                            +", SUBJECT: "+mail_subject
                            +", ATTACHMENTS: "+Boolean.toString(attachments!=null)+" - "+mail_attachment);
            
            CommonUtils.sendEmail(resend_mail_to, mail_to, mail_subject, mail_content, null, attachments);
            
            hm = db.selectOneRecord(
                    "SELECT * FROM "+Constants.TABLE.EMAIL+" WHERE id =? ",
                    new String[]{id}
            );
            
            String dc = hm.getOrDefault("dateCreated","").toString();
            String dm = hm.getOrDefault("dateModified","").toString();
            String mt = hm.getOrDefault("c_mail_type","").toString();
            
            msg("dc "+dc+", dm "+dm+", mt "+mt);
            
            hm = db.selectOneRecord(
                    "SELECT * FROM "+Constants.TABLE.EMAIL+" WHERE id =? ",
                    new String[]{newId}
            );
            
             dc = hm.getOrDefault("dateCreated","").toString();
             dm = hm.getOrDefault("dateModified","").toString();
             mt = hm.getOrDefault("c_mail_type","").toString();
            
            msg("dc "+dc+", dm "+dm+", mt "+mt);
        
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }

        
        return rowSet;
    }
}
