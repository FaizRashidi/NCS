/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.util.HashMap;
import java.util.Optional;
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
public class EnforcementBinder extends WorkflowFormBinder{
    
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
        return "Binder for Enforcement Flow";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Potential Employer Enforcement Binder";
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
    
    /*
    TODO
    update pe status - last move- enforcement
    email (?)
    audit trail possibly
    */
    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {        
        
        rows = super.store(element, rows, formData);
        
        String formId = super.getFormId();
        String id = rows.get(0).getId();
        DBHandler db = new DBHandler();
        
        try{            
            db.openConnection();
            
            switch(formId){
                case Constants.FORM_ID.POTEMP_ENFORCEMENT_SUBMISSION:
                    updateEmpStatus(db, id, Constants.LAST_MOVEMENT.COMPLAINT_TO_ENFORCEMENT);
                    rows.get(0).put("status", Constants.STATUS.ENF_STATUS.COMPLAINT_TO_ENFORCEMENT);
                break;
            }
        }catch(Exception e){
            
        }finally{
            db.closeConnection();
        }
        
        return rows;        
    }

    private void updateEmpStatus(DBHandler db, String id, String status) {
        
        HashMap hm = db.selectOneRecord(
                                "SELECT r.id FROM "+Constants.TABLE.POT_EMP_ENFORCEMENT+" enf "
                                + " INNER JOIN "+Constants.TABLE.POT_EMP+" pe ON pe.id = enf.c_pe_fk "
                                + " INNER JOIN "+Constants.TABLE.EMPREG+" r ON r.id = pe.c_emp_fk "
                                + " WHERE enf.id = ? ",
                                new String[]{id});
        
        if(hm==null){
            return;
        }
        
        String empId = hm.get("id").toString();
        
        db.update(
                "UPDATE "+Constants.TABLE.EMPREG+" SET c_last_move = ? WHERE id = ?",
                new String[]{status},
                new String[]{empId});
    }
    
    @Override
    public FormRowSet load(Element element, String id, FormData formData) {
        
        String complId = formData.getRequestParameter("complId");
        
        if(!StringUtils.isBlank(id)){
            WorkflowFormBinder binder = new WorkflowFormBinder();
            return binder.load(element, complId, formData);
        }
        
        DBHandler db = new DBHandler();
        FormRowSet rows = new FormRowSet();
        
        try{
            db.openConnection();
            HashMap empHm = db.selectOneRecord(
                    "select c.dateCreated,\n" +
                    "r.id, c.id, r.c_mycoid, r.c_comp_name, c.c_complaint_no, \n" +
                    "r.c_empl_address, r.c_empl_address2, r.c_empl_address3,\n" +
                    "r.c_empl_email_pri, r.c_empl_tel_no_pri, \n" +
                    "r.c_total_my_empl, r.c_total_non_my_empl,\n" +
                    "r.c_empl_city, r.c_empl_state, \n" +
                    "r.c_last_move, r.c_activity \n" +
                    "from app_fd_empm_pe_compl_enf c\n" +
                    "inner join app_fd_empm_pe_potEmp p on p.id = c.c_pe_fk\n" +
                    "inner join app_fd_empm_reg r on r.id = p.c_emp_fk " +
                    " WHERE c.id = ? ",
                    new String[]{complId}
            );
            
            if(empHm==null){
                db.closeConnection();
                return rows;
            }
            
            String comp_name = empHm.get("c_comp_name").toString();
            String mycoid = empHm.get("c_mycoid").toString();
            String address1 = empHm.get("c_empl_address").toString();
            String address2 = empHm.get("c_empl_address2").toString();
            String address3 = empHm.get("c_empl_address3").toString();
            String email = empHm.get("c_empl_email_pri").toString();
            String telno = empHm.get("c_empl_tel_no_pri").toString();
            String totalMy = empHm.get("c_total_my_empl").toString();
            String totalNonMy = empHm.get("c_total_non_my_empl").toString();
            String state = empHm.get("c_empl_state").toString();            
            String city = empHm.get("c_empl_city").toString();
            String empId = empHm.get("id").toString();
            String activity = empHm.get("activity").toString();
            
            FormRow row = new FormRow();
            row.put("total_empl_non_my", totalNonMy);
            row.put("total_empl_my", totalMy);
            row.put("comp_activity", activity);
            row.put("base_pay_my", state);
            row.put("comp_telno", telno);
            row.put("comp_email", email);
            row.put("comp_address3", address3);
            row.put("comp_address2", address2);
            row.put("comp_address1", address1);
            row.put("comp_mycoid", mycoid);
            row.put("comp_name", comp_name);
            row.put("empId", empId);
            row.put("complain_fk", complId);
            
            rows.add(row);
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }
        return rows;        
    }
    
}
