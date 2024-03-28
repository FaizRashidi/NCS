/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.util;

import com.tms.hrdc.dao.EmpmObj;
import org.apache.commons.lang3.StringUtils;
import org.joget.commons.util.LogUtil;

import java.util.*;

/**
 *
 * @author faizr
 */
public class EmpUtil {
    
    private HashMap<String, Integer> hm = new HashMap();
    DBHandler db;
    
    public EmpUtil(){
        hm = new HashMap();
        
        hm.put(Constants.LAST_MOVEMENT.NEW, 1);
        hm.put(Constants.LAST_MOVEMENT.POTENTIAL, 2);
        hm.put(Constants.LAST_MOVEMENT.TRUE_POTENTIAL, 3);
        hm.put(Constants.LAST_MOVEMENT.LETTER_SENT, 4);
        hm.put(Constants.LAST_MOVEMENT.LETTER_UNDELIVERED, 5);
        hm.put(Constants.LAST_MOVEMENT.LETTER_DELIVERED, 6);
        hm.put(Constants.LAST_MOVEMENT.FORM_1_RECEIVED, 7);
        hm.put(Constants.LAST_MOVEMENT.FORM_1A_RECEIVED, 8);
        hm.put(Constants.LAST_MOVEMENT.FORM_1_REJECTED, 9);
        hm.put(Constants.LAST_MOVEMENT.FORM_1A_REJECTED, 10);
        hm.put(Constants.LAST_MOVEMENT.FORM_1_QUERY, 11);
        hm.put(Constants.LAST_MOVEMENT.FORM_1A_QUERY, 12);
        hm.put(Constants.LAST_MOVEMENT.REGISTERED, 13);
        hm.put(Constants.LAST_MOVEMENT.REREGISTERED, 14);
        hm.put(Constants.LAST_MOVEMENT.FORM_1A_APPROVED, 15);
        hm.put(Constants.LAST_MOVEMENT.COMPLAINT_TO_ENFORCEMENT, 16);
        hm.put(Constants.LAST_MOVEMENT.VISITED_BY_ENFORCEMENT, 17);
        hm.put(Constants.LAST_MOVEMENT.NOTICE_ENFORCEMENT, 18);
        hm.put(Constants.LAST_MOVEMENT.COMPLAINT_TO_CKSP, 19);
        hm.put(Constants.LAST_MOVEMENT.VISITED_BY_CKSP, 20);
        hm.put(Constants.LAST_MOVEMENT.NOTICE_CKSP, 21);
        hm.put(Constants.LAST_MOVEMENT.COMPLAINT_TO_CKSP, 22);
        hm.put(Constants.LAST_MOVEMENT.NFA, 23);
        hm.put(Constants.LAST_MOVEMENT.PROSECUTION, 24);
        hm.put(Constants.LAST_MOVEMENT.DEREGISTRATION_APPLY_FORM4, 25);
        hm.put(Constants.LAST_MOVEMENT.DEREGISTRATION_APPLY_FORM4A, 26);
        hm.put(Constants.LAST_MOVEMENT.DEREGISTRATION_APPLY_FORM4_5, 27);
        hm.put(Constants.LAST_MOVEMENT.DEREGISTRATION_QUERY, 28);
        hm.put(Constants.LAST_MOVEMENT.DEREGISTRATION_REJECTED, 29);
        hm.put(Constants.LAST_MOVEMENT.DEREGISTRATION_FORM_4A, 30);
        hm.put(Constants.LAST_MOVEMENT.DEREGISTRATION_FORM_4, 31);
        hm.put(Constants.LAST_MOVEMENT.DEREGISTRATION_FORM_4_5, 32);
    }
    
    public boolean setStatus(DBHandler db, EmpmObj emp, String status){
        
        String curr_status = emp.getLastMoveStatus();
        int newStatsVal = hm.getOrDefault(status, -1);
        int currStatsVal = hm.getOrDefault(curr_status, -1);
        
        boolean updated = false;
        
        if(newStatsVal>currStatsVal){
            db.update("UPDATE app_fd_empm_reg SET c_last_move = ? WHERE id = ?",
                    new String[]{status},
                    new String[]{emp.getId()}
            );
            updated = true;
        }
        
        if(status.equals(Constants.LAST_MOVEMENT.FORM_1_RECEIVED) ||
                status.equals(Constants.LAST_MOVEMENT.FORM_1A_RECEIVED)){
            
            db.update("UPDATE app_fd_empm_reg SET c_last_move = ? WHERE id = ?",
                    new String[]{status},
                    new String[]{emp.getId()}
            );
            updated = true;            
        }
        
        return updated;
    }

    public static String createOrOverrideEmpFromTemplate(DBHandler db, String tempId, String existingEmpId){

        HashMap newEmpHm = new HashMap();
        String prefix = "c_";
        String sql = "SELECT * FROM "
                + Constants.TABLE.POT_EMP_EMPREG_TEMP
                + " WHERE id = ? ";
        HashMap empHm = db.selectOneRecord(sql, new String[]{tempId});

        if(empHm!=null){
            Set set = empHm.entrySet();
            Iterator iterator = set.iterator();
            while (iterator.hasNext()) {
                Map.Entry mentry = (Map.Entry) iterator.next();

                String colName = mentry.getKey().toString();
                String colVal = mentry.getValue().toString();


                if(colName.isEmpty() || colName.equals("id")){
                    continue;
                }

                if(StringUtils.isBlank(colVal)){
                    continue;
                }

                if(colName.startsWith("c_")){
                    colName = colName.replaceFirst("c_", "");
                }

                if(colName.startsWith("c_")){
                    colName = colName.substring(prefix.length());
                }

                newEmpHm.put(colName, colVal);
            }
        }
        newEmpHm.put("emp_status", Constants.STATUS.EMP.INACTIVE);
        newEmpHm.put("data_status", Constants.STATUS.EMP.POTENTIAL_EMPLOYER);
        newEmpHm.put("last_move", Constants.LAST_MOVEMENT.POTENTIAL);

        return CommonUtils.saveUpdateForm2("",
                Constants.FORM_ID.EMP_MAIN_FORM, existingEmpId, newEmpHm);
    }

    public static void copyTempAuditTrail(DBHandler db, String oldID, String newID){
        db.update(
                "UPDATE "+Constants.TABLE.AUDIT+" SET c_fk = ? WHERE c_fk = ?",
                new String[]{newID},
                new String[]{oldID}
        );
    }

}
