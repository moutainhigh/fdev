package com.spdb.fdev.component.web;

import com.spdb.fdev.base.annotation.NoNull;
import com.spdb.fdev.base.dict.Constants;
import com.spdb.fdev.base.dict.Dict;
import com.spdb.fdev.base.dict.ErrorConstants;
import com.spdb.fdev.base.generator.DictGenerator;
import com.spdb.fdev.base.utils.CommonUtils;
import com.spdb.fdev.common.JsonResult;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.common.util.JsonResultUtil;
import com.spdb.fdev.common.validate.RequestValidate;
import com.spdb.fdev.component.dao.IArchetypeInfoDao;
import com.spdb.fdev.component.dao.IMpassArchetypeDao;
import com.spdb.fdev.component.entity.*;
import com.spdb.fdev.component.service.*;
import com.spdb.fdev.component.service.impl.CommonInfoService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RefreshScope
@RequestMapping("/api/archetype")
public class ArchetypeController {

    private static final Logger logger = LoggerFactory.getLogger(ArchetypeController.class);

    @Value("${lock.scanComponent.timeout}")
    private int lock_scancmp_timeout;

    @Autowired
    private IMpassArchetypeService mpassArchetypeService;

    @Autowired
    private IArchetypeInfoService archetypeInfoService;

    @Autowired
    private IArchetypeIssueService archetypeIssueService;

    @Autowired
    private IArchetypeRecordService archetypeRecordService;

    @Autowired
    private IRoleService roleService;

    @Autowired
    private IArchetypeInfoDao archetypeInfoDao;

    @Autowired
    private IMpassArchetypeDao mpassArchetypeDao;

    @Autowired
    private IAppService appService;

    @Autowired
    private IApplicationArchetypeService applicationArchetypeService;

    @Autowired
    private IComponentArchetypeService componentArchetypeService;

    @Autowired
    private IComponentInfoService componentInfoService;

    @Autowired
    private IComponentRecordService componentRecordService;

    @Autowired
    private IArchetypeScanService archetypeScanService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CommonInfoService commonInfoService;

    @Autowired
    private IArchetypeTypeService archetypeTypeService;

    /**
     * ????????????????????????
     *
     * @param archetypeInfo
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/queryArchetypes")
    public JsonResult queryArchetypeInfos(@RequestBody ArchetypeInfo archetypeInfo) throws Exception {
        List<Map> result = new ArrayList<>();
        List<ArchetypeInfo> list = archetypeInfoService.query(archetypeInfo);
        for (ArchetypeInfo info : list) {
            Map map = CommonUtils.object2Map(info);
            //???????????????????????????????????????
            ArchetypeRecord record = archetypeRecordService.queryByArchetypeIdAndType(info.getId(), Constants.RECORD_RECOMMEND_TYPE);
            if (record != null) {
                map.put(Dict.RECOMMOND_VERSION, record.getVersion());
            }
            //????????????
            if (StringUtils.isNotBlank(info.getGroup())) {
                Map groupMap = roleService.queryByGroupId(info.getGroup());
                if (groupMap != null) {
                    map.put(Dict.GROUP_NAME, groupMap.get(Dict.NAME));
                }
            }
            result.add(map);
        }
        return JsonResultUtil.buildSuccess(result);
    }



    @PostMapping(value = "/queryLastArchetypes")
    public JsonResult queryLastArchetypes(@RequestBody ArchetypeInfo archetypeInfo) throws Exception {
        List<Map> result = new ArrayList<>();
        List<ArchetypeInfo> list = archetypeInfoService.query(archetypeInfo);
        if (!CommonUtils.isNullOrEmpty(list)) {
            for (ArchetypeInfo info : list) {
                Map map = CommonUtils.object2Map(info);
                //???????????????????????????????????????
                ArchetypeRecord record = archetypeRecordService.queryByArchetypeIdAndType(info.getId(), Constants.RECORD_RECOMMEND_TYPE);
                if (record != null) {
                    map.put(Dict.RECOMMOND_VERSION, record.getVersion());
                } else {
                    //??????????????????????????????????????????SNAPSHOT??????
                    ArchetypeRecord archetypeRecord = archetypeRecordService.queryLatestRelease(info.getId());
                    if (!CommonUtils.isNullOrEmpty(archetypeRecord)) {
                        map.put(Dict.RECOMMOND_VERSION, archetypeRecord.getVersion());
                    }
                }

                result.add(map);
            }
        }
        return JsonResultUtil.buildSuccess(result);
    }

    /**
     * ??????????????????
     *
     * @param archetypeInfo
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/addArchetype")
    @NoNull(require = {Dict.NAME_EN, Dict.NAME_CN, Dict.DESC, Dict.GITLAB_URL,
            Dict.GROUPID, Dict.ARTIFACTID, Dict.MANAGER_ID, Dict.GROUP, Dict.TYPE, Dict.ENCODING}, parameter = ArchetypeInfo.class)
    public JsonResult addArchetype(@RequestBody ArchetypeInfo archetypeInfo) throws Exception {
        return JsonResultUtil.buildSuccess(archetypeInfoService.save(archetypeInfo));
    }

    /**
     * ????????????
     *
     * @param archetypeInfo
     * @return
     * @throws Exception
     */
    @PostMapping(value = "updateArchetype")
    @NoNull(require = {Dict.ID, Dict.NAME_EN, Dict.NAME_CN, Dict.DESC, Dict.GITLAB_URL,
            Dict.GROUPID, Dict.ARTIFACTID, Dict.MANAGER_ID, Dict.GROUP, Dict.TYPE, Dict.ENCODING}, parameter = ArchetypeInfo.class)
    public JsonResult updateArchetype(@RequestBody ArchetypeInfo archetypeInfo) throws Exception {
        return JsonResultUtil.buildSuccess(archetypeInfoService.update(archetypeInfo));
    }

    /**
     * ??????????????????
     *
     * @param archetypeInfo
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/queryArchetypeDetail")
    @NoNull(require = {Dict.ID}, parameter = ArchetypeInfo.class)
    public JsonResult queryArchetypeDetail(@RequestBody ArchetypeInfo archetypeInfo) throws Exception {
        ArchetypeInfo info = archetypeInfoService.queryById(archetypeInfo);
        if (info != null) {
            Map map = CommonUtils.object2Map(info);
            //???????????????????????????????????????
            ArchetypeRecord record = archetypeRecordService.queryByArchetypeIdAndType(info.getId(), Constants.RECORD_RECOMMEND_TYPE);
            if (record != null) {
                map.put(Dict.RECOMMOND_VERSION, record.getVersion());
            }
            //????????????
            if (StringUtils.isNotBlank(info.getGroup())) {
                Map groupMap = roleService.queryByGroupId(info.getGroup());
                if (groupMap != null) {
                    map.put(Dict.GROUP_NAME, groupMap.get(Dict.NAME));
                }
            }
            return JsonResultUtil.buildSuccess(map);
        }
        return JsonResultUtil.buildSuccess();
    }

    /**
     * ????????????????????????
     *
     * @param archetypeRecord
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/queryArchetypeHistory")
    @NoNull(require = {Dict.ARCHETYPE_ID}, parameter = ArchetypeRecord.class)
    public JsonResult query(@RequestBody ArchetypeRecord archetypeRecord) throws Exception {
        List<Map> result = new ArrayList<>();
        List<ArchetypeRecord> list = archetypeRecordService.queryByArchetypeIdAndRegexVersion(archetypeRecord);
        for (ArchetypeRecord record : list) {
            Map map = CommonUtils.object2Map(record);
            //????????????????????????????????????????????????
            roleService.addUserName(map, record.getUpdate_user());
            result.add(map);
        }
        return JsonResultUtil.buildSuccess(result);
    }

    /**
     * ????????????????????????
     *
     * @param archetypeInfo
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/scanArchetypeHistory")
    @NoNull(require = {Dict.ID, Dict.GROUPID, Dict.ARTIFACTID}, parameter = ArchetypeInfo.class)
    public JsonResult scanArchetypeHistory(@RequestBody ArchetypeInfo archetypeInfo) throws Exception {
        archetypeScanService.initArchetypeHistory(archetypeInfo);
        return JsonResultUtil.buildSuccess();
    }

    /**
     * ????????????????????????
     *
     * @param archetypeRecord
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/updateArchetypeHistory")
    @NoNull(require = {Dict.ID}, parameter = ArchetypeRecord.class)
    public JsonResult updateArchetypeHistory(@RequestBody ArchetypeRecord archetypeRecord) throws Exception {
        return JsonResultUtil.buildSuccess(archetypeRecordService.update(archetypeRecord));
    }


    /**
     * ????????????????????????
     *
     * @param map
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/optimizeArchetype")
    @RequestValidate(NotEmptyFields = {Dict.ARCHETYPE_ID, Dict.TITLE, Dict.TARGET_VERSION, Dict.FEATURE_BRANCH, Dict.ASSIGNEE, Dict.DUE_DATE})
    public JsonResult addArchetypeIssue(@RequestBody Map<String, String> map) throws Exception {
        archetypeIssueService.save(map);
        return JsonResultUtil.buildSuccess();
    }

    /**
     * ???????????????
     *
     * @param map
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/judgeTargetVersion")
    @RequestValidate(NotEmptyFields = {Dict.ARCHETYPE_ID, Dict.TARGET_VERSION})
    public JsonResult judgeTargetVersion(@RequestBody Map<String, String> map) throws Exception {
        archetypeIssueService.judgeTargetVersion(map);
        return JsonResultUtil.buildSuccess();
    }

    /**
     * ???????????????????????????????????????????????????????????????
     *
     * @param map
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/queryFirstVersion")
    @RequestValidate(NotEmptyFields = {Dict.ARCHETYPE_ID})
    public JsonResult queryFirstVersion(@RequestBody Map<String, String> map) {
        String archetype_id = map.get(Dict.ARCHETYPE_ID);
        List<ArchetypeIssue> list = archetypeIssueService.queryFirstVersion(archetype_id);
        if (null != list && list.size() > 0) {
            return JsonResultUtil.buildSuccess(list.get(list.size() - 1).getTarget_version());
        } else {
            return JsonResultUtil.buildSuccess();
        }
    }

    /**
     * ??????????????????????????????
     *
     * @param archetypeIssue
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/queryArchetypeIssues")
    @NoNull(require = {Dict.ARCHETYPE_ID}, parameter = ArchetypeIssue.class)
    public JsonResult queryArchetypeIssues(@RequestBody ArchetypeIssue archetypeIssue) throws Exception {
        List<Map> result = new ArrayList<>();
        List<ArchetypeIssue> issueList = archetypeIssueService.query(archetypeIssue);
        for (ArchetypeIssue issue : issueList) {
            Map map = CommonUtils.object2Map(issue);
            //????????????????????????????????????????????????
            roleService.addUserName(map, issue.getAssignee());
            result.add(map);
        }
        return JsonResultUtil.buildSuccess(result);
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param map
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/destroyArchetypeIssue")
    @RequestValidate(NotEmptyFields = {Dict.ID})
    public JsonResult destroyArchetypeIssue(@RequestBody Map<String, String> map) throws Exception {
        String id = map.get(Dict.ID);
        archetypeIssueService.destroyArchetypeIssue(id);
        return JsonResultUtil.buildSuccess();
    }

    /**
     * ??????????????????????????????
     *
     * @param archetypeIssue
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/queryArchetypeIssueDetail")
    @NoNull(require = {Dict.ID}, parameter = ArchetypeIssue.class)
    public JsonResult queryArchetypeIssueDetail(@RequestBody ArchetypeIssue archetypeIssue) throws Exception {
        Map map = new HashMap();
        ArchetypeIssue issue = archetypeIssueService.queryById(archetypeIssue);
        if (issue != null) {
            map = CommonUtils.object2Map(issue);
            //????????????????????????????????????????????????
            roleService.addUserName(map, issue.getAssignee());
            //???????????????manageid
            ArchetypeInfo archetypeInfo = archetypeInfoService.queryById(issue.getArchetype_id());
            if (archetypeInfo != null) {
                map.put(Dict.MANAGER_ID, archetypeInfo.getManager_id());
            }
        }
        return JsonResultUtil.buildSuccess(map);
    }

    /**
     * ????????????
     *
     * @param map
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/changeStage")
    @RequestValidate(NotEmptyFields = {Dict.ID, Dict.STAGE})
    public JsonResult changeStage(@RequestBody Map<String, String> map) throws Exception {
        archetypeIssueService.changeStage(map);
        return JsonResultUtil.buildSuccess();
    }

    /**
     * ????????????
     *
     * @param map
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/package")
    @RequestValidate(NotEmptyFields = {Dict.ARCHETYPE_ID, Dict.STAGE,
            Dict.TARGET_VERSION, Dict.FEATURE_BRANCH})
    public JsonResult packageTag(@RequestBody Map<String, String> map) throws Exception {
        archetypeIssueService.packageTag(map);
        return JsonResultUtil.buildSuccess();
    }

    @PostMapping(value = "/relDevops")
    @RequestValidate(NotEmptyFields = {Dict.TYPE, Dict.ARCHETYPE_ID, Dict.BRANCH, "release_node_name"})
    public JsonResult relDevops(@RequestBody Map<String, String> map) throws Exception {
        archetypeInfoService.relDevops(map);
        return JsonResultUtil.buildSuccess();
    }

    @PostMapping(value = "/queryLatestVersion")
    @RequestValidate(NotEmptyFields = {Dict.ARCHETYPE_ID, "release_node_name"})
    public JsonResult queryLatestVersion(@RequestBody Map<String, String> map) throws Exception {
        return JsonResultUtil.buildSuccess(archetypeInfoService.queryLatestVersion(map));
    }


    @PostMapping(value = "/queryReleaseVersion")
    @RequestValidate(NotEmptyFields = {Dict.ARCHETYPE_ID})
    public JsonResult queryReleaseVersion(@RequestBody Map<String, String> map) throws Exception {
        return JsonResultUtil.buildSuccess(archetypeInfoService.queryReleaseVersion(map));
    }


    /**
     * ??????SNAPSHOT?????????????????????
     *
     * @param map
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/queryReleaseLog")
    @RequestValidate(NotEmptyFields = {Dict.ARCHETYPE_ID,
            Dict.TARGET_VERSION})
    public JsonResult queryReleaseLog(@RequestBody Map<String, String> map) {
        String archetypeId = map.get(Dict.ARCHETYPE_ID);
        String targetVersion = map.get(Dict.TARGET_VERSION);
        ArchetypeRecord record = archetypeIssueService.queryReleaseLog(archetypeId, targetVersion);
        return JsonResultUtil.buildSuccess(record);
    }


    @PostMapping(value = "/queryArchetypeIssueRecord")
    @RequestValidate(NotEmptyFields = {Dict.ARCHETYPE_ID, Dict.TARGET_VERSION, Dict.STAGE})
    public JsonResult queryArchetypeIssueRecord(@RequestBody Map<String, String> map) throws Exception {
        List<Map> result = new ArrayList<>();
        List<ArchetypeRecord> list = (List) archetypeIssueService.queryIssueRecord(map);
        if (!CommonUtils.isNullOrEmpty(list)) {
            for (ArchetypeRecord record : list) {
                Map beanMap = CommonUtils.object2Map(record);
                //????????????????????????????????????????????????
                roleService.addUserName(beanMap, record.getUpdate_user());
                result.add(beanMap);
            }
        }
        return JsonResultUtil.buildSuccess(result);
    }


    /**
     * ??????????????????????????????
     */
    /**
     * ????????????id???????????????????????????????????????????????????
     * ????????????????????????
     *
     * @param applicationArchetype
     * @return
     * @throws Exception
     */
    @PostMapping("/queryApplicatonsByArchetype")
    @NoNull(require = {Dict.ARCHETYPE_ID, Dict.ARCHETYPE_VERSION}, parameter = ApplicationArchetype.class)
    public JsonResult queryApplicatonsByArchetype(@RequestBody ApplicationArchetype applicationArchetype) throws Exception {
        List<ApplicationArchetype> list = applicationArchetypeService.query(applicationArchetype);
        List<Map> result = new ArrayList<>();
        for (ApplicationArchetype application : list) {
            Map map = CommonUtils.object2Map(application);
            Map app = null;
            try {
                app = appService.queryAPPbyid(application.getApplication_id());
            } catch (Exception e) {
                logger.error("????????????{}????????????,{}", application.getApplication_id(), e.getMessage());
                applicationArchetypeService.delete(application);
            }
            if (app != null) {
                map.put(Dict.NAME_EN, app.get(Dict.NAME_EN));
                map.put(Dict.NAME_ZH, app.get(Dict.NAME_ZH));
            }
            ArchetypeRecord record = archetypeRecordService.queryByArchetypeIdAndVersion(application.getArchetype_id(), application.getArchetype_version());
            if (record != null) {
                map.put(Dict.TYPE, record.getType());
            }
            result.add(map);
        }
        return JsonResultUtil.buildSuccess(result);
    }


    /**
     * ????????????????????????????????????
     *
     * @param componentArchetype
     * @return
     * @throws Exception
     */
    @PostMapping("/queryComponentByArchetype")
    @NoNull(require = {Dict.ARCHETYPE_ID, Dict.ARCHETYPE_VERSION}, parameter = ComponentArchetype.class)
    public JsonResult queryComponentByArchetype(@RequestBody ComponentArchetype componentArchetype) throws Exception {
        List<ComponentArchetype> list = componentArchetypeService.query(componentArchetype);
        logger.info("?????????????????????????????????" + list.size());
        List<Map> result = new ArrayList<>();
        for (ComponentArchetype component : list) {
            Map map = CommonUtils.object2Map(component);
            ComponentInfo componentInfo = componentInfoService.queryById(component.getComponent_id());
            if (componentInfo != null) {
                map.put(Dict.NAME_EN, componentInfo.getName_en());
                map.put(Dict.NAME_CN, componentInfo.getName_cn());
            }
            ComponentRecord record = componentRecordService.queryByComponentIdAndVersion(component.getComponent_id(), component.getComponent_version());
            if (record != null) {
                map.put(Dict.TYPE, record.getType());
            }
            result.add(map);
        }
        return JsonResultUtil.buildSuccess(result);
    }

    /**
     * ??????????????????????????????????????????????????????
     *
     * @param requestParam
     * @return
     */
    @PostMapping("/scanArchetype")
    @RequestValidate(NotEmptyFields = {Dict.ARCHETYPE_ID})
    public JsonResult scanArchetype(@RequestBody Map<String, String> requestParam) throws Exception {
        String archetype_id = requestParam.get(Dict.ARCHETYPE_ID);
        //??????????????????
        if (!redisTemplate.opsForValue().setIfAbsent("lock.scanArchetype." + archetype_id, "1",
                this.lock_scancmp_timeout, TimeUnit.MINUTES))
            throw new FdevException(ErrorConstants.SCAN_OPERATION_TOO_FREQUENT,
                    new String[]{String.valueOf(this.lock_scancmp_timeout)});

        ArchetypeInfo archetypeInfo = archetypeInfoService.queryById(archetype_id);
        if (archetypeInfo == null) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"archetype can not find"});
        }
        archetypeScanService.initArchetypeApplication(archetypeInfo);
        return JsonResultUtil.buildSuccess();
    }

    /**
     * ??????????????????????????????????????????
     */
    @PostMapping(value = "/updateArchetypeGitlabCiYaml")
    public JsonResult updateArchetypeGitlabCiYaml(@RequestBody Map<String, String> map) throws Exception {
        ArchetypeInfo archetypeInfo = new ArchetypeInfo();
        List<ArchetypeInfo> list = archetypeInfoService.query(archetypeInfo);
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                ArchetypeInfo info = list.get(i);
                if (StringUtils.isNotBlank(info.getGitlab_url())) {
                    commonInfoService.updateIntegration(info.getGitlab_url(), info.getName_en(), Constants.COMPONENT_ARCHETYPE);//????????????
                }
            }
        }
        return JsonResultUtil.buildSuccess();
    }

    /**
     * ????????????????????????
     *
     * @param ArchetypeType
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/queryArchetypeTypes")
    public JsonResult queryArchetypeTypes(@RequestBody ArchetypeType ArchetypeType) throws Exception {
        List<Map> result = new ArrayList<>();
        List<ArchetypeType> list = archetypeTypeService.query(ArchetypeType);
        if (!CommonUtils.isNullOrEmpty(list)) {
            for (ArchetypeType type : list) {
                Map map = new HashMap<>();
                map.put(Dict.LABEL, type.getName());
                map.put(Dict.VALUE, type.getName());
                result.add(map);
            }
        }
        return JsonResultUtil.buildSuccess(result);
    }

    /**
     * ??????????????????
     *
     * @param ArchetypeType
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/addArchetypeTypes")
    @NoNull(require = {Dict.NAME}, parameter = ArchetypeType.class)
    public JsonResult addArchetypeTypes(@RequestBody ArchetypeType ArchetypeType) throws Exception {
        return JsonResultUtil.buildSuccess(archetypeTypeService.save(ArchetypeType));
    }

    /**
     * ???????????????
     *
     * @param map
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/saveTargetVersion")
    @RequestValidate(NotEmptyFields = {Dict.ARCHETYPE_ID, Dict.TARGET_VERSION})
    public JsonResult saveTargetVersion(@RequestBody Map<String, String> map) throws Exception {
        archetypeInfoService.saveTargetVersion(map);
        return JsonResultUtil.buildSuccess();
    }



    /**
     * ????????????????????????
     *
     * @param hashMap
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/queryMyArchetypes")
    @RequestValidate(NotEmptyFields = {Dict.USER_ID})
    public JsonResult queryMyArchetypes(@RequestBody Map<String, String> hashMap) throws Exception {
        String user_id = hashMap.get(Dict.USER_ID);
        List<Map> result = new ArrayList<>();
        List<ArchetypeInfo> list = archetypeInfoService.queryByUserId(user_id);
        for (ArchetypeInfo info : list) {
            Map map = CommonUtils.object2Map(info);
            //???????????????????????????????????????
            ArchetypeRecord record = archetypeRecordService.queryByArchetypeIdAndType(info.getId(), Constants.RECORD_RECOMMEND_TYPE);
            if (record != null) {
                map.put(Dict.RECOMMOND_VERSION, record.getVersion());
            }
            //????????????
            if (StringUtils.isNotBlank(info.getGroup())) {
                Map groupMap = roleService.queryByGroupId(info.getGroup());
                if (groupMap != null) {
                    map.put(Dict.GROUP_NAME, groupMap.get(Dict.NAME));
                }
            }
            result.add(map);
        }
        return JsonResultUtil.buildSuccess(result);
    }

    /**
     * ?????????????????????
     *
     * @param resMap
     * @return
     * @throws Exception
     */
    @PostMapping(value = "/queryAllArchetypes")
    public JsonResult queryAllComponents(@RequestBody Map<String, String> resMap) throws Exception {

        List<Map> result = new ArrayList<>();
        //???????????????????????????
        List<ArchetypeInfo> list1 = archetypeInfoService.query(new ArchetypeInfo());
        for (ArchetypeInfo info : list1) {

            if (!CommonUtils.isNullOrEmpty(info.getGitlab_id())) {
                Map map = CommonUtils.object2Map(info);
                //????????????
                if (StringUtils.isNotBlank(info.getGroup())) {
                    Map groupMap = roleService.queryByGroupId(info.getGroup());
                    if (groupMap != null) {
                        map.put(Dict.GROUP_NAME, groupMap.get(Dict.NAME));
                    }
                }
                result.add(map);
            }
        }

        //???????????????????????????
        List<MpassArchetype> list2 = mpassArchetypeService.query(new MpassArchetype());
        for (MpassArchetype info : list2) {
            if (!CommonUtils.isNullOrEmpty(info.getGitlab_id())) {
                Map map = CommonUtils.object2Map(info);
                //????????????
                if (StringUtils.isNotBlank(info.getGroup())) {
                    Map groupMap = roleService.queryByGroupId(info.getGroup());
                    if (groupMap != null) {
                        map.put(Dict.GROUP_NAME, groupMap.get(Dict.NAME));
                    }
                }
                result.add(map);
            }
        }
        return JsonResultUtil.buildSuccess(result);
    }


}
