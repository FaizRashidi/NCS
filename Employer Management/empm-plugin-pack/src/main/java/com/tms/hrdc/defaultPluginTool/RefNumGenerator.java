/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import oracle.jdbc.driver.Const;
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
public class RefNumGenerator extends DefaultApplicationPlugin{

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
        return "To generate Reg No. accordingly";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Reg. No. Generator";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/customEmailProperty.json", null, true);
    }
    
    @Override
    public Object execute(Map props) {
        
        WorkflowAssignment wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");     
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        String id = appService.getOriginProcessId(wfAssignment.getProcessId());
        PluginManager pluginManager = (PluginManager) props.get("pluginManager");
        WorkflowManager workflowManager = (WorkflowManager) pluginManager.getBean("workflowManager");

        DataSource ds = (DataSource)AppUtil.getApplicationContext().getBean("setupDataSource");
        DBHandler db = new DBHandler();
        try {
            db.openConnection(ds);        
            
            String empId = CommonUtils.getEmpId_empReg(db, id);
            
            HashMap submitterHm = db.selectOneRecord(
                    "SELECT createdBy, createdByName FROM app_fd_empm_regAppl WHERE id = ?",
                    new String[]{id}
            );
            
            String createdBy = submitterHm==null?"":submitterHm.getOrDefault("createdBy", "").toString();
            String createdByName = submitterHm==null?"":submitterHm.getOrDefault("createdByName", "").toString();
            
            String query = "UPDATE app_fd_empm_reg SET c_register_dt = ?, createdBy = ?, createdByName = ? "
                        + "WHERE id = ? ";
            int i = db.update(query, 
                        new String[]{
                            CommonUtils.get_DT_CurrentDateTime("YYYY-MM-dd HH:mm:ss"),
                            createdBy,
                            createdByName
                        },
                        new String[]{
                            empId
                        });   
            
            EmpmObj obj = new EmpmObj(db, EmpmObj.BY_ID, empId);
            String mycoid = obj.getMycoid();
            String reg_type = obj.getRegType();
            String org_type = obj.getOrgType();
            String psmbNo = obj.getHrdcNo();
            
            if(org_type.equals("Others")){
                obj.setMycoid(psmbNo);
                mycoid = obj.getMycoid();
            }

            String current_type = "Employer Registration";
            if(obj.isDeregisteredBefore()) {
                obj.setCurrentStatusRemark(Constants.STATUS.EMP.REREGISTERING);
                current_type = "Employer Reregistration";
            }else {
                obj.setCurrentStatusRemark(Constants.STATUS.EMP.REGISTERING);
            }

            workflowManager.activityVariable(wfAssignment.getActivityId(), "reg_type", current_type);

            db.update(
                    "UPDATE app_fd_empm_regAppl SET c_reg_type = ? WHERE id = ?",
                    new String[]{current_type},
                    new String[]{id}
            );

//            LogUtil.info(this.getClassName(), "Reg Type =>> "+reg_type);
            if(reg_type.equals("HQ")){
                obj.initHQBranchCount();
                obj.updateAllbranch();

                db.closeConnection();
                return null;
            }

            String branch_mycoid = "", hq_emp_id = "";

            if(reg_type.equals("Branch")){
                hq_emp_id = getHQIDNo(db, mycoid);
                branch_mycoid = obj.generateBranchMyCoID(mycoid); //hrdcB0001
            }
            
            query = "UPDATE app_fd_empm_reg SET c_hq_id = ?, c_hq_mycoid = ?, c_branch_mycoid = ?, c_mycoid = ? WHERE id = ?";
            i = db.update(query, new String[]{hq_emp_id,mycoid,branch_mycoid,branch_mycoid}, new String[]{empId});
            
            LogUtil.info(this.getClassName(),"BRANCH DATA UPDATE "+Integer.toString(i));
            //update archive also
//            query = "UPDATE app_fd_empm_emp_arc SET c_mycoid = ? WHERE c_arc_fk = ?";
//            db.update(query, new String[]{myCoID}, new String[]{id});
            
        } catch (SQLException ex) {
            
        } finally{
            db.closeConnection();
        }
        
        return null;
    }

    private String getHQIDNo(DBHandler db, String mycoid) {
        String query = "SELECT id FROM app_fd_empm_reg WHERE c_mycoid = ? AND c_empl_reg_type = 'HQ'";
        
        HashMap hm = db.selectOneRecord(query, new String[]{mycoid});
        
        if(hm!=null){
            return hm.get("id").toString();
        }
        return "";
    }
    
}
