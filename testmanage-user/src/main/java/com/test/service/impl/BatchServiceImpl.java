package com.test.service.impl;

import com.test.dao.WorkConfigDao;
import com.test.dict.Constants;
import com.test.dict.Dict;
import com.test.dict.ErrorConstants;
import com.test.service.BatchService;
import com.test.service.FdevUserService;
import com.test.testmanagecommon.exception.FtmsException;
import com.test.testmanagecommon.transport.RestTransport;
import com.test.testmanagecommon.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RefreshScope
public class BatchServiceImpl implements BatchService {

    @Autowired
    private WorkConfigDao workConfigDao;
    @Autowired
    private RestTransport restTransport;
    private static Logger logger = LoggerFactory.getLogger(BatchServiceImpl.class);
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

    @Resource
    private FdevUserService fdevUserService;

    @Override
    public void batchConfigUser() throws Exception {
        workConfigDao.batchWorkLeader();
        workConfigDao.batchUatContact();
        List<Map<String, String>> groupLeaderNumList = workConfigDao.queryGroupLeaderAll();
        List<Map<String, String>> ftmsUserIdAndCode = workConfigDao.ftmsUserIdAndCode();
        Map<String, String> user = new HashMap<>();
        for (Map<String, String> userInfo : ftmsUserIdAndCode) {
            String userId = String.valueOf(userInfo.get(Dict.ID));
            String username = userInfo.get(Dict.USERNAME);
            user.put(userId, username);
        }
        for (Map<String, String> groupLeaders : groupLeaderNumList) {
            List<String> usernameEn = new ArrayList<>();
            String fdevGroupId = groupLeaders.get(Dict.GROUPID);
            String leaders = groupLeaders.get(Dict.GROUPLEADER);
            String[] leaderList = leaders.split(",");
            for (String userNum : leaderList) {
                usernameEn.add(user.get(userNum));
            }
            String nameEn = String.join(",", usernameEn);
            workConfigDao.setNameEn(fdevGroupId, nameEn);
        }
    }

    @Override
    public void batchUserRoleLevelMantisToken() throws Exception {
        List<Map<String, String>> ftmsData = workConfigDao.getUserRoleLevelMantisToken();
        for (Map<String, String> userData : ftmsData) {
            String userEnName = userData.get(Dict.USER_NAME_EN);
            String level = userData.get(Dict.LEVEL);
            String role = userData.get(Dict.ROLE);
            String token = userData.get(Dict.TOKEN);
            Map sendMap = new HashMap();
            sendMap.put(Dict.REST_CODE, "fdev.user.core.query");
            sendMap.put(Dict.USER_NAME_EN, userEnName);
            List<Map<String, Object>> user = new ArrayList<>();
            try {
                user = (List<Map<String, Object>>) restTransport.submitSourceBack(sendMap);
            } catch (Exception e) {
                logger.error("fail to find fdev user info :" + userEnName);
            }
            if (!Util.isNullOrEmpty(user)) {
                String userId = String.valueOf(user.get(0).get(Dict.ID));
                Set<String> roleIds = new HashSet<>();
                List<String> roles = (List<String>) user.get(0).get(Dict.ROLE_ID);
                roleIds.addAll(roles);
                String[] roleArray = role.split(",");
                for (String roleName : roleArray) {
                    if (Dict.MEMBER.equals(roleName)) {
                        roleIds.add(testerRoleId);
                    }
                    if (Dict.GROUPLEADER.equals(roleName)) {
                        roleIds.add(testLeaderRoleId);
                    }
                    if (Dict.ADMIN.equals(roleName)) {
                        roleIds.add(testManagerRoleId);
                    }
                    if (Dict.ASSESSOR.equals(roleName)) {
                        roleIds.add(assessorRoleId);
                    }
                    if (Dict.MASTER.equals(roleName)) {
                        roleIds.add(testAdminRoleId);
                    }
                }
                Map sendFdev = new HashMap();
                sendFdev.put(Dict.REST_CODE, "fuser.updateUserFtms");
                sendFdev.put(Dict.ID, userId);
                sendFdev.put(Dict.FTMS_LEVEL, transLevel(level));
                sendFdev.put(Dict.ROLE, roleIds);
                sendFdev.put(Dict.MANTIS_TOKEN, token);
                try {
                    restTransport.submitSourceBack(sendFdev);
                } catch (Exception e) {
                    logger.error("fdev user update error, id = " + userId);
                    logger.error("e:" + e);
                    throw new FtmsException(ErrorConstants.DATA_NOT_EXIST);
                }
            }
        }
    }

    private String transLevel(String level) {
        if (Util.isNullOrEmpty(level)) {
            return "";
        }
        switch (Integer.valueOf(level)) {
            case 0:
                return "??????";
            case 1:
                return "??????";
            case 2:
                return "??????";
            case 3:
                return "??????";
            default:
                return "";
        }
    }

    /**
     * ???????????????fdev??????????????????fdev
     *
     * @throws Exception
     */
    @Override
    public Map timingSyncFuser() throws Exception {
        Map areaOrgName = new HashMap();
        //????????????areaid??????
        areaOrgName.put("5ec746d38116505b80a06ba0", "???????????????????????????(??????)");
        areaOrgName.put("5ec746e98116505b80a06ba1", "???????????????????????????(??????)");
        areaOrgName.put("5ec746f58116505b80a06ba2", "???????????????????????????(??????)");
        areaOrgName.put("5ec746ff8116505b80a06ba3", "???????????????????????????(??????)");
        areaOrgName.put("5ec746ff8116505b80a06ba4", "???????????????????????????(??????)");
        Map sendOld = new HashMap();
        sendOld.put(Dict.REST_CODE, "fdev.old.user.query");
        List<Map> allOldUser = new ArrayList<>();
        List<Map> oldUsers = new ArrayList<>();
        try {
            allOldUser = (List<Map>) restTransport.submitSourceBack(sendOld);
            oldUsers = allOldUser.stream().filter(e -> "0".equals(e.get(Dict.STATUS))).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("fail to get userInfos from old user");
            throw new FtmsException(ErrorConstants.SERVER_ERROR);
        }
        if (!Util.isNullOrEmpty(oldUsers)) {
            //???????????????????????????
            oldUsers = oldUsers.stream().filter(e -> !Util.isNullOrEmpty(e.get(Dict.CREATETIME))).collect(Collectors.toList());
            for (Map oldUser : oldUsers) {
                String createTime = (String) oldUser.get(Dict.CREATETIME);
                SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT);
                Long createTimeDate = sdf.parse(createTime).getTime();
                Long now = new Date().getTime();
                Long minus = now - createTimeDate;
                if (minus <= 61 * 60 * 1000) {
                    //????????????????????????????????????,????????????????????????????????????
                    String nameEn = (String) oldUser.get(Dict.USER_NAME_EN);
                    Map sendNew = new HashMap();
                    sendNew.put(Dict.USER_NAME_EN, nameEn);
                    sendNew.put(Dict.REST_CODE, "fdev.user.query");
                    List<Map> newUser = new ArrayList<>();
                    try {
                        newUser = (List<Map>) restTransport.submitSourceBack(sendNew);
                    } catch (Exception e) {
                        logger.error("fail to get newUser for : " + nameEn);
                        throw new FtmsException(ErrorConstants.SERVER_ERROR);
                    }
                    if (Util.isNullOrEmpty(newUser)) {
                        //????????????????????????????????????
                        String orgName = (String) areaOrgName.get((String) oldUser.get("area_id"));
                        //????????????????????????
                        if (Util.isNullOrEmpty(orgName)) {
                            orgName = "???????????????????????????(??????)";
                        }
                        String orgId = "";
                        Map sendGroup = new HashMap();
                        sendGroup.put(Dict.NAME, orgName);
                        sendGroup.put(Dict.REST_CODE, "fdev.user.queryGroupDetail");
                        sendGroup.put("scope", "all");
                        try {
                            orgId = (String) ((Map) ((List) restTransport.submitSourceBack(sendGroup)).get(0)).get(Dict.ID);
                        } catch (Exception e) {
                            logger.error("fail to get groupinfo for : " + orgName);
                        }
                        oldUser.put("orgId", orgId);
                        //???????????????
                        oldUser.put(Dict.ROLE_ID, Arrays.asList(new String[]{"5c9dcebdd1785ec6a04c0dc8"}));
                        oldUser.put(Dict.REST_CODE, "fdev.user.add");
                        Map result = new HashMap();
                        try {
                            result = (Map) restTransport.submitSourceBack(oldUser);
                            return result;
                        } catch (Exception e) {
                            logger.error("fail to add user by sync for : " + nameEn);
                        }
                    }
                }
            }
        }
        //??????????????????
        if (!Util.isNullOrEmpty(allOldUser)) {
            List<Map> recentLeave = allOldUser.stream().filter(e -> "1".equals(e.get(Dict.STATUS))).collect(Collectors.toList());
            List<String> recentLeaveNameEns = recentLeave.stream().map(e -> (String) e.get(Dict.USER_NAME_EN)).collect(Collectors.toList());
            fdevUserService.setLeaveByIds(recentLeaveNameEns);
        }
        return null;
    }

}
