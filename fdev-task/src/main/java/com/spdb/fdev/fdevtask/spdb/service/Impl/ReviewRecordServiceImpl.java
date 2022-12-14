package com.spdb.fdev.fdevtask.spdb.service.Impl;

import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.fdevtask.base.dict.Constants;
import com.spdb.fdev.fdevtask.base.dict.Dict;
import com.spdb.fdev.fdevtask.base.dict.ErrorConstants;
import com.spdb.fdev.fdevtask.base.utils.AsyncService;
import com.spdb.fdev.fdevtask.base.utils.CommonUtils;
import com.spdb.fdev.fdevtask.base.utils.MailUtil;
import com.spdb.fdev.fdevtask.spdb.dao.IFdevTaskDao;
import com.spdb.fdev.fdevtask.spdb.dao.ReviewRecordDao;
import com.spdb.fdev.fdevtask.spdb.entity.*;
import com.spdb.fdev.fdevtask.spdb.service.*;
import org.apache.http.entity.ContentType;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Lazy;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import sun.misc.BASE64Decoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RefreshScope
public class ReviewRecordServiceImpl implements ReviewRecordService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ReviewRecordDao reviewRecordDao;

    @Autowired
    private IUserApi userApi;

    @Autowired
    private IFdevTaskService iFdevTaskService;

    @Autowired
    @Lazy
    private AsyncService asyncService;

    @Autowired
    private MailUtil mailUtil;

    @Autowired
    private ReviewRecordService reviewRecordService;

    @Autowired
    private IUserApi iUserApi;

    @Autowired
    private INotifyApi iNotifyApi;

    @Autowired
    private RequirementApi requirementApi;

    @Autowired
    private DemandService demandService;

    @Autowired
    private IFdevTaskDao iFdevTaskDao;

    @Autowired
    private IDocService iDocService;

    @Value("${review.href}")
    private String href;

    @Value("${isSendReviewEmail}")
    private boolean issendmail;

    @Override
    public Map fuzzyQuery(String key, int page, int pageSize, Map param) {
        return reviewRecordDao.fuzzySearch(key, page, pageSize, param);
    }

    @Override
    public void saveSqlReview(FdevTask task) throws Exception {
        try {
            TaskReview review = task.getReview();
            if (!CommonUtils.isNullOrEmpty(review)) {
                TaskReviewChild[] sqls = review.getData_base_alter();
                String reviewType = "";
                if (!CommonUtils.isNullOrEmpty(sqls)) {
                    for (TaskReviewChild sql : sqls) {
                        String id = sql.getId();
                        String taskName = task.getName();
                        String taskId = task.getId();
                        String appName = task.getProject_name();
                        String appId = task.getProject_id();
                        String[] master = task.getMaster();
                        List masterInfo = reformate(master);
                        String group = task.getGroup();
                        Map groupInfo = userApi.queryGroup(new HashMap() {{
                            put(Dict.ID, group);
                        }});
                        String groupName = (String) groupInfo.get(Dict.NAME);
                        String status = "";
                        if ("???".equalsIgnoreCase(sql.getName())) {
                            status = "1";
                            reviewType += "???????????????";
                            reviewRecordDao.save(id, id, taskName, taskId, appName, appId, masterInfo, group, groupName, status, reviewType);
                        }
                        //   ???????????????????????????????????????
                        if ("???".equalsIgnoreCase(sql.getName())) {
                            ReviewRecord r = reviewRecordDao.queryById(id);
                            if (CommonUtils.isNullOrEmpty(r)) {
                                return;
                            }
                            r.setStatus("0");
                            update(r);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new FdevException(ErrorConstants.SQL_REVIEW_ERROR, new String[]{});
        }
    }

    @Override
    public ReviewRecord update(ReviewRecord reviewRecord) throws Exception {
        String reviewStatus = reviewRecord.getReviewStatus();
        ReviewRecord newOne = reviewRecordDao.update(reviewRecord);
        if (Dict.APPROVE.equals(reviewStatus)) {
            iFdevTaskService.updateTaskView(Dict.DATA_BASE_ALTER, reviewRecord.getId(), null, true);
        } else {
            iFdevTaskService.updateTaskView(Dict.DATA_BASE_ALTER, reviewRecord.getId(), null, false);
        }
        return newOne;
    }

    @Override
    public Map queryPageable(Map param, int page, int pageSize) {
        return reviewRecordDao.queryPageable(param, page, pageSize);
    }

    private List reformate(String[] master) throws Exception {
        Map maptmp = new HashMap();
        maptmp.put(Dict.IDS, Arrays.asList(master));
        List result = new ArrayList();
        List userInfo = (ArrayList) userApi.queryUserList(maptmp);
        for (Object user : userInfo) {
            Map tmp = (Map) user;
            result.add(new HashMap() {{
                put("cid", tmp.get(Dict.ID));
                put(Dict.NAME, tmp.get(Dict.USER_NAME_CN));
            }});
        }
        return result;
    }

    /**
     * ??????id
     *
     * @param id
     * @return
     */
    @Override
    public Map queryTaskReview(String id) {
        Map<String, Object> result = new HashMap<>();
        FdevTask task = iFdevTaskDao.queryById(id);
        try {
            if (!CommonUtils.isNullOrEmpty(task)) {
                List<Object> toDoTasks = new ArrayList<>();
                boolean allPass = true;
                TaskReview view = task.getReview();
                Map<String, Object> rMap = new HashMap<>();
                rMap.putAll(CommonUtils.beanToMap(view));
                if (!CommonUtils.isNullOrEmpty(view.getData_base_alter())) {
                    List dataBase = (List) rMap.get(Dict.DATA_BASE_ALTER);
                    Map sql = (Map) dataBase.get(0);
                    sql.putAll(fileList(task, "?????????-?????????????????????"));
//                    dataBase.add(0,sql);
                    rMap.put(Dict.DATA_BASE_ALTER, dataBase);
                }
                if (!view.allChecked()) {
                    allPass = false;
                    rMap.put(Dict.AUDITRESULT, false);
                } else {
                    rMap.put(Dict.AUDITRESULT, true);
                }
                toDoTasks.add(rMap);
                if (allPass) {
                    result.put(Dict.ALLAUDITRESULT, true);
                } else {
                    result.put(Dict.ALLAUDITRESULT, false);
                }
                result.put(Dict.TASKLIST, toDoTasks);
                List resList = new ArrayList();
                ReviewRecord r = new ReviewRecord();
                r.setTaskId(id);
                List<ReviewRecord> listr = reviewRecordDao.query(r);
                listr.forEach(n -> resList.add(n.getReviewStatus()));
                if(CommonUtils.isNullOrEmpty(resList)) {
                    result.put(Dict.STATUS, "");
                }else {
                    result.put(Dict.STATUS, resList.get(0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private Map fileList(FdevTask task, String type) throws Exception {
        List<Map<String, String>> files = task.getNewDoc();
        if(CommonUtils.isNullOrEmpty(files))return new HashMap();
        return new HashMap() {{
            put("files", files.stream().filter(file -> type.equals(file.get(Dict.TYPE))).collect(Collectors.toMap(k -> k.get(Dict.NAME), v -> v.get(Dict.PATH))));
        }};
    }

    @Override
    public void deleteTaskRecord(FdevTask ntask, FdevTask otask) {
        String stage = ntask.getStage();
        if (CommonUtils.isNullOrEmpty(stage))
            return;
        List list = new ArrayList();
        list.add(Dict.TASK_STAGE_ABORT);
        list.add(Dict.TASK_STAGE_DISCARD);
        list.add(Dict.TASK_STAGE_FILE);
        list.add(Dict.TASK_STAGE_PRODUCTION);
        if (list.contains(stage)) {
            ReviewRecord r = new ReviewRecord();
            TaskReview review = otask.getReview();
            TaskReviewChild[] sqlReviews = review.getData_base_alter();
            //???????????????????????????????????????????????????????????????
            if(Dict.TASK_STAGE_FILE.equals(stage) && !CommonUtils.isNullOrEmpty(sqlReviews)){
                ReviewRecord rup = new ReviewRecord();
                rup.setId(ntask.getId());
                rup.setReviewStatus("?????????");
                reviewRecordDao.updateById(rup);
            }
            for (int i = 0; i < sqlReviews.length; i++) {
                TaskReviewChild sql = sqlReviews[i];
                r.setId(sql.getId());
                r.setStatus("0");
                try {
                    update(r);
                    logger.info("?????????????????? ??????id:{} ", otask.getId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void sendMail(Map mapping, ReviewRecordHistory reviewRecordHistory) {
        //http://10.134.13.25:9093/fdev/#/release/ReviewRelatedItems
        try {
            Set<String> userIds = new HashSet<>();
            String task_id = reviewRecordHistory.getTask_id();
            FdevTask task = new FdevTask();
            task.setId(task_id);
            List emailList = new ArrayList();
            task = iFdevTaskService.queryTaskById(task_id);
            String implement_unit_no = task.getFdev_implement_unit_no();
            String rqrmntNum = "";
            if(!CommonUtils.isNullOrEmpty(implement_unit_no)){
                Map rqrmnt_info = demandService.queryByFdevNo(task.getRqrmnt_no(),implement_unit_no);
                Map rqrmnt = (Map) rqrmnt_info.get(Dict.DEMAND_BASEINFO);
                rqrmntNum = (String) rqrmnt.get("oa_contact_no");
            }
            String[] master = Optional.ofNullable(task.getMaster()).orElse(new String[0]);
            String[] spdb_master = Optional.ofNullable(task.getSpdb_master()).orElse(new String[0]);
            String[] developer = Optional.ofNullable(task.getDeveloper()).orElse(new String[0]);
            String[] tester = Optional.ofNullable(task.getTester()).orElse(new String[0]);
            String db_type = reviewRecordHistory.getDb_type();
            String groupId = task.getGroup();
            Map init_auditor = reviewRecordHistory.getInit_auditor();
            if (init_auditor != null) {
                userIds.add((String) init_auditor.get("user_id"));
            }
            for (String s : master) {
                userIds.add(s);
            }
            for (String s : spdb_master) {
                userIds.add(s);
            }
            if (developer != null) {
                for (String s : developer) {
                    userIds.add(s);
                }
            }
            if (tester != null) {
                for (String s : tester) {
                    userIds.add(s);
                }
            }

            for (String userId : userIds) {
                Map map = new HashMap();
                map.put(Dict.ID, userId);
                Map map1 = iUserApi.queryUser(map);
                emailList.add(map1.get(Dict.EMAIL));
            }
            mapping.put(Dict.TASKNAME, reviewRecordHistory.getTask_name());
            mapping.put("href", href + "?groupid=" + groupId);
            mapping.put("rqrmntNum", rqrmntNum);
            mapping.put(Dict.DB_TYPE, db_type);
            mapping.put("system_remould", task.getSystem_remould());
            mapping.put("impl_data", task.getImpl_data());
            String reviewContent = getReviewRecord(task_id, new HashMap());
            mapping.put("reviewRecord", reviewContent);
            mailUtil.sendReviewRecordEmail(Constants.EMAIL_REVIEWUPDATE, mapping, new ArrayList(new HashSet(emailList)), db_type);
        } catch (Exception e) {
            logger.error("??????????????????");
//            throw new FdevException(ErrorConstants.SEND_EMAIL_ERROR);
        }
    }


    @Override
    public List<ReviewRecordHistory> queryReviewRecordHistoryByTaskId(String task_id) {
        ReviewRecordHistory reviewRecordHistory = new ReviewRecordHistory();
        reviewRecordHistory.setTask_id(task_id);
        return reviewRecordDao.queryReviewRecordHistory(reviewRecordHistory);
    }

    @Override
    public void saveReviewRecord(Map request) throws Exception {
        String task_id = (String) request.get(Dict.TASK_ID);
        FdevTask task = new FdevTask();
        task.setId(task_id);
        Map initiator = (Map) request.get("initiator");
        Map auditor = (Map) request.get("auditor");
        String review_status = (String) request.get(Dict.REVIEW_STATUS);
        //???????????????????????????????????????????????????-????????????????????????
        if (review_status.equals("????????????") && initiator != null) {
            boolean checkout = false;
            Map map2 = iFdevTaskService.queryDocDetail(task,null,null);
            if (map2 != null) {
                List<Map> doc = (List<Map>) map2.get(Dict.DOC);
                if (doc != null && doc.size() >= 1) {
                    for (Map map : doc) {
                        if ("?????????-?????????????????????".equals(map.get(Dict.TYPE))) {
                            checkout = true;
                        }
                    }
                }
            }

            if (!checkout) {
                throw new FdevException(ErrorConstants.Review_Fail, new String[]{"?????????????????????????????????????????????/?????????????????????????????????"});
            }
        }

        FdevTask oldTask = iFdevTaskService.queryTaskById(task_id);
        if (task != null ) {
            String groupId = oldTask.getGroup();
            String taskName = oldTask.getName();
            Map map2 = new HashMap();
            map2.put(Dict.ID, groupId);
            Map map3 = iUserApi.queryGroup(map2);
            String doc = (String) request.get(Dict.DOC);
            String review_type = (String) request.get(Dict.REVIEW_TYPE);
            String db_type = (String) request.get(Dict.DB_TYPE);
            if (CommonUtils.isNullOrEmpty(review_type) || CommonUtils.isNullOrEmpty(db_type)) {
                ReviewRecordHistory reviewRecordHistory = new ReviewRecordHistory();
                reviewRecordHistory.setTask_id(task_id);
                List<ReviewRecordHistory> reviewRecordHistories = reviewRecordDao.queryReviewRecordHistory(reviewRecordHistory);
                if (reviewRecordHistories != null && reviewRecordHistories.size() >= 1) {
                    review_type = reviewRecordHistories.get(0).getReview_type();
                    db_type = reviewRecordHistories.get(0).getDb_type();
                }
            }
            String init_auditor_id = (String) request.get("init_auditor_id");
            String user_id = (String) request.get(Dict.USER_ID);
            Map map = new HashMap();
            map.put(Dict.ID, user_id);
            Map map1 = iUserApi.queryUser(map);
            Map user = new HashMap();
            user.put(Dict.USER_ID, map1.get(Dict.ID));
            user.put(Dict.USER_NAME_CN, map1.get(Dict.USER_NAME_CN));
            user.put(Dict.USER_NAME_EN, map1.get(Dict.USER_NAME_EN));
            ReviewRecordHistory reviewRecordHistory = new ReviewRecordHistory();
            ReviewRecord reviewRecord2 = new ReviewRecord();
            reviewRecord2.setTaskId(task_id);
            reviewRecordHistory.setReview_status(review_status);
            reviewRecordHistory.setTask_id(task_id);
            reviewRecordHistory.setTask_name(taskName);
            reviewRecordHistory.setGroup((String) map3.get(Dict.NAME));
            reviewRecordHistory.setDoc(doc);
            reviewRecordHistory.setReview_type(review_type);
            reviewRecordHistory.setDb_type(db_type);
            Map map6 = new HashMap();
            if (initiator != null && CommonUtils.isNotNullOrEmpty((String) initiator.get(Dict.ID))) {
                reviewRecordHistory.setInitiator(initiator);
                Map initiator1 = new HashMap();
                initiator1.put(Dict.ID, initiator.get(Dict.ID));
                map6 = iUserApi.queryUser(initiator1);
            }
            if (auditor != null && CommonUtils.isNotNullOrEmpty((String) auditor.get(Dict.ID))) {
                reviewRecordHistory.setAuditor(auditor);
            }
            //?????????????????????????????????????????????
            ReviewRecord reviewRecord = new ReviewRecord();
            reviewRecord.setTaskId(task_id);
            List<ReviewRecord> records = reviewRecordDao.query(reviewRecord);
            if (records != null && records.size() >= 1) {
                ReviewRecord reviewRecord1 = records.get(0);
                if (reviewRecord1.getReviewStatus().equals(Dict.SECOND_REFUSE) && initiator != null) {
                    Map mail = new HashMap();
                    Map his = new HashMap();
                    reviewRecord1.setReviewStatus(Dict.SECOND_REVIEW);
                    ReviewRecord update = reviewRecordDao.update(reviewRecord1);
                    reviewRecordHistory.setReview_status(Dict.SECOND_REVIEW);
                    mail.put("reviewStatus", Dict.SECOND_REFUSE + "--->" + Dict.SECOND_REVIEW);
                    his.put("reviewTime", CommonUtils.dateFormat(new Date(), CommonUtils.DATE_TIME_PATTERN));
                    his.put("reviewStatus", Dict.SECOND_REVIEW);
                    his.put(Dict.USER_NAME_CN, initiator.get("initiator_name_cn"));
                    his.put(Dict.DOC, doc);
                    String reviewContent = getReviewRecord(task_id, his);
                    mail.put("reviewRecord", reviewContent);
                    reviewRecordService.sendUpdateMail(mail, update);
                    String content = "??????????????????????????????";
                    ArrayList<String> list = new ArrayList<>();
                    List<Map> jobUser = userApi.getJobUser();
                    for (Map map4 : jobUser) {
                        list.add((String) map4.get(Dict.USER_NAME_EN));
                    }
                    iNotifyApi.sendUserNotify(content, list, "0", href + "?groupid=" + groupId,"???????????????");
                }
            }
            Map init_audito = new HashMap();
            if (CommonUtils.isNotNullOrEmpty(init_auditor_id)) {
                Map map4 = new HashMap();
                Map map5 = new HashMap();
                map4.put(Dict.ID, init_auditor_id);
                init_audito = iUserApi.queryUser(map4);
                map5.put(Dict.USER_ID, init_audito.get(Dict.ID));
                map5.put(Dict.USER_NAME_CN, init_audito.get(Dict.USER_NAME_CN));
                map5.put(Dict.USER_NAME_EN, init_audito.get(Dict.USER_NAME_EN));
                reviewRecordHistory.setInit_auditor(map5);
            }
            ReviewRecordHistory reviewRecordHistory1 = reviewRecordDao.saveReviewRecord(reviewRecordHistory);
            Map mail = new HashMap();
            if (Dict.FIRST_REVIEW.equals(review_status)) {
                mail.put("reviewStatus", Dict.INIT + "--->" + Dict.FIRST_REVIEW);
                mail.put("applicant", initiator.get("initiator_name_cn") + "  " + map6.get(Dict.EMAIL));
                mail.put("plan_fire_time", oldTask.getPlan_fire_time());
                reviewRecordService.sendMail(mail, reviewRecordHistory1);
                String content = "??????????????????????????????";
                ArrayList<String> list = new ArrayList<>();
                list.add((String) init_audito.get(Dict.USER_NAME_EN));
                iNotifyApi.sendUserNotify(content, list, "0", href + "?groupid=" + groupId,"???????????????");
            }
        }
    }

    @Override
    public void sendUpdateMail(Map mapping, ReviewRecord reviewRecord) {
        try {
            List<Map> jobUser = iUserApi.getJobUser();
            List users = new ArrayList();
            List ids = new ArrayList();
            List master = reviewRecord.getMaster();
            List reviewers = reviewRecord.getReviewers();
            Map init_auditor = new HashMap();
            if (reviewers != null && reviewers.size() >= 1) {
                init_auditor = CommonUtils.obj2Map(reviewers.get(0));
            }
            String cid = (String) init_auditor.get("cid");
            String name = (String) init_auditor.get(Dict.NAME);
            Map init_auditor_user = new HashMap();
            Map init_auditor_email = new HashMap();
            if (CommonUtils.isNotNullOrEmpty(cid)) {
                init_auditor_user.put(Dict.ID, cid);
                init_auditor_email = iUserApi.queryUser(init_auditor_user);
            }
            String applicant = reviewRecord.getApplicant();
            Map user = new HashMap();
            user.put(Dict.ID, applicant);
            Map map1 = iUserApi.queryUser(user);
            String reviewStatus = reviewRecord.getReviewStatus();
            if (CommonUtils.isNotNullOrEmpty(reviewStatus) && (reviewStatus.contains("??????") || reviewStatus.equals("??????") || reviewStatus.equals("?????????"))) {
                ids.add(reviewRecord.getApplicant());
            }
            if (!CommonUtils.isNullOrEmpty(master))
                users.addAll(master);
            if (!CommonUtils.isNullOrEmpty(reviewers))
                users.addAll(reviewers);
            for (Object o : users) {
                ReviewUser tmp = (ReviewUser) o;
                String id = tmp.getcid();
                if (!CommonUtils.isNullOrEmpty(id))
                    ids.add(id);
            }
            String taskId = reviewRecord.getTaskId();
            FdevTask task = new FdevTask();
            task.setId(taskId);
            task = iFdevTaskService.queryTaskById(taskId);
            String implement_unit_no = task.getFdev_implement_unit_no();
            String rqrmntNum = "";
            if(!CommonUtils.isNullOrEmpty(implement_unit_no)){
                Map rqrmnt_info = demandService.queryByFdevNo(task.getRqrmnt_no(),implement_unit_no);
                Map rqrmnt = (Map) rqrmnt_info.get(Dict.DEMAND_BASEINFO);
                rqrmntNum = (String) rqrmnt.get("oa_contact_no");
            }
            String[] master1 = Optional.ofNullable(task.getMaster()).orElse(new String[0]);
            String[] spdb_master = Optional.ofNullable(task.getSpdb_master()).orElse(new String[0]);
            String[] developer = Optional.ofNullable(task.getDeveloper()).orElse(new String[0]);
            String[] tester = Optional.ofNullable(task.getTester()).orElse(new String[0]);
            String groupId = task.getGroup();
            String plan_fire_time = task.getPlan_fire_time();
            List emailList = new ArrayList();
            for (String s : master1) {
                ids.add(s);
            }
            for (String s : spdb_master) {
                ids.add(s);
            }
            if (developer != null) {
                for (String s : developer) {
                    ids.add(s);
                }
            }
            if (tester != null) {
                for (String s : tester) {
                    ids.add(s);
                }
            }
            Map param = new HashMap();
            if (!CommonUtils.isNullOrEmpty(ids)) {
                Set idset = new HashSet(ids);
                List tmp = (List) idset.stream().filter(n -> !StringUtils.isEmpty(n)).collect(Collectors.toList());
                if (CommonUtils.isNullOrEmpty(tmp)) {
                    logger.info("???????????????");
                    return;
                }
                param.put(Dict.IDS, tmp);
            }
            List userInfo = userApi.queryUserList(param);
            ReviewRecordHistory reviewRecordHistory = new ReviewRecordHistory();
            reviewRecordHistory.setTask_id(taskId);
            List<ReviewRecordHistory> reviewRecordHistories = reviewRecordDao.queryReviewRecordHistory(reviewRecordHistory);
            String db_type = reviewRecordHistories.get(0).getDb_type();
            String reviewContent = getReviewRecord(taskId, new HashMap());
            if (CommonUtils.isNullOrEmpty(mapping.get("reviewRecord"))) {
                mapping.put("reviewRecord", reviewContent);
            }
            if (CommonUtils.isNullOrEmpty(userInfo)) {
                logger.info("???????????????????????????");
            }

            userInfo.forEach(n -> emailList.add(((Map) n).get(Dict.EMAIL)));
            if ((CommonUtils.isNotNullOrEmpty(reviewStatus) && reviewStatus.equals(Dict.SECOND_REVIEW))||
                    (CommonUtils.isNotNullOrEmpty(reviewStatus) && reviewStatus.equals("??????"))) {
                for (Map map : jobUser) {
                    emailList.add(map.get(Dict.EMAIL));
                }
            }
            mapping.put(Dict.TASKNAME, reviewRecord.getTaskName());
            mapping.put("href", href + "?groupid=" + groupId);
            mapping.put("rqrmntNum", rqrmntNum);
            mapping.put("system_remould", task.getSystem_remould());
            mapping.put("impl_data", task.getImpl_data());
            mapping.put(Dict.DB_TYPE, db_type);
            mapping.put("applicant", reviewRecord.getApplicantName() + "  " + map1.get(Dict.EMAIL));
            if (CommonUtils.isNotNullOrEmpty(name)) {
                mapping.put("init_auditor", name + "  " + init_auditor_email.get(Dict.EMAIL));
            }
            mapping.put("plan_fire_time", plan_fire_time);
            mailUtil.sendReviewRecordEmail(Constants.EMAIL_REVIEWUPDATE, mapping, new ArrayList(new HashSet(emailList)), db_type);
        } catch (Exception e) {
            logger.error("??????????????????");
//            throw new FdevException(ErrorConstants.SEND_EMAIL_ERROR);
        }
    }

    @Override
    public void deleteByTaskId(String task_id) {
        reviewRecordDao.deleteByTaskId(task_id);
    }

    @Override
    public Map queryReviewBasicMsgById(Map request) {
        String taskId = (String) request.get("taskId");
        FdevTask fdevTask = iFdevTaskDao.queryById(taskId);
        ReviewRecord reviewRecord = reviewRecordDao.queryByTaskId(taskId);
        Map map = new HashMap();
        if (reviewRecord != null && CommonUtils.isNotNullOrEmpty(reviewRecord.getId())) {
            map.put(Dict.TASKNAME, reviewRecord.getTaskName());
            map.put(Dict.GROUP, userApi.getGroupNameById(reviewRecord.getGroup()));
            map.put(Dict.REVIEW_TYPE, reviewRecord.getReviewType());
            map.put("applicant", reviewRecord.getApplicant());
            map.put("applicantName", reviewRecord.getApplicantName());
            map.put("reviewers", reviewRecord.getReviewers());
            map.put("groupId", reviewRecord.getGroup());
            map.put("taskId", reviewRecord.getTaskId());
            map.put("reviewStatus", reviewRecord.getReviewStatus());
            //????????????????????????
            String dbType = reviewRecord.getDbType();
            ReviewRecordHistory reviewRecordHistory = new ReviewRecordHistory();
            reviewRecordHistory.setTask_id(taskId);
            List<ReviewRecordHistory> reviewRecordHistories = reviewRecordDao.queryReviewRecordHistory(reviewRecordHistory);
            if(CommonUtils.isNullOrEmpty(reviewRecord.getDbType())){
                if(!CommonUtils.isNullOrEmpty(reviewRecordHistories)){
                    dbType = reviewRecordHistories.get(reviewRecordHistories.size() - 1).getDb_type();
                }
            }
            String reason = reviewRecord.getReason();
            if(CommonUtils.isNullOrEmpty(reason)){
                if(!CommonUtils.isNullOrEmpty(reviewRecordHistories) && reviewRecordHistories.size() >= 2){
                    reason = reviewRecordHistories.get(1).getDoc();
                    if("?????????".equals(reviewRecordHistories.get(1).getReview_status()) && "?????????".equals(reviewRecordHistories.get(2).getReview_status())){
                        reason = reviewRecordHistories.get(2).getDoc();
                    }
                }
            }
            map.put("reason",CommonUtils.isNullOrEmpty(reason)?"":reason);
            map.put("dbType",CommonUtils.isNullOrEmpty(dbType)?"":dbType);
            map.put("plan_fire_time",CommonUtils.isNullOrEmpty(fdevTask.getPlan_fire_time())?"":fdevTask.getPlan_fire_time());
            map.put("docInfo",new ArrayList<>());
            if(!CommonUtils.isNullOrEmpty(fdevTask.getNewDoc())){
                List<Map<String, String>> doc = fdevTask.getNewDoc().stream().filter(p -> "?????????-?????????????????????".equals((String) p.get(Dict.TYPE))).collect(Collectors.toList());
                map.put("docInfo",doc);
            }
            //???????????????
            map.put("stock",CommonUtils.isNullOrEmpty(fdevTask.getMaster())?"0":"1");
        }
        return map;
    }

    private List<String> getDbaEmail() {
        List dba = new ArrayList();
        try {
            Map param = new HashMap();
            List roleId = new ArrayList();
            List roles = userApi.queryRole();
            for (Object role : roles) {
                Map tmp = (Map) role;
                if ("DBA?????????".equals(tmp.get(Dict.NAME)))
                    roleId.add(tmp.get(Dict.ID));
            }
            if (roleId.isEmpty()) {
                return new ArrayList();
            }
            param.put(Dict.ROLE_ID, roleId);
            dba = (List) userApi.queryUserAll(param);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List emailList = new ArrayList();
        if (CommonUtils.isNullOrEmpty(dba)) {
            logger.info("??????DBA?????????");
            return dba;
        }
        dba.forEach(n -> emailList.add(((Map) n).get(Dict.EMAIL)));
        return emailList;
    }

    @Override
    public void deleteByTask_Id(String task_id) {
        reviewRecordDao.deleteByTask_Id(task_id);
    }

    @Override
    public ReviewRecord queryTaskReviewByTaskId(String task_id) {

        return reviewRecordDao.queryTaskReviewByTaskId(task_id);
    }

    @Override
    public void addReviewIdea(Map request) throws Exception {
        //??????????????????
        String review_status = (String) request.get(Dict.REVIEW_STATUS);
        String task_id = (String) request.get(Dict.TASK_ID);
        FdevTask task = new FdevTask();
        task.setId(task_id);
        task = iFdevTaskService.queryTaskById(task_id);
        Map opno = (Map) request.get("opno");
        ReviewRecordHistory reviewRecordHistory = new ReviewRecordHistory();
        reviewRecordHistory.setTask_id(task_id);
        List<ReviewRecordHistory> list = reviewRecordDao.queryReviewRecordHistory(reviewRecordHistory);
        reviewRecordHistory.setOpno(opno);
        reviewRecordHistory.setDoc((String) request.get(Dict.DOC));
        reviewRecordHistory.setTask_name(list.get(0).getTask_name());
        reviewRecordHistory.setReview_status(review_status);
        reviewRecordHistory.setDb_type(list.get(0).getDb_type());
        reviewRecordHistory.setReview_type(list.get(0).getReview_type());
        reviewRecordHistory.setGroup(list.get(0).getGroup());
        reviewRecordDao.saveReviewRecord(reviewRecordHistory);
        String db_type = list.get(0).getDb_type();
        String implement_unit_no = task.getFdev_implement_unit_no();
        String rqrmntNum = "";
        if(!CommonUtils.isNullOrEmpty(implement_unit_no)){
            Map rqrmnt_info = demandService.queryByFdevNo(task.getRqrmnt_no(),implement_unit_no);
            Map rqrmnt = (Map) rqrmnt_info.get(Dict.DEMAND_BASEINFO);
            rqrmntNum = (String) rqrmnt.get("oa_contact_no");
        }
        //?????????  ???????????????????????????????????????
        ReviewRecord reviewRecord = reviewRecordDao.queryByTaskId(task_id);
        List reviewers = reviewRecord.getReviewers();
        Map init_auditor1 = new HashMap();
        if (reviewers != null && reviewers.size() >= 1) {
            init_auditor1 = CommonUtils.obj2Map(reviewers.get(0));
        }
        String cid = (String) init_auditor1.get("cid");
        String name = (String) init_auditor1.get(Dict.NAME);
        Map init_auditor_user = new HashMap();
        Map init_auditor_email = new HashMap();
        if (CommonUtils.isNotNullOrEmpty(cid)) {
            init_auditor_user.put(Dict.ID, cid);
            init_auditor_email = iUserApi.queryUser(init_auditor_user);
        }
        Set<String> user_ids = new HashSet<>();
        user_ids.add(reviewRecord.getApplicant());
        String applicant = reviewRecord.getApplicant();
        Map applicantId = new HashMap();
        applicantId.put(Dict.ID, applicant);
        Map map2 = iUserApi.queryUser(applicantId);
        List<Map> jobUser = iUserApi.getJobUser();
        ArrayList<String> list1 = new ArrayList<>();
        ArrayList<String> emailList = new ArrayList<>();
        for (Map map : jobUser) {
            emailList.add((String) map.get(Dict.EMAIL));
        }
        for (ReviewRecordHistory recordHistory : list) {
            Map init_auditor = recordHistory.getInit_auditor();
            if (init_auditor != null) {
                user_ids.add((String) init_auditor.get(Dict.USER_ID));
            }
            if ("????????????".equals(recordHistory.getReview_status()) || "??????".equals(recordHistory.getReview_status())) {
                Map auditor = recordHistory.getAuditor();
                if (auditor != null) {
                    user_ids.add((String) auditor.get(Dict.ID));
                }
            }
        }
        Map users = new HashMap();
        List<String> userIds = new ArrayList<>();
        for (String user_id : user_ids) {
            if (CommonUtils.isNotNullOrEmpty(user_id)) {
                userIds.add(user_id);
            }
        }
        users.put(Dict.IDS, userIds);
        List<Map> userList = iUserApi.queryUserList(users);
        for (Map map : userList) {
            list1.add((String) map.get(Dict.USER_NAME_EN));
            emailList.add((String) map.get(Dict.EMAIL));
        }
        String content = "??????????????????????????????";
        iNotifyApi.sendUserNotify(content, list1, "0", href + "?groupid=" + task.getGroup(),"???????????????");
        Map mapping = new HashMap();
        String reviewContent = getReviewRecord(task_id, new HashMap());
        mapping.put("reviewRecord", reviewContent);
        mapping.put("system_remould", task.getSystem_remould());
        mapping.put("impl_data",task.getImpl_data());
        mapping.put(Dict.TASKNAME, reviewRecord.getTaskName());
        mapping.put("href", href + "?groupid=" + task.getGroup());
        mapping.put("rqrmntNum", rqrmntNum);
        mapping.put(Dict.DB_TYPE, db_type);
        mapping.put("reviewStatus", review_status);
        mapping.put("applicant", reviewRecord.getApplicantName() + "  " + map2.get(Dict.EMAIL));
        if (CommonUtils.isNotNullOrEmpty(name)) {
            mapping.put("init_auditor", name + "  " + init_auditor_email.get(Dict.EMAIL));
        }
        mapping.put("plan_fire_time", task.getPlan_fire_time());
        mapping.put("opinion", request.get(Dict.DOC));
        try {
            mailUtil.sendReviewRecordEmail(Constants.EMAIL_REVIEWUPDATE, mapping, new ArrayList(new HashSet(emailList)), db_type);
        } catch (Exception e) {
            logger.error("??????????????????");
//            throw new FdevException(ErrorConstants.SEND_EMAIL_ERROR);
        }
    }

    private String getReviewRecord(String taskId, Map map) {
        ReviewRecordHistory reviewRecordHistory = new ReviewRecordHistory();
        reviewRecordHistory.setTask_id(taskId);
        List<ReviewRecordHistory> list = reviewRecordDao.queryReviewRecordHistory(reviewRecordHistory);
        String reviewContent = "";
        for (ReviewRecordHistory recordHistory : list) {
            String review_status = recordHistory.getReview_status();
            Map auditor = recordHistory.getAuditor();
            Map initiator = recordHistory.getInitiator();
            Map opno = recordHistory.getOpno();
            if (null != opno) {
                reviewContent += "????????????:" + recordHistory.getReview_time() + "   ????????????:" + review_status
                        + "   ?????????:" + opno.get("opno_name_cn") + "\n" + "                " + "????????????:" + recordHistory.getDoc() + "\n" + "                ";
            }
            if (null != auditor) {
                reviewContent += "????????????:" + recordHistory.getReview_time() + "   ????????????:" + review_status
                        + "   ?????????:" + auditor.get("auditor_name_cn") + "\n" + "                " + "????????????:" + recordHistory.getDoc() + "\n" + "                ";

            }
            if (null != initiator) {
                reviewContent += "????????????:" + recordHistory.getReview_time() + "   ????????????:" + review_status
                        + "   ?????????:" + initiator.get("initiator_name_cn") + "\n" + "                " + "????????????:" + recordHistory.getDoc() + "\n" + "                ";
            }
        }
        if (CommonUtils.isNotNullOrEmpty((String) map.get(Dict.DOC))) {
            String s = "????????????:" + map.get("reviewTime") + "   ????????????:" + map.get("reviewStatus")
                    + "   ?????????:" + map.get(Dict.USER_NAME_CN) + "\n" + "                " + "????????????:" + map.get(Dict.DOC) + "\n" + "                ";
            reviewContent = s + reviewContent;
        }
        return reviewContent;
    }

    @Override
    public void addNoCodeReview(Map params) throws Exception{
        Map<String,String> fileMap = (Map<String,String>) params.remove("docInfo");
        if(!CommonUtils.isNullOrEmpty(fileMap)){
            String fileType =  fileMap.get("name").substring(fileMap.get("name").lastIndexOf(".") + 1);
            if (!"zip".equals(fileType)) {
                throw new FdevException(ErrorConstants.FILE_FMT_ERROR, new String[]{"????????????????????????zip??????"});
            }
        }
        FdevTask task;
        ReviewRecordHistory reviewRecordHistory = new ReviewRecordHistory();
        String oldStatus,newStatus;
        //0 - ??????  1 - ???????????????????????????
        if("0".equals(params.get("type"))){
            //????????????????????????????????????
            try {
                logger.info("??????????????????{}",params);
                task = createTask(params);
                logger.info("???????????????{}",params);
                reviewRecordHistory = createReviewRecord(params,task);
            } catch (Exception e) {
                throw new FdevException(ErrorConstants.TASK_ERROR,new String[]{"???????????????????????????"});
            }
            oldStatus = Dict.INIT;
            newStatus = Dict.FIRST_REVIEW;
        }else {
            //???????????????????????????
            task = iFdevTaskDao.queryById((String) params.get("task_id"));
            task.setGroup((String) params.get(Dict.GROUP));
            task.setPlan_fire_time((String) params.get(Dict.PLAN_FIRE_TIME));
            ReviewRecord reviewRecord = reviewRecordDao.queryByTaskId(task.getId());
            //??????ReviewRecord ?????????ReviewRecordHistory
            reviewRecordHistory = updateReviewRecord(params, task);
            oldStatus = reviewRecord.getReviewStatus();
            newStatus = reviewRecordHistory.getReview_status();

        }
        //??????????????????????????????
        if(!CommonUtils.isNullOrEmpty(fileMap)){
            String name = fileMap.get("name");
            MultipartFile file = base64ToFile((String) fileMap.get("content"));
            String path = iDocService.uploadNoCodeDbReview(name,file, task);
            List<Map<String, String>> newDoc = new ArrayList<>();
            Map<String, String> doc = new HashMap<String, String>() {{
                put(Dict.PATH,path);
                put(Dict.NAME,name);
                put(Dict.TYPE,"?????????-?????????????????????");
            }};
            newDoc.add(doc);
            task.setNewDoc(newDoc);
        }
        //????????????
        iFdevTaskDao.update(task);
        //?????????????????????
        Map mail = new HashMap<>();
        String applicant = (String)params.get("applicantName");
        Map applicantMap = userApi.queryUser(new HashMap(){{
            put(Dict.ID,applicant);
        }});
        mail.put("reviewStatus", oldStatus + "--->" + newStatus);
        mail.put("applicant", applicantMap.get(Dict.USER_NAME_CN) + "  " + applicantMap.get(Dict.EMAIL));
        mail.put("plan_fire_time", task.getPlan_fire_time());
        sendNoCodeEmail(mail, reviewRecordHistory,task,(String)params.get("applicantName"),(String)params.get("reviewer"));
        logger.info("??????????????????");
        if(Dict.FIRST_REVIEW.equals(newStatus)){
            String content = "??????????????????????????????";
            ArrayList<String> list = new ArrayList<>();
            list.add((String) reviewRecordHistory.getInit_auditor().get(Dict.USER_NAME_EN));
            iNotifyApi.sendUserNotify(content, list, "0", href + "?groupid=" + (String) params.get(Dict.GROUP),"???????????????");
        }
    }

    private void sendNoCodeEmail(Map mapping, ReviewRecordHistory reviewRecordHistory,FdevTask task,String applicant, String reviewer){
        try {
            List<String> emailList = new ArrayList<String>();
            Map applicantMap = userApi.queryUser(new HashMap(){{
                put(Dict.ID,applicant);
            }});
            Map reviewMap = userApi.queryUser(new HashMap(){{
                put(Dict.ID,reviewer);
            }});
            List<Map> jobUser = iUserApi.getJobUser();
            emailList.add((String) applicantMap.get(Dict.EMAIL));
            if(Dict.FIRST_REVIEW.equals(reviewRecordHistory.getReview_status())){
                emailList.add((String) reviewMap.get(Dict.EMAIL));
            }else {
                jobUser.forEach(p -> {
                    emailList.add((String) p.get(Dict.EMAIL));
                });
            }
            mapping.put(Dict.TASKNAME, reviewRecordHistory.getTask_name());
            mapping.put("href", href + "?groupid=" + task.getGroup());
            mapping.put("rqrmntNum", "");
            mapping.put(Dict.DB_TYPE, reviewRecordHistory.getDb_type());
            mapping.put("system_remould", task.getSystem_remould());
            mapping.put("impl_data", task.getImpl_data());
            String reviewContent = getReviewRecord(task.getId(), new HashMap());
            mapping.put("reviewRecord", reviewContent);
            mailUtil.sendReviewRecordEmail(Constants.EMAIL_REVIEWUPDATE, mapping, new ArrayList(new HashSet(emailList)), reviewRecordHistory.getDb_type());
        } catch (Exception e) {
            logger.error("??????????????????");
        }
    }

    /**
     * ???????????????????????????????????????
     * @param params
     * @return
     * @throws Exception
     */
    private FdevTask createTask(Map params) throws Exception{
        FdevTask fdevTask = new FdevTask();
        fdevTask.setName((String) params.get(Dict.TASKNAME));
        fdevTask.setGroup((String) params.get(Dict.GROUP));
        fdevTask.setPlan_fire_time((String) params.get(Dict.PLAN_FIRE_TIME));
        fdevTask.setStage(Dict.TASK_STAGE_DISCARD);
        fdevTask.setSystem_remould("???");
        fdevTask.setImpl_data("???");
        TaskReview taskReview = new TaskReview();
        TaskReviewChild taskReviewChild = new TaskReviewChild();
        taskReviewChild.setAudit(false);
        taskReviewChild.setName("???");
        taskReview.setData_base_alter(new TaskReviewChild[]{taskReviewChild});
        fdevTask.setReview(taskReview);
        ObjectId objectId = new ObjectId();
        fdevTask.set_id(objectId);
        fdevTask.setId(objectId.toString());
        return iFdevTaskDao.save(fdevTask);
    }

    /**
     * ????????????????????????ReviewRecord?????????????????????ReviewRecordHistory???
     * @param params
     * @param task
     * @return
     */
    private ReviewRecordHistory createReviewRecord(Map params,FdevTask task) throws Exception{
        TaskReview review = task.getReview();
        TaskReviewChild[] sqls = review.getData_base_alter();
        ReviewRecordHistory reviewHistory = new ReviewRecordHistory();
        for (TaskReviewChild sql : sqls) {
            //??????ReviewRecord
            String id = sql.getId();
            ObjectId oid = new ObjectId(id);
            ReviewRecord reviewRecord = new ReviewRecord();
            reviewRecord.set_id(oid);
            reviewRecord.setId(sql.getId());
            reviewRecord.setTaskId(task.getId());
            reviewRecord.setTaskName(task.getName());
            reviewRecord.setDbType((String) params.get("dbType"));
            reviewRecord.setStock("0");
            String group = task.getGroup();
            reviewRecord.setGroup(group);
            //?????????
            Map groupInfo = userApi.queryGroup(new HashMap() {{
                put(Dict.ID, group);
            }});
            String groupName = (String) groupInfo.get(Dict.NAME);
            reviewRecord.setGroupName(groupName);
            reviewRecord.setStatus("1");
            reviewRecord.setReviewType("???????????????");
            reviewRecord.setReviewStatus("?????????");
            reviewRecord.setReason((String) params.get("reason"));
            String applicant = (String)params.get("applicantName");
            reviewRecord.setApplicant((String) params.get("applicantName"));
            //?????????
            Map applicantMap = userApi.queryUser(new HashMap(){{
              put(Dict.ID,applicant);
            }});
            reviewRecord.setApplicantName((String) applicantMap.get(Dict.USER_NAME_CN));
            String reviewer = (String)params.get("reviewer");
            //?????????
            Map reviewMap = userApi.queryUser(new HashMap(){{
                put(Dict.ID,reviewer);
            }});
            ReviewUser reviewUser = new ReviewUser();
            reviewUser.setcid(reviewer);
            reviewUser.setName((String) reviewMap.get(Dict.USER_NAME_CN));
            reviewRecord.setReviewers(new ArrayList<ReviewUser>(){{
                add(reviewUser);
            }});
            reviewRecord.setCreateDate(CommonUtils.dateFormat(new Date(), CommonUtils.DATE_TIME_PATTERN));
            reviewRecordDao.saveNoCodeReview(reviewRecord);

            //??????ReviewRecordHistory
            ReviewRecordHistory reviewRecordHistory = new ReviewRecordHistory();
            reviewRecordHistory.setTask_id(task.getId());
            reviewRecordHistory.setTask_name(task.getName());
            reviewRecordHistory.setGroup((String) groupInfo.get(Dict.NAME));
            reviewRecordHistory.setDoc((String) params.get("reason"));
            reviewRecordHistory.setInitiator(new HashMap(){{
                put(Dict.ID,applicant);
                put("initiator_name_cn",(String) applicantMap.get(Dict.USER_NAME_CN));
            }});
            reviewRecordHistory.setReview_type("???????????????");
            reviewRecordHistory.setReview_status("?????????");
            reviewRecordHistory.setDb_type((String) params.get("dbType"));
            reviewRecordHistory.setInit_auditor(new HashMap(){{
                put("user_id",reviewer);
                put("user_name_cn",reviewMap.get(Dict.USER_NAME_CN));
                put("user_name_en",reviewMap.get(Dict.USER_NAME_EN));
            }});
            reviewHistory =  reviewRecordDao.saveReviewRecord(reviewRecordHistory);
        }
        return reviewHistory;
    }

    private ReviewRecordHistory updateReviewRecord(Map params,FdevTask task) throws Exception{
        ReviewRecord reviewRecord = reviewRecordDao.queryByTaskId(task.getId());
        String reviewer = (String)params.get("reviewer");
        //?????????
        Map reviewMap = userApi.queryUser(new HashMap(){{
            put(Dict.ID,reviewer);
        }});
        ReviewUser reviewUser = new ReviewUser();
        reviewUser.setcid(reviewer);
        reviewUser.setName((String) reviewMap.get(Dict.USER_NAME_CN));
        //????????????????????????????????????
        List<ReviewUser> reviewers = reviewRecord.getReviewers();
        boolean isExist = false;
        int index = 0;
        if(!CommonUtils.isNullOrEmpty(reviewers)){
            for (int i = 0; i < reviewers.size(); i++) {
                if(reviewer.equals(reviewers.get(i).getcid())){
                    isExist = true;
                    index = i;
                    break;
                }
            }
        }
        if(!isExist){
            //????????????????????????
            reviewers.add(reviewUser);
        }else {
            //??????????????????????????????
            reviewers.remove(index);
            reviewers.add(reviewUser);
        }
        reviewRecord.setReviewers(reviewers);
        switch (reviewRecord.getReviewStatus()){
            case "??????":
            case "????????????":reviewRecord.setReviewStatus("?????????");break;
            case "????????????":reviewRecord.setReviewStatus("?????????");break;
            default:throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{reviewRecord.getReviewStatus() + ":??????????????????????????????????????????"});
        }
        ReviewRecord update = reviewRecordDao.update(reviewRecord);
        //??????ReviewRecordHistory
        ReviewRecordHistory reviewRecordHistory = new ReviewRecordHistory();
        reviewRecordHistory.setTask_id(task.getId());
        reviewRecordHistory.setTask_name(task.getName());
        String group = task.getGroup();
        //?????????
        Map groupInfo = userApi.queryGroup(new HashMap() {{
            put(Dict.ID, group);
        }});
        reviewRecordHistory.setGroup((String) groupInfo.get(Dict.NAME));
        reviewRecordHistory.setDoc((String) params.get("reason"));
        String applicant = (String)params.get("applicantName");
        //?????????
        Map applicantMap = userApi.queryUser(new HashMap(){{
            put(Dict.ID,applicant);
        }});
        reviewRecordHistory.setInitiator(new HashMap(){{
            put(Dict.ID,applicant);
            put("initiator_name_cn",(String) applicantMap.get(Dict.USER_NAME_CN));
        }});
        reviewRecordHistory.setReview_type("???????????????");
        reviewRecordHistory.setReview_status(update.getReviewStatus());
        reviewRecordHistory.setDb_type((String) params.get("dbType"));
        reviewRecordHistory.setInit_auditor(new HashMap(){{
            put("user_id",reviewer);
            put("user_name_cn",reviewMap.get(Dict.USER_NAME_CN));
            put("user_name_en",reviewMap.get(Dict.USER_NAME_EN));
        }});
        return reviewRecordDao.saveReviewRecord(reviewRecordHistory);
    }

    private MultipartFile base64ToFile(String base64) throws Exception{
        ByteArrayInputStream byteArrayInputStream = null;
        MultipartFile file = null ;
        try {
            String[] baseStr = base64.split(",");
            BASE64Decoder base64Decoder = new BASE64Decoder();
            byte[] b = new byte[0];
            b = base64Decoder.decodeBuffer(baseStr[1]);
            for (int i = 0; i <b.length ; i++) {
                if(b[i] < 0){
                    b[i] += 256;
                }
            }
            byteArrayInputStream = new ByteArrayInputStream(b);
            file = new MockMultipartFile(ContentType.APPLICATION_OCTET_STREAM.toString(),byteArrayInputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null != byteArrayInputStream){
                byteArrayInputStream.close();
            }
        }
        return file;
    }
}
