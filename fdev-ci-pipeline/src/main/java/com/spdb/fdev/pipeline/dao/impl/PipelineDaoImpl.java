package com.spdb.fdev.pipeline.dao.impl;

import com.mongodb.client.result.UpdateResult;
import com.spdb.fdev.base.dict.Constants;
import com.spdb.fdev.base.dict.Dict;
import com.spdb.fdev.base.dict.ErrorConstants;
import com.spdb.fdev.base.utils.CommonUtils;
import com.spdb.fdev.base.utils.TimeUtils;
import com.spdb.fdev.common.User;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.pipeline.dao.IPipelineCronDao;
import com.spdb.fdev.pipeline.dao.IPipelineDao;
import com.spdb.fdev.pipeline.dao.IPluginDao;
import com.spdb.fdev.pipeline.entity.*;
import com.spdb.fdev.pipeline.service.IUserService;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class PipelineDaoImpl implements IPipelineDao {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private IPluginDao pluginDao;
    @Autowired
    private IPipelineCronDao pipelineCronDao;
    @Autowired
    private IUserService userService;

    @Override
    public Pipeline queryById(String id) {
        Query query = new Query(Criteria.where(Dict.PIPELINEID).is(id));
        return mongoTemplate.findOne(query, Pipeline.class, "pipeline");
    }

    @Override
    public List<Pipeline> queryDetailByProjectId(String id) throws Exception {
        Query query = new Query(Criteria.where(Dict.BINDPROJECTPROJECTID)
                .is(id).and(Dict.TRIGGERRULES_PUSH_SWITCH).is(true).and(Dict.STATUS).is("1"));
        return mongoTemplate.find(query, Pipeline.class);
    }

    @Override
    public Pipeline queryOneByProjectId(String id) throws Exception {
        Query query = new Query(Criteria.where(Dict.BINDPROJECTPROJECTID)
                .is(id).and(Dict.STATUS).is("1"));
        return mongoTemplate.findOne(query, Pipeline.class);
    }

    @Override
    public String add(Pipeline pipeline) throws Exception{
        ObjectId objectId = new ObjectId();
        String id = objectId.toString();
        pipeline.setId(id);
        if(CommonUtils.isNullOrEmpty(pipeline.getNameId())){
            pipeline.setNameId(id);
        }
        pipeline.setStatus(Constants.STATUS_OPEN);
        //?????????????????????????????????????????????????????????
        pipeline.setCreateTime(TimeUtils.getDate(TimeUtils.FORMAT_DATE_TIME));
        pipeline.setUpdateTime(pipeline.getCreateTime());
        return this.mongoTemplate.save(pipeline).getId();
    }

    @Override
    public void delete(String pipelineId) {
        Query query = new Query(Criteria.where(Dict.PIPELINEID).is(pipelineId));
//        Pipeline pipeline = mongoTemplate.findOne(query, Pipeline.class);
        mongoTemplate.findAndRemove(query, Pipeline.class);
    }

    @Override
    public void updateStatusClose(String id , User user){
        Query query = Query.query(Criteria.where(Dict.PIPELINEID).is(id));
        Update update = Update.update(Dict.STATUS, Constants.STATUS_CLOSE);
        mongoTemplate.findAndModify(query, update, Pipeline.class);
    }

    /**
     * ?????????????????????
     *
     * @param skip
     * @param limit
     * @param applicationId ??????id
     * @param userId    ??????id
     * @param apps  ????????????
     * @param user  ???????????????????????????????????????
     * @param searchContent     ????????????
     * @return
     */
    @Override
    public Map<String, Object> queryPipelineList(long skip, int limit, String applicationId, String userId, List<String> apps, User user, String searchContent) {
        Map<String, Object> map = new HashMap<>();
        Criteria baseCri = new Criteria();
        baseCri.and(Dict.STATUS).ne(Constants.ZERO);
//        Query query = Query.query(Criteria.where(Dict.STATUS).ne(Constants.ZERO));
        if(!CommonUtils.isNullOrEmpty(applicationId)) {
//            Criteria appCriteria = Criteria.where(Dict.BINDPROJECTPROJECTID).is(applicationId);
//            query.addCriteria(appCriteria);
            baseCri.and(Dict.BINDPROJECTPROJECTID).is(applicationId);
        }
        if(!CommonUtils.isNullOrEmpty(userId)) {
//            Criteria collectedCriteria = Criteria.where(Dict.COLLECTED).in(userId);
//            query.addCriteria(collectedCriteria);
            baseCri.and(Dict.COLLECTED).in(userId);
        }
        if(!CommonUtils.isNullOrEmpty(apps)) {
//            Criteria collectedCriteria = Criteria.where(Dict.BINDPROJECTPROJECTID).in(apps);
//            query.addCriteria(collectedCriteria);
            baseCri.and(Dict.BINDPROJECTPROJECTID).in(apps);
        }
        if(!CommonUtils.isNullOrEmpty(searchContent)) {
            searchContent = ".*?" + searchContent + ".*";
            Criteria nameCriteria = Criteria.where(Dict.NAME).regex(searchContent);
            Criteria projectNameEnCriteria = Criteria.where(Dict.BINDPROJECT_NAMEEN).regex(searchContent);
            Criteria projectNameCnCriteria = Criteria.where(Dict.BINDPROJECT_NAMECN).regex(searchContent);
            Criteria descCriteria = Criteria.where(Dict.DESC).regex(searchContent);
            Criteria authorCriteria = Criteria.where(Dict.AUTHOR_NAMECN).regex(searchContent);
            Criteria criteria = new Criteria().orOperator(nameCriteria, projectNameEnCriteria,projectNameCnCriteria, descCriteria, authorCriteria);
            baseCri.orOperator(nameCriteria, projectNameEnCriteria,projectNameCnCriteria, descCriteria, authorCriteria);
//            query.addCriteria(criteria);
        }
        //?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
//        if(!CommonUtils.isNullOrEmpty(user)) {
//            String groupId = user.getGroup_id();
//            List<String> groupIds = userService.getLineIdsByGroupId(groupId);
//            if (!CommonUtils.isNullOrEmpty(groupIds)) {
////                query.addCriteria(Criteria.where(Dict.GROUPLINEID).in(groupIds));
//                baseCri.and(Dict.GROUPLINEID).in(groupIds);
//            }
//        }
        List<AggregationOperation> aggOperationList = new ArrayList<>();
        MatchOperation match = Aggregation.match(baseCri);
        LookupOperation lookup = Aggregation.lookup(Dict.PIPELINEDIGITALRATE, Dict.NAMEID, Dict.NAMEID, "digitalRate");
        UnwindOperation unwind = Aggregation.unwind("digitalRate", true);
        aggOperationList.add(match);
        aggOperationList.add(lookup);
        aggOperationList.add(unwind);

        List<String> sortList = new ArrayList<>();
        //sortList.add("build_time");
        sortList.add(Dict.UPDATETIME);
        Query query = new Query(baseCri);
        query.with(new Sort(Sort.Direction.DESC, sortList));
        long total = mongoTemplate.count(query, Pipeline.class);
        map.put(Dict.TOTAL, total);
        if(limit != 0) {
            query.skip(skip).limit(limit);
            Sort orders = new Sort(Sort.Direction.DESC, sortList);
            SortOperation sort = Aggregation.sort(orders);
            SkipOperation skipOperation = Aggregation.skip(skip);
            LimitOperation limitOperation = Aggregation.limit(limit);
            aggOperationList.add(sort);
            aggOperationList.add(skipOperation);
            aggOperationList.add(limitOperation);
        }
        Map addFieldsMap = new HashMap();
        addFieldsMap.put(Dict.AVEERRORTIME, "$digitalRate.aveErrorTime");
        addFieldsMap.put(Dict.ERROREXETOTAL, "$digitalRate.errorExeTotal");
        addFieldsMap.put(Dict.EXETOTAL, "$digitalRate.exeTotal");
        addFieldsMap.put(Dict.SUCCESSEXETOTAL, "$digitalRate.successExeTotal");
        addFieldsMap.put(Dict.SUCCESSRATE, "$digitalRate.successRate");
        addFieldsMap.put(Dict.ID, "$" + Dict.PIPELINEID);
        aggOperationList.add(getAddFieldsAggregationOper(addFieldsMap));
        Map projectMap = new HashMap();
        projectMap.put("digitalRate", 0);
        projectMap.put(Dict.PIPELINEID, 0);
        aggOperationList.add(getProjectFiterFields(projectMap));

        Aggregation aggregation = Aggregation.newAggregation(aggOperationList);
        AggregationResults<Map> results = this.mongoTemplate.aggregate(aggregation, Dict.PIPELINE, Map.class);
        List<Map> list = results.getMappedResults();
//        List<Pipeline> list = mongoTemplate.find(query, Pipeline.class);

        map.put(Dict.PIPELINELIST, list);
        return map;
    }

    /**
     * ??????spring????????????mongo???addFields???????????????????????????????????????
     * ?????????map????????????
     *  key ??????????????????????????????value???????????????$??????
     *  ?????????mongo?????????
     *      {
     *         $addFields:{
     *             "aveErrorTime":"$digitalRate.aveErrorTime",
     *             "errorExeTotal":"$digitalRate.errorExeTotal",
     *             "exeTotal":"$digitalRate.exeTotal",
     *             "successExeTotal":"$digitalRate.successExeTotal",
     *             "successRate":"$digitalRate.successRate"
     *         }
     *     }
     * @param data
     * @return
     */
    private AggregationOperation getAddFieldsAggregationOper(Map<String, Object> data) {
        AggregationOperation aggregationOperation = new AggregationOperation() {
            @Override
            public Document toDocument(AggregationOperationContext aggregationOperationContext) {
                return new Document("$addFields", new Document(data));
            }
        };
        return aggregationOperation;
    }

    /**
     * ??????project??????????????? ???mongo?????????
     *     {
     *         $project:{
     *             "digitalRate":0
     *         }
     *     }
     * @param data
     * @return
     */
    private AggregationOperation getProjectFiterFields(Map<String, Object> data) {
        AggregationOperation project = new AggregationOperation() {
            @Override
            public Document toDocument(AggregationOperationContext aggregationOperationContext) {
                return new Document("$project", new Document(data));
            }
        };
        return project;
    }

    @Override
    public String saveDraft(PipelineDraft draft) throws Exception{
        ObjectId objectId = new ObjectId();
        String id = objectId.toString();
        draft.setId(id);
        //1. ??????????????????
        String authorID = userService.getUserFromRedis().getId();
        deleteDraft(authorID);
        //2. ?????????????????????
        draft.setAuthorId(authorID);
        draft.setUpdateTime(TimeUtils.getDate(TimeUtils.FORMAT_DATE_TIME));
        return this.mongoTemplate.save(draft).getId();
    }

    @Override
    public void deleteDraft(String authorId) throws Exception{
        Query query = new Query(Criteria.where(Dict.AUTHORID).is(authorId));
        if(!CommonUtils.isNullOrEmpty(mongoTemplate.findOne(query, PipelineDraft.class))){
            mongoTemplate.findAndRemove(query, PipelineDraft.class);
        }
    }

    @Override
    public Images findImageById(String id) {
        Query query = Query.query(Criteria.where(Dict.IMAGEID).is(id));
        return  mongoTemplate.findOne(query, Images.class);
    }

    @Override
    public Images findDefaultImage() {
        Query query = Query.query(Criteria.where(Dict.NAME).is(Constants.IMAGE_BASE));
        return  mongoTemplate.findOne(query, Images.class);
    }

    @Override
    public PipelineDraft readDraftByAuthor(String authorID) {
        Query query = Query.query(Criteria.where(Dict.AUTHORID).is(authorID));
        return mongoTemplate.findOne(query, PipelineDraft.class);
    }

    @Override
    public long updateFollowStatus(String pipelineId, User user) {
        Query query = Query.query(Criteria.where(Dict.PIPELINEID).is(pipelineId));
        Pipeline pipeline = mongoTemplate.findOne(query,Pipeline.class);
        List<String>  users = null;
        if(!CommonUtils.isNullOrEmpty(pipeline)) {
            users = pipeline.getCollected(); //??????????????????????????????????????????
            if(CommonUtils.isNullOrEmpty(users)){ //??????????????????????????????
                users = new ArrayList<>();
                users.add(user.getId());
            }else{
                if(users.contains(user.getId())){
                    users.remove(user.getId());
                }else {
                    users.add(user.getId());
                }
            }
        }
        Update update = new Update().set(Dict.COLLECTED,users);
        UpdateResult updateResult = mongoTemplate.updateFirst(query,update,Pipeline.class);
        return Optional.ofNullable(updateResult.getMatchedCount()).orElse(0l);
    }


    @Override
    public List<Pipeline> queryAllPipeline() {
        Query query = new Query();
        List<Pipeline> pipelineList = mongoTemplate.find(query, Pipeline.class);
        return pipelineList;
    }

    @Override
    public Pipeline findActiveVersion(String nameId) {
        Query query = Query.query(Criteria.where(Dict.NAMEID).is(nameId).and(Dict.STATUS).is(Constants.STATUS_OPEN));
        query.with(new Sort(Sort.Direction.DESC, Dict.UPDATETIME));
        return mongoTemplate.findOne(query, Pipeline.class);
    }

    @Override
    public void updateBuildTime(Pipeline pipeline){
        Query query = Query.query(Criteria.where(Dict.PIPELINEID).is(pipeline.getId()));
        Update update = Update.update(Dict.BUILDTIME, pipeline.getBuildTime());
        update.set(Dict.UPDATETIME,pipeline.getUpdateTime());
        mongoTemplate.findAndModify(query, update, Pipeline.class);
    }

    @Override
    public Map<String, Object> findHistoryPipelineList(long skip, int limit, String nameId) {
        Map<String, Object> map = new HashMap<>();
        Query query = Query.query(Criteria.where(Dict.NAMEID).is(nameId).and(Dict.STATUS).is(Constants.STATUS_CLOSE));
        query.with(new Sort(Sort.Direction.DESC, Dict.UPDATETIME));
        long total = mongoTemplate.count(query, Pipeline.class);
        map.put(Dict.TOTAL, total);
        if(limit != 0) {
            query.skip(skip).limit(limit);
        }
        List<Pipeline> list = mongoTemplate.find(query, Pipeline.class);
        map.put(Dict.PIPELINELIST, Dict.LIST);
        return map;
    }

    /**
     * ?????????pipeline????????????????????????
     *
     * @param pluginId
     * @return
     */
    @Override
    public List<Pipeline> queryByPluginId(String pluginId) {
        //??????stages??????jobs??????steps??????plugin_info.id  is  pluginId
        Criteria pluginCriteria = new Criteria();
        //pluginCriteria.and("plugin_info.id").is(pluginId);
        pluginCriteria.and(Dict.PLUGININFO_PLUGINCODE).is(pluginId);
        Criteria stepCriteria = new Criteria();
        stepCriteria.and(Dict.STEPS).elemMatch(pluginCriteria);

        Criteria jobsCriteria = new Criteria();
        jobsCriteria.and(Dict.JOBS).elemMatch(stepCriteria);

        Criteria criteria = new Criteria();
        criteria.and(Dict.STAGES).elemMatch(jobsCriteria);

        Query query = new Query(criteria);
        List<Pipeline> pipeline = this.mongoTemplate.find(query, Pipeline.class, Dict.PIPELINE);
        return pipeline;
    }

    /**
     *
     * ??????????????????????????????
     * @return
     */
    @Override
    public List<Pipeline> querySchedulePipelines() {
        Criteria criteria = new Criteria();
        criteria.and(Dict.TRIGGERRULES_SCHEDULE_SWITCHFLAG).is(true);
        criteria.and(Dict.STATUS).is(Constants.ONE);
        Query query = new Query(criteria);
        List<Pipeline> pipelines = this.mongoTemplate.find(query, Pipeline.class, Dict.PIPELINE);
        return pipelines;
    }

    @Override
    public String updateTriggerRules(String pipelineId, TriggerRules triggerRules) throws Exception {
        Query query = Query.query(Criteria.where(Dict.PIPELINEID).is(pipelineId));
        Update update = Update.update(Dict.PIPELINEID, pipelineId);
        update.set(Dict.TRIGGERRULES, triggerRules);
        UpdateResult updateResult = mongoTemplate.updateFirst(query,update,Pipeline.class);
        long modifiedCount = updateResult.getModifiedCount();
        return modifiedCount>0 ? Constants.SUCCESS:Constants.FAIL;
    }

    /**
     * id?????????????????????status
     * nameId?????????status???1????????????
     *
     * @param param
     * @return
     */
    @Override
    public Pipeline queryPipelineByIdOrNameId(Map param) {
        String id = (String) param.get(Dict.ID);
        String nameId = (String) param.get(Dict.NAMEID);
        if (CommonUtils.isNullOrEmpty(id)) {
            if (CommonUtils.isNullOrEmpty(nameId)) {
                throw new FdevException(ErrorConstants.PARAMS_IS_ILLEGAL, new String[]{"nameId and id is null"});
            }
            return findActiveVersion(nameId);
        }else {
            return queryById(id);
        }
    }

    @Override
    public List queryByEntityId(Map request) {
        List<String> entities = (List) request.get(Dict.ENTITYIDS);
        //??????
        Integer pageSize = (Integer) request.get(Dict.PAGESIZE);
        Integer pageNum = (Integer) request.get(Dict.PAGE_NUM);
        List allList = new ArrayList();
        for (String entity : entities) {
            Criteria yamlConfigCriteria = new Criteria();
            yamlConfigCriteria.and(Dict.PARAMS+"."+Dict.ENTITY+".entityId").in(entity);
            Query yamlConfigQuery = new Query(yamlConfigCriteria);
            List<String> configIds = mongoTemplate.findDistinct(yamlConfigQuery, Dict.CONFIGID, YamlConfig.class, String.class);

            Map pipelineInfo = new HashMap();
            Criteria entityTemplateParamsMatch = new Criteria();
            entityTemplateParamsMatch.and(Dict.ENTITY__ID).in(entity);

            Criteria paramsMatch = new Criteria();
            paramsMatch.orOperator(
                    new Criteria().and(Dict.ENTITYTEMPLATEPARAMS).elemMatch(entityTemplateParamsMatch),
                    new Criteria().and(Dict.VALUE).in(configIds)
            );

            Criteria stepsMatch = new Criteria();
            stepsMatch.and(Dict.PLUGININFO__PARAMS).elemMatch(paramsMatch);

            Criteria jobsMatch = new Criteria();
            jobsMatch.and(Dict.STEPS).elemMatch(stepsMatch);

            Criteria stageMatch = new Criteria();
            stageMatch.and(Dict.JOBS).elemMatch(jobsMatch);

            Criteria criteria = new Criteria();
            criteria.and(Dict.STAGES).elemMatch(stageMatch);

            criteria.and(Dict.STATUS).is(Constants.ONE);

            Query query = new Query(criteria);
            long count = this.mongoTemplate.count(query, Pipeline.class);
            if (pageNum != null && pageNum > 0) {
                //???0???????????????????????????????????????
                if ((pageSize == null || pageSize <= 0)) {
                    //???????????? ???????????????????????????????????????5
                    pageSize = 5;
                }
                query.skip((pageNum - 1) * pageSize).limit(pageSize);
            }
            List<Pipeline> pipelines = this.mongoTemplate.find(query, Pipeline.class, Dict.PIPELINE);
            List resultList = new ArrayList();
            for (Pipeline pipeline : pipelines) {
                Map resultMap = new HashMap();
                String pipelineId = pipeline.getId();
                String nameId = pipeline.getNameId();
                BindProject bindProject = pipeline.getBindProject();
                String pipelineName = pipeline.getName();
                Author author = pipeline.getAuthor();
                resultMap.put(Dict.PIPELINEID, pipelineId);
                resultMap.put(Dict.NAMEID, nameId);
                resultMap.put(Dict.BINDPROJECT, bindProject);
                resultMap.put(Dict.PIPELINENAME, pipelineName);
                resultMap.put(Dict.AUTHOR, author);
                resultList.add(resultMap);
            }
            pipelineInfo.put("pipelineList", resultList);
            pipelineInfo.put(Dict.COUNT, count);
            allList.add(pipelineInfo);
        }
        return allList;
    }

    /**
     * ??????nameId??????pipeline??????
     *
     * @param nameId
     * @return
     */
    @Override
    public List<Pipeline> queryPipelinesByNameId(String nameId) {
        Criteria criteria = new Criteria();
        criteria.and(Dict.NAMEID).is(nameId);
        Query query = new Query(criteria);
        query.with(new Sort(Sort.Direction.DESC, Dict.PIPELINEID));
        return this.mongoTemplate.find(query, Pipeline.class, Dict.PIPELINE);
    }

    @Override
    public List<Map> queryPipLookDigital() {
        Criteria criteria = new Criteria();
        criteria.and(Dict.STATUS).is("1");
        MatchOperation match = Aggregation.match(criteria);
        LookupOperation lookup = Aggregation.lookup("pipeline_exe", "pipelineId", "pipelineId", "exeInfo");
        AggregationOperation addOperation = new AggregationOperation() {
            @Override
            public Document toDocument(AggregationOperationContext aggregationOperationContext) {
                return new Document("$addFields", new Document("exeTotal", "$exeInfo.length"));
            }
        };

        Aggregation aggregation = Aggregation.newAggregation(match, lookup, addOperation);
        AggregationResults<Map> results = this.mongoTemplate.aggregate(aggregation,  Dict.PIPELINE, Map.class);
        List<Map> mappedResults = results.getMappedResults();
        return mappedResults;
    }
}
