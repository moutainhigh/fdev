package com.gotest.service.serviceImpl;

import com.gotest.dao.RollbackInfoMapper;
import com.gotest.dao.TaskListMapper;
import com.gotest.dao.WorkOrderMapper;
import com.gotest.dict.Constants;
import com.gotest.dict.Dict;
import com.gotest.dict.ErrorConstants;
import com.gotest.domain.RollbackInfo;
import com.gotest.domain.WorkOrder;
import com.gotest.service.IFdevGroupApi;
import com.gotest.service.INotifyApi;
import com.gotest.service.ITaskApi;
import com.gotest.service.RollbacksInfoService;
import com.gotest.utils.MyUtil;
import com.test.testmanagecommon.exception.FtmsException;
import com.test.testmanagecommon.transport.RestTransport;
import com.test.testmanagecommon.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Service
public class RollbackInfoImpl implements RollbacksInfoService {
    @Autowired
    private RollbackInfoMapper rollbackInfoMapper;
    @Autowired
    private RestTransport restTransport;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private INotifyApi iNotifyApi;
    @Autowired
    private IFdevGroupApi fdevGroupApi;
    @Autowired
    private MyUtil myUtil;
    @Autowired
    private ITaskApi taskApi;
    @Autowired
    private TaskListMapper taskListMapper;

    private static Logger logger = LoggerFactory.getLogger(RollbackInfoImpl.class);

    @Override
    @Transactional
    public void workOrderRollback(WorkOrder workOrder, String userName, String reason, String detailInfo, String taskId) throws Exception {
        String fdevNew = workOrder.getFdevNew();
        String dateStr = MyUtil.getCurrentDateStr_1();//??????????????????
        //??????????????????????????????????????????
        Map task;
        if (Constants.NUMBER_1.equals(fdevNew)) {
            //?????????fdev??????
            task = taskApi.queryNewTaskDetail(Arrays.asList(taskId)).get(0);
        } else {
            //?????????fdev??????
            task = (Map) taskApi.queryTaskDetailByIds(Arrays.asList(taskId)).get(taskId);
        }
        if (MyUtil.isNullOrEmpty(task)) {
            throw new FtmsException(ErrorConstants.DATA_NOT_EXIST, new String[]{"fdev????????????????????????"});
        }
        String fdevGroupId = Constants.NUMBER_1.equals(fdevNew) ?
                (String) task.get(Dict.ASSIGNEEGROUPID) : (String) ((Map) task.get(Dict.GROUP)).get(Dict.ID);
        RollbackInfo rollbackInfo = new RollbackInfo();
        rollbackInfo.setDate(dateStr);
        rollbackInfo.setFdevGroupId(fdevGroupId);
        rollbackInfo.setWorkNo(workOrder.getWorkOrderNo());
        rollbackInfo.setReason(reason);
        rollbackInfo.setRollbcakOpr(userName);
        rollbackInfo.setMainTaskName(workOrder.getMainTaskName());
        rollbackInfo.setFdevTaskNo(taskId);
        rollbackInfo.setDetailInfo(detailInfo);
        //??????????????????
        rollbackInfoMapper.addRollbackInfo(rollbackInfo);
        //????????????????????? ?????????  ???????????? ??????????????????  ??????????????????????????????
        WorkOrder order = new WorkOrder();
        order.setWorkOrderNo(workOrder.getWorkOrderNo());
        order.setStage("1");
        order.setFnlRollbackDate(dateStr);
        order.setSitFlag("0");
        workOrderMapper.updateWorkOrder(order);
        //????????????  for fdev??????????????????
        Map message = new HashMap();
        List taskPersonNameEns = !Constants.NUMBER_1.equals(fdevNew) ?
                MyUtil.getTaskPersonNameEn(task) : taskApi.queryNewTaskAssignEns(task);
        message.put(Dict.TARGET, taskPersonNameEns);
        message.put(Dict.CONTENT, task.get(Dict.NAME) + ":" + reason + "-" + "????????????" + "??????????????????" + detailInfo);
        message.put(Dict.DESC, "????????????");
        message.put(Dict.TYPE, "2");
        try {
            iNotifyApi.sendUserNotifyForFdev(message, fdevNew);
        } catch (Exception e) {
            logger.error("ftms notify send error: submit test rollback");
        }
        //fdev???????????????fdev??????tag
        if (!Constants.NUMBER_1.equals(fdevNew)) {
            String tag = "????????????";
            List<String> tagList = (List<String>) task.get(Dict.TAG);
            if (!tagList.contains(tag)) {
                tagList.add(tag);
            }
            boolean a = true;
            tagList.remove("????????????");
            tagList.remove("????????????");
            //?????????
            try {
                //???fdev????????????update????????????tag
                Map sendFdev = new HashMap();
                sendFdev.put(Dict.ID, taskId);
                sendFdev.put(Dict.TAG, tagList);
                sendFdev.put(Dict.REST_CODE, "updatetaskinner");
                restTransport.submitSourceBack(sendFdev);
            } catch (Exception e) {
                logger.error("fail to update fdev tag");
            }
        } else {
            //??????fdev????????????????????????????????????
            taskApi.changeTestComponentStatus(taskId, "3");
        }
    }

    @Override
    public List queryRollbackReport(String mainTaskName, String groupId, String startDate, String endDate, String childGroupFlag, String developer, String reason) throws Exception {
        if ("1".equals(childGroupFlag)) {//?????????????????????
            List<String> groups = fdevGroupApi.getChildGroupId(groupId);
            groupId = StringUtils.join(groups, ",");
        }
        List<Map<String, Object>> dateMap = rollbackInfoMapper.countOrderRollback(mainTaskName, groupId, startDate, endDate, reason);
        Map<String, List<Map<String, Object>>> result = totalDataChange(dateMap);
        List list = new ArrayList(result.values());
        if (!MyUtil.isNullOrEmpty(developer)) {
            list = developerScreening(list, developer);
        }
        return list;
    }

    private List developerScreening(List<Map> list, String developer) throws Exception {
        List newList = new ArrayList();
        for (Map map : list) {
            if (map.get(Dict.DEVELOPERNAMEEN).toString().contains(developer)) {
                newList.add(map);
            }
        }
        return newList;
    }

    public Map<String, List<Map<String, Object>>> totalDataChange(List<Map<String, Object>> list) throws Exception {
        Map dateMap = new HashMap();
        for (Map<String, Object> map : list) {
            if (MyUtil.isNullOrEmpty(dateMap.get(map.get(Dict.WORKNO)))) {
                List<Map<String, String>> rollbacks = new ArrayList<>();
                //??????fdev??????????????????
                mapData(map);
                Map<String, String> rollbackDetail = new HashMap();
                rollbackDetail.put(Dict.REASON, MyUtil.mapReason((String) map.get(Dict.REASON)));
                rollbackDetail.put(Dict.DATE, (String) map.get(Dict.DATE));
                rollbackDetail.put(Dict.ROLLBACKOPR, (String) map.get(Dict.ROLLBACKOPR));
                rollbackDetail.put(Dict.DETAILINFO, (String) map.getOrDefault(Dict.DETAILINFO, Constants.FAIL_GET));
                rollbacks.add(rollbackDetail);
                map.put(Dict.DETAIL, rollbacks);
                dateMap.put(map.get(Dict.WORKNO), map);
            } else {
                mapData(map);
                Map<String, Object> rollbackMap = (Map<String, Object>) dateMap.get(map.get(Dict.WORKNO));
                List<Map<String, String>> rollbacks = (List<Map<String, String>>) rollbackMap.get(Dict.DETAIL);
                Map<String, String> rollbackDetail = new HashMap();
                rollbackDetail.put(Dict.REASON, MyUtil.mapReason((String) map.get(Dict.REASON)));
                rollbackDetail.put(Dict.DATE, (String) map.get(Dict.DATE));
                rollbackDetail.put(Dict.ROLLBACKOPR, (String) map.get(Dict.ROLLBACKOPR));
                rollbackDetail.put(Dict.DETAILINFO, (String) map.getOrDefault(Dict.DETAILINFO, Constants.FAIL_GET));
                rollbacks.add(rollbackDetail);
            }
        }
        return dateMap;
    }

    public Map mapData(Map<String, Object> map) throws Exception {
        Map group = fdevGroupApi.queryGroupDetail((String) map.get(Dict.GROUPID));
        //??????????????????
        Object fstSitDate = map.get(Dict.FSTSITDATE);
        map.put(Dict.FSTSITDATE, fstSitDate = MyUtil.isNullOrEmpty(fstSitDate) ? "" : fstSitDate);
        //??????????????????
        Object fnlRollbackDate = map.get(Dict.FNLROLLBACKDATE);
        map.put(Dict.FNLROLLBACKDATE, fnlRollbackDate = MyUtil.isNullOrEmpty(fnlRollbackDate) ? "" : fnlRollbackDate);
        if (!MyUtil.isNullOrEmpty(group)) {
            map.put(Dict.GROUPNAME, (String) group.get(Dict.NAME));
        }
        //???????????????????????????
        String testsers = String.valueOf(map.get(Dict.TESTERS));
        String testsersNameCn = myUtil.getArrayChName(testsers.split(","));
        map.put(Dict.TESTERS, testsersNameCn);
        if (!MyUtil.isNullOrEmpty(map.get(Dict.MAINTASKNO))) {
            String developNameCns = taskApi.queryTaskDevelopNameCns(String.valueOf(map.get(Dict.MAINTASKNO)), Dict.USERNAMECN);
            map.put(Dict.DEVELOPER, developNameCns);
            String developNameEns = taskApi.queryTaskDevelopNameCns(String.valueOf(map.get(Dict.MAINTASKNO)), Dict.USER_NAME_EN);
            map.put(Dict.DEVELOPERNAMEEN, developNameEns);
        }
        map.put(Dict.ROLLBACKOPR, myUtil.getArrayChName(String.valueOf(map.get(Dict.ROLLBACKOPR)).split(",")));
        return map;
    }

    @Override
    public void exportRollbackReport(String mainTaskName, String groupId, String startDate, String endDate, String childGroupFlag, HttpServletResponse resp, String developer, String reason) throws Exception {
        List<Map> totalData = queryRollbackReport(mainTaskName, groupId, startDate, endDate, childGroupFlag, developer, reason);
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet();
            int i = 0;
            for (Map rollbackInfo : totalData) {
                MyUtil.setCellValue(workbook, 0, i, 0, "??????");
                MyUtil.setCellValue(workbook, 0, i, 1, "?????????");
                MyUtil.setCellValue(workbook, 0, i, 2, "??????????????????");
                MyUtil.setCellValue(workbook, 0, i, 3, "??????????????????");
                MyUtil.setCellValue(workbook, 0, i, 4, "????????????");
                MyUtil.setCellValue(workbook, 0, i, 5, "????????????");
                MyUtil.setCellValue(workbook, 0, i, 6, "????????????");
                i++;
                MyUtil.setCellValue(workbook, 0, i, 0, String.valueOf(rollbackInfo.get(Dict.GROUPNAME)));
                MyUtil.setCellValue(workbook, 0, i, 1, String.valueOf(rollbackInfo.get(Dict.MAINTASKNAME)));
                MyUtil.setCellValue(workbook, 0, i, 2, String.valueOf(rollbackInfo.get(Dict.FSTSITDATE)));
                MyUtil.setCellValue(workbook, 0, i, 3, String.valueOf(rollbackInfo.get(Dict.FNLROLLBACKDATE)));
                MyUtil.setCellValue(workbook, 0, i, 4, String.valueOf(rollbackInfo.get(Dict.ROLLBACKNUM)));
                MyUtil.setCellValue(workbook, 0, i, 5, String.valueOf(rollbackInfo.get(Dict.DEVELOPER)));
                MyUtil.setCellValue(workbook, 0, i, 6, String.valueOf(rollbackInfo.get(Dict.TESTERS)));
                List<Map> detail = (List<Map>) rollbackInfo.get(Dict.DETAIL);
                i++;
                MyUtil.setCellValue(workbook, 0, i, 4, "????????????");
                MyUtil.setCellValue(workbook, 0, i, 5, "?????????");
                MyUtil.setCellValue(workbook, 0, i, 6, "??????");
                MyUtil.setCellValue(workbook, 0, i, 7, "????????????");
                for (Map map : detail) {
                    i++;
                    MyUtil.setCellValue(workbook, 0, i, 4, String.valueOf(map.get(Dict.DATE)));
                    MyUtil.setCellValue(workbook, 0, i, 5, String.valueOf(map.get(Dict.ROLLBACKOPR)));
                    MyUtil.setCellValue(workbook, 0, i, 6, String.valueOf(map.get(Dict.REASON)));
                    MyUtil.setCellValue(workbook, 0, i, 7, String.valueOf(map.getOrDefault(Dict.DETAILINFO, Constants.FAIL_GET)));
                }
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        resp.reset();
        resp.setContentType("application/octet-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Content-Disposition", "attachment;filename=" + "rollbackInfo.xlsx");
        workbook.write(resp.getOutputStream());
    }

    @Override
    public void refuseTask(List<String> taskIds, String workNo) throws Exception {
        WorkOrder workOrder = workOrderMapper.queryWorkOrderByNo(workNo);
        String fdevNew = workOrder.getFdevNew();
        if (Util.isNullOrEmpty(workOrder)) {
            logger.error("order does not exist" + workNo);
            throw new FtmsException(ErrorConstants.DATA_NOT_EXIST, new String[]{"??????" + workNo + "?????????"});
        }
        String unit = workOrder.getUnit();
        for (String taskId : taskIds) {
            Map taskInfo;
            if (Constants.NUMBER_1.equals(fdevNew)) {
                //?????????fdev??????
                taskInfo = taskApi.queryNewTaskDetail(Arrays.asList(taskId)).get(0);
            } else {
                //?????????fdev??????
                taskInfo = (Map) taskApi.queryTaskDetailByIds(Arrays.asList(taskId)).get(taskId);
            }
            if (MyUtil.isNullOrEmpty(taskInfo)) {
                throw new FtmsException(ErrorConstants.DATA_NOT_EXIST, new String[]{"fdev????????????????????????"});
            }
            List taskPersonNameEns = Constants.NUMBER_1.equals(fdevNew) ?
                 taskApi.queryNewTaskAssignEns(taskInfo):MyUtil.getTaskPersonNameEn(taskInfo);
            //????????????  for fdev??????????????????
            Map message = new HashMap();
            message.put(Dict.TARGET, taskPersonNameEns);
            message.put(Dict.CONTENT, "?????????" + taskInfo.get(Dict.NAME) + "????????????????????????" + unit + "??????????????????????????????????????????");
            message.put(Dict.DESC, "????????????");
            message.put(Dict.TYPE, "2");
            try {
                iNotifyApi.sendUserNotifyForFdev(message, fdevNew);
            } catch (Exception e) {
                logger.error("ftms notify send error: submit refuse task");
            }
        }
    }
}
