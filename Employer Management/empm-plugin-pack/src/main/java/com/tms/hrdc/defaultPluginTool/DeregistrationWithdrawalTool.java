/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.datalistAction.DeleteUserAndRecord;
import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
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

/**
 *
 * @author Kah Ying
 */
public class DeregistrationWithdrawalTool extends DefaultApplicationPlugin{

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
        return "Cancel Deregistration Process";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Employer Deregistration Withdrawal Tool";
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
            "            name:\"type\",\n" +
            "            label: \"Status\",\n" +
            "            type:\"SelectBox\",\n" +
            "            required : \"true\",            \n" +
            "            options : [\n" +
            "                {value: 'ABORT_DEREG', label : 'Abort Deregistration (Withdrawal Approved)'},\n" +
            "                {value: 'REVERT_DEREG', label : 'Revert Deregistration Status (Withdrawal Rejected)'},\n" +
            "                {value: 'WD_START', label : 'Update Deregistration'}\n" +
            "            ]\n" +
            "        }" +
            "    ] " +
            "}]";

    }
    
    public void msg(String msg){
        LogUtil.info(this.getClassName(), msg);
    }

    PluginManager pm = null;
    WorkflowManager wm = null;
    WorkflowAssignment wfAssignment = null;

    @Override
    public Object execute(Map props) {
        
        wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");     
        pm =  (PluginManager)props.get("pluginManager");
        wm = (WorkflowManager) pm.getBean("workflowManager");
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        
        String type = props.get("type").toString();
        String id = appService.getOriginProcessId(wfAssignment.getProcessId());
//        String procId = wfAssignment.getProcessId();
    
        DBHandler db = new DBHandler();
        
        try{
            db.openConnection();
            
            String deregId = "";
            String dereg_form_type = "";
            String dereg_wd_type = "";
            
            HashMap hm = db.selectOneRecord(
                    "SELECT * FROM "+Constants.TABLE.DEREG_WD+" WHERE id = ?",
                    new String[]{id});
            
            if(hm!=null){
                deregId = hm.getOrDefault("c_dereg_id","").toString();
                dereg_form_type = hm.getOrDefault("c_dereg_form_type","").toString();
                dereg_wd_type = hm.getOrDefault("c_wd_type","").toString();
            }
            
            HashMap deregHm = db.selectOneRecord(
                    "SELECT * FROM "+Constants.TABLE.DEREG+" WHERE id = ?",
                    new String[]{deregId}) ;
            
            if(deregHm==null){
                deregHm = db.selectOneRecord(
                    "SELECT * FROM "+Constants.TABLE.DEREG+" WHERE c_f5_fk = ?",
                    new String[]{deregId}) ;
            }
            
            if(deregHm!=null){
                deregId = deregHm.getOrDefault("id", "").toString();
            }
            
            switch(type){
                case "ABORT_DEREG":
                    doAbortDereg2(db, id, deregId, dereg_form_type, dereg_wd_type);
                break;
                case "REVERT_DEREG":
                    doRevertDereg(db, id, deregId, dereg_form_type, dereg_wd_type);
                break;
                case "WD_START":
                    doUpdateDereg(db, id,deregId, dereg_form_type, dereg_wd_type);
                break;
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }        
        
        return null;
    }
    
   

    private void doAbortDereg2(DBHandler db, String deregwdId, String deregId,
                              String dereg_wd_form_type, String dereg_wd_type) {

        msg("ABORT FORM "+dereg_wd_form_type+" - "+dereg_wd_type +" WD BINDER STARTS. ID "+deregwdId);
        WorkflowManager wm = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");

        HashMap<String, String> deregHm = db.selectOneRecord(
                "select c_dreg_emp_id, c_merge_comp_id,c_dereg_refno, c_f5_fk, "
                        + "c_form_type from app_fd_empm_dereg WHERE id=?",
                new String[]{deregId}
        );

        String sql = "";
        String f4RefNo = deregHm.getOrDefault("c_dereg_refno","");
        String f4EmpId = deregHm.getOrDefault("c_dreg_emp_id","");
        String f5EmpId = deregHm.getOrDefault("c_merge_comp_id","");
        String f5Id = deregHm.getOrDefault("c_f5_fk","");

        String dereg_last_status = "WITHDRAWN";
        String createdBy = "";
        String dereg_wd_remark = "";

        HashMap deregWdHm = db.selectOneRecord(
                "SELECT modifiedByName, c_dereg_wd_last_status , c_remark "
                        + "FROM app_fd_empm_dereg_wd WHERE id = ?",
                new String[]{deregwdId}
        );

        if(deregWdHm!=null){
            dereg_last_status = deregWdHm.getOrDefault("c_dereg_wd_last_status","").toString();
            createdBy = deregWdHm.getOrDefault("modifiedByName","").toString();
            dereg_wd_remark = deregWdHm.getOrDefault("c_remark","").toString();
        }

        boolean isForm4Wd = true;

        /*
        if f4 wd
        - has f5? upd status f5
        - abort main f4 dereg
        if f5 wd
        - if at submission, update wf env n complete
        - else update and reeval
         */
        String audit_status = "";
        if(dereg_wd_form_type.equals("5")){
            audit_status = "Form 5 Application "+f4RefNo+" Withdrawn";
            isForm4Wd = false;
        }else{
            audit_status = "Form 4 Application "+f4RefNo+" Withdrawn";

            db.update("update app_fd_empm_dereg SET c_flow_status = ? WHERE id = ?",
                    new String[]{"WITHDRAWN"},
                    new String[]{deregId}
            );

            db.update(
                    "UPDATE app_fd_empm_reg SET c_emp_status=?,c_last_move=?,c_data_status= ? WHERE id =?",
                    new String[]{
                            Constants.STATUS.EMP.ACTIVE,Constants.LAST_MOVEMENT.REGISTERED,Constants.STATUS.EMP.REGISTER_APPROVED
                    },
                    new String[]{f4EmpId}
            );
        }

        new AuditTrailUtil().insertAuditTrail2(db, f4EmpId,
                createdBy, audit_status,
                dereg_wd_remark,
                false, null
        );

        //IF FORM 5 is there
        if(!f5Id.isEmpty()){
            new AuditTrailUtil().insertAuditTrail2(db, f5EmpId,
                    createdBy, audit_status,
                    dereg_wd_remark,
                    false, null
            );

            db.update(
                    "UPDATE app_fd_empm_reg SET c_emp_status=?,c_last_move=?,c_data_status= ? WHERE id =?",
                    new String[]{
                            Constants.STATUS.EMP.ACTIVE,Constants.LAST_MOVEMENT.REGISTERED,Constants.STATUS.EMP.REGISTER_APPROVED
                    },
                    new String[]{f5EmpId}
            );
        }

        db.update("update app_fd_empm_dereg_wd SET c_status = 'APPROVED' WHERE id = '" + deregwdId +"'");

        //reset main dereg wfv and put to f5
        sql = "SELECT distinct a.ActivityId , processId FROM SHKAssignmentsTable a \n" +
                "INNER JOIN wf_process_link b\n" +
                "ON a.ActivityProcessId = b.processId \n" +
                "WHERE b.parentProcessId = ?\n" +
                "AND ActivityProcessId LIKE '%empm_emp_deregistration'";
        HashMap<String, String> hm = db.selectOneRecord(sql, new String[]{deregId});

        String processId = "";
        String actId = "";
        if(hm!=null){

//            pm("MAIN DEREG "+hm.toString());

            processId = hm.get("processId").toString();
            actId = hm.get("ActivityId").toString();

            String actDef = "";
            if(actId.contains("dereg_verify_form4_m")){
                actDef = "dereg_verify_form4";
            }else if(actId.contains("dereg_approval_form4_m")){
                actDef = "dereg_approval_form4";
            }

            wm.activityStart(processId, actDef, true);

            sql = "SELECT distinct a.ActivityId , processId FROM SHKAssignmentsTable a \n" +
                    "INNER JOIN wf_process_link b\n" +
                    "ON a.ActivityProcessId = b.processId \n" +
                    "WHERE b.parentProcessId = ?\n" +
                    "AND ActivityProcessId LIKE '%empm_emp_deregistration' "+
                    "AND a.ActivityId LIKE '%"+actDef+"'";
            hm = db.selectOneRecord(sql, new String[]{deregId});

            if(hm!=null){
                actId = hm.get("ActivityId").toString();
                wm.activityVariable(actId, "require_form5", "No");
            }

            wm.activityAbort(processId, actDef+"_m");
        }else{
            msg( "COULNDT FIND MAIN DEREG ID");
        }

        if(!isForm4Wd){
//            pm("MAIN DEREG ABORTING COMPLETED");
            return;
        }

        if(hm!=null){
            processId = hm.get("processId").toString();
            wm.processAbort(processId);
        }else{
            msg("WD DEREG F4 COULNDT FIND MAIN DEREG ID");
        }

        String hrdcno = db.selectOneValueFromId(Constants.TABLE.EMPREG, "c_hrdc_no", f4EmpId);
        //insertNoti
        new AuditTrailUtil().insertNotification(
                "Dereg. Application "+f4RefNo+" Withdrawn",
                f4EmpId, hrdcno, true, createdBy, ""
        );
        new AuditTrailUtil().insertNotification(
                "Dereg. Application "+f4RefNo+" Withdrawn",
                f4EmpId, hrdcno, false, createdBy, ""
        );

    }

    private void doAbortDereg(DBHandler db, String deregwdId, String deregId, String dereg_wd_form_type, String dereg_wd_type) { 
        
//        pm("ABORT FORM "+dereg_form_type+" - "+dereg_wd_type +" WD TOOL STARTS. ID "+deregwdId);
        
        HashMap<String, String> deregHm = db.selectOneRecord( 
                "select c_dreg_emp_id, c_merge_comp_id,c_dereg_refno, c_f5_fk, "
                + "c_form_type from app_fd_empm_dereg WHERE id=?", 
                new String[]{deregId}
        );
        
        String sql = "";
        String f4RefNo = deregHm.getOrDefault("c_dereg_refno","");
        String f4EmpId = deregHm.getOrDefault("c_dreg_emp_id","");
        String f5EmpId = deregHm.getOrDefault("c_merge_comp_id","");
        String f5Id = deregHm.getOrDefault("c_f5_fk","");
        
        String dereg_last_status = "WITHDRAWN";
        String createdBy = "";
        
        HashMap deregWdHm = db.selectOneRecord(
                "SELECT modifiedByName, c_dereg_wd_last_status "
                        + "FROM app_fd_empm_dereg_wd WHERE id = ?", 
                new String[]{deregwdId}
        );
        
        if(deregWdHm!=null){
            dereg_last_status = deregWdHm.getOrDefault("c_dereg_wd_last_status","").toString();
            createdBy = deregWdHm.getOrDefault("modifiedByName","").toString();
        }
        
        boolean isForm4Wd = true;
        
        if(dereg_wd_form_type.equals("5")){
            
//            pm("CHECKING FOR EXISTING FORM 4 WITHDRAWAL");
            //check if there's exisitng form 4 wd
            HashMap f4Wdmap = db.selectOneRecord(
                    "SELECT * FROM "+Constants.TABLE.DEREG_WD+" " + 
                    "WHERE c_dereg_id = ? "+
                    "AND c_wd_type = 'WITHDRAW' "+
                    "AND c_dereg_form_type != '5' AND c_status != 'REJECTED' "+
                    "ORDER BY dateCreated desc limit 1", 
                    new String[]{deregId});
            
            if(f4Wdmap!=null){
                String f4WdType = f4Wdmap.getOrDefault("c_dereg_form_type","").toString();                 
                dereg_last_status = "FORM "+f4WdType.toUpperCase()+" WITHDRAWAL SUBMITTED";
            }
            
            // update dereg status 
            // add audit trail 
            new AuditTrailUtil().insertAuditTrail2(db, f5EmpId, 
                    createdBy, "Form 5 Application Withdrawal Approved",
                    "Withdrawal for form 5 application approved ",
                    false, null
            );
            
            isForm4Wd = false;
        }else{
            //check if there's exisitng form 5 wd
            HashMap f5Wdmap = db.selectOneRecord(
                    "SELECT * FROM "+Constants.TABLE.DEREG_WD+" " + 
                    "WHERE c_dereg_id = ? "+
                    "AND c_wd_type = 'WITHDRAW' "+
                    "AND c_dereg_form_type = '5' AND c_status != 'REJECTED' "+
                    "ORDER BY dateCreated desc limit 1", 
                    new String[]{f5Id});
            
            if(f5Wdmap!=null){   
                String f5wdId = f5Wdmap.getOrDefault("id", "").toString();
                
                //UPDATE FORM 5 
                db.update("update app_fd_empm_dereg_wd SET c_status = 'CANCELLED' WHERE id = '" + f5wdId +"'");
                
                //ADD AUDIT TRAIL TO FORM 5 EMPLOYER
                new AuditTrailUtil().insertAuditTrail2(db, f5EmpId, 
                        createdBy, "Application Cancelled",
                        "Form 4 Withdrawal Application Approved. Form 5 withdrawal automatically cancelled",
                        false, null
                );
                
                //ABORTING FORM 5 PROCESS
                sql = "SELECT processId FROM SHKAssignmentsTable a \n" +
                            "INNER JOIN wf_process_link b\n" +
                            "ON a.ActivityProcessId = b.processId \n" +
                            "WHERE b.parentProcessId = ?\n" + 
                            "AND ActivityProcessId LIKE '%deregistration_withrdraw'";
                HashMap<String, String> hm = db.selectOneRecord(sql, new String[]{f5wdId});
                if(hm!=null){
                    String processId = hm.get("processId").toString();
                    wm.processAbort(processId);
                }else{
                    LogUtil.info("WD DEREG f4 for f5", "COULNDT FIND FORM 5 WD ID");
                }
                
                //SET FORM 5 EMP STATUS BACK TO ORI
                db.update(
                "UPDATE app_fd_empm_reg SET c_emp_status=?,c_last_move=?,c_data_status= ? WHERE id =?",
                    new String[]{
                        Constants.STATUS.EMP.ACTIVE,Constants.LAST_MOVEMENT.REGISTERED,Constants.STATUS.EMP.REGISTER_APPROVED
                    }, 
                    new String[]{f5EmpId}
                );
            }
        }
        
        //update the dereg flow status to withdrawn 
        db.update("update app_fd_empm_dereg SET c_flow_status = '"+dereg_last_status+"' WHERE id = '" + deregId +"'"
        );
        
        db.update("update app_fd_empm_dereg_wd SET c_status = 'APPROVED' WHERE id = '" + deregwdId +"'");
        
//        pm("RESETTING MAIN DEREG");
        //reset main dereg wfv and put to f5
        sql = "SELECT distinct a.ActivityId , processId FROM SHKAssignmentsTable a \n" +
                    "INNER JOIN wf_process_link b\n" +
                    "ON a.ActivityProcessId = b.processId \n" +
                    "WHERE b.parentProcessId = ?\n" + 
                    "AND ActivityProcessId LIKE '%empm_emp_deregistration'";
        HashMap<String, String> hm = db.selectOneRecord(sql, new String[]{deregId});
        
        String processId = "";
        String actId = "";
        if(hm!=null){
            
//            pm("MAIN DEREG "+hm.toString());
            
            processId = hm.get("processId").toString();
            actId = hm.get("ActivityId").toString();
            
            String actDef = "";
            if(actId.contains("dereg_verify_form4_m")){
                actDef = "dereg_verify_form4";
            }else if(actId.contains("dereg_approval_form4_m")){
                actDef = "dereg_approval_form4";
            }
            
//            pm("MAIN DEREG STARTIN NEW ACTIVITY "+actDef);
            wm.activityStart(processId, actDef, true);
            
            sql = "SELECT distinct a.ActivityId , processId FROM SHKAssignmentsTable a \n" +
                    "INNER JOIN wf_process_link b\n" +
                    "ON a.ActivityProcessId = b.processId \n" +
                    "WHERE b.parentProcessId = ?\n" + 
                    "AND ActivityProcessId LIKE '%empm_emp_deregistration' "+
                    "AND a.ActivityId LIKE '%"+actDef+"'";
            hm = db.selectOneRecord(sql, new String[]{deregId});
            
            if(hm!=null){
//                pm("MAIN DEREG New Activity "+hm.toString());
                actId = hm.get("ActivityId").toString();
                wm.activityVariable(actId, "require_form5", "No");   
            }
            
//            pm("MAIN DEREG ABORTING "+actDef+"_m");
            wm.activityAbort(processId, actDef+"_m");
        }else{
            LogUtil.info("WD DEREG F4", "COULNDT FIND MAIN DEREG ID");
        }
        
        if(!isForm4Wd){
//            pm("MAIN DEREG ABORTING COMPLETED");
            return;
        }
        
        db.update(
        "UPDATE app_fd_empm_reg SET c_emp_status=?,c_last_move=?,c_data_status= ? WHERE id =?",
            new String[]{
                Constants.STATUS.EMP.ACTIVE,Constants.LAST_MOVEMENT.REGISTERED,Constants.STATUS.EMP.REGISTER_APPROVED
            }, 
            new String[]{f4EmpId}
        );
   
        if(hm!=null){
            processId = hm.get("processId").toString();
            wm.processAbort(processId);
        }else{
            LogUtil.info("WD DEREG F4", "COULNDT FIND MAIN DEREG ID");
        }
        
        hm = db.selectOneRecord(
                "SELECT c_dreg_emp_id,c_dereg_refno FROM "+Constants.TABLE.DEREG+" WHERE id = ? ",
                new String[]{deregId}
        );       
        
        //insertEmployerAuditTrail
        new AuditTrailUtil().insertAuditTrail2(db, f4EmpId, 
                createdBy, "Deregistration Withdrawal - Request Approved",
                "Withdrawal for deregistration application - "+f4RefNo,
                true, null
        );
        
        String hrdcno = db.selectOneValueFromId(Constants.TABLE.EMPREG, "c_hrdc_no", f4EmpId);
        //insertNoti
        new AuditTrailUtil().insertNotification(
                "Withdrawal for Application "+f4RefNo+" Approved.", 
                f4EmpId, hrdcno, true, createdBy, ""
        );
        new AuditTrailUtil().insertNotification(
                "Withdrawal for Application "+f4RefNo+" Approved.", 
                f4EmpId, hrdcno, false, createdBy, ""
        );
        
    }

    private void doRevertDereg(DBHandler db, String deregwdId, String deregId, String dereg_form_type, String dereg_wd_type) {
        
//        LogUtil.info(this.getClassName(), "REVERT FORM "+dereg_form_type+" - "+dereg_wd_type +" WD TOOL STARTS. ID "+deregwdId);
        
        String dereg_last_status = db.selectOneValueFromId(Constants.TABLE.DEREG_WD, "c_dereg_wd_last_status", deregwdId);      
        
        if(dereg_form_type.equals("5")){
            //check if there's exisitng form 4 wd
            HashMap f4Wdmap = db.selectOneRecord(
                    "SELECT * FROM "+Constants.TABLE.DEREG_WD+" " + 
                    "WHERE c_dereg_id = ? "+
                    "AND c_wd_type = 'WITHDRAW' "+
                    "AND c_dereg_form_type != '5' AND c_status != 'REJECTED' "+
                    "ORDER BY dateCreated desc limit 1", 
                    new String[]{deregId});
            
            if(f4Wdmap!=null){
                String f4WdType = f4Wdmap.getOrDefault("c_dereg_form_type","").toString();                 
                dereg_last_status = "FORM "+f4WdType.toUpperCase()+" WITHDRAWAL SUBMITTED";
            }
        }else{
            //check if there's exisitng form 4 wd
            HashMap f5Wdmap = db.selectOneRecord(
                    "SELECT * FROM "+Constants.TABLE.DEREG_WD+" " + 
                    "WHERE c_dereg_id = ? "+
                    "AND c_wd_type = 'WITHDRAW' "+
                    "AND c_dereg_form_type = '5' AND c_status != 'REJECTED' "+
                    "ORDER BY dateCreated desc limit 1", 
                    new String[]{deregId});
            
            if(f5Wdmap!=null){                
                dereg_last_status = "FORM 5 WITHDRAWAL SUBMITTED";
            }
        }
        
        db.update(
                "UPDATE app_fd_empm_dereg SET c_flow_status = ? WHERE id = ?",
                new String[]{dereg_last_status},
                new String[]{deregId}
        );
        
        db.update("update app_fd_empm_dereg_wd SET c_status = 'REJECTED' WHERE id = '" + deregwdId +"'");        
        
        HashMap hm = db.selectOneRecord(
                "SELECT c_dreg_emp_id,c_dereg_refno FROM "+Constants.TABLE.DEREG+" WHERE id = ? ",
                new String[]{deregId}
        );        
        String empId = "";
        String deregRefno = "";
//        String createdBy = db.selectOneValueFromId("app_fd_empm_dereg_wd", "modifiedByName", deregwdId);    
        String createdBy = db.selectOneValueFromTable("SELECT modifiedByName FROM app_fd_empm_dereg_wd WHERE id = ?", new String[]{deregwdId});
        
        if(hm!=null){
            empId = hm.get("c_dreg_emp_id").toString();
            deregRefno = hm.get("c_dereg_refno").toString();
        }
        
        //insertEmployerAuditTrail
        new AuditTrailUtil().insertAuditTrail2(db, empId, 
                createdBy, "Deregistration Withdrawal - Request Rejected",
                "Withdrawal for deregistration application - "+deregRefno,
                false, null
        );
        
        String hrdcno = db.selectOneValueFromId(Constants.TABLE.EMPREG, "c_hrdc_no", empId);
        //insertNoti
        new AuditTrailUtil().insertNotification(
                "Withdrawal for Application "+deregRefno+" Rejected.", 
                empId, hrdcno, true, createdBy, ""
        );
        new AuditTrailUtil().insertNotification(
                "Withdrawal for Application "+deregRefno+" Rejected.", 
                empId, hrdcno, false, createdBy, ""
        );
    }

    private void doUpdateDereg(DBHandler db, String deregwdId, String deregId, String dereg_form_type, String dereg_wd_type) {
        
//        LogUtil.info(this.getClassName(), "UPDATE FORM "+dereg_form_type+" - "+dereg_wd_type +" WD TOOL STARTS. ID "+deregwdId);
        
        HashMap hm = db.selectOneRecord(
                "SELECT c_dreg_emp_id,c_dereg_refno,c_flow_status FROM "+Constants.TABLE.DEREG+" WHERE id = ? ",
                new String[]{deregId}
        );    
        
        String dereg_last_status = "";
        String empId = "";
        String deregRefno = "";
        String createdBy = db.selectOneValueFromTable("SELECT modifiedByName FROM app_fd_empm_dereg_wd WHERE id = ?", new String[]{deregwdId});
                
        if(hm!=null){
            empId = hm.get("c_dreg_emp_id").toString();
            deregRefno = hm.get("c_dereg_refno").toString();
            dereg_last_status = hm.get("c_flow_status").toString();
        }else{
            hm = db.selectOneRecord(
                "SELECT c_dreg_emp_id,c_dereg_refno,c_flow_status, id FROM "+Constants.TABLE.DEREG+" WHERE c_f5_fk = ? ",
                new String[]{deregId}
            );    
            
            if(hm!=null){
                empId = hm.get("c_dreg_emp_id").toString();
                deregRefno = hm.get("c_dereg_refno").toString();
                dereg_last_status = hm.get("c_flow_status").toString();
                deregId = hm.get("id").toString();
            }
        }   
        
//        pm("MAIN DEREG data "+hm.toString());
        
        String dereg_new_status = "";
        
        if(dereg_form_type.equals("5")){
            //check if there's exisitng form 4 wd
            HashMap f4Wdmap = db.selectOneRecord(
                    "SELECT * FROM "+Constants.TABLE.DEREG_WD+" " + 
                    "WHERE c_dereg_id = ? "+
                    "AND c_wd_type = 'WITHDRAW' "+
                    "AND c_dereg_form_type != '5' AND c_status != 'REJECTED' "+
                    "ORDER BY dateCreated desc limit 1", 
                    new String[]{deregId});
            
            if(f4Wdmap!=null){
                String f4WdId = f4Wdmap.getOrDefault("id","").toString();
                String f4WdType = f4Wdmap.getOrDefault("c_dereg_form_type","").toString();
                
                dereg_new_status = "FORM "+f4WdType.toUpperCase()+" & FORM 5 WITHDRAWAL SUBMITTED";
            }else{
                dereg_new_status = "FORM 5 WITHDRAWAL SUBMITTED";
            }
        }else{
            //check if there's exisitng form 4 wd
            HashMap f5Wdmap = db.selectOneRecord(
                    "SELECT * FROM "+Constants.TABLE.DEREG_WD+" " + 
                    "WHERE c_dereg_id = (SELECT c_f5_fk FROM "+Constants.TABLE.DEREG+" l WHERE l.id = ?) "+
                    "AND c_wd_type = 'WITHDRAW' "+
                    "AND c_dereg_form_type = '5' AND c_status != 'REJECTED' "+
                    "ORDER BY dateCreated desc limit 1", 
                    new String[]{deregId});
            
            if(f5Wdmap!=null){                
                dereg_new_status = "FORM "+dereg_form_type.toUpperCase()+" & FORM 5 WITHDRAWAL SUBMITTED";
            }else{
                dereg_new_status = "FORM "+dereg_form_type.toUpperCase()+" WITHDRAWAL SUBMITTED";
            }
        }
        
        //update the dereg flow status to withdrawn 
        int i = db.update("update app_fd_empm_dereg SET c_flow_status = '"+dereg_new_status+"' WHERE id = '" + deregId +"'" );
//        pm("UPDATE MAIN DEREG STATUS RESULT "+Integer.toString(i));
        //update the dereg flow status to withdrawn 
        i = db.update("update app_fd_empm_dereg_wd SET c_dereg_wd_last_status = '"+dereg_last_status+"' WHERE id = '" + deregwdId +"'" );
//        pm("UPDATE DEREG WD LAAST STATUS RESULT "+Integer.toString(i));
                
        //insertEmployerAuditTrail
        new AuditTrailUtil().insertAuditTrail2(db, empId, 
                createdBy, "Deregistration - Withdrawal Submitted",
                "Withdrawal for deregistration application - "+deregRefno,
                false, null
        );
        
        String hrdcno = db.selectOneValueFromId(Constants.TABLE.EMPREG, "c_hrdc_no", empId);
        //insertNoti
        new AuditTrailUtil().insertNotification(
                "Withdrawal for Application "+deregRefno+" Submitted.", 
                empId, hrdcno, true, createdBy, ""
        );
        new AuditTrailUtil().insertNotification(
                "Withdrawal for Application "+deregRefno+" Submitted.", 
                empId, hrdcno, false, createdBy, ""
        );
    }
}
