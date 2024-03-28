/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.dao.CurrentUser;
import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.webservice.models.AssignmentsImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;

/**
 *
 * @author faizr
 */
public class AssignmentManager extends WorkflowFormBinder{
    
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
        return "For task forwarding/returning/rassignments";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Assignment Binder";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }
    
    @Override
    public String getPropertyOptions() {
       return AppUtil.readPluginResource(getClass().getName(), "/properties/archive_binderX.json", null, true, "message/archive_binderX");
    }
    
    @Override
    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {
        
        FormRow row = rows.get(0);
        String procIds = row.get("reqIds")==null?"":row.get("reqIds").toString();
        String forward_to = row.get("forward_to")==null?"":row.get("forward_to").toString();
        
        DBHandler db = new DBHandler();
        
        try{
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
            
            //each record ID
            //get process ID & current assignment ID
            //determine current appvr type
            // add stack
            // include SV
            // reevaluate
            
            String fwded_assgnee = ""; // Get full names of the forwardees
            for( String fwdee: forward_to.split(";") ){
                fwdee=AssignmentsImpl.getName(db, fwdee);
                fwded_assgnee += fwded_assgnee.isEmpty()?fwdee:", "+fwdee;
            }
            
            String[] procArr = procIds.split(",");            
            String allAsgnee = AssignmentsImpl.includeSV(db, forward_to);
            
//            String usr = AppUtil.processHashVariable("#currentUser.firstName#", null, null, null)+
//                    " "+AppUtil.processHashVariable("#currentUser.lastName#", null, null, null);

            CurrentUser cu = new CurrentUser();
            String usr_full_name = cu.getFullName();
            String usr_id = cu.getId();
            
            for(String proc:procArr){
                HashMap hm = AssignmentsImpl.getTaskData(db, proc);

                String assignmentId = hm.get("assignmentId").toString();
                String wfV = AssignmentsImpl.getWorkFlowVar(assignmentId);
                
                AssignmentsImpl.updateAssignedOfficers(db, forward_to, proc, assignmentId, usr_id);                
                AssignmentsImpl.reassignToNew(assignmentId, wfV, allAsgnee);
                
                //update
                String table = CommonUtils.getTable(db, proc);
                int upd = db.update("UPDATE "+table+" SET c_is_viewed = ? WHERE id = ?",
                    new String[]{Constants.STATUS.VIEW_STATUS.NEW},
                    new String[]{proc});
                
                //Audit trail recording                
                proc = CommonUtils.getEmplId_ALL(db, proc);
                
                ArrayList list = new ArrayList();
                
                new AuditTrailUtil().insertAuditTrail2(db, proc, usr_full_name, "Forward", "Task reassigned to "+fwded_assgnee, true, list);
                
                //get current assignee & asgn data - cross check
                //save
                //reassign
                // add to audit trail
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }
        
        return rows;
    }

    

    
    
}
