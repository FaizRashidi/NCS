/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.webservice.models.AssignmentsImpl;
import com.tms.hrdc.dao.Process;
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
 * @author faizr
 */
public class ForwardReturnTool extends DefaultApplicationPlugin{

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
        return "To reassign to forwarded/returned assignee";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Return/Forward Controller Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

     @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/forwardReturnTool.json", null, true,null);
    }

    @Override
    public Object execute(Map props) {
        PluginManager pm =  (PluginManager)props.get("pluginManager");
        WorkflowManager wm = (WorkflowManager) pm.getBean("workflowManager");
        WorkflowAssignment wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");     
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        
        String id = appService.getOriginProcessId(wfAssignment.getProcessId());
        String opsType = props.get("ops_type").toString();
        String apvrType = props.get("apvr_type").toString();
        String fwdee = props.get("fwd_to").toString();
        String app_type = props.get("app_type").toString();
        
        LogUtil.info("value test", "I TRIED SO HARD< BUT GOT SO FARRRR!!! "+fwdee);
        
        DBHandler db = new DBHandler();
        
        try{
            db.openConnection();
            
            HashMap lastStckHm = new HashMap();
            String stckId = "";
            String asgnee = "";
            
            int i = 0;

            Process proc = new Process(db, app_type, id);
            
            switch(opsType){
                case "Forward":
                    lastStckHm = db.selectOneRecord(
                            "SELECT * FROM "+Constants.TABLE.ASSIGNEES_DATA+" "
                            + "WHERE c_type = ? AND c_record_id = ? "
                            + "ORDER BY dateCreated desc LIMIT 1 ",
                            new String[]{apvrType,id}
                    );
                    
                    if(lastStckHm!=null){
                        stckId = lastStckHm.get("id").toString();
                        asgnee = lastStckHm.get("c_officer_in_charge").toString();
                    }
                    
                    i = db.update(
                            "UPDATE "+Constants.TABLE.ASSIGNEES_DATA+" "
                            + "SET c_status = ? WHERE id = ? ", 
                            new String[]{Constants.STATUS.ASGN_STATUS.FORWARDED},
                            new String[]{stckId}
                    );
                    
                    HashMap newStck = new HashMap();
                    newStck.put("officer_in_charge", fwdee);
                    newStck.put("received_from", asgnee);
                    newStck.put("status", Constants.STATUS.ASGN_STATUS.CURRENT);
                    newStck.put("type", apvrType);
                    newStck.put("record_id", id);
                    
                    CommonUtils.saveUpdateForm2("", Constants.FORM_ID.EMP_ASGN_DATA, "", newStck);                    
                
                    updateStatus(db, proc, Constants.STATUS.VIEW_STATUS.NEW);
                break;
                
                case "Return":
                    lastStckHm = db.selectOneRecord(
                            "SELECT * FROM "+Constants.TABLE.ASSIGNEES_DATA+" "
                            + "WHERE c_type = ? AND c_record_id = ? AND c_status = ?"
                            + "ORDER BY dateCreated desc LIMIT 1 ",
                            new String[]{apvrType,id,Constants.STATUS.ASGN_STATUS.FORWARDED}
                    );
                    if(lastStckHm!=null){
                        stckId = lastStckHm.get("id").toString();
                        fwdee = lastStckHm.get("c_officer_in_charge").toString();
                    }
                    
                    i = db.delete(
                            "DELETE FROM "+Constants.TABLE.ASSIGNEES_DATA+" "
                            + "WHERE c_record_id = ? AND c_status = ?", 
                            new String[]{id, Constants.STATUS.ASGN_STATUS.CURRENT}
                    );
                    
                    i = db.update(
                            "UPDATE "+Constants.TABLE.ASSIGNEES_DATA+" "
                            + "SET c_status = ? WHERE id = ? ", 
                            new String[]{Constants.STATUS.ASGN_STATUS.CURRENT},
                            new String[]{stckId}
                    );
                    
                    updateStatus(db, proc, Constants.STATUS.VIEW_STATUS.RETURNED);
                break;
            }
            
            //include sv
//            String allAsgnee = AssignmentsImpl.includeSV(db, fwdee);
            String allAsgnee = AssignmentsImpl.includeSV2(db, proc.getSV(),fwdee);
            String selectWfv = "";
            
            if(apvrType.equals("officer")){
                selectWfv = props.get("officer_id").toString();
            }else{
                selectWfv = props.get("approver_id").toString();
            }
            
            LogUtil.info("value test", "IN THE END IT DOESNT EVEN MATTERR!! "+selectWfv);
            
            wm.activityVariable(wfAssignment.getActivityId(), selectWfv, allAsgnee);
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }
        
        return null;
    }

    private void updateStatus(DBHandler db, Process proc, String status) {
        String id = proc.getRecId();
        
        db.update(
                "UPDATE "+Constants.TABLE.EMPREG_APPL+" SET c_is_viewed=? WHERE id =?",
                new String[]{status},
                new String[]{id}
        );
        db.update(
                "UPDATE "+Constants.TABLE.DEREG+" SET c_is_viewed=? WHERE id =?",
                new String[]{status},
                new String[]{id}
        );
    }
    
}
