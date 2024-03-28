/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.datalistAction.DeleteUserAndRecord;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
public class DeregistrationCancellationTool extends DefaultApplicationPlugin{

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
        return "Reactivate company staff login users after undoing deregistration";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Employer Deregistration Cancellation Tool";
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
        LogUtil.info(this.getClass().toString(), msg);
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
        DBHandler db = new DBHandler();
        
        try{
            db.openConnection();
//            String getDeregwdIdQuery = "select originProcessId from wf_process_link \n" +
//                                        "WHERE processId=?";
//            HashMap<String, String> deregwd_idHM = db.selectOneRecord(getDeregwdIdQuery, new String[]{wfAssignment.getProcessId()});
//
            String deregwdId = appService.getOriginProcessId(wfAssignment.getProcessId());

            String getDeregIdQuery = "select c_dereg_id from app_fd_empm_dereg_wd \n" +
                    "WHERE id=?";
            HashMap<String, String> deregId_HM = db.selectOneRecord(getDeregIdQuery, new String[]{deregwdId});
            String deregId = deregId_HM.get("c_dereg_id");

            //update the dereg flow status to cancelled 
            db.update(
                    "update app_fd_empm_dereg " +
                    "SET c_flow_status = ? " +
                    "WHERE id = ? ",
                    new String[]{"CANCELLED"},
                    new String[]{deregId}
            );
            
            String getEmpIdQuery = "select c_dreg_emp_id, r.c_hrdc_no  from app_fd_empm_dereg d " +
                                    "INNER JOIN app_fd_empm_reg r ON r.id = d.c_dreg_emp_id " +
                                     "WHERE d.id=?";
            HashMap<String, String> empId_HM = db.selectOneRecord(getEmpIdQuery, new String[]{deregId});
            String empId = empId_HM.get("c_dreg_emp_id");
            String hrdcNo = empId_HM.get("c_hrdc_no");
            db.update(
                    "update app_fd_empm_reg\n" +
                    "SET c_emp_status=?,c_last_move=?,c_data_status=?\n" +
                    "WHERE id =?",
                    new String[]{Constants.STATUS.EMP.ACTIVE,Constants.LAST_MOVEMENT.REGISTERED,Constants.STATUS.EMP.REGISTER_APPROVED}, 
                    new String[]{empId}
            );

            DeregisterTool.doLevyTransferAPI(db, deregId, hrdcNo, "", true);

            String getUsersQuery = "SELECT c_userId FROM app_fd_empm_usermap\n" +
                                   "WHERE c_compId = ?";
            ArrayList<HashMap<String,String>> userList = db.select(getUsersQuery, new String[]{empId});

            if(userList==null){
                LogUtil.info(this.getClassName(), "No User Data");
                db.closeConnection();
                return null;
            }
            
            //reactivate users from the employer's company
            for(HashMap user:userList){
                String userId = user.get("c_userId").toString();
                int i = db.update(
                        "UPDATE dir_user SET active = 1 WHERE id='"+userId+"'"
                );
                
//                String activeQuery = "select active from dir_user where id=?";
//                HashMap<String, String> active_statusHM = db.selectOneRecord(activeQuery, new String[]{userId});
//                String active_status = active_statusHM.get("active");
//                LogUtil.info("active_status: ",active_status);
                DeleteUserAndRecord.enableUser(db, userId);

                msg("Enabling user: "+userId+" result: "+Integer.toString(i));
            }
    
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }        
        
        return null;
    }

}
