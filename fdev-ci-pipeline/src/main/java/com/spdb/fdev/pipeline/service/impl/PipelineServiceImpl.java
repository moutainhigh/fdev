package com.spdb.fdev.pipeline.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.ValueFilter;
import com.spdb.fdev.base.dict.Constants;
import com.spdb.fdev.base.dict.Dict;
import com.spdb.fdev.base.dict.ErrorConstants;
import com.spdb.fdev.base.listener.SaveMongoEventListener;
import com.spdb.fdev.base.utils.CommonUtils;
import com.spdb.fdev.base.utils.DiffUtils;
import com.spdb.fdev.base.utils.ObjectUtil;
import com.spdb.fdev.base.utils.VaildateUtil;
import com.spdb.fdev.common.User;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.pipeline.dao.*;
import com.spdb.fdev.pipeline.entity.*;
import com.spdb.fdev.pipeline.service.*;
import org.bson.types.ObjectId;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Encoder;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class PipelineServiceImpl implements IPipelineService {

    @Autowired
    private IPluginService pluginService;
    @Autowired
    private IImageDao imageDao;
    @Autowired
    private IPluginDao pluginDao;

    @Autowired
    private IJobExeService jobExeService;

    @Autowired
    private IAppService appService;

    @Autowired
    private IPipelineExeService pipelineExeService;

    @Autowired
    private IPipelineDao pipelineDao;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IPipelineExeDao pipelineExeDao;

    @Value("${gitlab.manager.username}")
    private String gitUserName;

    @Value("${gitlab.manager.password}")
    private String gitPassword;

    @Autowired
    private VaildateUtil vaildateUtil;

    @Autowired
    private SaveMongoEventListener mongoEventListener;

    @Autowired
    private IGitlabService gitlabService;

    @Autowired
    private IUserService userService;

    @Autowired
    private IJobExeDao jobExeDao;
    @Autowired
    private IAppService iAppService;

    @Autowired
    private IPluginService iPluginService;

    @Value("${mioBuket}")
    private String mioBuket;

    @Value("${minio.endpoint}")
    private String minioEndPoint;

    @Value("${minio.firstKey}")
    private String minioAccessKey;

    @Value("${minio.secondKey}")
    private String minioSecretKey;

    @Value("${gitlab.api.url}")
    private String gitLabApiUrl;

    @Value("${gitlab.manager.token}")
    private String userGitToken;

    @Value("${gitlab.token}")
    private String gitlabToken;

    @Value("${fdev.domain}")
    private String domain;

    @Value("${envUrl}")
    private String envUrl;

    @Value("${relPluginRelativeRootUrl}")
    private String relPluginRelativeRootUrl;

    @Autowired
    IFileService fileService;

    @Autowired
    IPipelineTemplateDao pipelineTemplateDao;

    @Autowired
    private IRunnerClusterService runnerClusterService;

    @Autowired
    private IPipelineUpdateDiffDao pipelineUpdateDiffDao;

    @Value("${jfrog.username}")
    private String username;

    @Value("${jfrog.password}")
    private String password;

    @Value("${group.role.admin.id}")
    private String groupRoleAdminId;

    @Autowired
    private IDigitalService digitalService;

    @Value("${pipelineTemplate.white.list:}")
    private List<String> whiteList;

    @Value("#{${env.plugin.map:}}")
    private Map<String, List<String>> envPluginMap;

    private static final Logger logger = LoggerFactory.getLogger(PipelineServiceImpl.class);

    @Override
    public String triggerPipeline(String pipelineId, String branch, Boolean tagFlag, String triggerType, List<Map> runVariables, List<Map> exeSkippedSteps, String userId) throws Exception {
        Pipeline orgPipeline = pipelineDao.queryById(pipelineId);
        if (CommonUtils.isNullOrEmpty(orgPipeline)) {
            logger.error("**********pipeline not exist, pipelineId=" + pipelineId);
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"pipelineId" + pipelineId});
        }
        Pipeline resPipeline = CommonUtils.copyEntityObj(orgPipeline, Pipeline.class);
        //????????????????????????
        if (!CommonUtils.isNullOrEmpty(exeSkippedSteps)) {
            Map skipMap = new HashMap();
            for (Map skipInput : exeSkippedSteps) {
                //???????????????????????????????????????skipMap?????????
                if (CommonUtils.isNullOrEmpty(skipInput.get(Dict.STAGEINDEX)) ||
                        CommonUtils.isNullOrEmpty(skipInput.get(Dict.JOBINDEX)) ||
                        CommonUtils.isNullOrEmpty(skipInput.get(Dict.STEPINDEX))
                ) {
                    continue;
                    // throw new FdevException(ErrorConstants.PARAMS_ERROR, new String[]{"??????????????????????????????????????????"});
                } else {
                    int stageIndexParam = (int) skipInput.get(Dict.STAGEINDEX);
                    int jobIndexParam = (int) skipInput.get(Dict.JOBINDEX);
                    int stepIndexParam = (int) skipInput.get(Dict.STEPINDEX);
                    String skipIndex = new StringBuffer().append(stageIndexParam).append("_")
                            .append(jobIndexParam).append("_").append(stepIndexParam).toString();
                    skipMap.put(skipIndex, null);
                }
            }
            logger.info("******triggerPipeline[" + resPipeline.getId() + "] input skipped step index :" + skipMap);

            if (!skipMap.isEmpty()) {
                //???????????????????????????
                List<Stage> stages = orgPipeline.getStages();
                for (int i = 0; i < stages.size(); i++) {
                    Stage stage = stages.get(i);
                    List<Job> jobs = stage.getJobs();
                    for (int j = 0; j < jobs.size(); j++) {
                        int removeJobNum = 0;
                        Job job = jobs.get(j);
                        List<Step> steps = job.getSteps();
                        //?????????????????????????????????????????????????????????
                        List<Step> resSteps = resPipeline.getStages().get(i).getJobs().get(j).getSteps();
                        resSteps.clear();
                        for (int k = 0; k < steps.size(); k++) {
                            String currentIndex = new StringBuffer().append(i).append("_")
                                    .append(j).append("_").append(k).toString();
                            if (!skipMap.containsKey(currentIndex)) {
                                //??????????????????????????????????????????????????????
                                resSteps.add(steps.get(k));
                            }
                        }
                        logger.info("******triggerPipeline[" + resPipeline.getId() + "] stage [" + i + "]" + "job [" + j + "] steps info:"
                                + JSONObject.toJSONString(resSteps));
                    }

                }

                //??????step??????,??????job; job??????,??????stage???stage???????????????
                List<Stage> resultStages = resPipeline.getStages();
                List<Stage> resultStages1 = new ArrayList<Stage>();
                for (int i = 0; i < resultStages.size(); i++) {
                    List<Job> resJobs = resultStages.get(i).getJobs();
                    List<Job> resJobs1 = new ArrayList<Job>();
                    for (int j = 0; j < resJobs.size(); j++) {
                        if (!resJobs.get(j).getSteps().isEmpty()) {
                            resJobs1.add(resJobs.get(j));
                        } else {
                            logger.info("******triggerPipeline[" + resPipeline.getId() + "] stage [" + i + "]"
                                    + "job [" + j + "] removed !");
                        }
                    }
                    resultStages.get(i).setJobs(resJobs1);
                    if (!resultStages.get(i).getJobs().isEmpty()) {
                        resultStages1.add(resultStages.get(i));
                    } else {
                        logger.info("******triggerPipeline[" + resPipeline.getId() + "] stage [" + i + "]"
                                + " removed !");
                    }
                }
                if (resultStages1.isEmpty()) {
                    throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"?????????????????????????????????????????????"});
                } else {
                    resPipeline.setStages(resultStages1);
                }
                logger.info("******after skip steps, triggerPipeline[" + resPipeline.getId() + "] info:" + JSONObject.toJSONString(resPipeline));
            }
        }
        BindProject bindProject = resPipeline.getBindProject();
        //?????????????????????pipelineExe
        Author author = null;
        if (triggerType.equals(Dict.SCHEDULE)) {
            author = new Author();
            author.setId(Dict.SYSTEM);
            author.setNameCn(Dict.SYSTEM);
            author.setNameEn(Dict.SYSTEM);
        } else if (CommonUtils.isNullOrEmpty(userId)) {
            author = userService.getAuthor();
        } else if (!CommonUtils.isNullOrEmpty(userId)) {
            //???userId??????????????????
            Map userInfo = userService.queryUserById(userId);
            author = new Author();
            author.setId((String) userInfo.get(Dict.ID));
            author.setNameCn((String) userInfo.get(Dict.USER_NAME_CN));
            author.setNameEn((String) userInfo.get(Dict.USER_NAME_EN));
        }
        //??????tagFlag????????????????????????pipeline??????????????????tag
        resPipeline.setRunVariables(runVariables);
        return executePipeline(resPipeline, triggerType, author, branch, bindProject.getGitlabProjectId(), tagFlag);
    }

    @Override
    public String add(Pipeline pipeline) throws Exception {
        boolean isAppManager = vaildateUtil.projectCondition(pipeline.getBindProject().getProjectId());
        if (!isAppManager) {
            throw new FdevException(ErrorConstants.USER_NOT_APPMANAGER);
        }
        //??????id
        Map project = appService.queryAppDetailById(pipeline.getBindProject().getProjectId());
        if (CommonUtils.isNullOrEmpty(project)) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"???????????????????????????"});
        }
//        String groupLineId = vaildateUtil.getProjectSectionId(project);
//        if (CommonUtils.isNullOrEmpty(groupLineId)) {
//            //?????????????????????
//            Map group = (Map) project.get(Dict.GROUP);
//            String groupId = (String) group.get(Dict.ID);
//            logger.error("******cannot find project section groupId ,projectNameEn:" + pipeline.getBindProject().getNameEn()
//                    + ", projectGroupId:" + groupId);
////            throw new FdevException(ErrorConstants.USER_IS_NOT_EXISTS_LINEID);
//        } else
//            pipeline.setGroupLineId(groupLineId);
        CommonUtils.checkCronExpression(pipeline);
        CommonUtils.stagesCheck(pipeline.getStages());
        //????????????
        pipelineDao.deleteDraft(userService.getUserFromRedis().getId());
        pipeline.setNameId(null);
        //???plugin????????????????????????pipeline
        preparePipeline(pipeline);
        pipeline.setVersion(Constants.UP_CHANGE_VERSION);
        pipeline.setAuthor(userService.getAuthor());
        String pipelineId = pipelineDao.add(pipeline);
        updateUseCountWhenAdd(pipelineId);
        return pipelineId;
    }

    @Override
    public Map addByTemplateNameIdAndBindProject(String nameId, BindProject bindProject, String userId) throws Exception {
        boolean isAppManager = vaildateUtil.projectConditionNoAuth(bindProject.getProjectId(), userId);
        if (!isAppManager) {
            throw new FdevException(ErrorConstants.USER_NOT_APPMANAGER);
        }
        PipelineTemplate pipelineTemplate = pipelineTemplateDao.queryByNameId(nameId);
        if (CommonUtils.isNullOrEmpty(pipelineTemplate) || pipelineTemplate == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"???????????????nameId" + nameId});
        }
        if (CommonUtils.isNullOrEmpty(bindProject)) {
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"????????????"});
        }
        Pipeline pipeline = new Pipeline();
//        String groupLineId = checkGetGroupLineId(userId, pipeline);
//        if (groupLineId == null) {
//            //???????????????????????????????????? or ????????????????????????????????????
//            logger.debug(">>>>>>>>>> current user is not exists group line <<<<<<<<<<<<<");
////            throw new FdevException(ErrorConstants.USER_IS_NOT_EXISTS_LINEID);
//        } else
//            pipeline.setGroupLineId(groupLineId);
        pipeline.setName(pipelineTemplate.getName());
        pipeline.setDesc(pipelineTemplate.getDesc());
        pipeline.setBindProject(bindProject);
        pipeline.setPipelineTemplateId(pipelineTemplate.getId());
        pipeline.setPipelineTemplateNameId(pipelineTemplate.getNameId());
        pipeline.setFixedModeFlag(pipelineTemplate.getFixedModeFlag());
        pipeline.setStages(pipelineTemplate.getStages());
        //???????????????image
        /*List<Stage> stages = pipeline.getStages();
        for (Stage stage : stages) {
            for (Job job : stage.getJobs()) {
                Images image = job.getImage();
                if (CommonUtils.isNullOrEmpty(image)) {
                    job.setImage(pipelineDao.findDefaultImage());
                }
            }
        }*/
        pipeline.setVersion(Constants.UP_CHANGE_VERSION);
        if (CommonUtils.isNullOrEmpty(userId)) {
            //??????userId?????????????????????????????????
            //??????userId?????????????????????id????????????????????????????????????????????????????????????????????????sourceback?????????
            pipeline.setAuthor(userService.getAuthor());
        } else {
            Author author = new Author();
            author.setId(userId);
            Map userInfo = this.userService.queryUserById(userId);
            String userNameEn = (String) userInfo.get("user_name_en");
            String userNameCn = (String) userInfo.get("user_name_cn");
            author.setNameEn(userNameEn);
            author.setNameCn(userNameCn);
            pipeline.setAuthor(author);
        }
        String pipelineId = pipelineDao.add(pipeline);
        //????????????????????????
        updateUseCountWhenAdd(pipelineId);
        Map resultMap = new HashMap<>();
        resultMap.put(Dict.ID, pipelineId);
        resultMap.put(Dict.NAMEID, pipelineId);
        resultMap.put(Dict.PIPELINENAME, pipelineTemplate.getName());
        return resultMap;
    }

    public Pipeline preparePipeline(Pipeline pipeline) throws Exception {
        for (Stage stage : pipeline.getStages()) {
            for (Job job : stage.getJobs()) {
                //1 ??????runner??????????????????
                if (CommonUtils.isNullOrEmpty(job.getRunnerClusterId())) {
                    logger.error("runnerCluster cannot be empty !pipelineName: " + pipeline.getName() + " jobName: " + job.getName());
                    throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"??????[" + job.getName() + "]???????????????"});
                }
                String runnerClusterID = job.getRunnerClusterId();
                RunnerCluster runnerCluster = runnerClusterService.getRunnerClusterById(runnerClusterID);
                if (runnerCluster == null) {
                    logger.error("runnerCluster not exist! runnerClusterID: " + runnerClusterID + " pipelineName: " + pipeline.getName() + " jobName: " + job.getName());
                    throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"??????[" + job.getName() + "]???????????????????????????????????????"});
                }
                //2 ???????????????id????????????????????????linux,???????????????????????????????????????
                String runnerPlatform = runnerCluster.getPlatform();
                if (runnerPlatform.equals(Constants.PLATFORM_LINUX)) {
                    if (job.getImage() == null || CommonUtils.isNullOrEmpty(job.getImage().getId())) {
                        logger.error("image cannot be empty !pipelineName: " + pipeline.getName() + " jobName: " + job.getName());
//                        throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"??????[" + job.getName() + "]???????????????"});
                    }
                } else {
                    job.setImage(null);
                }
                //3 ????????????id??????????????????????????????????????????????????????
                for (Step step : job.getSteps()) {
                    PluginInfo inputPlugin = step.getPluginInfo();
                    Plugin plugin = pluginDao.queryPluginDetail(inputPlugin.getPluginCode());
                    if (plugin == null) {
                        logger.error("**********plugin not exist, pluginId=" + inputPlugin.getPluginCode());
                        throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"??????[" + step.getName() + "]?????????"});
                    }
                    if (CommonUtils.isNullOrEmpty(plugin.getPlatform()) || !plugin.getPlatform().contains(runnerPlatform)) {
                        logger.error("platform not match !"
                                + " runnerPlatform: " + runnerPlatform
                                + " pluginplatform: " + plugin.getPlatform()
                                + " plugin: " + plugin.getPlatform());
                        throw new FdevException(ErrorConstants.PARAMS_IS_ILLEGAL, new String[]{"??????[" + plugin.getPluginName() + "]?????????" + runnerPlatform + "?????????????????????????????????????????????"});
                    }
                    //????????????????????????
                    prepareStep(step);
                }
            }
        }
        return pipeline;
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param step
     * @return
     * @throws Exception
     */
    @Override
    public Step prepareJobTemplateStep(Step step) throws Exception {
        //1 ??????????????????
        PluginInfo inputPlugin = step.getPluginInfo();

        Plugin plugin = pluginDao.queryPluginDetail(inputPlugin.getPluginCode());
        if (plugin == null) {
            logger.error("**********plugin not exist, pluginId=" + inputPlugin.getPluginCode());
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"plugin not exist"});
        }
        //????????????????????????????????????
        if (plugin.getStatus().equals(Constants.STATUS_CLOSE)) {
            List<Plugin> pluginList = pluginDao.queryPluginDetailByNameId(plugin.getNameId());
            plugin = pluginDao.queryPluginDetail(pluginList.get(0).getPluginCode());
        }

        inputPlugin.setName(plugin.getPluginName());
        step.setName(plugin.getPluginName());
        inputPlugin.setDesc(plugin.getPluginDesc());
        inputPlugin.setPluginCode(plugin.getPluginCode());

        //2 ????????????????????????????????????????????????????????????????????????????????????
        preparePluginArtifacts(inputPlugin, plugin);
        //3 ???????????????????????????script??????
        preparePluginScript(inputPlugin, plugin);
        //4 ??????????????????????????????????????????
        List<Map<String, Object>> params = new ArrayList<>();
        List<Map<String, Object>> pluginParams = plugin.getParams();
        if (!CommonUtils.isNullOrEmpty(pluginParams)) {
            //??????????????????
            List inputParamList = inputPlugin.getParams();
            Map<String, Map> inputParamMaps = CommonUtils.listToMap(inputParamList, Dict.KEY);
            //??????????????????????????????????????????????????????
            for (Map pluginParam : pluginParams) {
                Map resPluginParam = new HashMap();
                //??????????????????????????????DEFAULT,DEFAULTARR,HINT,LABEL,REQUIRED,HIDDEN,ITEMVALUE,
                resPluginParam.putAll(pluginParam);

                String key = (String) pluginParam.get(Dict.KEY);
                String type = (String) pluginParam.get(Dict.TYPE);
                if (inputParamMaps.containsKey(key)) {
                    //?????????????????????????????????
                    Map inputParam = inputParamMaps.get(key);
                    resPluginParam.put(Dict.VALUE, inputParam.get(Dict.VALUE));

                    //??????????????????????????????input???multipleInput???password???select, multipleSelect, fileEdit,fileUpload,entityType
                    //????????????????????????????????????????????????temporary?????????upload
                    if (Objects.equals(type, Dict.FILEUPLOAD)) {
                        String value = (String) inputParam.get(Dict.VALUE);
                        value = renameUploadFile(value);
                        resPluginParam.put(Dict.VALUE, value);
                    } else if (type.equals(Dict.ENTITYTYPE)) {
                        //?????????????????????????????????id ????????????
                        List entityTemplateList = (List) pluginParam.get(Dict.ENTITYTEMPLATEPARAMS);
                        List<Map> inputEntityTempParamList = (List<Map>) inputParam.get(Dict.ENTITYTEMPLATEPARAMS);
                        List list = prepareEntityTemplateParam(entityTemplateList, inputEntityTempParamList);
                        //???????????????????????????????????????????????????????????????????????????????????????
                        resPluginParam.put(Dict.ENTITYTEMPLATEPARAMS, list);
                    } else if (type.equals(Dict.MULTIPLESELECT)) {
                        resPluginParam.put(Dict.VALUEARRAY, inputParam.get(Dict.VALUEARRAY));
                    }
                } else {
                    //???????????????????????????
                    resPluginParam.put(Dict.VALUE, pluginParam.get(Dict.DEFAULT));
                    if (type.equals(Dict.ENTITYTYPE)) {
                        List entityTemplateList = (List) pluginParam.get(Dict.ENTITYTEMPLATEPARAMS);
                        if (!CommonUtils.isNullOrEmpty(entityTemplateList)) {
                            Map entityMap = new HashMap();
                            Map entityTemplateMap = (Map) entityTemplateList.get(0);
                            entityMap.put(Dict.ID, entityTemplateMap.get(Dict.ID));
                            entityMap.put(Dict.NAMEEN, entityTemplateMap.get(Dict.NAMEEN));
                            entityMap.put(Dict.NAMECN, entityTemplateMap.get(Dict.NAMECN));
                            entityMap.put(Dict.ENTITY, null);
                            entityMap.put(Dict.ENV, null);
                            //????????????????????????????????????????????????
                            List entityParamsList = prepareEntityParams((String) entityMap.get(Dict.ID), null);
                            entityMap.put(Dict.ENTITYPARAMS, entityParamsList);
                            List list = new ArrayList();
                            list.add(entityMap);
                            resPluginParam.put(Dict.ENTITYTEMPLATEPARAMS, list);
                        }
                    } else if (type.equals(Dict.MULTIPLESELECT)) {
                        resPluginParam.put(Dict.VALUEARRAY, pluginParam.get(Dict.DEFAULTARR));
                    }
                }
                params.add(resPluginParam);
            }
        }
        inputPlugin.setParams(params);
        return step;
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param step
     * @return
     * @throws Exception
     */
    @Override
    public Step prepareStep(Step step) throws Exception {
        //1 ??????????????????
        PluginInfo inputPlugin = step.getPluginInfo();
        Plugin plugin = pluginDao.queryPluginDetail(inputPlugin.getPluginCode());
        if (plugin == null) {
            logger.error("**********plugin not exist, pluginId=" + inputPlugin.getPluginCode());
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"plugin not exist"});
        }

        inputPlugin.setName(plugin.getPluginName());
        inputPlugin.setDesc(plugin.getPluginDesc());
        inputPlugin.setPluginCode(plugin.getPluginCode());

        //2 ????????????????????????????????????????????????????????????????????????????????????
        preparePluginArtifacts(inputPlugin, plugin);
        //3 ???????????????????????????script??????
        preparePluginScript(inputPlugin, plugin);
        //4 ??????????????????????????????????????????
        List<Map<String, Object>> params = new ArrayList<>();
        List<Map<String, Object>> pluginParams = plugin.getParams();
        if (!CommonUtils.isNullOrEmpty(pluginParams)) {
            //??????????????????
            List inputParamList = inputPlugin.getParams();
            Map<String, Map> inputParamMaps = CommonUtils.listToMap(inputParamList, Dict.KEY);
            //??????????????????????????????????????????????????????
            for (Map pluginParam : pluginParams) {
                String key = (String) pluginParam.get(Dict.KEY);
                String type = (String) pluginParam.get(Dict.TYPE);
                Map resPluginParam = new HashMap();
                resPluginParam.put(Dict.KEY, pluginParam.get(Dict.KEY));
                resPluginParam.put(Dict.TYPE, pluginParam.get(Dict.TYPE));
                if (inputParamMaps.containsKey(key)) {
                    //?????????????????????????????????
                    Map inputParam = inputParamMaps.get(key);
                    resPluginParam.put(Dict.VALUE, inputParam.get(Dict.VALUE));
                    resPluginParam.put(Dict.VALUEARRAY, inputParam.get(Dict.VALUEARRAY));

                    //input???multipleInput???password???select, multipleSelect, fileEdit,fileUpload,entityType
                    //????????????????????????????????????????????????temporary?????????upload
                    if (Objects.equals(type, Dict.FILEUPLOAD)) {
                        String value = (String) inputParam.get(Dict.VALUE);
                        value = renameUploadFile(value);
                        resPluginParam.put(Dict.VALUE, value);
                    } else if (type.equals(Dict.ENTITYTYPE)) {
                        //?????????????????????????????????id ????????????
                        List entityTemplateList = (List) pluginParam.get(Dict.ENTITYTEMPLATEPARAMS);
                        List<Map> inputEntityTempParamList = (List<Map>) inputParam.get(Dict.ENTITYTEMPLATEPARAMS);
                        List list = prepareEntityTemplateParam(entityTemplateList, inputEntityTempParamList);
                        //???????????????????????????????????????????????????????????????????????????????????????
                        resPluginParam.put(Dict.ENTITYTEMPLATEPARAMS, list);
                    }
                } else {
                    //???????????????????????????
                    resPluginParam.put(Dict.VALUE, pluginParam.get(Dict.DEFAULT));
                    if (type.equals(Dict.ENTITYTYPE)) {
                        List entityTemplateList = (List) pluginParam.get(Dict.ENTITYTEMPLATEPARAMS);
                        if (!CommonUtils.isNullOrEmpty(entityTemplateList)) {
                            Map entityMap = new HashMap();
                            Map entityTemplateMap = (Map) entityTemplateList.get(0);
                            entityMap.put(Dict.ID, entityTemplateMap.get(Dict.ID));
                            entityMap.put(Dict.NAMEEN, entityTemplateMap.get(Dict.NAMEEN));
                            entityMap.put(Dict.NAMECN, entityTemplateMap.get(Dict.NAMECN));
                            entityMap.put(Dict.ENTITY, null);
                            entityMap.put(Dict.ENV, null);
                            //????????????????????????????????????????????????
                            List entityParamsList = prepareEntityParams((String) entityMap.get(Dict.ID), null);
                            entityMap.put(Dict.ENTITYPARAMS, entityParamsList);
                            List list = new ArrayList();
                            list.add(entityMap);
                            resPluginParam.put(Dict.ENTITYTEMPLATEPARAMS, list);
                        }
                    } else if (type.equals(Dict.MULTIPLESELECT)) {
                        resPluginParam.put(Dict.VALUEARRAY, pluginParam.get(Dict.DEFAULTARR));
                    }
                }
                params.add(resPluginParam);
            }
        }
        inputPlugin.setParams(params);
        return step;
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????script??????
     *
     * @param inputPlugin
     * @param plugin
     * @throws Exception
     */
    private void preparePluginScript(PluginInfo inputPlugin, Plugin plugin) throws Exception {
        if (CommonUtils.isNullOrEmpty(plugin.getScript())) {
            inputPlugin.setScript(null);
        } else {
            logger.info("====test1:" + inputPlugin.getScriptCmd());
            logger.info("====test11:" + inputPlugin.getScriptUpdateFlag());
            //??????????????????????????????????????????????????????????????????????????????????????????????????????
            String scriptCmd = inputPlugin.getScriptCmd();
            //boolean scriptUpdateFlag = inputPlugin.getScriptUpdateFlag();
            if (!CommonUtils.isNullOrEmpty(inputPlugin.getScriptUpdateFlag()) && inputPlugin.getScriptUpdateFlag()) {
                Map script = uploadScript(scriptCmd);
                inputPlugin.setScript(script);
            }
        }
    }

    /**
     * ????????????????????????????????????????????????????????????????????????????????????
     *
     * @param inputPlugin
     * @param plugin
     */
    private void preparePluginArtifacts(PluginInfo inputPlugin, Plugin plugin) {
        Map<String, Object> pluginArtifactsInfo = plugin.getArtifacts();
        if (!CommonUtils.isNullOrEmpty(pluginArtifactsInfo)) {
            if (!CommonUtils.isNullOrEmpty(inputPlugin.getArtifacts())) {
                pluginArtifactsInfo.put(Dict.VALUE, inputPlugin.getArtifacts().get(Dict.VALUE));
            } else {
                pluginArtifactsInfo.put(Dict.VALUE, pluginArtifactsInfo.get(Dict.DEFAULT));
            }
            pluginArtifactsInfo.remove(Dict.DEFAULT);
        }
        inputPlugin.setArtifacts(pluginArtifactsInfo);
    }

    /**
     * ????????????????????????????????????????????????minio
     *
     * @param scriptCmd
     * @throws Exception
     */
    private Map uploadScript(String scriptCmd) throws Exception {
        if (CommonUtils.isNullOrEmpty(scriptCmd)) {
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"scriptCmd"});
        }
        Map script = new HashMap();
        String id = new ObjectId().toString();
        String filePath = id + ".sh";
        byte[] bytes = scriptCmd.getBytes();
        //????????????
        MockMultipartFile multipartFile = new MockMultipartFile(Dict.FILE, filePath, "text/plain", bytes);
        //??????minio
        Map<String, String> restMap = iPluginService.uploadFile(multipartFile, id);
        //??????mio??????
        script.put(Dict.MINIO_OBJECT_NAME, restMap.get(Dict.PACKAGE_PATH));
        logger.info("**********the script hava changed???new Address???" + script);
        return script;
    }

    /**
     * ????????????????????????????????????????????????temporary?????????upload
     *
     * @param sourceFileName
     * @return
     * @throws FileNotFoundException
     */
    private String renameUploadFile(String sourceFileName) throws FdevException {
        if (!CommonUtils.isNullOrEmpty(sourceFileName)) {
            String[] splitLeftSlash = sourceFileName.split("/");
            int i = splitLeftSlash.length - 1;
            int j = splitLeftSlash.length - 2;
            String fileName = splitLeftSlash[i];
            String idDirectory = splitLeftSlash[j];
            File nasRelUrlFile = new File(relPluginRelativeRootUrl + idDirectory);
            if (!nasRelUrlFile.exists()) {
                nasRelUrlFile.mkdirs();
            }
            String saveUrl = relPluginRelativeRootUrl + idDirectory + "/" + fileName;
            String temporary = splitLeftSlash[2];
            if (Dict.TEMPORARY.equals(temporary)) {
                try {
                    File sourceFile = new File(sourceFileName);
                    File outFile = new File(saveUrl);
                    sourceFile.renameTo(outFile);
                } catch (Exception e) {
                    logger.error("rename file failed, " + sourceFileName);
                    throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"???????????????????????????"});
                }
                sourceFileName = saveUrl;
            }
        }
        return sourceFileName;
    }

    /**
     * ??????????????????????????????
     *
     * @param pluginEntityTemplateList ??????param????????????????????????
     * @param inputEntityTempParamList ?????????????????????????????????
     * @return ???????????????????????????????????????
     * @throws Exception
     */
    public List prepareEntityTemplateParam(List pluginEntityTemplateList, List<Map> inputEntityTempParamList) throws Exception {
        List resEntityParamList = new ArrayList();
        if (!CommonUtils.isNullOrEmpty(pluginEntityTemplateList)) {
            Map pluginEntityTemplate = (Map) pluginEntityTemplateList.get(0);
            if (!CommonUtils.isNullOrEmpty(inputEntityTempParamList)) {
                for (int i = 0; i < inputEntityTempParamList.size(); i++) {
                    //??????????????????????????????????????????,?????????????????????????????????????????????????????????????????????
                    Map inputParamMap = inputEntityTempParamList.get(i);
                    if (CommonUtils.isNullOrEmpty(inputParamMap.get(Dict.ID)) || !((String) pluginEntityTemplate.get(Dict.ID)).equals((String) inputParamMap.get(Dict.ID))) {
                        //???????????????????????????ID?????????????????????id??????
                        continue;
                    }
                    Map resEntityMap = new HashMap();
                    resEntityMap.putAll(pluginEntityTemplate);
                    //????????????????????????????????????????????????????????????
                    Map entity = null;
                    if (!CommonUtils.isNullOrEmpty(inputParamMap.get(Dict.ENTITY))) {
                        entity = (Map) inputParamMap.get(Dict.ENTITY);
                    }

                    if (!CommonUtils.isNullOrEmpty(entity) && entity.containsKey(Dict.ID)) {
                        //????????????
                        resEntityMap.put(Dict.ENTITY, inputParamMap.get(Dict.ENTITY));
                        resEntityMap.put(Dict.ENV, inputParamMap.get(Dict.ENV));
                        resEntityMap.put(Dict.ENTITYPARAMS, null);
                    } else {
                        //????????????
                        resEntityMap.put(Dict.ENTITY, null);
                        resEntityMap.put(Dict.ENV, null);
                        //?????????????????????????????????????????????
                        List<Map> inputEntityParamList = (List<Map>) inputParamMap.get(Dict.ENTITYPARAMS);
                        List entityParamsList = prepareEntityParams((String) pluginEntityTemplate.get(Dict.ID), inputEntityParamList);
                        resEntityMap.put(Dict.ENTITYPARAMS, entityParamsList);
                    }
                    resEntityParamList.add(resEntityMap);
                }
            }
        }

        return resEntityParamList;
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param entityTemplateId  ????????????idist
     * @param inputEntityParams ????????????????????????????????????entityParams  ???????????????
     * @return
     * @throws Exception
     */
    public List prepareEntityParams(String entityTemplateId, List<Map> inputEntityParams) throws Exception {
        List entityParamsList = new ArrayList();
        /*Map inputEntityParamMap = null;
        if (!CommonUtils.isNullOrEmpty(inputEntityParams)) {
            inputEntityParamMap = CommonUtils.listToMap(inputEntityParams, Dict.KEY);
        }
        List<Map> tempParamList = iAppService.queryModelTemplateDetailInfo(entityTemplateId);
        if (!CommonUtils.isNullOrEmpty(tempParamList)) {
            for (Map tempParam : tempParamList) {
                Map entityParam = new HashMap();
                entityParam.put(Dict.KEY, tempParam.get(Dict.NAMEEN));
                entityParam.put(Dict.LABEL, tempParam.get(Dict.NAMECN));
                entityParam.put(Dict.REQUIRED, tempParam.get(Dict.REQUIRED));
                //??????????????????????????????value
                String value = null;
                if (!CommonUtils.isNullOrEmpty(inputEntityParamMap)) {
                    Map inputEntityParam = (Map) inputEntityParamMap.get(tempParam.get(Dict.NAMEEN));
                    if (!CommonUtils.isNullOrEmpty(inputEntityParam)) {
                        value = (String) inputEntityParam.get(Dict.VALUE);
                    }
                }
                entityParam.put(Dict.VALUE, value);
                entityParamsList.add(entityParam);
            }
        }*/
        /**
         * ???????????????????????????????????????????????????plugin?????????????????????ID????????????????????????key??????????????????????????????????????????????????????
         * ??????????????????????????????Pipeline??????entityParams???????????????????????????????????????????????????
         */
        if (CommonUtils.isNullOrEmpty(inputEntityParams)) {
            return entityParamsList;
        }
        for (Map inputMapItem : inputEntityParams) {
            Map entityParam = new HashMap();
            entityParam.put(Dict.KEY, inputMapItem.get(Dict.KEY));
            entityParam.put(Dict.LABEL, inputMapItem.get(Dict.LABEL));
            entityParam.put(Dict.REQUIRED, Boolean.TRUE);
            //??????????????????????????????value
            String value = null;
            if (!CommonUtils.isNullOrEmpty(inputMapItem)) {
                value = (String) inputMapItem.get(Dict.VALUE);
            }
            entityParam.put(Dict.VALUE, value);
            entityParamsList.add(entityParam);
        }

        return entityParamsList;
    }

    @Override
    public String update(Pipeline pipeline) throws Exception {
        boolean isAppManager = vaildateUtil.projectCondition(pipeline.getBindProject().getProjectId());
        if (!isAppManager) {
            throw new FdevException(ErrorConstants.USER_NOT_APPMANAGER);
        }
        CommonUtils.checkCronExpression(pipeline);
        CommonUtils.stagesCheck(pipeline.getStages());
        String nameId = pipeline.getNameId();
        String oldPipelineId = pipeline.getId();
        //1. ?????????status?????????0
        Pipeline orgPipeline = pipelineDao.findActiveVersion(pipeline.getNameId());
        if (CommonUtils.isNullOrEmpty(orgPipeline)) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"nameId = " + nameId});
        }
        Pipeline oldPipeline = this.pipelineDao.queryById(orgPipeline.getId());
        pipeline.setAuthor(userService.getAuthor());
        preparePipeline(pipeline);
        /*???JSONCompare??????????????????????????????????????????????????????????????????????????????????????????*/
        ValueFilter valueFilter = (o, s, o1) -> {
            if (s.contains("validFlag") || s.contains("scriptCmd") || s.contains("collectedOrNot") || s.contains("updateRight")) {
                return null;
            }
            if (s.contains("scriptUpdateFlag") && !Objects.equals(o1, true)) {
                return null;
            }
            return o1;
        };
        String oldPipelineString = JSON.toJSONString(oldPipeline, valueFilter);
        String pipelineString = JSON.toJSONString(pipeline, valueFilter);
        JSONCompareResult jsonCompareResult = JSONCompare.compareJSON(oldPipelineString, pipelineString, JSONCompareMode.STRICT);
        if (jsonCompareResult.passed()) {
            /*?????????????????????????????????????????????id*/
            return pipeline.getId();
        }
        /*??????????????????????????????*/
        //2. ?????????????????????????????????
        Integer version = Integer.valueOf(orgPipeline.getVersion().split("\\.")[0]);
        Integer newVersion = version + 1;
        pipeline.setVersion(String.valueOf(newVersion));
        String newPipelineId = pipelineDao.add(pipeline);
        pipelineDao.updateStatusClose(orgPipeline.getId(), userService.getUserFromRedis());
        PipelineUpdateDiff diffEntity = DiffUtils.getHandlerPipelineDiff(oldPipeline, pipeline);
        this.pipelineUpdateDiffDao.saveDiff(diffEntity);
        updateUseCountWhenUpdate(oldPipelineId, newPipelineId);
        return newPipelineId;
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param oldPipelineId ????????????ID
     * @param newPipelineId ????????????ID
     * @throws Exception
     */
    private void updateUseCountWhenUpdate(String oldPipelineId, String newPipelineId) throws Exception {
        //1.???????????????????????????????????????
        updateUseCountWhenDelPipeline(oldPipelineId);
        //2.???????????????????????????????????????
        updateUseCountWhenAdd(newPipelineId);

        /*
        if (!CommonUtils.isNullOrEmpty(newPipelineId)) {
            //?????????????????????????????????????????????
            List<PluginUseCount> beforeUpdatePluginUseCounts = pluginDao.statisticsPluginUseCount(oldPipelineId);
            //?????????????????????????????????????????????
            List<PluginUseCount> afterUpdatePluginUseCounts = pluginDao.statisticsPluginUseCount(newPipelineId);
            if (!CommonUtils.isNullOrEmpty(beforeUpdatePluginUseCounts) && !CommonUtils.isNullOrEmpty(afterUpdatePluginUseCounts)) {
                //?????????true???????????????????????????????????????????????????????????????
                if (beforeUpdatePluginUseCounts.get(0).getBindProject().getProjectId().equals(afterUpdatePluginUseCounts.get(0).getBindProject().getProjectId())) {
                    //??????????????????
                    Map<String,PluginUseCount> beforeUpdatePluginUseCountsMap = new HashMap<>();
                    for (PluginUseCount beforeUpdatePluginUseCount : beforeUpdatePluginUseCounts) {
                        beforeUpdatePluginUseCountsMap.put(beforeUpdatePluginUseCount.getPluginCode(),beforeUpdatePluginUseCount);
                    }
                    for (PluginUseCount afterUpdatePluginUseCount : afterUpdatePluginUseCounts) {
                        String pluginCode = afterUpdatePluginUseCount.getPluginCode();
                        String bindProjectId = afterUpdatePluginUseCount.getBindProject().getProjectId();
                        String useCount = afterUpdatePluginUseCount.getUseCount();
                        PluginUseCount remove = beforeUpdatePluginUseCountsMap.remove(pluginCode);
                        PluginUseCount temp = pluginDao.queryPluginUseCountByPluginCodeAndBindProjectId(pluginCode,bindProjectId);
                        //???????????????????????????????????????????????????????????????
                        if (CommonUtils.isNullOrEmpty(remove)) {
                            //??????????????????????????????????????????????????????
                            if (!CommonUtils.isNullOrEmpty(temp)) {
                                temp.setUseCount(String.valueOf(Integer.parseInt(temp.getUseCount()) + Integer.parseInt(useCount)));
                                pluginDao.savePluginUseCount(temp);
                            } else {
                                BindProject bindProject = pipelineDao.queryOneByProjectId(bindProjectId).getBindProject();
                                afterUpdatePluginUseCount.setBindProject(bindProject);
                                pluginDao.savePluginUseCount(afterUpdatePluginUseCount);
                            }
                        } else {
                            int resultCount = Integer.parseInt(useCount) - Integer.parseInt(remove.getUseCount());
                            //?????????0????????????????????????????????????????????????????????????0????????????????????????
                            if (resultCount != 0) {
                                temp.setUseCount(String.valueOf(Integer.parseInt(temp.getUseCount()) + resultCount));
                                pluginDao.savePluginUseCount(temp);
                            }
                        }
                    }
                    //????????????????????????????????????????????????????????????????????????????????????
                    if (!CommonUtils.isNullOrEmpty(beforeUpdatePluginUseCountsMap)) {
                        for (PluginUseCount pluginUseCount : beforeUpdatePluginUseCountsMap.values()) {
                            String pluginCode = pluginUseCount.getPluginCode();
                            String bindProjectId = pluginUseCount.getBindProject().getProjectId();
                            String useCount = pluginUseCount.getUseCount();
                            PluginUseCount temp = pluginDao.queryPluginUseCountByPluginCodeAndBindProjectId(pluginCode, bindProjectId);
                            //????????????????????????????????????0????????????????????????????????????
                            int resultCount = Integer.parseInt(temp.getUseCount()) - Integer.parseInt(useCount);
                            temp.setUseCount(String.valueOf(resultCount));
                            pluginDao.savePluginUseCount(temp);
                        }
                    }
                } else {
                    //1.???????????????????????????????????????
                    updateUseCountWhenDelPipeline(oldPipelineId);
                    //2.???????????????????????????????????????
                    updateUseCountWhenAdd(newPipelineId);
                }
            }
        }*/
    }

    @Override
    public Pipeline queryById(Map request) throws Exception {
        //1. ???????????????pipeline
        Pipeline pipeline = pipelineDao.queryPipelineByIdOrNameId(request);
        if (CommonUtils.isNullOrEmpty(pipeline))
            return null;
        pipeline = preparePipelineDetailInfo(pipeline);
        return pipeline;
    }

    @Override
    public void delete(String id) throws Exception {
        //??????????????????????????????
        User user = userService.getUserFromRedis();
        Pipeline pipeline = pipelineDao.queryById(id);
        if (CommonUtils.isNullOrEmpty(pipeline)) {
            logger.error("**********pipeline not exist, pipelineId:" + id);
            /*throw new Exception("?????????????????????");*/
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"pipelineId = " + id});
        }
        pipelineDao.updateStatusClose(id, user);
        iPluginService.closeYamlConfigInStages(pipeline.getStages());
        updateUseCountWhenDelPipeline(id);
    }

    /**
     * ??????pipeline???????????????????????????
     *
     * @param id
     * @throws Exception
     */
    private void updateUseCountWhenDelPipeline(String id) throws Exception {
        List<PluginUseCount> pluginUseCountList = pluginDao.statisticsPluginUseCount(id);
        if (!CommonUtils.isNullOrEmpty(pluginUseCountList)) {
            for (PluginUseCount pluginUseCount : pluginUseCountList) {
                String pluginCode = pluginUseCount.getPluginCode();
                String bindProjectId = pluginUseCount.getBindProject().getProjectId();
                String useCount = pluginUseCount.getUseCount();
                PluginUseCount temp = pluginDao.queryPluginUseCountByPluginCodeAndBindProjectId(pluginCode, bindProjectId);
                if (!CommonUtils.isNullOrEmpty(temp)) {
                    //??????????????????????????????????????????
                    int resultCount = Integer.parseInt(temp.getUseCount()) - Integer.parseInt(useCount);
                    temp.setUseCount(String.valueOf(resultCount));
                    pluginDao.savePluginUseCount(temp);
                }
            }
        }
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param pipelineId
     * @throws Exception
     */
    private void updateUseCountWhenAdd(String pipelineId) throws Exception {
        if (!CommonUtils.isNullOrEmpty(pipelineId)) {
            List<PluginUseCount> pluginUseCountList = pluginDao.statisticsPluginUseCount(pipelineId);
            if (!CommonUtils.isNullOrEmpty(pluginUseCountList)) {
                for (PluginUseCount pluginUseCount : pluginUseCountList) {
                    String pluginCode = pluginUseCount.getPluginCode();
                    String bindProjectId = pluginUseCount.getBindProject().getProjectId();
                    String useCount = pluginUseCount.getUseCount();
                    PluginUseCount temp = pluginDao.queryPluginUseCountByPluginCodeAndBindProjectId(pluginCode, bindProjectId);
                    //???????????????????????????????????????????????????useCount
                    if (!CommonUtils.isNullOrEmpty(temp)) {
                        temp.setUseCount(String.valueOf(Integer.parseInt(temp.getUseCount()) + Integer.parseInt(useCount)));
                        pluginDao.savePluginUseCount(temp);
                    } else {
                        pluginUseCount.setBindProject(pipelineDao.queryOneByProjectId(bindProjectId).getBindProject());
                        pluginDao.savePluginUseCount(pluginUseCount);
                    }
                }
            }
        }
    }

    @Override
    public String updateFollowStatus(String pipelineId) throws Exception {
        User user = userService.getUserFromRedis();
        long row = pipelineDao.updateFollowStatus(pipelineId, user);
        if (row > 0) {
            logger.info("*********UPDATE SUCCESS???Effect rows number:{}", row);
            return "SUCCESS";
        }
        logger.info("*********UPDATE FAILED???pipelineId???{}???nameId???{}???state???{},user:{}", pipelineId, user);
        throw new FdevException(ErrorConstants.UPDATE_FOLLOW_STATUS_FAIL, new String[]{"pipelineId = " + pipelineId});
    }

    @Override
    public Map<String, Object> queryAllPipelineList(String pageNum, String pageSize, User user, String searchContent) throws Exception {
        long skip = (Integer.valueOf(pageNum) - 1) * Integer.valueOf(pageSize);
        int limit = Integer.valueOf(pageSize);
        Map<String, Object> map = pipelineDao.queryPipelineList(skip, limit, null, null, null, user, searchContent);
        handlePipeline((List<Map>) map.get(Dict.PIPELINELIST), user.getId());
        return map;
    }

    @Override
    public Map<String, Object> queryAppPipelineList(String pageNum, String pageSize, String userId, String applicationId, String searchContent) {
        long skip = (Integer.valueOf(pageNum) - 1) * Integer.valueOf(pageSize);
        int limit = Integer.valueOf(pageSize);
        Map<String, Object> map = pipelineDao.queryPipelineList(skip, limit, applicationId, null, null, null, searchContent);
        handlePipeline((List<Map>) map.get(Dict.PIPELINELIST), userId);
        return map;
    }

    /**
     * ?????????????????????
     *
     * @param pageNum
     * @param pageSize
     * @param userId
     * @param searchContent
     * @return
     */
    @Override
    public Map<String, Object> queryCollectionPipelineList(String pageNum, String pageSize, String userId, String searchContent) {
        long skip = (Integer.valueOf(pageNum) - 1) * Integer.valueOf(pageSize);
        int limit = Integer.valueOf(pageSize);
        Map<String, Object> map = pipelineDao.queryPipelineList(skip, limit, null, userId, null, null, searchContent);
        handlePipeline((List<Map>) map.get(Dict.PIPELINELIST), userId);
        return map;
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param pageNum
     * @param pageSize
     * @param userId
     * @param searchContent
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> queryMinePipelineList(String pageNum, String pageSize, String userId, String searchContent) throws Exception {
        List<Map<String, Object>> applist = appService.queryMyApps(userId);
        /*??????????????????????????????id??????*/
        Map<String, List<String>> id_appMap = applist.stream()
                .collect(Collectors.toMap(app -> (String) app.get(Dict.ID), app -> {
                            List<String> managers = new ArrayList<>();
                            Optional.ofNullable((List<Map<String, String>>) app.get("spdb_managers"))
                                    .map(arr -> arr.stream().map(m -> m.get("id")))
                                    .ifPresent(s -> s.collect(Collectors.toCollection(() -> managers)));
                            Optional.ofNullable((List<Map<String, String>>) app.get("dev_managers"))
                                    .map(arr -> arr.stream().map(m -> m.get("id")))
                                    .ifPresent(s -> s.collect(Collectors.toCollection(() -> managers)));
                            return managers;
                        }
                ));
        if (CommonUtils.isNullOrEmpty(applist)) {
            Map<String, Object> map = new HashMap<>();
            map.put(Dict.TOTAL, "0");
            map.put(Dict.PIPELINELIST, new ArrayList<>());
            return map;
        }
        List<String> apps = applist.stream().map(app -> (String) app.get(Dict.ID)).collect(Collectors.toList());
        long skip = (Integer.valueOf(pageNum) - 1) * Integer.valueOf(pageSize);
        int limit = Integer.valueOf(pageSize);
        Map<String, Object> map = pipelineDao.queryPipelineList(skip, limit, null, null, apps, null, searchContent);
        List<Map> pipelineList = (List<Map>) map.get(Dict.PIPELINELIST);
        /*??????????????????id??????????????????id??????????????????????????????*/
        pipelineList.forEach(pipeline -> {
            pipeline.remove(Dict._ID);
            pipeline.put("collectedOrNot", !CommonUtils.isNullOrEmpty(pipeline.get(Dict.COLLECTED)) && ((List) pipeline.get(Dict.COLLECTED)).contains(userId));
            String appId = (String) ((Map) pipeline.get(Dict.BINDPROJECT)).get(Dict.PROJECTID);
            pipeline.put("updateRight", (id_appMap.get(appId).contains(userId)));
        });
        return map;
    }

    private void handlePipeline(List<Map> list, String userId) {
        for (Map pipeline : list) {
            if (!CommonUtils.isNullOrEmpty(pipeline.get(Dict._ID))) {
                pipeline.remove(Dict._ID);
            }
            if (!CommonUtils.isNullOrEmpty(pipeline.get(Dict.COLLECTED)) && ((List) pipeline.get(Dict.COLLECTED)).contains(userId)) {
                pipeline.put("collectedOrNot", true);
            } else {
                pipeline.put("collectedOrNot", false);
            }
            boolean flag = false;
            try {
                flag = vaildateUtil.projectCondition((String) ((Map) pipeline.get(Dict.BINDPROJECT)).get(Dict.PROJECTID));
            } catch (Exception e) {
                logger.error("**********the bindProject not belong to the user , userId=" + userId + ";projectId=" + (String) ((Map) pipeline.get(Dict.BINDPROJECT)).get(Dict.PROJECTID));
            }
            pipeline.put("updateRight", flag);
//            PipelineExe exe = pipelineExeDao.queryOneByPipelineIdSort((String) pipeline.get(Dict.ID));
//            if(!CommonUtils.isNullOrEmpty(exe)) {
//                pipeline.put(Dict.USER, exe.getUser().getNameEn());
//            }
        }
    }

    @Override
    public String saveDraft(PipelineDraft draft) throws Exception {
        return pipelineDao.saveDraft(draft);
    }

    @Override
    public PipelineDraft readDraft() throws Exception {
        //1. ???????????????????????????
        String authorID = userService.getUserFromRedis().getId();
        PipelineDraft draft = pipelineDao.readDraftByAuthor(authorID);
        if (CommonUtils.isNullOrEmpty(draft)) {
            return null;
        }
        return draft;
    }

    @Override
    public void retryPipeline(String pipelineExeId) throws Exception {
        Author author = userService.getAuthor();
        PipelineExe pipelineExe = pipelineExeService.queryPipelineExeByExeId(pipelineExeId);
        String pipelineId = pipelineExe.getPipelineId();
        Pipeline pipeline = this.pipelineDao.queryById(pipelineId);
        User user = userService.getUserFromRedis();
        if (!vaildateUtil.projectCondition(pipeline.getBindProject().getProjectId())
                && !pipelineExe.getUser().equals(user.getUser_name_en())) {
            logger.error("**********user has not permission to retry");
            throw new FdevException(ErrorConstants.RETRY_FAILED, new String[]{"current user illegal permission"});
        }
        checkRetryStatus(pipelineExe);
        //???????????????????????????????????????
        logger.info("*********** retry Pipeline??? pipelineExeId???" + pipelineExeId);
        List pendingJobs = new ArrayList();
        List<Map<String, Object>> stages = pipelineExe.getStages();
        for (int i = 0; i < stages.size(); i++) {
            //???????????????stage???pending
            String jobStatus = Dict.WAITING;
            if (i == 0) {
                jobStatus = Dict.PENDING;
            }
            Map<String, Object> stage = stages.get(i);
            stage.put(Dict.STATUS, jobStatus);
            Stage pipStage = pipeline.getStages().get(i);
            List<Map> jobs = (List<Map>) stage.get(Dict.JOBS);

            for (int j = 0; j < jobs.size(); j++) {
                Map job = jobs.get(j);
                List<Map> stageJobExes = (List<Map>) job.get(Dict.JOBEXES);
                //??????????????????jobexeMap
                Map map = stageJobExes.get(stageJobExes.size() - 1);
                String jobExeId = (String) map.get(Dict.JOBEXEID);
                String runnerClusterId = (String) job.get(Dict.RUNNERCLUSTERID);
                if (CommonUtils.isNullOrEmpty(runnerClusterId)) {
                    throw new FdevException(ErrorConstants.RETRY_FAILED, new String[]{"????????????????????????????????????????????????????????????!"});
                }
                //????????????jobexe
                JobExe jobExe = new JobExe();
                Long number = jobExe.getJobNumber();
                ObjectId objId = new ObjectId();
                JobExe orgJobExe = jobExeService.queryJobExeByExeId(jobExeId);
                BeanUtils.copyProperties(orgJobExe, jobExe);
                jobExe.setJobNumber(number);
                jobExe.setExeId(objId.toString());
                jobExe.setUser(author);
                jobExe.setStatus(jobStatus);
                jobExe.setJobStartTime("");
                jobExe.setJobCostTime("");
                jobExe.setJobEndTime("");
                jobExe.setMinioLogUrl("");
                jobExe.setInfo(null);
                jobExe.setToken("");
                //??????jobExe??????steps
                List<Map> jobExeSteps = jobExe.getSteps();
                for (Map jobExeStep : jobExeSteps) {
                    jobExeStep.remove(Dict.OUTPUT);
                    jobExeStep.remove(Dict.STEPSTARTTIME);
                    jobExeStep.remove(Dict.STEPENDTIME);
                    jobExeStep.remove(Dict.STEPCOSTTIME);
                    jobExeStep.remove(Dict.STATUS);
                }
                jobExeService.saveJobExe(jobExe);

                if (Dict.PENDING.equals(jobStatus)) {
//                    Job pipJob = pipStage.getJobs().get(j);
                    Map jobMap = new HashMap();
                    jobMap.put(Dict.JOB_EXE, jobExe);
                    //??????????????????pipelineExe???
                    jobMap.put(Dict.RUNNERCLUSTERID, runnerClusterId);
                    pendingJobs.add(jobMap);
                }

                //??????stage-jobs-jobexes??????job??????
                String jobNumber = String.valueOf(jobExe.getJobNumber());
                Map exeMap = new HashMap();
                exeMap.put(Dict.JOBEXEID, objId.toString());
                exeMap.put(Dict.JOBNUMBER, jobNumber);
                exeMap.put(Dict.JOBEXESTATUS, jobStatus);
                stageJobExes.add(exeMap);
            }

        }
        //??????pipelineExe
        pipelineExe.setStatus(Dict.PENDING);
        pipelineExe.setStartTime(CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
        pipelineExe.setCostTime("");
        pipelineExe.setEndTime("");
        pipelineExe.setUser(author);
        logger.info("********** retryPipeline will update the pipelineExe info:" + JSONObject.toJSON(pipelineExe));
        pipelineExeService.updateStagesAndStatusAndUser(pipelineExe);
        //??????pipeline???????????????????????????
        pipeline.setBuildTime(pipelineExe.getStartTime());
        pipeline.setUpdateTime(pipelineExe.getStartTime());
        pipelineDao.updateBuildTime(pipeline);
        for (Map pendingJob : (List<Map>) pendingJobs) {
            //??????pending???????????????
            JobExe jobExe = (JobExe) pendingJob.get(Dict.JOB_EXE);
            String runnerClusterId = (String) pendingJob.get(Dict.RUNNERCLUSTERID);
            addJobExeQueue(jobExe, runnerClusterId);
        }
    }

    /**
     * ??????????????????????????????????????????????????????????????????/??????/?????????
     *
     * @param pipelineExe
     * @throws Exception ??????????????????????????????????????????true
     */
    private void checkRetryStatus(PipelineExe pipelineExe) throws Exception {
        /*Map bindProject = pipelineExe.getBindProject();
        String projectId = (String)bindProject.get(Dict.PROJECTID);
        //????????????????????????????????????????????????????????????????????????
        if(!vaildateUtil.projectCondition(projectId) && !pipelineExe.getUser().equals(user.getUser_name_en())){
            logger.error("**********user has not role to retry");
            throw new FdevException(ErrorConstants.ROLE_ERROR);
        }*/
        if (!Dict.ERROR.equals(pipelineExe.getStatus()) && !Dict.SUCCESS.equals(pipelineExe.getStatus()) && !Dict.CANCEL.equals(pipelineExe.getStatus())) {
            logger.error("********** pipelineExe status cannot retry , pipelineExeId: " + pipelineExe.getExeId() + ", status: " + pipelineExe.getStatus());
            throw new FdevException(ErrorConstants.PIPELINE_IS_RUNNING);
        }
    }

    @Override
    public List<Images> queryAllImages() throws Exception {
        Query query = new Query();
        User user = userService.getUserFromRedis();
        Criteria statusCriteria = Criteria.where(Dict.STATUS).is(Constants.STATUS_OPEN);
        query.addCriteria(statusCriteria);
        //??????????????????
        if (!Dict.ADMIN.equals(user.getUser_name_en()) && !whiteList.contains(user.getUser_name_en())) {
//            Criteria groupsCriteria = Criteria.where(Dict.VISIBLERANGE).is(Dict.GROUP);
            Criteria visibleRangeCriteria = Criteria.where(Dict.VISIBLERANGE).is(Dict.PUBLIC);
            Criteria userCriteria = Criteria.where(Dict.AUTHOR__ID).is(user.getId());
//            String groupId = user.getGroup_id();
            //?????????id?????????????????????????????????
            //List<String> groupIds = appService.queryCurrentAndChildGroup(groupId);
//            List<String> groupIds = userService.getChildGroupIdsByGroupId(groupId);
//            if (!CommonUtils.isNullOrEmpty(groupIds)) {
//                groupsCriteria.and(Dict.GROUPID).in(groupIds);
//            }
            query.addCriteria(new Criteria().orOperator(/*groupsCriteria,*/visibleRangeCriteria, userCriteria));
        }
        return imageDao.queryImages(query);
    }

    @Override
    public Pipeline querySimpleObjectById(String pipelineId) {
        return pipelineDao.queryById(pipelineId);
    }

    @Override
    public Map<String, Object> queryPipelineByPluginCode(String pluginCode) throws Exception {
        //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????+??????????????????
        Map<String, Object> resMap = new HashMap<>();
        List<Pipeline> pipelines = pipelineDao.queryByPluginId(pluginCode);

        if (pipelines.size() >= 1) {
            resMap.put(Dict.LIST, pipelines.get(0));
            resMap.put(Dict.TOTAL, pipelines.size());
        } else {
            //??????pipeline???????????????????????????????????????
            logger.info("********* delete plugin;  pluginCode:" + pluginCode);
            pluginService.delPlugin(pluginCode);
            resMap.put(Dict.PLUGINCODE, pluginCode);
        }
        return resMap;
    }

    private List<Map> setVariables(String project_id, String exe_id, String branch, Boolean tagFlag, String userId) throws Exception {
        List<Map> variables = new ArrayList<>();
        Map<String, Object> project = appService.queryAppDetailById(project_id);
        if (CommonUtils.isNullOrEmpty(project)) {
            logger.error("*************fdev project not exist, projectId = " + project_id);
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST);
        }
        Map<String, Object> gitProject = appService.queryGitProjectDetail(project_id);
        if (CommonUtils.isNullOrEmpty(gitProject)) {
            logger.error("**************gitlab project not exist, projectId = " + project_id);
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST);
        }
        Map<String, String> namespace = (Map<String, String>) gitProject.get(Dict.NAMESPACE);
        String fullPath = namespace.get(Dict.FULL_PATH);
        setVariableMap(variables, Dict.CI_WORKSPACE, "/workspace/" + exe_id);
        setVariableMap(variables, Dict.CI_PROJECT_NAMESPACE, fullPath);
        setVariableMap(variables, Dict.CI_PROJECT_NAME, (String) project.get(Dict.NAME_EN));
        String gitUrl = (String) project.get(Dict.GIT);
        String[] split = gitUrl.split("http://");
        String projectUrl = "http://" + gitUserName + ":" + gitPassword + "@" + split[1];
        setVariableMap(variables, Dict.CI_PROJECT_URL, projectUrl);
        setVariableMap(variables, Dict.CI_PROJECT_BRANCH, branch);
        setVariableMap(variables, Dict.CI_COMMIT_REF_NAME, branch);
        StringBuilder dir = new StringBuilder("/workspace/" + exe_id);
        dir.append("/").append(fullPath).append("/").append(project.get(Dict.NAME_EN));
        setVariableMap(variables, Dict.CI_PROJECT_DIR, dir.toString());
        //??????CI_PROJECT_ID???CI_PIPELINE_NUMBER???CI_TAG_FLAG
        setVariableMap(variables, Dict.CI_PROJECT_ID, String.valueOf(gitProject.get(Dict.ID)));
        Long pipelineNum = mongoEventListener.getPipelineNum();
        setVariableMap(variables, Dict.CI_PIPELINE_NUMBER, String.valueOf(pipelineNum + 1));
        //?????????tag??????
        setVariableMap(variables, Dict.CI_TAG_FLAG, String.valueOf(tagFlag));
        //2021???6???11??? 14:14:49  ??????????????????userId
        setVariableMap(variables, Dict.CI_USERID, userId);
        return variables;
    }

    private void setVariableMap(List<Map> variables, String key, Object value) {
        Map item = new HashMap();
        item.put(Dict.KEY, key);
        item.put(Dict.VALUE, value);
        variables.add(item);
    }

    private List setVolumes() throws Exception {
        String jsonStr = CommonUtils.readJsonFile("volumes.json");
        Map volumes = JSONObject.parseObject(jsonStr, Map.class);
        return (List) volumes.get(Dict.VOLUMES);
    }

    @Override
    public void webhookPipeline(Map<String, Object> param) throws Exception {
        logger.info("*******************WebhookPipeline info; param :" + JSONObject.toJSONString(param));
        Integer gitlabProjectId = (Integer) param.get(Dict.PROJECT_ID);
        String userName = (String) param.get(Dict.USER_USERNAME);
        //????????????????????????????????????
        Author author = appService.queryUserByNameEn(userName);
        if (CommonUtils.isNullOrEmpty(author.getNameEn())) {
            author.setNameEn(userName);
        }
        String ref = (String) param.get(Dict.REF);
        String[] split = ref.split("/");
        String branch = split[split.length - 1];
        //??????gitlabProjectId ??????????????????
        Map projectMap = appService.queryAppDetailByGitId(gitlabProjectId);
        if (CommonUtils.isNullOrEmpty(projectMap)) {
            logger.error("******** can not find this project: " + gitlabProjectId);
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST);
        }
        String applicationId = (String) projectMap.get(Dict.ID);
        List<Pipeline> pipelines = queryDetailByProjectId(applicationId);
        if (CommonUtils.isNullOrEmpty(pipelines)) {
            logger.info("*********WebhookPipeline, cannot find pipelines!, applicationId: " + applicationId);
            //throw new Exception("?????????????????????fdev-ci?????????");
            throw new FdevException(ErrorConstants.PARAMS_ERROR, new String[]{"?????????????????????fdev-ci?????????"});
        }
        BindProject bindProject = pipelines.get(0).getBindProject();
        String projectId = bindProject.getProjectId();
        checkProjectFdevCI(projectId);

        //?????????????????? ??????????????? ??????????????????
        String commitTitle = null;
        String commitId = null;
        if (!CommonUtils.isNullOrEmpty(param.get(Dict.COMMITS))) {
            List<Map> commits = (List<Map>) param.get(Dict.COMMITS);
            Map lastCommitMap = commits.get(commits.size() - 1);
            commitTitle = String.valueOf(lastCommitMap.get(Dict.TITLE));
            commitId = String.valueOf(lastCommitMap.get(Dict.ID));
        } else {
            logger.info("*********WebhookPipeline, ??????commit???????????????????????????: " + applicationId);
            return;
        }
        for (Pipeline pipeline : pipelines) {
            TriggerRules triggerRules = pipeline.getTriggerRules();
            List<String> branchs = new ArrayList<>();
            //List<PushParams> pushParams = triggerRules.getPush().getPushParams();
            List<Map> pushParams = (List<Map>) triggerRules.getPush().getPushParams();
            if (!CommonUtils.isNullOrEmpty(pushParams)) {
                for (Map params : pushParams) {
                    String branchName = (String) params.get("branchName");
                    branchs.add(branchName);
                }
            }
            //????????????????????????
            if (isTrigger(branchs, branch)) {
                //??????gitlab???????????????fuser?????????????????????gitlab token
//                String userGitToken = userService.getUserGitToken(userName);
                //?????????token???????????????????????????tag??????
                List<String> tags = gitlabService.queryTags(gitlabProjectId, userGitToken);
                Boolean tagFlag = false;
                if (tags.contains(branch))
                    tagFlag = true;
                //??????tagFlag????????????????????????pipeline??????????????????tag
                logger.info("*******************WebhookPipeline  executePipeline info ; pipelineId???:" + pipeline.getId() + "?????????????????????********" + branch);
                pipeline.setCommitTitle(commitTitle);
                pipeline.setCommitId(commitId);
                executePipeline(pipeline, Dict.PUSH, author, branch, gitlabProjectId, tagFlag);
            }
        }
    }

    /**
     * ???????????????????????????fdev-ci????????????
     *
     * @param projectId ??????ID?????????gitlabID
     * @return
     * @throws Exception
     */
    private boolean checkProjectFdevCI(String projectId) throws Exception {
        Map<String, Object> project = appService.queryAppDetailById(projectId);
        if (CommonUtils.isNullOrEmpty(project)) {
            logger.error("******** can not find this project " + projectId);
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST);
        }
        String appCiType = (String) project.get(Dict.APPCITYPE);
        if (CommonUtils.isNullOrEmpty(appCiType) || !appCiType.equals(Dict.FDEV_CI)) {
            throw new FdevException(ErrorConstants.APP_NOT_CHOOSE_FDEV_CI);
        }
        return true;
    }

    @Override
    public String executePipeline(Pipeline pipeline, String type, Author user, String branch, Integer gitlabId, Boolean tagFlag) throws Exception{
        logger.info("??????????????????" + Thread.currentThread().getName());
        String pipelineExeId = new ObjectId().toString();
        asyncBuildPipelineExePending(pipeline, type, user, branch, gitlabId, tagFlag, pipelineExeId);
        return pipelineExeId;
    }

    /**
     * ????????????pending???pipelineExe
     * ??????????????????pipelineExe????????????????????????????????????????????????pipelineExe???JobExe ???????????????????????????????????????
     *
     * @param pipeline
     * @param type
     * @param user
     * @param branch
     * @param gitlabId
     * @param tagFlag
     * @param pipelineExeId
     */
    @Async
    public void asyncBuildPipelineExePending(Pipeline pipeline, String type, Author user, String branch, Integer gitlabId, Boolean tagFlag, String pipelineExeId) throws Exception {
        logger.info("??????????????????" + Thread.currentThread().getName());
        logger.info(">>>>>>>>>>>>>>>>>>> ??????????????????pending pipelineExe ?????? <<<<<<<<<<<<<<<<<<<<<<");
        BindProject bindProject = pipeline.getBindProject();
        PipelineExe pipelineExe = new PipelineExe();
        pipelineExe.setExeId(pipelineExeId);
        pipelineExe.setPipelineId(pipeline.getId());
        pipelineExe.setPipelineNameId(pipeline.getNameId());
        pipelineExe.setCommitTitle(pipeline.getCommitTitle());
        pipelineExe.setCommitId(pipeline.getCommitId());
        Map bindProjectMap = new HashMap();
        bindProjectMap.put(Dict.GITLABPROJECTID, gitlabId);
        bindProjectMap.put(Dict.PROJECTID, bindProject.getProjectId());
        bindProjectMap.put(Dict.NAMEEN, bindProject.getNameEn());
        bindProjectMap.put(Dict.NAMECN, bindProject.getNameCn());
        pipelineExe.setBindProject(bindProjectMap);
        pipelineExe.setUser(user);
        List<Map> variables = setVariables(bindProject.getProjectId(), pipelineExe.getExeId(), branch, tagFlag, user.getId());
        pipelineExe.setTriggerMode(type);//?????????????????????/??????
        if (!CommonUtils.isNullOrEmpty(pipeline.getRunVariables()) && pipeline.getRunVariables().size() > 0) {
            variables.addAll(pipeline.getRunVariables());
        }
        pipelineExe.setVariables(variables);
        List volumesList = setVolumes();
        for (Object map : volumesList) {
            Map volumesMap = (Map) map;
            if (Objects.equals(volumesMap.get(Dict.NAME), Dict.FILE_UPLOAD)) {
                String host_path = volumesMap.get(Dict.HOST_PATH) + envUrl;
                volumesMap.put(Dict.HOST_PATH, host_path);
            }
        }
        pipelineExe.setVolumes(volumesList);
        pipelineExe.setBranch(branch);
        pipelineExe.setStatus(Dict.PENDING);
        List<Stage> stages = pipeline.getStages();
        Integer stage_index = 0;
        List stageExes = new ArrayList();
        for (Stage stage : stages) {
            List<Job> jobs = stage.getJobs();
            Integer job_index = 0;
            List jobIds = new ArrayList();
            for (Job job : jobs) {
                Map jobInfo = new HashMap();
                String runnerClusterId = job.getRunnerClusterId();
                if (CommonUtils.isNullOrEmpty(runnerClusterId)) {
                    //??????????????????????????????????????????????????????
                    runnerClusterId = Dict.RUNNERCLUSTERID_FDEV;
                }
                jobInfo.put(Dict.RUNNERCLUSTERID, runnerClusterId);
                //3 ??????pipelineEXE??????job??????
                List jobExes = new ArrayList();
                Map jobExeInfo = new HashMap();
                jobExeInfo.put(Dict.JOBNUMBER, "");
                jobExeInfo.put(Dict.JOBEXEID, new ObjectId().toString());
                if (stage_index == 0) {
                    jobExeInfo.put(Dict.JOBEXESTATUS, Dict.PENDING);
                }else {
                    jobExeInfo.put(Dict.JOBEXESTATUS, Dict.WAITING);
                }
                jobExes.add(jobExeInfo);

                jobInfo.put(Dict.NAME, job.getName());
                jobInfo.put(Dict.JOBEXES, jobExes);
                List<Step> steps = job.getSteps();
                List<String> stepNames = new ArrayList<>();
                for (Step step : steps) {
                    stepNames.add(step.getName());
                }
                jobInfo.put(Dict.STEPS, stepNames);
                jobIds.add(jobInfo);
                job_index++;
            }

            Map stageExe = new HashMap();
            stageExe.put(Dict.NAME, stage.getName());
            stageExe.put(Dict.JOBS, jobIds);
            if (stage_index == 0) {
                stageExe.put(Dict.STATUS, Dict.PENDING);
            } else {
                stageExe.put(Dict.STATUS, Dict.WAITING);
            }
            stageExes.add(stageExe);
            stage_index++;
        }
        pipelineExe.setPipelineName(pipeline.getName());
        pipelineExe.setStartTime(CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
        pipelineExe.setStages(stageExes);
        logger.info("************* executePipeline will save pipelineExe, pipelineExe info:" + JSONObject.toJSON(pipelineExe));
        //???job??????????????????pipeline???????????????
        pipelineExeService.save(pipelineExe);
        //4 ?????????????????????????????????,????????????updateTime
        pipeline.setBuildTime(pipelineExe.getStartTime());
        pipeline.setUpdateTime(pipelineExe.getStartTime());
        pipelineDao.updateBuildTime(pipeline);


        logger.info(">>>>>>>>>>>>>>>>>>> ??????????????????pending pipelineExe ???????????? <<<<<<<<<<<<<<<<<<<<<<");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    asyncBuildJobExe(pipeline, pipelineExeId, branch, gitlabId, user);
                } catch (Exception e) {
                    logger.error(">>>>>>>>>>>>>>>>>>>  ?????? asyncBuildJobExe ??????! <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                    logger.error(e.getMessage());
                }
            }
        }).start();
    }

    /**
     * ????????????jobExe,???????????????pipelineExe?????????????????????jobExe?????? ????????????
     *
     * @param pipeline
     * @param pipelineExeId
     * @param branch
     * @param gitlabId
     * @param user
     * @throws Exception
     */
    @Async
    private void asyncBuildJobExe(Pipeline pipeline, String pipelineExeId, String branch, Integer gitlabId, Author user) throws Exception {
        logger.info("??????????????????" + Thread.currentThread().getName());
        logger.info(">>>>>>>>>>>>>>>>>>> ??????????????????pending jobExe ?????? <<<<<<<<<<<<<<<<<<<<<<");
        //?????????????????????job??????
        List<Map> jobQueueList = new ArrayList();
        PipelineExe pipelineExe = this.pipelineExeDao.queryPipelineExeByExeId(pipelineExeId);
        List<Map<String, Object>> exeStages = pipelineExe.getStages();
        List<Stage> stages = pipeline.getStages();
        for (int i = 0; i < stages.size(); i++) {
            Stage stage = stages.get(i);
            List<Job> jobs = stage.getJobs();
            for (int j = 0; j < jobs.size(); j++) {
                Job job = jobs.get(j);
                //1??????jobExe
                JobExe jobExe = new JobExe();
                List<Map> exeJobs = (List<Map>) exeStages.get(i).get(Dict.JOBS);
                List<Map> jobExesListMap = (List<Map>) exeJobs.get(j).get(Dict.JOBEXES);
                String jobExeId = (String) jobExesListMap.get(jobExesListMap.size() - 1).get(Dict.JOBEXEID);
//                ObjectId id = new ObjectId();
                jobExe.setExeId(jobExeId);
                jobExe.setPipelineId(pipeline.getId());
                jobExe.setPipelineExeId(pipelineExeId);
                jobExe.setStageIndex(i);
                jobExe.setStageName(stage.getName());
                jobExe.setJobIndex(j);
                jobExe.setJobName(job.getName());
                Map imageMap = new HashMap();
                Images image = job.getImage();
                if(image != null && !CommonUtils.isNullOrEmpty(image.getId())){
                    Images jobImage = pipelineDao.findImageById(image.getId());
                    if (!CommonUtils.isNullOrEmpty(jobImage)) {
                        imageMap.put(Dict.PATH, jobImage.getPath());
                        imageMap.put(Dict.IMAGEID, jobImage.getId());
                        imageMap.put(Dict.NAME, jobImage.getName());
                    }
                }
                jobExe.setImage(imageMap);
                //??????steps
                jobExe.setSteps(prepareSteps(job,branch,gitlabId));
                jobExe.setUser(user);

                String runnerClusterId = job.getRunnerClusterId();
                if(CommonUtils.isNullOrEmpty(runnerClusterId)) {
                    //??????????????????????????????????????????????????????
                    runnerClusterId = Dict.RUNNERCLUSTERID_FDEV;
                }
                //????????????stage??????job????????????pending
                String jobStatus = Dict.WAITING;
                if(i == 0) {
                    jobStatus = Dict.PENDING;
                }
                jobExe.setStatus(jobStatus);
                //??????????????????info??????
//                jobExe.setInfo();
                jobExeService.saveJobExe(jobExe);

                //???pending???jobExe????????????
                if (jobExe.getStatus().equals(Dict.PENDING)) {
                    Map jobMap = new HashMap();
                    jobMap.put(Dict.JOB_EXE, jobExe);
                    jobMap.put(Dict.RUNNERCLUSTERID, runnerClusterId);
                    jobQueueList.add(jobMap);
                }

                //???pipelineExe??????JobExe??????
                List jobExes = new ArrayList();
                Map jobExeInfo = new HashMap();
                jobExeInfo.put(Dict.JOBNUMBER, jobExe.getJobNumber());
                jobExeInfo.put(Dict.JOBEXEID, jobExe.getExeId());
                jobExeInfo.put(Dict.JOBEXESTATUS, jobExe.getStatus());
                jobExes.add(jobExeInfo);
                exeJobs.get(j).put(Dict.JOBEXES, jobExes);
            }
        }

        logger.info("************* executePipeline will save pipelineExe, pipelineExe info:" + JSONObject.toJSON(pipelineExe));
        //???job??????????????????pipeline???????????????
        pipelineExeService.updateStagesAndStatus(pipelineExe);

        //5???jobexe????????????
        for (Map jobMap : jobQueueList) {
            JobExe jobQueExe = (JobExe) jobMap.get(Dict.JOB_EXE);
            String runnerClusterId = (String) jobMap.get(Dict.RUNNERCLUSTERID);
            //??????????????????
            addJobExeQueue(jobQueExe, runnerClusterId);
        }

        logger.info(">>>>>>>>>>>>>>>>>>> ??????????????????asyncBuildJobExe ???????????? <<<<<<<<<<<<<<<<<<<<<<");
    }

    /**
     * ??????jobexe??????steps
     *
     * @param job
     * @return
     * @throws Exception
     */
    private List<Map> prepareSteps(Job job, String branch, Integer gitlabId) throws Exception {
        List<Map> jobExeSteps = new ArrayList<>();
        //??????jobExe??????steps
        List<Step> steps = job.getSteps();
        for (Step step : steps) {
            Map jobExeStep = new HashMap();
            jobExeStep.put(Dict.STEPNAME, step.getName());
            //??????pluginInfo?????????
            PluginInfo pluginInfo = step.getPluginInfo();
            Map jobExePlugin = preparePluginInfo(pluginInfo);
            jobExeStep.put(Dict.PLUGININFO, jobExePlugin);
            //??????input
            List<Map> input = preparePluginInput(pluginInfo, branch, gitlabId);
            jobExeStep.put(Dict.INPUT, input);
            //jobExeStep.put(Dict.STEPSTARTTIME, CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
            //????????????
            jobExeSteps.add(jobExeStep);
        }
        return jobExeSteps;
    }

    /**
     * ??????jobexe??????plugininfo
     *
     * @param pluginInfo
     * @return
     * @throws Exception
     */
    private Map preparePluginInfo(PluginInfo pluginInfo) throws Exception {
        String pluginCode = pluginInfo.getPluginCode();
        Map jobExePlugin = new HashMap();
        //??????plugininfo????????????
        jobExePlugin.put(Dict.PLUGINCODE, pluginCode);
        jobExePlugin.put(Dict.NAME, pluginInfo.getName());
        jobExePlugin.put(Dict.DESC, pluginInfo.getDesc());
        //??????execution
        Plugin plugin = pluginService.queryPluginDetail(pluginCode);
        if (!CommonUtils.isNullOrEmpty(plugin)) {
            jobExePlugin.put(Dict.EXECUTION, plugin.getExecution());
        }
        //??????artifacts
        Map<String, Object> artifacts = pluginInfo.getArtifacts();
        if (!CommonUtils.isNullOrEmpty(artifacts)) {
            Object value = artifacts.get(Dict.VALUE);
            if (!CommonUtils.isNullOrEmpty(value)) {
                jobExePlugin.put(Dict.ARTIFACTS, value);
            }
        }
        return jobExePlugin;
    }

    /**
     * ?????????plugin??????????????????input??????
     *
     * @param pluginInfo
     * @param branch
     * @param gitlabId
     * @return
     * @throws Exception
     */
    private List<Map> preparePluginInput(PluginInfo pluginInfo, String branch, Integer gitlabId) throws Exception {
        //??????input
        List<Map> input = new ArrayList<>();
        //?????????????????????commits??????????????????id
        String shortId = gitlabService.queryShortId(gitlabId, userGitToken, branch);
        Map gitLabUrlMap = inputValue(Dict.CI_API_V4_URL, gitLabApiUrl);
        input.add(gitLabUrlMap);
        Map shortIdMap = inputValue(Dict.CI_COMMIT_SHORT_SHA, shortId);
        input.add(shortIdMap);
        //input??????gitlab???token
        Map fdevToken = inputValue(Dict.CI_FDEV_TOKEN, gitlabToken);
        input.add(fdevToken);
        //????????????????????????????????????????????????false??? ??????????????????????????????????????????????????????true
        Map skipManualReview = inputValue(Dict.SKIP_MANUAL_REVIEW, Dict.PARAM_FALSE);
        input.add(skipManualReview);
        //input??????script  ????????????????????????
        Map<String, String> script = pluginInfo.getScript();
        if (!CommonUtils.isNullOrEmpty(script)) {
            //??????minio??????
            Map inputScriptMap = inputValue(Dict.PACKAGE_PATH, script.get(Dict.MINIO_OBJECT_NAME));
            input.add(inputScriptMap);
            Map inputScriptMinioObjectName = inputValue(Dict.MINIO_OBJECT_NAME, script.get(Dict.MINIO_OBJECT_NAME));
            input.add(inputScriptMinioObjectName);
            List<Map> mapMinioAccessInfo = minioAccessInfo();
            input.addAll(mapMinioAccessInfo);
        }
        //???????????????????????????,????????????
        /*List<Map> entityTemplateParams = pluginInfo.getEntityTemplateParams();
        if (!CommonUtils.isNullOrEmpty(entityTemplateParams)){
            List<Map> entityTemplateInputInfo = entityTypeInputInfo(entityTemplateParams);
            input.addAll(entityTemplateInputInfo);
        }*/

        //?????????????????????????????????
        Plugin plugin = this.pluginDao.queryPluginDetailById(pluginInfo.getPluginCode());
        //???????????????
        String pluginNameEn = plugin.getPluginNameEn();
        //input?????????????????????param
        List<Map<String, Object>> params = pluginInfo.getParams();
        if (!CommonUtils.isNullOrEmpty(params)) {
            for (Map<String, Object> paramMap : params) {
                //input???multipleInput???password	???select
                String type = (String) paramMap.get(Dict.TYPE);
                if (Dict.INPUT.equals(type) || Dict.MULTIPLEINPUT.equals(type)
                        || Dict.PASSWORD.equals(type) || Dict.SELECT.equals(type)|| Dict.HTTP_SELECT.equals(type) || Dict.FILEEDIT.equals(type) || Dict.FILEUPLOAD.equals(type)) {
                    Map inputParamMap = inputValue((String) paramMap.get(Dict.KEY), paramMap.get(Dict.VALUE));
                    input.add(inputParamMap);
                } else if (Dict.MULTIPLESELECT.equals(type)) {
                    //?????????
                    Map multipleParamMap = inputValue((String) paramMap.get(Dict.KEY), paramMap.get(Dict.VALUEARRAY));
                    input.add(multipleParamMap);
                } else if (Dict.ENTITYTYPE.equals(type)) {
                    //??????????????????
                    if (!CommonUtils.isNullOrEmpty(paramMap.get(Dict.ENTITYTEMPLATEPARAMS))) {
                        List<Map> templateMaps = (List<Map>) paramMap.get(Dict.ENTITYTEMPLATEPARAMS);
                        List<Map> entityTemplateInputInfo = entityTypeInputInfo(templateMaps);
                        input.addAll(entityTemplateInputInfo);
                        if (!CommonUtils.isNullOrEmpty(pluginNameEn)) {
                            if (envPluginMap.containsKey(pluginNameEn)) {
                                List<String> envParamKeys = envPluginMap.get(pluginNameEn);
                                for (int i = 0; i < templateMaps.size(); i++) {
                                    Map templateMap = templateMaps.get(i);
                                    String paramKey = (String) templateMap.get(Dict.NAMEEN);
                                    if (envParamKeys.contains(paramKey)) {
                                        Map innerEnv = (Map) templateMap.get(Dict.ENV);
                                        //??????????????????
                                        if (!CommonUtils.isNullOrEmpty(innerEnv)) {
                                            String envNameEn = (String) innerEnv.get(Dict.NAMEEN);
                                            Map imageEnvMap = inputValue(Dict.IMAGE_ENV,envNameEn);
                                            input.add(imageEnvMap);
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }

        //????????????????????????????????????input???
        List<Map> addParams = pluginInfo.getAddParams();
        if (!CommonUtils.isNullOrEmpty(addParams)) {
            for (Map addParam : addParams) {
                if (Dict.CUSTOMVARIABLETYPE.equals(addParam.get(Dict.TYPE))) {
                    Object customParams = addParam.get(Dict.CUSTOMPARAMS);
                    if (!CommonUtils.isNullOrEmpty(customParams)) {
                        Map customParamsMap = (Map) customParams;
                        Map customVariableTypeMap = inputValue((String) customParamsMap.get(Dict.KEY), customParamsMap.get(Dict.VALUE));
                        input.add(customVariableTypeMap);
                    }
                } else if (Dict.ENTITYTEMPLATETYPE.equals(addParam.get(Dict.TYPE))) {
                    List<Map> templateMaps = new ArrayList<>();
                    templateMaps.add(addParam);
                    List<Map> entityTemplateMap = entityTypeInputInfo(templateMaps);
                    input.addAll(entityTemplateMap);
                } else if (Dict.ENTITYTYPE.equals(addParam.get(Dict.TYPE))) {
                    List<Map> oldTemplateMaps = new ArrayList<>();
                    oldTemplateMaps.add(addParam);
                    List<Map> oldEntityTemplateMap = entityTypeInputInfo(oldTemplateMaps);
                    input.addAll(oldEntityTemplateMap);
                }
            }
        }
        return input;
    }


    /**
     * ?????????????????????
     *
     * @return
     */
    public List<Map> minioAccessInfo() {
        List<Map> input = new ArrayList<>();
        String[] splitInfo = minioEndPoint.split("//");
        String endPoint = splitInfo[1];
        Map minioUrlMap = inputValue(Dict.MINIO_URL, endPoint);
        input.add(minioUrlMap);
        Map minioBucketMap = inputValue(Dict.MINIO_BUCKET, mioBuket);
        input.add(minioBucketMap);
        Map minioAccessMap = inputValue(Dict.MINIO_ACCESS_KEY, minioAccessKey);
        input.add(minioAccessMap);
        Map minioScretMap = inputValue(Dict.MINIO_SECRET_KEY, minioSecretKey);
        input.add(minioScretMap);
        return input;
    }

    /**
     * ????????????????????????input
     *
     * @param templateMaps
     * @return
     */
    public List<Map> entityTypeInputInfo(List<Map> templateMaps) throws Exception {
        List<Map> input = new ArrayList<>();
        for (Map templateMap : templateMaps) {
            if (!CommonUtils.isNullOrEmpty(templateMap.get(Dict.ENTITY)) && !CommonUtils.isNullOrEmpty(templateMap.get(Dict.ENV))) {
                Map entity = (Map) templateMap.get(Dict.ENTITY);
                Map env = (Map) templateMap.get(Dict.ENV);
                String[] entityIds = new String[1];
                entityIds[0] = (String) entity.get(Dict.ID);
                String envName = (String) env.get(Dict.NAMEEN);
                if (!CommonUtils.isNullOrEmpty(entityIds) && !CommonUtils.isNullOrEmpty(envName)) {
                    /*List<Map> variablesList = appService.queryEntityVariables(entityName, envName);
                    if (!CommonUtils.isNullOrEmpty(variablesList)){
                        for (Map variables : variablesList) {
                            Map<String, String> inputEntityEnvMap = new HashMap<>();
                            inputEntityEnvMap.put(Dict.KEY, String.valueOf(variables.get(Dict.NAME_EN)));
                            inputEntityEnvMap.put(Dict.VALUE, String.valueOf(variables.get(Dict.VALUE)));
                            input.add(inputEntityEnvMap);
                        }
                    }*/
                    Map resultMap = appService.queryEntityMapping(entityIds, envName);
                    if (!CommonUtils.isNullOrEmpty(resultMap)) {
                        for (Object key : resultMap.keySet()) {
                            Map inputEntityEnvMap = new HashMap<>();
                            inputEntityEnvMap.put(Dict.KEY, key);
                            inputEntityEnvMap.put(Dict.VALUE, resultMap.get(key));
                            input.add(inputEntityEnvMap);
                        }
                    }

                }
            } else {
                if (!CommonUtils.isNullOrEmpty(templateMap.get(Dict.ENTITYPARAMS))) {
                    List<Map> map = (List<Map>) templateMap.get(Dict.ENTITYPARAMS);
                    for (Map paramMap : map) {
                        Map inputTemplateEntityParamMap = inputValue((String) paramMap.get(Dict.KEY), paramMap.get(Dict.VALUE));
                        input.add(inputTemplateEntityParamMap);
                    }
                }
            }
        }
        return input;
    }

    public Map inputValue(String firstValue, Object secondValue) {
        Map map = new HashMap<>();
        map.put(Dict.KEY, firstValue);
        map.put(Dict.VALUE, secondValue);
        return map;
    }

    private boolean isTrigger(List<String> branch, String ref) {
        //??????????????????????????????????????????\
        if (!CommonUtils.isNullOrEmpty(branch)) {
            for (String reg : branch) {
                Pattern pattern = Pattern.compile(reg);
                Matcher matcher = pattern.matcher(ref);
                if (matcher.matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addJobExeQueue(JobExe jobExe, String runnerClusterId) throws Exception {
        ByteArrayOutputStream bio = ObjectUtil.object2Bytes(jobExe);
        logger.info("**************** save redis data :" + bio.toByteArray());
        redisTemplate.opsForList().rightPush(Constants.JOB_EXE_QUEUE_REDIS_KEY_PROFIX + runnerClusterId, bio.toByteArray());
    }


    @Override
    public List<Pipeline> queryDetailByProjectId(String id) throws Exception {
        return pipelineDao.queryDetailByProjectId(id);
    }

    /**
     * ??????job??????
     *
     * @param pipelineExeId
     * @param stageIndex
     * @param jobIndex
     * @throws Exception
     */
    @Override
    public void retryPipeline(String pipelineExeId, Integer stageIndex, Integer jobIndex) throws Exception {
        PipelineExe pipelineExe = pipelineExeService.queryPipelineExeByExeId(pipelineExeId);
        String pipelineId = pipelineExe.getPipelineId();
        Pipeline pipeline = this.pipelineDao.queryById(pipelineId);
        User user = userService.getUserFromRedis();
        if (!vaildateUtil.projectCondition(pipeline.getBindProject().getProjectId())
                && !pipelineExe.getUser().equals(user.getUser_name_en())) {
            logger.error("**********user has not permission to retry");
            throw new FdevException(ErrorConstants.RETRY_FAILED, new String[]{"current user illegal permission"});
        }
        checkRetryStatus(pipelineExe);
        //??????job????????????????????????????????????
        List<Map<String, Object>> stagesList = pipelineExe.getStages();
        if (stageIndex > 0) {
            for (int i = 0; i < stageIndex; i++) {
                String stageStatus = (String) stagesList.get(i).get(Dict.STATUS);
                if (!Dict.SUCCESS.equals(stageStatus)) {
                    logger.error("*********** previous stage???" + i + " status???" + stageStatus + "???current stage???" + stageIndex
                            + " cannot retry, pipelineExeId: " + pipelineExeId);
                    //throw new Exception("????????????????????????????????????????????????????????????");
                    throw new FdevException(ErrorConstants.PREVIOUS_TASK_FAIL);
                }
            }
        }
        Author author = userService.getAuthor();
        JobExe orgJobExe = jobExeDao.queryJobExeByIndex(pipelineExeId, stageIndex, jobIndex);
        //1 ????????????jobexe
        JobExe jobExe = new JobExe();
        Long number = jobExe.getJobNumber();
        ObjectId objId = new ObjectId();
        BeanUtils.copyProperties(orgJobExe, jobExe);
        jobExe.setJobNumber(number);
        jobExe.setExeId(objId.toString());
        jobExe.setUser(author);
        jobExe.setStatus(Dict.PENDING);
        jobExe.setJobStartTime("");
        jobExe.setJobCostTime("");
        jobExe.setJobEndTime("");
        jobExe.setMinioLogUrl("");
        jobExe.setInfo(null);
        jobExe.setToken("");
        //??????jobExe??????steps
        List<Map> jobExeSteps = jobExe.getSteps();
        for (Map jobExeStep : jobExeSteps) {
            jobExeStep.remove(Dict.OUTPUT);
            jobExeStep.remove(Dict.STEPSTARTTIME);
            jobExeStep.remove(Dict.STEPENDTIME);
            jobExeStep.remove(Dict.STEPCOSTTIME);
            jobExeStep.remove(Dict.STATUS);
        }

        jobExeService.saveJobExe(jobExe);

        //2 ??????pipelineExe jobs??????jobExes????????????jobExe
        Map<String, Object> stage = pipelineExe.getStages().get(stageIndex);
        stage.put(Dict.STATUS, Dict.PENDING);
        List<Map> stageJobs = (List<Map>) stage.get(Dict.JOBS);
        List<Map> jobExes = (List<Map>) stageJobs.get(jobIndex).get(Dict.JOBEXES);
        //??????stage-jobs-jobexes??????job??????
        String jobNumber = String.valueOf(jobExe.getJobNumber());
        Map exeMap = new HashMap();
        exeMap.put(Dict.JOBEXEID, objId.toString());
        exeMap.put(Dict.JOBNUMBER, jobNumber);
        exeMap.put(Dict.JOBEXESTATUS, Dict.PENDING);
        jobExes.add(exeMap);
        pipelineExe.setStatus(Dict.PENDING);
        logger.info("***************** retryPipeline will update pipelineExe; pipelineExe info:" + JSONObject.toJSON(pipelineExe));
        pipelineExeService.updateStagesAndStatus(pipelineExe);

        //3 ??????????????????????????????????????????????????????????????????
        pipeline.setBuildTime(pipelineExe.getStartTime());
        pipeline.setUpdateTime(pipelineExe.getStartTime());
        pipelineDao.updateBuildTime(pipeline);

        //4 ??????pending???????????????
        Job job = pipeline.getStages().get(stageIndex).getJobs().get(jobIndex);
        String runnerClusterId = job.getRunnerClusterId();
        addJobExeQueue(jobExe, runnerClusterId);
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param request
     * @throws Exception
     */
    @Override
    public void continueRunPipeline(Map request) throws Exception {
        String pipelineExeId = (String) request.get(Dict.PIPELINEEXEID);
        String pipelineNumber = (String) request.get(Dict.PIPELINENUMBER);
        if (CommonUtils.isNullOrEmpty(pipelineExeId) && CommonUtils.isNullOrEmpty(pipelineNumber)) {
            logger.error("****** continueRunPipeline pipelineExeId and pipelineNumber cannot all be empty!");
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_BOTH_EMPTY, new String[]{"pipelineExeId and pipelineNumber"});
        }
        //1??????????????????????????????????????????????????????????????????????????????
        PipelineExe pipelineExe = pipelineExeService.queryPipelineExeByExeId(pipelineExeId);
        if (pipelineExe == null) {
            logger.error("******pipelineExe not exist, pipelineExeId: " + pipelineExeId);
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST);
        }
        checkRetryStatus(pipelineExe);
        String pipelineId = pipelineExe.getPipelineId();
        Pipeline pipeline = this.pipelineDao.queryById(pipelineId);
        //2???????????????????????????stage?????????????????????job???????????????job?????????jobexe??????????????????????????????
        logger.info("*********** continueRunPipeline??? pipelineExeId???" + pipelineExeId);
        List pendingJobs = new ArrayList();
        List<Map<String, Object>> stages = pipelineExe.getStages();
        Author author = pipelineExe.getUser();
        boolean firstStage = true;
        for (int i = 0; i < stages.size(); i++) {
            Map<String, Object> stage = stages.get(i);
            String stageStatus = (String) stage.get(Dict.STATUS);
            if (Dict.SUCCESS.equals(stageStatus)) {
                continue;
            }
            //???????????????stage???pending
            String jobStatus = Dict.WAITING;
            if (firstStage) {
                firstStage = false;
                jobStatus = Dict.PENDING;
            }
            stage.put(Dict.STATUS, jobStatus);
            Stage pipStage = pipeline.getStages().get(i);
            List<Map> jobs = (List<Map>) stage.get(Dict.JOBS);

            for (int j = 0; j < jobs.size(); j++) {
                Map job = jobs.get(j);
                List<Map> stageJobExes = (List<Map>) job.get(Dict.JOBEXES);
                //??????????????????jobexeMap
                Map map = stageJobExes.get(stageJobExes.size() - 1);
                String jobExeId = (String) map.get(Dict.JOBEXEID);
                //????????????jobexe
                JobExe jobExe = new JobExe();
                Long number = jobExe.getJobNumber();
                ObjectId objId = new ObjectId();
                JobExe orgJobExe = jobExeService.queryJobExeByExeId(jobExeId);
                if (firstStage && orgJobExe.getStatus().equals(Dict.SUCCESS)) {
                    //???????????????????????????????????????????????????job?????????????????????job?????????????????????stage??????job??????
                    continue;
                }
                BeanUtils.copyProperties(orgJobExe, jobExe);
                jobExe.setJobNumber(number);
                jobExe.setExeId(objId.toString());
                jobExe.setUser(author);
                jobExe.setStatus(jobStatus);
                jobExe.setJobStartTime("");
                jobExe.setJobCostTime("");
                jobExe.setJobEndTime("");
                jobExe.setMinioLogUrl("");
                jobExe.setInfo(null);
                jobExe.setToken("");
                //??????jobExe??????steps
                List<Map> jobExeSteps = jobExe.getSteps();
                for (Map jobExeStep : jobExeSteps) {
                    jobExeStep.remove(Dict.OUTPUT);
                    jobExeStep.remove(Dict.STEPSTARTTIME);
                    jobExeStep.remove(Dict.STEPENDTIME);
                    jobExeStep.remove(Dict.STEPCOSTTIME);
                    jobExeStep.remove(Dict.STATUS);
                }

                for (Map step : jobExe.getSteps()) {
                    List<Map> input = (List<Map>) step.get(Dict.INPUT);
                    for (Map inputMap : input) {
                        String skipKey = (String) inputMap.get(Dict.KEY);
                        if (Dict.SKIP_MANUAL_REVIEW.equals(skipKey)) {
                            logger.info(" current stuck point running key :" + skipKey + " ,value:" + inputMap.get(Dict.VALUE));
                            inputMap.put(Dict.VALUE, Dict.PARAM_TRUE);
                        }
                    }
//                    Map paramMap = new HashMap();
//                    paramMap.put(Dict.KEY, "SKIP_MANUAL_REVIEW");
//                    paramMap.put(Dict.VALUE, "true");
//                    input.add(paramMap);
                }
                jobExeService.saveJobExe(jobExe);
                if (Dict.PENDING.equals(jobStatus)) {
                    Job pipJob = pipStage.getJobs().get(j);
                    Map jobMap = new HashMap();
                    jobMap.put(Dict.JOB_EXE, jobExe);
                    jobMap.put(Dict.RUNNERCLUSTERID, pipJob.getRunnerClusterId());
                    pendingJobs.add(jobMap);
                }

                //??????stage-jobs-jobexes??????job??????
                String jobNumber = String.valueOf(jobExe.getJobNumber());
                Map exeMap = new HashMap();
                exeMap.put(Dict.JOBEXEID, objId.toString());
                exeMap.put(Dict.JOBNUMBER, jobNumber);
                exeMap.put(Dict.JOBEXESTATUS, jobStatus);
                stageJobExes.add(exeMap);
            }

        }
        //??????pipelineExe
        pipelineExe.setStatus(Dict.PENDING);
        //pipelineExe.setStartTime(CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
        pipelineExe.setCostTime("");
        pipelineExe.setEndTime("");
        //pipelineExe.setUser(author);
        logger.info("********** continueRunPipeline will update the pipelineExe info:" + JSONObject.toJSON(pipelineExe));
        pipelineExeService.updateStagesAndStatusAndUser(pipelineExe);
        //??????pipeline???????????????????????????
        pipeline.setBuildTime(pipelineExe.getStartTime());
        pipeline.setUpdateTime(pipelineExe.getStartTime());
        pipelineDao.updateBuildTime(pipeline);
        for (Map pendingJob : (List<Map>) pendingJobs) {
            //??????pending???????????????
            JobExe jobExe = (JobExe) pendingJob.get(Dict.JOB_EXE);
            String runnerClusterId = (String) pendingJob.get(Dict.RUNNERCLUSTERID);
            addJobExeQueue(jobExe, runnerClusterId);
        }

    }

    @Override
    public Map<String, Object> queryPipelineHistory(String nameId, String pageNum, String pageSize) throws Exception {
        long skip = (Integer.valueOf(pageNum) - 1) * Integer.valueOf(pageSize);
        int limit = Integer.valueOf(pageSize);
        return pipelineDao.findHistoryPipelineList(skip, limit, nameId);
    }

    /**
     * ??????????????????????????????
     *
     * @param pipeline
     * @return
     * @throws Exception
     */
    public Pipeline preparePipelineDetailInfo(Pipeline pipeline) throws Exception {
        if (CommonUtils.isNullOrEmpty(pipeline))
            return null;
        //2. ????????????????????????
        boolean isAppManager = vaildateUtil.projectCondition(pipeline.getBindProject().getProjectId());
        pipeline.setUpdateRight(isAppManager);
        //3???????????????image??????
        for (Stage stage : pipeline.getStages()) {
            for (Job job : stage.getJobs()) {
                //??????image
                Images image = job.getImage();
                if (image != null && !CommonUtils.isNullOrEmpty(image.getId())) {
                    image = pipelineDao.findImageById(image.getId());
                    job.setImage(image);
                }
                for (Step step : job.getSteps()) {
                    PluginInfo pluginInfo = step.getPluginInfo();
                    String scriptCmd = null;
                    Map<String, String> script = pluginInfo.getScript();
                    if (!CommonUtils.isNullOrEmpty(script)) {
                        String mioPath = script.get(Dict.MINIO_OBJECT_NAME);
                        if (!CommonUtils.isNullOrEmpty(mioPath)) {
                            scriptCmd = fileService.downloadDocumentFile(mioBuket, mioPath);
                        }
                    }
                    pluginInfo.setScriptCmd(scriptCmd);
                    prePareResultShowPluginInfo(pluginInfo);
                }
            }
        }
        //4???????????????
        String userId = userService.getUserFromRedis().getId();
        if (!CommonUtils.isNullOrEmpty(pipeline.getCollected())) {
            if (pipeline.getCollected().contains(userId)) {
                pipeline.setCollectedOrNot(true);
            } else {
                pipeline.setCollectedOrNot(false);
            }
        } else {
            pipeline.setCollectedOrNot(false);
        }
        return pipeline;
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param pluginInfo
     * @return
     * @throws Exception
     */
    @Override
    public PluginInfo prePareResultShowPluginInfo(PluginInfo pluginInfo) throws Exception {
        Plugin plugin = pluginDao.queryPluginDetail(pluginInfo.getPluginCode());
        //???????????????plugin  ????????????script?????????????????????????????????params???
        if (plugin == null) {
            logger.error("**********plugin not exist, pluginId=" + pluginInfo.getPluginCode());
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"plugin not exist"});
        }
        List<Map<String, Object>> params = pluginInfo.getParams();
        //??????????????????????????????????????????,?????????????????????????????????????????????????????????????????????
        List<Map> inputEntityTempParamList = pluginInfo.getEntityTemplateParams();
        if (!CommonUtils.isNullOrEmpty(inputEntityTempParamList)) {
            for (Map map : inputEntityTempParamList) {
                Map entityTemplateParam = new HashMap();
                entityTemplateParam.put(Dict.KEY, map.get(Dict.NAMEEN));
                entityTemplateParam.put(Dict.VALUE, null);
                entityTemplateParam.put(Dict.TYPE, Dict.ENTITYTYPE);
                //??????????????????  ????????????????????????
                List<Map> entityTemplateParamList = new ArrayList<>();
                entityTemplateParamList.add(map);
                entityTemplateParam.put(Dict.ENTITYTEMPLATEPARAMS, entityTemplateParamList);
                params.add(entityTemplateParam);
            }
        }

        //??????label hint default????????????
        if (!CommonUtils.isNullOrEmpty(params)) {
            List<Map<String, Object>> resultParam = prepareShowParam(params, plugin);
            pluginInfo.setParams(resultParam);
        }
        pluginInfo.setEntityTemplateParams(null);
        //??????????????????????????????
        Boolean validFlag = Constants.STATUS_OPEN.equals(plugin.getStatus()) ? true : false;
        pluginInfo.setValidFlag(validFlag);
        return pluginInfo;
    }

    @Override
    public Images findImageById(String id) {
        Images images = pipelineDao.findImageById(id);
        //??????????????????????????????
        if (!CommonUtils.isNullOrEmpty(images)) {
            images.setAuthor(null);
        }
        return images;
    }

    /**
     * ??????????????? param???
     *
     * @param params
     * @param plugin
     * @return
     */
    public List<Map<String, Object>> prepareShowParam(List<Map<String, Object>> params, Plugin plugin) {
        //??????????????????param
        List<Map<String, Object>> pluginParams = plugin.getParams();
        if (CommonUtils.isNullOrEmpty(pluginParams)) {
            params = null;
        } else {
            //??????????????????param???key?????????null
            for (Map resultMap : params) {
                if (!CommonUtils.isNullOrEmpty(resultMap.get(Dict.KEY))) {
                    //???plugin?????????key?????????
                    for (Map pluginMap : pluginParams) {
                        if (String.valueOf(resultMap.get(Dict.KEY)).equals(String.valueOf(pluginMap.get(Dict.KEY)))) {
                            pluginMap.remove(Dict.VALUE);
                            pluginMap.remove(Dict.VALUEARRAY);
                            pluginMap.remove(Dict.ENTITYTEMPLATEPARAMS);
                            resultMap.putAll(pluginMap);
                            //??????hidden label hint default defaultArr
                            /*resultMap.put(Dict.HIDDEN,pluginMap.get(Dict.HIDDEN));
                            resultMap.put(Dict.LABEL,pluginMap.get(Dict.LABEL));
                            resultMap.put(Dict.HINT,pluginMap.get(Dict.HINT));
                            resultMap.put(Dict.DEFAULT,pluginMap.get(Dict.DEFAULT));
                            resultMap.put(Dict.DEFAULTARR,pluginMap.get(Dict.DEFAULTARR));
                            resultMap.put(Dict.REQUIRED,pluginMap.get(Dict.REQUIRED));
                            resultMap.put(Dict.ITEMVALUE,pluginMap.get(Dict.ITEMVALUE));*/
                            break;
                        }
                    }
                }
            }
        }
        return params;
    }

    @Override
    public String copy(String id) throws Exception {
        Pipeline pipeline = pipelineDao.queryById(id);
        if (CommonUtils.isNullOrEmpty(pipeline)) {
            logger.error("**************The pipeline not exist***************");
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"pipelineId" + id});
        }
        //?????????pipeline
        Pipeline newPipeline = CommonUtils.map2Object(CommonUtils.obj2Map(pipeline), Pipeline.class);
//        BeanUtils.copyProperties(pipeline,newPipeline);
        newPipeline.setName(newPipeline.getName() + Dict._COPY);
        Author author = userService.getAuthor();
        newPipeline.setAuthor(author);
        //????????????
        List<String> user = new ArrayList<>();
        String userId = author.getId();
        if (!CommonUtils.isNullOrEmpty(newPipeline.getCollected())) {
            if (newPipeline.getCollected().contains(userId)) {
                user.add(userId);
            }
        }
        newPipeline.setNameId(null);
        newPipeline.setId(null);
        newPipeline.setCollected(user);
        newPipeline.setVersion(Constants.UP_CHANGE_VERSION);
        iPluginService.copyYamlConfigInStages(newPipeline.getStages());
        String pipelineId = pipelineDao.add(newPipeline);
        updateUseCountWhenAdd(pipelineId);
        return pipelineId;
    }

    /**
     * ??????pipeline
     *
     * @param pipelineExeId
     * @throws Exception
     */
    @Override
    public PipelineExe stopPipeline(String pipelineExeId, Boolean skipManualReview) throws Exception {
        PipelineExe pipelineExe = this.pipelineExeDao.queryPipelineExeByExeId(pipelineExeId);
        //1 ????????????????????????
        if (CommonUtils.isNullOrEmpty(skipManualReview) || !skipManualReview)
            checkCancelCondition(userService.getUserFromRedis(), pipelineExe);
        logger.info("**********begin to stopPipeline, pipelineExeId: " + pipelineExeId + ", status: " + pipelineExe.getStatus());
        List<Map<String, Object>> stages = pipelineExe.getStages();
        for (int i = 0; i < stages.size(); i++) {
            Map<String, Object> stage = stages.get(i);
            List<Map> jobs = (List<Map>) stage.get(Dict.JOBS);
            for (Map job : jobs) {
                List<Map> jobExes = (List<Map>) job.get(Dict.JOBEXES);
                Map jobExeMap = jobExes.get(jobExes.size() - 1);
                String jobExeId = (String) jobExeMap.get(Dict.JOBEXEID);
                String jobExeStatus = (String) jobExeMap.get(Dict.JOBEXESTATUS);
                JobExe jobExe = this.jobExeDao.queryJobExeByExeId(jobExeId);
                if (Dict.RUNNING.equals(jobExeStatus) || Dict.PENDING.equals(jobExeStatus)) {
                    //2 ??????jobexe
                    //??????job?????????????????????????????????
                    if (Dict.PENDING.equals(jobExe.getStatus())) {
                        jobExe.setJobStartTime(CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
                    }
                    jobExe.setJobEndTime(CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
                    jobExe.setJobCostTime(CommonUtils.getCostDate(jobExe.getJobStartTime(), jobExe.getJobEndTime(), CommonUtils.STANDARDDATEPATTERN));
                    jobExe.setStatus(Dict.CANCEL);
                    jobExe.setMinioLogUrl("");

                    //3 ??????pipelineExe???running??????pending???jobExeMap
                    jobExeMap.put(Dict.JOBEXESTATUS, jobExe.getStatus());
                    jobExeMap.put(Dict.JOBSTARTTIME, jobExe.getJobStartTime());
                    jobExeMap.put(Dict.JOBENDTIME, jobExe.getJobEndTime());
                    jobExeMap.put(Dict.JOBCOSTTIME, jobExe.getJobCostTime());
                    stage.put(Dict.STATUS, Dict.CANCEL);
                }
                // ????????????stage???cancel
                if (Dict.WAITING.equals(jobExeStatus)) {
                    jobExe.setStatus(Dict.CANCEL);
                    jobExe.setMinioLogUrl("");
                    jobExeMap.put(Dict.JOBEXESTATUS, jobExe.getStatus());
                    stage.put(Dict.STATUS, Dict.CANCEL);
                }
                this.jobExeDao.updateJobFinish(jobExe);
            }
        }

        //??????pipelineExe????????????cancel
        pipelineExe.setStatus(Dict.CANCEL);
        pipelineExe.setStages(stages);
        pipelineExe.setEndTime(CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
        pipelineExe.setCostTime(pipelineExeService.calculatePipelineExeCostTime(pipelineExe));
        pipelineExeDao.updateStagesAndStatus(pipelineExe);
        return pipelineExe;
    }

    /**
     * ????????????????????????/????????????????????????????????????????????????/????????????????????????????????????/????????????
     *
     * @param user        ????????????
     * @param pipelineExe
     * @throws Exception ??????????????????????????????????????????true
     */
    private void checkCancelCondition(User user, PipelineExe pipelineExe) throws Exception {
        Map bindProject = pipelineExe.getBindProject();
        String projectId = (String) bindProject.get(Dict.PROJECTID);
        //????????????????????????????????????????????????????????????????????????
        if (!vaildateUtil.projectCondition(projectId) && !pipelineExe.getUser().equals(user.getUser_name_en())) {
            logger.error("**********user has not role to cancel");
            throw new FdevException(ErrorConstants.ROLE_ERROR);
        }
        if (!Dict.RUNNING.equals(pipelineExe.getStatus()) && !Dict.PENDING.equals(pipelineExe.getStatus())) {
            logger.error("********** pipelineExe status cannot cancel , pipelineExeId: " + pipelineExe.getExeId() + ", status: " + pipelineExe.getStatus());
            throw new FdevException(ErrorConstants.PIPELINE_IS_FINISHED);
        }
    }

    /**
     * ??????jobExe ??????????????????????????????????????????jobExe??????????????????job/request???plugin/request???plugin/input???plugin/output???artifacts/webhook???artifacts/request  ???????????????stop?????????????????????
     *
     * @param pipelineExeId
     * @param stageIndex
     * @param jobIndex
     * @throws Exception
     */
    @Override
    public JobExe stopJob(String pipelineExeId, Integer stageIndex, Integer jobIndex) throws Exception {
        PipelineExe pipelineExe = this.pipelineExeDao.queryPipelineExeByExeId(pipelineExeId);
        //1 ????????????????????????
        checkCancelCondition(userService.getUserFromRedis(), pipelineExe);
        //2 ??????jobExe?????????
        JobExe jobExe = this.jobExeDao.queryJobExeByIndex(pipelineExeId, stageIndex, jobIndex);
        if (!Dict.RUNNING.equals(jobExe.getStatus()) && !Dict.PENDING.equals(jobExe.getStatus())) {
            logger.error("********** jobExe status cannot cancel , jobExeID: " + jobExe.getExeId() + ", status: " + pipelineExe.getStatus());
            throw new FdevException(ErrorConstants.JOB_CANNOT_CANCEL);
        }
        logger.info("**********begin to stopJob, jobExeId: " + jobExe.getExeId() + ", status: " + jobExe.getStatus());
        //??????job?????????????????????????????????
        if (Dict.PENDING.equals(jobExe.getStatus())) {
            jobExe.setJobStartTime(CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
        }
        jobExe.setJobEndTime(CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
        jobExe.setJobCostTime(CommonUtils.getCostDate(jobExe.getJobStartTime(), jobExe.getJobEndTime(), CommonUtils.STANDARDDATEPATTERN));
        jobExe.setStatus(Dict.CANCEL);
        jobExe.setMinioLogUrl("");
        this.jobExeDao.updateJobFinish(jobExe);
        //3 ??????pipelineExe???jobExe??????
        List<Map<String, Object>> stages = pipelineExe.getStages();
        Map<String, Object> stageMap = stages.get(stageIndex);
        List<Map> jobs = (List<Map>) stageMap.get(Dict.JOBS);
        Map job = jobs.get(jobIndex);
        List<Map> jobExes = (List<Map>) job.get(Dict.JOBEXES);
        Map jobExeMap = jobExes.get(jobExes.size() - 1);
        jobExeMap.put(Dict.JOBEXESTATUS, jobExe.getStatus());
        jobExeMap.put(Dict.JOBSTARTTIME, jobExe.getJobStartTime());
        jobExeMap.put(Dict.JOBENDTIME, jobExe.getJobEndTime());
        jobExeMap.put(Dict.JOBCOSTTIME, jobExe.getJobCostTime());
        //4 ??????????????????stage?????????????????????stage???pipeline?????????????????????????????????
        String stageStatus = pipelineExeService.getStageFinalStatus(pipelineExe, stageIndex);
        if (Dict.CANCEL.equals(stageStatus)) {
            stageMap.put(Dict.STATUS, stageStatus);
            pipelineExe.setStatus(stageStatus);
        }
        // ????????????stage
        for (int i = stageIndex + 1; i < stages.size(); i++) {
            Map<String, Object> stage = stages.get(i);
            List<Map> jobsAfter = (List<Map>) stage.get(Dict.JOBS);
            for (Map jobAfter : jobsAfter) {
                List<Map> jobExesSkip = (List<Map>) jobAfter.get(Dict.JOBEXES);
                Map jobExeMapAfter = jobExesSkip.get(jobExesSkip.size() - 1);
                String jobExeId = (String) jobExeMapAfter.get(Dict.JOBEXEID);
                String jobExeStatus = (String) jobExeMapAfter.get(Dict.JOBEXESTATUS);
                if (Dict.WAITING.equals(jobExeStatus)) {
                    //2 ??????jobexe
                    JobExe jobExeSkip = this.jobExeDao.queryJobExeByExeId(jobExeId);
                    jobExeSkip.setStatus(Dict.SKIP);
                    jobExeSkip.setMinioLogUrl("");
                    this.jobExeDao.updateJobFinish(jobExeSkip);

                    //3 ??????pipelineExe???running??????pending???jobExeMap
                    jobExeMapAfter.put(Dict.JOBEXESTATUS, jobExeSkip.getStatus());
                }
            }
            stage.put(Dict.STATUS, Dict.SKIP);
        }
        pipelineExe.setStages(stages);
        this.pipelineExeDao.updateStagesAndStatus(pipelineExe);
        return jobExe;
    }

    @Override
    public List<Pipeline> getSchedulePipelines() {
        return this.pipelineDao.querySchedulePipelines();
    }

    @Override
    public Map queryTriggerRules(String id) throws Exception {
        Pipeline pipeline = pipelineDao.queryById(id);
        if (CommonUtils.isNullOrEmpty(pipeline)) {
            logger.error("**************The pipeline not exist***************");
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"pipelineId" + id});
        }
        TriggerRules triggerRules = pipeline.getTriggerRules();
        if (CommonUtils.isNullOrEmpty(triggerRules)) {
            triggerRules = new TriggerRules();
            Push push = new Push();
            push.setSwitchFlag(false);
            triggerRules.setPush(push);
            Schedule schedule = new Schedule();
            schedule.setSwitchFlag(false);
            triggerRules.setPush(push);
            triggerRules.setSchedule(schedule);
        }
        //????????????schedule/push??????,??????????????????
        if (!CommonUtils.isNullOrEmpty(triggerRules)) {
            if (CommonUtils.isNullOrEmpty(triggerRules.getSchedule())) {
                Schedule schedule = new Schedule();
                schedule.setSwitchFlag(false);
                triggerRules.setSchedule(schedule);
            }
            if (CommonUtils.isNullOrEmpty(triggerRules.getPush())) {
                Push push = new Push();
                push.setSwitchFlag(false);
                triggerRules.setPush(push);
            }
        }
        Map resultNap = new HashMap<>();
        resultNap.put(Dict.TRIGGERRULES, triggerRules);
        return resultNap;
    }

    @Override
    public String updateTriggerRules(String pipelineId, TriggerRules triggerRules) throws Exception {
        Pipeline pipeline = pipelineDao.queryById(pipelineId);
        if (CommonUtils.isNullOrEmpty(pipeline)) {
            logger.error("**************The pipeline not exist***************");
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"pipelineId" + pipelineId});
        }
        boolean isAppManager = vaildateUtil.projectCondition(pipeline.getBindProject().getProjectId());
        if (!isAppManager) {
            throw new FdevException(ErrorConstants.USER_NOT_APPMANAGER);
        }
        String result = pipelineDao.updateTriggerRules(pipelineId, triggerRules);
        return result;
    }


    /**
     * ??????????????????????????????id???nameId???????????????
     *
     * @param request
     * @return
     */
    @Override
    public Pipeline queryByNameId(Map request) {
        //1. ???????????????pipeline
        Pipeline pipeline = pipelineDao.queryPipelineByIdOrNameId(request);
        return pipeline;
    }

    /**
     * ????????????
     *
     * @param
     * @return
     */
    @Override
    public void downLoadArtifacts(String name, HttpServletResponse response) throws Exception {
        User currentLoginUser = null;
        try {
            currentLoginUser = this.userService.getUserFromRedis();
        } catch (Exception e) {
            logger.info(" ????????????????????????????????? " + e.getMessage());
        }
        if (currentLoginUser != null) {
            /*List<String> lineIds = this.userService.getLineIdsByGroupId(currentLoginUser.getGroup_id());
            //?????????????????????id
            if (CommonUtils.isNullOrEmpty(lineIds)) {
                logger.info(" current user lineId is null, groupId:" + currentLoginUser.getGroup_id());
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{currentLoginUser.getGroup_id()});
            }*/
            String exeId = "";
            String[] strings = name.split("/");
            for (String string : strings) {
                if (!string.contains("-")) {
                    if (!string.contains(".")) {
                        exeId = string;
                        break;
                    }
                }
            }
            if (CommonUtils.isNullOrEmpty(exeId)) {
                logger.info(" exeId is null, exeId:" + exeId);
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{exeId});
            }
        }
        InputStream in = null;
        ServletOutputStream out = null;
        String path = name;
        try {
            String input = username + ":" + password;
            if (path.indexOf("/") == 0) {
                path = path.substring(1, path.length());
            }
            URL url = new URL("http://arti.spdb.com:80/artifactory/" + path);
            // URL url = new URL( "http://arti.spdb.com:80/artifactory/fdevtest-generic-local/golang-plugin-cmd.md");
            URLConnection connection = url.openConnection();
            BASE64Encoder base = new BASE64Encoder();
            String encode = base.encode(input.getBytes("UTF-8"));
            connection.setRequestProperty("Authorization", "Basic " + encode);
            in = connection.getInputStream();
            response.reset();
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            String filename = path.substring(path.lastIndexOf("/") + 1);
            response.setHeader("Content-Disposition", "attachment;filename=" + filename);
            out = response.getOutputStream();
            int len = 0;
            byte[] bytes = new byte[1024 * 9];
            while ((len = in.read(bytes)) != -1) {
                out.write(bytes, 0, len);
            }
            out.flush();
        } catch (Exception e) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????"});
        } finally {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }

    @Override
    public List getPipelineByEntityId(Map request) {
        return pipelineDao.queryByEntityId(request);
    }

    @Override
    public List<Map> getPipelineHistoryVersion(Map request) {
        String pipelineId = (String) request.get(Dict.PIPELINEID);
        if (!CommonUtils.isNullOrEmpty(pipelineId)) {
            //????????????pipeline???????????????
            Pipeline pipeline = this.pipelineDao.queryById(pipelineId);
            String nameId = pipeline.getNameId();
            List<Pipeline> pipelines = this.pipelineDao.queryPipelinesByNameId(nameId);
            List resultList = new ArrayList();
            for (Pipeline resultPip : pipelines) {
                //?????????????????????????????????
                if (Constants.ONE.equals(resultPip.getStatus())) {
                    continue;
                }
                Map resultMap = CommonUtils.obj2Map(resultPip);
                //??????????????????pipelineId
                String sourceId = resultPip.getId();
                //?????????????????????id??????diff???????????????id????????????????????????
                PipelineUpdateDiff diffEntity = pipelineUpdateDiffDao.queryDiffBySourceId(sourceId);

                resultMap.put("diffEntity", diffEntity);
                resultList.add(resultMap);
            }
            return resultList;
        } else {
            throw new FdevException(ErrorConstants.PARAMS_IS_ILLEGAL, new String[]{"id is null!!"});
        }
    }

    /**
     * ??????dataGroupId ???????????????User??????????????????????????????admin?????????true
     *
     * @param dataGroupId
     * @return
     * @throws Exception
     */
    @Override
    public Boolean checkGroupidInUserGroup(String dataGroupId) throws Exception {
        User user = userService.getUserFromRedis();
        if (pipelineTemplateDao.checkAdminRole(user.getUser_name_en()) || user.getRole_id().contains(groupRoleAdminId)) {
            return true;
        } /*else {
            List<String> groupIds = userService.getChildGroupIdsByGroupId(user.getGroup_id());
            if (!CommonUtils.isNullOrEmpty(dataGroupId)) {
                if (user.getRole_id().contains(groupRoleAdminId) && groupIds.contains(dataGroupId)) {
                    return true;
                }
            }
        }*/
        return false;
    }


    /**
     * ?????????????????????
     *
     * @param request
     * @return
     */
    @Override
    public String setPipelineRollBack(Map request) throws Exception {
        String id = (String) request.get(Dict.PIPELINEID);
        Pipeline pipeline = this.pipelineDao.queryById(id);
        if (Constants.ONE.equals(pipeline.getStatus())) {
            logger.error(" current pipeline status is using(1)???can't rollback ");
            throw new FdevException(ErrorConstants.PARAMS_IS_ILLEGAL, new String[]{" current pipeline status is using(1)???can't rollback "});
        }
        String nameId = pipeline.getNameId();
        Pipeline currentUsingPip = this.pipelineDao.findActiveVersion(nameId);
        if (CommonUtils.isNullOrEmpty(currentUsingPip)) {
            logger.error(" current using pipeline status is using(1)???can't rollback ");
            throw new FdevException(ErrorConstants.PARAMS_IS_ILLEGAL, new String[]{" current using pipeline status is using(1)???can't rollback "});
        }
        //?????????
        pipeline.setId(new ObjectId().toString());
        String version = currentUsingPip.getVersion();
        Integer versionInt = Integer.valueOf(version) + 1;
        pipeline.setVersion(String.valueOf(versionInt));

        User user = userService.getUserFromRedis();
        Author author = new Author();
        author.setId(user.getId());
        author.setNameCn(user.getUser_name_cn());
        author.setNameEn(user.getUser_name_en());

        pipeline.setAuthor(author);
        pipeline.setUpdateTime(CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));

        //?????????????????????0
        this.pipelineDao.updateStatusClose(currentUsingPip.getId(), user);
        //????????????
        String addedId = this.pipelineDao.add(pipeline);
        //??????diff
        PipelineUpdateDiff diffEntity = DiffUtils.getHandlerPipelineDiff(currentUsingPip, pipeline);
        this.pipelineUpdateDiffDao.saveDiff(diffEntity);
        return addedId;
    }

    /**
     * ????????????pipeline
     *
     * @param request
     * @throws Exception
     */
    @Override
    public void cronStopPipeline(Map request) throws Exception {
        List<PipelineExe> runningJobExes = this.pipelineExeDao.findRunningJobPipeline();
        for (PipelineExe pipelineExe : runningJobExes) {
            for (Map<String, Object> stage : pipelineExe.getStages()) {
                List<Map> jobs = (List<Map>) stage.get(Dict.JOBS);
                for (Map job : jobs) {
                    List<Map> jobExes = (List<Map>) job.get(Dict.JOBEXES);
                    Map jobExe = jobExes.get(jobExes.size() - 1);
                    String status = (String) jobExe.get(Dict.JOBEXESTATUS);
                    if (Dict.RUNNING.equals(status)) {
                        String jobStartTime = (String) jobExe.get(Dict.JOBSTARTTIME);
                        if (CommonUtils.calcTimesTarCurrent(jobStartTime, CommonUtils.STANDARDDATEPATTERN)) {
                            logger.info(" ????????????????????????????????? stop pipeline exe : " + pipelineExe.getExeId());
                            //running?????????????????????????????????
                            this.stopPipeline(pipelineExe.getExeId(), true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void countPipDigital() throws Exception {
        List<Map> pipelines = this.pipelineDao.queryPipLookDigital();
        for (Map pipeline : pipelines) {
            List<Map> pipelineExes = (List) pipeline.get("exeInfo");
            if (pipelineExes.size() == 0)
                continue;
            digitalService.calculateDigital((String) pipeline.get(Dict.NAMEID));
        }
    }
}
