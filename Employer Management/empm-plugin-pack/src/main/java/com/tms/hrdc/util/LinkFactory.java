/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.joget.commons.util.LogUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class LinkFactory {
    
    private void msg(String msg){
        LogUtil.info(this.getClass().toString(), msg);
    }

    public JSONObject getKeywords(DBHandler db, HttpServletRequest request) throws JSONException{
        
        String group = request.getParameter("group")==null?"":request.getParameter("group");
        
        String query = "SELECT c_keyword as keyword, c_descr as descr FROM app_fd_empm_links";
        
        ArrayList<HashMap<String,String>> result = db.select(query);
        
        JSONArray shell = new JSONArray();
        JSONObject data = new JSONObject();
        
        for(HashMap hm:result){
            data = new JSONObject(hm);
            shell.put(data);
        }
        return new JSONObject().put("special", shell);
    }
    
    /*
    @param db           - Query engine class
    @param link_type    - Keyword for Links for system to match with real urls
    @param keyvalue     - Data Ids to be set as parameter values
    @param label        - Label words set over the <a></a> tag
    @param mailId       - ID where the email data will is stored
    */
    public String generateLink(DBHandler db, String link_type, String keyValue, String recordId ,String label, String mailId) throws UnsupportedEncodingException {
        
        boolean isButton = false;
        
        HashMap hm = db.selectOneRecord("select md5(concat(id,'"+keyValue+"')) as hashKey from app_fd_empm_usr_mail WHERE id = '"+mailId+"'");
        String hashed = hm==null?"ERROR":hm.getOrDefault("hashKey", "ERROR").toString();
        String baseUrl = CommonUtils.getBaseURL()+"/jw/web/userview/empm/emp/_/";
            
        String html_link = "<a href='";
        String key = "", isInbox = "", url = "";
        
        if(link_type.contains("BUTTON")){
            isButton = true;
            link_type = link_type.replace("BUTTON_", "");
        }
        
        if(link_type.contains("{")){
            String[] splitField = link_type.split("\\{");
            link_type = splitField[0];
        }
//        LogUtil.info("Key", link_type);
        String sql = "SELECT * FROM app_fd_empm_links WHERE c_keyword = ?";
        HashMap linkHm = db.selectOneRecord(sql, new String[]{link_type});
//        
        if(linkHm!=null){            
            key = linkHm.get("c_link")==null?"":linkHm.get("c_link").toString();
            isInbox = linkHm.get("c_is_inbox")==null?"":linkHm.get("c_is_inbox").toString();
        }

        switch(link_type){
            case Constants.LINK_TEMPLATE.LINK_FORM1:
                url = baseUrl+key+"?empRegId="+keyValue+"&mailId="+hashed;
            break;
            case Constants.LINK_TEMPLATE.LINK_FORM1_REG:
                url = baseUrl+key;
            break;
            case Constants.LINK_TEMPLATE.LINK_LOGIN:
                url = baseUrl+key;
            break;
            case Constants.LINK_TEMPLATE.LINK_ONBOARDING:
                url = baseUrl+key;
            break;
            default:
                url = baseUrl+key+"?id="+recordId;
        }
//        LogUtil.info("URL", url);
        if(!isInbox.isEmpty() && !recordId.isEmpty() && link_type.equals("LINK_INBOX_EMPLOYER")){
            url = baseUrl+key+"?activityId="+getEmplActivityIdFromRecordId(db,recordId);
        }
        if(!isInbox.isEmpty() && !recordId.isEmpty() && link_type.equals("LINK_INBOX_STAFF")){
           url = baseUrl+key+"?activityId="+getStaffActivityIdFromRecordId(db,recordId);
        }
        
        if(isButton){
            return buttonElement(url, label);
        }else{
            return html_link += url + "'>" + label + "</a>";
        }
//        LogUtil.info("Link", html_link);
        
    }
    
    private String buttonElement(String url, String label){
            
        return "<table style=\"font-family: 'Cabin',sans-serif;\" width=\"100%\">\n" +
            "<tbody>\n" +
            "<tr>\n" +
            "<td style=\"overflow-wrap: break-word; word-break: break-word; padding: 10px; font-family: 'Cabin',sans-serif;\">\n" +
            "<div style=\"text-align: center;\">"
                + "<a class=\"v-button\" href='"+url+"' "
                + " style=\"box-sizing: border-box; display: inline-block; text-decoration: none; -webkit-text-size-adjust: none; text-align: center; color: #ffffff; background-color: #ff6600; border-radius: 4px; -webkit-border-radius: 4px; -moz-border-radius: 4px; width: auto; max-width: 100%; overflow-wrap: break-word; word-break: break-word; word-wrap: break-word; mso-border-alt: none; font-size: 14px;\" target=\"_blank\"> "
                + "<span style=\"display: block; padding: 14px 44px 13px; line-height: 120%;\">"
                + "<span style=\"font-size: 16px; line-height: 19.2px;\">"
                + "<strong>"
                + "<span style=\"line-height: 19.2px; font-size: 16px;\">"+label
                + "</span>"
                + "</strong> "
                + "</span> "
                + "</span> </a></div>\n" +
            "</td>\n" +
            "</tr>\n" +
            "</tbody>\n" +
            "</table>";
    }
    
    /*
    @param db           - Query engine class
    @param label        - QR/TRACK
    @param QRurl        - OPTIONAL : url where the qr will be redirecting to
    @param mailId       - ID where the email data will is stored
    */
    public String setTracker(DBHandler db, String label, String optURL,  String mailId) throws UnsupportedEncodingException {
        
        HashMap hm = db.selectOneRecord(
                "select md5(concat(id,c_mail_fk)) as hashKey from app_fd_empm_usr_mail WHERE id = '"+mailId+"'"
        );
        String hashed = hm==null?"ERROR":hm.getOrDefault("hashKey", "ERROR").toString();
        
        db.update(
                "UPDATE "+Constants.TABLE.EMAIL+" SET c_mail_status = ?, c_is_seen = ? WHERE id = ?",
                new String[]{Constants.STATUS.EMAIL.SENT, Constants.STATUS.EMAIL.SENT},
                new String[]{mailId}
        );
        
        String html_link = "";
        if(label.equals("QR")){
            
//            LogUtil.info("email", "mailId "+mailId+", hash "+hashed);
            String updAPI = CommonUtils.getBaseURL()
                            + "/jw/web/json/plugin/com.tms.hrdc.webservice.EmpmAPI/service?method=trackOpenQR"
                            + "&s="+URLEncoder.encode(hashed, "UTF-8")
                            + "&l="+URLEncoder.encode(optURL, "UTF-8")
                            + "&browser=#requestParam.id#";

            html_link = "<br /><img src=\""+updAPI+"\" style=\"width:200px;\" alt=\"QR\">";
        }else{ //1 pix tracker
//            LogUtil.info("email", "mailId "+mailId+", hash "+hashed);
            String updAPI = CommonUtils.getBaseURL()
                            + "/jw/web/json/plugin/com.tms.hrdc.webservice.EmpmAPI/service?method=trackOpen"
                            + "&s="+URLEncoder.encode(hashed, "UTF-8")
                            + "&l="+URLEncoder.encode(optURL, "UTF-8")
                            + "&browser=#requestParam.id#";

            html_link = "<br /><hr><img src=\""+updAPI+"\" style=\"width:200px;\" alt=\"HRDC-logo\">";
        }
        
        return html_link;
    }
    
    private String getStaffActivityIdFromRecordId(DBHandler db, String recordId){
        LogUtil.info("getStaffActivityIdFromRecordId: ",recordId);
        String sql = "SELECT shk.ActivityId\n" +
                    "FROM wf_process_link wpl\n" +
                    "INNER JOIN 	SHKAssignmentsTable shk on shk.ActivityProcessId = wpl.processId\n" +
                    "WHERE wpl.originProcessId = ? " +
                    "AND shk.ActivityId not like '%query%' ";
        HashMap hm = db.selectOneRecord(sql, new String[]{recordId});

        if(hm!=null){

            String activityId = hm.get("ActivityId")==null?"":hm.get("ActivityId").toString();
            msg("activityId: "+activityId);
            return activityId;
        }
        return "";
    }
    
    private String getEmplActivityIdFromRecordId(DBHandler db, String recordId){
        
        String sql = "SELECT shk.ActivityId\n" +
                    "FROM wf_process_link wpl\n" +
                    "INNER JOIN SHKAssignmentsTable shk on shk.ActivityProcessId = wpl.processId\n" +
                    "WHERE wpl.originProcessId = ? " +
                    "AND shk.ActivityId like '%query%' ";
        HashMap hm = db.selectOneRecord(sql, new String[]{recordId});

        if(hm!=null){
            String activityId = hm.get("ActivityId")==null?"":hm.get("ActivityId").toString();            
            return activityId;
        }
        
        msg("activityId not found for record id "+recordId);
        
        return "";
    }
    
    private static String getMyCoid(DBHandler db, String keyValue) {
        String sql = "SELECT c_mycoid FROM app_fd_empm_reg WHERE id = ?";
        HashMap hm = db.selectOneRecord(sql, new String[]{keyValue});
       
        if(hm!=null){
            return hm.get("c_mycoid")==null?"":hm.get("c_mycoid").toString();
        }
        return "";
    }
    
}
