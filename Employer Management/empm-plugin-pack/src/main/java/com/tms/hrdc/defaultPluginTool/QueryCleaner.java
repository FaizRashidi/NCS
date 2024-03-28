/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import static com.tms.hrdc.defaultPluginTool.QueryReminder.url_contextPath;
import static com.tms.hrdc.defaultPluginTool.QueryReminder.url_scheme;
import static com.tms.hrdc.defaultPluginTool.QueryReminder.url_serverName;
import static com.tms.hrdc.defaultPluginTool.QueryReminder.url_serverPort;
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
 * @author faizr
 */
public class QueryCleaner extends DefaultApplicationPlugin{

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
        return "To Kill Any Unresponded Queries After Approve/Reject";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Query Kill - Maintenance Tool";
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
    
    PluginManager pluginManager = null;
    WorkflowManager workflowManager = null;
    WorkflowAssignment wfAssignment = null;
    
    @Override
    public Object execute(Map props) {
        DBHandler db = new DBHandler();
        
        wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");             
        pluginManager = (PluginManager) props.get("pluginManager");
        workflowManager = (WorkflowManager) pluginManager.getBean("workflowManager"); 
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        
        String id = appService.getOriginProcessId(wfAssignment.getProcessId());
        
        //check if got query
        try{
            db.openConnection();
            
            ArrayList<HashMap<String,String>> standingList = db.select(
                    "select distinct ActivityId, act.ActivityDefinitionId from SHKAssignmentsTable a \n" +
                    "inner join SHKActivities act ON act.id = a.ActivityId\n" +
                    "where ActivityProcessId = ? \n" +
                    "and ActivityProcessId like '%empm%'\n" +
                    "and ActivityId like '%query%'",
                    new String[]{wfAssignment.getProcessId()}
            );
            
            for(HashMap hm:standingList){
                String actID = hm.getOrDefault("ActivityId", "").toString();
                String actDefID = hm.getOrDefault("ActivityDefinitionId", "").toString();
                
                workflowManager.activityAbort(wfAssignment.getProcessId(), actDefID);
                
                msg("UNRESPONDED QUERY "+actID+" ABORTED ON APPROVAL/REJECTION");
            }
            
            msg("NO UNRESPONDED QUERY ABORTED ON APPROVAL/REJECTION");
            
            
        }catch(Exception e){
            e.printStackTrace();
        }
        
        return null;
    }
}
