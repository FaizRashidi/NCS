/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.datalistAction;

import com.tms.hrdc.binder.AssignmentManager;
import com.tms.hrdc.dao.CurrentUser;
import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.webservice.models.AssignmentsImpl;
import static com.tms.hrdc.webservice.models.AssignmentsImpl.getName;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;

/**
 *
 * @author faizr
 */
public class ReturnTaskButton extends DataListActionDefault {

    @Override
    public String getName() {
        return this.getClass().toString();
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getDescription() {
        return "To return forwarded task back to original assignees"; 
    }

    @Override
    public String getLinkLabel() {
        String label = getPropertyString("label");
        if (label == null || label.isEmpty()) {
            label = "Loalalala";
        }
        return label;    }

    @Override
    public String getHref() {
        return getPropertyString("href"); 
    }

    @Override
    public String getTarget() {
        return "post";
    }

    @Override
    public String getHrefParam() {
        return getPropertyString("hrefParam"); 
    }

    @Override
    public String getHrefColumn() {
        return getPropertyString("hrefColumn");
    }

    @Override
    public String getConfirmation() {
        String confirm = getPropertyString("confirmation");
        if (confirm == null || confirm.isEmpty()) {
            confirm = "Sure to return task?";
        }
        return confirm;    }

    
    @Override
    public String getLabel() {
        return "HRDC - EMPM - Return Task Button";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        String json = "[{\n"
                + "    title : 'Mail List Button',\n"
                + "    properties : [{\n"
                + "        label : 'Label',\n"
                + "        name : 'label',\n"
                + "        type : 'textfield',\n"
                + "        description : 'Load Mail List button',\n"
                + "        value : 'Load Mail List button'\n"
                + "    }]\n"
                + "}]";
        return json;    }
    
    @Override
    public DataListActionResult executeAction(DataList dl, String[] rowKeys) {
        
        DBHandler db = new DBHandler();
        try {
            db.openConnection();
            
//            String usr = AppUtil.processHashVariable("#currentUser.firstName#", null, null, null)+
//                    " "+AppUtil.processHashVariable("#currentUser.lastName#", null, null, null);;
            
            CurrentUser cu = new CurrentUser();
            String usr_full_name = cu.getFullName();
            String usr_id = cu.getId();

            for(String procId:rowKeys){
                HashMap taskHm = AssignmentsImpl.getTaskData(db, procId); //x,x,x
//                String curr_assignees = taskHm.get("curr_assignees").toString();
                String assignmentId = taskHm.get("assignmentId").toString();
                String wfV = AssignmentsImpl.getWorkFlowVar(assignmentId);
                
                String oriAsgnee = AssignmentsImpl.getOldAssigner(db, wfV, procId, assignmentId);       
                String return_to = AssignmentsImpl.includeSV(db, oriAsgnee);   
                
                AssignmentsImpl.reassignToNew(assignmentId, wfV, return_to);
                AssignmentsImpl.updateAssignedOfficers(db, "", procId, assignmentId, usr_id);
                
                //change seen status
                String table = CommonUtils.getTable(db, procId);
                int upd = db.update("UPDATE "+table+" SET c_is_viewed = ? WHERE id = ?",
                    new String[]{Constants.STATUS.VIEW_STATUS.RETURNED},
                    new String[]{procId});
                
                //Audit trail recording
                String fwded_assgnee = "";
                for( String fwdee: oriAsgnee.split(";") ){
                    
                    String full_name = AssignmentsImpl.getName(db, fwdee);
                    fwdee = full_name.isEmpty()?fwdee:full_name;
                    fwded_assgnee += fwded_assgnee.isEmpty()?fwdee:", "+fwdee;
                }
                
                procId = CommonUtils.getEmplId_ALL(db, procId);

                ArrayList list = new ArrayList();
                
                new AuditTrailUtil().insertAuditTrail2(db, procId, usr_full_name, "Return", "Task reassigned back to "+fwded_assgnee, true, list);
            }
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally{
            db.closeConnection();
        }
        DataListActionResult result = new DataListActionResult();
        result.setType(DataListActionResult.TYPE_REDIRECT);
        result.setUrl("REFERER");
        
        return result;
    }
    
}
