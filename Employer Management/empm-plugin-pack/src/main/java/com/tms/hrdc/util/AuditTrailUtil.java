/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.hrdc.dao.EmpmObj;
import com.tms.hrdc.defaultPluginTool.FormManagerTool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.joget.commons.util.LogUtil;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class AuditTrailUtil {
    
    final String UUID = "uuid";
    final String COL = "column";
    final String VAL = "value";
    final String PREV_VALUE = "previous_value";
    final String CURR_VALUE = "current_value";
    final String FIELD_NAME = "fld_name";
    final String DATEMODIFIED = "date_modified";
    final String MODIFIEDBY = "modified_by_id";
    final String MODIFIEDBYNAME = "modified_by_name";
    final String VALUE_CHGE_LIST = "audit_trail_list";
    final String AUDIT_FK = "audit_fk";
    final String STATUS = "status";
    final String REMARK = "remark";
    
    private static void msg(String msg){
        LogUtil.info("com.tms.hrdc.util.AuditTrailUtil", msg);
    }
    
    public String insertAuditTrail(DBHandler db, String parent_id, HashMap dataBefore, HashMap dataNew, String modifiedBy, String status, String remarks){
        
        ArrayList<HashMap<String, String>>  value_change_list = new ArrayList();
        
        if(dataBefore != null || dataNew != null){
            value_change_list = getValueChangeList(dataNew, dataBefore);
        }        
        
        String aud_id = insertAuditTrail2(db, parent_id, modifiedBy, status, remarks, false, value_change_list);
        
        if(value_change_list==null){
            return aud_id;
        }
        
        HashMap hm;
        
        for(HashMap each_hm:value_change_list){
            
            hm = new HashMap();
            hm.put("field_name", each_hm.get(FIELD_NAME)==null?"":each_hm.get(FIELD_NAME));
            hm.put("prev_value", each_hm.get(PREV_VALUE)==null?"":each_hm.get(PREV_VALUE));
            hm.put("curr_value", each_hm.get(CURR_VALUE)==null?"":each_hm.get(CURR_VALUE));
            hm.put("fk", aud_id);

            CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                        Constants.FORM_ID.AUDIT_TRAIL_SUB, 
                                        "", hm);
        }
        
        return aud_id;
        
    }
    
    public String insertAuditTrail2(DBHandler db, String parent_id, String modifiedBy, 
            String status, String remarks, boolean isInternalView, 
            ArrayList<HashMap<String, String>> value_change_list){
                 
        HashMap hm = new HashMap();
        hm.put("createdBy", modifiedBy);
        hm.put("createdByName", modifiedBy);
        hm.put("modifiedBy", modifiedBy);
        hm.put("modifiedByName", modifiedBy);
        hm.put("status", status);
        hm.put("remarks", remarks);
        hm.put("fk", parent_id);
        hm.put("internal_view_only", Boolean.toString(isInternalView));


        String auditId = CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                                    Constants.FORM_ID.AUDIT_TRAIL, 
                                                    "", hm);        
        
        if(value_change_list!=null){
            for(HashMap each_hm:value_change_list){

                hm = new HashMap();
                hm.put("field_name", each_hm.get(Constants.CHANGE_KEYS.FIELD)==null?"":each_hm.get(Constants.CHANGE_KEYS.FIELD));
                hm.put("prev_value", each_hm.get(Constants.CHANGE_KEYS.OLD)==null?"":each_hm.get(Constants.CHANGE_KEYS.OLD));
                hm.put("curr_value", each_hm.get(Constants.CHANGE_KEYS.NEW)==null?"":each_hm.get(Constants.CHANGE_KEYS.NEW));
                hm.put("fk", auditId);

                CommonUtils.saveUpdateForm2(Constants.APP_ID.EMPM, 
                                            Constants.FORM_ID.AUDIT_TRAIL_SUB, 
                                            "", hm);
            }
        }
        
        return auditId;
    }
    
    public String insertNotification(String msg, String empId, String empHrdcNo, boolean isForEmployer, String action_by, String recp_detail){
        HashMap hm = new HashMap();
        String recp = "Officer";
        if(isForEmployer){
            hm.put("empl_id", empId);
            hm.put("hrdc_no", empHrdcNo);
            recp = "employer";
        }                
        hm.put("message", msg);
        hm.put("action_by", action_by);
        hm.put("recp", recp);
        hm.put("recp_detail", recp_detail);
        
        return CommonUtils.saveUpdateForm2("", "primaryKeyValue", "", hm);
    }
    
    private ArrayList<HashMap<String, String>> getValueChangeList(HashMap newValueHm, HashMap result_hm) {
        
        ArrayList<HashMap<String, String>> audit_list = new ArrayList();
//        KeywordDictionary kd = new KeywordDictionary(db, Constants.TYPE_EMP_REG);
        
        Set set = newValueHm.entrySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            Map.Entry mentry = (Map.Entry) iterator.next();
             
            String prev_value = result_hm.get(mentry.getKey()) == null?"":result_hm.get(mentry.getKey()).toString();
            String new_value = mentry.getValue() == null?"":mentry.getValue().toString();
            
            if(new_value.equals(prev_value)){
                continue;
            }
            
            HashMap aud_hm = new HashMap();
            aud_hm.put(PREV_VALUE, prev_value);
            aud_hm.put(CURR_VALUE, new_value);
            aud_hm.put(FIELD_NAME, mentry.getKey());
            
            audit_list.add(aud_hm);
        }
//        LogUtil.info("newValueHm test", audit_list.toString());      
        return audit_list;
    }
    
    public static void saveTempData(DBHandler db, EmpmObj eo) {
        
        KeywordDictionary kwd = new KeywordDictionary(db);
        
        String prefix = "c_";
        String tempId = eo.getId();
        
        HashMap newHm = eo.getEmpData();
  
        HashMap newTempHm = new HashMap();
        newHm = kwd.removeBasicKeys(newHm, db);
        Iterator hmIterator = newHm.entrySet().iterator();        
        
        while (hmIterator.hasNext()) { 
            Map.Entry hm
                = (Map.Entry)hmIterator.next();        
            String fieldName = hm.getKey().toString();
            
            if(fieldName.equals("dateCreated") || fieldName.equals("dateModified")){
                continue;
            }
            
            if(hm.getKey().toString().startsWith("c_")){
                fieldName = fieldName.substring(prefix.length());
            }
            newTempHm.put(fieldName, hm.getValue());
        }          
        
        tempId = CommonUtils.saveUpdateForm2("",
                Constants.FORM_ID.EMP_TEMP_MAIN_FORM,tempId, newTempHm);
    }
    
    public static void saveEmpCacheData(DBHandler db, EmpmObj eo) {
        
        //empReg
        //other contact person - empm_cntct_oth
        //attachment docs - empm_docs
        String empcachedatatype = "";
        String cacheId = "";
        
        ArrayList list = new ArrayList();
        list.add(eo.getEmpData());
        
        if(!list.isEmpty()){
            empcachedatatype = Constants.CACHE_TYPE.MAIN_EMP_DATA;
            cacheId = getCacheId(db, eo.getId(), empcachedatatype);
            saveCacheData(db, cacheId, list, eo.getId(), Constants.TABLE.EMPREG, empcachedatatype);
        }
        
        list = db.select(
                "SELECT * FROM app_fd_empm_cntct_oth WHERE c_fk = ? ",
                new String[]{eo.getId()}
        );
        
        if(!list.isEmpty()){
            empcachedatatype = Constants.CACHE_TYPE.OTHER_CONTACT_PERSON;
            cacheId = getCacheId(db, eo.getId(), empcachedatatype);
            saveCacheData(db, cacheId, list, eo.getId(), "app_fd_empm_cntct_oth", empcachedatatype);
        }
        
        list = db.select(
                "SELECT * FROM app_fd_empm_docs WHERE c_fk = ? ",
                new String[]{eo.getId()}
        );
        
        if(!list.isEmpty()){
            empcachedatatype = Constants.CACHE_TYPE.DOC_SSM;
            cacheId = getCacheId(db, eo.getId(), empcachedatatype);
            saveCacheData(db, cacheId, list, eo.getId(), "app_fd_empm_docs", empcachedatatype);
        }
        
        list = db.select(
                "SELECT * FROM app_fd_empm_docs WHERE c_fk_doc_a = ? ",
                new String[]{eo.getId()}
        );
        
        if(!list.isEmpty()){
            empcachedatatype = Constants.CACHE_TYPE.DOC_EPF_PAYROLL;
            cacheId = getCacheId(db, eo.getId(), empcachedatatype);
            saveCacheData(db, cacheId, list, eo.getId(), "app_fd_empm_docs", empcachedatatype);
        }
        
        list = db.select(
                "SELECT * FROM app_fd_empm_docs WHERE c_fk_doc_b = ? ",
                new String[]{eo.getId()}
        );
        
        if(!list.isEmpty()){
            empcachedatatype = Constants.CACHE_TYPE.DOC_FINANCT_STMT;
            cacheId = getCacheId(db, eo.getId(), empcachedatatype);
            saveCacheData(db, cacheId, list, eo.getId(), "app_fd_empm_docs", empcachedatatype);
        }
    }
    
    private static String getCacheId(DBHandler db, String empId, String cacheType){
        HashMap hm = db.selectOneRecord(
                "SELECT * FROM "+Constants.TABLE.CACHEDATA+" WHERE c_empcachedatatype = ? AND c_recId = ? ORDER BY dateCreated desc LIMIT 1",
                new String[]{cacheType, empId}
        );
        
        String id = "";
        
        if(hm!= null){
            id = hm.getOrDefault("id", "").toString();
        }
        
        return id;
    }
    
    public static void saveCacheData(DBHandler db, String cacheId, ArrayList<HashMap<String, String>> dataList, 
            String empId, String tblName, String empcachedatatype) {
        
        String cacheListStr = dataList.stream()
                .map(map -> map.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining("@@@")))
                .collect(Collectors.joining("~~~"));
        
        JSONArray cacheArray = new JSONArray();
        
        for(HashMap hm:dataList){
            JSONObject obj = new JSONObject(hm);            
            cacheArray.put(obj);
        }
        
        HashMap data = new HashMap();
        data.put("tblName", tblName);
        data.put("recId", empId);
        data.put("cacheData", cacheArray.toString());
//        data.put("cacheData", cacheListStr);
        data.put("empcachedatatype", empcachedatatype);
        
        CommonUtils.saveUpdateForm2("",
                Constants.FORM_ID.EMP_CACHE,cacheId, data);
    }
    
    public static ArrayList<HashMap<String, String>> loadCacheData(DBHandler db, String cacheListStr) {
        
        // Convert the String back to ArrayList<HashMap<String, String>>
//        ArrayList<HashMap<String, String>> cacheList = new ArrayList<>();
//        String[] maps = cacheListStr.split("~~~");
//        
//        msg("Cache data size: "+Integer.toString(maps.length));
//        
//        for (String mapStr : maps) {
//            HashMap<String, String> newMap = new HashMap<>();
//            String[] pairs = mapStr.split("@@@");
//            
//            msg("Splitting hashmap pairs size: "+Integer.toString(pairs.length));
//            for (String pair : pairs) {
////                msg("Splitting pairs : "+pair);
//                String[] keyValue = pair.split("=");
//                String value = keyValue.length==1?"":keyValue[1];
//                newMap.put(keyValue[0], value );
//            }
//            cacheList.add(newMap);
//        }
        
        ArrayList<HashMap<String, String>> cacheList = new ArrayList();
        
        try{
            
//            JSONArray cacheArray = new JSONArray(cacheListStr);
//            
//            for(int i=0;i<cacheArray.length();i++){
//                HashMap map = new ObjectMapper().readValue(cacheArray.getString(1), HashMap.class);
//                cacheList.add(map);
//            }
            
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, String>> list = objectMapper.readValue(cacheListStr, new TypeReference<List<Map<String, String>>>() {});

            cacheList = new ArrayList(list);
            
        }catch(Exception e){
            msg("ERROR CONVERTING");
        }
        
        return cacheList;
    }
    
//    public static ArrayList getRegChgeList(DBHandler db, EmpmObj eo){
//        
//        ArrayList chgeList = new ArrayList();
//
//        HashMap arcHm = db.selectOneRecord(
//                "SELECT f.* FROM "+Constants.TABLE.EMPREG_TEMP+" f "
//                + "WHERE f.id = ? ", 
//                new String[]{eo.getId()}
//        );
//        
//        if(arcHm == null){
//            LogUtil.info("Audit Trail Util", "No old data");
//            return chgeList;
//        }
//        
//        HashMap chgedData = eo.getEmpData();
//        
//        Set set = arcHm.entrySet();
//        Iterator iterator = set.iterator();
//        while (iterator.hasNext()) {
//            Map.Entry mentry = (Map.Entry) iterator.next();
//            
//            String old_val = mentry.getValue() == null?"":mentry.getValue().toString();
//            String new_val = chgedData.get(mentry.getKey()) == null?"":chgedData.get(mentry.getKey()).toString();
//            
//            
//            if(old_val.equals(new_val)){
//                continue;
//            }
//            
//            chgeList = buildChangeAuditHm(db, (String) mentry.getKey(), new_val, old_val, chgeList);  
//        }
//        
//        return chgeList;
//    }
    
    public static ArrayList getRegChgeList2(DBHandler db, EmpmObj eo){
        
        ArrayList chgeList = new ArrayList();
        HashMap cacheData = new HashMap();
        ArrayList<HashMap<String, String>> convertedCacheList = new ArrayList();
        String cacheListStr = "";
        
        ArrayList<HashMap<String, String>> currentData = new ArrayList();
        
        boolean isDeleted = false;
        boolean isAdded = false;
        
        //get main data ========================================================
        String cacheType = Constants.CACHE_TYPE.MAIN_EMP_DATA;
        
        cacheData = db.selectOneRecord(
                "SELECT c_cacheData FROM "+Constants.TABLE.CACHEDATA+" WHERE c_recId = ? and c_empcachedatatype = ? ORDER BY dateCreated desc LIMIT 1",
                new String[]{eo.getId(), cacheType}
        );
        
        if(cacheData != null){
            cacheListStr = cacheData.getOrDefault("c_cacheData", "").toString();
            convertedCacheList = loadCacheData(db, cacheListStr);
        }
                
        if(convertedCacheList.size()>0){
            HashMap cacheEmpData = convertedCacheList.get(0);
            
            if(cacheEmpData == null){
                LogUtil.info("Audit Trail Util", "No old data");
//                return chgeList; TODO
            }
            
            HashMap chgedData = eo.getEmpData();
        
            chgeList = findValueDifference(db, chgedData, cacheEmpData , 
                    chgeList, cacheType);  
        }else{
            msg("conv not lsit");
        }
        
        msg("Changed data EMployer "+chgeList.toString());
        
        //get other contact person =============================================
        cacheType = Constants.CACHE_TYPE.OTHER_CONTACT_PERSON;
        currentData = db.select(
                "SELECT * FROM "+Constants.TABLE.EMP_OTHER_CONTACT_DETAILS+" WHERE c_fk = ? ",
                new String[]{eo.getId()}
        );
        
        cacheData = db.selectOneRecord(
                "SELECT c_cacheData FROM "+Constants.TABLE.CACHEDATA+" WHERE c_recId = ? and c_empcachedatatype = ? ORDER BY dateCreated desc LIMIT 1",
                new String[]{eo.getId(), cacheType}
        );
        
        if(cacheData != null){
            cacheListStr = cacheData.getOrDefault("c_cacheData", "").toString();
            convertedCacheList = loadCacheData(db, cacheListStr);
        }
        
        if(currentData==null){
            currentData = new ArrayList();
        }
        if(convertedCacheList==null){
            convertedCacheList = new ArrayList();
        }
        
        // checkDeleted
        if(convertedCacheList.size() > currentData.size() ){
            isDeleted = true;      
            
            for (HashMap<String, String> oldHm : convertedCacheList) {
                String id1 = oldHm.get("id");
                String oth_name = oldHm.getOrDefault("c_name","");
                String oth_email = oldHm.getOrDefault("c_email","");
                
                boolean deleted = true;

                for (HashMap<String, String> newHm : currentData) {
                    String id2 = newHm.get("id");

                    if (id1.equals(id2)) {
                        chgeList = findValueDifference(db, oldHm, newHm , chgeList, cacheType);  

                        deleted = false;
                        break;
                    }
                }

                if(deleted){
                    chgeList = putAuditDataDeleted(db, oth_name+" "+oth_email , chgeList, cacheType);  
                }
            }
            
        }else if(currentData.size() > convertedCacheList.size()){
            isAdded = true;            
            
            for (HashMap<String, String> oldHm : currentData) {
                String id1 = oldHm.get("id");
                String oth_name = oldHm.get("c_name");
                String oth_email = oldHm.get("c_email");

                boolean added = true;

                for (HashMap<String, String> newHm : convertedCacheList) {
                    String id2 = newHm.get("id");

                    if (id1.equals(id2)) {
                        chgeList = findValueDifference(db, oldHm, newHm , chgeList, cacheType);  

                        added = false;
                        break;
                    }
                }

                if(added){
                    chgeList = putAuditDataInserted(db, oth_name+" "+oth_email , chgeList, cacheType); 
                }
            }
        }
                
        //get doc1
        cacheType = Constants.CACHE_TYPE.DOC_SSM;
        currentData = db.select(
                "SELECT * FROM "+Constants.TABLE.DOCS+" WHERE c_fk = ? ",
                new String[]{eo.getId()}
        );
        
        cacheData = db.selectOneRecord(
                "SELECT c_cacheData FROM "+Constants.TABLE.CACHEDATA+" WHERE c_recId = ? and c_empcachedatatype = ? ORDER BY dateCreated desc LIMIT 1",
                new String[]{eo.getId(), cacheType}
        );
        
        if(cacheData != null){
            cacheListStr = cacheData.getOrDefault("c_cacheData", "").toString();
            convertedCacheList = loadCacheData(db, cacheListStr);
        }
        
        if(currentData==null){
            currentData = new ArrayList();
        }
        if(convertedCacheList==null){
            convertedCacheList = new ArrayList();
        }
        
        // checkDeleted
        if(convertedCacheList.size() > currentData.size() ){
            isDeleted = true;      
            
            for (HashMap<String, String> oldHm : convertedCacheList) {
                String id1 = oldHm.get("id");
                String file_name = oldHm.getOrDefault("c_file","");
                
                boolean deleted = true;

                for (HashMap<String, String> newHm : currentData) {
                    String id2 = newHm.get("id");

                    if (id1.equals(id2)) {
                        chgeList = findValueDifference(db, oldHm, newHm , chgeList, cacheType);  

                        deleted = false;
                        break;
                    }
                }

                if(deleted){
                    chgeList = putAuditDataDeleted(db, file_name , chgeList, cacheType);  
                }
            }
            
        }else if(currentData.size() > convertedCacheList.size()){
            isAdded = true;            
            
            for (HashMap<String, String> oldHm : currentData) {
                String id1 = oldHm.get("id");
                String file_name = oldHm.getOrDefault("c_file","");

                boolean added = true;

                for (HashMap<String, String> newHm : convertedCacheList) {
                    String id2 = newHm.get("id");

                    if (id1.equals(id2)) {
                        chgeList = findValueDifference(db, oldHm, newHm , chgeList, cacheType);  

                        added = false;
                        break;
                    }
                }

                if(added){
                    chgeList = putAuditDataInserted(db, file_name, chgeList, cacheType); 
                }
            }
        }
        
        if(currentData==null){
            currentData = new ArrayList();
        }
        if(currentData==null){
            convertedCacheList = new ArrayList();
        }
        
        //get doc2 =============================================================       
        cacheType = Constants.CACHE_TYPE.DOC_EPF_PAYROLL;
        currentData = db.select(
                "SELECT * FROM "+Constants.TABLE.DOCS+" WHERE c_fk_doc_a = ? ",
                new String[]{eo.getId()}
        );
        
        cacheData = db.selectOneRecord(
                "SELECT c_cacheData FROM "+Constants.TABLE.CACHEDATA+" WHERE c_recId = ? and c_empcachedatatype = ? ORDER BY dateCreated desc LIMIT 1",
                new String[]{eo.getId(), cacheType}
        );
        
        if(cacheData != null){
            cacheListStr = cacheData.getOrDefault("c_cacheData", "").toString();
            convertedCacheList = loadCacheData(db, cacheListStr);
        }
        
        if(currentData==null){
            currentData = new ArrayList();
        }
        if(convertedCacheList==null){
            convertedCacheList = new ArrayList();
        }
        
        // checkDeleted
        if(convertedCacheList.size() > currentData.size() ){
            isDeleted = true;      
            
            for (HashMap<String, String> oldHm : convertedCacheList) {
                String id1 = oldHm.get("id");
                String file_name = oldHm.getOrDefault("c_file","");
                
                boolean deleted = true;

                for (HashMap<String, String> newHm : currentData) {
                    String id2 = newHm.get("id");

                    if (id1.equals(id2)) {
                        chgeList = findValueDifference(db, oldHm, newHm , chgeList, cacheType);  

                        deleted = false;
                        break;
                    }
                }

                if(deleted){
                    chgeList = putAuditDataDeleted(db, file_name , chgeList, cacheType);  
                }
            }
            
        }else if(currentData.size() > convertedCacheList.size()){
            isAdded = true;            
            
            for (HashMap<String, String> oldHm : currentData) {
                String id1 = oldHm.get("id");
                String file_name = oldHm.getOrDefault("c_file","");

                boolean added = true;

                for (HashMap<String, String> newHm : convertedCacheList) {
                    String id2 = newHm.get("id");

                    if (id1.equals(id2)) {
                        chgeList = findValueDifference(db, oldHm, newHm , chgeList, cacheType);  

                        added = false;
                        break;
                    }
                }

                if(added){
                    chgeList = putAuditDataInserted(db, file_name, chgeList, cacheType); 
                }
            }
        }
        
        //get doc3 =============================================================       
        cacheType = Constants.CACHE_TYPE.DOC_FINANCT_STMT;
        currentData = db.select(
                "SELECT * FROM "+Constants.TABLE.DOCS+" WHERE c_fk_doc_a = ? ",
                new String[]{eo.getId()}
        );
        
        cacheData = db.selectOneRecord(
                "SELECT c_cacheData FROM "+Constants.TABLE.CACHEDATA+" WHERE c_recId = ? and c_empcachedatatype = ? ORDER BY dateCreated desc LIMIT 1",
                new String[]{eo.getId(), cacheType}
        );
        
        if(cacheData != null){
            cacheListStr = cacheData.getOrDefault("c_cacheData", "").toString();
            convertedCacheList = loadCacheData(db, cacheListStr);
        }
        
        if(currentData==null){
            currentData = new ArrayList();
        }
        if(convertedCacheList==null){
            convertedCacheList = new ArrayList();
        }
        
        // checkDeleted
        if(convertedCacheList.size() > currentData.size() ){
            isDeleted = true;      
            
            for (HashMap<String, String> oldHm : convertedCacheList) {
                String id1 = oldHm.get("id");
                String file_name = oldHm.getOrDefault("c_file","");
                
                boolean deleted = true;

                for (HashMap<String, String> newHm : currentData) {
                    String id2 = newHm.get("id");

                    if (id1.equals(id2)) {
                        chgeList = findValueDifference(db, oldHm, newHm , chgeList, cacheType);  

                        deleted = false;
                        break;
                    }
                }

                if(deleted){
                    chgeList = putAuditDataDeleted(db, file_name , chgeList, cacheType);  
                }
            }
            
        }else if(currentData.size() > convertedCacheList.size()){
            isAdded = true;            
            
            for (HashMap<String, String> oldHm : currentData) {
                String id1 = oldHm.get("id");
                String file_name = oldHm.getOrDefault("c_file","");

                boolean added = true;

                for (HashMap<String, String> newHm : convertedCacheList) {
                    String id2 = newHm.get("id");

                    if (id1.equals(id2)) {
                        chgeList = findValueDifference(db, oldHm, newHm , chgeList, cacheType);  

                        added = false;
                        break;
                    }
                }

                if(added){
                    chgeList = putAuditDataInserted(db, file_name, chgeList, cacheType); 
                }
            }
        }
        
        return chgeList;
    }
    
    public static ArrayList findValueDifference(DBHandler db, HashMap newData, 
                            HashMap oldData, ArrayList chgeList, String cacheType){
        Set set = oldData.entrySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            Map.Entry mentry = (Map.Entry) iterator.next();

            String old_val = mentry.getValue() == null?"":mentry.getValue().toString();
            String new_val = newData.getOrDefault(mentry.getKey(), "").toString();            
            
            
            if(mentry.getKey().equals("c_mycoid")){
                msg("MYCOID "+(String) mentry.getKey()+": "+old_val+", "+new_val);
            }

            if(old_val.equals(new_val)){
                continue;
            }

            chgeList = buildChangeAuditHm(db, (String) mentry.getKey(), 
                    new_val, old_val, chgeList, cacheType);  
        }
        
        return chgeList;
    }
    
    public static ArrayList putAuditDataInserted(DBHandler db, String value, ArrayList chgeList, String cacheDataType) {
        
        HashMap chge = new HashMap();

        chge.put(Constants.CHANGE_KEYS.FIELD, "Inserted New Record");
        chge.put(Constants.CHANGE_KEYS.OLD, "");
        chge.put(Constants.CHANGE_KEYS.NEW, value);
        chge.put(Constants.CHANGE_KEYS.EMP_DATA_TYPE, cacheDataType);

        chgeList.add(chge);

        return chgeList;
    }
    
    public static ArrayList putAuditDataDeleted(DBHandler db, String value, ArrayList chgeList, String cacheDataType) {
        
        HashMap chge = new HashMap();

        chge.put(Constants.CHANGE_KEYS.FIELD, "Deleted Record");
        chge.put(Constants.CHANGE_KEYS.OLD, value);
        chge.put(Constants.CHANGE_KEYS.NEW, "");
        chge.put(Constants.CHANGE_KEYS.EMP_DATA_TYPE, cacheDataType);

        chgeList.add(chge);

        return chgeList;
    }
    
    public static ArrayList buildChangeAuditHm(DBHandler db, String colName, String value, String oldVal, 
                                        ArrayList chgeList, String cacheDataType) {
        HashMap newData = new HashMap();
        HashMap fieldProp = null;

        if(!StringUtils.isBlank(oldVal) && !oldVal.equals(value)){
            fieldProp = KeywordDictionary.getFieldProperty(db, colName);
        }

        if(fieldProp!=null){
            HashMap chge = new HashMap();

            String isSect = fieldProp.get("c_isSectorField").toString();
            String isLoc = fieldProp.get("c_isLocationField").toString();

            chge.put(Constants.CHANGE_KEYS.FIELD_ID, colName);
            chge.put(Constants.CHANGE_KEYS.FIELD, fieldProp.getOrDefault("c_columnName", "NODATA").toString());
            chge.put(Constants.CHANGE_KEYS.OLD, KeywordDictionary.formatVal(db, isLoc, isSect, oldVal));
            chge.put(Constants.CHANGE_KEYS.NEW, KeywordDictionary.formatVal(db, isLoc, isSect, value));
            chge.put(Constants.CHANGE_KEYS.EMP_DATA_TYPE, cacheDataType);

            chgeList.add(chge);
        }

        return chgeList;
    }
    
    public static ArrayList getEmpDataChgeListForEgmnt(DBHandler db, String egmntId){
        
        ArrayList chgeList = new ArrayList();    
        ArrayList<HashMap<String, String>> columnsArray;
        
        String col = "";        
        ArrayList<String> valueList = new ArrayList(); 
        
        HashMap tempProcHm = db.selectOneRecord(
                "SELECT f.* FROM "+Constants.TABLE.POT_EMP_EMPREG_TEMP+" f "
                + "INNER JOIN "+Constants.TABLE.POT_EMP_ENGAGEMENT+" r ON r.c_temp_emp_fk = f.id "
                + "WHERE r.id = ? ", 
                new String[]{egmntId}
        );
        
        if(tempProcHm==null){
            return chgeList;
        }
                
        String tempId = tempProcHm.get("id").toString();        
        String tempIdBase = Constants.BASE_DATA_PREFIX+tempId;       
        
        HashMap tempBase = db.selectOneRecord(
                "SELECT f.* FROM "+Constants.TABLE.POT_EMP_EMPREG_TEMP+" f "
                + "WHERE f.id = ? ", 
                new String[]{tempIdBase}
        );
        
        if(tempBase == null){
            return chgeList;
        }
        
        Set set = tempProcHm.entrySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            Map.Entry mentry = (Map.Entry) iterator.next();
            
            String new_val = mentry.getValue() == null?"":mentry.getValue().toString();
            String old_val = tempBase.get(mentry.getKey()) == null?"":tempBase.get(mentry.getKey()).toString();
            
            
            if(old_val.equals(new_val)){
                continue;
            }
            
            if(mentry.getKey().equals("id")){
                continue;
            }
            
            col += (col.isEmpty()?mentry.getKey():", "+mentry.getKey())+"=?";
            valueList.add(new_val);
            
            chgeList = buildChangeAuditHm(db, (String) mentry.getKey(), new_val, old_val, chgeList, "");  
        }
        
        String[] valueArr = valueList.toArray(new String[valueList.size()]);
        String query = "UPDATE "+Constants.TABLE.POT_EMP_EMPREG_TEMP+" SET " + col +" WHERE id = ?";
        
        int i = db.update(query, valueArr, new String[]{tempIdBase});        
        LogUtil.info("ENGAGEMENT VALUE CHANGE "+Integer.toString(i), 
                "basedata="+tempIdBase+", procData="+tempId+", Query is "+query+". Chge is "+chgeList.toString());
        
        return chgeList;
    }
    
    public static ArrayList egmntMergeEmpDataChge(DBHandler db, String egmntId, String empId){
        
        LogUtil.info("ENGAGEMENT VALUE CHANGE ", 
                "MERGING DATA");
        
        ArrayList chgeList = new ArrayList();
//        String sql = "SELECT * FROM "+Constants.TABLE.AUDIT+" a WHERE c_fk = ? "
//                + "ORDER BY dateCreated desc LIMIT 1";

        HashMap tempProcHm = db.selectOneRecord(
                "SELECT f.* FROM "+Constants.TABLE.POT_EMP_EMPREG_TEMP+" f "
                + "INNER JOIN "+Constants.TABLE.POT_EMP_ENGAGEMENT+" r ON r.c_temp_emp_fk = f.id "
                + "WHERE r.id = ? ", 
                new String[]{egmntId}
        );
        
        HashMap empHm = db.selectOneRecord(
                "SELECT * FROM "+Constants.TABLE.EMPREG+" WHERE id = ?",
                new String[]{empId}
        );

        Set set = tempProcHm.entrySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            Map.Entry mentry = (Map.Entry) iterator.next();
            
            String new_val = mentry.getValue() == null?"":mentry.getValue().toString();
            String old_val = empHm.get(mentry.getKey()) == null?"":empHm.get(mentry.getKey()).toString();
            
            
            if(old_val.equals(new_val)){
                continue;
            }
            
            if(mentry.getKey().equals("id")){
                continue;
            }
            
            String query = "UPDATE "+Constants.TABLE.EMPREG+" SET " 
                    + mentry.getKey() +" = ? WHERE id = ?";
        
            int i = db.update(query, new String[]{new_val}, new String[]{empId}); 
            
            LogUtil.info("NEW DATA UPDATE", "Updating "+mentry.getKey()+" FROM "
                    +old_val+" -> "+new_val+". Result "+Integer.toString(i));
            
            chgeList = buildChangeAuditHm(db, (String) mentry.getKey(), new_val, old_val, chgeList, "");  
        }        
        
        return chgeList;
    }
}
