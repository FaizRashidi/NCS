package com.tms.hrdc.binder;

import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import org.apache.commons.lang.StringUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.model.service.WorkflowManager;

import java.sql.SQLException;
import java.util.HashMap;

public class DeregWithdrawBinder  extends WorkflowFormBinder {

    @Override
    public String getName() {
        return "Deregistration Withdraw Binder";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "To process for deregistrations withdrawal";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Dereg. Withdrawal Binder";
    }
    
    private void msg(String msg){
        LogUtil.info(this.getClass().toString(), msg);
    }

    @Override
    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {
        rows = super.store(element, rows, formData);

        String dereg_wd_id = rows.get(0).getId();

        DBHandler db = new DBHandler();

        try {
            db.openConnection();
            HashMap hm = db.selectOneRecord(
                    "SELECT * FROM " + Constants.TABLE.DEREG_WD + " WHERE id = ?",
                    new String[]{dereg_wd_id});

            String deregId = "";
            String dereg_form_type = "";
            String dereg_wd_type = "";

            if (hm != null) {
                deregId = hm.getOrDefault("c_dereg_id", "").toString();
                dereg_form_type = hm.getOrDefault("c_dereg_form_type", "").toString();
                dereg_wd_type = hm.getOrDefault("c_wd_type", "").toString();
            }

            HashMap deregHm = db.selectOneRecord(
                    "SELECT * FROM "+Constants.TABLE.DEREG+" WHERE id = ?",
                    new String[]{deregId}) ;

            if(deregHm==null){ // IF FORM 5 WITHDRAW
                deregHm = db.selectOneRecord(
                        "SELECT * FROM "+Constants.TABLE.DEREG+" WHERE c_f5_fk = ?",
                        new String[]{deregId}) ;
            }

            if(deregHm!=null){
                deregId = deregHm.getOrDefault("id", "").toString();
            }

            doAbortDereg(db, dereg_wd_id, deregId, dereg_form_type, dereg_wd_type);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closeConnection();
        }

        return rows;
    }

    private void doAbortDereg(DBHandler db, String deregwdId, String deregId,
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
            msg("COULNDT FIND MAIN DEREG ID");
        }

        if(!isForm4Wd){
//            pm("MAIN DEREG ABORTING COMPLETED");
            return;
        }

        if(hm!=null){
            processId = hm.get("processId").toString();
            wm.processAbort(processId);
        }else{
            msg("WD DEREG F4, COULNDT FIND MAIN DEREG ID");
        }

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
}
