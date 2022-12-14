package com.spdb.fdev.spdb.service.impl;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.spdb.fdev.base.dict.Constants;
import com.spdb.fdev.base.dict.Dict;
import com.spdb.fdev.base.dict.ErrorConstants;
import com.spdb.fdev.base.utils.CommonUtil;
import com.spdb.fdev.common.User;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.common.util.FdevUserCacheUtil;
import com.spdb.fdev.common.util.UserVerifyUtil;
import com.spdb.fdev.common.util.Util;
import com.spdb.fdev.spdb.dao.IEntityDao;
import com.spdb.fdev.spdb.dao.IEntityTemplateDao;
import com.spdb.fdev.spdb.dao.IServiceConfigDao;
import com.spdb.fdev.spdb.entity.Entity;
import com.spdb.fdev.spdb.entity.EntityField;
import com.spdb.fdev.spdb.entity.EntityTemplate;
import com.spdb.fdev.spdb.entity.ServiceConfig;
import com.spdb.fdev.spdb.service.IConfigFileService;
import com.spdb.fdev.spdb.service.IGitlabApiService;
import com.spdb.fdev.spdb.service.IRestService;
import com.spdb.fdev.transport.RestTransport;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RefreshScope
public class ConfigFileServiceImpl implements IConfigFileService {

    @Value("${record.switch}")
    private Boolean recordSwitch;
    @Value("${gitlab.token}")
    private String token;
    @Value("${gitlab.application.file}")
    private String applicationFile;
    @Value("${gitlab.application.file.ci}")
   	private String applicationFileCi;
    @Value("${gitlab.application.yaml.file}")
    private String yamlFile;
    @Value("${gitlab.ci-application.file}")
    private String ci_applicationFile;
    @Autowired
    private IGitlabApiService gitlabApiService;
    @Autowired
    public UserVerifyUtil userVerifyUtil;
    @Autowired
    IRestService restService;
    @Autowired
    private IEntityDao entityDao;
    @Autowired
    private IEntityTemplateDao entityTemplateDao;
    @Autowired
    private IServiceConfigDao serviceConfigDao;
    @Autowired
    private RestTransport restTransport;
    @Autowired
    private FdevUserCacheUtil fdevUserCacheUtil;

    /**
     * ????????????????????????
     *
     * @param requestParam
     */
    @Override
    public Map saveConfigTemplate(Map<String, Object> requestParam) throws Exception {
        // ????????????
        String content = ((String) requestParam.get(Dict.CONTENT)).trim();
        // ?????????????????????
        String featureBranch = ((String) requestParam.get(Dict.FEATUREBRANCH)).trim();
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
        stringBuilder.append(Dict.COMMIT_MESSAGE);
        if (user != null && StringUtils.isNotEmpty(user.getEmail())) {
            stringBuilder.append(" by ").append(user.getEmail());
        }

        String gitlabId = "";
        String applicationFile = "";
        if (CommonUtil.isNullOrEmpty(requestParam.get(Dict.PROJECTID))) {     //???????????????????????????????????????
            gitlabId = ((String) requestParam.get(Dict.GITLABID)).trim();
            applicationFile = this.ci_applicationFile;
            // ?????? ??????????????????
            this.gitlabApiService.checkBranch(this.token, gitlabId, featureBranch);
        } else {
        	// ?????????????????????
            String projectId = ((String) requestParam.get(Dict.PROJECTID)).trim();
            // ??????id ??????????????????
            Map<String, Object> app  = this.restService.queryApp(projectId);
            gitlabId = ((Integer) app.get(Dict.GITLAB_PROJECT_ID)).toString();
             // ?????? ??????????????????
            this.gitlabApiService.checkBranch(this.token, gitlabId, featureBranch);
        	//??????ci?????????????????????
        	String oldContent = this.gitlabApiService.getFileContent(this.token, gitlabId, featureBranch, this.applicationFileCi);
    		//????????????gitlab-ci?????????????????????  ??????????????? ci ?????????????????????ci?????????
        	if(CommonUtil.isNullOrEmpty(oldContent)) {
        		//??????gitlab-ci?????????????????????
    			oldContent = this.gitlabApiService.getFileContent(this.token, gitlabId, featureBranch, this.applicationFile);
    			//???????????? ci ?????????????????????ci????????? ?????????????????? gitlab-ci?????????
    			if(CommonUtil.isNullOrEmpty(oldContent)) {
    				//ci/fdev-application.properties
    				applicationFile = this.applicationFileCi;
    			}else {
    				//gitlab-ci/fdev-application.properties
    				applicationFile = this.applicationFile;
    			}
    		}else
    			//ci/fdev-application.properties
    			applicationFile = this.applicationFileCi;
    		
        }
        // ????????????????????????
        this.gitlabApiService.createFile(this.token, gitlabId, featureBranch, applicationFile, content, stringBuilder.toString());
        return null;
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????
     *
     * @param configFileContent
     * @return
     */
    @Override
    public Map<String, Object> analysisConfigFile(String configFileContent) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        List<String> formatErrorList = new ArrayList<>();
        List<String> modelErrorList = new ArrayList<>();
        List<String> modelFieldErrorList = new ArrayList<>();
        // ?????????????????????appkey
        List<String> leftAppKey = new ArrayList<>();
        // ????????????????????????"$<"???">"???????????????
        Map<Integer, Set<String>> recordMap = new HashMap<>();
        Set<String> entityEnSet = new HashSet<>();
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
                                entityEnSet.add(centerContentSplit[0]);
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
        Set<String> repeatKey = CommonUtil.checkRepeat(leftAppKey);
        if (CollectionUtils.isNotEmpty(repeatKey)) {
            formatErrorList.add("??????????????????????????????key" + repeatKey + "???????????????");
        }
        // "$<"???">"??????????????????????????????
        List<Entity> entityList = this.entityDao.queryByNameEnSet(entityEnSet);
        Set<Map.Entry<Integer, Set<String>>> entries = recordMap.entrySet();
        for (Map.Entry<Integer, Set<String>> recordMapEntry : recordMap.entrySet()) {
            Integer lineNum = recordMapEntry.getKey();
            Set<String> lineModelFieldSet = recordMapEntry.getValue();
            for (String lineModelField : lineModelFieldSet) {
                String[] lineModelFieldSplit = lineModelField.split("\\.");
                String modelNameEn = lineModelFieldSplit[0];
                String fieldNameEn = lineModelFieldSplit[1];
                boolean modelExistFlag = false;
                boolean filedExistFlag = false;
                for (Entity  entity : entityList) {
                	String templateId = entity.getTemplateId();
                	List<Map<String, Object>> properties = entity.getProperties();
                	if(!CommonUtil.isNullOrEmpty(templateId)) {
                		 EntityTemplate entityTemplate = entityTemplateDao.queryById(templateId);
                		 properties = entityTemplate.getProperties();
                	}
                    if (modelNameEn != null && modelNameEn.equals(entity.getNameEn())) {
                        modelExistFlag = true;
                        for (Map propertie : properties) {
                            if (fieldNameEn != null && fieldNameEn.equals(propertie.get(Dict.NAMEEN))) {
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
        resultMap.put(Dict.MODELS, entityList);
        resultMap.put(Dict.MODEL_FIELD, modelFieldMultimap.asMap());
        return resultMap;
    }

	@Override
	public Map<String, Object> queryConfigDependency(Map<String, Object> requestParam) throws Exception {
		Integer page = (Integer) requestParam.get(Dict.PAGE);//??????
        Integer perPage = (Integer) requestParam.get(Dict.PERPAGE);//????????????
        String entityNameEn = (String) requestParam.get(Dict.ENTITYNAMEEN);//???????????????
		List<ServiceConfig> serviceConfigs = serviceConfigDao.queryServiceConfig(requestParam);
		Long count = serviceConfigDao.queryServiceConfigCount(requestParam); 
		List<Map> apps = new ArrayList<Map>();
		Set<String> fieldList = new HashSet<String>();
		if(!CommonUtil.isNullOrEmpty(page) && !CommonUtil.isNullOrEmpty(perPage)){
			//??????gitId??????
			Set<Integer> gitlabIds = new HashSet<Integer>();
			//??????map
			Map<String,String> branchMap = new HashMap<String,String>();
			for (ServiceConfig serviceConfig : serviceConfigs) {
				gitlabIds.add(Integer.valueOf(serviceConfig.getServiceGitId()));
				branchMap.put(serviceConfig.getServiceGitId(), serviceConfig.getBranch());
			}
			
			if(!CommonUtil.isNullOrEmpty(gitlabIds)) {
				//????????????git??????????????????
				Map<String, Object> map = new HashMap<String, Object>();
				map.put(Dict.GITLABIDS, gitlabIds);
				apps = (List<Map>)restService.queryApps(map);
			}
		}else {
			for(ServiceConfig serviceConfig : serviceConfigs) {
				List<EntityField> entityFieldList = serviceConfig.getEntityField();
				for(EntityField entityField : entityFieldList) {
					if(entityNameEn.equals(entityField.getEntityNameEn()) && !CommonUtil.isNullOrEmpty(entityField.getFields()) ) {
						fieldList.addAll(entityField.getFields());
					}
				}
			}
		}
			
		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put(Dict.COUNT, count);
		returnMap.put(Dict.SERVICELIST, apps);
		returnMap.put(Dict.FIELDS, fieldList);
		return returnMap;
	}

	private Map<String, Object> queryServiceByGitlabId(Integer gitlabId) throws Exception {
        Map<String, Object> map = new HashMap<String, Object>();
        List<Integer> gitlabIds = new ArrayList<>();
        gitlabIds.add(gitlabId);
        map.put(Dict.GITLABIDS, gitlabIds);
        List apps = (List<Map>)restService.queryApps(map);
        Map<String, Object> appMap = new HashMap<>();
        if(apps.size() > 0){
            appMap = (Map<String, Object>) apps.get(0);

            //??????????????????
            appMap.put(Dict.GROUPNAME, (String)(!Util.isNullOrEmpty(appMap) ? appMap.get("name") : ""));
            return appMap;
        }
       return null;
    }

    @Override
    public Map<String, Object> queryDeployDependency(Map<String, Object> requestParam) throws Exception {
        String entityNameEn = (String) requestParam.get(Dict.ENTITYNAMEEN);//???????????????
        List<String> fields =   (List<String>) requestParam.get(Dict.FIELDS);//??????????????????
        Integer page = (Integer) requestParam.get(Dict.PAGE);//??????
        Integer perPage = (Integer) requestParam.get(Dict.PERPAGE);//????????????
        Map<String, Object> result = new HashMap<>();//????????????
        Set<String> set = new HashSet();
        set.add(entityNameEn);
        List<Entity> entitys = entityDao.queryByNameEnSet(set);
        if(CommonUtil.isNullOrEmpty(entitys)){
            throw new FdevException(ErrorConstants.INVAILD_OPERATION_DATA);
        }
        String entityId = entitys.get(0).getId();
        List<String> entityIds = new ArrayList<>();
        entityIds.add(entityId);
        //??????????????????????????????????????????????????????
        Map<String, Object> param = new HashMap<>();
        param.put(Dict.ENTITYIDS, entityIds);
        param.put(Dict.PAGENUM, page);
        param.put(Dict.PAGESIZE, perPage);
        param.put(Dict.REST_CODE,"getPipelineByEntityId");
        List<Map<String,Object>> pipelineInfo = (List<Map<String, Object>>) restTransport.submit(param);
        List<Map<String, Object>> serviceList = new ArrayList<>();
        Integer count = 0;
        if(!CommonUtil.isNullOrEmpty(pipelineInfo)){
            Map<String, Object> pipelines = pipelineInfo.get(0);
            //??????????????????
            Map<String, Object> serviceMap = new HashMap();
            List<Map<String, Object>> pipelineList = (List<Map<String, Object>>) pipelines.get("pipelineList");
            count = (Integer) pipelines.get(Dict.COUNT);
            if(count != 0){
                for(Map<String, Object> pipeline:pipelineList){
                    Map<String, Object> bindProject = (Map<String, Object>) pipeline.get("bindProject");
                    serviceMap = this.queryServiceByGitlabId((Integer) bindProject.get("gitlabProjectId"));
                    if(!CommonUtil.isNullOrEmpty(serviceMap)){
                        serviceMap.put(Dict.PIPELINEID,pipeline.get(Dict.PIPELINEID).toString());
                        serviceMap.put(Dict.PIPELINENAME,pipeline.get(Dict.PIPELINENAME).toString());
                        serviceMap.put(Dict.PIPELINENAMEID,pipeline.get(Dict.NAMEID).toString());
                    }else {
                        //??????????????????
                        if(count > 0){
                            count--;
                        }
                    }
                    serviceList.add(serviceMap);
                }
            }

        }
        result.put(Dict.COUNT,count);
        result.put(Dict.SERVICELIST,serviceList);
        return result;
    }
}
