package com.spdb.fdev.fdevinterface.spdb.service.impl;

import com.spdb.fdev.common.util.Util;
import com.spdb.fdev.fdevinterface.base.dict.Dict;
import com.spdb.fdev.fdevinterface.base.utils.CommonUtil;
import com.spdb.fdev.fdevinterface.base.utils.FileUtil;
import com.spdb.fdev.fdevinterface.spdb.dao.EsbRelationDao;
import com.spdb.fdev.fdevinterface.spdb.dao.InterfaceDao;
import com.spdb.fdev.fdevinterface.spdb.dao.InterfaceRelationDao;
import com.spdb.fdev.fdevinterface.spdb.dto.Param;
import com.spdb.fdev.fdevinterface.spdb.entity.*;
import com.spdb.fdev.fdevinterface.spdb.service.InterfaceLazyInitService;
import com.spdb.fdev.fdevinterface.spdb.service.InterfaceService;
import com.spdb.fdev.fdevinterface.spdb.vo.InterfaceDetailShow;
import com.spdb.fdev.fdevinterface.spdb.vo.InterfaceParamShow;
import com.spdb.fdev.fdevinterface.spdb.vo.InterfaceShow;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;

@Service
public class InterfaceServiceImpl implements InterfaceService {
	@Resource
	private InterfaceDao interfaceDao;
	@Resource
	private InterfaceRelationDao interfaceRelationDao;
	@Resource
	private EsbRelationDao esbRelationDao;

	@Autowired
	private InterfaceLazyInitService interfaceLazyInitService;

	@Override
	public List<RestApi> getRestApiList(String appServiceId, String branchName) {
		return interfaceDao.getRestApiList(appServiceId, branchName);
	}

	@Override
	public void updateRestApiIsNewByIds(List<String> idList) {
		if (CollectionUtils.isNotEmpty(idList)) {
			interfaceDao.updateRestApiIsNewByIds(idList);
		}
	}

	@Override
	public void saveRestApiList(List<RestApi> restApiList) {
		interfaceDao.saveRestApiList(restApiList);
	}

	@Override
	public void updateRestApiRegister(List<RestApi> restApiList) {
		if (CollectionUtils.isNotEmpty(restApiList)) {
			interfaceDao.updateRestApiRegister(restApiList);
		}
	}

	@Override
	public List<String> getNotRegister(String serviceId, String branch) {
		List<String> notRegisterList = new ArrayList<>();
		List<RestApi> restApiList = interfaceDao.getNotRegister(serviceId, branch);
		for (RestApi restApi : restApiList) {
			notRegisterList.add(restApi.getTransId());
		}
		return notRegisterList;
	}

	@Override
	public void deleteRestData(Map params) {
		interfaceDao.deleteRestApi(params);
	}

	@Override
	public List<SoapApi> getSoapApiList(String appServiceId, String branchName) {
		return interfaceDao.getSoapApiList(appServiceId, branchName);
	}

	@Override
	public void updateSoapApiIsNewByIds(List<String> idList) {
		interfaceDao.updateSoapApiIsNewByIds(idList);
	}

	@Override
	public void saveSoapApiList(List<SoapApi> soapApiList) {
		interfaceDao.saveSoapApiList(soapApiList);
	}

	@Override
	public void deleteSoapData(Map params) {
		interfaceDao.deleteSoapApi(params);
	}

	@Override
	public Map showInterface(InterfaceParamShow interfaceParamShow) {
		if (Dict.SOAP.equals(interfaceParamShow.getInterfaceType())) {
			return getSoapApi(interfaceParamShow);
		} else {
			// ????????????REST?????????
			return getRestApi(interfaceParamShow);
		}

	}

	@Override
	public List<RestApi> getInterface() {
		return interfaceDao.getRestApi();
	}

	@Override
	public InterfaceDetailShow getInterfaceDetailById(String id, String interfaceType) {
		InterfaceDetailShow interfaceDetailShow = new InterfaceDetailShow();
		if (Dict.REST.equals(interfaceType)) {
			RestApi restApi = interfaceDao.getRestApiById(id);
			interfaceDetailShow = CommonUtil.convert(restApi, InterfaceDetailShow.class);
			interfaceDetailShow = getDescriptionAndRemark(interfaceDetailShow);
			interfaceDetailShow.setRelTransId(interfaceDetailShow.getTransId());
		}
		if (Dict.SOAP.equals(interfaceType)) {
			SoapRelation soapRelation = interfaceRelationDao.getSoapRelationById(id);
			if (Util.isNullOrEmpty(soapRelation)) {
				SoapApi soapApi = interfaceDao.getSoapApiById(id);
				interfaceDetailShow = CommonUtil.convert(soapApi, InterfaceDetailShow.class);
				interfaceDetailShow = getDescriptionAndRemark(interfaceDetailShow);
				interfaceDetailShow.setRelTransId(interfaceDetailShow.getTransId());
			} else {
				interfaceDetailShow = CommonUtil.convert(soapRelation, InterfaceDetailShow.class);
				interfaceDetailShow = getDescriptionAndRemark(interfaceDetailShow);
				interfaceDetailShow.setRelTransId(interfaceDetailShow.getTransId());
				// ????????????????????????
				getCommonServiceSoap(soapRelation, interfaceDetailShow);
			}
			// ??????ESB??????
			relatedEsb(interfaceDetailShow);
		}
		if (Dict.SOP.equals(interfaceType)) {
			SopRelation sopRelation = interfaceRelationDao.getSopRelationById(id);
			interfaceDetailShow = CommonUtil.convert(sopRelation, InterfaceDetailShow.class);
			interfaceDetailShow = getDescriptionAndRemark(interfaceDetailShow);
			interfaceDetailShow.setRelTransId(interfaceDetailShow.getTransId());
			relatedEsb(interfaceDetailShow);
		}

		return interfaceDetailShow;
	}

	/**
	 * ?????????????????????????????????
	 *
	 * @param param1
	 * @param param2
	 * @return
	 */
	private List<Param> getDescription(List<Param> param1, List<Param> param2) {
		List<Param> params = new ArrayList<>();
		for (Param param : param1) {
			if (!FileUtil.isNullOrEmpty(param.getParamList())) {
				for (Param pa : param2) {
                    if (param.getName() != null && param.getName().equals(pa.getName())) {
						param.setDescription(pa.getDescription());
						param.setRemark(pa.getRemark());
					}
					getDescription(param.getParamList(), pa.getParamList());
				}
			} else {
				if (!FileUtil.isNullOrEmpty(param2)) {
					for (Param pa : param2) {
                        if (param.getName() != null && param.getName().equals(pa.getName())) {
							param.setDescription(pa.getDescription());
							param.setRemark(pa.getRemark());
						}
					}
				}

			}
			params.add(param);
		}
		return params;

	}

	/**
	 * ???????????????????????????
	 *
	 * @param interfaceDetailShow
	 * @return
	 */
	private InterfaceDetailShow getDescriptionAndRemark(InterfaceDetailShow interfaceDetailShow) {
		String transId = interfaceDetailShow.getTransId();
		String serviceId = interfaceDetailShow.getServiceId();
		// ???????????????????????????????????????
		ParamDesciption paramDesciption = interfaceDao.getParamDescription(transId, serviceId,
				interfaceDetailShow.getInterfaceType());
		// ????????????????????????????????????
		if (!FileUtil.isNullOrEmpty(paramDesciption)) {
			List<Param> request = paramDesciption.getRequest();
			List<Param> response = paramDesciption.getResponse();
			List<Param> requestParam = interfaceDetailShow.getRequest();
			List<Param> responseParam = interfaceDetailShow.getResponse();
			if (!FileUtil.isNullOrEmpty(request)) {
				List<Param> requestParamNew = getDescription(requestParam, request);
				interfaceDetailShow.setRequest(requestParamNew);
			}
			if (!FileUtil.isNullOrEmpty(response)) {
				List<Param> responseParamNew = getDescription(responseParam, response);
				interfaceDetailShow.setResponse(responseParamNew);
			}
		}
		return interfaceDetailShow;
	}

	@Override
	public List<InterfaceDetailShow> getInterfaceVer(String id, String interfaceType) {
		List<InterfaceDetailShow> showList = new ArrayList<>();
		switch (interfaceType) {
		case Dict.REST:
			RestApi restApi = interfaceDao.getRestApiById(id);
			List<RestApi> restApiList = interfaceDao.getRestApiVer(restApi.getServiceId(), restApi.getTransId(),
					restApi.getBranch());
			for (RestApi api : restApiList) {
				InterfaceDetailShow interfaceDetailShow = CommonUtil.convert(api, InterfaceDetailShow.class);
				interfaceDetailShow = getDescriptionAndRemark(interfaceDetailShow);
				interfaceDetailShow.setRelTransId(interfaceDetailShow.getTransId());
				showList.add(interfaceDetailShow);
			}
			break;
		case Dict.SOAP:
			showList = getSoapDetail(id);
			break;
		case Dict.SOP:
			SopRelation sopRelation = interfaceRelationDao.getSopRelationById(id);
			if (Util.isNullOrEmpty(sopRelation)) {
				break;
			}
			List<SopRelation> sopRelationList = interfaceRelationDao.getSopRelationVer(sopRelation.getServiceCalling(),
					sopRelation.getInterfaceAlias(), sopRelation.getBranch());
			for (SopRelation api : sopRelationList) {
				InterfaceDetailShow interfaceDetailShow = CommonUtil.convert(api, InterfaceDetailShow.class);
				interfaceDetailShow = getDescriptionAndRemark(interfaceDetailShow);
				interfaceDetailShow.setRelTransId(interfaceDetailShow.getTransId());
				interfaceDetailShow.setReqHeader(new ArrayList<>());
				interfaceDetailShow.setRspHeader(new ArrayList<>());
				// ??????ESB??????
				relatedEsb(interfaceDetailShow);
				showList.add(interfaceDetailShow);
			}
			break;
		default:
			break;
		}
		return showList;
	}

	private Map getRestApi(InterfaceParamShow interfaceParamShow) {
		Map interfaceMap = new HashMap();
		List<InterfaceShow> interfaceShowList = new ArrayList<>();
		Map restMap = interfaceDao.showRestApi(interfaceParamShow);
		List<RestApi> restApiList = (List<RestApi>) restMap.get(Dict.LIST);
		for (RestApi restApi : restApiList) {
			InterfaceShow interfaceShow = CommonUtil.convert(restApi, InterfaceShow.class);
			// ?????????????????????Id
			interfaceShow.setAppId(interfaceLazyInitService.getAppIdByName(restApi.getServiceId()));
			interfaceShowList.add(interfaceShow);
		}
		interfaceMap.put(Dict.TOTAL, restMap.get(Dict.TOTAL));
		interfaceMap.put(Dict.LIST, interfaceShowList);
		return interfaceMap;
	}

	private Map getSoapApi(InterfaceParamShow interfaceParamShow) {
		Map interfaceMap = new HashMap();
		List<InterfaceShow> interfaceShowList = new ArrayList<>();
		Map soapMap = interfaceDao.showSoapApi(interfaceParamShow);
		List<Map<String, Object>> soapApiList = (List<Map<String, Object>>) soapMap.get(Dict.LIST);
		List<InterfaceShow> updateNameList = new ArrayList<>();
		for (Map<String, Object> soapApi : soapApiList) {
			InterfaceShow interfaceShow = CommonUtil.map2Object(soapApi, InterfaceShow.class);
			// ???????????????,??????ID,??????ID????????????
			List<String> transIdList = interfaceParamShow.getTransId();
			String transId = interfaceShow.getTransId();
			String serviceId = transId.substring(0, 10);
			String operationId = transId.substring(10);
			// ??????ESB?????????4???????????????????????????
			List<EsbRelation> esbRelationList = (List<EsbRelation>) soapApi.get(Dict.ESB);
			if (CollectionUtils.isNotEmpty(esbRelationList)) {
				EsbRelation esbRelation = null;
				for (EsbRelation esb : esbRelationList) {
					if (Dict.SOAP.equals(esb.getProviderMsgType())) {
						esbRelation = esb;
					}
				}
				if (esbRelation != null) {
					if (esbRelation.getTranName() != null
							&& !esbRelation.getTranName().equals(interfaceShow.getInterfaceName())) {
						interfaceShow.setInterfaceName(esbRelation.getTranName());
						updateNameList.add(interfaceShow);
					}
					interfaceShow.setTransId(esbRelation.getTranId());
				}
			} else {
				// ????????????4?????????????????????????????????????????????ESB?????????
				interfaceShow.setTransId("");
			}
			String transIdNew = interfaceShow.getTransId();// ??????transId????????????
			// ?????????????????????transId???????????????????????????
			if (CollectionUtils.isNotEmpty(transIdList)) {
				boolean flag = false;
				for (String id : transIdList) {
					if (id.equals(serviceId) || id.equals(transIdNew) || id.equals(operationId)) {
						flag = true;
						break;//??????????????????????????????
					}
				}
				// ????????????????????????????????????????????????
				if (!flag)
					continue;
			}

			// ???????????????????????????????????? ??????ID+?????????
			interfaceShow.setEsbServiceId(serviceId);
			interfaceShow.setEsbOperationId(operationId);
			interfaceShow.setServiceAndOperationId(transId);

			// ?????????????????????Id
			interfaceShow.setAppId(interfaceLazyInitService.getAppIdByName(interfaceShow.getServiceId()));
			interfaceShowList.add(interfaceShow);
		}
		// ?????????ESB???????????????
		interfaceDao.updateSoapApiInterfaceName(updateNameList);
		interfaceMap.put(Dict.TOTAL, interfaceShowList.size());
		Integer page = interfaceParamShow.getPage();
		Integer pageNum = interfaceParamShow.getPageNum();
		if (page == null) {
			page = 1;
		}
		if (pageNum == null) {
			pageNum = 10;
		}
		if(pageNum == 0){
			pageNum = Integer.MAX_VALUE;
		}
		int start = (page - 1) * pageNum;
		int end = start + pageNum;
		//??????????????????????????????list
		if(start >= interfaceShowList.size() || end <= 0 || start < 0)
			interfaceMap.put(Dict.LIST, new ArrayList<>());
		else{//end???????????????????????????????????????-1
			end = end > interfaceShowList.size() ? interfaceShowList.size() : end;
			interfaceMap.put(Dict.LIST, interfaceShowList.subList(start, end));
        }
		return interfaceMap;
	}

	@Override
	public String getInterfacesUrl(String branch, String serviceId, List<Map> idsList) {
		Map<String, Object> map = new HashMap<>();
		map.put(Dict.BRANCH, branch);
		map.put(Dict.SERVICEID, serviceId);
		map.put(Dict.IDS, idsList);
		return interfaceDao.saveTinyUrlKey(new TinyURL(map, new Date(), serviceId));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Map getInterfacesByUrl(String id) {
		Map interfaceMap = new HashMap();
		List<InterfaceShow> interfaceShowList = new ArrayList<>();
		Map urlMap = interfaceDao.getTinyUrlKeyById(id);
		if (!urlMap.isEmpty()) {
			List<Map> idsList = (List<Map>) urlMap.get(Dict.IDS);
			for (Map<String, String> map : idsList) {
				for (Map.Entry<String, String> entry : map.entrySet()) {
					String type = map.get(entry.getKey());
					InterfaceShow interfaceShow;
					if (Dict.SOAP.equalsIgnoreCase(type)) {
						SoapApi soapApi = interfaceDao.getSoapApiById(entry.getKey());
						interfaceShow = CommonUtil.convert(soapApi, InterfaceShow.class);
						// ?????????????????????Id
						interfaceShow.setAppId(interfaceLazyInitService.getAppIdByName(soapApi.getServiceId()));
						interfaceShowList.add(interfaceShow);
					} else {
						RestApi restApi = interfaceDao.getRestApiById(entry.getKey());
						interfaceShow = CommonUtil.convert(restApi, InterfaceShow.class);
						// ?????????????????????Id
						interfaceShow.setAppId(interfaceLazyInitService.getAppIdByName(restApi.getServiceId()));
						interfaceShowList.add(interfaceShow);
					}
				}
			}
			interfaceMap.put(Dict.TOTAL, interfaceShowList.size());
			interfaceMap.put(Dict.LIST, interfaceShowList);
		}
		return interfaceMap;
	}

	@Override
	public void modifiInterfaceDescription(InterfaceDetailShow interfaceDetailShow) {
		String transId = interfaceDetailShow.getTransId();
		String serviceId = interfaceDetailShow.getServiceId();
		String interfaceType = interfaceDetailShow.getInterfaceType();
		ParamDesciption paramDesciption = interfaceDao.getParamDescription(transId, serviceId, interfaceType);
		if (FileUtil.isNullOrEmpty(paramDesciption)) {
			ParamDesciption paramDesciptionNew = new ParamDesciption();
			List<ParamDesciption> paramDesciptions = new ArrayList<>();
			paramDesciptionNew.setTransId(transId);
			paramDesciptionNew.setServiceId(serviceId);
			paramDesciptionNew.setInterfaceType(interfaceType);
			List<Param> requestParam = interfaceDetailShow.getRequest();
			List<Param> responseParam = interfaceDetailShow.getResponse();
			if (!FileUtil.isNullOrEmpty(requestParam)) {
				paramDesciptionNew.setRequest(requestParam);
			}
			if (!FileUtil.isNullOrEmpty(responseParam)) {
				paramDesciptionNew.setResponse(responseParam);
			}
			paramDesciptions.add(paramDesciptionNew);
			interfaceDao.saveParamDescription(paramDesciptions);
		} else {
			List<Param> allRequestParam = getAllParam(interfaceDetailShow.getRequest(), paramDesciption.getRequest());
			interfaceDetailShow.setRequest(allRequestParam);
			List<Param> allResponseParam = getAllParam(interfaceDetailShow.getResponse(),
					paramDesciption.getResponse());
			interfaceDetailShow.setResponse(allResponseParam);
			interfaceDao.updateParamDescription(interfaceDetailShow);
		}
	}

	/**
	 * ????????????????????????????????????????????????????????????????????????????????????????????????
	 *
	 * @param newList
	 * @param oldList
	 * @return
	 */
	private List<Param> getAllParam(List<Param> newList, List<Param> oldList) {
		if (!FileUtil.isNullOrEmpty(newList) && !FileUtil.isNullOrEmpty(oldList)) {
			for (Param newParam : newList) {
				for (Param oldParam : oldList) {
                    if (newParam.getName() != null && newParam.getName().equals(oldParam.getName())
							&& newParam.getType().equals(oldParam.getType())) {
						List<Param> newparamList = newParam.getParamList();
						List<Param> oldparamList = oldParam.getParamList();
						if (CollectionUtils.isNotEmpty(newparamList) || CollectionUtils.isNotEmpty(oldparamList)) {
							List<Param> allParamList = getAllParam(newparamList, oldparamList);
							newParam.setParamList(allParamList);
						}
						oldList.remove(oldParam);
						break;
					}
				}
			}
		}
		if (CollectionUtils.isNotEmpty(oldList)) {
			if (CollectionUtils.isEmpty(newList)) {
				newList = oldList;
			} else {
				newList.addAll(oldList);
			}
		}
		return newList;
	}

	/**
	 * ??????????????????????????????Soap??????????????????common service?????????
	 *
	 * @param soapRelation
	 * @param interfaceDetailShow
	 */
	public void getCommonServiceSoap(SoapRelation soapRelation, InterfaceDetailShow interfaceDetailShow) {
		if (soapRelation.getServiceCalling().startsWith(Dict.MSPER)
				&& StringUtils.isEmpty(soapRelation.getRepositoryId())) {
			String branch = soapRelation.getBranch();
			// ??????master????????????????????????????????????SIT?????????
			if (!Dict.MASTER.equals(branch)) {
				branch = Dict.SIT;
			}
			SoapRelation commSoap = interfaceRelationDao.getSoapRelation(Dict.MSPER_WEB_COMMON_SERVICE,
					soapRelation.getInterfaceAlias(), branch);
			if (!Util.isNullOrEmpty(commSoap)) {
				interfaceDetailShow.setRequest(commSoap.getRequest());
				interfaceDetailShow.setResponse(commSoap.getResponse());
			}
		}
	}

	/**
	 * ??????Soap??????????????????????????????
	 *
	 * @param id
	 * @return
	 */
	private List<InterfaceDetailShow> getSoapDetail(String id) {
		List<InterfaceDetailShow> showList = new ArrayList<>();
		SoapRelation soapRelation = interfaceRelationDao.getSoapRelationById(id);
		if (!Util.isNullOrEmpty(soapRelation)) {
			List<SoapRelation> soapRelationList = interfaceRelationDao.getSoapRelationVer(
					soapRelation.getServiceCalling(), soapRelation.getInterfaceAlias(), soapRelation.getBranch());
			if (CollectionUtils.isEmpty(soapRelationList)) {
				return showList;
			}
			for (SoapRelation relation : soapRelationList) {
				InterfaceDetailShow interfaceDetailShow = CommonUtil.convert(relation, InterfaceDetailShow.class);
				interfaceDetailShow = getDescriptionAndRemark(interfaceDetailShow);
				interfaceDetailShow.setRelTransId(interfaceDetailShow.getTransId());
				// ???????????????????????????????????????????????????????????????
				if (relation.getIsNew() == 1) {
					getCommonServiceSoap(relation, interfaceDetailShow);
				}
				// ??????ESB??????
				relatedEsb(interfaceDetailShow);
				showList.add(interfaceDetailShow);
			}
			return showList;
		} else {
			SoapApi soapApi = interfaceDao.getSoapApiById(id);
			if (Util.isNullOrEmpty(soapApi)) {
				return showList;
			}
			List<SoapApi> soapApiList = interfaceDao.getSoapApiVer(soapApi.getServiceId(), soapApi.getInterfaceAlias(),
					soapApi.getBranch());
			for (SoapApi api : soapApiList) {
				InterfaceDetailShow interfaceDetailShow = CommonUtil.convert(api, InterfaceDetailShow.class);
				interfaceDetailShow = getDescriptionAndRemark(interfaceDetailShow);
				interfaceDetailShow.setRelTransId(interfaceDetailShow.getTransId());
				// ??????ESB??????
				relatedEsb(interfaceDetailShow);
				showList.add(interfaceDetailShow);
			}
			return showList;
		}
	}

	/**
	 * SOAP/SOP?????? ??????ESB??????
	 *
	 * @param interfaceDetailShow
	 */
	private void relatedEsb(InterfaceDetailShow interfaceDetailShow) {
		String interfaceType = interfaceDetailShow.getInterfaceType();
		EsbRelation esbRelation = esbRelationDao.getEsbRelation(interfaceDetailShow.getTransId(), interfaceType);
		if (esbRelation == null) {
			return;
		}
		if (Dict.SOAP.equals(interfaceType)) {
			interfaceDetailShow.setTransId(esbRelation.getTranId());
		}
		interfaceDetailShow.setEsbServiceId(esbRelation.getServiceId());
		interfaceDetailShow.setEsbServiceName(esbRelation.getServiceName());
		interfaceDetailShow.setEsbOperationId(esbRelation.getOperationId());
		interfaceDetailShow.setEsbOperationName(esbRelation.getOperationName());
		interfaceDetailShow.setServiceAndOperationId(esbRelation.getServiceAndOperationId());
		interfaceDetailShow.setEsbState(esbRelation.getState());
	}

}
