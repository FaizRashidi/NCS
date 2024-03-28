/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.util.ArrayList;
import java.util.HashMap;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author faizr
 */
public class ApproverBinder  extends WorkflowFormBinder{
    
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
        return "To process data after changes in approvers";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Approver Binder";
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
    
        super.store(element, rows, formData);
        
        FormRow row = rows.get(0);
        String type = row.getProperty("type");
        String id = row.getId();
        
        DBHandler db = new DBHandler();
        
        try{
            db.openConnection((DataSource)AppUtil.getApplicationContext().getBean("setupDataSource"));
            
            LogUtil.info(this.getClassName(),"Setting approver groups: "+type);
            
            setGroups(db, "", Constants.USER_GROUP.STAFF);
            switch(type){
                case "Employer":                    
                    setGroups(db, id, Constants.USER_GROUP.SV);                    
                    setGroups(db, id, Constants.USER_GROUP.OFFICERS);
                    setGroups(db, id, Constants.USER_GROUP.APPROVERS);
                    setGroups(db, id, Constants.USER_GROUP.ERD_ADMIN);
                break;
                case "Potential Employer":
                    setGroups(db, id, Constants.USER_GROUP.PE_SV);
                    setGroups(db, id, Constants.USER_GROUP.PE_OFFICERS);
                    setGroups(db, id, Constants.USER_GROUP.PE_APPROVERS);
                break;
                case "CKSP":
                    setGroups(db, id, Constants.USER_GROUP.PE_CSKP);
                break;
                case "Engagement":
                    setGroups(db, id, Constants.USER_GROUP.PE_ENG_OFFICER);
                break;
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }
        
        return rows;
    }

    private void setGroups(DBHandler db, String id, String groupName) {
        
        String keyCol = "";
        
        switch(groupName){
            case Constants.USER_GROUP.SV:
                keyCol = "c_sv_fk";
            break;
            case Constants.USER_GROUP.PE_SV:
                keyCol = "c_sv_fk";
            break;
            case Constants.USER_GROUP.OFFICERS:
                keyCol = "c_officer_fk";
            break;
            case Constants.USER_GROUP.PE_OFFICERS:
                keyCol = "c_officer_fk";
            break;
            case Constants.USER_GROUP.APPROVERS:
                keyCol = "c_approver_fk";
            break;
            case Constants.USER_GROUP.PE_APPROVERS:
                keyCol = "c_approver_fk";
            break;
            case Constants.USER_GROUP.STAFF:
                keyCol = "c_staff_fk";
            break;
            case Constants.USER_GROUP.ERD_ADMIN:
                keyCol = "c_erd_admin_fk";
            break;
            case Constants.USER_GROUP.PE_CSKP:
                keyCol = "c_cksp_fk";
            break;
            case Constants.USER_GROUP.PE_ENG_ADMIN:
                keyCol = "c_eng_admin_fk";
            break;
            case Constants.USER_GROUP.PE_ENG_OFFICER:
                keyCol = "c_eng_officer_fk";
            break;
            case Constants.USER_GROUP.PE_ENF_OFFICER:
                keyCol = "c_enforcement_fk";
            break;
        }
        
        String sql = "";
        ArrayList<HashMap<String, String>> stpList = new ArrayList();
        
        if(id.isEmpty()){
            sql = "SELECT distinct c_usr FROM app_fd_empm_appvr_usr_stp "
                    + "WHERE c_staff_fk is not null";
            stpList = db.select(sql);
        }else{
            sql = "SELECT distinct c_usr FROM app_fd_empm_appvr_usr_stp "
                + "WHERE "+keyCol+" = ? ";
            stpList = db.select(sql, new String[]{id});
        }
        
        String grpSql = "SELECT * FROM dir_user_group WHERE groupId = ?";
        ArrayList<HashMap<String, String>> grpList = db.select(grpSql, new String[]{groupName});
        
        for(HashMap hm:stpList){
            String usr = hm.get("c_usr").toString().trim();
            int pos = usrInGrp(usr, grpList);
            
            if(pos>=0){ //if -1 means takde, >0 means ade
                grpList.remove(pos);
                continue;
            }
            
            sql = "INSERT IGNORE INTO dir_user_group(groupId, userId) "
                    + "VALUES (?,?)";
            int i = db.insert(sql, new String[]{groupName, usr});
        }
                
        if(grpList.size()>0){
            for(HashMap hm:grpList){
                String usr = hm.get("userId").toString();

                sql = "DELETE FROM dir_user_group WHERE userId = ?"
                        + " and groupId = ?";
                int i = db.insert(sql, new String[]{usr, groupName});
            }
        }
        
        
    }

    private int usrInGrp(String usr, ArrayList<HashMap<String, String>> grpList) {
        int pos = -1;
        
        for(HashMap hm:grpList){
            int i = grpList.indexOf(hm);
            if(usr.equals(hm.get("userId"))){
                pos = i;
                break;
            }
        }
        
        return pos;
    }
    
}
