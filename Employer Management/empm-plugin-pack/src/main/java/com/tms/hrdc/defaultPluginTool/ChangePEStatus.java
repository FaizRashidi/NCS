/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
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
 * @author kahyi
 */
public class ChangePEStatus extends DefaultApplicationPlugin{
    
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
        return "Change Potential Employer Status to CKSP, TRUE or FORM 1 RECEIVED";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Change Potential Employer Status Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/change_peStatus_tool.json", null, true);

    }
    
    @Override
    public Object execute(Map props) {
        LogUtil.info("== EMP-REG","Change PE Status Start");
        
        WorkflowAssignment wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");     
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        String id = appService.getOriginProcessId(wfAssignment.getProcessId());
        
        String action = (String) props.get("status");
        
        DBHandler db = new DBHandler();
        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
                        
            String status = "";
            HashMap peIDHm = db.selectOneRecord(
                           "SELECT c_pe_fk FROM app_fd_empm_pe_compl_cksp WHERE id = ?",
                            new String[]{id}
                    );
            String peID = peIDHm!=null?peIDHm.get("c_pe_fk").toString():"";
            
            switch(action){
                case "cksp":
                    status = Constants.STATUS.POT_EMP.CKSP;     
                break;
                case "nfa":
                    status = Constants.STATUS.POT_EMP.TRUE;
                break;
               
            }
            String table = Constants.TABLE.POT_EMP;
            if (action.equals("cksp") || action.equals("nfa")){
                int update_status = db.update("UPDATE "+table+" SET c_status = ? WHERE id = ?",
                    new String[]{status},
                    new String[]{peID});

            }
                                             
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            db.closeConnection();
        }
        
        return null;
    }
}
