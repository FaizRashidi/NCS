/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.datalistAction;

import com.tms.hrdc.dao.APIManager;
import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.defaultPluginTool.RegisterEmpAPS;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.HttpUtil;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListActionDefault;
import org.joget.apps.datalist.model.DataListActionResult;
import org.joget.commons.util.LogUtil;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.User;
import org.joget.directory.model.service.DirectoryUtil;
import org.joget.directory.model.service.UserSecurity;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONException;

/**
 *
 * @author faizr
 */
public class DeleteUserAndRecord extends DataListActionDefault {

    public String getName() {
        return "HRDC - EMPM - Delete User Datalist Action"; 
    }

    public String getVersion() {
        return "1.0";
    }

    public String getDescription() {
        return "To delete user (Used in development) V2"; 
    }

    public String getLinkLabel() {
        String label = getPropertyString("label");
        if (label == null || label.isEmpty()) {
            label = "Delete User & Record";
        }
        return label;
    }
    
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
    
    public String getConfirmation() {
        String confirm = getPropertyString("confirmation");
        if (confirm == null || confirm.isEmpty()) {
            confirm = "Confirm Delete?";
        }
        return confirm;
    }

    public String getLabel() {
        return "HRDC - EMPM - Delete Employers Button"; 
    }

    public String getClassName() {
        return this.getClass().getName();
    }

    public String getPropertyOptions() {
        String json = "[{\n"
                + "    title : 'Application & User Delete Button',\n"
                + "    properties : [{\n"
                + "        label : 'Label',\n"
                + "        name : 'label',\n"
                + "        type : 'textfield',\n"
                + "        description : 'Delete User & Record',\n"
                + "        value : 'Delete User & Record'\n"
                + "    },{\n" 
                + "            name: 'del_type', " 
                + "            label: 'Delete Type (for Emp. Reg. only)', " 
                + "            type: 'radio', " 
                + "            options : [\n" 
                + "                {value: 'b2c_user', label : 'B2C User Only'}," 
                + "                {value: 'user_staff_only', label : 'Staff Users Only'}," 
                + "                {value: 'user_only', label : 'Users Only'}," 
                + "                {value: 'regAppl', label : 'Registration Application'}," 
                + "                {value: 'dereg', label : 'Deregistration Application'}," 
                + "                {value: 'usermap', label : 'Usermap'},"
                + "                {value: 'reg', label : 'Employers'}," 
                + "                {value: 'pe', label : 'Potential Employers'}" 
                + "            ]\n" 
                + "        }]\n"
                + "}]";
        return json;
    }
    
    @Override
    public DataListActionResult executeAction(DataList dataList, String[] rowKeys) {
        
        DBHandler db = new DBHandler();
        
        try{
            db.openConnection((DataSource)AppUtil.getApplicationContext().getBean("setupDataSource"));
        }catch(Exception e){
            
        }
        
        String message = "";
        if(rowKeys.length != 0){
            
            for(String id:rowKeys){
                
                ArrayList<String> userIds = new ArrayList();                
                String regAplId = "";
                String regId = "";
                String userId = "";
                        
                switch(getPropertyString("del_type")){
                    
                    case "user_staff_only":                        
                        id = db.selectOneValueFromId("app_fd_empm_persons_stp", "c_email", id);
                        deleteUser(db, id);                        
                    break;
                    
                    case "b2c_user":
                        deleteB2CUser(id);
                    break;
                    
                    case "user_only":
                        deleteUser(db, id);
                    break;
                    
                    case "regAppl":
                        regId = getRegId(db, id);
                        userIds = getUsers(db, regId);
                        
                        deleteRegAppl(db, id);
                        deleteEmp(db, regId);
                        
                        for(String user: userIds){
                            deleteUser(db, user);
                        }                       
                        
                    break;
                    
                    case "usermap":
                        HashMap uHm = getDataFromUsermap(db, id);   
                        if(uHm!=null){
                            regId = uHm.getOrDefault("c_compId", "").toString();
                            userId = uHm.getOrDefault("c_userId", "").toString();
                        }
                        regAplId = getRegApplId(db, regId);
                        
                        deleteEmp(db, regId);
                        deleteUser(db, userId);
                        deleteRegAppl(db, regAplId);
                    break;
                    
                    case "reg":
                        regAplId = getRegApplId(db, id);
                        userIds = getUsers(db, id);
                        deleteEmp(db, id);
                        deleteRegAppl(db, regAplId);
                        for(String user: userIds){
                            deleteUser(db, user);
                        }       
                    break;
                    
                    case "dereg":
                        deleteDeregAppl(db, id);
                    break;
                    
                    case "pe":
                        deletePE(db, id);
                    break;
                }
                
            }
            
        }else{
            message = "No Data Selected";
        }
        
        db.closeConnection();
        
        DataListActionResult result = new DataListActionResult();
        result.setType(DataListActionResult.TYPE_REDIRECT);
        result.setUrl("REFERER");
        
        if(!message.isEmpty()){
            result.setMessage(message);
        }
        
        return result;
        
    }

    private ArrayList getUsers(DBHandler db, String id) {
        String query = "SELECT c_userId FROM "+Constants.TABLE.USERMAP+" j " +
                        "where j.c_compId = ?";
        String[] cond = {id};
        
        ArrayList<HashMap<String, String>> hmList = db.select(query, cond);
        ArrayList userIds = new ArrayList();
        
        for(HashMap hm:hmList){
            userIds.add(hm.getOrDefault("c_userId", ""));
        }
        
        return userIds;
    }
    
    public static void disableUser(DBHandler db, String id){
        LogUtil.info("DeleteUserAndRecord", "Disabling user "+id);
        UserDao ud = (UserDao) AppUtil.getApplicationContext().getBean("userDao");
        User user = ud.getUserById(id);
        
        UserSecurity us = DirectoryUtil.getUserSecurity();
        
        if(user==null){
            LogUtil.info("DeleteUserAndRecord", "User not found");
            return;
        }
        
        user.setActive(0);
        ud.updateUser(user);
        
        if(us!=null && user!=null){
            us.updateUserPostProcessing(user);
            us.updateUserProfilePostProcessing(user);
            return;
        }
        
        int active = user.getActive();
        
        if(active!=0){
            LogUtil.info("DeleteUserAndRecord", id+" - User still active");
            
            db.update("UPDATE dir_user SET active = ? WHERE id = ?",
                    new String[]{"0"},
                    new String[]{id});
        }else{
            LogUtil.info("DeleteUserAndRecord", id+" - User set inactive completed");
        }
    }
    
    public static void enableUser(DBHandler db, String id){
        UserDao ud = (UserDao) AppUtil.getApplicationContext().getBean("userDao");
        User user = ud.getUserById(id);
        
        if(user==null){
            return;
        }
        
        user.setActive(1);
        ud.updateUser(user);
    }
    
    public static void deleteUser(DBHandler db, String id){
        LogUtil.info("Deleting user", id);
        UserSecurity us = DirectoryUtil.getUserSecurity();
        UserDao ud = (UserDao) AppUtil.getApplicationContext().getBean("userDao");
        User user = ud.getUserById(id);
        
        if(us!=null && user!=null){
            us.deleteUserPostProcessing(user.getUsername());
        }
        ud.deleteUser(id);
        
        if(ud.getUserById(id)!=null){
            
        }
        
        String[] cond = {id};

        String query = "DELETE FROM dir_user_group WHERE userId = ?";	
        db.delete(query, cond);

        query = "DELETE FROM dir_user_role WHERE userId = ?"; 
        db.delete(query, cond);
        
        query = "DELETE FROM dir_employment WHERE userId = ?";       
        db.delete(query, cond);

        query = "DELETE FROM dir_user WHERE id = ?";       
        db.delete(query, cond);

        query = "DELETE FROM app_fd_empm_usermap WHERE c_userId = ?";        
        db.delete(query, cond);
        
        query = "DELETE FROM app_fd_empm_persons_stp WHERE c_email = ?";        
        db.delete(query, cond);

        query = "DELETE FROM "+Constants.TABLE.USERMAP+" WHERE c_userId = ?";        
        db.delete(query, new String[]{id});
        
        //delete b2c
        HashMap hm = db.selectOneRecord(
                "select * from app_fd_stp_b2c_users WHERE c_user_principal_name = ? ",
                new String[]{id+RegisterEmpAPS.B2C_DOMAIN_TEST}
        );
        
        if(hm==null){
            return;
        }
        
        deleteB2CUser(hm.get("id").toString());
    }
    
    public static void deleteB2CUser(String b2cUserId){
        LogUtil.info("Deleting b2c user", b2cUserId);
        
        String url = Constants.B2C.USERDELETE + "?b2cUserId=" + b2cUserId;

        APIManager mgr = new APIManager(APIManager.APIType.APS);
        HashMap header = new HashMap();
        header.put("api_id", mgr.getApi_id());
        header.put("api_key", mgr.getApi_key());
        
        HttpUtil http = new HttpUtil();
        http.setHeader(header);
        try {
            http.sendDeleteRequest(url);
            int statusCode = http.getStatusCode();

            LogUtil.info("DATA DELETE USER B2C", 
                "Status Code :"+Integer.toString(statusCode));
            
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } catch (KeyManagementException ex) {
            ex.printStackTrace();
        } catch (KeyStoreException ex) {
            ex.printStackTrace();
        } catch (JSONException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void deleteRegAppl(DBHandler db, String id){
        String sql = "DELETE FROM "+Constants.TABLE.EMPREG_APPL+" WHERE id = ?";
        db.delete(sql, new String[]{id});
        
        WorkflowManager wm = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
        
        List<HashMap<String, String>> procList = CommonUtils.getRunningProcessList(db, id);
        for(HashMap procHm:procList){
            String procId = procHm.get("ActivityProcessId")!=null?procHm.get("ActivityProcessId").toString():"";
            LogUtil.info("ABORTING PROCESS", "ID - "+procId);
            wm.processAbort(procId);
        }
    }
    
    public void deleteDeregAppl(DBHandler db, String id){
        
        String sql = "SELECT * FROM "+Constants.TABLE.DEREG+" WHERE id = ? ";
        HashMap hm = db.selectOneRecord(sql, new String[]{id});
        
        if(hm== null){
            return;
        }
        
        String deregEmpId = hm.get("c_dreg_emp_id")==null?"":hm.get("c_dreg_emp_id").toString();
        String mergerEmpId = hm.get("c_merge_comp_id")==null?"":hm.get("c_merge_comp_id").toString();
        String f5_fk = hm.get("c_f5_fk")==null?"":hm.get("c_f5_fk").toString();
        
        sql = "UPDATE "+Constants.TABLE.EMPREG+" SET c_data_status = ?, c_last_move = ? WHERE id = ?";
        db.update(sql, new String[]{Constants.STATUS.EMP.REGISTER_APPROVED, Constants.LAST_MOVEMENT.REGISTERED}, new String[]{deregEmpId});
        
        if(!mergerEmpId.isEmpty()){
            sql = "UPDATE "+Constants.TABLE.EMPREG+" SET c_data_status = ?, c_last_move = ? WHERE id = ?";
            db.update(sql, new String[]{Constants.STATUS.EMP.REGISTER_APPROVED, Constants.LAST_MOVEMENT.REGISTERED}, new String[]{mergerEmpId});
        }  
        
        WorkflowManager wm = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
        
        sql = "DELETE FROM "+Constants.TABLE.DEREG_F5+" WHERE id = ?";
        db.delete(sql, new String[]{f5_fk});
        
        List<HashMap<String, String>> procList = CommonUtils.getRunningProcessList(db, f5_fk);        
        for(HashMap procHm:procList){
            String procId = procHm.get("ActivityProcessId")!=null?procHm.get("ActivityProcessId").toString():"";
            LogUtil.info("ABORTING PROCESS", "ID - "+procId);
            wm.processAbort(procId);
        }
        
        sql = "DELETE FROM "+Constants.TABLE.DEREG+" WHERE id = ?";
        db.delete(sql, new String[]{id});
        
        procList = CommonUtils.getRunningProcessList(db, id);
        for(HashMap procHm:procList){
            String procId = procHm.get("ActivityProcessId")!=null?procHm.get("ActivityProcessId").toString():"";
            LogUtil.info("ABORTING PROCESS", "ID - "+procId);
            wm.processAbort(procId);
        }
        
        String  deregWd = db.selectOneValueFromTable(Constants.TABLE.DEREG_WD, "id", "c_dereg_id", id);
        sql = "DELETE FROM "+Constants.TABLE.DEREG_WD+" WHERE c_dereg_id = ?";
        db.delete(sql, new String[]{id});   
        
        procList = CommonUtils.getRunningProcessList(db, deregWd);
        for(HashMap procHm:procList){
            String procId = procHm.get("ActivityProcessId")!=null?procHm.get("ActivityProcessId").toString():"";
            LogUtil.info("ABORTING PROCESS F4 WD", "ID - "+procId);
            wm.processAbort(procId);
        }
        
        deregWd = db.selectOneValueFromTable(Constants.TABLE.DEREG_WD, "id", "c_dereg_id", f5_fk);
        sql = "DELETE FROM "+Constants.TABLE.DEREG_WD+" WHERE c_dereg_id = ?";
        db.delete(sql, new String[]{f5_fk});   
        
        procList = CommonUtils.getRunningProcessList(db, deregWd);
        for(HashMap procHm:procList){
            String procId = procHm.get("ActivityProcessId")!=null?procHm.get("ActivityProcessId").toString():"";
            LogUtil.info("ABORTING PROCESS F5 WD", "ID - "+procId);
            wm.processAbort(procId);
        }
        
        sql = "DELETE FROM app_fd_empm_form4 WHERE c_fk = ?";
        db.delete(sql, new String[]{id});
        
        sql = "DELETE FROM app_fd_empm_form5 WHERE id = ?";
        db.delete(sql, new String[]{id});
    }
    
    public void deletePotEmp(DBHandler db, String id){
        
    }
    
    public void deleteEmp(DBHandler db, String id){
        String sql = "DELETE FROM "+Constants.TABLE.EMPREG+" WHERE id = ?";
        db.delete(sql, new String[]{id});
        
        sql = "DELETE FROM "+Constants.TABLE.BRANCH_RUNNO+" WHERE c_empId = ?";
        db.delete(sql, new String[]{id});
        
        sql = "DELETE FROM "+Constants.TABLE.EMPREG_TEMP+" WHERE id = ?";
        db.delete(sql, new String[]{id});
        
        sql = "DELETE FROM "+Constants.TABLE.EMP_OTHER_CONTACT_DETAILS+" WHERE c_fk = ?";
        db.delete(sql, new String[]{id});
    }

    private HashMap getDataFromUsermap(DBHandler db, String id) {
        String query = "SELECT * FROM "+Constants.TABLE.USERMAP+" WHERE id = ?";
        String[] cond = {id};
        
        HashMap hm = db.selectOneRecord(query, cond);
                
        return hm;
    }

    private boolean isDeregData(DBHandler db, String id) {
        String query = "SELECT * FROM app_fd_empm_dereg h WHERE id = ?";
        HashMap hm = db.selectOneRecord(query, new String[]{id});  
        
        if(hm!=null){
            return true;
        }else{
            return false;
        }
    }

    private void deleteDeregData(DBHandler db, String id) {
        String query = "DELETE FROM app_fd_empm_dereg WHERE id = ?";
        db.delete(query, new String[]{id});  
    }    

    private String getRegId(DBHandler db, String id) {
        String sql = "SELECT * FROM "+Constants.TABLE.EMPREG_APPL+" WHERE id = ?";
        
        HashMap hm = db.selectOneRecord(sql, new String[]{id});
        
        if(hm!=null){
            return hm.get("c_empl_fk").toString();
        }
        
        return null;
    }
    
    private String getRegApplId(DBHandler db, String id) {
        String sql = "SELECT * FROM "+Constants.TABLE.EMPREG_APPL+" WHERE c_empl_fk = ?";
        
        HashMap hm = db.selectOneRecord(sql, new String[]{id});
        
        if(hm!=null){
            return hm.get("id").toString();
        }
        
        return null;
    }

    private void deletePE(DBHandler db, String empId) {
        
//        String sql = "SELECT c_emp_fk, c_batch FROM "+Constants.TABLE.POT_EMP+" WHERE id = ?";
        String sql = "SELECT id, c_emp_fk, c_batch FROM "+Constants.TABLE.POT_EMP+" WHERE c_emp_fk = ?";
        HashMap hm = db.selectOneRecord(sql, new String[]{empId});
        
        LogUtil.info("DELETEING", empId);
        
        String id = "", batchId = "";
        if(hm!=null){
            id = hm.get("id")!=null?hm.get("id").toString():"";
            batchId = hm.get("c_batch")!=null?hm.get("c_batch").toString():"";           
        }
        
        deleteEmp(db, empId);
        
        sql = "DELETE FROM "+Constants.TABLE.POT_EMP+" WHERE id = ? ";
        int i = db.delete(sql, new String[]{id});
        
        LogUtil.info("DELETEING PE ", Integer.toString(i));
        
        sql = "DELETE FROM "+Constants.TABLE.POT_EMP_UPLOAD_DATA+" WHERE c_emp_fk = ?";
        i = db.delete(sql, new String[]{empId});
        
        LogUtil.info("DELETEING UPLOAD ITEM ", Integer.toString(i));
        
        String upl_count = db.selectOneValueFromTable(
                "SELECT COUNT(id) as count FROM "+Constants.TABLE.POT_EMP_UPLOAD_DATA+" WHERE c_batch = ?", 
                new String[]{batchId});
        int upl_count_int = 0;
        try{
            LogUtil.info("BATCH COUNT", upl_count);
            upl_count_int = Integer.parseInt(upl_count);
            
            if(upl_count_int == 0){
                sql = "DELETE FROM "+Constants.TABLE.POT_EMP_UPLOAD+" WHERE id = ?";
                i = db.delete(sql, new String[]{batchId});
                
                LogUtil.info("DELETING UPLOAD BATCH ", Integer.toString(i));
            }
            
        }catch(Exception e){
            
        }
        
        sql = "DELETE FROM "+Constants.TABLE.POT_EMP_EMPREG_TEMP+" "
                + "WHERE id = (SELECT c_temp_emp_fk FROM "+Constants.TABLE.POT_EMP_ENGAGEMENT+" e WHERE e.c_pe_fk = ? LIMIT 1 )";
        i = db.delete(sql, new String[]{id});
        
        sql = "DELETE FROM "+Constants.TABLE.POT_EMP_ENG_EVENT+" "
                + "WHERE c_e_fk = (SELECT id FROM "+Constants.TABLE.POT_EMP_ENGAGEMENT+" e WHERE e.c_pe_fk = ? LIMIT 1 )";
        i = db.delete(sql, new String[]{id});
        
        sql = "DELETE FROM "+Constants.TABLE.POT_EMP_ENGAGEMENT+" WHERE c_pe_fk = ?";
        i = db.delete(sql, new String[]{id});        
        
    }
}
