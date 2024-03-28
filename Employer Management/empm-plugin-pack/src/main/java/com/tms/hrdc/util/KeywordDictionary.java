/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.util;

import com.tms.hrdc.dao.CurrentUser;
import static com.tms.hrdc.util.CommonUtils.getEnvVar;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;

/**
 *
 * @author faizr
 */
public class KeywordDictionary {
    
    private Map<String,String> NONEMPLRKEYS;  //to hold the non-table related keys    
    private ArrayList<HashMap<String, String>> DATAMAPS = new ArrayList(); // holds the table related keys
    
    private String TABLENAME = "table_name";
    private String ISUSED = "is_used";
    private String DATAMAP = "dataMap";
    private String DOMAIN_URL = "";
    private String recordId = "";
    private Boolean isPreview = false;
//    private String group;
    private String table;
    String queryListHtml = "";
    String rejectListHtml = "";
    String subListHtml = "";

    private String COLLECTED_NON_EMPL_KEYS = "collected_non_empl_keys";
    private final String DATE_OF_LETTER = "date_of_letter";
    private final String QUERY_REASON = "query_reason";
    private final String REJECT_REASON = "reject_reason";
    private final String SUB_REASON = "sub_reason";
    private final String QUERY_DAYLIMIT = "query_dayLimit";
    private final String LINK_TO_FORM1 = "link_to_form";
    private final String MOTTO = "motto";
    private final String CURRENT_INQUIRER = "current_inquirer";    
    private final String POTEMP_BATCH = "ref_batch"; 
    private final String APPL_ADDRESS = "appl_address";
    private final String OFFICER_SIGN_NAME = "officer_sign_name";
    private final String CREATED_USER_MYCOID_LOGIN = "created_user_mycoid_login";
    private final String CREATED_USER_PASSWORD = "created_user_password";
    private final String TEMP_USER_MYCOID_LOGIN = "temp_user_mycoid_login";
    private final String TEMP_USER_PASSWORD = "temp_user_password";
    private final String REGISTRATION_TYPE = "reg_type";
    private final String PORTAL_URL = "portal_url";
    private final String DAYS_LEFT = "days_left";
    private final String DEREG_SUBMISSION_DATE = "dereg_submission_date";
    private final String REG_SUBMISSION_DATE = "reg_submission_date";
    private final String REQ_TYPE_SUBJECT = "req_type_subject";
    private final String REQ_TYPE_TITLE = "req_type_title";
    private final String OLD_SECTOR = "old_sector";
    private final String NEW_SECTOR = "new_sector";
    private final String REQ_TYPE_CONTENT  = "req_type_content";
    private final String REQCHANGE_DATE = "reqChange_date";
    private final String MERGING_ADDRESS = "merging_address";
    private final String DEREG_COMP_NAME = "dereg_comp_name";
    
    private void setNonEmplrKeywords() {
        
        NONEMPLRKEYS = new HashMap();
        
        NONEMPLRKEYS.put(DATE_OF_LETTER, "");
        NONEMPLRKEYS.put(CURRENT_INQUIRER, "");
        NONEMPLRKEYS.put(QUERY_REASON, "");
        NONEMPLRKEYS.put(REJECT_REASON, "");
        NONEMPLRKEYS.put(SUB_REASON, "");
        NONEMPLRKEYS.put(QUERY_DAYLIMIT, "");
        NONEMPLRKEYS.put(LINK_TO_FORM1, "");
        NONEMPLRKEYS.put(MOTTO, "");
        NONEMPLRKEYS.put(POTEMP_BATCH, "");
        NONEMPLRKEYS.put(APPL_ADDRESS, "");
        NONEMPLRKEYS.put(OFFICER_SIGN_NAME, "");
        NONEMPLRKEYS.put(CREATED_USER_PASSWORD, "");
        NONEMPLRKEYS.put(CREATED_USER_MYCOID_LOGIN, "");
        NONEMPLRKEYS.put(TEMP_USER_PASSWORD, "");
        NONEMPLRKEYS.put(TEMP_USER_MYCOID_LOGIN, "");
        NONEMPLRKEYS.put(REGISTRATION_TYPE, "");
        NONEMPLRKEYS.put(PORTAL_URL, "");
        NONEMPLRKEYS.put(DAYS_LEFT, "");
        NONEMPLRKEYS.put(DEREG_SUBMISSION_DATE, "");
        NONEMPLRKEYS.put(REG_SUBMISSION_DATE, "");        
        NONEMPLRKEYS.put(REQ_TYPE_SUBJECT, "");
        NONEMPLRKEYS.put(REQ_TYPE_TITLE, "");
        NONEMPLRKEYS.put(OLD_SECTOR, "");
        NONEMPLRKEYS.put(NEW_SECTOR, "");
        NONEMPLRKEYS.put(REQ_TYPE_CONTENT, "");
        NONEMPLRKEYS.put(REQCHANGE_DATE, "");
        NONEMPLRKEYS.put(MERGING_ADDRESS, "");
        NONEMPLRKEYS.put(DEREG_COMP_NAME, "");
     
    }
    
    public KeywordDictionary(DBHandler db){
        setKeywords(db);
        setNonEmplrKeywords();
    }
    
    private void msg(String msg){
        LogUtil.info(this.getClass().toString(), msg);
    }
    
    public void setIsPreview(){
        this.isPreview = true;
    }
    
    public void setRecordId(String recId){
        this.recordId = recId;
    }
    
    public Map<String,String> getEmplrMapList(){        
        HashMap empRKeywords = new HashMap();
                
        for(HashMap dm:DATAMAPS){
            String tbl = dm.get(TABLENAME).toString();
            
            if(tbl.equals(Constants.TABLE.EMPREG)){
                empRKeywords = (HashMap) dm.get(DATAMAP);
                break;
            }
        }
        
        return empRKeywords;
    }
    
    public HashMap removeBasicKeys(HashMap hm, DBHandler db){
        
        HashMap newHm = (HashMap) hm.clone();     
        
        Set set = hm.entrySet();
        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {            
            Map.Entry mentry = (Map.Entry) iterator.next();            
            if(!getEmplrMapList().containsKey(mentry.getKey())){
                newHm.remove(mentry.getKey());
            }     
        }        
        return newHm;
    }
    
    public Map<String,String> getNonEmplrMapList(){
        return NONEMPLRKEYS;
    }

    private void setKeywords(DBHandler db) {
        
        String query = "SELECT c_tableName,c_columnID,c_columnName, c_group_as "
                + "FROM app_fd_empm_keywords ";
        
        ArrayList<HashMap<String, String>> keywordHm = db.select(query);
        
        HashMap tmpHm = new HashMap();        
        
        for(HashMap keyHm:keywordHm){
            String tblName = keyHm.get("c_tableName").toString();
            
            HashMap colsHm = new HashMap();

            boolean hasKey = false;
            
            for(HashMap hm:DATAMAPS){                
                if( hm.get(TABLENAME).toString().equals(tblName)){
                    hasKey = true;
                    break;
                }
            }

            if(hasKey){
                tmpHm = (HashMap)DATAMAPS.get( getMap(DATAMAPS, tblName) );
                colsHm = (HashMap)tmpHm.get(DATAMAP);
            }else{
                tmpHm = new HashMap();
                tmpHm.put(TABLENAME,tblName);
                tmpHm.put(ISUSED,false);
                tmpHm.put(DATAMAP,null);
                
                DATAMAPS.add(tmpHm);
            }            
            colsHm.put(keyHm.get("c_columnID").toString(), keyHm.get("c_columnName").toString());            
            tmpHm.put(DATAMAP, colsHm);
        }
    }
    
    private boolean hasKey(ArrayList<HashMap<String, String>> dataKeyArray, String tblName) {
        boolean contain = false;
        
        for(HashMap hm: dataKeyArray){
            if(hm.containsValue(tblName)){
                contain=true;
                break;
            }
        }
        
        return contain;
    }
    
    //to get position in hashmap that contains map of data
    private int getMap(ArrayList<HashMap<String, String>> dataKeyArray, String tblName) {
        int map = 0;
        
        for(HashMap hm: dataKeyArray){
            if(hm.containsValue(tblName)){
                break;
            }
            map++;
        }
        
        return map;
    }
    
    public String queryPlaceHolderId = "placeholderID";
    
    public String chgeQryToPlaceHolder(String content){
        String pattern = "<div id=\"placeholderID\">[\\s\\S]*?</div>";

        return content.replaceAll(pattern, "[query_reason]");
    }
    
    String queryReason = "";
    
    public void buildQueryReason(DBHandler db, String q_reason){
        queryReason = "";
        queryListHtml = "";
        String[] qRsnArr = q_reason.split("\\|");
        
//        queryListHtml = "<div id='"+queryPlaceHolderId+"'>"
////                + "<br /><p>Query Reason: <br /> <ul>";
//                + "<br /><p> <ul>";

//        LogUtil.info("KWD","building query "+q_reason);

        String listItemHtml = "";

        for(String each:qRsnArr){
            String reason = getQueryReasonFromSetup(db, each);
            listItemHtml+="<li>"+reason+"</li>";
            
            if(queryReason.isEmpty()){
                queryReason+=reason;
            }else{
                queryReason+=" & "+reason;
            }

//            LogUtil.info("KWD","built query item "+listItemHtml);
        }

        if(!listItemHtml.isEmpty()){
            queryListHtml =
//                    "<div id='"+queryPlaceHolderId+"'>"
//                    + "<br /><p> "
//                    +
                            " <ul>"+listItemHtml+"</ul>";
//                    +"</p><br /> "
//                    +"</div>";
        }

//        LogUtil.info("KWD","built query html "+queryListHtml);
//        queryListHtml+="</ul></p><br /> </div>";
    }
    
    public String rejectPlaceHolderId = "rejectPlaceholderID";
    String rejectReason = "";
    
    public void buildRejectReason(DBHandler db, String reject_reason) {
        rejectListHtml = "";
        rejectReason = "";
        String[] rRsnArr = reject_reason.split("\\|");
        
//        rejectListHtml = "<div id='"+rejectPlaceHolderId+"'>"
////                + "<br /><p>Reject Reason: <br /> <ul>";
//                + "<br /><p><ul>";

        String listItemHtml = "";
        for(String each:rRsnArr){
            String reason = getRejectReasonFromSetup(db, each);
            listItemHtml+="<li>"+reason+"</li>";
            
            if(rejectReason.isEmpty()){
                rejectReason+=reason;
            }else{
                rejectReason+=" & "+reason;
            }
        }

        if(!listItemHtml.isEmpty()){
            rejectListHtml =
//                    "<div id='"+rejectPlaceHolderId+"'>"
//                    + "<br /><p>" +
                    "<ul>"+listItemHtml+"</ul>";
//                    + "</p><br />"
//                    +" </div>";
        }
//        rejectListHtml+="</ul></p><br /> </div>";
    }
    public String subPlaceHolderId = "subPlaceholderID";
    String subReason = "";

    public void buildSubReason(DBHandler db, String sub_reason) {
        String[] subRsnArr = sub_reason.split("\\|");
        
        subListHtml = "<div id='"+subPlaceHolderId+"'>"
                + "<br /><p><ul>";
        for(String each:subRsnArr){
            String sub = getSubReasonFromSetup(db, each);
            subListHtml+="<li>"+sub+"</li>";
            
            
            if(subReason.isEmpty()){
                subReason+=sub;
            }else{
                subReason+=" & "+sub;
            }
        }
        subListHtml+="</ul></p><br /> </div>";   
    }
    
    public String getSubReasonFromSetup(DBHandler db, String subId){
        HashMap hm = db.selectOneRecord(
                "SELECT c_sub_reason FROM app_fd_empm_sub_rsn_stp "
                + "WHERE id = ?", 
                new String[]{subId});
        String sub_reason = "";
        if(hm!=null){
            sub_reason = hm.get("c_sub_reason")==null?"":hm.get("c_sub_reason").toString();
        }else{
//            LogUtil.info("sub reason: ", "can't find");
        }
        
        return sub_reason;    
    
    }
    
    public String getRejectReasonFromSetup(DBHandler db, String reasonId) {
        HashMap hm = db.selectOneRecord(
                "SELECT c_reject_reason FROM app_fd_empm_reject_rsn_stp "
                + "WHERE id = ?", 
                new String[]{reasonId});
        String reject_reason = "";
        if(hm!=null){
            reject_reason = hm.get("c_reject_reason")==null?"":hm.get("c_reject_reason").toString();
        }
        
        return reject_reason;    
    }
        
    public String getBuiltRejectReason() {
        return rejectReason;
    }
    
    public String getBuiltSubReason() {
        return subReason;
    }
    
    public String getQueryReasonFromSetup(DBHandler db, String reasonId){
        HashMap hm = db.selectOneRecord(
                "SELECT c_query_reason FROM app_fd_empm_qry_reason_stp "
                + "WHERE id = ?", 
                new String[]{reasonId});
        String query_reason = "";
        if(hm!=null){
            query_reason = hm.get("c_query_reason")==null?"":hm.get("c_query_reason").toString();
        }
        
        return query_reason;
    }
    
    public String getBuiltQueryReason(){
        return queryReason;
    }
    
    public String addTracker( String content){
        return content+Constants.EMAIL_TRACK;
    }
    
    public String buildContent(DBHandler db, String content, String emplId, String mailId) throws SQLException, UnsupportedEncodingException{
        if(StringUtils.isBlank(content)){
            content = "ERROR 9001 - Please contact HRDC Admin";
        }
        
        if(StringUtils.isBlank(emplId) ){
            content = "ERROR 9002 - Please contact HRDC Admin " + content;
        }
        
        HashMap<String, ArrayList> wordQueryHm = collectKeyword(db,content);
        
        ArrayList<String> nonTableColumns = wordQueryHm.get(COLLECTED_NON_EMPL_KEYS);
//        content = content.replace("[","").replace("]","");

        for(HashMap dataHm:DATAMAPS){            
            if((boolean)dataHm.get(ISUSED)){                
                content = parseContent(db, content, dataHm, emplId);                
            }            
        }

        for (String field: nonTableColumns){

            if( (!isPreview && field.contains("LINK") ||
                    !field.contains("LINK") ||
                    !field.contains("BUTTON")
            )) {
                setNonEmplKeyValue(db, field, emplId, mailId);
                content = content.replace("["+field+"]",
                        NONEMPLRKEYS.get(field ));
            }
        }

        return content;
    }
    
    private String parseContent(DBHandler db, String content, HashMap dataHm, String emplId){
        HashMap tempHm = (HashMap)dataHm.get(DATAMAP);
        String table = (String)dataHm.get(TABLENAME);

        String sql = bldStrFrmHMHead(tempHm);        
        
        switch(table){
            case Constants.TABLE.EMPREG:
                sql = "SELECT "+sql+" FROM "+table +" WHERE id = '"+emplId+"'";
            break;
            case Constants.TABLE.EMPREG_APPL:
                sql = "SELECT "+sql+" FROM "+table +" WHERE c_empl_fk = '"+emplId+"'";
            break;
            case Constants.TABLE.DEREG:
                sql = "SELECT "+sql+" FROM "+table +" WHERE c_dreg_emp_id = '"+emplId+"'";
            break;
            case Constants.TABLE.POT_EMP:
                sql = "SELECT "+sql+" FROM "+table +" WHERE c_emp_fk = '"+emplId+"'";
            break;
            case Constants.TABLE.REQUEST_CHANGES:
                sql = "SELECT "+sql+" FROM "+table +" WHERE c_emp_fk = '"+emplId+"'";
            break;
            default:
                sql = "";
        }

        HashMap<String, String> resultHm = new HashMap();
        if(!sql.isEmpty()){                    
            resultHm = db.selectOneRecord(sql);  
        }

        if(resultHm!=null){
            Set set = resultHm.entrySet();
            Iterator iterator = set.iterator();

            while (iterator.hasNext()) {
                Map.Entry mentry = (Map.Entry) iterator.next();
                String col_head = mentry.getKey().toString();
                String col_value = mentry.getValue().toString();

                if(col_value.contains("-")){
                    col_value = getLabelVal(db, col_value);
                }

                if(col_head.contains("levy_perc")){
                    col_value+="%";
                }
                
                if(isDate(col_head)){
                    col_value = CommonUtils.set_DT_ChangeDateFormatString(col_value, "dd MMMM yyyy");
                }

                content = content.replace("["+col_head+"]",
                                            col_value);
            }
        }

        return content;
    }
    
    private boolean isDate(String col_head){
        boolean isDate = false;
        switch(col_head){
            case "c_levy_liab_pymnt_dt":
            case "c_register_dt":
            case "c_verify_dt":
                isDate = true;
            break;
        }
        
        return isDate;
    }
    
    private HashMap collectKeyword(DBHandler db,String content) throws SQLException {
        Pattern pattern = Pattern.compile("\\[[^\\]]+\\]");
        Matcher matcher;
        
        HashMap<String, ArrayList> words = new HashMap();
        ArrayList<String> non_empl_tblCol = new ArrayList();

        if(!StringUtils.isBlank(content)){
            matcher = pattern.matcher(content);
            while (matcher.find()) {            
                String placeholder = matcher.group().replace("[", "").replace("]", "");
                
                for(HashMap hm:DATAMAPS){ //Iterate between each saved tables
                    HashMap mapHm = (HashMap) hm.get(DATAMAP);
                    
                    if(mapHm.containsKey(placeholder)){
                        hm.put(ISUSED, true);
                        break;
                    }
                }
                if(!non_empl_tblCol.contains(placeholder) &&
                    NONEMPLRKEYS.containsKey(placeholder) ){
                    non_empl_tblCol.add(placeholder);
                }
                if(!non_empl_tblCol.contains(placeholder) && placeholder.contains("LINK")){
                    non_empl_tblCol.add(placeholder);
                    NONEMPLRKEYS.put(placeholder, "");
                }
            }
        }
        
        words.put(COLLECTED_NON_EMPL_KEYS, non_empl_tblCol);        
        return words;
    }

    private void setNonEmplKeyValue(DBHandler db, String field, String emplId, String mailId) throws UnsupportedEncodingException {
        Boolean skip = false;
        String[] splitField = field.split("\\{");
        String secParam = splitField.length>1?
                            splitField[1].replace("}", "")
                            :"";
        String value = "";
        CurrentUser cu;
        boolean isDate = false;
        
        switch(field){
            case DATE_OF_LETTER:
                value = getDate();
            break;
            case QUERY_REASON:
                value = queryListHtml;
            break;
            case REJECT_REASON:
                value = rejectListHtml;
            break;
            case SUB_REASON:
                value = subListHtml;
            break;
            case QUERY_DAYLIMIT:
                value = getDayLimit(db);
            break;
            case MOTTO:
                value = getMotto(db);
            break;
            case CURRENT_INQUIRER:
                value = getCurrentUser(db);
            break;
            case POTEMP_BATCH:
                value = getPotEmpData(db,"c_batch",emplId);
            break;
            case APPL_ADDRESS:
                value = buildLetterAddress(db, emplId);
            break;
            case OFFICER_SIGN_NAME:
                value = getOfficerSignName(db);
            break;
            case CREATED_USER_MYCOID_LOGIN:
                value = db.selectOneValueFromId(Constants.TABLE.EMPREG, 
                        "c_mycoid", emplId
                        );
            break;
            case CREATED_USER_PASSWORD:                
                value = new CurrentUser().byEmployerId(db, emplId).getTempPwMetadata();
            break;
            case TEMP_USER_MYCOID_LOGIN:
                value = db.selectOneValueFromTable(
                            "select\n" +
                            "case\n" +
                            "when c_req_email is null or c_req_email = ''\n" +
                            "then c_empl_email_pri\n" +
                            "else c_req_email \n" +
                            "end as c_req_email\n" +
                            "from app_fd_empm_reg r where id = ?",
                            new String[]{emplId}
                        );
            break;
            case TEMP_USER_PASSWORD:                
                value = db.selectOneValueFromId(Constants.TABLE.EMPREG, 
                        "c_req_pw_re", emplId
                        );
                value = SecurityUtil.decrypt(value);
            break;
            case REGISTRATION_TYPE:
                value = getLabelForRegType(db);
            break;
            case PORTAL_URL:
                value = getEnvVar(Constants.APP_ID.MASTER_STP, "BASE_URL");
            break;
            case DAYS_LEFT:
                value = getDaysLeft(db);
            break;
            case DEREG_SUBMISSION_DATE:
                value = db.selectOneValueFromId(Constants.TABLE.DEREG, 
                        "dateCreated", this.recordId
                        );
                isDate = true;
            break;
            case REG_SUBMISSION_DATE:
                value = db.selectOneValueFromId(Constants.TABLE.EMPREG, 
                        "c_register_dt", emplId
                        );
                isDate = true;
            break;
            case REQ_TYPE_SUBJECT:
                value = db.selectOneValueFromId(Constants.TABLE.REQUEST_CHANGES, 
                         "c_req_type", this.recordId
                         );
                if (value.equals("Change Company Name")){
                    value = "Nama";
                }
                else{
                    value = "Sektor Industri";
                }

                break;
             case REQ_TYPE_TITLE:
                 value = db.selectOneValueFromId(Constants.TABLE.REQUEST_CHANGES, 
                         "c_req_type", this.recordId
                         );
                 if (value.equals("Change Company Name")){
                    value = "NAMA";
                }
                else{
                    value = "SEKTOR INDUSTRI";
                }
                break;
             case REQ_TYPE_CONTENT:
                 value = db.selectOneValueFromId(Constants.TABLE.REQUEST_CHANGES, 
                         "c_req_type", this.recordId
                         );
                 if (value.equals("Change Company Name")){
                    value = "nama";
                }
                else{
                    value = "sektor industri";
                }
                break;
             case OLD_SECTOR:
                String old_sector_id = db.selectOneValueFromId(Constants.TABLE.REQUEST_CHANGES, 
                         "c_sector_search_id_old", this.recordId
                         );
                value = db.selectOneValueFromId(Constants.TABLE.STP_SUB_SECTOR, 
                         "c_descr_bm", old_sector_id
                         );
                 break;
             case NEW_SECTOR:
                String new_sector_id = db.selectOneValueFromId(Constants.TABLE.REQUEST_CHANGES, 
                         "c_sector_search_id_new", this.recordId
                         );

                value = db.selectOneValueFromId(Constants.TABLE.STP_SUB_SECTOR, 
                         "c_descr_bm", new_sector_id
                         );
                 break;
             case REQCHANGE_DATE:
                value = db.selectOneValueFromId(Constants.TABLE.REQUEST_CHANGES, 
                         "dateCreated", this.recordId
                         );
                 isDate = true;
                break;
             case MERGING_ADDRESS:
                String merge_comp = db.selectOneValueFromId(Constants.TABLE.DEREG, 
                            "c_merge_comp_id", this.recordId
                        );
                value = buildLetterAddress(db, merge_comp);
                break;
             case DEREG_COMP_NAME:
                value = db.selectOneValueFromId(Constants.TABLE.DEREG, 
                            "c_comp_name", this.recordId
                        );
                break; 
                 
        }

        if(isDate){
            value = CommonUtils.set_DT_ChangeDateFormatString(value, "dd MMMM yyyy");
        }
        
        //parse for html links
        if(field.contains("LINK") ){
            
            LinkFactory lf = new LinkFactory();

            if(secParam.equals("TRACK")){
                value = lf.setTracker(db, secParam, "", mailId);
            }else if(secParam.equals("QR")){
                value = lf.generateLink(db,field, emplId,this.recordId , secParam, mailId);
                value = lf.setTracker(db, secParam, value, mailId);
            }else{
                value = lf.generateLink(db,field, emplId,this.recordId, secParam, mailId);
            }
        }            

        NONEMPLRKEYS.put(field, value);                  
    }        
    
    public String getOfficerSignName(DBHandler db){
        HashMap hm = db.selectOneRecord(
                "SELECT CONCAT(firstName, ' ', lastName) as name\n" +
                "FROM app_fd_empm_appvr_usr_stp s\n" +
                "INNER JOIN dir_user d ON d.id = s.c_usr\n" +
                "WHERE c_eo_fk = ?", 
                new String[]{Constants.DATA_ID.MAIN_SETUP_ID});
        String name = "";
        if(hm!=null){
            name = hm.get("name").toString();
        }
        
        return name;
    }

    private String getDate() {
//        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy");
        LocalDateTime now = LocalDateTime.now();
        
        return dtf.format(now);
    }

    private String getCurrentUser(DBHandler db) {
        String firstName = AppUtil.processHashVariable("#currentUser.firstName#", null, null, null);
        String lastName = AppUtil.processHashVariable("#currentUser.lastName#", null, null, null);

        String officer_name = firstName+" "+lastName;
        
        return officer_name.isEmpty()?getOfficerSignName(db):officer_name;
    }
    
    private String buildLetterAddress(DBHandler db, String emplId) {
        String letterAddress = "";
        
//        String query = "SELECT " +
//                "CONCAT(r.c_empl_address, ' ', r.c_empl_address2) AS address1, " +
//                "r.c_empl_address3 AS address2, " +
//                "r.c_empl_postcode AS postcode, c.c_city AS city, UPPER(s.c_state) AS state " +
//                "FROM app_fd_empm_reg r " +
//                "INNER JOIN app_fd_stp_location l ON r.c_empl_postcode = l.c_postcode " +
//                "INNER JOIN app_fd_stp_city c ON r.c_empl_city = c.id " +
//                "INNER JOIN app_fd_stp_state s ON r.c_empl_state = s.id " +
//                "WHERE r.id = ? " +
//                "LIMIT 1 ";
        
        String query = "SELECT  \n" +
                "r.c_bu_address1 AS address1,  \n" +
                "r.c_bu_address2 AS address2,  \n" +
                "r.c_bu_address3 AS address3,  \n" +
                "r.c_bu_postcode AS postcode,  \n" +
                "r.c_bu_city as city,  \n" +
                "r.c_bu_state AS state  \n" +
                "FROM app_fd_empm_reg r  \n" +
                "WHERE r.id = ? \n" +
                "LIMIT 1; ";
        
        HashMap hm = db.selectOneRecord(query, new String[]{emplId});
        
        if (hm != null) {
            String address1 = hm.get("address1")== null ? "": hm.get("address1").toString();
            String address2 = hm.get("address2")== null ? "": hm.get("address2").toString();
            String address3 = hm.get("address3")== null ? "": hm.get("address3").toString();
            String postcode = hm.get("postcode")== null ? "": hm.get("postcode").toString();
            String city = hm.get("city")== null ? "": hm.get("city").toString();
            String state = hm.get("state")== null ? "": hm.get("state").toString();

            city = MasterSetupData.getCityLabel(db, city);
            city = StringUtils.isBlank(city)?MasterSetupData.getCityByPostcode(db, postcode):city;

            state = MasterSetupData.getStateLabel(db, state);
            state = StringUtils.isBlank(state)?MasterSetupData.getStateByPostcode(db, postcode):state;

            letterAddress =
                    address1 + "<br>" +
                    address2 + "<br>" +
                    (address3.isEmpty()?"":address3+"<br>") +
                    postcode + " " + city + "<br>" +
                    state;
        }
        return letterAddress;
    }

    private String getDaysLeft(DBHandler db) {
        String query = "SELECT c_idle_days FROM app_fd_empm_reg_stp WHERE id = 'epm_stp'";
        String days = "0";
        
        HashMap hm = db.selectOneRecord(query);
        
        if(hm!=null){
           days = hm.get("c_idle_days")==null?"0":hm.get("c_idle_days").toString();
        }
        String submissionDate_query = "SELECT dateCreated FROM app_fd_empm_usr_mail WHERE c_mail_fk=? ORDER BY dateCreated DESC LIMIT 1";
        HashMap submissionDate_HM = db.selectOneRecord(submissionDate_query, new String[]{this.recordId});
        String submissionDate =  submissionDate_HM.get("dateCreated")== null ? "": submissionDate_HM.get("dateCreated").toString();
        String deadline_query = "SELECT DATE_ADD(?, INTERVAL ? DAY) AS deadline";
        HashMap deadline_queryHM = db.selectOneRecord(deadline_query, new String[]{submissionDate,days});
        String deadline = deadline_queryHM.get("deadline")== null ? "": deadline_queryHM.get("deadline").toString();
        String days_left_query = "SELECT DATEDIFF(?,NOW()) AS days_left";
        HashMap days_leftHM = db.selectOneRecord(days_left_query,new String[]{deadline});
        String days_left = days_leftHM.get("days_left")== null ? "": days_leftHM.get("days_left").toString();
        return days_left;
    }

    private String getMotto(DBHandler db) {
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy");  
        LocalDateTime now = LocalDateTime.now();  
        
        String current_year = dtf.format(now);
        String baseMotto = "";
        String query = "SELECT * FROM app_fd_empm_stp_mail_motto "
                + "WHERE curdate() between STR_TO_DATE(concat(c_time_start,'-"+current_year+"'), '%d-%b-%Y') and " 
                + "STR_TO_DATE(concat(c_time_end,'-"+current_year+"'), '%d-%b-%Y')";
        
        ArrayList<HashMap<String, String>> stpList = db.select(query);
        
        for(HashMap stpHm:stpList){
            //motto, date_start date_end
            String motto = stpHm.get("c_motto")==null?"":stpHm.get("c_motto").toString();
            
            baseMotto+="\""+motto+"\"<br />";
        }
        
        return baseMotto;
    }

    private String getLabelVal(DBHandler db, String col_value) {
        
        HashMap hm = new HashMap();
        String query = "";
        
        query = "select id, concat(s.c_descr, '(',s.c_sub_sector_code,')') as i_value "
                + "from app_fd_stp_sub_sector s "
                + "where id = ?";
        hm = db.selectOneRecord(query, new String[]{col_value});
        
        if(hm!=null){
            return (String) hm.getOrDefault("i_value", col_value);
        }
        
        query = "select id, concat(c_descr, '(',c_main_sector_code,')') as i_value "
                + "from app_fd_stp_main_sector "
                + "where id = ?";
        hm = db.selectOneRecord(query, new String[]{col_value});
        
        if(hm!=null){
            return (String) hm.getOrDefault("i_value", col_value);
        }
        
        query = "select id, concat(a.c_descr, '(',a.c_div_code,')') as i_value "
                + "from app_fd_stp_industry_div a "
                + "where id = ?";
        hm = db.selectOneRecord(query, new String[]{col_value});
        
        if(hm!=null){
            return (String) hm.getOrDefault("i_value", col_value);
        }
        
        query = "select id, concat(s.c_industry_sector, ' (', s.c_industry_sector_code, ')') as i_value"
                + " from app_fd_stp_industry_sector s "
                + "where id = ?";
        hm = db.selectOneRecord(query, new String[]{col_value});
        
        if(hm!=null){
            return (String) hm.getOrDefault("i_value", col_value);
        }
        
        query = "select id, c_location as i_value "
                + "from app_fd_stp_location "
                + "where id = ?";
        hm = db.selectOneRecord(query, new String[]{col_value});
        
        if(hm!=null){
            return (String) hm.getOrDefault("i_value", col_value);
        }
        
        query = "select id, c_city as i_value from app_fd_stp_city "
                + "where id = ?";
        hm = db.selectOneRecord(query, new String[]{col_value});
        
        if(hm!=null){
            return (String) hm.getOrDefault("i_value", col_value);
        }
        
        query = "select id, c_state as i_value from app_fd_stp_state "
                + "where id = ?";
        hm = db.selectOneRecord(query, new String[]{col_value});
        
        if(hm!=null){
            return (String) hm.getOrDefault("i_value", col_value);
        }
        
        query = "select id, c_country as i_value from app_fd_stp_country "
                + "where id = ?";
        hm = db.selectOneRecord(query, new String[]{col_value});
        
        if(hm!=null){
            return (String) hm.getOrDefault("i_value", col_value);
        }
        
        return col_value;
    }

    private String getPotEmpData(DBHandler db, String c_batch, String keyValue) {
        String query = "SELECT u.c_batch FROM app_fd_empm_pe_potEmp h "
                + "INNER JOIN app_fd_empm_pe_file_upl u on u.id = h.c_batch "
                + "WHERE c_emp_fk = ?";
        HashMap hm = db.selectOneRecord(query, new String[]{keyValue});
        
        return hm==null?"":hm.getOrDefault("c_batch", "").toString();
    }

    private String bldStrFrmHMHead(HashMap<String, String> tempHm) {
        String sql = "";
        if (tempHm == null){
            return "";
        }
        Set set = tempHm.entrySet();
        Iterator iterator = set.iterator();

        sql = tempHm.entrySet().stream()
                .map(entry -> entry.getKey())
                .collect(Collectors.joining(", "));

//        while (iterator.hasNext()) {
//            Map.Entry mentry = (Map.Entry) iterator.next();
//            String col_head = mentry.getKey().toString();
//            String col_value = mentry.getValue().toString();
//
//            if(sql.isEmpty()){
//                sql += col_head;
//            }else{
//                sql += ","+col_head;
//            }
//        }
        return sql;
    }
    
    public HashMap getDifferenceValues(HashMap original, HashMap changed){
        
        if(original==null||changed==null){
//            LogUtil.info("Keyword Dictionary", "Null hashmap received in getting difference");
            return null;
        }

        if(original.equals(changed)){
//            LogUtil.info("Keyword Dictionary", "Is data change? Nuh uh..");
            return null;
        }
        
        Iterator bKeyIterator = changed.keySet().iterator();
        Object key;
        Object value;
        HashMap difference = new HashMap();

        while (bKeyIterator.hasNext()) {
            key = bKeyIterator.next();
            
            if(key.equals("id")){
                continue;
            }
            
            if (original.containsKey(key)) {
                value = changed.get(key);
                difference.put(key, value);
            }
        }        
        return difference;
    }
    
    public void mergeChanges(String empId, HashMap difference) {
        if(difference!=null){

            Set set = difference.entrySet();
            Iterator iterator = set.iterator();
            HashMap chgeHm = new HashMap();
            
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

                chgeHm.put(colName, colVal);
            }

            CommonUtils.saveUpdateForm2("", 
                    Constants.FORM_ID.EMP_TEMP_MAIN_FORM, empId, chgeHm);

//            String query = "INSERT INTO "+Constants.TABLE.POT_EMP_EMPREG_TEMP+" ("+colNames+") "
//                    + "VALUES ("+values+")";
//            int i = db.update(query);
//            LogUtil.info("Keyword Dictionary", "Change "+chgeHm.toString());
//            temp_empId = CommonUtils.saveUpdateForm2("", Constants.FORM_ID.POTEMP_ENG_TEMPREG_FORM, 
//                                    "", dupHm);
        }
    }
    
    public void recordChanges(String fk, HashMap original, HashMap changed) {        
        Iterator bKeyIterator = changed.keySet().iterator();
        Object key;
        Object newValue;
        Object oldValue;        

        while (bKeyIterator.hasNext()) {
            key = bKeyIterator.next();
            
            if(key.equals("id")){
                continue;
            }
                        
            if (original.containsKey(key)) {
                newValue = changed.get(key);
                oldValue = original.get(key);
                
                if(newValue.equals(oldValue)){
                    continue;
                }
                
                msg("Finding difference.. field "+key+" - oldval: "+oldValue+", newval: "+newValue);
                
                HashMap chgeHm = new HashMap();
                chgeHm.put("field_name", key);
                chgeHm.put("prev_value", oldValue);
                chgeHm.put("curr_value", newValue);
                chgeHm.put("status", Constants.STATUS.EGMNT_CHANGE_STATUS.NEW);
                chgeHm.put("fk", fk);
                
                CommonUtils.saveUpdateForm("", Constants.FORM_ID.AUDIT_TRAIL_SUB, "", chgeHm);
            }
        }        
    }
    
    public static String formatVal(DBHandler db, String isLocType, String isSectType, String value){
        if(isSectType.equals("true")){
            return getSectLabel(db, value);
        }
        
        if(isLocType.equals("true")){
            return getLocLabel(db, value);
        }
        
        return value;
    }
    
    public static HashMap getFieldProperty(DBHandler db, String id){
        String query = "select \n" +
                        "c_columnID, c_columnName, c_group_as,\n" +
                        "c_isCallNumber, c_isLocationField, c_isSectorField\n" +
                        "from app_fd_empm_keywords k where c_columnID = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{id});
        
        return hm;
    }
    
    public static String getSectLabel(DBHandler db, String id) {
        String query = "select data.id, data.i_value from (\n" +
                        "select id, concat(s.c_descr, '(',s.c_sub_sector_code,')') as i_value from app_fd_stp_sub_sector s\n" +
                        "UNION\n" +
                        "select id, concat(c_descr, '(',c_main_sector_code,')') from app_fd_stp_main_sector \n" +
                        "UNION\n" +
                        "select id, concat(a.c_descr, '(',a.c_div_code,')') from app_fd_stp_industry_div a\n" +
                        "UNION\n" +
                        "select id, concat(s.c_industry_sector, ' (', s.c_industry_sector_code, ')') from app_fd_stp_industry_sector s\n" +
                        ") data WHERE data.id = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{id});
        
        if(hm==null){
            return id;
        }
        
        return hm.get("i_value")==null?"":hm.get("i_value").toString();
    }

    public static String getLocLabel(DBHandler db, String id) {
        String query = "select data.id, data.i_value from (\n" +
                    "select id, c_location as i_value from app_fd_stp_location\n" +
                    "union\n" +
                    "select id, c_city from app_fd_stp_city\n" +
                    "union\n" +
                    "select id, c_state from app_fd_stp_state\n" +
                    "union\n" +
                    "select id, c_country from app_fd_stp_country\n" +
                    ") data WHERE data.id = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{id});
        
        if(hm==null){
            return id;
        }
        
        return hm.get("i_value")==null?"":hm.get("i_value").toString();
    }

    private String getLabelForRegType(DBHandler db) {
        String query = "select c_empl_reg_type from app_fd_empm_reg where id = ?";
        HashMap hm = db.selectOneRecord(query,new String[]{this.recordId});
        
        if(hm==null){
            return "";
        }
        if (hm.get("c_empl_reg_type")==null){
            return "";
        }
        return hm.get("c_empl_reg_type")=="HQ"?"MAJIKAN":"CAWANGAN MAJIKAN SECARA BERASINGAN";
        
    }

    private String getDayLimit(DBHandler db) {
        return "";
    }




 
}
