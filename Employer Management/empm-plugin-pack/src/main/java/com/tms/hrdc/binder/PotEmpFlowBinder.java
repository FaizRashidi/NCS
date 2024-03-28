/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.ExcelFileBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SetupManager;
import org.joget.workflow.util.WorkflowUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 *
 * @author faizr
 */
public class PotEmpFlowBinder extends WorkflowFormBinder{
    
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
        return "Store Binder for Potential Employers' Flow";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Potential Employer Binder";
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
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {
        
        String formId = super.getFormId();
        String id = rowSet.get(0).getId();
        
        rowSet = super.store(element, rowSet, formData);
        
        DBHandler db = new DBHandler();
        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
            
            switch(formId){
                case Constants.FORM_ID.POTEMP_WO_APPROVAL:
                    processWOApproval(db, id);
                break;
                case Constants.FORM_ID.POTEMP_MAIL_LIST:
                    String vendorId = rowSet.get(0).getProperty("vendor_email");
                    processEmailSending(db, id, vendorId);
                break;
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }finally{
            db.closeConnection();
        }
        
       return rowSet;
    }

    private void processWOApproval(DBHandler db, String wOId) {
        
        String sql = "SELECT w.c_status, pe.id as peId, pe.c_emp_fk as empId, r.c_data_status "
                + "FROM app_fd_empm_pe_writeoff w "
                + "INNER JOIN "+Constants.TABLE.POT_EMP+" pe ON pe.c_writeoff_fk = w.id "
                + "INNER JOIN "+Constants.TABLE.EMPREG+" r ON pe.c_emp_fk = r.id "
                + "WHERE w.id = ? ";
        HashMap hm = db.selectOneRecord(sql, new String[]{wOId});
        
        String approval = "", empId = "", potEmpId = "", empStatus = "";
        HashMap audHm = new HashMap();
        int i = 0;
        
        if(hm!=null){
            approval = hm.get("c_status")==null?"":hm.get("c_status").toString();
            empId = hm.get("empId")==null?"":hm.get("empId").toString();
            potEmpId = hm.get("peId")==null?"":hm.get("peId").toString();
            empStatus = hm.get("c_data_status")==null?"":hm.get("c_data_status").toString();
        }
        
        if(approval.equals("Approved")){
            //change status of potemp table
            sql = "UPDATE "+Constants.TABLE.POT_EMP+" SET c_status=? WHERE c_writeoff_fk = ?";
            i = db.update(sql, new String[]{Constants.STATUS.POT_EMP.WRITTEN_OFF}, new String[]{wOId});
            //change status of empreg - delete maybe?
            sql = "UPDATE "+Constants.TABLE.EMPREG+" SET c_data_status=? WHERE id = ?";
            i = db.update(sql, new String[]{Constants.STATUS.POT_EMP.WRITTEN_OFF}, new String[]{empId});            
            
            sql = "DELETE FROM  "+Constants.TABLE.POT_EMP+" WHERE c_writeoff_fk = ?";
            i = db.delete(sql, new String[]{wOId});
            //change status of empreg - delete maybe?
            sql = "DELETE FROM   "+Constants.TABLE.EMPREG+" WHERE id = ?";
            i = db.delete(sql, new String[]{empId});    
        }
        
        if(approval.equals("Rejected")){
            //change status of potemp table
            sql = "UPDATE "+Constants.TABLE.POT_EMP+" SET c_status=? WHERE c_writeoff_fk = ?";
            i = db.update(sql, new String[]{
                empStatus.equals(Constants.STATUS.EMP.TRUE_POTENTIAL_EMPLOYER)?
                        Constants.STATUS.POT_EMP.TRUE:
                        Constants.STATUS.POT_EMP.POTENTIAL
                                            }, new String[]{wOId});
            //change status of empreg - delete maybe?
            sql = "UPDATE "+Constants.TABLE.EMPREG+" SET c_data_status=? WHERE id = ?";
            i = db.update(sql, new String[]{Constants.STATUS.POT_EMP.WRITTEN_OFF}, new String[]{empId});  
        }
        
        if(i>0){
            //add auditHashMap audHm = new HashMap();
            audHm.put("status", "Write-Off "+approval);
            audHm.put("fk", wOId);
            audHm.put("createdByName", WorkflowUtil.getCurrentUserFullName());

            CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                                        Constants.FORM_ID.AUDIT_TRAIL, 
                                                        "", audHm);
        }
    }

    private void processEmailSending(DBHandler db, String id, String vendorId) {
        // update mail list - status - preview -> submitted
        // check type
        //  email - iterate through, rebuild & send
        // letter - iterate and write to pdf, zip, write details with status
        String mailType = db.selectOneValueFromId(
                Constants.TABLE.POT_EMP_MAIL_LIST, "c_mailType", id
        );
        
        int i = db.update(
                "UPDATE "+Constants.TABLE.POT_EMP_MAIL_LIST+" SET c_status = ? "
                + "WHERE id = ?", 
                new String[]{"SUBMITTED"}, new String[]{id}
        );
        
        String query = "SELECT "
                    + "a.c_mailRef,b.c_empl_email_pri, c_mail_subject, "
                    + "c_mail_content, c_mail_to, a.id as mailId, b.id as empId "
                    + "FROM "+Constants.TABLE.EMAIL+" a "
                    + "INNER JOIN "+Constants.TABLE.EMPREG+" b "
                    + "ON a.c_mail_fk = b.id "
                    + "WHERE a.c_list_fk= ? ";
        
        ArrayList<HashMap<String, String>> mailList = db.select(
                query,
                new String[]{id}
        );
        
        switch(mailType){
            case "EMAIL":
                processSendEmail(db, mailList);
            break;
            case "LETTER":
                processSendLetter(db, mailList, vendorId, id);
            break;
        }
    }

    private void processSendLetter(DBHandler db, ArrayList<HashMap<String, String>> mailList, String vendorId, String listId) {
        
        String vendor = db.selectOneValueFromId("app_fd_empm_mail_vendor_stp", 
                "c_email", vendorId);
        ArrayList<HashMap<String, String>> filesList = new ArrayList();
        
        String baseAbsolutePath = SetupManager.getBaseDirectory() + "app_formuploads" 
                + File.separator 
                + Constants.TABLE.POT_EMP_MAIL_LIST.replaceFirst("app_fd_", "") 
                + File.separator;
        String excelPath = baseAbsolutePath+listId;
        
//        filesList.add(excelHm);
        
        for (HashMap<String, String> email_message: mailList){

            String mailId = email_message.get("mailId").toString();
            String empId = email_message.get("empId").toString();
            String emp_email = email_message.get("c_mail_to").toString();
            String emp_subject = email_message.get("c_mail_subject").toString();
            String emp_message = email_message.get("c_mail_content").toString();
            String mail_ref = email_message.get("c_mailRef").toString();
            
            //TODO build file
            HashMap letterHm = writeToFile(emp_subject, emp_message, baseAbsolutePath+mailId);
            filesList.add(letterHm);
            
            String query = "UPDATE app_fd_empm_usr_mail SET c_letter_status = ? WHERE id = ?";
            db.update(query, new String[]{Constants.STATUS.LETTER.PENDING_SEND}, new String[]{mailId});

            query = "UPDATE "+Constants.TABLE.EMPREG+" SET c_last_move = ? WHERE id = ?";
            db.update(query, new String[]{Constants.LAST_MOVEMENT.LETTER_SENT}, new String[]{empId});
        }
        
        Object[] obj = convToObj(filesList);
        
        CommonUtils.sendEmail(vendor, "", "Employers' Letter To Be Delivered", 
                "Plz send, ty", null, obj);
    }
    
    private void processSendEmail(DBHandler db, ArrayList<HashMap<String, String>> mailList) {
        for (HashMap<String, String> email_message: mailList){

            String mailId = email_message.get("mailId").toString();
            String empId = email_message.get("empId").toString();
            String emp_email = email_message.get("c_mail_to").toString();
            String emp_subject = email_message.get("c_mail_subject").toString();
            String emp_message = email_message.get("c_mail_content").toString();

//            sendEmail("hrdc@no-reply.com",emp_email,"","",emp_subject,emp_message);
            CommonUtils.sendEmail(emp_email, "", emp_subject, emp_message, null, null);
            
            String query = "UPDATE app_fd_empm_usr_mail SET c_mail_status = ? WHERE id = ?";
            db.update(query, new String[]{Constants.STATUS.EMAIL.SENT}, new String[]{mailId});

            query = "UPDATE "+Constants.TABLE.EMPREG+" SET c_last_move = ? WHERE id = ?";
            db.update(query, new String[]{Constants.LAST_MOVEMENT.LETTER_SENT}, new String[]{empId});
        }
    }

    private HashMap writeToFile(String subject, String content, String absolutePath) {
        
        
        boolean dirExists = true; 
        HashMap fileHm = new HashMap();
        
        File folder = new File(absolutePath);

        if (!folder.exists()) {
            LogUtil.info("LETTER BUILD","Directory not exist!");
            if (folder.mkdirs()) {
                LogUtil.info("LETTER BUILD","Directory is created!");
            } else {
                LogUtil.info("LETTER BUILD","uploadFile error > Failed to create directory!");
                dirExists = false;
            }
        }else{
//                pm("Directory exist!");
        }
        
        subject += subject+".pdf";

        if (dirExists) {
            LogUtil.info("LETTER BUILD","Saving file");
            String filePath = absolutePath + File.separator + subject;
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            Document document = Jsoup.parse(content);             

            // Convert HTML to PDF using Flying Saucer
            try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
                ITextRenderer renderer = new ITextRenderer();
                renderer.setDocumentFromString(document.html());
                renderer.layout();
                renderer.createPDF(outputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }

            file = new File(filePath);
            if (file.exists()) {
                fileHm.put("fileName", subject);
                fileHm.put("type", "system");
                fileHm.put("path", file.getAbsolutePath());
            }
            
            LogUtil.info("LETTER BUILD","Letter Details => "+fileHm.toString());
        }         
        return fileHm;
    }    

    private Object[] convToObj(ArrayList<HashMap<String, String>> filesList) {
        Object[] obj = new Object[filesList.size()];
        
        for(int x=0;x<filesList.size();x++){
            obj[x] = filesList.get(x);
        }
        return obj;
    }

    private HashMap writeEmpToExcel(DBHandler db, String listId, String fileName, String excelPath) throws IOException {
        HashMap excelHm = new ExcelFileBuilder().buildExcel("POTENTIAL EMPLOYER DATA LISTS", excelPath, new ArrayList());
        return excelHm;
    }
}
