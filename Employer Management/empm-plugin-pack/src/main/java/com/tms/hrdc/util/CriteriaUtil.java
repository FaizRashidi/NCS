/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.util;

import com.tms.hrdc.dao.EmpmObj;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author faizr
 */
public class CriteriaUtil {
    
    DBHandler db;
    ArrayList<String> reasonList;
    
    public CriteriaUtil(DBHandler db){
        this.db = db;
    }
    
    public boolean isPassCriteria(EmpmObj emp){
        
        reasonList = new ArrayList();
        boolean pass = true;
        
//        HashMap empHm = getEmpData(db, empId);
        
        String query = "SELECT * FROM app_fd_empm_criteria WHERE id = ?";
        String tempVal = "";
        String empData = "";
        String[] valList;
        
        HashMap qHm = db.selectOneRecord(query, new String[]{Constants.DATA_ID.CRITERIA_SETUP_ID});
        
        if(qHm.get("c_status_emp_num_allowed")!=null && qHm.get("c_status_emp_num_allowed").toString().equals("active")){
            int empNum = Integer.parseInt(StringUtils.isBlank(emp.getEmployerNumber())?"0":emp.getEmployerNumber());
            int alwdNum = qHm.get("c_emp_num_allowed")!=null?Integer.parseInt(qHm.get("c_emp_num_allowed").toString()):0;
                
            if(empNum<alwdNum){
                pass = false;
                reasonList.add("Employee amount is less than allowed employees amount");
            }
        }
        
        if(qHm.get("c_status_excl_industry")!=null && qHm.get("c_status_excl_industry").toString().equals("active")){
            tempVal = qHm.get("c_excl_industry")!=null?qHm.get("c_excl_industry").toString():"";
            valList = tempVal.split(";");
            empData = emp.getIndustrySector();
            
            if(isContain(empData,valList)){
                pass=false;
                reasonList.add("Industry sector is in excluded list");
            }
        }
        
        if(qHm.get("c_status_excl_div")!=null && qHm.get("c_status_excl_div").toString().equals("active")){
            tempVal = qHm.get("c_excl_div")!=null?qHm.get("c_excl_div").toString():"";
            valList = tempVal.split(";");
            empData = emp.getDivSector();
            
            if(isContain(empData,valList)){
                pass=false;
                reasonList.add("Sector division is in excluded list");
            }
        }
        
        if(qHm.get("c_status_excl_sector_code")!=null && qHm.get("c_status_excl_sector_code").toString().equals("active")){
            tempVal = qHm.get("c_excl_sector_code")!=null?qHm.get("c_excl_sector_code").toString():"";
            valList = tempVal.split(";");
//            empData = empHm.get("c_main_sector_code")!=null?empHm.get("c_main_sector_code").toString():"";
            empData = emp.getMainSector();
            
            if(isContain(empData,valList)){
                pass=false;
                reasonList.add("Main sector code is in excluded list");
            }
        }
        
        if(qHm.get("c_status_excl_sub_sector_code")!=null && qHm.get("c_status_excl_sub_sector_code").toString().equals("active")){
            tempVal = qHm.get("c_excl_sub_sector_code")!=null?qHm.get("c_excl_sub_sector_code").toString():"";
            valList = tempVal.split(";");
//            empData = empHm.get("c_sector_code")!=null?empHm.get("c_sector_code").toString():"";
            empData = emp.getSubSector();
            
            if(isContain(empData,valList)){
                pass=false;
                reasonList.add("Sub sector code is in excluded list");
            }
        }
        
        if(qHm.get("c_status_excl_state")!=null && qHm.get("c_status_excl_state").toString().equals("active")){
            tempVal = qHm.get("c_excl_state")!=null?qHm.get("c_excl_state").toString():"";
            valList = tempVal.split(";");
//            empData = empHm.get("c_empl_state")!=null?empHm.get("c_empl_state").toString():"";
            empData = emp.getBusinessState();
            
            if(isContain(empData,valList)){
                pass=false;
                reasonList.add("State is in excluded list");
            }
        }
        
        if(qHm.get("c_status_excl_org_type")!=null && qHm.get("c_status_excl_org_type").toString().equals("active")){
            tempVal = qHm.get("c_excl_org_type")!=null?qHm.get("c_excl_org_type").toString():"";
            valList = tempVal.split(";");
            empData = emp.getOrgType();
            
            if(isContain(empData,valList)){
                pass=false;
                reasonList.add("Org. type is in excluded list");
            }
        }
        
        return pass;
    }
    
    public String getMessage(){
        
        String msg = "";
        
        for(String reason:reasonList){
            if(msg.isEmpty()){
                msg+=reason;
            }else{
                msg+=", "+reason;
            }
        }
        return msg;
    }

    private HashMap getEmpData(DBHandler db, String empId) {
        
        String query = "SELECT * FROM app_fd_empm_reg WHERE id = ?";
        HashMap qHm = db.selectOneRecord(query, new String[]{empId});
        
        if(qHm!=null){
            return qHm;
        }
        
        return qHm;
    }

    private boolean isContain(String empData, String[] valList) {
        
        boolean contain = false;
        
        for(String item:valList){
            if(item.equals(empData)){
                contain = true;
                break;
            }
        }
        
        return contain;
    }
    
}
