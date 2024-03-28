/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.util;

import com.tms.hrdc.dao.CurrentUser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.security.SecureRandom;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.joget.apps.app.dao.EnvironmentVariableDao;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.dao.PluginDefaultPropertiesDao;
import org.joget.apps.app.lib.EmailTool;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.EnvironmentVariable;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.model.PluginDefaultProperties;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormService;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SecurityUtil;
import org.joget.commons.util.SetupManager;
import org.joget.commons.util.StringUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.directory.dao.GroupDao;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.Group;
import org.joget.directory.model.Role;
import org.joget.directory.model.User;
import org.joget.directory.model.service.DirectoryUtil;
import org.joget.directory.model.service.UserSecurity;
import org.joget.plugin.property.service.PropertyUtil;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.WorkflowProcessResult;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.json.JSONArray;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class CommonUtils {
    
    public static String generateAndSavePDF(String receiptPdfURL, String doc_name, String mail_id, String tableName) throws  IOException {
                
        String absolutePath = SetupManager.getBaseDirectory() + "app_formuploads" + File.separator + tableName + File.separator + mail_id;
//        pm("Absolute Path: "+absolutePath);

        boolean dirExists = true, isPdf = false; 
        String result = "";
        
        URL url = new URL(receiptPdfURL); 
        
        // Contacting the URL
        URLConnection urlConn = url.openConnection();
        
        if (StringUtils.isBlank(urlConn.getContentType()) || !urlConn.getContentType().contains("application/pdf")) {
            LogUtil.info("JASPER REPORT GENERATION","FAILED.. [Sorry. This is not a PDF.]");
        } else {
            isPdf = true;
            File folder = new File(absolutePath);

            if (!folder.exists()) {
                LogUtil.info("JASPER REPORT GENERATION","Directory not exist!");
                if (folder.mkdirs()) {
//                    LogUtil.info("JASPER REPORT GENERATION","Directory is created!");
                } else {
                    LogUtil.info("JASPER REPORT GENERATION","uploadFile error > Failed to create directory!");
                    dirExists = false;
                }
            }else{
//                pm("Directory exist!");
            }

            if (dirExists) {
//                LogUtil.info("JASPER REPORT GENERATION","Saving file");
                String filePath = absolutePath + File.separator + doc_name;
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
                //generate Jasper report here
                FileUtils.copyURLToFile(url, file);
                
                //encrypt file                
                
                PDDocument pdd = PDDocument.load(file);
//                PDDocument pdd = Loader.loadPDF(file);
 
//                AccessPermission ap = new AccessPermission();
//                StandardProtectionPolicy stpp
//                    = new StandardProtectionPolicy(username, username, ap);
//                
//                stpp.setEncryptionKeyLength(128);
//                stpp.setPermissions(ap);
//                pdd.protect(stpp);
                pdd.save(file);
                pdd.close();
                
//                LogUtil.info("JASPER REPORT GENERATION","File Saved");
                result = filePath;               
                
            } 
        }
        
        return result;
    }
    
    public static List<HashMap<String, String>> getRunningProcessList(DBHandler db, String recordId){
        
        ArrayList procList = db.select(
            "SELECT distinct k.ActivityProcessId FROM \n" +
            "SHKAssignmentsTable k\n" +
            "INNER JOIN wf_process_link w ON w.processId = k.ActivityProcessId\n" +
            "WHERE w.originProcessId = ? ",
            new String[]{recordId}
        );
        
        return procList;       
    }
    
    public static JSONObject getArrangedJson(){
        return new JSONObject(){
            @Override
            public JSONObject put(String key, Object value) throws JSONException {
                try {
                    Field map = JSONObject.class.getDeclaredField("map");
                    map.setAccessible(true);
                    Object mapValue = map.get(this);
                    if (!(mapValue instanceof LinkedHashMap)) {
                        map.set(this, new LinkedHashMap<>());
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                return super.put(key, value);
            }
        };
    }
    
    public static void startProcess(String processDefKey, String recordId, Map variables){
        WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");

        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        
        WorkflowProcess processDef = appService.getWorkflowProcessForApp(
                appDef.getId(), 
                appDef.getVersion().toString(),
                processDefKey);
        
        String processDefId = processDef.getId();
        
        WorkflowProcessResult result = workflowManager.processStart(processDefId, null, variables, new CurrentUser().getId(),recordId, false);
        LogUtil.info(appDef.toString(), "Starting Process "+processDefKey+" - Status: " + result.getProcess().getInstanceId());
        
    }
    
    public static void duplicateDir(String srcId, String dupId, String tblName){
        String oriDir = Constants.JOGET_BASE_UPL_PATH
                +File.separator
                +tblName.replace("app_fd_","")
                +File.separator
                +srcId;
        String duplDir = Constants.JOGET_BASE_UPL_PATH
                +File.separator
                +tblName.replace("app_fd_","")
                +File.separator
                +dupId;

        File oriFile = new File(oriDir);
        File duplFile = new File(duplDir);

        try {
            FileUtils.copyDirectory(oriFile, duplFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String getTable(DBHandler db, String id){
        
        String query = 
                "select \n" +
                "case\n" +
                "	when exists (SELECT id FROM "+Constants.TABLE.EMPREG+" WHERE id = ?)\n" +
                "  then 'app_fd_empm_reg'\n" +
                "  when exists (SELECT id FROM "+Constants.TABLE.EMPREG_APPL+" WHERE id = ?)\n" +
                "  then 'app_fd_empm_regAppl'\n" +
                "  when exists (SELECT id FROM "+Constants.TABLE.DEREG+" WHERE id = ?)\n" +
                "  then 'app_fd_empm_dereg'\n" +
                "  when exists (SELECT id FROM "+Constants.TABLE.DEREG_F5+" WHERE id = ?)\n" +
                "  then 'app_fd_empm_dereg'\n" +
                "  when exists (SELECT id FROM app_fd_empm_dereg_wd WHERE id = ?)\n" +
                "  then 'app_fd_empm_dereg'\n"+ 
                "  else '' " +
                " end as 'table'";
        HashMap hm = db.selectOneRecord(query,new String[]{id, id, id, id, id});
        
        String table = "";
        
        if(hm!=null){
            table = hm.get("table").toString();
        }
        
        return table;
    }
    
    public static String getBaseURL(){
        String url = getEnvVar(Constants.APP_ID.MASTER_STP, "BASE_URL");
//        
        if(url.isEmpty()){
           return WorkflowUtil.getHttpServletRequest().getScheme()+"://"+WorkflowUtil.getHttpServletRequest().getServerName();
        }
        return url;
    }
    
    public static HashMap getCurrentUser(){
        WorkflowUserManager wum = (WorkflowUserManager) AppUtil.getApplicationContext().getBean("workflowUserManager");
        String username = wum.getCurrentUsername();
        User user = wum.getCurrentUser();
        
        String ERROR = "USER NULL";
        
        HashMap userHm = new HashMap();
        userHm.put("username", ERROR);
        userHm.put("firstName", ERROR);
        userHm.put("lastName", ERROR);
        userHm.put("email", ERROR);
        userHm.put("id", ERROR);
        
        if(user!=null){
            userHm.put("username", username);
            userHm.put("firstName", user.getFirstName());
            userHm.put("lastName", user.getLastName());
            userHm.put("email", user.getEmail());
            userHm.put("id", user.getId());
        }
        
        return userHm;
    }  
    
    public static String getRefNo(String size, String envVarKey){
        Integer count = 0;
        
        try {
            AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
            AppDefinition appDef = appService.getPublishedAppDefinition(Constants.APP_ID.EMPM);
            EnvironmentVariableDao environmentVariableDao = (EnvironmentVariableDao) AppUtil.getApplicationContext().getBean("environmentVariableDao");
            EnvironmentVariable evVar = environmentVariableDao.loadById(envVarKey, appDef);
            
            if (evVar != null) {
                count = environmentVariableDao.getIncreasedCounter(envVarKey, "Increase", appDef);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String formattedCount = String.format("%0"+size+"d", count);
        
        return formattedCount;
    }
    
    public static String getAPSITGCode(DBHandler db){
        
        String aps_itg_refno = "0";
        String getTodayDate = AppUtil.processHashVariable("#date.yyyyMMdd#", null, null, null);
        boolean sameDay = false;
        
        String getSavedDateCode = getEnvVar(Constants.APP_ID.EMPM, Constants.ENV_VAR.APS_ITG_DATE);
        if(!StringUtils.isBlank(getSavedDateCode) && getSavedDateCode.equals(getTodayDate)){
            sameDay = true;
        }
        
        if(sameDay){
            aps_itg_refno = getRefNo("6", Constants.ENV_VAR.APS_ITG_ID);
        }else{ //if new day, reset
            setEnvVar(Constants.APP_ID.EMPM, Constants.ENV_VAR.APS_ITG_ID, "0");
            setEnvVar(Constants.APP_ID.EMPM, Constants.ENV_VAR.APS_ITG_DATE, getTodayDate);
            aps_itg_refno = getRefNo("6", Constants.ENV_VAR.APS_ITG_ID);
        }
        
        return aps_itg_refno;        
    }
    
    public static String getEmpId_PotEmp(DBHandler db, String regId){
        String query = "SELECT r.c_emp_fk FROM app_fd_empm_pe_potEmp r WHERE id = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{regId});
        
        if(hm!=null){
            return hm.get("c_emp_fk")==null?"":hm.get("c_emp_fk").toString();
        }        
//        LogUtil.info("Get ID - getEmpId_PotEmp", "(Error) Registration ID: "+regId+" - Empl ID: NO DATA");
        
        return "";
    }
    
    public static ArrayList<HashMap<String, String>> getPeId_batch(DBHandler db, String batchId){
        String query = "SELECT r.id FROM app_fd_empm_pe_potEmp r WHERE c_batch = ?";
        
        ArrayList<HashMap<String, String>> list = db.select(query, new String[]{batchId});
        
        if(list.size()>0){
            return list;
        }        
//        LogUtil.info("Get ID - getEmpId_batch", "(Error) Registration ID: "+batchId+" - Batch ID: NO DATA");
        
        return new ArrayList();
    }
    
    public static String getEmpId_empReg(DBHandler db, String regId){
        String query = "SELECT c_empl_fk FROM app_fd_empm_regAppl WHERE id = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{regId});
        
        if(hm!=null){
            return hm.get("c_empl_fk")==null?"":hm.get("c_empl_fk").toString();
        }        
        LogUtil.info("Get ID - getEmpId_empReg", "(Error) Registration ID: "+regId+" - Empl ID: NO DATA");
        
        return "";
    }
    
    public static String getEmplId_DeregWD(DBHandler db, String deregwdId) {
        
        String deregId = "";
        
        HashMap hm = db.selectOneRecord(
                "SELECT c_dereg_id FROM app_fd_empm_dereg_wd WHERE id = ?", 
                new String[]{deregwdId});
        if(hm!=null){    
            deregId = hm.get("c_dereg_id").toString();
        }else{
            LogUtil.info("Get emp id", "no main dereg f4 id found for dwd "+deregwdId);
            return "";
        }
        
//        LogUtil.info("dereg_id: ",hm.get("c_dereg_id").toString());
        HashMap empHm = db.selectOneRecord(
                "SELECT c_dreg_emp_id FROM app_fd_empm_dereg WHERE id = ?",
                new String[]{deregId}
        );
        
        if (empHm != null){
            return empHm.get("c_dreg_emp_id")==null?"":empHm.get("c_dreg_emp_id").toString();
        }else{
            LogUtil.info("Get emp id", "no main dereg f4 data found for deregId "+deregId);
            return "";
        }
    }
    
    public static String getEmplId_DeregWD_F5(DBHandler db, String deregwdId) {
        
        String deregId = "";
        
        HashMap hm = db.selectOneRecord(
                "SELECT c_dereg_id FROM app_fd_empm_dereg_wd WHERE id = ?", 
                new String[]{deregwdId});
        if(hm!=null){            
            deregId = hm.getOrDefault("c_dereg_id","").toString();
        }else{
            LogUtil.info("Get emp id", "no main f5 dereg_id found for deregId "+deregwdId);
            return "";
        }
        
        HashMap empHm = db.selectOneRecord(
                "SELECT c_merge_comp_id FROM app_fd_empm_dereg where c_f5_fk = ?",
                new String[]{deregId}
        );
        
        if (empHm != null){
            return empHm.get("c_merge_comp_id")==null?"":empHm.get("c_merge_comp_id").toString();
        }else{
            LogUtil.info("Get emp id", "no main dereg f5 data found for deregId "+deregId);
            return "";
        }
    }
    
    public static String getEmplId_Egmnt(DBHandler db, String regId) {
        String query = "select pe.c_emp_fk from app_fd_empm_pe_potEmp pe\n" +
                        "INNER JOIN app_fd_empm_pe_egmnt e ON e.c_pe_fk = pe.id\n" +
                        "WHERE e.id = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{regId});
        if(hm!=null){            
            return hm.get("c_emp_fk")==null?"":hm.get("c_emp_fk").toString();
        }
        LogUtil.info("Get ID - getEmplIdEgmnt", "(Error) Registration ID: "+regId+" - Empl ID: NO DATA");
        return "";
    }
    
    public static String getEmplId_PE_FromEng(DBHandler db, String regId) {
        String query = "select p.c_emp_fk \n" +
                        "FROM app_fd_empm_pe_potEmp p\n" +
                        "INNER JOIN app_fd_empm_pe_egmnt e ON e.c_pe_fk = p.id\n" +
                        "WHERE e.id = ? ";
        
        HashMap hm = db.selectOneRecord(query, new String[]{regId});
        if(hm!=null){            
            return hm.get("c_emp_fk")==null?"":hm.get("c_emp_fk").toString();
        }
        LogUtil.info("Get ID - getPE FROM Eng", "(Error) Registration ID: "+regId+" - Empl ID: NO DATA");
        return "";
    }
    
    public static String getEmplId_WriteOff(DBHandler db, String regId) {
        String query = "select " +
                        "p.c_emp_fk" +
                        "from app_fd_empm_pe_writeoff w " +
                        "inner join app_fd_empm_pe_potEmp p on p.c_writeOff_fk = w.id "+
                        "WHERE w.id = ? ";
        
        HashMap hm = db.selectOneRecord(query, new String[]{regId});
        if(hm!=null){            
            return hm.get("c_emp_fk")==null?"":hm.get("c_emp_fk").toString();
        }
        LogUtil.info("Get ID - getEmplIdWriteOff", "(Error) Registration ID: "+regId+" - Empl ID: NO DATA");
        return "";
    }
    
    public static String getEmpId_empDereg(DBHandler db, String regId){
        String query = "SELECT c_dreg_emp_id FROM app_fd_empm_dereg WHERE id = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{regId});
        LogUtil.info("Get ID", hm.toString());
        if(hm!=null){            
            return hm.get("c_dreg_emp_id")==null?"":hm.get("c_dreg_emp_id").toString();
        }        
        LogUtil.info("Get ID - getEmpId_empDereg", "(Error) Registration ID: "+regId+" - Empl ID: NO DATA");
        return "";
    }
    
    public static String getEmpId_empDeregF5(DBHandler db, String regId){
        String query = "SELECT c_merge_comp_id FROM app_fd_empm_dereg WHERE id = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{regId});
        
        if(hm!=null){            
            return hm.get("c_merge_comp_id")==null?"":
                    hm.get("c_merge_comp_id").toString();
        }        
        LogUtil.info("Get ID - getEmpId_empDeregF5", "(Error) Registration ID: "+regId+" - Empl ID: NO DATA");
        return "";
    }
    
    public static String getEmpId_reqChange(DBHandler db, String regId){
        String query = "SELECT c_emp_fk FROM "+Constants.TABLE.REQUEST_CHANGES+" WHERE id = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{regId});
        if(hm!=null){
            return hm.get("c_emp_fk")==null?"":hm.get("c_emp_fk").toString();
        }        
        LogUtil.info("Get ID - getEmpId_reqChange", "(Error) Registration ID: "+regId+" - Empl ID: NO DATA");
        return "";
    }
    
    public static String getEmplId_CKSPComplaint(DBHandler db, String id) {
        String query = "select r.id from app_fd_empm_pe_compl_cksp c\n" +
                        "inner join app_fd_empm_pe_potEmp p on p.id = c.c_pe_fk\n" +
                        "inner join app_fd_empm_reg r on r.id = p.c_emp_fk\n"+
                        "where c.id = ?";
        
        HashMap hm = db.selectOneRecord(query, new String[]{id});
        if(hm!=null){
            return hm.get("id")==null?"":hm.get("id").toString();
        }        
        LogUtil.info("Get ID - getEmpId_reqChange", "(Error) Registration ID: "+id+" - Empl ID: NO DATA");
        return "";
    }
    
    public static String getEmplId_ALL(DBHandler db, String id){
        String empId = getEmpId_empReg(db, id);
//        getEmplId_DeregWD(db, id);
//        getEmplId_DeregWD_F5(db, id);
        if(empId.isEmpty()){
            empId = getEmpId_empDereg(db, id);      
        }
        if(empId.isEmpty()){
            empId = getEmpId_reqChange(db, id);
        }
        if(empId.isEmpty()){
            empId = getEmpId_empDeregF5(db, id);
        }
        if(empId.isEmpty()){
            empId = getEmplId_Egmnt(db, id);
        }
        if(empId.isEmpty()){
            empId = getEmplId_PE_FromEng(db, id);
        }
        if(empId.isEmpty()){
            empId = getEmplId_WriteOff(db, id);
        }
        if(empId.isEmpty()){
            empId = getEmplId_CKSPComplaint(db, id);
        }
          
        return empId;
    }

    
    public static void insertIntoUserMap(String userId, String compId) {
        HashMap userHm = new HashMap();
        userHm.put("userId", userId);
        userHm.put("compId", compId);
        
        CommonUtils.saveUpdateForm("","bridge_user", "", userHm); 
    }
    
    public static boolean createJogetUser(String userId, String username, String firstName,
            String lastName, String email, String pw_enc, String groupId){
                
        boolean created = false;
        try {
            UserSecurity us = DirectoryUtil.getUserSecurity();

            GroupDao groupDao = (GroupDao) AppUtil.getApplicationContext().getBean("groupDao");   
            UserDao ud = (UserDao) AppUtil.getApplicationContext().getBean("userDao");
            User user = new User();
            
            String password = SecurityUtil.decrypt(pw_enc);
                        
            HashSet setGroup = new HashSet();
            Group group = new Group();
            
            if(groupId.contains(";")){
                String[] groupArr = groupId.split(";");
                
                for(String grp:groupArr){
                    group = groupDao.getGroup(grp);
                    if(group!=null){
                        setGroup.add(group);
                    }
                }
            }else{
                group = groupDao.getGroup(groupId);
                if(group!=null){
                    setGroup.add(group);
                }
            }
            
            user.setGroups(setGroup);
            
            user.setId(userId); 
            user.setUsername(username);
            
            if(us!=null){
                user.setPassword(us.encryptPassword(username, password));
            }else{
                user.setPassword(StringUtil.md5Base16(password));
            }            
            user.setConfirmPassword(password);
            
            
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setActive(1);

            HashSet setRole = new HashSet();
            Role role = new Role();
            role.setId("ROLE_USER");
            setRole.add(role);
            user.setRoles(setRole);
            
            
            
            if (ud.getUserById(userId) != null) {
                LogUtil.info("USER CREATION", "Updating user "+user.getUsername());
                ud.updateUser(user);
                return created;
            }else{
                LogUtil.info("USER CREATION", "Creating user "+user.getUsername());
                ud.addUser(user);
            }
            
            created = true;
            if (us != null) {
                   us.insertUserPostProcessing(user);
               }
            
//            if(ud.getUserById(userId)!=null){
////                LogUtil.info("USER CREATION", "Assigning user to group ");
//                if(groupId.contains(";")){
//                    String[] groupArr = groupId.split(";");
//
//                    for(String grp:groupArr){
////                        LogUtil.info("USER CREATION", "Group "+grp);
//                        ud.assignUserToGroup(userId, grp);
//                    }
//                }else{
////                    LogUtil.info("USER CREATION", "Group "+groupId);
//                    ud.assignUserToGroup(userId, groupId);
//                }
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return created;
    }
    
    public static String generateRandomPassword(int length) {
        String uppercaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String symbols = "!@#$%^*()-_+=?";
        String numbers = "0123456789";

        SecureRandom random = new SecureRandom();

        // Ensure at least one uppercase letter, one symbol, and one number
        char upperCaseChar = uppercaseLetters.charAt(random.nextInt(uppercaseLetters.length()));
        char symbolChar = symbols.charAt(random.nextInt(symbols.length()));
        char numberChar = numbers.charAt(random.nextInt(numbers.length()));

        // Combine them with random lowercase letters
        String combinedChars = uppercaseLetters + lowercaseLetters + symbols + numbers;

        StringBuilder password = new StringBuilder();

        // Add the mandatory characters
        password.append(upperCaseChar);
        password.append(symbolChar);
        password.append(numberChar);

        // Add remaining characters
        for (int i = 0; i < 7; i++) {
            char randomChar = combinedChars.charAt(random.nextInt(combinedChars.length()));
            password.append(randomChar);
        }

        // Shuffle the characters to make the password more random
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            char temp = passwordArray[index];
            passwordArray[index] = passwordArray[i];
            passwordArray[i] = temp;
        }

        return new String(passwordArray);
    }
    
    public static java.util.Date set_DT_String2Date(String dateS, String dateFormat){
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);  
        java.util.Date date = null;
        try {
            date = formatter.parse(dateS);
        } catch (Exception ex) {
             ex.printStackTrace();
        }
        
        return date;
    }
    
    public static String set_DT_ChangeDateFormatString(String dateS, String chge_to_dateFormat){
        
        if(StringUtils.isBlank(dateS)){
            return "";
        }
        
        dateS = set_DT_DateReformatYYYYMMDD(dateS);        
        java.util.Date tempDateD = set_DT_String2Date( dateS,"yyyy-MM-dd");
        return set_DT_ChangeDateFormatString(tempDateD, chge_to_dateFormat);
    }
    
    public static String set_DT_ChangeDateFormatString(java.util.Date date, String chge_to_dateFormat){
        SimpleDateFormat formatter = new SimpleDateFormat(chge_to_dateFormat);  
        return formatter.format(date);
    }
    
    public static String set_DT_DateReformatYYYYMMDD(String date){
        
        String regex_ddMMyyyy_slash = "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{4}$";
        String regex_yyyyMMdd_slash = "^\\d{4}/(0[1-9]|1[0-2])/(0[1-9]|[12][0-9]|3[01])$";
        String regex_ddMMyyyy_hyphen = "^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[0-2])-\\d{4}$";
        String regex_yyyyMMdd_hyphen = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$";
        String regex_MMddyy_slash = "^(0[1-9]|1[0-2])/(0[1-9]|[12][0-9]|3[01])/\\d{2}$";
        String regex_ddMMyy_slash = "^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{2}$";
        String regex_Mdyy_slash = "^(0?[1-9]|1[0-2])/(0?[1-9]|1\\d|2[0-9]|3[01])/\\d{2}$";
        
        String timeRegex = "^\\d{2}:\\d{2}:\\d{2}\\.\\d$";
        
        String regex_yyyyMMdd_hhmmssS_hyphen = regex_yyyyMMdd_slash+" "+timeRegex;
        String regex_yyyyMMdd_hhmmss_hyphen = regex_yyyyMMdd_slash+" "+timeRegex;
                
        String dateFormat = "";
        String properFormat = "yyyy-MM-dd";

        // Check if the string matches the pattern
        if (Pattern.compile(regex_yyyyMMdd_hhmmss_hyphen).matcher(date).matches()) {
            dateFormat = "yyyy-MM-dd hh:mm:ss";
        } 
        
        if (Pattern.compile(regex_yyyyMMdd_hhmmssS_hyphen).matcher(date).matches()) {
            dateFormat = "yyyy-MM-dd hh:mm:ss.S";
        } 
        
        if (Pattern.compile(regex_ddMMyyyy_slash).matcher(date).matches()) {
            dateFormat = "dd/MM/yyyy";
        } 
        
        if (Pattern.compile(regex_yyyyMMdd_slash).matcher(date).matches()) {
            dateFormat = "yyyy-MM-dd";
        } 
        
        if (Pattern.compile(regex_ddMMyyyy_hyphen).matcher(date).matches()) {
            dateFormat = "dd-MM-yyyy";
        } 
        
        if (Pattern.compile(regex_yyyyMMdd_hyphen).matcher(date).matches()) {
            dateFormat = "yyyy-MM-dd";
        } 
        
        if (Pattern.compile(regex_ddMMyy_slash).matcher(date).matches()) {
            dateFormat = "dd/MM/yy";
        } 
        
        if (Pattern.compile(regex_MMddyy_slash).matcher(date).matches()) {
            dateFormat = "MM/dd/yy";
        } 
        
        if (Pattern.compile(regex_Mdyy_slash).matcher(date).matches()) {
            dateFormat = "M/d/yy";
        } 
        
        if(!dateFormat.isEmpty()){
//            date = CommonUtils.set_DT_ChangeDateFormatString(CommonUtils.set_DT_String2Date(date, dateFormat), "yyyy-MM-dd");
            SimpleDateFormat inputDateFormat = new SimpleDateFormat(dateFormat);
            SimpleDateFormat outputDateFormat = new SimpleDateFormat(properFormat);
        
            try {
                java.util.Date dateD = inputDateFormat.parse(date);
                date = outputDateFormat.format(dateD);
                
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }
        return date;
    }
    
    
    
    public static boolean is_DT_DateValid(String dateS, String dateFormat) {
        
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);  
        formatter.setLenient(false);
        try {
            formatter.parse(dateS);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }
    
    public static ArrayList VectorToArrayList(Vector dataVector){
        
        DataFormatter formatter = new DataFormatter();
        ArrayList rows = new ArrayList();
        
        for (int i = 0; i < dataVector.size(); i++){ //iterate thru rows
            Vector cellStoreVector = (Vector) dataVector.elementAt(i);   
            ArrayList columns = new ArrayList();
            for (int j = 0; j < cellStoreVector.size(); j++) { //iterate thru columns
                Cell myCell = (Cell) cellStoreVector.elementAt(j);
                
                String cell_value = formatter.formatCellValue(myCell); 
                cell_value = cell_value.replace("`", "");
                cell_value = set_DT_DateReformatYYYYMMDD(cell_value);
                
                columns.add(cell_value);
            }
            
            columns.add(Integer.toString(i+1));
            rows.add(columns);
        }
        if(rows.size() > 0){
            rows.remove(0);
        }
        
        return rows;
    }
    
    public static Vector ReadCSV(String fileName, int startAtRow) throws FileNotFoundException, IOException, InvalidFormatException {
        Vector cellVectorHolder = new Vector();
        startAtRow--;
       
        FileInputStream myInput = new FileInputStream(fileName);
        Workbook wb = WorkbookFactory.create(myInput);

        Sheet sheet = wb.getSheetAt(0);

        int headerCount = 0;
        //Get Header count
        Row header =  sheet.getRow(startAtRow);
        Iterator cellHeaderIter = header.cellIterator();

        while(cellHeaderIter.hasNext()) {
            Cell myCell = (Cell) cellHeaderIter.next();
            if(myCell != null && !myCell.toString().isEmpty()){
                headerCount++;
            }                
        }

        Iterator rowIter = sheet.rowIterator();

        while(rowIter.hasNext()) {

            if(startAtRow!=0){
                startAtRow--;
                rowIter.next();
                continue;
            }

            Row myRow = (Row) rowIter.next();
            Vector cellStoreVector = new Vector();

            for(int i=0;i<headerCount;i++){
                
                Cell myCell = (Cell) myRow.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);                
                cellStoreVector.addElement(myCell);
            }

            cellVectorHolder.addElement(cellStoreVector);
        }

        return cellVectorHolder;
    }
    
    public static String ReadTXT(String fileName) throws FileNotFoundException, IOException {
        
        int contInt = 0;
        String contStr = "";
        FileInputStream myInput = new FileInputStream(fileName);
        while ((contInt = myInput.read()) != -1) {
            contStr += (char)contInt;
        }
        myInput.close();      

        return contStr;
    }
    
    public static ArrayList<ArrayList<String>> cleanData(String dataRaw, String delimiter, String linebreak, String isFirstRowHeader){
        
        String[] rowsArr = dataRaw.split(Constants.SYMBOLTABLE.get(linebreak));
        
        ArrayList rows = new ArrayList();
//        LogUtil.info("data row", rowsArr[0]+" "+Constants.SYMBOLTABLE.get(linebreak));
        for(String row:rowsArr){
//            LogUtil.info("data row", row);
            String[] columnsArr = row.split(Constants.SYMBOLTABLE.get(delimiter));
            ArrayList columns = new ArrayList();
            for(String column:columnsArr){
                columns.add(column);
            }
            
            rows.add(columns);
        }
        
        if(isFirstRowHeader.equals("true")){
            rows.remove(0);
        }
        
        return rows;
    }

    public static int strToInt(String number){
        int converted = 0;

        try{
            converted = Integer.parseInt(number);
        }catch (Exception e){

        }

        return converted;
    }
    
    public static String logConnection(final HttpURLConnection connection) throws IOException, URISyntaxException {
        int code = connection.getResponseCode();
        String message = connection.getResponseMessage();
        String url = connection.getURL().toURI().toString();

        return String.format("Response from %s - Code: %d, Message: %s", url, code, message);
    }

    public static JSONObject JSONify(String payload) throws JSONException, TransformerConfigurationException, TransformerException {
        JSONObject data = null;

        Document doc = convertStringTOJSONDocument(payload);
        doc.getDocumentElement().normalize();

        DOMSource domSource = new DOMSource(doc);
        Transformer transformer = null;
        transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter sw = new StringWriter();
        StreamResult sr = new StreamResult(sw);
        transformer.transform(domSource, sr);
        data = XML.toJSONObject(sw.toString());

        return data;
    }

    private static Document convertStringTOJSONDocument(String JSONString) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder builder = null;
        try {
            
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(JSONString)));
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getEnvVar(String appId, String envVar) {
        String result = "";

        try {
            AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
            AppDefinition appDef = appService.getPublishedAppDefinition(appId);
            EnvironmentVariableDao environmentVariableDao = (EnvironmentVariableDao) AppUtil.getApplicationContext().getBean("environmentVariableDao");
            EnvironmentVariable evVar = environmentVariableDao.loadById(envVar, appDef);
            result = evVar.getValue();
        } catch (Exception e) {
        }

        return result;
    }

    public static void setEnvVar(String appId, String envVar, String value) {
        try {
            AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
            AppDefinition appDef = appService.getPublishedAppDefinition(appId);
            EnvironmentVariableDao environmentVariableDao = (EnvironmentVariableDao) AppUtil.getApplicationContext().getBean("environmentVariableDao");
            EnvironmentVariable evVar = environmentVariableDao.loadById(envVar, appDef);
            if (evVar != null) {
                evVar.setValue(value);
                environmentVariableDao.update(evVar);
            }
        } catch (Exception e) {
        }
    }
        
    public static void saveUpdateForm(String appId, String formId, String pk_id, HashMap<String, String> hashMap) {

        if (pk_id == null || pk_id.isEmpty()) {
            pk_id = UuidGenerator.getInstance().getUuid();
        }
        
        if(appId.isEmpty()){
            appId = Constants.APP_ID.EMPM;
        }

        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        Long appVersion = appService.getPublishedVersion(appId);
        AppDefinition appDef = appService.getAppDefinition(appId, appVersion.toString());
        FormDefinitionDao formDefinitionDao = (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");
        FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");

        FormDefinition formDef = formDefinitionDao.loadById(formId, appDef);

        Form form = null;
        if (formDef != null) {
            String formJson = formDef.getJson();

            if (formJson != null) {
                form = (Form) formService.createElementFromJson(formJson);
            }

            if (form != null && hashMap != null) {
                java.util.Date dt = new java.util.Date();

                FormRowSet rowSet = new FormRowSet();
                FormRow row = new FormRow();
                row.setId(pk_id);

                Set set = hashMap.entrySet();
                Iterator iterator = set.iterator();
                while (iterator.hasNext()) {
                    Map.Entry mentry = (Map.Entry) iterator.next();

                    if(mentry.getKey() != null && mentry.getValue() != null && !mentry.getKey().equals("request") && !mentry.getKey().equals("response") ) {
                        row.put(mentry.getKey(), mentry.getValue());
                    }
                }
                
                if (!hashMap.containsKey("dateCreated")) {
                    row.setDateCreated(dt);
                }else{                 
                    try {
                        row.setDateCreated(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").parse(hashMap.get("dateCreated")));
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                }
                if (!hashMap.containsKey("dateModified")) {
                    row.setDateModified(dt);
                }else{
                    try {
                        row.setDateModified(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").parse(hashMap.get("dateModified")));
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                }

                rowSet.add(row);

                formDataDao.saveOrUpdate(form, rowSet);
            }
        }
    }
    
    public static void saveUpdateForm3(String formId, String p_id, FormRowSet rowSet) {

        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        Long appVersion = appService.getPublishedVersion(Constants.APP_ID.EMPM);
        AppDefinition appDef = appService.getAppDefinition(Constants.APP_ID.EMPM, appVersion.toString());
        FormDefinitionDao formDefinitionDao = (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");
        FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");

        FormDefinition formDef = formDefinitionDao.loadById(formId, appDef);

        Form form = null;
        if (formDef != null) {
            String formJson = formDef.getJson();

            if (formJson != null) {
                form = (Form) formService.createElementFromJson(formJson);
            }

            if (form != null) {
                try{
                    rowSet = appService.storeFormData(form, rowSet, UuidGenerator.getInstance().getUuid());
                }catch(Exception e){
                    LogUtil.info("Archive Plugin", "error 00001");
                }
                
            } else{
                LogUtil.info("Archive Plugin", "error 00002");
            }
        }else{
            LogUtil.info("Archive Plugin", "error 00003");
        }
    }

    public static HashMap insertObjectIntoHM(HashMap hm, String jsonData) throws JSONException {
        
        try {
            new org.json.JSONObject(jsonData);
            
            hm.put("Content", new JSONObject(jsonData));
        } catch (JSONException ex) {
            try {
                new JSONArray(jsonData);
                
                hm.put("Content", new JSONArray(jsonData));
            } catch (JSONException ex1) {
                hm.put("Content", jsonData);
            }
        }
        
        return hm;
        
    }
    
    public static String paramBuilder(HttpServletRequest httpReq, String[] list){
        List<String> expectedParams = Arrays.asList(list); 
        Enumeration<String> parameterNames = httpReq.getParameterNames();
        String paramBuilder = "";
        
        while (parameterNames.hasMoreElements()) {
 
            String paramName = parameterNames.nextElement();
            String[] paramValues = httpReq.getParameterValues(paramName);
            
            for (int i = 0; i < paramValues.length; i++) {
                String paramValue = paramValues[i];
                
//                pm("Parameter: "+paramName+", Values: "+paramValue );
                if(expectedParams.contains(paramName) && !paramBuilder.contains(paramName)){
                    paramBuilder += paramName+"="+paramValue+"&";
                }
            }
        }
        return paramBuilder;
    }

    public static String saveUpdateForm2(String appId, String formId, String pk_id, HashMap<String, String> hashMap) {

        if (pk_id == null || pk_id.isEmpty()) {
            pk_id = UuidGenerator.getInstance().getUuid();
        }
        
        if(appId.isEmpty()){
            appId = Constants.APP_ID.EMPM;
        }

        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        Long appVersion = appService.getPublishedVersion(appId);
        AppDefinition appDef = appService.getAppDefinition(appId, appVersion.toString());
        FormDefinitionDao formDefinitionDao = (FormDefinitionDao) AppUtil.getApplicationContext().getBean("formDefinitionDao");
        FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
        FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");

        FormDefinition formDef = formDefinitionDao.loadById(formId, appDef);

        Form form = null;
        if (formDef != null) {
            String formJson = formDef.getJson();

            if (formJson != null) {
                form = (Form) formService.createElementFromJson(formJson);
            }

            if (form != null && hashMap != null) {
                java.util.Date dt = new java.util.Date();

                FormRowSet rowSet = new FormRowSet();
                FormRow row = new FormRow();
                row.setId(pk_id);

                Set set = hashMap.entrySet();
                Iterator iterator = set.iterator();
                while (iterator.hasNext()) {
                    Map.Entry mentry = (Map.Entry) iterator.next();

                    if(mentry.getKey() != null && mentry.getValue() != null) {
                        row.put(mentry.getKey(), mentry.getValue());
                    }
                }

//                if (!hashMap.containsKey("dateCreated")) {
//                    row.setDateCreated(dt);
//                }
//                if (!hashMap.containsKey("dateModified")) {
//                    row.setDateModified(dt);
//                }

                if (!hashMap.containsKey("dateCreated")) {
                    row.setDateCreated(dt);
                }else{                 
                    try {
                        row.setDateCreated(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").parse(hashMap.get("dateCreated")));
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                }
                if (!hashMap.containsKey("dateModified")) {
                    row.setDateModified(dt);
                }else{
                    try {
                        row.setDateModified(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").parse(hashMap.get("dateModified")));
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                }

                rowSet.add(row);

                formDataDao.saveOrUpdate(form, rowSet);
            }
        }

        return pk_id;
    }
    
    public static String get_DT_CurrentDateTime(String format){
        
        Calendar calendar = Calendar.getInstance(); // Returns instance with current date and time set
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(calendar.getTime());
    }
    
    public static void sendEmail(String emails, String bcc, String subject, String content, String formDefId, Object[] files) {
        try{
            EmailTool et = new EmailTool();   
            
            AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
            AppDefinition appDef = appService.getPublishedAppDefinition(Constants.APP_ID.EMPM);
            PluginDefaultPropertiesDao dao = (PluginDefaultPropertiesDao) AppUtil.getApplicationContext().getBean("pluginDefaultPropertiesDao");
            PluginDefaultProperties pluginDefaultProperties = dao.loadById("org.joget.apps.app.lib.EmailTool", appDef);
            Map properties = PropertyUtil.getPropertiesValueFromJson(pluginDefaultProperties.getPluginProperties());
           
            properties.put("from", Constants.EMAIL_FROM);
            properties.put("toSpecific", emails);
            properties.put("cc", bcc);
            properties.put("bcc", "onmtesting2@gmail.com, onmtesting15@gmail.com");
            properties.put("subject", subject);
            properties.put("message", content);
            properties.put("isHtml", "true");
            
            if(files != null && files.length != 0) {
                properties.put("files", files);
            }

            et.execute(properties);
        }
        catch (Exception e) {
            LogUtil.info("Util", "Email failed to be sent." + e.getMessage());
        }
    }


}
