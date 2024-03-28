/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.dao;

import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.MasterSetupData;
import com.tms.hrdc.util.WordCleanser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.joget.commons.util.PluginThread;

/**
 *
 * @author faizr
 */
public class TempPotEmp {
    private String org_type = "";
    private String reg_type = "";    
    private String mycoid = "";
    private String comp_name = "";    
    private String comp_email = "";
    
    private String address_full = "";
    private String address1 = "";
    private String address2 = "";
    private String address3 = "";
    private String postcode = "";
    private String city = "";
    private String state = "";
    private String country = "";
    
    private String bu_address1 = "";
    private String bu_address2 = "";
    private String bu_address3 = "";
    private String bu_postcode = "";
    private String bu_city = "";
    private String bu_state = "";
    private String bu_country = "";
    
    private String primary_contact_telno = "";
    private String primary_contact_email = "";
    
    private String business_entity = "";
    private String sub_business_entity = "";
    private String business_nature = "";
    
    private String industry_sector = "";
    private String industry_sector_label = "";
    private String div = "";
    private String div_label = "";
    private String main_sector_code = "";
    private String main_sector_label = "";
    private String class_code = "";
    private String class_label = "";
    private String sector_code = "";
    private String sector_label = "";
    private String sector_search = "";      
    
    private String commencement_dt = "";      
    private String own_type = "";      
    private String epf_no = "";      
    private String socso_no = "";      
    private String attain_complete_dt = "";      
    
    private String total_empl_my = "";  
    private String total_non_empl_my = "";  
    private String total_empl = "";  
    private String total_wage = "";  
    
    private String total_empl_my_prev = "";  
    private String total_non_empl_my_prev = "";  
    private String total_empl_prev = "";  
    private String total_wage_prev = "";  
     
    private String sector_desc = "";
    private ArrayList errorList;
    private boolean hasError;
    
    private OtherContactsUser ocu;
    private WordCleanser wc;
    private HashMap empDataHash;
    DBHandler db;
    private String EMP_DATA_TABLE = Constants.TABLE.EMPREG_TEMP;
    
    public TempPotEmp(DBHandler db_){
        db=db_;
        empDataHash= new HashMap();
        ocu = new OtherContactsUser();
        wc = new WordCleanser();
        
        hasError = false;
        errorList = new ArrayList();
    }

    public TempPotEmp(DBHandler db_, String tempId){
        db=db_;
        empDataHash= new HashMap();
//        ocu = new OtherContactsUser();
//        wc = new WordCleanser();
//
//        hasError = false;
//        errorList = new ArrayList();
        empDataHash = db.selectOneRecord(
                "SELECT * FROM "+EMP_DATA_TABLE+" WHERE id = ? ",
                new String[]{tempId}
        );
    }

    public HashMap checkIfAllDataSame(TempPotEmp temp, String row){
        HashMap compData = new HashMap();

        boolean allSame = true;
//        HashMap sameValues = new HashMap();
        String sameValues = "";

        HashMap checkTemp = temp.getAllData();
        Iterator hmIterator = empDataHash.entrySet().iterator();
        while (hmIterator.hasNext()) {
            Map.Entry hm
                    = (Map.Entry)hmIterator.next();
            String currName = hm.getKey().toString();
            String currValue = hm.getValue().toString();

            if(checkTemp.containsKey(currName)){
                String existingValue = checkTemp.getOrDefault(currName, "").toString();

                if(!existingValue.equals(currValue)){
                    allSame = false;
                    sameValues+= "<li> Value of row "+row+": <i>"+existingValue+"</i><br /> Current Value: <b>"+currValue+"</b> </li>";
                }
            }
        }

        if(!sameValues.isEmpty()){
            sameValues="<ul>"+sameValues+"</ul>";
        }

        compData.put("IS_ALL_VALUE_SAME",Boolean.valueOf(allSame));
        compData.put("DIFF_VALUES", sameValues);

        return compData;
    }

    public HashMap getAllData(){
        return empDataHash;
    }
    
    public boolean insertData(String colName, String value){
        empDataHash.put(colName, value.toUpperCase());        
        return true;
    }
    
    public String saveData(){
        String tempId = "";
        String prefix = "c_";
        
        if(empDataHash==null){            
        }
        
        HashMap newHm = new HashMap();        
        Iterator hmIterator = empDataHash.entrySet().iterator();        
        while (hmIterator.hasNext()) { 
            Map.Entry hm
                = (Map.Entry)hmIterator.next();        
            String fieldName = hm.getKey().toString();
            if(fieldName.startsWith("c_")){
                fieldName = fieldName.substring(prefix.length());
            }
            newHm.put(fieldName, hm.getValue());
        }  
        
        newHm.put("data_status", Constants.STATUS.EMP.UPLOADED);
        newHm.put("emp_status", Constants.STATUS.EMP.INACTIVE);
        newHm.put("last_move", Constants.LAST_MOVEMENT.NEW);
        
        String compName = newHm.containsKey("comp_name")?newHm.get("comp_name").toString():"";
        String mycoid = newHm.containsKey("mycoid")?newHm.get("mycoid").toString():"";
//        LogUtil.info("PE INSERT", "COMPANY "+compName+", "+mycoid);
        
        tempId = CommonUtils.saveUpdateForm2("",
                Constants.FORM_ID.EMP_TEMP_DATA,"", newHm);
        ocu.saveContact(tempId);
        cleanseInput(tempId, compName, mycoid);
        
        return tempId;
    }
    
    public String saveData(String emplId){
        String tempId = "";
        String prefix = "c_";
        
        if(empDataHash==null){            
        }
        
        HashMap newHm = new HashMap();        
        Iterator hmIterator = empDataHash.entrySet().iterator();        
        while (hmIterator.hasNext()) { 
            Map.Entry hm
                = (Map.Entry)hmIterator.next();        
            String fieldName = hm.getKey().toString();
            if(fieldName.startsWith("c_")){
                fieldName = fieldName.substring(prefix.length());
            }
            newHm.put(fieldName, hm.getValue());
        }  
        
        String compName = newHm.containsKey("comp_name")?newHm.get("comp_name").toString():"";
        String mycoid = newHm.containsKey("mycoid")?newHm.get("mycoid").toString():"";
//        LogUtil.info("PE INSERT", "COMPANY "+compName+", "+mycoid+" EXISTING ID - "+emplId  );
        
        tempId = CommonUtils.saveUpdateForm2("",
                Constants.FORM_ID.EMP_TEMP_DATA,emplId, newHm);
        
        ocu.saveContact(tempId);
        cleanseInput(tempId, compName, mycoid);
        
        return tempId;
    }
    
    private void cleanseInput(String tempId, String compName, String mycoid){

        final String empId_ = tempId;

        if(!compName.isEmpty() && !mycoid.isEmpty()){
            Thread checkingThread = new PluginThread(new Runnable(){
                @Override
                public void run() {
                    wc.setWord(mycoid);
                    String mycoid_t = wc.cleanMycoid(mycoid);

                    wc.setWord(compName);
                    String compName_t = wc.cleanMycoid(compName);
                    
                    HashMap hm = new HashMap();
                    hm.put("comp_name", compName_t);
                    hm.put("mycoid", mycoid_t);
                    
                    CommonUtils.saveUpdateForm2("",Constants.FORM_ID.EMP_TEMP_DATA,empId_, hm);
                }       
            });
        
            checkingThread.setDaemon(false);
            checkingThread.start();
                
        }
    }
    
    /**
     * @return the org_type
     */
    public String getOrg_type() {
        return org_type;
    }

    /**
     * @param org_type the org_type to set
     */
    public void setOrg_type(String org_type) {
        this.org_type = org_type;           
        empDataHash.put("c_empl_org_type", org_type);
    }

    /**
     * @return the reg_type
     */
    public String getReg_type() {
        return reg_type;
    }

    /**
     * @param reg_type the reg_type to set
     */
    public void setReg_type(String reg_type) {
        this.reg_type = reg_type;
        empDataHash.put("c_empl_reg_type", reg_type);
    }

    /**
     * @return the mycoid
     */
    public String getMycoid() {
        return mycoid;
    }

    /**
     * @param mycoid the mycoid to set
     */
    public void setMycoid(String mycoid) {
//        try{
//            this.mycoid =   new WordCleanser().cleanMycoid(mycoid);
//        }catch(Exception e){
            this.mycoid = mycoid;
//        }
        empDataHash.put("c_mycoid", getMycoid());
    }

    /**
     * @return the comp_name
     */
    public String getComp_name() {
        return comp_name;
    }

    /**
     * @param comp_name the comp_name to set
     */
    public void setComp_name(String comp_name) {
        
//        try{
//            this.comp_name =  new WordCleanser().cleanCompName(comp_name);
//        }catch(Exception e){
            this.comp_name = comp_name;
//        }
        empDataHash.put("c_comp_name", getComp_name());
    }

    /**
     * @return the comp_email
     */
    public String getComp_email() {
        return comp_email;
    }

    /**
     * @param comp_email the comp_email to set
     */
    public void setComp_email(String comp_email) {
        this.comp_email = comp_email;
        empDataHash.put("c_empl_email_pri", comp_email);
        empDataHash.put("c_req_email", comp_email);
    }

    /**
     * @return the address_full
     */
    public String getAddress_full() {
        return getAddress1()+", "+getAddress2()+", "+getAddress3();
    }

    /**
     * @return the address1
     */
    public String getAddress1() {
        return address1;
    }

    /**
     * @param address1 the address1 to set
     */
    public void setAddress1(String address1) {
        this.address1 = address1;        
        empDataHash.put("c_empl_address", address1);
    }

    /**
     * @return the address2
     */
    public String getAddress2() {
        return address2;
    }

    /**
     * @param address2 the address2 to set
     */
    public void setAddress2(String address2) {
        this.address2 = address2;
        empDataHash.put("c_empl_address2", address2);
    }

    /**
     * @return the address3
     */
    public String getAddress3() {
        return address3;
    }

    /**
     * @param address3 the address3 to set
     */
    public void setAddress3(String address3) {
        this.address3 = address3;
        empDataHash.put("c_empl_address3", address3);
    }
    
    public void setAllLocation(String postcode_){
        country = MasterSetupData.getCountryByPostcode(db, postcode_);
        state = MasterSetupData.getStateByPostcode(db, postcode_);
        city = MasterSetupData.getCityByPostcode(db, postcode_);
        postcode = postcode_;
        
        empDataHash.put("c_empl_country", country);
        empDataHash.put("c_empl_state", state);
        empDataHash.put("c_empl_city", city);
        empDataHash.put("c_empl_postcode", postcode);
    }
    
    public void setAllBULocation(String postcode_){
        country = MasterSetupData.getCountryByPostcode(db, postcode_);
        state = MasterSetupData.getStateByPostcode(db, postcode_);
        city = MasterSetupData.getCityByPostcode(db, postcode_);
        postcode = postcode_;
        
        empDataHash.put("c_bu_country", country);
        empDataHash.put("c_bu_state", state);
        empDataHash.put("c_bu_city", city);
        empDataHash.put("c_bu_postcode", postcode);
    }

    /**
     * @return the postcode
     */
    public String getPostcode() {
        return postcode;
    }

    /**
     * @param postcode the postcode to set
     */
    public void setPostcode(String postcode) {
        this.postcode = postcode;
        empDataHash.put("c_empl_postcode", postcode);
        setAllLocation(postcode);
    }

    /**
     * @return the city
     */
    public String getCity() {
        return city;
    }

    /**
     * @param city the city to set
     */
    public void setCity(String city_) {   
        if(getPostcode().isEmpty()){
            city = MasterSetupData.getCityId(db, city_);
            empDataHash.put("c_empl_city", city);
        }else{
            if(city.isEmpty()){
                setAllLocation(getPostcode());
            }            
        }
    }

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(String state_) {  
        if(getPostcode().isEmpty()){
            state = MasterSetupData.getStateId(db, state_);   
            empDataHash.put("c_empl_state", state);
        }else{
            if(state.isEmpty()){
                setAllLocation(getPostcode());
            }    
        }
    }

    /**
     * @return the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * @param country the country to set
     */
    public void setCountry(String country_) {
        if(getPostcode().isEmpty()){
            country = MasterSetupData.getCountryId(db, country_);
            empDataHash.put("c_empl_country", country);
        }else{
            if(country.isEmpty()){
                setAllLocation(getPostcode());
            }    
        }
    }

    /**
     * @return the bu_address1
     */
    public String getBu_address1() {
        return bu_address1;
    }

    /**
     * @param bu_address1 the bu_address1 to set
     */
    public void setBu_address1(String bu_address1) {
        this.bu_address1 = bu_address1;
        empDataHash.put("c_bu_address1", bu_address1);
    }

    /**
     * @return the bu_address2
     */
    public String getBu_address2() {
        return bu_address2;
    }

    /**
     * @param bu_address2 the bu_address2 to set
     */
    public void setBu_address2(String bu_address2) {
        this.bu_address2 = bu_address2;
        empDataHash.put("c_bu_address2", bu_address2);
    }

    /**
     * @return the bu_address3
     */
    public String getBu_address3() {
        return bu_address3;
    }

    /**
     * @param bu_address3 the bu_address3 to set
     */
    public void setBu_address3(String bu_address3) {
        this.bu_address3 = bu_address3;
        empDataHash.put("c_bu_address3", bu_address3);
    }

    /**
     * @return the bu_postcode
     */
    public String getBu_postcode() {
        return bu_postcode;
    }

    /**
     * @param bu_postcode the bu_postcode to set
     */
    public void setBu_postcode(String bu_postcode_) {
        this.bu_postcode = bu_postcode_;
        empDataHash.put("c_bu_postcode", bu_postcode_);
        setAllBULocation(bu_postcode);
    }

    /**
     * @return the bu_city
     */
    public String getBu_city() {
        return bu_city;
    }

    /**
     * @param bu_city the bu_city to set
     */
    public void setBu_city(String bu_city_) {        
        if(getBu_postcode().isEmpty()){
            this.bu_city = MasterSetupData.getCityId(db, bu_city_);
            empDataHash.put("c_bu_city", bu_city);
        }else{
            setAllLocation(getBu_postcode());
        }
    }

    /**
     * @return the bu_state
     */
    public String getBu_state() {
        return bu_state;
    }

    /**
     * @param bu_state the bu_state to set
     */
    public void setBu_state(String bu_state_) {
        if(getBu_postcode().isEmpty()){
            this.bu_state = MasterSetupData.getCityId(db, bu_state_);
            empDataHash.put("c_bu_state", bu_state);
        }else{
            setAllLocation(getBu_postcode());
        }
    }

    /**
     * @return the bu_country
     */
    public String getBu_country() {
        return bu_country;
    }

    /**
     * @param bu_country the bu_country to set
     */
    public void setBu_country(String bu_country_) {
        if(getBu_postcode().isEmpty()){
            this.bu_country = MasterSetupData.getCityId(db, bu_country_);
            empDataHash.put("c_bu_country", bu_country);
        }else{
            setAllLocation(getBu_postcode());
        }
    }

    /**
     * @return the primary_contact_email
     */
    public String getPrimary_contact_email() {
        return primary_contact_email;
    }

    /**
     * @param primary_contact_email the primary_contact_email to set
     */
    public void setPrimary_contact_email(String primary_contact_email) {
        this.primary_contact_email = primary_contact_email;
        empDataHash.put("c_empl_email_pic_pri", primary_contact_email);
        
        ocu.setEmail(primary_contact_email);
        ocu.setName(primary_contact_email);
    }
    
    public String getPrimary_contact_telno() {
        return primary_contact_telno;
    }

    /**
     * @param primary_contact_email the primary_contact_email to set
     */
    public void setPrimary_contact_telno(String primary_contact_telno) {
        this.primary_contact_telno = primary_contact_telno;
        empDataHash.put("c_empl_tel_no_pri", primary_contact_telno);
        
        ocu.setEmail(primary_contact_telno);
    }

//    /**
//     * @return the business_entity
//     */
//    public String getBusiness_entity() {
//        return business_entity;
//    }
//
//    /**
//     * @param business_entity the business_entity to set
//     */
//    public void setBusiness_entity(String business_entity) {
//        this.business_entity = business_entity;
//    }
//
//    /**
//     * @return the sub_business_entity
//     */
//    public String getSub_business_entity() {
//        return sub_business_entity;
//    }
//
//    /**
//     * @param sub_business_entity the sub_business_entity to set
//     */
//    public void setSub_business_entity(String sub_business_entity) {
//        this.sub_business_entity = sub_business_entity;
//    }
//
//    /**
//     * @return the business_nature
//     */
//    public String getBusiness_nature() {
//        return business_nature;
//    }
//
//    /**
//     * @param business_nature the business_nature to set
//     */
//    public void setBusiness_nature(String business_nature) {
//        this.business_nature = business_nature;
//    }
    
    public void setSectorData(String type, String code){
        code = code.replaceAll("[^a-zA-Z0-9]", "");  
        int code_int = 0;
        
        if(code.length()>1 && !type.equals(Constants.SECTOR_TYPE.INDUSTRY_SECTOR)){            
            try{
                code_int = Integer.parseInt(code);
            }catch(Exception e){
//                LogUtil.info("ERROR READING MSIC CODE", "NOT A CODE - "+e.getMessage());
                return;
            }            
        }
        
        if(code_int<1){
            return;
        }
                
        switch(type){
            case Constants.SECTOR_TYPE.INDUSTRY_SECTOR:
                setIndustry_sector(code);
            break;
            case Constants.SECTOR_TYPE.DIV:

                if(code.length()<2){
                    code = String.format("%02d", code_int);
                }                                
                if(getDiv_sector().isEmpty()){
                    setDiv_sector(code);         
                }                
                if(getIndustry_sector().isEmpty()){
                    code = MasterSetupData.getIndustrySectorCodeFromDiv(db, code);  
                    setSectorData(Constants.SECTOR_TYPE.INDUSTRY_SECTOR, code);
                }                
            break;
            case Constants.SECTOR_TYPE.MAIN_SECTOR_CODE:
                if(code.length()<3){
                    code = String.format("%03d", code_int);
                }          
                if(getMain_sector().isEmpty()){
                    setMain_sector(code);
                }
                setSectorData(Constants.SECTOR_TYPE.DIV, code.substring(0, 3));
            break;
            case Constants.SECTOR_TYPE.CLASS_SECTOR:
                if(code.length()<4){
                    code = String.format("%04d", code_int);
                }                
                if(getClass_sector().isEmpty()){
                    setClass_sector(code);
                }                
                setSectorData(Constants.SECTOR_TYPE.MAIN_SECTOR_CODE, code.substring(0, 4));
            break;
            case Constants.SECTOR_TYPE.SUB_SECTOR_CODE:
                if(code.length()<5){
                    code = String.format("%05d", code_int);
                }                
                if(getSub_sector().isEmpty()){
                    setSub_sector(code);
                }
                setSectorData(Constants.SECTOR_TYPE.CLASS_SECTOR, code.substring(0, 5));
            break;
        }
    }

    /**
     * @return the industry_sector
     */
    public String getIndustry_sector() {
        return industry_sector;
    }

    /**
     * @param industry_sector the industry_sector to set
     */
    public void setIndustry_sector(String code) {        
        HashMap hm = MasterSetupData.getIndustrySectorData(db, code);
        
        String id = "";
        String label = "";
        
        if(hm!=null){
            id = hm.get("id").toString();
            label = hm.get("label").toString();
        }else{
            errorList.add("Industry Sector - Section Not Found for Code "+code);
        }
        
        this.industry_sector = id;
        this.industry_sector_label = label;
        
        empDataHash.put("c_industry_sector", id);
        empDataHash.put("c_industry_sector_label", label);
    }

    /**
     * @return the industry_sector_label
     */
    public String getIndustry_sector_label() {
        return industry_sector_label;
    }

    /**
     * @return the main_sector
     */
    public String getDiv_sector() {
        return div;
    }

    /**
     * @param main_sector the main_sector to set
     */
    public void setDiv_sector(String code) {
        HashMap hm = MasterSetupData.getDivSectorData(db, code);
        
        String id = "";
        String label = "";
        
        if(hm!=null){
            id = hm.get("id").toString();
            label = hm.get("label").toString();
        }else{
            errorList.add("Industry Sector - Division Not Found for Code "+code);
        }
        
        this.div = id;
        this.div_label = label;
        
        empDataHash.put("c_div", id);
        empDataHash.put("c_div_label", label);
    }
    
    /**
     * @return the main_sector
     */
    public String getDiv_label() {
        return div_label;
    }
    
    /**
     * @return the main_sector
     */
    public String getMain_sector() {
        return main_sector_code;
    }
    
    /**
     * @return the main_sector_label
     */
    public String getMain_sector_label() {
        return main_sector_label;
    }

    /**
     * @param main_sector the main_sector to set
     */
    public void setMain_sector(String code) {
        HashMap hm = MasterSetupData.getMainSectorData(db, code);
        
        String id = "";
        String label = "";
        
        if(hm!=null){
            id = hm.get("id").toString();
            label = hm.get("label").toString();
        }else{
            errorList.add("Industry Sector - Main Sector Not Found for Code "+code);
        }
        
        this.main_sector_code = id;
        this.main_sector_label = label;
        
        empDataHash.put("c_main_sector_code", id);
        empDataHash.put("c_main_sector_label", label);
    }

    /**
     * @return the sub_sector
     */
    public String getClass_sector() {
        return class_code;
    }
    
    /**
     * @return the sub_sector_label
     */
    public String getClass_sector_label() {
        return class_label;
    }

    /**
     * @param sub_sector the sub_sector to set
     */
    public void setClass_sector(String code) {
        HashMap hm = MasterSetupData.getClassSectorData(db, code);
        
        String id = "";
        String label = "";
        
        if(hm!=null){
            id = hm.get("id").toString();
            label = hm.get("label").toString();
        }else{
            errorList.add("Industry Sector - Class Not Found for Code "+code);
        }
        
        this.class_code = id;
        this.class_label = label;
        
        empDataHash.put("c_class_code", id);
        empDataHash.put("c_class_label", label);
    }

    /**
     * @return the sub_sector
     */
    public String getSub_sector() {
        return sector_code;
    }
    
    /**
     * @return the sub_sector_label
     */
    public String getSub_sector_label() {
        return sector_desc;
    }

    /**
     * @param sub_sector the sub_sector to set
     */
    public void setSub_sector(String code) {
        HashMap hm = MasterSetupData.getSubSectorData(db, code);
        
        String id = "";
        String label = "";
        
        if(hm!=null){
            id = hm.get("id").toString();
            label = hm.get("label").toString();
        }else{
            errorList.add("Industry Sector - Sector Code Not Found for Code "+code);
        }
        
        this.sector_code = id;
        this.sector_code = label;
        
        empDataHash.put("c_sector_search_id", id);
        empDataHash.put("c_sector_code", id);
        empDataHash.put("c_sector_descr", label);
    }

//    /**
//     * @return the commencement_dt
//     */
//    public String getCommencement_dt() {
//        return commencement_dt;
//    }
//
//    /**
//     * @param commencement_dt the commencement_dt to set
//     */
//    public void setCommencement_dt(String commencement_dt) {
//        this.commencement_dt = commencement_dt;
//    }
//
//    /**
//     * @return the own_type
//     */
//    public String getOwn_type() {
//        return own_type;
//    }
//
//    /**
//     * @param own_type the own_type to set
//     */
//    public void setOwn_type(String own_type) {
//        this.own_type = own_type;
//    }
//
//    /**
//     * @return the epf_no
//     */
//    public String getEpf_no() {
//        return epf_no;
//    }
//
//    /**
//     * @param epf_no the epf_no to set
//     */
//    public void setEpf_no(String epf_no) {
//        this.epf_no = epf_no;
//    }
//
//    /**
//     * @return the socso_no
//     */
//    public String getSocso_no() {
//        return socso_no;
//    }
//
//    /**
//     * @param socso_no the socso_no to set
//     */
//    public void setSocso_no(String socso_no) {
//        this.socso_no = socso_no;
//    }
//
//    /**
//     * @return the attain_complete_dt
//     */
//    public String getAttain_complete_dt() {
//        return attain_complete_dt;
//    }
//
//    /**
//     * @param attain_complete_dt the attain_complete_dt to set
//     */
//    public void setAttain_complete_dt(String attain_complete_dt) {
//        this.attain_complete_dt = attain_complete_dt;
//    }
    
        /**
     * @return the total_employees
     */
    public String getTotal_empl() {
        return total_empl;
    }

    /**
     * @param total_employees the total_employees to set
     */
    public void setTotal_empl(String total_employees) {
        this.total_empl = total_employees;
        empDataHash.put("c_total_empl", total_employees);
        
        String totalNonMyEmpl= "0";
        try{
            int total_empl_i = Integer.parseInt(total_employees);
            if(!getTotal_empl_my().isEmpty()){
                int total_emp_my_i = Integer.parseInt(getTotal_empl_my());
                
                int totalNonMyEmpl_i = total_empl_i-total_emp_my_i;
                totalNonMyEmpl = Integer.toString(totalNonMyEmpl_i);
            }else{
                setTotal_empl_my(total_employees);
            }
        }catch(Exception e){
            
        }        
        setTotal_non_empl_my(totalNonMyEmpl);
    }

    /**
     * @return the total_empl_my
     */
    public String getTotal_empl_my() {
        return total_empl_my;
    }

    /**
     * @param total_empl_my the total_empl_my to set
     */
    public void setTotal_empl_my(String total_empl_my) {
        this.total_empl_my = total_empl_my;
        empDataHash.put("c_total_my_empl", total_empl_my);
    }

    /**
     * @return the total_non_empl_my
     */
    public String getTotal_non_empl_my() {
        return total_non_empl_my;
    }

    /**
     * @param total_non_empl_my the total_non_empl_my to set
     */
    public void setTotal_non_empl_my(String total_non_empl_my) {
        this.total_non_empl_my = total_non_empl_my;
        empDataHash.put("c_total_non_my_empl", total_non_empl_my);
    }
//
//    /**
//     * @return the total_wage
//     */
//    public String getTotal_wage() {
//        return total_wage;
//    }
//
//    /**
//     * @param total_wage the total_wage to set
//     */
//    public void setTotal_wage(String total_wage) {
//        this.total_wage = total_wage;
//    }
//
//    /**
//     * @return the total_empl_my_prev
//     */
//    public String getTotal_empl_my_prev() {
//        return total_empl_my_prev;
//    }
//
//    /**
//     * @param total_empl_my_prev the total_empl_my_prev to set
//     */
//    public void setTotal_empl_my_prev(String total_empl_my_prev) {
//        this.total_empl_my_prev = total_empl_my_prev;
//    }
//
//    /**
//     * @return the total_non_empl_my_prev
//     */
//    public String getTotal_non_empl_my_prev() {
//        return total_non_empl_my_prev;
//    }
//
//    /**
//     * @param total_non_empl_my_prev the total_non_empl_my_prev to set
//     */
//    public void setTotal_non_empl_my_prev(String total_non_empl_my_prev) {
//        this.total_non_empl_my_prev = total_non_empl_my_prev;
//    }
//
//    /**
//     * @return the total_empl_prev
//     */
//    public String getTotal_empl_prev() {
//        return total_empl_prev;
//    }
//
//    /**
//     * @param total_empl_prev the total_empl_prev to set
//     */
//    public void setTotal_empl_prev(String total_empl_prev) {
//        this.total_empl_prev = total_empl_prev;
//    }
//
//    /**
//     * @return the total_wage_prev
//     */
//    public String getTotal_wage_prev() {
//        return total_wage_prev;
//    }
//
//    /**
//     * @param total_wage_prev the total_wage_prev to set
//     */
//    public void setTotal_wage_prev(String total_wage_prev) {
//        this.total_wage_prev = total_wage_prev;
//    }
    
}
