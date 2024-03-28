/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.dao.APIManager;
import com.tms.hrdc.dao.EmpmObj;
import static com.tms.hrdc.defaultPluginTool.RegisterEmpAPS.B2C_DOMAIN_TEST;
import com.tms.hrdc.util.B2CUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.HttpUtil;

import java.util.HashMap;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.commons.util.StringUtil;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.User;
import org.joget.directory.model.service.DirectoryUtil;
import org.joget.directory.model.service.UserSecurity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class AccountCreatorBinder extends WorkflowFormBinder{
    
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
        return "Used in Employer Management to process account creation for companies' subusers";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - SubUsers Creation Binder";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }
    
    @Override
    public String getPropertyOptions() {
       return AppUtil.readPluginResource(getClass().getName(), "/properties/archive_binderx.json", null, true, "message/archive_binder");
    }
    
    public HashMap groupMap(){
        HashMap hm = new HashMap();
        
        hm.put("grnt", Constants.USER_GROUP.VIEW_PERMIT_GRANT);
        hm.put("clm", Constants.USER_GROUP.VIEW_PERMIT_CLAIM);
        hm.put("evnt_management", Constants.USER_GROUP.VIEW_PERMIT_EVENT);
        hm.put("empm", Constants.USER_GROUP.VIEW_PERMIT_EMPM);
        hm.put("hrdclevy", Constants.USER_GROUP.VIEW_PERMIT_LEVY);
        
        return hm;
    }
    
    private void msg(String msg){
        LogUtil.info(this.getClassName(), msg);
    }
    
    @Override
    public FormRowSet store(Element element, FormRowSet rows, FormData formData) {
        
        String formId = super.getFormId();      
        FormRow row =rows.get(0);
        
        DBHandler db = new DBHandler();
        try {
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
            
            if(formId.equals("change_pw")){
                String userID = row.get("userID").toString();
                String username = row.get("username").toString();
                String newPw = row.get("newPassword").toString();            

                updateUserTest(db, userID, username, newPw);
                // update user
                db.closeConnection();
                return rows;
            }
        
            super.store(element, rows, formData);

            String email = (String) row.get("email");
            String password = (String) row.get("password_re");
            String firstName = (String) row.get("firstName");
            String lastName = (String) row.get("lastName");
            String compId = (String)row.get("compId");
            String apps_permitted = (String)row.get("apps_permitted");
            String telNo = row.get("tel_no").toString();
            
            String[] apps = {};
            if(apps_permitted.contains(";")){
                apps = apps_permitted.split(";");
            }
            
            String groups = Constants.USER_GROUP.EMPLOYERS;
            
            for(String app:apps){
                if(groupMap().containsKey(app)){
                    groups+=groups.isEmpty()?groupMap().get(app).toString():";"+groupMap().get(app).toString();
                }
            }
            
            LogUtil.info(this.getClassName(), firstName+" "+lastName+" g "+groups);
            
            EmpmObj emp = new EmpmObj(db,EmpmObj.BY_ID, compId);
            String psmbNo = emp.getHrdcNo();
            
            if(StringUtils.isBlank(email) || StringUtils.isBlank(password) ||
                StringUtils.isBlank(firstName) ||
                StringUtils.isBlank(compId) || StringUtils.isBlank(apps_permitted)){
                //send Error
                formData.addFormError(this.getFormId(), "There are empty values.. ");
            }else{
                                
                boolean created = CommonUtils.createJogetUser(email, email, firstName+" "+lastName,
                    psmbNo, email, password, groups);  
                
                if(created){
                    CommonUtils.insertIntoUserMap(email, compId);
                }             
                
                //TODO checkifB2cUserExist
                
                HttpUtil http = new HttpUtil();
                APIManager mgr = new APIManager(APIManager.APIType.APS);
                
                B2CUtil b2c = new B2CUtil(db, http, mgr);
                
                JSONObject body = buildB2CJSONbody(email, firstName+" "+lastName, psmbNo, password, telNo);
                body.put(email, rows);
                b2c.createB2CUser(body);
            }

        } catch (Exception ex) {
            
        }finally{
            db.closeConnection();
        }
        
        return rows;
    }
    
    private void updateUserTest(DBHandler db, String userId, String username, String pw){
        UserSecurity us = DirectoryUtil.getUserSecurity();

        UserDao ud = (UserDao) AppUtil.getApplicationContext().getBean("userDao");
        User user = ud.getUserById(userId);

        String password = SecurityUtil.decrypt(pw);

        if (user != null) {
            msg("Updating user pw -> "+user.getUsername());
            
            if(us!=null){
                user.setPassword(us.encryptPassword(username, password));
//                user.setPassword(us.encryptPassword(userId, password));
            }else{
                user.setPassword(StringUtil.md5Base16(password));
            }            
            user.setConfirmPassword(password);
            
            ud.updateUser(user);
            
            int i = db.update(
                    "UPDATE "+Constants.TABLE.EMPREG+" SET c_req_pw = ?, c_req_pw_re = ? WHERE c_req_email = ?",
                    new String[]{password, password},
                    new String[]{username
                    }
            );
            
            msg("PASSWORD UPDATED!");
        }
    }
    
    private JSONObject buildB2CJSONbody(String username, String name, String psmbNo, String pw, String telno) throws JSONException {
        
        String upn = username.contains("@")?username.split("@")[0]:username;
        
        JSONObject userPayload = new JSONObject();
        userPayload.put("accountEnabled", true);                        //true
        userPayload.put("displayName", name);                           //mycoid
        userPayload.put("givenName", username);                         //Company name
        userPayload.put("surName", psmbNo);                             //psmb no
        userPayload.put("mailNickName", upn);                           //email without the @domain part        
        if(!telno.isEmpty()){
            userPayload.put("mobilePhone", telno);                      //telno
        }
        userPayload.put("userPrincipalName", upn+"@"+B2C_DOMAIN_TEST);        //mycoid + '@hrdcorpb2cdev.onmicrosoft.com' (registerd b2c issuer donmain)
        
        JSONArray mailArray = new JSONArray();
        mailArray.put(username);
        userPayload.put("otherMails", mailArray);                       //mails.  object
        
        JSONObject pwObj = new JSONObject();
        pwObj.put("forceChangePasswordNextSignIn", true);
        pwObj.put("password", pw);                                      // the P
        
        userPayload.put("passwordProfile", pwObj);                         //passwordProfile obj
        
        JSONArray identities = new JSONArray();
        JSONObject identity = new JSONObject();
        identity.put("signInType", "emailAddress");                     //type : userName for username type,  emailAddress if login by email
        identity.put("issuer", B2C_DOMAIN_TEST);                        //issuer domain - hrdcorpb2cdev.onmicrosoft.com
        identity.put("issuerAssignedId", username);                     //login id
        
        identities.put(identity);
        
        userPayload.put("identities", identities);                      //important user logins object
                
        return userPayload;
    }
    
    
    
}
