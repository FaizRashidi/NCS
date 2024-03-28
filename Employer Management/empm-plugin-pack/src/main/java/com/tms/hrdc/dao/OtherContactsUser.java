/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.dao;

import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import java.util.HashMap;

/**
 *
 * @author faizr
 */
public class OtherContactsUser {
    
    private String name = "";
    private String telno = "";
    private String designation = "";
    private String email = "";
    
    private boolean saveData = false;
        
    public void saveContact(String emplid){
        HashMap hm = new HashMap();
        
        hm.put("name", name);
        hm.put("tel_no", telno);
        hm.put("designation", designation);
        hm.put("email", email);
        hm.put("fk", emplid);
        
        if(saveData){
            CommonUtils.saveUpdateForm2("", 
                    Constants.FORM_ID.EMP_REG_SUBFORM_OTHERCONTACTS, "", hm);
        }
    }
    
    public void setName(String tname){
        name = tname;
        saveData = true;
    }
    public String getName(){
        return name;
    }
    public void setTelNo(String ttelno){
        telno = ttelno;
        saveData = true;
    }
    public String getTelNo(){
        return telno;
    }
    public void setDesg(String desg){
        designation = desg;
        saveData = true;
    }
    public String getDesg(){
        return designation;
    }
    public void setEmail(String temail){
        email = temail;
        saveData = true;
    }
    public String getEmail(){
        return email;
    }
    
}
