package com.spdb.fdev.fdevenvconfig.spdb.service.impl;

import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.fdevenvconfig.base.CommonUtils;
import com.spdb.fdev.fdevenvconfig.base.dict.Constants;
import com.spdb.fdev.fdevenvconfig.base.dict.Dict;
import com.spdb.fdev.fdevenvconfig.base.dict.ErrorConstants;
import com.spdb.fdev.fdevenvconfig.base.utils.DES3Util;
import com.spdb.fdev.fdevenvconfig.spdb.dao.IAppDeployMappingDao;
import com.spdb.fdev.fdevenvconfig.spdb.dao.IModelDao;
import com.spdb.fdev.fdevenvconfig.spdb.dao.IModelEnvDao;
import com.spdb.fdev.fdevenvconfig.spdb.dao.IModelTemplateDao;
import com.spdb.fdev.fdevenvconfig.spdb.dao.TagsDao;
import com.spdb.fdev.fdevenvconfig.spdb.entity.AppDeployMapping;
import com.spdb.fdev.fdevenvconfig.spdb.entity.AppOwnField;
import com.spdb.fdev.fdevenvconfig.spdb.entity.AutoConfigTags;
import com.spdb.fdev.fdevenvconfig.spdb.entity.Environment;
import com.spdb.fdev.fdevenvconfig.spdb.entity.Model;
import com.spdb.fdev.fdevenvconfig.spdb.entity.ModelEnv;
import com.spdb.fdev.fdevenvconfig.spdb.entity.ModelTemplate;
import com.spdb.fdev.fdevenvconfig.spdb.service.AppOwnFieldService;
import com.spdb.fdev.fdevenvconfig.spdb.service.IAppDeployMappingService;
import com.spdb.fdev.fdevenvconfig.spdb.service.IEnvironmentService;
import com.spdb.fdev.fdevenvconfig.spdb.service.IModelEnvService;
import com.spdb.fdev.fdevenvconfig.spdb.service.IModelService;
import com.spdb.fdev.fdevenvconfig.spdb.service.IRequestService;
import net.sf.json.JSONObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class AppDeployMappingServiceImpl implements IAppDeployMappingService {

    @Autowired
    IAppDeployMappingDao appDeployMappingDao;
    @Autowired
    IModelEnvService modelEnvService;
    @Autowired
    IModelService modelService;
    @Autowired
    private IRequestService requestService;

    @Autowired
    private IEnvironmentService environmentService;

    @Autowired
    private AppOwnFieldService appOwnFieldService;

    @Autowired
    private DES3Util des3Util;
    @Autowired
    private TagsDao tagsDao;
    @Autowired
    private IModelTemplateDao modelTemplateDao;

    @Autowired
    private IModelDao modelDao;


    @Autowired
    private IModelEnvDao modelEnvDao;

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDeployMappingServiceImpl.class);

    @Override
    public AppDeployMapping queryByGitlabId(Integer gitlabId) {
        return appDeployMappingDao.queryById(gitlabId);
    }

    /**
     * ??????????????????????????????key???value????????????????????????????????????variablesNameCnMap???null
     *
     * @param requestMap
     * @return ??????key???value???
     * @throws Exception
     */
    @Override
    public Map queryVariablesMapping(Map requestMap) throws Exception {
        return doQueryVariablesMapping(requestMap, null);
    }

    /**
     * ????????????deploy??????????????????????????????,??????key????????????
     *??
     * @param requestMap
     * @return
     * @throws Exception
     */
    public List<Map> queryVariablesMappingwithCn(Map requestMap) throws Exception {
        Map<String, Map> variablesNameCnMap = new HashMap<>();
        Map<String, String> variablesValueMap = doQueryVariablesMapping(requestMap, variablesNameCnMap);
        List<Map> result = new ArrayList<>();
        for (Map.Entry<String, Map> entry : variablesNameCnMap.entrySet()) {
            Map<String, String> variable = new HashMap<>();
            variable.put(Constants.KEY, entry.getKey());
            variable.put(Constants.NAME_CN, entry.getValue().get(Constants.NAME_CN).toString());
            variable.put(Constants.VALUE, variablesValueMap.get(entry.getKey()));
            result.add(variable);
        }
        return result;
    }

    /**
     * ????????????env,??????type=deploy????????????key???value??????key????????????List????????????????????????key???value?????????key???????????????????????????
     * ????????????????????????key???value???????????????????????????variablesNameCnMap????????????key??????key??????????????????????????????????????????????????????
     *
     * @param requestMap,variablesNameCnMap
     * @return ??????key???value??????variablesNameCnMap(???key??????key?????????)
     * @throws Exception
     */
    @Override
    public Map doQueryVariablesMapping(Map requestMap, Map variablesNameCnMap) throws Exception {
        Map result = new HashMap<>();
        Object gitlabid = requestMap.get(Constants.GITLAB_ID);
        String image = (String) requestMap.get(Dict.IMAGE);
        String env = "";
        if (!CommonUtils.isNullOrEmpty(requestMap.get(Dict.ENV))) {
            env = (String) requestMap.get(Dict.ENV);
        }
        Integer gitlabId;
        if (gitlabid instanceof String) {
            gitlabId = Integer.parseInt(gitlabid.toString());
        } else if (gitlabid instanceof Integer) {
            gitlabId = (Integer) gitlabid;
        } else {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{Constants.GITLAB_ID});
        }


        Map<String, Object> appByGitId;
        try {
            appByGitId = requestService.getAppByGitId(gitlabId);
        } catch (Exception e) {
            throw new FdevException(ErrorConstants.APP_NOT_EXIST);
        }
        String appId = (String) appByGitId.get(Dict.ID);
        //???????????????????????????????????????key--???key?????????map
        AppDeployMapping appDeployMapping = queryByGitlabId(gitlabId);
        if (appDeployMapping == null) {
            LOGGER.debug("????????? gitlabId= {} ?????????AppDeployMapping??????", gitlabId);
            return result;
        }
        List<Map> variablesAll = new ArrayList<>();
        List<Map> variables = new ArrayList<>();
        Set<String> modelNameEnSet = new HashSet<>();
        if (env.toLowerCase().contains("scc")) {
            //??????scc_variables
            variablesAll = appDeployMapping.getScc_variables();
            // ????????????????????????????????????
            if (!CommonUtils.isNullOrEmpty(variablesAll)) {
                for (Map var : variablesAll) {
                    String modelKey = (String) var.get(Dict.MODEL_KEY);
                    String[] modelKeySplit = modelKey.split("\\.", 2);
                    variables.add(var);
                    modelNameEnSet.add(modelKeySplit[0]);
                }
            }
        } else {
            variablesAll = appDeployMapping.getVariables();
            // ????????????????????????????????????
            for (Map var : variablesAll) {
                String modelKey = (String) var.get(Dict.MODEL_KEY);
                String[] modelKeySplit = modelKey.split("\\.", 2);
                String modelName = modelKeySplit[0];
                String[] modelNameSpilt = modelName.split("_");
                if (!(modelNameSpilt[0].equals(Dict.CI) && modelNameSpilt[1].equals(Dict.SCC))) {
                    variables.add(var);
                }
                modelNameEnSet.add(modelKeySplit[0]);
            }

        }
        List<Model> modelList = this.modelService.queryByNameEnSet(modelNameEnSet, Constants.ONE);
        List<Model> models = new ArrayList<>();

        if (env.toLowerCase().contains("scc")) {
            models = modelList;
        } else {
            // ??????scc??????????????????
            for (Model model : modelList) {
                if (!(model.getFirst_category().equals(Dict.CI) && model.getSecond_category().equals(Dict.SCC))) {
                    models.add(model);
                }
            }
        }

        // ?????????????????????key--value?????????map(?????????)????????????????????????????????????
        String envName = (String) requestMap.get(Constants.ENV);
        List<Map<String, Object>> modelKeyValueCnMapping = modelEnvService.queryEnvwithNameCnBySlug(appId, models, envName);

        Map<String, Map> modelKeyCnMapping = new HashMap<>();
        Map<String, String> modelKeyValue = new HashMap<>();
        //????????????key---value?????????map
        for (Map keyValue : modelKeyValueCnMapping) {
            Object keyValueObj = keyValue.get(Constants.VALUE);
            if (CommonUtils.isNullOrEmpty(keyValueObj)) {
                modelKeyValue.put(keyValue.get(Constants.NAME_EN).toString(), "");
            } else {
                if (keyValueObj instanceof String) {
                    modelKeyValue.put(keyValue.get(Constants.NAME_EN).toString(), keyValueObj.toString());
                } else {
                    modelKeyValue.put(keyValue.get(Constants.NAME_EN).toString(), com.alibaba.fastjson.JSONObject.toJSONString(keyValueObj));
                }
            }
        }
        //????????????key---value?????????map
        for (Map var : variables) {
            String modelKey = modelKeyValue.get(var.get(Constants.MODEL_KEY));
            if (CommonUtils.isNullOrEmpty(modelKey)) {
                result.put(var.get(Constants.CI_KEY), "");
            } else {
                result.put(var.get("ci_key"), modelKey);
            }
        }
        //??????variablesNameCnMap????????????
        if (variablesNameCnMap != null) {
            for (Map keyValue : modelKeyValueCnMapping) {
                modelKeyCnMapping.put(keyValue.get(Constants.NAME_EN).toString(), keyValue);
            }
            //????????????key---???key??????????????????map
            for (Map var : variables) {
                Map modelKeyCn = modelKeyCnMapping.get(var.get(Constants.MODEL_KEY));
                if (CommonUtils.isNullOrEmpty(modelKeyCn)) {
                    Map<String, String> map = new HashMap<>();
                    map.put(Constants.NAME_EN, var.get(Constants.MODEL_KEY).toString());
                    Map modelVar = this.modelService.queryModelVariables(map);
                    if (modelVar == null) {
                        modelVar = new HashMap<String, String>();
                        modelVar.put(Constants.NAME_CN, "");
                        modelVar.put(Constants.REQUIRE, "0");
                    }
                    variablesNameCnMap.put(var.get(Constants.CI_KEY), modelVar);
                } else {
                    variablesNameCnMap.put(var.get("ci_key"), modelKeyCn);
                }
            }
        }
        // ???????????????????????????Dockerfile????????????????????????
        if (StringUtils.isNotEmpty(image)) {
            Map<String, String> paramMap = new HashMap<>();
            paramMap.put("gitlabId", gitlabid.toString());
            paramMap.put(Dict.IMAGE, image);
            String imageVersion = requestService.queryBaseImageVersion(paramMap);
            if (StringUtils.isNotEmpty(imageVersion)) {
                result.put(Dict.FDEV_CAAS_BASE_IMAGE_VERSION, imageVersion);
            }
        }
        return result;
    }

    @Override
    public void update(AppDeployMapping deployMapping) {
        appDeployMappingDao.update(deployMapping);
    }

    @Override
    public Map<String, Object> queryByPage(int page, int per_page) {
        if (CommonUtils.isNullOrEmpty(page) || CommonUtils.isNullOrEmpty(per_page)) {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{Dict.PAGE + "," + Dict.PER_PAGE, Constants.PARAM_CAN_NOT_NULL});
        }
        return appDeployMappingDao.queryByPage(page, per_page);
    }

    @Override
    public Map<String, Object> queryByPage(Map<String, Object> requestMap, List<Integer> gitLabIdList) {
        String appId = (String) requestMap.get(Dict.APPID);
        int page = (int) requestMap.get(Dict.PAGE);
        int perPage = (int) requestMap.get(Dict.PER_PAGE);
        // ????????????id????????????GitLab id
        Integer gitLabId = null;
        if (StringUtils.isNotEmpty(appId)) {
            try {
                JSONObject appInfo = requestService.findByAppId(appId);
                gitLabId = appInfo.getInt(Dict.GITLAB_PROJECT_ID);
            } catch (Exception e) {
                Map<String, Object> emptyMap = new HashMap<>();
                emptyMap.put(Dict.TOTAL, 0);
                emptyMap.put(Dict.LIST, new ArrayList<>());
                return emptyMap;
            }
        }
        Map<String, Object> appDeployMap = appDeployMappingDao.queryByAppIdPage(gitLabId, gitLabIdList, page, perPage);
        List<AppDeployMapping> appDeployMappingList = (List<AppDeployMapping>) appDeployMap.get(Dict.LIST);
        // ?????????????????????
        List<Map<String, Object>> returnList = new ArrayList<>();
        for (AppDeployMapping appDeployMapping : appDeployMappingList) {
            Map<String, Object> modelSetMap = new HashMap<>();
            modelSetMap.put(Dict.NAMECN, appDeployMapping.getModelSet());
            Map<String, Object> returnMap = new HashMap<>();
            returnMap.put(Dict.MODELSET, modelSetMap);
            returnMap.put(Dict.GITLAB_PROJECT_ID, appDeployMapping.getGitlabId());
            returnList.add(returnMap);
        }
        appDeployMap.put(Dict.LIST, returnList);
        return appDeployMap;
    }

    @Override
    public AppDeployMapping save(AppDeployMapping deployMapping) throws Exception {
        return appDeployMappingDao.add(deployMapping);
    }

    @Override
    public List<AppDeployMapping> queryByGitlabIds(Set<Integer> pageGitLabIdList) {
        return appDeployMappingDao.queryByGitlabIds(pageGitLabIdList);
    }

    @Override
    public Map<String, Object> queryImagepwd(Map<String, String> requestMap) throws Exception {
        String env = "";
        if (!CommonUtils.isNullOrEmpty(requestMap.get(Dict.ENV))) {
            env = (String) requestMap.get(Dict.ENV);
        }
        Map<String, Object> variablesMapping = null;
        Map<String, Object> userAndPwd = new HashMap<>();

        //?????????SCC??????
        if (env.toLowerCase().contains("scc")) {
            variablesMapping = doQuerySccVariablesMapping(requestMap);
            if (MapUtils.isEmpty(variablesMapping)) {
                throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"???????????????????????????????????????"});
            }
            userAndPwd.put(Constants.DOCKERSERVICE_USER, variablesMapping.get(Constants.DOCKERSERVICE_USER));
            String registryPwd = (String) variablesMapping.get(Constants.DOCKERSERVICE_PASSWD);
            if (StringUtils.isNotEmpty(registryPwd)) {
                userAndPwd.put(Constants.DOCKERSERVICE_PASSWD, registryPwd);
            }

        } else {

            //?????????CaaS??????
            variablesMapping = doQueryVariablesMapping(requestMap, null);
            if (MapUtils.isEmpty(variablesMapping)) {
                throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"???????????????????????????????????????"});
            }
            userAndPwd.put(Constants.FDEV_CAAS_REGISTRY_USER_UP, variablesMapping.get(Constants.FDEV_CAAS_REGISTRY_USER_UP));
            String registryPwd = (String) variablesMapping.get(Constants.FDEV_CAAS_REGISTRY_PD_UP);
            if (StringUtils.isNotEmpty(registryPwd)) {
                userAndPwd.put(Constants.FDEV_CAAS_REGISTRY_PD_UP, des3Util.decrypt(registryPwd));
            }
        }

        return userAndPwd;
    }

    @Override
    public AppDeployMapping queryTagsByGitlabId(Integer gitlabId) {
        AutoConfigTags autoConfigTags = tagsDao.queryByGitlabId(gitlabId);
        AppDeployMapping appDeployMapping = appDeployMappingDao.queryById(gitlabId);
        // ??????????????????????????????AppDeployMapping????????????????????????
        if (appDeployMapping == null) {
            AppDeployMapping appDeployMappingData = new AppDeployMapping();
            List<Map<String, Object>> tagInfo = autoConfigTags.getTagInfo();
            appDeployMappingData.setTagInfo(tagInfo);
            appDeployMappingData.setConfigGitlabId(autoConfigTags.getConfigGitlabId());
            appDeployMappingData.setGitlabId(autoConfigTags.getGitlab_id());
            return appDeployMappingData;
        }
        if (autoConfigTags != null) {
            List<Map<String, Object>> tagInfo = autoConfigTags.getTagInfo();
            appDeployMapping.setTagInfo(tagInfo);
            appDeployMapping.setConfigGitlabId(autoConfigTags.getConfigGitlabId());
        }
        return appDeployMapping;
    }

    @Override
    public Map queryAllVariablesMapping(Map<String, Object> paramMap) throws Exception {
        return doQueryAllVariablesMapping(paramMap);
    }

    @Override
    public Map querySccVariablesMapping(Map<String, Object> paramMap) throws Exception {
        return doQuerySccVariablesMapping(paramMap);
    }

    private Map doQueryAllVariablesMapping(Map<String, Object> requestMap) throws Exception {
        Map result = new HashMap<>();
        Object gitlabid = requestMap.get(Constants.GITLAB_ID);
        String image = (String) requestMap.get(Dict.IMAGE);
        Integer gitlabId;
        if (gitlabid instanceof String) {
            gitlabId = Integer.parseInt(gitlabid.toString());
        } else if (gitlabid instanceof Integer) {
            gitlabId = (Integer) gitlabid;
        } else {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{Constants.GITLAB_ID});
        }
        Map<String, Object> appByGitId;
        try {
            appByGitId = requestService.getAppByGitId(gitlabId);
        } catch (Exception e) {
            throw new FdevException(ErrorConstants.APP_NOT_EXIST);
        }
        String appId = (String) appByGitId.get(Dict.ID);
        //???????????????????????????????????????key--???key?????????map
        AppDeployMapping appDeployMapping = queryByGitlabId(gitlabId);
        if (appDeployMapping == null) {
            LOGGER.debug("????????? gitlabId= {} ?????????AppDeployMapping??????", gitlabId);
            return result;
        }
        List<Map> variablesAll = appDeployMapping.getVariables();

        // ????????????????????????????????????
        Set<String> modelNameEnSet = new HashSet<>();
        for (Map var : variablesAll) {
            String modelKey = (String) var.get(Dict.MODEL_KEY);
            String[] modelKeySplit = modelKey.split("\\.", 2);
            String modelName = modelKeySplit[0];
            String[] modelNameSpilt = modelName.split("_");
            modelNameEnSet.add(modelKeySplit[0]);
        }
        List<Model> modelList = this.modelService.queryByNameEnSet(modelNameEnSet, Constants.ONE);

        // ?????????????????????key--value?????????map(?????????)????????????????????????????????????
        String envName = (String) requestMap.get(Constants.ENV);
        List<Map<String, Object>> modelKeyValueCnMapping = modelEnvService.queryEnvwithNameCnBySlug(appId, modelList, envName);

        Map<String, Map> modelKeyCnMapping = new HashMap<>();
        Map<String, String> modelKeyValue = new HashMap<>();
        //????????????key---value?????????map
        for (Map keyValue : modelKeyValueCnMapping) {
            Object keyValueObj = keyValue.get(Constants.VALUE);
            if (CommonUtils.isNullOrEmpty(keyValueObj)) {
                modelKeyValue.put(keyValue.get(Constants.NAME_EN).toString(), "");
            } else {
                if (keyValueObj instanceof String) {
                    modelKeyValue.put(keyValue.get(Constants.NAME_EN).toString(), keyValueObj.toString());
                } else {
                    modelKeyValue.put(keyValue.get(Constants.NAME_EN).toString(), com.alibaba.fastjson.JSONObject.toJSONString(keyValueObj));
                }
            }
        }
        //????????????key---value?????????map
        for (Map var : variablesAll) {
            String modelKey = modelKeyValue.get(var.get(Constants.MODEL_KEY));
            if (CommonUtils.isNullOrEmpty(modelKey)) {
                result.put(var.get(Constants.CI_KEY), "");
            } else {
                result.put(var.get("ci_key"), modelKey);
            }
        }
        return result;
    }

    private Map doQuerySccVariablesMapping(Map requestMap) throws Exception {
        Map result = new HashMap<>();
        Object gitlabid = requestMap.get(Constants.GITLAB_ID);
        String image = (String) requestMap.get(Dict.IMAGE);
        Integer gitlabId;
        if (gitlabid instanceof String) {
            gitlabId = Integer.parseInt(gitlabid.toString());
        } else if (gitlabid instanceof Integer) {
            gitlabId = (Integer) gitlabid;
        } else {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{Constants.GITLAB_ID});
        }
        Map<String, Object> appByGitId;
        try {
            appByGitId = requestService.getAppByGitId(gitlabId);
        } catch (Exception e) {
            throw new FdevException(ErrorConstants.APP_NOT_EXIST);
        }
        String appId = (String) appByGitId.get(Dict.ID);
        //???????????????????????????????????????key--???key?????????map
        AppDeployMapping appDeployMapping = queryByGitlabId(gitlabId);
        if (appDeployMapping == null) {
            LOGGER.debug("????????? gitlabId= {} ?????????AppDeployMapping??????", gitlabId);
            return result;
        }
        List<Map> variablesAll = appDeployMapping.getScc_variables();

        // ????????????????????????????????????
        Set<String> modelNameEnSet = new HashSet<>();
        for (Map var : variablesAll) {
            String modelKey = (String) var.get(Dict.MODEL_KEY);
            String[] modelKeySplit = modelKey.split("\\.", 2);
            String modelName = modelKeySplit[0];
            String[] modelNameSpilt = modelName.split("_");
            modelNameEnSet.add(modelKeySplit[0]);
        }
        List<Model> modelList = this.modelService.queryByNameEnSet(modelNameEnSet, Constants.ONE);

        // ?????????????????????key--value?????????map(?????????)????????????????????????????????????
        String envName = (String) requestMap.get(Constants.ENV);
        List<Map<String, Object>> modelKeyValueCnMapping = modelEnvService.queryEnvwithNameCnBySlug(appId, modelList, envName);

        Map<String, Map> modelKeyCnMapping = new HashMap<>();
        Map<String, String> modelKeyValue = new HashMap<>();
        //????????????key---value?????????map
        for (Map keyValue : modelKeyValueCnMapping) {
            Object keyValueObj = keyValue.get(Constants.VALUE);
            if (CommonUtils.isNullOrEmpty(keyValueObj)) {
                modelKeyValue.put(keyValue.get(Constants.NAME_EN).toString(), "");
            } else {
                if (keyValueObj instanceof String) {
                    modelKeyValue.put(keyValue.get(Constants.NAME_EN).toString(), keyValueObj.toString());
                } else {
                    modelKeyValue.put(keyValue.get(Constants.NAME_EN).toString(), com.alibaba.fastjson.JSONObject.toJSONString(keyValueObj));
                }
            }
        }
        //????????????key---value?????????map
        for (Map var : variablesAll) {
            String modelKey = modelKeyValue.get(var.get(Constants.MODEL_KEY));
            if (CommonUtils.isNullOrEmpty(modelKey)) {
                result.put(var.get(Constants.CI_KEY), "");
            } else {
                result.put(var.get("ci_key"), modelKey);
            }
        }
        return result;
    }


    @Override
    public Map querySpecifiedVariablesMapping(Map<String, Object> requestMap) throws Exception {

        Map<String, Object> resultMap = new HashMap<String, Object>();
        Object gitlabId = requestMap.get(Dict.GITLABID);
        String appId = (String) requestMap.get(Dict.APP_ID);
        String envName = (String) requestMap.get(Dict.ENVNAME);
        Integer gitLabId;
        if (!CommonUtils.isNullOrEmpty(gitlabId)) {

            if (gitlabId instanceof String) {
                gitLabId = Integer.parseInt(gitlabId.toString());
            } else if (gitlabId instanceof Integer) {
                gitLabId = (Integer) gitlabId;
            } else {
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{Constants.GITLAB_ID});
            }
            // ??????gitlabId????????????id
            Map<String, Object> appByGitId;
            try {
                appByGitId = requestService.getAppByGitId(gitLabId);
            } catch (Exception e) {
                throw new FdevException(ErrorConstants.APP_NOT_EXIST);
            }
            appId = (String) appByGitId.get(Dict.ID);
        } else if (!StringUtils.isEmpty(appId)) {
            // ????????????id??????gitLabId
            JSONObject appInfo = requestService.findByAppId(appId);
            gitLabId = appInfo.getInt(Dict.GITLAB_PROJECT_ID);
        } else {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{Dict.GITLABID + "???" + Dict.APP_ID + "?????????"});
        }

        AppDeployMapping appDeployMapping = queryByGitlabId(gitLabId);
        if (appDeployMapping == null) {
            LOGGER.debug("????????? gitlabId= {} ?????????AppDeployMapping??????", gitLabId);
            return resultMap;
        }
        // ???????????????????????????id
        Environment envEntity = new Environment();
        envEntity.setName_en(envName);
        List<Environment> envEntityList = this.environmentService.query(envEntity);
        if (CollectionUtils.isEmpty(envEntityList)) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????: " + envName});
        }
        envEntity = envEntityList.get(0);
        String envId = envEntity.getId();

        AppOwnField appOwnFieldParam = new AppOwnField();
        appOwnFieldParam.setApp_Id(appId);
        appOwnFieldParam.setEnv_id(envId);
        AppOwnField appOwnField = appOwnFieldService.queryByAppIdAndEnvId(appOwnFieldParam);
        Map<String, Object> appModelFieldValueMap = new HashMap<>();
        if (appOwnField != null) {
            List<Map> appModelFieldValueList = appOwnField.getModelFleld_value();
            for (Map map : appModelFieldValueList) {
                appModelFieldValueMap.put((String) map.get(Dict.MODEL_FIELD), map.get(Dict.VALUE));
            }
        }


        Set<String> modelNameSet = new HashSet<>();
        if (envEntity.getLabels().contains("scc")) {
            modelNameSet = queryBindModels(appDeployMapping, "scc");
        } else if (envEntity.getLabels().contains("caas")) {
            modelNameSet = queryBindModels(appDeployMapping, "caas");
        }
        // ?????????????????????keyId, ??????????????????id?????????id???????????????????????????????????????????????????id??????????????????
        List<String> keyList = (List<String>) requestMap.get(Dict.KEYLIST);
        for (int i = 0; i < keyList.size(); i++) {
            String keyName = keyList.get(i);
            Map resMap = queryValue(modelNameSet, keyName, envId, appModelFieldValueMap);
            boolean isExist = (boolean) resMap.get("isExist");
            if (envEntity.getLabels().contains("scc") && envEntity.getLabels().contains("caas") && !isExist) {
                modelNameSet = queryBindModels(appDeployMapping, "caas");
                resMap = queryValue(modelNameSet, keyName, envId, appModelFieldValueMap);
            }
            resultMap.put(keyName, resMap.get("keyValue"));
        }
        return resultMap;
    }

    /**
     *  ????????????????????????????????????????????????
     * @param modelNameSet
     * @param keyName
     * @param envId
     * @param appModelFieldValueMap
     * @return
     */
    private Map<String, Object> queryValue(Set<String> modelNameSet, String keyName, String envId, Map appModelFieldValueMap) {
        Map<String, Object> resMap = new HashMap<String, Object>();
        Object keyValue = null;
        boolean isExist = false;
        for (String modelName : modelNameSet) {
            Model model = this.modelDao.queryVaribleForOne(modelName, keyName);
            List<Object> env_key = model.getEnv_key();
            if (!CommonUtils.isNullOrEmpty(env_key)) {
                Map<String, Object> envKeyMap = (Map<String, Object>) env_key.get(0);
                String keyId = (String) envKeyMap.get(Constants.ID);
                String modelId = model.get_id().toString();
                ModelEnv modelEnv = modelEnvDao.getModelEnv(modelId, envId);
                if (modelEnv != null) {
                    Map<String, Object> variables = modelEnv.getVariables();
                    keyValue = variables.get(keyId);
                    String modelFieldName = modelName + "." + keyName;
                    if (MapUtils.isNotEmpty(appModelFieldValueMap) && appModelFieldValueMap.containsKey(modelFieldName)) {
                        keyValue = appModelFieldValueMap.get(modelFieldName);
                    }
                }
                isExist = true; //??????????????????????????????
                break;
            }
        }
        resMap.put("keyValue", keyValue);
        resMap.put("isExist", isExist);
        return resMap;
    }

    /**
     * ?????????????????????scc????????????caas??????
     *
     * @param appDeployMapping
     * @param envLabel
     * @return
     */
    private Set<String> queryBindModels(AppDeployMapping appDeployMapping, String envLabel) {
        List<Map> variablesAll = new ArrayList<Map>();
        if ("caas".equals(envLabel)) {
            variablesAll = appDeployMapping.getVariables();
        } else if ("scc".equals(envLabel)) {
            variablesAll = appDeployMapping.getScc_variables();
        }
        // ????????????????????????????????????
        Set<String> modelNameSet = new HashSet<>();
        if (variablesAll != null && variablesAll.size() >= 1){
            for (Map var : variablesAll) {
                String modelKey = (String) var.get(Dict.MODEL_KEY);
                String[] modelKeySplit = modelKey.split("\\.", 2);
                modelNameSet.add(modelKeySplit[0]);
            }
        }
        return modelNameSet;
    }
}
