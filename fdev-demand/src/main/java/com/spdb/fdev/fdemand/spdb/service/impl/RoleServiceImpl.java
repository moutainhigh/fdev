package com.spdb.fdev.fdemand.spdb.service.impl;

import com.spdb.fdev.common.User;
import com.spdb.fdev.common.annoation.LazyInitProperty;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.fdemand.base.dict.Dict;
import com.spdb.fdev.fdemand.base.dict.ErrorConstants;
import com.spdb.fdev.fdemand.base.utils.CommonUtils;
import com.spdb.fdev.fdemand.spdb.dao.IDemandAssessDao;
import com.spdb.fdev.fdemand.spdb.dao.IDemandBaseInfoDao;
import com.spdb.fdev.fdemand.spdb.dao.IImplementUnitDao;
import com.spdb.fdev.fdemand.spdb.dao.IOtherDemandTaskDao;
import com.spdb.fdev.fdemand.spdb.dao.impl.IpmpUnitDaoImpl;
import com.spdb.fdev.fdemand.spdb.entity.*;
import com.spdb.fdev.fdemand.spdb.service.IIpmpUnitService;
import com.spdb.fdev.fdemand.spdb.service.IRoleService;
import com.spdb.fdev.transport.RestTransport;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.util.*;

@RefreshScope
@Service
public class RoleServiceImpl implements IRoleService {

    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    @Value("${fdev.role.control.enabled:true}")
    private boolean roleControlEnabled;

    @Value("${basic.demand.manager.role.id}")
    private String basic_demand_manager;

    @Value("${fdev.demand.ip}")
    private String demandIp;

    @Autowired
    private RestTransport restTransport;

    @Autowired
    private IDemandBaseInfoDao demandBaseInfoDao;

    @Autowired
    private IImplementUnitDao implementUnitDao;

    @Autowired
    private IpmpUnitDaoImpl ipmpUnitDao;

    @Autowired
    private IIpmpUnitService ipmpUnitService;

    @Autowired
    private IOtherDemandTaskDao otherDemandTaskDao;

    @Autowired
    private IDemandAssessDao demandAssessDao;

    /**
     * ???????????????????????????
     *
     * @return
     * @throws Exception
     */
    @Override
    public boolean isDemandManager() throws Exception {
        if (roleControlEnabled) {
            User user = CommonUtils.getSessionUser();
            if (user.getRole_id().contains(basic_demand_manager)) {
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * ???????????????????????????
     *
     * @return
     * @throws Exception
     */
    @Override
    public boolean isDemandManager(User user) throws Exception {
        if (roleControlEnabled) {
            if (user.getRole_id().contains(basic_demand_manager)) {
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    @LazyInitProperty(redisKeyExpression = "userinfo.{id}")
    public Map<String, Object> queryUserbyid(String id) throws Exception {
        Map<String, Object> send_map = new HashMap<>();
        send_map.put(Dict.REST_CODE, Dict.QUERYUSERCOREDATA);
        send_map.put(Dict.ID, id);// ???????????????????????????????????????
        List<Map<String, Object>> list = (ArrayList) restTransport.submit(send_map);
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    @Override
    @LazyInitProperty(redisKeyExpression = "userinfo.{userNameEn}")
    public Map<String, Object> queryUserbyUserNameEn(String userNameEn) throws Exception {
        Map<String, Object> send_map = new HashMap<>();
        send_map.put(Dict.REST_CODE, Dict.QUERYUSERCOREDATA);
        send_map.put(Dict.USER_NAME_EN, userNameEn);// ???????????????????????????????????????
        List<Map<String, Object>> list = (ArrayList) restTransport.submit(send_map);
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    @Override
    @LazyInitProperty(redisKeyExpression = "userinfo.{gitlab_user_id}")
    public Map<String, Object> queryUserbyGitId(String gitlab_user_id) throws Exception {
        Map<String, Object> send_map = new HashMap<>();
        send_map.put(Dict.REST_CODE, Dict.QUERYUSER);
        send_map.put(Dict.GIT_USER_ID, gitlab_user_id);// ???????????????????????????????????????
        List<Map<String, Object>> list = (ArrayList) restTransport.submit(send_map);
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    @Override
    @LazyInitProperty(redisKeyExpression = "group.{id}")
    public Map<String, Object> queryByGroupId(String id) {
        try {
            Map<String, Object> send_map = new HashMap<>();
            send_map.put(Dict.REST_CODE, Dict.QUERYCHILDGROUPBYID);
            send_map.put(Dict.ID, id);// ???????????????????????????????????????
            List<Map<String, Object>> list = (ArrayList) restTransport.submit(send_map);
            if (list != null && list.size() > 0) {
                return list.get(0);
            }
            return null;
        } catch (Exception e) {
            logger.error("?????????????????????????????????,??????id{}", id);
            return null;
        }

    }

    /**
     * ???????????????????????????????????????
     *
     * @param id
     * @return
     */
    @Override
    @LazyInitProperty(redisKeyExpression = "queryGroup.{id}")
    public Map<String, Object> queryGroup(String id) {
        try {
            Map<String, Object> send_map = new HashMap<>();
            send_map.put(Dict.REST_CODE, "getGroups");
            send_map.put(Dict.ID, id);// ???????????????????????????????????????
            List<Map<String, Object>> list = (ArrayList) restTransport.submit(send_map);
            if (list != null && list.size() > 0) {
                return list.get(0);
            }
            return null;
        } catch (Exception e) {
            logger.error("?????????????????????????????????,??????id{}", id);
            return null;
        }

    }

    /**
     * ????????????id????????????????????????
     *
     * @param map
     * @param user_id
     */
    @Override
    public void addUserName(Map map, String user_id) {
        try {
            if (StringUtils.isNotBlank(user_id)) {
                Map userMap = this.queryUserbyid(user_id);
                if (userMap != null) {
                    map.put(Dict.NAME_EN, userMap.get(Dict.USER_NAME_EN));
                    map.put(Dict.NAME_CN, userMap.get(Dict.USER_NAME_CN));
                    Map assigneeMap = new HashMap();
                    assigneeMap.put(Dict.ID, user_id);
                    assigneeMap.put(Dict.USER_NAME_EN, userMap.get(Dict.USER_NAME_EN));
                    assigneeMap.put(Dict.USER_NAME_CN, userMap.get(Dict.USER_NAME_CN));
                    map.put("assigneeMap", assigneeMap);
                }
            }
        } catch (Exception e) {
            logger.error("????????????id??????????????????????????????,{}", e.getMessage());
        }

    }

    /**
     * ?????????????????????????????????????????????????????????id?????????
     * zhanghp4
     *
     * @return
     * @throws Exception
     */
    @Override
    public boolean isDemandLeader(String demand_id, String user_id) throws Exception {
        if (CommonUtils.isNullOrEmpty(demand_id)) {
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"??????id"});
        }
        if (CommonUtils.isNullOrEmpty(user_id)) {
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"??????id"});
        }
        DemandBaseInfo demandBaseInfo = demandBaseInfoDao.queryById(demand_id);
        if (CommonUtils.isNullOrEmpty(demandBaseInfo)) {
            return false;
        }
        HashSet<String> demand_leader = demandBaseInfo.getDemand_leader();
        if (CommonUtils.isNullOrEmpty(demand_leader)) {
            return false;
        }
        if (demand_leader.contains(user_id)) {
            return true;
        }
        return false;
    }


    /**
     * ??????????????????????????????????????????????????????????????????
     * zhanghp4
     *
     * @return
     * @throws Exception
     */
    @Override
    public boolean isDemandLeader(DemandBaseInfo demandBaseInfo, String user_id) throws Exception {
        if (CommonUtils.isNullOrEmpty(demandBaseInfo)) {
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"??????"});
        }
        if (CommonUtils.isNullOrEmpty(user_id)) {
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"??????id"});
        }
        if (CommonUtils.isNullOrEmpty(demandBaseInfo)) {
            return false;
        }
        HashSet<String> demand_leader = demandBaseInfo.getDemand_leader();
        if (CommonUtils.isNullOrEmpty(demand_leader)) {
            return false;
        }
        if (demand_leader.contains(user_id)) {
            return true;
        }
        return false;
    }

    /**
     * ???????????????????????????????????????????????????????????????id?????????
     * zhanghp4
     *
     * @return
     * @throws Exception
     */
    @Override
    public boolean isDemandGroupLeader(String demand_id, String user_id) throws Exception {
        if (CommonUtils.isNullOrEmpty(demand_id)) {
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"??????id"});
        }
        if (CommonUtils.isNullOrEmpty(user_id)) {
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"??????id"});
        }
        DemandBaseInfo demandBaseInfo = demandBaseInfoDao.queryById(demand_id);
        if (CommonUtils.isNullOrEmpty(demandBaseInfo)) {
            return false;
        }
        ArrayList<RelatePartDetail> relate_part_detail = demandBaseInfo.getRelate_part_detail();
        if (CommonUtils.isNullOrEmpty(relate_part_detail)) {
            return false;
        }
        /**
         * ??????????????????????????????????????????????????????????????????????????????
         */
        for (RelatePartDetail relatePartDetail : relate_part_detail) {
            HashSet<String> assessUser = relatePartDetail.getAssess_user();
            if (!CommonUtils.isNullOrEmpty(assessUser) && assessUser.contains(user_id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> queryChildGroupByIds(List<String> ids) throws Exception {
        Set<String> resDate = new HashSet();
        for (String id : ids) {
            List<LinkedHashMap> resList = queryChildGroupById(id);
            for (LinkedHashMap item : resList) {
                resDate.add((String) item.get(Dict.ID));
            }
        }
        return resDate;
    }

    public Map queryByUserCoreData(List userIds) {
        Map resDate;
        Map sendDate = new HashMap();
        sendDate.put(Dict.REST_CODE, "queryByUserCoreData");
        sendDate.put("ids", userIds);
        try {
            resDate = (Map) restTransport.submit(sendDate);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new FdevException(ErrorConstants.FUSER_QUERY_ERROR, new String[]{"??????????????????????????????????????????"});
        }
        return resDate;
    }

    @Override
    public List queryChildGroupById(String groupId) {
        List<LinkedHashMap> resDate;
        Map<String, String> sendDate = new HashMap<>();
        sendDate.put(Dict.REST_CODE, Dict.QUERYCHILDGROUPBYID);
        sendDate.put(Dict.ID, groupId);
        try {
            resDate = (List<LinkedHashMap>) restTransport.submit(sendDate);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new FdevException(ErrorConstants.FUSER_QUERY_ERROR);
        }
        return resDate;
    }

    public Map queryFdevRoleByName(String roleName) {
        List<LinkedHashMap> resDate;
        Map sendDate = new HashMap();
        sendDate.put(Dict.REST_CODE, "queryRole");
        sendDate.put("name", roleName);
        try {
            resDate = (List<LinkedHashMap>) restTransport.submit(sendDate);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new FdevException(ErrorConstants.FUSER_QUERY_ERROR, new String[]{"??????????????????????????????????????????"});
        }
        if (!CommonUtils.isNullOrEmpty(resDate))
            return resDate.get(0);
        return null;
    }

    @Override
    public List queryDevResource(List<String> groupIds) {
        List resDate;
        Map sendDate = new HashMap();
        sendDate.put(Dict.REST_CODE, Dict.QUERYDEVRESOURCE);
        sendDate.put(Dict.GROUPIDS, groupIds);
        try {
            resDate = (List<LinkedHashMap>) restTransport.submit(sendDate);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new FdevException(ErrorConstants.FUSER_QUERY_ERROR);
        }
        return resDate;
    }

    /**
     * @param demand
     * @param userId
     * @param type
     * @return
     * @// TODO: 2020/11/6
     */
    @Override
    public Map addCommissionEvent(DemandBaseInfo demand, List<String> userId, String type) {
        String[] userIdAll = userId.toArray(new String[userId.size()]);
        Map resDate;
        Map sendDate = new HashMap();
        String link = demandIp + "/fdev/#/rqrmn/designReviewRqr/" + demand.getId();
        sendDate.put(Dict.REST_CODE, "addCommissionEvent");
        sendDate.put("user_ids", userIdAll);
        sendDate.put("module", "rqrmnt");
        sendDate.put("description", "???????????????" + demand.getOa_contact_no() + "?????????????????????ui???????????????");
        sendDate.put("link", link);
        sendDate.put("type", type);
        sendDate.put("target_id", demand.getId());
        try {
            resDate = (Map) restTransport.submit(sendDate);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new FdevException(ErrorConstants.FUSER_QUERY_ERROR);
        }
        return resDate;
    }

    @Override
    public List queryUserByRole(String mainId) {
        List<Map> resDate;
        Map sendDate = new HashMap();
        sendDate.put(Dict.REST_CODE, "queryUsers");
        sendDate.put("role_id", Arrays.asList(mainId));
        try {
            resDate = (List<Map>) restTransport.submit(sendDate);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new FdevException(ErrorConstants.FUSER_QUERY_ERROR);
        }
        return resDate;
    }

    /**
     * ????????????????????????
     */
    @Override
    public List<Map> queryUser() {
        List<Map> resDate;
        Map sendDate = new HashMap();
        sendDate.put(Dict.REST_CODE, "queryUser");
        try {
            resDate = (List<Map>) restTransport.submit(sendDate);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new FdevException(ErrorConstants.FUSER_QUERY_ERROR);
        }
        return resDate;

    }

    /**
     * ????????????????????????
     */
    @Override
    @LazyInitProperty(redisKeyExpression = "fdemand.groupuser.{groupId}")
    public List<Map> queryGroupUser(String groupId) {
        List<Map> resDate;
        Map sendDate = new HashMap();
        sendDate.put(Dict.REST_CODE, "queryUser");
        sendDate.put(Dict.GROUP_ID_YH, groupId);
        try {
            resDate = (List<Map>) restTransport.submit(sendDate);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new FdevException(ErrorConstants.FUSER_QUERY_ERROR);
        }
        return resDate;
    }

    /**
     * ???????????????????????????
     * @param groupId
     * @return
     */
    @Override
    public List<Map> queryGroupManage(String groupId, String roleId) {
        List<Map> resDate;
        Map sendDate = new HashMap();
        sendDate.put(Dict.REST_CODE, Dict.QUERYUSERCOREDATA);
        sendDate.put(Dict.GROUP_ID_YH, groupId);
        List<String> roles = new ArrayList<>();
        roles.add(roleId);
        sendDate.put(Dict.ROLEID, roles);
        try {
            resDate = (List<Map>) restTransport.submit(sendDate);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new FdevException(ErrorConstants.FUSER_QUERY_ERROR);
        }
        return resDate;
    }

    /**
     * ?????????????????????
     *
     * @// TODO: 2020/11/6
     */
    @Override
    public void sendNotify(List<String> userIds, String content, String demandId) {
        Map sendDate = new HashMap();
        sendDate.put(Dict.REST_CODE, "sendUserNotify");
        sendDate.put("target", userIds);
        sendDate.put("content", content);
        sendDate.put("desc", "ui???????????????");
        sendDate.put("type", "0");
        /**
         * @// TODO: 2020/11/6
         **/
        sendDate.put("hyperlink", demandIp + "/fdev/#/rqrmn/designReviewRqr/" + demandId);
        try {
            restTransport.submit(sendDate);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new FdevException("???????????????????????????notify??????");
        }
    }

    /**
     * ???????????????????????????????????????
     */
    @Override
    public boolean isPartAsesser(String demandId, String implpart, User user) {
        if (CommonUtils.isNullOrEmpty(demandId)) {
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"??????"});
        }
        if (CommonUtils.isNullOrEmpty(implpart)) {
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"????????????"});
        }
        if (CommonUtils.isNullOrEmpty(user)) {
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"??????id"});
        }

        DemandBaseInfo demandBaseInfo = demandBaseInfoDao.queryById(demandId);

        if (CommonUtils.isNullOrEmpty(demandBaseInfo)) {
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{"??????????????????"});
        }


        Set<String> parts = demandBaseInfo.getRelate_part();
        List<RelatePartDetail> relatePartDetails = demandBaseInfo.getRelate_part_detail();
        List<String> assesser = new ArrayList<String>();
        if (CommonUtils.isNullOrEmpty(parts)) {
            return false;
        }
        if (CommonUtils.isNullOrEmpty(relatePartDetails)) {
            return false;
        }

        //String userpart = user.getGroup_id();

        if (parts.contains(implpart)) {
            for (RelatePartDetail relatePartDetail : relatePartDetails) {
                if (!relatePartDetail.getPart_id().equalsIgnoreCase(implpart)) {
                    continue;
                }
                Set<String> users = relatePartDetail.getAssess_user();
                if (!CommonUtils.isNullOrEmpty(users)) {
                    if (users.contains(user.getId())) {
                        return true;
                    }
                }
            }
        }
        return false;

    }

    /**
     * ??????????????????????????????????????????????????????
     *
     * @param demandId
     * @param user_en
     * @return
     */
    @Override
    public boolean isIpmpUnitLeader(String demandId, String user_en) throws Exception {
        DemandBaseInfo demandBaseInfo = demandBaseInfoDao.queryById(demandId);
        //??????????????????????????????
        Map<String, Object> paramMap = new HashMap();
        paramMap.put(Dict.DEMANDID, demandBaseInfo.getId());
        paramMap.put(Dict.ISTECH, demandBaseInfo.getDemand_type());

        Map<String, Object> ipmpUnitsObj = ipmpUnitService.queryIpmpUnitByDemandId(paramMap);
        List<IpmpUnit> ipmpUnits = (List<IpmpUnit>) ipmpUnitsObj.get(Dict.DATA);
        if (!CommonUtils.isNullOrEmpty(ipmpUnits)) {
            for (IpmpUnit ipmpUnit : ipmpUnits) {
                if (!CommonUtils.isNullOrEmpty(ipmpUnit.getImplLeader())) {
                    String implLeader = ipmpUnit.getImplLeader();//ipmp?????????????????????????????????????????????,??????
                    if (implLeader.contains(user_en)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param impl_unit_id
     * @param user_id
     * @return
     */
    @Override
    public boolean isFdevUnitLeader(String impl_unit_id, String user_id) {
        FdevImplementUnit fdevImplementUnit = implementUnitDao.queryById(impl_unit_id);
        if (!CommonUtils.isNullOrEmpty(fdevImplementUnit)) {
            HashSet<String> implement_leaders = fdevImplementUnit.getImplement_leader();
            if (implement_leaders.contains(user_id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param fdevImplementUnitList
     * @param user_id
     * @return
     */
    @Override
    public boolean isFdevUnitListLeader(List<FdevImplementUnit> fdevImplementUnitList, String user_id) {
        if (!CommonUtils.isNullOrEmpty(fdevImplementUnitList)) {
            for (FdevImplementUnit fdevImplementUnit : fdevImplementUnitList) {
                HashSet<String> implement_leaders = fdevImplementUnit.getImplement_leader();
                if (implement_leaders.contains(user_id)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param ipmpUnit
     * @param user_en
     * @return
     */
    @Override
    public boolean isIpmpUnitLeader(IpmpUnit ipmpUnit, String user_en) throws Exception {
        String implLeader = ipmpUnit.getImplLeader();//ipmp?????????????????????????????????????????????,??????
        if (!CommonUtils.isNullOrEmpty(implLeader) && implLeader.contains(user_en)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isOtherDemandTaskLeader(OtherDemandTask otherDemandTask, String userId) throws Exception {
        String firmLeaderId = otherDemandTask.getFirmLeaderId();//???????????????
        String taskLeaderId = otherDemandTask.getTaskLeaderId();//???????????????
        if ((!CommonUtils.isNullOrEmpty(firmLeaderId) && firmLeaderId.equals(userId)) || taskLeaderId.equals(userId))
            return true;
        return false;
    }

    @Override
    public boolean isOtherDemandTaskLeader(String demandId, String userId) throws Exception {
        //??????????????????????????????????????????????????????????????????????????????????????????????????????
        Map<String, Object> param = new HashMap<>();
        param.put(Dict.DEMANDID, demandId);
        Map<String, Object> otherDemandTaskData = otherDemandTaskDao.queryOtherDemandTaskList(param);
        List<OtherDemandTask> otherDemandTaskList = (List<OtherDemandTask>) otherDemandTaskData.get(Dict.DATA);
        return otherDemandTaskList.stream().anyMatch(otherDemandTask -> userId.equals(otherDemandTask.getTaskLeaderId())
                || userId.equals(otherDemandTask.getFirmLeaderId()));
    }

    @Override
    public boolean isManagerAndDemandAssessLeader(String demandAssessId, String userId) throws Exception {
        DemandAssess getDemandAssess = demandAssessDao.queryById(demandAssessId);
        if (CommonUtils.isNullOrEmpty(getDemandAssess)) {
            throw new FdevException(ErrorConstants.DEMAND_NULL_ERROR);
        }
        HashSet<String> demandLeader = getDemandAssess.getDemand_leader();
        if (demandLeader.contains(userId)) {
            return true;
        }
        return false;
    }

    @Override
    public List<Map<String, String>> queryGroupByIds(Set<String> groupIdList) throws Exception {
        return (List<Map<String, String>>) restTransport.submit(new HashMap() {{
            put("groupIds", groupIdList);
            put(Dict.REST_CODE, "queryGroupByIds");
        }});
    }
}


