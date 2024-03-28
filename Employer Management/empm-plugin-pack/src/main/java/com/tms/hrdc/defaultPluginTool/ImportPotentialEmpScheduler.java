/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.EmpModuleSetup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;

/**
 *
 * @author faizr
 */
public class ImportPotentialEmpScheduler extends DefaultApplicationPlugin{

    @Override
    public String getName() {
        return "Insert Emp. Data to Potential Tool";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "To insert data of rejected or deregistered employers into list of potential employers";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Potential Emp. Data Inserter Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return
                "[{\n" +
                "    title : '"+getLabel()+"',\n" +
                "    properties : [{\n" +
                "            name:\"isTest\",\n" +
                "            label: \"Ts Test\",\n" +
                "            type:\"SelectBox\",\n" +
                "            options : [\n" +
                "                {value: 'true', label : 'Yes'},\n" +
                "                {value: 'false', label : 'No'}\n" +
                "            ]\n" +
                "        }] " +
                "}]";
    }
    
    int recordCount = 0;
    
    @Override
    public Object execute(Map props) {
        //emp that has been rejected/deregistered for x(setup) will be inserted 
        // to pot_emp
        
        String isTest = props.get("isTest")==null?"":props.get("isTest").toString();
        
        DBHandler db = new DBHandler();
        
        try{
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
            
            if(!isTest.isEmpty() && isTest.equals("true")){
                
                LogUtil.info(this.getClass().toString(),"Updating temp data started");
                
                ArrayList<HashMap<String, String>> activeEmp = db.select(
                        "SELECT id FROM "+Constants.TABLE.EMPREG+" WHERE c_emp_status = ?",
                        new String[]{"ACTIVE"}
                );
                
                EmpmObj eo;
                int count = 0;
                
                for(HashMap ids:activeEmp){
                    String id = ids.getOrDefault("id", "").toString();
                    
                    if(id.isEmpty()){
                        continue;
                    }
                    
                    eo = new EmpmObj(db, EmpmObj.BY_ID, id);
                    AuditTrailUtil.saveTempData(db, eo);
                    count++;
                }
                
                LogUtil.info(this.getClass().toString(),"Updating temp data completed, Records updated/inserted: "+Integer.toString(count));
                
                db.closeConnection();
                return null;
            }
            
            EmpModuleSetup stp = new EmpModuleSetup(db);
            String periodToPeDays = stp.getPeriodToPE();
            int periodToPeDays_int = 0;   
            
            ArrayList rejectedEmp = getRejectedEmpAppl(db, periodToPeDays);
            ArrayList deregedEmp = getDeregEmp(db, periodToPeDays);
            
            rejectedEmp = formalize(rejectedEmp, Constants.FLOW_TYPE.EMP_REG);
            
            insertIntoPotentialEmp(rejectedEmp);
            insertIntoPotentialEmp(deregedEmp);
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }
        
        return null;
    }

    private String getPendingImportPeriod(DBHandler db) {
        String query = "SELECT c_import_pe_days FROM app_fd_empm_reg_stp h WHERE id = 'epm_stp'";
        HashMap hm = db.selectOneRecord(query);
        
        return hm.get("c_import_pe_days")==null?"0":hm.get("c_import_pe_days").toString();
    }

    private ArrayList getRejectedEmpAppl(DBHandler db, String days) {
        String query = "SELECT * FROM app_fd_empm_reg r WHERE \n" +
                        "CURRENT_TIMESTAMP() > \n" +
                        "DATE_ADD(STR_TO_DATE(c_approve_dt, '%Y-%m-%d %H:%i:%s'), INTERVAL "+days+" DAY)\n" +
                        "AND r.c_flow_status = 'Rejected'\n" +
                        "AND not EXISTS(\n" +
                        "	SELECT * FROM app_fd_empm_pe_pot_empl j WHERE j.c_hrdc_no = r.c_hrdc_no\n" +
                        ")";
        return db.select(query);
    }

    private ArrayList getDeregEmp(DBHandler db, String days) {
        String query = "SELECT  e.* FROM app_fd_empm_dereg r \n" +
                        "inner JOIN app_fd_empm_reg e ON e.id = r.c_dreg_emp_id\n" +
                        "WHERE\n" +
                        "CURRENT_TIMESTAMP() > \n" +
                        "DATE_ADD(STR_TO_DATE(r.c_approve_dt, '%Y-%m-%d %H:%i:%s'), INTERVAL "+days+" DAY)\n" +
                        "AND r.c_flow_status = 'Approved'\n" +
                        "AND not EXISTS(\n" +
                        "	SELECT * FROM app_fd_empm_pe_pot_empl j WHERE j.c_hrdc_no = e.c_hrdc_no\n" +
                        ");";
        return db.select(query);
    }

    private void insertIntoPotentialEmp(ArrayList<HashMap<String, String>> rejectedEmp) {
        HashMap empData;
        
        //check by employer ID - if exist update status to TRUE_POTENTIAL
        //else, create new
        for(HashMap hm:rejectedEmp){
            
            empData = new HashMap();
            empData.put("mycoid",hm.get("c_mycoid"));
            empData.put("business_entity",hm.get(""));
            empData.put("sub_business_entity",hm.get(""));
            empData.put("business_nature",hm.get(""));
            empData.put("total_employees",hm.get(""));
            empData.put("comp_name",hm.get(""));    
            empData.put("comp_email",hm.get(""));
            empData.put("contact_no",hm.get(""));
            empData.put("tel_no",hm.get(""));
            empData.put("fax_no",hm.get(""));
            empData.put("postcode",hm.get(""));
            empData.put("country_code",hm.get(""));
            empData.put("country",hm.get(""));
            empData.put("state",hm.get(""));
            empData.put("city",hm.get(""));
            empData.put("address",hm.get(""));
            empData.put("industry_sector_id",hm.get(""));
            empData.put("industry_sector",hm.get(""));
            empData.put("sector_code_id",hm.get(""));
            empData.put("sector_code",hm.get(""));    
            empData.put("sector_desc",hm.get(""));
            empData.put("hrdc_no",hm.get("hrdc_no"));
            
            CommonUtils.saveUpdateForm2("", "potential_acc_form", hm.get("id").toString(), empData);
        }     
    }

    private ArrayList formalize(ArrayList<HashMap<String, String>> data_list, String type) {
        return new ArrayList();
    }
    
}
