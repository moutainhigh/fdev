package com.spdb.fdev.release.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.spdb.fdev.release.dao.IProdApplicationDao;
import org.apache.http.entity.ContentType;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import com.spdb.fdev.base.dict.Constants;
import com.spdb.fdev.base.dict.Dict;
import com.spdb.fdev.base.dict.ErrorConstants;
import com.spdb.fdev.base.utils.CommonUtils;
import com.spdb.fdev.base.utils.TimeUtils;
import com.spdb.fdev.common.annoation.LazyInitProperty;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.common.util.Util;
import com.spdb.fdev.release.dao.IProdAssetDao;
import com.spdb.fdev.release.dao.IReleaseApplicationDao;
import com.spdb.fdev.release.entity.ApplicationImage;
import com.spdb.fdev.release.entity.AutomationEnv;
import com.spdb.fdev.release.entity.AutomationParam;
import com.spdb.fdev.release.entity.ProdApplication;
import com.spdb.fdev.release.entity.ProdAsset;
import com.spdb.fdev.release.entity.ProdRecord;
import com.spdb.fdev.release.entity.RelDevopsRecord;
import com.spdb.fdev.release.entity.ReleaseApplication;
import com.spdb.fdev.release.service.IAppService;
import com.spdb.fdev.release.service.IApplicationImageService;
import com.spdb.fdev.release.service.IAsyncAutoReleaseService;
import com.spdb.fdev.release.service.IAutomationEnvService;
import com.spdb.fdev.release.service.IAutomationParamService;
import com.spdb.fdev.release.service.IFileService;
import com.spdb.fdev.release.service.IGitlabService;
import com.spdb.fdev.release.service.IProdApplicationService;
import com.spdb.fdev.release.service.IProdAssetService;
import com.spdb.fdev.release.service.IProdRecordService;
import com.spdb.fdev.release.service.IPushImageService;
import com.spdb.fdev.release.service.IRelDevopsRecordService;
import com.spdb.fdev.release.service.IReleaseApplicationService;
import com.spdb.fdev.release.service.IRoleService;
import com.spdb.fdev.transport.RestTransport;

import net.sf.json.JSONObject;

@Service
@RefreshScope
public class PushImageServiceImpl implements IPushImageService {

    @Autowired
    IRoleService roleService;
    @Autowired
    IAppService appService;
    @Autowired
    IProdRecordService prodRecordService;
    @Autowired
    IProdApplicationService prodApplicationService;
    @Autowired
    IRelDevopsRecordService relDevopsRecordService;
    @Autowired
    IAutomationParamService automationParamService;
    @Autowired
    IApplicationImageService applicationImageService;
    @Autowired
    IAsyncAutoReleaseService asyncAutoReleaseService;
    @Autowired
    IReleaseApplicationService releaseApplicationService;
    @Autowired
    IProdAssetService prodAssetService;
    @Autowired
    IGitlabService gitlabService;
    @Autowired
    IReleaseApplicationDao releaseApplicationDao;
    @Autowired
    IProdAssetDao prodAssetDao;
    @Autowired
    private IFileService fileService;
    @Autowired
    private IProdApplicationDao prodApplicationDao;

    @Value("${aws.bucketName}")
    String bucketName;

    @Value("${fdev.autorelease.dmz.env.gray.scc.map}")
    private String sccGrayEnv;

    @Value("${fdev.autorelease.dmz.env.scc.map}")
    private String sccProductEnv;
    @Value("${prod.record.minio.url}")
    private String recordAssetsUrl;// /testassets-sit/
    private final Logger logger = LoggerFactory.getLogger(PushImageServiceImpl.class);
    
    @Autowired
    private RestTransport restTransport;
    
    @Autowired
    private IAutomationEnvService automationEnvService;
    
    @Value("${fdev.biz.mapping}")
    private String fdevBiz;
    @Value("${fdev.dmz.mapping}")
    private String fdevDmz;
    @Value("${gitlab.manager.token}")
    private String Token;
    
    @Override
    @LazyInitProperty(redisKeyExpression = "frelease.envinfo.{gitlabId}")
    public Map<String, Object> findConfigByGitlab(String gitlabId) throws Exception {
        Map<String, Object> send_map = new HashMap<>();
        send_map.put(Dict.GITLABID, gitlabId);// ???fdev-env-config??????????????????????????????????????????
        send_map.put(Dict.REST_CODE, "queryByGitlabId");
        return (Map<String, Object>) restTransport.submit(send_map);
    }

    @Override
    public Object pushImage(Map<String, Object> requestParam) throws Exception {
        //1.????????????
        String applicationId = (String) requestParam.get(Dict.APPLICATION_ID);
        if (!roleService.isGroupReleaseManager(CommonUtils.getSessionUser().getGroup_id())
                && !roleService.isAppSpdbManager(applicationId)
                && !roleService.isApplicationManager(applicationId)) {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"????????????????????????,??????????????????"});
        }
        //2.????????????id??????????????????
        String prodId = (String) requestParam.get(Dict.PROD_ID);
        ProdApplication prodApplication = prodApplicationService.queryApplication(prodId, applicationId);//???????????????
        ProdRecord prodRecord = prodRecordService.queryDetail(prodId);//???????????????
        Map<String, Object> application = appService.queryAPPbyid(applicationId);//??????????????????
        String proImageUri = (String) requestParam.get(Dict.PRO_IMAGE_URI);
        String proSccImageUri = (String) requestParam.get(Dict.PRO_SCC_IMAGE_URI);
        
        List<String> prodDirList = prodApplication.getProd_dir();
        Map<String,Object> changeMap = prodApplication.getChange();
        if(!Util.isNullOrEmpty(prodDirList)) {
			// ?????????????????????docker_yaml??????scc_yaml?????????auto-config???????????????fdev?????????yaml??????
            if (prodDirList.contains(Dict.DOCKER_YAML)) {
                if (!"1".equals(prodApplication.getCaas_add_sign())) {
                    prodApplicationService.getCaasNewConfig(prodDirList, applicationId, prodRecord, prodApplication, proImageUri, changeMap);
                }
            }
            if (prodDirList.contains(Dict.SCC_YAML)) {
                if (!"1".equals(prodApplication.getScc_add_sign())) {
                    prodApplicationService.getSccNewConfig(prodDirList, applicationId, prodRecord, prodApplication, proSccImageUri, changeMap);
                }
            }
        }
        
        //3.????????????
        if (!CommonUtils.isNullOrEmpty(proImageUri)) {//caas
            if (!prodApplication.getProd_dir().contains(Dict.DOCKER_LOWERCASE)
                    && !prodApplication.getProd_dir().contains(Dict.DOCKER_YAML)) {
                logger.info("?????????????????????docker???docker_yaml??????");
            } else {
                logger.info("push caas image");
                push(prodApplication, prodRecord, application, requestParam, Dict.CAAS);
            }
        }
        if (!CommonUtils.isNullOrEmpty(proSccImageUri)) {//scc
            if (!prodApplication.getProd_dir().contains(Dict.SCC)
                    && !prodApplication.getProd_dir().contains(Dict.SCC_YAML)) {
                logger.info("?????????????????????scc???scc_yaml??????");
            } else {
                logger.info("push scc image");
                push(prodApplication, prodRecord, application, requestParam, Dict.SCC);
            }
        }
        
        //5.??????prodApplication????????????
        return record(prodRecord, prodApplication, application, requestParam);

    }

    private void syncAwsProdasset(String proImageUri, String proSccImageUri, String applicationId, String prodId, ProdRecord prodRecord) throws Exception {
        List<String> envList = Arrays.asList("DEV","PROC");
        for(String env: envList){
                String tag = "" ;
                if(!CommonUtils.isNullOrEmpty(proImageUri)){
                    tag = proImageUri.split(":")[1];
                }else{
                    tag = proSccImageUri.split(":")[1];
                }
                ReleaseApplication releaseApplication = releaseApplicationDao.queryAppByIdAndTag(applicationId, tag);
                Map<String,String> staticResourceMap = releaseApplication.getStatic_resource();
                if(CommonUtils.isNullOrEmpty(staticResourceMap) || CommonUtils.isNullOrEmpty(staticResourceMap.get(tag))){
                    return;
                }
                String path = staticResourceMap.get(tag);
                String[] git_url = path.split(";");
                String filename = git_url[0].substring(git_url[0].lastIndexOf("/")+1);
                ProdAsset prodAsset = new ProdAsset();
                prodAsset.setProd_id(prodId);
                prodAsset.setAsset_catalog_name("AWS_STATIC");
                prodAsset.setRuntime_env(env);
                prodAsset.setBucket_name(bucketName);
                String type = prodRecord.getType();
                if("proc".equals(type)) {
                    type = "prd";
                }
                prodAsset.setBucket_path(type);
                prodAsset.setRelease_node_name(prodRecord.getRelease_node_name());
                prodAsset.setSource("1");
                prodAsset.setAws_type("0"); // ????????????????????????????????????
                prodAsset.setSource_application(releaseApplication.getApplication_id());
                String upload_time = CommonUtils.formatDate("yyyy-MM-dd HH:mm:ss");
                prodAsset.setUpload_user(CommonUtils.getSessionUser().getId());
                prodAsset.setUpload_time(upload_time);
                prodAsset.setFileName(filename);
                // ????????????????????????mio????????????????????????
                for(String gitUrl: git_url){
                    if(gitUrl.contains(env) && gitUrl.contains(type)){
                        prodAsset.setFile_giturl(gitUrl);
                    }
                }
                ProdAsset result = prodAssetDao.queryAwsAsset(prodRecord.getProd_id(), "AWS_STATIC", applicationId, env, "0", bucketName, type);
                logger.info("query from ci aws prodasset");
                if(CommonUtils.isNullOrEmpty(result)){
                    prodAssetDao.save(prodAsset);
                    logger.info("sync aws_static from releaseApplication to prodAsset");
                }else{
                    prodAssetDao.updateAsset(prodAsset);
                    logger.info("update prodAsset");
                }

        }

    }

    /**
     * ????????????
     *
     * @param prodApplication
     * @param prodRecord
     * @param application
     * @param requestParam
     * @param type
     * @throws Exception
     */
    private void push(ProdApplication prodApplication, ProdRecord prodRecord, Map<String, Object> application, Map<String, Object> requestParam, String type) throws Exception {
        // ????????????id??????????????????
        String proImageUri;//?????????????????????
        String prodAppImageUri;//?????????????????????????????????
        AutomationParam ip;//?????????????????????????????????
        String prodId = (String) requestParam.get(Dict.PROD_ID);
        if (Dict.CAAS.equals(type)) {
            proImageUri = (String) requestParam.get(Dict.PRO_IMAGE_URI);
            prodAppImageUri = prodApplication.getPro_image_uri();
            ip = automationParamService.queryByKey("autorelease.docker.push.registry");
        } else {
            proImageUri = (String) requestParam.get(Dict.PRO_SCC_IMAGE_URI);
            prodAppImageUri = prodApplication.getPro_scc_image_uri();
            ip = automationParamService.queryByKey("autorelease.docker.push.scc.registry");
        }
        // ?????????????????????
        String newTag = proImageUri.split(":")[1];
        boolean docker = true;
        String applicationId = (String) requestParam.get(Dict.APPLICATION_ID);
        if ("proc".equals(prodRecord.getType())) {
            if (Constants.APPLICATION_DOCKER_STOP_START_ALL.equals(prodApplication.getRelease_type())) {
                RelDevopsRecord relDevopsRecord = new RelDevopsRecord();
                relDevopsRecord.setApplication_id(applicationId);
                relDevopsRecord.setRelease_node_name(prodRecord.getRelease_node_name());
                docker = relDevopsRecordService.queryDockerDir(relDevopsRecord, prodRecord, prodAppImageUri);
            } else if (Constants.APPLICATION_DOCKER_RESTART.equals(prodApplication.getRelease_type())) {
                docker = false;
            }
        }
        String fakeTag = newTag + "-" + Dict.FAKETIME;
        String fakeUri = "";
        RelDevopsRecord relDevopsRecordFake = relDevopsRecordService.findAppByTagAndAppid(applicationId, fakeTag);
        if (!CommonUtils.isNullOrEmpty(relDevopsRecordFake)) {
            if (!CommonUtils.isNullOrEmpty(relDevopsRecordFake.getPro_image_uri())) {
                fakeUri = relDevopsRecordFake.getPro_image_uri();
            } else {
                fakeUri = relDevopsRecordFake.getPro_scc_image_uri();
            }
        }
        requestParam.put("newTag", newTag);
        requestParam.put("fakeUri", fakeUri);
        Map<String, String> prodImages = prodRecordService.queryBeforePordImages(prodRecord.getRelease_node_name(), prodRecord.getVersion());
        // ??????????????????????????????????????????docker_restart?????????????????????????????????????????????
        if ((CommonUtils.isNullOrEmpty(prodImages) || CommonUtils.isNullOrEmpty(prodImages.get(proImageUri)))
                && !Constants.APPLICATION_DOCKER_RESTART.equals(prodApplication.getRelease_type()) && docker) {
            Map<String,Set<String>> namespaceMap = prodApplicationService.queryVarByLabelAndType((Integer) application.get(Dict.GITLAB_PROJECT_ID));
            if(CommonUtils.isNullOrEmpty(namespaceMap)){
                throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????deploy??????????????????????????????????????????????????????????????????"});
            }
            Set<String> namespaces = namespaceMap.get(type);
            if (namespaces.size() == 0) {
                throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????deploy??????????????????????????????????????????????????????????????????"});
            }
            applicationImageService.deleteByDeployType(prodId, applicationId,type);
            String logPath = "/fdev/log/push_docker_image/" + prodRecord.getRelease_node_name() + Constants.SPLIT_STRING;
            String logFileName = prodRecord.getVersion() + "_" + application.get(Dict.NAME_EN) + "_"
                    + CommonUtils.formatDate("yyyyMMdd:HH:mm:ss") + ".log";
            for (String space : namespaces) {
                String[] proImageArr = proImageUri.split("/");
                String imageUri = ip.getValue() + "/" + space + "/" + proImageArr[2];
                ObjectId objectId = new ObjectId();
                ApplicationImage applicationImage = new ApplicationImage(objectId, objectId.toString(),
                        applicationId, (String) application.get(Dict.NAME_EN), prodId, prodRecord.getVersion(),
                        prodRecord.getRelease_node_name(), space, imageUri, "0",
                        logPath + logFileName, imageUri + "???????????????????????????", TimeUtils.formatDate("yyyy-MM-dd HH:mm:ss"), "",type);
                applicationImageService.save(applicationImage);
            }
            RelDevopsRecord tempRecord = relDevopsRecordService.findAppByTagAndAppid(applicationId,newTag);
            String caasEnv = null;
            String sccEnv = null;
            if(!CommonUtils.isNullOrEmpty(tempRecord)){
                caasEnv = tempRecord.getCaas_env();
                sccEnv = tempRecord.getScc_env();
            }
            // ??????????????????
            asyncAutoReleaseService.pushDockerImage((Integer) application.get(Dict.GITLAB_PROJECT_ID),
                    (String) application.get(Dict.NAME_EN), proImageUri, fakeUri, prodRecord.getRelease_node_name(),
                    prodRecord.getVersion(), prodId, applicationId, logPath, logFileName, type,caasEnv,sccEnv);
        }
        if (!CommonUtils.isNullOrEmpty(prodApplication) && proImageUri.equals(prodAppImageUri)) {
            return;
        }
    }

    /**
     * ??????prodApplication????????????
     *
     * @param prodRecord
     * @param prodApplication
     * @param application
     * @param requestParam
     * @return
     * @throws Exception
     */
    private Object record(ProdRecord prodRecord, ProdApplication prodApplication, Map<String, Object> application, Map<String, Object> requestParam) throws Exception {
        String tips = null;
        String fdevConfigConfirm = null;
        boolean fdevConfigChanged = false;
        String compareUrl = "";
        String applicationId = (String) requestParam.get(Dict.APPLICATION_ID);
        String prodId = (String) requestParam.get(Dict.PROD_ID);
        String userId = CommonUtils.getSessionUser().getId();
        String proImageUri = (String) requestParam.get(Dict.PRO_IMAGE_URI);
        String proSccImageUri = (String) requestParam.get(Dict.PRO_SCC_IMAGE_URI);
        ReleaseApplication releaseApplication = releaseApplicationService.findOneReleaseApplication(applicationId,
                prodRecord.getRelease_node_name());
        if (!Constants.APPLICATION_DOCKER_STOP_START_ALL.equals(prodApplication.getRelease_type())
                && !CommonUtils.isNullOrEmpty(releaseApplication.getFdev_config_confirm())
                && !"1".equals(releaseApplication.getFdev_config_confirm())) {
            throw new FdevException(ErrorConstants.CONFIG_FILE_WITHOUT_AUDIT);
        }
        // ????????????????????????????????????????????????????????????????????????????????????seq_no??????????????????
        List<ProdAsset> prodAssetList = prodAssetDao.findAssetByAppAndProd(applicationId, prodId);
        if(!CommonUtils.isNullOrEmpty(prodAssetList)){
            for(ProdAsset prodAsset: prodAssetList){
                reSortProdAsset(prodAsset);
            }
        }
        prodAssetService.delCommonConfigByAppAndProd(applicationId, prodId);
        // ?????????????????????gitlab???id????????????
        if (!CommonUtils.isNullOrEmpty(application) && !CommonUtils.isNullOrEmpty(application.get(Dict.GITLAB_PROJECT_ID))) {
            // ????????????gitlab???id????????????????????????????????????
            String newTag = (String) requestParam.get("newTag");
            Map<String, Object> configGitlab = prodApplicationService.findConfigByGitlab(String.valueOf(application.get(Dict.GITLAB_PROJECT_ID)));
            String configGitlabId = prodApplicationService.checkEnvConfig(configGitlab, newTag);
            // ??????????????????????????????
            if (!CommonUtils.isNullOrEmpty(configGitlabId)) {
                String webUrl = gitlabService.findWebUrlByGitlabId(configGitlabId);
                //???????????????????????????????????????????????????
                Set<String> set = gitlabService.queryResourceFiles(configGitlabId, newTag);
                String network = (String) application.get(Dict.NETWORK);
                List<String> standardEnvList;
                Map<String, String> standardEnvMapping;
                if (!CommonUtils.isNullOrEmpty(network) && !network.contains("dmz")) {
                    standardEnvList = (List<String>) (Dict.GRAY.equals(prodRecord.getType()) ? requestParam.get("bizGrayStatus") : requestParam.get("bizProductStatus"));
                    standardEnvMapping = (Map<String, String>) (Dict.GRAY.equals(prodRecord.getType()) ?
                            requestParam.get("bizEnvGrayMapping") : requestParam.get("bizEnvProductMapping"));
                } else {
                    standardEnvList = (List<String>) (Dict.GRAY.equals(prodRecord.getType()) ? requestParam.get("dmzGrayStatus") : requestParam.get("dmzProductStatus"));
                    standardEnvMapping = (Map<String, String>) (Dict.GRAY.equals(prodRecord.getType()) ?
                            requestParam.get("dmzEnvGrayMapping") : requestParam.get("dmzEnvProductMapping"));
                }
                String application_name_en = (String) application.get(Dict.NAME_EN);
                if ((Dict.SPDB_CLI_MOBCLI.equals(application_name_en) || application_name_en.startsWith(Dict.MSPMK_CLI_))
                        && !CommonUtils.isNullOrEmpty(proImageUri)) {
                    if (set.size() == 0) {
                        fdevConfigConfirm = "1";
                    } else {
                        compareUrl = webUrl + "/tree/" + newTag;
                        // ??????????????????
                        for (String branches : set) {
                            // ????????????md5??????
                            if (Dict.CLI_MD5.equals(branches.split("/")[0])) {
                                fdevConfigConfirm = "0";
                                fdevConfigChanged = true;
//                                ProdAsset prodAsset = new ProdAsset(webUrl + "/blob/" + newTag + "/" + branches,
//                                        branches.substring(branches.lastIndexOf("/") + 1),
//                                        prodRecord.getRelease_node_name(), Dict.COMMONCONFIG, Constants.SOURCE_FDEV,
//                                        userId, prodId, applicationId, (String) application.get(Dict.NAME_ZH),
//                                        configGitlabId);
//                                // ??????md5????????????????????????????????????
//                                for (String env : standardEnvMapping.values()) {
//                                    prodAsset.setRuntime_env(env);
//                                    prodAssetService.addGitlabAsset(prodAsset);
//                                }
                            }
                        }
                    }
                } else {
                    // ?????????????????????uri
                    String oldTag = prodApplicationService.findLatestUri(applicationId, prodRecord,Dict.CAAS);
                    if(CommonUtils.isNullOrEmpty(oldTag)){
                        oldTag = prodApplicationService.findLatestUri(applicationId, prodRecord,Dict.SCC);
                    }
                    Map<String, Object> differs = new HashMap<>();
                    //?????????????????????????????????uri
                    if (!CommonUtils.isNullOrEmpty(oldTag) && oldTag.contains("pro")) {
                        if (oldTag.contains(":")) {
                            // ????????????????????????????????????
                            oldTag = oldTag.split(":")[1];
                        }
                        // ????????????????????????
                        differs = gitlabService.compareBranches(configGitlabId, oldTag, newTag);
                    }
                    List diffList = (List) differs.get(Dict.DIFFS);
                    List diffs = new ArrayList<>();
                    if (!CommonUtils.isNullOrEmpty(diffList)) {
                    	for (Iterator iterator = diffList.iterator(); iterator.hasNext();) {
    						Map<String,Object> diffMap = (Map<String,Object>) iterator.next();
    						String filePath = (String) diffMap.get("new_path");
    						if(!filePath.endsWith(".properties")) {
    							continue;
    						}else {
    							diffs.add(diffMap);
    						}
    					}
					}
                    // ?????????????????????????????????????????????????????????
                    if (CommonUtils.isNullOrEmpty(oldTag) || !CommonUtils.isNullOrEmpty(diffs)) {
                        List<String> envList = new ArrayList<>();
                        for (String envSingle : set) {
                            envList.add(envSingle.split("/")[0]);
                        }
                        if (!envList.containsAll(standardEnvList)) {
                            StringBuilder sb = new StringBuilder();
                            for (String env : standardEnvList) {
                                if (!envList.contains(env)) {
                                    sb.append(env).append("???");
                                }
                            }
                            String lackEnv = sb.toString();
                            if (lackEnv.endsWith("???")) {
                                lackEnv = lackEnv.substring(0, lackEnv.length() - 1);
                            }
                            if ("PROCHF".equals(standardEnvMapping.get(lackEnv))) {
                                // ????????????PROCHF????????????????????????????????????
                                tips = "????????????????????????????????????commonfig??????PROCHF????????????????????????" + lackEnv + "????????????????????????";
                            } else {
                                if(!CommonUtils.isNullOrEmpty(requestParam.get(Dict.PRO_IMAGE_URI))) {
                                    // ??????????????????????????????
                                    throw new FdevException(ErrorConstants.RELEASE_LACK_FDEV_CONFIG,
                                            new String[]{lackEnv, webUrl + "/tree/" + newTag});
                                }
                            }
                        }
                        if (!CommonUtils.isNullOrEmpty(differs)) {
                            compareUrl = webUrl + differs.get(Dict.COMPAREBRANCHES_URL);
                        } else {
                            compareUrl = webUrl + "/tree/" + newTag;
                        }
                        fdevConfigConfirm = "0";
                        fdevConfigChanged = true;
                        Map<String,String> sccEnvMap;
                        if(Dict.GRAY.equals(prodRecord.getType())){
                            sccEnvMap = JSONObject.fromObject(this.sccGrayEnv);
                        }else{
                            sccEnvMap = JSONObject.fromObject(this.sccProductEnv);
                        }
                        for (String branchFile : set) {
                            String env = branchFile.split("/")[0];
                            if (CommonUtils.isNullOrEmpty(standardEnvMapping.get(env)) && CommonUtils.isNullOrEmpty(sccEnvMap.get(env)) || !branchFile.endsWith(".properties")) {
                                continue;
                            }
                            ProdAsset prodAsset = new ProdAsset(webUrl + "/blob/" + newTag + "/" + branchFile,
                                    branchFile.substring(branchFile.lastIndexOf("/") + 1),
                                    prodRecord.getRelease_node_name(), Dict.COMMONCONFIG, Constants.SOURCE_FDEV,
                                    userId, prodId, applicationId, (String) application.get(Dict.NAME_ZH),
                                    configGitlabId);
                            if(!CommonUtils.isNullOrEmpty(proImageUri)) {
                                //??????caas??????????????????
                                if(!CommonUtils.isNullOrEmpty(standardEnvMapping.get(env))) {
                                    prodAsset.setRuntime_env(standardEnvMapping.get(env));
                                    prodAssetService.addGitlabAsset(prodAsset);
                                }
                            }
                            if(!CommonUtils.isNullOrEmpty(proSccImageUri)) {
                                //??????scc??????????????????
                                if(!CommonUtils.isNullOrEmpty(sccEnvMap.get(env))){
                                    prodAsset.setRuntime_env(sccEnvMap.get(env));
                                    prodAssetService.addGitlabAsset(prodAsset);
                                }
                            }
                        }
                    } else {
                        fdevConfigConfirm = "1";
                    }
                }
            }
        }
        String fakeUri = (String) requestParam.get("fakeUri");
        String newTag = (String) requestParam.get("newTag");
        if (CommonUtils.isNullOrEmpty(fdevConfigConfirm)) {
            // ????????????
            prodApplicationService.setImageUri(prodId, applicationId, proImageUri, proSccImageUri, fakeUri);
            prodAssetService.uploadRouter(userId, prodRecord, "config", applicationId,
                    (String) application.get(Dict.NAME_ZH), (String) application.get(Dict.NAME_EN), releaseApplication, newTag);
            // ???????????????????????????config??????
            if (!CommonUtils.isNullOrEmpty(releaseApplication) && !CommonUtils.isNullOrEmpty(releaseApplication.getPath())
                    && !CommonUtils.isNullOrEmpty(releaseApplication.getPath().get(newTag))) {
                prodAssetService.uploadSourceMap(userId, prodRecord, "config", applicationId,
                        (String) application.get(Dict.NAME_ZH), releaseApplication, newTag);
            }
            prodApplication = new ProdApplication(prodId, applicationId, proImageUri,proSccImageUri);
        } else {
            //??????TAG??????(??????????????????tag)
            String tag;
            if("1".equals(prodApplication.getEsf_flag())){
                tag = prodApplication.getTag();
            }else{
                if(CommonUtils.isNullOrEmpty(proImageUri)){
                    tag = proSccImageUri.split(":")[1];
                }else {
                    tag = proImageUri.split(":")[1];
                }
            }
            prodApplication = prodApplicationService.setImageConfigUri(prodId, applicationId, proImageUri,proSccImageUri,
                    fdevConfigChanged, compareUrl, fdevConfigConfirm, fakeUri, tag);
        }
        Map<String, Object> returnMap = CommonUtils.beanToMap(prodApplication);
        returnMap.put(Dict.APPLICATION_TIPS, tips);
        // ???????????????????????????????????????????????????sql??????
        String name_en = (String) application.get(Dict.NAME_EN);
        if("1".equals(prodRecord.getScc_prod()) && name_en.startsWith(Dict.MSPMK_CLI_)){
            // ???????????????????????????????????????????????????????????????????????????????????????????????????mio??????
            syncAwsProdasset(proImageUri, proSccImageUri, applicationId, prodId, prodRecord);
            // ?????????????????????????????????????????????????????????????????????sql?????????prodAsset????????????dbbef_mbank???????????????
            syncSqltoProdasset(proImageUri, proSccImageUri,applicationId, prodId, prodRecord, (String) application.get(Dict.NAME_EN));
        }
        return returnMap;
    }

    private void reSortProdAsset(ProdAsset prodAsset) throws Exception {
        String DelSeqNo = prodAsset.getSeq_no();
        String asset_catalog_name = prodAsset.getAsset_catalog_name();
        prodAssetService.deleteAsset(prodAsset);
        List<ProdAsset> seqNo = prodAssetService.queryAssetsList(prodAsset.getProd_id());
        for (ProdAsset asset : seqNo) {
            if (!CommonUtils.isNullOrEmpty(asset.getSeq_no()) && asset_catalog_name.equals(asset.getAsset_catalog_name())) {
                if (Integer.parseInt(DelSeqNo) < Integer.parseInt(asset.getSeq_no())) {
                    asset.setSeq_no((Integer.parseInt(asset.getSeq_no()) - 1) + "");
                    prodAssetService.updateAssetSeqNo(asset);
                }
            }
        }
    }

    /**
     * ??????release_application????????????tag?????????sql???prod_asset??????
     * */
    private void syncSqltoProdasset(String proImageUri, String proSccImageUri,String applicationId, String prodId, ProdRecord prodRecord,String application_cn) throws Exception {
        String tag = null ;
        if(!CommonUtils.isNullOrEmpty(proImageUri)){
            tag = proImageUri.split(":")[1];
        }else{
            tag = proSccImageUri.split(":")[1];
        }
        ReleaseApplication releaseApplication = releaseApplicationDao.queryAppByIdAndTag(applicationId, tag);
        // ????????????id????????????????????????????????????seq_no??????
        List<ProdAsset> prodAssetList = prodAssetDao.queryAssetsWithSeqnoDesc(prodId, "dbbef_mbank");
        if(!CommonUtils.isNullOrEmpty(releaseApplication.getApp_sql())){
            Map<String, Object> staticSqlMap = releaseApplication.getApp_sql();
            if(CommonUtils.isNullOrEmpty(staticSqlMap) || CommonUtils.isNullOrEmpty(staticSqlMap.get(tag))){
                return;
            }
            Map<String,Object> sqlMap = (Map<String, Object>) staticSqlMap.get(tag);
            int count = 0;
            if(!CommonUtils.isNullOrEmpty(prodAssetList)){
                count = Integer.parseInt(prodAssetList.get(0).getSeq_no());
            }
            for(Map.Entry<String,Object> entry: sqlMap.entrySet())
            {
                ProdAsset prodAsset = new ProdAsset();
                String key = entry.getKey(); // InsertSqlGray InsertSqlPro RevertSqlGray RevertSqlPro
                String value = (String) entry.getValue();
                // ??????????????????????????? appEn_pro_insert.sql????????????mio
                String filePath = "/frelease/static/sql";
                String[] sql_arr = key.split("Sql");
                String type = sql_arr[1].toLowerCase();
                if("pro".equals(type)){
                    type = "proc";
                }
                // ebank_dbmbank_insert_mbs_pubresource_info_xxx(???????????????)_gray.sql
                // ebank_dbmbank_insert_mbs_pubresource_info_xxx(???????????????)_gray_rollback.sql
                String fileName = null;
                if(type.equals(prodRecord.getType())){
                    count++;
                    String prefix = "ebank_dbmbank_insert_mbs_pubresource_info_";
                    String prod_type = sql_arr[1].toLowerCase();
                    if("pro".equals(prod_type)){
                        prod_type = "prd";
                    }
                    String sql_type = ".sql";
                    if("Revert".equals(key.split("Sql")[0])){
                        sql_type = "_rollback.sql";
                    }
                    fileName = prefix + application_cn.substring(application_cn.lastIndexOf("-") + 1) + "_" + prod_type + sql_type;
                    // ?????????????????????
                    createFile(key,value,filePath, fileName);
                    MultipartFile f;
                    try {
                        f = new MockMultipartFile(fileName, fileName, ContentType.APPLICATION_OCTET_STREAM.toString(), new FileInputStream(new File(filePath + "/" + fileName)));
                    } catch (IOException e) {
                        throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{fileName + "??????????????????"});
                    }
                    String minioPath = recordAssetsUrl + prodRecord.getRelease_node_name() + "/" + prodRecord.getVersion() + "/dbbef_mbank" + "/" + fileName;
                    //????????????
                    try {
                        fileService.uploadFiles(minioPath, f, fileName, "fdev-release");
                    } catch (Exception e) {
                        throw new FdevException(ErrorConstants.THIRD_SERVER_ERROR, new String[]{fileName + "????????????minio??????"});
                    }
                    // ???prod_asset????????????????????????
                    prodAsset.setFileName(fileName);
                    prodAsset.setFile_giturl(minioPath);
                    prodAsset.setProd_id(prodId);
                    prodAsset.setAsset_catalog_name("dbbef_mbank");
                    prodAsset.setRelease_node_name(prodRecord.getRelease_node_name());
                    prodAsset.setSource("1");
                    prodAsset.setSeq_no(String.valueOf(count));
                    prodAsset.setSource_application(releaseApplication.getApplication_id());
                    String upload_time = CommonUtils.formatDate("yyyy-MM-dd HH:mm:ss");
                    prodAsset.setUpload_user(CommonUtils.getSessionUser().getId());
                    prodAsset.setUpload_time(upload_time);
                    /*if("Insert".equals(key.split("Sql")[0])){
                        prodAsset.setWrite_flag("0");
                    }*/
                    prodAssetDao.save(prodAsset);
                }
            }
        }
    }

    private void createFile(String key, Object value, String filePath, String fileName) throws IOException {
        File dir = new File(filePath);
        if(!dir.exists()){
            dir.mkdirs();
        }
        File checkFile = new File(filePath + "/" + fileName);
        FileWriter fw = null;
        try {
            if(!checkFile.exists()){
                checkFile.createNewFile();
            }
            fw = new FileWriter(checkFile,false);
            fw.append((String)value);
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if( null != fw ){
                fw.close();
            }
        }
    }
    
    private void getNewConfig(List<String> prod_dir, String prd_id, String application_id, String prod_type, String tag, String deployName, String lastCaasTag, String lastSccTag, ProdApplication prodApplication) throws Exception {
        //??????????????????
        Map<String, Object> application = appService.queryAPPbyid(application_id);
        //????????????????????????
        Map<String, Object> configGitlab = findConfigByGitlab(String.valueOf(application.get(Dict.GITLAB_PROJECT_ID)));
        if(prod_dir.contains(Dict.DOCKER_YAML) && !Util.isNullOrEmpty(lastCaasTag)){
            List<Map<String, Object>> yamls;
            try {
                yamls = appService.getNewYaml(deployName, tag);
            } catch (Exception e) {
                throw new FdevException(ErrorConstants.THIRD_SERVER_ERROR, new String[]{e.getMessage()});
            }
            if (!CommonUtils.isNullOrEmpty(yamls)) {
                Map<String, Object> changeMap = new HashMap();
                Map changeTemp = new HashMap();
                for (Map<String, Object> map : yamls) {
                    String tenant = (String) map.get("cluster");
                    String namespace = (String) map.get("namespace");
                    ProdRecord prodRecord = prodRecordService.queryDetail(prd_id);
                    String type = namespace.contains(Dict.GRAY) ? Dict.GRAY : "proc";
                    if (type.equals(prodRecord.getType())) {
                        StringBuffer sb = new StringBuffer();
                        sb.append(tenant, 0, 2).append("-").append(tenant, 2, 4);
                        List<String> bizs = Arrays.asList(fdevBiz.split(","));
                        List<String> dmzs = Arrays.asList(fdevDmz.split(","));
                        if (bizs.contains(namespace)) {
                            sb.append("-").append("biz");
                        } else if (dmzs.contains(namespace)) {
                            sb.append("-").append("dmz");
                        } else {
                            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{namespace + "????????????"});
                        }
                        if (namespace.contains(Dict.GRAY)) {
                            sb.append("-").append(Dict.GRAY);
                        }
                        sb.append("/").append(deployName.split("-")[0]).append("/").append(deployName).append(".yaml");
                        logger.info("????????????yaml??????????????????{}", deployName + "/" + tag);
                        Yaml yaml = new Yaml();
                        String yamlStr = yaml.dumpAsMap(map.get("yaml")).replaceAll("'######'", "");
                        appService.updateGitFile(String.valueOf(configGitlab.get(Dict.CONFIGGITLABID)),
                                URLEncoder.encode(sb.toString(), "UTF-8"), Dict.MASTER, yamlStr,
                                "??????tag???????????????", Token);
                        logger.info("??????yaml????????????");
                        String key = "/PROC/" + tenant.toUpperCase();
                        Map<String, Object> change = (Map<String, Object>) map.get(Dict.CHANGE);
                        if (CommonUtils.isNullOrEmpty(change)) {
                            change = new HashMap<>();
                        }
                        if(!CommonUtils.isNullOrEmpty(prodApplication) && !CommonUtils.isNullOrEmpty(prodApplication.getChange())){
                            changeMap = prodApplication.getChange();
                        }
                        if (!CommonUtils.isNullOrEmpty(changeMap)) {
                            Map<String, Object> envMap = (Map<String, Object>) changeMap.get(key);
                            if (!CommonUtils.isNullOrEmpty(envMap)) {
                                change.putAll(envMap);
                            }
                        }
                        changeMap.put(key, change);
                        changeTemp.putAll(change);
                    }
                }
                // ??????????????????docker_startall???scc_startall??????DEV,TEST,TCYZ??????????????????????????????????????????????????????????????????????????????????????????1???
                if(!prod_dir.contains(Dict.DOCKER_STARTALL) && !prod_dir.contains(Dict.SCC_STARTALL)){
                    changeMap.put("/DEV/DEV", changeTemp);
                    changeMap.put("/TEST/TEST", changeTemp);
                    changeMap.put("/TCYZ/TCYZ", changeTemp);
                }
                prodApplicationService.updateProdChange(prd_id, application_id, changeMap);
            }
        }
        if(prod_dir.contains(Dict.SCC_YAML) && !Util.isNullOrEmpty(lastSccTag)){
            // ???????????????scc??????????????????????????????gitlab??????yaml
            List<Map<String, Object>> scc_yamls;
            try {
                scc_yamls = appService.getSccNewYaml(deployName, tag);
            } catch (Exception e) {
                throw new FdevException(ErrorConstants.THIRD_SERVER_ERROR, new String[]{e.getMessage()});
            }
            if(!CommonUtils.isNullOrEmpty(scc_yamls)){
                for (Map<String, Object> scc_map : scc_yamls) {
                    String namespace = (String) scc_map.get("namespace_code"); //??????
                    // ??????????????????fdev???????????????auto-config?????????yaml??????
                    List<AutomationEnv> automationEnvList = automationEnvService.query();
                    StringBuffer sb = new StringBuffer();
                    boolean flag = false;
                    for (AutomationEnv automationEnv: automationEnvList){
                        Map<String,Object> scc_fdev_map = (Map<String, Object>) automationEnv.getScc_fdev_env_name().get(prod_type); // scc???fdev??????
                        Map<String,Object> scc_namespace_map = (Map<String, Object>) automationEnv.getScc_namespace().get(prod_type); // scc?????????
                        String scc_dmz_namespace = (String) scc_namespace_map.get("dmz"); // ???????????????????????????
                        String scc_biz_namespace = (String) scc_namespace_map.get("biz"); // ???????????????????????????
                        if(namespace.equals(scc_dmz_namespace)){
                            flag = true;
                            sb.append(scc_fdev_map.get("dmz")); // ?????????????????????fdev??????
                            break;
                        }else if(namespace.equals(scc_biz_namespace)){
                            flag = true;
                            sb.append(scc_fdev_map.get("biz")); // ?????????????????????fdev??????
                            break;
                        }else{
                            continue;
                        }
                    }
                    if(flag){
                        sb.append("/").append(deployName.split("-")[0]).append("/").append(deployName).append("-scc.yaml");
                        logger.info("????????????scc yaml??????????????????{}", deployName + "/" + tag);
                        Yaml yaml = new Yaml();
                        String yamlStr = yaml.dumpAsMap(scc_map.get("yaml")).replaceAll("'######'", "");
                        appService.updateGitFile(String.valueOf(configGitlab.get(Dict.CONFIGGITLABID)),
                                URLEncoder.encode(sb.toString(), "UTF-8"), Dict.MASTER, yamlStr,
                                "??????tag???????????????", Token);
                        logger.info("??????scc yaml????????????");
                    }
                }
            }
        }
        logger.info("????????????tag????????????{}", tag);
        gitlabService.deleteTag((Integer) configGitlab.get(Dict.CONFIGGITLABID), tag);
        logger.info("??????tag??????");

        logger.info("??????????????????tag????????????{}", tag);
        gitlabService.createTag(String.valueOf(configGitlab.get(Dict.CONFIGGITLABID)), tag, Dict.MASTER, Token);
        logger.info("????????????tag??????");

    }
    
    @Override
	public void queryProfile(Map<String, Object> requestParam) throws Exception {
		String tips = null;
        String compareUrl = "";
		String applicationId = (String) requestParam.get(Dict.APPLICATION_ID);
        String prodId = (String) requestParam.get(Dict.PROD_ID);
        String proImageUri = (String) requestParam.get(Dict.PRO_IMAGE_URI);
        String proSccImageUri = (String) requestParam.get(Dict.PRO_SCC_IMAGE_URI);
        String newTag = (String) requestParam.get(Dict.TAG);
        String userId = CommonUtils.getSessionUser().getId();
        ProdApplication prodApplication = prodApplicationService.queryApplication(prodId, applicationId);//???????????????
        ProdRecord prodRecord = prodRecordService.queryDetail(prodId);//???????????????
        Map<String, Object> application = appService.queryAPPbyid(applicationId);//??????????????????
        if (!CommonUtils.isNullOrEmpty(application) && !CommonUtils.isNullOrEmpty(application.get(Dict.GITLAB_PROJECT_ID))) {
            // ????????????gitlab???id????????????????????????????????????
            Map<String, Object> configGitlab = prodApplicationService.findConfigByGitlab(String.valueOf(application.get(Dict.GITLAB_PROJECT_ID)));
            String configGitlabId = prodApplicationService.checkEnvConfig(configGitlab, newTag);
            // ??????????????????????????????
            if (!CommonUtils.isNullOrEmpty(configGitlabId)) {
                String webUrl = gitlabService.findWebUrlByGitlabId(configGitlabId);
                //???????????????????????????????????????????????????
                Set<String> set = gitlabService.queryResourceFiles(configGitlabId, newTag);
                boolean flag = false;
                for (String string : set) {
                	if (string.endsWith(".properties")) {
						flag = true;
					}
				}
                if (!flag) {
                	throw new FdevException("???????????????properties????????????");
				}
                String network = (String) application.get(Dict.NETWORK);
                List<String> standardEnvList;
                Map<String, String> standardEnvMapping;
                if (!CommonUtils.isNullOrEmpty(network) && !network.contains("dmz")) {
                    standardEnvList = (List<String>) (Dict.GRAY.equals(prodRecord.getType()) ? requestParam.get("bizGrayStatus") : requestParam.get("bizProductStatus"));
                    standardEnvMapping = (Map<String, String>) (Dict.GRAY.equals(prodRecord.getType()) ?
                            requestParam.get("bizEnvGrayMapping") : requestParam.get("bizEnvProductMapping"));
                } else {
                    standardEnvList = (List<String>) (Dict.GRAY.equals(prodRecord.getType()) ? requestParam.get("dmzGrayStatus") : requestParam.get("dmzProductStatus"));
                    standardEnvMapping = (Map<String, String>) (Dict.GRAY.equals(prodRecord.getType()) ?
                            requestParam.get("dmzEnvGrayMapping") : requestParam.get("dmzEnvProductMapping"));
                }
                String application_name_en = (String) application.get(Dict.NAME_EN);
                // ?????????????????????uri
                String oldTag = prodApplicationService.findLatestUri(applicationId, prodRecord,Dict.CAAS);
                if(CommonUtils.isNullOrEmpty(oldTag)){
                    oldTag = prodApplicationService.findLatestUri(applicationId, prodRecord,Dict.SCC);
                }
                Map<String, Object> differs = new HashMap<>();
                //?????????????????????????????????uri
                if (!CommonUtils.isNullOrEmpty(oldTag) && oldTag.contains("pro")) {
                    if (oldTag.contains(":")) {
                        // ????????????????????????????????????
                        oldTag = oldTag.split(":")[1];
                    }
                    // ????????????????????????
                    differs = gitlabService.compareBranches(configGitlabId, oldTag, newTag);
                }
                // ?????????????????????????????????????????????????????????
                if (CommonUtils.isNullOrEmpty(oldTag) || !CommonUtils.isNullOrEmpty(differs.get(Dict.DIFFS))) {
                	prodAssetService.delCommonConfigByAssetCatalogName(applicationId, prodId, Dict.COMMONCONFIG);
                    List<String> envList = new ArrayList<>();
                    for (String envSingle : set) {
                        envList.add(envSingle.split("/")[0]);
                    }
                    if (!envList.containsAll(standardEnvList)) {
                        StringBuilder sb = new StringBuilder();
                        for (String env : standardEnvList) {
                            if (!envList.contains(env)) {
                                sb.append(env).append("???");
                            }
                        }
                        String lackEnv = sb.toString();
                        if (lackEnv.endsWith("???")) {
                            lackEnv = lackEnv.substring(0, lackEnv.length() - 1);
                        }
                        if ("PROCHF".equals(standardEnvMapping.get(lackEnv))) {
                            // ????????????PROCHF????????????????????????????????????
                            tips = "????????????????????????????????????commonfig??????PROCHF????????????????????????" + lackEnv + "????????????????????????";
                        } else {
                            if(!CommonUtils.isNullOrEmpty(requestParam.get(Dict.PRO_IMAGE_URI))) {
                                // ??????????????????????????????
                                throw new FdevException(ErrorConstants.RELEASE_LACK_FDEV_CONFIG,
                                        new String[]{lackEnv, webUrl + "/tree/" + newTag});
                            }
                        }
                    }
                    if (!CommonUtils.isNullOrEmpty(differs)) {
                        compareUrl = webUrl + differs.get(Dict.COMPAREBRANCHES_URL);
                    } else {
                        compareUrl = webUrl + "/tree/" + newTag;
                    }
                    Map<String,String> sccEnvMap;
                    if(Dict.GRAY.equals(prodRecord.getType())){
                        sccEnvMap = JSONObject.fromObject(this.sccGrayEnv);
                    }else{
                        sccEnvMap = JSONObject.fromObject(this.sccProductEnv);
                    }
                    for (String branchFile : set) {
                        String env = branchFile.split("/")[0];
                        if (CommonUtils.isNullOrEmpty(standardEnvMapping.get(env)) && CommonUtils.isNullOrEmpty(sccEnvMap.get(env)) || !branchFile.endsWith(".properties")) {
                            continue;
                        }
                        ProdAsset prodAsset = new ProdAsset(webUrl + "/blob/" + newTag + "/" + branchFile,
                                branchFile.substring(branchFile.lastIndexOf("/") + 1),
                                prodRecord.getRelease_node_name(), Dict.COMMONCONFIG, Constants.SOURCE_FDEV,
                                userId, prodId, applicationId, (String) application.get(Dict.NAME_ZH),
                                configGitlabId);
                        if(!CommonUtils.isNullOrEmpty(proImageUri)) {
                            //??????caas??????????????????
                            if(!CommonUtils.isNullOrEmpty(standardEnvMapping.get(env))) {
                                prodAsset.setRuntime_env(standardEnvMapping.get(env));
                                prodAssetService.addGitlabAsset(prodAsset);
                            }
                        }
                        if(!CommonUtils.isNullOrEmpty(proSccImageUri)) {
                            //??????scc??????????????????
                            if(!CommonUtils.isNullOrEmpty(sccEnvMap.get(env))){
                                prodAsset.setRuntime_env(sccEnvMap.get(env));
                                prodAssetService.addGitlabAsset(prodAsset);
                            }
                        }
                    }
                }
            }
        }
	}
}


