/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.util;

import java.util.HashMap;

/**
 *
 * @author faizr
 */
public class EmpModuleSetup {
    DBHandler db;
    String id = "";
    HashMap stpHm = new HashMap();
    
    public EmpModuleSetup(DBHandler db){
        this.db = db;
        this.id = Constants.DATA_ID.MAIN_SETUP_ID;
        
        init();
    }
    
    public void init(){
        stpHm = db.selectOneRecord(
                "SELECT * FROM "+Constants.TABLE.STP_EMPREG+" WHERE id = ?",
                new String[]{id}
        );
    }
    
    public HashMap getSetupData(){
        return stpHm;
    }
    
    public String getCancellationPeriod(){
        if(stpHm!=null){
            return stpHm.get("c_dereg_cancel_period").toString();
        }        
        return "0";
    }
    public String getPeriodToPE(){
        if(stpHm!=null){
            return stpHm.get("c_import_pe_days").toString();
        }        
        return "0";
    }
    
    public String getF4Bypass(){
        if(stpHm!=null){
            return stpHm.get("c_dereg_no_check").toString();
        }        
        return "false";
    }    
    public String getF4BypassOnBehalf(){
        if(stpHm!=null){
            return stpHm.get("c_dereg_bhlf_no_check").toString();
        }        
        return "false";
    }
    public String getF4ABypass(){
        if(stpHm!=null){
            return stpHm.get("c_f4a_dereg_no_check").toString();
        }        
        return "false";
    }    
    public String getF4ABypassOnBehalf(){
        if(stpHm!=null){
            return stpHm.get("c_f4a_dereg_bhlf_no_check").toString();
        }        
        return "false";
    }
    public String getF5Bypass(){
        if(stpHm!=null){
            return stpHm.get("c_f5_dereg_no_check").toString();
        }        
        return "false";
    }    
    
    public String getEmpRegAutoApprove(){
        if(stpHm!=null){
            return stpHm.get("c_auto_approval").toString();
        }        
        return "false";
    }  
}
