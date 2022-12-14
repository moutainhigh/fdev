package com.spdb.fdev.fdemand.spdb.service.impl;


import com.spdb.fdev.common.User;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.fdemand.base.dict.Constants;
import com.spdb.fdev.fdemand.base.dict.DemandDocEnum;
import com.spdb.fdev.fdemand.base.dict.Dict;
import com.spdb.fdev.fdemand.base.dict.ErrorConstants;
import com.spdb.fdev.fdemand.base.utils.CommonUtils;
import com.spdb.fdev.fdemand.base.utils.DemandBaseInfoUtil;
import com.spdb.fdev.fdemand.base.utils.TimeUtil;
import com.spdb.fdev.fdemand.spdb.dao.IDemandAssessDao;
import com.spdb.fdev.fdemand.spdb.dao.IDemandDocDao;
import com.spdb.fdev.fdemand.spdb.dao.IImplementUnitDao;
import com.spdb.fdev.fdemand.spdb.dao.impl.IpmpUnitDaoImpl;
import com.spdb.fdev.fdemand.spdb.entity.*;
import com.spdb.fdev.fdemand.spdb.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

@RefreshScope
@Service
public class DocServiceImpl implements IDocService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());


    @Autowired
    private IDemandDocDao demandDocDao;

    @Autowired
    private DemandDoc demandDoc;

    @Autowired
    private DemandBaseInfo demandBaseInfo;

    @Autowired
    private IRoleService roleService;

    @Autowired
    private IDemandAssessDao demandAssessDao;

    @Autowired
    private ITaskService taskService;

    @Autowired
    private IpmpUnitDaoImpl ipmpUnitDao;

    @Autowired
    private IGitlabService gitlabService;

    @Value("${fdev.docmanage.domain}")
    private String url;

    @Autowired
    private RestTemplate restTemplate;

    @Resource
    private FdocmanageService fdocmanageService;

    @Autowired
    private IImplementUnitDao implementUnitDao;

    @Value("${fdemand.doc.folder}")
    private String docFolder;

    @Override
    public Map queryDemandDocPagination(Map<String, Object> param) {
        Integer pageSize = (Integer) param.get(Dict.SIZE);//????????????
        Integer currentPage = (Integer) param.get(Dict.INDEX);//?????????
        if (CommonUtils.isNullOrEmpty(pageSize)) {
            pageSize = 0;
        }
        if (CommonUtils.isNullOrEmpty(currentPage)) {
            currentPage = 1;
        }
        if (CommonUtils.isNullOrEmpty(param.get(Dict.DEMAND_ID))) {
            String[] args = {Dict.DEMAND_ID};
            throw new FdevException("COMM002", args);
        }

        if (CommonUtils.isNullOrEmpty(param.get(Dict.DEMAND_DOC_TYPE))) {
            param.put(Dict.DEMAND_DOC_TYPE, "");
        }

        if (CommonUtils.isNullOrEmpty(param.get(Dict.DEMAND_KIND))) {
            param.put(Dict.DEMAND_KIND, "demand");
        }

        Integer start = pageSize * (currentPage - 1);   //??????
        List data = demandDocDao.queryDemandDocPagination(start, pageSize, param.get(Dict.DEMAND_ID).toString(), param.get(Dict.DEMAND_DOC_TYPE).toString(), param.get(Dict.DEMAND_KIND).toString());
        Long count = demandDocDao.queryCountDemandDoc(param.get(Dict.DEMAND_ID).toString(), param.get(Dict.DEMAND_DOC_TYPE).toString(), param.get(Dict.DEMAND_KIND).toString());
        Map result = new HashMap();
        result.put(Dict.DATA, data);
        result.put(Dict.COUNT, count);
        return result;
    }


    /**
     * ???????????????doc
     *
     * @param demand_id
     * @param
     * @return
     */
    @Override
    public void uploadFile(String demand_id, String doc_type, String doc_link, String user_group_id, String user_group_cn, MultipartFile[] file, String demand_kind) throws Exception {
        User user = (User) ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest()
                .getSession().getAttribute("_USER");
//        boolean isDemandLeader = roleService.isDemandLeader(demand_id, user.getId());
//        boolean isDemandGroupLeader = roleService.isDemandGroupLeader(demand_id, user.getId());
//        if (!roleService.isDemandManager() && !isDemandLeader && !isDemandGroupLeader) {
//            throw new FdevException(ErrorConstants.ROLE_ERROR, new String[]{"????????????????????????????????????????????????????????????????????????????????????"});
//        }
        DemandDoc demandDoc = new DemandDoc();
        List<String> listPathAll = new ArrayList<String>();
        if (CommonUtils.isNullOrEmpty(doc_link)) {
            //path:??????/??????id/????????????/?????????
            String pathCommon = docFolder + "/" + demand_id + "/" + DemandBaseInfoUtil.getDocType(doc_type) + "/";
            listPathAll = fdocmanageService.uploadFilestoMinio("fdev-demand", pathCommon, file, user);
            if (CommonUtils.isNullOrEmpty(listPathAll)) {
                throw new FdevException(ErrorConstants.DOC_ERROR_UPLOAD, new String[]{"??????????????????,?????????!"});
            }
            //????????????????????????????????????????????????
            updateDemandDoc(user, demand_id, doc_type, listPathAll, user_group_id, user_group_cn, file, demand_kind);
        } else {
            //???????????????????????????????????????
            updateDemandDocLink(user, demand_id, doc_type, doc_link, user_group_id, user_group_cn, demand_kind);
        }


    }

    /**
     * ???????????????doc
     *
     * @param demand_id
     * @return
     */

    @Override
    public void updateDemandDoc(User user, String demand_id, String doc_type, List<String> listPathAll, String user_group_id, String user_group_cn, MultipartFile[] file, String demand_kind) throws Exception {
        DemandDoc demandDoc = new DemandDoc();
        for (String path : listPathAll) {
            demandDoc.setDemand_id(demand_id);
            demandDoc.setDoc_path(path);
            demandDoc.setDoc_type(doc_type);
            demandDoc.setUser_group_id(user_group_id);
            demandDoc.setUser_group_cn(user_group_cn);
            if (StringUtils.isEmpty(demand_kind)) {
                demandDoc.setDemand_kind("demand");
            } else {
                demandDoc.setDemand_kind(demand_kind);
            }
            String doc_name = path.substring(path.lastIndexOf("/"));
            demandDoc.setDoc_name(doc_name.substring(1));
            List<DemandDoc> demandDocList = demandDocDao.query(demandDoc);
            if (CommonUtils.isNullOrEmpty(demandDocList)) {
                demandDoc.setUpload_user(user.getId());//????????????id
                UserInfo userInfo = new UserInfo();
                userInfo.setId(user.getId());
                userInfo.setUser_name_cn(user.getUser_name_cn());
                userInfo.setUser_name_en(user.getUser_name_en());
                demandDoc.setUpload_user_all(userInfo);
                demandDoc.setCreate_time(TimeUtil.getTimeStamp(new Date()));//??????????????????
                demandDocDao.save(demandDoc);
            } else {
                DemandDoc demandDocBefore = demandDocList.get(0);
                demandDoc.setUpdate_time(TimeUtil.getTimeStamp(new Date()));
                demandDoc.setId(demandDocBefore.getId());
                demandDocDao.updateById(demandDoc);
            }
        }
    }

    @Override
    public void updateDemandDocLink(User user, String demand_id, String doc_type, String doc_link, String user_group_id, String user_group_cn, String demand_kind) throws Exception {
        DemandDoc demandDoc = new DemandDoc();
        demandDoc.setDemand_id(demand_id);
        demandDoc.setDoc_link(doc_link);
        demandDoc.setDoc_type(doc_type);
        demandDoc.setUser_group_id(user_group_id);
        demandDoc.setUser_group_cn(user_group_cn);
        if (StringUtils.isEmpty(demand_kind)) {
            demandDoc.setDemand_kind("demand");
        } else {
            demandDoc.setDemand_kind(demand_kind);
        }
        List<DemandDoc> demandDocList = demandDocDao.query(demandDoc);
        if (CommonUtils.isNullOrEmpty(demandDocList)) {
            demandDoc.setUpload_user(user.getId());//????????????id
            UserInfo userInfo = new UserInfo();
            userInfo.setId(user.getId());
            userInfo.setUser_name_cn(user.getUser_name_cn());
            userInfo.setUser_name_en(user.getUser_name_en());
            demandDoc.setUpload_user_all(userInfo);
            demandDoc.setCreate_time(TimeUtil.getTimeStamp(new Date()));//??????????????????
            demandDocDao.save(demandDoc);
        } else {
            DemandDoc demandDocBefore = demandDocList.get(0);
            demandDoc.setUpdate_time(TimeUtil.getTimeStamp(new Date()));
            demandDoc.setId(demandDocBefore.getId());
            demandDocDao.updateById(demandDoc);
        }
    }


    /**
     * ????????????????????????
     */
    @Override
    public DemandDoc save(DemandDoc demandDoc) throws Exception {
        return demandDocDao.save(demandDoc);
    }

    @Override
    public boolean uploadFiletoMinio(String moduleName, String path, MultipartFile multipartFile, User user) {
        String authorization = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest()
                .getHeader("Authorization");
        try {
            MultiValueMap<String, Object> param = new LinkedMultiValueMap<>();
            param.add("moduleName", moduleName);
            param.add("path", path);
            param.add("files", multipartFile.getResource());
            param.add("user", user);
            HttpEntity httpHeaders = setHttpHeader(param, authorization);
            String ula = url + "/fdocmanage/api/file/filesUpload";
            restTemplate.exchange(ula, HttpMethod.POST, httpHeaders, String.class);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            logger.info("???????????????????????????????????????:{},{}Error Trace:{}",
                    moduleName,
                    path,
                    sw.toString());
            return false;
        }
        return true;
    }

    //?????????gitlab?????????????????????????????????
    private final static List<String> docTypeList = new ArrayList<String>(){{
        add(DemandDocEnum.DemandDocTypeEnum.DEMAND_INSTRUCTION.getValue());
        add(DemandDocEnum.DemandDocTypeEnum.DEMAND_PLAN_INSTRUCTION.getValue());
        add(DemandDocEnum.DemandDocTypeEnum.INNER_TEST_REPORT.getValue());
        add(DemandDocEnum.DemandDocTypeEnum.DEMAND_ASSESS_REPORT.getValue());
        add(DemandDocEnum.DemandDocTypeEnum.DEMAND_PLAN_CONFIRM.getValue());
        add(DemandDocEnum.DemandDocTypeEnum.BUSINESS_TEST_REPORT.getValue());
        add(DemandDocEnum.DemandDocTypeEnum.LAUNCH_CONFIRM.getValue());
    }};
    @Async
    @Override
    public void fileToGitlab(DemandBaseInfo demand) {
        //????????????????????????????????????
        List<Map> taskList = taskService.queryTaskByDemandId(demand.getId());
        if (!CommonUtils.isNullOrEmpty(taskList)
                && taskList.stream().anyMatch(task -> null == task.get(Dict.TASKTYPE))) {
            //?????????????????????????????????
            List<IpmpUnit> ipmpUnitList = null;
            try {
                if (Constants.TECH.equals(demand.getDemand_type())) {
                    //????????????????????????
                    List<FdevImplementUnit> fdevUnitList = implementUnitDao.queryAllFdevUnitByDemandId(demand.getId());
                    if(!CommonUtils.isNullOrEmpty(fdevUnitList)){
                        Set implUnitNumList = fdevUnitList.stream().map(fdevUnit -> fdevUnit.getIpmp_implement_unit_no()).collect(Collectors.toSet());
                        if(!CommonUtils.isNullOrEmpty(implUnitNumList)){
                            Map<String,Object> ipmpMap = ipmpUnitDao.queryIpmpUnitByNums(new HashMap<String, Object>(){{
                                put(Dict.IMPLUNITNUMLIST,implUnitNumList);
                            }});
                            if (!CommonUtils.isNullOrEmpty(ipmpMap)) {
                                ipmpUnitList = (List<IpmpUnit>) ipmpMap.get(Dict.DATA);
                            }
                        }
                    }
                } else {
                    ipmpUnitList = ipmpUnitDao.queryIpmpUnitByDemandId(demand.getOa_contact_no());
                }
            } catch (Exception e) {
                throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[] {"?????????????????????????????????"});
            }
            if (!CommonUtils.isNullOrEmpty(ipmpUnitList)) {
                //???????????????????????????
                List<String> prjNumList = ipmpUnitList.stream().map(IpmpUnit::getPrjNum).distinct().collect(Collectors.toList());
                prjNumList.remove(null);
                //????????????????????????
                fileDemandDoc(demand, prjNumList);
                //????????????????????????
                fileTaskDoc(demand, taskList, prjNumList);
            }
        }
    }

    /**
     * ????????????????????????
     * @param demand
     * @param prjNumList
     */
    private void fileDemandDoc(DemandBaseInfo demand, List<String> prjNumList) {
        //?????????????????????
        DemandDoc demandDoc = new DemandDoc();
        demandDoc.setDemand_id(demand.getId());
        List<DemandDoc> demandDocList = new ArrayList<>();
        try {
            demandDocList = demandDocDao.queryAll(demandDoc);
        } catch (Exception e) {
            logger.info(">>>queryDemandFile fail,{}", demand.getId());
            throw new FdevException(ErrorConstants.DATA_QUERY_ERROR);
        }
        for (DemandDoc doc : demandDocList) {
            byte[] file = fdocmanageService.downloadFileByte("fdev-demand", doc.getDoc_path());
            //????????????????????????????????????????????????????????????????????????????????????
            StringBuffer path = new StringBuffer();
            path.append("/");
            if (docTypeList.contains(doc.getDoc_type())) {
                //?????????????????????
                if (DemandDocEnum.DemandDocTypeEnum.INNER_TEST_REPORT.getValue().equals(doc.getDoc_type())) {
                    //?????????????????????????????????????????????????????????????????????
                    if (doc.getDoc_name().contains(demand.getOa_contact_name())) {
                        path.append(DemandBaseInfoUtil.getDocType(doc.getDoc_type()));
                    } else {
                        continue;
                    }
                } else {
                    path.append(DemandBaseInfoUtil.getDocType(doc.getDoc_type()));
                }
            } else {
                path.append("??????????????????");
            }
            path.append("/");
            path.append(demand.getOa_contact_no());
            path.append("/");
            path.append(doc.getDoc_name());
            //???????????????gitlab
            uploadToGitLab(prjNumList, file, path);
        }
    }

    /**
     * ????????????????????????
     * @param demand
     * @param taskList
     * @param prjNumList
     */
    private void fileTaskDoc(DemandBaseInfo demand, List<Map> taskList, List<String> prjNumList) {
        //???????????????????????????????????????
        for (Map<String, Object> task : taskList) {
            List<Map> taskDocList = taskService.queryDocDetail((String) task.get(Dict.ID));
            if (!CommonUtils.isNullOrEmpty(taskDocList)) {
                for (Map<String, Object> taskDoc : taskDocList) {
                    if (Constants.BUSINESS.equals(demand.getDemand_type()) && "?????????-????????????".equals(taskDoc.get(Dict.TYPE))) {
                        continue;
                    }
                    byte[] file = fdocmanageService.downloadFileByte("fdev-task", (String) taskDoc.get(Dict.PATH));
                    StringBuffer path = new StringBuffer();
                    path.append("/");
                    if ("?????????-????????????".equals(taskDoc.get(Dict.TYPE))) {
                        path.append("????????????");
                    } else {
                        path.append("??????????????????");
                    }
                    path.append("/");
                    path.append(demand.getOa_contact_no());
                    path.append("/");
                    path.append(task.get(Dict.NAME));
                    path.append("/");
                    path.append(taskDoc.get(Dict.NAME));
                    //???????????????gitlab
                    uploadToGitLab(prjNumList, file, path);
                }
            }
        }
    }

    /**
     * ???????????????gitlab
     * @param prjNumList
     * @param file
     * @param path
     */
    private void uploadToGitLab(List<String> prjNumList, byte[] file, StringBuffer path) {
        //???????????????????????????????????????????????????
        for (String prjNum : prjNumList) {
            //?????????gitlab
            List<byte[]> files = new ArrayList<>();
            files.add(file);
            gitlabService.uploadFile(files, prjNum + path.toString());
        }
    }

    private HttpEntity setHttpHeader(MultiValueMap<String, Object> param, String auth) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", auth);
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "multipart/form-data");
        HttpEntity request = new HttpEntity<Object>(param, httpHeaders);
        return request;
    }

    /**
     * ????????????doc
     * ????????????
     *
     * @param
     */
    @Override
    public void deleteDemandDoc(Map params) throws Exception {
        User user = (User) ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest()
                .getSession().getAttribute("_USER");
        List<String> listIds = (List<String>) params.get(Dict.IDS);
        for (String id : listIds) {
            String doc_id = id;
            DemandDoc demandDoc = demandDocDao.queryById(doc_id);
            String demand_id = demandDoc.getDemand_id();
            // ???????????????????????????
            if ("demandAccess".equals(String.valueOf(params.get("demand_kind")))) {
                // ??????id ????????????????????????????????????
                DemandAssess getDemandAssess = demandAssessDao.queryById(demand_id);
                // ????????????????????????????????????????????????????????????????????????
                if (CommonUtils.isNullOrEmpty(getDemandAssess)) {
                    throw new FdevException(ErrorConstants.DEMAND_NULL_ERROR);
                }
                // ????????????????????????
                if (!(roleService.isDemandManager() ||
                        (!CommonUtils.isNullOrEmpty(getDemandAssess.getDemand_leader()) &&
                                getDemandAssess.getDemand_leader().contains(CommonUtils.getSessionUser().getId()))
                )) {
                    // ????????????????????????????????????????????????
                    throw new FdevException(ErrorConstants.FUSER_ROLE_ERROR);
                }
            } else {
                // ????????????????????????
                boolean isDemandLeader = roleService.isDemandLeader(demand_id, user.getId());
                boolean isDemandGroupLeader = roleService.isDemandGroupLeader(demand_id, user.getId());
                if (!roleService.isDemandManager() && !isDemandLeader && !isDemandGroupLeader) {
                    throw new FdevException(ErrorConstants.ROLE_ERROR, new String[]{"????????????????????????????????????????????????????????????????????????????????????"});
                }
            }

        }
        List<String> listIdStrings = new ArrayList<>();
        List<String> listLinkStrings = new ArrayList<>();
        listIdStrings = (List<String>) params.get(Dict.IDS);
        listLinkStrings = (List<String>) params.get(Dict.DOC_LINK);
        if (CommonUtils.isNullOrEmpty(listLinkStrings)) {
            for (String id : listIdStrings) {
                DemandDoc deamnDocs = demandDocDao.queryById(id);
                if (CommonUtils.isNullOrEmpty(deamnDocs)) {
                    continue;
                }
                String path = deamnDocs.getDoc_path();
                boolean result = fdocmanageService.deleteFiletoMinio("fdev-demand", path, user);
                if (!result) {
                    throw new FdevException(ErrorConstants.DOC_ERROR_DELETE, new String[]{"??????????????????,?????????!"});
                }
                demandDocDao.deleteById(id);
            }
        } else {
            for (String id : listIdStrings) {
                DemandDoc deamnDocs = demandDocDao.queryById(id);
                if (CommonUtils.isNullOrEmpty(deamnDocs)) {
                    continue;
                }
                demandDocDao.deleteById(id);
            }
        }

    }
}
