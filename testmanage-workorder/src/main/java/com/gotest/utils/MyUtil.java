package com.gotest.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotest.dao.GroupMapper;
import com.gotest.dao.TaskListMapper;
import com.gotest.dao.WorkOrderMapper;
import com.gotest.dict.Constants;
import com.gotest.dict.Dict;
import com.gotest.dict.ErrorConstants;
import com.gotest.domain.TaskList;
import com.gotest.domain.WorkOrder;
import com.gotest.domain.WorkOrderUser;
import com.gotest.service.ITaskApi;
import com.gotest.service.IUserService;
import com.test.testmanagecommon.exception.FtmsException;
import com.test.testmanagecommon.rediscluster.RedisUtils;
import com.test.testmanagecommon.transport.RestTransport;
import com.test.testmanagecommon.util.Util;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RefreshScope
public class MyUtil {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private TaskListMapper taskListMapper;
    @Autowired
    private MailUtil mailUtil;
    @Autowired
    private FdevTransport fdevTransport;
    @Value("${isSendMail}")
    private boolean isSendMail;
    @Value("${spring.profiles.active}")
    private String profile;
    @Autowired
    private GroupMapper groupMapper;
    @Autowired
    private RestTransport restTransport;
    @Autowired
    private IUserService userService;
    @Autowired
    private ITaskApi taskApi;

    @Autowired
    WebApplicationContext applicationContext;

    /**
     *     ???????????????????????????
     */

    public String getProjectUri(String workNo){
        String port = "";
        switch (profile){
            case "sit " : port = "9093";
            break;
            case "rel " : port = "9091";
            break;
            case "pro " : port = "8080";
            break;
            default:port = "";
        }
        StringBuilder uri = new StringBuilder();
        //http://10.134.13.25:9093/tui/?#/Plan?workOrderNo=202005110002
        uri.append("http://10.134.13.25:").append(port).append("/tui/?#/Plan?workOrderNo=").append(workNo);
        return uri.toString();
    }

    public String getCurrentUserEnName(){
        Map<String,Object> userInfo = null;
        String userEnName;
        try {
            userInfo = redisUtils.getCurrentUserInfoMap();
            if(Util.isNullOrEmpty(userInfo)){
                throw new FtmsException(ErrorConstants.GET_CURRENT_USER_INFO_ERROR);
            }
            userEnName = (String)userInfo.get(Dict.USER_NAME_EN);
        } catch (Exception e) {
            throw new FtmsException(ErrorConstants.GET_CURRENT_USER_INFO_ERROR);
        }
        return userEnName;
    }

    /**
     *     ??????????????????YYYYMMDD?????????
     */
    public static String getCurrentDateStr(){
        SimpleDateFormat format = new SimpleDateFormat(Constants.YYYYMMDD);
        String dateStr = format.format(new Date());
        return dateStr;
    }

    public static String getCurrentDateStr_1(){
        SimpleDateFormat format = new SimpleDateFormat(Constants.YYYYMMDD_2);
        String dateStr = format.format(new Date());
        return dateStr;
    }

    /**
     *     ??????????????????YYYYMMDD?????????
     */
    public static String getDateStr(){
        SimpleDateFormat format = new SimpleDateFormat(Constants.YYYYMMDD_1);
        String dateStr = format.format(new Date());
        return dateStr;
    }


    /**
     * ??????????????? ??????WorkOrder ??? ?????????????????????????????????????????????
     *  ?????????????????????????????????
     */
    public List<WorkOrderUser> makeEnNameToChName(List<WorkOrder> userOrderList) throws Exception{
        List<WorkOrderUser> orderList = new ArrayList<WorkOrderUser>();
        WorkOrderUser orderUser;
        for (WorkOrder tempOrder : userOrderList){
            orderUser = new WorkOrderUser();
            //??????????????????
            orderUser.setWorkOrderNo(tempOrder.getWorkOrderNo());
            orderUser.setMainTaskNo(tempOrder.getMainTaskNo());
            orderUser.setMainTaskName(tempOrder.getMainTaskName());
            orderUser.setStage(tempOrder.getStage());
            orderUser.setUnit(tempOrder.getUnit());
            orderUser.setPlanSitDate(tempOrder.getPlanSitDate());
            orderUser.setPlanUatDate(tempOrder.getPlanUatDate());
            orderUser.setPlanProDate(tempOrder.getPlanProDate());
            orderUser.setWorkOrderFlag(tempOrder.getWorkOrderFlag());
            orderUser.setRemark(tempOrder.getRemark());
            orderUser.setCreateTime(tempOrder.getCreateTime());
            orderUser.setField1(tempOrder.getField1());
            orderUser.setField3(tempOrder.getField3());
            orderUser.setGroupId(tempOrder.getGroupId());
            orderUser.setDemandNo(tempOrder.getDemandNo());
            orderUser.setDemandName(tempOrder.getDemandName());
            orderUser.setFdevGroupId(tempOrder.getFdevGroupId());
            int countCase = workOrderMapper.queryOrderHasCase(tempOrder.getWorkOrderNo());
            if(!isNullOrEmpty(tempOrder.getFdevGroupId())){
                Map<String, Object> group = userService.queryGroupDetailById(tempOrder.getFdevGroupId());
                if(!Util.isNullOrEmpty(group)){
                    orderUser.setGroupName((String)group.get(Dict.FULLNAME));
                }
            }
            if (countCase > 0){
                orderUser.setHasCaseFlag("1");
            }else{
                orderUser.setHasCaseFlag("0");
            }
            //????????????????????????
            if(Constants.ORDERTYPE_SECURITY.equals(tempOrder.getOrderType())) {
                TaskList taskList = taskListMapper.queryTaskByTaskNo(tempOrder.getMainTaskNo());
                if(CommonUtils.isNullOrEmpty(taskList)) {
                    orderUser.setHasTaskFlag("0");
                }else {
                    orderUser.setHasTaskFlag("1");
                }
            }else {
                List<TaskList> subTaskList = taskListMapper.queryTaskByNo(tempOrder.getWorkOrderNo());
                if(Util.isNullOrEmpty(subTaskList)){
                    orderUser.setHasTaskFlag("0");
                }else{
                    orderUser.setHasTaskFlag("1");
                }
            }
            orderUser.setField5(tempOrder.getField5());
            orderUser.setRiskDescription(tempOrder.getRiskDescription());
            if(!MyUtil.isNullOrEmpty(tempOrder.getUatSubmitDate())) {
            	orderUser.setUatSubmitDate(OrderUtil.timeStampToStr(tempOrder.getUatSubmitDate(),OrderUtil.dateFormate));
            }
            orderUser.setSitFlag(tempOrder.getSitFlag());
            orderUser.setImageLink(tempOrder.getImageLink());
            makeStrToUser(tempOrder, orderUser);
            Map sendMap = new HashMap();
            sendMap.put(Dict.WORKNO, tempOrder.getWorkOrderNo());
            sendMap.put(Dict.REST_CODE,"mantis.countMantisByWorkNo");
            List resultList = (List)restTransport.submit(sendMap);
            if(resultList.size() > 0){
                orderUser.setMantisFlag("1");
            }
            orderUser.setOrderType(tempOrder.getOrderType());
            orderList.add(orderUser);
        }
        return orderList;
    }

    private static Map<String, Map> userInfo = new HashMap<String, Map>();


    public  String changeEnNameToCn(String nameEn) throws Exception{
        if(!Util.isNullOrEmpty(nameEn)){
            String[] users = nameEn.split(",");
            List<String> nameCns = new ArrayList<>();
            for(String userEnName : users) {
                Map<String, Object> user = userService.queryUserCoreDataByNameEn(userEnName);
                if(!Util.isNullOrEmpty(user)){
                   String userNameCn = (String)user.get(Dict.USER_NAME_CN);
                    if(!CommonUtils.isNullOrEmpty(userNameCn)) {
                        nameCns.add(userNameCn);
                    }
                }
            }
            return  String.join(",",nameCns);
        }
        return null;
    }

    public List<Map> testersStrToUser(WorkOrder workOrder) throws Exception{
        List<Map> userList = new ArrayList<>();
        List<String> users = new ArrayList<>();
        if(!Util.isNullOrEmpty(workOrder.getTesters())){
            users.addAll(Arrays.asList(workOrder.getTesters().split(",")));
        }
        if(!Util.isNullOrEmpty(workOrder.getGroupLeader())){
            users.addAll(Arrays.asList(workOrder.getGroupLeader().split(",")));
        }
        users = users.stream().distinct().collect(Collectors.toList());
        Map<String, String> send = new HashMap<>();
        for(String userEnName : users) {
            Map user = userService.queryUserCoreDataByNameEn(userEnName);
            if(!Util.isNullOrEmpty(user)){
                userList.add(user);
            }
        }
        return userList;
    }

    /**
     * ?????? ????????????????????????????????? FtmsUser??????
     */
    public void makeStrToUser(WorkOrder workOrder, WorkOrderUser orderUser) throws Exception{
        //?????? ???????????????
        Map user = null;
        if (!Util.isNullOrEmpty(workOrder.getWorkManager())) {
            if (userInfo.get(workOrder.getWorkManager()) == null){
                user = userService.queryUserCoreDataByNameEn(workOrder.getWorkManager());
                userInfo.put(workOrder.getWorkManager(),user);
            }
            user = userInfo.get(workOrder.getWorkManager());
            orderUser.setWorkManager(user);
        }else{
            user = new HashMap();
            user.put(Dict.USER_NAME_EN, "");
            orderUser.setWorkManager(user);
        }
        String[] users;
        //?????? ????????????
        orderUser.getGroupLeader().clear();
        if(!Util.isNullOrEmpty(workOrder.getGroupLeader())){
            users = workOrder.getGroupLeader().split(",");
            for(String userEnName : users) {
                if (userInfo.get(userEnName) == null){
                    user = userService.queryUserCoreDataByNameEn(userEnName);
                    userInfo.put(userEnName, user);
                }
                user = userInfo.get(userEnName);
                if (!Util.isNullOrEmpty(user)) {
                    orderUser.getGroupLeader().add(user);
                } else {
                    logger.error("user:" + userEnName + " is not exit");
                }
            }
        }
        //?????? ????????????
        orderUser.getTesters().clear();
        if(!Util.isNullOrEmpty(workOrder.getTesters())){
            users = workOrder.getTesters().split(",");
            for(String userEnName : users) {
                if (userInfo.get(userEnName) == null){
                    user = userService.queryUserCoreDataByNameEn(userEnName);
                    userInfo.put(userEnName, user);
                }
                user = userInfo.get(userEnName);
                if (!Util.isNullOrEmpty(user)) {
                    orderUser.getTesters().add(user);
                }else {
                    logger.error("user:" + userEnName + " is not exit");
                }
            }
        }
        //?????? ????????????
        orderUser.getField2().clear();
        if(!Util.isNullOrEmpty(workOrder.getField2())) {
            users = workOrder.getField2().split(",");
            for (String userEnName : users) {
                if (userInfo.get(userEnName) == null){
                    user = userService.queryUserCoreDataByNameEn(userEnName);
                    userInfo.put(userEnName, user);
                }
                user = userInfo.get(userEnName);
                if (!Util.isNullOrEmpty(user)) {
                    orderUser.getField2().add(user);
                }else {
                    logger.error("user:" + userEnName + " is not exit");
                }
            }
        }
    }


    public void makeUserToCh(WorkOrder tempOrder) throws Exception{
        Map<String, Object> user = userService.queryUserCoreDataByNameEn(tempOrder.getWorkManager());
        tempOrder.setWorkManager((String)user.get(Dict.USER_NAME_CN));
        String [] users;
        if (!Util.isNullOrEmpty(tempOrder.getGroupLeader())) {
            users = tempOrder.getGroupLeader().split(",");
            tempOrder.setGroupLeader(getArrayChName(users));
        } 
        if (!Util.isNullOrEmpty(tempOrder.getTesters())){
            users = tempOrder.getTesters().split(",");
            tempOrder.setTesters(getArrayChName(users));
        }
        if (!Util.isNullOrEmpty(tempOrder.getField2())){
            users = tempOrder.getField2().split(",");
            tempOrder.setField2(getArrayChName(users));
        }
    }

    /**
     * ????????????????????? ???????????????????????? ?????????
     */
    public  String getArrayChName(String[] users){
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < users.length; i++){
            try{
                String userChName = "";
                Map<String, Object> user = userService.queryUserCoreDataByNameEn(users[i]);
                if(!Util.isNullOrEmpty(user)){
                    userChName = (String) user.get(Dict.USER_NAME_CN);
                }
                if(userChName!= null && userChName.length() > 0){
                    buffer.append(userChName);
                } else {
                    buffer.append(users[i]);
                }
            }catch (Exception e){
                buffer.append(users[i]);
            }
            if (i != users.length -1){
                buffer.append(",");
            }
        }
        return  buffer.toString();
    }


    public void sendFastMail(HashMap model, Set<String> person, String key, String taskName) throws Exception {
        if(isSendMail){  //????????????
            Set<String> emails = new HashSet();
            Set<String> fdevEmails = new HashSet<>();
            String workFlag = String.valueOf(model.get(Dict.WORKORDERFLAG));
            String fdevNew = String.valueOf(model.get(Dict.FDEVNEW));
            if (Constants.NUMBER_1.equals(workFlag) && !Constants.NUMBER_1.equals(fdevNew)) {
                //???fdev
                try {
                    fdevEmails = fdevTransport.getUsersEmail(model, (String) model.get(Dict.MAINTASKNO));
                }catch (Exception e){
                	e.printStackTrace();
                    logger.error("fdev???????????????????????????fdev???????????????");
                }
            } else {
                //???fdev
                try {
                    fdevEmails = this.getNewFdevTaskAssigneeEmails((String)model.get(Dict.WORKORDERNO));
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("fdev???????????????????????????fdev???????????????");
                }
            }
            for (String s : person) {
                Map<String, Object> user = userService.queryUserCoreDataByNameEn(s);
                if(!Util.isNullOrEmpty(user)){
                    String email = (String) user.get(Dict.EMAIL);
                    emails.add(email);
                }
            }
            emails.addAll(fdevEmails);
            emails.remove("");
            String[] to = emails.toArray(new String[emails.size()]);
            Map<String, Object> user = redisUtils.getCurrentUserInfoMap();
            if (!Util.isNullOrEmpty(user)){
                model.put(Dict.UPDATETASKUSER, user.get(Dict.USER_NAME_CN));
            }
            model.put(Dict.WORKORDERFLAG, Constants.NUMBER_1.equals(model.get(Dict.WORKORDERFLAG))?"fdev??????":"????????????");
            mailUtil.sendTaskMail(key, model, taskName, to);
        }
    }

    private Set<String> getNewFdevTaskAssigneeEmails(String workOrderNo) throws Exception{
        Set<String> result = new HashSet<>();
        Set<String> userIds = new HashSet<>();
        List<TaskList> subTaskList = taskListMapper.queryTaskByNo(workOrderNo);
        if(!Util.isNullOrEmpty(subTaskList)) {
            List<Map<String, Object>> taskInfos = taskApi.queryNewTaskDetail(subTaskList.stream().map(TaskList::getTaskno).collect(Collectors.toList()));
            taskInfos.stream().forEach(e->{
                userIds.addAll((List<String>)e.get("assigneeList"));
            });
        }
        userIds.stream().forEach(e->{
            try {
                result.add((String)userService.queryUserCoreDataById(e).get(Dict.EMAIL));
            } catch (Exception e1) {
                logger.error("fail to query user info for :" + e);
            }
        });
        return result;
    }


    public static Map beanToMap(Object cls) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(cls, Map.class);
    }

    /**
     * ????????????Object???????????? ???????????????List ,Map ,String add by linsx 2016-3-9
     *
     * @param obj
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static boolean isNullOrEmpty(Object obj) {
        if (obj instanceof Object[]) {
            Object[] o = (Object[]) obj;
            if (o == null || o.length == 0) {
                return true;
            }
            return false;
        } else {
            if (obj instanceof String) {
                if ((obj == null) || (("").equals(((String) obj).trim()))) {
                    return true;
                }
                return false;
            }
            if (obj instanceof List) {
                List objList = (List) obj;
                if (objList == null || objList.isEmpty()) {
                    return true;
                }
                return false;
            }
            if (obj instanceof Map) {
                Map objMap = (Map) obj;
                if (objMap == null || objMap.isEmpty()) {
                    return true;
                }
                return false;
            }
            if ((obj == null) || (("").equals(obj))) {
                return true;
            }
        }
        return false;
    }

    public static List getTaskPersonNameEn(Map task){
        Set set = new HashSet();
        List master = (List) task.get("master");
        List spdb_master = (List) task.get("spdb_master");
        List developer = (List) task.get("developer");
        set.addAll(master);
        set.addAll(spdb_master);
        set.addAll(developer);
        List resultList = new ArrayList();
        if(!Util.isNullOrEmpty(set)) {
            resultList = new ArrayList<String>();
            for(Object item : set) {
                resultList.add((String) ((Map)item).get("user_name_en"));
            }
        }
        return  resultList;
    }

    /**
     * excel??????
     *
     * @param workbook
     *            excel??????
     * @param sheetIndex
     * @param rowIndex
     * @param cellIndex
     * @param cellValue
     * @throws Exception
     */
    public static void setCellValue(Workbook workbook, int sheetIndex, int rowIndex, int cellIndex, String cellValue)
            throws Exception {
        Sheet sheet = workbook.getSheetAt(sheetIndex);
        if (sheet == null) {
            sheet = workbook.createSheet(String.valueOf(sheetIndex));
        }
        if (sheet.getRow(rowIndex) == null) {
            sheet.createRow(rowIndex);
        }
        if (sheet.getRow(rowIndex).getCell(cellIndex) == null) {
            sheet.getRow(rowIndex).createCell(cellIndex);
        }
        sheet.getRow(rowIndex).getCell(cellIndex).setCellValue(cellValue);
    }

    public static String replaceSpecialStr(String str){
        String rep = "";
        if(!Util.isNullOrEmpty(str)){
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            rep = m.replaceAll("");
        }
        return rep;
    }

    /**
     * ????????????????????????unicode???160
     * @param str
     * @return
     */
    public static String replace160Char(String str){
        return str.replace("\u00a0","&nbsp;");
    }

    /**
     * ??????fdev??????
     * @param order
     * @throws Exception
     */
    public void addFdevGroupName(WorkOrder order,Map<String,String> groupNameMap) throws Exception {
        Map send;
        if("1".equals(order.getWorkOrderFlag())){
            if(Util.isNullOrEmpty(order.getFdevGroupName())){
                send = new HashMap();
                if(Util.isNullOrEmpty(order.getFdevGroupId())){
                    if(!Util.isNullOrEmpty(order.getMainTaskNo())){
                        send.put(Dict.ID, order.getMainTaskNo());
                        send.put(Dict.REST_CODE, "queryTaskDetail");
                        try {
                            send = (Map)restTransport.submitSourceBack(send);
                            order.setFdevGroupName(String.valueOf(((Map)(send.get(Dict.GROUP))).get(Dict.NAME)));
                            String fdevGroupId = String.valueOf(((Map)(send.get(Dict.GROUP))).get(Dict.ID));
                            workOrderMapper.setOrderFdevGroupId(order.getWorkOrderNo(), fdevGroupId);
                        } catch (Exception e) {
                            logger.error("fail to add fdev group name");
                        }
                    }
                }else{
                    order.setFdevGroupName(groupNameMap.get(order.getFdevGroupId()));
                }
            }
        }
    }

    /**
     * ??????????????????fdev??????????????????
     * @param key
     * @throws Exception
     */
    public String getFdevNameCn(Map<String, Object> taskInfo, String key) throws Exception {
        List<String> nameCn = new ArrayList<>();
        List<Map<String, String>> list = (List<Map<String, String>>)taskInfo.get(key);
        for(Map<String, String> person : list){
            nameCn.add(person.get(Dict.USER_NAME_CN));
        }
        return String.join(",", nameCn);
    }

    /**
     * ???????????? status ????????????
     */
    public String getChStatus(String priority) {
        if (priority == null)
            return "";
        switch (priority) {
            case "10":
                return "??????";
            case "20":
                return "??????";
            case "30":
                return "????????????";
            case "40":
                return "????????????";
            case "50":
                return "??????";
            case "80":
                return "?????????";
            case "90":
                return "??????";
        }
        return priority;
    }

    public String restoreDate(String planSitDate) throws Exception {
        if(Util.isNullOrEmpty(planSitDate)){
            return null;
        }
        String date = "";
        try {
            date = planSitDate.substring(0,4) + "-" + planSitDate.substring(4,6) + "-" + planSitDate.substring(6,8);
        } catch (Exception e) {
            return planSitDate;
        }
        return date;
    }

    public  String isNewFtms() {
        if (applicationContext.getServletContext().getContextPath().contains("-new")) {
            return "1";
        }
        return "0";
    }

    /**
     * ???????????????map??????????????????
     * @param userId
     * @param userMap
     * @return
     */
    public String getUserName(Object userId, Map<String, Map> userMap) {
        if(userId instanceof List) {
            if(CommonUtils.isNullOrEmpty(userId)) {
                return "";
            }
            List<String> ids = (List<String>) userId;
            StringBuffer userName = new StringBuffer();
            for (String id : ids) {
                if(!CommonUtils.isNullOrEmpty(userMap.get(id))) {
                    Map<String, Object> userInfo = userMap.get(id);
                    userName.append(userInfo.get(Dict.USER_NAME_CN));
                    userName.append(",");
                }
            }
            String name = userName.toString();
            if (name.endsWith(",")) {
                name = name.substring(0, name.length() - 1);
            }
            return name;
        }
        return "";
    }

    /**
     * ???????????????map??????????????????
     * @param groupId
     * @param groupMap
     * @return
     */
    public String getGroupName(String groupId, Map<String, Map> groupMap) {
        if(CommonUtils.isNullOrEmpty(groupId)) {
            return "";
        }
        Map<String, Object> groupInfo = groupMap.get(groupId);
        if(!CommonUtils.isNullOrEmpty(groupInfo)) {
            return (String) groupInfo.get(Dict.NAME);
        }else {
            return "";
        }
    }

    /**
     * ????????????????????????????????????????????????0
     * @param str
     * @return
     */
    public static String formatPercent(String str) {
        return new BigDecimal(str).multiply(new BigDecimal(100)).stripTrailingZeros().toPlainString() + "%";
    }

    /**
     * ?????????????????????????????????
     * @param severity
     * @return
     */
    public static String getSeverityNameCH(String severity) {
        switch (severity) {
            case "10" :
                return "?????????";
            case "20" :
                return "??????";
            case "30" :
                return "??????";
            case "40" :
                return "?????????";
            case "50" :
                return "?????????";
            case "60" :
                return "?????????";
            case "70" :
                return "??????";
            case "80" :
                return "??????";
            default:
                return severity;
        }
    }

    /**
     * ?????????????????????????????????
     * @param priority
     * @return
     */
    public static String getPriorityNameCH(String priority) {
        switch (priority) {
            case "10" :
                return "???";
            case "20" :
                return "???";
            case "30" :
                return "???";
            case "40" :
                return "???";
            case "50" :
                return "??????";
            case "60" :
                return "????????????";
            default:
                return priority;
        }
    }

    public static String setReportDataAndCloum(List<Map> data, LinkedHashMap<String, String> cloumMap, Map<String, Object> qualityReportNewUnit, String reportType) {
        switch (reportType) {
            case Constants.REPORTTYPE_TIMELYRATE:
                data.addAll((List<Map>) qualityReportNewUnit.get(Dict.SUBMITINFO));
                cloumMap.put(Dict.TASKNAME, "?????????");
                cloumMap.put(Dict.ORDERNAME, "?????????");
                cloumMap.put(Dict.PLANSITDATE, "????????????sit??????");
                cloumMap.put(Dict.SPDBMASTER, "???????????????");
                cloumMap.put(Dict.MASTER, "?????????");
                cloumMap.put(Dict.DEVELOPER, "????????????");
                cloumMap.put(Dict.GROUPNAME, "?????????");
                cloumMap.put(Dict.STAGE, "????????????");
                cloumMap.put(Dict.REALSITTIME, "????????????");
                cloumMap.put(Dict.TIMELY, "????????????");
                return "?????????????????????";
            case Constants.REPORTTYPE_SMOKERATE:
                data.addAll((List<Map>) qualityReportNewUnit.get(Dict.ROLLBACKINFO));
                cloumMap.put(Dict.ORDERNAME, "?????????");
                cloumMap.put(Dict.PLANSITDATE, "????????????sit??????");
                cloumMap.put(Dict.ROLLBACKTIME, "????????????");
                cloumMap.put(Dict.ROLLBACKOPR, "?????????");
                cloumMap.put(Dict.TASKNAME, "?????????");
                cloumMap.put(Dict.SPDBMASTER, "???????????????");
                cloumMap.put(Dict.MASTER, "?????????");
                cloumMap.put(Dict.DEVELOPER, "????????????");
                cloumMap.put(Dict.GROUPNAME, "?????????");
                cloumMap.put(Dict.STAGE, "????????????");
                cloumMap.put(Dict.REASON, "????????????");
                return "???????????????????????????";
            case Constants.REPORTTYPE_REOPENRATE:
                data.addAll((List<Map>) qualityReportNewUnit.get(Dict.REOPENISSUE));
                cloumMap.put(Dict.ID, "??????id");
                cloumMap.put(Dict.TASKNAME, "?????????");
                cloumMap.put(Dict.FDEVGROUPID, "?????????");
                cloumMap.put(Dict.SUMMARY, "????????????");
                cloumMap.put(Dict.STATUS, "????????????");
                cloumMap.put(Dict.REPORTERNAME, "???????????????");
                cloumMap.put(Dict.DEVELOPER, "???????????????");
                cloumMap.put(Dict.HANDLERNAME, "?????????");
                cloumMap.put(Dict.OPENTIMES, "????????????");
                return "??????reopen?????????";
            case Constants.REPORTTYPE_NORMALAVGTIME:
                data.addAll((List<Map>) qualityReportNewUnit.get(Dict.SOLVETIMERECORD));
                cloumMap.put(Dict.ID, "??????id");
                cloumMap.put(Dict.TASKNAME, "?????????");
                cloumMap.put(Dict.FDEVGROUPID, "?????????");
                cloumMap.put(Dict.SUMMARY, "????????????");
                cloumMap.put(Dict.STATUS, "????????????");
                cloumMap.put(Dict.REPORTERNAME, "???????????????");
                cloumMap.put(Dict.DEVELOPER, "???????????????");
                cloumMap.put(Dict.HANDLERNAME, "?????????");
                cloumMap.put(Dict.SOLVETIME, "????????????");
                return "??????????????????????????????";
            case Constants.REPORTTYPE_SEVAVGTIME:
                data.addAll((List<Map>) qualityReportNewUnit.get(Dict.SEVERESOLVETIMERECORD));
                cloumMap.put(Dict.ID, "??????id");
                cloumMap.put(Dict.TASKNAME, "?????????");
                cloumMap.put(Dict.FDEVGROUPID, "?????????");
                cloumMap.put(Dict.SUMMARY, "????????????");
                cloumMap.put(Dict.STATUS, "????????????");
                cloumMap.put(Dict.SEVERITY, "?????????");
                cloumMap.put(Dict.PRIORITY, "?????????");
                cloumMap.put(Dict.REPORTERNAME, "???????????????");
                cloumMap.put(Dict.DEVELOPER, "???????????????");
                cloumMap.put(Dict.HANDLERNAME, "?????????");
                cloumMap.put(Dict.SOLVETIME, "????????????");
                return "??????????????????????????????";
            default:
                return "??????????????????";
        }
    }

    /**
     * ??????Map???List<Map>?????????????????????
     * @param user
     * @return
     */
    public static String getNameByUser(Object user) {
        if(CommonUtils.isNullOrEmpty(user)) {
            return "";
        }
        if (user instanceof List) {
            List<Map> userList = (List<Map>) user;
            StringBuffer userName = new StringBuffer();
            for (Map userInfo : userList) {
                userName.append(userInfo.get(Dict.USER_NAME_CN));
                userName.append(",");
            }
            String name = userName.toString();
            if (name.endsWith(",")) {
                name = name.substring(0, name.length() - 1);
            }
            return name;
        }
        if (user instanceof Map) {
            Map userInfo = (Map) user;
            return (String) userInfo.get(Dict.USER_NAME_CN);
        }
        return "";
    }

    /**
     * ?????????????????????????????????????????????
     * @return
     */
    public static Date getDateByDayNum(Date startDate, int num) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.DATE, num);
        return calendar.getTime();
    }

    /**
     * ????????????
     * @param reason
     * @return
     */
    public static String mapReason(String reason) {
        switch (reason) {
            case "1":
                reason = Constants.REASON_1;
                break;
            case "2":
                reason = Constants.REASON_2;
                break;
            case "3":
                reason = Constants.REASON_3;
                break;
            default:
                reason = "";
        }
        return reason;
    }
}
