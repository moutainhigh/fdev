package com.spdb.fdev.pipeline.web;

import com.spdb.fdev.base.dict.Constants;
import com.spdb.fdev.base.dict.Dict;
import com.spdb.fdev.base.dict.ErrorConstants;
import com.spdb.fdev.base.utils.CommonUtils;
import com.spdb.fdev.base.utils.VaildateUtil;
import com.spdb.fdev.common.JsonResult;
import com.spdb.fdev.common.User;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.common.util.JsonResultUtil;
import com.spdb.fdev.common.validate.RequestValidate;
import com.spdb.fdev.pipeline.dao.IJobExeDao;
import com.spdb.fdev.pipeline.dao.IPluginDao;
import com.spdb.fdev.pipeline.entity.*;
import com.spdb.fdev.pipeline.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RequestMapping("api/pipelineLog")
@RestController
public class PipelineLogController {

    @Autowired
    IAppService appService;
    @Autowired
    IPipelineExeService pipelineExeService;
    @Autowired
    IJobExeService jobExeService;
    @Autowired
    IFileService fileService;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    VaildateUtil vaildateUtil;
    @Autowired
    IJobExeService iJobExeService;
    @Autowired
    private IUserService userService;

    @Autowired
    IPluginService iPluginService;

    @Autowired
    IJobExeDao iJobExeDao;

    @Autowired
    IPluginDao iPluginDao;

    @Autowired
    private IPipelineService pipelineService;

    private static final Logger logger = LoggerFactory.getLogger(PipelineLogController.class);

    @RequestValidate(NotEmptyFields = {Dict.PAGE_NUM, Dict.PAGE_SIZE})
    @PostMapping(value = "/queryFdevciLogList")
    public JsonResult queryFdevciLogList(@RequestBody Map<String, String> requestParam) throws Exception {
        String pipelineId = requestParam.get(Dict.PIPELINEID);
        String branch = requestParam.get(Dict.BRANCH);
        String commitId = requestParam.get(Dict.COMMITID);
        String searchContent = requestParam.get(Dict.SEARCHCONTENT);
        String pageNum = requestParam.get(Dict.PAGE_NUM);
        String pageSize = requestParam.get(Dict.PAGE_SIZE);
        User user = userService.getUserFromRedis();
        Map<String, Object> map;
        if (!CommonUtils.isNullOrEmpty(pipelineId)) {
            /*???????????????ID???????????????*/
            map = pipelineExeService.queryListByPipelineIdSort(pipelineId, pageNum, pageSize);
        } else {
            /*??????commitid branch serchContent ???????????????*/
            map = pipelineExeService.queryListRegexSort(commitId, branch, searchContent, pageNum, pageSize);
        }
        map = preparePipelineExe(map);
        return JsonResultUtil.buildSuccess(map);
    }

    /**
     * ??????pipelineExe,??????????????????????????????
     */
    private Map<String, Object> preparePipelineExe(Map<String, Object> map) throws Exception {
        User user = userService.getUserFromRedis();
        if (!CommonUtils.isNullOrEmpty(map)) {
            Object pipelineExeList = map.get(Dict.PIPELINEEXELIST);
            if (!CommonUtils.isNullOrEmpty(pipelineExeList)) {
                List<Map<String,Object>> list = (List<Map<String,Object>>) pipelineExeList;
                if (!CommonUtils.isNullOrEmpty(list)) {
                    boolean flag = vaildateUtil.projectCondition(String.valueOf(
                            ((Map) list.get(0)
                                    .get("bindProject"))
                                    .get(Dict.PROJECTID)));
                    for (Map<String,Object> pipelineExe : list) {
                        // ??????????????????????????????????????????
                        if (user.getUser_name_en().equals(((Map) pipelineExe.get("user")).get("nameEn")) || flag) {
                            pipelineExe.put("retry", Constants.ONE);
                        } else {
                            pipelineExe.put("retry", Constants.ZERO);
                        }
                    }
                }
            }
        }
        return map;
    }

    @RequestValidate(NotEmptyFields = {Dict.PIPELINEEXEID})
    @PostMapping(value = "/queryPipelineDetail")
    public JsonResult queryPipelineDetail(@RequestBody Map<String, String> requestParam) throws Exception {
        String pipelineExeId = requestParam.get(Dict.PIPELINEEXEID);
        PipelineExe pipelineExe = pipelineExeService.queryPipelineExeByExeId(pipelineExeId);
        if (pipelineExe == null){
            logger.error("********************queryPipelineDetail  can not find pipelineExe;pipelineExeId:"+pipelineExeId);
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST);
        }
        User user = userService.getUserFromRedis();
        Map bindProject = pipelineExe.getBindProject();
        String projectID= (String) bindProject.get(Dict.PROJECTID);
        String retry = getPipelineRetryFlag(pipelineExe, user, projectID);
        //?????????????????????????????????
        List<Map> artifacts = pipelineExe.getArtifacts();
        Map<String,Map> artifactsMap = new HashMap<>();
        if (!CommonUtils.isNullOrEmpty(artifacts)){
            //???????????????key?????????map
            for (Map map : artifacts){
                if(!CommonUtils.isNullOrEmpty(map)){
                    String key = new StringBuffer().append(map.get(Dict.STAGE_INDEX)).append("_")
                            .append(map.get(Dict.JOB_INDEX)).append("_")
                            .append(map.get(Dict.PLUGIN_INDEX)).toString();
                    artifactsMap.put(key,map);
                }
            }
        }
        pipelineExe.setRetry(retry);
        //?????????????????????????????????????????????
        List<Map<String, Object>> stages = pipelineExe.getStages();
        for (int i = 0; i < stages.size(); i++) {
            Map<String, Object> stage = stages.get(i);
            List<Map> jobs = (List<Map>) stage.get(Dict.JOBS);
            for (int n = 0; n < jobs.size(); n++) {
                List<Map> stepsResultInfo = new ArrayList<>();
                List<Map> jobExes = (List<Map>) jobs.get(n).get(Dict.JOBEXES);
                Map lastJobMap = jobExes.get(jobExes.size() - 1);
                String jobExeId = (String) lastJobMap.get(Dict.JOBEXEID);
                JobExe jobExe = iJobExeDao.queryJobExeByExeId(jobExeId);
                if(CommonUtils.isNullOrEmpty(jobExe)) {
                    //???????????????trigger???????????????????????????????????????jobExe?????????????????????
                    continue;
                }
                List<Map> steps = jobExe.getSteps();
                for (int j=0 ; j< steps.size(); j++){
                    Map step = steps.get(j);
                    Map stepResultMap = new HashMap<>();
                    Object output = step.get(Dict.OUTPUT);
                    Map pluginInfo = (Map)step.get(Dict.PLUGININFO);
                    String pluginCode = (String) pluginInfo.get(Dict.PLUGINCODE);
                    stepResultMap.put(Dict.PLUGINCODE,pluginCode);
                    Plugin plugin = iPluginDao.queryPluginDetail(pluginCode);
                    if (CommonUtils.isNullOrEmpty(plugin) || plugin == null){
                        logger.error("**********plugin not exist, pluginId="+pluginCode);
                        throw new FdevException(ErrorConstants.DATA_NOT_EXIST,new String[]{"plugin not exist"+pluginCode});
                    }else {
                        stepResultMap.put(Dict.PLUGINNAME,plugin.getPluginName());
                        stepResultMap.put(Dict.CATEGORYNAME,plugin.getCategory().getCategoryName());
                        stepResultMap.put(Dict.NAMEID,plugin.getNameId());
                        stepResultMap.put(Dict.STEPINDEX,j);
                        //?????????????????????
                        String key = new StringBuffer().append(i).append("_")
                                .append(n).append("_")
                                .append((j)).toString();
                        if (artifactsMap.containsKey(key)){
                            Map map = artifactsMap.get(key);
                            String obName = (String) map.get(Dict.OBJECT_NAME);
                            if (obName.startsWith("fdevtest-generic-local") || obName.startsWith("fdev-generic-local")){
                                stepResultMap.put(Dict.ARTIFACTSFLAG, true);
                            }else {
                                stepResultMap.put(Dict.ARTIFACTSFLAG, false);
                            }
                        }else {
                            stepResultMap.put(Dict.ARTIFACTSFLAG, false);
                        }
                        //?????????????????????
                        if (plugin.getResultDisplayFlag()  && !CommonUtils.isNullOrEmpty(output)){
                            stepResultMap.put(Dict.RESULTDISPLAYFLAG,true);
                        }else {
                            stepResultMap.put(Dict.RESULTDISPLAYFLAG,false);
                        }
                    }
                    stepsResultInfo.add(stepResultMap);
                }
                jobs.get(n).put(Dict.STEPSRESULTINFO,stepsResultInfo);
            }
        }
        return JsonResultUtil.buildSuccess(pipelineExe);
    }

    /**
     * ???????????????????????????????????????
     * @param pipelineExe
     * @param user ????????????
     * @param projectID
     * @return 0-?????????  1-??????
     * @throws Exception
     */
    private String getPipelineRetryFlag(PipelineExe pipelineExe, User user, String projectID) throws Exception {
        String retry = Constants.ONE; //????????????
        boolean appManagerFlag = vaildateUtil.projectCondition(projectID);
        // ??????????????????????????????????????????
        if(user.getUser_name_en().equals(pipelineExe.getUser().getNameEn()) || appManagerFlag) {
        } else {
            retry = Constants.ZERO;
        }
        //???????????????????????????????????????
        if(!Dict.ERROR.equals(pipelineExe.getStatus()) && !Dict.SUCCESS.equals(pipelineExe.getStatus())){
            retry = Constants.ZERO;
        }
        return retry;
    }

    @RequestValidate(NotEmptyFields = {Dict.JOBEXEID})
    @PostMapping(value = "/queryLogDetailById")
    public JsonResult queryLogDetailById(@RequestBody Map<String, String> requestParam) throws Exception {
        String jobExeId = requestParam.get(Dict.JOBEXEID);
        JobExe jobExe = jobExeService.queryJobExeByExeId(jobExeId);
        PipelineExe pipelineExe = pipelineExeService.queryPipelineExeByExeId(jobExe.getPipelineExeId());
        String status = jobExe.getStatus();
        String logContent = "";
        if (!CommonUtils.isNullOrEmpty(jobExe.getMinioLogUrl())) {
            if(Dict.SUCCESS.equals(status) || Dict.ERROR.equals(status)) {
                String logPath = jobExe.getMinioLogUrl();
                String[] pathArray = logPath.split("/minio/");
                if(pathArray.length > 1) {
                    String[] minioArr = pathArray[1].split("/");
                    String bucket = minioArr[0];
                    StringBuilder minioPath = new StringBuilder();
                    for(int i = 1;i < minioArr.length;i ++) {
                        minioPath.append("/").append(minioArr[i]);
                    }
                    try {
                        logContent = fileService.downloadDocumentFile(bucket, minioPath.toString());
                    } catch (Exception e) {
                        logContent = getRedisLog(jobExe);
                    }
                } else {
                    logger.error("*************minio address  error", logPath);
                    logContent = getRedisLog(jobExe);
                }
            }
        }else if(Dict.RUNNING.equals(status)) {
            logContent = getRedisLog(jobExe);
        }else if (!Dict.RUNNING.equals(status)) {
                logContent = "??????job?????????";
        }
        Map<String, Object> returnMap = new HashMap<>();
        Map info = jobExe.getInfo();
        String executorId =null;
        if (!CommonUtils.isNullOrEmpty(info)){
            executorId = (String) info.get(Dict.EXECUTOR);
        }
        returnMap.put(Dict.EXECUTORID,executorId);
        returnMap.put(Dict.LOG, logContent);
        returnMap.put(Dict.JOBEXEID, jobExe.getExeId());
        returnMap.put(Dict.JOBNUMBER, jobExe.getJobNumber());
        returnMap.put(Dict.STATUS, jobExe.getStatus());
        returnMap.put(Dict.JOBNAME, jobExe.getJobName());
        returnMap.put(Dict.JOBSTARTTIME, jobExe.getJobStartTime());
        returnMap.put(Dict.JOBENDTIME, jobExe.getJobEndTime());
        returnMap.put(Dict.JOBCOSTTIME, jobExe.getJobCostTime());
        returnMap.put(Dict.STAGEINDEX,jobExe.getStageIndex());
        returnMap.put(Dict.JOBINDEX,jobExe.getJobIndex());
        returnMap.put(Dict.USER, pipelineExe.getUser().getNameEn());
        returnMap.put(Dict.BRANCH, pipelineExe.getBranch());
        returnMap.put(Dict.STAGE, pipelineExe.getStages());
        //????????????????????????
        User user = userService.getUserFromRedis();
        Map bindProject = pipelineExe.getBindProject();
        String projectID= String.valueOf(bindProject.get(Dict.PROJECTID));
        String retry = getPipelineRetryFlag(pipelineExe, user, projectID);
        returnMap.put(Dict.STAGE_RETRYJOB, retry);
        return JsonResultUtil.buildSuccess(returnMap);
    }

    private String getRedisLog(JobExe jobExe) {
        String cacheKey = Constants.PIPELINE_JOE_EXE_LOG_KEY_PROFIX + jobExe.getExeId();
        Map<String, Object> logMap = redisTemplate.opsForHash().entries(cacheKey);
        List<String> keys = new ArrayList<>(logMap.keySet());
        Collections.sort(keys, (o1, o2) -> {
            // key????????????1-49????????????????????????
            Integer a = Integer.valueOf(o1.split("-")[0]);
            Integer b = Integer.valueOf(o2.split("-")[0]);
            if(a > b) {
                return 1;
            } else if (a < b) {
                return -1;
            } else {
                return 0;
            }
        });
        StringBuilder sb = new StringBuilder();
        for(String key : keys) {
            sb.append(logMap.get(key));
        }
        return sb.toString();
    }
    //??????commitId???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
    @RequestMapping(value = "/getCodeScanOutput",method = RequestMethod.POST)
//    @RequestValidate(NotEmptyFields = {Dict.PROJECTID,Dict.COMMITID})
    public JsonResult getCodeScanOutput(@RequestBody Map requestParam) throws Exception{
        String pipleNumber = (String) requestParam.get(Dict.PIPELINENUMBER);
        String commitId = (String) requestParam.get(Dict.COMMITID);
        String nameEn = (String) requestParam.get(Dict.NAMEEN);
        if (CommonUtils.isNullOrEmpty(pipleNumber)&&CommonUtils.isNullOrEmpty(commitId))
        {
            logger.error("**********pelease input pipelineNumber or  commitId");
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY,new String[]{"pipelineNumber or  commitId empty"});
        }
        List<PipelineExe> pipelineExeList = pipelineExeService.queryExeByPipeLineNumberOrCommitId(pipleNumber, commitId);
        List<Map> resultList = new ArrayList<>();
        if (!CommonUtils.isNullOrEmpty(pipelineExeList)) {
//            PipelineExe pipelineExe = pipelineExeList.get(0);
            for (PipelineExe pipelineExe : pipelineExeList) {
                //???????????????????????????????????????,???????????????????????????????????????????????????????????????
                Map result_map = new LinkedHashMap();
                List outputList = new ArrayList();
                //????????????????????????????????????????????????????????????
                String exeId = pipelineExe.getExeId();
                String status = pipelineExe.getStatus();
                String branch = pipelineExe.getBranch();
                String commitTitle = pipelineExe.getCommitTitle();
                Author author = pipelineExe.getUser();
                String startTIme = pipelineExe.getStartTime();
                result_map.put(Dict.EXEID, exeId);
                result_map.put(Dict.BRANCH, branch);
                result_map.put(Dict.STARTTIME, startTIme);
                result_map.put(Dict.COMMITTITLE, commitTitle);
                result_map.put(Dict.AUTHOR, author);
                result_map.put(Dict.STATUS, status);
                List<Map<String, Object>> stages = pipelineExe.getStages();
                //??????????????????id
                List<String> jobExeIds = new ArrayList<>();
                for (Map stage : stages) {
                    List<Map> Jobs = (List) stage.get(Dict.JOBS);

                    for (Map job : Jobs) {
                        List<Map> JobExes = (List) job.get(Dict.JOBEXES);
                        //???????????????????????????????????????????????????????????????
                        Map jobexe = JobExes.get(JobExes.size() - 1);
                        String jobExeId = (String) jobexe.get(Dict.JOBEXEID);
                        //??????jobexes?????????????????????
                        JobExe jobExeDetail = jobExeService.queryJobExeByExeId(jobExeId);
                        //????????????
                        Integer stageIndex = jobExeDetail.getStageIndex();
                        Integer jobIndex = jobExeDetail.getJobIndex();

                        List<Map> steps = jobExeDetail.getSteps();
                        int stepLen = steps.size();
                        for (int stepIndex = 0;stepIndex < stepLen; ++stepIndex) {
                            Map step = steps.get(stepIndex);
//                System.out.println(step);
                            Map pluginInfo = (Map) step.get(Dict.PLUGININFO);
                            //???????????????
                            String pluginCode = (String) pluginInfo.get(Dict.PLUGINCODE);
                            //???????????????
                            String name = (String) pluginInfo.get(Dict.NAME);
                            //???????????????????????????
                            Plugin plugin = iPluginService.queryPluginDetail(pluginCode);
                            //?????????????????????id
                            String category_id = plugin.getCategory().getCategoryId();
                            //???????????????????????????????????????????????????????????????????????????????????????????????????
                            boolean setData = false;
                            if (!CommonUtils.isNullOrEmpty(nameEn)) {
                                if (nameEn.equals(plugin.getPluginNameEn())||nameEn.equals(Dict.ALL)) {
                                    setData = true;
                                }
                            } else {
                                //?????????????????????sonar????????????????????????????????????????????????,???????????????????????????
                                for (String str : Constants.pluginCategoryList) {
                                    if (str.equals(category_id)) {
                                        setData = true;
                                        break;
                                    }
                                }
                            }
                            if (setData) {
                                //?????????????????????
                                Map output = (Map) step.get(Dict.OUTPUT);
                                Map map = new LinkedHashMap();
                                map.put(Dict.PLUGINNAME, name);
                                map.put(Dict.STAGEINDEX,stageIndex);
                                map.put(Dict.JOBINDEX,jobIndex);
                                map.put(Dict.STEPINDEX,stepIndex);
                                map.put(Dict.OUTPUT, output);
//                System.out.println(name+":"+pluginCode+":"+output);
                                outputList.add(map);
                            }
                        }
                    }
                }
                //??????????????????????????????????????????????????????????????????
                if (!CommonUtils.isNullOrEmpty(outputList))
                {
                    result_map.put(Dict.OUTPUTLIST, outputList);
                    resultList.add(result_map);
                }
            }
        }
        return JsonResultUtil.buildSuccess(resultList);
    }

    @RequestValidate(NotEmptyFields = {Dict.JOBEXEID})
    @PostMapping(value = "/downLoadLog")
    public void downLoadLog(@RequestParam("jobExeId") String jobExeId,HttpServletResponse response) throws Exception {
        JobExe jobExe = jobExeService.queryJobExeByExeId(jobExeId);
        User currentLoginUser = null;
        try {
            //?????????????????????
            currentLoginUser = this.userService.getUserFromRedis();
        } catch (Exception e) {
            logger.info(" ?????????????????????????????? ");
        }
        if (currentLoginUser != null) {
//            List<String> lineIds = this.userService.getLineIdsByGroupId(currentLoginUser.getGroup_id());
            //????????????????????????
            PipelineExe pipelineExe = pipelineExeService.queryPipelineExeByExeId(jobExe.getPipelineExeId());
            Map param = new HashMap();
            param.put(Dict.ID, pipelineExe.getPipelineId());
            Pipeline pipeline = this.pipelineService.queryById(param);
            //??????pipeline?????????
//            String groupLineId = pipeline.getGroupLineId();
//            if (!lineIds.contains(groupLineId)) {
//                //??????????????????????????????????????????????????????????????????
//                logger.info(" ???????????????????????????????????????????????? pipelineId" + pipeline.getId());
//                throw new FdevException(ErrorConstants.USER_IS_NOT_AUTH, new String[]{currentLoginUser.getUser_name_en()});
//            }
        }
        String status = jobExe.getStatus();
        String logContent = "";
        if (!CommonUtils.isNullOrEmpty(jobExe.getMinioLogUrl())) {
            if (Dict.SUCCESS.equals(status) || Dict.ERROR.equals(status)) {
                String logPath = jobExe.getMinioLogUrl();
                String[] pathArray = logPath.split("/minio/");
                if (pathArray.length > 1) {
                    String[] minioArr = pathArray[1].split("/");
                    String bucket = minioArr[0];
                    StringBuilder minioPath = new StringBuilder();
                    for (int i = 1; i < minioArr.length; i++) {
                        minioPath.append("/").append(minioArr[i]);
                    }
                    try {
                        logContent = fileService.downloadDocumentFile(bucket, minioPath.toString());
                    } catch (Exception e) {
                        logContent = getRedisLog(jobExe);
                    }
                } else {
                    logger.error("*************minio address  error", logPath);
                    logContent = getRedisLog(jobExe);
                }
            }
        } else if (Dict.RUNNING.equals(status)) {
            logContent = getRedisLog(jobExe);
        } else if (!Dict.RUNNING.equals(status)) {
            logContent = "??????job?????????";
        }
        byte[] bytes = logContent.getBytes();
        response.reset();
        response.setContentType("application/form-data");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setHeader("Content-Disposition", "attachment;filename=" +
                "log" + ".txt");
        ServletOutputStream out = null;
        try {
            out = response.getOutputStream();
            out.write(bytes);
            out.flush();
        } catch (Exception e) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????"});
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}