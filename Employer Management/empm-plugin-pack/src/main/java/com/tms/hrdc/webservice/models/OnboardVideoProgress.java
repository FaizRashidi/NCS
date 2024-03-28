/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.webservice.models;

import com.tms.hrdc.util.CommonUtils;
import com.tms.hrdc.util.Constants;
import com.tms.hrdc.util.DBHandler;
import com.tms.hrdc.util.HttpUtil;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author kahyi
 */
public class OnboardVideoProgress {
    
    HttpServletRequest request;
    DBHandler db;
    HttpUtil http;
    
    public OnboardVideoProgress(DBHandler d, HttpServletRequest req) throws SQLException {
        db = d;
        request = req;      
    }
    public JSONObject updateVideoProgress() throws JSONException, IOException, ParseException{
        JSONObject json = new JSONObject();

        JSONObject reqBody = HttpUtil.getRequestBody(request);
        String videoId = reqBody.optString("videoId", "0");
        String video_status = reqBody.optString("video_status", "undefined");
        String username = reqBody.optString("username", "");
        
        if (video_status.equals("undefined")){
            json.put("Status", "NOTHING TO UPDATE!");
            return json;

        }
        else{

            HashMap userMap_hm = db.selectOneRecord(
                "select c_compId from app_fd_empm_usermap where c_userId = ?",
                new String[]{username});
            String compId = "";
            if(userMap_hm!=null){
                compId = userMap_hm.get("c_compId")==null?"":userMap_hm.get("c_compId").toString();
            }else{
                json.put("Status", "NO SUCH USERID!");
                return json;

            }
            
            HashMap onboard_hm = db.selectOneRecord(
                "select id, c_is_view_completed from app_fd_empm_onboardStatus where c_empId = ?  and c_fk_videoId = ?",
                new String[]{compId, videoId});
            String onboardId = "";
            String view_completed = "";
            if(onboard_hm!=null){
                onboardId = onboard_hm.get("id")==null?"":onboard_hm.get("id").toString();
                view_completed = onboard_hm.get("c_is_view_completed")==null?"":onboard_hm.get("c_is_view_completed").toString();
            }
            
            if (video_status.equals("video ended")){
                if (view_completed.equals("true")){
                    json.put("Status", "ALREADY WATCHED, NO NEED TO UPDATE AGAIN");
                    return json;
                }
                
                HashMap hm = new HashMap();

                hm.put("is_view_completed", "true");
                
                if (onboardId == null){
                    json.put("Status", "VIDEO ENDED BUT CAN'T FIND ONBOARD ID!");
                    return json;
                }
                CommonUtils.saveUpdateForm(Constants.APP_ID.EMPM, 
                                                    Constants.FORM_ID.EMP_ONBOARD, 
                                                    onboardId, hm);
                json.put("Status", "SUCCESS");
            }
            else if (video_status.equals("video started")) {
                if (onboardId.equals("")){
                    HashMap hm = new HashMap();
                    hm.put("fk_videoId", videoId);
                    hm.put("is_viewed", "true");
                    hm.put("is_view_completed", "false");
                    hm.put("empId", compId);

                    CommonUtils.saveUpdateForm(Constants.APP_ID.EMPM, 
                                                        Constants.FORM_ID.EMP_ONBOARD, 
                                                        "", hm);
                    json.put("Status", "NEW VIDEO STARTED");
                }
                else{
                    json.put("Status", "ALREADY STARTED");
                }

            }
            else{
                json.put("Status", "NOTHING TO UPDATE!");
            }
                
        }

        return json;
    }
    
    
}
