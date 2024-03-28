/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.datalistAction;

import com.tms.hrdc.defaultPluginTool.EmailTemplateTool;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.KeywordDictionary;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;

/**
 *
 * @author kahyi
 */
public class PotEmpMailListButton extends DataListActionDefault {

    @Override
    public String getName() {
        return "HRDC - EMPM - Load MailList Form Datalist Action"; 
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "To load MailList Form with the correct Id."; 
    }

    @Override
    public String getLinkLabel() {
        String label = getPropertyString("label");
        if (label == null || label.isEmpty()) {
            label = "Load Mail Content for True Potential Employers within Batch Id";
        }
        return label;    }

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

    @Override
    public String getConfirmation() {
        String confirm = getPropertyString("confirmation");
        if (confirm == null || confirm.isEmpty()) {
            confirm = "Edit Mail Content for True Potential Employer(s)?";
        }
        return confirm;    }

    
    @Override
    public String getLabel() {
        return "HRDC - EMPM - Email PE Button";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        String json = "[{\n"
                + "    title : 'Mail List Button',\n"
                + "    properties : [{\n"
                + "        label : 'Label',\n"
                + "        name : 'label',\n"
                + "        type : 'textfield',\n"
                + "        description : 'Load Mail List button',\n"
                + "        value : 'Load Mail List button'\n"
                + "    },{"
                + "        name: 'send_type',"
                + "        label: 'Send Type',"
                + "        type: 'radio',"
                + "        options: ["
                + "              {value: 'EMAIL', label: 'EMAIL'},"
                + "              {value: 'LETTER', label: 'LETTER'}"
                + "        ]"
                + "     }]"
                + "}]";
        return json;    }
    
    int mailCount = 0;
    
    /*
    batch - empm_pe_file_upl
    item - empm_pe_potEmp
    */
    
    String vendor_email = "";
    String vendor_name = "";
    
    @Override
    public DataListActionResult executeAction(DataList dl, String[] rowKeys) {
        DBHandler db = new DBHandler();
          
        String type = (String) getProperty("send_type");
        int totalCount = 0;
        String message = "";
        String list_id = "";
        
        boolean isEmail = false;
        boolean isLetter = false;
        
        int emailSize = 0;
        int letterSize = 0;
        
        try{
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
            if(rowKeys.length != 0){
                
                HashMap hm = getAllEmpData(db, rowKeys);

                if(hm.get("EMAIL")!=null){
                    ArrayList mailList = (ArrayList) hm.get("EMAIL");
                    if(mailList.size()>0){
                        emailSize = mailList.size();
                        isEmail = true;
                        list_id = processEmailSending(db, mailList);
                    }
                }
                
                if(hm.get("LETTER")!=null){                    
                    ArrayList nonMailList = (ArrayList) hm.get("LETTER");
                    if(nonMailList.size()>0){
                        letterSize = nonMailList.size();
                        isLetter = true;
                        processLetterSending(db, nonMailList);
                    }
                }
                
//                switch(type){
//                    case "EMAIL":
//                        list_id = processEmailSending(db, rowKeys);
//                    break;
//                    case "LETTER":
//                        processLetterSending(db, rowKeys);
//                    break;
//                }
            
            }else{
                message = "No Data Selected";
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }        
        
        DataListActionResult result = new DataListActionResult();
        result.setType(DataListActionResult.TYPE_REDIRECT);
        
//        if(type.equals("EMAIL") && !list_id.isEmpty()){
//            result.setUrl("pe_mail_list?_mode=edit&id="+list_id);            
//        }else{
//            result.setUrl("REFERER");
//            message = "Email sent to "+vendor_name+" ("+vendor_email+")";
//        }        
        
        //format : x letters sent to vendor
        // sending x emails
        
        if(isEmail){
            result.setUrl("pe_mail_list?_mode=edit&id="+list_id); 
            message += "Sending "+Integer.toString(emailSize)+" emails. ";
            result.setUrl("pe_mail_list?_mode=edit&id="+list_id);       
        }else{
            result.setUrl("REFERER");
        }
        
        if(isLetter){            
            message+=!message.isEmpty()? ",":"";
            message += "Data of "+Integer.toString(letterSize)+" employers "
                    + " sent to vendor "+vendor_name+" ("+vendor_email+") for letter processing.";   
        }
        
        if(!message.isEmpty()){
            result.setMessage(message);
        }
        
        return result;   
    }
    
    private HashMap getAllEmpData(DBHandler db, String[] rowKeys){
        
        //rowkeys - batch/indv
        //if batch query by c_batch = ?
        //else query by id
        
        HashMap peHm = new HashMap();
        peHm.put("EMAIL", "");
        peHm.put("LETTER", "");
        
        ArrayList peListWtEmail = new ArrayList();
        ArrayList peListWOEmail = new ArrayList();
        
        for(String id:rowKeys){
            
            ArrayList<HashMap<String, String>> peList_ = getPotEmpData(db, id);
            
            peListWtEmail.addAll((ArrayList) peList_.stream()
                .filter(hashmap -> hashmap.containsKey("c_empl_email_pri"))
                .filter(hashmap -> !StringUtils.isBlank(hashmap.get("c_empl_email_pri")))
                .collect(Collectors.toList())
            );
            
            peListWOEmail.addAll((ArrayList) peList_.stream()
                .filter(hashmap -> hashmap.containsKey("c_empl_email_pri"))
                .filter(hashmap -> StringUtils.isBlank(hashmap.get("c_empl_email_pri")))
                .collect(Collectors.toList())
            );
        }
        
        peHm.put("EMAIL", peListWtEmail);
        peHm.put("LETTER", peListWOEmail);
        
        return peHm;
    }
    
    private String processEmailSending(DBHandler db, ArrayList<HashMap<String, String>> empList) throws SQLException, UnsupportedEncodingException {
        String ref = "PE/MAIL/"+CommonUtils.get_DT_CurrentDateTime("YYYY")+"/"+CommonUtils.getRefNo("6","potEmp_mail_counter");
            
        HashMap mail_list_hm = new HashMap();        
        mail_list_hm.put("registeredCount", "0");
        mail_list_hm.put("mailCount", Integer.toString(empList.size()));
        mail_list_hm.put("mailRef", ref);
        String list_id = CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                                Constants.FORM_ID.POTEMP_MAIL_LIST, 
                                                "", mail_list_hm);
        
        for (HashMap empHm: empList){
            initializeMailList(db,empHm,list_id);
        }

        mail_list_hm = new HashMap();     
        mail_list_hm.put("mailCount", Integer.toString(mailCount));
        CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                        Constants.FORM_ID.POTEMP_MAIL_LIST, 
                                        list_id, mail_list_hm);        
        
        return list_id;
    }
    
    private void initializeMailList(DBHandler db, HashMap pe, String list_id ) throws SQLException, UnsupportedEncodingException{      
        // get the list of pe's with batchid
        KeywordDictionary kwd = new KeywordDictionary(db);
//        ArrayList<HashMap<String, String>> pe_list = getPotEmpData(db, dataId);
        
        HashMap mail_preview_hm;        
        
//        mailCount += pe_list.size();
        //need to get default email template, parse it with empm values and store
        HashMap emailTemplate = EmailTemplateTool.getEmailTemplate(db, 
                                    Constants.MAIL_TYPE.POT_EMP.ORDER);
        
//        for (HashMap<String, String> pe: pe_list){
            
            String emp_id = pe.get("id").toString();
            String emp_email = pe.get("c_empl_email_pri").toString();
            String emp_comp = pe.get("c_comp_name").toString();
            String id = pe.get("c_batch")==null?emp_id:pe.get("c_batch").toString();
            
            mail_preview_hm = new HashMap();
            mail_preview_hm.put("mail_type", Constants.MAIL_TYPE.POT_EMP.ORDER);
            mail_preview_hm.put("sent_as", Constants.MAIL_SENT_AS.EMAIL);
            mail_preview_hm.put("mail_fk", emp_id);
            mail_preview_hm.put("list_fk", list_id);
            mail_preview_hm.put("batch_id", id);
            mail_preview_hm.put("mail_to", emp_email);
            mail_preview_hm.put("comp_name", emp_comp);
            mail_preview_hm.put("is_seen", Constants.STATUS.EMAIL.PENDING_SEND);
            
            String mailId = CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                        Constants.FORM_ID.MAIL_PREVIEW, 
                                        "", mail_preview_hm);
            
            String subject = kwd.buildContent(db, emailTemplate.get("c_template_subject").toString(), emp_id, mailId);
            String message = kwd.addTracker(emailTemplate.get("c_template_content").toString());
            message = kwd.buildContent(db, message, emp_id, mailId);            
            message = "<span style='font-family:Segoe UI,Roboto,Helvetica Neue,sans-serif'>"+message+"</span>";            
            
            mail_preview_hm.put("mail_subject", subject);
            mail_preview_hm.put("mail_content", message);            
            
            CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                        Constants.FORM_ID.MAIL_PREVIEW, 
                                        mailId, mail_preview_hm);
//        }
    }

    private ArrayList<HashMap<String, String>> getPotEmpData(DBHandler db, String dataId) {
        String pe_query = "SELECT r.id, r.c_comp_name ,r.c_empl_email_pri, c_batch,\n" +
                            "r.c_empl_address, r.c_empl_address2, r.c_empl_address3,\n" +
                            "(SELECT c.c_city FROM app_fd_stp_city c WHERE id = r.c_empl_city limit 1) city, \n" +
                            "(SELECT s.c_state FROM app_fd_stp_state s WHERE id = r.c_empl_state limit 1) state,\n" +
                            "(SELECT s.c_country FROM app_fd_stp_country s WHERE id = r.c_empl_country limit 1) country,\n" +
                            "r.c_empl_postcode, r.c_empl_country,\n" +
                            "r.c_empl_tel_no_pri \n" +
                            "from app_fd_empm_pe_potEmp p \n" +
                            "inner join app_fd_empm_reg r on r.id = p.c_emp_fk " +
                            "where c_batch = ?";
        
        ArrayList<HashMap<String, String>> pe_list = db.select(pe_query, new String[]{dataId});
        
        if(pe_list.size()==0){
            pe_query = "SELECT r.id, r.c_comp_name ,r.c_empl_email_pri, c_batch,\n" +
                        "r.c_empl_address, r.c_empl_address2, r.c_empl_address3,\n" +
                        "(SELECT c.c_city FROM app_fd_stp_city c WHERE id = r.c_empl_city limit 1) city, \n" +
                        "(SELECT s.c_state FROM app_fd_stp_state s WHERE id = r.c_empl_state limit 1) state,\n" +
                        "(SELECT s.c_country FROM app_fd_stp_country s WHERE id = r.c_empl_country limit 1) country,\n" +
                        "r.c_empl_postcode, r.c_empl_country,\n" +
                        "r.c_empl_tel_no_pri " +
                        "from app_fd_empm_pe_potEmp p " +
                        "inner join app_fd_empm_reg r on r.id = p.c_emp_fk " +
                        "where p.id = ?";
            pe_list = db.select(pe_query, new String[]{dataId});
        }
        return pe_list;
    }

    //get vendor data /
    //get PE data / 
    //build data / 
    //make excel file with pe data 
    //write contnt into file and zip
    //update status and send email
    private void processLetterSending(DBHandler db, ArrayList<HashMap<String, String>> pe_list) throws UnsupportedEncodingException, SQLException {
//        ArrayList<HashMap<String, String>> pe_list = new ArrayList();
//        for (String dataId: rowKeys){
//            ArrayList pe = getPotEmpData(db, dataId);
//            pe_list.addAll(pe);
//        }
        
        HashMap vendorData = db.selectOneRecord(
                            "SELECT c_name, c_email FROM app_fd_empm_mail_vendor_stp");
        
        vendor_name = vendorData==null?"":vendorData.get("c_name").toString();
        vendor_email = vendorData==null?"":vendorData.get("c_email").toString();
        
        KeywordDictionary kwd = new KeywordDictionary(db);
        HashMap emailTemplate = EmailTemplateTool.getEmailTemplate(db, 
                                    Constants.MAIL_TYPE.POT_EMP.ORDER);
        
        int runno = 0;
        
        for (HashMap<String, String> pe: pe_list){
            
            String emp_id = pe.get("id");
//            String emp_email = pe.get("c_empl_email_pri");
            String emp_comp = pe.get("c_comp_name");
            String address = 
                    emp_comp
                    + (StringUtils.isBlank(pe.get("c_empl_address"))?"":", "+pe.get("c_empl_address").toUpperCase() )
                    + ( StringUtils.isBlank(pe.get("c_empl_address2"))?"":", "+pe.get("c_empl_address2").toUpperCase() )
                    + ( StringUtils.isBlank(pe.get("c_empl_address3"))?"":", "+pe.get("c_empl_address3").toUpperCase() )
                    + ( StringUtils.isBlank(pe.get("city"))?"":", "+pe.get("city").toUpperCase() )
                    + ( StringUtils.isBlank(pe.get("state"))?"":", "+pe.get("state").toUpperCase() )
                    + ( StringUtils.isBlank(pe.get("c_empl_postcode"))?"":", "+pe.get("c_empl_postcode").toUpperCase() )
                    + ( StringUtils.isBlank(pe.get("country"))?"":", "+pe.get("country").toUpperCase() );
            
            HashMap mail_preview_hm = new HashMap();
            String mailId = CommonUtils.get_DT_CurrentDateTime("YYYYMMddHHmmss")+"-"+Integer.toString(runno++);
            
            mail_preview_hm.put("mail_type", Constants.MAIL_TYPE.POT_EMP.ORDER);
            mail_preview_hm.put("sent_as", Constants.MAIL_SENT_AS.LETTER);
            mail_preview_hm.put("mail_fk", emp_id);
            mail_preview_hm.put("mail_to", address);
            mail_preview_hm.put("comp_name", emp_comp);
            mail_preview_hm.put("letter_status", Constants.STATUS.LETTER.PENDING_SEND);
            
            CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                        Constants.FORM_ID.MAIL_PREVIEW, 
                                        mailId, mail_preview_hm);
            
            String subject = kwd.buildContent(db, emailTemplate.get("c_template_subject").toString(), emp_id, mailId);
            String message = kwd.buildContent(db, emailTemplate.get("c_template_content").toString(), emp_id, mailId);            
            message = "<span style='font-family:Segoe UI,Roboto,Helvetica Neue,sans-serif'>"+message+"</span>";            
            
            mail_preview_hm.put("mail_subject", subject);   
            mail_preview_hm.put("mail_content", message);            
            
            CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                        Constants.FORM_ID.MAIL_PREVIEW, 
                                        mailId, mail_preview_hm);
        }
    }
}
