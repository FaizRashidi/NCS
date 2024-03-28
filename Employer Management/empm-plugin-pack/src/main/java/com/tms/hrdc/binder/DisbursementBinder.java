/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.dao.APIManager;
import com.tms.hrdc.dao.CurrentUser;
import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.HttpUtil;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PluginThread;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class DisbursementBinder extends WorkflowFormBinder{
    
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
        return "E-Disbursement";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - E-Disbursement Binder";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }
    
    @Override
    public String getPropertyOptions() {
       return AppUtil.readPluginResource(getClass().getName(), "/properties/archive_binderx.json", null, true, "message/archive_binder");
    }
    
    private void msg(String msg){
        LogUtil.info(this.getClass().toString(), msg);
    }
    
     @Override
    public FormRowSet load(Element element, String id, FormData formData) {
        
        FormRowSet rows = new FormRowSet();
        String empId = AppUtil.processHashVariable("#requestParam.empId#", null, null, null);;
        DBHandler db = new DBHandler();
        
        try{
            db.openConnection();
            
            if(!StringUtils.isEmpty(empId)){
                id= db.selectOneValueFromTable(
                        "SELECT id FROM app_fd_empm_disburse WHERE c_emp_fk = ?", 
                        new String[]{empId}
                );

//                if(!disbId.isEmpty()){
//                    rows = super.load(element, id, formData);
//                    FormRow row = rows.get(0);
//                    row.setProperty("show", "true");
//                    row.setProperty("emp_fk", empId);
//
//                    db.closeConnection();
//                    return rows;
//                }

            }
        
            if(!StringUtils.isBlank(id)){
               
                rows = super.load(element, id, formData);

                FormRow row = rows.get(0);
                row.setProperty("show", "true");
                
                if(!StringUtils.isEmpty(empId)){
                    row.setProperty("emp_fk", empId);
                }
                
                db.closeConnection();
                return rows;
            }

            // --------------------------------------------------------------------------------------------------------
            
            String userId = new CurrentUser().getId();

            HashMap hm = db.selectOneRecord(
                    "SELECT c_compId FROM app_fd_empm_usermap WHERE c_userId = ? ",
                    new String[]{userId}
            );           
            
            //if null - not registered, no comp data. nothing to load. proceed only if admin
            
            if(hm==null && new CurrentUser().isAdmin()){
                FormRow row = new FormRow();
                row.setProperty("show", "true");
            }else if(hm!=null){
                FormRow row = new FormRow();
                row.setProperty("show", "true");
                empId = hm.getOrDefault("c_compId","").toString();
            }else{
                FormRow row = new FormRow();
                row.setProperty("show", "false");
                rows.add(row);
                
                db.closeConnection();
                return rows;
            }
            
            HashMap empHm = db.selectOneRecord(
                    "SELECT c_emp_status, c_data_status, c_mycoid, c_comp_name FROM "+Constants.TABLE.EMPREG
                    +" WHERE id=? ",
                    new String[]{empId}
            );
            
            String emp_status = "INACTIVE";
            
            if(empHm!=null){
                emp_status = empHm.get("c_emp_status").toString();
            }
            
            if(emp_status.equals("ACTIVE")){
                hm = db.selectOneRecord(
                    "SELECT * FROM app_fd_empm_disburse WHERE c_emp_fk = ?",
                        new String[]{empId}
                );
            }else if(!new CurrentUser().isAdmin()){
                FormRow row = new FormRow();
                row.setProperty("show", "false");                    
                rows.add(row);
                
                db.closeConnection();
                return rows;
            }            
            
            String disbId = "";
            if(hm!=null){
                disbId = hm.get("id").toString();
                rows = super.load(element, disbId, formData);
                FormRow row = rows.get(0);
                row.setProperty("show", "true");
            }else{
                
                if(empHm!=null){
                    String mycoid = empHm.get("c_mycoid").toString();
                    String comp_name = empHm.get("c_comp_name").toString();
//                    empId = empHm.get("id").toString();

                    FormRow row = new FormRow();
                    row.setProperty("mycoid", mycoid);
                    row.setProperty("comp_name", comp_name);
                    row.setProperty("emp_fk", empId);
                    row.setProperty("show", "true");
                    
                    rows.add(row);
                }else if(empHm==null && new CurrentUser().isAdmin()){
                    FormRow row = new FormRow();
                    row.setProperty("show", "admin");
                    rows.add(row);
                }else{
                    FormRow row = new FormRow();
                    row.setProperty("show", "false");
                    rows.add(row);
                }
                
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }
        
        return rows;
    }
    
    @Override
    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {         
        FormRow row =rows.get(0);
        
        final String empId = row.get("emp_fk").toString();
        
        if(StringUtils.isBlank(empId)){
            formData.addFormError("emp_fk", "Company Key Missing");
            return rows;
        }
        
        DBHandler db = new DBHandler();
        String disbId = "";
        try{
            db.openConnection();
            HashMap hm = db.selectOneRecord(
                    "SELECT id FROM app_fd_empm_disburse WHERE c_emp_fk = ?",
                    new String[]{empId});
            
            disbId = hm!=null?hm.get("id").toString():"";
        }catch(Exception e){
            
        }finally{
            db.closeConnection();
        }
        
        if(!disbId.isEmpty()){
            row.setId(disbId);
        }
        rows = super.store(element, rows, formData);
        
        if(StringUtils.isBlank(empId)){
            return rows;
        }
        
        Thread checkingThread = new PluginThread(new Runnable() {
            @Override
            public void run() {
                sendUpdateToAPS(empId);
            }
        });

        checkingThread.setDaemon(true);
        checkingThread.start();
        
        return rows;        
    }
    
    public void sendUpdateToAPS(String empId) {
        DBHandler db = new DBHandler();
        APIManager mgr = new APIManager(APIManager.APIType.APS);
        try{
            db.openConnection();
            
            JSONObject jsonBody = buildJSONbody(db, empId);
            
            HashMap header = new HashMap();
            header.put("api_id", mgr.getApi_id());
            header.put("api_key", mgr.getApi_key());
            
            HttpUtil http = new HttpUtil();
            http.setHeader(header);
            http.setBody(jsonBody);
            http.sendPostRequest(Constants.APS.APS_PROFILE_REG);
            
            JSONObject resp_data = http.getJSONResponse();
            int statusCode = http.getStatusCode();
            
            msg("DATA SENT TO APS , Status Code :"+Integer.toString(statusCode)
                    + ", Response: "+resp_data.toString());
        
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }
    }
    
    public JSONObject buildJSONbody(DBHandler db, String empId) throws JSONException {
        
        EmpmObj emp = new EmpmObj(db, EmpmObj.BY_ID, empId);
        
        JSONObject profile_service = new JSONObject();
        JSONObject profile = new JSONObject();
        
        HashMap contactPerson = emp.getOneContactPerson();
        
        String cpName = "";
        String cpEmail = "";
        String cpDesg = "";
        String cpTel = "";
        
        if(contactPerson !=null){
            cpName = contactPerson.get("c_name")==null?"":contactPerson.get("c_name").toString();
            cpDesg = contactPerson.get("c_designation")==null?"":contactPerson.get("c_designation").toString();
            cpEmail = contactPerson.get("c_email")==null?"":contactPerson.get("c_email").toString();
            cpTel = contactPerson.get("c_tel_no")==null?"":contactPerson.get("c_tel_no").toString();
        }
        
        profile.put("status", Constants.APS.ACTIVE);
        profile.put("customer_type", Constants.APS.CUSTYPE.EMPR);
        profile.put("my_co_id", emp.getMycoid());
        profile.put("ic_number", "");
        profile.put("country", emp.getBusinessCountryName());
        profile.put("name", emp.getCompName());
        profile.put("address1", emp.getBusinessAddress1());
        profile.put("address2", emp.getBusinessAddress2());
        profile.put("address3", emp.getBusinessAddress3());
        profile.put("city", emp.getBusinessCityName());
        profile.put("state", emp.getBusinessStateName());
        profile.put("postcode", emp.getBusinessPostcode());
        profile.put("email", emp.getPrimaryEmail());
        profile.put("tel_num", emp.getField("c_empl_tel_no_pri")
                        .replace("-", "")
                        .replace(" ", "")
                        .replace("+", ""));
        profile.put("fax_num", "");
        profile.put("occupation", cpDesg);
        profile.put("contact_person_name", cpName);
        profile.put("contact_person_email", cpEmail);
        profile.put("contact_person_designation", cpDesg);
        profile.put("branch", (emp.isHQ()?"HQ":"BR"));
        profile.put("sector", emp.getSubSectorName());
        profile.put("liability_date", 
                CommonUtils.set_DT_ChangeDateFormatString(emp.getField("c_levy_liab_pymnt_dt"), 
                        "dd-MM-yyyy")
        );
        profile.put("cessation_date", "");
        profile.put("remarks", Constants.APS.TXNTYPE.CREATE);
        profile.put("partner_my_co_id", "");
        profile.put("effective_date", 
                CommonUtils.set_DT_ChangeDateFormatString(emp.getField("dateCreated"), 
                        "dd-MM-yyyy")
        );
        profile.put("transaction_type", Constants.APS.TXNTYPE.CREATE);
        profile.put("int_app_id", "EMPM_"+
                AppUtil.processHashVariable("#date.yyyyMMdd#", null, null, null)+
                "_"+
                CommonUtils.getAPSITGCode(db)
        );
        
        HashMap disbHm = db.selectOneRecord(
                "select \n" +
                "d.c_bank_acc_no,\n" +
                "b.c_swift_code,\n" +
                "b.c_bank_name\n" +
                "from app_fd_empm_disburse d\n" +
                "INNER JOIN app_fd_stp_bank b ON b.id = d.c_bankId "
                        + "WHERE c_emp_fk = ?",
                new String[]{empId}
        );
        
        String bank_acc_no = "";
        String bank_name = "";
        String bank_swift_code = "";
        
        if(disbHm != null){
            bank_acc_no = disbHm.get("c_bank_acc_no").toString();
            bank_name = disbHm.get("c_bank_name").toString();
            bank_swift_code = disbHm.get("c_swift_code").toString();
        }
        
        profile.put("payment_mode", "");
        profile.put("bank_swift_code", bank_swift_code);
        profile.put("bank_account_number", bank_acc_no);
        profile.put("bank_modified_on_date", CommonUtils.get_DT_CurrentDateTime("dd-MM-yyyy"));
        profile.put("bank_name", bank_name);
        profile.put("sst_registered", "NO");
        profile.put("sst_reg_number", "");        
        
        profile_service.put("profile", profile);
        
        return new JSONObject().put("profile_service", profile_service);
    }
    
}
