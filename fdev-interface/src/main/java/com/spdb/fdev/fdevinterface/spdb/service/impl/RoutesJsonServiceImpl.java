package com.spdb.fdev.fdevinterface.spdb.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.alibaba.fastjson.JSON;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jcraft.jsch.ChannelSftp;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.common.util.Util;
import com.spdb.fdev.fdevinterface.base.dict.Constants;
import com.spdb.fdev.fdevinterface.base.dict.Dict;
import com.spdb.fdev.fdevinterface.base.dict.ErrorConstants;
import com.spdb.fdev.fdevinterface.base.utils.CommonUtil;
import com.spdb.fdev.fdevinterface.base.utils.FileUtil;
import com.spdb.fdev.fdevinterface.base.utils.MD5Util;
import com.spdb.fdev.fdevinterface.base.utils.SFTPClient;
import com.spdb.fdev.fdevinterface.base.utils.SSHComandUtil;
import com.spdb.fdev.fdevinterface.base.utils.TimeUtils;
import com.spdb.fdev.fdevinterface.spdb.dao.util.HttpRequestUtils;
import com.spdb.fdev.fdevinterface.spdb.entity.AppJson;
import com.spdb.fdev.fdevinterface.spdb.entity.TotalJson;
import com.spdb.fdev.fdevinterface.spdb.service.AppJsonService;
import com.spdb.fdev.fdevinterface.spdb.service.CentralJsonService;
import com.spdb.fdev.fdevinterface.spdb.service.RestTransportService;
import com.spdb.fdev.fdevinterface.spdb.service.RoutesJsonService;
import com.spdb.fdev.fdevinterface.spdb.service.RoutesService;
import com.spdb.fdev.fdevinterface.spdb.service.TotalJsonService;
import com.spdb.oss.OSSException;
import com.spdb.oss.OSSService;

import net.sf.json.JSONObject;

/**
 * @author csii_hexy
 * @date 2020/7/27 16:42
 */
@Service
@RefreshScope
public class RoutesJsonServiceImpl implements RoutesJsonService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private ChannelSftp channelSftp;

    @Value("${scripts.path}")
    private String scriptsPath;
    @Value("${central.alg}")
    private String centralAlg;
    @Value("${central.key}")
    private String centralKey;
    @Value("${baseUrl.sit}")
    private String baseUrlSit;
    @Value("${baseUrl.uat}")
    private String baseUrlUat;
    @Value("${baseUrl.rel}")
    private String baseUrlRel;
    @Value("${baseUrl.gray}")
    private String baseUrlGray;
    @Value("${baseUrl.pro}")
    private String baseUrlPro;
    @Value("${finterface.nas}")
    private String finterfaceNas;
    @Value("${fresource.sit.nas}")
    private String fresourceSitNas;
    @Value("${fresource.uat.nas}")
    private String fresourceUatNas;
    @Value("${fresource.rel.nas}")
    private String fresourceRelNas;
    @Value("${fdev.interface.ip}")
    private String fdevInterfaceIp;
    @Value("${scp.user}")
    private String user;
    @Value("${scp.password}")
    private String password;
    @Value("${scp.sit.host}")
    private String scpSitHost;
    @Value("${scp.uat.host}")
    private String scpUatHost;
    @Value("${scp.rel.host}")
    private String scpRelHost;
    @Autowired
    private CentralJsonService centralJsonService;
    @Autowired
    private RestTransportService restTransportService;
    @Autowired
    private RoutesService routesService;
//    @Autowired
//    private AppDatService appDatService;
    @Autowired
    private AppJsonService appJsonService;
    @Autowired
    private TotalJsonService totalJsonService;
    
	@Resource
	private OSSService ossService;
    
    @Value("${nacos.sit.url}")
    private String nacosSitUrl;
    @Value("${nacos.uat.url}")
    private String nacosUatUrl;
    @Value("${nacos.rel.url}")
    private String nacosRelUrl;
    @Value("${nacos.qy.url}")
    private String nacosQyUrl;
    @Value("${nacos.cp.url}")
    private String nacosCpUrl;
    
    
    @Value("${nacos.nacosUserName}")
    private String nacosUserName;
    @Value("${nacos.nacosPassword}")
    private String nacosPassword;
    @Value("${nacos.nacosGroup}")
    private String nacosGroup;
    
    @Value("${spdb.oss.bucketName}")
    private String bucketName;
    
    
    
    @Override
    public String buildTestDatTar(Map<String, Object> requestMap) {
        Integer gitlabProjectId = (Integer) requestMap.get(Dict.GITLAB_PROJECT_ID);
        Set<Integer> gitlabIds = new HashSet<>();
        gitlabIds.add(gitlabProjectId);
        String branch = (String) requestMap.get(Dict.BRANCH);
        List<Map<String, Object>> appInfoList = restTransportService.getAppByIdsOrGitlabIds(Dict.GITLAB_PROJECT_ID, new HashSet<>(), gitlabIds);
        if (CollectionUtils.isEmpty(appInfoList)) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????????????????GitLab ID???" + gitlabProjectId});
        }
        Map<String, Object> appInfoMap = appInfoList.get(0);
        String appNameEn = (String) appInfoMap.get(Dict.NAME_EN);
        String appNameZh = (String) appInfoMap.get(Dict.NAME_ZH);
        // ??????project.json?????????routes?????????md5???????????????????????????????????????????????????????????????
        Map<String, Object> projectJsonMap = (Map<String, Object>) requestMap.get(Dict.PROJECT_JSON);
        if (MapUtils.isEmpty(projectJsonMap)) {
            // ???project.json?????????????????????????????????????????????,ver???-9??????????????????
            routesService.deleteRoutesApi(appNameEn, branch, -9);
            return "project.json??????????????????!";
        }
        List<Map<String, Object>> routesList = (List<Map<String, Object>>) projectJsonMap.get(Dict.ROUTES);
        if (CollectionUtils.isEmpty(routesList)) {
            // ???routesList???????????????????????????????????????
            routesService.deleteRoutesApi(appNameEn, branch, -9);
            return "project.json????????????routes????????????!";
        }
        String projectJson = JSON.toJSONString(projectJsonMap);
        Boolean tagFlag = (Boolean) requestMap.get(Dict.TAG_FLAG);
        String env = getEnvByBranch(branch, tagFlag);
        // ???????????????MD5
        String newMD5Str = MD5Util.encoder("", projectJson.replace(" ", ""));
        JsonObject jsonObject = new JsonParser().parse(projectJson).getAsJsonObject();
        // ????????????????????????????????????
        String lastBranch = this.getLastBranch(appNameEn, gitlabProjectId, branch);
        Map<String, Integer> md5FlagAndVer = routesService.getMD5AndVer(appNameEn, gitlabProjectId, lastBranch, newMD5Str, jsonObject);
        // MD5???????????????????????????
        Integer md5Flag = md5FlagAndVer.get(Dict.FLAG);
        if (md5Flag == 0) {
            return "?????????????????????!";
        }
        
        String cicdTime = TimeUtils.getFormat(TimeUtils.FORMAT_DATE_TIME);
        String date = TimeUtils.getFormat(TimeUtils.FORMAT_DATE);
        String module = finterfaceNas + appNameEn + Constants.UNDER_LINE + date;
        String local_module = scriptsPath + appNameEn + Constants.UNDER_LINE + date+Constants.UNDER_LINE+env.toLowerCase()+Constants.SLASH;

        Map<String, Object> repoJson = createRepoJson( module, local_module ,routesList);
        String repoJsonMD5 = (String) repoJson.get(Dict.REPO_JSON_MD5);

        //??????????????????
        String repoJsonPath = (String) repoJson.get(Dict.PATH);
        
        // ????????????????????????????????????
        AppJson appJson = new AppJson();
        appJson.setProjectName(appNameEn);
        appJson.setNameZh(appNameZh);
        appJson.setBranch(branch);
        appJson.setRouteNum(routesList.size());
        appJson.setProjectJson(projectJsonMap);
        appJson.setRepoJson((Map<String, Object>) repoJson.get(Dict.REPO_JSON));
        appJson.setRepoJsonMD5(repoJsonMD5);
        appJson.setRoutesVersion(md5FlagAndVer.get(Dict.VER));
        appJson.setCicdTime(cicdTime);
        String repoTar = appNameEn + "_" + date + "_repo" + Constants.JSON_FILE;
        appJson.setRepoTarName(repoTar);
        appJson.setRepoTarUrl(fdevInterfaceIp + Constants.DOWNLOAD_URL + repoTar);
        appJson.setIsNew(1);
        appJson.setRepoJsonPath(repoJsonPath);
        appJsonService.save(appJson);

        Map<String, Object> envMap = new HashMap<>();
        envMap.put(Dict.ENV, env);
        //?????????????????????????????????
        List<TotalJson> oldTotalJsons = totalJsonService.getTotalJsonListByEnv(envMap);
        //????????????????????????
        Map<String, String> appJsonUrlMap = new HashMap<>();
        //?????????????????????
        Map<String, Object> centralJson = new HashMap<>();
        //????????????
        Map<String, String> appJsonZhMap = new HashMap<>();
        //???????????????????????????????????????????????????????????? ????????????????????????????????????????????????????????????????????????????????????????????????
        if (!oldTotalJsons.isEmpty()) {
            TotalJson oldTotalJson = oldTotalJsons.get(0);
            appJsonUrlMap = oldTotalJson.getAppJsonUrlMap();
            centralJson = oldTotalJson.getCentralJson();
            if (Util.isNullOrEmpty(centralJson)){
                centralJson = new HashMap<>();
                centralJson.put(Dict.REPOS , new HashMap<>());
            }
        }else {
            centralJson.put(Dict.REPOS , new HashMap<>());
        }
        Map<String , Object> reposMap = (Map<String, Object>) centralJson.get(Dict.REPOS);
        appJsonUrlMap.put(appNameEn, repoJsonPath);
        appJsonZhMap.put(Dict.NAME,appNameZh);
        reposMap.put(appNameEn,appJsonZhMap);
        //???????????????????????????????????????????????????????????????
        for (String key : appJsonUrlMap.keySet()) {
            String sourcePath = appJsonUrlMap.get(key);
            String destPath = finterfaceNas + Dict.TOTAL + "_" + env.toLowerCase() + "_" + date + Constants.SLASH + "xrouter" + Constants.SLASH;
            copyFile(new File(sourcePath), destPath);
        }
        //????????????????????????tar???
        String fileName = Dict.TOTAL + "_" + env.toLowerCase() + "_" + date;
        String tarName = fileName + Constants.TAR_FILE;
        String cmd_tar = "cd " + finterfaceNas + " && tar -cvf " + tarName + " " + fileName;
        CommonUtil.runCmd(cmd_tar);

        //????????????????????????????????????
        TotalJson totalJson = new TotalJson();
        totalJson.setEnv(env);
        totalJson.setDatTime(cicdTime);
        totalJson.setIsNew(1);
        totalJson.setAppJsonUrlMap(appJsonUrlMap);
        totalJson.setAppNum(appJsonUrlMap.size());
        totalJson.setTotalTarName(tarName);
        totalJson.setCentralJson(centralJson);
        //tar??????????????????
        totalJson.setTotalTarUrl(fdevInterfaceIp + Constants.DOWNLOAD_URL + tarName);
        totalJsonService.save(totalJson);
        // ?????????????????????fresource???nas???
        String fresourceNas;
        String host;
        if (Dict.SIT.equals(env)) {
            host = scpSitHost;
            fresourceNas = fresourceSitNas;
        } else if (Dict.UAT.equals(env)) {
            host = scpUatHost;
            fresourceNas = fresourceUatNas;
        } else {
            host = scpRelHost;
            fresourceNas = fresourceRelNas;
        }
        //??????stfp????????????
        SFTPClient sftpClient = new SFTPClient(host, user, password);
        SSHComandUtil.excute(user, password, host, "cd " + fresourceNas + " && mkdir -p " +Constants.XROUTER_DIR+Constants.SLASH+appNameEn );
        //????????????repo.json?????????fresource???nas
        sftpClient.pushRomote(fresourceNas+Constants.SLASH+Constants.XROUTER_DIR+Constants.SLASH+appNameEn,local_module+Constants.REPO_JSON);
        // ??????????????????
        routesService.analysisRoutesApi(md5FlagAndVer, appNameEn, branch, newMD5Str, jsonObject, Constants.AUTO_SCAN);
        FileUtil.deleteFiles(new File(scriptsPath));
        
        //????????????????????????nacos
        List<String> deployEnvList = new ArrayList<>();
        if(!Util.isNullOrEmpty(requestMap.get("env"))) {
        	deployEnvList = (List<String>) requestMap.get("env");
        }
        String resultMes = repoUploadNacos(deployEnvList, appNameEn, repoJsonMD5, repoJson,branch);
        logger.info(resultMes);
        return "??????????????????????????????!";
    }

    /**
     * ??????????????????
     * ??????sit?????????????????????????????????????????????sit??????
     * ??????release?????????????????????????????????????????????release??????
     * ??????tag????????????????????????tag
     *
     * @param branch
     * @return
     */
    private String getLastBranch(String appNameEn, Integer gitlabProjectId, String branch) {
        String lastBranch;
        if (branch.startsWith(Dict.SIT) || branch.startsWith(Dict.SIT_LOWER)) {
            lastBranch = appJsonService.getLastBranch(appNameEn, Dict.SIT_LOWER);
        } else if (branch.startsWith(Dict.RELEASE) || branch.startsWith(Dict.RELEASE_LOWER)) {
            // ???????????????????????????release??????
            lastBranch = appJsonService.getLastBranch(appNameEn, Dict.RELEASE_LOWER);
        } else {
        	//????????????pro???????????????????????? ??????pro-20220517_011-001
        	String removePro = "";
        	String releaseNodeName = "";
        	try {
        		removePro = branch.substring((branch.indexOf("-")+1));
        		releaseNodeName = removePro.substring(0,removePro.indexOf("-"));
			} catch (Exception e) {
				throw new FdevException("?????????????????????????????????");
			}
            // ?????????????????????tag???
            lastBranch = restTransportService.queryLastTagByGitlabId(gitlabProjectId,releaseNodeName);
        }
        return lastBranch;
    }

    @Override
    public Map<String, String> buildProDatTar(Map<String, Object> requestMap) {
        String env = (String) requestMap.get(Dict.ENV);
        String prodId = (String) requestMap.get(Dict.PROD_ID);
        String prodVersion = (String) requestMap.get(Dict.PROD_VERSION);
        List<Map<String, String>> projectNameList = (List<Map<String, String>>) requestMap.get(Dict.PROJECT_NAME);
        String date = TimeUtils.getFormat(TimeUtils.FORMAT_DATE);
        String datTime = TimeUtils.getFormat(TimeUtils.FORMAT_DATE_TIME);
        // ??????????????????????????????????????????????????????????????????????????????????????????????????????
        Map<String, Object> appJsonLastMap = appJsonService.getAppJsonLast(projectNameList);
        // ??????????????????????????????????????????????????????????????????????????????
        if (MapUtils.isEmpty(appJsonLastMap)) {
            return null;
        }


        //?????????????????????TotalJson?????? ????????????
        List<TotalJson> oldTotalJsons = totalJsonService.getTotalJsonListByEnv(requestMap);
        //????????????
        List<AppJson> appJsonList = (List<AppJson>) appJsonLastMap.get(Dict.APPJSON);
        //????????????????????????
        Map<String, String> appJsonUrlMap = new HashMap<>();
        //?????????????????????
        Map<String, String> prodAppJsonNameMap = new HashMap<>();
        //?????????????????????
        Map<String, Object> appJsonNameMap = new HashMap<>();
        //??????????????????????????????
        Map<String, String> newAppJsonUrlMap = new HashMap<>();
        String prodTime = prodVersion.substring(prodVersion.length() - 13).replace("_", "");
        prodTime = TimeUtils.getFormat(prodTime, TimeUtils.FORMAT_DATE_PROD);
        //?????????????????????????????????????????????????????????
        for (AppJson appjson : appJsonList) {
            //?????? key???name  value?????????
            Map<String, String> appJsonZhMap = new HashMap<>();
            String projetName = appjson.getProjectName();
            String appJsonName = appjson.getRepoTarName();
            String nameZn = appjson.getNameZh();
            String appJsonUrl = finterfaceNas+appJsonName;
            String destPath = finterfaceNas + Constants.SLASH + env + "-" + prodTime + Constants.SLASH + "xrouter" + Constants.SLASH;
            copyFile(new File(appJsonUrl), destPath);
            appJsonUrlMap.put(projetName, appJsonUrl);
            prodAppJsonNameMap.put(projetName,appJsonName);
            appJsonZhMap.put(Dict.NAME,nameZn);
            appJsonNameMap.put(projetName , appJsonZhMap);
        }
        String prodFileName = env + "-" + prodTime;
        String prodTarName = prodFileName + Constants.TAR_FILE;
        //????????????????????????tar???
        String prod_cmd_tar = "cd " + finterfaceNas + " && tar -cvf " + prodTarName + " " + prodFileName;
        CommonUtil.runCmd(prod_cmd_tar);

        Map<String, Object> centralJson = new HashMap<>();
        //???????????????????????????????????????????????????????????? ????????????????????????????????????????????????????????????????????????????????????????????????
        if (!oldTotalJsons.isEmpty()) {
            TotalJson oldTotalJson = oldTotalJsons.get(0);
            newAppJsonUrlMap = oldTotalJson.getAppJsonUrlMap();
            centralJson = oldTotalJson.getCentralJson();
            if (Util.isNullOrEmpty(centralJson)){
                centralJson = new HashMap<>();
                centralJson.put(Dict.REPOS , new HashMap<>());
            }
        }else {
            centralJson.put(Dict.REPOS , new HashMap<>());
        }
        Map<String , Object> reposMap = (Map<String, Object>) centralJson.get(Dict.REPOS);
        newAppJsonUrlMap.putAll(appJsonUrlMap);
        reposMap.putAll(appJsonNameMap);
        //?????????????????????????????? ?????????????????????????????????
        for (String key : newAppJsonUrlMap.keySet()) {
            String newAppJsonUrl = newAppJsonUrlMap.get(key);
            String destPath = finterfaceNas + Constants.SLASH + Dict.TOTAL + "_" + env + "_" + date + Constants.SLASH + "xrouter" + Constants.SLASH;
            copyFile(new File(newAppJsonUrl), destPath);
        }
        //???????????????????????????tar
        String fileName = Dict.TOTAL + "_" + env.toLowerCase() + "_" + date;
        String tarName = fileName + Constants.TAR_FILE;
        String cmd_tar = "cd " + finterfaceNas + " && tar -cvf " + tarName + " " + fileName;
        CommonUtil.runCmd(cmd_tar);

        //????????????????????????????????????
        TotalJson totalJson = new TotalJson();
        totalJson.setEnv(env);
        totalJson.setAppNum(newAppJsonUrlMap.size());
        totalJson.setDatTime(datTime);
        totalJson.setIsNew(1);
        totalJson.setAppJsonUrlMap(newAppJsonUrlMap);
        totalJson.setProdId(prodId);
        totalJson.setProdVersion(prodVersion);
        totalJson.setTotalTarName(tarName);
        totalJson.setTotalTarUrl(fdevInterfaceIp + Constants.DOWNLOAD_URL + tarName);
        totalJson.setCentralJson(centralJson);
        totalJsonService.save(totalJson);
        return prodAppJsonNameMap;
    }

    @Override
    public void exportFile(String fileName, HttpServletResponse response) {
        String filePath = finterfaceNas + fileName;
        FileUtil.downloadFile(fileName, filePath, response);
    }

    /**
     * ??????repo_mspmk-cli-XXX.json??????
     *
     * @param module
     * @param routesList
     * @return
     */
    private Map<String, Object> createRepoJson(String module, String local_module ,List<Map<String, Object>> routesList) {
        Map<String, Object> repoJson = new HashMap<>();
        Map<String, Object> stageMap = new HashMap<>();
        for (Map<String, Object> routesMap : routesList) {
            String name = (String) routesMap.get(Dict.NAME);
            if(Util.isNullOrEmpty(name)) {
            	logger.info("?????????????????????????????????project.json?????????routes????????????name??????");
            	throw new FdevException("?????????????????????????????????project.json?????????routes????????????name??????");
            }else {
            	stageMap.put(name, routesMap);
            }
        }
        repoJson.put(Constants.URL, "");
        repoJson.put(Dict.STAGES, stageMap);
        // ???repo.json????????????MD5????????????repo.json
        String repoJsonMD5 = MD5Util.encoder("", JSON.toJSONString(repoJson));
        repoJson.put(Dict.VERSION, repoJsonMD5);
        String fileDir = module + Constants.UNDER_LINE +Constants.REPO_JSON;
        String local_fileDir = local_module + Constants.REPO_JSON;
        FileUtil.createFile(fileDir, JSON.toJSONString(repoJson));
        FileUtil.createFile(local_fileDir, JSON.toJSONString(repoJson));

        // ??????????????????repo_mspmk-cli-XXX.json??????????????????
        Map<String, Object> returnMap = new HashMap<>();
        returnMap.put(Dict.PATH, fileDir);
        returnMap.put(Dict.REPO_JSON, repoJson);
        returnMap.put(Dict.REPO_JSON_MD5, repoJsonMD5);
        return returnMap;
    }


    /**
     * CICD??????????????????????????????????????????
     *
     * @param branch
     * @return
     */
    private String getEnvByBranch(String branch, Boolean tagFlag) {
        String env;
        if (branch.startsWith(Dict.SIT) || branch.startsWith(Dict.SIT_LOWER)) {
            env = Dict.SIT;
        } else if (branch.startsWith(Dict.RELEASE) || branch.startsWith(Dict.RELEASE_LOWER)) {
            env = Dict.UAT;
        } else if ((branch.startsWith(Dict.PRO) || branch.startsWith(Dict.PRO_LOWER)) && tagFlag) {
            env = Dict.REL;
        } else {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????????????????" + branch + "??????????????????"});
        }
        return env;
    }


    /**
     * ????????????
     * @param source ????????????????????????
     * @param dest ???????????????
     */
    private void copyFile(File source, String dest) {
        //?????????????????????
        File destfile = new File(dest);
        if (!destfile.exists()) {
            destfile.mkdirs();
        }
        //??????source?????????????????????????????????????????????????????????
        if (source.isDirectory()) {
            File file = new File(dest + "\\" + source.getName());
            file.mkdir();
            //??????source?????????????????????????????????
            File[] files = source.listFiles();
            if (files.length == 0) {
                return;
            } else {
                for (int i = 0; i < files.length; i++) {
                    copyFile(files[i], file.getPath());
                }
            }
        }
        //sources??????????????????????????????????????????????????????
        else if (source.isFile()) {
            FileInputStream fis = null;
            File dfile;
            FileOutputStream fos = null;
            byte[] b;
            try {
                fis = new FileInputStream(source);
                dfile = new File(dest + "\\" + source.getName());
                if (!dfile.exists()) {
                    dfile.createNewFile();
                }
                fos = new FileOutputStream(dfile);
                b = new byte[1024];
                int len;
                while ((len = fis.read(b)) != -1) {
                    fos.write(b, 0, len);
                }
            } catch (FileNotFoundException e) {
                logger.error("File not find error !" + source.getPath());
            } catch (IOException e) {
                logger.error("File IO error !" + source.getPath());
            } finally {
                if (fis != null && fos != null) {
                    try {
                        fis.close();
                        fos.close();
                    } catch (IOException e) {
                        logger.error("File IO close error!" + source.getPath());
                    }
                }
            }
        }
    }
    
    
    @Override
    public String buildSccDatTar(Map<String, Object> requestMap){
    	Integer gitlabProjectId = (Integer) requestMap.get(Dict.GITLAB_PROJECT_ID);
        Set<Integer> gitlabIds = new HashSet<>();
        gitlabIds.add(gitlabProjectId);
        String branch = (String) requestMap.get(Dict.BRANCH);
        List<Map<String, Object>> appInfoList = restTransportService.getAppByIdsOrGitlabIds(Dict.GITLAB_PROJECT_ID, new HashSet<>(), gitlabIds);
        if (CollectionUtils.isEmpty(appInfoList)) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????????????????GitLab ID???" + gitlabProjectId});
        }
        Map<String, Object> appInfoMap = appInfoList.get(0);
        String appNameEn = (String) appInfoMap.get(Dict.NAME_EN);
        // ??????project.json?????????routes?????????md5???????????????????????????????????????????????????????????????
        Map<String, Object> projectJsonMap = (Map<String, Object>) requestMap.get(Dict.PROJECT_JSON);
        if (MapUtils.isEmpty(projectJsonMap)) {
            // ???project.json?????????????????????????????????????????????,ver???-9??????????????????
            routesService.deleteRoutesApi(appNameEn, branch, -9);
            return "project.json??????????????????!";
        }
        List<Map<String, Object>> routesList = (List<Map<String, Object>>) projectJsonMap.get(Dict.ROUTES);
        if (CollectionUtils.isEmpty(routesList)) {
            // ???routesList???????????????????????????????????????
            routesService.deleteRoutesApi(appNameEn, branch, -9);
            return "project.json????????????routes????????????!";
        }
        Boolean tagFlag = (Boolean) requestMap.get(Dict.TAG_FLAG);
        String env = getEnvByBranch(branch, tagFlag);
        String date = TimeUtils.getFormat(TimeUtils.FORMAT_DATE);
        String module = finterfaceNas + appNameEn + Constants.UNDER_LINE + date;
        String local_module = scriptsPath + appNameEn + Constants.UNDER_LINE + date+Constants.UNDER_LINE+env.toLowerCase()+Constants.SLASH;

        Map<String, Object> repoJson = createRepoJson( module, local_module ,routesList);
        String repoJsonMD5 = (String) repoJson.get(Dict.REPO_JSON_MD5);
        
        //??????????????????
        JSONObject reJson = JSONObject.fromObject(repoJson.get(Dict.REPO_JSON));
        //??????????????????????????????
        String ossEnv = null;
        if(!Util.isNullOrEmpty(requestMap.get("env"))) {
        	List<String> deployEnvList = (List<String>) requestMap.get("env");
        	for (String key : deployEnvList) {
        		if (key.contains(Dict.SIT) || key.contains(Dict.SIT_LOWER)) {
        			ossEnv = "sit";
				} else if (key.contains(Dict.UAT) || key.contains(Dict.UAT_LOWER)) {
					ossEnv = "uat";
				} else if (key.contains(Dict.REL) || key.contains(Dict.REL_LOWER)) {
					ossEnv = "rel";
				} else {
					logger.info("?????????????????????????????????????????????");
					continue;
				}
        	}
        }
        
        String ossPath = ossEnv + "/" + appNameEn +"/repo.json"; 
        File file = new File(local_module+"/repo.json");
        FileInputStream fis = null;
        try {
        	fis = new FileInputStream(file);
			ossService.createObjectWithKey(ossPath, fis, file.length());
			ossService.getS3Client().setObjectAcl(bucketName,ossPath,CannedAccessControlList.PublicRead);
		} catch (OSSException | FileNotFoundException e) {
			logger.info("????????????????????????????????????fdev????????????");
		}finally {
			try {
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
        
    	/**
    	 * ????????????????????????????????????????????????nacos????????????????????????
    	 * ??????nacos api???????????????{app_name}_repo.json??????
    	 * ??????{app_name}_repo.json????????????????????????md5?????????????????????????????????md5???????????????????????????nacos
    	 * ??????{app_name}_repo.json???????????????????????????fresource_xrouter.yaml??????
    	 * ??????fresource_xrouter.yaml??????????????????{app_name}_repo.json??????????????????nacos
    	 * */
        if(!Util.isNullOrEmpty(requestMap.get("env"))) {
        	List<String> deployEnvList = (List<String>) requestMap.get("env");
        	String tenant = null;
            List<String> resultMessage = new ArrayList<String>();
			String nacosUrl = null;
			for (String key : deployEnvList) {
				if (key.contains(Dict.SIT) || key.contains(Dict.SIT_LOWER)) {
					nacosUrl = nacosSitUrl;
				} else if (key.contains(Dict.UAT) || key.contains(Dict.UAT_LOWER)) {
					nacosUrl = nacosUatUrl;
				} else if (key.contains(Dict.REL) || key.contains(Dict.REL_LOWER)) {
					nacosUrl = nacosRelUrl;
				} else if (key.contains(Dict.QY) || key.contains(Dict.QY_LOWER)) {
					nacosUrl = nacosQyUrl;
				} else if (key.contains(Dict.CP) || key.contains(Dict.CP_LOWER)) {
					nacosUrl = nacosCpUrl;
				} else {
					logger.info("?????????????????????????????????????????????");
					continue;
				}

				tenant = key;
				StringBuffer queryRepoUrl = new StringBuffer();
				queryRepoUrl.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
				queryRepoUrl.append("&").append("password").append("=").append(nacosPassword);
				queryRepoUrl.append("&").append("tenant").append("=").append(tenant);
				queryRepoUrl.append("&").append("group").append("=").append(nacosGroup);
				queryRepoUrl.append("&").append("dataId").append("=").append(appNameEn).append("_repo.json");

				ResponseEntity responseEntity = queryNacosRepo(queryRepoUrl.toString());
				JSONObject json = new JSONObject();
				String md5Version;

				if (!Util.isNullOrEmpty(responseEntity)) {
					String rep = responseEntity.getBody().toString();
					if (!Util.isNullOrEmpty(rep)) {
						json = JSONObject.fromObject(rep);
						md5Version = (String) json.get("version");
						if (!Util.isNullOrEmpty(md5Version) && !Util.isNullOrEmpty(repoJsonMD5)
								&& repoJsonMD5.equals(md5Version)) {
							String message = null;
							message = "nacos" + tenant + "???????????????????????????";
							resultMessage.add(message);
							logger.info(message);
							continue;
						} else {
							// ????????????????????????
							Boolean uploadRepoFlag = false;
							JSONObject repo = JSONObject.fromObject(repoJson.get(Dict.REPO_JSON));
							StringBuffer uploadRepo = new StringBuffer();
							uploadRepo.append(nacosUrl).append("?").append("username").append("=")
									.append(nacosUserName);
							uploadRepo.append("&").append("password").append("=").append(nacosPassword);
							uploadRepo.append("&").append("tenant").append("=").append(tenant);
							uploadRepo.append("&").append("group").append("=").append(nacosGroup);
							uploadRepo.append("&").append("dataId").append("=").append(appNameEn).append("_repo.json");
							uploadRepo.append("&").append("type").append("=").append("json");
							uploadRepo.append("&").append("content").append("=")
									.append(URLEncoder.encode(repo.toString()));
							uploadRepoFlag = uploadNacosURI(uploadRepo.toString());
							if (uploadRepoFlag) {
								logger.info("????????????" + appNameEn + "_repo.json???????????????????????????");
							}
						}
					}
				} else {
					// ????????????????????????
					Boolean uploadRepoFlag = false;
					JSONObject repo = JSONObject.fromObject(repoJson.get(Dict.REPO_JSON));
					StringBuffer uploadRepo = new StringBuffer();
					uploadRepo.append(nacosUrl).append("?").append("username").append("=")
							.append(nacosUserName);
					uploadRepo.append("&").append("password").append("=").append(nacosPassword);
					uploadRepo.append("&").append("tenant").append("=").append(tenant);
					uploadRepo.append("&").append("group").append("=").append(nacosGroup);
					uploadRepo.append("&").append("dataId").append("=").append(appNameEn).append("_repo.json");
					uploadRepo.append("&").append("type").append("=").append("json");
					uploadRepo.append("&").append("content").append("=")
							.append(URLEncoder.encode(repo.toString()));
					uploadRepoFlag = uploadNacosURI(uploadRepo.toString());
					if (uploadRepoFlag) {
						logger.info("????????????" + appNameEn + "_repo.json????????????????????????????????????");
					}
					
					StringBuffer queryYamlUrl = new StringBuffer();
					queryYamlUrl.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
					queryYamlUrl.append("&").append("password").append("=").append(nacosPassword);
					queryYamlUrl.append("&").append("tenant").append("=").append(tenant);
					queryYamlUrl.append("&").append("group").append("=").append(nacosGroup);
					queryYamlUrl.append("&").append("dataId").append("=").append("fresource_xrouter.yaml");

					responseEntity = queryNacosYaml(queryYamlUrl.toString());
					StringBuffer fresourceUpdateStr = new StringBuffer();
					String oldYaml = null;
					if (!Util.isNullOrEmpty(responseEntity)) {
						oldYaml = responseEntity.getBody().toString();
					}
					StringBuffer addContent = new StringBuffer();
					addContent.append(nacosGroup).append(".").append(appNameEn).append("_repo.json").append(": ")
							.append("xrouter_").append(appNameEn).append("_repo.json");
					if (!Util.isNullOrEmpty(oldYaml)) {
						if (oldYaml.contains(addContent.toString())) {
							logger.info("fresource_xrouter.yaml????????????" + addContent.toString() + "?????????????????????");
							continue;
						}
						fresourceUpdateStr = fresourceUpdateStr.append(oldYaml).append("\n").append(addContent);
						StringBuffer uploadYamlUrl = new StringBuffer();
						uploadYamlUrl.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
						uploadYamlUrl.append("&").append("password").append("=").append(nacosPassword);
						uploadYamlUrl.append("&").append("tenant").append("=").append(tenant);
						uploadYamlUrl.append("&").append("group").append("=").append(nacosGroup);
						uploadYamlUrl.append("&").append("dataId").append("=").append("fresource_xrouter.yaml");
						uploadYamlUrl.append("&").append("type").append("=").append("yaml");
						uploadYamlUrl.append("&").append("content").append("=").append(fresourceUpdateStr);
						Boolean uploadYamlFlag = false;
						// ?????? fresource_xrouter.yaml
						uploadYamlFlag = uploadNacos(uploadYamlUrl.toString());
						if (uploadYamlFlag) {
							logger.info("fresource_xrouter.yaml?????????????????????");
						}
					} else {
						fresourceUpdateStr = fresourceUpdateStr.append(addContent);
						StringBuffer uploadYamlUrl = new StringBuffer();
						uploadYamlUrl.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
						uploadYamlUrl.append("&").append("password").append("=").append(nacosPassword);
						uploadYamlUrl.append("&").append("tenant").append("=").append(tenant);
						uploadYamlUrl.append("&").append("group").append("=").append(nacosGroup);
						uploadYamlUrl.append("&").append("dataId").append("=").append("fresource_xrouter.yaml");
						uploadYamlUrl.append("&").append("type").append("=").append("yaml");
						uploadYamlUrl.append("&").append("content").append("=").append(fresourceUpdateStr);
						Boolean uploadYamlFlag = false;
						// ?????? fresource_xrouter.yaml
						uploadYamlFlag = uploadNacos(uploadYamlUrl.toString());
						if (uploadYamlFlag) {
							logger.info("fresource_xrouter.yaml????????????");
						}
					}
				}
			}
            StringBuffer messageSb = new StringBuffer();
            if(!Util.isNullOrEmpty(resultMessage)) {
            	for (Iterator iterator = resultMessage.iterator(); iterator.hasNext();) {
        			String message = (String) iterator.next();
        			if(!Util.isNullOrEmpty(message)) {
        				if(!Util.isNullOrEmpty(messageSb.toString())) {
        					messageSb.append(",");
        				}
        				messageSb.append(message);
        			}
        		}
            	return messageSb.toString();
            }
        	return "scc???????????????????????????????????????????????????!";
        }else {
        	//scc???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        	List<String> sit1Nacos = new ArrayList<>();
 			List<String> sit2Nacos = new ArrayList<>();
 			List<String> uat1Nacos = new ArrayList<>();
 			List<String> uat2Nacos = new ArrayList<>();
 			List<String> rel1Nacos = new ArrayList<>();
 			List<String> rel2Nacos = new ArrayList<>();
 			if(branch.startsWith(Dict.SIT) || branch.startsWith(Dict.SIT_LOWER)) {
	 			sit1Nacos = exeNacos(appNameEn, "scc-sit1", nacosSitUrl, repoJsonMD5, repoJson);
	        	sit2Nacos = exeNacos(appNameEn, "scc-sit2", nacosSitUrl, repoJsonMD5, repoJson);
	 		}else if(branch.startsWith(Dict.RELEASE) || branch.startsWith(Dict.RELEASE_LOWER)) {
	 			uat1Nacos = exeNacos(appNameEn, "scc-uat-uz11", nacosUatUrl, repoJsonMD5, repoJson);
	        	uat2Nacos = exeNacos(appNameEn, "scc-uat-uz12", nacosUatUrl, repoJsonMD5, repoJson);
	 		}else if((branch.startsWith(Dict.PRO) || branch.startsWith(Dict.PRO_LOWER))){
	 			rel1Nacos = exeNacos(appNameEn, "scc-rel-uz11", nacosRelUrl, repoJsonMD5, repoJson);
	        	rel2Nacos = exeNacos(appNameEn, "scc-rel-uz12", nacosRelUrl, repoJsonMD5, repoJson);
	 		}else {
	 			return "??????????????????SIT???RELEASE???PRO???????????????????????????????????????";
	 		}
 			
        	List<String> resultMessage = new ArrayList<String>();
        	resultMessage.addAll(sit1Nacos);
        	resultMessage.addAll(sit2Nacos);
        	resultMessage.addAll(uat1Nacos);
        	resultMessage.addAll(uat2Nacos);
        	resultMessage.addAll(rel1Nacos);
        	resultMessage.addAll(rel2Nacos);
        	 StringBuffer messageSb = new StringBuffer();
             if(!Util.isNullOrEmpty(resultMessage)) {
             	for (Iterator iterator = resultMessage.iterator(); iterator.hasNext();) {
         			String message = (String) iterator.next();
         			if(!Util.isNullOrEmpty(message)) {
         				if(!Util.isNullOrEmpty(messageSb.toString())) {
         					messageSb.append(",");
         				}
         				messageSb.append(message);
         			}
         		}
             	return messageSb.toString();
             }
         	return "scc???????????????????????????????????????????????????!";
        }
    }
    
    
    /**
     * ??????nacos api ????????????{app_name}_repo.json??????
     * */
    public ResponseEntity queryNacosRepo(String url) {
    	MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
    	ResponseEntity responseEntity = null;
    	try {
			responseEntity = HttpRequestUtils.sendGET(url, header);
		} catch (Exception e) {
			return responseEntity;
		}
    	return responseEntity;
    }
    
    /**
     * ??????nacos api
     * {app_name}_repo.json????????????????????? ??????fresource_xrouter.yaml??????
     * */
    public ResponseEntity queryNacosYaml(String url) {
    	MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
    	ResponseEntity responseEntity = null;
    	try {
			responseEntity = HttpRequestUtils.sendGET(url, header);
		} catch (Exception e) {
			logger.info("fresource_xrouter.yaml???????????????");
		}
    	return responseEntity;
    }
    
    /**
     * nacos ????????????
     * */
    public boolean uploadNacos(String url) {
    	boolean flag = false;
    	try {
    		ResponseEntity responseEntity = HttpRequestUtils.sendPOST(url, null);
    		flag = "true".equals(responseEntity.getBody());
			return flag;
		} catch (Exception e) {
			throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"???????????????nacos??????"});
		}
    }
    
    /**
     * nacos ????????????
     * */
    public boolean uploadNacosURI(String reqUrl) {
    	boolean flag = false;
    	URI url ;
    	try {
    		url = new URI(reqUrl);
    		ResponseEntity responseEntity = HttpRequestUtils.sendPOST(url, null);
    		flag = "true".equals(responseEntity.getBody());
			return flag;
		} catch (Exception e) {
			throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"???????????????nacos??????"});
		}
    }
    
    public List<String> exeNacos(String appNameEn, String tenant,String nacosUrl,String repoJsonMD5,Map<String, Object> repoJson) {
    	StringBuffer queryRepoUrl = new StringBuffer();
    	
		queryRepoUrl.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
		queryRepoUrl.append("&").append("password").append("=").append(nacosPassword);
		queryRepoUrl.append("&").append("tenant").append("=").append(tenant);
		queryRepoUrl.append("&").append("group").append("=").append(nacosGroup);
		queryRepoUrl.append("&").append("dataId").append("=").append(appNameEn).append("_repo.json");
		
		ResponseEntity responseEntity = queryNacosRepo(queryRepoUrl.toString());
		JSONObject json = new JSONObject();
		String md5Version;
		List<String> resultMessage = new ArrayList<String>();
		if(!Util.isNullOrEmpty(responseEntity)) {
			String rep = responseEntity.getBody().toString();
			if(!Util.isNullOrEmpty(rep)) {
				json = JSONObject.fromObject(rep);
				md5Version = (String) json.get("version");
				if(!Util.isNullOrEmpty(md5Version) && !Util.isNullOrEmpty(repoJsonMD5) && repoJsonMD5.equals(md5Version)) {
					String message = null;
					message = "nacos"+tenant+"???????????????????????????";
					resultMessage.add(message);
					logger.info(message);
				}else {
					//????????????????????????
			    	Boolean uploadRepoFlag = false; 
			    	JSONObject repo = JSONObject.fromObject(repoJson.get(Dict.REPO_JSON));
			    	StringBuffer uploadRepo = new StringBuffer();
			    	uploadRepo.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
			    	uploadRepo.append("&").append("password").append("=").append(nacosPassword);
			    	uploadRepo.append("&").append("tenant").append("=").append(tenant);
			    	uploadRepo.append("&").append("group").append("=").append(nacosGroup);
			    	uploadRepo.append("&").append("dataId").append("=").append(appNameEn).append("_repo.json");
			    	uploadRepo.append("&").append("type").append("=").append("json");
			    	uploadRepo.append("&").append("content").append("=").append(URLEncoder.encode(repo.toString()));
			    	uploadRepoFlag = uploadNacosURI(uploadRepo.toString());
			    	if(uploadRepoFlag) {
			    		logger.info("????????????"+appNameEn+"_repo.json???????????????????????????");
			    	}
				}
			}
		}else {
			//????????????????????????
	    	Boolean uploadRepoFlag = false; 
	    	JSONObject repo = JSONObject.fromObject(repoJson.get(Dict.REPO_JSON));
	    	StringBuffer uploadRepo = new StringBuffer();
	    	uploadRepo.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
	    	uploadRepo.append("&").append("password").append("=").append(nacosPassword);
	    	uploadRepo.append("&").append("tenant").append("=").append(tenant);
	    	uploadRepo.append("&").append("group").append("=").append(nacosGroup);
	    	uploadRepo.append("&").append("dataId").append("=").append(appNameEn).append("_repo.json");
	    	uploadRepo.append("&").append("type").append("=").append("json");
	    	uploadRepo.append("&").append("content").append("=").append(URLEncoder.encode(repo.toString()));
	    	uploadRepoFlag = uploadNacosURI(uploadRepo.toString());
	    	if(uploadRepoFlag) {
	    		logger.info(appNameEn+"_repo.json????????????");
	    	}
	    	
			StringBuffer queryYamlUrl = new StringBuffer();
			queryYamlUrl.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
			queryYamlUrl.append("&").append("password").append("=").append(nacosPassword);
			queryYamlUrl.append("&").append("tenant").append("=").append(tenant);
			queryYamlUrl.append("&").append("group").append("=").append(nacosGroup);
			queryYamlUrl.append("&").append("dataId").append("=").append("fresource_xrouter.yaml");
	    	
			responseEntity = queryNacosYaml(queryYamlUrl.toString());
	    	StringBuffer fresourceUpdateStr = new StringBuffer();
	    	String oldYaml = null;
	    	if(!Util.isNullOrEmpty(responseEntity)) {
	    		oldYaml = responseEntity.getBody().toString();
	    	}
	    	StringBuffer addContent = new StringBuffer();
	    	addContent.append(nacosGroup).append(".").append(appNameEn).append("_repo.json").append(": ").append("xrouter_").append(appNameEn).append("_repo.json");
	    	if(!Util.isNullOrEmpty(oldYaml)) {
	    		if(oldYaml.contains(addContent.toString())) {
	    			logger.info("fresource_xrouter.yaml????????????"+addContent.toString()+"?????????????????????");
	    		}else {
	    			fresourceUpdateStr = fresourceUpdateStr.append(oldYaml).append("\n").append(addContent);
			    	StringBuffer uploadYamlUrl = new StringBuffer();
			    	uploadYamlUrl.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
			    	uploadYamlUrl.append("&").append("password").append("=").append(nacosPassword);
			    	uploadYamlUrl.append("&").append("tenant").append("=").append(tenant);
			    	uploadYamlUrl.append("&").append("group").append("=").append(nacosGroup);
			    	uploadYamlUrl.append("&").append("dataId").append("=").append("fresource_xrouter.yaml");
			    	uploadYamlUrl.append("&").append("type").append("=").append("yaml");
			    	uploadYamlUrl.append("&").append("content").append("=").append(fresourceUpdateStr);
			    	Boolean uploadYamlFlag = false; 
			    	//?????? fresource_xrouter.yaml
			    	uploadYamlFlag = uploadNacos(uploadYamlUrl.toString());
			    	if(uploadYamlFlag) {
			    		logger.info("fresource_xrouter.yaml?????????????????????");
			    	}
	    		}
	    	}else {
	    		fresourceUpdateStr = fresourceUpdateStr.append(addContent);
		    	StringBuffer uploadYamlUrl = new StringBuffer();
		    	uploadYamlUrl.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
		    	uploadYamlUrl.append("&").append("password").append("=").append(nacosPassword);
		    	uploadYamlUrl.append("&").append("tenant").append("=").append(tenant);
		    	uploadYamlUrl.append("&").append("group").append("=").append(nacosGroup);
		    	uploadYamlUrl.append("&").append("dataId").append("=").append("fresource_xrouter.yaml");
		    	uploadYamlUrl.append("&").append("type").append("=").append("yaml");
		    	uploadYamlUrl.append("&").append("content").append("=").append(fresourceUpdateStr);
		    	Boolean uploadYamlFlag = false; 
		    	//?????? fresource_xrouter.yaml
		    	uploadYamlFlag = uploadNacos(uploadYamlUrl.toString());
		    	if(uploadYamlFlag) {
		    		logger.info("fresource_xrouter.yaml????????????");
		    	}
	    	}
		}
		
    	return resultMessage;
    }
    
    
    /**
	 * ????????????????????????????????????????????????nacos????????????????????????
	 * ??????nacos api???????????????{app_name}_repo.json??????
	 * ??????{app_name}_repo.json????????????????????????md5?????????????????????????????????md5???????????????????????????nacos
	 * ??????{app_name}_repo.json???????????????????????????{app_name}_repo.json???????????????fresource_xrouter.yaml??????
	 * ??????fresource_xrouter.yaml??????????????????fresource_xrouter.yaml?????????{app_name}_repo.json???????????????????????????{app_name}_repo.json??????????????????nacos
	 * */
    public String repoUploadNacos(List<String> env,String appNameEn,String repoJsonMD5,Map<String, Object> repoJson,String branch) {
    	if(!Util.isNullOrEmpty(env)) {
        	String tenant = null;
        	List<String> resultMessage = new ArrayList<String>();
 			String nacosUrl = null;
 			for (String key : env) {
 				if (key.contains(Dict.SIT) || key.contains(Dict.SIT_LOWER)) { 
					nacosUrl = nacosSitUrl;
				} else if (key.contains(Dict.UAT) || key.contains(Dict.UAT_LOWER)) {
					nacosUrl = nacosUatUrl;
				} else if (key.contains(Dict.REL) || key.contains(Dict.REL_LOWER)) {
					nacosUrl = nacosRelUrl;
				} else if (key.contains(Dict.QY) || key.contains(Dict.QY_LOWER)) {
					nacosUrl = nacosQyUrl;
				} else if (key.contains(Dict.CP) || key.contains(Dict.CP_LOWER)) {
					nacosUrl = nacosCpUrl;
				} else {
					logger.info(key + "?????????????????????????????????????????????");
					continue;
				}
 				//??????nacos API??????????????????md5???
				tenant = key;
				StringBuffer queryRepoUrl = new StringBuffer();
				queryRepoUrl.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
				queryRepoUrl.append("&").append("password").append("=").append(nacosPassword);
				queryRepoUrl.append("&").append("tenant").append("=").append(tenant);
				queryRepoUrl.append("&").append("group").append("=").append(nacosGroup);
				queryRepoUrl.append("&").append("dataId").append("=").append(appNameEn).append("_repo.json");
				
				ResponseEntity responseEntity = queryNacosRepo(queryRepoUrl.toString());
				JSONObject json = new JSONObject();
				String md5Version;
				//??????nacos?????????????????????????????????????????????md5?????????????????????????????????
				if (!Util.isNullOrEmpty(responseEntity)) {
					String rep = responseEntity.getBody().toString();
					if (!Util.isNullOrEmpty(rep)) {
						json = JSONObject.fromObject(rep);
						md5Version = (String) json.get("version");//nacos?????????????????????md5
						//??????nacos?????????????????????????????????????????????
						if (!Util.isNullOrEmpty(md5Version) && !Util.isNullOrEmpty(repoJsonMD5)
								&& repoJsonMD5.equals(md5Version)) {
							String message = null;
							message = "nacos" + tenant + "???????????????????????????";
							resultMessage.add(message);
							logger.info(message);
							continue;
						} else {
							// ????????????????????????
							Boolean uploadRepoFlag = false;
							JSONObject repo = JSONObject.fromObject(repoJson.get(Dict.REPO_JSON));
							StringBuffer uploadRepo = new StringBuffer();
							uploadRepo.append(nacosUrl).append("?").append("username").append("=")
									.append(nacosUserName);
							uploadRepo.append("&").append("password").append("=").append(nacosPassword);
							uploadRepo.append("&").append("tenant").append("=").append(tenant);
							uploadRepo.append("&").append("group").append("=").append(nacosGroup);
							uploadRepo.append("&").append("dataId").append("=").append(appNameEn).append("_repo.json");
							uploadRepo.append("&").append("type").append("=").append("json");
							uploadRepo.append("&").append("content").append("=")
									.append(URLEncoder.encode(repo.toString()));
							uploadRepoFlag = uploadNacosURI(uploadRepo.toString());
							if (uploadRepoFlag) {
								logger.info("????????????" + appNameEn + "_repo.json???????????????????????????");
							}
						}
					}
				} else {
					// ????????????????????????
					Boolean uploadRepoFlag = false;
					JSONObject repo = JSONObject.fromObject(repoJson.get(Dict.REPO_JSON));
					StringBuffer uploadRepo = new StringBuffer();
					uploadRepo.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
					uploadRepo.append("&").append("password").append("=").append(nacosPassword);
					uploadRepo.append("&").append("tenant").append("=").append(tenant);
					uploadRepo.append("&").append("group").append("=").append(nacosGroup);
					uploadRepo.append("&").append("dataId").append("=").append(appNameEn).append("_repo.json");
					uploadRepo.append("&").append("type").append("=").append("json");
					uploadRepo.append("&").append("content").append("=").append(URLEncoder.encode(repo.toString()));
					uploadRepoFlag = uploadNacosURI(uploadRepo.toString());
					if (uploadRepoFlag) {
						logger.info(appNameEn + "_repo.json????????????");
					}
					
					StringBuffer queryYamlUrl = new StringBuffer();
					queryYamlUrl.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
					queryYamlUrl.append("&").append("password").append("=").append(nacosPassword);
					queryYamlUrl.append("&").append("tenant").append("=").append(tenant);
					queryYamlUrl.append("&").append("group").append("=").append(nacosGroup);
					queryYamlUrl.append("&").append("dataId").append("=").append("fresource_xrouter.yaml");

					responseEntity = queryNacosYaml(queryYamlUrl.toString());
					StringBuffer fresourceUpdateStr = new StringBuffer();
					String oldYaml = null;
					if (!Util.isNullOrEmpty(responseEntity)) {
						oldYaml = responseEntity.getBody().toString();
					}
					StringBuffer addContent = new StringBuffer();
					addContent.append(nacosGroup).append(".").append(appNameEn).append("_repo.json").append(": ")
							.append("xrouter_").append(appNameEn).append("_repo.json");
					if (!Util.isNullOrEmpty(oldYaml)) {
						if (oldYaml.contains(addContent.toString())) {
							logger.info("fresource_xrouter.yaml????????????" + addContent.toString() + "?????????????????????");
							continue;
						}
						fresourceUpdateStr = fresourceUpdateStr.append(oldYaml).append("\n").append(addContent);
						StringBuffer uploadYamlUrl = new StringBuffer();
						uploadYamlUrl.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
						uploadYamlUrl.append("&").append("password").append("=").append(nacosPassword);
						uploadYamlUrl.append("&").append("tenant").append("=").append(tenant);
						uploadYamlUrl.append("&").append("group").append("=").append(nacosGroup);
						uploadYamlUrl.append("&").append("dataId").append("=").append("fresource_xrouter.yaml");
						uploadYamlUrl.append("&").append("type").append("=").append("yaml");
						uploadYamlUrl.append("&").append("content").append("=").append(fresourceUpdateStr);
						Boolean uploadYamlFlag = false;
						// ?????? fresource_xrouter.yaml
						uploadYamlFlag = uploadNacos(uploadYamlUrl.toString());
						if (uploadYamlFlag) {
							logger.info("fresource_xrouter.yaml?????????????????????");
						}
					} else {
						fresourceUpdateStr = fresourceUpdateStr.append(addContent);
						StringBuffer uploadYamlUrl = new StringBuffer();
						uploadYamlUrl.append(nacosUrl).append("?").append("username").append("=").append(nacosUserName);
						uploadYamlUrl.append("&").append("password").append("=").append(nacosPassword);
						uploadYamlUrl.append("&").append("tenant").append("=").append(tenant);
						uploadYamlUrl.append("&").append("group").append("=").append(nacosGroup);
						uploadYamlUrl.append("&").append("dataId").append("=").append("fresource_xrouter.yaml");
						uploadYamlUrl.append("&").append("type").append("=").append("yaml");
						uploadYamlUrl.append("&").append("content").append("=").append(fresourceUpdateStr);
						Boolean uploadYamlFlag = false;
						// ?????? fresource_xrouter.yaml
						uploadYamlFlag = uploadNacos(uploadYamlUrl.toString());
						if (uploadYamlFlag) {
							logger.info("fresource_xrouter.yaml????????????");
						}
					}
				}
 			}
 			StringBuffer messageSb = new StringBuffer();
            if(!Util.isNullOrEmpty(resultMessage)) {
            	for (Iterator iterator = resultMessage.iterator(); iterator.hasNext();) {
        			String message = (String) iterator.next();
        			if(!Util.isNullOrEmpty(message)) {
        				if(!Util.isNullOrEmpty(messageSb.toString())) {
        					messageSb.append(",");
        				}
        				messageSb.append(message);
        			}
        		}
            	return messageSb.toString();
            }
        	return "??????????????????????????????!";
 		}else {
 			//scc???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
 			List<String> sit1Nacos = new ArrayList<>();
 			List<String> sit2Nacos = new ArrayList<>();
 			List<String> uat1Nacos = new ArrayList<>();
 			List<String> uat2Nacos = new ArrayList<>();
 			List<String> rel1Nacos = new ArrayList<>();
 			List<String> rel2Nacos = new ArrayList<>();
	 		if(branch.startsWith(Dict.SIT) || branch.startsWith(Dict.SIT_LOWER)) {
	 			sit1Nacos = exeNacos(appNameEn, "sit1-dmz", nacosSitUrl, repoJsonMD5, repoJson);
	        	sit2Nacos = exeNacos(appNameEn, "sit2-dmz", nacosSitUrl, repoJsonMD5, repoJson);
	 		}else if(branch.startsWith(Dict.RELEASE) || branch.startsWith(Dict.RELEASE_LOWER)) {
	 			uat1Nacos = exeNacos(appNameEn, "uat1-dmz", nacosUatUrl, repoJsonMD5, repoJson);
	        	uat2Nacos = exeNacos(appNameEn, "uat2-dmz", nacosUatUrl, repoJsonMD5, repoJson);
	 		}else if((branch.startsWith(Dict.PRO) || branch.startsWith(Dict.PRO_LOWER))){
	 			rel1Nacos = exeNacos(appNameEn, "rel1-dmz", nacosRelUrl, repoJsonMD5, repoJson);
	        	rel2Nacos = exeNacos(appNameEn, "rel2-dmz", nacosRelUrl, repoJsonMD5, repoJson);
	 		}else {
	 			return "??????????????????SIT???RELEASE???PRO???????????????????????????????????????";
	 		}
        	
        	List<String> resultMessage = new ArrayList<String>();
        	resultMessage.addAll(sit1Nacos);
        	resultMessage.addAll(sit2Nacos);
        	resultMessage.addAll(uat1Nacos);
        	resultMessage.addAll(uat2Nacos);
        	resultMessage.addAll(rel1Nacos);
        	resultMessage.addAll(rel2Nacos);
        	 StringBuffer messageSb = new StringBuffer();
             if(!Util.isNullOrEmpty(resultMessage)) {
             	for (Iterator iterator = resultMessage.iterator(); iterator.hasNext();) {
         			String message = (String) iterator.next();
         			if(!Util.isNullOrEmpty(message)) {
         				if(!Util.isNullOrEmpty(messageSb.toString())) {
         					messageSb.append(",");
         				}
         				messageSb.append(message);
         			}
         		}
             	return messageSb.toString();
             }
         	return "??????????????????????????????!";
        }
    } 
    
    
}
