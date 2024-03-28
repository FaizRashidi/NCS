/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.KeywordDictionary;
import com.tms.hrdc.dao.Process;
import com.tms.hrdc.webservice.models.AssignmentsImpl;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.security.access.method.P;

/**
 *
 * @author faizr
 */
public class AuditTrailTool extends DefaultApplicationPlugin{

    @Override
    public String getName() {
        return "Audit Trail Tool";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "To record action and data changes";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Audit Trail Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/audit_trail_tool.json", null, true);
    }
    
    final String UUID = "uuid";
    final String COL = "column";
    final String VAL = "value";
    final String PREV_VALUE = "previous_value";
    final String CURR_VALUE = "current_value";
    final String FIELD_NAME = "fld_name";
    final String DATEMODIFIED = "date_modified";
    final String MODIFIEDBY = "modified_by_id";
    final String MODIFIEDBYNAME = "modified_by_name";
    final String VALUE_CHGE_LIST = "audit_trail_list";
    final String AUDIT_FK = "audit_fk";
    final String STATUS = "status";
    final String REMARK = "remark";
    
    String reprocess = "";
    
    @Override
    public Object execute(Map props) {
        LogUtil.info("== EMP-REG","Audit Trail Tool Starts ");
        
        PluginManager pluginManager = (PluginManager) props.get("pluginManager");
        WorkflowManager WorkflowManager = (WorkflowManager) pluginManager.getBean("workflowManager"); 
        WorkflowAssignment wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");     
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        String id = appService.getOriginProcessId(wfAssignment.getProcessId());
        String app_type = (String) props.get("app_type");
        String action = (String) props.get("status");
        String updateEmpStatus = props.get("updateEmpStatus")==null?"":props.get("updateEmpStatus").toString();
//        reprocess = props.get("reprocess")==null?"":" (Reprocessed)";
        reprocess = "";

        String logRemarks = "";
        String logStatus  = "";
//        String modifiedBy  = "";       
        boolean is_data_same_as_prev = false;   
        
        HashMap hm_clone = new HashMap();       
        HashMap hm_arch_clone = new HashMap();
        
        ArrayList<HashMap<String, String>> value_chge_list = new ArrayList();
        
        DBHandler db = new DBHandler();
        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
                        
            String status = "";
            String remarks = "";
            String modifiedBy  = ""; 
            HashMap hm = new HashMap();
            
            ArrayList<HashMap<String, String>> chgeVal  = new ArrayList(); 
            
            switch(action){
                case "submitted":
                    status = "Submitted";
                break;

                case "approval":
                    status = "Approved";
                break;
                case "ckspStatusUpdate":
                    status = "CKSP Status Update";
                break;

                case "rejection":
                    status = "Rejected";
            }
            
            String empId = "";
            String f5empId = "";
            
            boolean special = false;
            Process proc;

            switch(app_type){
                case Process.TYPE_E_EMPREG:
                    
//                    empId = id; // in form, audit trail is loaded with empm_reg id as fk anyways
                    hm = handleRegAudit(db, id, CommonUtils.getEmpId_empReg(db, id),action);
                    empId = CommonUtils.getEmpId_empReg(db, id);
                    id = empId;
                    special = true;
                break;
                case Process.TYPE_E_DEREG:
                    empId = CommonUtils.getEmpId_empDereg(db, id);
                    
                    String isForm5Submitted = WorkflowManager.getProcessVariable(wfAssignment.getProcessId(), "require_form5");
                    
                    if(!StringUtils.isBlank(isForm5Submitted)){
                        f5empId = CommonUtils.getEmpId_empDeregF5(db, id);
                    }
                    
                    hm = handleDeregAudit(db, id, action, isForm5Submitted);
                    
                break;
                case "empl_dereg_wd":
                    HashMap empIdHm = db.selectOneRecord(
                            "select r.id,r.dateCreated, r.c_comp_name, r.c_mycoid from app_fd_empm_dereg_wd wd\n" +
                            "INNER jOIN app_fd_empm_dereg d on d.id = wd.c_dereg_id\n" +
                            "INNER JOIN app_fd_empm_reg r on r.id = d.c_dreg_emp_id\n" +
                            " WHERE wd.id = ?",
                            new String[]{id}
                    );
                    empId = empIdHm!=null?empIdHm.get("id").toString():"";
                    hm = handleDeregWDAudit(db, id, empId, action);
                break;
                case "empl_dereg_cancel":
                    HashMap empIdCancelHm = db.selectOneRecord(
                            "select r.id,r.dateCreated, r.c_comp_name, r.c_mycoid from app_fd_empm_dereg_wd wd\n" +
                            "INNER jOIN app_fd_empm_dereg d on d.id = wd.c_dereg_id\n" +
                            "INNER JOIN app_fd_empm_reg r on r.id = d.c_dreg_emp_id\n" +
                            " WHERE wd.id = ?",
                            new String[]{id}
                    );
                    empId = empIdCancelHm!=null?empIdCancelHm.get("id").toString():"";

                    hm = handleDeregCancellationAudit(db, id, empId, action);
                break;
                case Process.TYPE_PE_ENGAGEMENT:                    
                    proc = new Process(db, app_type, id);
                    empId = proc.getEmpObj().getId();
                    hm = handleEgmntAudit(proc, action);
                    
                break;
                case Process.TYPE_E_REQ_CHANGE:                    
                    proc = new Process(db, app_type, id);
                    empId = proc.getEmpObj().getId();
                    hm = handleInfoReqChge(proc, action);

                    break;
                case "cksp_complaint":
                    HashMap empId_ckspcHm = db.selectOneRecord(
                            "select r.id, c.c_cksp_status from app_fd_empm_pe_compl_cksp c\n" +
                            "inner join app_fd_empm_pe_potEmp p on p.id = c.c_pe_fk\n" +
                            "inner join app_fd_empm_reg r on r.id = p.c_emp_fk\n" +
                            " WHERE c.id = ?",
                            new String[]{id}
                    );
                    empId = empId_ckspcHm!=null?empId_ckspcHm.get("id").toString():"";
                    String cksp_status = empId_ckspcHm!=null?empId_ckspcHm.get("c_cksp_status").toString():"";
                    hm = handleCKSPComplaintAudit(db, cksp_status, empId, action);
                break;                    
                default:
                    empId = CommonUtils.getEmpId_empReg(db, id);
            }
            
            status = hm.get("STATUS").toString();
            remarks = hm.get("REMARKS").toString();
            modifiedBy = hm.get("BY").toString();
            chgeVal = hm.containsKey("CHANGEVALUE")?
                    (ArrayList<HashMap<String, String>>) hm.get("CHANGEVALUE") :
                    new ArrayList();
            
            if(updateEmpStatus.equals("YES") && action.contains("F5")){
                new AuditTrailUtil().insertAuditTrail2(db, f5empId, modifiedBy, status, remarks, true, chgeVal);
            }else if(action.contains("F5")){
                new AuditTrailUtil().insertAuditTrail2(db, f5empId, modifiedBy, status, remarks, false, chgeVal);
            }
            
            if(updateEmpStatus.equals("YES")){
                new AuditTrailUtil().insertAuditTrail2(db, empId, modifiedBy, status, remarks, true, chgeVal);
            }else{
                new AuditTrailUtil().insertAuditTrail2(db, empId, modifiedBy, status, remarks, false, chgeVal);
            }            
            
                        
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            db.closeConnection();
        }
        
        return null;
    }
    
    private HashMap handleRegAudit(DBHandler db, String id, String empId, String action) {
        
        String actionBy = "";
        String date = "";
        String status = "";
        String remarks = "";
        String chgeVal = "";
        
        ArrayList chgeList = new ArrayList();
        
        EmpmObj eo = new EmpmObj(db, EmpmObj.BY_ID, empId);
        
        String formType = db.selectOneValueFromId(
                Constants.TABLE.EMPREG_APPL, 
                "c_form_type", id);
        
        HashMap hm = eo.getEmpData();
        
        actionBy = hm==null?"":hm.get("modifiedByName").toString();
        date = hm==null?"":hm.get("dateModified").toString();

        String form_title = "Form "+formType;
        String reqMail = eo.getPrimaryEmail();
        String mycoid = eo.getMycoid();
        String requester = reqMail+"("+mycoid+")";
        
        switch(action){
            case "submitted":
                status = "Submitted "+form_title;
                
                String so_name = hm.getOrDefault("c_so_name", "").toString();
                String createdBy = hm.getOrDefault("createdByName", mycoid).toString();
                
                actionBy = so_name.isEmpty()?createdBy:so_name+" ("+
                        hm.getOrDefault("c_comp_name", "").toString()+")";                
            break;
            
            case "verification":
                status = hm==null?"":hm.get("c_verify").toString()+reprocess;
                remarks = hm==null?"":hm.get("c_verify_remark").toString();
                
                if(status.equals("Approved")){
                    status = form_title+" "+status;
                }
                
                if(status.equals("Forward")){
                    status = "Forwarded "+form_title+" Approval to "+AssignmentsImpl.getNameStr(db, hm.get("c_forward_to").toString() );
//                    remarks += hm==null?"":"Forwarded to "+AssignmentsImpl.getNameStr(db, hm.get("c_forward_to").toString() );
                }

                if(status.equals("Query")){
                    status = form_title+" Query To "+requester;
//                    remarks += hm==null?"":"Forwarded to "+AssignmentsImpl.getNameStr(db, hm.get("c_forward_to").toString() );
                }
                
                chgeList = AuditTrailUtil.getRegChgeList2(db, eo);
                if(!chgeList.isEmpty()){
                    remarks += Constants.SEE_DETAIL_WORD;
                }
                
            break;
            
            case "approval":
//                status = ("Employer Registration "+hm==null?"":hm.get("c_approval").toString())+reprocess;
                status = form_title+" "+(hm==null?"":hm.get("c_verify").toString()+reprocess);
                remarks = hm==null?"":hm.get("c_approve_remark").toString();
                
                if(status.equals("Forward")){
                    status = "Forwarded "+form_title+" Approval to "+AssignmentsImpl.getNameStr(db, hm.get("c_forward_to").toString() );
//                    remarks += hm==null?"":"Forwarded to "+AssignmentsImpl.getNameStr(db, hm.get("c_forward_to").toString() );
                }
                
                chgeList = AuditTrailUtil.getRegChgeList2(db, eo);
                if(!chgeList.isEmpty()){
                    remarks += Constants.SEE_DETAIL_WORD;
//                    chgeVal = FormManagerTool.buildRemarksString(chgeList);
                }
                
                if(status.equals("Approved")){
                    status = form_title+" "+status;
                }
            break;
            
            case "query":
                status = form_title+" Query Responded ";
            break;

            case "query_timeout":
                status = form_title+" Query Unresponded - Reminder Timed Out ";
            break;
        }
        return hash(date, actionBy, status, remarks, chgeList);        
    }
    
    private HashMap handleDeregAudit(DBHandler db, String id, String action, String form5Submitted) {
        
        String actionBy = "";
        String date = "";
        String status = "";
        String remarks = "";
        ArrayList chgeVal = new ArrayList();
        
        HashMap hm = db.selectOneRecord(
                "select createdByName, modifiedByName, dateModified,\n" +
                "c_form_type, dd.c_dreg_reason,\n" +
                "dd.c_dereg_approval, dd.c_dereg_verify,\n" +
                "dd.c_verify_remark, dd.c_approve_remark,\n" +
                "dd.c_dereg_verify_f5, dd.c_dereg_approval_f5,\n" +
                "dd.c_verify_remark_f5, dd.c_approve_remark_f5,\n" +
                "dd.c_submitting_name, dd.c_submit_mode, dd.c_flow_status," +
                "CONCAT(c_merge_comp_name, ' (', c_merge_mycoid, ')' ) as merger " +
                "from app_fd_empm_dereg dd WHERE id = ?",
                new String[]{id}
        );
        
        String formType = hm==null?"":hm.get("c_form_type").toString();
        actionBy = hm==null?"":hm.get("modifiedByName").toString();
        date = hm==null?"":hm.get("dateModified").toString();
        String flowStatus = hm==null?"":hm.get("c_flow_status").toString();
        String merger = hm==null?"":hm.get("merger").toString();

        String form_title = "Form "+formType;
        
        String approval = "";
        
        switch(action){
            case "submitted":
                String submit_mode = hm!=null?hm.getOrDefault("c_submit_mode", "ONLINE").toString():"ONLINE";
                String submit_person = hm!=null?hm.getOrDefault("c_submitting_name", "").toString():actionBy;
                String createdByName = hm!=null?hm.getOrDefault("createdByName", submit_person).toString():submit_person;
                status = "Submitted "+form_title;
                remarks = hm==null?"":hm.get("c_dreg_reason").toString();
                
                if(submit_mode.equals("ONLINE")){
                    actionBy = submit_person;
                }else{
                    actionBy = createdByName+" (On Behalf) ";
                }
                
            break;
            
            case "submittedF5":
                status = "Submitted Form 5"; //Form 
                remarks = hm==null?"":hm.get("c_merge_remark").toString();
            break;
            
            case "verification":
                approval = hm==null?"":hm.get("c_dereg_verify").toString();
                
                switch(approval){
                    case "f4_approved":
                        status = form_title + " Approved "; 
                    break;
                    case "f4_approved_f5_approved":
                        status = form_title + " & Form 5 Approved "; 
                    break;
                    case "f4_approved_f5_rejected":
                        status = form_title + " Approved & Form 5 Rejected"; 
                    break;
                    case "f4_rejected":
                        status = form_title + " Rejected "; 
                    break;
                    case "f4_rejected_f5_rejected":
                        status = form_title + " & Form 5 Rejected "; 
                    break;
                    case "f4_query_f5_query":
                        status = form_title + " & Form 5 Query "; 
                    break;
                    case "f4_query":
                        status = form_title + " Query "; 
                    break;
                    case "f5_query":
                        status = " Form 5 Query "; 
                    break;
                }
                remarks = hm==null?"":hm.get("c_verify_remark").toString();
                
                if(flowStatus.equals("PENDING FORM 5 SUBMISSION")){
                    status += "<br /> Form 5 Rejected due to inactivity";
                    remarks += "<br /> "+merger+" didn't submit Form 5" ;
                }
            break;
            
            case "verificationF5":
                approval = hm==null?"":hm.get("c_dereg_verify").toString();
                
                switch(approval){
                    case "f4_approved":
                        status = form_title + " Approved "; 
                    break;
                    case "f4_approved_f5_approved":
                        status = form_title + " & Form 5 Approved "; 
                    break;
                    case "f4_approved_f5_rejected":
                        status = form_title + " Approved & Form 5 Rejected"; 
                    break;
                    case "f4_rejected":
                        status = form_title + " Rejected "; 
                    break;
                    case "f4_rejected_f5_rejected":
                        status = form_title + " & Form 5 Rejected "; 
                    break;
                    case "f4_query_f5_query":
                        status = form_title + " & Form 5 Query "; 
                    break;
                    case "f4_query":
                        status = form_title + " Query "; 
                    break;
                    case "f5_query":
                        status = " Form 5 Query "; 
                    break;
                }
                
                status = " Form 5 " + hm==null?"":hm.get("c_dereg_verify_f5").toString();
                remarks = hm==null?"":hm.get("c_verify_remark").toString();
                
                if(flowStatus.equals("PENDING FORM 5 SUBMISSION")){
                    status = "Form 5 Rejected due to inactivity";
                    remarks = "<br /> "+merger+" didn't submit Form 5" ;
                }
            break;

            case "approval":
                
                approval = hm==null?"":hm.get("c_dereg_approval").toString();
                
                switch(approval){
                    case "f4_approved":
                        status = form_title + " Approved "; 
                    break;
                    case "f4_approved_f5_approved":
                        status = form_title + " & Form 5 Approved "; 
                    break;
                    case "f4_approved_f5_rejected":
                        status = form_title + " Approved & Form 5 Rejected"; 
                    break;
                    case "f4_rejected":
                        status = form_title + " Rejected "; 
                    break;
                    case "f4_rejected_f5_rejected":
                        status = form_title + " & Form 5 Rejected "; 
                    break;
                    case "f4_query_f5_query":
                        status = form_title + " & Form 5 Query "; 
                    break;
                    case "f4_query":
                        status = form_title + " Query "; 
                    break;
                    case "f5_query":
                        status = " Form 5 Query "; 
                    break;
                }
                
//                status = form_title + " " +hm==null?"":hm.get("c_dereg_approval").toString();
                remarks = hm==null?"":hm.get("c_approve_remark").toString();
            break;
            
            case "approvalF5":
                
                approval = hm==null?"":hm.get("c_dereg_approval").toString();
                
                switch(approval){
                    case "f4_approved":
                        status = form_title + " Approved "; 
                    break;
                    case "f4_approved_f5_approved":
                        status = form_title + " & Form 5 Approved "; 
                    break;
                    case "f4_approved_f5_rejected":
                        status = form_title + " Approved & Form 5 Rejected"; 
                    break;
                    case "f4_rejected":
                        status = form_title + " Rejected "; 
                    break;
                    case "f4_rejected_f5_rejected":
                        status = form_title + " & Form 5 Rejected "; 
                    break;
                    case "f4_query_f5_query":
                        status = form_title + " & Form 5 Query "; 
                    break;
                    case "f4_query":
                        status = form_title + " Query "; 
                    break;
                    case "f5_query":
                        status = " Form 5 Query "; 
                    break;
                }
                        
//                status = " Form 5 "+hm==null?"":hm.get("c_dereg_approval_f5").toString();
                remarks = hm==null?"":hm.get("c_approve_remark_f5").toString();
            break;
            
            case "query":
                status = form_title + " Query Responded";
            break;

            case "rejection":
                status = "Rejected";
            break;
        }
        return hash(date, actionBy, status, remarks, chgeVal);        
    }
    
    private HashMap handleDeregWDAudit(DBHandler db, String id, String empId, String action) {
        
        String actionBy = "";
        String date = "";
        String status = "";
        String remarks = "";
        ArrayList chgeVal = new ArrayList();
        
        HashMap hm = db.selectOneRecord(
                "select modifiedByName, dateModified, c_wd_reason, c_remark, c_approval from app_fd_empm_dereg_wd WHERE id = ?",
                new String[]{id}
        );
        
        actionBy = hm==null?"":hm.get("modifiedByName").toString();
        date = hm==null?"":hm.get("dateModified").toString();
        
        switch(action){
            case "submitted":
                status = "Deregistration Withdrawn";
                remarks = hm==null?"":hm.get("c_wd_reason").toString();
            break;
        }
        return hash(date, actionBy, status, remarks, chgeVal);        
    }
    
    private HashMap handleDeregCancellationAudit(DBHandler db, String id, String empId, String action) {
                
        String actionBy = "";
        String date = "";
        String status = "";
        String remarks = "";
        ArrayList chgeVal = new ArrayList();
        
        HashMap hm = db.selectOneRecord(
                "select modifiedByName, dateModified, c_wd_reason, c_remark, c_approval from app_fd_empm_dereg_wd WHERE id = ?",
                new String[]{id}
        );

        actionBy = hm==null?"":hm.get("modifiedByName").toString();
        date = hm==null?"":hm.get("dateModified").toString();

        switch(action){
            case "submitted":
                status = "Submitted Deregistration Cancellation ";
                remarks = hm==null?"":hm.get("c_wd_reason").toString();
            break;

            case "approval":
                status = "Dereg. Cancellation "+hm==null?"":hm.getOrDefault("c_approval","").toString();
                remarks = hm==null?"":hm.get("c_remark").toString();
            break;
            
            case "query":
                status = "Approved";
            break;

            case "rejection":
                status = "Rejected";
            break;
        }
        return hash(date, actionBy, status, remarks, chgeVal);  
    }

    private HashMap hash(String date, String who, String status, String remarks, ArrayList chgeList){
        HashMap hm = new HashMap();
        hm.put("DATE", date);
        hm.put("BY", who);
        hm.put("STATUS", status);
        hm.put("REMARKS", remarks);
        hm.put("CHANGEVALUE", chgeList);
        
        return hm;
    }
    
    private  HashMap handleInfoReqChge(Process proc, String action){
        DBHandler db = proc.getDBHandler();

        String actionBy = "";
        String date = "";
        String status = "";
        String remarks = "";
        String chgeVal = "";

        ArrayList chgeList = new ArrayList();

        HashMap hm = db.selectOneRecord(
                "SELECT r.* "
                        + "FROM "+Constants.TABLE.REQUEST_CHANGES+" r WHERE id = ?",
                new String[]{proc.getRecId()}
        );

        actionBy = hm==null?"":hm.get("modifiedByName").toString();
        date = hm==null?"":hm.get("dateModified").toString();
        action = hm.get("c_approval")==null?"":hm.getOrDefault("c_approval","").toString().toUpperCase();
        remarks = hm==null?"":hm.get("c_remarks").toString();

        String newValue = hm==null?"":hm.getOrDefault("","").toString();
        String reqType = hm==null?"":hm.get("c_req_type").toString();
        String oldValue = "";
        String fieldChange = "";

        switch(action){

            case "APPROVED":

                remarks += Constants.SEE_DETAIL_WORD;
                status = "Request of "+reqType+" - Approved";

                if(reqType.equals("Change Company Name")){

                    fieldChange = "c_comp_name";
                    oldValue = proc.getEmpObj().getCompName();
                    newValue = hm==null?"":hm.getOrDefault("c_new_comp_name", "").toString();
                    chgeList = AuditTrailUtil.buildChangeAuditHm(db, fieldChange, 
                            newValue, oldValue, chgeList, Constants.CACHE_TYPE.MAIN_EMP_DATA);

                }else if(reqType.equals("Change Company Activity (Sector Code)")){

                    oldValue = proc.getEmpObj().getSubSector();
                    newValue = hm==null?"":hm.getOrDefault("c_sector_search_id_new", "").toString();

                    HashMap oldMSIC_hm = getMSICDetails(db, oldValue);
                    HashMap newMSIC_hm = getMSICDetails(db, newValue);

                    Set set = oldMSIC_hm.entrySet();
                    Iterator iterator = set.iterator();
                    while (iterator.hasNext()) {
                        Map.Entry mentry = (Map.Entry) iterator.next();

                        fieldChange = mentry.getKey().toString();
                        oldValue = mentry.getValue().toString();
                        newValue = newMSIC_hm.getOrDefault(mentry.getKey(), "").toString();

                        chgeList = AuditTrailUtil.buildChangeAuditHm(db, fieldChange, 
                                newValue, oldValue, chgeList, Constants.CACHE_TYPE.MAIN_EMP_DATA);
                    }
                }

                break;

            case "REJECTED":
                status = "Request of "+reqType+" - Rejected";
                break;
            case "QUERY":
                status = "Request of "+reqType+" - Queried";
                break;
            case "QUERY_RESPONDED":
                status = "Request of "+reqType+" - Query Responded ";
                break;
            case "QUERY_TIMEOUT":
                status = "Request of "+reqType+" (Queried Unresponded - reminder timed out)";
                break;
        }

        return hash(date, actionBy, status, remarks, chgeList);
    }

    private HashMap getMSICDetails(DBHandler db, String sectorId){
        HashMap hm = db.selectOneRecord(
                "select \n" +
//                        "# q.c_sector_section as c_industry_sector,\n" +
                        "(\n" +
                        "select \n" +
                        "  concat(s1.c_industry_sector_code,' - ', s1.c_industry_sector)\n" +
                        "  from app_fd_stp_industry_sector s1 where id = q.c_sector_section\n" +
                        "  limit 1\n" +
                        ") as c_industry_sector_label,\n" +
                        "\n" +
//                        "# q.c_sector_div as c_div,\n" +
                        "(\n" +
                        "select \n" +
                        "  concat(s2.c_div_code,' - ', s2.c_descr)\n" +
                        "  from app_fd_stp_industry_div s2 where id = q.c_sector_div\n" +
                        "  limit 1\n" +
                        ") as c_div_label,\n" +
                        "\n" +
//                        "# q.c_main_sector_code as c_main_sector_code,\n" +
                        "(\n" +
                        "select \n" +
                        "  concat(s4.c_main_sector_code,' - ', s4.c_descr)\n" +
                        "  from app_fd_stp_main_sector s4 where id = q.c_main_sector_code\n" +
                        "  limit 1\n" +
                        ") as c_main_sector_label,\n" +
                        "\n" +
//                        "# q.c_sector_class as c_class_code,\n" +
                        "(\n" +
                        "select \n" +
                        "  concat(s5.c_sector_class_code,' - ', s5.c_descr)\n" +
                        "  from app_fd_stp_class_sector s5 where id = q.c_sector_class\n" +
                        "  limit 1\n" +
                        ") as c_class_label,\n" +
                        "\n" +
                        "concat(q.c_sub_sector_code, ' - ', q.c_descr) as c_sector_descr\n" +
                        "\n" +
                        "from app_fd_stp_sub_sector q\n" +
                        "where id = ? ",
                new String[]{sectorId}
        );

//        hm.put("c_sector_search_id", sectorId);

        return hm;
    }

    private HashMap handleEgmntAudit(Process proc, String action) {
        
        DBHandler db = proc.getDBHandler();
        
        String actionBy = "";
        String date = "";
        String status = "";
        String remarks = "";
        String chgeVal = "";
        
        ArrayList chgeList = new ArrayList();
        
        HashMap hm = db.selectOneRecord(
                "SELECT r.* "
                        + "FROM "+Constants.TABLE.POT_EMP_ENGAGEMENT+" r WHERE id = ?",
                new String[]{proc.getRecId()}
        );
        
        actionBy = hm==null?"":hm.get("modifiedByName").toString();
        date = hm==null?"":hm.get("dateModified").toString();
        action = hm.get("c_eng_action")==null?"":hm.get("c_eng_action").toString();
        remarks = hm==null?"":hm.get("c_remarks").toString();
        
        switch(action){
            
            case Constants.STATUS.EGMNT.SAVED:
                status = "Saved";                
                
                chgeList = AuditTrailUtil.getEmpDataChgeListForEgmnt(db, proc.getRecId());
                if(!chgeList.isEmpty()){
                    remarks += Constants.SEE_DETAIL_WORD;
//                    chgeVal = FormManagerTool.buildRemarksString(chgeList);
                }
                
            break;
            
            case Constants.STATUS.EGMNT.COMPLETED:
                status = "Engagement Completed";
                chgeList = AuditTrailUtil.getEmpDataChgeListForEgmnt(db, proc.getRecId());
                if(!chgeList.isEmpty()){
                    remarks += Constants.SEE_DETAIL_WORD;
//                    chgeVal = FormManagerTool.buildRemarksString(chgeList);
                }
            break;
            
            case Constants.STATUS.EGMNT.FORWARD:
                status = "Forwarded to "+AssignmentsImpl.getNameStr(db, hm.get("c_forward_to").toString() );
                
                chgeList = AuditTrailUtil.getEmpDataChgeListForEgmnt(db, proc.getRecId());
                if(!chgeList.isEmpty()){
                    remarks += Constants.SEE_DETAIL_WORD;
//                    chgeVal = FormManagerTool.buildRemarksString(chgeList);
                }
            break;

            case Constants.STATUS.EGMNT.RETURN:
                status = "Returned";
                
                chgeList = AuditTrailUtil.getEmpDataChgeListForEgmnt(db, proc.getRecId());
                if(!chgeList.isEmpty()){
                    remarks += Constants.SEE_DETAIL_WORD;
//                    chgeVal = FormManagerTool.buildRemarksString(chgeList);
                }
            break;
            
            case Constants.STATUS.EGMNT.ACKNOWLEDGED:
                status = "Acknowledged";
                
                chgeList = AuditTrailUtil.egmntMergeEmpDataChge(db, proc.getRecId(), proc.getEmpObj().getId());
                if(!chgeList.isEmpty()){
                    remarks += "Employer Data Updated";
                }
                
                //update PE STATUS
                
                HashMap peId_hm = db.selectOneRecord(
                        "SELECT * FROM app_fd_empm_pe_egmnt e WHERE id = ?",
                        new String[]{proc.getRecId()}
                );
                
                if(peId_hm!=null){
                    String peId = peId_hm.getOrDefault("c_pe_fk", "").toString();
                    
                    int i = db.update("UPDATE app_fd_empm_pe_potEmp SET c_status = 'TRUE' WHERE id = '"+peId+"'");
                    LogUtil.info("EGMNT PE STATUS UPDATE ", Integer.toString(i));
                }
                
                LogUtil.info(this.getClassName(), chgeList.toString());
            break;
            
            case Constants.STATUS.EGMNT.REJECTED:
                status = "Rejected";
            break;
        }
        
        return hash(date, actionBy, status, remarks, chgeList);        
    }

    private HashMap handleCKSPComplaintAudit(DBHandler db, String cksp_status, String empId, String action) {
        
            String actionBy = AppUtil.processHashVariable("#currentUser.firstName#", null, null, null)+
                    " "+AppUtil.processHashVariable("#currentUser.lastName#", null, null, null);
            String date = new Date().toString();
            String status = "";
            String remarks = "";
            
            ArrayList chgeList = new ArrayList();

            HashMap hm = db.selectOneRecord(
                    "SELECT c_mycoid "
                            + "FROM app_fd_empm_reg r WHERE id = ?",
                    new String[]{empId}
            );

            switch(action){
                case "submitted":
                    status = "Submitted CKSP Complaint Form";

                break;
   
                case "ckspStatusUpdate":
                    status = "CKSP status changed to " + cksp_status;
                    remarks = "";

                break;

            }
            return hash(date, actionBy, status, remarks, chgeList);          
    }
}
