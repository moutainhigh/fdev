package com.spdb.fdev.freport.spdb.service.impl;

import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.freport.base.dict.*;
import com.spdb.fdev.freport.base.utils.*;
import com.spdb.fdev.freport.spdb.dao.*;
import com.spdb.fdev.freport.spdb.dto.CommitStatisticsDto;
import com.spdb.fdev.freport.spdb.entity.BaseEntity;
import com.spdb.fdev.freport.spdb.entity.app.AppEntity;
import com.spdb.fdev.freport.spdb.entity.component.*;
import com.spdb.fdev.freport.spdb.entity.demand.DemandBaseInfo;
import com.spdb.fdev.freport.spdb.entity.git.Commit;
import com.spdb.fdev.freport.spdb.entity.task.Task;
import com.spdb.fdev.freport.spdb.entity.user.Company;
import com.spdb.fdev.freport.spdb.entity.user.Group;
import com.spdb.fdev.freport.spdb.entity.user.Role;
import com.spdb.fdev.freport.spdb.entity.user.User;
import com.spdb.fdev.freport.spdb.service.DevelopmentService;
import com.spdb.fdev.freport.spdb.vo.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

import static com.spdb.fdev.freport.base.dict.TaskEnum.TaskStage.*;

@Service
public class DevelopmentServiceImpl implements DevelopmentService {

    @Autowired
    private DemandDao demandDao;

    @Autowired
    private ReportDao reportDao;

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private AppDao appDao;

    @Autowired
    private GitDao gitDao;

    @Autowired
    private ComponentDao componentDao;

    @Autowired
    private UserUtils userUtils;

    @Value("${fdev.user.group.default.ids}")
    private String defaultIds;

    @Override
    public ProductVo queryDemandNewTrend(Integer cycle, DemandBaseInfo demand) throws Exception {
        List<Product> result = new ArrayList<>();//???????????????
        Map<String, Date[]> periodMap = DashboardUtils.handleCycle(cycle);//?????????????????????????????????
        String[] startAndEnd = DashboardUtils.getStartAndEndByPeriodMap(periodMap);//??????????????????????????????
        demand.setDemandLeaderGroup(CommonUtils.getSessionUser().getGroup_id());//???????????????
        List<DemandBaseInfo> demandBaseInfoList = demandDao.findByGroupIdWithOutCanceled(demand, startAndEnd[0], startAndEnd[1]);//????????????id????????????
        Map<String, Integer[]> periodDemandCountMap = getPeriodDemandCountMap(periodMap, demandBaseInfoList);
        for (String period : periodDemandCountMap.keySet()) {
            Integer[] periodDemandCountArray = periodDemandCountMap.get(period);
            result.add(new Product(period, periodDemandCountArray[0]));//????????????????????????
        }
        return new ProductVo(reportDao.findDashboardTips(DashboardDict.DEMAND_NEW_TREND).getContent(), result);

    }

    @Override
    public ProductVo queryDemandProTrend(Integer cycle, DemandBaseInfo demand) throws Exception {
        List<Product> result = new ArrayList<>();//???????????????
        Map<String, Date[]> periodMap = DashboardUtils.handleCycle(cycle);//?????????????????????????????????
        String[] startAndEnd = DashboardUtils.getStartAndEndByPeriodMap(periodMap);//??????????????????????????????
        demand.setDemandLeaderGroup(CommonUtils.getSessionUser().getGroup_id());//???????????????
        List<DemandBaseInfo> demandBaseInfoList = demandDao.findByGroupIdWithOutCanceled(demand, startAndEnd[0], startAndEnd[1]);//????????????id????????????
        Map<String, Integer[]> periodDemandCountMap = getPeriodDemandCountMap(periodMap, demandBaseInfoList);
        for (String period : periodDemandCountMap.keySet()) {
            Integer[] periodDemandCountArray = periodDemandCountMap.get(period);
            result.add(new Product(period, periodDemandCountArray[1]));
        }
        return new ProductVo(reportDao.findDashboardTips(DashboardDict.DEMAND_PRO_TREND).getContent(), result);
    }

    @Override
    public ProductVo queryDemandThroughputTrend(Integer cycle, DemandBaseInfo demand) throws Exception {
        List<Product> result = new ArrayList<>();//???????????????
        Map<String, Date[]> periodMap = DashboardUtils.handleCycle(cycle);//?????????????????????????????????
        String[] startAndEnd = DashboardUtils.getStartAndEndByPeriodMap(periodMap);//??????????????????????????????
        demand.setDemandLeaderGroup(CommonUtils.getSessionUser().getGroup_id());//???????????????
        List<DemandBaseInfo> demandBaseInfoList = demandDao.findByGroupIdWithOutCanceled(demand, startAndEnd[0], startAndEnd[1]);//????????????id????????????
        Map<String, Integer[]> periodDemandCountMap = getPeriodDemandCountMap(periodMap, demandBaseInfoList);
        for (String period : periodDemandCountMap.keySet()) {
            Integer[] periodDemandCountArray = periodDemandCountMap.get(period);
            result.add(new Product(period, periodDemandCountArray[0] + periodDemandCountArray[1]));
        }
        return new ProductVo(reportDao.findDashboardTips(DashboardDict.DEMAND_THROUGHPUT_TREND).getContent(), result);
    }

    @Override
    public ProductVo queryTaskTrend(Integer cycle, String stage) throws Exception {
        List<Product> result = new ArrayList<>();//???????????????
        Map<String, Date[]> periodMap = DashboardUtils.handleCycle(cycle);//?????????????????????????????????
        String[] startAndEnd = DashboardUtils.getStartAndEndByPeriodMap(periodMap, TimeUtils.FORMAT_DATESTAMP_2);//??????????????????????????????
        //?????????????????????
        List<Task> tasks = taskDao.findByStage(CommonUtils.getSessionUser().getGroup_id(), stage, startAndEnd[0], startAndEnd[1]);
        //????????????????????????
        Map<String, Integer> countMap = new LinkedHashMap<>();//??????map????????????
        for (String period : periodMap.keySet()) {
            countMap.put(period, 0);
            Date[] dateArray = periodMap.get(period);//????????????????????????????????? - ????????????????????????
            for (Task item : tasks) {
                Date condition = null;
                switch (stage) {
                    case "create":
                        condition = TimeUtils.FORMAT_DATESTAMP_2.parse(item.getStartTime());
                        break;
                    case "pro":
                        condition = TimeUtils.FORMAT_DATESTAMP_2.parse(item.getFireTime());
                        break;
                }
                if (0 <= condition.compareTo(dateArray[0]) && 0 >= condition.compareTo(dateArray[1])) {
                    countMap.put(period, countMap.get(period) + 1);
                }
            }
        }
        for (String period : countMap.keySet()) {
            result.add(new Product(period, countMap.get(period)));
        }
        return new ProductVo(reportDao.findDashboardTips(DashboardDict.TASK_TREND).getContent(), result);
    }

    @Override
    public ProductVo queryAppNewTrend(Integer cycle) throws Exception {
        List<Product> result = new ArrayList<>();//???????????????
        Map<String, Date[]> periodMap = DashboardUtils.handleCycle(cycle);//?????????????????????????????????
        String[] startAndEnd = DashboardUtils.getStartAndEndByPeriodMap(periodMap);//??????????????????????????????
        List<AppEntity> apps = appDao.find(new AppEntity() {{//?????????????????????
            setGroup(CommonUtils.getSessionUser().getGroup_id());
        }}, startAndEnd[0], startAndEnd[1]);
        //????????????????????????
        Map<String, Integer> countMap = new LinkedHashMap<>();//??????map????????????
        for (String period : periodMap.keySet()) {
            countMap.put(period, 0);
            Date[] dateArray = periodMap.get(period);//????????????????????????????????? - ????????????????????????
            for (AppEntity item : apps) {
                Date condition = TimeUtils.FORMAT_TIMESTAMP.parse(item.getCreateTime());
                if (0 <= condition.compareTo(dateArray[0]) && 0 >= condition.compareTo(dateArray[1])) {
                    countMap.put(period, countMap.get(period) + 1);
                }
            }
        }
        for (String period : countMap.keySet()) {
            result.add(new Product(period, countMap.get(period)));
        }
        return new ProductVo(reportDao.findDashboardTips(DashboardDict.APP_NEW_TREND).getContent(), result);
    }

    @Override
    public List<List<Object>> queryDemandThroughputStatistics(String startTime, String endTime, List<String> groupIds, Boolean showDaily) throws Exception {
        return new ArrayList<List<Object>>() {{
            List<Group> groups = userDao.findGroups(new HashSet<>(groupIds), "1");//?????????????????????
            Set<Group> totalGroup = getGroupChildrenTotal(groups);
            //??????????????????????????????????????????
            Set<Group> filterGroup = totalGroup.stream().filter(x -> groupIds.contains(x.getId())).collect(Collectors.toSet());//????????????
            //?????????????????????
            Set<Group> resultGroup = GroupUtils.getGroupChildrenTotalSingleLevel(filterGroup);
            //???????????????????????????????????????
            Map<String, String> groupIdNameMap = groups.stream().collect(Collectors.toMap(Group::getId,
                    Group::getName,
                    (k1, k2) -> k2,//key????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                    LinkedHashMap::new));//????????????
            boolean isShowDaily = CommonUtils.isNullOrEmpty(showDaily) ? false : showDaily;//?????????false
            List<DemandBaseInfo> dataList = demandDao.findDemandThroughputStatistics(totalGroup.stream().map(Group::getId).collect(Collectors.toSet()),
                    isShowDaily,
                    startTime,
                    endTime);
            //??????Map??????????????????
            Map<String, List<DemandBaseInfo>> groupIdDemandMap = dataList.stream().collect(Collectors.groupingBy(DemandBaseInfo::getDemandLeaderGroup));            //??????????????????????????????
            //??????????????????????????????
            Map<String, Integer> proBusMap = new LinkedHashMap<String, Integer>() {{
                groupIdNameMap.keySet().forEach(x -> put(x, 0));
            }};
            //??????????????????????????????
            Map<String, Integer> proTechMap = new LinkedHashMap<String, Integer>() {{
                groupIdNameMap.keySet().forEach(x -> put(x, 0));
            }};
            //??????????????????????????????
            Map<String, Integer> proDailyMap = new LinkedHashMap<String, Integer>() {{
                groupIdNameMap.keySet().forEach(x -> put(x, 0));
            }};
            //??????????????????
            for (Group x : resultGroup) {
                Set<Group> currentAndChildrenTotal = getChildrenTotal(new ArrayList<Group>() {{
                    add(x);
                }});
                List<DemandBaseInfo> groupDemand = new ArrayList<DemandBaseInfo>() {{
                    currentAndChildrenTotal.forEach(y -> addAll(groupIdDemandMap.getOrDefault(y.getId(), new ArrayList<>())));
                }};
                for (DemandBaseInfo item : groupDemand) {
                    if (!CommonUtils.isNullOrEmpty(x.getId())) {//?????????????????????????????????groupId???null ??????group??????????????? ????????????????????????????????????????????????
                        switch (item.getDemandType()) {
                            case EntityDict.DEMAND_TYPE_BUSINESS:
                                proBusMap.put(x.getId(), proBusMap.get(x.getId()) + 1);
                                break;
                            case EntityDict.DEMAND_TYPE_TECH:
                                proTechMap.put(x.getId(), proTechMap.get(x.getId()) + 1);
                                break;
                            case EntityDict.DEMAND_TYPE_DAILY:
                                proDailyMap.put(x.getId(), proDailyMap.get(x.getId()) + 1);
                                break;
                        }
                    }
                }
            }
            List<Integer> proBusList = new ArrayList<>(proBusMap.values());
            List<Integer> proTechList = new ArrayList<>(proTechMap.values());
            List<Integer> proDailyList = new ArrayList<>(proDailyMap.values());
            add(new ArrayList<Object>() {{//?????????
                add("");
                addAll(groupIdNameMap.values());
                add(StatisticsDict.TOTAL);
            }});
            add(new ArrayList<Object>() {{
                add(StatisticsDict.PRO_BUS_DEMAND);
                addAll(proBusList);
                add(proBusList.stream().mapToInt(x -> x).sum());
            }});
            add(new ArrayList<Object>() {{
                add(StatisticsDict.PRO_TECH_DEMAND);
                addAll(proTechList);
                add(proTechList.stream().mapToInt(x -> x).sum());
            }});
            if (isShowDaily) {
                add(new ArrayList<Object>() {{
                    add(StatisticsDict.PRO_DAILY_DEMAND);
                    addAll(proDailyList);
                    add(proDailyList.stream().mapToInt(x -> x).sum());
                }});
            }
            //??????
            add(new ArrayList<Object>() {{
                Integer total = 0;
                add(StatisticsDict.TOTAL_PRO_DEMAND);
                for (int i = 0; i < groupIdNameMap.size(); i++) {//+1????????????
                    int sum = proBusList.get(i) + proTechList.get(i);
                    if (isShowDaily) {
                        sum += proDailyList.get(i);
                    }
                    total += sum;
                    add(sum);
                }
                add(total);//??????
            }});
        }};
    }

    @Override
    public Map<String, Object> queryTaskThroughputStatistics(String startTime, String endTime, List<String> groupIds, List<String> demandTypeList, List<Integer> taskTypeList, Boolean includeChild) throws Exception {
        List<TaskTroughputStatisticsVo> resultList = new ArrayList<TaskTroughputStatisticsVo>() {{
            List<Group> groups = userDao.findGroups(new HashSet<>(groupIds), "1");//?????????????????????
            if (!CommonUtils.isNullOrEmpty(groups)) {
                Set<Group> totalGroup = getGroupChildrenTotal(groups);
                Set<Group> resultGroup = null;
                if (includeChild) {
                    //????????????
                    resultGroup = totalGroup;
                } else {
                    //??????????????????????????????????????????
                    Set<Group> filterGroup = totalGroup.stream().filter(x -> groupIds.contains(x.getId())).collect(Collectors.toSet());//????????????
                    //?????????????????????
                    resultGroup = GroupUtils.getGroupChildrenTotalSingleLevel(filterGroup);
                }
                //?????????????????????
                List<Task> dataList = taskDao.findThroughputStatistics(resultGroup.stream().map(Group::getId).collect(Collectors.toSet()), startTime.replaceAll("-", "/"), endTime.replaceAll("-", "/"));
                //??????????????????????????????????????????
                Map<String, String> demandIdTypeMap = demandDao.findDemandByIdSet(dataList.stream().map(Task::getRqrmntNo).collect(Collectors.toSet())).stream().collect(Collectors.toMap(DemandBaseInfo::getId, DemandBaseInfo::getDemandType));
                //groupBy????????????????????????
                Map<String, List<Task>> taskGroupBy = dataList.stream().collect(Collectors.groupingBy(Task::getGroup));
                //???????????????
                Map<String, List<User>> userGroupBy = userDao.findUserByGroupIds(resultGroup.stream().map(Group::getId).collect(Collectors.toSet())).stream().collect(Collectors.groupingBy(User::getGroupId));
                Map<String, String> groupIdNameMap = resultGroup.stream().collect(Collectors.toMap(Group::getId, Group::getName));
                //????????????????????????????????????
                Date startDate = null;
                Date endDate = null;
                if (!CommonUtils.isNullOrEmpty(startTime) && !CommonUtils.isNullOrEmpty(endTime)) {
                    startDate = TimeUtils.FORMAT_DATESTAMP.parse(startTime);
                    endDate = TimeUtils.FORMAT_DATESTAMP.parse(endTime);
                }
                //????????????????????????
                for (Group group : resultGroup) {
                    TaskTroughputStatisticsVo vo = new TaskTroughputStatisticsVo();
                    TaskTroughputStatisticsTaskTypeVo addBusSum = new TaskTroughputStatisticsTaskTypeVo(0, 0, 0, 0);//?????????????????????
                    TaskTroughputStatisticsTaskTypeVo addTechSum = new TaskTroughputStatisticsTaskTypeVo(0, 0, 0, 0);//?????????????????????
                    TaskTroughputStatisticsTaskTypeVo addDailySum = new TaskTroughputStatisticsTaskTypeVo(null, null, 0, 0);//?????????????????????
                    TaskTroughputStatisticsTaskTypeVo proBusSum = new TaskTroughputStatisticsTaskTypeVo(0, 0, 0, 0);//?????????????????????
                    TaskTroughputStatisticsTaskTypeVo proTechSum = new TaskTroughputStatisticsTaskTypeVo(0, 0, 0, 0);//?????????????????????
                    TaskTroughputStatisticsTaskTypeVo proDailySum = new TaskTroughputStatisticsTaskTypeVo(null, null, 0, 0);//?????????????????????
                    TaskTroughputStatisticsTaskTypeVo proSum = new TaskTroughputStatisticsTaskTypeVo(0, 0, 0, 0);//??????????????????
                    int person = 0;//??????
                    TaskTroughputStatisticsAvgVo perProBusAvg = new TaskTroughputStatisticsAvgVo(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);//???????????????????????????
                    TaskTroughputStatisticsAvgVo perProTechAvg = new TaskTroughputStatisticsAvgVo(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);//???????????????????????????
                    TaskTroughputStatisticsAvgVo perProDailyAvg = new TaskTroughputStatisticsAvgVo(null, null, BigDecimal.ZERO);//???????????????????????????
                    TaskTroughputStatisticsAvgVo perProAvg = new TaskTroughputStatisticsAvgVo(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);//?????????????????????
                    List<String> currentGroupTotalId = new ArrayList<String>() {{
                        add(group.getId());
                        if (!CommonUtils.isNullOrEmpty(group.getChildren())) {
                            addAll(group.getChildren().stream().map(Group::getId).collect(Collectors.toSet()));
                        }
                    }};
                    for (String groupId : currentGroupTotalId) {
                        person += userGroupBy.getOrDefault(groupId, new ArrayList<>()).size();
                    }
                    for (String groupId : currentGroupTotalId) {
                        List<Task> tasks = taskGroupBy.get(groupId);
                        if (!CommonUtils.isNullOrEmpty(tasks)) {
                            for (Task task : tasks) {
                                String demandType = demandIdTypeMap.get(task.getRqrmntNo());
                                boolean isCreateInDateCondition = false;
                                boolean isProductInDateCondition = false;
                                if (!CommonUtils.isNullOrEmpty(task.getStartTime())) {
                                    if (null != startDate && null != endDate) {
                                        Date date = TimeUtils.FORMAT_DATESTAMP_2.parse(task.getStartTime());
                                        isCreateInDateCondition = 0 <= date.compareTo(startDate) && 0 >= date.compareTo(endDate);
                                    } else {
                                        isCreateInDateCondition = true;
                                    }
                                }
                                if (!CommonUtils.isNullOrEmpty(task.getFireTime())) {
                                    if (null != startDate && null != endDate) {
                                        Date date = TimeUtils.FORMAT_DATESTAMP_2.parse(task.getFireTime());
                                        isProductInDateCondition = 0 <= date.compareTo(startDate) && 0 >= date.compareTo(endDate);
                                    } else {
                                        isProductInDateCondition = true;
                                    }
                                }
                                if (demandTypeList.contains(EntityDict.DEMAND_TYPE_BUSINESS) && EntityDict.DEMAND_TYPE_BUSINESS.equals(demandType) && isCreateInDateCondition) {
                                    addBusSum = recordTaskNum(task, addBusSum, taskTypeList);
                                }
                                if (demandTypeList.contains(EntityDict.DEMAND_TYPE_TECH) && EntityDict.DEMAND_TYPE_TECH.equals(demandType) && isCreateInDateCondition) {
                                    addTechSum = recordTaskNum(task, addTechSum, taskTypeList);
                                }
                                if (demandTypeList.contains(EntityDict.DEMAND_TYPE_DAILY) && EntityDict.DEMAND_TYPE_DAILY.equals(demandType) && isCreateInDateCondition) {
                                    addDailySum = recordTaskNum(task, addDailySum, taskTypeList);
                                }
                                if (demandTypeList.contains(EntityDict.DEMAND_TYPE_BUSINESS) && EntityDict.DEMAND_TYPE_BUSINESS.equals(demandType) && isProductInDateCondition) {
                                    proBusSum = recordTaskNum(task, proBusSum, taskTypeList);
                                }
                                if (demandTypeList.contains(EntityDict.DEMAND_TYPE_TECH) && EntityDict.DEMAND_TYPE_TECH.equals(demandType) && isProductInDateCondition) {
                                    proTechSum = recordTaskNum(task, proTechSum, taskTypeList);
                                }
                                if (demandTypeList.contains(EntityDict.DEMAND_TYPE_DAILY) && EntityDict.DEMAND_TYPE_DAILY.equals(demandType) && isProductInDateCondition) {
                                    proDailySum = recordTaskNum(task, proDailySum, taskTypeList);
                                }
                            }
                        }
                    }
                    proSum.setDailyTaskSum(proSum.getDailyTaskSum() + proBusSum.getDailyTaskSum() + proTechSum.getDailyTaskSum() + proDailySum.getDailyTaskSum());//??????????????????????????????????????????
                    proSum.setDevTaskSum(proSum.getDevTaskSum() + proBusSum.getDevTaskSum() + proTechSum.getDevTaskSum());//??????????????????????????????????????????
                    proSum.setNoCodeTaskSum(proSum.getNoCodeTaskSum() + proBusSum.getNoCodeTaskSum() + proTechSum.getNoCodeTaskSum());//?????????????????????????????????????????????
                    proSum.init();
                    BigDecimal personBigDecimal = new BigDecimal(person);
                    if (0 < person) {
                        //?????????????????????????????????
                        if (demandTypeList.contains(EntityDict.DEMAND_TYPE_BUSINESS)) {
                            if (CommonUtils.isNullOrEmpty(taskTypeList) || taskTypeList.contains(0)) {
                                perProBusAvg.setDevTaskAvg(new BigDecimal(proBusSum.getDevTaskSum()).divide(personBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
                            }
                            if (CommonUtils.isNullOrEmpty(taskTypeList) || taskTypeList.contains(1)) {
                                perProBusAvg.setNoCodeTaskAvg(new BigDecimal(proBusSum.getNoCodeTaskSum()).divide(personBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
                            }
                            if (CommonUtils.isNullOrEmpty(taskTypeList) || taskTypeList.contains(2)) {
                                perProBusAvg.setDailyTaskAvg(new BigDecimal(proBusSum.getDailyTaskSum()).divide(personBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
                            }
                            perProBusAvg.setSum(new BigDecimal(proBusSum.getSum()).divide(personBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
                        }
                        //?????????????????????????????????
                        if (demandTypeList.contains(EntityDict.DEMAND_TYPE_TECH)) {
                            if (CommonUtils.isNullOrEmpty(taskTypeList) || taskTypeList.contains(0)) {
                                perProTechAvg.setDevTaskAvg(new BigDecimal(proTechSum.getDevTaskSum()).divide(personBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
                            }
                            if (CommonUtils.isNullOrEmpty(taskTypeList) || taskTypeList.contains(1)) {
                                perProTechAvg.setNoCodeTaskAvg(new BigDecimal(proTechSum.getNoCodeTaskSum()).divide(personBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
                            }
                            if (CommonUtils.isNullOrEmpty(taskTypeList) || taskTypeList.contains(2)) {
                                perProTechAvg.setDailyTaskAvg(new BigDecimal(proTechSum.getDailyTaskSum()).divide(personBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
                            }
                            perProTechAvg.setSum(new BigDecimal(proTechSum.getSum()).divide(personBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
                        }
                        //?????????????????????????????????
                        if (demandTypeList.contains(EntityDict.DEMAND_TYPE_DAILY)) {
                            if (CommonUtils.isNullOrEmpty(taskTypeList) || taskTypeList.contains(2)) {
                                perProDailyAvg.setDailyTaskAvg(new BigDecimal(proDailySum.getDailyTaskSum()).divide(personBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
                            }
                            perProDailyAvg.setSum(new BigDecimal(proDailySum.getSum()).divide(personBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
                        }
                        //?????????????????????
                        BigDecimal devTaskTotal = new BigDecimal(proSum.getDevTaskSum());
                        BigDecimal noCodeTaskTotal = new BigDecimal(proSum.getNoCodeTaskSum());
                        BigDecimal dailyTaskTotal = new BigDecimal(proSum.getDailyTaskSum());
                        BigDecimal AllTaskTotal = new BigDecimal(proSum.getSum());
                        perProAvg.setDevTaskAvg(devTaskTotal.divide(personBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
                        perProAvg.setNoCodeTaskAvg(noCodeTaskTotal.divide(personBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
                        perProAvg.setDailyTaskAvg(dailyTaskTotal.divide(personBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
                        perProAvg.setSum(AllTaskTotal.divide(personBigDecimal, 2, BigDecimal.ROUND_HALF_UP));
                    }
                    vo.setGroupId(group.getId());
                    vo.setGroupName(groupIdNameMap.get(group.getId()));
                    if (demandTypeList.contains(EntityDict.DEMAND_TYPE_BUSINESS)) vo.setAddBusSum(addBusSum);
                    if (demandTypeList.contains(EntityDict.DEMAND_TYPE_TECH)) vo.setAddTechSum(addTechSum);
                    if (demandTypeList.contains(EntityDict.DEMAND_TYPE_DAILY)) vo.setAddDailySum(addDailySum);
                    if (demandTypeList.contains(EntityDict.DEMAND_TYPE_BUSINESS)) vo.setProBusSum(proBusSum);
                    if (demandTypeList.contains(EntityDict.DEMAND_TYPE_TECH)) vo.setProTechSum(proTechSum);
                    if (demandTypeList.contains(EntityDict.DEMAND_TYPE_DAILY)) vo.setProDailySum(proDailySum);
                    vo.setProSum(proSum);
                    vo.setPerson(person);
                    if (demandTypeList.contains(EntityDict.DEMAND_TYPE_BUSINESS)) vo.setPerProBusAvg(perProBusAvg);
                    if (demandTypeList.contains(EntityDict.DEMAND_TYPE_TECH)) vo.setPerProTechAvg(perProTechAvg);
                    if (demandTypeList.contains(EntityDict.DEMAND_TYPE_DAILY)) vo.setPerProDailyAvg(perProDailyAvg);
                    vo.setPerProAvg(perProAvg);
                    add(vo);
                }
            }
        }};
        //??????????????????
        Map totalData = new HashMap() {{
            int addBusSum = 0;//???????????????????????????
            int addTechSum = 0;//???????????????????????????
            int addDailySum = 0;//???????????????????????????
            int proBusSum = 0;//???????????????????????????
            int proTechSum = 0;//???????????????????????????
            int proDailySum = 0;//???????????????????????????
            int proSum = 0;//??????????????????
            int person = 0;//??????
            BigDecimal perProBusAvg = BigDecimal.ZERO;//?????????????????????????????????
            BigDecimal perProTechAvg = BigDecimal.ZERO;//?????????????????????????????????
            BigDecimal perProDailyAvg = BigDecimal.ZERO;//?????????????????????????????????
            BigDecimal perProAvg = BigDecimal.ZERO;//?????????????????????
            for (TaskTroughputStatisticsVo vo : resultList) {
                if (demandTypeList.contains(EntityDict.DEMAND_TYPE_BUSINESS))
                    addBusSum += vo.getAddBusSum().getDevTaskSum() + vo.getAddBusSum().getNoCodeTaskSum() + vo.getAddBusSum().getDailyTaskSum();
                if (demandTypeList.contains(EntityDict.DEMAND_TYPE_TECH))
                    addTechSum += vo.getAddTechSum().getDevTaskSum() + vo.getAddTechSum().getNoCodeTaskSum() + vo.getAddTechSum().getDailyTaskSum();
                if (demandTypeList.contains(EntityDict.DEMAND_TYPE_DAILY))
                    addDailySum += vo.getAddDailySum().getDailyTaskSum();
                if (demandTypeList.contains(EntityDict.DEMAND_TYPE_BUSINESS))
                    proBusSum += vo.getProBusSum().getDevTaskSum() + vo.getProBusSum().getNoCodeTaskSum() + vo.getProBusSum().getDailyTaskSum();
                if (demandTypeList.contains(EntityDict.DEMAND_TYPE_TECH))
                    proTechSum += vo.getProTechSum().getDevTaskSum() + vo.getProTechSum().getNoCodeTaskSum() + vo.getProTechSum().getDailyTaskSum();
                if (demandTypeList.contains(EntityDict.DEMAND_TYPE_DAILY))
                    proDailySum += vo.getProDailySum().getDailyTaskSum();
                proSum += vo.getProSum().getDevTaskSum() + vo.getProSum().getNoCodeTaskSum() + vo.getProSum().getDailyTaskSum();
                person += vo.getPerson();
            }
            if (person > 0) {
                BigDecimal personBigDec = new BigDecimal(person);
                perProBusAvg = new BigDecimal(proBusSum).divide(personBigDec, 2, BigDecimal.ROUND_HALF_UP);
                perProTechAvg = new BigDecimal(proTechSum).divide(personBigDec, 2, BigDecimal.ROUND_HALF_UP);
                perProDailyAvg = new BigDecimal(proDailySum).divide(personBigDec, 2, BigDecimal.ROUND_HALF_UP);
                perProAvg = new BigDecimal(proSum).divide(personBigDec, 2, BigDecimal.ROUND_HALF_UP);
            }
            put(EntityDict.GROUPNAME, StatisticsDict.TOTAL);
            if (demandTypeList.contains(EntityDict.DEMAND_TYPE_BUSINESS)) put(EntityDict.ADDBUSSUM, addBusSum);
            if (demandTypeList.contains(EntityDict.DEMAND_TYPE_TECH)) put(EntityDict.ADDTECHSUM, addTechSum);
            if (demandTypeList.contains(EntityDict.DEMAND_TYPE_DAILY)) put(EntityDict.ADDDAILYSUM, addDailySum);
            if (demandTypeList.contains(EntityDict.DEMAND_TYPE_BUSINESS)) put(EntityDict.PROBUSSUM, proBusSum);
            if (demandTypeList.contains(EntityDict.DEMAND_TYPE_TECH)) put(EntityDict.PROTECHSUM, proTechSum);
            if (demandTypeList.contains(EntityDict.DEMAND_TYPE_DAILY)) put(EntityDict.PRODAILYSUM, proDailySum);
            put(EntityDict.PROSUM, proSum);
            put(EntityDict.PERSON, person);
            if (demandTypeList.contains(EntityDict.DEMAND_TYPE_BUSINESS)) put(EntityDict.PERPROBUSAVG, perProBusAvg);
            if (demandTypeList.contains(EntityDict.DEMAND_TYPE_TECH)) put(EntityDict.PERPROTECHAVG, perProTechAvg);
            if (demandTypeList.contains(EntityDict.DEMAND_TYPE_DAILY)) put(EntityDict.PERPRODAILYAVG, perProDailyAvg);
            put(EntityDict.PERPROAVG, perProAvg);
        }};
        return new HashMap<String, Object>() {{
            put(EntityDict.DATAS, resultList);
            put(EntityDict.TOTALDATA, totalData);
        }};
    }

    private TaskTroughputStatisticsTaskTypeVo recordTaskNum(Task task, TaskTroughputStatisticsTaskTypeVo taskNum, List<Integer> taskTypeList) {
        //????????????
        if ((CommonUtils.isNullOrEmpty(taskTypeList) || taskTypeList.contains(0)) && (CommonUtils.isNullOrEmpty(task.getTaskType()) || 0 == task.getTaskType())) {
            taskNum.setDevTaskSum(CommonUtils.isNullOrEmpty(taskNum.getDevTaskSum()) ? 1 : (taskNum.getDevTaskSum() + 1));
            taskNum.setSum(taskNum.getSum() + 1);
        }
        if ((CommonUtils.isNullOrEmpty(taskTypeList) || taskTypeList.contains(1)) && (!CommonUtils.isNullOrEmpty(task.getTaskType()) && 1 == task.getTaskType())) {
            //???????????????
            taskNum.setNoCodeTaskSum(CommonUtils.isNullOrEmpty(taskNum.getNoCodeTaskSum()) ? 1 : (taskNum.getNoCodeTaskSum() + 1));
            taskNum.setSum(taskNum.getSum() + 1);
        }
        if ((CommonUtils.isNullOrEmpty(taskTypeList) || taskTypeList.contains(2)) && (!CommonUtils.isNullOrEmpty(task.getTaskType()) && 2 == task.getTaskType())) {
            //????????????
            taskNum.setDailyTaskSum(CommonUtils.isNullOrEmpty(taskNum.getDailyTaskSum()) ? 1 : (taskNum.getDailyTaskSum() + 1));
            taskNum.setSum(taskNum.getSum() + 1);
        }
        return taskNum;
    }

    @Override
    public XSSFWorkbook exportTaskThroughputDetail(String startTime, String endTime, List<String> groupIds, List<String> demandTypeList, List<Integer> taskTypeList, Boolean includeChild) throws Exception {
        // ?????????workbook
        InputStream inputStream;
        XSSFWorkbook workbook;
        //????????????
        try {
            ClassPathResource classPathResource = new ClassPathResource("taskDetail.xlsx");
            inputStream = classPathResource.getInputStream();
            workbook = new XSSFWorkbook(inputStream);
        } catch (Exception e) {
            throw new FdevException("?????????????????????");
        }
        List<Group> groups = userDao.findGroups(new HashSet<>(groupIds), "1");//?????????????????????
        if (!CommonUtils.isNullOrEmpty(groups) && !CommonUtils.isNullOrEmpty(demandTypeList)) {
            Set<Group> totalGroup = getGroupChildrenTotal(groups);
            Set<Group> resultGroup = null;
            if (includeChild) {
                //????????????
                resultGroup = totalGroup;
            } else {
                //??????????????????????????????????????????
                Set<Group> filterGroup = totalGroup.stream().filter(x -> groupIds.contains(x.getId())).collect(Collectors.toSet());//????????????
                //?????????????????????
                resultGroup = GroupUtils.getGroupChildrenTotalSingleLevel(filterGroup);
            }
            //?????????????????????
            List<Task> dataList = taskDao.findThroughputStatistics(resultGroup.stream().map(Group::getId).collect(Collectors.toSet()), startTime.replaceAll("-", "/"), endTime.replaceAll("-", "/"));
            //????????????groupBy
            Map<String, List<Task>> taskGroupBy = dataList.stream().collect(Collectors.groupingBy(Task::getGroup));
            //????????????id???????????????map
            Set<String> userIdSet = new HashSet<>();
            Set<String> demandIdSet = new HashSet<>();
            dataList.forEach(x -> {
                if (!CommonUtils.isNullOrEmpty(x.getMaster())) {
                    userIdSet.addAll(x.getMaster());
                }
                if (!CommonUtils.isNullOrEmpty(x.getDeveloper())) {
                    userIdSet.addAll(x.getDeveloper());
                }
                if (!CommonUtils.isNullOrEmpty(x.getRqrmntNo())) {
                    demandIdSet.add(x.getRqrmntNo());
                }
            });
            //????????????map
            Map<String, String> userIdNameMap = userDao.findUsers(userIdSet).stream().collect(Collectors.toMap(BaseEntity::getId, x -> x.getUserNameCn() + x.getUserNameEn()));
            Map<String, String> groupIdNameMap = resultGroup.stream().collect(Collectors.toMap(Group::getId, Group::getName));
            Map<String, String> demandIdTypeMap = demandDao.findDemandByIdSet(demandIdSet).stream().collect(Collectors.toMap(DemandBaseInfo::getId, DemandBaseInfo::getDemandType));
            Group[] groupArray = resultGroup.toArray(new Group[]{});
            //?????????sheet??????
            workbook.setSheetName(0, groupIdNameMap.get(groupArray[0].getId()));
            XSSFRow templateRow = workbook.getSheetAt(0).getRow(0);
            //???????????????sheet
            for (int i = 1; i < groupArray.length; i++) {
                XSSFSheet sheet = workbook.createSheet(groupIdNameMap.get(groupArray[i].getId()));
                XSSFRow row = sheet.createRow(0);
                for (int j = 0; j < templateRow.getLastCellNum() - 1; j++) {
                    row.createCell(j).setCellValue(templateRow.getCell(j).getStringCellValue());//????????????
                }
            }
            for (int i = 0; i < groupArray.length; i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                Group group = groupArray[i];
                int line = 1;//??????????????????
                List<String> currentGroupTotalId = new ArrayList<String>() {{
                    add(group.getId());
                    if (!CommonUtils.isNullOrEmpty(group.getChildren())) {
                        addAll(group.getChildren().stream().map(Group::getId).collect(Collectors.toSet()));
                    }
                }};
                for (String groupId : currentGroupTotalId) {
                    List<Task> tasks = taskGroupBy.get(groupId);
                    if (!CommonUtils.isNullOrEmpty(tasks)) {
                        for (Task task : tasks) {
                            TaskUtils.set(task);
                            XSSFRow row = sheet.createRow(line);
                            StringBuilder master = new StringBuilder();
                            if (!CommonUtils.isNullOrEmpty(task.getMaster())) {
                                for (String x : task.getMaster()) {
                                    if (!CommonUtils.isNullOrEmpty(userIdNameMap.get(x)))
                                        master.append(userIdNameMap.get(x)).append(",");
                                }
                                if (master.length() > 0) {
                                    master.deleteCharAt(master.length() - 1);
                                }
                            }
                            StringBuilder developer = new StringBuilder();
                            if (!CommonUtils.isNullOrEmpty(task.getDeveloper())) {
                                for (String x : task.getDeveloper()) {
                                    if (!CommonUtils.isNullOrEmpty(userIdNameMap.get(x)))
                                        developer.append(userIdNameMap.get(x)).append(",");
                                }
                                if (developer.length() > 0) {
                                    developer.deleteCharAt(developer.length() - 1);
                                }
                            }
                            String demandType = demandIdTypeMap.get(task.getRqrmntNo());
                            if (!CommonUtils.isNullOrEmpty(demandType)) {
                                switch (demandType) {
                                    case EntityDict.DEMAND_TYPE_BUSINESS:
                                        demandType = DemandEnum.DemandTypeEnum.BUSINESS.getName();
                                        break;
                                    case EntityDict.DEMAND_TYPE_TECH:
                                        demandType = DemandEnum.DemandTypeEnum.TECH.getName();
                                        break;
                                    case EntityDict.DEMAND_TYPE_DAILY:
                                        demandType = DemandEnum.DemandTypeEnum.DAILY.getName();
                                        break;
                                }
                            }
                            if (demandTypeList.contains(demandIdTypeMap.get(task.getRqrmntNo())) && (CommonUtils.isNullOrEmpty(taskTypeList) || taskTypeList.contains(task.getTaskType()) || (taskTypeList.contains(0) && CommonUtils.isNullOrEmpty(task.getTaskType())))) {
                                row.createCell(0).setCellValue(task.getName());//????????????
                                row.createCell(1).setCellValue(task.getStage());//????????????
                                row.createCell(2).setCellValue(task.getFdevImplementUnitNo());//??????????????????
                                row.createCell(3).setCellValue(demandType);//????????????
                                row.createCell(4).setCellValue(task.getStartTime());//????????????
                                row.createCell(5).setCellValue(task.getFireTime());//????????????
                                row.createCell(6).setCellValue(!CommonUtils.isNullOrEmpty(task.getFireTime()) ? "??????" : "??????");//????????????
                                row.createCell(7).setCellValue(groupIdNameMap.get(task.getGroup()));//????????????
                                row.createCell(8).setCellValue(master.toString());//???????????????
                                row.createCell(9).setCellValue(developer.toString());////????????????
                                line++;
                            }
                        }
                    }
                }
            }
        }
        return workbook;
    }

    @Override
    public List<TaskPhaseStatisticsVo> queryTaskPhaseStatistics(List<String> groupIds, Boolean includeChild, List<String> taskPersonTypeForAvg) throws Exception {
        return new ArrayList<TaskPhaseStatisticsVo>() {{
            Set<Group> totalGroups = new HashSet<>(userDao.findGroups(new HashSet<>(groupIds), "1"));
            Set<Group> resultGroups;
            if (!CommonUtils.isNullOrEmpty(includeChild) && includeChild) {//??????????????????
                totalGroups.addAll(userDao.findGroupBySortNum(totalGroups.stream().map(Group::getSortNum).collect(Collectors.toSet())));
                GroupUtils.setChildren(totalGroups); //???????????????
                //?????????????????????  ??? ???????????????????????????
                resultGroups = GroupUtils.getGroupChildrenTotalSingleLevel(GroupUtils.filterByGroupIds(totalGroups, new HashSet<>(groupIds)));
            } else {
                resultGroups = totalGroups;
            }
            Map<String, String> groupIdNameMap = resultGroups.stream().collect(Collectors.toMap(Group::getId, Group::getName));
            //?????????????????????
            List<Task> dataList = taskDao.findByGroupIds(totalGroups.stream().map(Group::getId).collect(Collectors.toSet()));
            //groupBy????????????????????????
            Map<String, List<Task>> taskGroupBy = dataList.stream().collect(Collectors.groupingBy(Task::getGroup));
            //????????????????????????
            for (Group group : resultGroups) {
                add(new TaskPhaseStatisticsAvgVo() {{
                    List<String> createIds = new ArrayList<>();
                    List<String> devIds = new ArrayList<>();
                    List<String> sitIds = new ArrayList<>();
                    List<String> uatIds = new ArrayList<>();
                    List<String> relIds = new ArrayList<>();
                    List<String> proIds = new ArrayList<>();
                    List<String> currentGroupTotalId = new ArrayList<String>() {{
                        add(group.getId());
                        if (!CommonUtils.isNullOrEmpty(group.getChildren())) {
                            addAll(group.getChildren().stream().map(Group::getId).collect(Collectors.toSet()));
                        }
                    }};
                    boolean forAvg = !CommonUtils.isNullOrEmpty(taskPersonTypeForAvg);
                    Set<String> taskCreatePerson = forAvg ? new HashSet<>() : null;
                    Set<String> taskDevPerson = forAvg ? new HashSet<>() : null;
                    Set<String> taskSitPerson = forAvg ? new HashSet<>() : null;
                    Set<String> taskUatPerson = forAvg ? new HashSet<>() : null;
                    Set<String> taskRelPerson = forAvg ? new HashSet<>() : null;
                    Set<String> taskProPerson = forAvg ? new HashSet<>() : null;
                    for (String groupId : currentGroupTotalId) {
                        List<Task> tasks = taskGroupBy.get(groupId);
                        if (!CommonUtils.isNullOrEmpty(tasks)) {
                            for (Task task : tasks) {
                                TaskEnum.TaskStage taskStage = getByName(task.getStage());
                                if (!CommonUtils.isNullOrEmpty(taskStage)) {
                                    switch (taskStage) {
                                        case CREATE_INFO:
                                        case CREATE_APP:
                                        case CREATE_FEATURE:
                                            createIds.add(task.getId());
                                            if (forAvg) taskCreatePerson.addAll(forAvg(taskPersonTypeForAvg, task));
                                            break;
                                        case DEVELOP:
                                            devIds.add(task.getId());
                                            if (forAvg) taskDevPerson.addAll(forAvg(taskPersonTypeForAvg, task));
                                            break;
                                        case SIT:
                                            sitIds.add(task.getId());
                                            if (forAvg) taskSitPerson.addAll(forAvg(taskPersonTypeForAvg, task));
                                            break;
                                        case UAT:
                                            uatIds.add(task.getId());
                                            if (forAvg) taskUatPerson.addAll(forAvg(taskPersonTypeForAvg, task));
                                            break;
                                        case REL:
                                            relIds.add(task.getId());
                                            if (forAvg) taskRelPerson.addAll(forAvg(taskPersonTypeForAvg, task));
                                            break;
                                        case PRODUCTION:
                                            proIds.add(task.getId());
                                            if (forAvg) taskProPerson.addAll(forAvg(taskPersonTypeForAvg, task));
                                            break;
                                    }
                                }

                            }
                        }
                    }
                    setId(group.getId());
                    String groupName = groupIdNameMap.get(group.getId());
                    int leftBracketIndex = groupName.indexOf("???");//???????????????????????? ??????????????????????????????
                    setName(-1 != leftBracketIndex ? groupName.substring(0, leftBracketIndex) : groupName);
                    setCreateIds(createIds);
                    setDevIds(devIds);
                    setSitIds(sitIds);
                    setUatIds(uatIds);
                    setRelIds(relIds);
                    setProIds(proIds);
                    setCount();
                    if (forAvg) {
                        setCreateAvg(0 == taskCreatePerson.size() || CommonUtils.isNullOrEmpty(getCreateCount()) ? BigDecimal.ZERO : new BigDecimal(getCreateCount()).divide(new BigDecimal(taskCreatePerson.size()), 2, BigDecimal.ROUND_HALF_UP));
                        setDevAvg(0 == taskDevPerson.size() || CommonUtils.isNullOrEmpty(getDevCount()) ? BigDecimal.ZERO : new BigDecimal(getDevCount()).divide(new BigDecimal(taskDevPerson.size()), 2, BigDecimal.ROUND_HALF_UP));
                        setSitAvg(0 == taskSitPerson.size() || CommonUtils.isNullOrEmpty(getSitCount()) ? BigDecimal.ZERO : new BigDecimal(getSitCount()).divide(new BigDecimal(taskSitPerson.size()), 2, BigDecimal.ROUND_HALF_UP));
                        setUatAvg(0 == taskUatPerson.size() || CommonUtils.isNullOrEmpty(getUatCount()) ? BigDecimal.ZERO : new BigDecimal(getUatCount()).divide(new BigDecimal(taskUatPerson.size()), 2, BigDecimal.ROUND_HALF_UP));
                        setRelAvg(0 == taskRelPerson.size() || CommonUtils.isNullOrEmpty(getRelCount()) ? BigDecimal.ZERO : new BigDecimal(getRelCount()).divide(new BigDecimal(taskRelPerson.size()), 2, BigDecimal.ROUND_HALF_UP));
                        setProAvg(0 == taskProPerson.size() || CommonUtils.isNullOrEmpty(getProCount()) ? BigDecimal.ZERO : new BigDecimal(getProCount()).divide(new BigDecimal(taskProPerson.size()), 2, BigDecimal.ROUND_HALF_UP));
                    }
                }});
            }
        }};
    }

    @Override
    public List<TaskPhaseStatisticsVo> queryTaskPhaseChangeStatistics(String startTime, String endTime, List<String> groupIds, Boolean includeChild, List<String> taskPersonTypeForAvg, List<String> demandTypeList, List<Integer> taskTypeList) throws Exception {
        return new ArrayList<TaskPhaseStatisticsVo>() {{
            Set<Group> totalGroups = new HashSet<>(userDao.findGroups(new HashSet<>(groupIds), "1"));
            Set<Group> resultGroups;
            if (!CommonUtils.isNullOrEmpty(includeChild) && includeChild) {//??????????????????
                totalGroups.addAll(userDao.findGroupBySortNum(totalGroups.stream().map(Group::getSortNum).collect(Collectors.toSet())));
                GroupUtils.setChildren(totalGroups); //???????????????
                //?????????????????????  ??? ???????????????????????????
                resultGroups = GroupUtils.getGroupChildrenTotalSingleLevel(GroupUtils.filterByGroupIds(totalGroups, new HashSet<>(groupIds)));
            } else {
                resultGroups = totalGroups;
            }
            Map<String, String> groupIdNameMap = resultGroups.stream().collect(Collectors.toMap(Group::getId, Group::getName));
            //?????????????????????
            List<Task> dataList = taskDao.findByGroupIds(totalGroups.stream().map(Group::getId).collect(Collectors.toSet()));
            //groupBy????????????????????????
            Map<String, List<Task>> taskGroupBy = dataList.stream().collect(Collectors.groupingBy(Task::getGroup));
            //??????????????????????????????????????????
            Map<String, String> demandIdTypeMap = demandDao.findDemandByIdSet(dataList.stream().map(Task::getRqrmntNo).collect(Collectors.toSet())).stream().collect(Collectors.toMap(DemandBaseInfo::getId, DemandBaseInfo::getDemandType));
            //????????????????????????????????????
            Date startDate = TimeUtils.FORMAT_DATESTAMP.parse(startTime);
            Date endDate = TimeUtils.FORMAT_DATESTAMP.parse(endTime);
            //????????????????????????
            for (Group group : resultGroups) {
                add(new TaskPhaseStatisticsAvgVo() {{
                    List<String> createIds = new ArrayList<>();
                    List<String> devIds = new ArrayList<>();
                    List<String> sitIds = new ArrayList<>();
                    List<String> uatIds = new ArrayList<>();
                    List<String> relIds = new ArrayList<>();
                    List<String> proIds = new ArrayList<>();
                    List<String> currentGroupTotalId = new ArrayList<String>() {{
                        add(group.getId());
                        if (!CommonUtils.isNullOrEmpty(group.getChildren())) {
                            addAll(group.getChildren().stream().map(Group::getId).collect(Collectors.toSet()));
                        }
                    }};
                    boolean forAvg = !CommonUtils.isNullOrEmpty(taskPersonTypeForAvg);
                    Set<String> taskCreatePerson = forAvg ? new HashSet<>() : null;
                    Set<String> taskDevPerson = forAvg ? new HashSet<>() : null;
                    Set<String> taskSitPerson = forAvg ? new HashSet<>() : null;
                    Set<String> taskUatPerson = forAvg ? new HashSet<>() : null;
                    Set<String> taskRelPerson = forAvg ? new HashSet<>() : null;
                    Set<String> taskProPerson = forAvg ? new HashSet<>() : null;
                    for (String groupId : currentGroupTotalId) {
                        List<Task> tasks = taskGroupBy.get(groupId);
                        if (!CommonUtils.isNullOrEmpty(tasks)) {
                            Date date = null;//?????????????????????????????????????????????????????????????????????????????????????????????
                            for (Task task : tasks) {
                                String demandType = demandIdTypeMap.get(task.getRqrmntNo());
                                if (demandTypeList.contains(demandType) && ((taskTypeList.contains(0) && CommonUtils.isNullOrEmpty(task.getTaskType())) || taskTypeList.contains(task.getTaskType()))) {
                                    //???????????????????????????????????????????????????????????????null?????????????????????
                                    if (!CommonUtils.isNullOrEmpty(task.getStartTime())) {
                                        date = TimeUtils.FORMAT_DATESTAMP_2.parse(task.getStartTime());
                                        if (date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0)
                                            createIds.add(task.getId());
                                        if (forAvg) taskCreatePerson.addAll(forAvg(taskPersonTypeForAvg, task));
                                    }
                                    if (!CommonUtils.isNullOrEmpty(task.getStartTime())) {
                                        date = TimeUtils.FORMAT_DATESTAMP_2.parse(task.getStartTime());
                                        if (date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0)
                                            devIds.add(task.getId());
                                        if (forAvg) taskDevPerson.addAll(forAvg(taskPersonTypeForAvg, task));
                                    }
                                    if (!CommonUtils.isNullOrEmpty(task.getStartInnerTestTime())) {
                                        date = TimeUtils.FORMAT_DATESTAMP_2.parse(task.getStartInnerTestTime());
                                        if (date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0)
                                            sitIds.add(task.getId());
                                        if (forAvg) taskSitPerson.addAll(forAvg(taskPersonTypeForAvg, task));
                                    }
                                    if (!CommonUtils.isNullOrEmpty(task.getStartUatTestTime())) {
                                        date = TimeUtils.FORMAT_DATESTAMP_2.parse(task.getStartUatTestTime());
                                        if (date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0)
                                            uatIds.add(task.getId());
                                        if (forAvg) taskUatPerson.addAll(forAvg(taskPersonTypeForAvg, task));
                                    }
                                    if (!CommonUtils.isNullOrEmpty(task.getStartRelTestTime())) {
                                        date = TimeUtils.FORMAT_DATESTAMP_2.parse(task.getStartRelTestTime());
                                        if (date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0)
                                            relIds.add(task.getId());
                                        if (forAvg) taskRelPerson.addAll(forAvg(taskPersonTypeForAvg, task));
                                    }
                                    if (!CommonUtils.isNullOrEmpty(task.getFireTime())) {
                                        date = TimeUtils.FORMAT_DATESTAMP_2.parse(task.getFireTime());
                                        if (date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0)
                                            proIds.add(task.getId());
                                        if (forAvg) taskProPerson.addAll(forAvg(taskPersonTypeForAvg, task));
                                    }
                                }
                            }
                        }
                    }
                    setId(group.getId());
                    String groupName = groupIdNameMap.get(group.getId());
                    int leftBracketIndex = groupName.indexOf("???");//???????????????????????? ??????????????????????????????
                    setName(-1 != leftBracketIndex ? groupName.substring(0, leftBracketIndex) : groupName);
                    setCreateIds(createIds);
                    setDevIds(devIds);
                    setSitIds(sitIds);
                    setUatIds(uatIds);
                    setRelIds(relIds);
                    setProIds(proIds);
                    setCount();
                    if (forAvg) {
                        setCreateAvg(0 == taskDevPerson.size() || CommonUtils.isNullOrEmpty(getCreateCount()) ? BigDecimal.ZERO : new BigDecimal(getCreateCount()).divide(new BigDecimal(taskCreatePerson.size()), 2, BigDecimal.ROUND_HALF_UP));
                        setDevAvg(0 == taskDevPerson.size() || CommonUtils.isNullOrEmpty(getDevCount()) ? BigDecimal.ZERO : new BigDecimal(getDevCount()).divide(new BigDecimal(taskDevPerson.size()), 2, BigDecimal.ROUND_HALF_UP));
                        setSitAvg(0 == taskSitPerson.size() || CommonUtils.isNullOrEmpty(getSitCount()) ? BigDecimal.ZERO : new BigDecimal(getSitCount()).divide(new BigDecimal(taskSitPerson.size()), 2, BigDecimal.ROUND_HALF_UP));
                        setUatAvg(0 == taskUatPerson.size() || CommonUtils.isNullOrEmpty(getUatCount()) ? BigDecimal.ZERO : new BigDecimal(getUatCount()).divide(new BigDecimal(taskUatPerson.size()), 2, BigDecimal.ROUND_HALF_UP));
                        setRelAvg(0 == taskRelPerson.size() || CommonUtils.isNullOrEmpty(getRelCount()) ? BigDecimal.ZERO : new BigDecimal(getRelCount()).divide(new BigDecimal(taskRelPerson.size()), 2, BigDecimal.ROUND_HALF_UP));
                        setProAvg(0 == taskProPerson.size() || CommonUtils.isNullOrEmpty(getProCount()) ? BigDecimal.ZERO : new BigDecimal(getProCount()).divide(new BigDecimal(taskProPerson.size()), 2, BigDecimal.ROUND_HALF_UP));
                    }
                }});
            }
        }};
    }

    @Override
    public List<TaskPhaseStatisticsVo> queryAppTaskPhaseStatistics(List<String> appIds) throws Exception {
        Set<String> appIdSet = new HashSet<>(appIds);
        List<AppEntity> apps = appDao.findByIds(appIdSet);//????????????????????????(???????????????????????????????????????????????????????????????)
        List<Task> totalTask = taskDao.findByAppIds(appIdSet);//??????????????????????????????
        Map<String, List<Task>> appIdTaskMap = totalTask.stream().collect(Collectors.groupingBy(Task::getProjectId));
        return new ArrayList<TaskPhaseStatisticsVo>() {{
            for (AppEntity app : apps) {
                List<String> createIds = new ArrayList<>();
                List<String> devIds = new ArrayList<>();
                List<String> sitIds = new ArrayList<>();
                List<String> uatIds = new ArrayList<>();
                List<String> relIds = new ArrayList<>();
                List<String> proIds = new ArrayList<>();
                List<Task> tasks = appIdTaskMap.get(app.getId());
                if (!CommonUtils.isNullOrEmpty(tasks)) {
                    for (Task task : tasks) {
                        switch (getByName(task.getStage())) {
                            case CREATE_INFO:
                            case CREATE_APP:
                            case CREATE_FEATURE:
                                createIds.add(task.getId());
                                break;
                            case DEVELOP:
                                devIds.add(task.getId());
                                break;
                            case SIT:
                                sitIds.add(task.getId());
                                break;
                            case UAT:
                                uatIds.add(task.getId());
                                break;
                            case REL:
                                relIds.add(task.getId());
                                break;
                            case PRODUCTION:
                                proIds.add(task.getId());
                                break;
                        }
                    }
                }
                add(new TaskPhaseStatisticsVo() {{
                    setName(app.getNameEn());
                    setCreateIds(createIds);
                    setDevIds(devIds);
                    setSitIds(sitIds);
                    setUatIds(uatIds);
                    setRelIds(relIds);
                    setProIds(proIds);
                    setCount();
                }});
            }
        }};
    }

    @Override
    public List<DemandStatisticsVo> queryDemandStatistics(List<String> groupIds, String priority, Boolean includeChild) throws Exception {
        // ??????????????????????????????????????????????????????????????????
        if (groupIds == null || groupIds.size() == 0) {
            groupIds = Arrays.asList(defaultIds.split(","));
        }
        // ?????????????????????id ??????????????????
        Set<Group> totalGroups = new HashSet<>(userDao.findGroups(new HashSet<>(groupIds), "1"));
        Set<Group> resultGroups;
        if (!CommonUtils.isNullOrEmpty(includeChild) && includeChild) {//??????????????????
            // ????????????????????????
            totalGroups.addAll(userDao.findGroupBySortNum(totalGroups.stream().map(Group::getSortNum).collect(Collectors.toSet()), "1"));
            GroupUtils.setChildren(totalGroups); //???????????????
            //?????????????????????  ??? ???????????????????????????
            resultGroups = GroupUtils.getGroupChildrenTotalSingleLevel(GroupUtils.filterByGroupIds(totalGroups, new HashSet<>(groupIds)));
        } else {
            resultGroups = totalGroups;
        }
        //groupNameMap??????
        // ???????????????????????????map
        Map<String, String> groupIdNameMap = resultGroups.stream().collect(Collectors.toMap(Group::getId, Group::getName));
        // ??????????????????id ??????
        Set<String> groupIdSet = totalGroups.stream().map(Group::getId).collect(Collectors.toSet());
        //??????????????????
        List<DemandBaseInfo> demandByLeaderGroup = demandDao.findDemandByLeaderGroup(groupIdSet, priority);
        //????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        demandSpOrCanRemove(demandByLeaderGroup);//??????/??????
        //groupId??????Map??????
        Map<String, List<DemandBaseInfo>> groupDemandMap = demandByLeaderGroup.stream().collect(Collectors.groupingBy(DemandBaseInfo::getDemandLeaderGroup));
        //??????????????????
        List<User> totalUser = userDao.findUserByGroupIds(groupIdSet);
        //????????????  - ????????????????????????????????????????????????????????????????????????????????????????????????????????????
        userUtils.removeByUnResourceRoleAndLabel(totalUser);
        // ????????????map ???key ?????????id ???value ???????????????
        Map<String, List<User>> groupIdUserMap = totalUser.stream().collect(Collectors.groupingBy(User::getGroupId));
        Calendar calendar = Calendar.getInstance();
        TimeUtils.setDayMin(calendar);
        Date today = calendar.getTime();
        calendar.add(Calendar.DAY_OF_WEEK, 7);
        Date future = calendar.getTime();
        return new ArrayList<DemandStatisticsVo>() {{
            //???????????? - ??????????????????????????????????????????????????????????????????????????????
            DemandStatisticsVo total = new DemandStatisticsVo("??????");
            for (Group group : resultGroups) {
                add(new DemandStatisticsVo(groupIdNameMap.get(group.getId())) {{
                    List<DemandBaseInfo> groupDemands = new ArrayList<>();
                    Set<String> groupUsers = new HashSet<>();
                    if (!CommonUtils.isNullOrEmpty(groupDemandMap.get(group.getId()))) {
                        groupDemands.addAll(groupDemandMap.get(group.getId()));
                    }
                    if (!CommonUtils.isNullOrEmpty(groupIdUserMap.get(group.getId()))) {
                        List<User> users = groupIdUserMap.get(group.getId());
                        groupUsers.addAll(users.stream().map(User::getId).collect(Collectors.toSet()));
                    }
                    if (!CommonUtils.isNullOrEmpty(group.getChildren())) {
                        for (Group child : group.getChildren()) {
                            if (!CommonUtils.isNullOrEmpty(groupDemandMap.get(child.getId()))) {
                                groupDemands.addAll(groupDemandMap.get(child.getId()));
                            }
                            if (!CommonUtils.isNullOrEmpty(groupIdUserMap.get(child.getId()))) {
                                List<User> users = groupIdUserMap.get(child.getId());
                                groupUsers.addAll(users.stream().map(User::getId).collect(Collectors.toSet()));
                            }
                        }
                    }
                    int totalTechCount = 0;
                    int totalBusCount = 0;
                    int totalDailyCount = 0;
                    if (!CommonUtils.isNullOrEmpty(groupDemands)) {
                        for (DemandBaseInfo demand : groupDemands) {
                            if (!CommonUtils.isNullOrEmpty(demand.getDemandStatusSpecial()) && (demand.getDemandStatusSpecial() == 1 || demand.getDemandStatusSpecial() == 2)) {
                                getWait().count2Plus(demand.getDemandType());
                                total.getWait().count2Plus(demand.getDemandType());
                            } else {
                                if (!CommonUtils.isNullOrEmpty(demand.getDemandStatusNormal())) {
                                    switch (DemandEnum.DemandStatusEnum.getByValue(demand.getDemandStatusNormal())) {
                                        case PRE_EVALUATE:
                                            getPreEvaluate().count2Plus(demand.getDemandType());
                                            total.getPreEvaluate().count2Plus(demand.getDemandType());
                                            total.getTotal().count2Plus(demand.getDemandType());
                                            break;
                                        case EVALUATE:
                                            getEvaluate().count2Plus(demand.getDemandType());
                                            total.getEvaluate().count2Plus(demand.getDemandType());
                                            total.getTotal().count2Plus(demand.getDemandType());
                                            break;
                                        case PRE_IMPLEMENT:
                                            getPreImplement().count2Plus(demand.getDemandType());
                                            total.getPreImplement().count2Plus(demand.getDemandType());
                                            total.getTotal().count2Plus(demand.getDemandType());
                                            String planStartDate = demand.getPlanStartDate();
                                            if (null != planStartDate) {
                                                try {
                                                    Date parse = TimeUtils.FORMAT_DATESTAMP.parse(planStartDate);
                                                    if (0 >= parse.compareTo(future) && 0 <= parse.compareTo(today)) {
                                                        getPreImplement().lineCount2Plus(demand.getDemandType());
                                                        total.getPreImplement().lineCount2Plus(demand.getDemandType());
                                                    }
                                                } catch (ParseException e) {
                                                }
                                            }
                                            break;
                                        case DEVELOP:
                                            getLeaderDevelop().count2Plus(demand.getDemandType());
                                            total.getLeaderDevelop().count2Plus(demand.getDemandType());
                                            total.getTotal().count2Plus(demand.getDemandType());
                                            break;
                                        case SIT:
                                            getSit().count2Plus(demand.getDemandType());
                                            total.getSit().count2Plus(demand.getDemandType());
                                            total.getTotal().count2Plus(demand.getDemandType());
                                            break;
                                        case UAT:
                                            getUat().count2Plus(demand.getDemandType());
                                            total.getUat().count2Plus(demand.getDemandType());
                                            total.getTotal().count2Plus(demand.getDemandType());
                                            break;
                                        case REL:
                                            getRel().count2Plus(demand.getDemandType());
                                            total.getRel().count2Plus(demand.getDemandType());
                                            total.getTotal().count2Plus(demand.getDemandType());
                                            break;
                                        case PRODUCT:
                                            getPro().count2Plus(demand.getDemandType());
                                            total.getPro().count2Plus(demand.getDemandType());
                                            break;
                                    }
                                }
                            }
                        }
                        for (DemandStatisticsType type : new ArrayList<DemandStatisticsType>() {{
                            add(getPreEvaluate());
                            add(getEvaluate());
                            add(getPreImplement());
                            add(getLeaderDevelop());
                            add(getJoinDevelop());
                            add(getSit());
                            add(getUat());
                            add(getRel());
                            // add(getPro());//?????????????????????????????????
                        }}) {
                            totalTechCount += type.getTechCount();
                            totalBusCount += type.getBusCount();
                            totalDailyCount += type.getDailyCount();
                        }
                    }
                    //????????????????????????????????????
                    List<DemandBaseInfo> unLeaderDemand = unLeaderDemand(group);
                    for (DemandBaseInfo demand : unLeaderDemand) {
                        if (DemandEnum.DemandStatusEnum.DEVELOP.equals(DemandEnum.DemandStatusEnum.getByValue(demand.getDemandStatusNormal()))) {
                            getJoinDevelop().count2Plus(demand.getDemandType());
                            total.getJoinDevelop().count2Plus(demand.getDemandType());
                            total.getTotal().count2Plus(demand.getDemandType());
                        }
                    }
                    getTotal().setTechCount(totalTechCount);
                    getTotal().setBusCount(totalBusCount);
                    getTotal().setDailyCount(totalDailyCount);
                    getTotal().setTotalCount(totalTechCount + totalBusCount + totalDailyCount);
                    int person = groupUsers.size();
                    setBusAvg(0 == person ? "???" : new BigDecimal(getTotal().getBusCount()).divide(new BigDecimal(person), 2, BigDecimal.ROUND_HALF_UP).toString());
                    setDevelopAvg(0 == person ? "???" : new BigDecimal(getLeaderDevelop().getBusCount() + getLeaderDevelop().getTechCount() + getLeaderDevelop().getDailyCount() + getJoinDevelop().getBusCount() + getJoinDevelop().getTechCount() + getJoinDevelop().getDailyCount()).divide(new BigDecimal(person), 2, BigDecimal.ROUND_HALF_UP).toString());
                }});
            }
            total.getTotal().setTotalCount(total.getTotal().getBusCount() + total.getTotal().getTechCount() + total.getTotal().getDailyCount());
            total.setBusAvg(0 == totalUser.size() ? "???" : new BigDecimal(total.getTotal().getBusCount()).divide(new BigDecimal(totalUser.size()), 2, BigDecimal.ROUND_HALF_UP).toString());
            total.setDevelopAvg(0 == totalUser.size() ? "???" : new BigDecimal(total.getLeaderDevelop().getBusCount() + total.getLeaderDevelop().getTechCount() + total.getLeaderDevelop().getDailyCount() + total.getJoinDevelop().getBusCount() + total.getJoinDevelop().getTechCount() + total.getJoinDevelop().getDailyCount()).divide(new BigDecimal(totalUser.size()), 2, BigDecimal.ROUND_HALF_UP).toString());
            sort((a, b) -> {
                if (a.getGroupName().compareTo(b.getGroupName()) < 0) return -1;
                else return 1;
            });
            add(total);
        }};
    }

    @Override
    public PageVo<CommitStatisticsVo> queryCommitStatistics(CommitStatisticsDto dto) throws Exception {
        if (null != dto.getUser() && !CommonUtils.isNullOrEmpty(dto.getStatisticRange())) {
            Map<String, User> emailUser = new LinkedHashMap<>();
            Set<String> roleIds = new HashSet<>();
            Set<String> companyIds = new HashSet<>();
            Set<String> groupIds = new HashSet<>();
            boolean flag = CommonUtils.isNullOrEmpty(dto.getIncludeChild()) ? false : dto.getIncludeChild();
            Set<String> groupIdSet = dto.getUser().getGroupIds();
            if (flag) {
                List<Group> groups = userDao.findGroups(new HashSet<>(groupIdSet), "1");
                Set<Group> totalGroup = getGroupChildrenTotal(groups);
                totalGroup.stream().forEach(x -> groupIdSet.add(x.getId()));
            }
            dto.getUser().setGroupIds(groupIdSet);
            PageVo<User> userPages = userDao.find(dto.getUser(), dto.getPage());
            userPages.getData().forEach(user -> {
                user.setEmail(user.getEmail().toLowerCase());//????????????????????????????????????????????????????????????????????????
                emailUser.put(user.getEmail(), user);
                companyIds.add(user.getCompanyId());
                roleIds.addAll(user.getRoleId());
                groupIds.add(user.getGroupId());
            });
            Map<String, String> roleMap = userDao.findRole(roleIds).stream().collect(Collectors.toMap(Role::getId, Role::getName));
            Map<String, String> companyMap = userDao.findCompany(companyIds).stream().collect(Collectors.toMap(Company::getId, Company::getName));
            Map<String, String> groupMap = userDao.findGroups(groupIds).stream().collect(Collectors.toMap(Group::getId, Group::getName));
            //??????????????????????????????????????????????????????
            Set<Integer> appGitIds = appDao.findAllApp().stream().filter(item -> !CommonUtils.isNullOrEmpty(item.getGitlabProjectId())).map(AppEntity::getGitlabProjectId).collect(Collectors.toSet());//????????????gitid
            Set<String> componentGitIds = componentDao.findAllComponent().stream().filter(item -> !CommonUtils.isNullOrEmpty(item.getGitlabId())).map(ComponentEntity::getGitlabId).collect(Collectors.toSet());//??????????????????gitid
            Set<String> mpaasComponentGitIds = componentDao.findAllMpaasComponent().stream().filter(item -> !CommonUtils.isNullOrEmpty(item.getGitlabId())).map(MpassComponentEntity::getGitlabId).collect(Collectors.toSet());//??????????????????gitId
            Set<String> archeGitIds = componentDao.findAllArche().stream().filter(item -> !CommonUtils.isNullOrEmpty(item.getGitlabId())).map(ArchetypeEntity::getGitlabId).collect(Collectors.toSet());//??????????????????gitid
            Set<String> mpaasArcheGitIds = componentDao.findAllMpaasArche().stream().filter(item -> !CommonUtils.isNullOrEmpty(item.getGitlabId())).map(MpassArchetypeEntity::getGitlabId).collect(Collectors.toSet());//??????????????????gitId
            Set<String> imageGitIds = componentDao.findAllImage().stream().filter(item -> !CommonUtils.isNullOrEmpty(item.getGitlabId())).map(BaseImageEntity::getGitlabId).collect(Collectors.toSet());//????????????gitid
            Set<Integer> gitIds = this.includeStatisticRange(dto.getStatisticRange(), appGitIds, componentGitIds, mpaasComponentGitIds, archeGitIds, mpaasArcheGitIds, imageGitIds);//????????????gitId??????
            List<Commit> byCommitterEmail = new ArrayList<>();
            if (!CommonUtils.isNullOrEmpty(emailUser.keySet())) {
                byCommitterEmail.addAll(gitDao.findByCommitterEmail(emailUser.keySet(), dto.getStartDate(), dto.getEndDate(), gitIds));
            }
            Map<String, List<Commit>> emailCommitMap = byCommitterEmail.stream().peek(commit -> commit.setCommitterEmail(commit.getCommitterEmail().toLowerCase())).collect(Collectors.groupingBy(Commit::getCommitterEmail));
            return new PageVo<CommitStatisticsVo>() {{
                setTotal(userPages.getTotal());
                setData(new ArrayList<CommitStatisticsVo>() {{
                    for (User user : userPages.getData()) {
                        CommitStatisticsVo commitStatisticsVo = new CommitStatisticsVo();
                        BeanUtils.copyProperties(user, commitStatisticsVo);
                        commitStatisticsVo.setUserId(user.getId());
                        if (!CommonUtils.isNullOrEmpty(user.getRoleId())) {
                            commitStatisticsVo.setRole(new HashSet<String>() {{
                                for (String roleId : user.getRoleId()) {
                                    if (null != roleMap.get(roleId)) add(roleMap.get(roleId));
                                }
                            }});
                        }
                        if (null != companyMap.get(user.getCompanyId()))
                            commitStatisticsVo.setCompany(companyMap.get(user.getCompanyId()));
                        if (null != groupMap.get(user.getGroupId()))
                            commitStatisticsVo.setGroup(groupMap.get(user.getGroupId()));
                        int total = 0;
                        int additions = 0;
                        int deletions = 0;
                        String email = user.getEmail();
                        List<Commit> commits = emailCommitMap.get(email);
                        Date startDate = null;
                        Date endDate = null;
                        if (!CommonUtils.isNullOrEmpty(commits)) {
                            Date committedDate = TimeUtils.FORMAT_TIMESTAMP.parse(commits.get(0).getCommittedDate());
                            startDate = committedDate;
                            endDate = committedDate;
                            for (Commit commit : commits) {
                                if (!CommonUtils.isNullOrEmpty(commit.getStats())) {
                                    total += commit.getStats().getTotal();
                                    additions += commit.getStats().getAdditions();
                                    deletions += commit.getStats().getDeletions();
                                    Date parse = TimeUtils.FORMAT_TIMESTAMP.parse(commit.getCommittedDate());
                                    if (parse.compareTo(startDate) < 0) startDate = parse;
                                    if (parse.compareTo(endDate) > 0) endDate = parse;
                                }
                            }
                        }
                        commitStatisticsVo.setTotal(total);
                        commitStatisticsVo.setAdditions(additions);
                        commitStatisticsVo.setDeletions(deletions);
                        commitStatisticsVo.setStartDate(startDate == null ? null : TimeUtils.FORMAT_DATESTAMP.format(startDate));
                        commitStatisticsVo.setEndDate(startDate == null ? null : TimeUtils.FORMAT_DATESTAMP.format(endDate));
                        add(commitStatisticsVo);
                    }
                }});
            }};
        }
        return null;
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????
     *
     * @return
     */
    private Set<Integer> includeStatisticRange(List<String> statisticRange, Set<Integer> appGitIds, Set<String> componentGitIds, Set<String> mpaasComponentGitIds, Set<String> archeGitIds, Set<String> mpaasArcheGitIds, Set<String> imageGitIds) {
        return new HashSet<Integer>() {{
            if (statisticRange.contains("app")) {
                addAll(appGitIds);
            }
            if (statisticRange.contains("component")) {
                addAll(componentGitIds.stream().map(x -> Integer.parseInt(x)).collect(Collectors.toSet()));
                addAll(mpaasComponentGitIds.stream().map(x -> Integer.parseInt(x)).collect(Collectors.toSet()));
            }
            if (statisticRange.contains("archetype")) {
                addAll(archeGitIds.stream().map(x -> Integer.parseInt(x)).collect(Collectors.toSet()));
                addAll(mpaasArcheGitIds.stream().map(x -> Integer.parseInt(x)).collect(Collectors.toSet()));
            }
            if (statisticRange.contains("baseImage")) {
                addAll(imageGitIds.stream().map(x -> Integer.parseInt(x)).collect(Collectors.toSet()));
            }
        }};
    }

    @Override
    public PageVo<Commit> queryCommitByUser(CommitStatisticsDto dto) throws Exception {
        if (CommonUtils.isNullOrEmpty(dto.getStatisticRange())) {
            return null;
        }
        //??????????????????????????????????????????????????????
        Set<Integer> appGitIds = appDao.findAllApp().stream().filter(item -> !CommonUtils.isNullOrEmpty(item.getGitlabProjectId())).map(AppEntity::getGitlabProjectId).collect(Collectors.toSet());//????????????gitid
        Set<String> componentGitIds = componentDao.findAllComponent().stream().filter(item -> !CommonUtils.isNullOrEmpty(item.getGitlabId())).map(ComponentEntity::getGitlabId).collect(Collectors.toSet());//??????????????????gitid
        Set<String> mpaasComponentGitIds = componentDao.findAllMpaasComponent().stream().filter(item -> !CommonUtils.isNullOrEmpty(item.getGitlabId())).map(MpassComponentEntity::getGitlabId).collect(Collectors.toSet());//??????????????????gitId
        Set<String> archeGitIds = componentDao.findAllArche().stream().filter(item -> !CommonUtils.isNullOrEmpty(item.getGitlabId())).map(ArchetypeEntity::getGitlabId).collect(Collectors.toSet());//??????????????????gitid
        Set<String> mpaasArcheGitIds = componentDao.findAllMpaasArche().stream().filter(item -> !CommonUtils.isNullOrEmpty(item.getGitlabId())).map(MpassArchetypeEntity::getGitlabId).collect(Collectors.toSet());//??????????????????gitId
        Set<String> imageGitIds = componentDao.findAllImage().stream().filter(item -> !CommonUtils.isNullOrEmpty(item.getGitlabId())).map(BaseImageEntity::getGitlabId).collect(Collectors.toSet());//????????????gitid
        Set<Integer> gitIds = this.includeStatisticRange(dto.getStatisticRange(), appGitIds, componentGitIds, mpaasComponentGitIds, archeGitIds, mpaasArcheGitIds, imageGitIds);//????????????gitId??????

        return gitDao.findByCommitterEmail(new HashSet<String>() {{
            add(dto.getUser().getEmail());
        }}, dto.getStartDate(), dto.getEndDate(), gitIds, dto.getPage());
    }

    @Override
    public void exportCommitStatistics(CommitStatisticsDto dto, HttpServletResponse resp) throws Exception {
        //   ?????????workbook
        XSSFWorkbook workbook = new XSSFWorkbook();
        List<CommitStatisticsVo> commitList = queryCommitStatistics(dto).getData();
        XSSFSheet sheet = workbook.createSheet("??????????????????");
        XSSFRow row = sheet.createRow(0);
        row.createCell(0).setCellValue("??????");
        row.createCell(1).setCellValue("git ?????????");
        row.createCell(2).setCellValue("??????");
        row.createCell(3).setCellValue("??????");
        row.createCell(4).setCellValue("??????");
        row.createCell(5).setCellValue("?????????");
        row.createCell(6).setCellValue("????????????");
        row.createCell(7).setCellValue("????????????");
        row.createCell(8).setCellValue("????????????");
        row.createCell(9).setCellValue("????????????");

        int i = 0;
        for (CommitStatisticsVo commitStatisticsVo : commitList) {
            i++;
            XSSFRow row1 = sheet.createRow(i);
            row1.createCell(0).setCellValue(commitStatisticsVo.getUserNameCn());
            row1.createCell(1).setCellValue(commitStatisticsVo.getUserNameEn());
            row1.createCell(2).setCellValue(commitStatisticsVo.getCompany());
            row1.createCell(3).setCellValue(String.join(",", commitStatisticsVo.getRole()));
            row1.createCell(4).setCellValue(commitStatisticsVo.getGroup());
            row1.createCell(5).setCellValue(commitStatisticsVo.getTotal());
            row1.createCell(6).setCellValue(commitStatisticsVo.getAdditions());
            row1.createCell(7).setCellValue(commitStatisticsVo.getDeletions());
            row1.createCell(8).setCellValue(commitStatisticsVo.getStartDate());
            row1.createCell(9).setCellValue(commitStatisticsVo.getEndDate());
        }
        OutputStream outputStream = resp.getOutputStream();
        resp.reset();
        resp.setContentType("application/octet-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        resp.setHeader("Content-Disposition", "attachment;filename=" +
                "??????????????????" + ".xlsx");
        workbook.write(outputStream);
        outputStream.close();
    }

    private void demandSpOrCanRemove(List<DemandBaseInfo> demandByLeaderGroup) {
        demandByLeaderGroup.removeIf(demand -> !CommonUtils.isNullOrEmpty(demand.getIsCanceled()) && demand.getIsCanceled());
    }

    /**
     * ?????????????????????????????????????????????????????????????????????groupBy????????????Id???????????????
     */
    private List<DemandBaseInfo> unLeaderDemand(Group group) {
        Set<String> groupIds = new HashSet<String>() {{
            add(group.getId());
            if (!CommonUtils.isNullOrEmpty(group.getChildren())) {
                addAll(group.getChildren().stream().map(Group::getId).collect(Collectors.toSet()));
            }
        }};
        List<Task> groupTask = taskDao.findByGroupIds(groupIds);
        //????????????
        List<String> stages = new ArrayList<String>() {{
            add(CREATE_APP.getName());
            add(CREATE_INFO.getName());
            add(CREATE_FEATURE.getName());
            add(DEVELOP.getName());
            add(SIT.getName());
            add(UAT.getName());
            add(REL.getName());
        }};
        groupTask.removeIf(task -> !stages.contains(task.getStage()));
        Set<String> unLeaderDemandIds = groupTask.stream().filter(task -> null != task.getRqrmntNo()).map(Task::getRqrmntNo).collect(Collectors.toSet());
        return new ArrayList<DemandBaseInfo>() {{
            if (!CommonUtils.isNullOrEmpty(unLeaderDemandIds)) {
                List<DemandBaseInfo> unLeaderDemand = demandDao.findDemandByIdSet(unLeaderDemandIds);
                demandSpOrCanRemove(unLeaderDemand);
                unLeaderDemand.removeIf(demand -> CommonUtils.isNullOrEmpty(demand.getDemandLeaderGroup()) || groupIds.contains(demand.getDemandLeaderGroup()));
                addAll(unLeaderDemand);
            }
        }};
    }

    /**
     * ????????????(??????-??????{??????|??????})map????????????
     */
    private Map<String, Integer[]> getPeriodDemandCountMap(Map<String, Date[]>
                                                                   periodMap, List<DemandBaseInfo> demandBaseInfoList) throws ParseException {
        Map<String, Integer[]> periodDemandCountMap = new LinkedHashMap<>();//??????(??????-??????{??????|??????})map????????????
        for (String period : periodMap.keySet()) {
            periodDemandCountMap.put(period, new Integer[]{0, 0});//??????|??????
            Date[] dateArray = periodMap.get(period);//????????????????????????????????? - ????????????????????????
            for (DemandBaseInfo item : demandBaseInfoList) {
                if (!CommonUtils.isNullOrEmpty(item.getDemandCreateTime())) {
                    if (10 != item.getDemandCreateTime().length()) {
                        item.setDemandCreateTime(item.getDemandCreateTime().substring(0, 10));
                    }
                    Date demandCreateTime = TimeUtils.FORMAT_DATESTAMP.parse(item.getDemandCreateTime());
                    if (0 <= demandCreateTime.compareTo(dateArray[0]) && 0 >= demandCreateTime.compareTo(dateArray[1])) {
                        periodDemandCountMap.get(period)[0]++;
                    }
                }
                if (!CommonUtils.isNullOrEmpty(item.getRealProductDate())) {
                    Date realProductDate = TimeUtils.FORMAT_DATESTAMP.parse(item.getRealProductDate());
                    if (0 <= realProductDate.compareTo(dateArray[0]) && 0 >= realProductDate.compareTo(dateArray[1])) {
                        periodDemandCountMap.get(period)[1]++;
                    }
                }
            }
        }
        return periodDemandCountMap;
    }

    /**
     * ??????????????????????????????list??????
     *
     * @param resource
     * @return
     */
    private Set<Group> getChildrenTotal(List<Group> resource) {
        return new HashSet<Group>() {{
            for (Group item : resource) {
                add(item);
                if (!CommonUtils.isNullOrEmpty(item.getChildren())) {
                    addAll(getChildrenTotal(item.getChildren()));
                }
            }
        }};
    }

    private Set<Group> getGroupChildrenTotal(List<Group> groups) throws Exception {
        return new HashSet<Group>() {{//???????????????
            addAll(groups);
            addAll(userDao.findGroupBySortNum(groups.stream().map(Group::getSortNum).collect(Collectors.toSet()), "1"));//???????????????
        }};
    }

    private Set<String> forAvg(List<String> taskPersonTypeForAvg, Task task) {
        return new HashSet<String>() {{
            if (taskPersonTypeForAvg.contains("spdbMaster") && !CommonUtils.isNullOrEmpty(task.getSpdbMaster())) {
                addAll(task.getSpdbMaster());
            }
            if (taskPersonTypeForAvg.contains("master") && !CommonUtils.isNullOrEmpty(task.getMaster())) {
                addAll(task.getMaster());
            }
            if (taskPersonTypeForAvg.contains("developer") && !CommonUtils.isNullOrEmpty(task.getDeveloper())) {
                addAll(task.getDeveloper());
            }
            if (taskPersonTypeForAvg.contains("tester") && !CommonUtils.isNullOrEmpty(task.getTester())) {
                addAll(task.getTester());
            }
        }};
    }
}
