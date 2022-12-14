package com.spdb.fdev.pipeline.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.spdb.fdev.base.dict.Constants;
import com.spdb.fdev.base.dict.Dict;
import com.spdb.fdev.base.dict.ErrorConstants;
import com.spdb.fdev.base.utils.CommonUtils;
import com.spdb.fdev.base.utils.MinioUtils;
import com.spdb.fdev.base.utils.ObjectUtil;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.pipeline.dao.IJobExeDao;
import com.spdb.fdev.pipeline.dao.IPipelineDao;
import com.spdb.fdev.pipeline.entity.*;
import com.spdb.fdev.pipeline.service.*;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RefreshScope
public class RunnerServiceImpl implements IRunnerService {

    @Autowired
    private IJobExeDao jobExeDao;

    @Autowired
    private IPipelineExeService pipelineExeService;

    @Autowired
    private IPipelineDao pipelineDao;

    @Autowired
    private IAppService appService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IPluginService pluginService;

    @Value("${spring.redis.expire-time-in-hours:10}")
    private long expireTimeInMinutes = 10L;

    @Value("${spring.profiles.active}")
    public String env;

    @Autowired
    private MailService mailService;

    @Autowired
    private IUserService userService;

    @Autowired
    private IRunnerInfoService runnerInfoService;

    @Autowired
    private IRunnerClusterService runnerClusterService;

    @Autowired
    private MinioUtils minioUtils;

    @Autowired
    private IDigitalService digitalService;

    @Value("${mioBuket}")
    private String mioBuket;

    private static final Logger logger = LoggerFactory.getLogger(RunnerServiceImpl.class);

    @Override
    public Map getJob(String token) throws Exception {
        RunnerInfo runnerInfo = this.runnerInfoService.getRunnerInfoByToken(token);
        //??????runnerInfo
        if (CommonUtils.isNullOrEmpty(runnerInfo)) {
            logger.error(">>>>>>>>>>>  runnerInfo is null <<<<<<<<<<<<");
            return null;
        }
        String runnerId = runnerInfo.getRunnerId();
        //??????runner??????????????????runner??????
        RunnerCluster runnerCluster = this.runnerClusterService.getRunnerClusterByRunner(runnerId);
        String runnerClusterId = runnerCluster.getRunnerClusterId();
        JobExe jobExe = getJobExeQueueItem(runnerClusterId);
        if (CommonUtils.isNullOrEmpty(jobExe)) {
            //??????job???204
            return null;
        }
        logger.info("************** pop jobExe from JobExeQueue :" + JSONObject.toJSONString(jobExe));
        //??????job????????? pipeline??????id ??????pipeline??????
//        JobExe jobExe = this.jobExeDao.queryJobExeByExeId("60500eb71f0cf60012fb4cff");
        PipelineExe pipelineExe = pipelineExeService.queryPipelineExeByExeId(jobExe.getPipelineExeId());
        logger.info("**************  query pipelineExe from DB, pipelineExe :" + JSONObject.toJSON(pipelineExe));
        if (CommonUtils.isNullOrEmpty(pipelineExe)) {
            return null;
        }
        if (Dict.CANCEL.equals(pipelineExe.getStatus())) {
            //????????????/???????????????????????????
            logger.info("************ pipeline already canceled, pipelineExeId:" + pipelineExe.getExeId());
            return null;
        }
        JobExe dataJobExe = this.jobExeDao.queryJobExeByExeId(jobExe.getExeId());
        if (Dict.CANCEL.equals(dataJobExe.getStatus())) {
            logger.info("************ pipeline already canceled, pipelineExeId:" + pipelineExe.getExeId());
            return null;
        }
        //job??????????????????running
        jobExe.setToken(token);
        jobExe.setStatus(Dict.RUNNING);

        Map info = CommonUtils.obj2Map(runnerInfo);
        info.remove(Dict.RUNNERID);
        info.remove(Dict.REVISION);
        info.remove(Dict.TOKEN);
        info.remove(Dict.STATUS);
        //????????????token?????????????????????
        jobExe.setInfo(info);
        jobExe.setJobStartTime(CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
        logger.info("************** jobExe update running, jobExe :" + JSONObject.toJSON(jobExe));
        jobExeDao.updateJobStart(jobExe);

        //?????????????????????????????????????????????????????????????????????running???stage????????????running???job??????????????????running
        Map stage = (Map) pipelineExe.getStages().get(jobExe.getStageIndex());
        stage.put(Dict.STATUS, Dict.RUNNING);
        List jobIds = (List) pipelineExe.getStages().get(jobExe.getStageIndex()).get(Dict.JOBS);
        Map jobIdInfo = (Map) jobIds.get(jobExe.getJobIndex());
        List<Map<String, Object>> jobExes = (List<Map<String, Object>>) jobIdInfo.get(Dict.JOBEXES);
        Map jobExeMap = jobExes.get(jobExes.size() - 1);
        jobExeMap.put(Dict.JOBEXESTATUS, Dict.RUNNING);
        jobExeMap.put(Dict.JOBSTARTTIME, jobExe.getJobStartTime());
        pipelineExe.setStatus(Dict.RUNNING);
        logger.info("************** update pipelineExe running, pipelineExe :" + JSONObject.toJSON(pipelineExe));
        pipelineExeService.updateStagesAndStatus(pipelineExe);

        //??????????????????
        Map result = new HashMap();
        result.put(Dict.ID, jobExe.getExeId());
        result.put(Dict.TOKEN, jobExe.getExeId());
        Map jobInfo = new HashMap();
        jobInfo.put(Dict.PIPELINE, pipelineExe.getExeId());
        jobInfo.put(Dict.PIPELINE_NUMBER, pipelineExe.getPipelineNumber());
        jobInfo.put(Dict.STAGE, jobExe.getStageName());
        jobInfo.put(Dict.STAGE_INDEX, jobExe.getStageIndex());
        jobInfo.put(Dict.JOB_INDEX, jobExe.getJobIndex());
        jobInfo.put(Dict.JOB_NUMBER, jobExe.getJobNumber());
        Map plugins = new HashMap();
        plugins.put(Dict.COUNT, jobExe.getSteps().size());
        jobInfo.put(Dict.PLUGINS, plugins);
        Map<String, String> image = jobExe.getImage();
        Map resImage = new HashMap();
        resImage.put(Dict.NAME, image.get(Dict.PATH));
        jobInfo.put(Dict.IMAGE, resImage);
        jobInfo.put(Dict.VARIABLES, pipelineExe.getVariables());
        jobInfo.put(Dict.VOLUMES, pipelineExe.getVolumes());
        jobInfo.put(Dict.TIMESTAMP, System.currentTimeMillis());
        jobInfo.put(Dict.PROJECT_ID, pipelineExe.getBindProject().get(Dict.GITLABPROJECTID));
        result.put(Dict.JOB_INFO, jobInfo);
        return result;
    }


    /**
     * ???????????????????????????plugin
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public Map getPlugins(Map<String, Object> param) throws Exception {
        String pipelineExeId = (String) param.get(Dict.PIPELINE);
        Integer stageIndex = (Integer) param.get(Dict.STAGE_INDEX);
        Integer jobIndex = (Integer) param.get(Dict.JOB_INDEX);
        Integer pluginIndex = (Integer) param.get(Dict.PLUGIN_INDEX);
        String jobToken = (String) param.get(Dict.JOB_TOKEN);
        JobExe jobExe = jobExeDao.queryJobExeByIndex(pipelineExeId, stageIndex, jobIndex);
        if (CommonUtils.isNullOrEmpty(jobExe.getSteps())) {
            logger.error("**************getPlugins, plugin not exist, jobexeId =" + jobExe.getExeId());
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST);
        }
        List<Map> steps = jobExe.getSteps();
        if (steps.size() <= pluginIndex) {
            logger.error("**************getPlugins, pluginIndex error, jobexeId =" + jobExe.getExeId());
            throw new FdevException(ErrorConstants.WRONG_PLUGIN_INDEX);
        }
        Map jobExeStep = steps.get(pluginIndex);
        Map plugin = null;
        if (CommonUtils.isNullOrEmpty(jobExeStep.get(Dict.PLUGININFO))) {
            logger.error("**************getPlugins, pluginInfo is null, jobexeId =" + jobExe.getExeId());
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST);
        } else {
            plugin = (Map) jobExeStep.get(Dict.PLUGININFO);
        }
        Map result = new HashMap();
        result.put(Dict.ID, 1);
        result.put(Dict.TOKEN, jobExe.getExeId());
        Map pluginInfo = new HashMap();
        pluginInfo.put(Dict.PLUGIN_NAME, plugin.get(Dict.NAME));
        Map execution = (Map) plugin.get(Dict.EXECUTION);
        execution.put(Dict.ARTIFACTS, plugin.get(Dict.ARTIFACTS));
        pluginInfo.put(Dict.EXECUTION, execution);
        //???????????????minio???md5
        String md5 = minioUtils.getMd5(mioBuket, (String) execution.get(Dict.PACKAGE_PATH));
        pluginInfo.put(Dict.PLUGIN_MD5, md5);
        //???redis???????????????????????????variables
        List variables = getPipelineCacheVariables(jobExe.getPipelineExeId());
        List<Map<String, String>> input = null;
        if (!CommonUtils.isNullOrEmpty(jobExeStep.get(Dict.INPUT))) {
            input = (List<Map<String, String>>) jobExeStep.get(Dict.INPUT);
        }

        jobExeStep.put(Dict.STEPSTARTTIME, CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
        logger.info("************** getPlugins, jobExe update stepStartTime, jobExe :" + JSON.toJSON(jobExe));
        jobExeDao.updateSteps(jobExe);

        //????????????????????????
        if (CommonUtils.isNullOrEmpty(variables)) {
            variables = new ArrayList();
        }
        if (CommonUtils.isNullOrEmpty(input)) {
            input = new ArrayList<>();
        } else if (!CommonUtils.isNullOrEmpty(variables) && !CommonUtils.isNullOrEmpty(input)) {
            //?????????????????????????????????
            //????????????????????????input???????????????????????????
            for (Object variable : variables) {
                Map vaMap = (Map) variable;
                String key = (String) vaMap.get(Dict.KEY);
                if (Dict.SKIP_MANUAL_REVIEW.equals(key)) {
                    //????????? input????????????SKIP_MANUAL_REVIEW??????
                    Iterator<Map<String, String>> iterator = input.iterator();
                    while (iterator.hasNext()) {
                        Map<String, String> inputMap = iterator.next();
                        String inputKey = (String) inputMap.get(Dict.KEY);
                        if (Dict.SKIP_MANUAL_REVIEW.equals(inputKey)) {
                            iterator.remove();
                        }
                    }
                }
            }
        }
        logger.info(" handler input is " + JSONObject.toJSONString(input) + " , handler variables is " + JSONObject.toJSONString(variables));
        variables.addAll(input);
        logger.info("return variables is " + JSONObject.toJSONString(variables));
        Map fdevUrlMap = new HashMap();
        fdevUrlMap.put(Dict.KEY, Dict.CI_FDEV_URL);
        String fdevUrl = "http://10.134.13.25:" + CommonUtils.getPort(env);
        fdevUrlMap.put(Dict.VALUE, fdevUrl);
        variables.add(fdevUrlMap);
        pluginInfo.put(Dict.PLUGIN_NAME, plugin.get(Dict.NAME));
        //????????????plugin??????
        Plugin queryPlugin = this.pluginService.queryPluginDetail((String) plugin.get(Dict.PLUGINCODE));
        //?????????????????????????????????script??????????????????????????????param
        pluginInfo.put(Dict.PLUGIN_VERSION, queryPlugin.getVersion());
//       pluginInfo.put(Dict.ARTIFACTS, plugin.get(Dict.ARTIFACTS));
        Author user = jobExe.getUser();
//       Map author = plugin.getAuthor();
        pluginInfo.put(Dict.PLUGIN_AUTHOR, queryPlugin.getAuthor().getNameEn());
        pluginInfo.put(Dict.VARIABLES, variables);
        result.put(Dict.PLUGIN_INFO, pluginInfo);
        return result;
    }

    @Override
    public Map getPluginInput(Map<String, Object> param) throws Exception {
        String pipelineExeId = (String) param.get(Dict.PIPELINE);
        Integer stageIndex = (Integer) param.get(Dict.STAGE_INDEX);
        Integer jobIndex = (Integer) param.get(Dict.JOB_INDEX);
        Integer pluginIndex = (Integer) param.get(Dict.PLUGIN_INDEX);
        String jobToken = (String) param.get(Dict.JOB_TOKEN);
        JobExe jobExe = jobExeDao.queryJobExeByIndex(pipelineExeId, stageIndex, jobIndex);
        List<Map> steps = jobExe.getSteps();
        if (steps.size() <= pluginIndex) {
            logger.error("*************pluginIndex error, jobexeId =" + jobExe.getExeId());
            throw new FdevException(ErrorConstants.WRONG_PLUGIN_INDEX);
        }
        Map jobExeStep = steps.get(pluginIndex);
        logger.info("*******getPluginInput, pluginIndex:" + pluginIndex + " jobExeStep:" + JSONObject.toJSONString(jobExeStep));
        Map result = new HashMap();
        List<Map> input = null;
        if (!CommonUtils.isNullOrEmpty(jobExeStep.get(Dict.INPUT))) {
            input = (List<Map>) jobExeStep.get(Dict.INPUT);
        }

        if (CommonUtils.isNullOrEmpty(input)) {
            return result;
        }
        return changeInput(input);
    }

    private Map changeInput(List<Map> input) throws Exception {
        Map result = new HashMap();
        for (Map map : input) {
            result.put(map.get(Dict.KEY), map.get(Dict.VALUE));
        }
        return result;
    }


    @Override
    public void getPluginOutput(Map<String, Object> param) throws Exception {
        String pipelineExeId = (String) param.get(Dict.PIPELINE);
        Integer stageIndex = (Integer) param.get(Dict.STAGE_INDEX);
        Integer jobIndex = (Integer) param.get(Dict.JOB_INDEX);
        Integer pluginIndex = (Integer) param.get(Dict.PLUGIN_INDEX);
        Map output = (Map) param.get(Dict.PLUGIN_OUTPUT);
        logger.info("************ plugin output " + JSONObject.toJSONString(output));
        String status = ((String) output.get(Dict.STATUS));
        String message = ((String) output.get(Dict.MESSAGE));
        Integer errcode = ((Integer) output.get(Dict.ERRORCODE));
        JobExe jobExe = jobExeDao.queryJobExeByIndex(pipelineExeId, stageIndex, jobIndex);
        //???jobExe?????????????????????output???data??????redis??????????????????pipelineExe?????????????????????????????????????????????????????????jobExe???output????????????
        if (Dict.CANCEL.equals(jobExe.getStatus())) {
            logger.info("************ job already canceled, pipelineExeId:" + pipelineExeId + ", jobExeId: " + jobExe.getExeId());
            return;
        }
        //1 ?????????????????????????????????data?????????data????????????redis?????????????????????pipeline??????
        Map data = ((Map) output.get(Dict.DATA));
        Map redisDataMap = new HashMap();
        logger.info("************ output data " + JSONObject.toJSONString(data));
        if (!CommonUtils.isNullOrEmpty(data)) {
            List oldRedisData = getPipelineCacheVariables(jobExe.getPipelineExeId());
            if (!CommonUtils.isNullOrEmpty(oldRedisData)) {
                redisDataMap = handlePipelineCacheVariables(oldRedisData);
            }
            redisDataMap.putAll(data);
            logger.info("************  output save redis ,redisdata map  " + JSONObject.toJSONString(redisDataMap));
            //??????????????????success??????data??????????????????Redis
            if (status.equals(Dict.SUCCESS)) {
                //??????data???resultRequest???????????????runner
                if (!CommonUtils.isNullOrEmpty(data.get(Dict.RESULTREQUEST)))
                    data.remove(Dict.RESULTREQUEST);
                setPipelineCacheVariables(jobExe.getPipelineExeId(), redisDataMap);
            }
        }
        //2 ??????jobexe?????????step??????????????????????????????
        List<Map> steps = jobExe.getSteps();
        if (steps.size() < pluginIndex + 1) {
            logger.error("************pluginIndex error, jobexeId =" + jobExe.getExeId());
            throw new FdevException(ErrorConstants.WRONG_PLUGIN_INDEX);
        }
        Map stepMap = steps.get(pluginIndex);
        if (status.equals(Dict.SUCCESS)) {
            stepMap.put(Dict.STATUS, Dict.SUCCESS);
        } else {
            stepMap.put(Dict.STATUS, Dict.ERROR);
        }
        stepMap.put(Dict.OUTPUT, redisDataMap);
        stepMap.put(Dict.STEPENDTIME, CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
        stepMap.put(Dict.STEPCOSTTIME, CommonUtils.getCostDate((String) stepMap.get(Dict.STEPSTARTTIME), (String) stepMap.get(Dict.STEPENDTIME)
                , CommonUtils.STANDARDDATEPATTERN));

        logger.info("********** getPluginOutput jobExe update step status:[" + status + "], jobExe: " + JSONObject.toJSON(jobExe));
        jobExeDao.updateSteps(jobExe);
    }

    @Override
    public void jobWebHook(Map<String, Object> param) throws Exception {
        logger.info("**********  jobwebhook request param: " + JSONObject.toJSONString(param));
        Map info = (Map) param.get(Dict.INFO);
        String token = (String) param.get(Dict.TOKEN);
        Map jobInfo = (Map) param.get(Dict.JOB_INFO);
        Map resultData = (Map) param.get(Dict.RESULT_DATA);
        String status = (String) resultData.get(Dict.STATUS);
        String code = (String) resultData.get(Dict.CODE);
        String message = (String) resultData.get(Dict.MESSAGE);
        String minioLogUrl = (String) resultData.get(Dict.MINIO_LOG_URL);
        String pipelineExeId = (String) jobInfo.get(Dict.PIPELINE);
        Integer stageIndex = (Integer) jobInfo.get(Dict.STAGE_INDEX);
        Integer jobIndex = (Integer) jobInfo.get(Dict.JOB_INDEX);

        //??????pipeline????????????????????????????????????????????????????????????job???????????????????????????return
        PipelineExe pipelineExe = pipelineExeService.queryPipelineExeByExeId(pipelineExeId);
        if (pipelineExe.getStatus().equals(Dict.CANCEL)) {
            logger.info("************ pipeline already canceled, pipelineExeId:" + pipelineExeId);
            return;
        }

        logger.info("********** jobWebHook status " + status);
        if (!status.equals(Dict.SUCCESS) && !status.equals(Dict.ERROR)) {
            return;
        }

        //1?????????jobExe
        JobExe jobExe = jobExeDao.queryJobExeByIndex(pipelineExeId, stageIndex, jobIndex);
        if (jobExe.getStatus().equals(Dict.SUCCESS) || jobExe.getStatus().equals(Dict.ERROR) || jobExe.getStatus().equals(Dict.CANCEL)) {
            logger.info("**************** jobexe already finished, status " + jobExe.getStatus());
            return;
        }
        jobExe.setJobEndTime(CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
        jobExe.setJobCostTime(CommonUtils.getCostDate(jobExe.getJobStartTime(), jobExe.getJobEndTime(), CommonUtils.STANDARDDATEPATTERN));
        jobExe.setStatus(status);
        jobExe.setMinioLogUrl(minioLogUrl);
        logger.info("***********  jobExe result " + status + ", update DB, jobExe :" + JSONObject.toJSON(jobExe));
        jobExeDao.updateJobFinish(jobExe);

        //2?????????piperlineExe????????????
        //??????jobExes??????
        List<Map> stageJobs = (List<Map>) pipelineExe.getStages().get(stageIndex).get(Dict.JOBS);
        List<Map> jobExes = (List<Map>) stageJobs.get(jobIndex).get(Dict.JOBEXES);
        Map map = jobExes.get(jobExes.size() - 1);
        map.put(Dict.JOBEXESTATUS, status);
        map.put(Dict.JOBENDTIME, jobExe.getJobEndTime());
        map.put(Dict.JOBCOSTTIME, jobExe.getJobCostTime());

        //?????????????????????job??????
        List<Map> jobQueueList = new ArrayList();
        //??????stage???????????????stage??????
        String stageStatus = pipelineExeService.getStageFinalStatus(pipelineExe, stageIndex);
        logger.info("************ pipelineExeId " + pipelineExe.getExeId() + ", stage[" + stageIndex + "] status???" + stageStatus);
        if (Dict.ERROR.equals(stageStatus) || Dict.SUCCESS.equals(stageStatus) || Dict.CANCEL.equals(stageStatus)) {
            Map stageExe = pipelineExe.getStages().get(stageIndex);
            stageExe.put(Dict.STATUS, stageStatus);
            if (pipelineExe.getStages().size() == (stageIndex + 1) || Dict.ERROR.equals(stageStatus) || Dict.CANCEL.equals(stageStatus)) {
                //???????????????????????????????????????????????????????????????????????????????????????
                // ?????????????????????????????????????????????????????????????????????????????????????????????piperlineExe
                logger.info("*************** pipelineExeId " + pipelineExe.getExeId() + " finished, status???" + stageStatus);
                pipelineExe.setStatus(stageStatus);
                if (CommonUtils.isNullOrEmpty(pipelineExe.getEndTime())) {
                    pipelineExe.setEndTime(CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
                    pipelineExe.setCostTime(pipelineExeService.calculatePipelineExeCostTime(pipelineExe));
                }
                // ??????????????????????????????stage???????????????
                if (Dict.ERROR.equals(stageStatus) && pipelineExe.getStages().size() != (stageIndex + 1)) {
                    List<Map<String, Object>> stages = pipelineExe.getStages();
                    for (int i = stageIndex + 1; i < stages.size(); i++) {
                        Map<String, Object> stage = stages.get(i);
                        List<Map> jobs = (List<Map>) stage.get(Dict.JOBS);
                        for (Map job : jobs) {
                            List<Map> jobExesSkip = (List<Map>) job.get(Dict.JOBEXES);
                            Map jobExeMap = jobExesSkip.get(jobExesSkip.size() - 1);
                            String jobExeId = (String) jobExeMap.get(Dict.JOBEXEID);
                            String jobExeStatus = (String) jobExeMap.get(Dict.JOBEXESTATUS);
                            if (Dict.WAITING.equals(jobExeStatus)) {
                                //2 ??????jobexe
                                JobExe jobExeSkip = this.jobExeDao.queryJobExeByExeId(jobExeId);
                                jobExeSkip.setStatus(Dict.SKIP);
                                jobExeSkip.setMinioLogUrl("");
                                this.jobExeDao.updateJobFinish(jobExeSkip);

                                //3 ??????pipelineExe???running??????pending???jobExeMap
                                jobExeMap.put(Dict.JOBEXESTATUS, jobExeSkip.getStatus());
                            }
                        }
                        stage.put(Dict.STATUS, Dict.SKIP);
                    }
                }
            } else {
                //????????????????????????????????????????????????????????????????????????job??????????????????pipeline??????
                if (!CommonUtils.isNullOrEmpty(pipelineExe.getEndTime())) {
                    String pipelineStatus = getPipelineFinalStatus(pipelineExe);
                    pipelineExe.setStatus(pipelineStatus);
                } else {
                    //?????????????????????????????????stage???job????????????pending???????????????jobExe
                    Map nextStageExe = pipelineExe.getStages().get(stageIndex + 1);
                    nextStageExe.put(Dict.STATUS, Dict.PENDING);
                    logger.info("*************** pipelineExeId " + pipelineExe.getExeId() +
                            " nextStage [+stageIndex + 1+]" +
                            " is " + JSONObject.toJSONString(nextStageExe));
                    List<Map> nextJobList = (List<Map>) nextStageExe.get(Dict.JOBS);
                    Pipeline pipeline = this.pipelineDao.queryById(pipelineExe.getPipelineId());
                    List<Job> jobList = pipeline.getStages().get(stageIndex + 1).getJobs();
                    int jobListIndex = 0;
                    //??????stage???jobs??????jobs????????????job?????????????????????jobExeStatus??????pending
                    for (Map job : nextJobList) {
                        logger.info(">>>>>>>>>> pipeline job is " + JSONObject.toJSONString(job) + " <<<<<<<<<<<");
                        String runnerClusterId = (String) job.get(Dict.RUNNERCLUSTERID);
                        if (CommonUtils.isNullOrEmpty(runnerClusterId)) {
                            logger.warn(">>>>>>>>>>>>  runnerClusterId is null <<<<<<<<<<<<<<<<");
                        }
                        List<Map> jobExelist = (List<Map>) job.get(Dict.JOBEXES);
                        Map jobExeInfo = jobExelist.get(jobExelist.size() - 1);
                        jobExeInfo.put(Dict.JOBEXESTATUS, Dict.PENDING);
                        //??????jobExe???pending
                        String jobExeId = (String) jobExeInfo.get(Dict.JOBEXEID);
                        jobExeDao.updateStatusByExeId(jobExeId, Dict.PENDING);
                        logger.info(" ***************  update jobExe " + jobExeId + " status to pending ");
                        //jobExe????????????
                        JobExe queryJobExe = jobExeDao.queryJobExeByExeId(jobExeId);
                        Map jobMap = new HashMap();
                        jobMap.put(Dict.JOB_EXE, queryJobExe);
                        jobMap.put(Dict.RUNNERCLUSTERID, runnerClusterId);
                        jobQueueList.add(jobMap);
                        jobListIndex++;
                    }
                }
            }
        }
        this.pipelineExeService.updateStagesAndStatus(pipelineExe);

        //5???jobexe????????????
        for (Map jobMap : jobQueueList) {
            JobExe queryJobExe = (JobExe) jobMap.get(Dict.JOB_EXE);
            String runnerClusterId = (String) jobMap.get(Dict.RUNNERCLUSTERID);
            addJobExeQueue(queryJobExe, runnerClusterId);
        }
        //??????????????????
        String userNameEn = pipelineExe.getUser().getNameEn();
        //????????????
        if (Dict.ERROR.equals(pipelineExe.getStatus()) && !Dict.SYSTEM.equals(userNameEn)) {
            String userId = pipelineExe.getUser().getId();
            Map userInfo = this.userService.queryUserById(userId);
            String email = (String) userInfo.get("email");
            sendMailWhenPipelineComplete(pipelineExe, email);
        }
        /**************************************************************/
        //????????????????????????exeTotal ????????????????????????successExeTotal ?????????????????????errorExeTotal ?????????????????????successRate ????????????aveErrorTime ????????????????????????
        this.digitalService.calculateDigital(pipelineExe.getPipelineNameId());
    }

    /**
     * ??????stage??????????????????pipeline???????????????
     *
     * @param pipelineExe
     * @return status ??????success??????error
     */
    public String getPipelineFinalStatus(PipelineExe pipelineExe) {
        String status = Dict.SUCCESS;
        for (Map stage : pipelineExe.getStages()) {
            String stageStatus = (String) stage.get(Dict.STATUS);
            if (Dict.RUNNING.equals(stageStatus)) {
                status = Dict.RUNNING;
                break;
            } else if (Dict.CANCEL.equals(stageStatus)) {
                status = Dict.CANCEL;
                break;
            } else if (Dict.ERROR.equals(stageStatus)) {
                status = Dict.ERROR;
                break;
            }
        }
        logger.info("*************** pipelineExeId " + pipelineExe.getExeId() + ", status???" + status);
        return status;
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param pipelineExe
     * @throws Exception
     */
    private void sendMailWhenPipelineComplete(PipelineExe pipelineExe, String email) throws Exception {
        String userEn = pipelineExe.getUser().getNameEn();
        logger.info("*************** pipeline  result notify????????????email :" + email);
        List<String> emails = new ArrayList();
        emails.add(email);
        HashMap model = new HashMap<>();
        List errorList = new ArrayList();
        String errorStage = "";
        if (!Dict.ERROR.equals(pipelineExe.getStatus())) {
            logger.info("*********** pipelineExe status is not fail");
            return;
        }
        String errorJob = "";
        Integer stageIndex = null;
        Integer jobIndex = null;
        for (int j = 0; j < pipelineExe.getStages().size(); j++) {
            Map<String, Object> stage = pipelineExe.getStages().get(j);
            String stageStatus = (String) stage.get(Dict.STATUS);
            if (Dict.ERROR.equals(stageStatus)) {
                errorStage = (String) stage.get(Dict.NAME);
                List<Map> jobs = (List<Map>) stage.get(Dict.JOBS);
                for (int i = 0; i < jobs.size(); i++) {
                    Map job = jobs.get(i);
                    List<Map> jobExes = (List<Map>) job.get(Dict.JOBEXES);
                    Map jobExe = jobExes.get(jobExes.size() - 1);
                    String jobExeStatus = (String) jobExe.get(Dict.JOBEXESTATUS);
                    if (Dict.ERROR.equals(jobExeStatus)) {
                        errorJob = (String) job.get(Dict.NAME);
                        jobIndex = i;
                        stageIndex = j;
                        break;
                    }
                }
                break;
            }
        }

        String pipelineName = pipelineExe.getPipelineName();
        model.put("stageName", errorStage);
        model.put("jobName", errorJob);
        model.put("user_name", userEn);
        model.put(Dict.PIPELINENAME, pipelineName);
        model.put(Dict.ID, pipelineExe.getExeId());
        if (CommonUtils.isNullOrEmpty(errorStage)) {
            logger.info("**********  error stage is empty,no send????????? ");
            return;
        }
        String localEnv = "[" + env + "]";
        mailService.sendEmail(localEnv + "????????????????????????fdev-pipeline????????????", "pipeline_error", model, emails.toArray(new String[emails.size()]));
    }

    @Override
    public void artifactsWebhook(Map<String, Object> param) throws Exception {
        String pipelineExeId = (String) param.get(Dict.PIPELINE);
        PipelineExe pipelineExe = pipelineExeService.queryPipelineExeByExeId(pipelineExeId);
        List<Map> artifacts = pipelineExe.getArtifacts();
        //??????pipeline????????????cancel????????????return
        if (pipelineExe.getStatus().equals(Dict.CANCEL)) {
            logger.info("************ pipeline already canceled, pipelineExeId:" + pipelineExe.getExeId());
            return;
        }
        //?????????????????????jobExe cancel ??????return
        Integer stageIndex = (Integer) param.get(Dict.STAGE_INDEX);
        Integer jobIndex = (Integer) param.get(Dict.JOB_INDEX);
        Integer pluginIndex = (Integer) param.get(Dict.PLUGIN_INDEX);
        JobExe jobExe = this.jobExeDao.queryJobExeByIndex(pipelineExeId, stageIndex, jobIndex);
        if (Dict.CANCEL.equals(jobExe.getStatus())) {
            logger.info("************ job already canceled, jobExeId:" + jobExe.getExeId());
            return;
        }
        //??????????????????????????????????????????object_name????????????artifacts?????????
        String objectName = (String) param.get(Dict.OBJECT_NAME);
        if (CommonUtils.isNullOrEmpty(objectName) && CommonUtils.isNullOrEmpty(artifacts)) {
            artifacts = new ArrayList<>();
        } else if (!CommonUtils.isNullOrEmpty(artifacts)) {
            //???object_name???????????????artifacts?????????????????????artifacts????????????????????????????????????
            List tempList = new ArrayList();
            Integer replaceIndex = null;
            for (int i = 0; i < artifacts.size(); i++) {
                Map artifact = artifacts.get(i);
                if (artifact.get(Dict.STAGE_INDEX) == stageIndex && artifact.get(Dict.JOB_INDEX) == jobIndex && artifact.get(Dict.PLUGIN_INDEX) == pluginIndex) {
                    replaceIndex = i;
                    break;
                }
            }
            if (!CommonUtils.isNullOrEmpty(replaceIndex)) {
                //???????????????????????????????????????????????????
                if (!CommonUtils.isNullOrEmpty(objectName)) {
                    //???objectName????????????????????????
                    for (int i = 0; i < artifacts.size(); i++) {
                        if (i == replaceIndex) {
                            tempList.add(param);
                        } else tempList.add(artifacts.get(i));
                    }
                    artifacts = tempList;
                } else {
                    //objectName??????????????????artifacts????????????
                    artifacts.remove(replaceIndex);
                }
            } else {
                logger.info("************** artifacts is not exists objectName ");
                if (!CommonUtils.isNullOrEmpty(objectName)) {
                    artifacts.add(param);
                }
            }
        } else {
            artifacts = new ArrayList<>();
            artifacts.add(param);
        }
        pipelineExe.setArtifacts(artifacts);
        pipelineExeService.updateArtifacts(pipelineExe);
    }

    @Override
    public List<Map> getArtifacts(Map<String, Object> param) throws Exception {
        String pipelineExeId = (String) param.get(Dict.PIPELINE);
        PipelineExe pipelineExe = this.pipelineExeService.queryPipelineExeByExeId(pipelineExeId);
        List artifacts = null;
        if (!CommonUtils.isNullOrEmpty(pipelineExe.getArtifacts()))
            artifacts = pipelineExe.getArtifacts();
        else
            artifacts = new ArrayList();
        return artifacts;
    }

    @Override
    public void jobClear(String runnerClusterId) throws Exception {
        getJobExeQueueItem(runnerClusterId);
    }

    @Override
    public void setJobLog(String output, String token, String range) throws Exception {
        String cacheKey = Constants.PIPELINE_JOE_EXE_LOG_KEY_PROFIX + token;
        redisTemplate.opsForHash().put(cacheKey, range, output);
        //Map entries = redisTemplate.opsForHash().entries(cacheKey);
    }

    private List getPipelineCacheVariables(String pipelineExeId) throws Exception {
        String cacheKey = Constants.PIPELINE_EXE_REDIS_KEY_PROFIX + pipelineExeId;
        return (List) redisTemplate.opsForValue().get(cacheKey);
    }

    private void addJobExeQueue(JobExe jobExe, String runnerClusterId) throws Exception {
        ByteArrayOutputStream bio = ObjectUtil.object2Bytes(jobExe);
        logger.info("**************   save redis data :" + bio.toByteArray());
        redisTemplate.opsForList().rightPush(Constants.JOB_EXE_QUEUE_REDIS_KEY_PROFIX + runnerClusterId, bio.toByteArray());
    }

    private JobExe getJobExeQueueItem(String runnerClusterId) throws Exception {

        Object o = redisTemplate.opsForList().leftPop(Constants.JOB_EXE_QUEUE_REDIS_KEY_PROFIX + runnerClusterId);
        logger.info("**************  redis get object:" + o);
        String value = (String) o;
        if (CommonUtils.isNullOrEmpty(value)) {
            return null;
        }
        return (JobExe) ObjectUtil.bytes2Object(Base64.decode(value), JobExe.class);
    }

    private void setPipelineCacheVariables(String pipelineExeId, Map data) throws Exception {
        List variables = new ArrayList();
        if (!CommonUtils.isNullOrEmpty(data)) {
            Set<String> set = data.keySet();
            for (String key : set) {
                Map item = new HashMap();
                item.put(Dict.KEY, key);
                Map map = (Map) data.get(key);
                item.put(Dict.VALUE, map.get(Dict.VALUE));
                variables.add(item);
            }
            String cacheKey = Constants.PIPELINE_EXE_REDIS_KEY_PROFIX + pipelineExeId;
            redisTemplate.opsForValue().set(cacheKey, variables, this.expireTimeInMinutes, TimeUnit.HOURS);
        }
    }


    /**
     * ?????????redis????????????data???????????????????????????????????????
     *
     * @param outputData
     * @return
     */
    private Map handlePipelineCacheVariables(List<Map> outputData) {
//        [{"value":"10.144.38.56/ebank-service/nbh-product-hxy:2021-01-28_18-12-27","key":"ci_image"}]
        Map resultData = new HashMap();
        for (Map outputMap : outputData) {
            Map value = new HashMap();
            value.put(Dict.TYPE, "string");
            value.put(Dict.VALUE, outputMap.get(Dict.VALUE));
            String key = (String) outputMap.get(Dict.KEY);
            resultData.put(key, value);
        }
        return resultData;
    }

}
