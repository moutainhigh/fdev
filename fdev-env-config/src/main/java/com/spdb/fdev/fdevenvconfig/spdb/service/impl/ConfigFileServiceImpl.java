package com.spdb.fdev.fdevenvconfig.spdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.spdb.fdev.common.User;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.common.util.ErrorMessageUtil;
import com.spdb.fdev.common.util.UserVerifyUtil;
import com.spdb.fdev.fdevenvconfig.base.CommonUtils;
import com.spdb.fdev.fdevenvconfig.base.dict.Constants;
import com.spdb.fdev.fdevenvconfig.base.dict.Dict;
import com.spdb.fdev.fdevenvconfig.base.dict.ErrorConstants;
import com.spdb.fdev.fdevenvconfig.base.utils.DateUtil;
import com.spdb.fdev.fdevenvconfig.base.utils.FileUtils;
import com.spdb.fdev.fdevenvconfig.base.utils.GitUtils;
import com.spdb.fdev.fdevenvconfig.base.utils.SFTPClient;
import com.spdb.fdev.fdevenvconfig.base.utils.excel.Export;
import com.spdb.fdev.fdevenvconfig.spdb.cache.IConfigFileCache;
import com.spdb.fdev.fdevenvconfig.spdb.dao.*;
import com.spdb.fdev.fdevenvconfig.spdb.entity.*;
import com.spdb.fdev.fdevenvconfig.spdb.service.*;
import com.spdb.fdev.transport.RestTransport;
import net.sf.json.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ???????????? service ???
 *
 * @author csii_shenzy
 * @date 2019/7/29 14:10
 */
@Service
@RefreshScope
public class ConfigFileServiceImpl implements IConfigFileService {

    @Value("${record.switch}")
    private Boolean recordSwitch;
    @Value("${gitlab.fileDir}")
    private String gitlabDir;
    @Value("${gitlab.token}")
    private String token;
    @Value("${gitlab.application.file}")
    private String applicationFile;
    @Value("${gitlab.ci.config.application.file}")
    private String ciConfigApplicationFile;
    @Value("${gitlab.application.yaml.file}")
    private String yamlFile;
    @Value("${gitlab.ci-application.file}")
    private String ci_applicationFile;
    @Value("${path.excel.save}")
    private String tmpPath;
    @Value("${gitlab.web.url}")
    private String gitlab_web_url;
    @Value("${fenvconfig.repo.path}")
    private String configRepoPath;
    @Value("${fenvconfig.repo.id}")
    private Integer configRepoNamespaceId;
    @Value("${fdev.application.properties.dir}")
    private String propertiesDir;
    @Value("${fdev.config.host.ip}")
    private String fdevConfigHostip;
    @Value("${fdev.config.dir}")
    private String fdevConfigDir;
    @Value("${fdev.config.user}")
    private String fdevConfigUser;
    @Value("${fdev.config.password}")
    private String fdevConfigPassword;
    @Value("${sit1dmz}")
    private String sit1Dmz;
    @Value("${sit1biz}")
    private String sit1Biz;
    @Autowired
    private IRequestService requestService;
    @Autowired
    private IModelEnvDao modelEnvDao;
    @Autowired
    private IModelDao modelDao;
    @Autowired
    private IEnvironmentDao environmentDao;
    @Autowired
    private IEnvironmentService environmentService;
    @Autowired
    private IGitlabApiService gitlabApiService;
    @Autowired
    private IAppDeployMappingService appDeployMappingService;
    @Autowired
    private IConfigFileCache configFileCache;
    @Autowired
    private IMailService mailService;
    @Autowired
    private IOutsideTemplateDao outsideTemplateDao;
    @Autowired
    private IAppConfigMappingDao iAppConfigMappingDao;
    @Autowired
    private AppEnvMappingDao appEnvMappingDao;
    @Autowired
    private TagsService tagsService;
    @Autowired
    private RestTransport restTransport;
    @Autowired
    private ErrorMessageUtil errorMessageUtil;
    @Autowired
    private TagsDao tagsDao;
    @Autowired
    public UserVerifyUtil userVerifyUtil;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * ????????????????????????
     *
     * @param requestParam
     */
    @Override
    public Map saveConfigTemplate(Map<String, Object> requestParam) throws Exception {
        // ????????????
        String content = ((String) requestParam.get(Dict.CONTENT)).trim();
        // ??????????????????
        Map<String, Object> errorMap = this.analysisConfigFile(content);
        List<String> formatError = (List<String>) errorMap.get(Dict.FORMATERROR);
        List<String> modelErrorList = (List<String>) errorMap.get(Dict.MODELERRORLIST);
        List<String> FiledErrorList = (List<String>) errorMap.get(Dict.FILEDERRORLIST);
        if (CollectionUtils.isNotEmpty(formatError) || CollectionUtils.isNotEmpty(modelErrorList) || CollectionUtils.isNotEmpty(FiledErrorList)) {
            errorMap.remove(Dict.MODELS);
            errorMap.remove(Dict.MODEL_FIELD);
            return errorMap;
        }

        // ????????????????????????????????????
        User user = null;
        if (Boolean.TRUE.equals(this.recordSwitch)) {
            user = userVerifyUtil.getRedisUser();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Constants.COMMIT_MESSAGE);
        if (user != null && StringUtils.isNotEmpty(user.getEmail())) {
            stringBuilder.append(" by ").append(user.getEmail());
        }

        // ?????????????????????
        String feature_branch = ((String) requestParam.get(Dict.FEATURE_BRANCH)).trim();
        String gitlabId = "";
        String applicationFile = "";
        if (CommonUtils.isNullOrEmpty(requestParam.get(Dict.PROJECT_ID))) {     //???????????????????????????????????????
            gitlabId = ((String) requestParam.get(Dict.GITLABID)).trim();
            applicationFile = this.ci_applicationFile;
        } else {
            // ?????????????????????
            String project_id = ((String) requestParam.get(Dict.PROJECT_ID)).trim();
            // ??????id ??????????????????
            JSONObject appEntity = this.requestService.findByAppId(project_id);
            gitlabId = ((Integer) appEntity.get(Dict.GITLAB_PROJECT_ID)).toString().trim();
            applicationFile = this.applicationFile;
            boolean exists = this.gitlabApiService.checkFileExists(this.token, gitlabId, feature_branch, this.applicationFile);
            if(!exists) {
                applicationFile = this.ciConfigApplicationFile;
            }
        }

        // ?????? ??????????????????
        this.gitlabApiService.checkBranch(this.token, gitlabId, feature_branch);
        // ????????????????????????
        this.gitlabApiService.createFile(this.token, gitlabId, feature_branch, applicationFile, content, stringBuilder.toString());
        return null;
    }

    /**
     * ?????????????????? (??????)
     *
     * @param requestParam
     * @return
     */
    @Override
    public String queryConfigTemplate(Map<String, String> requestParam) throws Exception {
        // ??????id
        String project_id = requestParam.get(Dict.PROJECT_ID).trim();
        // feature ??????
        String feature_branch = requestParam.get(Dict.FEATURE_BRANCH).trim();
        // ??????id ??????????????????
        JSONObject appEntity = this.requestService.findByAppId(project_id);
        String gitlabId = ((Integer) appEntity.get(Dict.GITLAB_PROJECT_ID)).toString();
        this.gitlabApiService.checkBranch(this.token, gitlabId, feature_branch);
        String filePath = this.ciConfigApplicationFile;
        boolean exists = this.gitlabApiService.checkFileExists(this.token, gitlabId, feature_branch, this.ciConfigApplicationFile);
        if(!exists) {
            filePath = this.applicationFile;
        }
        return this.gitlabApiService.getFileContent(this.token, gitlabId, feature_branch, filePath);
    }

    /**
     * ??????????????????
     *
     * @param requestParam
     * @return
     */
    @Override
    public Map<String, Object> previewConfigFile(Map<String, Object> requestParam) throws Exception {
        // flag==null?????????previewConfigFile?????????????????????????????????saveConfigProperties??????
        String flag = (String) requestParam.get(Dict.FLAG);
        String envName = (String) requestParam.get(Constants.ENV_NAME);
        String content = (String) requestParam.get(Dict.CONTENT);
        // 0:???????????????????????? 1:?????????????????? ?????????????????????????????????
        String type = (String) requestParam.get(Constants.TYPE);
        Environment queryenv = environmentService.queryByNameEn(envName);
        if (queryenv == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"??????????????????", envName});
        }
        List<String> labels = queryenv.getLabels();
        if (Constants.ZERO.equals(type) && (labels.containsAll(Arrays.asList(Dict.DEV, Dict.DEFAULT, Dict.DMZ)) || labels.containsAll(Arrays.asList(Dict.DEV, Dict.DEFAULT, Dict.BIZ))) && flag == null) {
            Environment env = devLableToSitEnv(queryenv.getLabels());
            if (env != null) {
                envName = env.getName_en();
            }
        }
        // ?????????????????????????????????????????????????????????
        Map<String, Object> analysisConfigFileMap = this.analysisConfigFile(content);
        List<Model> modelList = (List<Model>) analysisConfigFileMap.get(Dict.MODELS);
        // ??????????????????
        Environment environment = environmentService.queryByNameEn(envName);
        String appId = (String) requestParam.get(Dict.PROJECT_ID);
        if (Constants.ONE.equals(type)) {
            // ????????????project_id???gitlabId,????????????????????????project_id
            Integer id = Integer.parseInt(appId);
            Map param = this.requestService.getAppByGitId(id);
            appId = (String) param.get(Dict.ID);
        }
        Map<String, Object> replaceConfigFileMap = this.replaceConfigFile(content, modelList, environment, appId);
        replaceConfigFileMap.remove(Dict.REQUIRE);
        if (Constants.ONE.equals(type) && flag == null) {
            String fileContent = (String) replaceConfigFileMap.get(Dict.CONTENT);
            pushConfigFile(requestParam, fileContent, environment.getName_en());
        }
        return replaceConfigFileMap;
    }

    @Override
    public String saveDevConfigProperties(Map<String, Object> requestParam) throws Exception {
        List<String> labels = (List<String>) requestParam.get(Constants.LABELS);
        // ?????? ????????????
        Environment env = null;
        if ((labels.containsAll(Arrays.asList(Dict.DEV, Dict.DEFAULT, Dict.DMZ)) || labels.containsAll(Arrays.asList(Dict.DEV, Dict.DEFAULT, Dict.BIZ)))) {
            env = devLableToSitEnv(labels);
        } else {
            List<Environment> environmentList = this.environmentDao.queryByLabelsFuzzy(labels);
            for (Environment env1 : environmentList) {
                if (CollectionUtils.isEqualCollection(env1.getLabels(), labels)) {
                    env = env1;
                }
            }
        }
        String content = ((String) requestParam.get(Dict.CONTENT));
        // ?????????????????? ?????? ??????????????????
        Map<String, Object> analysisConfigFileMap = this.analysisConfigFile(content);
        List<Model> modelList = (List<Model>) analysisConfigFileMap.get(Dict.MODELS);
        String appId = (String) requestParam.get(Dict.PROJECT_ID);
        JSONObject appEntity = this.requestService.findByAppId(appId);
        Map<String, Object> replaceConfigFileMap = this.replaceConfigFile(content, modelList, env, appId);
        String fileCont = (String) replaceConfigFileMap.get(Dict.CONTENT);
        Map<String, Object> devMap = new HashMap<>();
        devMap.put(Dict.CI_PROJECT_NAME, appEntity.get(Dict.NAME_EN));
        devMap.put(Dict.FDEV_CONFIG_HOST_IP, fdevConfigHostip);
        devMap.put(Dict.FDEV_CONFIG_USER, fdevConfigUser);
        devMap.put(Dict.FDEV_CONFIG_PASSWORD, fdevConfigPassword);
        devMap.put(Dict.FDEV_CONFIG_DIR, fdevConfigDir);
        String remotePath = pushConfigFile(devMap, fileCont, env.getName_en());
        return remotePath + "???????????????Ip:" + devMap.get(Dict.FDEV_CONFIG_HOST_IP);
    }

    private Environment devLableToSitEnv(List labels) {
        List<String> sitLabels = null;
        if (labels.containsAll(Arrays.asList(Dict.DEV, Dict.DEFAULT, Dict.BIZ))) {
            sitLabels = Arrays.asList(sit1Biz.split(","));
        } else if (labels.containsAll(Arrays.asList(Dict.DEV, Dict.DEFAULT, Dict.DMZ))) {
            sitLabels = Arrays.asList(sit1Dmz.split(","));
        }
        //SIT??????
        List<Environment> environmentList = this.environmentDao.queryByLabelsFuzzy(Arrays.asList(Dict.SIT, Dict.DEFAULT));
        for (Environment env : environmentList) {
            if ((env.getLabels()).containsAll(sitLabels)) {
                return env;
            }
        }
        return null;
    }

    /**
     * ?????????????????????????????????
     *
     * @param requestParam
     * @param fileCont
     * @param envName
     * @return
     */
    private String pushConfigFile(Map<String, Object> requestParam, String fileCont, String envName) {
        // ????????????????????????
        String firstContentShowEnvName = Constants.CONFIG_FILE_FIRST_LINE + envName + "\n";
        fileCont = firstContentShowEnvName + fileCont;
        String ciProjectName = (String) requestParam.get(Dict.CI_PROJECT_NAME);
        String fileName = ciProjectName + "-fdev.properties";
        File file = new File(propertiesDir);
        if (!file.exists()) {
            file.mkdirs();
        }
        String fdevRuntimePath = propertiesDir + fileName;
        try (PrintWriter printWriter = new PrintWriter(fdevRuntimePath, StandardCharsets.UTF_8.name())) {
            printWriter.write(fileCont);
        } catch (IOException e) {
            throw new FdevException(ErrorConstants.APP_FILE_ERROR, new String[]{e.getMessage()});
        }
        // ??????????????????
        String fdevConfigHostIp = (String) requestParam.get(Dict.FDEV_CONFIG_HOST_IP);
        String fdevConfigUser = (String) requestParam.get(Dict.FDEV_CONFIG_USER);
        String fdevConfigPassword = (String) requestParam.get(Dict.FDEV_CONFIG_PASSWORD);
        String fdevConfigDir = (String) requestParam.get(Dict.FDEV_CONFIG_DIR);
        String[] nameEnSplit = ciProjectName.split("-");
        String projectName = nameEnSplit[0];
        if (!fdevConfigDir.endsWith("/")) {
            fdevConfigDir += "/";
        }
        String remoteDirectory = fdevConfigDir + projectName + "/";
        SFTPClient sftpClient = new SFTPClient(fdevConfigHostIp, fdevConfigUser, fdevConfigPassword);
        sftpClient.pushConfig(remoteDirectory, fdevRuntimePath);
        return remoteDirectory;
    }

    /**
     * ???????????????????????? ??????????????????????????? ?????????????????????????????????,??????????????????????????????
     *
     * @return
     */
    @Override
    public List<Map> queryConfigDependency(String model_name_en, String field_name_en, List<String> range) throws Exception {
        List<AppConfigMapping> appConfigMappingList = iAppConfigMappingDao.getAppConfigMapping(model_name_en, field_name_en, range);
        // ???????????????
        List<Map<String, Object>> appInfoList = requestService.queryAllAppInfo();
        List<Map> allAppList = new ArrayList<>();
        for (AppConfigMapping appConfigMapping : appConfigMappingList) {
            for (Map<String, Object> appInfo : appInfoList) {
                if (String.valueOf(appConfigMapping.getAppId()).equals(String.valueOf(appInfo.get(Dict.GITLAB_PROJECT_ID)))) {
                    Map<String, Object> returnAppInfo = new HashMap<>();
                    returnAppInfo.put(Dict.NAME_ZH, appInfo.get(Dict.NAME_ZH));
                    returnAppInfo.put(Dict.NAME_EN, appInfo.get(Dict.NAME_EN));
                    returnAppInfo.put(Dict.DEV_MANAGERS, appInfo.get(Dict.DEV_MANAGERS));
                    returnAppInfo.put(Dict.SPDB_MANAGERS, appInfo.get(Dict.SPDB_MANAGERS));
                    returnAppInfo.put(Dict.GROUP, appInfo.get(Dict.GROUP));
                    returnAppInfo.put(Dict.GIT, appInfo.get(Dict.GIT));
                    returnAppInfo.put(Dict.BRANCH, appConfigMapping.getBranch());
                    returnAppInfo.put(Dict.APPID, appInfo.get(Dict.ID));    //??????ID
                    returnAppInfo.put(Dict.GITLAB_ID, ((Integer) appInfo.get(Dict.GITLAB_PROJECT_ID)).toString());  //git??????id
                    allAppList.add(returnAppInfo);
                    break;

                }
            }
        }
        return replaceUserAndGroup(allAppList);
    }

    @Override
    public void exportDependencySearchResult(List<Map> maps, HttpServletResponse response) throws Exception {
        for (Map m : maps) {
            Map<String, Object> group = (Map<String, Object>) m.get(Dict.GROUP);
            if (group != null && group.size() != 0) {
                m.put(Dict.GROUP, group.get(Dict.NAME));
            }
        }
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(DateUtil.DATETIME_COMPACT_FORMAT);
        String timeStamp = sdf.format(date);
        String[] headers = {"???????????????", "???????????????", "???????????????", "???????????????", "????????????", "git??????", "??????"};
        String excelPath = tmpPath + "/" + Constants.CONFIG_FILE_PRE + timeStamp + ".xls";
        String filename = Constants.CONFIG_FILE_PRE + timeStamp + ".xls";
        String sheetName = "??????????????????";
        Export.export(headers, excelPath, sheetName, maps);
        Export.commonDown(filename, excelPath, response);
    }

    /**
     * ????????????????????????????????????????????????????????????
     *
     * @param appMaps ??????????????????
     * @return ?????????????????????????????????????????????????????????, ?????????
     * @throws Exception
     */
    public List<Map> replaceUserAndGroup(List<Map> appMaps) throws Exception {
        for (Map appMap : appMaps) {
            List<Map> dev_managers = (List<Map>) appMap.get(Dict.DEV_MANAGERS);
            List<Map> spdb_managers = (List<Map>) appMap.get(Dict.SPDB_MANAGERS);
            Map<String, Object> userMap = this.mailService.valdateKeys(dev_managers, spdb_managers);
            Map<String, Map> params = this.configFileCache.getUsersByIdsOrUserNameEn(
                    (String) userMap.get(Constants.keys), (Map<String, List<String>>) userMap.get(Constants.DATA));
            spdb_managers = this.updateManagers(spdb_managers, (String) userMap.get(Constants.KEY), params);
            appMap.replace(Dict.SPDB_MANAGERS, spdb_managers);
            dev_managers = this.updateManagers(dev_managers, (String) userMap.get(Constants.KEY), params);
            appMap.replace(Dict.DEV_MANAGERS, dev_managers);
        }
        return appMaps;
    }


    /***
     * ?????????????????????????????????/?????????????????????
     *
     * @param Mapdata ???????????????/??????????????????????????????
     * @param key     Dict.IDS/usernameEns
     * @param params  ??????????????????
     * @return ?????????????????????
     */
    private List<Map> updateManagers(List<Map> Mapdata, String key, Map<String, Map> params) {
        if (Dict.IDS.equals(key)) {
            for (Map<String, String> map : Mapdata) {
                if (map.containsKey(Dict.ID)) {
                    String Id = map.get(Dict.ID);
                    Object ob = params.get(Id);
                    if (ob instanceof Map) {
                        Map maps = (Map) ob;
                        String userName_CN = (String) maps.get(Dict.USER_NAME_CN);
                        map.replace(Dict.USER_NAME_CN, userName_CN);
                    }
                }
            }
        } else if (Constants.USERNAMERNS.equals(key)) {
            for (Map<String, String> map : Mapdata) {
                String name_en = map.get(Dict.USER_NAME_EN);
                Object ob = params.get(name_en);
                if (ob instanceof Map) {
                    Map maps = (Map) ob;
                    String userName_CN = (String) maps.get(Dict.USER_NAME_CN);
                    map.replace(Dict.USER_NAME_CN, userName_CN);
                }
            }
        }
        return Mapdata;
    }

    @Override
    public String saveConfigProperties(Map map) throws Exception {
        Integer gitlabId = (Integer) map.get(Constants.GITLAB_PROJECT_ID);
        String branch = (String) map.get(Constants.BRANCH);
        if (!branch.startsWith(Constants.PRO)) {
            throw new FdevException(ErrorConstants.PUSH_CONFIG_FILE_ERROR, new String[]{"???tag?????????"});
        }
        // ??????????????????
        String content = (String) map.get(Dict.CONTENT);
        // ?????????????????????????????????true:??????????????????
        boolean contentFlag = CommonUtils.checkFileContentIsNone(content);
        // ???????????????????????????????????????
        Map<String, Object> paramMap = new HashMap();
        String[] branchSplit = branch.split("-");
        if (branchSplit.length < 3) {
            throw new FdevException(ErrorConstants.PUSH_CONFIG_FILE_ERROR, new String[]{"tag????????????fdev?????????pro-??????????????????-??????"});
        }
        paramMap.put(Dict.RELEASE_NODE_NAME, branch.split("-")[1]);
        paramMap.put(Dict.GITLAB_PROJECT_ID, gitlabId);
        // caas yaml????????????
        String caasYamlContent = (String) map.get(Dict.YAML_CONTENT);
        // scc yaml????????????
        String sccYamlContent = (String) map.get(Dict.SCC_YAML_CONTENT);
        logger.info("GitLabID:{},tag:{},?????????????????????????????????true:{},CaaS??????yaml???????????????????????????true:{},SCC??????yaml???????????????????????????true:{}", gitlabId, branch, contentFlag, StringUtils.isEmpty(caasYamlContent), StringUtils.isEmpty(sccYamlContent));

        // ???????????????,??????yaml?????????????????????????????????????????????
        if (contentFlag && StringUtils.isEmpty(caasYamlContent) && StringUtils.isEmpty(sccYamlContent)) {
            return "";
        }
        String commitMessage = "?????????????????? by pipeline " + map.get(Dict.PIPELINE_ID);
        Map appMap = this.requestService.getAppByGitId(gitlabId);
        String nameEn = (String) appMap.get(Constants.NAME_EN);
        String configRepoId = String.valueOf(makeConfigProjectExist(nameEn, gitlabId));
        String appId = (String) appMap.get(Dict.ID);
        String network = (String) appMap.get(Dict.NETWORK);
        // ??????????????????
        List<Map<String, Object>> lists = new ArrayList<>();
        List<Map<String, Object>> scclists = new ArrayList<>();
        List<Map<String, Object>> caaslists = new ArrayList<>();
        // ?????????????????????"tcyz"?????????????????????????????????rel???????????????????????????????????????GitLab
        List<Environment> caasenvironments = new ArrayList<>();
        List<Environment> sccenvironments = new ArrayList<>();
        if (!CommonUtils.isNullOrEmpty(sccYamlContent)) {
            sccenvironments = getSccEnvironments(appId);
        }
        if(!CommonUtils.isNullOrEmpty(caasYamlContent)){
            caasenvironments = getEnvironments(appId, network);
        }
        // ??????yaml???????????????????????????????????????
        Multimap<String, Object> envAndField = ArrayListMultimap.create();
        //??????????????????????????????????????????????????????
        Map<String,Object> paramsMap = new HashMap<>();
        Map<String,Object> caasMap = new HashMap<>();
        Map<String,Object> sccMap = new HashMap<>();
        caasMap.put(Dict.LIST, caaslists);
        caasMap.put(Dict.ENV, caasenvironments);
        caasMap.put(Dict.YAML_CONTENT, caasYamlContent);
        sccMap.put(Dict.LIST, scclists);
        sccMap.put(Dict.ENV, sccenvironments);
        sccMap.put(Dict.YAML_CONTENT, sccYamlContent);
        paramsMap.put(Constants.SCC, sccMap);
        paramsMap.put(Constants.CAAS, caasMap);
        contentByEnv(paramsMap, content, gitlabId.toString(), contentFlag, envAndField);

        Map<String, Collection<Object>> envAndFieldMap = envAndField.asMap();
        StringBuilder stringBuilder = new StringBuilder();
        if (MapUtils.isNotEmpty(envAndFieldMap)) {
            for (Map.Entry<String, Collection<Object>> envAndFieldEntry : envAndFieldMap.entrySet()) {
                Collection<Object> value = envAndFieldEntry.getValue();
                stringBuilder.append("???????????????????????????").append(value).append("???").append(envAndFieldEntry.getKey()).append("???????????????????????????\n");
            }
            stringBuilder.append("???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????\n");
        }
        //??????????????????
        push_config(paramsMap, nameEn, configRepoId, branch, commitMessage, map);
        return stringBuilder.toString();
    }

    //  ?????????????????????gitlab
    private void push_config(Map<String, Object> paramMap, String nameEn, String configRepoId, String branch, String commitMessage, Map map) throws Exception {
        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            Map entrymap = (Map) entry.getValue();
            List<Map<String, Object>> lists = (List<Map<String, Object>>) entrymap.get(Dict.LIST);
            if (CollectionUtils.isNotEmpty(lists)) {
                for (Map<String, Object> m : lists) {
                    String name = (String) m.get(Constants.ENV_NAME);
                    String[] nameSplit = nameEn.split("-");
                    String fileDir;
                    String yamlFileDir;
                    if (nameSplit.length > 0) {
                        fileDir = name + File.separator + nameSplit[0] + File.separator + nameEn + "-fdev.properties";
                        if (entry.getKey().equals(Constants.SCC)) {
                            yamlFileDir = name + File.separator + nameSplit[0] + File.separator + nameEn + "-scc.yaml";
                        } else {
                            yamlFileDir = name + File.separator + nameSplit[0] + File.separator + nameEn + ".yaml";
                        }
                    } else {
                        throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"???????????????????????????"});
                    }
                    m.put(Constants.FILEDIR, fileDir);
                    m.put(Constants.YAML_FILE_DIR, yamlFileDir);
                }
                // ???????????????push
                this.commitConfigProperties(nameEn, lists, commitMessage);
                if (this.gitlabApiService.checkTag(configRepoId, branch, token)) {
                    this.gitlabApiService.deleteTag(configRepoId, branch, token);
                }
                //???master???tag
                this.gitlabApiService.createTag(configRepoId, branch, Dict.MASTER, token);
                // ??????tag
                tagsService.saveTags(map);
            }
        }

    }

    private List<Environment> getSccEnvironments(String appId) {
        // ??????????????????"tcyz"?????????
        List<String> labels = new ArrayList<>();
        labels.add(Constants.TCYZ);
        labels.add(Constants.SCC);
        List<Environment> environments = this.environmentDao.queryByLabelsFuzzy(labels);
        List<AppEnvMapping> appEnvMappings = appEnvMappingDao.queryEnvByAppId(appId);
        for (AppEnvMapping appEnvMapping : appEnvMappings) {
            if (!CommonUtils.isNullOrEmpty(appEnvMapping.getScc_rel_id())) {
                environments.add(environmentDao.queryById(appEnvMapping.getScc_rel_id()));
            }
            if (!CommonUtils.isNullOrEmpty(appEnvMapping.getScc_pro_id())) {
                environments.add(environmentDao.queryById(appEnvMapping.getScc_pro_id()));
            }
        }
        return environments;
    }

    /**
     * ??????????????????"tcyz"?????????????????????????????????rel?????????????????????
     *
     * @return
     */
    private List<Environment> getEnvironments(String appId, String network) {
        // ??????????????????"tcyz"?????????
        List<String> labels = new ArrayList<>();
        labels.add(Constants.TCYZ);
        labels.add(Constants.CAAS);
        List<Environment> environments = this.environmentDao.queryByLabelsFuzzy(labels);
        // ????????????????????????rel??????
        if (StringUtils.isNotEmpty(network)) {
            List<String> relLabels = new ArrayList<>();
            relLabels.add(Constants.REL);
            relLabels.add(Constants.CAAS);
            String[] networks = network.split(",");
            Collections.addAll(relLabels, networks);
            Map<String, Object> paramLabel = new HashMap<>();
            paramLabel.put(Dict.LABELS, relLabels);
            environments.addAll(environmentService.queryByLabelsFuzzy(paramLabel));
        }
        // ?????????????????????????????????
        List<Map> proEnvList = appEnvMappingDao.queryProEnvByAppId(appId);
        for (Map map : proEnvList) {
            Environment environment = new Environment();
            environment.setId(String.valueOf(map.get(Dict.ID)));
            environment.setName_en(String.valueOf(map.get(Dict.NAME_EN)));
            environment.setName_cn(String.valueOf(map.get(Dict.NAME_CN)));
            environment.setLabels((List<String>) map.get(Dict.LABELS));
            environments.add(environment);
        }
        return environments;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param contentMapList Map<fileDir,content>????????????<->????????????
     * @param message        push???message
     * @throws Exception
     */
    private void commitConfigProperties(String projectName, List<Map<String, Object>> contentMapList, String message) throws Exception {
        String configProjectPath = gitlabDir + projectName;
        //????????????1
        try {
            if (new File(configProjectPath + File.separator + ".git").exists()) {
                GitUtils.gitPullFromGitlab(configProjectPath);
            }
        } catch (Exception e) {
            logger.warn("git pull failed ! delete local git reponsitory!");
            if (!FileUtils.deleteFile(new File(configProjectPath))) {
                logger.warn(" delete local git reponsitory failed!");
            } else {
                logger.warn(" delete local git reponsitory success!");
            }
        }
        if (!new File(configProjectPath + File.separator + ".git").exists()) {
            File configProject = FileUtils.createFile(configProjectPath, true);
            GitUtils.gitCloneFromGitlab(gitlab_web_url + '/' + projectName + ".git", configProject.getAbsolutePath());
        }
        //??????master?????????push
        updateFile(projectName, contentMapList);
        GitUtils.gitPushFromGitlab(configProjectPath, message);
        return;
    }

    private void updateFile(String projectName, List<Map<String, Object>> contentMapList) throws Exception {
        String configProjectPath = gitlabDir + projectName;
        for (Map<String, Object> contentMap : contentMapList) {
            Object contentObj = contentMap.get(Constants.CONTENT);
            Object yamlContentObj = contentMap.get(Dict.YAML_CONTENT);
            if (contentObj != null) {
                String content = (String) contentObj;
                String fileDir = configProjectPath + "/" + contentMap.get(Constants.FILEDIR);
                updateFile(fileDir, content, false, new HashMap<>());
            }
            if (yamlContentObj != null) {
                Map<String, Object> yamlContent = (Map<String, Object>) yamlContentObj;
                String yamlFileDir = configProjectPath + "/" + contentMap.get(Constants.YAML_FILE_DIR);
                updateFile(yamlFileDir, "", true, yamlContent);
            }
        }
        return;
    }

    private void updateFile(String fileDir, String content, boolean yamlFlag, Map<String, Object> yamlContent) throws Exception {
        File configproperties = new File(fileDir);
        //??????????????????????????????md5?????????????????????????????????????????????????????????
        if (configproperties.exists()) {
            boolean deleteFlag = configproperties.delete();
            if (!deleteFlag) {
                logger.error("????????????{}??????", fileDir);
                throw new FdevException(ErrorConstants.SERVER_ERROR);
            }
        }
        configproperties = FileUtils.createFile(configproperties.getAbsolutePath(), false);
        if (yamlFlag) {
            DumperOptions dumperOptions = new DumperOptions();
            dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(dumperOptions);
            try (FileWriter fileWriter = new FileWriter(configproperties);) {
                yaml.dump(yamlContent, fileWriter);
                return;
            } catch (IOException e) {
                logger.error("yaml?????????????????????", e);
                throw new FdevException(ErrorConstants.SERVER_ERROR);
            }

        }
        if (configproperties.exists()) {
            try (FileWriter fileWriter = new FileWriter(configproperties);) {
                fileWriter.write(content);
            } catch (Exception e) {
                logger.error("???????????????????????????", e);
                throw new FdevException(ErrorConstants.SERVER_ERROR);
            }
        } else {
            throw new FdevException(ErrorConstants.SERVER_ERROR);
        }
    }

    private Integer makeConfigProjectExist(String projectName, Integer gitlabId) throws Exception {
        // ??????configGitlabId ????????????????????????????????????????????????????????????
        AutoConfigTags autoConfigTags = tagsDao.queryByGitlabId(gitlabId);
        // ???auto_config_tags????????????????????????????????????
        if (CommonUtils.isNullOrEmpty(autoConfigTags)) {
            AutoConfigTags tagInfo = new AutoConfigTags();
            List<Map<String, Object>> tagList = new ArrayList<>();
            tagInfo.setGitlab_id(gitlabId);
            tagInfo.setTagInfo(tagList);
            tagsDao.saveTag(tagInfo);
            autoConfigTags = tagInfo;
        }
        // ??????namespace+project???gitlab??????????????????
        Integer findGitlabId = gitlabApiService.isProjectExist(configRepoNamespaceId, projectName, token);
        if (CommonUtils.isNullOrEmpty(findGitlabId)) {
            // ????????????????????????
            Map<String, Object> createProjectDetail = requestService.createGitlabProject(configRepoPath, projectName);
            autoConfigTags.setConfigGitlabId((Integer) createProjectDetail.get(Dict.ID));
            // ?????????????????????????????????AutoConfigTags??????
            tagsDao.updateConfigGitlabId(autoConfigTags);
        } else if (!findGitlabId.equals(autoConfigTags.getConfigGitlabId())) {
            autoConfigTags.setConfigGitlabId(findGitlabId);
            // ?????????????????????????????????AutoConfigTags??????
            tagsDao.updateConfigGitlabId(autoConfigTags);
        }
        return autoConfigTags.getConfigGitlabId();
    }

    /**
     * ??????????????????????????????????????????????????????
     * @param paramMap
     * @param content       ????????????
     * @param gitlabId
     * @param contentFlag
     * @param envAndField   ??????????????????????????????
     * @throws Exception
     */
    private void contentByEnv(Map<String, Object> paramMap, String content, String gitlabId, boolean contentFlag, Multimap<String, Object> envAndField) throws Exception {
        for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
            Map map =(Map) entry.getValue();
            List<Environment> environments =(List<Environment>) map.get(Dict.ENV);
            List<Map<String,Object>>lists =(List<Map<String, Object>>) map.get(Dict.LIST);
            String yamlContent =(String) map.get(Dict.YAML_CONTENT);
            if (environments != null && !environments.isEmpty()) {
                for (Environment environment : environments) {
                    String envName = environment.getName_en();
                    // ?????????????????????????????????
                    Map<String, Object> testMap = new HashedMap<>();
                    testMap.put(Constants.ENV_NAME, envName);
                    testMap.put(Dict.PROJECT_ID, gitlabId);
                    testMap.put(Constants.TYPE, Constants.ONE);
                    testMap.put(Dict.FLAG, "save");
                    String firstContentShowEnvName = Constants.CONFIG_FILE_FIRST_LINE + envName + "\n";
                    if (!contentFlag) {
                        testMap.put(Dict.CONTENT, content);
                        Map<String, Object> result = this.previewConfigFile(testMap);
                        String fileContent = firstContentShowEnvName + result.get(Dict.CONTENT);
                        testMap.put(Dict.CONTENT, fileContent);
                    }
                    // ??????yaml??????
                    if (StringUtils.isEmpty(yamlContent)) {
                        throw new FdevException(ErrorConstants.PUSH_CONFIG_FILE_ERROR, new String[]{"deployment.yaml???????????????????????????"});
                    }
                    if (entry.getKey().equals(Constants.SCC)) {
                        testMap.put(Dict.YAML_CONTENT, this.previewSccYamlFile(envName, gitlabId, yamlContent, envAndField));
                    } else {
                        testMap.put(Dict.YAML_CONTENT, this.previewYamlFile(envName, gitlabId, yamlContent, envAndField));
                    }
                    lists.add(testMap);
                }
            }

        }

    }

    private Map<String, Object> previewSccYamlFile(String envName, String gitlabId, String yamlContent, Multimap<String, Object> envAndField) throws Exception {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put(Dict.ENV, envName);
        paramMap.put(Dict.GITLABID, gitlabId);
        paramMap.put(Constants.TYPE, "deploy");
        Map variablesMapping = appDeployMappingService.querySccVariablesMapping(paramMap);
        Pattern p = Pattern.compile("\\{\\{(.*?)\\}\\}");
        Matcher m = p.matcher(yamlContent);
        String keyFiled;
        String keyValue;
        System.out.println("++++++++++++++++++++++++++++++++++++++"+envName+"++++++++");
        while (m.find()) {
            keyFiled = m.group(1).trim();
            keyValue = (String) variablesMapping.get(keyFiled);
            if (keyValue == null) {
                // value == null ????????????????????????key
                throw new FdevException(ErrorConstants.PROPERTIES_ERROR, new String[]{keyFiled});
            } else if (keyValue == "") {
                // value == ""???????????????key?????????????????????????????????????????????????????????????????????
                envAndField.put(envName, keyFiled.toLowerCase());
            }
            yamlContent = yamlContent.replace(m.group(), keyValue);
        }
        // ????????????pvc
        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap = yaml.load(yamlContent);
        String scc_pvc_nastemp = (String) variablesMapping.get("scc_pvc_nastemp");
        if (!CommonUtils.isNullOrEmpty(scc_pvc_nastemp)) {
            Map<String, Object> specMap = (Map<String, Object>) yamlMap.get("spec");
            Map<String, Object> metadataMap = (Map<String, Object>) yamlMap.get("metadata");
            String project_name = (String) metadataMap.get("name");
            if (MapUtils.isNotEmpty(specMap)) {
                Map<String, Object> specTemplateMap = (Map<String, Object>) specMap.get("template");
                if (MapUtils.isNotEmpty(specTemplateMap)) {
                    Map<String, Object> specTemplateSpecMap = (Map<String, Object>) specTemplateMap.get("spec");
                    List<Map<String, Object>> containersList = (List<Map<String, Object>>) specTemplateSpecMap.get("containers");
                    for (Map<String, Object> containersMap : containersList) {
                        List<Map<String, Object>> volumeMounts = (List<Map<String, Object>>) containersMap.get("volumeMounts");
                        Map<String, Object> pvcMap = new HashMap<>();
                        pvcMap.put("mountPath", "/ebank/spdb/temp/");
                        pvcMap.put("name", "storage-nastemp-pvc");
                        pvcMap.put("subPath", project_name);
//                        pvcMap.put("subPath", "yxq-yxq-yxq");
                        volumeMounts.add(pvcMap);
                        containersMap.put("volumeMounts", volumeMounts);
                    }
                    List<Map<String, Object>> volumes = (List<Map<String, Object>>) specTemplateSpecMap.get("volumes");
                    Map<String, Object> volumeMap = new HashMap<>();
                    Map<String, Object> persistentVolumeClaim = new HashMap<>();
                    persistentVolumeClaim.put("claimName", scc_pvc_nastemp);
                    volumeMap.put("name", "storage-nastemp-pvc");
                    volumeMap.put("persistentVolumeClaim", persistentVolumeClaim);
                    volumes.add(volumeMap);
                    specTemplateSpecMap.put("volumes", volumes);
                }
            }
        }
        return yamlMap;
    }

    private Map<String, Object> previewYamlFile(String envName, String gitlabId, String yamlContent, Multimap<String, Object> envAndField) throws Exception {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put(Dict.ENV, envName);
        paramMap.put(Dict.GITLABID, gitlabId);
        paramMap.put(Constants.TYPE, "deploy");
        Map variablesMapping = appDeployMappingService.queryVariablesMapping(paramMap);
        StringBuilder yamlContentStringBuilder = new StringBuilder();
        yamlContent = yamlContent.replace("\r", "");
        String[] yamlContentSplit = yamlContent.split("\n");
        // yaml???????????????"{{  }}"
        for (String line : yamlContentSplit) {
            int left = line.indexOf("{{ ");
            int right = line.indexOf(" }}");
            if (left > 0 && right > 0) {
                String key = line.substring(left + 3, right);
                String value = (String) variablesMapping.get(key);
                int first = key.indexOf('-');
                int last = key.indexOf('.');
                int index = 0;
                String name = "";
                if (first > 0 && last > 0) {
                    // ?????????"-"???"."?????????????????????"-"???"."????????????????????????
                    name = key.split("\\.")[1];
                    index = Integer.valueOf(key.substring(first + 1, last));
                    String modelField = key.substring(0, first);
                    value = (String) variablesMapping.get(modelField);
                }
                if (value == null) {
                    // value == null ????????????????????????key
                    throw new FdevException(ErrorConstants.PROPERTIES_ERROR, new String[]{key});
                } else if (value == "") {
                    // value == ""???????????????key?????????????????????????????????????????????????????????????????????
                    envAndField.put(envName, key.toLowerCase());
                    line = line.replace("{{ " + key + " }}", value);
                } else {
                    if (StringUtils.isNotEmpty(name)) {
                        List<Map> valueList = JSONArray.parseArray(value, Map.class);
                        if (CollectionUtils.isNotEmpty(valueList) && valueList.size() > index) {
                            Map<String, Object> valueMap = valueList.get(index);
                            value = (String) valueMap.get(name);
                        } else {
                            // ??????????????????????????????????????????
                            envAndField.put(envName, key.toLowerCase());
                        }
                    }
                    line = line.replace("{{ " + key + " }}", value);
                }
            }
            yamlContentStringBuilder.append(line).append("\n");
        }
        // ??????????????????fdev_caas_service_registry/fdev_caas_registry_namespace/name_en:pro_name
        // ??????imagePullSecrets????????????-fdev_caas_service_registry
        String fdevCaasServiceRegistry = (String) variablesMapping.get("FDEV_CAAS_SERVICE_REGISTRY");
        String fdevCaasRegistryNamespace = (String) variablesMapping.get("FDEV_CAAS_REGISTRY_NAMESPACE");
        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap = yaml.load(yamlContentStringBuilder.toString());
        Map<String, Object> metadataMap = (Map<String, Object>) yamlMap.get("metadata");
        Map<String, Object> specMap = (Map<String, Object>) yamlMap.get("spec");
        if (MapUtils.isNotEmpty(specMap)) {
            Map<String, Object> specTemplateMap = (Map<String, Object>) specMap.get("template");
            if (MapUtils.isNotEmpty(specTemplateMap)) {
                Map<String, Object> specTemplateSpecMap = (Map<String, Object>) specTemplateMap.get("spec");
                // ??????????????????fdev_caas_service_registry/fdev_caas_registry_namespace/name_en:pro_name
                List<Map<String, Object>> containersList = (List<Map<String, Object>>) specTemplateSpecMap.get("containers");
                for (Map<String, Object> containersMap : containersList) {
                    for (Map.Entry<String, Object> entry : containersMap.entrySet()) {
                        if ("image".equals(entry.getKey())) {
                            String image = (String) entry.getValue();
                            if (StringUtils.isNotEmpty(image) && image.contains("/")) {
                                String[] imageSplit = image.split("/");
                                image = fdevCaasServiceRegistry + "/" + fdevCaasRegistryNamespace + "/" + imageSplit[imageSplit.length - 1];
                                containersMap.put("image", image);
                            }
                            break;
                        }
                    }
                }
                // ??????imagePullSecrets????????????-fdev_caas_service_registry
                List<Map<String, Object>> imagePullSecrets = new ArrayList<>();
                Map<String, Object> imagePullSecretsMap = new HashMap();
                imagePullSecretsMap.put(Dict.NAME, metadataMap.get(Dict.NAME) + "-" + fdevCaasServiceRegistry);
                imagePullSecrets.add(imagePullSecretsMap);
                specTemplateSpecMap.put("imagePullSecrets", imagePullSecrets);
                //??????host
                List<Object> hostAliasesList = (List<Object>) specTemplateSpecMap.get("hostAliases");
                if (null != hostAliasesList && hostAliasesList.size() != 0){
                    specTemplateSpecMap.remove("hostAliases");
                }
            }
        }
        return yamlMap;
    }

    /**
     * ??????????????????????????? ?????? war??????md5??? ????????? gitlab auto-config??????
     *
     * @param requestParam
     */
    @Override
    public void saveWarMd5ToGitlab(Map<String, Object> requestParam) throws Exception {
        Integer gitlabId = (Integer) requestParam.get(Dict.GITLABID);
        String tagName = (String) requestParam.get(Dict.TAG_NAME);
        String content = (String) requestParam.get(Dict.CONTENT);
        Integer pipelineId = Integer.valueOf(requestParam.get(Dict.PIPELINE_ID).toString());
        String commitMessage = "??????md5?????? by pipeline " + pipelineId;
        // ??????????????????
        Map appMap = this.requestService.getAppByGitId(gitlabId);
        // ???????????????
        String name_en = (String) appMap.get(Constants.NAME_EN);
        String configRepoId = String.valueOf(makeConfigProjectExist(name_en, gitlabId));
        String fileDir = "cli-md5%2F" + name_en + "%2Einfo";
        // ??????md5???
        this.gitlabApiService.createFile(token, String.valueOf(configRepoId), Dict.MASTER, fileDir, content, commitMessage);
        // ???????????? ??????????????? config?????????id
        if (this.gitlabApiService.checkTag(configRepoId, tagName, token)) {
            this.gitlabApiService.deleteTag(configRepoId, tagName, token);
        }
        //???master???tag
        this.gitlabApiService.createTag(configRepoId, tagName, Dict.MASTER, token);
        // ??????tag
        Map paramMap = new HashMap();
        paramMap.put(Constants.BRANCH, tagName);
        paramMap.put(Constants.GITLAB_PROJECT_ID, gitlabId);
        paramMap.put(Dict.PIPELINE_ID, pipelineId);
        tagsService.saveTags(paramMap);
    }

    @Override
    public void saveSitConfigProperties(Map<String, Object> requestParam) throws Exception {
        String project_id = (String) requestParam.get(Dict.APPID);
        AppEnvMapping appEnvMapping = new AppEnvMapping();
        appEnvMapping.setApp_id(project_id);
        if (CollectionUtils.isNotEmpty(appEnvMappingDao.query(appEnvMapping))) {
            return;
        }
        List<Map> envTestList = (List<Map>) requestParam.get(Dict.ENVTESTLIST);
        //??????master??????????????????
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put(Dict.PROJECT_ID, project_id);
        dataMap.put(Dict.FEATURE_BRANCH, Dict.MASTER);
        String content = queryConfigTemplate(dataMap);
        for (Map map : envTestList) {
            Environment environment = environmentDao.queryById((String) map.get(Constants.ID));
            // ?????????????????????????????????????????????????????????
            Map<String, Object> analysisConfigFileMap = this.analysisConfigFile(content);
            List<Model> modelList = (List<Model>) analysisConfigFileMap.get(Dict.MODELS);
            Map<String, Object> replaceConfigFileMap = this.replaceConfigFile(content, modelList, environment, project_id);
            String fileCont = (String) replaceConfigFileMap.get(Dict.CONTENT);
            JSONObject appEntity = this.requestService.findByAppId(project_id);
            String nameEn = environment.getName_en();
            List<Map> envTestConfig = (List<Map>) requestParam.get(Dict.ENV_TEST_CONFIG);
            List<Map> listenvKey = new ArrayList();
            for (Map config : envTestConfig)
                if (environment.getName_cn().equals(config.get(Dict.NAME_EN))) {
                    listenvKey = (List<Map>) config.get(Dict.ENV_KEY);
                }
            Map<String, Object> envKey = new HashMap<>();
            for (Map mapKey : listenvKey) {
                envKey.putAll(mapKey);
            }
            String fdev_config_host1_ip = (String) envKey.get(Dict.FDEV_CONFIG_HOST1_IP);
            String fdev_config_host2_ip = (String) envKey.get(Dict.FDEV_CONFIG_HOST2_IP);
            String fdev_config_user = (String) envKey.get(Dict.FDEV_CONFIG_USER);
            String fdev_config_password = (String) envKey.get(Dict.FDEV_CONFIG_PASSWORD);
            String fdev_config_dir = (String) envKey.get(Dict.FDEV_CONFIG_DIR);
            String fdev_config_host_ip = "";
            if (StringUtils.isNotBlank(fdev_config_host1_ip)) {
                fdev_config_host_ip = fdev_config_host1_ip;
            } else {
                fdev_config_host_ip = fdev_config_host2_ip;
            }
            Map<String, Object> devMap = new HashMap<>();
            devMap.put(Dict.CI_PROJECT_NAME, appEntity.get(Dict.NAME_EN));
            devMap.put(Dict.FDEV_CONFIG_HOST_IP, fdev_config_host_ip);
            devMap.put(Dict.FDEV_CONFIG_USER, fdev_config_user);
            devMap.put(Dict.FDEV_CONFIG_PASSWORD, fdev_config_password);
            devMap.put(Dict.FDEV_CONFIG_DIR, fdev_config_dir);
            try {
                pushConfigFile(devMap, fileCont, environment.getName_en());
                logger.info(nameEn + "????????????");
            } catch (Exception e) {
                logger.info(nameEn + "????????????");
            }
        }
    }

    @Override
    public Map<String, Object> checkConfigFile(Map<String, Object> requestParam) throws Exception {
        Map<String, Object> result = new HashMap<>();
        String gitlabProjectId = (String) requestParam.get(Dict.GITLAB_PROJECT_ID);
        String envName = (String) requestParam.get(Dict.ENV_NAME);
        Environment environment = environmentService.queryByNameEn(envName);
        String content = (String) requestParam.get(Dict.CONTENT);
        // ?????????pro?????????tag
        boolean tag = (boolean) requestParam.get(Dict.TAG);
        // ???????????????????????????id
        Map<String, Object> appMap = this.requestService.getAppByGitId(Integer.parseInt(gitlabProjectId));
        if (CommonUtils.isNullOrEmpty(appMap)) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST);
        }
        String appId = (String) appMap.get(Dict.ID);
        // ?????????????????????????????????????????????????????????
        Map<String, Object> analysisConfigFileMap = this.analysisConfigFile(content);
        List<Model> modelList = (List<Model>) analysisConfigFileMap.get(Dict.MODELS);
        // ??????modelList???????????????????????????????????????
        Map<String, Object> replaceConfigFileMap = this.replaceConfigFile(content, modelList, environment, appId);
        List<Map<String, String>> modelEnvList = (List<Map<String, String>>) replaceConfigFileMap.get(Dict.REQUIRE);
        if (tag) {
            // ????????????id?????????????????????????????????????????????
            List<Map> appEnvList = new ArrayList();
            // ????????????id?????????????????????????????????????????????
            if(envName.contains(Constants.SCC)){
                Map<String,String> map = new HashMap();
                map.put(Dict.APPID,appId);
                map.put(Dict.DEPLOY_ENV,Dict.PRO);
                List<Environment> sccEnvList = environmentService.querySccEnvByAppId(map);
                for (Environment appEnvMap : sccEnvList) {
                    // ??????????????????id
                    Map idMap = new HashMap();
                    if (!CommonUtils.isNullOrEmpty(appEnvMap)){
                        String envId = (String) appEnvMap.getId();
                        idMap.put(Dict.ID, envId);
                        appEnvList.add(idMap);
                    }
                }
            }else{
                appEnvList = appEnvMappingDao.queryProEnvByAppId(appId);
            }
            if (CollectionUtils.isEmpty(appEnvList)) {
                throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????"});
            }
            for (Map appEnvMap : appEnvList) {
                // ??????????????????id
                String envId = (String) appEnvMap.get(Dict.ID);
                Map<String, Object> replaceProConfigFileMap = this.replaceConfigFile(content, modelList, environmentService.queryById(envId), appId);
                modelEnvList.addAll((List<Map<String, String>>) replaceProConfigFileMap.get(Dict.REQUIRE));
            }
        }
        // ??????????????????
        List<String> formatError = (List<String>) analysisConfigFileMap.get(Dict.FORMATERROR);
        List<String> modelErrorList = (List<String>) analysisConfigFileMap.get(Dict.MODELERRORLIST);
        List<String> filedErrorList = (List<String>) analysisConfigFileMap.get(Dict.FILEDERRORLIST);
        if (CollectionUtils.isEmpty(formatError) && CollectionUtils.isEmpty(modelErrorList) && CollectionUtils.isEmpty(filedErrorList) && CollectionUtils.isEmpty(modelEnvList)) {
            result.put(Dict.FLAG, true);
        } else {
            result.put(Dict.FLAG, false);
        }
        result.put(Dict.MODEL_ENV_AMP, modelEnvList);
        analysisConfigFileMap.remove(Dict.MODELS);
        analysisConfigFileMap.remove(Dict.MODEL_FIELD);
        result.putAll(analysisConfigFileMap);
        return result;
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????
     *
     * @param configFileContent
     * @return
     */
    @Override
    public Map<String, Object> analysisConfigFile(String configFileContent) {
        Map<String, Object> resultMap = new HashMap<>();
        List<String> formatErrorList = new ArrayList<>();
        List<String> modelErrorList = new ArrayList<>();
        List<String> modelFieldErrorList = new ArrayList<>();
        // ?????????????????????appkey
        List<String> leftAppKey = new ArrayList<>();
        // ????????????????????????"$<"???">"???????????????
        Map<Integer, Set<String>> recordMap = new HashMap<>();
        Set<String> modelSet = new HashSet<>();
        SetMultimap<String, Object> modelFieldMultimap = LinkedHashMultimap.create();
        // ??????????????????
        configFileContent = configFileContent.replace("\r", "");
        String[] configFileContentSplit = configFileContent.split("\n");
        for (int i = 0; i < configFileContentSplit.length; i++) {
            Integer lineNum = i + 1;
            String lineContent = configFileContentSplit[i];
            // ????????????
            if (lineContent.startsWith(Constants.NOTE_PLACEHOLDER)) {
                continue;
            }
            // ???????????????????????????
            String[] lineSplit = lineContent.split("=", 2);
            if (lineSplit.length <= 1) {
                continue;
            }
            // ?????????????????????appkey
            leftAppKey.add(lineSplit[0]);
            // ??????????????????????????????$<>????????????$<>????????????
            String rightContent = lineSplit[1];
            if (rightContent.contains("$<")) {
                String[] rightContentSplit = rightContent.split("\\$<");
                Set<String> lineModelFieldSet = new HashSet<>();
                for (int k = 1; k < rightContentSplit.length; k++) {
                    String singleRightContentSplit = rightContentSplit[k];
                    if (singleRightContentSplit.contains(">")) {
                        String modelField = singleRightContentSplit.split(">", 2)[0];
                        if (StringUtils.isEmpty(modelField) || modelField.contains("$") || modelField.contains("<")) {
                            formatErrorList.add("??????????????????" + lineNum + "????????????\"$<???????????????.?????????????????????>\"????????????");
                        } else {
                            String[] centerContentSplit = modelField.split("\\.");
                            // "$<"???">"??????????????????????????????????????????"."??????"."??????????????????????????????
                            if (!modelField.endsWith(".") && centerContentSplit.length == 2 && StringUtils.isNotEmpty(centerContentSplit[0]) && StringUtils.isNotEmpty(centerContentSplit[1])) {
                                lineModelFieldSet.add(modelField);
                                modelSet.add(centerContentSplit[0]);
                            } else {
                                formatErrorList.add("??????????????????" + lineNum + "????????????\"$<???????????????.?????????????????????>\"????????????");
                            }
                        }
                    } else {
                        formatErrorList.add("??????????????????" + lineNum + "????????????\"$<???????????????.?????????????????????>\"????????????");
                    }
                }
                recordMap.put(lineNum, lineModelFieldSet);
            }
            if (rightContent.endsWith("$<")) {
                formatErrorList.add("??????????????????" + lineNum + "????????????\"$<???????????????.?????????????????????>\"????????????");
            }
        }
        // ???????????????appKey????????????
        Set<String> repeatKey = CommonUtils.checkRepeat(leftAppKey);
        if (CollectionUtils.isNotEmpty(repeatKey)) {
            formatErrorList.add("??????????????????????????????key" + repeatKey + "???????????????");
        }
        // "$<"???">"??????????????????????????????
        List<Model> modelList = this.modelDao.queryByNameEnSet(modelSet, Constants.ONE);
        for (Map.Entry<Integer, Set<String>> recordMapEntry : recordMap.entrySet()) {
            Integer lineNum = recordMapEntry.getKey();
            Set<String> lineModelFieldSet = recordMapEntry.getValue();
            for (String lineModelField : lineModelFieldSet) {
                String[] lineModelFieldSplit = lineModelField.split("\\.");
                String modelNameEn = lineModelFieldSplit[0];
                String fieldNameEn = lineModelFieldSplit[1];
                boolean modelExistFlag = false;
                boolean filedExistFlag = false;
                for (Model model : modelList) {
                    List<Object> envKeys = model.getEnv_key();
                    if (modelNameEn != null && modelNameEn.equals(model.getName_en())) {
                        modelExistFlag = true;
                        for (Object envKey : envKeys) {
                            Map<String, String> fieldKey = (Map) envKey;
                            if (fieldNameEn != null && fieldNameEn.equals(fieldKey.get(Dict.NAME_EN))) {
                                filedExistFlag = true;
                                modelFieldMultimap.put(modelNameEn, fieldNameEn);
                                break;
                            }
                        }
                        break;
                    }
                }
                if (!modelExistFlag) {
                    modelErrorList.add("??????????????????" + lineNum + "???" + modelNameEn + "??????????????????");
                } else if (!filedExistFlag) {
                    modelFieldErrorList.add("??????????????????" + lineNum + "???" + modelNameEn + "?????????" + fieldNameEn + "??????????????????");
                }
            }
        }
        resultMap.put(Dict.FORMATERROR, formatErrorList);
        resultMap.put(Dict.MODELERRORLIST, modelErrorList);
        resultMap.put(Dict.FILEDERRORLIST, modelFieldErrorList);
        resultMap.put(Dict.MODELS, modelList);
        resultMap.put(Dict.MODEL_FIELD, modelFieldMultimap.asMap());
        return resultMap;
    }

    @Override
    public List<Map> batchSaveConfigFile(List<Map> requestList) throws Exception {
        List<Map> errorList = new ArrayList<>();
        for (Map request : requestList) {
            String appId = (String) request.get(Dict.APP_ID);
            String gitlabId = (String) request.get(Dict.GITLABID);
            String appName = (String) request.get(Dict.APPNAME);
            String tagName = (String) request.get(Dict.TAGNAME);
            Map saveReqInfo = new HashMap();
            Map errorInfo = new HashMap();
            saveReqInfo.put(Dict.BRANCH, tagName);
            saveReqInfo.put(Dict.GITLAB_PROJECT_ID, Integer.valueOf(gitlabId));
            saveReqInfo.put(Dict.PIPELINE_ID, -1);
            try {
                String filePath = this.ciConfigApplicationFile;
                boolean exists = this.gitlabApiService.checkFileExists(this.token, gitlabId, Dict.MASTER, this.ciConfigApplicationFile);
                if(!exists) {
                    filePath = this.applicationFile;
                }
                //??????fdev-application.properties????????????
                String content = gitlabApiService.getFileContent(token, gitlabId, Dict.MASTER, filePath);   //tag????????????master??????
                saveReqInfo.put(Dict.CONTENT, content);
                ///??????deployment.yaml????????????
                String yamlContent = gitlabApiService.getFileContent(token, gitlabId, Dict.MASTER, yamlFile);   //tag????????????master??????
                saveReqInfo.put(Dict.YAML_CONTENT, yamlContent);
                logger.info("??????????????????" + saveReqInfo.toString());
                saveConfigProperties(saveReqInfo);
            } catch (FdevException fdevException) {
                errorInfo.put(Dict.APP_ID, appId);
                errorInfo.put(Dict.FLAG, "0");
                errorInfo.put(Dict.ERRORINFO, errorMessageUtil.get(fdevException));
                errorList.add(errorInfo);
            } catch (Exception e) {
                errorInfo.put(Dict.APP_ID, appId);
                errorInfo.put(Dict.FLAG, 0);
                errorInfo.put(Dict.ERRORINFO, e.getMessage());
                errorList.add(errorInfo);
            }
        }
        return errorList;
    }

    /**
     * @Author??? lisy26
     * @Description??? ????????????????????????????????????
     * @Date??? 2020/12/7 19:28
     * @Param: [requestParam]
     * @return: java.util.List<java.util.Map>
     **/
    @Override
    public List<Map> batchPreviewConfigFile(List<Map> requestParam) throws Exception {
        List<Map> errList = new ArrayList<>();
        int listLength = requestParam.size();
        //???List????????????release??????
        Map releaseMap = new HashMap();
        for (int i = 0; i < listLength; i++) {
            Map appMap = new HashedMap();
            appMap = requestParam.get(i);
            String appId = (String) appMap.get(Dict.APP_ID);
            String branch = (String) appMap.get(Dict.BRANCH);
            if (branch.contains(Dict.RELEASE)) {
                if (releaseMap.containsKey(appId)) {
                    String newRel = branch;
                    String oldRel = (String) releaseMap.get(appId);
                    String[] newComp = compareRelease(newRel);
                    String[] oldComp = compareRelease(oldRel);
                    if (Integer.valueOf(newComp[0]) > Integer.valueOf(oldComp[0])) {
                        releaseMap.put(appId, branch);
                    } else if (Integer.valueOf(newComp[0]).equals(Integer.valueOf(oldComp[0]))) {
                        if (Integer.valueOf(newComp[1]) > Integer.valueOf(oldComp[1])) {
                            releaseMap.put(appId, branch);
                        }
                    }
                } else {
                    releaseMap.put(appId, branch);
                }
            }
        }

        for (int i = 0; i < listLength; i++) {
            Map errMap = new HashedMap();
            Map appMap = new HashedMap();
            appMap = requestParam.get(i);
            //???????????????????????????envName
            String gitlabId = (String) appMap.get(Dict.GITLABID);
            String appName = (String) appMap.get(Dict.APPNAME);
            String appId = (String) appMap.get(Dict.APP_ID);
            String branch = (String) appMap.get(Dict.BRANCH);
            String envName = "";
            errMap.put(Dict.APP_ID, appId);
            errMap.put(Dict.FLAG, 1);
            errMap.put(Dict.BRANCH, branch);
            errMap.put(Dict.APPNAME, appName);
            String newBranch = branch;
            if (newBranch.contains(Dict.RELEASE)) {
                newBranch = (String) releaseMap.get(appId);
            }
            envName = findEnvName(appId, gitlabId, newBranch);
            if (envName.contains("ERROR")) {
                errMap.put(Dict.FLAG, 0);
                errMap.put(Dict.ERRORINFO, envName.substring(5, envName.length()));
                errList.add(errMap);
                continue;
            } else if (envName.contains("??????")) {
                errMap.put(Dict.FLAG, 0);
                errMap.put(Dict.ERRORINFO, envName);
                errList.add(errMap);
                continue;
            }
            //?????????????????????????????????
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put(Dict.IMAGE, "");
            paramMap.put(Dict.GITLABID, gitlabId);
            paramMap.put(Dict.ENV, envName);
            Map responseMap = new HashMap();
            try {
                responseMap = appDeployMappingService.queryVariablesMapping(paramMap);
            } catch (Exception e) {
                errMap.put(Dict.FLAG, 0);
                errMap.put(Dict.ERRORINFO, "??????appDeployMappingService.queryVariablesMapping(paramMap)???????????????????????????" + e.getMessage());
                errList.add(errMap);
                continue;
            }
            String fdevConfigUser = (String) responseMap.get(Dict.FDEV_CONFIG_USER_UP);
            String fdevConfigPassword = (String) responseMap.get(Dict.FDEV_CONFIG_PASSWORD_UP);
            String fdevConfigDir = (String) responseMap.get(Dict.FDEV_CONFIG_DIR_UP);
            String fdevConfigHostIP1 = (String) responseMap.get(Dict.FDEV_CONFIG_HOST1_IP_UP);
            String fdevConfigHostIP2 = (String) responseMap.get(Dict.FDEV_CONFIG_HOST2_IP_UP);
            //?????????????????????IP
            String fdevConfigHostIP = "";
            if (StringUtils.isNotBlank(fdevConfigHostIP1)) {
                fdevConfigHostIP = fdevConfigHostIP1;
            } else {
                fdevConfigHostIP = fdevConfigHostIP2;
            }
            //??????????????????????????????
            //????????????ID???feature??????,????????????Content??????
            String content = "";
            try {
                String filePath = this.ciConfigApplicationFile;
                boolean exists = this.gitlabApiService.checkFileExists(this.token, gitlabId, newBranch, this.ciConfigApplicationFile);
                if(!exists) {
                    filePath = this.applicationFile;
                }
                content = this.gitlabApiService.getFileContent(this.token, gitlabId, newBranch, filePath);
            } catch (Exception e) {
                errMap.put(Dict.FLAG, 0);
                errMap.put(Dict.ERRORINFO, "??????queryConfigTemplate(contentMap)??????content?????????" + e.getMessage());
                errList.add(errMap);
                continue;
            }
            //????????????????????????????????????
            Map<String, Object> configMap = new HashMap<>();
            configMap.put(Dict.FDEV_CONFIG_USER, fdevConfigUser);
            configMap.put(Dict.FDEV_CONFIG_HOST_IP, fdevConfigHostIP);
            configMap.put(Dict.FDEV_CONFIG_PASSWORD, fdevConfigPassword);
            configMap.put(Dict.FDEV_CONFIG_DIR, fdevConfigDir);
            configMap.put(Dict.ENV_NAME, envName);
            configMap.put(Dict.PROJECT_ID, gitlabId);
            configMap.put(Dict.CONTENT, content);
            configMap.put(Dict.TYPE, "1");
            //??????parent??????
            if (appName.contains("-parent")) {
                int index = appName.length() - 7;
                appName = appName.substring(0, index);
            }
            configMap.put(Dict.CI_PROJECT_NAME, appName);

            try {
                Map responseTwoMap = previewConfigFile(configMap);
                logger.info("appName:" + appName.toString() + ",branch:" + branch.toString() + "??????????????????(??????)!");
            } catch (Exception e) {
                errMap.put(Dict.FLAG, 0);
                errMap.put(Dict.ERRORINFO, "??????previewConfigFile(configMap)?????????????????????????????????" + e.getMessage());
                errList.add(errMap);
                continue;
            }
            errList.add(errMap);
        }
        return errList;
    }

    /**
     * @Author??? lisy26
     * @Description??? ????????????????????????????????????
     * @Date??? 2020/12/7 19:27
     * @Param: [appId, gitlabId, branch]
     * @return: java.lang.String
     **/
    private String findEnvName(String appId, String gitlabId, String branch) {
        Map<String, Object> requestMap = new HashMap<>();
        String envName;
        if (Dict.SIT_UP.equals((String) branch)) {
            requestMap.put(Dict.CI_PROJECT_ID, Integer.valueOf(gitlabId));
            requestMap.put(Dict.CI_COMMIT_REF_NAME, branch);
            requestMap.put(Dict.CI_SCHEDULE, false);
            requestMap.put(Dict.REST_CODE, "get_sit_slug");
            try {
                List responseList = (ArrayList) this.restTransport.submit(requestMap);
                Map responseMap = (Map) responseList.get(0);
                envName = (String) responseMap.get(Dict.ENV_NAME);
            } catch (Exception e) {
                return "ERROR" + e.getMessage();
            }
            if (StringUtils.isNotEmpty(envName)) {
                return envName;
            } else {
                return "SIT?????????????????????";
            }
        } else if (branch.contains(Dict.RELEASE)) {
            requestMap.put(Dict.APPLICATION, "");
            requestMap.put(Dict.GITLAB_PROJECT_ID, Integer.valueOf(gitlabId));
            requestMap.put(Dict.RELEASE_BRANCH, branch);
            requestMap.put(Dict.REST_CODE, "queryUatEnv");
            try {
                envName = (String) this.restTransport.submit(requestMap);
            } catch (Exception e) {
                return "ERROR" + e.getMessage();
            }
            if (StringUtils.isNotEmpty(envName)) {
                return envName;
            } else {
                return "release?????????????????????";
            }
        } else if (branch.equals(Dict.MASTER)) {
            requestMap.put(Dict.APPLICATION, "");
            requestMap.put(Dict.APPLICATION_ID, appId);
            requestMap.put(Dict.GITLAB_PROJECT_ID, Integer.valueOf(gitlabId));
            requestMap.put(Dict.RELEASE_BRANCH, branch);
            requestMap.put(Dict.REST_CODE, "queryRelEnv");
            try {
                envName = (String) this.restTransport.submit(requestMap);
            } catch (Exception e) {
                return "ERROR" + e.getMessage();
            }
            if (StringUtils.isNotEmpty(envName)) {
                return envName;
            } else {
                return "master?????????????????????";
            }
        } else {
            return "????????????????????????";
        }
    }

    /**
     * @Author??? lisy26
     * @Description??? ???release????????????????????????+?????????
     * @Date??? 2020/12/7 19:29
     * @Param: [branch]
     * @return: java.lang.String[]
     **/
    private String[] compareRelease(String branch) {
        String[] num = branch.split("-");
        String[] comp = num[1].split("_");
        return comp;
    }

    /**
     * ??????????????????
     *
     * @param configFileContent
     * @param modelList
     * @param environment
     * @param appId
     * @return
     */
    private Map<String, Object> replaceConfigFile(String configFileContent, List<Model> modelList, Environment
            environment, String appId) {
        Map<String, Object> resultMap = new HashMap<>();
        // ??????????????????????????????????????????
        List<Map<String, String>> requiredModelFieldList = new ArrayList<>();
        // ???????????????????????? ??? ???????????????????????????
        Map<String, Object> fieldValueMap = this.getFieldValueMap(modelList, environment);
        List<Map<String, Map<String, String>>> requiredFieldList = (List<Map<String, Map<String, String>>>) fieldValueMap.get(Dict.REQUIRE);
        Map<String, String> fieldValue = (Map<String, String>) fieldValueMap.get(Dict.MODEL_ENV_VALUE);
        // ????????????????????????
        Map<String, String> appKeyMap = this.getAppKeyMap(appId, environment.getName_en());
        Map<String, String> modelParam = new HashMap<>();
        StringBuilder contentBuilder = new StringBuilder();
        // ??????????????????
        configFileContent = configFileContent.replace("\r", "");
        String[] configFileContentSplit = configFileContent.split("\n");
        for (int i = 0; i < configFileContentSplit.length; i++) {
            String lineContent = configFileContentSplit[i];
            // ????????????
            if (lineContent.startsWith(Constants.NOTE_PLACEHOLDER)) {
                contentBuilder.append(lineContent).append("\n");
                continue;
            }
            // ???????????????????????????
            String[] lineSplit = lineContent.split("=", 2);
            if (lineSplit.length <= 1) {
                contentBuilder.append(lineContent).append("\n");
                continue;
            }
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            String appKey = lineSplit[0];
            String outSideValue = appKeyMap.get(appKey);
            if (outSideValue != null) {
                lineContent = appKey + "=" + outSideValue;
                contentBuilder.append(lineContent).append("\n");
                continue;
            }
            // ??????????????????????????????$<>????????????$<>????????????
            String rightContent = lineSplit[1];
            if (rightContent.contains("$<")) {
                String[] rightContentSplit = rightContent.split("\\$<");
                for (int k = 1; k < rightContentSplit.length; k++) {
                    String singleRightContentSplit = rightContentSplit[k];
                    if (singleRightContentSplit.contains(">")) {
                        String modelField = singleRightContentSplit.split(">", 2)[0];
                        //??????????????????????????????????????????
                        for (Map<String, Map<String, String>> requiredField : requiredFieldList) {
                            Map<String, String> requiredFieldMap = requiredField.get(modelField);
                            // ???????????????key??????????????????????????????
                            if (MapUtils.isNotEmpty(requiredFieldMap) && outSideValue == null) {
                                requiredModelFieldList.add(requiredFieldMap);
                            }
                        }
                        // ???????????????????????????
                        String envValue = fieldValue.get(modelField);
                        if (envValue == null) {
                            envValue = Constants.ERROR;
                        }
                        modelParam.put(modelField, envValue);
                        rightContent = rightContent.replace("$<" + modelField + ">", envValue);
                    }
                }
                contentBuilder.append(appKey).append("=").append(rightContent).append("\n");
            } else {
                contentBuilder.append(lineContent).append("\n");
            }
        }
        resultMap.put(Dict.CONTENT, contentBuilder.toString());
        resultMap.put(Dict.MODELPARAM, modelParam);
        resultMap.put(Dict.OUTSIDEPARAM, appKeyMap);
        resultMap.put(Dict.REQUIRE, requiredModelFieldList);
        return resultMap;
    }

    /**
     * ???????????????????????? ??? ???????????????????????????
     *
     * @param modelList
     * @param environment
     * @return
     */
    private Map<String, Object> getFieldValueMap(List<Model> modelList, Environment environment) {
        Map<String, Object> resultMap = new HashMap<>();
        List<Map<String, Map<String, String>>> requiredFieldList = new ArrayList<>();
        HashSet<String> modelIds = new HashSet<>();
        modelList.forEach(model -> modelIds.add(model.getId()));
        List<ModelEnv> modelEnvList = this.modelEnvDao.queryModelEnvByModelsAndEnvId(modelIds, environment.getId());
        Map<String, String> fieldValueMap = new HashMap<>();
        for (Model model : modelList) {
            String modelNameEn = model.getName_en();
            List<Object> envKeys = model.getEnv_key();
            // ????????????????????????????????????????????????????????????????????????????????????????????????
            boolean haveModelEnv = false;
            for (ModelEnv modelEnv : modelEnvList) {
                Map<String, Object> variables = modelEnv.getVariables();
                if (model.getId().equals(modelEnv.getModel_id())) {
                    haveModelEnv = true;
                    for (Object envKey : envKeys) {
                        Map<String, String> fieldKey = (Map) envKey;
                        String keyId = fieldKey.get(Constants.ID);
                        String fieldNameEn = fieldKey.get(Dict.NAME_EN);
                        String modelFieldNamEn = modelNameEn + "." + fieldNameEn;
                        Set<String> variablesId = variables.keySet();
                        if (variablesId.contains(keyId)) {
                            String fieldValue = String.valueOf(variables.get(fieldKey.get(Dict.ID)));
                            fieldValueMap.put(modelFieldNamEn, StringUtils.isEmpty(fieldValue) ? "" : fieldValue);
                            if ("1".equals(fieldKey.get(Constants.REQUIRE)) && StringUtils.isEmpty(fieldValue)) {
                                addRequiredField(modelNameEn, fieldNameEn, environment.getName_en(), requiredFieldList);
                                fieldValueMap.put(modelFieldNamEn, Constants.ERROR);
                            }
                        } else {
                            if ("1".equals(fieldKey.get(Constants.REQUIRE))) {
                                addRequiredField(modelNameEn, fieldNameEn, environment.getName_en(), requiredFieldList);
                                fieldValueMap.put(modelFieldNamEn, Constants.ERROR);
                            } else {
                                fieldValueMap.put(modelFieldNamEn, "");
                            }
                        }
                    }
                }
            }
            if (!haveModelEnv) {
                for (Object envKey : envKeys) {
                    Map<String, String> fieldKey = (Map) envKey;
                    String modelFieldNamEn = modelNameEn + "." + fieldKey.get(Dict.NAME_EN);
                    if ("1".equals(fieldKey.get(Constants.REQUIRE))) {
                        // ????????????????????????
                        addRequiredField(modelNameEn, fieldKey.get(Dict.NAME_EN), environment.getName_en(), requiredFieldList);
                        fieldValueMap.put(modelFieldNamEn, Constants.ERROR);
                    } else {
                        fieldValueMap.put(modelFieldNamEn, "");
                    }
                }
            }
        }
        resultMap.put(Dict.REQUIRE, requiredFieldList);
        resultMap.put(Dict.MODEL_ENV_VALUE, fieldValueMap);
        return resultMap;
    }

    /**
     * ????????????????????????
     *
     * @param modelNameEn
     * @param fieldNameEn
     * @param envNameEn
     * @param requiredFieldList
     */
    private void addRequiredField(String modelNameEn, String fieldNameEn, String
            envNameEn, List<Map<String, Map<String, String>>> requiredFieldList) {
        Map<String, Map<String, String>> requiredField = new HashMap<>();
        Map<String, String> message = new HashMap<>();
        message.put(Dict.MODEL_NAME_EN, modelNameEn);
        message.put(Dict.MODEL_FIELD_NAME_EN, fieldNameEn);
        message.put(Dict.ENV_NAME_EN, envNameEn);
        requiredField.put(modelNameEn + "." + fieldNameEn, message);
        requiredFieldList.add(requiredField);
    }

    /**
     * ???????????????????????????????????????????????????????????????
     *
     * @param appId
     * @param envNameEn
     * @return
     */
    private Map<String, String> getAppKeyMap(String appId, String envNameEn) {
        Map<String, String> appKeyMap = new HashMap<>();
        OutSideTemplate outSideTemplate = this.outsideTemplateDao.queryByProjectId(appId);
        if (outSideTemplate != null) {
            List<Map<String, String>> variableList = outSideTemplate.getVariables();
            for (Map<String, String> params : variableList) {
                String appkey = params.get(Constants.APPKEY);
                String value = params.get(Constants.VALUE);
                if (envNameEn != null && envNameEn.equals(params.get(Constants.ENV_NAME))) {
                    appKeyMap.put(appkey, value);
                }
            }
        }
        return appKeyMap;
    }


}
