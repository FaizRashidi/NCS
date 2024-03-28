/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.dao.CurrentUser;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.User;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;

/**
 *
 * @author faizr
 */
public class EmployerStatusUpdate extends DefaultApplicationPlugin{

    @Override
    public String getName() {
        return "Employer Status Updater";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "To Update Status After Process";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Employer Status Updater";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return 
            "[{\n" +
            "    title : 'EMPM Employer Data Update Tool',\n" +
            "    properties : [{\n" +
            "            name:\"status\",\n" +
            "            label: \"Status\",\n" +
            "            type:\"SelectBox\",\n" +
            "            required : \"true\",            \n" +
            "            options : [\n" +
            "                {value: 'integration', label : 'Integration'},\n" +
            "                {value: 'scheduler', label : 'Scheduler'}\n" +
            "            ]\n" +
            "        }" +
            "    ] " +
            "}]";
    }
    
    int i = 0;
    
    @Override
    public Object execute(Map props) {
        DBHandler db = new DBHandler();
        try {
            db.openConnection((DataSource)AppUtil.getApplicationContext().getBean("setupDataSource"));
        
            String type = (String) props.get("status");
            
            if(type.equals("integration")){
                WorkflowAssignment wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");     
                AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");

                String id = appService.getOriginProcessId(wfAssignment.getProcessId());
                
                updateRejectedUsers(db, id);
            }
            
            //this option to get dereg'd/rejected records past certain days & put to PE
            if(type.equals("scheduler")){
                String days = getMoveDays(db);
                
                ArrayList<HashMap> empRec = getInactiveRec(db, days); 
                
                for(HashMap hm:empRec){
                    updatePotEmp(db, hm.get("id").toString(), "rejected");
                }
                
                LogUtil.info(this.getName(), "Employers Set as Potential Employers: "+
                        Integer.toString(i));
            }
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            db.closeConnection();
        }
        return null;
    }
    
    public void updatePotEmp(DBHandler db, String empId, String status){
        
        String sql = "SELECT * FROM app_fd_empm_pe_potEmp WHERE c_emp_fk = ?";
        HashMap hm = db.selectOneRecord(sql, new String[]{empId});
        
        if(hm==null){
            createPotEmpData(db, empId, status.equals("rejected")?"REJECT":"TRUE",
                                status.equals("rejected")?"No":"Yes");
            i++;
        }else{
            sql = "UPDATE app_fd_empm_pe_potEmp c_status = ?, c_is_registered = ? WHERE c_emp_fk = ? ";
            i += db.update(sql, 
                                new String[]{status.equals("rejected")?"REJECT":"TRUE",
                                                status.equals("rejected")?"No":"Yes"},
                                new String[]{empId});
        }
    }
    
    public void createPotEmpData(DBHandler db, String empId, String status, String isRegistered){
        HashMap hm = new HashMap();
        
        hm.put("emp_fk", empId);
        hm.put("status", status);
        hm.put("is_registered", "Yes");
        CommonUtils.saveUpdateForm2("", Constants.FORM_ID.POTEMP_DETAIL,
                "", hm);
    }

    private String getMoveDays(DBHandler db) {
        String sql = "select c_import_pe_days from app_fd_empm_reg_stp h "
                + "WHERE id = ?";
        
        HashMap hm = db.selectOneRecord(sql, new String[]{Constants.DATA_ID.MAIN_SETUP_ID});
        
        if(hm!=null){
            return hm.get("c_import_pe_days") == null?"0":
                                hm.get("c_import_pe_days").toString();    
        }        
        return "0";
    }

    private ArrayList getInactiveRec(DBHandler db, String days) {
        String sql = "select id, r.c_approve_dt \n" +
                        "from app_fd_empm_reg r\n" +
                        "where \n" +
                        "c_approve_dt is not null \n" +
                        "and r.c_emp_status = 'Inactive'\n" +
                        "and r.c_data_status in ('REGISTER_REJECTED','DEREGISTER_APPROVED')\n" +
                        "and DATEDIFF(now(),c_approve_dt) >= ?";
        
        return db.select(sql, new String[]{days});
    }

    private void updateRejectedUsers(DBHandler db, String id) {
        
        // get currentUser based on c_req_email
        /// change groups to rejected
        
        HashMap hm = db.selectOneRecord(
                "SELECT r.id, c_req_email FROM "+Constants.TABLE.EMPREG_APPL+" ra "
                    + "INNER JOIN "+Constants.TABLE.EMPREG+" r ON r.id = ra.c_empl_fk "
                    + "WHERE ra.id = ? ",
                new String[]{id}
        );
        
        if(hm==null){
            return;
        }
        
        String userId = hm.getOrDefault("c_req_email", "").toString();
        
        UserDao ud = (UserDao) AppUtil.getApplicationContext().getBean("userDao");        
        User user = ud.getUserById(userId);
        
        if(user!=null){
            ud.assignUserToGroup(userId, Constants.USER_GROUP.REJECTED_USERS);
            LogUtil.info("Rejected Users", Constants.USER_GROUP.REJECTED_USERS+" "+userId);
        }
        
    }
    
}
