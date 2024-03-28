/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.defaultPluginTool;

import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.HttpUtil;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.http.HttpHeaders;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONException;
import org.json.JSONObject;
    
/**
 *
 * @author faizr
 */
public class QueryReminder extends DefaultApplicationPlugin{

    @Override
    public String getName() {
        return this.getClass().toString();
    }

    @Override
    public String getVersion() {
        return "69";
    }

    @Override
    public String getDescription() {
        return "Used with a scheduler to send reminders to any unresponded queries";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - Scheduler For Query Reminder Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/data_collector_json.json", null, true, "message/emailConfig");
    }
    
    PluginManager pluginManager = null;
    WorkflowManager workflowManager = null;
    WorkflowAssignment wfAssignment = null;
    
    static String url_scheme = "";
    static String url_serverName = "";
    static String url_serverPort = "";
    static String url_contextPath = "";
    
    String sql = "SELECT "
                + "c_mail_type, c_mail_to, c_mail_subject, c_mail_content, " 
                + "c_reminder_isActive, c_reminder_count, c_days_between, c_bsns_days_only,"
                + "c_mail_sent_count, c_last_mail_dt "
                + "FROM app_fd_empm_usr_mail m WHERE "
                + " id = ? ";
    
    final String MAIL_TYPE = "c_mail_type";
    final String MAIL_TO = "c_empl_email_pri";
    final String MAIL_SUBJECT = "c_mail_subject";
    final String MAIL_CONTENT = "c_mail_content";
    final String MAIL_SENT_COUNT = "c_mail_sent_count";
    final String MAIL_LAST_SENT_DT = "c_last_mail_dt";
    final String MAIL_REM_COUNT = "c_reminder_count"; //how many to send
    final String MAIL_REM_ISACTIVE = "c_reminder_isActive";
    final String MAIL_DAYS_BETWEEN = "c_days_between";
    final String MAIL_BUSINESS_DAY_ONLY = "c_bsns_days_only";
//    final String MAIL_TIMING_HM = "mail_timing";
    
    final String MAIL_REM_DAYS_BETWEEN = "c_days_between";
    final String MAIL_REM_DAYS_BSNS_ONLY = "c_bsns_days_only";
    
    @Override
    public Object execute(Map props) {
        LogUtil.info(this.getClassName(), "Query Scheduler Starting... ");
        DBHandler db = new DBHandler();
        
        wfAssignment = (WorkflowAssignment) props.get("workflowAssignment");             
        pluginManager = (PluginManager) props.get("pluginManager");
        workflowManager = (WorkflowManager) pluginManager.getBean("workflowManager"); 
        
        url_scheme = getPropertyString("url_scheme");
        url_serverName = getPropertyString("url_serverName");
        url_serverPort = getPropertyString("url_serverPort");
        url_contextPath = getPropertyString("url_contextPath");
        
        try{
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
            
//            HashMap stpHm = getQuerySetupData(db);
            ArrayList<HashMap<String, String>> empArr = getEmpData(db);
            LogUtil.info(this.getClassName(), 
                    "Getting standing query starting... emp size: "+ empArr.size());
            //iterate
            // calculate date of reminders, number of dates based on setup number
            // if rem_count ++ < stp number : send email, update rem_count
            // if rem_count ++ == stp_number : complete query task, and add audit trail
            
            String rem_sent_count = "", query_dt = "", actId = "", empId = "", 
                    pDefId = "", pId = "", queryId = "";
            
            String remIsActive = "No";
            String remCountLimit = "0";
            String remSentCount = "0";
            String bsnsDaysOnly = "No";
            String daysBtwn = "0";
            String lastSentStr = "";
            
            String mailSubject = "";
            String mailContent = "";
            String mailTo = "";
            
            for(HashMap empHm: empArr){
                LogUtil.info(this.getClassName(), 
                    "Query for employer "+ empArr.toString());
                
                queryId = (empHm.get("c_q_templ") == null)? "":(String)empHm.get("c_q_templ");
                remSentCount = (empHm.get("c_rem_count") == null || empHm.get("c_rem_count") == "")? "0":(String)empHm.get("c_rem_count");
                query_dt = (empHm.get("queryDate") == null)? "":(String)empHm.get("queryDate");
                actId = (empHm.get("ActivityId") == null)? "":(String)empHm.get("ActivityId");
                empId = (empHm.get("id") == null)? "":(String)empHm.get("id");
                pDefId = (empHm.get("PdefName") == null)? "":empHm.getOrDefault("PdefName","").toString().replace("#",":");;
                pId = (empHm.get("ProcessId") == null)? "":(String)empHm.get("ProcessId");
                
                pDefId = pDefId.replace("#", ":");
                
                HashMap qDataHm = getQueryData(db, queryId);
//                ArrayList<HashMap<String, String>> qRemHm = null;
                
                if(qDataHm!=null){
                    mailSubject = qDataHm.get(MAIL_SUBJECT)==null?"ERROR":qDataHm.get(MAIL_SUBJECT).toString();
                    mailContent = qDataHm.get(MAIL_CONTENT)==null?"ERROR":qDataHm.get(MAIL_CONTENT).toString();
                    mailTo = qDataHm.get(MAIL_TO)==null?"":qDataHm.get(MAIL_TO).toString();
                    
                    remIsActive = qDataHm.get(MAIL_REM_ISACTIVE)==null?"No":qDataHm.get(MAIL_REM_ISACTIVE).toString();
                    remCountLimit = qDataHm.get(MAIL_REM_COUNT)==null?"5":qDataHm.get(MAIL_REM_COUNT).toString();
                    remSentCount = qDataHm.get(MAIL_SENT_COUNT)==null?"0":qDataHm.get(MAIL_SENT_COUNT).toString();
                    daysBtwn = qDataHm.get(MAIL_DAYS_BETWEEN)==null?"1":qDataHm.get(MAIL_DAYS_BETWEEN).toString();
                    bsnsDaysOnly = qDataHm.get(MAIL_BUSINESS_DAY_ONLY)==null?"No":qDataHm.get(MAIL_BUSINESS_DAY_ONLY).toString();
                    lastSentStr = qDataHm.get(MAIL_LAST_SENT_DT)!=null?
                            qDataHm.get(MAIL_LAST_SENT_DT).toString():
                            CommonUtils.get_DT_CurrentDateTime("YYYY-MM-dd HH:mm:ss");
//                    qRemHm = (ArrayList) qDataHm.get(MAIL_TIMING_HM);                    
                }else{
                    LogUtil.info(this.getClassName(), 
                    "No empData for this bih");
                }
                                
                //using today - 
                // if reminderActive
                // if sentCount < sendLimit
                // if today & last sent = daysBtwn
                // if bsnsdaysonly = Yes, is today bsnsday?
                
                int remSentCountInt = Integer.parseInt(remSentCount);
                int remCountLimitInt = Integer.parseInt(remCountLimit);
                int daysBtwnInt = Integer.parseInt(daysBtwn);
                
                boolean SENDDAEMAILBISH = false;
                boolean QUERYCOMPLETE = false;
                
                if(remIsActive.equals("No")){
                    QUERYCOMPLETE =true;
                }
                
                if(remSentCountInt>=remCountLimitInt){
                    QUERYCOMPLETE =true;
                }else{
                    remSentCountInt++;
                }
                
                String todayStr = CommonUtils.get_DT_CurrentDateTime("YYYY-MM-dd HH:mm:ss");
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss", Locale.ENGLISH);
                LocalDate todayDt = LocalDate.parse(todayStr, formatter);
                LocalDate lastSentDt = LocalDate.parse(lastSentStr, formatter);
                
                int daysSinceLastMail = countBtwnDays(lastSentDt, todayDt, bsnsDaysOnly);
                                
                if(daysSinceLastMail<daysBtwnInt || daysBtwnInt==0){
                    continue;
                }
                
                workflowManager.activityVariable(wfAssignment.getActivityId(), "query_respond", "false"); 
                if(QUERYCOMPLETE){                    
                    workflowManager.activityVariable(wfAssignment.getActivityId(), "pass_query_limit", "true"); 
                    LogUtil.info(this.getClassName(), 
                                "Query limit for "+empId);
                }else{
                    workflowManager.activityVariable(wfAssignment.getActivityId(), "pass_query_limit", "false"); 
//                    sendEmail(db, queryId);
                    LogUtil.info(this.getClassName(), 
                                        "Sending query to "+empId);
                    CommonUtils.sendEmail(mailTo, "", mailSubject, mailContent, "", null);
                    updateMailDetail(db, queryId, todayStr, Integer.toString(remSentCountInt));
                }
                //if all favors - send email
                completeQueryAct(pDefId, pId, actId);

                //update empm_usr_mail - c_last_mail_dt, c_mail_sent_count
                LogUtil.info("QUERY REMINDER", "10 - end tool");
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            db.closeConnection();
        }
        LogUtil.info("QUERY REMINDER", "11 - end outside tool");
        return null;
    }
    
    public void updateMailDetail(DBHandler db, String queryId, String todayDt, String remSentCount){
        String sql = "UPDATE "+Constants.TABLE.EMAIL+" "
                        + "SET c_last_mail_dt = ?, c_mail_sent_count "
                        + "WHERE id = ?";
                db.update(sql, new String[]{todayDt,remSentCount}, 
                        new String[]{queryId});
    }
    
    private int countBtwnDays(LocalDate startDt, LocalDate endDt, String inclBsDays) {
        
        int daysCount = 0;
        
        for (LocalDate date = startDt;
                date.isBefore(endDt);
                date = date.plusDays(1)){
            
            DayOfWeek day = date.getDayOfWeek();
            
            //1-Monday, 6-Saturday, 7-Sunday
            if ( ((day != DayOfWeek.SATURDAY) &&
                    (day != DayOfWeek.SUNDAY)) ||
                    inclBsDays.equals("No")){
                daysCount++;
            }
        }
        return daysCount;
    }
    
    public static boolean isWeekend(final LocalDate ld){
        DayOfWeek day = DayOfWeek.of(ld.get(ChronoField.DAY_OF_WEEK));
        return day == DayOfWeek.SUNDAY || day == DayOfWeek.SATURDAY;
    }

    private HashMap getQuerySetupData(DBHandler db) {
        String query = "SELECT s.c_rem_num_limit, s.c_rem_duration_unit, c_idle_days FROM app_fd_empm_reg_stp s";        
        HashMap stpHm = db.selectOneRecord(query);
        
        query = "SELECT c_query_days FROM app_fd_empm_query_stp ";        
        ArrayList daysHm = db.select(query);
        
        stpHm.put("days", daysHm);
        
        return stpHm;
    }

    private ArrayList getEmpData(DBHandler db) {
        String query = "SELECT \n" +
                        "e.id,\n" +
                        "e.dateCreated AS AppDate,\n" +
                        "e.dateModified AS queryDate,\n" +
                        "r.c_rem_count,\n" +
                        "shk.ActivityId,\n" +
                        "a.ProcessId,  a.PdefName, \n" +
                        "r.c_q_templ, \n" +
                        "r.c_empl_email_pri \n" +
                        "from app_fd_empm_regAppl e\n" +
                        "INNER JOIN app_fd_empm_reg r ON r.id = e.c_empl_fk\n" +
                        "INNER JOIN wf_process_link wpl on e.id = wpl.originProcessId\n" +
                        "INNER JOIN SHKAssignmentsTable shk on shk.ActivityProcessId = wpl.processId\n" +
                        "INNER JOIN  SHKActivities a ON a.Id = shk.ActivityId \n" +
                        "WHERE shk.ActivityId like '%activity_query_2'" ;

        return db.select(query);
    }

    private ArrayList calcRemDates(int rem_day_limit, int rem_count_limit, String query_dt) {  
        
        //some dates is yyyy-MM-dd HH:mm:ss.S
//        query_dt = query_dt.split(".")[0];
        
        int counter = 0;
        
        ArrayList datesArr = new ArrayList();
        String format =  "yyyy-MM-dd HH:mm:ss";
        
        if(query_dt.contains(".")){
            format =  "yyyy-MM-dd HH:mm:ss.SSS";
            LogUtil.info("Query Reminder", "has microseconds");
        }
        
        Date base_date = CommonUtils.set_DT_String2Date(query_dt, format);
        
        while(counter<rem_count_limit){            
            Calendar c = Calendar.getInstance();
            c.setTime(base_date);
            c.add(Calendar.DATE, rem_day_limit);  // number of days to add
            
            Date added_day = c.getTime();
            base_date = added_day;
            datesArr.add(added_day);
            
            counter++;
        }
        
        return datesArr;
    }
    
    private boolean ifTodayIsEmailDay(ArrayList<Date> emailDates) throws ParseException {
        //today
        long millis = System.currentTimeMillis();
        java.sql.Date todayDate = new java.sql.Date(millis);
        emailDates.add(todayDate);
        boolean todayIsEmailDay = false;
        
        for(Date emailDate:emailDates){
            if(emailDate == null){
                continue;
            }         

            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
            fmt.format(emailDate);
            fmt.format(todayDate);
            
            if(fmt.format(emailDate).equals(fmt.format(todayDate))){
                todayIsEmailDay = true;
            }            
        }        
        return todayIsEmailDay;
    }

    private void completeQueryAct(String pDefId, String pId, String actId) {
        
        String encoding = Base64.getEncoder().encodeToString((Constants.API.JOGETAPI.MASTERAPILOGIN + ":" + Constants.API.JOGETAPI.MASTERAPIPW).getBytes());
        String params = "processDefId="+pDefId+"&processId="+pId+"&activityId="+actId;        
        String endpoint = CommonUtils.getBaseURL()+"/jw/web/json/monitoring/running/activity/complete?"+params;
        HashMap headers = new HashMap();

        HttpUtil httpUtil = new HttpUtil();

        try {
            headers.put(HttpHeaders.AUTHORIZATION, "Basic " + encoding);
            httpUtil.setHeader(headers);
            httpUtil.sendPostRequest(endpoint);
            
            JSONObject obj = httpUtil.getJSONResponse();           
                        
            LogUtil.info("Query Reminder","Completing current activity: "+actId+" content: "+ 
                    obj.toString() );
            
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } catch (KeyManagementException ex) {
            ex.printStackTrace();
        } catch (KeyStoreException ex) {
            ex.printStackTrace();
        } catch (JSONException ex) {
            ex.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private HashMap getQueryData(DBHandler db, String qId) {
        String sql = "SELECT "
                + "c_mail_type, c_mail_to, c_mail_subject, c_mail_content, " 
                + "c_reminder_isActive, c_reminder_count, c_days_between, c_bsns_days_only,"
                + "c_mail_sent_count, c_last_mail_dt "
                + "FROM app_fd_empm_usr_mail m WHERE "
                + " id = ? ";
        HashMap hm = db.selectOneRecord(sql, new String[]{qId});
        
        if(hm==null){
            return null;
        }
        
        return hm;
    }
    
}
