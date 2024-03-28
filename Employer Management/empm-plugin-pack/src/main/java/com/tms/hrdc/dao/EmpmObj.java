/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.dao;

import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.KeywordDictionary;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author faizr
 */
public class EmpmObj {
    
    private String org_type = "";
    private String reg_type = "";    
    private String mycoid = "";
    private String comp_name = "";    
    private String comp_email = "";
    
    private String address_full = "";
    private String address1 = "";
    private String address2 = "";
    private String address3 = "";
    
    private HashMap compData = null;
    
    public static String BY_ID = "id";
    public static String BY_MYCOID = "mycoid";
    public static String BY_HRDC_NO = "hrdcNo";
    
    DBHandler db;
    
    public EmpmObj(DBHandler db){
//        setValueFields(db);
    }
    
    /**
     * 
     * @param db - DBHandler Instance Object
     * @param value_type - Type of Key (BY_MYCOID, BY_HRDCNO, BY_ID)
     * @param value - Key Value
     * @param fromTempData - True if trying to get Emp Data From app_fd_empm_reg_temp
     */
    public EmpmObj(DBHandler db, String value_type, String value){
        
        this.db = db;
        String query = "SELECT * FROM "+Constants.TABLE.EMPREG+" s";
        
        if(value_type.equals(BY_ID)){
            query+=" WHERE id = '"+value+"'";
        }else if(value_type.equals(BY_MYCOID)){
            query+=" WHERE c_mycoid = '"+value+"'";
        }else{
            query+=" WHERE c_hrdc_no = '"+value+"'";
        }
        
        query += " ORDER BY dateCreated DESC LIMIT 1 ";
        
        compData = db.selectOneRecord(query);
    }
    
    /**
     * 
     * @param db - DBHandler Instance Object
     * @param value_type - Type of Key (BY_MYCOID, BY_HRDCNO, BY_ID)
     * @param value - Key Value
     * @param fromTempData - True if trying to get Emp Data From app_fd_empm_reg_temp
     */
    public EmpmObj(DBHandler db, String value_type, String value, boolean fromTempData){
        
        this.db = db;
        String query = "SELECT * FROM "+Constants.TABLE.EMPREG_TEMP+" s";
        
        if(value_type.equals(BY_ID)){
            query+=" WHERE id = '"+value+"'";
        }else if(value_type.equals(BY_MYCOID)){
            query+=" WHERE c_mycoid = '"+value+"'";
        }else{
            query+=" WHERE c_hrdc_no = '"+value+"'";
        }
        
        compData = db.selectOneRecord(query);
    }
    
    public String getField(String column){
        if(compData == null){
            return "";
        }         
        return compData.get(column)==null?"":compData.get(column).toString();
    }
    
    public boolean insertField(String column, String value){
        if(compData == null){
            return false;
        }               
        compData.put(column, value);
        return true;
    }
    
    public int updateField(String column, String value){
        if(compData == null){
            return 0;
        }        
        
        String query = "UPDATE app_fd_empm_reg SET "+column+" = ? WHERE id = ?";
        return db.update(query, new String[]{value}, new String[]{getId()});
    }
    
    public String getTelNo(){
        if(compData==null){
            return "";
        }
        
        return compData.get("c_empl_tel_no_pri").toString();
    }
        
    public String getFullBusinessAddress(){               
        address_full = getBusinessAddress1()
                +( getBusinessAddress2().isEmpty()?"":", "+getBusinessAddress2() )
                +( getBusinessAddress3().isEmpty()?"":", "+getBusinessAddress3() )
                +( getBusinessCityName().isEmpty()?"":", "+getBusinessCityName() )
                +( getBusinessPostcode().isEmpty()?"":", "+getBusinessPostcode() )
                +( getBusinessCountryName().isEmpty()?"":", "+getBusinessCountryName() );
        return address_full;   
    }    
    public String getBusinessAddress1(){        
        if(compData!=null){
            address1 = compData.get("c_empl_address").toString();
        }        
        return address1;
    }        
    public void setBusinessAddress1(String address1){        
        this.address1 = address1;
    }
    public String getBusinessAddress2(){        
        if(compData!=null){
            address2 = compData.get("c_empl_address2").toString();
        }        
        return address2;
    }
    public void setBusinessAddress2(String address2){        
        this.address2 = address2;
    }
    public String getBusinessAddress3(){        
        if(compData!=null){
            address3 = compData.get("c_empl_address3").toString();
        }        
        return address3;
    }
    public void setBusinessAddress3(String address3){        
        this.address3 = address3;
    }
    
    public String getBusinessCity(){        
        if(compData == null){
            return "";
        }      
        return compData.get("c_bu_city").toString();   
    }
    
    public String getBusinessCityName(){        
        if(compData == null){
            return "";
        }      
        return db.selectOneValueFromId("app_fd_stp_city", 
                "c_city", compData.get("c_bu_city").toString());   
    }
    
    public String getBusinessState(){        
        if(compData == null){
            return "";
        }        
        
        return compData.get("c_bu_state").toString();   
    }
    
    public String getBusinessStateName(){        
        if(compData == null){
            return "";
        }        
        return db.selectOneValueFromId("app_fd_stp_state", 
                "c_state", 
                compData.get("c_bu_state").toString());
    }
    
    public String getBusinessCountry(){        
        if(compData == null){
            return "";
        }        
        
        return compData.get("c_bu_country").toString();   
    }
    
    public String getBusinessCountryName(){        
        if(compData == null){
            return "";
        }        
        return db.selectOneValueFromId("app_fd_stp_country", 
                "c_country", 
                compData.get("c_bu_country").toString());
    }
    
    public String getBusinessPostcode(){        
        if(compData == null){
            return "";
        }        
        
        return compData.get("c_bu_postcode").toString();   
    }
    
    public String getOrgType(){        
        if(compData == null){
            return "";
        }        
        
        return compData.get("c_empl_org_type").toString();   
    }
    
    public String getRegType(){        
        if(compData == null){
            return "";
        }        
        
        return compData.get("c_empl_reg_type").toString();   
    }
    
    public String getSMECategId(){
        if(compData == null){
            return "";
        }               
        return compData.get("c_sme_category").toString();      
    }    
    
    public String getIndustrySector(){
        if(compData == null){
            return "";
        }               
        return compData.get("c_industry_sector").toString();      
    }
    
    public String getDivSector(){        
        if(compData == null){
            return "";
        }        
        
        return compData.get("c_div").toString();      
    }
    
    public String getMainSector(){
        if(compData == null){
            return "";
        }        
        
        return compData.get("c_main_sector_code").toString();    
    }
    
     public String getClassSector(){
        if(compData == null){
            return "";
        }        
        
        return compData.get("c_class_code").toString();    
    }
    
    public String getSubSector(){
        if(compData == null){
            return "";
        }                
        return compData.get("c_sector_code").toString(); 
    }
    
    public String getSubSectorName(){
        
        String sql = "select CONCAT(c_descr, '(',c_sub_sector_code,')') as sub_sector  "
                + "from app_fd_stp_sub_sector where id = ?  ";
        
        String sector_name = 
                db.selectOneValueFromTable(sql, new String[]{getSubSector()} );
             
        return sector_name; 
    }
    
    public String getEmployerNumber(){
        if(compData == null){
            return "0";
        }
        
        return compData.get("c_total_empl")==null?
                        (compData.get("c_total_my_empl")==null?"0":
                            compData.get("c_total_my_empl").toString()):
                            compData.get("c_total_empl").toString();
    }
    
    public HashMap getOneContactPerson(){
        ArrayList ocp = getContactPerson();
        if(ocp.size()>0){
            return (HashMap) ocp.get(0);
        }
        
        return null;
    }
    
    public ArrayList getContactPerson(){
        String sql = "SELECT * FROM "
                +Constants.TABLE.EMP_OTHER_CONTACT_DETAILS+" WHERE c_fk = ? " ;
        ArrayList<HashMap<String, String>> hm = 
                db.select(sql, new String[]{this.getId()});
        
        return hm;
    }

    public String getMycoid(){
        if(compData == null){
            return "";
        }     
        
        if(compData.get("c_mycoid").toString().isEmpty()){
            return compData.get("c_hrdc_no").toString();
        }
        
        return compData.get("c_mycoid").toString();
    }
    
    public void setMycoid(String mycoid){
        LogUtil.info(this.getClass().toString(), "Old Mycoid "+getMycoid());
        String query = "UPDATE app_fd_empm_reg SET c_mycoid = ? WHERE id = ?";
        db.update(query, new String[]{mycoid}, new String[]{getId()});
        
        if(compData != null){
            compData.put("c_mycoid", mycoid);
        }
        
        LogUtil.info(this.getClass().toString(), "Old Mycoid "+getMycoid());
    }
    
    public String getBranchMycoid(){
        if(compData == null){
            return "";
        }     
        
        return compData.get("c_branch_mycoid").toString();
    }
    
    public String getHrdcNo(){
        if(compData == null){
            return "";
        }        
        return compData.get("c_hrdc_no").toString();
    }
    
    public String getLoginEmail(){
        if(compData == null){
            return "";
        }

//        if(!StringUtils.isBlank(compData.getOrDefault("c_req_email", "").toString())){
//            return compData.get("c_req_email").toString();
//        }else{
//            return getPrimaryEmail();
//        }

        return compData.getOrDefault("c_req_email", "").toString();
    }
    
    public void setLoginEmail(String loginEmail){
        int i = db.update(
                "UPDATE "+Constants.TABLE.EMPREG+" SET c_req_email = ? WHERE id =  ? ",
                new String[]{loginEmail},
                new String[]{getId()}
        );
    }

    public void setLoginPw(String pw){
        int i = db.update(
                "UPDATE "+Constants.TABLE.EMPREG+" SET c_req_pw = ?, c_req_pw_re = ? WHERE id =  ? ",
                new String[]{pw, pw},
                new String[]{getId()}
        );
    }
    
    public String getPrimaryEmail(){
        if(compData == null){
            return "";
        }               
        return compData.get("c_empl_email_pri").toString();
    }
    
    public String getCompName(){
        if(compData == null){
            return "";
        }               
        return compData.get("c_comp_name").toString();
    }
    
    public String getId(){
        if(compData == null){
            return "";
        }        
        return compData.get("id").toString();
    }
    
    public boolean isHQ(){
        if(compData == null){
            return false;
        } 
        
        return compData.get("c_empl_reg_type").toString().equals("HQ");
    }
    
    public String getEmpStatus(){
        if(compData == null){
            return "";
        }         
        return compData.get("c_emp_status").toString();
    }
    
    public String getDataStatus(){
        if(compData == null){
            return "";
        }         
        return compData.get("c_data_status").toString();
    }
    
    public String getLastMoveStatus(){
        if(compData == null){
            return "";
        }         
        return compData.get("c_last_move").toString();
    }
    
    private String duplEmpId = "";
    
    public String getStatus(){
        if(compData == null){
            return "";
        }       
        
        String remark = "";
        String emp_status = compData.get("c_emp_status").toString();
        String emp_status_det = compData.get("c_data_status").toString();   
        String last_move = compData.get("c_last_move").toString();   
        
//        LogUtil.info("EMPLOYER STATUS", emp_status+" "+emp_status_det);
        
        HashMap<String, String> duplBatchHm = new HashMap();
        
        if(emp_status.equals(Constants.STATUS.EMP.ACTIVE)){
            if(emp_status_det.equals(Constants.STATUS.EMP.DEREGISTERING)){
                remark = "Active, Under deregistration process";
            }else{
                remark = "Registered, Active";
            }            
        }else{            
            switch(emp_status_det){
                case Constants.STATUS.EMP.REGISTER_REJECTED:
                    remark = "Submitted Form 1/1A but rejected";
                case Constants.STATUS.EMP.REGISTERING:
                    remark = "In registration process";
                break;
                case Constants.STATUS.EMP.DEREGISTER_APPROVED:
                    remark = "Deregistered, Inactive";
                break;                
                case Constants.STATUS.EMP.UPLOADED:          
                    duplBatchHm = getBatchOfDiffUpload();    
                    if(duplBatchHm!=null){
                        String batch = duplBatchHm.getOrDefault("batch", "").toString();
                        duplEmpId = duplBatchHm.getOrDefault("empId", "").toString();
                        String status = duplBatchHm.getOrDefault("status", "").toString();
                        
                        remark = "Data merged with employer status <b>"+status+"</b> in batch <b>("+batch+")</b>. ";
                    }else{
                        remark = ""; //get batch id
                    }
                break;
                case Constants.STATUS.EMP.POTENTIAL_EMPLOYER:
                    duplBatchHm = getBatchOfDiffUpload();    
                    if(duplBatchHm!=null){
                        String batch = duplBatchHm.getOrDefault("batch", "").toString();
                        duplEmpId = duplBatchHm.getOrDefault("empId", "").toString();
                        String status = duplBatchHm.getOrDefault("status", "").toString();
                        
                        remark = "Data merged with employer status <b>"+status+"</b> in batch <b>("+batch+")</b>. ";
                    }else{
                        remark = ""; //get batch id
                    }
                break;
                case Constants.STATUS.EMP.TRUE_POTENTIAL_EMPLOYER:
                    remark = "As True Potential Employer";
                break;
                case Constants.STATUS.EMP.DIRTY_LISTED:
                    remark = "In Potential Employer - Dirty Listed";
                break;
                default:
                    remark = " Status: "+last_move;
            }
        }
        return remark;   
    }
    
    public String getDuplEmpId(){
        return duplEmpId;
    }
        
    private HashMap<String, String> getBatchOfDiffUpload() {
        
        //check if there are any duplicate in potential
        HashMap batch = db.selectOneRecord(
                "select u.c_batch as batch, r.id as empId, r.c_last_move as status "+
                "from app_fd_empm_reg r\n" +
                "INNER JOIN app_fd_empm_pe_potEmp p ON p.c_emp_fk = r.id\n" +
                "INNER JOIN app_fd_empm_pe_file_upl u ON u.id = p.c_batch\n" +
                "where r.id = ? \n" +
                "order by r.dateCreated desc limit 1",
                new String[]{getId()}
        );
        
        if(batch!=null){
            return batch;
        }
        
        // if never went to potential
        batch = db.selectOneRecord(
                "select u.c_batch as batch, r.id as empId, r.c_last_move as status  \n" +
                "from app_fd_empm_reg r\n" +
                "INNER JOIN app_fd_empm_pe_upl_data d ON d.c_emp_fk = r.id\n" +
                "INNER JOIN app_fd_empm_pe_file_upl u ON u.id = d.c_batch\n" +
                "where r.id = ? \n" +
                "order by r.dateCreated desc limit 1",
                new String[]{getId()}
        );
        
        if(batch!=null){
            return batch;
        }
        
        return batch;
    }
    
    public HashMap getEmpData(){     
        if(compData!=null){
            return compData;
        }
        return new HashMap();
    }
    
    
    public String generateBranchMyCoID(String mycoid){
//        return getMycoid()+"B"+getTotalBranchCount(hq_empId);
        return getMycoid()+"B"+String.format("%03d", getTotalBranchCount(mycoid));
    }
    
    public int getTotalBranchCount(String mycoid){
        
        int count = initHQBranchCount();
        count++;
        
        int upd = db.update(
                "UPDATE "+Constants.TABLE.BRANCH_RUNNO+" SET c_running_no = ? WHERE c_mycoid = ? ",
                new String[]{Integer.toString(count)},
                new String[]{mycoid}
        );
        
        return count;
    }
    
    public int initHQBranchCount(){
        HashMap rnHm = db.selectOneRecord(
                "SELECT * FROM "+Constants.TABLE.BRANCH_RUNNO+" WHERE c_mycoid = ?", 
                new String[]{getMycoid()}
        );
        
        int runno = 0;
        
        if(rnHm != null){
            runno = rnHm.get("c_running_no")==null?0:Integer.parseInt(rnHm.get("c_running_no").toString());
        }else{
            HashMap hm = new HashMap();
            
            hm.put("mycoid", getMycoid());
            hm.put("empId", getId());
            hm.put("running_no", "0");
            
            CommonUtils.saveUpdateForm2("", Constants.FORM_ID.EMP_BRANCH_RUNNO, "", hm);
        }
        
        return runno;       
    }
    
    public int getTotalUserCount(){
        String query = "SELECT count(*) userCount FROM app_fd_empm_persons_stp s "
                + "where s.c_compId = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{getId()});
        
        if(hm == null){
            return 0;
        }
        
        return Integer.parseInt(hm.get("userCount").toString());
    }
    
    public String generateNewUserId(){
        return "emp-"+getHrdcNo()+"-"+String.format("%06d", getTotalUserCount()-1);
    }
    
    public String generateUsername(){
        return getMycoid()+"U"+getTotalUserCount();
    }
    
    public static String createEmployerData(HashMap empHm, String empStatus, String lastAct){
        
        empHm.put("emp_status", empStatus);
        empHm.put("data_status", lastAct);

        return CommonUtils.saveUpdateForm2("",
                Constants.FORM_ID.EMP_MAIN_FORM,"", empHm); 
    }    
    
    /*
    @param db : db conn
    @param empId : self explanatory
    Saved 2 times bcos the second is needed as ref for checking for value changes
    */
    public static String duplicateEmpData(DBHandler db, String empId){
        String sql = "SELECT * FROM "
                        +Constants.TABLE.EMPREG
                        +" WHERE id = ? ";
        HashMap empHm = db.selectOneRecord(sql, new String[]{empId});
        
        empHm = processDuplication(db, empHm);    
        
        String tempId = CommonUtils.saveUpdateForm2("", Constants.FORM_ID.EMP_TEMP_MAIN_FORM, "", empHm);        
        CommonUtils.saveUpdateForm2("", Constants.FORM_ID.EMP_TEMP_MAIN_FORM, Constants.BASE_DATA_PREFIX+tempId, empHm);        
        
        return tempId;        
    }
    
//    public static String createFromTempData(DBHandler db, String empId){
//        String sql = "SELECT * FROM "
//                        + Constants.TABLE.POT_EMP_EMPREG_TEMP
//                        + " WHERE id = ? ";
//        HashMap empHm = db.selectOneRecord(sql, new String[]{empId});
//
//        empHm = processDuplication(db, empHm);
//        empHm.put("emp_status", Constants.STATUS.EMP.INACTIVE);
//        empHm.put("data_status", Constants.STATUS.EMP.POTENTIAL_EMPLOYER);
//        empHm.put("last_move", Constants.LAST_MOVEMENT.POTENTIAL);
//
//        return CommonUtils.saveUpdateForm2("",
//                    Constants.FORM_ID.EMP_MAIN_FORM, "", empHm);
//
//    }
    
    public static HashMap processDuplication(DBHandler db, HashMap dataHm){
        
        HashMap dupHm = new HashMap();
        KeywordDictionary kd = new KeywordDictionary(db);                
        dataHm = kd.removeBasicKeys((HashMap) dataHm, db);         
        
        if(dataHm!=null){                        
            Set set = dataHm.entrySet();
            Iterator iterator = set.iterator();
            while (iterator.hasNext()) {
                Map.Entry mentry = (Map.Entry) iterator.next();

                String colName = mentry.getKey().toString();
                String colVal = mentry.getValue().toString();

                if(colName.isEmpty() || colName.equals("id")){
                    continue;
                }

                if(colName.startsWith("c_")){
                    colName = colName.replaceFirst("c_", "");
                }

                dupHm.put(colName, colVal);
            }
        }
        
        return dupHm;
    }
    
//    public static void copyTempAuditTrail(DBHandler db, String oldID, String newID){
//        ArrayList<HashMap<String, String>> audtList = db.select(
//                "SELECT * FROM "+Constants.TABLE.AUDIT+" WHERE c_fk = ?",
//                new String[]{oldID}
//        );
//
//        for(HashMap aud:audtList){
//
//            String status = aud.get("c_status").toString();
//            String remarks = aud.get("c_remarks").toString();
//            String createdByName = aud.get("createdByName").toString();
//            String dateCreated = aud.get("dateCreated").toString();
//            String format = "yyyy-MM-dd HH:mm:ss";
//
//            if(dateCreated.contains(".")){
//                format = "yyyy-MM-dd HH:mm:ss.S";
//            }
//
//            HashMap nAud = new HashMap();
//            nAud.put("status", status);
//            nAud.put("remarks", remarks);
//            nAud.put("dateCreated", CommonUtils.set_DT_String2Date(dateCreated, format));
//            nAud.put("createdByName", createdByName);
//            nAud.put("fk", newID);
//
//            String audId = CommonUtils.saveUpdateForm2("", Constants.FORM_ID.AUDIT_TRAIL, "", nAud);
//
//            LogUtil.info("PE Changer Button", "Importing audit trail "+aud.toString()+", id ==> "+audId);
//        }
//    }
    
//    public static void copyTempOthContactUsr(DBHandler db, String oldID, String newID){
//        ArrayList<HashMap<String, String>> audtList = db.select(
//                "SELECT * FROM "+Constants.TABLE.EMP_OTHER_CONTACT_DETAILS+" WHERE c_fk = ?",
//                new String[]{oldID}
//        );
//
//        for(HashMap aud:audtList){
//            String name = aud.get("c_name").toString();
//            String tel_no = aud.get("c_tel_no").toString();
//            String desg = aud.get("c_designation").toString();
//            String email = aud.get("c_email").toString();
//            String createdByName = aud.get("createdByName").toString();
//            String dateCreated = aud.get("dateCreated").toString();
//            String format = "yyyy-MM-dd HH:mm:ss";
//
//            if(dateCreated.contains(".")){
//                format = "yyyy-MM-dd HH:mm:ss.S";
//            }
//
//            HashMap nAud = new HashMap();
//            nAud.put("c_name", name);
//            nAud.put("c_tel_no", tel_no);
//            nAud.put("c_designation", desg);
//            nAud.put("c_email", email);
//            nAud.put("dateCreated", CommonUtils.set_DT_String2Date(dateCreated, format));
//            nAud.put("createdByName", createdByName);
//            nAud.put("fk", newID);
//
//            CommonUtils.saveUpdateForm2("", Constants.FORM_ID.EMP_REG_SUBFORM_OTHERCONTACTS, "", aud);
//        }
//    }

    public void updateAllbranch() { //c_mycoid like 'REM1234B%'
        int i = db.update("UPDATE app_fd_empm_reg "
                + "SET c_hq_id = '"+getId()+"', c_hq_mycoid = '"+getMycoid()+"' "
                + "WHERE c_mycoid like '"+getMycoid()+"B%'");
    }

    public boolean isDeregisteredBefore(){

        HashMap dereg = db.selectOneRecord(
                "SELECT * FROM "+Constants.TABLE.DEREG+" WHERE c_dreg_emp_id = ? " +
                    "AND c_flow_status IN (?,?,?) " +
                    "ORDER BY dateCreated DESC LIMIT 1",
                new String[]{getId(), "APPROVED","FORM 4 APPROVED","FORM 4A APPROVED"}
        );

        if(dereg!=null){
            return true;
        }
        return false;
    }

    public void setCurrentStatusRemark(String status){
        db.update(
                "UPDATE app_fd_empm_reg SET c_data_status = ? WHERE id = ?",
                new String[]{status},
                new String[]{getId()}
        );
    }
    
}
