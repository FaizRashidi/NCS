/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.dao.APIManager;
import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.CriteriaUtil;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.HttpUtil;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class CriteriaChecker extends DefaultApplicationPlugin{

    @Override
    public String getName() {
        return this.getClassName();
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "To check criteria";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Criteria Chekcer";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/abc.json", null, true);
    }
    
    String levy_perc = "0";
    String levy_class_code = "";
    String isSME = "";
    String smeType = "";
    
    @Override
    public Object execute(Map props) {
        
        WorkflowAssignment wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");     
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        String id = appService.getOriginProcessId(wfAssignment.getProcessId());
        
        LogUtil.info("== EMP-REG","Criteria Checker Tool Start "+id);
        
        boolean is_ngo = true;
        boolean is_crit_meet = false;
        
        DBHandler db = new DBHandler();
        
        try{
            db.openConnection((DataSource)AppUtil.getApplicationContext().getBean("setupDataSource"));
            
            id = CommonUtils.getEmpId_empReg(db, id);
            
            EmpmObj emp = new EmpmObj(db,EmpmObj.BY_ID, id);
            CriteriaUtil cut = new CriteriaUtil(db);
            
            String num_emp = emp.getEmployerNumber();
            String ind_code = emp.getIndustrySector();
            String sme_categ = emp.getSMECategId();
            String org_type = emp.getOrgType();
            String emp_state = emp.getBusinessState();
            
            boolean isNGO = false;
            if(org_type.equals("ROS")){
                isNGO = true;
            }
            
            // isSME sme_category  sme_type
            getSMEType(db, num_emp, sme_categ);
            getLevyPerc(num_emp, ind_code, isNGO);
            getLevyClassCodeLabel(db);
            String stateCode = getSetupStateCode(db, emp_state);

            int i = db.update("UPDATE "+Constants.TABLE.EMPREG+
                    " SET c_levy_perc = ?, c_code_classification = ?,"
                    + "c_isSME = ?, c_sme_type = ?, c_state_code = ? "
                    + "WHERE id = ? ", 
                    new String[]{levy_perc, levy_class_code, isSME, smeType, stateCode},
                    new String[]{id});
            
            is_crit_meet = cut.isPassCriteria(emp);
            String reason = cut.getMessage();

            PluginManager pluginManager = (PluginManager) props.get("pluginManager");
            WorkflowManager workflowManager = (WorkflowManager) pluginManager.getBean("workflowManager");
            workflowManager.activityVariable(wfAssignment.getActivityId(), "criteria_meet", Boolean.toString(is_crit_meet));      
            workflowManager.activityVariable(wfAssignment.getActivityId(), "reject_reason", reason);      
                
        
        }catch(Exception e){
            LogUtil.info(getClassName(), "Can't Open Connection");
            e.printStackTrace();
        }finally{
            
            LogUtil.info("== EMP-REG","Criteria Checker Tool End "+id);
            
            db.closeConnection();
            return null;
        }        
    }

    private String getSetupStateCode(DBHandler db, String empState) {
        HashMap hm = db.selectOneRecord(
                "SELECT id , c_state_code, s.c_state FROM app_fd_empm_enf_st_cde_stp s WHERE s.c_state = ? ",
                new String[]{empState}
        );

        String stateCode = hm==null?"":hm.getOrDefault("id","").toString();

        return stateCode;
    }

    private void updateLevyPerc(DBHandler db, String id, String levy_perc) {
        String query = "UPDATE app_fd_empm_reg SET c_levy_perc = ? WHERE id = ?";
        String[] val = {levy_perc};
        String[] cond = {id};
        
        db.update(query, val, cond);
    }
    
    public String getLevyPerc(String num_emp, String ind_code, boolean isNGO) throws JSONException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException{
        
        HttpUtil httpUtil = new HttpUtil();
//        APIManager mgr = new APIManager(APIManager.APIType.LEVY);
        String encoding = Base64.getEncoder().encodeToString(
                (Constants.API.JOGETAPI.MASTERAPILOGIN + ":" + Constants.API.JOGETAPI.MASTERAPIPW).getBytes()
        );
        
        HashMap header = new HashMap();
        header.put(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
//        header.put("api_key", mgr.getApi_key());
                
        String url = Constants.LEVYAPI.LEVY_PERCENTAGE
                + "?industrySectorId="+ind_code
                +"&noOfEmployees="+num_emp
                +"&isNGO="+Boolean.valueOf(isNGO);
        
        httpUtil.setHeader(header);        
        httpUtil.sendPostRequest(url);
        if(httpUtil.getStatusCode() != 200){
            return "0";
        }
        
        JSONObject resp_data = httpUtil.getJSONResponse();
        
        if(resp_data == null){            
            return "0";
        }
        
        JSONArray data_array = resp_data.getJSONArray("data");
        if(data_array.length() > 0){
            JSONObject obj = (JSONObject) data_array.get(0);
            levy_perc = obj.get("levy_percent") == null?"0":obj.getString("levy_percent");
            levy_class_code = obj.get("class_code_id") == null?"0":obj.getString("class_code_id");
        }
        
        return "0";
    }

    private void getSMEType(DBHandler db, String num_emp, String smecateg) {
        
        String query = "SELECT * FROM app_fd_stp_sme " +
                        "WHERE c_min_emp < "+num_emp+" AND c_max_emp > "+num_emp+" " +
                        "AND c_smeCategory = ?";
        HashMap data = db.selectOneRecord(query, new String[]{smecateg});
        
        if(data == null){
            
        }else{
            LogUtil.info("Criteria", "sme categ "+smecateg+" num_emp "+num_emp+" query "+query+", hashmap "+data.toString());
            smeType = data.get("c_smeType") == null?
                    "":
                    data.get("c_smeType").toString();
            isSME = "Yes";
        }  
    }

    private void getLevyClassCodeLabel(DBHandler db) {
        HashMap hm = new HashMap();
        try{
            hm = db.selectOneRecord(
                    "SELECT * FROM app_fd_levm_stp_class_code WHERE id = ?",
                    new String[]{levy_class_code}
            );
        }catch(Exception e){
            LogUtil.info("Table not exist ", "LEVY! app_fd_levm_stp_class_code");
        }
        
        if(hm!=null){
            levy_class_code = hm.get("c_title").toString();
        }
    }
    
}
