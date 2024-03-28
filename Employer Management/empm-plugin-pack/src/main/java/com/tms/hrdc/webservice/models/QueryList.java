/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.webservice.models;

import com.tms.hrdc.util.DBHandler;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import org.joget.apps.app.service.AppUtil;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author faizr
 */
public class QueryList {
    
    HttpServletRequest request;
    DBHandler db;
    
    public QueryList(DBHandler d, HttpServletRequest req) throws SQLException {
        db = d;
        request = req;        
    }
    
    public JSONObject getQueryData() throws JSONException{
        
        String userId = request.getParameter("userId");
        ArrayList<HashMap<String, String>> queryHm = getQueries(userId);
        String htmlList = buildHtmlList(queryHm);
        int qCount = queryHm.size();
        
        JSONObject returnData = new JSONObject();
        returnData.put("query_count", qCount);
        returnData.put("html", htmlList);
        
        return returnData;
    }

    private ArrayList<HashMap<String, String>> getQueries(String userId) {
        
        String query = "SELECT q.* FROM app_fd_empm_qry_proc q\n" +
                        "INNER JOIN app_fd_empm_reg e ON e.id = q.c_fk\n" +
                        "INNER JOIN app_fd_empm_usermap u ON u.c_compId = e.id\n" +
                        "WHERE u.c_userId = ?";
//        tring query = "SELECT * FROM app_fd_empm_qry_proc s WHERE '"+userId+"' "
//                + "like CONCAT('%',c_req_mycoid)";
        return db.select(query, new String[]{userId});        
    }
    
    private String buildHtmlList(ArrayList<HashMap<String, String>> queryHm) {
        
        String html = "";
        String url = "/jw/web/userview/empm/emp/_/approval_query_crud?_mode=edit&";
        
        int count = 10; 
        
        for(HashMap hm:queryHm){
            
            if(count==0){
                break;
            }
            
            html += "<li class=\"task\"> " +
                    "   <a href=\""+url+"id="+hm.get("id")+"\" > " +
                    "   <span class=\"header\">"+hm.get("c_query_subject")+"</span> " +
                    "   <span class=\"message\">"+hm.get("c_query_remark")+"</span> " +
                    "   <span class=\"time\">"+hm.get("dateCreated")+"</span> " +
                    "   </a> " +
                    "</li>";
            
            count--;
        }
        
        html = html.replace("\\", "");
        
        return html;
    }
    
}
