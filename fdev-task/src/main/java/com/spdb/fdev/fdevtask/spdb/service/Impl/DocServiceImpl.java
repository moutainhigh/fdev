package com.spdb.fdev.fdevtask.spdb.service.Impl;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.client.result.UpdateResult;
import com.spdb.fdev.common.User;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.common.util.UserVerifyUtil;
import com.spdb.fdev.fdevtask.base.dict.Constants;
import com.spdb.fdev.fdevtask.base.dict.Dict;
import com.spdb.fdev.fdevtask.base.dict.ErrorConstants;
import com.spdb.fdev.fdevtask.base.unit.BatchUpdateUnit;
import com.spdb.fdev.fdevtask.base.utils.BatchUpdateOptions;
import com.spdb.fdev.fdevtask.base.utils.CommonUtils;
import com.spdb.fdev.fdevtask.base.utils.WordUtil;
import com.spdb.fdev.fdevtask.spdb.dao.IFdevTaskDao;
import com.spdb.fdev.fdevtask.spdb.entity.DesignDoc;
import com.spdb.fdev.fdevtask.spdb.entity.FdevTask;
import com.spdb.fdev.fdevtask.spdb.service.*;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RefreshScope
public class DocServiceImpl implements IDocService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${fileUrl}")
    private String fileUrl;

    @Autowired
    private IFdevTaskService fdevTaskService;

    @Autowired
    private IUserApi userApi;

    @Autowired
    private IFdevTaskDao fdevTaskDao;

    @Autowired
    private RequirementApi requirementApi;

    @Autowired
    private IAppApi appApi;

    @Autowired
    private ITestApi testApi;

    @Autowired
    private UserVerifyUtil userVerifyUtil;

    @Autowired
    private FreeMarkerConfigurer freeMarkerConfigurer;

    @Resource
    private FdocmanageService fdocmanageService;

    @Value("${minio.docResource}")
    private String docResource;
    @Resource
    private BatchUpdateUnit batchUpdateUnit;
    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private DemandService demandService;

    @Override
    public String uploadNoCodeDbReview(String name,MultipartFile file,FdevTask fdevTask) throws Exception{
        User user = userVerifyUtil.getSessionUser(((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest()
                .getHeader(com.spdb.fdev.common.dict.Dict.TOKEN_AUTHORIZATION_KEY));
        if (null == user) {
            throw new FdevException(ErrorConstants.USR_AUTH_FAIL, new String[]{"????????????????????????"});
        }
        Map rGroup = (Map) userApi.queryGroup(new HashMap() {{
            put(Dict.ID, fdevTask.getGroup());
        }});
        String groupName = (String) rGroup.get(Dict.NAME);
        String taskName = CommonUtils.urlEncode(fdevTask.getName());
        if (StringUtils.isNotBlank(taskName) && taskName.length() > 10) {
            taskName = taskName.substring(0, 10);
        }
        String pathMinio = docResource + "/" + groupName + "/" + taskName.replace("/","") + "-" + fdevTask.getId() + "/?????????-?????????????????????" + "/" + name.replace("/","");
        List<Map<String, String>> newDoc = fdevTask.getNewDoc();
        if(!CommonUtils.isNullOrEmpty(newDoc)){
            //?????????????????????????????????????????????
            newDoc.forEach(p ->{
                if(!CommonUtils.isNullOrEmpty(p) || "?????????-?????????????????????".equals(p.get(Dict.TYPE))){
                    //????????????
                    try {
                        deleteTaskDoc(fdevTask.getId(),p.get(Dict.PATH));
                    } catch (Exception e) {
                        logger.info("??????????????????");
                    }
                }
            });
        }
        //??????
        fdocmanageService.uploadFiletoMinio("fdev-task", pathMinio, file, user);
        return pathMinio;
    }

    @Override
    public void testReportCreate(Map request) throws Exception {
        User user = userVerifyUtil.getSessionUser(((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest()
                .getHeader(com.spdb.fdev.common.dict.Dict.TOKEN_AUTHORIZATION_KEY));
        if (null == user) {
            throw new FdevException(ErrorConstants.USR_AUTH_FAIL, new String[]{"????????????????????????"});
        }
        String taskId = (String) request.get(Dict.ID);
        List<FdevTask> tasks = fdevTaskService.query(new FdevTask.FdevTaskBuilder().id(taskId).init());
        if (CommonUtils.isNullOrEmpty(tasks)) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????ID???????????????????????????"});
        }
        FdevTask task = tasks.get(0);
        Map user1 = new HashMap();
        Map dev = new HashMap();
        if (task.getDeveloper().length >= 1) {
            user1.put(Dict.ID, task.getDeveloper()[0]);
            dev = userApi.queryUser(user1);
        }
        String simpleFlag = (String) request.get("simpleDemand"); // ??????????????????
        simpleFlag = CommonUtils.isNullOrEmpty(simpleFlag)? "1": task.getSimpleDemand();
        String testMsg = "??????????????????";
        if("1".equals(simpleFlag)){ // ?????????????????????
            //????????????????????????
            testMsg = testApi.queryFdevSitMsg(taskId);
        }
        //????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        Map demandInfo = demandService.queryDemandInfoDetail(task.getRqrmnt_no());
        String dept = "???????????????";
        if(!CommonUtils.isNullOrEmpty(demandInfo) && Constants.DEMANDTYPE_BUSINESS.equals(demandInfo.get(Dict.DEMAND_TYPE))
                && !CommonUtils.isNullOrEmpty(demandInfo.get("propose_demand_dept"))) {
            dept = (String) demandInfo.get("propose_demand_dept");
        }
        Map<String, Object> params = new HashMap<>();
        params.put(Dict.TASKNAME, task.getName());
        params.put(Dict.DEVELOPER, dev.get(Dict.USER_NAME_CN));
        params.put("sitDate", CommonUtils.dateFormat(new Date(), CommonUtils.DATE_PATTERN));
        params.put("tel", dev.get("telephone"));
        params.put("testContent", testMsg);
        params.put("dept", dept);
        String testenv = "sit";
        if("1".equals(simpleFlag)){
            testenv = (String) request.get("testEnv");
        }
        params.put("testEnv", testenv);
        params.put(Dict.PRO_WANT_WINDOW, task.getProWantWindow());
        String fileName = "??????????????????_" + (task.getFdev_implement_unit_no()!=null?task.getFdev_implement_unit_no():"") + "_" + task.getName() + ".doc";
        String path = fileUrl + fileName.replace("/","");
        WordUtil.createWord(params, "testReportTemplate.ftl", path, freeMarkerConfigurer);
        //??????????????????
        Map rGroup = (Map) userApi.queryGroup(new HashMap() {{
            put(Dict.ID, task.getGroup());
        }});
        String groupName = (String) rGroup.get(Dict.NAME);
        //??????
        MultipartFile multipartFile = new MockMultipartFile(fileName, fileName, ContentType.APPLICATION_OCTET_STREAM.toString(), new FileInputStream(new File(path)));
        // ?????????????????? ?????????????????????doc??????
        String taskName = CommonUtils.urlEncode(task.getName());
        if (StringUtils.isNotBlank(taskName) && taskName.length() > 10) {
            taskName = taskName.substring(0, 10);
        }
        String pathMinio = docResource + "/" + groupName + "/" + taskName.replace("/","") + "-" + task.getId() + "/?????????-????????????" + "/" + fileName.replace("/","");
        //??????????????????
        if (!isExistFile(task, pathMinio)) {
            //????????????
            fdocmanageService.uploadFiletoMinio("fdev-task", pathMinio, multipartFile, user);
            //????????????????????????????????????
            List<Map<String, String>> doc = new ArrayList<>();
            HashMap<String, String> map = new HashMap<>();
            map.put("path", pathMinio);
            map.put("name", fileName);
            map.put("type", "?????????-????????????");
            doc.add(map);
            Long aLong = updateTaskDoc(task.getId(), doc);
            logger.info("?????????????????????????????????update={}", aLong);
        }
    }

    /**
     * ??????????????????doc
     *
     * @param moduleName
     * @param path
     * @return
     */
    @Override
    public String batchUpdateTaskDoc(String moduleName, String path) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        //?????????????????????
        List<String> filesPath = fdocmanageService.getFilesPath(moduleName, path);
        //??????????????????????????????????????????
        List<FdevTask> query = fdevTaskDao.query(new FdevTask());
        logger.info("???????????????:{},????????????{}", filesPath.size(), query.size());
        if (filesPath.size() <= 1 || query.size() <= 1) {
            logger.info("??????????????????????????????");
            return "false";
        }
        //????????????
        List<List<FdevTask>> partition = Lists.partition(query, query.size() / 6);
        partition.forEach(list -> {
            Future<List<BatchUpdateOptions>> newDoc1 = executorService.submit(() -> {
                logger.info("????????????????????????{}", list.size());
                return list.stream().parallel().map(task -> {
                    List<Map<String, String>> newDoc = getPath(task, filesPath);
                    //??????options??????
                    FdevTask fdevTask = new FdevTask();
                    fdevTask.setId(task.getId());
                    fdevTask.setNewDoc(newDoc);
                    return batchUpdateUnit.buildBatchUpdateOption(fdevTask);
                }).filter(newTask -> CommonUtils.isNullOrEmpty(newTask.getUpdate().getUpdateObject().get("newDoc")))
                        .collect(Collectors.toList());
            });
            List<BatchUpdateOptions> batchUpdateOptions = null;
            try {
                batchUpdateOptions = newDoc1.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            //????????????
            int i = batchUpdateUnit.executeBatchUpdate(mongoTemplate, "task", batchUpdateOptions, true);
            logger.info("????????????{}??????????????????{}", Thread.currentThread().getName(), i);
        });
        executorService.shutdown();
        return "success";
    }

    @Override
    public FdevTask uploadDesignFile(MultipartFile file, String fileName, String taskId, String fileType, String uploadStage, String remark) throws Exception {
        //?????????minio
        User user = userVerifyUtil.getSessionUser(((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest()
                .getHeader(com.spdb.fdev.common.dict.Dict.TOKEN_AUTHORIZATION_KEY));
        //????????????
        String pathMinio  = taskId + "/"+ fileType+ "/"+ uploadStage + "/" + fileName;
        boolean result = fdocmanageService.uploadFiletoMinio("fdev-design", pathMinio, file, user);
        //????????????????????????????????????
        if (result) {
            List<FdevTask> fdevTasks = fdevTaskDao.queryAll(new FdevTask.FdevTaskBuilder().id(taskId).init());
            FdevTask fdevTask = fdevTasks.get(0);
            DesignDoc designDoc = new DesignDoc();
            designDoc.setDocType(fileType);
            designDoc.setFileName(fileName);
            designDoc.setUploadStage(uploadStage);
            designDoc.setMinioPath(pathMinio);
            List<DesignDoc> designDocs = Optional.ofNullable(fdevTask.getDesignDoc()).orElse(new ArrayList<>());
            //??????????????????????????????????????????
            designDocs = designDocs.stream().filter((designDoc1)-> !(uploadStage.equals(designDoc1.getUploadStage()) && fileType.equals(designDoc1.getDocType()))).collect(Collectors.toList());
            designDocs.add(designDoc);
            if("0".equals(uploadStage)){
                //???????????????desinMap
                FdevTask updateTask = new FdevTask();
                updateTask.setId(taskId);
                Map<String, List<Map<String, String>>> designMap = Optional
                        .ofNullable(fdevTask.getDesignMap())
                        .orElse(new HashMap<>());
                //??????upload????????????
                Map<String, String> dataMap = new HashMap<>();
                dataMap.put("name",user.getUser_name_cn());
                dataMap.put("time",CommonUtils.dateFormat(new Date(), Dict.FORMATE_DATE));
                dataMap.put("remark",remark);
                designMap.put(Dict.UPLOADED,Arrays.asList(dataMap));
                updateTask.setDesignMap(designMap);
                if(CommonUtils.isNullOrEmpty(fdevTask.getReview_status()) || Dict.WAIT_UPLOAD.equals(fdevTask.getReview_status())){
                    //?????????uploaded???????????????????????????
                    updateTask.setReview_status("uploaded");
                }
                updateTask.setDesignDoc(designDocs);
                FdevTask update = fdevTaskDao.update(updateTask);
                logger.info("upload????????????=>update={}", JSON.toJSONString(updateTask,true));
                return update;
            }
            FdevTask updateTask = new FdevTask();
            updateTask.setId(taskId);
            updateTask.setDesignDoc(designDocs);
            FdevTask update = fdevTaskDao.update(updateTask);
            logger.info("????????????????????????????????????=>update={}{}",JSON.toJSONString(designDocs,true));
            return update;
        }
        throw new FdevException(ErrorConstants.TASK_ERROR, new String[]{"??????????????????,?????????!"});
    }

    private Long updateDesignDoc(String taskId,  List<DesignDoc> designDocs) {
        Query query = Query.query(Criteria.where(Dict.ID).is(taskId));
        Update update = new Update()
                .set("designDoc", designDocs);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, FdevTask.class);
        return updateResult.getMatchedCount();
    }
    private Long updateDesignDoc(FdevTask fdevTask) {
        Query query = Query.query(Criteria.where(Dict.ID).is(fdevTask.getId()));
        Update update = new Update()
                .set("designDoc", fdevTask.getDesignDoc())
                .set("designMap", fdevTask.getDesignMap())
                .set("review_status", fdevTask.getReview_status());
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, FdevTask.class);
        return updateResult.getMatchedCount();
    }

    @Override
    public Long deleteDesignDoc(String taskId, String path) throws Exception {
        User user = userVerifyUtil.getSessionUser(((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest()
                .getHeader(com.spdb.fdev.common.dict.Dict.TOKEN_AUTHORIZATION_KEY));
        boolean result = fdocmanageService.deleteFiletoMinio("fdev-task", path, user);
        if(!result){
            throw new FdevException(ErrorConstants.TASK_ERROR, new String[]{"????????????,?????????!"});
        }
        FdevTask fdevTask = new FdevTask();
        fdevTask.setId(taskId);
        List<FdevTask> fdevTasks = fdevTaskDao.queryAll(fdevTask);
        List<DesignDoc> designDoc = fdevTasks.get(0).getDesignDoc();
        for (int i = 0; i < designDoc.size(); i++) {
            String path1 = designDoc.get(i).getMinioPath();
            if (path.equals(path1)) {
                designDoc.remove(i);
                break;
            }
        }
        Query query = Query.query(Criteria.where(Dict.ID).is(taskId));
        Update update = new Update()
                .set("designDoc", designDoc);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, FdevTask.class);
        return updateResult.getMatchedCount();
    }

    @Override
    public String batchUpdateTaskDoc(String moduleName) throws Exception {
        //??????????????????
        List<FdevTask> fdevTasks = fdevTaskDao.queryTaskForDesign();
        logger.info("???????????????????????????:{}",fdevTasks.size());
        //????????????
        AtomicInteger atomicInteger = new AtomicInteger(0);
        for (FdevTask task : fdevTasks) {
            dealWpsFile(task);
            logger.info("???{}????????????",atomicInteger.addAndGet(1));
        }
        return null;
    }


    public void dealWpsFile(FdevTask fdevTask) throws Exception{
        //        createFolder
        Map folder = fdocmanageService.createFolder(fdevTask.getId());
        if(CommonUtils.isNullOrEmpty(folder)){
            logger.error("?????????????????????id???????????????id={}",fdevTask.getId());
            return;
        }
        String androidId = (String)folder.get("androidUploadId");
        String iosId = (String)folder.get("iosUploadId");
        //        wpsList
        Map<String,List> listFile = fdocmanageService.wpsList(fdevTask.getFolder_id());
        List lists = Optional.ofNullable(listFile.get("value")).orElse(new ArrayList());
        if(lists.size() < 4 ){
            return;
        }
        for (Object list : lists) {
            Map<String,Object> listMap = (Map)list;
            if(androidId.equals(listMap.get("id"))){
                List childFile = (List)listMap.get("childFile");
                //???????????????
                if (!CommonUtils.isNullOrEmpty(listMap.get("childFile"))){
                    //?????????????????????????????????
                    for (Object child : childFile) {
                        Map<String,Object> chilldMap = (Map)child;
                        Map childInfo = fdocmanageService.content((String) chilldMap.get("id"));
                        String url = (String)childInfo.get("url");
                        logger.info("????????????????????????:{}",url);
                        String pathMinio  = fdevTask.getId() + "/" + "android" + "/" + chilldMap.get("uploadStage") + "/"+chilldMap.get("name");
                        //??????
                        boolean result = fdocmanageService.downloadAndUpload(url,pathMinio,(String)chilldMap.get("name"));
                        //??????
                        if(result){
                            DesignDoc designDoc = new DesignDoc();
                            designDoc.setDocType("android");
                            designDoc.setMinioPath(pathMinio);
                            designDoc.setUploadStage((String)chilldMap.get("uploadStage"));
                            designDoc.setFileName((String)chilldMap.get("name"));
                            long l = updateDesignDoc(fdevTask.getId(), designDoc);
                            logger.info("????????????????????????:{}",l);
                        }
                    }
                }
            }
            if(iosId.equals(listMap.get("id"))){
                //ios?????????
                List childFile = (List)listMap.get("childFile");
                if (!CommonUtils.isNullOrEmpty(listMap.get("childFile"))){
                    //?????????????????????????????????
                    for (Object child : childFile) {
                        Map<String,Object> chilldMap = (Map)child;
                        Map childInfo = fdocmanageService.content((String) chilldMap.get("id"));
                        String url = (String)childInfo.get("url");
                        logger.info("????????????????????????:{}",url);
                        String pathMinio  = fdevTask.getId() + "/"+"ios" + "/"+chilldMap.get("uploadStage") + "/" +chilldMap.get("name");
                        //??????
                        boolean result = false;
                        result = fdocmanageService.downloadAndUpload(url,pathMinio,(String)chilldMap.get("name"));
                        //??????
                        if(result){
                            DesignDoc designDoc = new DesignDoc();
                            designDoc.setDocType("ios");
                            designDoc.setMinioPath(pathMinio);
                            designDoc.setUploadStage((String)chilldMap.get("uploadStage"));
                            designDoc.setFileName((String)chilldMap.get("name"));
                            long l = updateDesignDoc(fdevTask.getId(), designDoc);
                            logger.info("????????????????????????:{}",l);
                        }
                    }
                }
            }
        }

    }
    public long updateDesignDoc(String taskId, DesignDoc designDoc){
        FdevTask fdevTask = new FdevTask();
        fdevTask.setId(taskId);
        List<FdevTask> fdevTasks = null;
        try {
            fdevTasks = fdevTaskDao.queryAll(fdevTask);
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("??????????????????");
            return 0;
        }
        List<DesignDoc> designDocs = Optional.ofNullable(fdevTasks.get(0).getDesignDoc()).orElse(new ArrayList<>());
        designDocs.add(designDoc);
        return updateDesignDoc(taskId, designDocs);
    }
    /**
     * ?????????????????????doc
     *
     * @param taskId
     * @param doc
     * @return
     */
    @Override
    public Long updateTaskDoc(String taskId, List<Map<String, String>> doc) throws Exception {
        User user = userVerifyUtil.getSessionUser(((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest()
                .getHeader(com.spdb.fdev.common.dict.Dict.TOKEN_AUTHORIZATION_KEY));
        FdevTask fdevTask = new FdevTask();
        fdevTask.setId(taskId);
        List<FdevTask> fdevTasks = fdevTaskDao.queryAll(fdevTask);
        List<Map<String, String>> newDoc = Optional.ofNullable(fdevTasks.get(0).getNewDoc()).orElse(new ArrayList<>());
        //???????????????????????????????????????????????????
        for(Map docMap: doc){
            if( !CommonUtils.isNullOrEmpty( newDoc ) ){
                for (Map<String, String> newDocMap : newDoc) {
                    if(!CommonUtils.isNullOrEmpty(docMap.get("path")) &&
                            docMap.get("path").equals(newDocMap.get("path"))){
                        newDoc.remove(newDocMap);
                        break;
                    }
                }
            }
            docMap.put(Dict.UPDATE_TIME,CommonUtils.dateFormat(new Date(),CommonUtils.DATE_TIME_PATTERN));
            docMap.put(Dict.USER_ID,user.getId());
            docMap.put(Dict.USER_NAME_EN,user.getUser_name_en());
            docMap.put(Dict.USER_NAME_CN,user.getUser_name_cn());
            if (!CommonUtils.isNullOrEmpty(docMap.get("type")) && "?????????-?????????????????????".equals(docMap.get("type"))) {
                newDoc = newDoc.stream().filter(p -> !"?????????-?????????????????????".equals(p.get("type"))).collect(Collectors.toList());
            }
        }
        newDoc.addAll(doc);
        /*newDoc = newDoc
                .stream()
                .distinct()
                .filter(map -> !CommonUtils.isNullOrEmpty(map))
                .collect(Collectors.toList());*/
        Query query = Query.query(Criteria.where(Dict.ID).is(taskId));
        Update update = new Update()
                .set("newDoc", newDoc);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, FdevTask.class);
        return updateResult.getMatchedCount();
    }

    /**
     * ?????????????????????doc
     *
     * @param taskId
     * @param path
     * @return
     */
    @Override
    public Long deleteTaskDoc(String taskId, String path) throws Exception {
        User user = userVerifyUtil.getSessionUser(((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest()
                .getHeader(com.spdb.fdev.common.dict.Dict.TOKEN_AUTHORIZATION_KEY));
        boolean result = fdocmanageService.deleteFiletoMinio("fdev-task", path, user);
        if(!result){
            throw new FdevException(ErrorConstants.TASK_ERROR, new String[]{"??????????????????,?????????!"});
        }
        FdevTask fdevTask = new FdevTask();
        fdevTask.setId(taskId);
        List<FdevTask> fdevTasks = fdevTaskDao.queryAll(fdevTask);
        List<Map<String, String>> newDoc = fdevTasks.get(0).getNewDoc();
        for (int i = 0; i < newDoc.size(); i++) {
            String path1 = newDoc.get(i).get("path");
            if (path.equals(path1)) {
                newDoc.remove(i);
                break;
            }
        }
        Query query = Query.query(Criteria.where(Dict.ID).is(taskId));
        Update update = new Update()
                .set("newDoc", newDoc);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, FdevTask.class);
        return updateResult.getMatchedCount();
    }
    @Override
    public Long uploadFile(String taskId, List<Map<String, String>> doc, MultipartFile[] file) throws Exception {
        User user = userVerifyUtil.getSessionUser(((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest()
                .getHeader(com.spdb.fdev.common.dict.Dict.TOKEN_AUTHORIZATION_KEY));
        //?????????????????????????????????????????????????????????????????????????????????ZIP?????????
        for(Map docMap: doc){
            if (!CommonUtils.isNullOrEmpty(docMap.get("type")) && "?????????-?????????????????????".equals(docMap.get("type"))) {
                for (MultipartFile files : file) {
                    String fileType = files.getOriginalFilename().substring(files.getOriginalFilename().lastIndexOf(".") + 1);
                    if (!fileType.equals("zip")) {
                        throw new FdevException(ErrorConstants.FILE_FMT_ERROR, new String[]{"????????????????????????"});
                    }
                }
            }
        }
        //????????????
        boolean result = fdocmanageService.uploadFilestoMinio("fdev-task", doc, file, user);
        //????????????????????????????????????
        if (result) {
            Long aLong = updateTaskDoc(taskId, doc);
            logger.info("?????????????????????????????????update={}", aLong);
            return aLong;
        }
        throw new FdevException(ErrorConstants.TASK_ERROR, new String[]{"??????????????????,?????????!"});
    }

    public List<Map<String, String>> getPath(FdevTask fdevTask, List<String> filesPath) {
        //??????????????????
        List<String> paths = new ArrayList<>();
        try {
            String taskName = fdevTask.getName();//????????????
            String task_id = fdevTask.getId();//??????id
            String encoderTaskName = URLEncoder.encode(taskName, "utf-8").replaceAll("\\+", "%20").replaceAll("%", "a");
            // ????????? ???????????????????????????????????????????????????????????????/taskName(?????????)-taskId(??????id)/????????????/??????
            String pathFirst = taskName + "-" + task_id;
            //???????????????????????????URLEncoder??????
            String pathSecond = encoderTaskName + "-" + task_id;
            //??????????????????????????????????????????10???
            String pathThird = "";
            if (encoderTaskName.length() > 10) {
                pathThird = encoderTaskName.substring(0, 10) + "-" + task_id;
            }
            paths.add(pathFirst);
            paths.add(pathSecond);
            paths.add(pathThird);
            paths = paths.stream().filter((str) -> !CommonUtils.isNullOrEmpty(str))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.info("?????????????????????");
            logger.error(e.toString());
        }
        //??????????????????
        List<String> objects = new ArrayList<>();
        Optional.ofNullable(paths)
                .map(List::stream)
                .orElseGet(Stream::empty)
                .forEach(str -> {
                    filesPath.forEach(minioPath -> {
                        boolean contains = minioPath.contains(str);
                        if (contains) {
                            objects.add(minioPath);
                        }
                    });
                });
        //??????????????????name type path
        List<Map<String, String>> lists = new ArrayList<>();
        objects.forEach(str -> {
            List<String> list = Lists.reverse(Arrays.asList(str.split("/")));
            if (list.size() >= 2) {
                HashMap<String, String> map = Maps.newHashMap();
                map.put("name", list.get(0));
                map.put("type", list.get(1));
                map.put("path", str.replaceFirst("fdev-task/", ""));
                lists.add(map);
            }
        });
        return lists;
    }

    public boolean isExistFile(FdevTask fdevTask, String path) {
        boolean flag = false;
        List<Map<String, String>> newDoc = Optional.ofNullable(fdevTask.getNewDoc()).orElse(new ArrayList<>());
        for (int i = 0; i < newDoc.size(); i++) {
            String path1 = newDoc.get(i).get("path");
            if (path.equals(path1)) {
                flag = true;
            }
        }
        logger.info("??????????????????????????????Flag:{}", flag);
        return flag;
    }

    @Override
    public Map<String, Object> uploadSecurityTestDoc(MultipartFile file, String fileType, String taskId) throws Exception {
        //?????????minio
        User user = userVerifyUtil.getSessionUser(((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest()
                .getHeader(com.spdb.fdev.common.dict.Dict.TOKEN_AUTHORIZATION_KEY));
        String fileName = file.getOriginalFilename();
        //????????????
        String pathMinio  = Constants.APPLICATION_NAME + "/" + taskId + "/"+ fileType+ "/" + fileName;
        boolean result = fdocmanageService.uploadFiletoMinio(Constants.APPLICATION_NAME, pathMinio, file, user);
        if(result) {
            Map<String,Object> resultMap = new HashMap<>();
            resultMap.put(Dict.FILEPATH, pathMinio);
            if(Constants.FILETYPE_TRANS.equals(fileType)) {
                List<Map<String,String>> transList = new ArrayList<>();
                //??????excel??????????????????????????????
                readTransListFile(file, transList);
                resultMap.put(Dict.TRANSLIST, transList);
            }
            return resultMap;
        }else {
            throw new FdevException(ErrorConstants.TASK_ERROR, new String[] {"??????????????????"});
        }
    }

    /**
     * ????????????????????????????????????
     * @param file
     * @param transList
     * @throws IOException
     */
    public void readTransListFile(MultipartFile file, List<Map<String,String>> transList) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
        XSSFSheet sheet = workbook.getSheetAt(0);
        int i = 1;
        Map<String,String> transMap;
        while (true) {
            //???????????????????????????2?????????
            XSSFRow row = sheet.getRow(i);
            //??????
            XSSFCell cell = row.getCell(0);
            String index = "";
            if("STRING".equals(cell.getCellTypeEnum().name())) {
                index = cell.getStringCellValue();
            }else if("NUMERIC".equals(cell.getCellTypeEnum().name())) {
                index = String.valueOf((int)cell.getNumericCellValue());
            }else {
                index = cell.toString();
            }
            //????????????
            cell = row.getCell(1);
            String transName = cell.getStringCellValue();
            if(CommonUtils.isNullOrEmpty(transName)) {
                break;
            }
            //????????????
            cell = row.getCell(2);
            String transDesc = cell.getStringCellValue();
            //????????????
            cell = row.getCell(3);
            String functionMenu = cell.getStringCellValue();
            transMap = new HashMap<>();
            transMap.put(Dict.INDEX, index);
            transMap.put(Dict.TRANSNAME, transName);
            transMap.put(Dict.TRANSDESC, transDesc);
            transMap.put(Dict.FUNCTIONMENU, functionMenu);
            transList.add(transMap);
            ++i;
        }
    }


    @Override
    public void downloadTemplateFile(HttpServletResponse resp, String fileType) {
        String templatePath = "";
        String fileName = "";
        if(Constants.FILETYPE_TRANS.equals(fileType)) {
            templatePath = Constants.TRANS_TEMPLATE_XLSX;
            fileName= Constants.TRANS_XLSX_NAME;
        }
        ClassPathResource resource = new ClassPathResource(templatePath);
        try (OutputStream output = resp.getOutputStream();
              InputStream in = resource.getInputStream()) {
            // ????????????
            resp.reset();
            resp.setContentType("application/octet-stream");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Access-Control-Allow-Origin", "*");
            resp.setHeader("Content-Disposition", "attachment;filename="+URLEncoder.encode(fileName,"UTF-8")+".xlsx");
            // ?????????
            byte buffer[] = new byte[4096];
            int x = -1;
            while ((x = in.read(buffer, 0, 4096)) != -1) {
                output.write(buffer, 0, x);
            }
            resp.flushBuffer();
        } catch (Exception e) {
            logger.error(">>>downloadTemplateFile error:"+e);
            throw new FdevException(ErrorConstants.TASK_ERROR, new String[] {"????????????????????????"});
        }
    }
}
