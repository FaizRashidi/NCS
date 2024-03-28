/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.webservice;

import com.tms.hrdc.binder.ArchiveBinder;
import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.HttpUtil;
import com.tms.hrdc.util.LinkFactory;
import com.tms.hrdc.webservice.models.AssignmentsImpl;
import com.tms.hrdc.webservice.models.BranchData;
import com.tms.hrdc.webservice.models.DeregButtons;
import com.tms.hrdc.webservice.models.EmailUtil;
import com.tms.hrdc.webservice.models.EmpData;
import com.tms.hrdc.webservice.models.EmpSetup;
import com.tms.hrdc.webservice.models.OnboardVideoProgress;
import com.tms.hrdc.webservice.models.PEData;
import com.tms.hrdc.webservice.models.QueryList;
import com.tms.hrdc.webservice.models.ReregButtons;
import com.tms.hrdc.webservice.models.TemplateConfig;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.UuidGenerator;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.PluginWebSupport;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author faizr
 */
public class EmpmAPI extends DefaultApplicationPlugin implements PluginWebSupport{

    @Override
    public Object execute(Map props) {
        return null;
    }

    @Override
    public String getName() {
        return this.getClass().toString();
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "API for Employer Management Module";
    }

    @Override
    public String getLabel() {
        return "HRDC - EMPM - API";
    }

    @Override
    public String getClassName() {
        return this.getClass().toString();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        DBHandler db = new DBHandler();
        try {  
            
            JSONObject payload = new JSONObject();
            
            boolean isAuthed = false;
            boolean specialReqs = false;
            boolean isNotPost = false;
        
            String type = request.getParameter("method");
            isAuthed = authenticateReq(request);
            
            if(StringUtils.isBlank(type)){
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please specify API method");;
                return;
            }
            
            db.openConnection((DataSource) AppUtil.getApplicationContext().getBean("setupDataSource"));
            switch(type){ 
                case "getDeregBypassState": //Used after empreg submission
                    try{
                        payload = new DeregButtons(db, request).checkDeregPending();
                    }catch(Exception e){
                        payload.put("Error", e.getMessage());
                    }
                    specialReqs = true;
                    break;
                case "getDeregButton": //Used after empreg submission
                    payload = new DeregButtons(db, request).setWDButton();
                    specialReqs = true;
                    break;
                case "getCancelButton": //Used after empreg submission
                    payload = new DeregButtons(db, request).setCancelButton();
                    specialReqs = true;
                    break;
                case "getRegRejectedButton": //Used after empreg submission
                    payload = new ReregButtons(db, request).setReregButton();
                    specialReqs = true;
                    break;
                case "createPEMailList": //Used after empreg submission
                    payload = new PEData(db, request).buildContent();
                    specialReqs = true;
                    break;
                case "getSubmitReport": //Used after empreg submission
                    try{
                        payload = new AssignmentsImpl(db, request).getAssignedOfficerDetailed();
                    }catch(Exception e){
                        e.printStackTrace();
                        payload.put("Error", e.getMessage());
                    }
                    specialReqs = true;
                    break;
                case "getProcessingOfficer": //Used after empreg submission
                    try{
                        payload = new AssignmentsImpl(db, request).getAssignedOfficerSimplified();
                    }catch(Exception e){
                        e.printStackTrace();
                        payload.put("Error", e.getMessage());
                    }
                    specialReqs = true;
                    break;
                case "getAssignees": // used in datalist to show officer in task
                    payload = new AssignmentsImpl(db, request).getAssignees();
                    specialReqs = true;
                    break;
                case "getRecvdFromAssignees": // used in datalist to show previous assigned pic
                    payload = new AssignmentsImpl(db, request).getPrevAssignees();
                    specialReqs = true;
                    break;
                case "getRecvdFromAssigneesInActivity": //used in forwarded task, to show previous assignee
                    payload = new AssignmentsImpl(db, request).getCurrentTaskReturnAsgnee();
                    specialReqs = true;
                    break;
                case "getcurrentempl":     
//                    payload = new EmpData(db,request).getEmplDataForReg();
                    
                    try{
//                        payload = new DeregButtons(db, request).checkDeregPending();
                        payload = new EmpData(db,request).getEmplDataForReg();
                    }catch(Exception e){
                        payload.put("Error", e.getMessage());
                    }
                    break;                    
                case "getquerylist":     
                    payload = new QueryList(db,request).getQueryData();
                    break;
                    
                case "getCurrentUserPermission":
                    payload = new EmpData(db,request).getIfUserPermit();
                    break;
                    
                case "getEmpData":    
                    try{
                        payload = new EmpData(db,request).getEmplData();
                    }catch(Exception e){
                        payload.put("Error", e.getMessage());
                    }
                    break;
                
                case "getEmpDeregData":    
                    try{
                        payload = new EmpData(db,request).getEmplDeregData();
                    }catch(Exception e){
                        payload.put("Error", e.getMessage());
                    }
                    break;
                    
                case "searchCompPnS":                         
                    payload = new EmpData(db,request).getPnSResult();
                    break;
                                       
                case "getValidCompanies":
                    payload = new EmpData(db,request).getValidCompanies();
                    break;
                    
                case "insertCompSubs":                         
                    payload = new EmpData(db,request).insertCpToSub();
                    break;
                    
                case "checkEmailExist":                         
                    payload = new EmpData(db,request).checkExistingEmail();
                    break;
                    
                case "getEmailTemplate":
                    payload = new TemplateConfig(db,request).getEmplEmailTemplate();
                    specialReqs = true;
                    break;
                    
                    //unused, proper is in DeregButtons class
                case "getDeregBypassCheck":
                    payload = new EmpSetup(db,request).getEmplDeregBypassSetup();
                    specialReqs = true;
                    break;
                    
                case "getDeregDummy":
                    payload = new EmpSetup(db,request).getDeregDummy();
                    specialReqs = true;
                    break;
                    
                case "getEMPMLinks":
                    payload = new LinkFactory().getKeywords(db,request);
                    specialReqs = true;
                    break;
                    
                case "getFieldMap":
                    payload = new TemplateConfig(db,request).getKeywords();
                    specialReqs = true;
                    break;
                    
                case "getCountryCode":     
                    payload = new TemplateConfig(db,request).getCountryCode();
                    specialReqs = true;
                    break;
                
                case "hashMe":     
                    payload = doHash(request);
                    specialReqs = true;
                    break;
                    
                case "trackOpenQR":     
                    payload = new EmailUtil(db,request, response).trackOpenQR();
                    specialReqs = true;
                break;
                
                case "trackOpen":     
                    payload = new EmailUtil(db,request, response).trackOpen();
                    specialReqs = true;
                break;      
                
                case "insertLog": //Audit trail insertion for other modules to use
                    if(request.getMethod().equals("POST")){
                        payload = new EmpData(db,request).insertAuditTrail();
                    }else{
                        isNotPost = true;
                    }
                    
                    break;
                
                case "updateEmp":
                    if(request.getMethod().equals("POST")){
                        payload = new EmpData(db,request).updateEmpData2();
                    }else{
                        isNotPost = true;
                    }
                    
                    break;
                case "updateBranch":
                    if(request.getMethod().equals("POST")){
                        payload = new BranchData(db,request).updateBranchDetails();
                    }else{
                        isNotPost = true;
                    }
                    
                    break;    
                case "deregEmp":
                    if(request.getMethod().equals("POST")){
                        payload = new EmpData(db,request).updateEmpData2();
                    }else{
                        isNotPost = true;
                    }
                    
                    break;    
                case "updateVideoStatus":
                    if(request.getMethod().equals("POST")){
                        payload = new OnboardVideoProgress(db,request).updateVideoProgress();
                    }else{
                        isNotPost = true;
                    }
                    break;  
                default:
                    db.closeConnection();
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                                        Constants.API.STATUS.ERROR_SPECIFYAPIMETHOD);
                return;
            }
            
            if(db.getConnection()!=null){
                db.closeConnection();
            }
            
            if(isNotPost ){
                db.closeConnection();
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
                                            Constants.API.STATUS.ERROR_USEPOST);
                return;
            }
            
            if(!isAuthed && !specialReqs ){
                response.sendError(HttpServletResponse.SC_FORBIDDEN, 
                                    Constants.API.STATUS.ERROR_CREDENTIALS);;
                return;
            }
            
            if(payload!=null){

                JSONObject outer = new JSONObject();
                JSONArray in = new JSONArray();
                in.put(payload);
                
                outer.put("empmApiId", UuidGenerator.getInstance().getUuid());
                outer.put("dateTime", CommonUtils.get_DT_CurrentDateTime("YYYY-MM-dd hh:mm:ss"));
                                
                if(payload.has("dataArray")){
                    in = payload.getJSONArray("dataArray");
                }
                
                outer.put("size", in.length());
                outer.put("data", in);
                
                if(payload.has("special")){
                    in = payload.getJSONArray("special");
                    in.write(response.getWriter());
                    return;
                }
                
                if((type.equals("trackOpenQR") || type.equals("trackOpen"))
                        && payload.get("data").equals("FAILED")){
                    response.getWriter().write(payload.get("data").toString());
                    return;
                }else if((type.equals("trackOpenQR") || type.equals("trackOpen"))
                        && !payload.get("data").equals("FAILED")){
                    return;
                }

                outer.write(response.getWriter());
            }
            
        } catch (JSONException ex) {
            ex.printStackTrace();
//            response.sendError(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
        } catch (SQLException ex) {
            ex.printStackTrace();
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
        } catch (ParseException ex) {
            ex.printStackTrace();
            response.sendError(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
        } finally {
            db.closeConnection();
            return;
        }
    }

    private boolean authenticateReq(HttpServletRequest request) {
                
        String call_dateTime = request.getHeader("dateTime")== null?"":request.getHeader("dateTime") ;
        String call_key = request.getHeader("apiKey")== null?"":request.getHeader("apiKey");            
        String call_hash = request.getHeader("hash")== null?"":request.getHeader("hash");
        
//        LogUtil.info("string is", call_hash+ "|" + call_dateTime + "|" + call_key);
        if(StringUtils.isBlank(call_dateTime) || StringUtils.isBlank(call_key) || StringUtils.isBlank(call_hash)){
            return false;
        }
        
        java.util.Date dt = new java.util.Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateTime = dateFormat.format(dt);
        dateTime = dateTime.replace("-", "");
        dateTime = dateTime.replace(" ", "");
        dateTime = dateTime.replace(":", "");

        String hash = Constants.API_SECRET + "|" + call_dateTime + "|" + call_key;
        String hashCheck = DigestUtils.sha256Hex(hash);
        
//        LogUtil.info("string is", hash);
//        LogUtil.info("hashed string is", hash);

        if(call_hash.equals(hashCheck)){
            return true;
        }else{
            return false;
        }
        
    }

    private JSONObject doHash(HttpServletRequest request) throws JSONException {
        String text = request.getHeader("message")==null?"":request.getHeader("message");
        String hashed = DigestUtils.sha256Hex(text);        
        
        return new JSONObject().put("msg", hashed);
    }
    
}
