/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.dao;

import com.tms.hrdc.util.CommonUtils;

/**
 *
 * @author faizr
 */
public class APIManager {
    
    public static class APIType{
        public static final String LEVY = "levy";
        public static final String APS = "aps";
        public static final String GRANT = "grant";
    }

    
    String api_id;
    String api_key;
    String env;
    String api_type;
    
    /**
     * 
     * @param apiType : Type of API, Should use APIManager.APIType Obj
     */
    public APIManager(String apiType){
        api_type = apiType;
        init();
    }
    
    public void init(){
        env = "";
        
        if(CommonUtils.getBaseURL().contains("sit")){
           env = "SIT";
        }
        if(CommonUtils.getBaseURL().contains("dev")){
           env = "DEV";
        }
        if(CommonUtils.getBaseURL().contains("uat")){
           env = "UAT";
        }
        if(CommonUtils.getBaseURL().contains("pilot")){
           env = "PILOT";
        }
        
        switch(api_type){
            case APIType.APS:
                switch(env){
                    case "DEV":
                        api_id = "API-f024d4ba-c466-4f67-b2cf-47c1a56a11f1";
                        api_key = "e5df0f680de14a74b30f9cfc204d7fcb";
                    break;
                    case "SIT":
                        api_id = "API-f024d4ba-c466-4f67-b2cf-47c1a56a11f1";
                        api_key = "fb8c4d9a88c2457098c951fe7d9a3540";
                    break;
                    case "UAT":
                        api_id = "API-f024d4ba-c466-4f67-b2cf-47c1a56a11f1";
                        api_key = "1f7d615fcccb4262ac2b6e4d85255f89";
                    break;
                    case "PILOT":
                        api_id = "API-f024d4ba-c466-4f67-b2cf-47c1a56a11f1";
                        api_key = "9b2fc1a4ec6e43009daa4a96f43931a3";
                    break;
                }
            break;
            case APIType.LEVY:
                switch(env){
                    case "DEV":
                        api_id = "API-949bcad4-ba9e-4bda-9ebb-88cb3df29ccb";
                        api_key = "767c027d74774e198818ff432b642a82";
                    break;
                    case "SIT":
                        api_id = "";
                        api_key = "";
                    break;
                    case "UAT":
                        api_id = "";
                        api_key = "";
                    break;
                    case "PROD":
                        api_id = "";
                        api_key = "";
                    break;
                }
            break;
            case APIType.GRANT:
                switch(env){
                    case "DEV":
                        api_id = "";
                        api_key = "";
                    break;
                    case "SIT":
                        api_id = "API-9474a97f-65a1-473c-b294-795421ec9e64";
                        api_key = "5bcb5921ff48464abff295d56bbf1a3c";
                    break;
                    case "UAT":
                        api_id = "";
                        api_key = "";
                    break;
                    case "PROD":
                        api_id = "";
                        api_key = "";
                    break;
                }
            break;
            default:                
                api_id = "";
                api_key = "";                
        }
    }
    
    public String getApi_id() {
        return api_id;
    }

    public String getApi_key() {
        return api_key;
    }
}
