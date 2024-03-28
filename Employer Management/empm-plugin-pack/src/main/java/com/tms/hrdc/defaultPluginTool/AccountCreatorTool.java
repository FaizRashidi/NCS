/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.dao.CurrentUser;
import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.PluginThread;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.User;
import org.joget.directory.model.service.DirectoryManager;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;

/**
 *
 * @author faizr
 */
public class AccountCreatorTool extends DefaultApplicationPlugin{

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
        return "To create user account after approved";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - User Account Creation Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/acc_creator_tool.json", null, true);
    }
    
    private void msg(String msg){
        LogUtil.info(this.getClass().toString(), msg);
    }
    
    String FLD_EMPLID = "";
    String FLD_HRDCNO = "";
    String FLD_MYCOID = "";
    String FLD_COMPNAME = "";
    String FLD_REQEMAIL = "";
    String FLD_REQPW = "";
    String FLD_REGEMAIL = "";
    
    String FORMID_REG = "";
    String TABLE = "";
    
    String GROUPID_TEMP = "";
    String GROUPID_APPR = "";
    
    String PREFIX_TEMP = "";
    String PREFIX_APPR = "";
    
    @Override
    public Object execute(Map props) {
        LogUtil.info(this.getClassName(), "User Creation Starting");
        Thread checkingThread = new PluginThread(new Runnable() {
            @Override
            public void run() {
                doCreateUser(props);
            }
        });

        checkingThread.setDaemon(true);
        checkingThread.start();       
        
        return null;
    }

    PluginManager pm = null;
    WorkflowManager wm = null;
    WorkflowAssignment wfa = null;
    
    public void doCreateUser(Map props){
        WorkflowAssignment wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");     
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        String id = appService.getOriginProcessId(wfAssignment.getProcessId());
        
        String acc_type = (props.get("acc_type") == null)?"":props.get("acc_type").toString();
        String app_type = (props.get("app_type") == null)?"":props.get("app_type").toString();

        pm = (PluginManager) props.get("pluginManager");
        wm = (WorkflowManager) pm.getBean("workflowManager");
        
        setFieldNames(app_type);
        
        DBHandler db = new DBHandler();
                
        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
                        
            EmpmObj emp = null;
            
            HashMap hm = getRegApplData(db,id);
            id = CommonUtils.getEmpId_empReg(db, id);   
//            String isOnStaffBehalf = hm.get("c_onStaffBehalf").toString();
            String submitMode = hm.get("c_submit_mode").toString();
            String formType = hm.get("c_form_type").toString();
            
            if(formType.equals("1A")){
                
                db.update(
                        "UPDATE "+Constants.TABLE.EMPREG+" "
                        + "SET c_req_email = c_empl_email_pri "
                        + "WHERE id = '"+id+"'"
                );
                
                db.closeConnection();
                return;
            }
            
            emp = new EmpmObj(db, EmpmObj.BY_ID, id);
            hm = emp.getEmpData();
            
            String mycoid = emp.getMycoid().trim();
            String psmbNo = emp.getHrdcNo().trim();            
            String compName = emp.getCompName().trim();
            String loginMail = emp.getLoginEmail().trim();
            String email = emp.getPrimaryEmail().trim();
            String reqPw = (hm.get("c_req_pw_re")==null)?"":(String)hm.get("c_req_pw_re");
            String userId = "";

//            if(submitMode.equals("OFFLINE") || submitMode.equals("MANUAL")){
//                reqPw = email;                
//                loginMail = loginMail.isEmpty()?email:loginMail;
//                
//                emp.setLoginEmail(loginMail);
//                emp.setLoginPw(email);
//            }
            
            //IF FORM 1 is submitted on behalf
            if(loginMail.isEmpty()){
                loginMail = email;
                emp.setLoginEmail(email);
            }
            if(reqPw.isEmpty()){
                reqPw = email;
                emp.setLoginPw(email);
            }
            
            if(acc_type.equals("temp")){
                userId = createJogetUser(db,"temp", psmbNo, loginMail, compName,email,reqPw);            
                updateProcessRequester(db, userId, wfAssignment.getProcessId());
                wm.activityVariable(wfAssignment.getActivityId(), "reqId", userId);
            }else{
                
                reqPw = new CurrentUser(loginMail).getTempPwMetadata();
                userId = createJogetUser(db,"approved", psmbNo, mycoid, compName,email,reqPw);
                //deleting temp user
//                DeleteUserAndRecord.deleteUser(db, email);
            }
            
            if(!userMapped(db, id, userId)){
                insertUserIdToMapper(id, userId);
            }            
//            updateRegEmpId(id, reg_empl_fk);
            db.closeConnection();
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally{
            db.closeConnection();
        } 
    }
        
    public String createJogetUser(DBHandler db, String type,String psmbNo,String userId_, String empName, String email, String pw) {
        
        UserDao ud = (UserDao) AppUtil.getApplicationContext().getBean("userDao");        
        
        String group = "", username = "", userId;
        
        userId = userId_; 
        username = userId_;
        
        boolean create = true;
        boolean changePw = false;

        if(type.equals("temp")){
//            userId = Constants.USERID_PREFIX_TEMP + psmbNo; 
            
            group = Constants.USER_GROUP.TEMP_USERS;
            
            User user = ud.getUserById(userId);
            
            if(user!=null){
                msg("user created, updating");
                ud.assignUserToGroup(userId, group);
                ud.unassignUserFromGroup(userId, Constants.USER_GROUP.DRAFT_USERS);
                ud.unassignUserFromGroup(userId, Constants.USER_GROUP.REJECTED_USERS);
                create = false;
                
                savePasswordInCurretMetadataForApprovedUser(user);
            }
        }else{
            changePw = true;
            group = 
                    Constants.USER_GROUP.EMPLOYERS + ";" +
                    Constants.USER_GROUP.VIEW_PERMIT_EMPM + ";" +
                    Constants.USER_GROUP.VIEW_PERMIT_TP + ";" +
                    Constants.USER_GROUP.VIEW_PERMIT_CLAIM + ";" +
//                    Constants.USER_GROUP.VIEW_PERMIT_EVENT + ";" +
                    Constants.USER_GROUP.VIEW_PERMIT_GRANT + ";" +
                    Constants.USER_GROUP.VIEW_PERMIT_LEVY ;
        }        
        
        if(create){
            CommonUtils.createJogetUser(userId, username, empName, "", email, pw, group);  
            
            if(!changePw){
                DirectoryManager dm = (DirectoryManager) AppUtil.getApplicationContext().getBean("directoryManager");
                User user = dm.getUserById(userId);

                savePasswordInCurretMetadataForApprovedUser(user);
            }else{               
                
                db.update(
                "UPDATE dir_user_extra SET requiredPasswordChange = 1 WHERE username = '"+userId+"'"
                );
            }
        }        
        return userId;            
    }
    
    

    public void insertUserIdToMapper(String id, String userId) {
        HashMap userHm = new HashMap();
        userHm.put("userId", userId);
        userHm.put("compId", id);
        
        CommonUtils.saveUpdateForm("","bridge_user", "", userHm);    
    }

    private void setFieldNames(String app_type) {
        if(app_type.equals("employer")){
            FLD_EMPLID = "c_empl_id";
            FLD_HRDCNO = "c_hrdc_no";
            FLD_MYCOID = "c_mycoid";
            FLD_COMPNAME = "c_comp_name";
            FLD_REQEMAIL = "c_req_email";
            FLD_REQPW = "c_req_pw_re";
            FLD_REGEMAIL = "c_empl_email_pri";
            
            FORMID_REG = "emp_reg";
            TABLE = "app_fd_empm_reg";
            
            GROUPID_TEMP = "empm_temp_employers";
            GROUPID_APPR = "empm_employers";
            
            PREFIX_TEMP = "temp-";
            PREFIX_APPR = "emp-";
        }else{
            FLD_EMPLID = "c_empl_id";
            FLD_HRDCNO = "c_hrdc_no";
            FLD_MYCOID = "c_mycoid";
            FLD_COMPNAME = "c_sh_com_name";
            FLD_REQEMAIL = "c_sh_email";
            FLD_REQPW = "c_sh_re_con_pw";
            
            FORMID_REG = "stakeholder_mainForm";
            TABLE = "app_fd_empm_stkhldr";
            
            GROUPID_TEMP = "empm_temp_stakeholders";
            GROUPID_APPR = "empm_stakeholders";
            
            PREFIX_TEMP = "temp-stkh-";
            PREFIX_APPR = "stkh-";
        }        
    }

    private boolean permitUserCreation(DBHandler db) {
        String sql = "SELECT * FROM app_fd_empm_dev_ctrl WHERE id = ?";

        HashMap data = db.selectOneRecord(sql, new String[]{Constants.DATA_ID.DEV_CTRL_STP_ID});
        
        if(data != null && data.get("c_account_creation") != null){            
            return Boolean.valueOf(data.get("c_account_creation").toString());           
        }else{
            return false;
        }
    }

    private void updateProcessRequester(DBHandler db, String userId, String processId) {
        String query = "UPDATE SHKProcesses SET ResourceRequesterId = ? WHERE id = ? ";
        int i = db.update(query, new String[]{userId}, new String[]{processId});   
    }

    public boolean userMapped(DBHandler db, String id, String userId) {
        String sql = "SELECT * FROM app_fd_empm_usermap WHERE c_compId = ? AND c_userId = ?";
        HashMap hm = db.selectOneRecord(sql, new String[]{userId, id});
        
        if(hm!=null){
            return true;
        }
        
        return false;
    }

    private HashMap getRegApplData(DBHandler db, String id) {
        String sql = "SELECT c_onStaffBehalf, c_submit_mode, c_form_type FROM "+Constants.TABLE.EMPREG_APPL+" WHERE id = ?";
        HashMap hm = db.selectOneRecord(sql, new String[]{id});
        
        if(hm!= null){
            return hm;
        }
        
        hm=new HashMap();
        hm.put("c_onStaffBehalf", "");
        hm.put("c_form_type", "");
        hm.put("c_submit_mode", "");
        return hm;
    }

    private void savePasswordInCurretMetadataForApprovedUser(User user) {
        if(user!=null){ //For Approved Users            
            new CurrentUser(user.getId()).setTempPwMetadata(CommonUtils.generateRandomPassword(8));
        }
    }
    
}
