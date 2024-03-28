/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.binder;

import com.tms.hrdc.dao.CurrentUser;
import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.datalistAction.DeleteUserAndRecord;
import com.tms.hrdc.defaultPluginTool.AccountCreatorTool;
import com.tms.hrdc.util.AuditTrailUtil;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author faizr
 */
public class EmployerProfileSaveBinder extends WorkflowFormBinder {

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
        return "Used in Employer Profile Update";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Employer Save Profile Binder";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    String unsuccessfulReason = "";
    int unsuccessfulRowsUploaded = 0;
    int successfulRowsUploaded = 0;
    int totalRows = 0;

    public void msg(String msg) {
        LogUtil.info(getName(), msg);
    }

    @Override
    public FormRowSet store(Element element, FormRowSet rowSet, FormData formData) {

        rowSet = super.store(element, rowSet, formData);

        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        FormDefinitionDao formDefinitionDao = (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");
        FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");

//        String id = formData.getPrimaryKeyValue();
        final String id = rowSet.get(0).getId();
        final String hq_no = (String) rowSet.get(0).get("hqId");       
       

        final HashMap appUtilhm = new HashMap();
        appUtilhm.put("appService", appService);
        appUtilhm.put("formDefinitionDao", formDefinitionDao);
        appUtilhm.put("formService", formService);
        appUtilhm.put("formDataDao", formDataDao);

        String formId = super.getFormId();
        CurrentUser cu = new CurrentUser();
        String modifiedBy = cu.getFullName();
        
        DBHandler db = new DBHandler();
        
        try{
            db.openConnection();
            
            String remarks = "";
             //get old mycoid
            String newMycoid = db.selectOneValueFromId(Constants.TABLE.EMPREG, "mycoid", id);
            msg("newMycoid "+newMycoid);            
            
            EmpmObj eo = new EmpmObj(db, EmpmObj.BY_ID, id);
            String mycoid = eo.getMycoid();
            
//            LogUtil.info(this.getClassName(), "EmplID: "+id+", Mycoid: "+mycoid);
            
            ArrayList<HashMap<String, String>> chgeList = AuditTrailUtil.getRegChgeList2(db, eo);
            if(!chgeList.isEmpty()){
                remarks += Constants.SEE_DETAIL_WORD;
                
                new AuditTrailUtil().insertAuditTrail2(db, id, modifiedBy, 
                    "Updated Employer Profile", remarks, false, chgeList);
            }
            
            AuditTrailUtil.saveEmpCacheData(db, eo);
            
            msg("chgeList "+chgeList.toString());
            
            Optional<String> result = chgeList.stream()
                .filter(map -> map.containsKey(Constants.CHANGE_KEYS.FIELD_ID) && map.get(Constants.CHANGE_KEYS.FIELD_ID).equals("c_empl_email_pri"))
                .map(map -> map.get(Constants.CHANGE_KEYS.NEW))
                .findFirst();
            
            if (result.isPresent()) {
                String newEmail = result.get();
                
                db.update(
                        "UPDATE dir_user SET email = ? WHERE id = ?",
                        new String[]{newEmail},
                        new String[]{mycoid}
                );
            }
            
            result = chgeList.stream()
                .filter(map -> map.containsKey(Constants.CHANGE_KEYS.FIELD_ID) && map.get(Constants.CHANGE_KEYS.FIELD_ID).equals("c_mycoid"))
                .map(map -> map.get(Constants.CHANGE_KEYS.OLD))
                .findFirst();
            
            if (result.isPresent()) {
                String oldMycoid = result.get();
                msg("Old Mycoid "+oldMycoid);
                
                String compName =  eo.getCompName();
                String email = eo.getPrimaryEmail();
                String psmbNo = eo.getHrdcNo();
                String reqPw = CommonUtils.generateRandomPassword(8);
                
//                db.update(
//                        "UPDATE dir_user SET username = ? WHERE id = ?",
//                        new String[]{newMycoid},
//                        new String[]{mycoid}
//                );

                AccountCreatorTool act = new AccountCreatorTool();

                String userId = act.createJogetUser(db,"approved", psmbNo, mycoid, compName,email,reqPw);
                
                if(!act.userMapped(db, id, userId)){
                    act.insertUserIdToMapper(id, userId);
                }    
                
                DeleteUserAndRecord.deleteUser(db, oldMycoid);
                //create new joget user xx
                //add new usermap xx
                //create b2c user
                //delete old joget user
                //remove old usermap
                //delete b2c user
                
                //send email
                sendNewUsernameEmail(email, compName, mycoid, oldMycoid, reqPw);
            }
            
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }

 
        return rowSet;
    }

    private void sendNewUsernameEmail(String email, String compName, String mycoid, String oldMycoid, String reqPw) {
        
        String subject = "PEMBERITAHUAN PERTUKARAN MYCOID UNTUK SYARIKAT "+compName;
        String content = "<br />"
                + "Tuan/Puan, <br /><br />"
                + "Merujuk perkara diatas, kod MYCOID anda telah diubah daripada <b>"+oldMycoid+"</b> kepada "
                + "<b>"+mycoid+"</b>. <br />"
                + "Sehubungan dengan itu, anda diminta untuk log masuk ke NCS dengan menggunakan username dan kata laluan seperti berikut: <br />"
                + "<b>USERNAME: "+mycoid+"</b> <br />"
                + "<b>KATA LALUAN: "+reqPw+"</b> <br />"
                + "<br />"
                + "Sekian terima kasih. " ;
        
        CommonUtils.sendEmail(email, "onmtesting15@gmail.com", subject, content, null, null);        
    }
    
}
