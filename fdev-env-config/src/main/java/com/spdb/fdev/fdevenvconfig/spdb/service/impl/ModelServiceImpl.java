package com.spdb.fdev.fdevenvconfig.spdb.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.fdevenvconfig.base.CommonUtils;
import com.spdb.fdev.fdevenvconfig.base.CommonValidate;
import com.spdb.fdev.fdevenvconfig.base.dict.Constants;
import com.spdb.fdev.fdevenvconfig.base.dict.Dict;
import com.spdb.fdev.fdevenvconfig.base.dict.ErrorConstants;
import com.spdb.fdev.fdevenvconfig.base.utils.DateUtil;
import com.spdb.fdev.fdevenvconfig.base.utils.ServiceUtil;
import com.spdb.fdev.fdevenvconfig.spdb.cache.IConfigFileCache;
import com.spdb.fdev.fdevenvconfig.spdb.dao.ICommonDao;
import com.spdb.fdev.fdevenvconfig.spdb.dao.IModelDao;
import com.spdb.fdev.fdevenvconfig.spdb.dao.IModelEnvDao;
import com.spdb.fdev.fdevenvconfig.spdb.dao.IModelTemplateDao;
import com.spdb.fdev.fdevenvconfig.spdb.entity.Model;
import com.spdb.fdev.fdevenvconfig.spdb.entity.ModelCategory;
import com.spdb.fdev.fdevenvconfig.spdb.entity.ModelTemplate;
import com.spdb.fdev.fdevenvconfig.spdb.entity.Scope;
import com.spdb.fdev.fdevenvconfig.spdb.notify.INotifyEventStrategy;
import com.spdb.fdev.fdevenvconfig.spdb.service.IModelService;
import com.spdb.fdev.fdevenvconfig.spdb.service.IVerifyCodeService;
import com.spdb.fdev.fdevenvconfig.spdb.service.JsonSchemaService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.swing.text.html.parser.Entity;
import java.util.*;

/**
 * @author csii_shenzy
 * @date 2019/7/5 13:20
 */
@Service
@RefreshScope
public class ModelServiceImpl implements IModelService {

    @Value("${fenvconfig.sendMail}")
    private boolean sendMail;
    @Value("${update.model.sendMail}")
    private boolean subSendMail;
    @Value("${delete.model.sendMail}")
    private boolean deleteModelSendMail;
    @Autowired
    private IModelDao modelDao;
    @Autowired
    private IModelEnvDao modelEnvDao;
    @Autowired
    private ICommonDao commonDao;
    @Resource(name = "UpdateModel")
    private INotifyEventStrategy notifyEventStrategy;
    @Autowired
    private ServiceUtil serviceUtil;
    @Autowired
    private JsonSchemaService jsonSchemaService;
    @Autowired
    private IVerifyCodeService verifyCodeService;
    @Autowired
    private IConfigFileCache configFileCache;
    @Autowired
    private IModelTemplateDao modelTemplateDao;

    private Logger logger = LoggerFactory.getLogger(ModelServiceImpl.class);

    @Override
    public List<Model> query(Model model) throws Exception {
        model.setStatus(Constants.STATUS_OPEN);
        List<Model> modelList = this.modelDao.query(model);
        //List<Map> modelMapList = joinTemplateName(modelList);
        return modelList;
    }

    @Override
    public Model add(Model model) throws Exception {
        //??????????????????ci???,????????????platform
        if ("ci".equals(model.getFirst_category())) {
            if (CommonUtils.isNullOrEmpty(model.getPlatform())) {
                String errorMsg = "??????ci?????????,????????????????????????";
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"platform", errorMsg});
            }
        }
        //??????type???first_category???second_category???suffix_name
        String nameEn = CommonValidate.validateRepeatParamPattern(model, new String[]{Constants.FIRST_CATEGORY, Constants.SECOND_CATEGORY, Constants.SUFFIX_NAME, Constants.TYPE});
        if (!nameEn.equals(model.getName_en())) {
            String errorMsg = "???????????????????????????????????????????????????";
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{errorMsg});
        }
        CommonValidate.validateRepeatParam(model, Constants.OR, new String[]{Constants.NAME_EN, Constants.NAME_CN}, Model.class, this.commonDao);
        CommonValidate.validateRepeatParam(model.getEnv_key(), Constants.NAME_EN);
        CommonValidate.validateRepeatParam(model.getEnv_key(), Constants.NAME_CN);
        //?????????????????????????????????????????????????????????
        if (!CommonUtils.isNullOrEmpty(model.getModel_template_id())) {
            if (!validateModelAndTemplate(model))
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"??????????????????????????????"});
        }
        model.setOpno(serviceUtil.getOpno());
        List<Object> envKey = model.getEnv_key();
        // ??????????????????getModelAttribute
        model.setEnv_key(getModelAttribute(envKey, nameEn));
        return this.modelDao.add(model);
    }

    /**
     * ???????????????????????????????????????
     * ?????????????????????
     * ???????????????Stirng
     * ?????????????????????
     *
     * @param model
     * @return
     */
    public boolean validateModelAndTemplate(Model model) {
        boolean flag = true;
        ModelTemplate template = modelTemplateDao.queryById(model.getModel_template_id());
        List<Object> modelEnvKeyList = (List<Object>) model.getEnv_key();
        List<Object> templateEnvKeyList = (List<Object>) template.getEnvKey();
        for (Object modelPropObj : modelEnvKeyList) {
            Map modelProp = (Map) modelPropObj;
            String propKey = (String) modelProp.get(Dict.PROP_KEY);
            if (propKey == null)
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"prop_key????????????"});
            int match = 0;             //????????????????????????
            for (Object templatePropObj : templateEnvKeyList) {
                Map templateProp = (Map) templatePropObj;
                if (propKey.equals((String) templateProp.get(Dict.PROP_KEY))) {
                    match = 1;  //???????????????
                    templateEnvKeyList.remove(templatePropObj);   //????????????
                    //?????????????????????String
                    if (!(CommonUtils.isNullOrEmpty(modelProp.get(Dict.DATA_TYPE)) || "String".equals(modelProp.get(Dict.DATA_TYPE))))
                        return false;
                    //?????????????????????
                    if (!"1".equals(modelProp.get(Dict.REQUIRE)))
                        return false;
                    break;
                }
            }
            if (match == 0)
                return false;
        }
        return flag;
    }

    @Override
    public Model update(Model model) throws Exception {
        //??????????????????ci???,????????????platform
        if ("ci".equals(model.getFirst_category())) {
            if (CommonUtils.isNullOrEmpty(model.getPlatform())) {
                String errorMsg = "??????ci?????????,????????????????????????";
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"platform", errorMsg});
            }
        }
        String nameEn = model.getName_en();
        model.setUtime(DateUtil.getDate(new Date(), DateUtil.DATETIME_COMPACT_FORMAT));
        // ????????????????????????
       // Model beforeModel = this.modelDao.queryById(model);
        // ???????????????????????????????????????
        //checkUseModelField(beforeModel, model);
      //  model.setVersion(CommonUtils.CompareGetVersion(model.getEnv_key(), beforeModel.getEnv_key(), beforeModel.getVersion()));
        List<Object> envKey = model.getEnv_key();
        // ??????????????????
        model.setEnv_key(getModelAttribute(envKey, nameEn));
        // ????????????????????????
        Model updateModel = this.modelDao.update(model);
        ObjectMapper objectMapper = new ObjectMapper();
        logger.info("@@@@@@ ???????????? ??????{}", objectMapper.writeValueAsString(updateModel));
        return updateModel;
    }

    @Override
    public void delete(Map map) throws Exception {
        String verfityCode = (String) map.get(Dict.VERFITYCODE);
        // ???????????????
        verifyCodeService.checkVerifyCode(verfityCode);
        // ????????????????????????
        Model model = new Model();
        model.setId((String) map.get(Dict.ID));
        Model beforeModel = this.modelDao.queryById(model);
        // ????????????????????????????????????
        checkUseModel(beforeModel.getName_en(), "");
        // ????????????
        String opno = serviceUtil.getOpno();
        model.setOpno(opno);
        model.setStatus(Constants.STATUS_LOSE);
        model.setUtime(DateUtil.getDate(new Date(), DateUtil.DATETIME_COMPACT_FORMAT));
        this.modelDao.delete(model);
        // ??????????????????
        if (sendMail && deleteModelSendMail) {
            HashMap<String, Object> parse = new HashMap<>();
            parse.put(Dict.BEFORE, beforeModel);
            parse.put(Dict.AFTER, null);
            parse.put(Dict.USER_ID, opno);
            this.notifyEventStrategy.doNotify(parse);
        }
        logger.info("@@@@@@ ???????????? ??????");
        this.modelEnvDao.deleteModelEnv(Constants.MODEL_ID, model.getId(), opno);
        logger.info("@@@@@@ ???????????????????????? ??????");
    }

    @Override
    public Model queryById(Model model) throws Exception {
        model.setStatus(Constants.STATUS_OPEN);
        return this.modelDao.queryById(model);
    }

    @Override
    public Model queryById(String id) {
        return modelDao.queryById(id);
    }

    @Override
    public Model getByNameEn(String nameEn) {
        return modelDao.getByNameEn(nameEn);
    }

    @Override
    public List<Model> queryByIdList(Set<String> modelIdList, String var, String value) {
        if (CommonUtils.isNullOrEmpty(value)) {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{var, value});
        }
        return this.modelDao.queryByIdList(modelIdList, var, value);
    }

    @Override
    public List<Model> queryFuzz(Map map) throws Exception {
        Map paramMap = new HashMap();
        // ????????????
        String term = (String) map.get(Constants.TERM);
        // ????????????
        String type = (String) map.get(Constants.TYPE);
        // ??????????????????
        String firstCategory = (String) map.get(Constants.FIRST_CATEGORY);
        // ??????????????????
        String secondCategory = (String) map.get(Constants.SECOND_CATEGORY);
        term = StringUtils.isBlank(term) ? "" : term;
        paramMap.put(Constants.TYPE, type);
        paramMap.put(Constants.FIRST_CATEGORY, firstCategory);
        paramMap.put(Constants.SECOND_CATEGORY, secondCategory);
        paramMap.put(Constants.NAME_EN, term);
        paramMap.put(Constants.NAME_CN, term);
        return modelDao.queryFuzz(paramMap, Constants.STATUS_OPEN, Model.class);
    }

    @Override
    public Map<String, Object> queryModelCategory() {
        // ????????????????????????
        ModelCategory modelCategory = modelDao.queryModelCateCategory();
        Scope scope = modelDao.queryScope();
        Map<String, Object> map = new HashMap();
        map.put(Constants.ID, modelCategory.getId());
        map.put(Constants.CATEGORY, modelCategory.getCategory());
        map.put(Constants.SCOPE, scope.getScope());
        map.put(Constants.CTIME, modelCategory.getCtime());
        map.put(Constants.UTIME, modelCategory.getUtime());
        map.put(Constants.OPNO, modelCategory.getOpno());
        return map;
    }

    @Override
    public Map queryModelVariables(Map requestMap) {
        String modelNameVariable = requestMap.get(Constants.NAME_EN).toString();
        String[] var = modelNameVariable.split("\\.");
        if (var.length <= 1) {
            throw new FdevException(ErrorConstants.PARAM_ERROR);
        }
        Model model = this.modelDao.queryVaribleForOne(var[0], var[1]);
        if (CommonUtils.isNullOrEmpty(model.getEnv_key())) {
            logger.warn(Constants.NAME_EN + "?????????");
            return null;
        }
        return (Map) model.getEnv_key().get(0);
    }

    @Override
    public List<Model> queryExcludePirvateModel(Model model) throws Exception {
        model.setStatus(Constants.STATUS_OPEN);
        String name_en = model.getName_en();
        model.setName_en("");
        List<Model> queryModel = this.modelDao.query(model);
        List<Model> newModel = new ArrayList<>();
        for (Model model1 : queryModel) {
            String name_en1 = model1.getName_en();
            if (!(name_en1.startsWith(Dict.PRIVATE) && !name_en1.endsWith(name_en))) {
                newModel.add(model1);
            }
        }
        return newModel;
    }

    /**
     * / ??????????????????????????????????????????????????????????????????????????????????????????json_schema??????
     *
     * @param envKey
     * @param nameEn
     */
    private List<Object> getModelAttribute(List<Object> envKey, String nameEn) throws Exception {
        List<Object> modelAttributeList = new ArrayList<>();
        for (Object envKeyObj : envKey) {
            Map<String, Object> map = (Map<String, Object>) envKeyObj;
            // ????????????id
            String id = (String) map.get(Constants.ID);
            if (CommonUtils.isNullOrEmpty(id)) {
                map.put(Constants.ID, new ObjectId().toString());
            }
            // ??????data_type
            String dataType = (String) map.get(Dict.DATA_TYPE);
            if (Dict.OBJECT.equals(dataType) || Dict.ARRAY.equals(dataType)) {
                String jsonSchema = (String) map.get(Dict.JSON_SCHEMA);
                String jsonSchemaId = (String) map.get(Dict.JSON_SCHEMA_ID);
                if (StringUtils.isEmpty(jsonSchema) && StringUtils.isEmpty(jsonSchemaId)) {
                    throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{Dict.JSON_SCHEMA + "???" + Dict.JSON_SCHEMA_ID, Constants.PARAM_CAN_NOT_NULL});
                }
                // ???json_schema_id????????????????????????json_schema
                if (StringUtils.isEmpty(jsonSchemaId)) {
                    // ??????json_schema?????????????????????json_schema?????????id
                    map.put(Dict.JSON_SCHEMA_ID, jsonSchemaService.saveJsonSchema(nameEn, map));
                } /*else {
                    jsonSchemaService.updateJsonSchema(map, serviceUtil.getOpno());
                }*/
            }
            // ?????????????????????????????????
            Map<String, Object> modelAttribute = new HashMap<>();
            modelAttribute.put(Dict.ID, map.get(Dict.ID));
            modelAttribute.put(Dict.NAME_EN, map.get(Dict.NAME_EN));
            modelAttribute.put(Dict.NAME_CN, map.get(Dict.NAME_CN));
            modelAttribute.put(Dict.DESC, map.get(Dict.DESC));
            modelAttribute.put(Dict.REQUIRE, map.get(Dict.REQUIRE));
            modelAttribute.put(Dict.TYPE, map.get(Dict.TYPE));
            modelAttribute.put(Dict.DATA_TYPE, map.get(Dict.DATA_TYPE));
            if(map.get(Dict.PROP_KEY)!=null){
                modelAttribute.put(Dict.PROP_KEY, map.get(Dict.PROP_KEY));
            }
            if (Dict.OBJECT.equals(dataType) || Dict.ARRAY.equals(dataType)) {
                modelAttribute.put(Dict.JSON_SCHEMA_ID, map.get(Dict.JSON_SCHEMA_ID));
            }
            modelAttributeList.add(modelAttribute);
        }
        return modelAttributeList;
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @param modelNameEn
     * @param fieldNameEn
     */
    @Override
    public void checkUseModel(String modelNameEn, String fieldNameEn) {
        Map<String, Object> requestParam = new HashMap<>();
        requestParam.put(Constants.MODEL_NAME_EN, modelNameEn);
        requestParam.put(Constants.FIELD_NAME_EN, fieldNameEn);
        List<String> range = new ArrayList<>();
        range.add(Dict.MASTER);
        range.add(Dict.SIT);
        range.add(Dict.RELEASE);
        requestParam.put(Constants.RANGE, range);
        List<Map> configDependencyList;
        try {
            configDependencyList = configFileCache.preQueryConfigDependency(requestParam);
        } catch (Exception e) {
            throw new FdevException(ErrorConstants.SERVER_ERROR, new String[]{"??????????????????????????????"});
        }
        if (CollectionUtils.isNotEmpty(configDependencyList)) {
            if (StringUtils.isEmpty(fieldNameEn)) {
                throw new FdevException(ErrorConstants.DELETE_MODEL_ERROR, new String[]{modelNameEn, "?????????????????????????????????????????????"});
            }
            throw new FdevException(ErrorConstants.UPDATE_MODEL_ERROR, new String[]{modelNameEn, "????????????" + fieldNameEn + "??????????????????????????????????????????"});
        }
    }

    @Override
    public Map pageQuery(Map map) {
        return this.modelDao.pageQuery(map);
    }

    @Override
    public List<Model> getModels(String type) {
        return modelDao.getModels(type);
    }

    @Override
    public List<Model> queryByNameEnSet(Set<String> modelNameEnSet, String status) {
        return modelDao.queryByNameEnSet(modelNameEnSet, status);
    }

    /**
     * ???????????????????????????
     *
     * @return
     * @throws Exception
     */
    @Override
    public List<Map> queryNoCiEnvKeyList() throws Exception {
        List<Model> modelList = this.configModel();
        List<Map> result = new ArrayList<>();
        for (Model model : modelList) {
            List<Map<String, Object>> envKeyList = objectListToMapList(model.getEnv_key());
            for (Map envKey : envKeyList) {
                Map map = new HashMap();
                map.put(Dict.NAMEEN, envKey.get(Dict.NAME_EN));
                map.put(Dict.NAMECN, envKey.get(Dict.NAME_CN));
                map.put(Dict.MODELID, model.getId());
                map.put(Dict.MODELNAMEEN, model.getName_en());
                map.put(Dict.MODELNAMECN, model.getName_cn());
                map.put(Dict.NAMEALL, model.getName_en() + "." + envKey.get(Dict.NAME_EN));
                result.add(map);
            }
        }
        return result;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param before
     * @param after
     */
    private void checkUseModelField(Model before, Model after) {
        List<Object> beforeEnvKeys = before.getEnv_key();
        List<Object> afterEnvKeys = after.getEnv_key();
        List<Map<String, Object>> beforeEnvKeyList = objectListToMapList(beforeEnvKeys);
        List<Map<String, Object>> afterEnvKeyList = objectListToMapList(afterEnvKeys);
        // ???????????????????????????????????????????????????
        Map<String, List<Map<String, Object>>> updateModel = getUpdateModel(beforeEnvKeyList, afterEnvKeyList);
        List<Map<String, Object>> removedKeyList = updateModel.get(Dict.DELETE);
        for (Map<String, Object> map : removedKeyList) {
            checkUseModel(before.getName_en(), (String) map.get(Dict.NAME_EN));
        }
    }

    private List<Map<String, Object>> objectListToMapList(List<Object> objectList) {
        List<Map<String, Object>> removedKeyMapList = new ArrayList<>();
        for (Object removedKey : objectList) {
            Map<String, Object> removedKeyMap = (Map<String, Object>) removedKey;
            removedKeyMapList.add(removedKeyMap);
        }
        return removedKeyMapList;
    }

    /**
     * ?????????????????????????????????????????????????????????????????????
     *
     * @param beforeEnvKeyList
     * @param afterEnvKeyList
     * @return
     */
    public Map<String, List<Map<String, Object>>> getUpdateModel(List<Map<String, Object>> beforeEnvKeyList, List<Map<String, Object>> afterEnvKeyList) {
        Map<String, List<Map<String, Object>>> updateModelMap = new HashMap<>();
        // ????????????list???????????????id???????????????beforeEnvKeyList??????????????????????????????afterEnvKeyList???????????????????????????
        Iterator<Map<String, Object>> beforeIterator = beforeEnvKeyList.iterator();
        while (beforeIterator.hasNext()) {
            Map<String, Object> beforeEnvKeyMap = beforeIterator.next();
            Iterator<Map<String, Object>> afterIterator = afterEnvKeyList.iterator();
            while (afterIterator.hasNext()) {
                Map<String, Object> afterEnvKeyMap = afterIterator.next();
                if (beforeEnvKeyMap.get(Dict.ID).equals(afterEnvKeyMap.get(Dict.ID))) {
                    beforeIterator.remove();
                    afterIterator.remove();
                }
            }
        }
        updateModelMap.put(Dict.DELETE, beforeEnvKeyList);
        updateModelMap.put(Dict.ADD, afterEnvKeyList);
        return updateModelMap;
    }

    /**
     * ??????????????????????????????
     *
     * @param modelList
     * @return
     */
    public List<Map> joinTemplateName(List<Model> modelList) {
        return modelDao.joinTemplateName(modelList);
    }

    /**
     * ?????????CI????????????
     *
     * @return
     * @throws Exception
     */
    public List<Model> configModel() throws Exception {
        Model queryModel = new Model();
        queryModel.setStatus(Constants.STATUS_OPEN); //?????????????????????1?????????
        List<Model> modelList = modelDao.query(queryModel);
        for (int i = 0; i < modelList.size(); ) {
            Model model = modelList.get(i);
            if (model.getFirst_category().equals(Dict.CI)) {
                modelList.remove(model);
                continue;
            }
            i++;
        }
        return modelList;
    }
}
