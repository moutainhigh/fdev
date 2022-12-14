package com.spdb.fdev.component.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spdb.fdev.base.Constant;
import com.spdb.fdev.base.dict.Constants;
import com.spdb.fdev.base.dict.Dict;
import com.spdb.fdev.base.dict.ErrorConstants;
import com.spdb.fdev.base.dict.MpassComEnum;
import com.spdb.fdev.base.utils.CommonUtils;
import com.spdb.fdev.base.utils.ShellUtils;
import com.spdb.fdev.base.utils.TimeUtils;
import com.spdb.fdev.base.utils.Util;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.component.dao.IComponentRecordDao;
import com.spdb.fdev.component.dao.IMpassDevIssueDao;
import com.spdb.fdev.component.dao.IMpassReleaseIssueDao;
import com.spdb.fdev.component.dao.ITagRecordDao;
import com.spdb.fdev.component.entity.*;
import com.spdb.fdev.component.schedule.service.ArchetypeVersionNotify;
import com.spdb.fdev.component.service.*;
import com.spdb.fdev.transport.RestTransport;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
@RefreshScope
public class MpassRelaseIssueServiceImpl implements IMpassRelaseIssueService {

    private static final Logger logger = LoggerFactory.getLogger(MpassRelaseIssueServiceImpl.class);

    @Autowired
    private IMpassReleaseIssueDao mpassReleaseIssueDao;

    @Autowired
    private IMpassDevIssueDao mpassDevIssueDao;

    @Autowired
    private IMpassComponentService mpassComponentService;

    @Autowired
    private IGitlabSerice gitlabSerice;

    @Autowired
    private IGitlabUserService gitlabUserService;

    @Autowired
    private ICommonInfoService commonInfoService;

    @Autowired
    private IRoleService roleService;

    @Autowired
    private IComponentInfoService componentInfoService;

    @Autowired
    private IComponentRecordService componentRecordService;

    @Autowired
    ReleaseNodeServiceImpl iReleaseNodeService;

    @Autowired
    private ITagRecordDao tagRecordDao;

    @Autowired
    private IMailService mailService;

    @Autowired
    private RestTransport restTransport;

    @Autowired
    private IMpassReleaseIssueDao iMpassReleaseIssueDao;

    @Autowired
    private IComponentRecordDao componentRecordDao;

    @Value("${gitlab.manager.token}")
    private String token;

    @Value("${history.mpasscomponent.url}")
    private String history_mpasscomponent_url;

    @Autowired
    private IGitlabSerice gitlabService;

    /**
     * ??????npm????????????????????????
     *
     * @param npmName
     * @return
     */
    @Override
    public String getLasterVersion(String npmName, String npmGroup) {
        //??????npm???????????????????????????npm view npm_name version
        String version = ShellUtils.cellShell(npmName, npmGroup, false);
        //?????????1.3.0-alpha??????????????????????????????
        if (StringUtils.isNotBlank(version) && version.contains("-")) {
            version = version.split("-")[0];
        }
        return version;
    }


    /**
     * ??????npm????????????????????????
     *
     * @param npmName
     * @return
     */
    @Override
    public List<String> getVersion(String npmName, String npmGroup) {
        String result = ShellUtils.cellShell(npmName, npmGroup, true);
        List<String> versionList = new ArrayList<>();
        if (StringUtils.isNotBlank(result) && result.length() > 1) {
            String[] versionArray = result.substring(1, result.length() - 1).split(",");
            if (!CommonUtils.isNullOrEmpty(versionArray)) {
                for (String version : versionArray) {
                    version = version.replaceAll("[\\[|\\]|\\']", "").trim();
                    versionList.add(version);
                }
            }
        }
        return versionList;
    }

    /**
     * ????????????mpass????????????
     *
     * @param mpassReleaseIssue
     * @return
     * @throws Exception
     */
    @Override
    public List query(MpassReleaseIssue mpassReleaseIssue) throws Exception {
        return mpassReleaseIssueDao.query(mpassReleaseIssue);
    }

    /**
     * ??????????????????????????????
     *
     * @param id
     * @return
     * @throws Exception
     */
    @Override
    public MpassReleaseIssue queryMpassReleaseIssueDetail(String id) throws Exception {
        return mpassReleaseIssueDao.queryById(id);
    }


    /**
     * ????????????????????????
     * ???????????????
     * ???????????????????????????????????????release?????????release??????????????????
     * ?????????release????????????????????????????????????????????????
     *
     * @param mpassReleaseIssue
     * @return
     * @throws Exception
     */
    @Override
    public MpassReleaseIssue save(MpassReleaseIssue mpassReleaseIssue) throws Exception {
        MpassComponent component = mpassComponentService.queryById(mpassReleaseIssue.getComponent_id());
        if (component == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????"});
        }
        //?????????????????? ????????????????????????
        if (!roleService.isComponentManager(component.getManager())) {
            throw new FdevException(ErrorConstants.ROLE_ERROR, new String[]{"?????????????????????????????????????????????"});
        }
        List list = gitlabSerice.queryBranches(component.getGitlab_id());
        //????????????????????????
        if (list.contains(mpassReleaseIssue.getFeature_branch())) {
            throw new FdevException(ErrorConstants.BRANCH_EXISTS, new String[]{"??????: " + mpassReleaseIssue.getFeature_branch() + "?????????  "});
        }
        //??????npm??????????????????
        List<String> versions = this.getVersion(component.getNpm_name(), component.getNpm_group());
        if (!CommonUtils.isNullOrEmpty(versions) && versions.contains(mpassReleaseIssue.getPredict_version())) {
            throw new FdevException(ErrorConstants.VERSION_EXISTS, new String[]{"??????: " + mpassReleaseIssue.getPredict_version() + "Npm???????????????  "});
        }
        //??????????????????????????????????????????
        MpassReleaseIssue releaseIssue = new MpassReleaseIssue();
        releaseIssue.setComponent_id(mpassReleaseIssue.getComponent_id());
        List<MpassReleaseIssue> releaseIssueList = this.query(releaseIssue);
        if (!CommonUtils.isNullOrEmpty(releaseIssueList)) {
            for (MpassReleaseIssue issue : releaseIssueList) {
                if (mpassReleaseIssue.getPredict_version().equals(issue.getPredict_version())
                        || mpassReleaseIssue.getFeature_branch().equals(issue.getFeature_branch())) {
                    throw new FdevException(ErrorConstants.RELEASE_WINDOW_EXISTS, new String[]{"??????: " + issue.getFeature_branch() + "?????????  "});
                }
            }
        }
        // ??????????????????
        mpassReleaseIssue.setCreate_date(Util.simdateFormat(new Date()));
        //????????????
        gitlabSerice.createBranch(component.getGitlab_id(), mpassReleaseIssue.getFeature_branch(), Dict.MASTER, token);
        //??????????????????
        gitlabSerice.setProtectedBranches(component.getGitlab_id(), mpassReleaseIssue.getFeature_branch());
        //?????????????????????Maintainer??????
        this.gitlabUserService.addMembers(commonInfoService.addMembersForApp(mpassReleaseIssue.getManager(), component.getGitlab_id(), Dict.MAINTAINER));
        //??????
        MpassReleaseIssue result = mpassReleaseIssueDao.save(mpassReleaseIssue);
        return result;
    }

    /**
     * ??????Release????????????
     *
     * @param mpassReleaseIssue
     * @return
     * @throws Exception
     */
    @Override
    public MpassReleaseIssue update(MpassReleaseIssue mpassReleaseIssue) throws Exception {
        MpassReleaseIssue issue = mpassReleaseIssueDao.queryById(mpassReleaseIssue.getId());
        MpassComponent component = mpassComponentService.queryById(issue.getComponent_id());
        //?????????????????? ????????????????????????
        if (!roleService.isComponentManager(component.getManager())) {
            throw new FdevException(ErrorConstants.ROLE_ERROR, new String[]{"?????????????????????????????????????????????"});
        }
        return mpassReleaseIssueDao.update(mpassReleaseIssue);
    }

    @Override
    public void delete(MpassReleaseIssue mpassReleaseIssue) throws Exception {
        mpassReleaseIssueDao.delete(mpassReleaseIssue);
    }

    /**
     * ???????????????????????????????????????????????????
     * ????????????????????????release?????????fix??????????????????release?????????????????????release20200609???release20200609_fix???
     * ????????????????????????????????????????????????????????????????????????????????????1.2.3????????????????????????????????????1.3.0???fix???????????????????????????1.2.4???
     *
     * @param component_id
     * @param issue_type
     * @return
     * @throws Exception
     */
    @Override
    public Map defaultBranchAndVersion(String component_id, String issue_type) throws Exception {
        Map result = new HashMap();
        String featureBranch;
        String predict_version = "";
        MpassComponent component = mpassComponentService.queryById(component_id);
        if (component == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????"});
        }
        String version = this.getLasterVersion(component.getNpm_name(), component.getNpm_group());
        String[] versionArray = version.split("\\.");
        //??????????????????
        if (MpassComEnum.IssueTypeEnum.OPTIMIZE.getValue().equals(issue_type)) {
            featureBranch = Constants.RELEASE_LOWCASE + Util.getDateStamp(new Date());
            if (versionArray.length == 3) {
                versionArray[1] = String.valueOf(Integer.parseInt(versionArray[1]) + 1);
                versionArray[2] = "0";
                predict_version = String.join(".", versionArray);
            }
        }
        //bug????????????
        else if (MpassComEnum.IssueTypeEnum.BUGFIX.getValue().equals(issue_type)) {
            featureBranch = Constants.RELEASE_LOWCASE + Util.getDateStamp(new Date()) + "_fix";
            if (versionArray.length == 3) {
                versionArray[2] = String.valueOf(Integer.parseInt(versionArray[2]) + 1);
                predict_version = String.join(".", versionArray);
            }
        } else {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{issue_type, "???????????????????????????"});
        }
        result.put(Dict.FEATURE_BRANCH, featureBranch);
        result.put(Dict.PREDICT_VERSION, predict_version);
        return result;
    }

    /**
     * ??????????????????????????????
     *
     * @param mpassDevIssue
     * @return
     * @throws Exception
     */
    @Override
    public List query(MpassDevIssue mpassDevIssue) throws Exception {
        return mpassDevIssueDao.query(mpassDevIssue);
    }

    /**
     * ??????????????????????????????
     *
     * @param id
     * @return
     */
    @Override
    public MpassDevIssue queryMpassDevIssueDetail(String id) throws Exception {
        return mpassDevIssueDao.queryById(id);
    }


    /**
     * ??????mpass????????????????????????
     *
     * @param param
     * @return
     */
    @Override
    public List<ComponentRecord> queryMpassIssueRecord(Map param) throws Exception {
        String id = (String) param.get(Dict.ID);//??????????????????????????????id
        String type = (String) param.get(Dict.TYPE);//????????????????????????????????????
        String stage = (String) param.get(Dict.STAGE);//????????????????????????
        List<ComponentRecord> result = null;
        if (Constants.MPASS_RELEASE.equals(type)) {
            //?????????????????????????????????release????????????????????????release????????????????????????????????????
            MpassReleaseIssue mpassReleaseIssue = mpassReleaseIssueDao.queryById(id);
            if (null != mpassReleaseIssue) {
                //??????Release????????????
                result = componentRecordService.queryByComponentIdAndBranch(mpassReleaseIssue.getComponent_id(), "", mpassReleaseIssue.getFeature_branch(), "");
                //??????????????????????????????
                List<MpassDevIssue> devIssueList = mpassDevIssueDao.queryByIssueId(mpassReleaseIssue.getId());
                List<String> branchList = new ArrayList<>();
                if (!CommonUtils.isNullOrEmpty(devIssueList)) {
                    for (MpassDevIssue mpassDevIssue : devIssueList) {
                        branchList.add(mpassDevIssue.getFeature_branch());
                    }
                }
                List<ComponentRecord> devRecordList = componentRecordService.queryByComponentIdAndBranchs(mpassReleaseIssue.getComponent_id(), mpassReleaseIssue.getPredict_version(), branchList);
                if (null == result)
                    result = devRecordList;
                else
                    result.addAll(devRecordList);
            }
        } else if (Constants.MPASS_DEV.equals(type)) {
            //??????????????????????????????????????????????????????????????????????????????
            MpassDevIssue mpassDevIssue = mpassDevIssueDao.queryById(id);
            MpassReleaseIssue mpassReleaseIssue = mpassReleaseIssueDao.queryById(mpassDevIssue.getIssue_id());
            if (null != mpassDevIssue) {
                result = componentRecordService.queryByComponentIdAndBranch(mpassDevIssue.getComponent_id(), mpassReleaseIssue.getPredict_version(), mpassDevIssue.getFeature_branch(), stage);
            }
        }
        return result;
    }

    /**
     * ??????mpass???????????????????????????
     * <p>
     * ?????????????????????????????????????????????????????????????????????????????????relese???????????????????????????????????????fdev??????fdev??????
     * ??????????????????????????????????????????????????????????????????fdev?????????fdev??????
     *
     * @param param
     */
    @Override
    public void destroyIssue(Map param) throws Exception {
        String id = (String) param.get(Dict.ID);//??????????????????????????????id
        String type = (String) param.get(Dict.TYPE);//????????????????????????????????????
        if (Constants.MPASS_RELEASE.equals(type)) {
            //?????????????????????????????????release?????????????????????
            MpassReleaseIssue mpassReleaseIssue = mpassReleaseIssueDao.queryById(id);
            //??????Mpass??????
            MpassComponent mpassComponent = mpassComponentService.queryById(mpassReleaseIssue.getComponent_id());
            //?????????????????? ????????????????????????
            if (!roleService.isComponentManager(mpassComponent.getManager())) {
                throw new FdevException(ErrorConstants.ROLE_ERROR, new String[]{"?????????????????????????????????????????????"});
            }
            if (null != mpassReleaseIssue) {
                //????????????????????????????????????????????????release
                List<MpassDevIssue> devIssueList = mpassDevIssueDao.queryByIssueId(mpassReleaseIssue.getId());
                if (!CommonUtils.isNullOrEmpty(devIssueList)) {
                    throw new FdevException(ErrorConstants.RELEASE_WINDOW_DESTORY_FORBID, new String[]{"??????????????????????????????"});
                }
                mpassReleaseIssueDao.delete(mpassReleaseIssue);
            }
        } else if (Constants.MPASS_DEV.equals(type)) {
            //??????????????????????????????????????????????????????????????????????????????
            MpassDevIssue mpassDevIssue = mpassDevIssueDao.queryById(id);
            //?????????????????? ????????????????????????
            MpassReleaseIssue mpassReleaseIssue = mpassReleaseIssueDao.queryById(mpassDevIssue.getIssue_id());
            if (!roleService.isComponentManager(mpassReleaseIssue.getManager())) {
                throw new FdevException(ErrorConstants.ROLE_ERROR, new String[]{"?????????????????????????????????????????????"});
            }
            //???????????????????????????????????????????????????
            if (null != mpassDevIssue) {
                List<ComponentRecord> recordList = componentRecordService.queryByComponentIdAndBranch(mpassDevIssue.getComponent_id(), "", mpassReleaseIssue.getFeature_branch(), "");
                if (!CommonUtils.isNullOrEmpty(recordList)) {
                    throw new FdevException(ErrorConstants.DEV_DESTORY_FORBID, new String[]{"??????????????????????????????"});
                }
                List<ComponentRecord> deleteList = componentRecordService.queryByComponentIdAndBranch(mpassDevIssue.getComponent_id(), mpassReleaseIssue.getPredict_version(), mpassDevIssue.getFeature_branch(), "");
                //?????????????????????alpha???beta???rc???tag
                if (!CommonUtils.isNullOrEmpty(deleteList)) {
                    for (ComponentRecord record : deleteList) {
                        componentRecordService.delete(record);
                    }
                }
                mpassDevIssueDao.delete(mpassDevIssue);
            }
        }
    }

    /**
     * ??????????????????????????????????????????????????????
     *
     * @param component_id
     * @return
     */
    @Override
    public List<MpassReleaseIssue> queryTransgerReleaseIssue(String component_id, String feature_branch) {
        List<MpassReleaseIssue> list = mpassReleaseIssueDao.queryByComponentId(component_id);
        if (!CommonUtils.isNullOrEmpty(list)) {
            Iterator<MpassReleaseIssue> iterator = list.iterator();
            while (iterator.hasNext()) {
                MpassReleaseIssue mpassReleaseIssue = iterator.next();
                if (!CommonUtils.isNullOrEmpty(mpassReleaseIssue.getFeature_branch()) && mpassReleaseIssue.getFeature_branch().equals(feature_branch)) {
                    iterator.remove();
                    continue;
                }
                List<ComponentRecord> recordList = componentRecordService.queryByComponentIdAndBranch(component_id, "", mpassReleaseIssue.getFeature_branch(), "");
                if (!CommonUtils.isNullOrEmpty(recordList)) {
                    iterator.remove();
                }
            }
        }
        return list;
    }

    /**
     * ????????????????????????????????????????????????????????????????????????
     *
     * @param param
     * @throws Exception
     */
    @Override
    public void devIssueTransger(Map<String, String> param) throws Exception {
        String id = param.get(Dict.ID);
        String issue_id = param.get(Dict.ISSUE_ID);
        MpassDevIssue mpassDevIssue = mpassDevIssueDao.queryById(id);
        MpassReleaseIssue mpassReleaseIssue = mpassReleaseIssueDao.queryById(mpassDevIssue.getIssue_id());
        List<ComponentRecord> recordList = componentRecordService.queryByComponentIdAndBranch(mpassDevIssue.getComponent_id(), "", mpassReleaseIssue.getFeature_branch(), "");
        if (!CommonUtils.isNullOrEmpty(recordList)) {
            throw new FdevException(ErrorConstants.DEV_TEANSFER_FORBID, new String[]{"??????????????????????????????"});
        }
        //?????????????????????id
        mpassDevIssue.setIssue_id(issue_id);
        // ????????????stage
        mpassDevIssue.setStage(MpassComEnum.DevIssueStageEnum.CREATE.getValue());
        mpassDevIssueDao.update(mpassDevIssue);
    }


    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????
     * ??????????????????????????????????????????????????????
     *
     * @param mpassDevIssue
     * @return
     * @throws Exception
     */
    @Override
    public MpassDevIssue save(MpassDevIssue mpassDevIssue) throws Exception {
        MpassReleaseIssue mpassReleaseIssue = mpassReleaseIssueDao.queryById(mpassDevIssue.getIssue_id());
        if (mpassReleaseIssue == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????????????????"});
        }
        MpassComponent component = mpassComponentService.queryById(mpassReleaseIssue.getComponent_id());
        if (component == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????"});
        }
        //??????????????????????????????
        if (component.getType().equals("0")) {
            if (CommonUtils.isNullOrEmpty(mpassDevIssue.getSit_date())) {
                throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"??????SIT??????"});
            }
            if (CommonUtils.isNullOrEmpty(mpassDevIssue.getUat_date())) {
                throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"??????UAT??????"});
            }
        }


        //?????????????????? ????????????????????????
        if (!roleService.isComponentManager(mpassReleaseIssue.getManager())) {
            throw new FdevException(ErrorConstants.ROLE_ERROR, new String[]{"?????????????????????????????????????????????"});
        }
        List list = gitlabSerice.queryBranches(component.getGitlab_id());
        //????????????????????????
        if (list.contains(mpassDevIssue.getFeature_branch())) {
            throw new FdevException(ErrorConstants.BRANCH_EXISTS, new String[]{"??????: " + mpassDevIssue.getFeature_branch() + "?????????  "});
        }
        //??????????????????????????????
        List<MpassDevIssue> devIssueList = mpassDevIssueDao.queryByIssueId(mpassReleaseIssue.getId());
        if (!CommonUtils.isNullOrEmpty(devIssueList)) {
            for (MpassDevIssue devIssue : devIssueList) {
                if (mpassDevIssue.getFeature_branch().equals(devIssue.getFeature_branch())) {
                    throw new FdevException(ErrorConstants.DEV_ISSUE_EXIST, new String[]{devIssue.getFeature_branch()});
                }
            }
        }
        // ??????????????????
        mpassDevIssue.setCreate_date(Util.simdateFormat(new Date()));
        //??????????????????
        mpassDevIssue.setComponent_id(component.getId());
        // ????????????stage
        mpassDevIssue.setStage(MpassComEnum.DevIssueStageEnum.CREATE.getValue());
        //????????????
        gitlabSerice.createBranch(component.getGitlab_id(), mpassDevIssue.getFeature_branch(), Dict.MASTER, token);
        //?????????????????????Developer??????
        this.gitlabUserService.addMembers(commonInfoService.addMembersForApp(mpassDevIssue.getAssignee(), component.getGitlab_id(), Dict.DEVELOPER));
        //??????
        MpassDevIssue result = mpassDevIssueDao.save(mpassDevIssue);
        //??????????????????
        sendDevNotify(component.getName_cn(), mpassReleaseIssue, mpassDevIssue);
        return result;
    }

    public void sendDevNotify(String nameCn, MpassReleaseIssue mpassReleaseIssue, MpassDevIssue mpassDevIssue) {
        try {
            HashMap hashMap = new HashMap();
            hashMap.put(Dict.NAME_CN, nameCn);
            hashMap.put(Dict.RELEASE_WINDOW, mpassReleaseIssue.getFeature_branch());
            hashMap.put(Dict.FEATURE_BRANCH, mpassDevIssue.getFeature_branch());
            hashMap.put(Dict.DESC, mpassDevIssue.getDesc());
            hashMap.put(Dict.HISTORY_MPASSCOMPONENT_URL, history_mpasscomponent_url + mpassReleaseIssue.getComponent_id());
            String mailContent = mailService.mpassMailDevNotifyContent(hashMap);
            Set<String> users = new HashSet<>();
            users.add(mpassDevIssue.getAssignee());
            //3.?????????????????????
            Map<String, Object> mapId = new HashMap<>();
            mapId.put(Dict.IDS, users);
            mapId.put(Dict.REST_CODE, Dict.QUERYBYUSERCOREDATA);
            Map<String, Map> userMap = (Map<String, Map>) restTransport.submit(mapId);
            List<String> email_receivers = new ArrayList<>();
            Set<Map.Entry<String, Map>> entries = userMap.entrySet();
            if (!CommonUtils.isNullOrEmpty(entries)) {
                for (Map.Entry<String, Map> entry : entries) {
                    try {
                        String email = (String) entry.getValue().get(Dict.EMAIL);
                        email_receivers.add(email);
                    } catch (Exception e) {
                        logger.error("??????????????????????????????" + entry.getKey());
                    }
                }
            }
            //4.????????????
            HashMap<String, String> sendMap = new HashMap();
            sendMap.put(Dict.EMAIL_CONTENT, mailContent);
            String topic = "????????????????????????";
            mailService.sendEmail(topic, Dict.FCOMPONENT_MPASSDEV_NOTIFY, sendMap, email_receivers.toArray(new String[email_receivers.size()]));
        } catch (Exception e) {
            logger.error("Mpass????????????????????????????????????,{}", e.getMessage());
        }
    }


    /**
     * @param mpassDevIssue
     * @return ??????????????????????????????????????????
     * @throws Exception
     */
    @Override
    public MpassDevIssue update(MpassDevIssue mpassDevIssue) throws Exception {
        MpassDevIssue devIssue = mpassDevIssueDao.queryById(mpassDevIssue.getId());
        MpassReleaseIssue mpassReleaseIssue = mpassReleaseIssueDao.queryById(devIssue.getIssue_id());
        //?????????????????? ????????????????????????
        if (!roleService.isComponentManager(mpassReleaseIssue.getManager())) {
            throw new FdevException(ErrorConstants.ROLE_ERROR, new String[]{"?????????????????????????????????????????????"});
        }
        return mpassDevIssueDao.update(mpassDevIssue);
    }

    /**
     * mpass??????pipeline???????????????????????????????????????????????????
     *
     * @param hashMap
     */
    @Override
    public void createComponentRecord(Map hashMap) throws Exception {
        String gitlabUrl = (String) hashMap.get(Dict.GITLAB_URL);//gitlab????????????
        String sourceBranch = (String) hashMap.get(Dict.SOURCE_BRANCH);//??????release?????????????????????
        String targetBranch = (String) hashMap.get(Dict.TARGET_BRANCH);//??????????????????
        String version = (String) hashMap.get(Dict.VERSION);//??????????????????????????????
        String releaseLog = (String) hashMap.get(Dict.RELEASE_LOG);//????????????
        MpassComponent mpassComponent = mpassComponentService.queryByGitlabUrl(gitlabUrl);
        if (mpassComponent == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????" + gitlabUrl + "?????????"});
        }
        MpassReleaseIssue mpassReleaseIssue = mpassReleaseIssueDao.queryByComponentAndBranch(mpassComponent.getId(), targetBranch);
        if (mpassReleaseIssue == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"??????????????????" + targetBranch + "?????????"});
        }
        //????????????id?????????????????????????????????
        MpassDevIssue mpassDevIssue = mpassDevIssueDao.queryByComIdAndIssueIdBranch(mpassComponent.getId(), mpassReleaseIssue.getId(), sourceBranch);
        if (mpassDevIssue == null) {
            throw new FdevException(ErrorConstants.BRANCH_DONOT_EXIST, new String[]{"?????????????????????" + sourceBranch + "?????????"});
        }
        ComponentRecord componentRecord = new ComponentRecord();
        componentRecord.setBranch(sourceBranch);
        componentRecord.setComponent_id(mpassComponent.getId());
        componentRecord.setVersion(version);//??????
        componentRecord.setRelease_log(releaseLog);//????????????
        componentRecord.setDate(Util.simdateFormat(new Date()));//????????????
        componentRecord.setIssue_id(mpassDevIssue.getIssue_id());//??????Release??????????????????id
        if (StringUtils.isNotBlank(version) && version.contains(Constants.ALPHA)) {
            componentRecord.setType(Constants.RECORD_TEST_TYPE);//?????????alpha???????????????????????????
        }
        componentRecord.setUpdate_user(mpassDevIssue.getAssignee());//??????????????????
        componentRecordService.save(componentRecord);

    }

    /**
     * ????????????????????????????????????
     *
     * @param hashMap
     * @return
     * @throws Exception
     */
    @Override
    public String getNextAlphaVersion(Map hashMap) throws Exception {
        String gitlabUrl = (String) hashMap.get(Dict.GITLAB_URL);//gitlab????????????
        String targetBranch = (String) hashMap.get(Dict.TARGET_BRANCH);//????????????release????????????
        String type = (String) hashMap.get(Dict.TYPE);//????????????alpha??????beta???rc??????
        MpassComponent mpassComponent = mpassComponentService.queryByGitlabUrl(gitlabUrl);
        if (mpassComponent == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????" + gitlabUrl + "?????????"});
        }
        MpassReleaseIssue mpassReleaseIssue = mpassReleaseIssueDao.queryByComponentAndBranch(mpassComponent.getId(), targetBranch);
        if (mpassReleaseIssue == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????????????????"});
        }
        String predictVersion = mpassReleaseIssue.getPredict_version();
        return getNextVersion(mpassComponent.getId(), type, predictVersion, false);
    }


    @Override
    public String getNextAlphaorBetaVersion(Map map) throws Exception {

        String gitlabUrl = (String) map.get(Dict.GITLAB_URL);//gitlab????????????
        String type = (String) map.get(Dict.TYPE);//????????????alpha??????beta??????
        String version = (String) map.get(Dict.VERSION);
        String componentType = (String) map.get("component_type");

        String componentId = "";
        if ("mpass".equals(componentType)) {
            MpassComponent mpassComponent = mpassComponentService.queryByGitlabUrl(gitlabUrl);
            if (mpassComponent == null) {
                throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????" + gitlabUrl + "?????????"});
            }
            componentId = mpassComponent.getId();
        } else if ("back".equals(componentType)) {
            ComponentInfo component = componentInfoService.queryByWebUrl(gitlabUrl);
            if (component == null) {
                throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????" + gitlabUrl + "?????????"});
            }
            componentId = component.getId();
        }
        return getNextAlphaorBetaVersion(componentId, type, version);
    }


    private String getNextAlphaorBetaVersion(String componentId, String alphaOrBeta, String predictVersion) {
        List<ComponentRecord> componentRecord = componentRecordService.getAlphaOrRcVersion(componentId, alphaOrBeta, predictVersion, true);
        if (!CommonUtils.isNullOrEmpty(componentRecord)) {
            String version = componentRecord.get(0).getVersion();
            try {
                String[] versionArray = version.split("\\.");
                versionArray[versionArray.length - 1] = String.valueOf(Integer.parseInt(versionArray[versionArray.length - 1]) + 1);
                return String.join(".", versionArray);
            } catch (Exception e) {
                throw new FdevException(ErrorConstants.PATTERN_ERROR, new String[]{version, "????????????????????????"});
            }
        } else {
            return predictVersion + "-" + alphaOrBeta + ".0";
        }
    }


    /**
     * Mpass??????????????????
     *
     * @param param
     * @throws Exception
     */
    @Override
    public void devPackage(Map param) throws Exception {
        String id = (String) param.get(Dict.ID);
        String stage = (String) param.get(Dict.STAGE);
        String updateUser = (String) param.get(Dict.UPDATE_USER);//????????????
        String releaseLog = (String) param.get(Dict.RELEASE_LOG);//????????????
        MpassDevIssue mpassDevIssue = mpassDevIssueDao.queryById(id);
        if (null == mpassDevIssue) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????????????????"});
        }
        MpassReleaseIssue mpassReleaseIssue = mpassReleaseIssueDao.queryById(mpassDevIssue.getIssue_id());
        if (null == mpassReleaseIssue) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????????????????"});
        }
        MpassComponent mpassComponent = mpassComponentService.queryById(mpassDevIssue.getComponent_id());
        //????????????????????????????????????
        if (MpassComEnum.DevIssueStageEnum.ALPHA.getValue().equals(stage)) {
            gitlabSerice.createMergeRequest(mpassComponent.getGitlab_id(), mpassDevIssue.getFeature_branch(), mpassReleaseIssue.getFeature_branch(), mpassDevIssue.getFeature_branch(), "??????????????????????????????release??????", token);
        }
        //beta????????????tag
        else if (MpassComEnum.DevIssueStageEnum.BETA.getValue().equals(stage)) {
            String tag = getNextVersion(mpassComponent.getId(), Constants.BETA, mpassReleaseIssue.getPredict_version(), true);
            String vTag = Constants.VERSION_TAG + tag;
            //??????Tag???????????????
            boolean existTag = gitlabSerice.checkBranchOrTag(token, mpassComponent.getGitlab_id(), Constants.TAG, vTag);
            if (existTag) {
                throw new FdevException(ErrorConstants.TAG_EXISTS, new String[]{"Tag: " + vTag + "?????????  "});
            }
            gitlabSerice.createTags(mpassComponent.getGitlab_id(), vTag, mpassReleaseIssue.getFeature_branch(), token);
            ComponentRecord componentRecord = new ComponentRecord();
            componentRecord.setComponent_id(mpassDevIssue.getComponent_id());
            componentRecord.setVersion(tag);
            componentRecord.setUpdate_user(updateUser);
            componentRecord.setBranch(mpassDevIssue.getFeature_branch());
            componentRecord.setRelease_log(releaseLog);
            componentRecord.setPackagetype(Constants.PREPACKAGE);
            componentRecord.setDate(TimeUtils.getFormat(TimeUtils.FORMAT_DATE_TIME));
            componentRecord.setType(Constants.RECORD_TEST_TYPE);//beta?????????????????????????????????
            componentRecord.setIssue_id(mpassDevIssue.getIssue_id());//??????Release??????????????????id
            componentRecordService.save(componentRecord);
        }
        //rc????????????rc???tag
        else if (MpassComEnum.DevIssueStageEnum.RC.getValue().equals(stage)) {
            String tag = getNextVersion(mpassComponent.getId(), Constants.RC_LOWCASE, mpassReleaseIssue.getPredict_version(), true);
            String vTag = Constants.VERSION_TAG + tag;
            //??????Tag???????????????
            boolean existTag = gitlabSerice.checkBranchOrTag(token, mpassComponent.getGitlab_id(), Constants.TAG, vTag);
            if (existTag) {
                throw new FdevException(ErrorConstants.TAG_EXISTS, new String[]{"Tag: " + vTag + "?????????  "});
            }
            gitlabSerice.createTags(mpassComponent.getGitlab_id(), vTag, mpassReleaseIssue.getFeature_branch(), token);
            ComponentRecord componentRecord = new ComponentRecord();
            componentRecord.setComponent_id(mpassDevIssue.getComponent_id());
            componentRecord.setVersion(tag);
            componentRecord.setUpdate_user(updateUser);
            componentRecord.setBranch(mpassDevIssue.getFeature_branch());
            componentRecord.setRelease_log(releaseLog);
            componentRecord.setPackagetype(Constants.PREPACKAGE);
            componentRecord.setDate(TimeUtils.getFormat(TimeUtils.FORMAT_DATE_TIME));
            componentRecord.setType(Constants.RECORD_TEST_TYPE);//rc?????????????????????????????????
            componentRecord.setIssue_id(mpassDevIssue.getIssue_id());//??????Release??????????????????id
            componentRecordService.save(componentRecord);
        } else {
            throw new FdevException(ErrorConstants.STAGE_ERROR, new String[]{"???????????????????????????"});
        }
    }

    @Override
    public void changeStage(Map<String, String> map) throws Exception {
        String id = map.get(Dict.ID);//??????????????????id
        MpassDevIssue mpassDevIssue = mpassDevIssueDao.queryById(id);
        if (null == mpassDevIssue) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????????????????"});
        }
        String stage = map.get(Dict.STAGE);//????????????
        if (!Arrays.asList("0", "1", "2").contains(stage)) {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{stage, "??????stage????????????"});
        }
        Integer devStage = Integer.valueOf(mpassDevIssue.getStage());
        devStage++;
        mpassDevIssue.setStage(String.valueOf(devStage));
        mpassDevIssueDao.update(mpassDevIssue);
    }

    /**
     * ???????????????????????????????????????release?????????release???????????????????????????????????????alpha????????????????????????+1????????????????????????
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????merge??????pipeline??????????????????alpha??????
     *
     * @param param
     * @return
     */
    @Override
    public boolean canPackage(Map param) throws Exception {
        String id = (String) param.get(Dict.ID);
        MpassDevIssue mpassDevIssue = mpassDevIssueDao.queryById(id);
        MpassReleaseIssue mpassReleaseIssue = mpassReleaseIssueDao.queryById(mpassDevIssue.getIssue_id());
        if (null == mpassReleaseIssue) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????????????????"});
        }
        MpassComponent mpassComponent = mpassComponentService.queryById(mpassDevIssue.getComponent_id());
        //???????????????open?????????merge??????
        Object merge = gitlabSerice.queryMergeList(mpassComponent.getGitlab_id(), mpassReleaseIssue.getFeature_branch(), Constant.MERGE_STATE_OPENED);
        JSONArray mergelist = JSONObject.parseArray((String) merge);
        if (null != mergelist && mergelist.size() > 0) {
            throw new FdevException(ErrorConstants.MERGE_EXISTS, new String[]{mpassReleaseIssue.getFeature_branch()});
        }
        //???????????????running?????????pipeline??????
        Object pipeRuning = gitlabSerice.queryPipelineList(mpassComponent.getGitlab_id(), mpassReleaseIssue.getFeature_branch(), Constant.PIPELINE_STATUS_RUNNING);
        JSONArray runningList = JSONObject.parseArray((String) pipeRuning);
        if (null != runningList && runningList.size() > 0) {
            throw new FdevException(ErrorConstants.PIPELINE_EXISTS, new String[]{mpassReleaseIssue.getFeature_branch()});
        }
        //???????????????pending?????????pipeline??????
        Object pipePending = gitlabSerice.queryPipelineList(mpassComponent.getGitlab_id(), mpassReleaseIssue.getFeature_branch(), Constant.PIPELINE_STATUS_PENDING);
        JSONArray pendinglist = JSONObject.parseArray((String) pipePending);
        if (null != pendinglist && pendinglist.size() > 0) {
            throw new FdevException(ErrorConstants.PIPELINE_EXISTS, new String[]{mpassReleaseIssue.getFeature_branch()});
        }
        return true;
    }

    @Override
    public String getRootDir(Map param) {
        String gitlabUrl = (String) param.get(Dict.GITLAB_URL);//gitlab????????????
        MpassComponent mpassComponent = mpassComponentService.queryByGitlabUrl(gitlabUrl);
        if (mpassComponent == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????" + gitlabUrl + "?????????"});
        }
        return mpassComponent.getRoot_dir();
    }

    @Override
    public void mergedCallBack(Integer iid, Integer project_id) throws Exception {
        String gitlabId = String.valueOf(project_id);
        String merge_request_id = String.valueOf(iid);
        //???tag_record??????????????????merge?????????????????????
        TagRecord tagRecord = tagRecordDao.findByMidAndGid(gitlabId, merge_request_id);
        if (tagRecord != null) {
            ComponentRecord componentRecord = new ComponentRecord();
            componentRecord.setComponent_id(tagRecord.getComponent_id());
            componentRecord.setVersion(tagRecord.getTarget_version());
            componentRecord.setUpdate_user(tagRecord.getUpdate_user());
            componentRecord.setBranch(tagRecord.getBranch());
            componentRecord.setRelease_log(tagRecord.getRelease_log());
            componentRecord.setPackagetype(Constants.PREPACKAGE);
            componentRecord.setDate(TimeUtils.getFormat(TimeUtils.FORMAT_DATE_TIME));
            componentRecord.setType(Constants.RECORD_OFFICIAL_TYPE);//?????????????????????
            componentRecord.setIssue_id(tagRecord.getIssue_id());//??????Release??????????????????id
            componentRecordService.upsert(componentRecord);
            gitlabSerice.createTags(gitlabId, Constants.VERSION_TAG + tagRecord.getTarget_version(), Dict.MASTER, token);
        } else {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????release??????master?????????????????????"});
        }
    }


    /**
     * webhook???????????????????????????????????????????????????
     *
     * @param gitlab_url
     * @param ref
     */
    @Override
    public void pipiCallBack(String gitlab_url, String gitlabId, String ref) throws Exception {
        MpassComponent mpassComponent = mpassComponentService.queryByGitlabId(gitlabId);
        ComponentInfo componentInfo = null;
        String id;
        boolean flag = true;
        if (mpassComponent == null) {
            componentInfo = componentInfoService.queryByWebUrl(gitlab_url);
            flag = false;
            if (componentInfo == null) {
                throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????" + gitlab_url + "?????????"});
            }
        }
        if (ref.startsWith(Constants.VERSION_TAG)) {
            ref = ref.substring(1);
        }
        if (flag) {
            id = mpassComponent.getId();
        } else {
            id = componentInfo.getId();
        }
        ComponentRecord componentRecord = componentRecordService.queryByComponentIdAndVersion(id, ref);
        if (null != componentRecord) {
            componentRecord.setPackagetype(Constants.PIPEPACKAGE);
            componentRecordService.updateForMpass(componentRecord);
        }
    }

    /**
     * release????????????
     *
     * @param param
     */
    @Override
    public void releasePackage(Map param) throws Exception {
        String id = (String) param.get(Dict.ID);
        String updateUser = (String) param.get(Dict.UPDATE_USER);
        String releaseLog = (String) param.get(Dict.RELEASE_LOG);
        String tag_name = (String) param.get(Dict.TAG_NAME);
        MpassReleaseIssue mpassReleaseIssue = mpassReleaseIssueDao.queryById(id);
        if (mpassReleaseIssue == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"???????????????????????????"});
        }
        MpassComponent mpassComponent = mpassComponentService.queryById(mpassReleaseIssue.getComponent_id());
        //release??????????????????????????????????????????????????????
        if (StringUtils.isBlank(updateUser))
            updateUser = CommonUtils.getSessionUser().getId();
        if (!CommonUtils.checkUser(updateUser, mpassComponent.getManager()) && !CommonUtils.checkUser(updateUser, mpassReleaseIssue.getManager())) {
            throw new FdevException(ErrorConstants.ROLE_ERROR, new String[]{"???????????????????????????????????????????????????????????????"});
        }
        //release??????????????????beta???
        List<ComponentRecord> recordList = componentRecordService.getAlphaOrRcVersion(mpassReleaseIssue.getComponent_id(), Constants.BETA, mpassReleaseIssue.getPredict_version(), false);
        if (CommonUtils.isNullOrEmpty(recordList)) {
            throw new FdevException(ErrorConstants.RELEASE_PACKAGE_LIMIT);
        }
        //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        String tag = StringUtils.isNotBlank(tag_name) ? tag_name : mpassReleaseIssue.getPredict_version();
        String vTag = Constants.VERSION_TAG + tag;
        //??????Tag???????????????
        boolean existTag = gitlabSerice.checkBranchOrTag(token, mpassComponent.getGitlab_id(), Constants.TAG, vTag);
        if (existTag) {
            throw new FdevException(ErrorConstants.TAG_EXISTS, new String[]{"Tag: " + vTag + "?????????  "});
        }
        //??????npm??????????????????
        List<String> versions = this.getVersion(mpassComponent.getNpm_name(), mpassComponent.getNpm_group());
        if (!CommonUtils.isNullOrEmpty(versions) && versions.contains(tag)) {
            throw new FdevException(ErrorConstants.VERSION_EXISTS, new String[]{"??????: " + tag + "Npm???????????????  "});
        }
        // ???release???????????????npm --no-git-tag-version version??????????????????changelog
        gitlabSerice.updateReleaseChangeLog(mpassComponent, mpassReleaseIssue.getFeature_branch(), tag);
        //??????release?????????master???????????????
        Object result = gitlabSerice.createMergeRequest(mpassComponent.getGitlab_id(), mpassReleaseIssue.getFeature_branch(), Dict.MASTER, mpassReleaseIssue.getFeature_branch(), "?????????????????????release???????????????master", token);
        net.sf.json.JSONObject jsonObject = net.sf.json.JSONObject.fromObject(result);
        ObjectMapper objectMapper = new ObjectMapper();
        Map merge = objectMapper.readValue(jsonObject.toString(), Map.class);
        String mergeId = String.valueOf(merge.get(Dict.IID));
        TagRecord tagRecord = new TagRecord();
        tagRecord.setGitlab_id(mpassComponent.getGitlab_id());
        tagRecord.setMerge_request_id(mergeId);
        tagRecord.setComponent_id(mpassReleaseIssue.getComponent_id());
        tagRecord.setTarget_version(tag);
        tagRecord.setUpdate_user(updateUser);
        tagRecord.setBranch(mpassReleaseIssue.getFeature_branch());
        tagRecord.setRelease_log(releaseLog);
        tagRecord.setIssue_id(mpassReleaseIssue.getId());
        tagRecordDao.save(tagRecord);
    }


    /**
     * ??????alpha???rc??????????????????
     *
     * @param componentId       ??????id
     * @param alphaOrRc         ??????????????????????????????alpha???beta???rc???release
     * @param predictVersion    ????????????
     * @param containPrepackage ???????????????prepackage???pipepackage
     * @return
     */
    public String getNextVersion(String componentId, String alphaOrRc, String predictVersion, Boolean
            containPrepackage) {
        List<ComponentRecord> componentRecord = componentRecordService.getAlphaOrRcVersion(componentId, alphaOrRc, predictVersion, containPrepackage);
        if (!CommonUtils.isNullOrEmpty(componentRecord)) {
            String version = componentRecord.get(0).getVersion();
            try {
                String[] versionArray = version.split("\\.");
                versionArray[versionArray.length - 1] = String.valueOf(Integer.parseInt(versionArray[versionArray.length - 1]) + 1);
                return String.join(".", versionArray);
            } catch (Exception e) {
                throw new FdevException(ErrorConstants.PATTERN_ERROR, new String[]{version, "????????????????????????"});
            }
        } else {
            return predictVersion + "-" + alphaOrRc + ".0";
        }
    }

    @Override
    public List<HashMap<String, Object>> queryQrmntsData(Map requestParam) {
        String user_id = (String) requestParam.get(Dict.ID);
        List<Map> results = iMpassReleaseIssueDao.queryQrmntsData(user_id);
        List<HashMap<String, Object>> componentResult = new ArrayList<>();
        for (Map doc : results) {
            HashMap<String, Object> iMpassReleaseIssueMap = new HashMap<>();
            MpassReleaseIssue mpassReleaseIssueInfo = (MpassReleaseIssue) (doc.get(Dict.MPASS_RELEASE_ISSUE));
            MpassComponent MpassComponentInfo = (MpassComponent) (doc.get(Dict.MPASS_COMPONENT));
            HashSet<Map<String, String>> manager_id = MpassComponentInfo.getManager();
            switch ((String) doc.get(Dict.STAGE)) {
                case Dict.ZERO:
                    iMpassReleaseIssueMap.put(Dict.STAGE, MpassComEnum.DevIssueStageEnum.CREATE.getName());
                    break;
                case Dict.ONE:
                    iMpassReleaseIssueMap.put(Dict.STAGE, MpassComEnum.DevIssueStageEnum.ALPHA.getName());
                    break;
                case Dict.TWO:
                    iMpassReleaseIssueMap.put(Dict.STAGE, MpassComEnum.DevIssueStageEnum.BETA.getName());
                    break;
                case Dict.THREE:
                    iMpassReleaseIssueMap.put(Dict.STAGE, MpassComEnum.DevIssueStageEnum.RC.getName());
                    break;
                case Constants.FOUR:
                    iMpassReleaseIssueMap.put(Dict.STAGE, Constants.DEV_END);
                    break;
                default:
                    continue;
            }
            iMpassReleaseIssueMap.put(Dict.PREDICT_VERSION, mpassReleaseIssueInfo.getPredict_version());
            iMpassReleaseIssueMap.put(Dict.RECOMMOND_VERSION, doc.get(Dict.TARGET_VERSION));
            iMpassReleaseIssueMap.put(Dict.FEATURE_BRANCH, doc.get(Dict.FEATURE_BRANCH));
            iMpassReleaseIssueMap.put(Dict.ID, MpassComponentInfo.getId());
            iMpassReleaseIssueMap.put(Dict.RQRMNTS_ID, mpassReleaseIssueInfo.getId());
            iMpassReleaseIssueMap.put(Dict.NAME, MpassComponentInfo.getName_en());
            iMpassReleaseIssueMap.put(Dict.RQRMNTS_ADMIN, manager_id);
            iMpassReleaseIssueMap.put(Dict.RQRMNTS_NAME, mpassReleaseIssueInfo.getTitle());
            iMpassReleaseIssueMap.put(Dict.DEVELOP, requestParam);
            iMpassReleaseIssueMap.put(Dict.DUE_DATE, doc.get(Dict.DUE_DATE));
            iMpassReleaseIssueMap.put(Dict.DESC, doc.get(Dict.DESC));
            iMpassReleaseIssueMap.put(Dict.TYPE, Constants.MPASS_COMPONENT_TYPE);
            componentResult.add(iMpassReleaseIssueMap);
        }
        return componentResult;
    }

    @Override
    public List<Map> queryIssueDelay(Map requestParam) throws Exception {
        List<Map> mpassDevIssueInfo = mpassReleaseIssueDao.queryIssueDelay(requestParam);
        List<Map> issueDelayInfo = new ArrayList<>();
        for (Map issueInfo : mpassDevIssueInfo) {
            if (CommonUtils.isNullOrEmpty(issueInfo.get(Dict.MPASS_RELEASE_ISSUE)) || CommonUtils.isNullOrEmpty(issueInfo.get(Dict.MPASS_COMPONENT)))
                continue;
            MpassReleaseIssue mpassReleaseIssue = (MpassReleaseIssue) issueInfo.get(Dict.MPASS_RELEASE_ISSUE);
            MpassComponent mpassComponent = (MpassComponent) issueInfo.get(Dict.MPASS_COMPONENT);
            //??????record??????
            String componentId = (String) issueInfo.get(Dict.COMPONENT_ID);
            String featureBranch = (String) issueInfo.get(Dict.FEATURE_BRANCH);
            String stage = (String) issueInfo.get(Dict.STAGE);//????????????
            String version = mpassReleaseIssue.getPredict_version();
            String predict_version = null;
            if (StringUtils.isNotBlank(stage)) {
                switch (stage) {
                    case Dict.ZERO:
                        break;
                    case Dict.ONE:
                        predict_version = version + Constants._ALPHA + ".*";
                        break;
                    case Dict.TWO:
                        predict_version = version + Constants._BETA + ".*";
                        break;
                    default:
                        continue;
                }
            }
            List list = componentRecordDao.queryRecordByComponentIdAndVersionandFeature(componentId, predict_version, featureBranch);
            String date = null;
            ComponentRecord componentRecord = null;
            for (int i = 0; i < list.size(); i++) {
                componentRecord = (ComponentRecord) list.get(0);
                date = componentRecord.getDate();
            }
            Map delayDays = new LinkedHashMap();
            String now = CommonUtils.dateFormat(new Date(), CommonUtils.DATE_PARSE);
            switch (stage) {
                case Dict.ZERO:
                    delayDays.put(Dict.DELAY_DATE, CommonUtils.dateCompare((String) issueInfo.get(Dict.DUE_DATE), now));
                    delayDays.put(Dict.STAGE, MpassComEnum.DevIssueStageEnum.CREATE.getName());
                    break;

                case Dict.ONE:
                    if (CommonUtils.isNullOrEmpty(componentRecord)) {
                        delayDays.put(Dict.DELAY_DATE, CommonUtils.dateCompare((String) issueInfo.get(Dict.DUE_DATE), now));
                        delayDays.put(Dict.STAGE, MpassComEnum.DevIssueStageEnum.ALPHA.getName());
                        break;
                    }
                    delayDays.put(Dict.DELAY_DATE, CommonUtils.dateCompare((String) issueInfo.get(Dict.DUE_DATE), date));
                    delayDays.put(Dict.STAGE, MpassComEnum.DevIssueStageEnum.ALPHA.getName());
                    break;

                case Dict.TWO:
                    //beta?????????
                    if (!CommonUtils.isNullOrEmpty(componentRecord)) continue;
                    delayDays.put(Dict.DELAY_DATE, CommonUtils.dateCompare((String) issueInfo.get(Dict.DUE_DATE), now));
                    delayDays.put(Dict.STAGE, MpassComEnum.DevIssueStageEnum.BETA.getName());
                    break;
                default:
                    continue;
            }
            //????????????????????????
            String assignee = (String) issueInfo.get(Dict.ASSIGNEE);
            Map<String, Object> userName = roleService.queryUserbyid(assignee);
            HashMap<String, Object> developerInfo = new HashMap<>();
            if (userName != null) {
                developerInfo.put(Dict.ID, userName.get(Dict.ID));
                developerInfo.put(Dict.USER_NAME_CN, userName.get(Dict.USER_NAME_CN));
            }
            delayDays.put(Dict.NAME, mpassComponent.getName_cn());
            delayDays.put(Dict.ID, mpassComponent.getId());
            delayDays.put(Dict.RQRMNTS_ADMIN, mpassComponent.getManager());
            delayDays.put(Dict.RQRMNTS_ID, mpassReleaseIssue.getId());
            delayDays.put(Dict.RQRMNTS_NAME, mpassReleaseIssue.getTitle());
            delayDays.put(Dict.DEVELOP, developerInfo);
            delayDays.put(Dict.FEATURE_BRANCH, issueInfo.get(Dict.FEATURE_BRANCH));
            delayDays.put(Dict.DUE_DATE, issueInfo.get(Dict.DUE_DATE));
            delayDays.put(Dict.TYPE, Constants.MPASS_COMPONENT_TYPE);
            issueDelayInfo.add(delayDays);
        }
        return issueDelayInfo;
    }

    @Override
    public String getComponentAlphaVersion(Map<String, String> hashMap) throws Exception {
        String gitlabUrl = hashMap.get(Dict.GITLAB_URL);//gitlab????????????
        String targetBranch = hashMap.get(Dict.TARGET_BRANCH);//????????????release????????????
        ComponentInfo componentInfo = componentInfoService.queryByWebUrl(gitlabUrl);
        if (componentInfo == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????" + gitlabUrl + "?????????"});
        }
        MpassReleaseIssue mpassReleaseIssue = mpassReleaseIssueDao.queryByComponentAndBranch(componentInfo.getId(), targetBranch);
        if (mpassReleaseIssue == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????????????????"});
        }
        String predictVersion = mpassReleaseIssue.getPredict_version();
        return predictVersion + Constants._ALPHA + Dict._SNAPSHOT;
    }

    @Override
    public String queryJdkByGitlabUrl(Map<String, String> hashMap) throws Exception {
        String gitlabUrl = hashMap.get(Dict.GITLAB_URL);//gitlab????????????
        ComponentInfo componentInfo = componentInfoService.queryByWebUrl(gitlabUrl);
        if (componentInfo == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????" + gitlabUrl + "?????????"});
        }
        return componentInfo.getJdk_version();
    }

    @Override
    public void createMutiComponentRecord(Map hashMap) throws Exception {
        String gitlabUrl = (String) hashMap.get(Dict.GITLAB_URL);//gitlab????????????
        String targetBranch = (String) hashMap.get(Dict.TARGET_BRANCH);//??????????????????
        String version = (String) hashMap.get(Dict.VERSION);//??????????????????????????????
        String releaseLog = (String) hashMap.get(Dict.RELEASE_LOG);//????????????
        ComponentInfo componentInfo = componentInfoService.queryByWebUrl(gitlabUrl);
        if (componentInfo == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????" + gitlabUrl + "?????????"});
        }
        MpassReleaseIssue mpassReleaseIssue = mpassReleaseIssueDao.queryByComponentAndBranch(componentInfo.getId(), targetBranch);
        if (mpassReleaseIssue == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"??????????????????" + targetBranch + "?????????"});
        }
        //???????????????????????????????????????
        ComponentRecord old = componentRecordDao.queryByComponentIdAndVersion(componentInfo.getId(), version);
        if (null == old) {
            ComponentRecord componentRecord = new ComponentRecord();
            componentRecord.setComponent_id(componentInfo.getId());
            componentRecord.setVersion(version);//??????
            componentRecord.setRelease_log(releaseLog);//????????????
            componentRecord.setDate(Util.simdateFormat(new Date()));//????????????
            componentRecord.setIssue_id(mpassReleaseIssue.getId());//??????Release??????????????????id
            if (StringUtils.isNotBlank(version) && version.contains(Constants.ALPHA)) {
                componentRecord.setType(Constants.RECORD_TEST_TYPE);//?????????alpha???????????????????????????
            }
            componentRecordDao.save(componentRecord);
        } else {
            old.setRelease_log(releaseLog);//????????????
            old.setDate(Util.simdateFormat(new Date()));//????????????
            componentRecordDao.update(old);
        }
    }

    @Override
    public void mergedSitAndReleaseCallBack(String state, Integer iid, Integer project_id, String target_branch) throws Exception {
        String gitlabId = String.valueOf(project_id);
        String merge_state = state;
        String merge_request_id = String.valueOf(iid);

        //???tag_record??????????????????merge?????????????????????
        TagRecord tagRecord = tagRecordDao.findByMidAndGid(gitlabId, merge_request_id);
        if (tagRecord != null) {
            if (Dict.MERGED.equals(merge_state)) {
                String target_version = tagRecord.getTarget_version();

                if ("SIT".equals(target_branch)) {

                    ArrayList<Map<String, String>> variables = new ArrayList<>();

                    HashMap<String, String> version = new HashMap<>();
                    version.put(Dict.KEY, Dict.VERSION);
                    version.put(Dict.VALUE, target_version);
                    variables.add(version);

                    HashMap<String, String> type = new HashMap<>();
                    type.put(Dict.KEY, Dict.TYPE);
//                    type.put(Dict.VALUE, "alpha");
                    type.put(Dict.VALUE, "beta");
                    variables.add(type);
                    Object result = gitlabService.createPipeline(gitlabId, target_branch, variables, token);
                } else {
                    //??????beta tag
                    createTag(Constants.RC_LOWCASE, gitlabId, target_version, target_branch);
                }
            }
        }
    }

    @Override
    public void mergedMasterCallBack(String state, Integer iid, Integer project_id, String target_branch) throws Exception {
        String gitlabId = String.valueOf(project_id);
        String merge_state = state;
        String merge_request_id = String.valueOf(iid);

        //???tag_record??????????????????merge?????????????????????
        TagRecord tagRecord = tagRecordDao.findByMidAndGid(gitlabId, merge_request_id);
        if (tagRecord != null) {
            if (Dict.MERGED.equals(merge_state)) {
                String target_version = tagRecord.getTarget_version();
                gitlabSerice.createTags(gitlabId, Constants.VERSION_TAG + tagRecord.getTarget_version(), Dict.MASTER, token);

                //???????????????????????????
                ComponentRecord componentRecord = new ComponentRecord(tagRecord.getComponent_id(), target_version, TimeUtils.getFormat(TimeUtils.FORMAT_DATE_TIME));
                componentRecord.setPackagetype(Constants.PIPEPACKAGE);
                ComponentRecord record = componentRecordDao.upsert(componentRecord);

                //??????tag
                Map<String, String> map = new HashMap<String, String>();
                map.put("app_id", tagRecord.getComponent_id());
                map.put("release_node_name", tagRecord.getRelease_node_name());
                map.put("tag", Constants.VERSION_TAG + tagRecord.getTarget_version());
                iReleaseNodeService.saveTag(map);
            }
        }
    }

    @Override
    public void addComponentRecord(Map hashMap) throws Exception {
        String gitlabUrl = (String) hashMap.get(Dict.GITLAB_URL);//gitlab????????????
        String version = (String) hashMap.get(Dict.VERSION);//??????
        String releaseLog = (String) hashMap.get(Dict.RELEASE_LOG);//????????????
        String componentType = (String) hashMap.get("component_type");
        String componentId = "";
        if ("mpass".equals(componentType)) {

            MpassComponent mpassComponent = mpassComponentService.queryByGitlabUrl(gitlabUrl);
            if (mpassComponent == null) {
                throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????" + gitlabUrl + "?????????"});
            }
            componentId = mpassComponent.getId();

        } else if ("back".equals(componentType)) {

            ComponentInfo component = componentInfoService.queryByWebUrl(gitlabUrl);
            if (component == null) {
                throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"????????????" + gitlabUrl + "?????????"});
            }
            componentId = component.getId();
        }
        ComponentRecord componentRecord = new ComponentRecord();
        componentRecord.setComponent_id(componentId);
        componentRecord.setVersion(version);//??????
        componentRecord.setRelease_log(releaseLog);//????????????
        componentRecord.setDate(Util.simdateFormat(new Date()));//????????????

        componentRecordService.save(componentRecord);
    }


    /**
     * ??????tag
     *
     * @param alphaOrRc
     * @param gitlabId
     * @param target_version
     * @throws Exception
     */
    private void createTag(String alphaOrRc, String gitlabId, String target_version, String target_branch) throws Exception {
        String tag;
        String version = target_version + "-" + alphaOrRc;
        List list = gitlabSerice.queryTag(gitlabId, version);
        if (CommonUtils.isNullOrEmpty(list)) {
            tag = version + ".0";
        } else {
            tag = version + "." + list.size();
        }

        String vtag = Constants.VERSION_TAG + tag;
        //??????Tag???????????????
        boolean existTag = gitlabSerice.checkBranchOrTag(token, gitlabId, Constants.TAG, vtag);
        if (existTag) {
            throw new FdevException(ErrorConstants.TAG_EXISTS, new String[]{"Tag: " + vtag + "?????????  "});
        }
        gitlabSerice.createTags(gitlabId, vtag, target_branch, token);
    }
}
