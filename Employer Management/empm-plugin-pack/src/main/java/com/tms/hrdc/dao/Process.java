/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.dao;

import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.webservice.models.AssignmentsImpl;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author faizr
 */
public class Process {
    
    public static final String TYPE_E_EMPREG = "empl_reg";
    public static final String TYPE_E_DEREG = "empl_dereg";
    public static final String TYPE_E_DEREG_F5 = "empl_dereg_f5";
    public static final String TYPE_E_DEREG_WD = "empl_dereg_wd";
    public static final String TYPE_E_DEREG_CANCEL = "empl_dereg_cancel";
    public static final String TYPE_E_REQ_CHANGE = "req_change";
    public static final String TYPE_PE_WRITEOFF = "pe_write_off";
    public static final String TYPE_PE_ENGAGEMENT = "pe_egmnt";
    public static final String TYPE_PE_EMG_ACKNOWLEDGE = "pe_egmnt_ack";
    
    public static final String DISTRIBUTION = "dist";
    public static final String CAT_FIELD = "cat_field";
    public static final String CAT_VALUE = "cat_value";
    public static final String CATEGORY = "category";
    public static final String SETUP_ID = "setup_id";
    
    private EmpmObj emp;
    private ArrayList<HashMap<String, String>> svList;
    private String processType;
    private String setupType;
    private String recId;
    private String empId;
    private String processStarter;
    private String table;
    
    HashMap stpHm;
    
    DBHandler db;
    
    public Process(DBHandler db, String processType, String recId){
        this.db = db;
        setRecId(recId);
        setProcessType(processType);
    }
    
    private void setEmpId(String recId, String processType){
        setupType = "Employer";
        switch(processType){
            case TYPE_E_REQ_CHANGE:
                empId = CommonUtils.getEmpId_reqChange(db, recId);
                table = Constants.TABLE.REQUEST_CHANGES;
            break;
            case TYPE_E_DEREG:
                empId = CommonUtils.getEmpId_empDereg(db, recId);
                table = Constants.TABLE.DEREG;
            break;
            case TYPE_E_DEREG_F5:
                empId = CommonUtils.getEmpId_empDeregF5(db, recId);
                table = Constants.TABLE.DEREG;
            break;
            case TYPE_E_DEREG_CANCEL:
                empId = CommonUtils.getEmplId_DeregWD(db, recId);
            break;
            case TYPE_E_DEREG_WD:
                empId = CommonUtils.getEmplId_DeregWD(db, recId);
                
                if(empId.isEmpty()){
                    empId = CommonUtils.getEmplId_DeregWD_F5(db, recId);
                }
            break;
            case TYPE_PE_WRITEOFF:
                empId = CommonUtils.getEmplId_WriteOff(db, recId);
                setupType = "Potential Employer";
                setSV(AssignmentsImpl.getPESVs(db));
                table = Constants.TABLE.POT_EMP_WRITEOFF;
            break;
            case TYPE_PE_EMG_ACKNOWLEDGE:
                empId = CommonUtils.getEmplId_PE_FromEng(db, recId);
                setupType = "Potential Employer";
                setSV(AssignmentsImpl.getPESVs(db));
                table = Constants.TABLE.POT_EMP_ENGAGEMENT;
            break;
            case TYPE_PE_ENGAGEMENT:
                empId = CommonUtils.getEmplId_Egmnt(db, recId);
                setupType = "Engagement";
                setSV(AssignmentsImpl.getEgmntSVs(db));
                table = Constants.TABLE.POT_EMP_ENGAGEMENT;
            break;
            default:
                empId = CommonUtils.getEmpId_empReg(db, recId);
                table = Constants.TABLE.EMPREG_APPL;
        }
        
        if(getSV().isEmpty()){
            setSV(AssignmentsImpl.getSVs(db));
        }
        
        if(!empId.isEmpty()){
            emp = new EmpmObj(db, EmpmObj.BY_ID, empId);
        }else{
            LogUtil.info("PROCESS OBJ", "NO EMP DATA");
        }
        
        setSetupData();
    }
    
    private HashMap setSetupData(){
        String query = "SELECT * FROM app_fd_empm_appvr_stp WHERE c_type = ?";
        
        HashMap stpData = db.selectOneRecord(query, new String[]{setupType});        
        String dist = StringUtils.isBlank(stpData.get("c_dist_conf").toString())?
                                            "Manual Distribution":
                                            stpData.get("c_dist_conf").toString();
        String category = StringUtils.isBlank(stpData.get("c_category").toString())?
                                            "":
                                            stpData.get("c_category").toString();  
        String setup_id = StringUtils.isBlank(stpData.get("id").toString())?
                                            "":
                                            stpData.get("id").toString();  
        
        HashMap catHm = populateCategoryFields(category);
        String cat_field = catHm.get("cat_field").toString();
        String cat_value = catHm.get("cat_value").toString();
        
        if(dist.equals("Round Robin") && !category.isEmpty()){            
        }else{            
            cat_field = "";
            cat_value = "";
        }
        
        stpHm = new HashMap();
        stpHm.put(this.DISTRIBUTION, dist);
        stpHm.put(this.SETUP_ID, setup_id);
        stpHm.put(this.CAT_FIELD, cat_field);
        stpHm.put(this.CAT_VALUE, cat_value);
        stpHm.put(this.CATEGORY, category);
        
        return stpHm;
    }
    
    private HashMap populateCategoryFields(String category){
        
        HashMap hm = new HashMap();
        String cat_field = "", cat_value = "";
        switch(category){
            case "By State":
                cat_field = "c_state";
                cat_value = emp.getBusinessState();
            break;
            
            case "By City":
                cat_field = "c_city";
                cat_value = emp.getBusinessCity();
            break;
            
            case "By Industry":
                cat_field = "c_industry";
                cat_value = emp.getIndustrySector();
            break;  
            
            case "By Sector":
                cat_field = "c_sector_code";
                cat_value = emp.getMainSector();
            break;
        }
        
        hm.put("cat_field", cat_field);
        hm.put("cat_value", cat_value);
        
        return hm;
    }
    

    //1
    public HashMap getSetupData(){
        if(stpHm==null){
            return new HashMap();
        }
        return stpHm;
    }
    
    public DBHandler getDBHandler(){
        return db;
    }
    
    public String getProcessStarter(){
        return processStarter;
    }
    
    public void setProcessStarter(String starter){
        this.processStarter = starter;
    }
    
    public String getSetupType(){
        return setupType;
    }
    
    public EmpmObj getEmpObj(){
        return emp;
    }
    
    public void setRecId(String id){
        this.recId = id;
    }
    
    public String getRecId(){
        return recId;
    }
    
    public void setProcessType(String processType){
        this.processType = processType;
        
        setEmpId(recId, processType);
    }
    
    public String getProcessType(){
        return processType;
    }
    
    public void setSV(ArrayList list){
        svList = list;
    }
    
    public ArrayList<HashMap<String, String>> getSV(){
        
        if(svList==null){
            return new ArrayList();
        }
        
        return svList;
    }
}
