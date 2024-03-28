/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.dao.CurrentUser;
import com.tms.hrdc.defaultPluginTool.EmailTemplateTool;
import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.KeywordDictionary;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.workflow.util.WorkflowUtil;

/**
 *
 * @author faizr
 */
public class DraftSavesBinder extends WorkflowFormBinder{
    
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
        return "Employers email sending after draft is submitted";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Employer Registration Binder";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }
    
    @Override
    public String getPropertyOptions() {
       return "";
    }
    
    public void msg(String msg){
        LogUtil.info(getName(), msg);
    }
    
    @Override
    public FormRowSet load(Element element, String id, FormData formData) {
        
        String empRegId = AppUtil.processHashVariable("#requestParam.empRegId#", null, null, null);
        FormRowSet rows = new FormRowSet();
        
        if(StringUtils.isBlank(id) && StringUtils.isBlank(empRegId)){
            return rows;
        }
        
        String formId = super.getFormId();
        
        DBHandler db = new DBHandler();
//        LogUtil.info("id", id);
        try{
            db.openConnection();
            switch(formId){
                case Constants.FORM_ID.EMP_REG_FORM:
                    
                    String mailId = AppUtil.processHashVariable("#requestParam.mailId#", null, null, null);
                    
                    if(!StringUtils.isBlank(mailId)){
                        String query = "UPDATE app_fd_empm_usr_mail SET c_is_seen = ?, c_mail_status = ? WHERE md5(concat(id, c_mail_fk)) = ?";
                        int i = db.update(query, new String[]{Constants.STATUS.EMAIL.RESPONDED, Constants.STATUS.EMAIL.RESPONDED}, new String[]{mailId});
                        LogUtil.info("Oyen", mailId+" result "+Integer.toString(i));
                    
                        FormRow row = new FormRow();
                        row.put("empl_fk", empRegId);                   

                        rows.add(row);
                    }else{
                        rows = super.load(element, id, formData);
                    }
                    
                    
                break;
                case Constants.FORM_ID.EMP_REG_DRAFT_FORM:
                    
                    CurrentUser cu = new CurrentUser();
                    boolean is_cu_officer = false;
                    
                    if(!cu.isNull()){
                        is_cu_officer = cu.isCurrentUserHRDCOfficer();
                    }
                    
                    HashMap applHm = getRegData(db, id);
                    
                    String form_type = applHm!=null?applHm.getOrDefault("appRegFormType", "").toString():"";
                    //check if email already sent
                    
                    if(!form_type.equals("1A") && !isUserCreated(db, applHm)){
                        createUser(applHm);
                    }
                    
                    String modifiedBy = cu.isNull()?applHm.get("c_req_email").toString():cu.getFullName();
                    
                    new AuditTrailUtil().insertAuditTrail2(
                            db, 
                            applHm.get("id").toString(), 
                            modifiedBy, 
                            "Saved Form 1", "", 
                            false, null
                    );
                    
                    if(!is_cu_officer){
                        sendEmail(db, applHm);
                    }                    
                    
                    rows = super.load(element, id, formData);
                break;
                default:
                    rows = super.load(element, id, formData);
            }
                        
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        } finally {
            db.closeConnection();
        }
                
        return rows;        
    }

    private HashMap getRegData(DBHandler db, String id) {
        String sql = "SELECT r.*, a.c_form_type as appRegFormType, a.id as appRegId FROM app_fd_empm_reg r "
                + "INNER JOIN  app_fd_empm_regAppl a on r.id = a.c_empl_fk "
                + " WHERE a.id = ? ";
        
        HashMap hm = db.selectOneRecord(sql, new String[]{id});
        
        return hm;
    }

    private boolean isUserCreated(DBHandler db, HashMap applHm) {
        String email = applHm.get("c_req_email")==null?"":applHm.get("c_req_email").toString();
        String sql = "select m.* from app_fd_empm_usermap m\n" +
                    "inner join dir_user d on d.id = m.c_userId\n" +
                    "where username = ?";
        
        HashMap hm = db.selectOneRecord(sql, new String[]{email});
        
        if(hm!=null){
            return true;
        }else{
            return false;
        }
    }

    private void createUser(HashMap applHm) {
        String email = applHm.get("c_req_email").toString();
        String pw_re = applHm.get("c_req_pw_re").toString();
        String mycoid = applHm.get("c_mycoid").toString();
        String compName = applHm.get("c_comp_name").toString().isEmpty()?email:
                applHm.get("c_comp_name").toString();
        String compId = applHm.get("id").toString();
        String hrdc_no = applHm.get("c_hrdc_no").toString();
        
        String userId = email;
        
        CommonUtils.createJogetUser(userId, email, compName,
                "", email, pw_re, Constants.USER_GROUP.DRAFT_USERS);  
        CommonUtils.insertIntoUserMap(userId, compId);
    }
    
    private void sendEmail(DBHandler db, HashMap applHm) throws SQLException, UnsupportedEncodingException {
        String email = applHm.get("c_req_email").toString();
        String pw_re = applHm.get("c_req_pw_re").toString();
        String mycoid = applHm.get("c_mycoid").toString();
        String compName = applHm.get("c_comp_name").toString().isEmpty()?email:
                applHm.get("c_comp_name").toString();
        String compId = applHm.get("id").toString();
        String hrdc_no = applHm.get("c_hrdc_no").toString();
        String appRegId = applHm.get("appRegId").toString();
                
        HashMap emailTemplate = EmailTemplateTool.
                                    getEmailTemplate(db, Constants.MAIL_TYPE.REG.DRAFT);
        
        String userId = "temp-"+hrdc_no;
        
        if(emailTemplate==null){
            return;
        }
        
        HashMap mail_preview_hm = new HashMap();
        mail_preview_hm.put("mail_type", Constants.MAIL_TYPE.REG.DRAFT);
        mail_preview_hm.put("mail_fk", compId);
        mail_preview_hm.put("mail_to", email);
        mail_preview_hm.put("comp_name", compName);

        String mailId = CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                    Constants.FORM_ID.MAIL_PREVIEW, 
                                    "", mail_preview_hm);
        
        KeywordDictionary kwd = new KeywordDictionary(db);
        kwd.setRecordId(appRegId);
        
        String subject = emailTemplate.get("c_template_subject")==null?"":emailTemplate.get("c_template_subject").toString();
        String msg = emailTemplate.get("c_template_content")==null?"":emailTemplate.get("c_template_content").toString();

        subject = kwd.buildContent(db, subject, compId, "");
        msg = kwd.buildContent(db, msg, compId, mailId);
        
        msg = "<span style='font-family:Segoe UI,Roboto,Helvetica Neue,sans-serif'>"+msg+"</span>";  
        
        mail_preview_hm.put("mail_subject", subject);
        mail_preview_hm.put("mail_content", msg);  
        
        CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                    Constants.FORM_ID.MAIL_PREVIEW, 
                                    mailId, mail_preview_hm);
        
        CommonUtils.sendEmail(email, "", subject, msg, "", null);
    }
}
