package com.spdb.fdev.gitlabwork.schedule.service;

import com.alibaba.fastjson.JSONArray;
import com.spdb.fdev.gitlabwork.dao.IGitLabUserDao;
import com.spdb.fdev.gitlabwork.dao.IGitlabCommitDao;
import com.spdb.fdev.gitlabwork.dao.IGitlabProjectDao;
import com.spdb.fdev.gitlabwork.dao.IGitlabWorkDao;
import com.spdb.fdev.gitlabwork.entiy.GitlabCommit;
import com.spdb.fdev.gitlabwork.entiy.GitlabProject;
import com.spdb.fdev.gitlabwork.entiy.GitlabUser;
import com.spdb.fdev.gitlabwork.entiy.StatisticCodeLine;
import com.spdb.fdev.gitlabwork.service.IGitLabUserService;
import com.spdb.fdev.gitlabwork.service.IGitlabWorkService;
import com.spdb.fdev.gitlabwork.service.IStatisticCodeLineService;
import com.spdb.fdev.gitlabwork.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.*;

@Component
@RefreshScope
public class UpdateGitlabWorkTask {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    IGitlabCommitDao gitlabCommitDao;
    @Autowired
    IGitlabProjectDao gitlabProjectDao;
    @Autowired
    IGitLabUserDao gitLabUserDao;
    @Autowired
    IGitlabWorkService gitlabWorkService;
    @Autowired
    IGitLabUserService gitLabUserService;
    @Autowired
    private IGitlabWorkDao gitlabWorkDao;
    @Autowired
    private IStatisticCodeLineService statisticsService;

    @Value("${schedule.since}")
    private String startDate;

    public void updateWorkInfo() {
        try {
            this.updateNewUser();
            this.updateExistUserAndProject();
            this.updateExistUserAndNewProject();
            gitLabUserDao.updateSign(0, 1);
            gitlabProjectDao.updateSign(1, 2);
        } catch (Exception e) {
            logger.error(">>>>>UpdateGitlabWorkTask.updateWorkInfo????????????????????????", e.getMessage());
        }
    }

    /**
     * ???????????????????????????????????????commit??????
     */
    public void updateNewUser() throws ParseException {
        String endDate = Util.simpleDateFormat("yyyy-MM-dd").format(new Date());
        logger.info(">>>>>????????????????????????????????????commit????????????" + startDate + "???????????????" + endDate);
        List<Map<String, Object>> resultMapList = new ArrayList<>();
        List<GitlabUser> newUserList = gitLabUserDao.selectBySign(0);
        for (int i = 0; i < newUserList.size(); i++) {
            GitlabUser gitlabUser = newUserList.get(i);
            logger.info("????????????????????????" + (i + 1) + "?????????" + newUserList.size() + "???");
            //????????????????????????????????????????????????????????????????????????????????????
            List<String> dateList = Util.getBetweenDates(startDate, endDate);
            for (String committed_date : dateList) {
                Map<String, Object> resultMap = new HashMap<>();
                List<Map<String, Object>> resultDeatilMapList = new ArrayList<>();
                List<GitlabCommit> gitlabCommitList = gitlabCommitDao.selectGitlabCommitInfo(gitlabUser.getName(), gitlabUser.getUsername(), gitlabUser.getGituser(), gitlabUser.getConfigname(), committed_date);
                if (gitlabCommitList.size() > 0) {
                    gitlabWorkService.addCommitInfoToMap(gitlabCommitList, resultMap, resultDeatilMapList, gitlabUser);
                }
                if (resultMap.size() > 0)
                    resultMapList.add(resultMap);
            }
        }
        gitlabWorkService.addWorkInfoFromMap(resultMapList);
    }

    /**
     * ???????????????????????????????????????????????????????????????
     */
    public void updateExistUserAndProject() {
        logger.info(">>>>>?????????????????????????????????????????????????????????????????????" + Util.getDateBefore());
        List<Map<String, Object>> resultMapList = new ArrayList<>();
        List<GitlabUser> existUserList = gitLabUserDao.selectBySign(1);
        for (int i = 0; i < existUserList.size(); i++) {
            GitlabUser gitlabUser = existUserList.get(i);
            logger.info("?????????????????????????????????????????????" + (i + 1) + "?????????" + existUserList.size() + "???");
            List<GitlabProject> gitlabProjectList = gitlabProjectDao.selectBySign(2);
            List<String> projectList = new ArrayList();
            for (GitlabProject gitlabProject : gitlabProjectList) {
                String projectName = gitlabProject.getNameEn();
                projectList.add(projectName);
            }
            Map<String, Object> resultMap = new HashMap<>();
            List<Map<String, Object>> resultDeatilMapList = new ArrayList<>();
            String committed_date = Util.getDateBefore();
            List<GitlabCommit> gitlabCommitList = gitlabCommitDao.selectGitlabCommitInfo(gitlabUser.getName(), gitlabUser.getUsername(), gitlabUser.getGituser(), gitlabUser.getConfigname(), committed_date, projectList);
            if (gitlabCommitList.size() > 0) {
                gitlabWorkService.addCommitInfoToMap(gitlabCommitList, resultMap, resultDeatilMapList, gitlabUser);
            }
            if (resultMap.size() > 0)
                resultMapList.add(resultMap);
        }
        gitlabWorkService.addWorkInfoFromMap(resultMapList);

    }

    /**
     * ????????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    public void updateExistUserAndNewProject() throws ParseException {
        String startDate = Util.getWeekBefore();
        String endDate = Util.simpleDateFormat("yyyy-MM-dd").format(new Date());
        logger.info(">>>>>????????????????????????????????????????????????????????????????????????" + startDate + "???????????????" + endDate);
        List<Map<String, Object>> resultMapList = new ArrayList<>();
        List<GitlabUser> existUserList = gitLabUserDao.selectBySign(1);
        List<GitlabProject> gitlabProjectList = gitlabProjectDao.selectBySign(1);
        if (null != gitlabProjectList && gitlabProjectList.size() > 0) {
            logger.info(">>>>???????????????" + gitlabProjectList.size() + "???");
            for (int i = 0; i < existUserList.size(); i++) {
                GitlabUser gitlabUser = existUserList.get(i);
                logger.info("????????????????????????????????????????????????" + (i + 1) + "?????????" + existUserList.size() + "???");
                List<String> projectList = new ArrayList();
                for (GitlabProject gitlabProject : gitlabProjectList) {
                    String projectName = gitlabProject.getNameEn();
                    projectList.add(projectName);
                }
                List<String> dateList = Util.getBetweenDates(startDate, endDate);
                for (String committed_date : dateList) {
                    Map<String, Object> resultMap = new HashMap<>();
                    List<Map<String, Object>> resultDeatilMapList = new ArrayList<>();
                    List<GitlabCommit> gitlabCommitList = gitlabCommitDao.selectGitlabCommitInfo(gitlabUser.getName(), gitlabUser.getUsername(), gitlabUser.getGituser(), gitlabUser.getConfigname(), committed_date, projectList);
                    if (gitlabCommitList.size() > 0) {
                        gitlabWorkService.addCommitInfoToMap(gitlabCommitList, resultMap, resultDeatilMapList, gitlabUser);
                    }
                    if (resultMap.size() > 0)
                        resultMapList.add(resultMap);
                }
            }
            gitlabWorkService.updateWorkInfoFromMap(resultMapList);//????????????????????????????????????????????????????????????????????????????????????
        }

    }

    public List<String> getNameList(GitlabUser gitlabUser) {
        List<String> nameList = new ArrayList<>();
        nameList.add(gitlabUser.getUsername());
        nameList.add(gitlabUser.getName());
        nameList.add(gitlabUser.getGituser());
        nameList.add(gitlabUser.getConfigname());
        return nameList;
    }


}
