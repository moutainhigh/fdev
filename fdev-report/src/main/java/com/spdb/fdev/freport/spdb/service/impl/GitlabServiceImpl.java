package com.spdb.fdev.freport.spdb.service.impl;

import com.spdb.fdev.freport.base.dict.GitlabDict;
import com.spdb.fdev.freport.base.utils.CommonUtils;
import com.spdb.fdev.freport.base.utils.TimeUtils;
import com.spdb.fdev.freport.spdb.dao.AppDao;
import com.spdb.fdev.freport.spdb.dao.ReportDao;
import com.spdb.fdev.freport.spdb.dao.UserDao;
import com.spdb.fdev.freport.spdb.dto.gitlab.MergeRequestDto;
import com.spdb.fdev.freport.spdb.dto.gitlab.WebHooksDto;
import com.spdb.fdev.freport.spdb.entity.app.AppEntity;
import com.spdb.fdev.freport.spdb.entity.report.GitlabMergeRecord;
import com.spdb.fdev.freport.spdb.entity.user.Group;
import com.spdb.fdev.freport.spdb.service.GitlabService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GitlabServiceImpl implements GitlabService {

    private static final Logger logger = LoggerFactory.getLogger(GitlabServiceImpl.class);

    @Value("${gitlab.api.url}")
    private String gitLabApiUrl;

    @Value("${gitlab.manager.token}")
    private String gitlabManagerToken;

    @Value("${fdev.user.group.internet.sortNum}")
    private String internetSortNum;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ReportDao reportDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private AppDao appDao;

    @Override
    public void dealWebHooksMerge(WebHooksDto webHooksDto) throws Exception {
        Set<String> internetGroupIdSet = getInternetGroupIdSet();//???????????????????????????????????????groupIdSet
        AppEntity app = appDao.findByProjectId(webHooksDto.getProject().getId());
        if (!CommonUtils.isNullOrEmpty(app) && internetGroupIdSet.contains(app.getGroup())) {//????????????merge????????????????????????????????????????????????
            GitlabMergeRecord record = new GitlabMergeRecord();
            record.setMergeId(webHooksDto.getObjectattributes().getId());
            record.setProjectId(webHooksDto.getProject().getId());
            record.setRepositoryName(webHooksDto.getRepository().getName());
            record.setTitle(webHooksDto.getObjectattributes().getTitle());
            record.setCreateTime(TimeUtils.FORMAT_TIMESTAMP.format(new Date()));//???????????????????????????????????????????????????????????????????????????????????????
            record.setCreatorGitlabId(webHooksDto.getObjectattributes().getAuthor_id());
            record.setCreatorGitlabName(webHooksDto.getUser().getName());
            record.setSourceBranch(webHooksDto.getObjectattributes().getSource_branch());
            record.setTargetBranch(webHooksDto.getObjectattributes().getTarget_branch());
            record.setGroupId(app.getGroup());
            reportDao.insertGitlabMergeRecord(record);
        }
    }

    /**
     * gitlabApi????????????
     *
     * @param projectId
     * @return
     */
    @Override
    public List<MergeRequestDto> getProjectMergeRequest(String projectId) {
        try {
            return restTemplate.exchange(gitLabApiUrl + GitlabDict.PROJECTS + "/" + projectId + GitlabDict.MERGE_REQUESTS,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders() {{
                        setBearerAuth(gitlabManagerToken);
                    }}),
                    new ParameterizedTypeReference<List<MergeRequestDto>>() {
                    }).getBody();
        } catch (HttpClientErrorException e) {
            logger.info(e.getMessage() + ":" + projectId);//404 Not Found
            return null;
        }
    }

    private Set<String> getInternetGroupIdSet() {
        List<Group> groupList = userDao.findGroup(new Group() {{
            setStatus("1");//?????????????????????
            setSortNum(internetSortNum);//?????????????????????????????????
        }});
        dealChildren(groupList);
        return getAllGroupIdSet(groupList);
    }

    /**
     * ?????????????????????
     *
     * @param target
     */
    private void dealChildren(List<Group> target) {
        target.forEach(item -> {
            item.setChildren(new ArrayList<>());//?????????
            target.forEach(resource -> {
                if (item.getId().equals(resource.getParentId())) {
                    item.getChildren().add(resource);
                }
            });
        });
    }

    /**
     * ??????????????????groupId
     */
    private Set<String> getAllGroupIdSet(List<Group> resourse) {
        return new HashSet<String>() {{
            for (Group item : resourse) {
                add(item.getId());
                if (!CommonUtils.isNullOrEmpty(item.getChildren())) {
                    addAll(getAllGroupIdSet(item.getChildren()));
                }
            }
        }};
    }
}
