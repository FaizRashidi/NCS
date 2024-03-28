/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.dao.APIManager;
import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.datalistAction.DeleteUserAndRecord;
import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.B2CUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.EmpModuleSetup;
import com.tms.hrdc.util.HttpUtil;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class DeregisterTool extends DefaultApplicationPlugin{

    @Override
    public String getName() {
        return this.getClassName();
    }

    @Override
    public String getVersion() {
        return "2.0.0";
    }

    @Override
    public String getDescription() {
        return "To deregister an employer. Removed scheduler to deregister user";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Employer Deregister Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }
    
    private void msg(String msg){
        LogUtil.info(this.getClass().getName(), msg);
    }

    @Override
    public String getPropertyOptions() {
        return 
            "[{\n" +
            "    title : '"+getLabel()+"',\n" +
            "    properties : [{\n" +
            "            name:\"type\",\n" +
            "            label: \"Status\",\n" +
            "            type:\"SelectBox\",\n" +
            "            required : \"true\",            \n" +
            "            options : [\n" +
            "                {value: 'REJECT', label : 'Rejected'},\n" +
            "                {value: 'DEREGISTER', label : 'Deregister'},\n" +
            "                {value: 'DEREGISTER_REAL_SCH', label : 'Deregister Cancel Scheduler'},\n" +
            "                {value: 'MERGE', label : 'Merge'},\n" +
            "                {value: 'F5_RESPONDED', label : 'Form 5 Responded'}\n" +
            "            ]\n" +
            "        }" +
            "    ] " +
            "}]";
    }
    
    PluginManager pm = null;
    WorkflowManager wm = null;
    WorkflowAssignment wfAssignment = null;

    @Override
    public Object execute(Map props) {
        
        String id = "";
        String status_form5 = "";
        String status = "";
        String type = props.get("type").toString();  
        DBHandler db = new DBHandler();

        msg("DEREGISTER TOOL type "+type);
        
        try{
            db.openConnection();

            wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");
            pm =  (PluginManager)props.get("pluginManager");
            wm = (WorkflowManager) pm.getBean("workflowManager");
            AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");

            id = appService.getOriginProcessId(wfAssignment.getProcessId());
            status_form5 = wm.getProcessVariable(wfAssignment.getProcessId(), "status_form5");
            status = wm.getProcessVariable(wfAssignment.getProcessId(), "status");
            
            String sql = "SELECT * FROM "+Constants.TABLE.DEREG+" WHERE id = ?";
            HashMap drgHm = db.selectOneRecord(sql, new String[]{id});
                        
            if(drgHm==null){
                LogUtil.info(this.getClassName(), "No Dereg Data");
                db.closeConnection();
                return null;
            }
            
            String mergeEmpId = drgHm.get("c_merge_comp_id")==null?"":drgHm.get("c_merge_comp_id").toString();
            String empId = drgHm.get("c_dreg_emp_id")==null?"":drgHm.get("c_dreg_emp_id").toString();
//            String empId =  CommonUtils.getEmpId_empDereg(db, id);           
            EmpmObj emp = new EmpmObj(db, EmpmObj.BY_ID, empId);
            
            msg( type+" "+empId+" into "+mergeEmpId);
            
            switch(type){
                case "REJECT":
                    
                    if(!mergeEmpId.isEmpty()){
//                        abortForm5Process(db,wfAssignment.getProcessId());

                        sql = "UPDATE "+Constants.TABLE.EMPREG+" SET c_data_status = ? "
                                + "WHERE id = ?";
                        int i = db.update(sql, 
                                new String[]{Constants.STATUS.EMP.REGISTER_APPROVED},
                                new String[]{mergeEmpId}
                        );
                        
                        String modifiedBy = drgHm.get("modifiedByName").toString();
                        ArrayList list = new ArrayList();
                        new AuditTrailUtil().insertAuditTrail2(db, mergeEmpId, modifiedBy, "Form 5 Rejected",
                                    "Form 5 rejected because of Form 4 was rejected", false, list);
                    }
                    
                break;
                
                case "DEREGISTER":                    
//                    DeleteUserAndRecord.disableUser(db, emp.getMycoid());
                    doLevyTransferAPI(db, id, emp.getHrdcNo(), mergeEmpId, false);
                    disableUser(db, emp);

                    new AuditTrailUtil().insertAuditTrail2(
                            db, empId, "SYSTEM",
                            "User Login Disabled", "User accounts and/or subaccounts disabled after deregistration",
                            false, null
                    );
                break;
                
                case "MERGE":
                    LogUtil.info(this.getClassName(), "Merging company "+empId+" into "+mergeEmpId);
                    HashMap hm = new HashMap();
                    hm.put("comp_merge_mycoid", emp.getMycoid());
                    hm.put("comp_merge_name", emp.getCompName());
                    hm.put("comp_merge_id", empId);
                    hm.put("fk", mergeEmpId);

                    CommonUtils.saveUpdateForm2("", 
                            Constants.FORM_ID.COMPANY_MERGE, "", hm);
                break;
                
                case "DEREGISTER_CANCEL":                    
//                    DeleteUserAndRecord.disableUser(db, emp.getMycoid());
                    doLevyTransferAPI(db, id, emp.getHrdcNo(), "", true);
                break;
                
                 case "F5_RESPONDED":                    
//                    DeleteUserAndRecord.disableUser(db, emp.getMycoid());
//                    doLevyTransferAPI(db, id, emp.getHrdcNo(), "", true);
                    startForm5ApprovalActivity(db, wm, wfAssignment.getProcessId());
                break;
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }        
        
        return null;
    }

    private void abortForm5Process(DBHandler db, String processId) {
        String sql = "select ActivityId from SHKAssignmentsTable \n" +
                    "WHERE ActivityProcessId = ? " +
                    "and ActivityId like '%form5%'";
        ArrayList<HashMap<String, String>> list = db.select(sql, new String[]{processId});
                
        for(HashMap hm:list){
            String actId = hm.get("ActivityId").toString();
            wm.activityAbort(processId, actId);
        }
    }

    public void disableUser(DBHandler db, EmpmObj emp) throws JSONException, IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

        HttpUtil http = new HttpUtil();
        APIManager mgr = new APIManager(APIManager.APIType.APS);
        B2CUtil b2c = new B2CUtil(db, http, mgr);

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("accountEnabled", false);

//        b2c.deleteB2CUser(emp.getMycoid());
        b2c.updateB2CUser(emp.getMycoid(), jsonBody);
        DeleteUserAndRecord.disableUser(db, emp.getMycoid());

        //TODO query get all subaccounts
        ArrayList<HashMap<String, String>> subUserList = db.select(
                "SELECT c_email FROM app_fd_empm_persons_stp WHERE c_compId = ?",
                new String[]{emp.getId()});
        for(HashMap user:subUserList){
            String email = user.get("c_email").toString();
//            b2c.updateB2CUser(email, jsonBody);
            b2c.deleteB2CUser(email);
            DeleteUserAndRecord.disableUser(db, email);
        }
    }
    
    public static void doLevyTransferAPI(DBHandler db, String dereg_id,
            String dregHrdcNo, String mergeId, boolean isCancel) throws JSONException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        
        String sql = "select "
                + "id, c_form_type, d.c_dreg_reason, d.c_approve_dt \n" +
                "from app_fd_empm_dereg d "
                + "WHERE id = ? ";
        
        HashMap hm = db.selectOneRecord(
                    sql,
                    new String[]{dereg_id}
                );
        
        if(hm==null){
            return;
        }
        
        String form_type = hm!=null?hm.get("c_form_type").toString():"";
        String dreg_reason = hm!=null?hm.get("c_dreg_reason").toString():"";
        String dateCreated = hm!=null?hm.get("c_approve_dt").toString():"";
        
        JSONObject body = new JSONObject();
        
        String reason = "CEASE OPERATION";
        String action = "4";
        String url = Constants.LEVYAPI.LEVY_TRANSFER;
        
        if(!StringUtils.isBlank(mergeId)){
            reason = "MERGE";
            EmpmObj eoM = new EmpmObj(db, EmpmObj.BY_ID, mergeId);
            
            body.put("mycoid_merge", eoM.getHrdcNo());
        }
        if(form_type.equals("4A")){
            reason = "LESS THAN 10 EMPLOYEES";
            action = "4a";
        }       
        
        if(isCancel){
            action = "cancel";
            reason = "Cancel";
        }
        
        url = url+"?action="+action;
        
        body.put("id_mycoid", dregHrdcNo);
        body.put("submission_date", dateCreated);
        body.put("reason", reason);
        
        HttpUtil http = new HttpUtil();
        
        http.setBody(body);
        http.sendGetRequest(url);
        
        JSONObject resp_data = http.getJSONResponse();
        int statusCode = http.getStatusCode();

        LogUtil.info("LEVY TRANSFER API CALLED "+url, 
                "Req Body: "+body.toString()+
                ", Status Code :"+Integer.toString(statusCode)
                + ", Response: "+resp_data.toString());
    }    

    private void startForm5ApprovalActivity(DBHandler db, WorkflowManager wm, String processId) {        
        //reset main dereg wfv and put to f5
        
        HashMap<String, String> hm = db.selectOneRecord(
                "SELECT distinct a.ActivityId  FROM SHKAssignmentsTable a \n" +
//                "INNER JOIN wf_process_link b\n" +
//                "ON a.ActivityProcessId = b.processId \n" +
                "WHERE ActivityProcessId = ?\n",
//                "AND ActivityProcessId LIKE '%empm_emp_deregistration'", 
                new String[]{processId}
        );

//        String processId = "";
        String actId = "";
        if(hm!=null){

//            pm("MAIN DEREG "+hm.toString());

//            processId = hm.get("processId").toString();
            actId = hm.get("ActivityId").toString();

            String actDef = "";
            if(actId.endsWith("dereg_verify_form4")){
                actDef = "dereg_verify_form4_m";
            }

            wm.activityStart(processId, actDef, true);
            hm = db.selectOneRecord(
                    "SELECT distinct a.ActivityId FROM SHKAssignmentsTable a \n" +
                    "WHERE ActivityProcessId = ?\n" +
                    "AND a.ActivityId LIKE '%"+actDef+"'", 
                    new String[]{processId}
            );

            if(hm!=null){
                actId = hm.get("ActivityId").toString();
                wm.activityVariable(actId, "require_form5", "Yes");
            }

            wm.activityAbort(processId, actDef+"_m");
        }else{
            msg("COULNDT FIND MAIN DEREG ID");
        }
    }
}
