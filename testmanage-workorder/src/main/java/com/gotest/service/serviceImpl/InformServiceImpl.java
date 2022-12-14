package com.gotest.service.serviceImpl;

import com.gotest.controller.InformController;
import com.gotest.dao.*;
import com.gotest.dict.Constants;
import com.gotest.dict.Dict;
import com.gotest.dict.ErrorConstants;
import com.gotest.domain.*;
import com.gotest.service.*;
import com.gotest.utils.CommonUtils;
import com.gotest.utils.MailUtil;
import com.gotest.utils.MyUtil;
import com.gotest.utils.OrderUtil;
import com.test.testmanagecommon.exception.FtmsException;
import com.test.testmanagecommon.rediscluster.RedisUtils;
import com.test.testmanagecommon.transport.RestTransport;
import com.test.testmanagecommon.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RefreshScope
public class InformServiceImpl implements InformService {

    private Logger logger = LoggerFactory.getLogger(InformController.class);
    @Autowired
    private MessageFdevMapper msgFdevMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private FdevSitMsgMapper fdevSitMsgMapper;
    @Autowired
    private ITaskApi taskApi;
    @Autowired
    private MyUtil myUtil;
    @Autowired
    private RestTransport restTransport;
    @Autowired
    private MailUtil mailUtil;
    @Autowired
    private TaskListMapper taskListMapper;
    @Autowired
    private OrderUtil orderUtil;
    @Autowired
    private GroupMapper groupMapper;
    @Autowired
    private INotifyApi iNotifyApi;
    @Value("${fdev.task.domain}")
    private String fdevTask;
    @Value("${security.test.addressee}")
    private List<String> addressee;
    @Autowired
    private IUserService userService;
    @Autowired
    private OrderDimensionService orderDimensionService;

    @Value("${user.testManager.role.id}")
    private String testManagerRoleId;

    @Value("${user.testLeader.role.id}")
    private String testLeaderRoleId;

    @Value("${user.tester.role.id}")
    private String testerRoleId;

    @Value("${user.testAdmin.role.id}")
    private String testAdminRoleId;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private IFdevGroupApi fdevGroupApi;
    @Autowired
    private IDemandService demandService;

    @Autowired
    private NewFdevMapper newFdevMapper;

    @Autowired
    private ITaskApi iTaskApiImpl;

    @Autowired
    private MantisService mantisService;
    /**
     * ?????? ???????????????????????????messageFlag=1
     * ???????????????userEnName???currentPage???pageSize
     */
    @Override
    public List<MessageFdev> queryMessage(Map map) throws Exception {
        String userEnName = (String) map.getOrDefault(Dict.USER_EN_NAME, ErrorConstants.DATA_NOT_EXIST);
        Integer pageSize = (Integer) map.getOrDefault(Dict.PAGESIZE, Constants.PAGESIZE_DEF);
        Integer currentPage = (Integer) map.getOrDefault(Dict.CURRENTPAGE, Constants.CURRENTPAGE_DEF);
        Integer start = pageSize * (currentPage - 1);//??????
        List<MessageFdev> resultlist = msgFdevMapper.queryMessage(userEnName, start, pageSize);
        return resultlist;
    }

    /**
     * ??????????????? userEnName ??????????????????????????????
     */
    @Override
    public Map queryMsgCountByUserEnName(Map map) throws Exception {
        String userEnName = (String) map.getOrDefault(Dict.USER_EN_NAME, ErrorConstants.DATA_NOT_EXIST);
        if ("".equals(userEnName) || ErrorConstants.DATA_NOT_EXIST.equals(userEnName)) {
            map.put(Dict.TOTAL, 0);
        } else {
            Integer count = msgFdevMapper.queryMsgCountByUserEnName(userEnName);
            map.put(Dict.TOTAL, count);
        }
        return map;
    }

    /**
     * ????????????
     * ?????? ?????????????????????messageFlag=1
     * *  ???????????????userEnName???currentPage???pageSize
     */
    @Override
    public List<MessageFdev> queryMessageRecord(Map map) throws Exception {
        String userEnName = (String) map.getOrDefault(Dict.USER_EN_NAME, Constants.FAIL_GET);
        Integer pageSize = (Integer) map.getOrDefault(Dict.PAGESIZE, Constants.PAGESIZE_DEF);
        Integer currentPage = (Integer) map.getOrDefault(Dict.CURRENTPAGE, Constants.CURRENTPAGE_DEF);
        Integer start = pageSize * (currentPage - 1);//??????
        String messageFlag = (String) map.getOrDefault(Dict.MESSAGEFLAG, Constants.FAIL_GET);
        //???????????????????????????
        Map<String, Object> currentUser = redisUtils.getCurrentUserInfoMap();
        if (Util.isNullOrEmpty(currentUser)) {
            throw new FtmsException(ErrorConstants.GET_CURRENT_USER_INFO_ERROR);
        }
        List<String> role_id = (List<String>) currentUser.get(Dict.ROLE_ID);
        List<MessageFdev> resultlist;
        if (role_id.contains(testAdminRoleId)) {
            resultlist = msgFdevMapper.queryMasterMessageRecord(start, pageSize, messageFlag);
        } else {
            resultlist = msgFdevMapper.queryMessageRecord(userEnName, start, pageSize, messageFlag);
        }
        return resultlist;
    }

    /**
     * ???????????? ??????????????? userEnName ????????????????????????
     */
    @Override
    public Map queryMessageRecordCount(Map map) throws Exception {
        String userEnName = (String) map.getOrDefault(Dict.USER_EN_NAME, Constants.FAIL_GET);
        String messageFlag = map.getOrDefault(Dict.MESSAGEFLAG, Constants.FAIL_GET).toString();
        //???????????????????????????
        Map<String, Object> currentUser = redisUtils.getCurrentUserInfoMap();
        if (Util.isNullOrEmpty(currentUser)) {
            throw new FtmsException(ErrorConstants.GET_CURRENT_USER_INFO_ERROR);
        }
        List<String> role_id = (List<String>) currentUser.get(Dict.ROLE_ID);
        Integer count;
        if (role_id.contains(testAdminRoleId)) {
            count = msgFdevMapper.queryMasterMessageRecordCount(messageFlag);
        } else {
            count = msgFdevMapper.queryMessageRecordCount(userEnName, messageFlag);
        }
        map.put(Dict.TOTAL, count);
        return map;
    }


    /**
     * ??????messageId ??????????????????????????????
     */
    @Override
    public Map updateOneMsgByMsgId(Map map) throws Exception {
        Integer messageId = (Integer) map.getOrDefault(Dict.MESSAGE_ID, Constants.MESSAGE_ID_DEF);
        Integer count = msgFdevMapper.updateOneMsgByMsgId(messageId);
        map.put("count", count);
        return map;
    }

    /**
     * ?????? userEnName ???????????????????????? ??? ??????
     */
    @Override
    public Map updateAllMsgByUserEnName(Map map) throws Exception {
        String userEnName = (String) map.getOrDefault(Dict.USER_EN_NAME, ErrorConstants.DATA_NOT_EXIST);
        Integer count = msgFdevMapper.updateAllMsgByUserEnName(userEnName);
        map.put("count", count);
        return map;
    }

    /**
     * Fdev???????????? ??????
     * taskNo???????????????
     * jobId???????????????
     * taskName????????????
     * taskStage???????????????
     * testers???????????????????????? Map{(1,"aa"),(2, "22")}
     * taskReason???????????????
     * JIRANo???JIRA??????
     * taskDesc?????????
     * ???????????????????????????????????????????????????????????????????????????????????????
     */
    @Override
    public Collection<String> addMsgFromFdev(Map map) throws Exception {
        Collection<String> sendUser = new HashSet<>();
        List<String> testers = (List) map.getOrDefault(Dict.TESTERS, new ArrayList<>());
        MessageFdev messageFdev = null;
        WorkOrder workOrder = workOrderMapper.queryWorkOrderByNo((String) map.getOrDefault(Dict.JOBID, Constants.FAIL_GET));
        if (!Util.isNullOrEmpty(testers)) {
            sendUser.addAll(testers);
        } else {

            Set<String> allUser = new HashSet<>();
            // ????????? ?????????
            if (!Util.isNullOrEmpty(workOrder.getWorkManager()))
                allUser.add(workOrder.getWorkManager());

            //?????? ???????????????
            if (!Util.isNullOrEmpty(workOrder.getGroupLeader()))
                allUser.addAll(Arrays.asList(workOrder.getGroupLeader().split(",")));

            // ????????? ????????????
            if (!Util.isNullOrEmpty(workOrder.getTesters()))
                allUser.addAll(Arrays.asList(workOrder.getTesters().split(",")));
            sendUser.addAll(allUser);
        }
        Iterator<String> iterator = sendUser.iterator();
        while (iterator.hasNext()) {
            messageFdev = new MessageFdev();
            String userEnName = iterator.next().toString();
            messageFdev.setRqrNo(workOrder.getDemandNo());
            messageFdev.setUserEnName(userEnName);
            messageFdev.setTaskNo((String) map.getOrDefault(Dict.JOBNO, Constants.FAIL_GET));
            messageFdev.setTaskName((String) map.getOrDefault(Dict.TASKNAME, Constants.FAIL_GET));
            messageFdev.setWorkNo((String) map.getOrDefault(Dict.JOBID, Constants.FAIL_GET));
            messageFdev.setWorkStage((String) map.getOrDefault(Dict.TASKSTAGE, Constants.FAIL_GET));
            messageFdev.setTaskReason((String) map.getOrDefault(Dict.TASKREASON, Constants.FAIL_GET));
            messageFdev.setJiraNo((String) map.getOrDefault(Dict.JIRANO, Constants.FAIL_GET));
            messageFdev.setTaskDesc((String) map.getOrDefault(Dict.TASKDESC, Constants.FAIL_GET));
            messageFdev.setCreateTime((int) (new Date().getTime() / 1000));
            messageFdev.setMessageFlag(Constants.NUMBER_1); //??????
            msgFdevMapper.addMsgFromFdev(messageFdev);
        }

        return sendUser;
    }

    @Override
    public Collection<String> queryAllUser(Map map) throws Exception {
        Collection<String> sendUser = new HashSet<>();
        WorkOrder workOrder = workOrderMapper.queryWorkOrderByNo((String) map.getOrDefault(Dict.JOBID, Constants.FAIL_GET));
        Set<String> allUser = new HashSet<>();
        // ????????? ?????????
        if (!Util.isNullOrEmpty(workOrder.getWorkManager()))
            allUser.add(workOrder.getWorkManager());

        //?????? ???????????????
        if (!Util.isNullOrEmpty(workOrder.getGroupLeader()))
            allUser.addAll(Arrays.asList(workOrder.getGroupLeader().split(",")));

        // ????????? ????????????
        if (!Util.isNullOrEmpty(workOrder.getTesters()))
            allUser.addAll(Arrays.asList(workOrder.getTesters().split(",")));
        sendUser.addAll(allUser);
        return sendUser;
    }

    /**
     * ??????fdev????????????
     *
     * @param map
     * @return
     * @throws Exception
     */
    public String addFdevSitMsg(Map<String, Object> map) throws Exception {
        //??????????????????
        String regressionTestScope = (String) map.get(Dict.REGRESSIONTESTSCOPE);
        //????????????
        String interfaceChange = (String) map.get(Dict.INTERFACECHANGE);
        //??????????????????
        String otherSystemChange = (String) map.get(Dict.OTHERSYSTEMCHANGE);
        //???????????????
        String databaseChange = (String) map.get(Dict.DATABASECHANGE);
        //????????????
        String env = (String) map.get(Dict.TESTENV);
        //????????????
        List<String> developers = (List<String>) map.get(Dict.DEVELOPER);
        //?????????
        String appName = (String) map.get(Dict.APPNAME);
        //?????????
        String workNo = (String) map.get(Dict.JOBID);
        //???????????????
        String clientVersion = (String) map.get(Dict.CLIENTVERSION);
        //????????????
        String rqrNo = (String) map.get(Dict.RQRNO);
        //?????????id
        String groupId = (String) map.get(Dict.GROUPID);
        //????????????
        String copyTo = "";
        if(!CommonUtils.isNullOrEmpty(map.get(Dict.COPYTO))) {
            copyTo = String.join(",", (List<String>) map.get(Dict.COPYTO));
        }
        FdevSitMsg fdevSitMsg = new FdevSitMsg();
        fdevSitMsg.setTaskNo(String.valueOf(map.get(Dict.JOBNO)));
        fdevSitMsg.setTestReason(String.valueOf(map.get(Dict.TASKREASON)));
        fdevSitMsg.setRepairDesc(String.valueOf(map.get(Dict.TASKDESC)));
        fdevSitMsg.setJiraNo("");
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);
        String date = sdf.format(new Date());
        fdevSitMsg.setCreateTime(date);
        fdevSitMsg.setAppName(appName);
        fdevSitMsg.setClientVersion(clientVersion);
        fdevSitMsg.setRegressionTestScope(regressionTestScope);
        fdevSitMsg.setTestEnv(env);
        fdevSitMsg.setDeveloper(String.join(",", developers));
        fdevSitMsg.setOtherSystemChange(otherSystemChange);
        fdevSitMsg.setDatabaseChange(databaseChange);
        fdevSitMsg.setInterfaceChange(interfaceChange);
        fdevSitMsg.setWorkNo(workNo);
        fdevSitMsg.setRqrNo(rqrNo);
        fdevSitMsg.setGroupId(groupId);
        fdevSitMsg.setCopyTo(copyTo);
        fdevSitMsgMapper.addFdevSitMsg(fdevSitMsg);
        return fdevSitMsg.getId().toString();
    }

    @Override
    public String addFdevSitMsg(FdevSitMsg fdevSitMsg) throws Exception {
        fdevSitMsgMapper.addFdevSitMsg(fdevSitMsg);
        return fdevSitMsg.getId().toString();
    }

    /**
     * ????????????id??????fdev????????????
     *
     * @param map
     * @return
     * @throws Exception
     */
    public Map<String, String> queryFdevSitMsg(Map map) throws Exception {
        Map<String, String> result = new HashMap<>();
        String taskNo = String.valueOf(map.get(Dict.ID));
        //??????"FTMS_SUBMIT_SIT_RECORD"????????????????????????????????????????????????????????????????????????"msg_fdev"??????
        FdevSitMsg fdevSitMsg = fdevSitMsgMapper.queryFdevSitMsg(taskNo);
        if (!Util.isNullOrEmpty(fdevSitMsg)) {
            result.put(Dict.TASKDESC, fdevSitMsg.getRepairDesc());
        } else {
            String workNo = workOrderMapper.queryWorkNoByTaskId(taskNo, (String) map.get(Dict.ORDERTYPE));
            String desc = msgFdevMapper.queryFirstTestDesc(workNo);
            result.put(Dict.TASKDESC, desc);
        }
        if (Util.isNullOrEmpty(result.get(Dict.TASKDESC))) {
            logger.error("query fdevSitMsg fail");
            throw new FtmsException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????id???????????????"});
        }
        return result;
    }

    /**
     * ????????????id????????????????????????
     *
     * @param id
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> querySitMsgDetail(String id) throws Exception {
        Map result = new HashMap();
        //??????????????????
        FdevSitMsg fdevSitMsg = fdevSitMsgMapper.querySitMsgDetail(id);
        String developers = taskApi.queryTaskDevelopNameCns(fdevSitMsg.getTaskNo(), Dict.USERNAMECN);
        fdevSitMsg.setDeveloper(developers);
        if (!Util.isNullOrEmpty(fdevSitMsg.getTesters())) {
            String testsers = myUtil.changeEnNameToCn(fdevSitMsg.getTesters());
            fdevSitMsg.setTesters(testsers);
        }
        //??????????????????
        getTestReason(fdevSitMsg);
        result.put(Dict.SUBMITINFO, fdevSitMsg);
        String taskNo = fdevSitMsg.getTaskNo();
        String fdevNew = workOrderMapper.queryNewFdevBytaskId(taskNo, Constants.ORDERTYPE_FUNCTION);
        //????????????fdev
        if ("1".equals(fdevNew)) {
            try {
                Map<String, Object> task = taskApi.getNewTaskById(taskNo);
                String createUserId = (String) task.get(Dict.CREATEUSERID);
                Map<String, Object> map1 = userService.queryUserCoreDataById(createUserId);
                //???????????????
                Map creator = new HashMap();
                creator.put(Dict.ID, task.get(Dict.CREATEUSERID));
                creator.put(Dict.USER_NAME_CN, map1.get(Dict.USER_NAME_CN));
                creator.put(Dict.USER_NAME_EN, map1.get(Dict.USER_NAME_EN));
                task.put(Dict.CREATOR, creator);
                //???????????????
                List<String> assigneeList = (List<String>) task.get(Dict.ASSIGNEELIST);
                Map<String, Object> map3 = userService.queryUserCoreDataById(assigneeList.get(0));
                Map developer = new HashMap();
                developer.put(Dict.ID, assigneeList.get(0));
                developer.put(Dict.USER_NAME_CN, map3.get(Dict.USER_NAME_CN));
                developer.put(Dict.USER_NAME_EN, map3.get(Dict.USER_NAME_EN));
                task.put(Dict.DEVELOPER, developer);
                //???????????????
                Map<String, Object> groupMap = userService.queryGroupDetailById((String) task.get(Dict.ASSIGNEEGROUPID));
                Map group = new HashMap();
                group.put(Dict.NAME, groupMap.get(Dict.NAME));
                group.put(Dict.ID, task.get(Dict.ASSIGNEEGROUPID));
                task.put(Dict.GROUP, group);
                //??????????????????
                task.put(Dict.FEATURE_BRANCH, task.get(Dict.BRANCHNAME));
                //???????????????????????????????????????
                Map unit = demandService.queryNewUnitInfoById((String) task.get(Dict.IMPLUNITID));
                if (!Util.isNullOrEmpty(unit)) {
                    task.put(Dict.IMPLUNITNUM, unit.get(Dict.IMPLUNITNUM));
                }
                result.put(Dict.TASKINFO, task);
            } catch (Exception e) {
                logger.error("fail to get task info");
                throw new FtmsException(ErrorConstants.DATA_QUERY_ERROR, new String[]{"????????????????????????"});
            }
        } else {
            try {
                Map taskInfo = (Map) taskApi.queryTaskDetailByIds(Arrays.asList(fdevSitMsg.getTaskNo())).get(fdevSitMsg.getTaskNo());
                result.put(Dict.TASKINFO, taskInfo);
            } catch (Exception e) {
                logger.error("fail to get task info");
                throw new FtmsException(ErrorConstants.DATA_QUERY_ERROR, new String[]{"????????????????????????"});
            }
        }
        return result;
    }

    @Override
    public List<FdevSitMsg> querySitMsgList(String workNo, Integer pageSize, Integer startPage,
                                            List<String> sitGroupIds, List<String> orderGroupIds, String tester, String startDate,
                                            String endDate, String stage, String orderType, Boolean isIncludeChildren) throws Exception {
        //?????????????????????????????????
        if (!Util.isNullOrEmpty(sitGroupIds) && isIncludeChildren) {
            Map<String, List<Map>> groupMap = userService.queryChildGroupByIds(sitGroupIds);
            sitGroupIds = new ArrayList<>();
            for (List<Map> groupList : groupMap.values()) {
                sitGroupIds.addAll(groupList.stream().map(group -> (String)group.get(Dict.ID)).distinct().collect(Collectors.toList()));
            }
            sitGroupIds.remove(null);
        }
        if (!Util.isNullOrEmpty(orderGroupIds) && isIncludeChildren) {
            Map<String, List<Map>> groupMap = userService.queryChildGroupByIds(orderGroupIds);
            orderGroupIds = new ArrayList<>();
            for (List<Map> groupList : groupMap.values()) {
                orderGroupIds.addAll(groupList.stream().map(group -> (String)group.get(Dict.ID)).collect(Collectors.toList()));
            }
        }
        List<FdevSitMsg> list = fdevSitMsgMapper.querySitMsgList(workNo, pageSize, startPage, sitGroupIds, tester, startDate, endDate, stage, orderType, orderGroupIds);
        if (!Util.isNullOrEmpty(list)) {
            //?????????fdev???????????????
            List<FdevSitMsg> oldFdevSitMsgs = list.stream().filter(e -> !"1".equals(e.getFdev_new())).collect(Collectors.toList());
            List<String> oldTaskIds = oldFdevSitMsgs.stream().map(fdevSitMsg -> fdevSitMsg.getTaskNo()).collect(Collectors.toList());
            oldTaskIds.remove(null);
            //?????????fdev????????????
            List<Map> tasks = new ArrayList<>();
            if (!Util.isNullOrEmpty(oldTaskIds)) {
                tasks = taskApi.queryTaskBaseInfoByIds(oldTaskIds, null, Arrays.asList(Dict.ID, Dict.NAME, Dict.DEVELOPER));
            }
            //???????????????????????????id
            Set<String> taskUserIds = new HashSet<>();
            taskUserIds.remove(null);
            for (Map task : tasks) {
                if(!CommonUtils.isNullOrEmpty(task.get(Dict.DEVELOPER))) {
                    taskUserIds.addAll((List<String>)task.get(Dict.DEVELOPER));
                }
            }
            //????????????????????????
            Map userInfoMap = userService.queryUserByIds(taskUserIds);
            //?????????id???????????????????????????
            Map<String, Map> taskInfoMap = new HashMap();
            for (Map task : tasks) {
                taskInfoMap.put((String) task.get(Dict.ID), task);
            }
            //????????????????????????
            Map<String, Map> groupInfoMap = new HashMap<>();
            Set<String> groupIds = oldFdevSitMsgs.stream().map(fdevSitMsg -> fdevSitMsg.getGroupId()).collect(Collectors.toSet());
            groupIds.addAll(oldFdevSitMsgs.stream().map(fdevSitMsg -> fdevSitMsg.getOrderGroupId()).collect(Collectors.toSet()));
            groupIds.remove(null);
            List<Map> groupList = userService.queryGroupByIds(new ArrayList<>(groupIds));
            if (!CommonUtils.isNullOrEmpty(groupList)) {
                for (Map group : groupList) {
                    groupInfoMap.put((String) group.get(Dict.ID), group);
                }
            }
            for (FdevSitMsg fdevSitMsg : list) {
                //??????????????????fdev_new
                String fdev_new = fdevSitMsg.getFdev_new();
                if ("1".equals(fdev_new)) {
                    //??????????????????
                    Map<String, Object> task = getNewTaskById(fdevSitMsg.getTaskNo());
                    if (Util.isNullOrEmpty(task)) {
                        continue;
                    }
                    List<String> assigneeList = (List<String>) task.get("assigneeList");
                    String userId = assigneeList.get(0);
                    //??????????????????
                    Map<String, Object> map = userService.queryUserCoreDataById(userId);
                    String usernameCn = (String) map.get(Dict.USERNAMECN);
                    fdevSitMsg.setDeveloper(usernameCn);
                    fdevSitMsg.setFdevTaskName((String) task.get(Dict.NAME));
                } else {
                    //???????????????????????????
                    Map taskInfo = taskInfoMap.get(fdevSitMsg.getTaskNo());
                    if (!CommonUtils.isNullOrEmpty(taskInfo)) {
                        List<String> developers = (List<String>) taskInfo.get(Dict.DEVELOPER);
                        StringBuffer developerNameStr = new StringBuffer();
                        String developerName = "";
                        if (!CommonUtils.isNullOrEmpty(developers)) {
                            for (String developer : developers) {
                                if(!CommonUtils.isNullOrEmpty(userInfoMap.get(developer))) {
                                    Map userInfo = (Map) userInfoMap.get(developer);
                                    developerNameStr.append(userInfo.get(Dict.USER_NAME_CN));
                                    developerNameStr.append(",");
                                }
                            }
                            if (developerNameStr.toString().endsWith(",")) {
                                developerName = developerNameStr.substring(0, developerNameStr.length()-1);
                            }
                        }
                        fdevSitMsg.setDeveloper(developerName);
                    }

                    try {
                        fdevSitMsg.setFdevTaskName((String) taskInfo.get(Dict.NAME));
                    } catch (Exception e) {
                        logger.error("fail to get fdevTaskName");
                        fdevSitMsg.setFdevTaskName("/");
                    }
                }
                //???????????????????????????(?????????????????????)??????
                String group_id = fdevSitMsg.getGroupId();
                if (!Util.isNullOrEmpty(group_id)) {
                    Map<String, Object> group = groupInfoMap.get(group_id);
                    if (!Util.isNullOrEmpty(group)) {
                        fdevSitMsg.setGroupName((String) group.get(Dict.NAME));
                    } else {
                        fdevSitMsg.setGroupName("/");
                    }
                } else {
                    fdevSitMsg.setGroupName("/");
                }
                //???????????????????????????
                String orderGroupId = fdevSitMsg.getOrderGroupId();
                if (!Util.isNullOrEmpty(orderGroupId)) {
                    Map<String, Object> group = groupInfoMap.get(orderGroupId);
                    if (!Util.isNullOrEmpty(group)) {
                        fdevSitMsg.setOrderGroupName((String) group.get(Dict.NAME));
                    } else {
                        fdevSitMsg.setOrderGroupName("/");
                    }
                } else {
                    fdevSitMsg.setOrderGroupName("/");
                }
                //??????????????????
                if (!Util.isNullOrEmpty(fdevSitMsg.getTesters())) {
                    String testsers = myUtil.changeEnNameToCn(fdevSitMsg.getTesters());
                    fdevSitMsg.setTesters(testsers);
                }
                //?????????????????????
                getTestReason(fdevSitMsg);
            }
        }
        return list;
    }

    @Override
    public Integer countSitMsgList(String workNo, List<String> sitGroupIds, List<String> orderGroupIds, String tester, String startDate, String endDate, String stage, String orderType, Boolean isIncludeChildren) throws Exception {
        //?????????????????????????????????
        if (!Util.isNullOrEmpty(sitGroupIds) && isIncludeChildren) {
            Map<String, List<Map>> groupMap = userService.queryChildGroupByIds(sitGroupIds);
            sitGroupIds = new ArrayList<>();
            for (List<Map> groupList : groupMap.values()) {
                sitGroupIds.addAll(groupList.stream().map(group -> (String)group.get(Dict.ID)).collect(Collectors.toList()));
            }
        }
        if (!Util.isNullOrEmpty(orderGroupIds) && isIncludeChildren) {
            Map<String, List<Map>> groupMap = userService.queryChildGroupByIds(orderGroupIds);
            orderGroupIds = new ArrayList<>();
            for (List<Map> groupList : groupMap.values()) {
                orderGroupIds.addAll(groupList.stream().map(group -> (String)group.get(Dict.ID)).collect(Collectors.toList()));
            }
        }
        return fdevSitMsgMapper.countSitMsgList(workNo, sitGroupIds, tester, startDate, endDate, stage, orderType, orderGroupIds);
    }

    private void getTestReason(FdevSitMsg fdevSitMsg) {
        String testReason = "";
        switch (fdevSitMsg.getTestReason()) {
            case "1":
                testReason = Constants.TEST_REASON_1;
                break;
            case "2":
                testReason = Constants.TEST_REASON_2;
                break;
            case "3":
                testReason = Constants.TEST_REASON_3;
                break;

        }
        fdevSitMsg.setTestReason(testReason);
    }

    /**
     * ????????????????????????
     *
     * @param map
     * @throws Exception
     */
    @Override
    public void sendStartUatMail(Map map) throws Exception {
        String workNo = String.valueOf(map.get(Dict.WORKNO));
        WorkOrder order = workOrderMapper.queryWorkOrderByNo(workNo);
        String fdevNew = order.getFdevNew();
        if ("1".equals(fdevNew)) {
            Set<String> emails = new HashSet();
            //???????????????fdev????????????
            String flag = order.getWorkOrderFlag();
            if ("0".equals(flag)) {
                logger.error("order does not come from fdev");
                throw new FtmsException(ErrorConstants.MAINTASKNO_NOT_EXIST_ERROR);
            }
            Set<String> to = new HashSet();
            if (map.keySet().contains("additionalCCPerson")) {
                List<String> additionalCCPerson = (List<String>) map.get("additionalCCPerson");
                emails.addAll(additionalCCPerson);
            }
            List<String> taskNames = new ArrayList<>();
            //???fdev???????????????????????????????????????????????????
            List<TaskList> taskLists = taskListMapper.queryTaskByNo(workNo);
            List<String> taskIds = taskLists.stream().map(e -> e.getTaskno()).collect(Collectors.toList());
            Set<String> copyToIds = new HashSet<>();
            if (!Util.isNullOrEmpty(taskIds)) {
                for (String taskId : taskIds) {
                    Map<String, Object> task = iTaskApiImpl.getNewTaskById(taskId);
                    if (Util.isNullOrEmpty(task)) {
                        logger.error("order does not come from fdev");
                        throw new FtmsException(ErrorConstants.MAINTASKNO_NOT_EXIST_ERROR);
                    }
                    List<String> assigneeList = (List<String>) task.get("assigneeList");
                    to.add(assigneeList.get(0));
                    to.add((String) task.get("createUserId"));
                    taskNames.add((String) task.get(Dict.NAME));
                }
                List<FdevSitMsg> copyTos = fdevSitMsgMapper.queryAllCopyToByTaskIds(taskIds);
                copyTos.forEach(copyTo -> copyToIds.addAll(Arrays.asList((copyTo.getCopyTo()).split(","))));
            }
            //??????????????????????????????
            String workManager = order.getWorkManager();//???????????????
            String workLeader = order.getGroupLeader();//?????????
            String testers = order.getTesters();//????????????
            Set<String> ftms = new HashSet<>();
            ftms.add(workManager);
            if (!Util.isNullOrEmpty(workLeader)) {
                ftms.addAll(Arrays.asList(workLeader.split(",")));
            }
            if (!Util.isNullOrEmpty(testers)) {
                ftms.addAll(Arrays.asList(testers.split(",")));
            }
            ftms.remove("");
            to.addAll(getFdevUserIdsByEn(ftms));
            to.addAll(copyToIds);
            for (String users : to) {
                try {
                    Map user = userService.queryUserCoreDataById(users);
                    if (!Util.isNullOrEmpty(user)) {
                        emails.add((String) user.getOrDefault("email", null));
                    }
                } catch (Exception e) {
                    logger.error("id:" + users + "??????????????????");
                }
            }
            if (Util.isNullOrEmpty(emails)) {
                logger.error("no target to send mail");
                throw new FtmsException(ErrorConstants.NO_MAIL_TARGET_ERROR);
            }
            //????????????
            if (emails.size() > 0) {
                emails.remove("");
            }
            String[] target = emails.toArray(new String[emails.size()]);
            //??????fdev???
            String groupName = "";
            //??????uat?????????
            String uatContact = "";
            String uatContactMail = "";
            String uatContactName = "";
            String jira = "";
            if (!Util.isNullOrEmpty(order.getFdevGroupId())) {
                groupName = "???" + String.valueOf(fdevGroupApi.queryGroupDetail(order.getFdevGroupId()).get(Dict.NAME)) + "???";
                uatContact = groupMapper.queryUatContact(order.getFdevGroupId());
            }
            if (!Util.isNullOrEmpty(uatContact)) {
                Map<String, Object> user = userService.queryUserCoreDataByNameEn(uatContact);
                if (!Util.isNullOrEmpty(user)) {
                    uatContactMail = (String) user.getOrDefault(Dict.EMAIL, null);
                    uatContactName = (String) user.getOrDefault("user_name_cn", null);
                    jira += "??????????????????jira????????????????????????" + uatContactName + "???" + uatContactMail + "???";
                }
            }
            //??????model??????
            HashMap<String, String> model = new HashMap<>();
            String rqrmntNo = order.getDemandNo();
            String rqrmntName = order.getDemandName();
            model.put(Dict.RQRMNTNO, rqrmntNo);
            model.put(Dict.RQRMNTNAME, rqrmntName);
            model.put(Dict.MAINTASKNAME, order.getMainTaskName());
            model.put(Dict.PLANPRODATE, ifNull(String.valueOf(map.get(Dict.PLANPRODATE)), "???"));
            if (Util.isNullOrEmpty(map.get(Dict.REPAIR_DESC))) {
                model.put(Dict.REPAIR_DESC, "???");
            } else {
                String repairDesc = String.valueOf(map.get(Dict.REPAIR_DESC)).replace("\n", "<br/>");
                model.put(Dict.REPAIR_DESC, MyUtil.replace160Char(repairDesc));
            }
            model.put(Dict.REGRESSIONTESTSCOPE, ifNull(String.valueOf(map.get(Dict.REGRESSIONTESTSCOPE)), "???"));
            model.put(Dict.INTERFACECHANGE, ifNull(String.valueOf(map.get(Dict.INTERFACECHANGE)), "???"));
            model.put(Dict.DATABASECHANGE, ifNull(String.valueOf(map.get(Dict.DATABASECHANGE)), "???"));
            model.put(Dict.CLIENTVERSION, ifNull(String.valueOf(map.get(Dict.CLIENTVERSION)), "???"));
            if (Util.isNullOrEmpty(map.get(Dict.TESTENV))) {
                model.put(Dict.TESTENV, "???");
            } else {
                String repairDesc = String.valueOf(map.get(Dict.TESTENV)).replace("\n", "<br/>");
                model.put(Dict.TESTENV, MyUtil.replace160Char(repairDesc));
            }
            model.put(Dict.APPNAME, ifNull(String.valueOf(map.get(Dict.APPNAME)), "???"));
            model.put(Dict.OTHERSYSTEMCHANGE, ifNull(String.valueOf(map.get(Dict.OTHERSYSTEMCHANGE)), "???"));
            model.put(Dict.DEVELOPER, ifNull(String.valueOf(map.get(Dict.DEVELOPER)), "???"));
            model.put(Dict.TASKLIST, String.join("???", taskNames));
            model.put(Dict.UNIT, order.getUnit());
            if (Util.isNullOrEmpty(map.get(Dict.UATREMARK))) {
                model.put(Dict.UATREMARK, "???");
            } else {
                String repairDesc = String.valueOf(map.get(Dict.UATREMARK)).replace("\n", "<br/>");
                model.put(Dict.UATREMARK, MyUtil.replace160Char(repairDesc));
            }
            model.put(Dict.JIRA, jira);
            //??????
            String file = "";
            model.put(Dict.FILE, file);
            String subject = groupName + "????????????????????????"
                    + rqrmntNo + "  " + String.valueOf(order.getMainTaskName());
            mailUtil.sendSitReportMail(subject, "ftms_uatInform", model, target);
            //?????????????????????????????????sit_flag???2?????????????????????
            workOrderMapper.updateUatFlag(workNo);
        } else {
            //??????????????????????????????????????????fdev??????????????????????????????;????????????????????????????????????
            String flag = order.getWorkOrderFlag();
            if ("0".equals(flag)) {
                logger.error("order does not come from fdev");
                throw new FtmsException(ErrorConstants.MAINTASKNO_NOT_EXIST_ERROR);
            }
            List<String> taskNames = new ArrayList<>();
            Set<String> to = new HashSet();
            //???fdev????????????????????????
            String rqrmntNo = "";
            String rqrSerialNo = "";
            String rqrmntName = "";
            if (!Util.isNullOrEmpty(order.getMainTaskNo())) {
                Map send = new HashMap();
                send.put(Dict.ID, order.getMainTaskNo());
                send.put(Dict.REST_CODE, "queryTaskDetail");
                send = (Map) restTransport.submitSourceBack(send);
                rqrmntNo = String.valueOf(((Map) send.get(Dict.DEMAND)).get(Dict.OA_CONTACT_NO));//????????????
                rqrSerialNo = String.valueOf(send.get(Dict.RQRMNT_NO));//????????????
                rqrmntName = String.valueOf(((Map) send.get(Dict.DEMAND)).get(Dict.OA_CONTACT_NAME));
                taskNames.add((String) send.get(Dict.NAME));
            } else {
                Map<String, Object> unitResult = demandService.queryByFdevNoAndDemandId(order.getUnit());
                rqrmntNo = String.valueOf(((Map) unitResult.get(Dict.DEMAND_BASEINFO)).get(Dict.OA_CONTACT_NO));
                rqrSerialNo = String.valueOf(((Map) unitResult.get(Dict.DEMAND_BASEINFO)).get(Dict.ID));
                rqrmntName = String.valueOf(((Map) unitResult.get(Dict.DEMAND_BASEINFO)).get(Dict.OA_CONTACT_NAME));
            }
            List<TaskList> taskList = taskListMapper.queryTaskByNo(workNo);
            if (!Util.isNullOrEmpty(order.getMainTaskNo())) {
                Map sendSub = new HashMap();
                sendSub.put(Dict.ID, order.getMainTaskNo());
                sendSub.put(Dict.REST_CODE, "queryTaskDetail");
                sendSub = (Map) restTransport.submitSourceBack(sendSub);
                addUserIdByRoleName(to, sendSub, Dict.SPDBMASTER);//???????????????
                addUserIdByRoleName(to, sendSub, Dict.MASTER);//???????????????
                addUserIdByRoleName(to, sendSub, Dict.DEVELOPER);//????????????
            }
            if (!Util.isNullOrEmpty(taskList)) {
                for (TaskList t : taskList) {
                    Map sendSub = new HashMap();
                    sendSub.put(Dict.ID, t.getTaskno());
                    sendSub.put(Dict.REST_CODE, "queryTaskDetail");
                    sendSub = (Map) restTransport.submitSourceBack(sendSub);
                    addUserIdByRoleName(to, sendSub, Dict.SPDBMASTER);//???????????????
                    addUserIdByRoleName(to, sendSub, Dict.MASTER);//???????????????
                    addUserIdByRoleName(to, sendSub, Dict.DEVELOPER);//????????????
                    taskNames.add((String) sendSub.get(Dict.NAME));
                }
            }
            Set<String> emails = new HashSet();
            //??????????????????????????????
            String workManager = order.getWorkManager();//???????????????
            String workLeader = order.getGroupLeader();//?????????
            String testers = order.getTesters();//????????????
            Set<String> ftms = new HashSet<>();
            ftms.add(workManager);
            if (!Util.isNullOrEmpty(workLeader)) {
                ftms.addAll(Arrays.asList(workLeader.split(",")));
            }
            if (!Util.isNullOrEmpty(testers)) {
                ftms.addAll(Arrays.asList(testers.split(",")));
            }
            ftms.remove("");
            to.addAll(getFdevUserIdsByEn(ftms));
            for (String users : to) {
                try {
                    Map user = userService.queryUserCoreDataById(users);
                    if (!Util.isNullOrEmpty(user)) {
                        emails.add((String) user.getOrDefault("email", null));
                    }
                } catch (Exception e) {
                    logger.error("id:" + users + "??????????????????");
                }
            }
            if (Util.isNullOrEmpty(emails)) {
                logger.error("no target to send mail");
                throw new FtmsException(ErrorConstants.NO_MAIL_TARGET_ERROR);
            }
            //????????????
            emails.remove("");
            String[] target = emails.toArray(new String[emails.size()]);
            //??????fdev???
            String groupName = "";
            //??????uat?????????
            String uatContact = "";
            String uatContactMail = "";
            String uatContactName = "";
            String jira = "";
            if (!Util.isNullOrEmpty(order.getFdevGroupId())) {
                groupName = "???" + String.valueOf(fdevGroupApi.queryGroupDetail(order.getFdevGroupId()).get(Dict.NAME)) + "???";
                uatContact = groupMapper.queryUatContact(order.getFdevGroupId());
            }
            if (!Util.isNullOrEmpty(uatContact)) {
                Map<String, Object> user = userService.queryUserCoreDataByNameEn(uatContact);
                if (!Util.isNullOrEmpty(user)) {
                    uatContactMail = (String) user.getOrDefault(Dict.EMAIL, null);
                    uatContactName = (String) user.getOrDefault("user_name_cn", null);
                    jira += "??????????????????jira????????????????????????" + uatContactName + "???" + uatContactMail + "???";
                }
            }
            //??????model??????
            HashMap<String, String> model = new HashMap<>();
            model.put(Dict.RQRMNTNO, rqrmntNo);
            model.put(Dict.RQRMNTNAME, rqrmntName);
            model.put(Dict.MAINTASKNAME, order.getMainTaskName());
            model.put(Dict.PLANPRODATE, ifNull(String.valueOf(map.get(Dict.PLANPRODATE)), "???"));
            if (Util.isNullOrEmpty(map.get(Dict.REPAIR_DESC))) {
                model.put(Dict.REPAIR_DESC, "???");
            } else {
                String repairDesc = String.valueOf(map.get(Dict.REPAIR_DESC)).replace("\n", "<br/>");
                model.put(Dict.REPAIR_DESC, MyUtil.replace160Char(repairDesc));
            }
            model.put(Dict.REGRESSIONTESTSCOPE, ifNull(String.valueOf(map.get(Dict.REGRESSIONTESTSCOPE)), "???"));
            model.put(Dict.INTERFACECHANGE, ifNull(String.valueOf(map.get(Dict.INTERFACECHANGE)), "???"));
            model.put(Dict.DATABASECHANGE, ifNull(String.valueOf(map.get(Dict.DATABASECHANGE)), "???"));
            model.put(Dict.CLIENTVERSION, ifNull(String.valueOf(map.get(Dict.CLIENTVERSION)), "???"));
            if (Util.isNullOrEmpty(map.get(Dict.TESTENV))) {
                model.put(Dict.TESTENV, "???");
            } else {
                String repairDesc = String.valueOf(map.get(Dict.TESTENV)).replace("\n", "<br/>");
                model.put(Dict.TESTENV, MyUtil.replace160Char(repairDesc));
            }
            model.put(Dict.APPNAME, ifNull(String.valueOf(map.get(Dict.APPNAME)), "???"));
            model.put(Dict.OTHERSYSTEMCHANGE, ifNull(String.valueOf(map.get(Dict.OTHERSYSTEMCHANGE)), "???"));
            model.put(Dict.DEVELOPER, ifNull(String.valueOf(map.get(Dict.DEVELOPER)), "???"));
            model.put(Dict.TASKLIST, String.join("???", taskNames));
            model.put(Dict.UNIT, order.getUnit());
            if (Util.isNullOrEmpty(map.get(Dict.UATREMARK))) {
                model.put(Dict.UATREMARK, "???");
            } else {
                String repairDesc = String.valueOf(map.get(Dict.UATREMARK)).replace("\n", "<br/>");
                model.put(Dict.UATREMARK, MyUtil.replace160Char(repairDesc));
            }
            model.put(Dict.JIRA, jira);
            //??????
            String file = "";
            if (!Util.isNullOrEmpty(rqrSerialNo)) {
                file = "???????????????????????????  " + fdevTask + "/fdev/#/rqrmn/rqrProfile/" + rqrSerialNo;
            }
            model.put(Dict.FILE, file);
            String subject = groupName + "????????????????????????"
                    + rqrmntNo + "  " + String.valueOf(order.getMainTaskName());
            mailUtil.sendSitReportMail(subject, "ftms_uatInform", model, target);
            //?????????????????????????????????sit_flag???2?????????????????????
            workOrderMapper.updateUatFlag(workNo);
        }
    }

    private void addUserIdByRoleName(Set<String> target, Map source, String role) {
        if (!Util.isNullOrEmpty(source.get(role))) {
            List<Map<String, String>> list = (List<Map<String, String>>) source.get(role);
            for (Map m : list) {
                target.add(String.valueOf(m.get(Dict.ID)));
            }
        }
    }

    /**
     * ???fdev????????????????????????????????????????????????
     *
     * @param target
     * @param source
     * @param role
     */
    private void addTargetByRoleName(Set<String> target, Map source, String role) {
        if (!Util.isNullOrEmpty(source.get(role))) {
            List<Map<String, String>> list = (List<Map<String, String>>) source.get(role);
            for (Map m : list) {
                target.add(m.get(Dict.USER_NAME_EN).toString());
            }
        }
    }

    private String ifNull(String str, String replace) {
        if (Util.isNullOrEmpty(str) || "null".equals(str)) {
            return replace;
        }
        return str;
    }

    /**
     * ??????sit?????????????????????????????????
     *
     * @param map
     * @throws Exception
     */
    @Override
    public void sendSitDoneMail(Map map) throws Exception {
        String workNo = String.valueOf(map.get(Dict.WORKNO));
        String mainTaskName = String.valueOf(map.get(Dict.MAINTASKNAME));
        Set<String> emails = new HashSet();
        if (map.keySet().contains("additionalCCPerson")) {
            List<String> additionalCCPerson = (List<String>) map.get("additionalCCPerson");
            emails.addAll(additionalCCPerson);
        }
        //??????????????????????????????
        WorkOrder order = workOrderMapper.queryWorkOrderByNo(workNo);
        String workManager = order.getWorkManager();//???????????????
        String workLeader = order.getGroupLeader();//?????????
        String testers = order.getTesters();//????????????
        Set<String> ftms = new HashSet<>();
        ftms.add(workManager);
        if (!Util.isNullOrEmpty(workLeader)) {
            ftms.addAll(Arrays.asList(workLeader.split(",")));
        }
        if (!Util.isNullOrEmpty(testers)) {
            ftms.addAll(Arrays.asList(testers.split(",")));
        }
        ftms.remove("");
        String imageLink = String.valueOf(map.getOrDefault(Dict.IMAGELINK, Constants.FAIL_GET));
        if (Util.isNullOrEmpty(imageLink)) {
            imageLink = order.getImageLink();
        }
        //???fdev????????????????????????
        Set<String> to = new HashSet();
        String mainTaskNo = order.getMainTaskNo();
        Map send = new HashMap();
        String rqrmntNo = "";
        String groupName = "";
        String unit = "";
        String rqrmntName = "";
        List<String> taskNames = new ArrayList<>();
        // ?????????????????????????????????????????????????????????
        Set<String> copyToIds = new HashSet<>();
        if (!Util.isNullOrEmpty(mainTaskNo)) {
            //?????????????????????????????????fdev???????????????????????????????????????
            try {
                send.put(Dict.ID, mainTaskNo);
                send.put(Dict.REST_CODE, "queryTaskDetail");
                send = (Map) restTransport.submitSourceBack(send);
                addUserIdByRoleName(to, send, Dict.SPDBMASTER);//???????????????
                addUserIdByRoleName(to, send, Dict.MASTER);//???????????????
                addUserIdByRoleName(to, send, Dict.DEVELOPER);//????????????
                rqrmntNo = String.valueOf(((Map) send.get(Dict.DEMAND)).get(Dict.OA_CONTACT_NO));
                rqrmntName = String.valueOf(((Map) send.get(Dict.DEMAND)).get(Dict.OA_CONTACT_NAME));
                unit = rqrmntNo;
                taskNames.add((String) send.get(Dict.NAME));
            } catch (Exception e) {
                logger.error("fail to query fdev task info" + mainTaskNo + ":" + e);
            }
            if (!Util.isNullOrEmpty(order.getFdevGroupId())) {
                groupName = "???" + String.valueOf(fdevGroupApi.queryGroupDetail(order.getFdevGroupId()).get(Dict.NAME)) + "???";
            }
        } else {
            if ("1".equals(order.getWorkOrderFlag())) {
                if ("1".equals(order.getFdevNew())) {
                    rqrmntNo = order.getDemandNo();
                    rqrmntName = order.getDemandName();
                    String fdevGroupId = order.getFdevGroupId();
                    if (!Util.isNullOrEmpty(fdevGroupId)) {
                        groupName = "???" + fdevGroupApi.queryGroupDetail(fdevGroupId).get(Dict.NAME) + "???";
                    }
                    unit = rqrmntNo;
                    //????????????taskNo???????????????
                    List<TaskList> taskList = taskListMapper.queryTaskByNo(workNo);
                    List<String> taskIds = taskList.stream().map(e -> e.getTaskno()).collect(Collectors.toList());

                    if (!Util.isNullOrEmpty(taskIds)) {
                        for (String taskId : taskIds) {
                            Map<String, Object> task = iTaskApiImpl.getNewTaskById(taskId);
                            if (Util.isNullOrEmpty(task)) {
                                logger.error("order does not come from fdev");
                                throw new FtmsException(ErrorConstants.MAINTASKNO_NOT_EXIST_ERROR);
                            }
                            List<String> assigneeList = (List<String>) task.get("assigneeList");
                            to.add(assigneeList.get(0));
                            to.add((String) task.get("createUserId"));
                            taskNames.add((String) task.get(Dict.NAME));
                        }
                        List<FdevSitMsg> copyTos = fdevSitMsgMapper.queryAllCopyToByTaskIds(taskIds);
                        copyTos.forEach(copyTo -> copyToIds.addAll(Arrays.asList((copyTo.getCopyTo()).split(","))));
                    }
                } else {
                    //??????????????????????????????fdev?????????????????????????????????????????????
                    unit = order.getUnit();
                    Map<String, Object> unitResult = new HashMap<>();
                    try {
                        unitResult = demandService.queryByFdevNoAndDemandId(unit);
                        rqrmntNo = String.valueOf(((Map) unitResult.get(Dict.DEMAND_BASEINFO)).get(Dict.OA_CONTACT_NO));
                        rqrmntName = String.valueOf(((Map) unitResult.get(Dict.DEMAND_BASEINFO)).get(Dict.OA_CONTACT_NAME));
                        String fdevGroupId = String.valueOf(((Map) unitResult.get(Dict.IMPLEMENT_UNIT_INFO)).get(Dict.GROUP));
                        if (!Util.isNullOrEmpty(fdevGroupId)) {
                            groupName = "???" + String.valueOf(fdevGroupApi.queryGroupDetail(fdevGroupId).get(Dict.NAME)) + "???";
                        }
                    } catch (Exception e) {
                        logger.error("fail to query fdev unit info" + order.getUnit() + ":" + e);
                    }
                    //?????????????????????????????????taskNo???????????????
                    try {
                        List<TaskList> taskList = taskListMapper.queryTaskByNo(workNo);
                        if (!Util.isNullOrEmpty(taskList)) {
                            for (TaskList t : taskList) {
                                Map sendSub = new HashMap();
                                sendSub.put(Dict.ID, t.getTaskno());
                                sendSub.put(Dict.REST_CODE, "queryTaskDetail");
                                sendSub = (Map) restTransport.submitSourceBack(sendSub);
                                addUserIdByRoleName(to, sendSub, Dict.SPDBMASTER);//???????????????
                                addUserIdByRoleName(to, sendSub, Dict.MASTER);//???????????????
                                addUserIdByRoleName(to, sendSub, Dict.DEVELOPER);//????????????
                                taskNames.add((String) sendSub.get(Dict.NAME));
                            }
                        }
                    } catch (Exception e) {
                        logger.error("fail to query fdev subtask info");
                    }
                }
            }
        }
        map.put(Dict.RQRMNTNO, ifNull(rqrmntNo, ""));
        map.put(Dict.RQRMNTNAME, ifNull(rqrmntName, ""));
        map.put(Dict.UNIT, ifNull(unit, "???"));

        Set ftmsIds = getFdevUserIdsByEn(ftms);
        to.addAll(copyToIds);
        to.addAll(ftmsIds);
        for (String users : to) {
            try {
                Map<String, Object> user = userService.queryUserCoreDataById(users);
                if (!Util.isNullOrEmpty(user)) {
                    emails.add((String) user.getOrDefault(Dict.EMAIL, null));
                }
            } catch (Exception e) {
                logger.error("id:" + users + "??????????????????");
            }
        }
        String subject = "";//?????????
        String templateName = "";//?????????????????????
        String messageType = "";//??????????????????????????????????????????
        //???????????????????????????????????????????????????
        if (Constants.ORDERTYPE_SECURITY.equals(order.getOrderType())) {
            emails.addAll(addressee);
            //?????????????????????????????????
//            Map<String, String> resultMap = groupMapper.queryAutoWorkOrder(order.getFdevGroupId());
            String groupLeaderStr = order.getGroupLeader();//??????????????????
            if (!CommonUtils.isNullOrEmpty(groupLeaderStr)) {
                List<String> groupLeaderList = Arrays.asList(groupLeaderStr.split(","));
                for (String leaderName : groupLeaderList) {
                    Map<String, Object> userInfo = userService.queryUserCoreDataByNameEn(leaderName);
                    if (!CommonUtils.isNullOrEmpty(userInfo)) {
                        emails.add((String) userInfo.get(Dict.EMAIL));
                    }
                }
            }
            subject = groupName + "????????????????????????" + order.getMainTaskName();
            templateName = "ftms_securityTestReport";
            messageType = "??????????????????";
        }else {
            subject = groupName + "??????????????????" + rqrmntNo + " " + mainTaskName;
            templateName = "ftms_sitReport";
            messageType = "SIT????????????";
        }
        if (Util.isNullOrEmpty(emails)) {
            logger.error("no target to send mail");
            throw new FtmsException(ErrorConstants.NO_MAIL_TARGET_ERROR);
        }
        map.put(Dict.IMAGELINK, Util.isNullOrEmpty(imageLink) ? " " : imageLink);
        map.put(Dict.TASKLIST, String.join("???", taskNames));
        //????????????
        String[] targetEmail = emails.toArray(new String[emails.size()]);
        mailUtil.sendSitReportMail(subject, templateName, (HashMap) map, targetEmail);
        //??????????????????
        Set<String> target = new HashSet<String>();
        //??????????????????????????????
        String userName = myUtil.getCurrentUserEnName();
        target.addAll(ftms);
        target.remove(userName);
        Map messageMap = new HashMap();
        messageMap.put("content", mainTaskName);
        messageMap.put("target", new ArrayList<>(target));
        messageMap.put("type", messageType);
        messageMap.put("hyperlink", "");
        if (!(target == null || target.size() <= 0)) {
            iNotifyApi.sendUserNotify(messageMap);
        }
        workOrderMapper.updateImageLink(workNo, imageLink);
    }

    private Set getFdevUserIdsByEn(Set<String> ftms) throws Exception {
        Set<String> result = new HashSet<>();
        for (String en : ftms) {
            Map<String, Object> user = userService.queryUserCoreDataByNameEn(en);
            if (!Util.isNullOrEmpty(user)) {
                result.add((String) user.get(Dict.ID));
            }
        }
        return result;
    }

    /**
     * fdev?????????????????????tag
     *
     * @param map
     * @throws Exception
     */
    public void fdevSubmitSitTag(Map map) throws Exception {
        String taskNo = String.valueOf(map.get(Dict.JOBNO));
        Map send = new HashMap();
        send.put(Dict.ID, taskNo);
        send.put(Dict.REST_CODE, "queryTaskDetail");
        send = (Map) restTransport.submitSourceBack(send);
        List<String> tagList = (List<String>) send.get(Dict.TAG);
        String tag = "????????????";
        if (!tagList.contains(tag)) {
            tagList.add(tag);
        }
        tagList.remove("????????????");
        tagList.remove("????????????");
        Map sendFdev = new HashMap();
        sendFdev.put(Dict.ID, taskNo);
        sendFdev.put(Dict.TAG, tagList);
        sendFdev.put(Dict.REST_CODE, "updatetaskinner");
        restTransport.submitSourceBack(sendFdev);
    }

    @Override
    public List<FdevSitMsg> queryTaskSitMsg(String taskNo) throws Exception {
        return fdevSitMsgMapper.queryTaskSitMsg(taskNo, Constants.ORDERTYPE_ALL);
    }

    /**
     * ??????????????????????????????
     *
     * @param map
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, String> queryUatMailInfo(Map map) throws Exception {
        Map<String, String> result = new HashMap<>();
        String workNo = String.valueOf(map.get(Dict.WORKNO));
        WorkOrder order = workOrderMapper.queryWorkOrderByNo(workNo);
        //??????????????????????????????????????????fdev??????????????????????????????;????????????????????????????????????
        List<FdevSitMsg> fdevSitMsg = orderUtil.checkFdevTaskAndSitMsg(order);
        //??????????????????
        result.put(Dict.MAINTASKNAME, ifNull(order.getMainTaskName(), "???"));//????????????
        result.put(Dict.UNIT, ifNull(order.getUnit(), "???"));//????????????
        result.put(Dict.PLANPRODATE, ifNull(order.getPlanProDate(), "???"));//??????????????????
        Set<String> repair_desc = new HashSet<>();
        Set<String> regressionTestScope = new HashSet<>();
        Set<String> interfaceChange = new HashSet<>();
        Set<String> databaseChange = new HashSet<>();
        Set<String> clientVersion = new HashSet<>();
        Set<String> testEnv = new HashSet<>();
        Set<String> appName = new HashSet<>();
        Set<String> otherSystemChange = new HashSet<>();
        Set<String> developer = new HashSet<>();
        for (FdevSitMsg fsg : fdevSitMsg) {
            if (!Util.isNullOrEmpty(fsg.getRepairDesc())) {
                repair_desc.add(fsg.getRepairDesc());
            }
            if (!"?????????".equals((fsg.getRegressionTestScope()))) {
                regressionTestScope.add(fsg.getRegressionTestScope());
            }
            if (!"???".equals((fsg.getInterfaceChange()))) {
                interfaceChange.add(fsg.getInterfaceChange());
            }
            if (!"???".equals((fsg.getDatabaseChange()))) {
                databaseChange.add(fsg.getDatabaseChange());
            }
            if (!"?????????".equals((fsg.getClientVersion()))) {
                clientVersion.add(fsg.getClientVersion());
            }
            if (!"?????????".equals((fsg.getTestEnv()))) {
                testEnv.add(fsg.getTestEnv());
            }
            if (!Util.isNullOrEmpty(fsg.getAppName())) {
                appName.add(fsg.getAppName());
            }
            if (!"???".equals(fsg.getOtherSystemChange())) {
                otherSystemChange.add(fsg.getOtherSystemChange());
            }
            if (!Util.isNullOrEmpty(fsg.getDeveloper())) {
                String[] devs = fsg.getDeveloper().split(",");
                for (String dev : devs) {
                    Map<String, Object> user = userService.queryUserCoreDataByNameEn(dev);
                    if (!Util.isNullOrEmpty(user)) {
                        developer.add((String) user.getOrDefault("user_name_cn", ""));
                    }
                }
            }
        }
        result.put(Dict.REPAIR_DESC, ifNull(String.join(";", repair_desc), "???"));//????????????
        result.put(Dict.REGRESSIONTESTSCOPE, ifNull(String.join(";", regressionTestScope), "?????????"));//??????????????????
        result.put(Dict.INTERFACECHANGE, ifNull(String.join(";", interfaceChange), "???"));//????????????
        result.put(Dict.DATABASECHANGE, ifNull(String.join(";", databaseChange), "???"));//???????????????
        result.put(Dict.CLIENTVERSION, ifNull(String.join(";", clientVersion), "?????????"));//???????????????
        result.put(Dict.TESTENV, ifNull(String.join(";", testEnv), "?????????"));//????????????
        result.put(Dict.APPNAME, ifNull(String.join(";", appName), "???"));//???????????????????????????
        result.put(Dict.OTHERSYSTEMCHANGE, ifNull(String.join(";", otherSystemChange), "???"));//????????????????????????
        result.put(Dict.DEVELOPER, ifNull(String.join(";", developer), "???"));//????????????
        return result;
    }

    @Override
    public void getSitSubmitGroup() throws Exception {

    }

    //?????????fdev???????????????
    private Map<String, Object> getNewTaskById(String id) {
        Map send = new HashMap();
        send.put(Dict.ID, id);
        send.put(Dict.REST_CODE, "getTaskById");
        try {
            return (Map<String, Object>) restTransport.submitSourceBack(send);
        } catch (Exception e) {
            logger.error("fail to get new Task info");
        }
        return null;
    }

    @Override
    public String queryLastTransFilePath(String workNo) throws Exception {
        FdevSitMsg fdevSitMsg = fdevSitMsgMapper.queryLastFdevSitMsg(workNo, "");
        if(!CommonUtils.isNullOrEmpty(fdevSitMsg)) {
            return fdevSitMsg.getTransFilePath();
        }
        return null;
    }

    @Override
    public void addSecurityTestTrans(String fdevSitMsgId, List<Map> transList) {
        for (Map<String,String> transInfo : transList) {
            SecurityTestTrans testTrans = new SecurityTestTrans();
            testTrans.setSubmitSitId(Integer.parseInt(fdevSitMsgId));
            testTrans.setTransIndex(transInfo.get(Dict.INDEX));
            testTrans.setTransName(transInfo.get(Dict.TRANSNAME));
            testTrans.setTransDesc(transInfo.get(Dict.TRANSDESC));
            testTrans.setFunctionMenu(transInfo.get(Dict.FUNCTIONMENU));
            fdevSitMsgMapper.addSecurityTestTrans(testTrans);
        }
    }

    @Override
    public List<Map> querySubmitTimelyAndMantis(String startDate, String endDate, List<String> groupIds, Boolean isIncludeChildren) throws Exception {
        //??????????????????????????????????????????????????????
        Map<String, List<Map>> groupMap = null;
        Map<String,String> groupNameMap = null;
        Map<String, List<String>> childGroupIdMap = new HashMap<>();
        Set<String> inChildGroupIds = new HashSet<>();
        if (isIncludeChildren) {
            groupMap = userService.queryChildGroupByIds(groupIds);
            groupNameMap = new HashMap<>();
            for (String key : groupMap.keySet()) {
                List<Map> groupList = groupMap.get(key);
                List<String> childGroupIds = new ArrayList<>();
                for (Map group : groupList) {
                    childGroupIds.add((String)group.get(Dict.ID));
                    groupNameMap.put((String)group.get(Dict.ID), (String) group.get(Dict.NAME));
                }
                childGroupIdMap.put(key, childGroupIds);
                inChildGroupIds.addAll(childGroupIds);
            }
            inChildGroupIds.remove(null);
        } else {
            inChildGroupIds.addAll(groupIds);
            groupNameMap = userService.queryGroupNameByIds(groupIds);
            for (String groupId : groupIds) {
                childGroupIdMap.put(groupId, Arrays.asList(groupId));
            }
        }
        //??????????????????????????????????????????????????????
        List<Map> fdevSitMsgList = fdevSitMsgMapper.querySubmitTime(startDate, endDate, new ArrayList<>(inChildGroupIds));
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.YYYYMMDD_2);
        Date pointStartDate = null;//?????????????????????????????????
        Date pointEndDate = null;//?????????????????????????????????
        Map<String, Object> pointData;//???????????????????????????
        StringBuffer str = null;
        boolean endFlag = true;
        List<Map> result = new LinkedList<>();
        do {
            pointData = new HashMap<>();
            try {
                //??????????????????????????????????????????????????????????????????????????????
                str = new StringBuffer();
                if (pointEndDate == null) {
                    pointStartDate = sdf.parse(startDate);
                } else {
                    pointStartDate = MyUtil.getDateByDayNum(pointEndDate, 1);
                }
                str.append(sdf.format(pointStartDate).substring(0,10).replaceAll("-",""));
                str.append("-");
                pointEndDate = MyUtil.getDateByDayNum(pointStartDate, 6);
                //????????????????????????????????????????????????????????????????????????????????????
                if (pointEndDate.after(sdf.parse(endDate)) || pointEndDate.equals(sdf.parse(endDate))) {
                    pointEndDate = sdf.parse(endDate);
                    endFlag = false;
                }
                str.append(sdf.format(pointEndDate).substring(0,10).replaceAll("-",""));
            } catch (Exception e) {
                throw new FtmsException(ErrorConstants.DATA_QUERY_ERROR, new String[]{"????????????????????????"});
            }
            //?????????????????????
            Map<String, Integer> mantisCountMap = mantisService.countMantisByGroup(sdf.format(pointStartDate), sdf.format(pointEndDate), new ArrayList<>(inChildGroupIds));
            //???????????????????????????????????????
            Map map = new HashMap();
            map.put(Dict.STARTDATE, sdf.format(pointStartDate));
            map.put(Dict.ENDDATE, sdf.format(pointEndDate));
            map.put(Dict.GROUPID, groupIds);
            map.put(Dict.ISPARENT, isIncludeChildren ? "1" : "0");
            List<Map<String, Object>> qualityReportMap = orderDimensionService.qualityReportNew(map, orderDimensionService.cacheQualityReport());
            //?????????id??????????????????????????????
            Map<String, String> timelyMap = new HashMap<>();
            for (Map<String, Object> report : qualityReportMap) {
                timelyMap.put(String.valueOf(report.get(Dict.FDEVGROUPID)), String.valueOf(report.get(Dict.TIMELYRATE)));
            }
            List<Map> groupDataList = new ArrayList<>();//???????????????????????????
            Map<String, String> groupData = null;//??????????????????
            for (String groupId : groupIds) {
                groupData = new HashMap<>();
                int sitTaskCount = 0;//??????????????????
                List<String> childGroupIds = childGroupIdMap.get(groupId);//????????????????????????id??????
                for (Map fdevSitMsg : fdevSitMsgList) {
                    if (!childGroupIds.contains(fdevSitMsg.get(Dict.GROUPID))) {
                        continue;
                    }
                    Date realSitDate = sdf.parse(String.valueOf(fdevSitMsg.get(Dict.REALSITTIME)));
                    //????????????????????????sit??????????????????????????????????????????
                    if ((realSitDate.after(pointStartDate) || realSitDate.equals(pointStartDate))
                            && (realSitDate.before(pointEndDate) || realSitDate.equals(pointEndDate))) {
                        sitTaskCount++;
                    }
                }
                groupData.put(Dict.TASKCOUNT, String.valueOf(sitTaskCount));
                groupData.put(Dict.TIMELYRATE, timelyMap.get(groupId));
                int mantisCount = 0;
                for (String childGroupId : childGroupIds) {
                    mantisCount = mantisCount + (mantisCountMap.get(childGroupId) == null ? 0 : mantisCountMap.get(childGroupId));
                }
                groupData.put(Dict.COUNT_MANTIS, String.valueOf(mantisCount));
                groupData.put(Dict.GROUPNAME, groupNameMap.get(groupId));
                groupDataList.add(groupData);
            }
            pointData.put(str.toString(), groupDataList);
            result.add(pointData);
        } while (endFlag);
        return result;
    }

    @Override
    public List<Map> queryInnerTestData(String demandNo) throws Exception {
        List<Map> result = new ArrayList<>();
        //???????????????????????????
        List<WorkOrder> workOrderList = workOrderMapper.queryWorkOrderByDemandNo(demandNo);
        if (!CommonUtils.isNullOrEmpty(workOrderList)) {
            for (WorkOrder workOrder : workOrderList) {
                workOrder.setGroupLeader(myUtil.changeEnNameToCn(workOrder.getGroupLeader()));
                workOrder.setTesters(myUtil.changeEnNameToCn(workOrder.getTesters()));
                Map dataMap = MyUtil.beanToMap(workOrder);
                dataMap = orderDimensionService.exportSitReportData(workOrder.getWorkOrderNo(), dataMap);
                result.add(dataMap);
            }
        }
        return result;
    }

}
