/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.util;

import java.util.ArrayList;
import java.util.HashMap;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author faizr
 */
public class MasterSetupData {
    
    public static String getCountryByPostcode(DBHandler db, String postcode) {        
        String query = "select \n" +
                        "l.c_country,\n" +
                        "l.c_state,\n" +
                        "l.c_district,\n" +
                        "l.c_city,\n" +
                        "l.c_location\n" +
                        "FROM app_fd_stp_location l\n" +
                        "WHERE l.c_postcode = ?";
        
        HashMap q_result = db.selectOneRecord(query, new String[]{postcode});
        if(q_result != null){
            return q_result.get("c_country").toString();
        }        
        return "";
    }
    
    public static String getStateByPostcode(DBHandler db, String postcode) {        
        String query = "select \n" +
                        "l.c_country,\n" +
                        "l.c_state,\n" +
                        "l.c_district,\n" +
                        "l.c_city,\n" +
                        "l.c_location\n" +
                        "FROM app_fd_stp_location l\n" +
                        "WHERE l.c_postcode = ?";
        
        HashMap q_result = db.selectOneRecord(query, new String[]{postcode});
        if(q_result != null){
            return q_result.get("c_state").toString();
        }        
        return "";
    }
    
    public static String getDistrictByPostcode(DBHandler db, String postcode) {        
        String query = "select \n" +
                        "l.c_country,\n" +
                        "l.c_state,\n" +
                        "l.c_district,\n" +
                        "l.c_city,\n" +
                        "l.c_location\n" +
                        "FROM app_fd_stp_location l\n" +
                        "WHERE l.c_postcode = ?";
        
        HashMap q_result = db.selectOneRecord(query, new String[]{postcode});
        if(q_result != null){
            return q_result.get("c_district").toString();
        }        
        return "";
    }
    
    public static String getCityByPostcode(DBHandler db, String postcode) {        
        String query = "select \n" +
                        "l.c_country,\n" +
                        "l.c_state,\n" +
                        "l.c_district,\n" +
                        "l.c_city,\n" +
                        "l.c_location\n" +
                        "FROM app_fd_stp_location l\n" +
                        "WHERE l.c_postcode = ?";
        
        HashMap q_result = db.selectOneRecord(query, new String[]{postcode});
        if(q_result != null){
            return q_result.get("c_city").toString();
        }        
        return "";
    }
    
    public static String getCountryLabel(DBHandler db, String country) {
        
        String query = "SELECT * FROM app_fd_stp_country WHERE id = '"+country+"' ";
        
        HashMap q_result = db.selectOneRecord(query);
        if(q_result != null){
            return q_result.get("c_country").toString();
        }
        
        return "";
    }
    
    
    public static String getCountryId(DBHandler db, String country) {
        
        String query = "SELECT * FROM app_fd_stp_country WHERE c_country like '%"+country+"%' ";
        
        HashMap q_result = db.selectOneRecord(query);
        if(q_result != null){
            return q_result.get("id").toString();
        }
        
        return "";
    }
    
    public static String getStateLabel(DBHandler db, String state) {
        
        String query = "SELECT * FROM app_fd_stp_state WHERE id = '"+state+"' ";
        
        HashMap q_result = db.selectOneRecord(query);
        if(q_result != null){
            return q_result.get("c_state").toString();
        }
        
        return "";
    }
    
    public static String getStateId(DBHandler db, String state) {
        
        String query = "SELECT * FROM app_fd_stp_state WHERE c_state like '%"+state+"%' ";
        
        HashMap q_result = db.selectOneRecord(query);
        if(q_result != null){
            return q_result.get("id").toString();
        }
        
        return "";
    }
    
    public static String getCityLabel(DBHandler db, String cityId) {
        String query = "SELECT * FROM app_fd_stp_city WHERE id = '"+cityId+"'";
        
        ArrayList<HashMap<String, String>> q_result = db.select(query);
        if(q_result != null && q_result.size()>0){
            return q_result.get(0).get("c_city").toString();
        }else{
//            LogUtil.info("getCity", "takde city");
        }
        
        return "";
    }
    
    public static String getCityId(DBHandler db, String city) {
        String query = "SELECT * FROM app_fd_stp_city WHERE c_city like '%"+city+"%' ";
        
        ArrayList<HashMap<String, String>> q_result = db.select(query);
        if(q_result != null && q_result.size()>0){
            return q_result.get(0).get("id").toString();
        }else{
//            LogUtil.info("getCity", "takde bang");
        }
        
        return "";
    }
    
    public static HashMap getIndustrySectorData(DBHandler db, String code){
        HashMap hm = db.selectOneRecord(
                "SELECT id, concat(c_industry_sector_code, ' - ', c_industry_sector) as label "
                    + "FROM app_fd_stp_industry_sector "
                    + "WHERE c_industry_sector_code = ?", 
                new String[]{code}
        );
        
        return hm;
    }
    
    public static String getIndustrySectorCodeFromDiv(DBHandler db, String code){
        
        HashMap hm = db.selectOneRecord(
                "select i.c_industry_sector_code from app_fd_stp_industry_div d\n" +
                "INNER JOIN app_fd_stp_industry_sector i ON i.id = d.c_industry_sector\n" +
                "WHERE d.c_div_code = ?", 
                new String[]{code}
        );
        
        if(hm!=null){
            code = hm.get("c_industry_sector_code").toString();
        }
        
        return code;
    }
    
    public static HashMap getDivSectorData(DBHandler db, String code){
        HashMap hm = db.selectOneRecord(
                "SELECT id, concat(c_div_code, ' - ', c_descr) as label "
                    + "FROM app_fd_stp_industry_div "
                    + "WHERE c_div_code = ?", 
                new String[]{code}
        );
        
        return hm;
    }
    
    public static HashMap getMainSectorData(DBHandler db, String code){
        HashMap hm = db.selectOneRecord(
                "SELECT id, concat(c_main_sector_code, ' - ', c_descr) as label "
                    + "FROM app_fd_stp_main_sector "
                    + "WHERE c_main_sector_code = ?", 
                new String[]{code}
        );
        
        return hm;
    }
    
    public static HashMap getClassSectorData(DBHandler db, String code){
        HashMap hm = db.selectOneRecord(
                "SELECT id, concat(c_sector_class_code, ' - ', c_descr) as label "
                    + "FROM app_fd_stp_class_sector "
                    + "WHERE c_sector_class_code = ?", 
                new String[]{code}
        );
        
        return hm;
    }
    
    public static HashMap getSubSectorData(DBHandler db, String code){
        HashMap hm = db.selectOneRecord(
                "SELECT id, concat(c_sub_sector_code, ' - ', c_descr) as label "
                    + "FROM app_fd_stp_sub_sector "
                    + "WHERE c_sub_sector_code = ?", 
                new String[]{code}
        );
        
        return hm;
    }
}
