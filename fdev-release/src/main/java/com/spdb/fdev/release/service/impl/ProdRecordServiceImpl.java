package com.spdb.fdev.release.service.impl;

import com.spdb.fdev.base.dict.Constants;
import com.spdb.fdev.base.dict.Dict;
import com.spdb.fdev.base.dict.ErrorConstants;
import com.spdb.fdev.base.utils.CommonUtils;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.release.dao.IProdRecordDao;
import com.spdb.fdev.release.dao.IProdTemplateDao;
import com.spdb.fdev.release.dao.IReleaseApplicationDao;
import com.spdb.fdev.release.entity.*;
import com.spdb.fdev.release.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RefreshScope
public class ProdRecordServiceImpl implements IProdRecordService {
	@Autowired
	private IProdRecordDao prodRecordDao;
	@Autowired
	private IProdTemplateService prodTemplateService;
	@Autowired
	private IProdApplicationService prodApplicationService;
	@Autowired
	private IUserService userService;
	@Value("${scripts.path}")
	private String scripts_path;
	@Autowired
	private IAsyncAutoReleaseService asyncAutoReleaseService;
	@Autowired
	private Environment enviroment;
	@Autowired
	private IProdAssetService prodAssetService;
	@Autowired
	private IReleaseCatalogService catalogService;
	@Autowired
	private IReleaseNodeService releaseNodeService;
	@Autowired
	private IRoleService roleService;
    @Autowired
    private IGroupAbbrService groupAbbrService;
    @Autowired
	private IFileService fileService;
	@Autowired
	private IReleaseApplicationDao releaseApplicationDao;
	
	private final static Logger logger = LoggerFactory.getLogger(ProdRecordServiceImpl.class);

	@Override
	public ProdRecord create(ProdRecord releaseRecords) throws Exception {
		return prodRecordDao.create(releaseRecords);

	}

	@Override
	public List query(String release_node_name) throws Exception {
		 List prodRecords = prodRecordDao.query(release_node_name);
		 return prodClassifier(prodRecords);
	}

	@Override
	public ProdRecord queryDetail(String prod_id) throws Exception {
		ProdRecord prodRecord = prodRecordDao.queryDetail(prod_id);
		if (!CommonUtils.isNullOrEmpty(prodRecord)) {
			prodRecord.setTemp_name(prodRecord.getExcel_template_name());
			if (!CommonUtils.isNullOrEmpty(prodRecord.getLauncher())) {
				Map<String, Object> user = userService.queryUserById(prodRecord.getLauncher());
				prodRecord.setLauncher_name_cn(
						CommonUtils.isNullOrEmpty(user) ? "" : (String) user.get(Dict.USER_NAME_CN));
			}
		}
		return prodRecord;
	}

	@Override
	public ProdRecord queryTrace(String prod_id) throws Exception {
//		String host_name = enviroment.getProperty(Dict.HOSTNAME);
		ProdRecord prodRecord = prodRecordDao.queryTrace(prod_id);
		//????????????????????????hostname???????????????????????????
		//????????????????????????????????????????????????????????????????????????????????????????????????
//		if(!CommonUtils.isNullOrEmpty(prodRecord.getProd_env())
//				&&!host_name.equals(prodRecord.getProd_env())) {
//			//???????????????????????????????????????????????????
//			if(Constants.PROD_RECORD_STSTUS_STARED.equals(prodRecord.getStatus())) {
//				prodRecordDao.updateStatus(prod_id, Constants.PROD_RECORD_STSTUS_FAILED);
//			}
//		}
		return prodRecord;
	}

	@Override
	public Map audit(String prod_id, String audit_type, String reject_reason, String template_properties)
			throws Exception {
		if (Constants.OPERATION_TYPR_REFUSE.equals(audit_type)) {
			// ???????????????2-????????????????????????reject_reason??????????????????release_records????????????
			prodRecordDao.audit(prod_id, audit_type, reject_reason);
		} else {
			asyncAutoReleaseService.autoRelease(prod_id);
			if (CommonUtils.isNullOrEmpty(template_properties)) {
				prodRecordDao.audit(prod_id, audit_type, reject_reason);
			} else {
				prodRecordDao.auditSetTemplate(prod_id, audit_type, reject_reason, template_properties);
			}
			prodRecordDao.updateAutoReleaseStage(prod_id, "0");
		}
		Map<String, String> map = new HashMap<String, String>();
		map.put(Dict.ID, prod_id);
		map.put("release_status", audit_type);
		map.put(Dict.REJECT_REASON, reject_reason);
		return map;
	}

	@Override
	public ProdRecord queryProdRecordByVersion(String release_node_name, String version) throws Exception {
		return prodRecordDao.queryProdRecordByVersion(release_node_name, version);
	}

	@Override
	public void setTemplate(String prod_id, String template_id ,String application_id) throws Exception {
		// ?????????????????????????????????????????????????????????
		ProdTemplate prodTemplate = new ProdTemplate();
		prodTemplate.setId(template_id);
		ProdTemplate queryProdTemplate = prodTemplateService.queryDetail(prodTemplate);

		ProdRecord queryProdRecord = queryDetail(prod_id);
		if(!queryProdTemplate.getTemplate_type().equals(queryProdRecord.getType())) {
			throw new FdevException(ErrorConstants.PROD_TYEP_DIFF_WHIT_TEMP_TPYE);
		}	
		List<Map<String, Object>> prodpplications = prodApplicationService.queryApplicationsNoException(prod_id);

		//????????????????????????????????????????????????????????????????????????
		List<ProdAsset> queryAssetsList = prodAssetService.queryAssetsList(prod_id);
		List<AssetCatalog> queryCatalogList = catalogService.query(template_id);
		
		if(!CommonUtils.isNullOrEmpty(prodpplications)) {
			boolean flag=false;
			for (AssetCatalog assetCatalog : queryCatalogList) {
				if (assetCatalog.getCatalog_type().equals(Constants.CATALOG_TYPE_NORMAL)
						|| assetCatalog.getCatalog_type().equals(Constants.CATALOG_TYPE_MICROSERIVICE)) {
					flag=true;
					break;
				}
			}
			if(!flag) {
				throw new FdevException(ErrorConstants.PROD_TEMP_NOT_EXIST_PROD_APPLICATION);
			}
		}

		//??????????????????????????????
		HashSet<String> newCataLogs = new HashSet<>();
		for (AssetCatalog assetCatalog : queryCatalogList) {
			newCataLogs.add(assetCatalog.getCatalog_name());
		}

		HashSet<String> uncompatibleCatalogs = new HashSet<>();
		for (ProdAsset prodAsset : queryAssetsList){
			if(!newCataLogs.contains(prodAsset.getAsset_catalog_name())){
				uncompatibleCatalogs.add(prodAsset.getAsset_catalog_name());
			}
		}
		
		if(!CommonUtils.isNullOrEmpty(uncompatibleCatalogs)) {
			throw new FdevException(ErrorConstants.UNCOMPATIBLE_TEMPLATE,
					new String[] { uncompatibleCatalogs.toString(), queryProdTemplate.getTemp_name()});
		}
		
		ReleaseNode queryReleaseNode = releaseNodeService.queryDetail(queryProdRecord.getRelease_node_name());

		if (CommonUtils.isNullOrEmpty(application_id)) {
			// ????????????????????????,?????????????????????????????????????????????????????????????????????
			if (!(roleService.isGroupReleaseManager(CommonUtils.getSessionUser().getGroup_id())
					|| queryReleaseNode.getRelease_manager().equals(CommonUtils.getSessionUser().getUser_name_en())
					|| queryReleaseNode.getRelease_spdb_manager()
							.equals(CommonUtils.getSessionUser().getUser_name_en()))) {
				throw new FdevException(ErrorConstants.PARAM_ERROR, new String[] { "????????????????????????,??????????????????" });
			}
			// ????????????
			     prodRecordDao.setTemplate(prod_id, template_id);
		} else {
			// ???????????????????????????????????????????????????
			if (roleService.isAppSpdbManager(application_id) || roleService.isApplicationManager(application_id)) {
				// ????????????
				 prodApplicationService.setApplicationTemplate(prod_id, application_id, template_id);
			} else {
				throw new FdevException(ErrorConstants.PARAM_ERROR, new String[] { "????????????????????????,??????????????????" });
			}
		}		
	}

	@Override
	public List queryByTemplateId(String template_id) throws Exception {
		return prodRecordDao.queryByTemplateId(template_id);
	}

	@Override
	public ProdRecord updateAutoReleaseLog(String prod_id, String auto_release_log) throws Exception {
		return prodRecordDao.updateAutoReleaseLog(prod_id, auto_release_log);
	}

	@Override
	public ProdRecord updateAutoReleaseStage(String prod_id, String auto_release_stage) throws Exception {
		return prodRecordDao.updateAutoReleaseStage(prod_id, auto_release_stage);
	}

	@Override
	public Map<String, String> queryBeforePordImages(String release_node_name, String version) throws Exception {
		return prodRecordDao.queryBeforePordImages(release_node_name,version);
	}

	@Override
	public void update(Map<String, String> requestParam) throws Exception {
		prodRecordDao.update(requestParam);
	}

	@Override
	public void delete(String prod_id) throws Exception {
		ProdRecord prodRecord = queryDetail(prod_id);
		//????????????
		prodRecordDao.delete(prod_id);
		//??????????????????
		this.updateOtherProdRecords(prodRecord.getProd_spdb_no());
		//??????????????????
		prodApplicationService.deleteAppByProd(prod_id);
		//??????????????????
		List<ProdAsset> prodAssets = prodAssetService.deleteByProd(prod_id);
		//?????????????????????
		for (ProdAsset prodAsset : prodAssets) {
			//source???1??????????????????minio????????????
			if("1".equals(prodAsset.getSource()) && !"0".equals(prodAsset.getAws_type())){
				fileService.deleteFiles(prodAsset.getFile_giturl(),"fdev-release");
			}
		}
	}

	private boolean deleteFile(File project, String localProjectPath) {
		if(CommonUtils.isNullOrEmpty(project) || !project.exists()) {
			logger.info("?????????????????????????????????{}", localProjectPath);
			return false;
		}
		File[] files = project.listFiles();
		for(File file : files) {
			if(file.isDirectory()) {
				deleteFile(file, null);
			} else {
				file.delete();
			}
		}
		project.delete();
		return true;
	}

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????
     * @param prod_spdb_no ????????????
     */
	public void updateOtherProdRecords(String prod_spdb_no) throws Exception {
		ProdRecord prodRecord = this.queryEarliestByProdSpdbNo(prod_spdb_no);
		if(!CommonUtils.isNullOrEmpty(prodRecord)) {
			String oldVersion = prodRecord.getVersion();
			String old_prod_assets_version = prodRecord.getProd_assets_version();
			String prod_assets_version = oldVersion + "_" + prod_spdb_no;
			this.updatePordAssetsVersionByProdSpdbNo(prod_spdb_no, prod_assets_version);
			//????????????????????????????????????????????????????????????????????????
			if(!old_prod_assets_version.equals(prod_assets_version)) {
                GroupAbbr groupAbbr = groupAbbrService.queryGroupAbbr(prodRecord.getOwner_groupId());
				CommonUtils.runPythonArray(scripts_path + "change_remote_dir.py",
						new String[]{groupAbbr.getSystem_abbr(),
								old_prod_assets_version,
								prod_assets_version, ""}
				);
            }
		}
	}

	@Override
	public ProdRecord queryProdByVersion(String version) throws Exception {
		return prodRecordDao.queryProdByVersion(version);
	}

	@Override
	public List<ProdRecord> queryPlan(String start_date, String end_date) throws Exception {
		List<ProdRecord> prodRecords = prodRecordDao.queryPlan(start_date ,end_date);
		return prodClassifier(prodRecords);
	}
	
	public List prodClassifier(List<ProdRecord> prodRecords) throws Exception{
		Map<String,Map> result = new HashMap<>();
		for (ProdRecord prodRecord : prodRecords) {
			if (!CommonUtils.isNullOrEmpty(prodRecord.getLauncher())) {
				Map<String, Object> user = userService.queryUserById(prodRecord.getLauncher());
				prodRecord.setLauncher_name_cn(
						CommonUtils.isNullOrEmpty(user) ? "" : (String) user.get(Dict.USER_NAME_CN));
			}
			Map prodMap = new HashMap();
			boolean flag = roleService.isUserOperatorProd(prodRecord.getProd_id()) //?????????????????????
					&& CommonUtils.laterThanOneDay(prodRecord.getDate()); //???????????????????????????????????????
			Map records_map = CommonUtils.beanToMap(prodRecord);
			records_map.put(Dict.CAN_OPERATION, flag);
			prodMap.putAll(records_map);
			List<Map<String, Object>> applications = prodApplicationService.queryApplicationsNoException(prodRecord.getProd_id());
			String isProdRisk = "";
			for (Map map :	applications) {
				String application_id = (String) map.get(Dict.APPLICATION_ID);
				//??????????????????????????????????????????????????????????????????
				List<Map<String, Object>> apps =  prodRecordDao.queryRiskProd(application_id ,prodRecord.getDate());
				if(!CommonUtils.isNullOrEmpty(apps)){
					//???????????????,??????????????????
					isProdRisk = "1";
				}
			}
			prodMap.put(Dict.APPLICATIONS, applications);
			prodMap.put(Dict.ISRISK,isProdRisk);//??????????????????
			if(CommonUtils.isNullOrEmpty(result.get(prodRecord.getProd_assets_version()))) {
				List<Map> list = new ArrayList<>();
				list.add(prodMap);
				Map<String , Object> map = new HashMap<>();
				map.put(Dict.PROD_RECORDS, list);
				map.put(Dict.DATE, prodRecord.getDate());
				map.put(Dict.PROD_SPDB_NO, prodRecord.getProd_spdb_no());
				map.put(Dict.TYPE, prodRecord.getType());
				map.put(Dict.CAN_OPERATION, flag);
				map.put(Dict.PROD_ASSETS_VERSION, prodRecord.getProd_assets_version());
				result.put(prodRecord.getProd_assets_version(), map);
			} else {
			    Map<String , Object> map = result.get(prodRecord.getProd_assets_version());
				map.put(Dict.CAN_OPERATION, ((boolean)map.get(Dict.CAN_OPERATION)) | flag);
			    List list = (List<ProdRecord>)map.get(Dict.PROD_RECORDS);
			    list.add(prodMap);
			}
		}
		return new ArrayList(result.values());

	}

	@Override
	public Map<String, String> queryProdInfo(String prod_assets_version) throws Exception{
		return prodRecordDao.queryProdInfo(prod_assets_version);
	}

	@Override
	public void updateReleaseNodeName(String old_release_node_name, String release_node_name) throws Exception {
		prodRecordDao.updateReleaseNodeName(old_release_node_name,release_node_name);
	}

	@Override
	public ProdRecord queryEarliestByProdSpdbNo(String prod_spdb_no) {
		return prodRecordDao.queryEarliestByProdSpdbNo(prod_spdb_no);
	}

	@Override
	public void updatePordAssetsVersionByProdSpdbNo(String prod_spdb_no , String prod_assets_version) {
		prodRecordDao.updatePordAssetsVersionByProdSpdbNo(prod_spdb_no , prod_assets_version);
	}

	@Override
	public void updateProdStatus(String prod_id, String status) throws Exception {
		prodRecordDao.updateStatus(prod_id, status);
	}

    @Override
    public String checkApplication(ProdRecord prodRecord) {
		//?????????????????????id
		List<ProdApplication> prodApps = prodApplicationService.queryByProdId(prodRecord.getProd_id());
		List<String> applicationIds = prodApps.stream().filter(s -> "1".equals(s.getCaas_add_sign()) || "1".equals(s.getScc_add_sign()) ).map(pa -> pa.getApplication_id()).collect(Collectors.toList());
		String content = null;
		if(!CommonUtils.isNullOrEmpty(applicationIds)) {
			List<ProdRecord> list = prodRecordDao.queryOldProdList(prodRecord, applicationIds);
			if(!CommonUtils.isNullOrEmpty(list)) {
				content = "?????????????????????????????????" + list.get(0).getVersion() + "????????????????????????????????????????????????";
			}
		}
		return content;
    }

	@Override
	public Boolean checkAddSign(String application_id, ProdRecord prodRecord) {
		Boolean signflag = false;
		ReleaseApplication releaseApp = releaseApplicationDao.queryByIdAndNodeName(application_id, prodRecord.getRelease_node_name());
		if(!CommonUtils.isNullOrEmpty(releaseApp)){
			//????????????????????????????????????????????????
			List<ReleaseApplication> releaseApplicationList = releaseApplicationDao.queryOldApplication(releaseApp.getApplication_id(), prodRecord.getRelease_node_name());
			if(CommonUtils.isNullOrEmpty(releaseApplicationList)){
				//??????????????????
				String last_tag = prodApplicationService.queryLastTagByGitlabId(application_id, prodRecord.getProd_id(), prodRecord.getType(),null);
				if(!CommonUtils.isNullOrEmpty(releaseApp.getNew_add_sign()) && releaseApp.getNew_add_sign().equals("1") && CommonUtils.isNullOrEmpty(last_tag)){
					signflag = true;
				}
			}
		}
		return signflag;
	}

	@Override
	public void updateAwsAssetGroupId(String prodId, String groupId) {
		prodRecordDao.updateAwsAssetGroupId(prodId, groupId);
	}

}
