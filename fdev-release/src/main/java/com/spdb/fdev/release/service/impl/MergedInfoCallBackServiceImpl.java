package com.spdb.fdev.release.service.impl;

import com.spdb.fdev.base.dict.Constants;
import com.spdb.fdev.base.dict.Dict;
import com.spdb.fdev.base.dict.ErrorConstants;
import com.spdb.fdev.base.utils.CommonUtils;
import com.spdb.fdev.common.util.JsonResultUtil;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.common.JsonResult;
import com.spdb.fdev.release.entity.*;
import com.spdb.fdev.release.service.*;
import org.apache.commons.codec.binary.Base64;
import org.bson.types.ObjectId;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

@Service
@RefreshScope
public class MergedInfoCallBackServiceImpl implements IMergedInfoCallBackService {

    private Logger logger = LoggerFactory.getLogger(Dict.LOGGER_BATCH);

    @Autowired
    private IReleaseApplicationService releaseApplicationService;
    @Autowired
    private IUserService userService;
    @Autowired
    private IRelDevopsRecordService relDevopsRecordService;
    @Autowired
    private IGitlabService gitlabService;
    @Autowired
    private IAppService appService;
    @Autowired
    private ITestRunService testRunService;
    @Autowired
    private ICommissionEventService commissionEventService;
    @Autowired
    private ITaskService taskService;
    @Autowired
    private IReleaseTaskService releaseTaskService;
    @Autowired
    private IErrorsService errorsService;
    @Autowired
    private IMailService mailService;

    @Value("${link.port}")
    private String port;
    @Value("${notice.managers}")
    private String managers;

    @Override
    public JsonResult mergedCallBack(Map<String, Object> requestParam) throws Exception{
        MDC.put(Dict.METHOD_NAME, "mergeToMaster");
        String application_id = releaseApplicationService.
                queryApplicationId((String) (requestParam.get(Dict.APPLICATION_ID)), (Integer) (requestParam.get(Dict.GITLAB_PROJECT_ID)));
        String devops_status;
        String merge_state = String.valueOf(requestParam.get(Dict.MERGE_STATE));
        String merge_request_id = String.valueOf(requestParam.get(Dict.MERGE_REQUEST_ID));
        if (Dict.MERGED.equals(merge_state)) {
            devops_status = Constants.DEVOPS_MERGEREQ_MERGED;// merge_state???"merged"
        } else if (Dict.CLOSED.equals(merge_state)) {
            devops_status = Constants.DEVOPS_MERGEREQ_CLOSED;
        } else {
            logger.error("merge_state has wrong format !@@@@@:{}", merge_state);
            throw new FdevException(ErrorConstants.MERGE_STATE_ERROR);
        }
        Map<String, Object> application = appService.queryAPPbyid(application_id);
        String token;
        RelDevopsRecord relDevopsRecord = relDevopsRecordService.findAppByMidAndAppid(application_id, merge_request_id);
        if(CommonUtils.isNullOrEmpty(relDevopsRecord)) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"merge_id???" + merge_request_id + "??????fdev???????????????????????????fdev??????????????????,?????????tag"});
        }
        Map<String, Object> result = userService.queryUserByGitUser((String) requestParam.get(Dict.USERNAME));
        if(!CommonUtils.isNullOrEmpty(result)) {
            String userId = (String) result.get(Dict.ID);
            token = (String) result.get(Dict.GIT_TOKEN);
            logger.info("git?????????{}?????????userId??????{}, token???:{}", requestParam.get(Dict.USERNAME), userId, token);
            try {
                commissionEventService.updateCommissionEvent(relDevopsRecord.getId(),
                        (String) result.get(Dict.ID), Dict.RELEASE_PUBLISH);
            } catch (Exception e) {
                logger.error("??????????????????????????????????????????", e);
            }
        } else {
            token = userService.queryAppManagerGitlabToken(application_id);
            logger.info("??????git???????????????????????????id???????????????{}, ???????????????token:{}", requestParam.get(Dict.USERNAME), token);
        }
        List<String> tags = relDevopsRecordService.queryNormalTags(relDevopsRecord.getRelease_node_name(), application_id);
        int num = 1;
        if(!CommonUtils.isNullOrEmpty(tags)) {
            String[] split = tags.get(0).split("-");
            num = Integer.parseInt(split[split.length - 1]) + 1;
        }
        String serialNo = String.format("%03d", num);
        String tag_name = relDevopsRecord.getProduct_tag();
        String tag_version = relDevopsRecord.getTag_version();
        String date = relDevopsRecord.getRelease_node_name().substring(0, relDevopsRecord.getRelease_node_name().indexOf("_"));

        if (Dict.MERGED.equals(merge_state)) {// ??????????????????merged??? ??????tag??????
            try {
                //?????????
                tag_name = "pro-" + relDevopsRecord.getRelease_node_name()//??????????????????
                        + (CommonUtils.isNullOrEmpty(tag_version) ? "" : ("-" + tag_version))
                        + "-" + serialNo;
                logger.info("????????????tag????????????{}", tag_name);
                gitlabService.createTag(String.valueOf(application.get(Dict.GITLAB_PROJECT_ID)), tag_name, Dict.MASTER, token);
                logger.info("??????tag??????");
                devops_status = Constants.DEVOPS_PRODTAG_CREATED;// product tag????????????
                // ?????????????????????????????????rel??????
                editTaskStage(application_id, relDevopsRecord.getRelease_node_name());
                logger.info("????????????????????????");
            } catch (FdevException ex) {
                logger.error("tag create failed : {}", tag_name);
                List<String> users = new ArrayList<>(Arrays.asList(managers.split("\\|")));
                users.add((String) result.get(Dict.USER_NAME_EN));
                String exception_content = "?????????" + application.get(Dict.NAME_EN)
                        + "?????????" + relDevopsRecord.getRelease_node_name() + "????????????????????????tag??????";
                saveErrors(ex.toString(), "3", users, "http://10.134.13.25:" + port + "/fdev/#/release/list/"
                        + relDevopsRecord.getRelease_node_name() + "/applist", exception_content, exception_content);
                throw ex;
            }
        }
        relDevopsRecord.setApplication_id(application_id);
        relDevopsRecord.setProduct_tag(tag_name);
        relDevopsRecord.setDevops_status(devops_status);
        relDevopsRecord.setMerge_request_id(merge_request_id);
        relDevopsRecord = relDevopsRecordService.setTag(relDevopsRecord);
        //createPipeline????????????????????????????????????(???????????????android/ios)
        String application_type = (String) application.get(Dict.TYPE_NAME);
        Integer git_id = (Integer) application.get(Dict.GITLAB_PROJECT_ID);
        if(CommonUtils.isNullOrEmpty(git_id)){
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST);
        }
        if("IOS??????".equals(application_type) || "Android??????".equals(application_type)){
            createPipelineMethod(tag_name,date,(String)requestParam.get(Dict.USERNAME),relDevopsRecord.getIs_reinforce(),git_id.toString(),token);
        }
        ReleaseApplication releaseApplication = releaseApplicationService.findOneReleaseApplication(application_id,
                relDevopsRecord.getRelease_node_name());
        logger.info("faketime?????????????????????{}", Constants.PULL_FAKE_TAG.equals(releaseApplication.getFake_flag()));
        if(Constants.PULL_FAKE_TAG.equals(releaseApplication.getFake_flag())) {
            String branch_name = Dict.FAKETIME + "-" + relDevopsRecord.getRelease_node_name() + "-" + serialNo;
            String fake_tag = tag_name + "-" + Dict.FAKETIME;
            logger.info("{}????????????????????????tag{}faketime???????????????{}", relDevopsRecord.getRelease_node_name(),
                    relDevopsRecord.getProduct_tag(), branch_name);
            // ???master????????????
            gitlabService.createBranch((Integer) application.get(Dict.GITLAB_PROJECT_ID), branch_name, Dict.MASTER);
            logger.info("?????????????????????{}", branch_name);
            InputStream is = null;
            try {
                String gitFileContent = appService.getGitFileContentById(String.valueOf(application.get(Dict.GITLAB_PROJECT_ID)),
                        URLEncoder.encode(Dict.GITLAB_CI + "/" + Dict.DOCKERFILE,"UTF-8"), branch_name);
                byte[] content = Base64.decodeBase64(gitFileContent);
                byte[] buff = new byte[1024];
                is = new ByteArrayInputStream(content);
                StringBuilder sb = new StringBuilder();
                int len;
                while (-1 != (len=is.read(buff))) {
                    String file_content = new String(buff, 0, len);
                    logger.info("?????????????????????{}", file_content);
                    // ???????????????\r\n????????????\r
                    if(file_content.contains("\r")) {
                        file_content.replace("\r", "");
                    }
                    String[] content_array = file_content.split("\n");
                    int line = 1;
                    for(String line_contant : content_array) {
                        if(line == 1) {
                            sb.append(line_contant, 0, line_contant.lastIndexOf("/") + 1)
                                    .append(releaseApplication.getFake_image_name()).append(":")
                                    .append(releaseApplication.getFake_image_version());
                        } else {
                            sb.append("\n").append(line_contant);
                        }
                        line ++;
                    }
                }
                appService.updateGitFile(String.valueOf(application.get(Dict.GITLAB_PROJECT_ID)),
                        URLEncoder.encode(Dict.GITLAB_CI + "/" + Dict.DOCKERFILE,"UTF-8"), branch_name, sb.toString(),
                        "????????????????????????????????????", token);
                logger.info("?????????????????????????????????????????????{}", sb.toString());
                // ????????????????????????tag
                gitlabService.createTag(String.valueOf(application.get(Dict.GITLAB_PROJECT_ID)), fake_tag, branch_name, token);
                logger.info("??????{}tag??????", fake_tag);
            } catch (Exception e) {
                if(is != null) {
                    is.close();
                }
                // ????????????????????????????????????????????????
                gitlabService.deleteBranch((Integer) application.get(Dict.GITLAB_PROJECT_ID), branch_name);
                logger.info("??????????????????????????????????????????????????????????????????{}", branch_name);
                throw e;
            }
            // ??????????????????
            gitlabService.deleteBranch((Integer) application.get(Dict.GITLAB_PROJECT_ID), branch_name);
            logger.info("{}??????????????????", branch_name);
            RelDevopsRecord fake_record = new RelDevopsRecord();
            fake_record.setApplication_id(application_id);
            fake_record.setRelease_node_name(relDevopsRecord.getRelease_node_name());
            fake_record.setProduct_tag(fake_tag);
            fake_record.setDevops_type("3");
            fake_record.setDevops_status(Constants.DEVOPS_PRODTAG_CREATED);
            relDevopsRecordService.save(fake_record);
        }
        MDC.clear();
        return JsonResultUtil.buildSuccess(null);
    }
    //???????????????createPipeline??????
    private void createPipelineMethod(String ref, String date, String username, String is_reinforce, String git_id,String token) throws Exception {
        List<Map<String, String>> variables=new ArrayList<>();
        Map<String,String> param1=new HashMap<>(2);
        Map<String,String> param2=new HashMap<>(2);
        Map<String,String> param3=new HashMap<>(2);
        Map<String,String> param4=new HashMap<>(2);
        Map<String,String> param5=new HashMap<>(2);
        param1.put("key","FDEV_BRANCH_NAME");
        param1.put("value",ref);
        param2.put("key","FDEV_DESC");
        param2.put("value",date);
        param3.put("key","FDEV_USERNAME");
        param3.put("value",username);
        param4.put("key","FDEV_IS_REINFORCE");
        param4.put("value",is_reinforce);
        param5.put("key","FDEV_ENVS");
        param5.put("value","prod");
        variables.add(param1);
        variables.add(param2);
        variables.add(param3);
        variables.add(param4);
        variables.add(param5);
        gitlabService.createPipeline(git_id,ref,variables,token);
    }

    private void editTaskStage(String application_id, String release_node_name) throws Exception {
        List<ReleaseTask> releaseTasks = releaseTaskService.queryReleaseTaskByAppid(application_id, release_node_name);
        Set<String> ids = new HashSet<>();
        for(ReleaseTask releaseTask : releaseTasks) {
            ids.add(releaseTask.getTask_id());
        }
        Map<String, Object> allTaskMap = new HashMap<>();
        try {
            allTaskMap = taskService.queryTasksByIds(ids);
        } catch (Exception e) {
            logger.error("?????????????????????????????????id???{}, ?????????????????????{}", application_id, release_node_name);
            saveErrors(e.toString(), "2", Arrays.asList(managers.split("\\|")),
                    "http://10.134.13.25:" + port + "/fdev/#/release/list/" + release_node_name + "/joblist",
                    new StringBuilder("????????????????????????,?????????????????????").append(release_node_name).append("??????id???").append(application_id).toString(),
                    Constants.QUERY_TASK_ERROR);
        }
        for(ReleaseTask releaseTask : releaseTasks) {
            try {
                Map<String, Object> taskmap = (Map<String, Object>) allTaskMap.get(releaseTask.getTask_id());
                if(Dict.UAT_LOWER.equals(taskmap.get(Dict.STAGE))) {
                    taskService.updateTaskForRel(releaseTask.getTask_id(), Dict.REL_LOWERCASE,
                            CommonUtils.formatDate(CommonUtils.INPUT_DATE));
                } else {
                    logger.info("{}????????????UAT?????????????????????", releaseTask.getTask_id());
                }
            } catch (Exception e) {
                logger.error("???????????????rel?????????????????????task_id???{}", releaseTask.getTask_id());
                saveErrors(e.toString(), "1", Arrays.asList(managers.split("\\|")),
                        "http://10.134.13.25:" + port + "/fdev/#/job/list/" + releaseTask.getTask_id(),
                        new StringBuilder("???????????????rel?????????????????????task_id???").append(releaseTask.getTask_id()).toString(),
                        Constants.RELEASE_REL_ERROR);
            }
        }
    }

    private void saveErrors(String exception, String type, List<String> users, String link, String description, String content) {
        ObjectId objectId = new ObjectId();
        ErrorCollection errors = new ErrorCollection(objectId, objectId.toString(), "0", type,
                link, exception, description, CommonUtils.formatDate("yyyy-MM-dd HH:mm:ss"));
        errorsService.save(errors);
        try {
            // ????????????????????????
            mailService.sendUserNotify(content, users, Constants.SYSTEM_ERROR, link, "0");
        } catch (Exception e) {
            logger.error("??????????????????");
        }
    }

    @Override
    public JsonResult testRunMergedCallBack(Map<String, Object> requestParam) throws Exception{
        String merge_state = (String) requestParam.get(Dict.MERGE_STATE);
        String merge_request_id = String.valueOf(requestParam.get(Dict.MERGE_REQUEST_ID));
        Integer gitlab_project_id = (Integer) requestParam.get(Dict.GITLAB_PROJECT_ID);
        TestRunTask testRunTask = testRunService.findTaskByGitMergeId(new TestRunTask(gitlab_project_id, merge_request_id));
        if(CommonUtils.isNullOrEmpty(testRunTask)) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"??????????????????fdev????????????????????????"});
        }
        if (Dict.MERGED.equals(merge_state)) {
            testRunTask.setStatus("1");
            testRunService.updateTaskStatusById(testRunTask);
        } else if (Dict.CLOSED.equals(merge_state)) {
            testRunService.removeTaskById(testRunTask);
        } else {
            logger.error("merge_state has wrong format !@@@@@:{}", merge_state);
            throw new FdevException(ErrorConstants.MERGE_STATE_ERROR);
        }
        List<TestRunTask> list = testRunService.findTaskByTestRunId(testRunTask);
        boolean flag = true;
        for(TestRunTask task : list) {
            if(!Constants.MERGED.equals(task.getStatus())) {
                flag = false;
                break;
            }
        }
        // ?????????????????????????????????
        if(flag) {
            TestRunApplication testRunApplication = testRunService.findById(testRunTask.getTestrun_id());
            gitlabService.createMergeRequest(testRunApplication.getGitlab_project_id(),
                    testRunApplication.getTransition_branch(),
                    testRunApplication.getTestrun_branch(), "???????????????????????????", null);
        }
        return JsonResultUtil.buildSuccess(null);
    }

}
