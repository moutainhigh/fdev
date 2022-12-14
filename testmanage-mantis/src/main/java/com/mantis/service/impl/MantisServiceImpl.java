package com.mantis.service.impl;

import com.mantis.dao.MantisDao;
import com.mantis.dao.MantisUserDao;
import com.mantis.dict.Constants;
import com.mantis.dict.Dict;
import com.mantis.dict.ErrorConstants;
import com.mantis.entity.MantisIssue;
import com.mantis.service.*;
import com.mantis.util.DES3;
import com.mantis.util.MantisRestTemplate;
import com.mantis.util.MyUtil;
import com.mantis.util.Utils;
import com.test.testmanagecommon.cache.LazyInitProperty;
import com.test.testmanagecommon.exception.FtmsException;
import com.test.testmanagecommon.rediscluster.RedisUtils;
import com.test.testmanagecommon.transport.RestTransport;
import com.test.testmanagecommon.util.Util;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;


@Service
@SuppressWarnings("unchecked")
@RefreshScope
public class MantisServiceImpl implements MantisService {
    @Autowired
    private MantisDao mantisDao;
    @Autowired
    private MantisRestTemplate MantisRestTemplate;
    @Value("${manits.issue.url}")
    private String mantis_url;
    @Value("${manits.admin.token}")
    private String mantis_token;
    @Autowired
    private RoleVaildateService roleVaildateService;
    @Autowired
    private MantisFileService mantisFileService;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private MyUtil myUtil;
    @Autowired
    private SendEmailService sendEmailService;
    @Autowired
    private ApiService apiService;
    @Autowired
    private FtmsService ftmsService;
    @Value("${spring.profiles.active}")
    private String env;
    @Value("${user.assessor.role.id}")
    private String assessorRoleId;
    @Value("${user.testManager.role.id}")
    private String testManagerRoleId;
    @Value("${user.testLeader.role.id}")
    private String testLeaderRoleId;
    @Value("${user.tester.role.id}")
    private String testerRoleId;
    @Value("${user.testAdmin.role.id}")
    private String testAdminRoleId;
    @Value("${user.auditer.role.id}")
    private String auditerRoleId;
    @Autowired
    private TaskExecutor taskExecutor;
    @Autowired
    private DES3 des3;
    @Autowired
    private MantisFdevServiceImpl mantisFdevService;
    @Autowired
    private MantisUserDao mantisUserDao;
    @Autowired
    private UserService userService;


    @Autowired
    private RestTransport restTransport;
    private static final Logger logger = LoggerFactory.getLogger(MantisServiceImpl.class);


    @Override
    public Map query(String currentPage, String pageSize, String reporter,
                     String handler, String status, String workNo, String startDate, String endDate,
                     String includeCloseFlag, String id, List<String> groupIds, String redmine_id,
                     String app_name, String project_name, String task_no, String openTimes, String auditFlag, Boolean isIncludeChildren) throws Exception {
        //?????????????????????????????????
        if (!Util.isNullOrEmpty(groupIds) && isIncludeChildren) {
            Map<String, List<Map>> groupMap = userService.queryChildGroupByIds(groupIds);
            groupIds = new ArrayList<>();
            for (List<Map> groupList : groupMap.values()) {
                groupIds.addAll(groupList.stream().map(group -> (String)group.get(Dict.ID)).distinct().collect(Collectors.toList()));
            }
            groupIds.remove(null);
        }
        Integer current_page = Integer.parseInt(currentPage);
        Integer page_size = Integer.parseInt(pageSize);
        Integer start_page = page_size * (current_page - 1);
        Map result = new HashMap();
        List<String> fdevGroupIds = groupIds;
        FutureTask count = new FutureTask(new Callable() {
            @Override
            public Object call() throws Exception {
                int total = mantisDao.count(reporter, handler, status, workNo, startDate, endDate, env, includeCloseFlag, id,
                        fdevGroupIds, redmine_id, app_name, project_name,task_no,openTimes, auditFlag);
                return total;
            }
        });
        FutureTask query = new FutureTask(new Callable() {
            @Override
            public Object call() throws Exception {
                List<MantisIssue> mantisIssues = mantisDao.queryALLissue(start_page, page_size, reporter, handler, status, workNo,
                        startDate, endDate, env, includeCloseFlag, id, fdevGroupIds, redmine_id, app_name, project_name,task_no,openTimes, auditFlag);
                return mantisIssues;
            }
        });
        taskExecutor.execute(count);
        taskExecutor.execute(query);
        try {
            result.put(Dict.TOTAL,count.get());
            result.put(Dict.ISSUES, query.get());
            return result;
        }catch (Exception e){
            logger.error(""+e);
            throw new FtmsException(ErrorConstants.DATA_QUERY_ERROR);
        }
    }

    @Override
    public void update(Map<String, Object> map) throws Exception {
        String id = String.valueOf(map.get(Dict.ID));
        if (!roleVaildateService.userCanEditIssueVaildate(id)) {
            throw new FtmsException(ErrorConstants.NO_ROLE_EDIT_ISSUE);
        }
        String mantis_token = (String) map.get(Dict.MANTIS_TOKEN);
        if (Utils.isEmpty(mantis_token)) {
            throw new FtmsException(ErrorConstants.DO_NOT_HAVE_MANITS_TOKEN);
        }
        //???????????????mantis-token??????
        if (env.equals("sit-new") || env.equals("rel-new") || env.equals("pro-new")){
            mantis_token = des3.decrypt(mantis_token);
        }
        String serverity = (String) map.get(Dict.SEVERITY);
        String priority = (String) map.get(Dict.PRIORITY);
        String summary = (String) map.get(Dict.SUMMARY);
        String description = (String) map.get(Dict.DESCRIPTION);
        Integer project = (Integer) map.get(Dict.PROJECT_ID);
        String handler = (String) map.get(Dict.HANDLER);//????????????
        String handler_en_name = (String) map.get(Dict.HANDLER_EN_NAME);//T-fengwy
        String stage = (String) map.get(Dict.STAGE);//????????????
        String reason = (String) map.get(Dict.REASON); //??????????????????
        String flaw_source = (String) map.get(Dict.FLAW_SOURCE); //????????????
        String system_version = (String) map.get(Dict.SYSTEM_VERSION); //????????????
        system_version = system_version == null ? "" : system_version;
        String developer = (String) map.get(Dict.DEVELOPER);//????????????
        String developer_cn = (String) map.get(Dict.DEVELOPER_CN);//?????????????????????
        String reporter = (String) map.get(Dict.REPORTER);//??????????????????
        String reporter_en_name = (String) map.get(Dict.REPORTER_EN_NAME);//??????????????????
        String plan_fix_date = (String) map.get(Dict.PLAN_FIX_DATE);//??????????????????????????????
        String flaw_type = (String) map.get(Dict.FLAW_TYPE);//????????????
        String status = (String) map.get(Dict.STATUS);
        String appName_en = (String) map.get(Dict.APPNAME_EN);
        String taskNo = (String) map.getOrDefault(Dict.TASK_NO, "");
        String reopen_reason = (String) map.getOrDefault(Dict.REOPEN_REASON, "");
        String url = new StringBuilder(mantis_url).append("/api/rest/issues/").append(id).toString();
        //???????????????????????????,????????????????????????
        if (!Utils.isEmpty(reporter)) {
            if (reporter.equals(developer) || reporter.equals(handler)) {
                throw new FtmsException(ErrorConstants.UPDATE_MANTIS_PERSONNEL_ERROR);
            }
        }
        //???????????? ??????????????????
        Map<String, Object> oldData = queryIssueDetail(id);
        map.put(Dict.OLDMANTISDATA, oldData);

        //????????????:  '10:??????,20:??????,30:????????????,40:????????????,50:??????,80:?????????,90:??????';
        Map<String, Object> currentUserInfoMap = null;
        try {
            //??????????????????
            currentUserInfoMap = redisUtils.getCurrentUserInfoMap();
        } catch (Exception e) {
            throw new FtmsException(ErrorConstants.GET_CURRENT_USER_INFO_ERROR);
        }
        String userEnName = (String)currentUserInfoMap.get(Dict.USER_NAME_EN);
        List<String> role_id = (List<String>)currentUserInfoMap.get(Dict.ROLE_ID);
        //???????????????????????? ?????????????????? ?????????????????? ???????????????
        if (!role_id.contains(testManagerRoleId) && !role_id.contains(testAdminRoleId) && !reporter_en_name.equals(userEnName)) {
            throw new FtmsException(ErrorConstants.NO_ROLE_EDIT_ISSUE);
        }

        //???????????? ????????????
        String oldStatus = (String) oldData.get(Dict.STATUS);
        //????????? ????????? ?????????????????? ??????????????????????????????????????? ??? ????????????
        StringBuilder statusChange = new StringBuilder("issue status:");
        statusChange.append(oldStatus).append("->").append(myUtil.getChStatus(status));
        if (oldStatus.equals("20") && (!status.equals("20") && !status.equals("30") && !status.equals("50") && !status.equals("90"))) {
            logger.error(statusChange.toString());
            throw new FtmsException(ErrorConstants.UPDATE_MANTIS_STATUS_ERROR, new String[]{statusChange.toString()});
        } else if (oldStatus.equals("80") && (!status.equals("80") && !status.equals("90") && !status.equals("50"))) {
            //??????????????? ????????? ?????????????????? ??????????????????????????? ??????????????????
            logger.error(statusChange.toString());
            throw new FtmsException(ErrorConstants.UPDATE_MANTIS_STATUS_ERROR, new String[]{statusChange.toString()});
        } else if ((oldStatus.equals("90") || oldStatus.equals("10") || oldStatus.equals("50") || oldStatus.equals("40") || oldStatus.equals("30")) && !status.equals(oldStatus)) {
            //??????????????? ??????10?????????50???????????????40???????????????30?????????90??? ??????????????????????????????????????????
            logger.error(statusChange.toString());
            throw new FtmsException(ErrorConstants.UPDATE_MANTIS_STATUS_ERROR, new String[]{statusChange.toString()});
        }
        Map<String, Object> sendMap = new HashMap<String, Object>();
        sendMap.put(Dict.SUMMARY, summary);
        sendMap.put(Dict.DESCRIPTION, description);
        sendMap.put(Dict.STATUS, assemblyParamMap(Dict.ID, status));
        sendMap.put(Dict.PROJECT, assemblyParamMap(Dict.ID, project));
        sendMap.put(Dict.HANDLER, assemblyParamMap(Dict.NAME, handler_en_name));
        sendMap.put(Dict.PRIORITY, assemblyParamMap(Dict.ID, priority));
        sendMap.put(Dict.SEVERITY, assemblyParamMap(Dict.ID, serverity));
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        list.add(assemblyCustomMap(3, stage));//'????????????'
        list.add(assemblyCustomMap(4, reason));//'??????????????????'
        list.add(assemblyCustomMap(5, flaw_source));//'????????????'
        list.add(assemblyCustomMap(6, system_version));//'????????????'
        list.add(assemblyCustomMap(65, developer));//'????????????'
        list.add(assemblyCustomMap(9, Utils.dateStrToLong(plan_fix_date)));//'??????????????????????????????'
        list.add(assemblyCustomMap(11, flaw_type));//'????????????'
        list.add(assemblyCustomMap(8, developer_cn));//?????????????????????
        list.add(assemblyCustomMap(72, appName_en));//???????????????
        list.add(assemblyCustomMap(19, taskNo));//???????????????
        list.add(assemblyCustomMap(81, reopen_reason));//??????????????????
        //???????????????????????????????????????
//        if(!Util.isNullOrEmpty(taskNo)) {
//            //???????????????????????????????????????????????????fdev????????????fdev
//            Map sendMap1 = new HashMap();
//            sendMap1.put("taskId", taskNo);
//            sendMap1.put(Dict.REST_CODE, "queryNewFdevByTaskNo");
//            String newFdev = null;
//            try {
//                newFdev = (String) restTransport.submit(sendMap1);
//            } catch (Exception e) {
//                logger.error("fail to query  task workNo");
//            }
//            if ("1".equals(newFdev)) {
//                Map taskDatail = apiService.queryNewTaskInfoByTaskNo(taskNo);
//                if (Util.isNullOrEmpty(taskDatail)){
//                    throw new FtmsException(ErrorConstants.DATA_NOT_EXIST);
//                }
//                String fdev_groupId =(String) taskDatail.get("assigneeGroupId");
//                Map map1 = apiService.queryFdevGroupInfo(fdev_groupId);
//                String groupName =(String) map1.get(Dict.NAME);
//                list.add(assemblyCustomMap(21, groupName));   //??????fdev????????????
//                list.add(assemblyCustomMap(73, fdev_groupId));   //??????fdev??????id
//            } else {
//                Map taskDetail = apiService.queryInfoByTaskNo(taskNo);
//                if (Util.isNullOrEmpty(taskDetail)) {
//                    throw new FtmsException(ErrorConstants.DATA_NOT_EXIST);
//                }
//                Map<String, String> group = (Map<String, String>) taskDetail.get(Dict.GROUP);
//                String fdev_groupId = group.get(Dict.ID);
//                String fdev_groupName = group.get(Dict.NAME);
//                list.add(assemblyCustomMap(21, fdev_groupName));   //??????fdev????????????
//                list.add(assemblyCustomMap(73, fdev_groupId));   //??????fdev??????id
//            }
//        }
        sendMap.put(Dict.CUSTOM_FIELDS, list);
        try {
            MantisRestTemplate.sendPatch(url, mantis_token, sendMap);
        } catch (Exception e) {
            logger.error("======" + e.getMessage());
            throw new FtmsException(ErrorConstants.UPDATE_MANTIS_ISSUES_ERROR);
        }
        sendEmailService.updateMantis(map);

        //?????????????????????????????????????????????????????????fdev???????????????????????????
        Map<String, String> data = new HashMap<>();
        try {
            data = (Map<String, String>) restTransport.submit(restTransport.getUrl("query.ftms.workorder"), String.valueOf(oldData.get(Dict.WORKNO)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ("20".equals(oldStatus) && "50".equals(status)) {
            try {
                Set<String> target = new HashSet<>();
                target.add(handler_en_name);
                Map mm = new HashMap<String, String>();
                target.remove(null);
                //??????fdev????????????
                mm.put(Dict.CONTENT, summary+"???????????????"+"-????????????");
                mm.put(Dict.TARGET, new ArrayList(target));
                mm.put(Dict.DESC, Constants.MANTISNOTICE);//????????????
                mm.put(Dict.TYPE, "2");
            //    mm.put(Dict.HYPERLINK, restTransport.getUrl("fdev.task.detail.page") + data.getOrDefault(Dict.MAINTASKNO, ""));
                mm.put(Dict.REST_CODE, "new.fdev.fnotify.sendUserNotify");
                try {
                    restTransport.submitSourceBack(mm);
                } catch (Exception e) {
                    logger.error("fail to send fdev nofity");
                }
            } catch (Exception e) {
                logger.error("fail to send fdev nofity");
            }
        }
    }

    @Override
    public List<Map<String, String>> countReporterSum(String startDate, String endDate) throws Exception {
        try {
            return mantisDao.countReporterSum(startDate, endDate, env);
        } catch (Exception e) {
            logger.error("======" + e.getMessage());
            throw new FtmsException(ErrorConstants.DATA_QUERY_ERROR);
        }
    }

    @Override
    public Map<String, Object> queryIssueDetail(String id) throws Exception {
        MantisIssue mantisIssue = mantisDao.queryIssueDetail(id);
        String taskId = mantisIssue.getTask_id();//????????????id
        String workNo = mantisIssue.getWorkNo();//???????????????
        Map<String, String> data = (Map<String, String>) restTransport.submit(restTransport.getUrl("query.ftms.workorder"), workNo);
        String mainTaskName = "";//????????????fdev????????????????????????
        if(!Util.isNullOrEmpty(data)){
            mainTaskName = data.get(Dict.MAINTASKNAME);
        }
        String fdevNew = data.get(Dict.FDEVNEW);//????????????????????????
        //???????????????
        if(!Util.isNullOrEmpty(taskId) && !"1".equals(fdevNew)) {
            //???????????????
            Map taskDetail = apiService.queryInfoByTaskNo(taskId);
            if (!Util.isNullOrEmpty(taskDetail)) {
                mantisIssue.setTask_name((String) taskDetail.get(Dict.NAME));
            }
        } else if(!Util.isNullOrEmpty(taskId) && "1".equals(fdevNew)) {
            //???????????????
            Map newTaskInfo = apiService.queryNewTaskInfoByTaskNo(taskId);
            if (!Util.isNullOrEmpty(newTaskInfo)) {
                mantisIssue.setTask_name((String) newTaskInfo.get(Dict.NAME));
            }
        }
        //??????????????????(????????????????????????)
        String solveTime = mantisIssue.getSolve_time();
        if(!Util.isNullOrEmpty(solveTime)){
            double result = (double)Long.parseLong(solveTime) / 86400;
            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMaximumFractionDigits(3);
            mantisIssue.setSolve_time(nf.format(result)+"???");
        }
        List<Map<String, Object>> files = mantisFileService.queryIssueFiles(id);
        Map result = Utils.beanToMap(mantisIssue);
        if (Utils.isEmpty(result)) return null;
        result.put(Dict.FILES, files);
        result.put(Dict.MAINTASKNAME, mainTaskName);
        return result;
    }

    public Map<String, Object> assemblyParamMap(String type, Object value) {
        Map<String, Object> projectMap = new HashMap<String, Object>();
        projectMap.put(type, value);
        return projectMap;
    }

    public Map<String, Object> assemblyCustomMap(Integer id, Object value) {
        Map<String, Object> customMap = new HashMap<String, Object>();
        Map<String, Integer> custom_item = new HashMap<String, Integer>();
        custom_item.put(Dict.ID, id);
        customMap.put(Dict.FIELD, custom_item);
        customMap.put(Dict.VALUE, value);
        return customMap;
    }

    private String getMapValue(Map map, String field, String key) {
        Map field_map = (Map) map.get(field);
        return (String) field_map.get(key);
    }

    @Override
    public void delete(Map<String, String> map) throws Exception {
        String id = map.get(Dict.ID);
        if (!roleVaildateService.userCanEditIssueVaildate(id)) {
            throw new FtmsException(ErrorConstants.NO_ROLE_EDIT_ISSUE);
        }
        String mantis_token = (String) map.get(Dict.MANTIS_TOKEN);
        if (Utils.isEmpty(mantis_token)) {
            throw new FtmsException(ErrorConstants.DO_NOT_HAVE_MANITS_TOKEN);
        }
        //???????????????mantis-token??????
        if (env.equals("sit-new") || env.equals("rel-new") || env.equals("pro-new")){
            mantis_token = des3.decrypt(mantis_token);
        }
        String url = new StringBuilder(mantis_url).append("/api/rest/issues/").append(id).toString();
        try {
            MantisRestTemplate.sendDelete(url, mantis_token);
        } catch (Exception e) {
            logger.error("======" + e.getMessage());
            throw new FtmsException(ErrorConstants.DELETE_MANTIS_ISSUES_ERROR);
        }
    }

    @Override
    public List<MantisIssue> queryIssueByPlanResultId(String id) throws Exception {
        try {
            return mantisDao.queryIssueByPlanResultId(id, env);
        } catch (Exception e) {
            logger.error("======" + e.getMessage());
            throw new FtmsException(ErrorConstants.DATA_QUERY_ERROR);
        }
    }


    @Override
    public void exportMantis(String reporter, String handler, String status, String workNo, String startDate, String endDate, String includeCloseFlag,
                             List<String> groupIds, String redmine_id, String app_name, String project_name, String auditFlag, Boolean isIncludeChildren, HttpServletResponse resp) throws Exception {
        //?????????????????????????????????
        if (!Util.isNullOrEmpty(groupIds) && isIncludeChildren) {
            Map<String, List<Map>> groupMap = userService.queryChildGroupByIds(groupIds);
            groupIds = new ArrayList<>();
            for (List<Map> groupList : groupMap.values()) {
                groupIds.addAll(groupList.stream().map(group -> (String)group.get(Dict.ID)).distinct().collect(Collectors.toList()));
            }
            groupIds.remove(null);
        }
        List<MantisIssue> exportMantis = mantisDao.exportMantis(reporter, handler, status, workNo, startDate, endDate,
                includeCloseFlag, env, groupIds, redmine_id, app_name, project_name, auditFlag);
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet();
            setCellValue(workbook, 0, 0, 0, "????????????");
            setCellValue(workbook, 0, 0, 1, "????????????");
            setCellValue(workbook, 0, 0, 2, "????????????");
            setCellValue(workbook, 0, 0, 3, "?????????");
            setCellValue(workbook, 0, 0, 4, "??????");
            setCellValue(workbook, 0, 0, 5, "??????");
            setCellValue(workbook, 0, 0, 6, "????????????");
            setCellValue(workbook, 0, 0, 7, "????????????");
            setCellValue(workbook, 0, 0, 8, "????????????");
            setCellValue(workbook, 0, 0, 9, "?????????");
            setCellValue(workbook, 0, 0, 10, "????????????");
            setCellValue(workbook, 0, 0, 11, "????????????");
            setCellValue(workbook, 0, 0, 12, "??????");
            setCellValue(workbook, 0, 0, 13, "??????????????????");
            setCellValue(workbook, 0, 0, 14, "??????????????????");
            setCellValue(workbook, 0, 0, 15, "????????????");
            setCellValue(workbook, 0, 0, 16, "?????????");
            int i = 1;
            for (MantisIssue mantis : exportMantis) {
                if (Utils.isEmpty(mantis)) {
                    continue;
                }
                setCellValue(workbook, 0, i, 0, String.valueOf(mantis.getId()));
                setCellValue(workbook, 0, i, 1, String.valueOf(mantis.getRedmine_id()));
                setCellValue(workbook, 0, i, 2, String.valueOf(mantis.getProject_name()));
                setCellValue(workbook, 0, i, 3, String.valueOf(mantis.getWorkNo()));
                setCellValue(workbook, 0, i, 4, String.valueOf(mantis.getSummary()));
                setCellValue(workbook, 0, i, 5, String.valueOf(mantis.getDescription()));
                setCellValue(workbook, 0, i, 6, String.valueOf(mantis.getFlaw_source()));
                setCellValue(workbook, 0, i, 7, String.valueOf(mantis.getDeveloper_cn()));
                setCellValue(workbook, 0, i, 8, String.valueOf(mantis.getHandler()));
                setCellValue(workbook, 0, i, 9, String.valueOf(mantis.getReporter()));
                setCellValue(workbook, 0, i, 10, String.valueOf(mantis.getFdev_group_name()));
                setCellValue(workbook, 0, i, 11, String.valueOf(mantis.getApp_name()));
                setCellValue(workbook, 0, i, 12, String.valueOf(TransformString(mantis.getStatus())));
                setCellValue(workbook, 0, i, 13, String.valueOf(mantis.getReason()));
                setCellValue(workbook, 0, i, 14, String.valueOf(Util.isNullOrEmpty(mantis.getReopen_reason())?"":mantis.getReopen_reason()));
                setCellValue(workbook, 0, i, 15, String.valueOf(mantis.getDate_submitted()));
                setCellValue(workbook, 0, i, 16, MyUtil.getSeverityText(mantis.getSeverity()));
                i++;
            }
        } catch (Exception e) {
            logger.error("e" + e);
            throw new FtmsException(ErrorConstants.EXPORT_EXCEL_ERROR);
        }
        resp.reset();
        resp.setContentType("application/octet-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Content-Disposition", "attachment;filename=" + "MantisList.xlsx");
        workbook.write(resp.getOutputStream());
    }

    /**
     * excel??????
     *
     * @param workbook   excel??????
     * @param sheetIndex
     * @param rowIndex
     * @param cellIndex
     * @param cellValue
     * @throws Exception
     */
    private void setCellValue(Workbook workbook, int sheetIndex, int rowIndex, int cellIndex, String cellValue)
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

    /**
     * '10:??????,20:??????,30:????????????,40:????????????,50:??????,80:?????????,90:??????';
     */
    private String TransformString(String status) {
        String result = "";
        if ("10".equals(status)) {
            result = "??????";
        }
        if ("20".equals(status)) {
            result = "??????";
        }
        if ("30".equals(status)) {
            result = "????????????";
        }
        if ("40".equals(status)) {
            result = "????????????";
        }
        if ("50".equals(status)) {
            result = "??????";
        }
        if ("80".equals(status)) {
            result = "?????????";
        }
        if ("90".equals(status)) {
            result = "??????";
        }
        return result;
    }

    @LazyInitProperty(redisKeyExpression = "tmantis.countWorkOrderSum.${startDate}.${endDate}")
    @Override
    public List<Map<String, String>> countWorkOrderSum(String startDate, String endDate) throws Exception {
        return mantisDao.countWorkOrderSum(startDate, endDate, env);
    }

    @LazyInitProperty(redisKeyExpression = "tmantis.queryWorkOrderIssues.${workNo}.${startDate}.${endDate}")
    @Override
    public Map<String, Integer> queryWorkOrderIssues(String workNo, String startDate, String endDate) throws Exception {
        return mantisDao.queryWorkOrderIssues(workNo, startDate, endDate, env);
    }

    @LazyInitProperty(redisKeyExpression = "tmantis.queryOrderUnderwayIssues.${workNo}")
    @Override
    public Map<String, Integer> queryOrderUnderwayIssues(String workNo) throws Exception {
        return mantisDao.queryOrderUnderwayIssues(workNo, env);
    }

    /**
     * ????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param map
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, String> queryIssueByTimeUser(Map map) throws Exception {
        Map<String, String> issueInfo = new HashMap<>();
        String workNo = String.valueOf(map.get(Dict.WORKNO));
        String userEnName = String.valueOf(map.get(Dict.USER_NAME_EN));
        String startDate = String.valueOf(map.get(Dict.STARTDATE));
        String endDate = String.valueOf(map.get(Dict.ENDDATE));
        try {
            issueInfo = mantisDao.queryIssueByTimeUser(workNo, userEnName, startDate, endDate, env);
        } catch (Exception e) {
            logger.error("fail to queryIssueByTimeUser: " + e);
            throw new FtmsException(ErrorConstants.DATA_QUERY_ERROR);
        }
        return issueInfo;
    }

    /**
     * ????????????????????????????????????
     *
     * @param workNo
     * @param groupIds
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> countIssueDetailByOrderNos(List<String> workNo, String startDate, String endDate, List<String> groupIds) throws Exception {
        //?????????????????????
        String workNos = "";
        if (!Util.isNullOrEmpty(workNo)) {
            workNos = "'" + String.join("','", workNo) + "'";
        }
        //????????????????????????
        Map<String, Object> finalResult = new HashMap<>();
        //sql????????????
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            result = mantisDao.countIssueDetailByOrderNos(workNos, startDate, endDate, env, groupIds);
        } catch (Exception e) {
            logger.error("fail to query issue detail by workNos");
        }
        //??????????????????
        Map<String, Object> workOrders = new HashMap<>();
        Integer rqrRuleNum = 0;//??????????????????
        Integer funcLackNum = 0;//???????????????????????????
        Integer funcErrNum = 0;//????????????????????????
        Integer rqrNum = 0;//????????????????????????
        Integer historyNum = 0;//????????????????????????
        Integer optimizeNum = 0;//??????????????????
        Integer backNum = 0;//??????????????????
        Integer packageNum = 0;//??????????????????
        Integer compatibilityNum = 0;//?????????????????????
        Integer dataNum = 0;//??????????????????
        Integer envNum = 0;//??????????????????
        Integer otherNum = 0;//??????????????????
        Integer sumIssue = 0;//????????????
        Integer effectiveIssue = 0;//????????????
        Integer ineffectiveIssue = 0;//????????????
        Integer devIssue = 0;//???????????????
        Integer solveIssue = 0;//???????????????
        Integer unSolveIssue = 0;//???????????????
        if (!Utils.isEmpty(result)) {
            Map<String, Object> count;
            for (Map<String, Object> order : result) {
                count = new HashMap<>();
                String key = String.valueOf(order.get(Dict.WORKNO));
                count.put(Dict.RQRRULENUM, order.get(Dict.RQRRULENUM));
                count.put(Dict.FUNCLACKNUM, order.get(Dict.FUNCLACKNUM));
                count.put(Dict.FUNCERRNUM, order.get(Dict.FUNCERRNUM));
                count.put(Dict.RQRNUM, order.get(Dict.RQRNUM));
                count.put(Dict.HISTORYNUM, order.get(Dict.HISTORYNUM));
                count.put(Dict.OPTIMIZENUM, order.get(Dict.OPTIMIZENUM));
                count.put(Dict.BACKNUM, order.get(Dict.BACKNUM));
                count.put(Dict.PACKAGENUM, order.get(Dict.PACKAGENUM));
                count.put(Dict.COMPATIBILITYNUM, order.get(Dict.COMPATIBILITYNUM));
                count.put(Dict.DATANUM, order.get(Dict.DATANUM));
                count.put(Dict.ENVNUM, order.get(Dict.ENVNUM));
                count.put(Dict.OTHERNUM, order.get(Dict.OTHERNUM));
                count.put(Dict.SUMISSUE, countCertainTypeIssue(order, Constants.SUMISSUE));
                count.put(Dict.EFFECTIVEISSUE, countCertainTypeIssue(order, Constants.EFFECTIVEISSUE));
                count.put(Dict.INEFFECTIVEISSUE, countCertainTypeIssue(order, Constants.INEFFECTIVEISSUE));
                count.put(Dict.DEVISSUE, countCertainTypeIssue(order, Constants.DEVISSUE));
                count.put(Dict.SOLVEISSUE, order.get(Dict.SOLVEISSUE));
                count.put(Dict.UNSOLVEISSUE, order.get(Dict.UNSOLVEISSUE));
                count.put(Dict.DEVELOPER_CN, order.get(Dict.DEVELOPER_CN));
                workOrders.put(key, count);
                rqrRuleNum += Integer.valueOf(order.get(Dict.RQRRULENUM).toString());
                funcLackNum += Integer.valueOf(order.get(Dict.FUNCLACKNUM).toString());
                funcErrNum += Integer.valueOf(order.get(Dict.FUNCERRNUM).toString());
                rqrNum += Integer.valueOf(order.get(Dict.RQRNUM).toString());
                historyNum += Integer.valueOf(order.get(Dict.HISTORYNUM).toString());
                optimizeNum += Integer.valueOf(order.get(Dict.OPTIMIZENUM).toString());
                backNum += Integer.valueOf(order.get(Dict.BACKNUM).toString());
                packageNum += Integer.valueOf(order.get(Dict.PACKAGENUM).toString());
                compatibilityNum += Integer.valueOf(order.get(Dict.COMPATIBILITYNUM).toString());
                dataNum += Integer.valueOf(order.get(Dict.DATANUM).toString());
                envNum += Integer.valueOf(order.get(Dict.ENVNUM).toString());
                otherNum += Integer.valueOf(order.get(Dict.OTHERNUM).toString());
                solveIssue += Integer.valueOf(order.get(Dict.SOLVEISSUE).toString());
                unSolveIssue += Integer.valueOf(order.get(Dict.UNSOLVEISSUE).toString());
            }
            sumIssue = rqrRuleNum + funcLackNum + funcErrNum + rqrNum + historyNum + optimizeNum +
                    backNum + packageNum + compatibilityNum + dataNum + envNum + otherNum;
            effectiveIssue = rqrRuleNum + funcLackNum + funcErrNum + rqrNum + historyNum + optimizeNum +
                    backNum + packageNum + compatibilityNum;
            ineffectiveIssue = dataNum + envNum + otherNum;
            devIssue = rqrRuleNum + funcLackNum + funcErrNum;
        }
        finalResult.put(Dict.WORKORDERS, workOrders);
        finalResult.put(Dict.SUMISSUE, sumIssue);
        finalResult.put(Dict.EFFECTIVEISSUE, effectiveIssue);
        finalResult.put(Dict.INEFFECTIVEISSUE, ineffectiveIssue);
        finalResult.put(Dict.DEVISSUE, devIssue);
        finalResult.put(Dict.SOLVEISSUE, solveIssue);
        finalResult.put(Dict.UNSOLVEISSUE, unSolveIssue);
        finalResult.put(Dict.RQRRULENUM, rqrRuleNum);
        finalResult.put(Dict.FUNCLACKNUM, funcLackNum);
        finalResult.put(Dict.FUNCERRNUM, funcErrNum);
        finalResult.put(Dict.RQRNUM, rqrNum);
        finalResult.put(Dict.HISTORYNUM, historyNum);
        finalResult.put(Dict.OPTIMIZENUM, optimizeNum);
        finalResult.put(Dict.BACKNUM, backNum);
        finalResult.put(Dict.PACKAGENUM, packageNum);
        finalResult.put(Dict.COMPATIBILITYNUM, compatibilityNum);
        finalResult.put(Dict.DATANUM, dataNum);
        finalResult.put(Dict.ENVNUM, envNum);
        finalResult.put(Dict.OTHERNUM, otherNum);
        return finalResult;
    }

    @Override
    public void batchGroupInfo() throws Exception {
        List<MantisIssue> mantisIssues = mantisDao.queryALLissue(1, 0, "", "", "", "", "", "", env, "0", "", null, "", "", "","","", "");
        for (MantisIssue mantisIssue : mantisIssues) {
            String fdev_group_name = mantisIssue.getFdev_group_name();
            String app_name = mantisIssue.getApp_name();
//            if (Util.isNullOrEmpty(fdev_group_name) && Util.isNullOrEmpty(app_name)) {
                try {
                    String mainTask = apiService.querymainTaskNoByWorkNo(mantisIssue.getWorkNo());
                    if (!Util.isNullOrEmpty(mainTask)) {
                        Map taskInfos = apiService.queryInfoByTaskNo(mainTask);
                        String appName_en = (String) taskInfos.get(Dict.PROJECT_NAME);
                        Map<String, String> group = (Map<String, String>) taskInfos.get(Dict.GROUP);
                        String fdev_groupId = group.get(Dict.ID);
                        String fdev_groupName = group.get(Dict.NAME);
                        Integer mantisId = mantisIssue.getId();

                        //??????????????????????????????
                        String url = new StringBuilder(mantis_url).append("/api/rest/issues/").append(mantisId).toString();
                        Map<String, Object> sendMap = new HashMap<String, Object>();
                        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                        list.add(assemblyCustomMap(21, fdev_groupName));//??????????????????
                        list.add(assemblyCustomMap(72, appName_en));//??????????????????
                        list.add(assemblyCustomMap(73, fdev_groupId));//????????????id
                        sendMap.put(Dict.CUSTOM_FIELDS, list);
                        MantisRestTemplate.sendPatch(url, mantis_token, sendMap);
                    }
                } catch (Exception e) {
                    logger.error("??????????????????" + e.getMessage());
//						throw new FtmsException(ErrorConstants.UPDATE_MANTIS_ISSUES_ERROR);
                }
        }
    }

	@Override
	public List<Map<String, String>> countMantisByWorkNo(String workNo) throws Exception {
        try {
			return mantisDao.countMantisByWorkNo(workNo,env);
		} catch (Exception e) {
			logger.error("e" + e);
			throw new FtmsException(ErrorConstants.DATA_QUERY_ERROR);
		}
	}

    /**
     * ??????fdev??????id??????????????????????????????
     * @param groupIds
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Integer> queryGroupIssueInfo(List<String> groupIds) throws Exception {
        List<Map<String, Object>> list = mantisDao.queryGroupIssueInfo(groupIds, env);
        Map result = new HashMap();
        for (Map<String, Object> map: list) {
            Map data = new HashMap();
            data.put(Dict.FDEV_GROUP_ID, map.get(Dict.FDEV_GROUP_ID));
            data.put(Dict.COUNTMANTIS, map.get(Dict.COUNTMANTIS));
            data.put(Dict.VALIDCOUNTMANTIS, map.get(Dict.VALIDCOUNTMANTIS));
            result.put(map.get(Dict.FDEV_GROUP_ID), data);
        }
        return result;
    }

    /**
     * ????????????
     * @param fdevGroup
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Object> qualityReport(String fdevGroup, String startDate, String endDate) throws Exception {
        Map<String, Object> result = new HashMap<>();
        //????????????
        List<Map<String, String>> effectiveIssue = mantisDao.qualityReport(fdevGroup, startDate, endDate, env);
        //reopen???
        List<Map<String, String>> reopenIssue = mantisDao.reopenIssue(fdevGroup, startDate, endDate, env);
        //????????????????????????
        List<Map<String, String>> solveTime =  mantisDao.solveTime(fdevGroup, startDate, endDate, env);
        result.put(Dict.EFFECTIVEISSUE, effectiveIssue);
        result.put(Dict.REOPENISSUE, reopenIssue);
        result.put(Dict.SOLVETIME, solveTime);
        return result;
    }


    @Override
    public void updateMantis(Map map) {
        String id = String.valueOf(map.get(Dict.ID));
        String workNo = (String) map.get(Dict.WORKNO);
        String unitNo = (String) map.get(Dict.UNITNO);
        String mantis_token = (String) map.get(Dict.MANTIS_TOKEN);
        String url = new StringBuilder(mantis_url).append("/api/rest/issues/").append(id).toString();
        if (Utils.isEmpty(mantis_token)) {
            throw new FtmsException(ErrorConstants.DO_NOT_HAVE_MANITS_TOKEN);
        }
        //???????????????mantis-token??????
        if (env.equals("sit-new") || env.equals("rel-new") || env.equals("pro-new")){
            mantis_token = des3.decrypt(mantis_token);
        }

        if(Util.isNullOrEmpty(workNo) && Util.isNullOrEmpty(unitNo)){
            return;
        }
        Map<String, Object> sendMap = new HashMap<String, Object>();
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        if(!Util.isNullOrEmpty(unitNo)) {
            list.add(assemblyCustomMap(10, unitNo));//'????????????????????????'
        }
        if(!Util.isNullOrEmpty(workNo)) {
            list.add(assemblyCustomMap(13, workNo));
        }
        sendMap.put(Dict.CUSTOM_FIELDS, list);
        try {
            MantisRestTemplate.sendPatch(url, mantis_token, sendMap);
        } catch (Exception e) {
            logger.error("======" + e.getMessage());
            throw new FtmsException(ErrorConstants.UPDATE_MANTIS_ISSUES_ERROR);
        }
    }

    private Integer countCertainTypeIssue(Map<String, Object> order, String type) throws Exception {
		if(!Util.isNullOrEmpty(order)){
			Integer count = 0;
			for(String key : order.keySet()){
				if(type.contains(key)){
					count += Integer.valueOf(order.get(key).toString());
				}
			}
			return count;
		}
		return 0;
	}

    @Override
    public List<Map<String, String>> qualityReportAll() throws Exception {
        List<Map<String, String>> issueInfo = mantisDao.qualityReportAll(env);
        return issueInfo;
    }

    @Override
    public List<Integer> queryMantisIdByTaskNo(String taskNo) {
        return mantisDao.queryMantisIdByTaskNo(taskNo, env);
    }

    @Override
    public void auditMantis(String id, String auditFlag) throws Exception {
        //???????????????????????????????????????
        Map<String, String> auditInfo = mantisDao.queryMantisAuditInfo(id);
        if (!"0".equals(auditInfo.get(Dict.AUDITFLAG))) {
            throw new FtmsException(ErrorConstants.MANTIS_STATUS_REFRESH);
        }
        //?????????????????????
        checkAuditAuthority(id, String.valueOf(auditInfo.get(Dict.HANDLER)));
        if ("1".equals(auditFlag)) {
            //??????????????????
            mantisDao.updateMantisAudit(id, "1");
            //??????????????????
            mantisDao.updateMantisByAudit(id, (String)auditInfo.get(Dict.WANTSTATUS), (String)auditInfo.get(Dict.AUDITREASON), (String)auditInfo.get(Dict.WANTFLAWSOURCE));
            //?????????????????????
            Map<String, String> statusHistory = new HashMap<String, String>(){{
                put(Dict.USER_ID, "743");
                put(Dict.BUG_ID, id);
                put(Dict.FIELD_NAME, "status");
                put(Dict.OLD_VALUE, auditInfo.get(Dict.STATUS));
                put(Dict.NEW_VALUE, auditInfo.get(Dict.WANTSTATUS));
                put(Dict.TYPE, "0");
                put(Dict.DATE_MODIFIED, String.valueOf(System.currentTimeMillis()/1000));
            }};
            Map<String, String> reasonHistory = new HashMap<String, String>(){{
                put(Dict.USER_ID, "743");
                put(Dict.BUG_ID, id);
                put(Dict.FIELD_NAME, "??????????????????");
                put(Dict.OLD_VALUE, auditInfo.get(Dict.REASON));
                put(Dict.NEW_VALUE, auditInfo.get(Dict.AUDITREASON));
                put(Dict.TYPE, "0");
                put(Dict.DATE_MODIFIED, String.valueOf(System.currentTimeMillis()/1000));
            }};
            List<Map> historyList = new ArrayList<Map>(){{
                add(statusHistory);
                add(reasonHistory);
            }};
            mantisDao.addMantisHistory(historyList);
            //?????????????????????????????????,????????????????????????
            //??????????????????????????????
            Map<String, String> mapInfo = mantisDao.queryReportNameAndSummary(id);
            mantisFdevService.sendTestManageNotify(mapInfo.get(Dict.REPORTERENNAME), mapInfo.get(Dict.SUMMARY), id);
        } else {
            //??????????????????
            mantisDao.updateMantisAudit(id, null);
            //???????????????????????????
            mantisDao.deleteFieldString(id, new ArrayList<String>(){{
                add("82");//?????????????????????????????????
                add("83");//??????????????????????????????
                add("84");//??????????????????????????????
            }});
        }

    }

    @Override
    public Boolean checkAuditAuthority(String id, String handlerId) throws Exception {
        Map<String, Object> user = redisUtils.getCurrentUserInfoMap();
        if (Util.isNullOrEmpty(user)) {
            throw new FtmsException(ErrorConstants.GET_CURRENT_USER_INFO_ERROR);
        }
        //?????????????????????????????????????????????????????????
        List<String> roleIds = (List<String>) user.get(Dict.ROLE_ID);
        if (auditerRoleId.equals(roleIds)) {
            throw new FtmsException(ErrorConstants.ROLE_ERROR, new String[] {"????????????-????????????"});
        }
        //????????????id
        String groupId = (String) user.get(Dict.GROUP_ID);
        //???????????????????????????????????????
        Map<String, Object> paramMap = new HashMap<String, Object>(){{
            put(Dict.ID, groupId);
            put(Dict.REST_CODE, "queryChildGroupById");
        }};
        List<Map> groupList = (List<Map>) restTransport.submitSourceBack(paramMap);
        if (!Util.isNullOrEmpty(groupList)) {
            List<String> groupIds = groupList.stream().map(group -> (String)group.get(Dict.ID)).collect(Collectors.toList());
            //???????????????
            if (Util.isNullOrEmpty(handlerId)) {
                //???????????????????????????????????????
                Map<String, String> auditInfo = mantisDao.queryMantisAuditInfo(id);
                handlerId = String.valueOf(auditInfo.get(Dict.HANDLER));
            }
            Map<String, String> mantisUser = mantisUserDao.queryUserById(handlerId);
            String userEmail = mantisUser.get(Dict.EMAIL);
            Map<String, Object> sendMap = new HashMap<String, Object>(){{
                put(Dict.EMAIL, userEmail);
                put(Dict.REST_CODE, "queryUserCoreData");
            }};
            List<Map> userList = (List<Map>) restTransport.submitSourceBack(sendMap);
            if (!Util.isNullOrEmpty(userList)) {
                String handlerGroupId = (String) userList.get(0).get(Dict.GROUP_ID);
                if (groupIds.contains(handlerGroupId)) {
                    return true;
                }else {
                    logger.info(">>>>checkAuditAuthority fail");
                    throw new FtmsException(ErrorConstants.ROLE_ERROR, new String[] {"???????????????????????????"});
                }
            }else {
                logger.info(">>>>checkAuditAuthority fail, userList empty");
                throw new FtmsException(ErrorConstants.ROLE_ERROR, new String[] {"?????????????????????????????????"+userEmail});
            }
        }else {
            logger.info(">>>>checkAuditAuthority fail, groupList empty");
            throw new FtmsException(ErrorConstants.ROLE_ERROR, new String[] {"????????????????????????"});
        }
    }

    @Override
    public Map<String, String> queryAuditMantisInfo(String id) {
        Map<String, String> auditInfo = mantisDao.queryMantisAuditInfo(id);
        if (!Util.isNullOrEmpty(auditInfo)) {
            String handlerId = String.valueOf(auditInfo.get(Dict.HANDLER));
            Map<String, String> mantisUser = mantisUserDao.queryUserById(handlerId);
            auditInfo.put(Dict.HANDLER, mantisUser.get(Dict.REALNAME));
            return auditInfo;
        }
        return new HashMap<>();
    }

    @Override
    public List<Map> countMantisByGroup(String startDate, String endDate, List<String> groupIds) {
        //???????????????????????????????????????,?????????????????????????????????????????????8?????????????????????????????????????????????????????????1???
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_YYYY_MM_DD);
        Calendar calendar = Calendar.getInstance();
        long startTime = 0L;
        long endTime = 0L;
        try {
            calendar.setTime(sdf.parse(startDate));
            startTime = calendar.getTimeInMillis()/1000 - 60*60*8;
            calendar.setTime(sdf.parse(endDate));
            endTime = calendar.getTimeInMillis()/1000 + 60*60*24 - 60*60*8;
        } catch (Exception e) {
            throw new FtmsException(ErrorConstants.MANTIS_FAIL, new String[]{"??????????????????"});
        }
        return mantisDao.countMantisByGroup(startTime, endTime, groupIds, env);
    }

    @Override
    public Map countReporterSumNew(String startDate, String endDate, List<String> userNameEnList) {
        try {
            List<Map<String, String>> mantisList = mantisDao.countReporterSumNew(startDate, endDate, userNameEnList, env);
            if (Util.isNullOrEmpty(mantisList)) {
                return new HashMap();
            } else {
                Set<String> workNos = mantisList.stream().map(mantis -> mantis.get(Dict.WORKNO)).collect(Collectors.toSet());
                List<Map> workOrderList = apiService.queryWorkOrderByNos(workNos, null);
                Map<String, String> orderTypeMap = new HashMap<>();
                for (Map workOrder : workOrderList) {
                    orderTypeMap.put((String)workOrder.get(Dict.WORKORDERNO), (String)workOrder.get(Dict.ORDERTYPE));
                }
                //??????????????????
                Iterator iterator = mantisList.iterator();
                while (iterator.hasNext()) {
                    Map<String, String> mantis = (Map<String, String>) iterator.next();
                    if (Constants.ORDERTYPE_SECURITY.equals(orderTypeMap.get(mantis.get(Dict.WORKNO)))) {
                        iterator.remove();
                    }
                }
                return mantisList.stream().collect(Collectors.groupingBy(mantis -> mantis.get(Dict.USERNAME), Collectors.counting()));
            }
        } catch (Exception e) {
            logger.error("======" + e.getMessage());
            throw new FtmsException(ErrorConstants.DATA_QUERY_ERROR);
        }
    }

    @Override
    public List<Map<String, String>> queryIssueByTimeUserNew(List<String> workNoList, String userEnName, String startDate, String endDate) {
        List<Map<String, String>> issueInfo = new ArrayList<>();
        try {
            issueInfo = mantisDao.queryIssueByTimeUserNew(workNoList, userEnName, startDate, endDate, env);
        } catch (Exception e) {
            logger.error("fail to queryIssueByTimeUserNew: " + e);
            throw new FtmsException(ErrorConstants.DATA_QUERY_ERROR);
        }
        return issueInfo;
    }
}
