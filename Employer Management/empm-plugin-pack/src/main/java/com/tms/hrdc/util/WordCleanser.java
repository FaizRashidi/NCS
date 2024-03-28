/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.tms.hrdc.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joget.commons.util.LogUtil;

/**
 *
 * @author faizr
 */
public class WordCleanser {
    Map<String, String> hm = new LinkedHashMap<>();
    
    public WordCleanser(){
        setMap();
    }
    
    public void setMap(){
        
        hm = new LinkedHashMap<>();
        hm.put("[^a-zA-Z0-9\\s\\.\\&\\(\\)\\'\\-]", "");                             //replace any special characters
        hm.put("\\b\\-(?<!\\s)", " -");      
        hm.put("(?!\\s)\\-\\b", "- ");      
        hm.put("\\s{2,}", " ");                                         //replace double whitespace white single
        hm.put("(?<!\\s)&|&(?!\\s)", " & ");                            //find & w/o whitespace b4/after
        hm.put("\\.{2,}", ".");                                         //find 2 or more . and replace with single        
        hm.put("\\&{2,}", "&");                                         //find 2 or more . and replace with single        
        hm.put("\\(\\s*\\({1,}", "(");                                         //find 2 or more ( and replace with single        
        hm.put("\\)\\s*\\){1,}", ")");                                         //find 2 or more ) and replace with single        
        hm.put("\\'{2,}", "'");                                         //find 2 or more . and replace with single        
        hm.put("\\-{2,}", "-");                                         //find 2 or more . and replace with single          
        hm.put("(?<!\\s)\\.(?!\\s)", ". ");                             //find if no w/space behind a dot and add one
        hm.put("\\((?!\\S)\\s", "(");                                      //w/space after (
        hm.put("\\s(?<!\\S)\\)", ")");                                     //w/space b4 )   
        hm.put("(?<!\\s)\\(", " (");                                    // add w/space if b4 ( is not empty
        hm.put("(?<!\\s)\\)(?!\\s)", ") ");                             // add w/space after ) if after is not empty
        hm.put("\\s\\.(?<!\\s)", ".");                                  // remove w/space b4 closing .              
        hm.put("(?i)CHEM[\\s.]", "CHEMICAL ");        
        hm.put("(?i)SYKT[\\s.]", "SYARIKAT ");        
        hm.put("(?i)ENT[\\s.]", "ENTERPRISE ");        
        hm.put("(?i)MKTG[\\s.]", "MARKETING ");        
        hm.put("(?i)MFG[\\s.]", "MANUFACTURING ");        
        hm.put("(?i)IND[\\s.]", "INDUSTRY ");        
        hm.put("(?i)ENG[\\s.]", "ENGINEERING ");        
        hm.put("(?i)ASSOC[\\s.]|ASSOCS[\\s.]|ASS[\\s.]", "ASSOCIATION ");        
        hm.put("(?i)ADVOC[\\s.]", "ADVOCATE ");        
        hm.put("(?i)ARCHIT[\\s.]", "ARCHITECT ");        
        hm.put("(?i)CMPY[\\s.]", "COMPANY ");        
        hm.put("(?i)ADVOC[\\s.]", "ADVOCATE ");        
//        hm.put("(?i)(?:\\((M|M\\.|MY|MY\\.|MSIA|M'SIA)\\)|\\b(M|M\\.|MY|MY\\.|MSIA|M'SIA)\\b)", "(MALAYSIA)");                  
        hm.put("\\bM(?:\\'?)S(?:IA)?\\b", "MALAYSIA");                  
        hm.put("\\((M|MY)\\)", "(MALAYSIA)");                  
        hm.put("(?i)PLT[\\s.]", "PLT ");        
        hm.put("(?i)LTD[\\s.]", "LTD ");            
        hm.put("(?i)SEK[\\s.]", "SEKOLAH ");            
        hm.put("(?i)KEB[\\s.]", "KEBANGSAAN ");            
        hm.put("(?i)INC[\\s.]", "INCORPORATED ");            
        hm.put("(?i)PROD[\\s.]", "PRODUCTION ");            
        hm.put("(?i)CHAMB[\\s.]", "CHAMBER ");            
        hm.put("(?i)ELEC[\\s.]|ELECT[\\s.]", "ELECTRICAL ");            
        hm.put("(?i)TRNSPT[\\s.]", "TRANSPORT ");        
        hm.put("(?i)\\b(?:SDN|SENDIRIAN)[^\\w]*(?:BHD|BERHAD|HD)", "SDN. BHD.");  
    }
    
    String currWord = "";
    ArrayList patternHitList;
    
    public void setWord(String word){
        patternHitList=new ArrayList();
    }
    
    public String cleanCompName(String word) throws StackOverflowError{
        boolean hit = false;
        
        Set<String> keySet = hm.keySet();

        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            
            String key = iterator.next();
            String val = hm.get(key);            
            
            Matcher m = Pattern.compile(key).matcher(word);
            Matcher mBase = Pattern.compile("SDN\\.[^\\w]*BHD\\.").matcher(word);
            
            if(m.find() && !mBase.find()){                
                word = word.replaceAll(key, val);
                LogUtil.info("WC", "match "+m.group()+" "+word);
                
                if(!patternHitList.contains(key)){
                    patternHitList.add(key);
                    hit = true;
                    LogUtil.info("WC", "key "+key+" - hit ");
                }              
                break;
            }
        }
        
        if(hit){       
            return cleanCompName(word);
        }else{
            return word.toUpperCase();
        }
    }
    
    public String cleanMycoid(String mycoid){
        return mycoid.replaceAll("[^a-zA-Z0-9 ]", "").toUpperCase();
    }
}
