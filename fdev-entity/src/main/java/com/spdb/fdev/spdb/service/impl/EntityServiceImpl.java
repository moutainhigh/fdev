package com.spdb.fdev.spdb.service.impl;


import com.spdb.fdev.base.dict.Constants;
import com.spdb.fdev.base.dict.Dict;
import com.spdb.fdev.base.dict.ErrorConstants;
import com.spdb.fdev.base.utils.CommonUtil;
import com.spdb.fdev.base.utils.SFTPClient;
import com.spdb.fdev.base.utils.TimeUtils;
import com.spdb.fdev.common.User;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.common.util.FdevUserCacheUtil;
import com.spdb.fdev.common.util.UserVerifyUtil;
import com.spdb.fdev.spdb.dao.*;
import com.spdb.fdev.spdb.entity.*;
import com.spdb.fdev.spdb.service.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author huyz
 * @description  IEntityService
 * @date 2021/5/7
 */
@Service
@RefreshScope
@SuppressWarnings("all")
public class EntityServiceImpl implements IEntityService {

	@Value("${gitlab.token}")
	private String token;
	@Value("${gitlab.application.file}")
	private String applicationFile;
	@Value("${gitlab.application.file.ci}")
	private String applicationFileCi;
	@Autowired
	private IEntityDao entityDao;
	@Autowired
    public UserVerifyUtil userVerifyUtil;
	@Autowired
    private IRestService restService;
	@Autowired
    IEntityTemplateService entityTemplateService;
	@Autowired
	IEnvDao envDao;
	@Autowired
    IEntityTemplateDao entityTemplateDao;
	@Autowired
	private OutsideTemplateDao outsideTemplateDao;
    @Value("${fdev.config.host.ip}")
    private String fdevConfigHostIp;
    @Value("${fdev.config.dir}")
    private String fdevConfigDir;
    @Value("${fdev.config.user}")
    private String fdevConfigUser;
    @Value("${fdev.config.password}")
    private String fdevConfigPassword;
    @Value("${fdev.application.properties.dir}")
    private String propertiesDir;
	@Autowired
	private IGitlabApiService gitlabApiService;
	@Autowired
	private IServiceConfigDao serviceConfigDao;
	@Autowired
	private IConfigFileService configFileService;
	@Autowired
	private FdevUserCacheUtil fdevUserCacheUtil;

	@Override
	public Map queryEntityModel(Map<String, Object> requestParam) throws Exception {
		//??????????????????????????????
		Map map = entityDao.queryEntityModel(requestParam);
		List<Entity> entityModelList = (List<Entity>) map.get(Dict.ENTITYMODELLIST);
		//?????????????????????
		if(!CommonUtil.isNullOrEmpty(entityModelList)) {

			for (Entity entity : entityModelList) {
				//??????????????????
				entity.setCreateUserName(entity.getCreateUser().getNameCn());
				entity.setUpdateUserName(entity.getUpdateUser().getNameCn());
				entity.setCreateUserId(entity.getCreateUser().getUserId());
				entity.setUpdateUserId(entity.getUpdateUser().getUserId());
				String templateId = entity.getTemplateId();
				//?????????????????? ????????????
				if(!CommonUtil.isNullOrEmpty(templateId)) {
					EntityTemplate entityTemplate = entityTemplateDao.queryById(templateId);
					entity.setTemplateName(entityTemplate.getNameCn());
					entity.setProperties(entityTemplate.getProperties());
				} else {
					//???????????????????????????
					entity.setTemplateName(Constants.EMPTYTEMPLATE);
				}
			}
		}
		map.put(Dict.ENTITYMODELLIST, entityModelList);
		return map;
	}

	@Override
	public Entity queryEntityModelDetail(String id) throws Exception {
		//??????ID??????????????????
		Entity entity = entityDao.queryEntityModelById(id);
		if(!CommonUtil.isNullOrEmpty(entity)) {
			String templateId = entity.getTemplateId();
			//????????????ID????????? ????????????????????????
			if(!CommonUtil.isNullOrEmpty(templateId)) {
				Map map = new HashMap();
				map.put(Dict.ID,templateId);
				List<Map<String, Object>> properties = entityTemplateService.queryById(map).getProperties();
				entity.setProperties(properties);
				String templateName = entityTemplateService.queryById(map).getNameCn();
				entity.setTemplateName(templateName);
			}
			//???????????????????????????
			entity.setPropertiesValue(CommonUtil.isNullOrEmpty(entity.getPropertiesValue()) ? new HashMap() : entity.getPropertiesValue());;
		}

		return entity;
	}

	@Override
	public Map<String, Boolean> checkEntityModel(Map<String, Object> requestParam) throws Exception {
		Map map = new HashMap();
		Map<String, Object> nameEnMap = new HashMap();
		nameEnMap.put(Dict.NAMEEN, requestParam.get(Dict.NAMEEN));//???????????????
		//????????????????????????????????? ????????????true ???????????????false nameEn????????????false
		map.put(Dict.NAMEENFLAG, CommonUtil.isNullOrEmpty(requestParam.get(Dict.NAMEEN)) ? false :
								!CommonUtil.isNullOrEmpty(entityDao.queryOneEntityModel(nameEnMap)));
		Map<String, Object> nameCnMap = new HashMap();
		nameCnMap.put(Dict.NAMECN, requestParam.get(Dict.NAMECN));//???????????????
		//????????????????????????????????? ????????????true ???????????????false nameCn????????????false
		map.put(Dict.NAMECNFLAG, CommonUtil.isNullOrEmpty(requestParam.get(Dict.NAMECN)) ? false :
								!CommonUtil.isNullOrEmpty(entityDao.queryOneEntityModel(nameCnMap)));
		return map;
	}


	@Override
	public Map<String,String> addEntityModel(Map<String, Object> requestParam) throws Exception {
		Map<String,String> map = new HashMap<String,String>();
		if(!checkEntityModel(requestParam).get(Dict.NAMEENFLAG) && !checkEntityModel(requestParam).get(Dict.NAMECNFLAG)) {
			//????????????
			Entity entity = getEntity(requestParam);
			entityDao.addEntityModel(entity);
			//????????????
			addEntityLog(entity,new ArrayList<>(),"??????");

			//??????????????????
			Map<String, Object> editHistoryDetailsMap = new  HashMap<String, Object>();
			String templateId = entity.getTemplateId();
			//??????????????????
			if(!CommonUtil.isNullOrEmpty(templateId)) {
				EntityTemplate entityTemplate = entityTemplateDao.queryById(templateId);
				entity.setProperties(entityTemplate.getProperties());
			}
            editHistoryDetailsMap.put(Dict.ENTITYID, entity.getId());//??????ID
            editHistoryDetailsMap.put(Dict.ENTITYNAMEEN, entity.getNameEn());//???????????????
            editHistoryDetailsMap.put(Dict.ENTITYNAMECN, entity.getNameCn());//???????????????
            editHistoryDetailsMap.put(Dict.OPERATETYPE, "1");//0-?????????1-?????????2-??????
            editHistoryDetailsMap.put(Dict.FIELDS, entity.getProperties());//????????????
            editHistoryDetailsMap.put(Dict.BEFORE, new HashMap());//?????? ??????
            editHistoryDetailsMap.put(Dict.AFTER, (Map) requestParam.get(Dict.PROPERTIESVALUE));//???????????????
            editHistoryDetailsMap.put(Dict.ENVNAME, (String) requestParam.get(Dict.ENVNAME));//?????????
            editHistoryDetailsMap.put(Dict.DESC, (String) requestParam.get(Dict.DESC));//??????
			editHistoryDetails(editHistoryDetailsMap);
			map.put( Dict.ID , entity.getId());
		}
		return map;
	}

	@Override
	public Map<String, String> updateEntityModel(Map<String, Object> requestParam) throws Exception {
		String id = (String) requestParam.get(Dict.ID);//??????ID
		String nameEn = (String) requestParam.get(Dict.NAMEEN);//???????????????
		String nameCn = (String) requestParam.get(Dict.NAMECN);//???????????????
		List<Map<String,Object>> propertiesList = (List<Map<String,Object>>) requestParam.get(Dict.PROPERTIESLIST);//????????????
		//???????????????
		Entity oldEntity = entityDao.queryEntityModelById(id);
		String oldNameEnDependency = oldEntity.getNameEn();
		boolean dependency = queryDependency(oldNameEnDependency,null);//true?????????????????? false??????????????????
		List<String> contentList = new ArrayList<String>();

		//?????????????????????????????????
		if(!CommonUtil.isNullOrEmpty(nameEn)) {
			if(!nameEn.equals(oldEntity.getNameEn())){
				if(dependency){
					contentList.add("?????????????????????\"" + oldEntity.getNameEn() + "\"??????\"" + nameEn + "\".");
					oldEntity.setNameEn(nameEn);
				}else{
					throw new FdevException(ErrorConstants.ENT_UPDATE_ERROR,new String[]{"?????????????????????"} );
				}
			}
		}
		//?????????????????????????????????
		if(!CommonUtil.isNullOrEmpty(nameCn)) {
			if(!nameCn.equals(oldEntity.getNameCn())){
				contentList.add("?????????????????????\"" + oldEntity.getNameCn() + "\"??????\"" + nameCn + "\".");
				oldEntity.setNameCn(nameCn);
			}
		}

		//????????????
		if(!CommonUtil.isNullOrEmpty(propertiesList) && CommonUtil.isNullOrEmpty(oldEntity.getTemplateId()) ) {
			List<Map<String, Object>> newPropertiesList = new ArrayList<>();
			Map<String, Object> oldPropertiesValue = oldEntity.getPropertiesValue();
			List<String> fields = new ArrayList<String>();
			for( Map<String,Object> properties : propertiesList ){
				String oldNameEn = (String) properties.get(Dict.OLDNAMEEN);//??????????????????
				String oldNameCn = (String) properties.get(Dict.OLDNAMECN);//??????????????????
				Boolean oldRequired = null ;
				if(!CommonUtil.isNullOrEmpty(properties.get(Dict.OLDREQUIRED))){
					oldRequired = (Boolean) properties.get(Dict.OLDREQUIRED);//??????????????? true=???
				}
				String newNameEn = (String) properties.get(Dict.NEWNAMEEN);//??????????????????
				String newNameCn = (String) properties.get(Dict.NEWNAMECN);//??????????????????
				Boolean newRequired = null ;
				if(!CommonUtil.isNullOrEmpty(properties.get(Dict.NEWREQUIRED))){
					newRequired = (Boolean) properties.get(Dict.NEWREQUIRED);//??????????????? true=???
				}
				if( !CommonUtil.isNullOrEmpty(newNameEn) && !CommonUtil.isNullOrEmpty(oldNameEn) && !newNameEn.equals(oldNameEn) ){
					//????????????????????????
					fields.add(oldNameEn);
					contentList.add("?????????\"" +oldNameEn + "\"????????????\""+ oldNameEn + "\"??????\"" + newNameEn + "\".");//????????????
					//??????propertiesValue
					updatePropertiesValue(oldPropertiesValue,oldNameEn,newNameEn);
				} else if(!CommonUtil.isNullOrEmpty(newNameEn) && CommonUtil.isNullOrEmpty(oldNameEn) ){
					contentList.add("????????????\"" +newNameEn + "\".");//???????????? ????????????
				} else if( CommonUtil.isNullOrEmpty(newNameEn) && !CommonUtil.isNullOrEmpty(oldNameEn) ) {
					contentList.add("????????????\"" + oldNameEn + "\".");//???????????? ????????????
					//??????propertiesValue ????????????
					updatePropertiesValue(oldPropertiesValue,oldNameEn,newNameEn);
				}
				//?????????????????????????????? //????????????
				if( !CommonUtil.isNullOrEmpty(newNameCn) && !CommonUtil.isNullOrEmpty(oldNameCn) && !newNameCn.equals(oldNameCn) )
					contentList.add("?????????\"" +oldNameEn + "\"????????????\""+ oldNameCn + "\"??????\"" + newNameCn + "\".");

				//????????????????????????????????? //????????????
				if( !CommonUtil.isNullOrEmpty(newRequired) && !CommonUtil.isNullOrEmpty(oldRequired) && !newRequired.equals(oldRequired) )
					contentList.add("?????????\"" +oldNameEn + "\"???\"" + ( oldRequired ? "??????": "?????????") + "\"??????\"" + ( newRequired ? "??????": "?????????") + "\".");

				if( !CommonUtil.isNullOrEmpty(newNameCn) && !CommonUtil.isNullOrEmpty(newRequired) && !CommonUtil.isNullOrEmpty(newNameEn) ){
					Map<String, Object> newPropertiesMap = new HashMap<>();
					newPropertiesMap.put(Dict.TYPE,"string");//??????????????????string
					newPropertiesMap.put(Dict.NAMEEN,newNameEn);
					newPropertiesMap.put(Dict.NAMECN,newNameCn);
					newPropertiesMap.put(Dict.REQUIRED,newRequired);
					newPropertiesList.add(newPropertiesMap);
				}
			}
			//??????????????????????????? ????????????
			if( !CommonUtil.isNullOrEmpty(fields) && !queryDependency(oldNameEnDependency,fields)) {
				throw new FdevException(ErrorConstants.ENT_UPDATE_ERROR,new String[]{"??????????????????"} );
			}
			oldEntity.setProperties(newPropertiesList);
			oldEntity.setPropertiesValue(oldPropertiesValue);
		}
		//??????????????????????????????
		oldEntity.setUpdateUser(getEntityUser());
		oldEntity.setUpdateTime(TimeUtils.formatToday());
		//??????????????????
		entityDao.updateEntityModel(oldEntity);
		if(!CommonUtil.isNullOrEmpty(contentList)) {
			//????????????
			addEntityLog(oldEntity,contentList,"??????");
		}

		Map<String,String> map = new HashMap<String,String>();
		map.put( Dict.ID , oldEntity.getId() );
		return map;
	}

	//????????????????????????
	public void addEntityLog(Entity entity,List<String> contentList,String updateType ) throws Exception {
		User user = userVerifyUtil.getRedisUser();//??????????????????
		EntityLog entityLog = new EntityLog();
		entityLog.setEntityId(entity.getId());
		entityLog.setUpdateUserId(user.getId());
		entityLog.setUpdateUserName(user.getUser_name_cn());
		entityLog.setUpdateTime(TimeUtils.formatToday());
		entityLog.setUpdateType(updateType);
		entityLog.setContent(contentList);
		entityDao.addEntityLog(entityLog);
	}

	//???????????? ????????????
	private void updatePropertiesValue(Map<String, Object> oldPropertiesValue, String oldNameEn, String newNameEn) {
		if(!CommonUtil.isNullOrEmpty(oldPropertiesValue)){
			//??????????????????key
			Set<String> envSet = oldPropertiesValue.keySet();
			for(String env : envSet){
				Map envMap = (Map)oldPropertiesValue.get(env);
				if(!CommonUtil.isNullOrEmpty(envMap)){
					//????????????????????????key
					Set<String> envNameSet = envMap.keySet();
					for(String envName : envNameSet){
						//????????????????????????????????????
						Map envNameMap = (Map)envMap.get(envName);
						if(!CommonUtil.isNullOrEmpty(newNameEn)){
							if(envNameMap.keySet().contains(oldNameEn)) {
								if(!CommonUtil.isNullOrEmpty(envNameMap.get(oldNameEn))) {
									envNameMap.put(newNameEn, envNameMap.get(oldNameEn));
								}
							}
						}
						envNameMap.remove(oldNameEn);
					}
				}
			}
		}
	}


	public boolean queryDependency(String entityNameEn,List<String> fields) throws Exception {
		Map<String, Object> requestParam = new HashMap<>();
		requestParam.put(Dict.ENTITYNAMEEN,entityNameEn);
		requestParam.put(Dict.FIELDS,fields);
		//???????????????????????????
		Long count = serviceConfigDao.queryServiceConfigCount(requestParam);
		//???????????????????????????
		Map<String, Object> deployDependency = configFileService.queryDeployDependency(requestParam);
		if(count > 0 || (Integer) deployDependency.get(Dict.COUNT) > 0){
			return false;
		}
		return true;
	}

	@Override
	public Map<String, Object> getVariablesValue(List<String> envNames, List<String> variablesKeys) throws Exception {
		Map<String, Object> returnMap = new HashMap<String, Object>();
		// ???variablesKeys????????????????????????
        if (CommonUtil.isNullOrEmpty(variablesKeys)){
        	return returnMap;
        }
        Map<String, Object> normal = new HashMap<String, Object>();
        Map<String, Object> error = new HashMap<String, Object>();
        for (String envName : envNames) {
        	List<String> errorList = new ArrayList<String>();
        	Map<String, Object> map = new HashMap<String, Object>();
        	//??????????????????
    		Map<String, Object>  envMap = new HashMap();
            envMap.put(Dict.NAMEEN,envName);
      	    Env	env = envDao.queryEnv(envMap);
        	if(!CommonUtil.isNullOrEmpty(env)) {
            	//????????????type
                String envType = env.getType();
                Map<String, Object> queryEntityMap = new HashMap<String, Object>();
                for (String variablesKey : variablesKeys) {
                	// ?????????  ?????????
                	String[] variablesKeyList= variablesKey.split("\\.");
                	queryEntityMap.put(Dict.NAMEEN, variablesKeyList[0]);
                	//?????????
                	Object value = "";
                    //????????????
            		Entity entity = entityDao.queryOneEntityModel(queryEntityMap);
            		//???????????????????????? ????????????
    				Boolean required = true ;
            		if(!CommonUtil.isNullOrEmpty(entity) && variablesKeyList.length >= 2 ) {
            			//?????????
                    	String key = variablesKeyList[1];
            			List<Map<String, Object>> properties = entity.getProperties();
                		String templateId = entity.getTemplateId();
        				//?????????????????? ????????????
        				if(!CommonUtil.isNullOrEmpty(templateId)) {
        					EntityTemplate entityTemplate = entityTemplateDao.queryById(templateId);
        					properties = entityTemplate.getProperties();
        				}
        				for (Map<String, Object> propertiesMap : properties) {
    						if(key.equals(propertiesMap.get(Dict.NAMEEN))) {
    							required = CommonUtil.isNullOrEmpty(propertiesMap.get(Dict.REQUIRED)) ? false : (Boolean)propertiesMap.get(Dict.REQUIRED);
    						}
    					}
            			//??????????????????type?????????
            			Map propertiesValue = (Map) entity.getPropertiesValue();
            			if(!CommonUtil.isNullOrEmpty(propertiesValue)) {
            				Map envTypeMap = (Map)propertiesValue.get(envType);
                    		if(!CommonUtil.isNullOrEmpty(envTypeMap)) {
                    			//???????????????????????????
                    			Map envValue = (Map) envTypeMap.get(envName);
                    			if(!CommonUtil.isNullOrEmpty(envValue)) {
                    				value = envValue.get(key);
                    			}
                    		}
            			}
            		}
            		if(required && CommonUtil.isNullOrEmpty(value)) errorList.add(variablesKey);
            		else map.put(variablesKey,value);
                }

        	}else errorList = variablesKeys ;
        	normal.put(envName, map);
        	if(!CommonUtil.isNullOrEmpty(errorList)) {
           	 error.put(envName, errorList);
           }
        }
        returnMap.put(Dict.NORMAL, normal);
        returnMap.put(Dict.ERROR, error);
		return returnMap;
	}

	//??????Entity??????
	public Entity getEntity(Map<String, Object> requestParam) throws Exception {
        String nameEn = (String) requestParam.get(Dict.NAMEEN);//???????????????
        String nameCn = (String) requestParam.get(Dict.NAMECN);//???????????????
        String templateId = (String) requestParam.get(Dict.TEMPLATEID);//????????????
        String envType = (String) requestParam.get(Dict.ENVTYPE);//??????
        String envName = (String) requestParam.get(Dict.ENVNAME);//????????????
        Map propertiesValue = (Map) requestParam.get(Dict.PROPERTIESVALUE);//???????????????
		ObjectId objectId = new ObjectId();
        String id = objectId.toString();
		Entity entity = new Entity();
		entity.set_id(objectId);
        entity.setId(id);
		entity.setNameEn(nameEn);//???????????????
		entity.setNameCn(nameCn);//???????????????
		entity.setCreateUser(getEntityUser());//?????????
		entity.setCreateTime(TimeUtils.formatToday());//????????????
		entity.setUpdateUser(getEntityUser());//???????????????
		entity.setUpdateTime(TimeUtils.formatToday());//????????????
		if(!CommonUtil.isNullOrEmpty(templateId)) {
			entity.setTemplateId(templateId);//????????????
		}else {
			List<Map<String, Object>> properties = (List<Map<String, Object>>) requestParam.get(Dict.PROPERTIES);
			if(!CommonUtil.isNullOrEmpty(properties)) {
				//???????????????????????????????????????
				checkProperties(properties);
				entity.setProperties(properties);
			}
		}
		if(!CommonUtil.isNullOrEmpty(envName)&&
			!CommonUtil.isNullOrEmpty(envType)&&
			!CommonUtil.isNullOrEmpty(propertiesValue)) {
			//?????????????????? ???????????????
			Map envNameMap = new HashMap();
			envNameMap.put(envName, propertiesValue);
			Map envTypeMap = new HashMap();
			envTypeMap.put(envType, envNameMap);
			entity.setPropertiesValue(envTypeMap);
			//??????????????????
			checkPropertiesValue(entity,propertiesValue);
		}
		return entity;
	}

	//???????????????????????????????????????
	private void checkProperties(List<Map<String, Object>> properties) throws Exception {
		List<String> nameEnList = new ArrayList<String>();
		List<String> nameCnList = new ArrayList<String>();
		for (Map<String, Object> map : properties) {
			String nameEn = (String) map.get(Dict.NAMEEN);
			String nameCn = (String) map.get(Dict.NAMECN);
			if(nameEnList.contains(nameEn)) {
				throw new FdevException(ErrorConstants.PROPERTIES_NAMEEN_ERROR, new String[]{nameEn});
			}else {
				nameEnList.add(nameEn);
			}
			if(nameCnList.contains(nameCn)) {
				throw new FdevException(ErrorConstants.PROPERTIES_NAMECN_ERROR, new String[]{nameCn});
			}else {
				nameCnList.add(nameCn);
			}
		}
	}

	public void checkPropertiesValue(Entity entity , Map propertiesValue) throws Exception {
		List<Map<String, Object>> propertiesList = new ArrayList<Map<String, Object>>();
		if(!CommonUtil.isNullOrEmpty(entity.getTemplateId())){
			EntityTemplate entityTemplate = entityTemplateDao.queryById(entity.getTemplateId());
			propertiesList = entityTemplate.getProperties();
		}else if(!CommonUtil.isNullOrEmpty(entity.getProperties())) {
			propertiesList = entity.getProperties();
		}
		for( Map<String, Object> properties : propertiesList ) {
			if(Constants.STRING.equals(properties.get(Dict.TYPE))) {//??????????????????
				if(((Boolean) properties.get(Dict.REQUIRED)).booleanValue()) {
					if(CommonUtil.isNullOrEmpty(propertiesValue.get(properties.get(Dict.NAMEEN)))) {
						throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{(String) properties.get(Dict.NAMECN)});
					}
				}
			}
		}
	}



	@Override
	public String addEntityClass(Map<String, Object> requestParam) throws Exception {
		String id = (String) requestParam.get(Dict.ID);//ID
        String envType = (String) requestParam.get(Dict.ENVTYPE);//??????
        String envName = (String) requestParam.get(Dict.ENVNAME);//????????????
        Map propertiesValue = (Map) requestParam.get(Dict.PROPERTIESVALUE);//???????????????
		List<Map<String, String>> envs = (List)requestParam.get("envs");//?????????????????????????????????
		//???????????????
		Entity entity = entityDao.queryEntityModelById(id);
		//??????????????????
      	checkPropertiesValue(entity,propertiesValue);
		Map oldPropertiesValue = entity.getPropertiesValue();
		if(CommonUtil.isNullOrEmpty(envs) && !CommonUtil.isNullOrEmpty(envType) && !CommonUtil.isNullOrEmpty(envName)){
			envs = new ArrayList<>();
			Map envMap = new HashMap();
			envMap.put(Dict.ENVTYPE,envType);
			envMap.put(Dict.ENVNAME,envName);
			envs.add(envMap);
		}else if((CommonUtil.isNullOrEmpty(envType) || CommonUtil.isNullOrEmpty(envName)) && CommonUtil.isNullOrEmpty(envs)){
			throw new FdevException(ErrorConstants.PARAM_ERROR);
		}

		for(Map envMap:envs){
			envType = (String) envMap.get(Dict.ENVTYPE);
			envName = (String) envMap.get(Dict.ENVNAME);
			if(!CommonUtil.isNullOrEmpty(oldPropertiesValue)) {
				Map propertiesEnv = (Map) oldPropertiesValue.get(envType);
				if(!CommonUtil.isNullOrEmpty(propertiesEnv)) {
					propertiesEnv.put(envName, propertiesValue);
				} else {
					propertiesEnv = new HashMap();
					propertiesEnv.put(envName, propertiesValue);
				}
				oldPropertiesValue.put(envType, propertiesEnv);
				entity.setPropertiesValue(oldPropertiesValue);
			} else {
				//?????????????????? ???????????????
				Map envNameMap = new HashMap();
				envNameMap.put(envName, propertiesValue);
				Map envTypeMap = new HashMap();
				envTypeMap.put(envType, envNameMap);
				entity.setPropertiesValue(envTypeMap);
			}
			entity.setUpdateUser(getEntityUser());//???????????????
			entity.setUpdateTime(TimeUtils.formatToday());//????????????
			entityDao.updateEntityModel(entity);
			//??????????????????
			Map<String, Object> editHistoryDetailsMap = new  HashMap<String, Object>();
			String templateId = entity.getTemplateId();
			//??????????????????
			if(!CommonUtil.isNullOrEmpty(templateId)) {
				EntityTemplate entityTemplate = entityTemplateDao.queryById(templateId);
				entity.setProperties(entityTemplate.getProperties());
			}
			editHistoryDetailsMap.put(Dict.ENTITYID, id);//??????ID
			editHistoryDetailsMap.put(Dict.ENTITYNAMEEN, entity.getNameEn());//???????????????
			editHistoryDetailsMap.put(Dict.ENTITYNAMECN, entity.getNameCn());//???????????????
			editHistoryDetailsMap.put(Dict.OPERATETYPE, "1");//0-?????????1-?????????2-??????
			editHistoryDetailsMap.put(Dict.FIELDS, entity.getProperties());//????????????
			editHistoryDetailsMap.put(Dict.BEFORE, new HashMap());//?????? ??????
			editHistoryDetailsMap.put(Dict.AFTER, propertiesValue);//???????????????
			editHistoryDetailsMap.put(Dict.ENVNAME, envName);//?????????
			editHistoryDetailsMap.put(Dict.DESC, (String) requestParam.get(Dict.DESC));//??????
			editHistoryDetails(editHistoryDetailsMap);
		}
		return id;
	}

	@Override
	public String updateEntityClass(Map<String, Object> requestParam) throws Exception {
		String id = (String) requestParam.get(Dict.ID);//ID
        String envType = (String) requestParam.get(Dict.ENVTYPE);//??????
        String envName = (String) requestParam.get(Dict.ENVNAME);//????????????
        Map propertiesValue = (Map) requestParam.get(Dict.PROPERTIESVALUE);//???????????????
		//???????????????
		Entity entity = entityDao.queryEntityModelById(id);
		//??????????????????
      	checkPropertiesValue(entity,propertiesValue);
		Map oldPropertiesValue = entity.getPropertiesValue();
		Map propertiesEnv = (Map) oldPropertiesValue.get(envType);
		//?????????????????????
		Map oldPropertiesEnvName = (Map)propertiesEnv.get(envName);
		propertiesEnv.put(envName, propertiesValue);
		oldPropertiesValue.put(envType, propertiesEnv);
		entity.setPropertiesValue(oldPropertiesValue);
		entity.setUpdateUser(getEntityUser());//???????????????
		entity.setUpdateTime(TimeUtils.formatToday());//????????????
		entityDao.updateEntityModel(entity);
		String templateId = entity.getTemplateId();
		//??????????????????
		if(!CommonUtil.isNullOrEmpty(templateId)) {
			EntityTemplate entityTemplate = entityTemplateDao.queryById(templateId);
			entity.setProperties(entityTemplate.getProperties());
		}
		//??????????????????
		Map<String, Object> editHistoryDetailsMap = new  HashMap<String, Object>();
		editHistoryDetailsMap.put(Dict.ENTITYID, id);//??????ID
		editHistoryDetailsMap.put(Dict.ENTITYNAMEEN, entity.getNameEn());//???????????????
		editHistoryDetailsMap.put(Dict.ENTITYNAMECN, entity.getNameCn());//???????????????
		editHistoryDetailsMap.put(Dict.OPERATETYPE, "0");//0-?????????1-?????????2-??????
		editHistoryDetailsMap.put(Dict.FIELDS, entity.getProperties());//????????????
		editHistoryDetailsMap.put(Dict.BEFORE, oldPropertiesEnvName);//???????????????
		editHistoryDetailsMap.put(Dict.AFTER, propertiesValue);//???????????????
		editHistoryDetailsMap.put(Dict.ENVNAME, envName);//?????????
		editHistoryDetailsMap.put(Dict.DESC, (String) requestParam.get(Dict.DESC));//??????
		editHistoryDetails(editHistoryDetailsMap);
		return id;
	}

	@Override
	public String deleteEntityClass(Map<String, Object> requestParam) throws Exception {
		String id = (String) requestParam.get(Dict.ID);//ID
        String envType = (String) requestParam.get(Dict.ENVTYPE);//??????
        String envName = (String) requestParam.get(Dict.ENVNAME);//????????????
        //???????????????
        Entity entity = entityDao.queryEntityModelById(id);
        Map oldPropertiesValue = entity.getPropertiesValue();
        //???????????????????????????
        Map propertiesEnv = (Map) oldPropertiesValue.get(envType);
        //?????????????????????
        Map oldPropertiesEnvName = (Map)propertiesEnv.get(envName);
        //???????????????
        propertiesEnv.remove(envName);
        if(!CommonUtil.isNullOrEmpty(propertiesEnv)) {
        	oldPropertiesValue.put(envType, propertiesEnv);
        } else {
        	oldPropertiesValue.remove(envType);
        }
        //?????????????????????
        entity.setPropertiesValue(oldPropertiesValue);
        //??????
        entity.setUpdateUser(getEntityUser());//???????????????
		entity.setUpdateTime(TimeUtils.formatToday());//????????????
		entityDao.updateEntityModel(entity);
		String templateId = entity.getTemplateId();
		//??????????????????
		if(!CommonUtil.isNullOrEmpty(templateId)) {
			EntityTemplate entityTemplate = entityTemplateDao.queryById(templateId);
			entity.setProperties(entityTemplate.getProperties());
		}
		//??????????????????
		Map<String, Object> editHistoryDetailsMap = new  HashMap<String, Object>();
		editHistoryDetailsMap.put(Dict.ENTITYID, id);//??????ID
		editHistoryDetailsMap.put(Dict.ENTITYNAMEEN, entity.getNameEn());//???????????????
		editHistoryDetailsMap.put(Dict.ENTITYNAMECN, entity.getNameCn());//???????????????
		editHistoryDetailsMap.put(Dict.OPERATETYPE, "2");//0-?????????1-?????????2-??????
		editHistoryDetailsMap.put(Dict.FIELDS, entity.getProperties());//????????????
		editHistoryDetailsMap.put(Dict.BEFORE, oldPropertiesEnvName);//???????????????
		editHistoryDetailsMap.put(Dict.AFTER, new HashMap());//???????????????
		editHistoryDetailsMap.put(Dict.ENVNAME, envName);//?????????
		editHistoryDetailsMap.put(Dict.DESC, (String) requestParam.get(Dict.DESC));//??????
		editHistoryDetails(editHistoryDetailsMap);
		return id;
	}

	private EntityUser getEntityUser() throws Exception {
		EntityUser entityUser = new EntityUser();
		User user = userVerifyUtil.getRedisUser();
		entityUser.setUserId(user.getId());
		entityUser.setNameEn(user.getUser_name_en());
		entityUser.setNameCn(user.getUser_name_cn());
		return entityUser;
	}

	@Override
	public List<Entity> queryEntityMapping(List<String> entityIdList, String envNameEn) throws Exception {
		List<Entity> entityList = entityDao.queryEntityModelByIds(entityIdList);
		//??????????????????
		Env env = new Env();
		if(!CommonUtil.isNullOrEmpty(envNameEn)) {
			Map<String, Object>  envMap = new HashMap();
	        envMap.put(Dict.NAMEEN,envNameEn);
			env = envDao.queryEnv(envMap);
		}
		if (CommonUtil.isNullOrEmpty(env)) {
			throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"??????????????????", envNameEn});
		}
        String envType = env.getType();
		if(!CommonUtil.isNullOrEmpty(entityList)) {
			for(Entity entity : entityList) {
				Map<String, Object> propertiesValue = entity.getPropertiesValue();
				Map<String, Object> newPropertiesValue = new HashMap();
				if(!CommonUtil.isNullOrEmpty(envType)) {
					Map propertiesValueEnvName = (Map) propertiesValue.get(envType);
					if(!CommonUtil.isNullOrEmpty(propertiesValueEnvName))
					newPropertiesValue.put(envNameEn,propertiesValueEnvName.get(envNameEn));
				}else {
					Set<String> propertiesKeys= propertiesValue.keySet();
					for(String propertiesKey : propertiesKeys) {
						Map propertiesValueEnvName = (Map) propertiesValue.get(propertiesKey);
						newPropertiesValue.putAll(propertiesValueEnvName);
					}
				}
				entity.setPropertiesValue(newPropertiesValue);
			}
		}
		return entityList;
	}

	@Override
	public String queryConfigTemplate(Map<String, String> requestParam) throws Exception {
		// ??????id
		String projectId = requestParam.get(Dict.PROJECTID).trim();
		// feature ??????
		String featureBranch = requestParam.get(Dict.FEATUREBRANCH).trim();
		// ??????id ??????????????????
		Map<String, Object> app = restService.queryApp(projectId);
		String gitlabId = ((Integer) app.get(Dict.GITLAB_PROJECT_ID)).toString();
		this.gitlabApiService.checkBranch(this.token, gitlabId, featureBranch);
		String content = this.gitlabApiService.getFileContent(this.token, gitlabId, featureBranch, this.applicationFileCi);
		if(CommonUtil.isNullOrEmpty(content)) {
			 content = this.gitlabApiService.getFileContent(this.token, gitlabId, featureBranch, this.applicationFile);
		}
		return content ;
	}

	@Override
	public Map queryServiceEntity(Map<String, Object> requestParam) throws Exception {
		String id = (String) requestParam.get(Dict.ID);
		Integer page = (Integer) requestParam.get(Dict.PAGE);//??????
		Integer perPage = (Integer) requestParam.get(Dict.PERPAGE);//????????????
		String entityTemplateId = (String) requestParam.get(Dict.TEMPLATEID);//????????????
		//??????????????????????????????
		Map map = entityDao.queryServiceEntity(entityTemplateId,page,perPage);
		List<Entity> entityModelList = (List<Entity>) map.get(Dict.ENTITYMODELLIST);
		//?????????????????????
		if(!CommonUtil.isNullOrEmpty(entityModelList)) {
			for (Entity entity : entityModelList) {
				//??????????????????
				entity.setCreateUserName(entity.getCreateUser().getNameCn());
				entity.setUpdateUserName(entity.getUpdateUser().getNameCn());
				entity.setCreateUserId(entity.getCreateUser().getUserId());
				entity.setUpdateUserId(entity.getUpdateUser().getUserId());
				//????????????????????????
				String templateId = entity.getTemplateId();
				//????????????ID????????? ????????????????????????
				if(!CommonUtil.isNullOrEmpty(templateId)) {
					Map param = new HashMap();
					param.put(Dict.ID,templateId);
					EntityTemplate entityTemplate = entityTemplateService.queryById(param);
					entity.setProperties(entityTemplate.getProperties());
					//??????????????????
					entity.setTemplateName(entityTemplate.getNameCn());
				} else {
					//???????????????????????????
					entity.setTemplateName(Constants.EMPTYTEMPLATE);
				}
			}
		}
		map.put(Dict.ENTITYMODELLIST, entityModelList);
		return map;
	}

	@Override
	public Map<String, Object> previewConfigFile(Map<String, Object> requestParam) throws Exception {
		String envName = (String) requestParam.get(Dict.ENVNAME);
        String content = (String) requestParam.get(Dict.CONTENT);
        String projectId = (String) requestParam.get(Dict.PROJECTID);
        //??????????????????
		Map envMap = new HashMap();
	    envMap.put(Dict.NAMEEN,envName);
	    Env env = envDao.queryEnv(envMap);
	    if (CommonUtil.isNullOrEmpty(env)) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"??????????????????", envName});
        }
		// ???????????????????????????????????????????????????????????????????????????
		return replaceConfigFile(content,env,projectId);
	}

	@Override
	public String saveDevConfigProperties(Map<String, Object> requestParam) throws Exception {
		String projectId = (String) requestParam.get(Dict.PROJECTID);//??????id
        String content = (String) requestParam.get(Dict.CONTENT);//??????:?????????????????????????????????
        String envName = (String) requestParam.get(Dict.ENVNAME);//?????????
    	Map sitEnvMap = new HashMap();
    	sitEnvMap.put(Dict.NAMEEN, envName );
    	Env env = envDao.queryEnv(sitEnvMap);
        if (CommonUtil.isNullOrEmpty(env)) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"??????????????????", envName});
        }
        Map<String, Object> appInfo = restService.queryApp(projectId);
        String fileCont = (String) replaceConfigFile(content,env,projectId).get(Dict.CONTENT);
        Map<String, Object> devMap = new HashMap<>();
        devMap.put(Dict.PROJECTNAME, appInfo.get(Dict.NAMEENN));
        devMap.put(Dict.FDEVCONFIGHOSTIP, fdevConfigHostIp);
        devMap.put(Dict.FDEVCONFIGUSER, fdevConfigUser);
        devMap.put(Dict.FDEVCONFIGPASSWORD, fdevConfigPassword);
        devMap.put(Dict.FDEVCONFIGDIR, fdevConfigDir);
        String remotePath = pushConfigFile(devMap, fileCont, env.getNameEn());
        return remotePath + "???????????????Ip:" + devMap.get(Dict.FDEVCONFIGHOSTIP);
	}

	/**
     * ?????????????????????????????????
     *
     * @param requestParam
     * @param fileCont
     * @param envName
     * @return
     */
    private String pushConfigFile(Map<String, Object> requestParam, String fileCont, String envName) {
        // ????????????????????????
        String firstContentShowEnvName = Constants.CONFIG_FILE_FIRST_LINE + envName + "\n";
        fileCont = firstContentShowEnvName + fileCont;
        String ciProjectName = (String) requestParam.get(Dict.PROJECTNAME);
        String fileName = ciProjectName + "-fdev.properties";
        File file = new File(propertiesDir);
        if (!file.exists()) {
            file.mkdirs();
        }
        String fdevRuntimePath = propertiesDir + fileName;
        try (PrintWriter printWriter = new PrintWriter(fdevRuntimePath, StandardCharsets.UTF_8.name())) {
            printWriter.write(fileCont);
        } catch (IOException e) {
            throw new FdevException(ErrorConstants.APP_FILE_ERROR, new String[]{e.getMessage()});
        }
        // ??????????????????
        String fdevConfigHostIp = (String) requestParam.get(Dict.FDEVCONFIGHOSTIP);
        String fdevConfigUser = (String) requestParam.get(Dict.FDEVCONFIGUSER);
        String fdevConfigPassword = (String) requestParam.get(Dict.FDEVCONFIGPASSWORD);
        String fdevConfigDir = (String) requestParam.get(Dict.FDEVCONFIGDIR);
        String[] nameEnSplit = ciProjectName.split("-");
        String projectName = nameEnSplit[0];
        if (!fdevConfigDir.endsWith("/")) {
            fdevConfigDir += "/";
        }
        String remoteDirectory = fdevConfigDir + projectName + "/";
        SFTPClient sftpClient = new SFTPClient(fdevConfigHostIp, fdevConfigUser, fdevConfigPassword);
        sftpClient.pushConfig(remoteDirectory, fdevRuntimePath);
        return remoteDirectory;
    }
	/**
     * ???????????????????????????????????????????????????????????????????????????
     *
     * @param configFileContent
     * @return
	 * @throws Exception
     */
    public Map<String, Object> replaceConfigFile(String configFileContent, Env env ,String projectId) throws Exception {
    	String envType = env.getType();
    	String envName = env.getNameEn();
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, String> modelParam = new HashMap<>();
        // ?????????????????????appkey
        List<String> leftAppKey = new ArrayList<>();
        //??????????????????
        StringBuilder contentBuilder = new StringBuilder();

        Set<String> entityNameSet = new HashSet<>();
        Map<String,Entity> entityMap = new HashMap<String,Entity>();
        // ??????????????????
        configFileContent = configFileContent.replace("\r", "");
        String[] configFileContentSplit = configFileContent.split("\n");
        // ????????????????????????
        Map<String, String> appKeyMap = getAppKeyMap(projectId, env.getType());
        for (int i = 0; i < configFileContentSplit.length; i++) {
        	Integer lineNum = i + 1;
            String lineContent = configFileContentSplit[i];
            // ????????????
            if (lineContent.startsWith(Constants.NOTE_PLACEHOLDER)) {
            	contentBuilder.append(lineContent).append("\n");
                continue;
            }
            // ???????????????????????????
            String[] lineSplit = lineContent.split("=", 2);
            if (lineSplit.length <= 1) {
            	contentBuilder.append(lineContent).append("\n");
                continue;
            }
            // ?????????????????????appkey
            leftAppKey.add(lineSplit[0]);
            // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            String appKey = lineSplit[0];
            String outSideValue = appKeyMap.get(appKey);
            if (!CommonUtil.isNullOrEmpty(outSideValue)) {
                lineContent = appKey + "=" + outSideValue;
                contentBuilder.append(lineContent).append("\n");
                continue;
            }
            String rightContent = lineSplit[1];
            if (rightContent.contains("$<")) {
                String[] rightContentSplit = rightContent.split("\\$<");
                for (int k = 1; k < rightContentSplit.length; k++) {
                	String envValue = "";
                    String singleRightContentSplit = rightContentSplit[k];
                    if (singleRightContentSplit.contains(">")) {
                        String modelField = singleRightContentSplit.split(">", 2)[0];

                        if (!(StringUtils.isEmpty(modelField) || modelField.contains("$") || modelField.contains("<"))) {
                            String[] centerContentSplit = modelField.split("\\.");
                            // "$<"???">"??????????????????????????????????????????"."??????"."??????????????????????????????
                            if (!modelField.endsWith(".") && centerContentSplit.length == 2 && StringUtils.isNotEmpty(centerContentSplit[0]) && StringUtils.isNotEmpty(centerContentSplit[1])) {
                            	//?????????
                            	String entityName = centerContentSplit[0];
                            	Entity entity = new Entity();
                            	if(!entityNameSet.contains(entityName)) {
                            		Map<String, Object> entityQuery = new HashMap<String, Object>();
                            		entityQuery.put(Dict.NAMEEN, entityName);
                                	entity = entityDao.queryOneEntityModel(entityQuery);
                                	entityNameSet.add(entityName);
                                	entityMap.put(entityName, entity);
                            	}else {
                            		entity = entityMap.get(entityName);
                            	}
                            	if(!CommonUtil.isNullOrEmpty(entity)) {
                            		//??????????????????type?????????
                            		if(!CommonUtil.isNullOrEmpty(entity.getPropertiesValue())) {
                            			Map envTypeMap = (Map) entity.getPropertiesValue().get(envType);
                            			if(!CommonUtil.isNullOrEmpty(envTypeMap)) {
                                			//???????????????????????????
                                			Map envValueMap = (Map) envTypeMap.get(envName);
                                			if(!CommonUtil.isNullOrEmpty(envValueMap)) {

                                				// ???????????????????????????
                                				envValue = (String) envValueMap.get(centerContentSplit[1]);
                                			}
                                		}
                            		}
                            	}
                            }
                        }
                        if (CommonUtil.isNullOrEmpty(envValue)) {
                            envValue = Constants.ERROR;
                        }
                        modelParam.put(modelField, envValue);
                        rightContent = rightContent.replace("$<" + modelField + ">", envValue);
                    }
                }
                contentBuilder.append(appKey).append("=").append(rightContent).append("\n");
            } else {
                contentBuilder.append(lineContent).append("\n");
            }
        }
     // ???????????????appKey????????????
        Set<String> repeatKey = CommonUtil.checkRepeat(leftAppKey);
        if (CollectionUtils.isNotEmpty(repeatKey)) {
        	throw new FdevException(ErrorConstants.CONFIGFILE_KEY_ERROR, new String[]{repeatKey.toString()});
        }
        resultMap.put(Dict.CONTENT, contentBuilder.toString());
        resultMap.put(Dict.MODELPARAM, modelParam);
        resultMap.put(Dict.OUTSIDEPARAM, appKeyMap);
        return resultMap;
    }

    /**
     * ???????????????????????????????????????????????????????????????
     *
     * @param appId
     * @param envNameEn
     * @return
     */
    private Map<String, String> getAppKeyMap(String appId, String envNameEn) throws Exception {
        Map<String, String> appKeyMap = new HashMap<>();
        OutSideTemplate outSideTemplate = new OutSideTemplate();
        outSideTemplate.setProjectId(appId);
        List<OutSideTemplate> outSideTemplateList = this.outsideTemplateDao.query(outSideTemplate);
        if(!CommonUtil.isNullOrEmpty(outSideTemplateList)) {
        	outSideTemplate = outSideTemplateList.get(0);
        	if (outSideTemplate != null) {
                List<Map<String, String>> variableList = outSideTemplate.getVariables();
                for (Map<String, String> params : variableList) {
                    String appkey = params.get(Dict.APPKEY);
                    String value = params.get(Dict.VALUE);
                    if (envNameEn != null && envNameEn.equals(params.get(Dict.ENVNAME))) {
                        appKeyMap.put(appkey, value);
                    }
                }
            }
        }

        return appKeyMap;
    }

	@Override
	public Map querySectionEntity(Map<String, Object> requestParam) throws Exception {
		Integer page = (Integer) requestParam.get(Dict.PAGE);//??????
		Integer perPage = (Integer) requestParam.get(Dict.PERPAGE);//????????????
		String entityTemplateId = (String) requestParam.get(Dict.TEMPLATEID);//????????????
		//??????????????????????????????
		Map map = entityDao.queryServiceEntity(entityTemplateId,page,perPage);
		List<Entity> entityModelList = (List<Entity>) map.get(Dict.ENTITYMODELLIST);
		//?????????????????????
		if(!CommonUtil.isNullOrEmpty(entityModelList)) {
			for (Entity entity : entityModelList) {
				//??????????????????
				entity.setCreateUserName(entity.getCreateUser().getNameCn());
				entity.setUpdateUserName(entity.getUpdateUser().getNameCn());
				entity.setCreateUserId(entity.getCreateUser().getUserId());
				entity.setUpdateUserId(entity.getUpdateUser().getUserId());
				//????????????????????????
				String templateId = entity.getTemplateId();
				//????????????ID????????? ????????????????????????
				if(!CommonUtil.isNullOrEmpty(templateId)) {
					Map param = new HashMap();
					param.put(Dict.ID,templateId);
					EntityTemplate entityTemplate = entityTemplateService.queryById(param);
					entity.setProperties(entityTemplate.getProperties());
					//??????????????????
					entity.setTemplateName(entityTemplate.getNameCn());
				} else {
					//???????????????????????????
					entity.setTemplateName(Constants.EMPTYTEMPLATE);
				}
			}
		}
		map.put(Dict.ENTITYMODELLIST, entityModelList);
		return map;
	}

	@Override
	public Map<String, Object> getMappingHistoryList(Map<String, Object> requestParam) throws Exception {
		Map<String, Object> historyDetails = entityDao.queryHistoryDetails(requestParam);
		return historyDetails;
	}

	//???????????????????????? ??????
	public void editHistoryDetails(Map<String, Object> requestParam) throws Exception {
		User user = userVerifyUtil.getRedisUser();
		ObjectId objectId = new ObjectId();
        String id = objectId.toString();
		HistoryDetails historyDetails = new HistoryDetails();
        historyDetails.set_id(objectId);
        historyDetails.setId(id);
        historyDetails.setUpdateUserId(user.getId());
        historyDetails.setUpdateUserName(user.getUser_name_cn());
        historyDetails.setEntityId((String)requestParam.get(Dict.ENTITYID));
        historyDetails.setEntityNameEn((String)requestParam.get(Dict.ENTITYNAMEEN));
        historyDetails.setEntityNameCn((String)requestParam.get(Dict.ENTITYNAMECN));
        historyDetails.setOperateType((String)requestParam.get(Dict.OPERATETYPE));
        historyDetails.setFields((List<Map<String, Object>>)requestParam.get(Dict.FIELDS));
        historyDetails.setBefore((Map<String, Object>)requestParam.get(Dict.BEFORE));
        historyDetails.setAfter((Map<String, Object>)requestParam.get(Dict.AFTER));
        historyDetails.setUpdateTime(TimeUtils.formatToday());
        historyDetails.setEnvName((String)requestParam.get(Dict.ENVNAME));
        historyDetails.setDesc((String)requestParam.get(Dict.DESC));
        entityDao.addHistoryDetails(historyDetails);
	}

	@Override
	public void deleteEntity(Map<String, Object> requestParam) throws Exception {
    	String entityId = requestParam.get(Dict.ID).toString();
		Entity entity = entityDao.queryEntityModelById(entityId);
		String entityNameEn = entity.getNameEn();
		requestParam.put(Dict.ENTITYNAMEEN,entityNameEn);
		//???????????????????????????
		Long count = (Long) configFileService.queryConfigDependency(requestParam).get(Dict.COUNT);
		//???????????????????????????
		Map<String, Object> deployDependency = configFileService.queryDeployDependency(requestParam);
		if(count == 0 && (Integer) deployDependency.get(Dict.COUNT) == 0){
			entityDao.deleteEntity(entity);
			//???????????? ????????????
			addEntityLog(entity,new ArrayList<>(),"??????");
		}else {
			throw new FdevException(ErrorConstants.CON_NOT_DELETE);
		}
	}

	@Override
	public  Map<String, Object> queryEntityLog(Map<String, Object> requestParam) throws Exception {
		return entityDao.queryEntityLog(requestParam);
	}

	/**
	 * ????????????
	 * ?????????????????????????????????????????????????????????
	 * @param requestParam
	 * @return
	 */
	@Override
	public Map<String, String> copyEntity(Map<String, Object> requestParam) throws Exception {
		Entity newEntity = new Entity();
    	String nameEn = (String) requestParam.get(Dict.NAMEEN);
    	String nameCn = (String) requestParam.get(Dict.NAMECN);
    	String copyId = (String) requestParam.get(Dict.COPYID); //??????????????????id
		if(checkEntityModel(requestParam).get(Dict.NAMEENFLAG) || checkEntityModel(requestParam).get(Dict.NAMECNFLAG)){
			throw new FdevException(ErrorConstants.ENTITY_NAME_ERROR);
		}

		Entity oldEntity = entityDao.queryEntityModelById(copyId);
		//??????????????????
		newEntity.setNameEn(nameEn);
		newEntity.setNameCn(nameCn);
		User user = userVerifyUtil.getRedisUser();
		EntityUser entityUser = new EntityUser(user.getId(),user.getUser_name_en(),user.getUser_name_cn());
		newEntity.setCreateUser(entityUser);
		newEntity.setUpdateUser(entityUser);
		newEntity.setCreateTime(CommonUtil.formatDate("yyyy-MM-dd hh:mm:ss"));
		newEntity.setUpdateTime(CommonUtil.formatDate("yyyy-MM-dd hh:mm:ss"));

		List<Map<String, Object>> properties = (List<Map<String, Object>>)requestParam.get(Dict.PROPERTIES);
		List<Map<String, Object>> newProperties = new ArrayList<>();//????????????????????????
		Map<String, Object> newPropertiesValue = new HashMap<>();//?????????????????????
		//???????????????????????????
		Map<String, Object> oldPropertiesValue = oldEntity.getPropertiesValue();
		newEntity.setPropertiesValue(oldPropertiesValue);
		newPropertiesValue = oldPropertiesValue;
		if(CommonUtil.isNullOrEmpty(oldEntity.getTemplateId())){
			//??????????????????????????????????????????
			for(Map property:properties){
				Map newProperty = new HashMap();
				newProperty.put(Dict.NAMEEN,property.get(Dict.NEWNAMEEN));
				newProperty.put(Dict.NAMECN,property.get(Dict.NEWNAMECN));
				newProperty.put(Dict.REQUIRED,property.get(Dict.NEWREQUIRED));
				newProperty.put(Dict.TYPE,"string");
				if(CommonUtil.isNullOrEmpty(property.get(Dict.OLDNAMEEN)) && !CommonUtil.isNullOrEmpty(property.get(Dict.NEWNAMEEN))){
					newProperties.add(newProperty);
					//????????????--???????????????
				}else if(!CommonUtil.isNullOrEmpty(property.get(Dict.OLDNAMEEN)) && !CommonUtil.isNullOrEmpty(property.get(Dict.NEWNAMEEN)) && !property.get("newNameEn").equals(property.get("oldNameEn"))){
					newProperties.add(newProperty);
					//???????????????--????????????????????????key
					this.updatePropertiesValue(newPropertiesValue,(String) property.get(Dict.OLDNAMEEN),(String) property.get(Dict.NEWNAMEEN));
				}else if(!CommonUtil.isNullOrEmpty(property.get(Dict.NEWNAMEEN)) && property.get(Dict.NEWNAMEEN).equals(property.get(Dict.OLDNAMEEN))){
					newProperties.add(newProperty);
					//????????????--????????????????????????
				}else {
					//?????????????????????????????????null???--????????????????????????key???value
					this.updatePropertiesValue(newPropertiesValue,(String) property.get(Dict.OLDNAMEEN),(String) property.get(Dict.NEWNAMEEN));
				}
			}
			newEntity.setPropertiesValue(newPropertiesValue);
			newEntity.setProperties(newProperties);
		}else {
			//????????????????????????????????????????????????
			newEntity.setTemplateId(oldEntity.getTemplateId());
		}
		ObjectId objectId = new ObjectId();
		String id = objectId.toString();
		newEntity.set_id(objectId);
		newEntity.setId(id);
		entityDao.addEntityModel(newEntity);
		addEntityLog(newEntity,new ArrayList<>(),"??????");
		Map resultMap = new HashMap();
		resultMap.put(Dict.ID,id);
		return resultMap;
	}

	@Override
	public void deleteConfigDependency(Map<String, Object> requestParam) throws Exception {
		//??????????????????
		serviceConfigDao.deleteServiceConfig(requestParam);

		//???????????????????????????
		String serviceId = String.valueOf(requestParam.get(Dict.ID));
		Map<String, Object> map = new HashMap<String,Object>();
		map.put(Dict.SCOPEID, serviceId);
		Map<String , Object> entityMap = entityDao.queryEntityModel(map);
		if(!CommonUtil.isNullOrEmpty(entityMap.get(Dict.ENTITYMODELLIST))) {
			List<Entity> list = (List<Entity>) entityMap.get(Dict.ENTITYMODELLIST);
			for(Entity entity : list) {
				entityDao.deleteEntity(entity);
			}
		}

	}

}
