/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.datalistAction.DeleteUserAndRecord;
import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;

/**
 *
 * @author kahyi
 */
public class AbortGeneralProcesses extends DefaultApplicationPlugin{
        
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
        return "Abort General Processes";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Abort General Processes";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }
    
    private void msg(String msg){
        LogUtil.info(this.getClassName(), msg);
    }
    
    PluginManager pm = null;
    WorkflowManager wm = null;
    WorkflowAssignment wfAssignment = null;
    
    @Override
    public Object execute(Map props) {
        msg("Abort Selected Application Process");
        wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");     
        pm =  (PluginManager)props.get("pluginManager");
        wm = (WorkflowManager) pm.getBean("workflowManager");
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        String id = appService.getOriginProcessId(wfAssignment.getProcessId());

        DBHandler db = new DBHandler();
        
        try{
            db.openConnection();
            
            //if reg/dereg previously rejected but reapplied
            HashMap hm = new HashMap();
            ArrayList<HashMap<String, String>> oldProc = new ArrayList();
            
            HashMap curHm = getMycoID(db, id);
            String flowType =  curHm==null?"":curHm.getOrDefault("flowType", "").toString();
            String mycoid =  curHm==null?"":curHm.getOrDefault("c_mycoid", "").toString();
            String old_rec_id = "";
            
            if(flowType.equals("EMPREG")){
                oldProc = db.select(
                        "SELECT ra.id FROM "+Constants.TABLE.EMPREG_APPL+" ra "
                        + "INNER JOIN "+Constants.TABLE.EMPREG+" r ON r.id = ra.c_empl_fk "
                        + "WHERE ra.c_flow_status = ? AND ra.id != ? "
                        + "AND r.c_mycoid = ? "
                        + "ORDER BY ra.dateCreated desc ",
                        new String[]{"REJECTED", id, mycoid}
                );
            }
            if(flowType.equals("EMPDEREG")){
                oldProc = db.select(
                        "SELECT d.id FROM "+Constants.TABLE.DEREG+" d "
                        + "INNER JOIN "+Constants.TABLE.EMPREG+" r ON r.id = d.c_dreg_emp_id "
                        + "WHERE d.c_flow_status = ? AND d.id != ? "
                        + "AND r.c_mycoid = ? "
                        + "ORDER BY d.dateCreated desc ",
                        new String[]{"REJECTED", id, mycoid}
                );
            }
            
            boolean isEmpReg = false;
            
            for(HashMap oldProcHm:oldProc){
                
                isEmpReg = true;
                
                old_rec_id = oldProcHm.getOrDefault("id", "").toString();
            
                List<HashMap<String, String>> procList = CommonUtils.getRunningProcessList(db, old_rec_id);
                for(HashMap procHm:procList){
                    String procId = procHm.get("ActivityProcessId")!=null?procHm.get("ActivityProcessId").toString():"";
                    msg("DELETING REJECTION REPROCESS "+procId+" COS GOT NEW RESUBMISSION");
                    wm.processAbort(procId);
                }
            }
            
            
            if(isEmpReg){
                db.closeConnection();
                
                return null;
            }
            
            // -----------------------------------------------------------------------------
            
            String getPotEmpID = "select pe.id as peID, b.id as regID from app_fd_empm_regAppl a\n" +
                                    "inner join app_fd_empm_reg b on b.id = a.c_empl_fk\n" +
                                    "inner join app_fd_empm_pe_potEmp pe on pe.c_emp_fk = b.id\n"+
                                    "where a.id = ?";
            HashMap<String, String> getPotEmpIDHM = db.selectOneRecord(getPotEmpID, new String[]{id});
            
            
            if(getPotEmpIDHM==null){
                db.closeConnection();
                return null;
            }
            
            String potEmpID = getPotEmpIDHM.get("peID").toString();
            String regID = getPotEmpIDHM.get("regID").toString();
            
            //abort all activities related to the process
            abortProcessesRelatedToPotEmpID(db,potEmpID);
            insertIntoAuditTrail(db,regID);
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }        
        
        return null;
    }
    
    private void abortCKSPProcess (DBHandler db, String pe_id){
        String sql = "SELECT processId FROM SHKAssignmentsTable a \n" +
                    "INNER JOIN wf_process_link b\n" +
                    "ON a.ActivityProcessId = b.processId \n" +
                    "WHERE b.parentProcessId in (\n" + 
                    "select id from app_fd_empm_pe_compl_cksp \n" +
                    "where c_pe_fk = ?\n" +
                    "and ( c_cksp_status <> 'NFA' and c_cksp_status <> 'Form 1 Received' )\n"+
                    ")\n" + 
                    "AND ActivityProcessId LIKE '%empm_complaintToCKSP%'";
        ArrayList<HashMap<String, String>> processID_hm = db.select(sql, new String[]{pe_id});
        String CKSPComplaintTable = Constants.TABLE.POT_EMP_CKSP; 
        for (HashMap<String, String> process: processID_hm){
            String processId = process.get("processId").toString();
            LogUtil.info("process ID abortion: ", processId);
            wm.processAbort(processId);
            int updateCKSPStatus = db.update("UPDATE "+CKSPComplaintTable+" SET c_cksp_status = ? WHERE id = ?",
            new String[]{"Form 1 Received"},
            new String[]{processId});
        }
        LogUtil.info("UPDATE status for peID: ", pe_id);
        String potEmpTable = Constants.TABLE.POT_EMP;
        int updatePotEmpStatus = db.update("UPDATE "+potEmpTable+" SET c_status = ?, c_is_registered = 'Yes' WHERE id = ?",
            new String[]{Constants.STATUS.POT_EMP.TRUE},
            new String[]{pe_id});
    }
    
    private void abortProcessesRelatedToPotEmpID(DBHandler db, String ckspComplaint_id) {
        abortCKSPProcess(db, ckspComplaint_id);
        
    }

    private void insertIntoAuditTrail(DBHandler db, String regID) {
        String modifiedBy = AppUtil.processHashVariable("#currentUser.firstName#", null, null, null)+
                    " "+AppUtil.processHashVariable("#currentUser.lastName#", null, null, null);
        new AuditTrailUtil().insertAuditTrail2(db, regID, modifiedBy, "CKSP Complaint Application Cancelled", "", true, new ArrayList());
    }

    private HashMap getMycoID(DBHandler db, String id) {
        
        HashMap hm = db.selectOneRecord(
                "SELECT r.c_mycoid, 'EMPREG' as flowType FROM "+Constants.TABLE.EMPREG_APPL+" ra "
                + "INNER JOIN "+Constants.TABLE.EMPREG+" r ON r.id = ra.c_empl_fk "
                + "WHERE ra.id = ? " ,
                new String[]{id}
        );
        
        if(hm==null){
            hm = db.selectOneRecord(
                "SELECT r.c_mycoid, 'EMPDEREG' as flowType FROM "+Constants.TABLE.DEREG+" d "
                + "INNER JOIN "+Constants.TABLE.EMPREG+" r ON r.id = d.c_dreg_emp_id "
                + "WHERE d.id = ? " ,
                new String[]{id}
            );
        }
        
        return hm;
    }

}
