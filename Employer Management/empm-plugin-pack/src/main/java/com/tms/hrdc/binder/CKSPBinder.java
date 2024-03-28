/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.util.DBHandler;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author kahyi
 */
public class CKSPBinder extends WorkflowFormBinder {
        
    @Override
    public String getName() {
        return "CKSPBinder";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "To load officer details";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - CKSP Binder";
    }
    
     @Override
    public FormRowSet load(Element element, String id, FormData formData) {
 
        // load existing record - just load from table according to record id 
       
        if (id == null|| id.equals("")){
        
            WorkflowFormBinder binder = new WorkflowFormBinder();
            FormRowSet rows = new FormRowSet();
            DBHandler db = new DBHandler();
            try{
                db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
                FormRow row = new FormRow();
                String username= AppUtil.processHashVariable("#currentUser.username#", null, null, null);
                String peID = AppUtil.processHashVariable("#requestParam.peID#", null, null, null);
                LogUtil.info("username and peID get: ",username+ " | "+peID);
                String find_reg_id = "select c_emp_fk from app_fd_empm_pe_potEmp where id = ?";
                
                HashMap<String, String> regID_hm = db.selectOneRecord(find_reg_id,new String[]{peID});
                String regID = regID_hm.get("c_emp_fk").toString();               
                

                String query = "select * from app_fd_stp_hrdc_usr where c_username = ? ";             
                HashMap<String, String> hm = db.selectOneRecord(query,new String[]{username});
                
                if(hm!=null){
                    row.setProperty("pe_fk", peID);
                    row.setProperty("cmpl_username", username);
                    row.setProperty("cmpl_name", hm.get("c_firstName").toString()+ " "+ hm.get("c_lastName").toString());            
                    row.setProperty("cmpl_ic_no", hm.get("c_ic_no").toString());          
                    row.setProperty("cmpl_office_add_1", hm.get("c_office_add_1").toString());         
                    row.setProperty("cmpl_office_add_2", hm.get("c_office_add_2").toString());
                    row.setProperty("cmpl_office_add_3", hm.get("c_office_add_3").toString());
                    row.setProperty("cmpl_office_postcode", hm.get("c_office_postcode").toString());
                    row.setProperty("cmpl_office_city", hm.get("c_office_city").toString());
                    row.setProperty("cmpl_office_state", hm.get("c_office_state").toString());           
                    row.setProperty("cmpl_office_country", hm.get("c_office_country").toString());         
                    row.setProperty("cmpl_office_telephone_no", hm.get("c_office_telephone_mobile_no").toString());           
                    row.setProperty("cmpl_office_fax_no", hm.get("c_fax_no").toString());            
                    row.setProperty("cmpl_designation", hm.get("c_desg").toString());
                    row.setProperty("cmpl_home_add_1", hm.get("c_home_add_1").toString());
                    row.setProperty("cmpl_home_add_2", hm.get("c_home_add_2").toString());
                    row.setProperty("cmpl_home_add_3", hm.get("c_home_add_3").toString());
                    row.setProperty("cmpl_home_postcode", hm.get("c_home_postcode").toString());
                    row.setProperty("cmpl_home_city", hm.get("c_home_city").toString());
                    row.setProperty("cmpl_home_state", hm.get("c_home_state").toString());
                    row.setProperty("cmpl_home_country", hm.get("c_home_country").toString());
                    row.setProperty("cmpl_home_telephone_no", hm.get("c_home_telephone_mobile_no").toString());
                }
                
                
                String companyDetails_query = "select * from app_fd_empm_reg where id = ?";
                HashMap<String, String> companyDetails_hm = db.selectOneRecord(companyDetails_query,new String[]{regID});
                
                if(companyDetails_hm!=null){
                    row.setProperty("employer_status", companyDetails_hm.get("c_last_move").toString());          
                    row.setProperty("mycoid", companyDetails_hm.get("c_mycoid").toString());         
                    row.setProperty("company_name", companyDetails_hm.get("c_comp_name").toString());
                    row.setProperty("recp_office_add_1", companyDetails_hm.get("c_empl_address").toString());         
                    row.setProperty("recp_office_add_2", companyDetails_hm.get("c_empl_address2").toString());
                    row.setProperty("recp_office_add_3", companyDetails_hm.get("c_empl_address3").toString());            
                    row.setProperty("recp_office_postcode", companyDetails_hm.get("c_empl_postcode").toString());
                    row.setProperty("recp_office_city", companyDetails_hm.get("c_empl_city").toString());
                    row.setProperty("recp_office_state", companyDetails_hm.get("c_empl_state").toString());
                    row.setProperty("recp_home_country", companyDetails_hm.get("c_empl_country").toString());
                }
                
                
                Date date = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");  
                String strDate= formatter.format(date);  
                
                row.setProperty("complaint_date",strDate);                        
                
                rows.add(row);

            }
            catch(Exception e){
                e.printStackTrace();
            }
            finally{
                db.closeConnection();
            }

            return rows;
        }
        else{
            LogUtil.info("CKSP Binder ", "loading "+id);
            return super.load(element, id, formData);
        }
        
    }
}
