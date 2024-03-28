/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.dao;

import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;

import java.util.ArrayList;
import java.util.Set;
import java.util.spi.CurrencyNameProvider;
import java.util.stream.Collectors;

import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.directory.dao.UserMetaDataDao;
import org.joget.directory.model.Group;
import org.joget.directory.model.Role;
import org.joget.directory.model.User;
import org.joget.directory.model.UserMetaData;
import org.joget.directory.model.service.DirectoryManager;
import org.joget.workflow.model.service.WorkflowUserManager;

/**
 *
 * @author faizr
 */
public class CurrentUser {
    private User user;
    private DirectoryManager dm;
    private final String TEMP_KEY = "TEMP_KEY";
    private final String TEMP_VALUE = "TEMP_VALUE";
    private boolean isAdmin = false;
    
    
    public CurrentUser(){
        WorkflowUserManager wum = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");
        this.user = wum.getCurrentUser();
    }
    
    public CurrentUser(String id){
        dm = (DirectoryManager) AppUtil.getApplicationContext().getBean("directoryManager");
        this.user = dm.getUserById(id);
    }

    public CurrentUser byEmployerId(DBHandler db, String emplId){
        String userId = db.selectOneValueFromId(Constants.TABLE.EMPREG,
                "c_req_email", emplId);

        return new CurrentUser(userId);
    }

    public ArrayList<String> getGroup(){
        Set<Group> grpSet = user.getGroups();
        ArrayList<String> grp_list = (ArrayList<String>) grpSet.stream().map(Group::getId).collect(Collectors.toList());
//        LogUtil.info("groups", grp_list.toString());
        return grp_list;
    }
    
    public boolean isAdmin(){
        if(user!=null){
            Set<Role> set = user.getRoles();
            
            for(Role role:set){
                if(role.getId().equals("ROLE_ADMIN")){
                    isAdmin = true;
                }
            }
        }
        
        return isAdmin;
    }
    
    public boolean isCurrentUserHRDCOfficer(){
        ArrayList permittedGrpList = new ArrayList();

        permittedGrpList.add("hrdc_empm_admin");

        permittedGrpList.add("hrdc_empm_pe_enf_officer");
        permittedGrpList.add("hrdc_empm_pe_eng_officer");
        permittedGrpList.add("hrdc_empm_pe_approvers");
        permittedGrpList.add("hrdc_empm_pe_cksp");
        permittedGrpList.add("hrdc_empm_pe_officer");
        permittedGrpList.add("hrdc_empm_pe_sv");

        permittedGrpList.add("hrdc_empm_staff");
        permittedGrpList.add("hrdc_empm_erdAdmin");
        permittedGrpList.add("hrdc_empm_sv");
        permittedGrpList.add("hrdc_empm_approver");
        permittedGrpList.add("hrdc_empm_officer");

        Set<Group> grpSet = user.getGroups();
        boolean inPermittedGroup = grpSet.stream()
                .anyMatch( g -> permittedGrpList.contains(g.getId() ));

        return inPermittedGroup;
    }

    public boolean inEmpmPermittedGroups(){
        ArrayList permittedGrpList = new ArrayList();

        permittedGrpList.add("hrdc_empm_admin");

        permittedGrpList.add("hrdc_empm_pe_enf_officer");
        permittedGrpList.add("hrdc_empm_pe_eng_officer");
        permittedGrpList.add("hrdc_empm_pe_approvers");
        permittedGrpList.add("hrdc_empm_pe_cksp");
        permittedGrpList.add("hrdc_empm_pe_officer");
        permittedGrpList.add("hrdc_empm_pe_sv");

        permittedGrpList.add("hrdc_empm_staff");
        permittedGrpList.add("hrdc_empm_erdAdmin");
        permittedGrpList.add("hrdc_empm_sv");
        permittedGrpList.add("hrdc_empm_approver");
        permittedGrpList.add("hrdc_empm_officer");

        permittedGrpList.add("hrdc_empm_employers");
        permittedGrpList.add("hrdc_staffPermit_view_empm");
        permittedGrpList.add("hrdc_empm_branch_employers");

        permittedGrpList.add("hrdc_empm_draft");
        permittedGrpList.add("hrdc_empm_temp");

        Set<Group> grpSet = user.getGroups();
        boolean inPermittedGroup = grpSet.stream()
                .anyMatch( g -> permittedGrpList.contains(g.getId() ));

        return inPermittedGroup;
    }
    
    public boolean isNull(){
        boolean userNotExist = true;
        
        if(user!=null){
            userNotExist = false;
        }
        
        return userNotExist;
    }
    
    public String getUsername(){
        if(user!=null){
            return user.getUsername();
        }
        
        return "";
    }
    
    public String getId(){
        if(user!=null){
            return user.getId();
        }
        
        return "";
    }
    
    public String getFirstName(){
        if(user!=null){
            return user.getFirstName();
        }
        return "";
    }
    
    public String getLastName(){
        if(user!=null){
            return user.getLastName();
        }
        return "";
    }
    
    public String getFullName(){
        if(user!=null){
            return user.getFirstName()+" "+user.getLastName();
        }
        return "";
    }
    
    public String getEmail(){
        if(user!=null){
            return user.getEmail();
        }
        return "";
    }
    
    public String getTempPwMetadata(){
        if(user!=null){
            UserMetaDataDao dao = (UserMetaDataDao) AppUtil.getApplicationContext().getBean("userMetaDataDao");
            UserMetaData usecret = dao.getUserMetaData(user.getId(), this.TEMP_KEY);            
            String pw = usecret.getValue().isEmpty()?"":SecurityUtil.decrypt(usecret.getValue());
            return pw;
        }
        return "";
    }
    
    public void setTempPwMetadata(String pw){
        UserMetaDataDao dao = (UserMetaDataDao) AppUtil.getApplicationContext().getBean("userMetaDataDao");
        UserMetaData umOTP = new UserMetaData();
        
        dao.deleteUserMetaData(user.getId(), this.TEMP_KEY);
        
        umOTP.setUsername(user.getId());
        umOTP.setKey(this.TEMP_KEY);
        umOTP.setValue(SecurityUtil.encrypt(pw));
        
        dao.addUserMetaData(umOTP);
    }
}
