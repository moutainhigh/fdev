package com.spdb.fdev.fdevinterface.spdb.service.impl;

import com.spdb.fdev.common.User;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.common.util.ErrorMessageUtil;
import com.spdb.fdev.fdevinterface.base.dict.Constants;
import com.spdb.fdev.fdevinterface.base.dict.Dict;
import com.spdb.fdev.fdevinterface.base.dict.ErrorConstants;
import com.spdb.fdev.fdevinterface.base.utils.FileUtil;
import com.spdb.fdev.fdevinterface.spdb.callable.BaseScanCallable;
import com.spdb.fdev.fdevinterface.spdb.callable.CallableFactory;
import com.spdb.fdev.fdevinterface.spdb.entity.ScanRecord;
import com.spdb.fdev.fdevinterface.spdb.service.RestTransportService;
import com.spdb.fdev.fdevinterface.spdb.service.ScanRecordService;
import com.spdb.fdev.fdevinterface.spdb.service.ScannerService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class ScannerServiceImpl implements ScannerService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value(value = "${path.git.clone}")
    private String gitClonePath;
    @Value(value = "${git.clone.user}")
    private String gitCloneUser;
    @Value(value = "${git.clone.password}")
    private String gitClonePassword;
    @Value(value = "${msper.web.common.service.clone.url}")
    private String commonServiceCloneUrl;
    @Value(value = "${vue.project.type.id}")
    private String vueProjectTypeId;
    @Resource
    private ScanRecordService scanRecordService;
    @Autowired
    private CallableFactory callableFactory;
    @Resource
    private RestTransportService transportService;
    @Resource
    private ErrorMessageUtil errorMessageUtil;
    @Autowired
    private RestTransportServiceImpl restTransportServiceImpl;

    private ScanRecord scanRecord;
    // ???????????????
    private static ExecutorService pool;

    static {
        pool = new ThreadPoolExecutor(Constants.TEN, Constants.TEN,
                60L, TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(Constants.HUNDRED));
    }

    @Override
    public synchronized Map scanInterface(String appServiceId, String branchName, String type, Integer projectId) {
        // ?????????????????????????????????-parent
        if (appServiceId.endsWith(Dict.PARENT)) {
            appServiceId = appServiceId.replace(Dict.PARENT, "");
        }
        // ??????????????????????????????????????????????????????cloneUrl???GitLab Project ID
        Map urlAndProIdMap = transportService.getAppCloneUrlAndProId(appServiceId);
        String cloneUrl = "";
        if (urlAndProIdMap != null && urlAndProIdMap.size() != 0) {
            cloneUrl = (String) urlAndProIdMap.get(Dict.GIT);
            if (projectId == 0) {
                projectId = (Integer) urlAndProIdMap.get(Dict.GITLAB_PROJECT_ID);
            }
        }
        // clone????????????
        FileUtil.cloneProject(appServiceId, cloneUrl, branchName, gitClonePath, gitCloneUser, gitClonePassword, commonServiceCloneUrl);
        // ??????src??????????????????
        List<String> srcPathList = new ArrayList<>();
        FileUtil.getSrcPath(gitClonePath, srcPathList);
        if (CollectionUtils.isEmpty(srcPathList)) {
            throw new FdevException(ErrorConstants.SRC_FILE_NOT_EXIST, new String[]{appServiceId});
        }
        Map map = restTransportServiceImpl.getAppInfo(appServiceId);
        String typeId = "";
        if (!FileUtil.isNullOrEmpty(map)) {
            typeId = (String) map.get("type_id");
        }
        // ????????????????????????????????????????????????????????????
        User user;
        String scanType = Constants.AUTO_SCAN;
        try {
            user = (User) ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest()
                    .getSession().getAttribute("_USER");
            if (user != null) {
                scanType = Constants.HAND_SCAN + "(" + user.getUser_name_cn() + ")";
            }
        } catch (Exception e) {
            logger.info("??????????????????????????????!{}", e.getMessage());
        }
        scanRecord = new ScanRecord();
        scanRecord.setType(scanType);
        scanRecord.setServiceId(appServiceId);
        scanRecord.setBranch(branchName);
        // ???????????????????????????
        List<Callable> callableList = getCallableList(srcPathList, appServiceId, branchName, type, projectId, typeId, scanType);
        Map returnMap;
        try {
            returnMap = runCallable(callableList);
            if (StringUtils.isEmpty(returnMap.get(Dict.SUCCESS)) && StringUtils.isEmpty(returnMap.get(Dict.ERROR))) {
                if (vueProjectTypeId.equals(typeId)) {
                    returnMap.put(Dict.ERROR, "???????????????????????????vue????????????????????????????????????/????????????/??????-?????????????????????????????????!");
                } else {
                    returnMap.put(Dict.ERROR, "????????????????????????????????????????????????????????????????????????/??????/????????????-???????????????????????????!");
                }
            }
        } catch (Exception e) {
            logger.error("??????{}??????!{}", appServiceId, e.getMessage());
            unknownError();
            throw new FdevException(ErrorConstants.SERVER_ERROR, new String[]{"????????????!"});
        } finally {
            // ??????????????????
            scanRecordService.save(scanRecord);
            // ???????????????????????????clone???????????????
            FileUtil.deleteFiles(new File(gitClonePath));
        }
        return returnMap;
    }

    /**
     * ???????????????????????????
     *
     * @param srcPathList
     * @param appServiceId
     * @param branchName
     * @param type
     * @param projectId
     * @return
     */
    private List<Callable> getCallableList(List<String> srcPathList, String appServiceId, String branchName, String type, Integer projectId, String typeId, String scanType) {
        List<Callable> callableList = new ArrayList<>();
        List<String> callableNameList = new ArrayList<>();
        if (Dict.MSPER_WEB_COMMON_SERVICE.equals(appServiceId)) {
            // ????????????????????????
            BaseScanCallable commonSoapCallable = callableFactory.getCallableByName(Dict.COMMON_SOAP_CALLABLE, srcPathList, branchName, appServiceId, projectId, scanType);
            callableList.add(commonSoapCallable);
            return callableList;
        }
        //??????fdev???????????????????????????,???????????????master??????
        if (Dict.TEST_MANAGE_UI.equals(appServiceId) || Dict.FDEV_VUE_ADMIN.equals(appServiceId)) {
            BaseScanCallable InterfaceStatisticsCallable = callableFactory.getCallableByName(Dict.VUE_INTERFACE_STATISTICS_CALLABLE, srcPathList, branchName, appServiceId, projectId, scanType);
            callableList.add(InterfaceStatisticsCallable);
            return callableList;
        }
        // ???????????????type
        if (type.equals("124")) {
            type = "09";
        }
        if (type.contains("0")) {
            // ?????????????????????
            callableNameList.add(Dict.TRANS_CALLABLE);
            if (Dict.SPDB_CLI_MOBCLI.equals(appServiceId) || Dict.SPDB_CLI_CMNET.equals(appServiceId)) {
                // ?????????????????????????????????????????????
                callableNameList.add(Dict.TRANS_RELATION_CONTAINER_CALLABLE);
            } else if (vueProjectTypeId.equals(typeId)) {
                // ??????vue??????????????????
                callableNameList.add(Dict.TRANS_RELATION_VUE_CALLABLE);
            }
        }
        if (type.contains("9")) {
            if (vueProjectTypeId.equals(typeId)) {
                // ???????????????vue??????????????????
                callableNameList.add(Dict.VUE_ROUTER_CALLABLE);
            } else {
                // ??????SOAP???????????????
                callableNameList.add(Dict.SOAP_RELATION_CALLABLE);
                if (!appServiceId.startsWith(Dict.MSPER)) {
                    // ??????SOP?????????????????????msper?????????????????????
                    callableNameList.add(Dict.SOP_RELATION_CALLABLE);
                }
                // ??????REST???????????????
                callableNameList.add(Dict.REST_RELATION_CALLABLE);
                // ??????REST???????????????
                callableNameList.add(Dict.REST_CALLABLE);
                // ??????SOAP???????????????
                callableNameList.add(Dict.SOAP_CALLABLE);
            }
        }
        for (String callableName : callableNameList) {
            BaseScanCallable soapCallable = callableFactory.getCallableByName(callableName, srcPathList, branchName, appServiceId, projectId, scanType);
            callableList.add(soapCallable);
        }
        return callableList;
    }

    /**
     * ????????????????????????????????????
     *
     * @param callableList
     * @return
     */
    private Map runCallable(List<Callable> callableList) throws ExecutionException, InterruptedException {
        Map<String, String> returnMap = new HashMap<>();
        List<Future> futureList = new ArrayList<>();
        // ???????????????????????????
        StringBuilder successMsg = new StringBuilder();
        // ??????????????????
        StringBuilder errorMsg = new StringBuilder();
        // ???????????????
        for (Callable callable : callableList) {
            Future future = pool.submit(callable);
            futureList.add(future);
        }
        // ????????????????????????
        for (Future future : futureList) {
            Map futureMap = (Map) future.get();
            String success = (String) futureMap.get(Dict.SUCCESS);
            String error = (String) futureMap.get(Dict.ERROR);
            if (!StringUtils.isEmpty(success)) {
                successMsg.append(success);
                getSuccessScanRecord(success);
            }
            if (!StringUtils.isEmpty(error)) {
                errorMsg.append(error);
                getErrorScanRecord(error);
            }
        }
        // ??????????????????????????????
        returnMap.put(Dict.SUCCESS, successMsg.toString());
        returnMap.put(Dict.ERROR, errorMsg.toString());
        return returnMap;
    }

    private void getSuccessScanRecord(String success) {
        if (success.contains("REST???????????????")) {
            scanRecord.setRest(getSuccessMap(success));
        }
        if (success.contains("REST???????????????")) {
            scanRecord.setRestRel(getSuccessMap(success));
        }
        if (success.contains("SOAP???????????????")) {
            scanRecord.setSoap(getSuccessMap(success));
        }
        if (success.contains("SOAP???????????????")) {
            scanRecord.setSoapRel(getSuccessMap(success));
        }
        if (success.contains("SOP???????????????")) {
            scanRecord.setSopRel(getSuccessMap(success));
        }
        if (success.contains("???????????????")) {
            scanRecord.setTrans(getSuccessMap(success));
        }
        if (success.contains("???????????????")) {
            scanRecord.setTransRel(getSuccessMap(success));
        }
        if (success.contains("VUE????????????")) {
            scanRecord.setRouter(getSuccessMap(success));
        }
    }

    private void getErrorScanRecord(String error) {
        if (error.contains("REST???????????????")) {
            scanRecord.setRest(getErrorMap(error));
        }
        if (error.contains("REST???????????????")) {
            scanRecord.setRestRel(getErrorMap(error));
        }
        if (error.contains("SOAP???????????????")) {
            scanRecord.setSoap(getErrorMap(error));
        }
        if (error.contains("SOAP???????????????")) {
            scanRecord.setSoapRel(getErrorMap(error));
        }
        if (error.contains("SOP???????????????")) {
            scanRecord.setSopRel(getErrorMap(error));
        }
        if (error.contains("???????????????")) {
            scanRecord.setTrans(getErrorMap(error));
        }
        if (error.contains("???????????????")) {
            scanRecord.setTransRel(getErrorMap(error));
        }
        if (error.contains("VUE????????????")) {
            scanRecord.setRouter(getErrorMap(error));
        }
    }

    private Map getSuccessMap(String success) {
        Map<String, String> map = new HashMap<>();
        map.put(Dict.CODE, "1");
        map.put(Dict.MSG, success);
        return map;
    }

    private Map getErrorMap(String error) {
        Map<String, String> map = new HashMap<>();
        map.put(Dict.CODE, "2");
        map.put(Dict.MSG, error);
        return map;
    }

    private void unknownError() {
        Map<String, String> map = new HashMap<>();
        map.put(Dict.CODE, "2");
        map.put(Dict.MSG, "???????????????");
        scanRecord.setRest(map);
        scanRecord.setRestRel(map);
        scanRecord.setSoap(map);
        scanRecord.setSoapRel(map);
        scanRecord.setSopRel(map);
        scanRecord.setTrans(map);
        scanRecord.setTransRel(map);
        scanRecord.setRouter(map);
    }

}
