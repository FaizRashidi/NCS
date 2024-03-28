/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.defaultPluginTool.EmailTemplateTool;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.KeywordDictionary;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PluginThread;
import org.joget.commons.util.UuidGenerator;
import org.joget.directory.model.User;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.WorkflowProcessLink;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;

/**
 *
 * @author faizr
 */
public class QueryBinder extends WorkflowFormBinder{
    
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
        return "Used in Query Processing, contains load binder and store binder";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Query Binder";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
       return "";
    }
    
    private void msg(String msg){
        LogUtil.info(this.getClass().toString(), msg);
    }
    
    @Override
    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {
        FormRow row = rows.get(0);
        String mail_content = row.getProperty("mail_content");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
        LocalDateTime now = LocalDateTime.now();  
        row.setProperty("query_date",dtf.format(now));
        
        final FormRowSet rows_ = super.store(element, rows, formData);        
        return rows_;  
    }

    private void updateQueryFields(FormRowSet rows, DBHandler db) {
        
        FormRow row = rows.get(0);
        String pkv = row.getProperty("fk");
        String id = row.getId();
 
        String query = "SELECT * FROM app_fd_empm_reg WHERE id = ?";
        String[] cond = {pkv};
        
        HashMap hm = db.selectOneRecord(query, cond);        

        if(hm == null || hm.size()<0){
            return;
        }
        
        String hrdc_no = hm.get("c_hrdc_no") == null?"":hm.get("c_hrdc_no").toString();
        
        String suffix = UuidGenerator.getInstance().getUuid().substring(0, 8);
        if(!id.contains("query")){
            rows.get(0).setId("query-"+hrdc_no+"-"+suffix);
        }        
        rows.get(0).setProperty("req_mycoid", hrdc_no);
    }
    
    String recordId = "";
        
    @Override
    public FormRowSet load(Element element, String id, FormData formData) {
        
        msg("Template loda binder starting");
            
        if(!StringUtils.isBlank(formData.getProcessId())){
            WorkflowManager wm = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
            WorkflowProcessLink wpl = wm.getWorkflowProcessLink(formData.getProcessId());
            recordId = wpl.getOriginProcessId();
        }
                
        FormRowSet rows = new FormRowSet();
        msg("templ id "+id);
        if(StringUtils.isBlank(id)){
            return rows;
        }
        
        DBHandler db = new DBHandler();
        
        try {
            
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
        
            String[] rawValue = id.split("_"); //0-tag, 1-emp_reg id, 2-tmeplate id, 3-reason
            
            msg("raw _ size "+Integer.toString(rawValue.length));
            if(rawValue.length<3){
                db.closeConnection();
                return rows;
            }

            //- --------------------------------------------------------------------
            // add email
            String rejectReason = "";
            String queryReason = "";
            String subReason = "";
            try{
                queryReason = rawValue[3]==null?"":rawValue[3];
                rejectReason = rawValue[3] == null ? "" : rawValue[3];
                subReason = rawValue[3] == null?"":rawValue[3];
            }catch(Exception e){
//                LogUtil.info("Query binder", "No Reason selected");
            }
            String mailTemplateId = rawValue[2]==null?"":rawValue[2];
            String empId = rawValue[1]==null?"":rawValue[1];
            id = rawValue[0]==null?"":rawValue[0];
            if(mailTemplateId==null || empId.contains("#")){
                db.closeConnection();
                LogUtil.info("mailtype/","empid #");
                return rows;
            }

            id = id+"_"+empId+"_"+mailTemplateId;
//            HashMap hm = EmailTemplateTool.getEmailTemplate(db, mailType);
            HashMap hm = db.selectOneRecord(
                     "SELECT \n" +
                    "	mt.id mailType, CONCAT(mt.c_moduleType, ' - ',mt.c_emailType) templ_name,\n" +
                    "	s.c_template_subject, s.c_template_content\n" +
                    "FROM app_fd_empm_email_stp mt\n" +
                    "INNER JOIN app_fd_empm_template_stp s ON mt.id = s.c_email_fk " + 
                    "AND s.id = ?",
                    new String[]{mailTemplateId}
            );
                    
//            hm.put("mail_type", mailTemplateId);
            hm.put("mail_fk", empId);
            hm.put("id", id);
            hm.put("query_reason", queryReason);
            hm.put("reject_reason",rejectReason);
            hm.put("sub_reason",subReason);
            
            
            msg("Loading template "+mailTemplateId);

            
            if(hm != null){                
                rows = getRows(db, hm);    
                String mail_content = rows.get(0).getProperty("mail_content");
            }
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }finally {
            db.closeConnection();
        }
        LogUtil.info("row id: ", rows.get(0).getProperty("id"));
        return rows;
    }
    
    public boolean isQueryTrue(String query, String id, DBHandler db){
        try{
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
        }catch(Exception e){            
        }          
        
        String[] cond = {id};            

        HashMap<String, String> hm = db.selectOneRecord(query, cond);

        db.closeConnection();
        
        if(hm != null && !hm.isEmpty()){
            return true;
        }else{
            return false;
        }
    }

    private FormRowSet getRows(DBHandler db, HashMap hm) throws SQLException, UnsupportedEncodingException{
        
        String subject = hm.get("c_template_subject")==null?"":hm.get("c_template_subject").toString();
        String content = hm.get("c_template_content")==null?"":hm.get("c_template_content").toString();
        String remark = hm.get("remark")==null?"":hm.get("remark").toString();
        String empId = hm.get("mail_fk")==null?"":hm.get("mail_fk").toString();     // empId
        String type = hm.get("mailType")==null?"":hm.get("mailType").toString();  // formerly ET--
        String id = hm.get("id")==null?"":hm.get("id").toString();                  //random
        String query_reason = hm.get("query_reason")==null?"":hm.get("query_reason").toString();
        String reject_reason = hm.get("reject_reason")==null?"":hm.get("reject_reason").toString();
        String sub_reason = hm.get("sub_reason")==null?"":hm.get("sub_reason").toString();
        
        KeywordDictionary kwd = new KeywordDictionary(db);
        kwd.setIsPreview();
        
        if(!recordId.isEmpty()){
            kwd.setRecordId(recordId);
            
        }        
        if(!query_reason.isEmpty()){
            kwd.buildQueryReason(db,query_reason);
            query_reason = kwd.getBuiltQueryReason();
        }
        
        if(!reject_reason.isEmpty()){
            kwd.buildRejectReason(db,reject_reason);
            reject_reason = kwd.getBuiltRejectReason();
        }
        
        if(!sub_reason.isEmpty()){
            kwd.buildSubReason(db,sub_reason);
            sub_reason = kwd.getBuiltSubReason();
        }
        
        
        String parsedSubject = kwd.buildContent(db, subject, empId, id);
        String parsedMsg = kwd.buildContent(db, content, empId, id);
                
        FormRowSet rows = new FormRowSet();
        FormRow row = new FormRow();
        
        HashMap cu = CommonUtils.getCurrentUser();
        
        if(hm!= null && !hm.isEmpty()){
            row.setCreatedByName(cu.get("firstName").toString()+" "+cu.get("lastName").toString());
            row.setCreatedBy(cu.get("username").toString());
            row.setProperty("mail_subject", parsedSubject);
            row.setProperty("mail_remark", remark);
            row.setProperty("mail_content", parsedMsg);
            row.setProperty("mail_fk", empId);
            row.setProperty("mail_type", type);
            row.setProperty("is_seen", Constants.STATUS.EMAIL.SENT);
            row.setProperty("query_reason", query_reason);
            row.setProperty("reject_reason",reject_reason);
            row.setProperty("sub_reason",sub_reason);
            row.setId(id);
            LogUtil.info("hm: ", "not empty");
        }
        else{
            LogUtil.info("hm: ", " empty!");
        }
        
        rows.add(row);

        return rows;        
    }

    private String getEmpEmail(DBHandler db, String empId) {
        String sql =  "SELECT d.email FROM app_fd_empm_usermap u "
        + " INNER JOIN dir_user d ON d.id = u.c_userId "
        + "WHERE u.c_compId = ?";
        
        HashMap hm = db.selectOneRecord(sql, new String[]{empId});
        
        if(hm!=null){
            return hm.get("email").toString();
        }
        
        return "";
    }
    
}
