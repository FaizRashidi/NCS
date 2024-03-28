/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tms.hrdc.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.joget.commons.util.LogUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author faizr
 */
public class HttpUtil {
    
    private JSONObject respObj = new JSONObject();
    private String statusLine = "NO_DATA";
    private int statusCode = 500;
    private JSONObject reqBody = new JSONObject();
    private String reqParam = "";
    private String contentType = "";
    private String callType = "";
    private HashMap<String, String> header;
    
    public HttpUtil(){
        contentType = "application/json; charset=UTF-8";
    }   
    
    public void msg(String msg){
        LogUtil.info(this.getClass().toString(), msg);
    }
    
    public int getStatusCode(){
        return statusCode;
    }
    
    public JSONObject getJSONResponse(){
        return respObj;
    }
    
    public String getStatusLine(){
        return statusLine;
    }
    
    public JSONObject getJSONReqBody(){
        return reqBody;
    }
    
    public String getReqParam(){
        return reqParam;
    }
    
    public String getAPIMethod(){
        return callType;
    }
    
    public void setAPIMethod(String type){
        callType = type;
    }
    
    public void setContentType_X_WWW(){
        contentType = "application/x-www-form-urlencoded;";
    }
    
    public void setHeader(HashMap<String, String> header_){
        header = header_;
    }
    
    public void setBody(JSONObject jsonBody){
        reqBody = jsonBody;
    }
    
    public void printHeaders(HttpUriRequest http){
        Header[] hm = http.getAllHeaders();
        for (Header header : hm) {         
            msg( " HEADR REAL == > "+header.getName()+"<= =>"+ header.getValue());
        }
    }
    
    public void printBody(HttpEntity ent) throws ParseException, IOException{
        msg("JSON BODY REAL => "+EntityUtils.toString(ent));
    }
    
    public JSONObject sendPostRequest(String url) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, JSONException{
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", contentType);
        post.setHeader("Accept", "application/json");
        
        //Header
        if(header!=null && !header.isEmpty()){
            for (Map.Entry<String,String> entry : header.entrySet()) {
                post.setHeader(entry.getKey(), entry.getValue());
            }
        }          
        
        //Body
        if(reqBody != null){
            StringEntity params = new StringEntity(reqBody.toString());
            post.setEntity(params);
        }  
        
        sendHttp(post, url);
        
        return null;
    }
    
    public JSONObject sendGetRequest(String url) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, JSONException{
        HttpGet get = new HttpGet(url);        
        
        //Header
        if(header!=null && !header.isEmpty()){
            for (Map.Entry<String,String> entry : header.entrySet()) {
                get.setHeader(entry.getKey(), entry.getValue());
            }
        }          
        
        sendHttp(get, url);
        
        return null;
    }
    
    public JSONObject sendDeleteRequest(String url) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, JSONException{
        HttpDelete delete = new HttpDelete(url);
        
        //Header
        if(header!=null && !header.isEmpty()){
            for (Map.Entry<String,String> entry : header.entrySet()) {
                delete.setHeader(entry.getKey(), entry.getValue());
            }
        }          
        
        sendHttp(delete, url);
        
        return null;
    }
    
    public JSONObject sendPatchRequest(String url) throws IOException, NoSuchAlgorithmException, KeyManagementException, KeyStoreException, JSONException{
        HttpPatch patch = new HttpPatch(url);
                
        //Header
        if(header!=null && !header.isEmpty()){
            for (Map.Entry<String,String> entry : header.entrySet()) {
                patch.setHeader(entry.getKey(), entry.getValue());
            }
        }  
        //Body
        if(reqBody != null){
            StringEntity params = new StringEntity(reqBody.toString());
            patch.setEntity(params);
        }  
        
        sendHttp(patch, url);
        
        return null;
    }
    
    public void sendHttp(HttpUriRequest http, String url) throws IOException, JSONException{
        
        //param
        try{
            if(url.contains("?") && url.split("\\?").length>1){         

                String[] urlSplit = url.split("\\?");          
                String qParam = urlSplit[1];

                Map<String, String> paramMap = getQueryMap(qParam);  
                reqParam = paramMap.toString();
            }
        }catch(Exception e){
            e.printStackTrace();
        }  
        
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try{
            httpClient = HttpClients.custom()
                    .setSSLSocketFactory(new SSLConnectionSocketFactory(org.apache.http.ssl.SSLContexts.custom()
                            .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                            .build()
                    )
                    ).build();
        }catch(Exception e){
            msg( "NO HTTPS");
            httpClient = HttpClients.createDefault();
        }
       
        HttpResponse response = httpClient.execute(http);  
        HttpEntity entity = response.getEntity();
        statusLine = response.getStatusLine().toString();
        statusCode = response.getStatusLine().getStatusCode();
        
        msg("URL: "+url+
                "Status Code: "+Integer.toString(statusCode)+
                "HTTP Type: "+http.getMethod());
        
        if(entity!=null){
            String responseDataStr = EntityUtils.toString(entity, "UTF-8");
            
            try{
                respObj = new JSONObject(responseDataStr);
            }catch(JSONException e){
                try{
                    JSONArray arr = new JSONArray(responseDataStr);
                    respObj.put("data", arr);
                }catch(JSONException ee){
                    respObj = new JSONObject();
                    respObj.put("data_string", responseDataStr);
                } 
            }
        }else{
            respObj = new JSONObject();
        }
    }
    
    public static HttpsURLConnection sendRequestOAuth(String urlStr, String method, String contentType, String data) throws IOException{
        
        URL url = new URL(urlStr);
        HttpsURLConnection con = null;
        
        con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod(method);

        con.setRequestProperty("Content-Type", contentType);
        con.setRequestProperty("Accept", "*/*");
        con.setDoOutput(true);

        if(!StringUtils.isBlank(data)){
            OutputStream os = con.getOutputStream();
            byte[] input = data.getBytes("utf-8");
            os.write(input, 0, input.length);

            os.flush();
            os.close();
        }
//        con.connect(); 
        
//        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
//        wr.write(data);
//        wr.flush();
        
        return con;
    }
    
    public static HttpURLConnection sendRequest(URL url, String method, String cookies, String jsonBody) throws IOException{
        
        HttpURLConnection con = null;
//            url = new URL("http://"+Glossary.GRP_IP+"/"+Glossary.GRP_INSTANCE+"/entity/auth/login");
        con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(method);

        if(!StringUtils.isBlank(cookies)){
            con.setRequestProperty("Cookie", cookies);
        }

        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);

        if(!StringUtils.isBlank(jsonBody)){
            OutputStream os = con.getOutputStream();
            byte[] input = jsonBody.getBytes("utf-8");
            os.write(input, 0, input.length);

            os.flush();
            os.close();
        }
            
        
        return con;
    }
    
    private Map<String, String> getQueryMap(String url){
        String[] params = url.split("&");  
        Map<String, String> map = new HashMap<String, String>();

        for (String param : params) {  
            String name = param.split("=")[0];  
            String value = param.split("=")[1];  
            map.put(name, value);  
        }  
        return map;    
    }
    
    public static JSONObject getRequestBody(HttpServletRequest request) throws IOException, org.json.simple.parser.ParseException, JSONException {
        
        String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

        JSONParser parser = new JSONParser();
        org.json.simple.JSONObject payloadOld = (org.json.simple.JSONObject)parser.parse(body); //JSONValue
        JSONObject payload = (JSONObject) new JSONObject(payloadOld.toString());

        return payload;        
    }
    
}

