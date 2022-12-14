package com.spdb.fdev.release.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.spdb.fdev.base.dict.Constants;
import com.spdb.fdev.base.dict.Dict;
import com.spdb.fdev.base.dict.ErrorConstants;
import com.spdb.fdev.base.utils.CommonUtils;
import com.spdb.fdev.common.JsonResult;
import com.spdb.fdev.common.annoation.OperateRecord;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.common.util.JsonResultUtil;
import com.spdb.fdev.common.util.Util;
import com.spdb.fdev.common.validate.RequestValidate;
import com.spdb.fdev.release.dao.IAssetCatalogDao;
import com.spdb.fdev.release.dao.IBatchTaskDao;
import com.spdb.fdev.release.dao.IEsfRegistrationDao;
import com.spdb.fdev.release.entity.AssetCatalog;
import com.spdb.fdev.release.entity.AwsConfigure;
import com.spdb.fdev.release.entity.OptionalCatalog;
import com.spdb.fdev.release.entity.ProdApplication;
import com.spdb.fdev.release.entity.ProdAsset;
import com.spdb.fdev.release.entity.ProdRecord;
import com.spdb.fdev.release.entity.ProdTemplate;
import com.spdb.fdev.release.entity.ReleaseNode;
import com.spdb.fdev.release.entity.SystemReleaseInfo;
import com.spdb.fdev.release.service.IAppService;
import com.spdb.fdev.release.service.IFileService;
import com.spdb.fdev.release.service.IGitlabService;
import com.spdb.fdev.release.service.IOptionalCatalogService;
import com.spdb.fdev.release.service.IProdAppScaleService;
import com.spdb.fdev.release.service.IProdApplicationService;
import com.spdb.fdev.release.service.IProdAssetService;
import com.spdb.fdev.release.service.IProdRecordService;
import com.spdb.fdev.release.service.IProdTemplateService;
import com.spdb.fdev.release.service.IRelDevopsRecordService;
import com.spdb.fdev.release.service.IReleaseCatalogService;
import com.spdb.fdev.release.service.IReleaseNodeService;
import com.spdb.fdev.release.service.IRoleService;
import com.spdb.fdev.release.service.ISystemReleaseInfoService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = "??????????????????")
@RequestMapping("/api/release")
@RestController
public class ProdAssetController {

    private static final Logger logger = LoggerFactory.getLogger(ProdAssetController.class);

    private static final String SPACING = "  ";

    private static final String WRAP_WINDOWS = "\r\n";

    private static final String ORDER_TXT_NAME = "order.txt (???????????????????????????)";

    @Autowired
    private IProdAssetService prodAssetService;
    @Autowired
    private IRoleService roleService;
    @Autowired
    private IProdRecordService prodRecordService;
    @Autowired
    IReleaseNodeService releaseNodeService;
    @Autowired
    IGitlabService gitlabService;
    @Autowired
    IProdTemplateService prodTemplateService;
    @Autowired
    ISystemReleaseInfoService systemReleaseInfoService;
    @Autowired
    private IAppService appService;
    @Autowired
    private IReleaseCatalogService releaseCatalogService;
    @Autowired
    private IProdApplicationService prodApplicationService;
    @Autowired
    private IOptionalCatalogService optionalCatalogService;
    @Autowired
    private IProdAppScaleService prodAppScaleService;
    @Autowired
    private IFileService fileService;
    @Autowired
    private IReleaseCatalogService catalogService;
    @Autowired
    IRelDevopsRecordService relDevopsRecordService;
    @Autowired
    private IAssetCatalogDao assetCatalogDao;
    @Autowired
    private IEsfRegistrationDao esfRegistrationDao;
    @Autowired
	private IBatchTaskDao batchTaskDao;
    
    @Value("${release.git.special.url}")
    private String specialUrl;
    

    @ApiOperation(value = "??????????????????")
    @PostMapping(value = "/uploadAssets", consumes = {"multipart/form-data"})
    public JsonResult uploadAssets(@RequestParam(Dict.PROD_ID) String prod_id,
                                   @RequestParam(value = Dict.FILE_ENCODING, required = false) String file_encoding,
                                   @RequestParam(Dict.ASSET_CATALOG_NAME) String asset_catalog_name,
                                   @RequestParam(value = Dict.SOURCE_APPLICATION, required = false) String source_application,
                                   @RequestParam(value = Dict.RUNTIME_ENV, required = false) String runtime_env,
                                   @RequestParam(value = Dict.SEQ_NO, required = false) String seq_no,
                                   @RequestParam(value = Dict.CHILD_CATALOG, required = false) String child_catalog,
                                   @RequestParam(value = Dict.BUCKET_NAME, required = false) String bucket_name,
                                   @RequestParam(value = Dict.BUCKET_PATH, required = false) String bucket_path,
                                   @RequestParam(value = Dict.TYPE, required = false) String aws_type,
                                   @RequestParam(Dict.FILE) MultipartFile[] files) throws Exception {
        return JsonResultUtil.buildSuccess(prodAssetService.uploadAssets(CommonUtils.getSessionUser(), prod_id, file_encoding, asset_catalog_name, source_application, runtime_env, seq_no, child_catalog, bucket_name, bucket_path, aws_type, files));
    }

    @ApiOperation(value = "??????????????????")
    @PostMapping(value = "/updateEsfcommonconfigAssets", consumes = {"multipart/form-data"})
    public JsonResult updateEsfcommonconfigAssets(@RequestParam(Dict.PROD_ID) String prod_id,
                                   @RequestParam(value = Dict.FILE_ENCODING, required = false) String file_encoding,
                                   @RequestParam(Dict.ASSET_CATALOG_NAME) String asset_catalog_name,
                                   @RequestParam(value = Dict.SOURCE_APPLICATION, required = true) String source_application,
                                   @RequestParam(value = Dict.RUNTIME_ENV, required = false) String runtime_env,
                                   @RequestParam(value = Dict.CHILD_CATALOG, required = false) String child_catalog,
                                   @RequestParam(value = Dict.BUCKET_NAME, required = false) String bucket_name,
                                   @RequestParam(Dict.FILE) MultipartFile[] files) throws Exception {
        prodAssetService.updateEsfcommonconfigAssets(CommonUtils.getSessionUser(), prod_id, file_encoding, asset_catalog_name, source_application, runtime_env,child_catalog, bucket_name, files);
        return JsonResultUtil.buildSuccess(null);
    }

    @RequestValidate(NotEmptyFields = {Dict.PROD_ID})
    @ApiOperation(value = "????????????????????????")
    @PostMapping(value = "/queryAssets")
    public JsonResult queryAssets(@RequestBody @ApiParam Map<String, String> requestParam) throws Exception {
        String prod_id = requestParam.get(Dict.PROD_ID);
        String source_application = requestParam.get(Dict.SOURCE_APPLICATION);
        List result = prodAssetService.queryAssets(prod_id, source_application);
        return JsonResultUtil.buildSuccess(result);
    }

    @RequestValidate(NotEmptyFields = {Dict.PROD_ID})
    @ApiOperation(value = "??????????????????????????????")
    @PostMapping(value = "/queryAllProdAssets")
    public JsonResult queryAllProdAssets(@RequestBody @ApiParam Map<String, String> requestParam) throws Exception {
        Map<String, Object> retuen_map = prodAssetService.queryAllProdAssets(requestParam);
        return JsonResultUtil.buildSuccess(retuen_map);
    }


    @RequestValidate(NotEmptyFields = {Dict.PROD_ID})
    @ApiOperation(value = "??????????????????")
    @PostMapping(value = "/exportProdDirection")
    public JsonResult exportProdDirection(@RequestBody @ApiParam Map<String, String> requestParam) throws Exception {
        String prod_id = requestParam.get(Dict.PROD_ID);
        ProdRecord prodRecord = prodRecordService.queryDetail(prod_id);
        StringBuilder stringBuilder = new StringBuilder();
        // ????????????????????????
        List<Map> prodApplications = prodApplicationService.queryApplications(new ProdApplication(prod_id));
        Map<String, OptionalCatalog> catalogs = optionalCatalogService.queryOptionalCatalogs();
        Set<String> titleAdded = new HashSet<>();
        Set<String> orderTxtAdded = new HashSet<>();
        for (Map prodApplication : prodApplications) {
            String catalogName = (String) prodApplication.get(Dict.ASSET_CATALOG);
            addTitle(catalogName, catalogs, stringBuilder, titleAdded, false);
            String proImageUri = (String) prodApplication.get(Dict.PRO_IMAGE_URI);
            newLine(stringBuilder, 1, new StringBuilder().append(prodApplication.get(Dict.APP_NAME_EN)).append("(")
                    .append(proImageUri == null ? "????????????????????????" : proImageUri.split(":")[1])
                    .append(")").toString());
            addOrderTxt(stringBuilder, catalogName, orderTxtAdded);
        }
        stringBuilder.append(WRAP_WINDOWS);
        // ??????????????????????????????
        Map<String, Object> sortedAssets = prodAssetService.querySortedAssets(prod_id);
        for (Iterator sortedAssetsIter = sortedAssets.entrySet().iterator(); sortedAssetsIter.hasNext(); ) {
            Map.Entry sortedAssetsEntry = (Map.Entry) sortedAssetsIter.next();
            String catalogName = (String) sortedAssetsEntry.getKey();
            if (sortedAssetsEntry.getValue() instanceof List) { // ???????????????????????????
                List<ProdAsset> assets = (List<ProdAsset>) sortedAssetsEntry.getValue();
                assets.forEach(asset -> {
                    addTitle(catalogName, catalogs, stringBuilder, titleAdded, false);
                    //???????????????????????????order.txt
                    if (asset.getSeq_no() != null) {
                        addOrderTxt(stringBuilder, catalogName, orderTxtAdded);
                    }
                    newLine(stringBuilder, 1, assetShowName(asset));

                });
            } else if (sortedAssetsEntry.getValue() instanceof Map) {  //??????????????????
                Map<String, List> envProdFiles = (Map<String, List>) sortedAssetsEntry.getValue();
                addTitle(catalogName, catalogs, stringBuilder, titleAdded, false);
                for (Iterator envProdFilesIter = envProdFiles.entrySet().iterator(); envProdFilesIter.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) envProdFilesIter.next();
                    String envName = (String) entry.getKey();
                    List<ProdAsset> assets = (List<ProdAsset>) entry.getValue();
                    assets.forEach(asset -> {
                        addTitle(envName, catalogs, stringBuilder, titleAdded, true);
                        newLine(stringBuilder, 2, assetShowName(asset));
                    });
                }
            }
            stringBuilder.append(WRAP_WINDOWS);
        }
        // ????????????????????????
        Map paramMap = new HashMap<>();
        paramMap.put(Dict.PROD_ID, prod_id);
        List<Map<String, Object>> appScaleList = prodAppScaleService.queryAppScale(paramMap);
        if (!Util.isNullOrEmpty(appScaleList)) {
            OptionalCatalog optionalCatalog = new OptionalCatalog();
            optionalCatalog.setDescription("????????????");
            catalogs.put(Dict.DOCKER_SCALE, optionalCatalog);
            addTitle(Dict.DOCKER_SCALE, catalogs, stringBuilder, titleAdded, false);
            List<Map<String, String>> env_scales = (List<Map<String, String>>) appScaleList.get(0).get("env_scales");
            env_scales.forEach(env -> {
                newLine(stringBuilder, 1, env.get("env_name"));
                newLine(stringBuilder, 2, ORDER_TXT_NAME);
            });
        }
        return JsonResultUtil.buildSuccess(stringBuilder.toString());
    }

    @OperateRecord(operateDiscribe = "????????????-??????????????????")
    @ApiOperation(value = "??????????????????")
    @PostMapping(value = "/deleteAsset")
    public JsonResult deleteAsset(@RequestBody @ApiParam Map<String, Object> requestParam) throws Exception {
        String id = (String) requestParam.get(Dict.ID);
        ProdAsset prodAsset = prodAssetService.queryAssetsOne(id);
        if (CommonUtils.isNullOrEmpty(prodAsset)) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????"});
        }
        ProdRecord prodRecord = prodRecordService.queryDetail(prodAsset.getProd_id());
        // ???????????????????????????????????????
        ReleaseNode queryReleaseNode = releaseNodeService.queryDetail(prodRecord.getRelease_node_name());
        // ??????source_application???????????????????????????????????????????????????
        if (CommonUtils.isNullOrEmpty(prodAsset.getSource_application())) {
            // ????????????????????? ????????????????????????,?????????????????????????????????????????????????????????????????????
            if (!(roleService.isGroupReleaseManager(CommonUtils.getSessionUser().getGroup_id())
                    || queryReleaseNode.getRelease_manager().equals(CommonUtils.getSessionUser().getUser_name_en())
                    || queryReleaseNode.getRelease_spdb_manager()
                    .equals(CommonUtils.getSessionUser().getUser_name_en()))) {
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"??????????????????????????????,??????????????????"});
            }
        } else {
            // ????????????????????????????????????????????????????????????????????????
            if (!(roleService.isApplicationManager(prodAsset.getSource_application())
                    || roleService.isAppSpdbManager(prodAsset.getSource_application()))) {
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"??????????????????????????????,??????????????????"});
            }
        }
        if (!CommonUtils.isNullOrEmpty(prodAsset.getSeq_no())) {
            String DelSeqNo = prodAsset.getSeq_no();
            String asset_catalog_name = prodAsset.getAsset_catalog_name();
            prodAssetService.deleteAsset(prodAsset);
            List<ProdAsset> seqNo = prodAssetService.queryAssetsList(prodAsset.getProd_id());
            for (ProdAsset asset : seqNo) {
                if (!CommonUtils.isNullOrEmpty(asset.getSeq_no()) && asset_catalog_name.equals(asset.getAsset_catalog_name())) {
                    if (Integer.parseInt(DelSeqNo) < Integer.parseInt(asset.getSeq_no())) {
                        asset.setSeq_no((Integer.parseInt(asset.getSeq_no()) - 1) + "");
                        prodAssetService.updateAssetSeqNo(asset);
                    }
                }
            }
        } else {
            prodAssetService.deleteAsset(prodAsset);
        }
        return JsonResultUtil.buildSuccess("");
    }

    @ApiOperation(value = "????????????????????????")
    @PostMapping(value = "/deleteAssets")
    public JsonResult batchDeleteGitlabAsset(@RequestBody @ApiParam Map<String, Object> requestParam) throws Exception {
        List<String> ids = (List<String>) requestParam.get(Dict.IDS);
        List<String> fileNameArray = new ArrayList<>();
        for (String id : ids) {
            ProdAsset prodAsset = prodAssetService.queryAssetsOne(id);
            if (CommonUtils.isNullOrEmpty(prodAsset)) {
                throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????"});
            }
            if ("2".equals(prodAsset.getSource())) {
                fileNameArray.add(prodAsset.getFileName());
            }
        }
        for (int i = 0; i < fileNameArray.size(); i++) {
            if (!fileNameArray.get(0).equals(fileNameArray.get(i))) {
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"????????????,??????????????????"});
            }
        }

        for (String id : ids) {
            ProdAsset prodAsset = prodAssetService.queryAssetsOne(id);
            ReleaseNode queryReleaseNode = releaseNodeService.queryDetail(prodAsset.getRelease_node_name());
            if (!(roleService.isGroupReleaseManager(CommonUtils.getSessionUser().getGroup_id())
                    || queryReleaseNode.getRelease_manager().equals(CommonUtils.getSessionUser().getUser_name_en())
                    || queryReleaseNode.getRelease_spdb_manager()
                    .equals(CommonUtils.getSessionUser().getUser_name_en()))) {
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"????????????????????????"});
            }

            ProdRecord prodRecord = prodRecordService.queryDetail(prodAsset.getProd_id());
            if (CommonUtils.isNullOrEmpty(prodRecord)) {
                throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????"});
            }
            if ("2".equals(prodAsset.getSource())) {
                AssetCatalog assetCatalog = releaseCatalogService.queryAssetCatalogByName(prodRecord.getTemplate_id(), prodAsset.getAsset_catalog_name());
                if (!"5".equals(assetCatalog.getCatalog_type()) && !"9".equals(assetCatalog.getCatalog_type()) && !"10".equals(assetCatalog.getCatalog_type())) {
                    throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"??????????????????????????????????????????????????????"});
                }
                
            }

            prodAssetService.deleteAsset(prodAsset);
        }
        return JsonResultUtil.buildSuccess("");
    }

    @RequestValidate(NotEmptyFields = {Dict.RESOURCE_GITURL})
    @ApiOperation(value = "????????????")
    @PostMapping(value = "/queryResourceBranches")
    public JsonResult queryBranches(@RequestBody @ApiParam Map<String, String> requestParam) throws Exception {
        String resource_giturl = requestParam.get(Dict.RESOURCE_GITURL);
        String clear_cache = requestParam.get("clear_cache");
        String resource_giturl_projectid = gitlabService.queryProjectIdByUrl(resource_giturl);
        //????????????????????????1 ??????????????????
        if (!CommonUtils.isNullOrEmpty(clear_cache) && "1".equals(clear_cache)) {
            gitlabService.cleanCacheBranches(resource_giturl_projectid);
        }
        List result = gitlabService.queryBranches(resource_giturl_projectid);
        Map map = new HashMap();
        map.put("branchList", result);
        map.put("resource_url", resource_giturl);
        return JsonResultUtil.buildSuccess(map);
    }

    @OperateRecord(operateDiscribe = "????????????-git????????????")
    @RequestValidate(NotEmptyFields = {Dict.BRANCH, Dict.RESOURCE_GITURL})
    @ApiOperation(value = "??????????????????")
    @PostMapping(value = "/queryResourceFiles")
    public JsonResult Asset(@RequestBody @ApiParam Map<String, Object> requestParam) throws Exception {
        String resource_giturl = (String) requestParam.get(Dict.RESOURCE_GITURL);
        String clear_cache = String.valueOf(requestParam.get("clear_cache"));
        String branch = (String) requestParam.get(Dict.BRANCH);
        //????????????????????????1 ??????????????????
        String resource_giturl_projectid = gitlabService.queryProjectIdByUrl(resource_giturl);
        if (!CommonUtils.isNullOrEmpty(clear_cache) && "1".equals(clear_cache)) {
            gitlabService.cleanCacheFiles(resource_giturl_projectid, branch);
        }
        return JsonResultUtil.buildSuccess(gitlabService.queryResourceFiles(resource_giturl_projectid, branch));
    }

    @RequestValidate(NotEmptyFields = {Dict.PROD_ID})
    @ApiOperation(value = "????????????????????????")
    @PostMapping(value = "/queryResourceProjects")
    public JsonResult queryResourceProjects(@RequestBody @ApiParam Map<String, String> requestParam) throws Exception {
        String prod_id = requestParam.get(Dict.PROD_ID);
        ProdRecord prodRecord = prodRecordService.queryDetail(prod_id);
        if (CommonUtils.isNullOrEmpty(prodRecord)) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"?????????????????????"});
        }
        String template_id = prodRecord.getTemplate_id();
        ProdTemplate proTemplate = new ProdTemplate();
        proTemplate.setId(template_id);
        ProdTemplate newproTemplate = prodTemplateService.queryDetail(proTemplate);
        if (CommonUtils.isNullOrEmpty(newproTemplate)) {
            throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"??????????????????????????????"});
        }
        SystemReleaseInfo systemReleaseInfo;
        String configType = requestParam.get("configType");
        //??????????????????????????????????????????
        if("dynamicconfig".equals(configType)) {
        	systemReleaseInfo = systemReleaseInfoService.querySysInfoByConfigType(configType);
        }else {
        	String own_system = newproTemplate.getOwner_system();
        	systemReleaseInfo = systemReleaseInfoService.querySysRlsDetail(own_system);
        	if (CommonUtils.isNullOrEmpty(systemReleaseInfo)) {
                throw new FdevException(ErrorConstants.DATA_NOT_EXIST, new String[]{"???????????????????????????????????????"});
            }
        }
        
        List<String> resourceProjectList = Arrays.asList(systemReleaseInfo.getResource_giturl().split(";"));
        return JsonResultUtil.buildSuccess(resourceProjectList);
    }

    @RequestValidate(NotEmptyFields = {Dict.PROD_ID, Dict.ASSET_CATALOG_NAME, Dict.FILES, Dict.BRANCH, Dict.RESOURCE_GITURL})
    @ApiOperation(value = "??????gitlab????????????")
    @PostMapping(value = "/addGitlabAsset")
    public JsonResult addGitlabAsset(@RequestBody @ApiParam Map<String, Object> requestParam) throws Exception {
        String prod_id = (String) requestParam.get(Dict.PROD_ID);
        String branch = (String) requestParam.get(Dict.BRANCH);
        String asset_catalog_name = (String) requestParam.get(Dict.ASSET_CATALOG_NAME);
        String source_application = (String) requestParam.get(Dict.SOURCE_APPLICATION);
        ProdRecord prodRecord = prodRecordService.queryDetail(prod_id);
        String resource_giturl = (String) requestParam.get(Dict.RESOURCE_GITURL);
        String resource_gitlab_id = gitlabService.queryProjectIdByUrl(resource_giturl);
        ReleaseNode queryReleaseNode = releaseNodeService.queryDetail(prodRecord.getRelease_node_name());

        if (!(roleService.isGroupReleaseManager(CommonUtils.getSessionUser().getGroup_id())
                || queryReleaseNode.getRelease_manager().equals(CommonUtils.getSessionUser().getUser_name_en())
                || queryReleaseNode.getRelease_spdb_manager().equals(CommonUtils.getSessionUser().getUser_name_en()))) {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"??????????????????????????????,??????????????????"});
        }
        ArrayList<Map> list = (ArrayList) requestParam.get(Dict.FILES);
        for (Map file_path : list) {
            if (!gitlabService.isFileExist((String) file_path.get(Dict.FILE), resource_gitlab_id, branch)) {
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"gitlab??????????????????"});
            }
        }
        //proc????????????gray?????????
        String type = prodRecord.getType();
        
        for (Map file_name : list) {
        	String runTime_env = (String) file_name.get(Dict.RUNTIME_ENV);
        	String gitFilePath = (String) file_name.get("file");
            ProdAsset prodAsset = new ProdAsset();
            prodAsset.setProd_id(prod_id);
            prodAsset.setFileName(((String) file_name.get(Dict.FILE)).substring((gitFilePath).lastIndexOf('/') + 1));
            prodAsset.setFile_giturl(resource_giturl + "/blob/" + branch + "/" + file_name.get("file"));
            prodAsset.setAsset_catalog_name(asset_catalog_name);
            prodAsset.setSource_application(source_application);
            prodAsset.setRuntime_env(runTime_env);
            prodAsset.setSource("2");
            prodAsset.setRelease_node_name(prodRecord.getRelease_node_name());
            prodAsset.setUpload_user(CommonUtils.getSessionUser().getId());
            prodAsset.setUpload_time(CommonUtils.formatDate(CommonUtils.STANDARDDATEPATTERN));
            if("bastioncommonconfig".equals(asset_catalog_name) && !Util.isNullOrEmpty(runTime_env) && specialUrl.equals(resource_giturl)) {
            	if("gray".equals(type)) {
            		if(gitFilePath.contains("/params/mobgray/dynamicconfig")) {
            			String [] sp = gitFilePath.split("/params/mobgray/dynamicconfig");
            			if(!Util.isNullOrEmpty(sp)) {
            				if(runTime_env.equals("DEV") || runTime_env.equals("TEST")) {
            					StringBuffer graySh = new StringBuffer();
            					graySh.append(Constants.SPLIT_STRING).append(runTime_env).append("/grayparams/mobgray/dynamicconfig").append(sp[1]);
            					prodAsset.setChild_catalog(graySh.toString());
            					prodAssetService.addGitlabAsset(prodAsset);
            					
            					StringBuffer grayHf = new StringBuffer();
            					grayHf.append(Constants.SPLIT_STRING).append(runTime_env).append("/hfgrayparam/mobgray/dynamicconfig").append(sp[1]);
            					prodAsset.setChild_catalog(grayHf.toString());
            					prodAssetService.addGitlabAsset(prodAsset);
            				}else if(runTime_env.equals("PROCSH")) {
            					StringBuffer procSh = new StringBuffer();
            					procSh.append(Constants.SPLIT_STRING).append(runTime_env).append("/grayparam/mobgray/dynamicconfig").append(sp[1]);
            					prodAsset.setChild_catalog(procSh.toString());
            					prodAssetService.addGitlabAsset(prodAsset);
            				}else if(runTime_env.equals("PROCHF")) {
            					StringBuffer prochf = new StringBuffer();
            					prochf.append(Constants.SPLIT_STRING).append(runTime_env).append("/hfgrayparam/mobgray/dynamicconfig").append(sp[1]);
            					prodAsset.setChild_catalog(prochf.toString());
            					prodAssetService.addGitlabAsset(prodAsset);
            				}
            			}
            		}else {
            			throw new FdevException("??????????????????????????????????????????/params/mobgray/dynamicconfig/?????????????????????");
            		}
            	}else if("proc".equals(type)) {
            		if(gitFilePath.contains("/params/dynamicconfig/")) {
            			String [] sp = gitFilePath.split("/params/dynamicconfig");
            			if(!Util.isNullOrEmpty(sp)) {
            				StringBuffer sb = new StringBuffer();
            				sb.append(Constants.SPLIT_STRING).append(runTime_env).append("/params/dynamicconfig").append(sp[1]);
            				prodAsset.setChild_catalog(sb.toString());
            				prodAssetService.addGitlabAsset(prodAsset);
            			}
            		}else {
            			throw new FdevException("??????????????????????????????????????????/params/dynamicconfig/?????????????????????");
            		}
            	}
            }else {
            	prodAssetService.addGitlabAsset(prodAsset);
            }
            
        }
        return JsonResultUtil.buildSuccess();
    }

    @OperateRecord(operateDiscribe = "????????????-??????")
    @ApiOperation(value = "????????????????????????")
    @PostMapping(value = "/updateAssetSeqNo")
    public JsonResult updateAssetSeqNo(@RequestBody @ApiParam Map<String, Object> requestParam) throws Exception {
        if (CommonUtils.isNullOrEmpty(requestParam)) {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"??????????????????????????????"});
        }
        List<String> ids = (List<String>) requestParam.get(Dict.IDS);
        if (ids.size() != 2) {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"??????????????????????????????"});
        }

        List<String> tempList = new ArrayList<>();
        List<ProdAsset> prodAssetList = new ArrayList<>();
        for (String id : ids) {
            ProdAsset prodAsset = prodAssetService.queryAssetsOne(id);
            prodAssetList.add(prodAsset);
            String seq_no = prodAsset.getSeq_no();
            tempList.add(seq_no);
        }
        Map hashMap = new HashMap();
        prodAssetList.get(0).setSeq_no(tempList.get(1));
        hashMap.put(prodAssetList.get(0).getId(), prodAssetService.updateAssetSeqNo(prodAssetList.get(0)));
        prodAssetList.get(1).setSeq_no(tempList.get(0));
        hashMap.put(prodAssetList.get(1).getId(), prodAssetService.updateAssetSeqNo(prodAssetList.get(1)));
        return JsonResultUtil.buildSuccess(hashMap);
    }


    /**
     * ????????????????????????
     *
     * @param prodAssets
     * @param catalogName
     * @param optionalCatalog
     * @param assetMap
     */
    private void assembleAssets(Map<String, Map> prodAssets, String catalogName, OptionalCatalog optionalCatalog,
                                Map assetMap) {
        Map catalogMap = prodAssets.get(catalogName);
        if (catalogMap == null) {
            catalogMap = new HashMap();
            catalogMap.put(Dict.CATALOG_NAME, catalogName);
            catalogMap.put(Dict.CATALOG_DESCRIPTION, optionalCatalog == null ? "" : optionalCatalog.getDescription());
            catalogMap.put(Dict.CATALOG_TYPE, optionalCatalog == null ? "" : optionalCatalog.getCatalog_type());
            prodAssets.put(catalogName, catalogMap);
        }
        if (assetMap != null) {
            List assets = (List) catalogMap.get(Dict.CHILDREN);
            if (assets == null) {
                assets = new ArrayList();
            }
            assets.add(assetMap);
            catalogMap.put(Dict.CHILDREN, assets);
        }
    }

    /**
     * ??????order.txt
     *
     * @param prodAssets
     * @param catalogName
     * @param optionalCatalog
     * @param orderTxtAdded
     */
    private void showOrderTxt(Map<String, Map> prodAssets, String catalogName,
                              OptionalCatalog optionalCatalog, Set<String> orderTxtAdded) {
        if (!orderTxtAdded.contains(catalogName)) {
            Map orderMap = new HashMap();
            orderMap.put(Dict.ASSET_NAME, ORDER_TXT_NAME);
            assembleAssets(prodAssets, catalogName, optionalCatalog, orderMap);
            orderTxtAdded.add(catalogName);
        }
    }


    /**
     * ?????????????????????
     *
     * @param prodAsset
     * @return
     */
    private String assetShowName(ProdAsset prodAsset) {
        StringBuilder nameSb = new StringBuilder();
        if (!Util.isNullOrEmpty(prodAsset.getSeq_no())) {
            nameSb.append(prodAsset.getSeq_no()).append(". ");
        }
        if (!Util.isNullOrEmpty(prodAsset.getChild_catalog())) {
            nameSb.append(prodAsset.getChild_catalog()).append("/");
        }
        String namestr = nameSb.append(prodAsset.getFileName()).toString();
        if(namestr.indexOf("repo.json") != -1){
            namestr = "repo.json";
        }
        return namestr;
    }


    /**
     * ??????map???????????????
     *
     * @param FilesMap    ?????????????????????Map??????
     * @param prodAssets  ??????????????????
     * @param catalogName ?????????
     * @param catalogs    ????????????????????????Map
     */
    private void setMapTypeDate(Map<String, List> FilesMap, Map<String, Map> prodAssets, String catalogName, Map<String, OptionalCatalog> catalogs ,String type) {
        for (Iterator iter = FilesMap.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iter.next();
            String parentName = (String) entry.getKey();
            List<ProdAsset> assets = (List<ProdAsset>) entry.getValue();
            Map<String, Map> childAssetsMap = new HashMap<>();
            for (ProdAsset asset : assets) {
                Map fileMap = new HashMap();
                fileMap.put(Dict.ASSET_NAME, assetShowName(asset));
                fileMap.put(Dict.ASSET_URL, asset.getFile_giturl());
                //??????????????????
                String str;
                String child_catalog;
                String cfg_type;
                String updateType = "#cover:";
                if (asset.getChild_catalog() == null) {
                    child_catalog = "";
                } else {
                    child_catalog = asset.getChild_catalog() + "/";
                }
                if (asset.getCfg_type() == null) {
                    cfg_type = "cfg_core&all#";
                } else {
                    cfg_type = asset.getCfg_type() + "&all#";
                }
                if (asset.isFirst()) {
                    updateType = "#add:";
                }
                if (catalogName.equals("config")) {
                    if(assetShowName(asset).equals("repo.json")){
                        if (type.equals("gray")){
                            str = updateType + cfg_type + child_catalog +assetShowName(asset) +"#/ebank/spdb/grayparam/mobgray/dynamicconfig/syncconfig/xrouter/"+ child_catalog + assetShowName(asset)+"\n"+
                                  updateType + cfg_type + child_catalog +assetShowName(asset) +"#/ebank/spdb/hfgrayparam/mobgray/dynamicconfig/syncconfig/xrouter/"+ child_catalog + assetShowName(asset);
                        }else {
                            str = updateType + cfg_type + child_catalog +assetShowName(asset) +"#/ebank/spdb/params/dynamicconfig/syncconfig/xrouter/"+ child_catalog + assetShowName(asset);
                        }
                        fileMap.put("asset_templateContent", str);
                    }else {
                        str = updateType + cfg_type + child_catalog +assetShowName(asset) +"#/ebank/spdb/configs/"+ child_catalog + assetShowName(asset);
                        fileMap.put("asset_templateContent", str);
                    }
                }
                assembleAssets(childAssetsMap, parentName, null, fileMap);
            }
            for (Map map : childAssetsMap.values()) {
                assembleAssets(prodAssets, catalogName, catalogs.get(catalogName), map);
            }
        }
    }

    /**
     * ?????????????????? ????????????
     *
     * @param catalogName
     * @param catalogs
     * @param stringBuilder
     * @param titleAdded
     * @param isChild
     */
    private void addTitle(String catalogName, Map<String, OptionalCatalog> catalogs,
                          StringBuilder stringBuilder, Set<String> titleAdded, boolean isChild) {
        if (!titleAdded.contains(catalogName)) {
            titleAdded.add(catalogName);
            if (isChild) {
                newLine(stringBuilder, 1, catalogName);
                return;
            }
            OptionalCatalog optionalCatalog = catalogs.get(catalogName);
            newLine(stringBuilder, 0, new StringBuilder("???????????????").append(catalogName).append(SPACING)
                    .append("?????????").append(optionalCatalog == null ? "" : optionalCatalog.getDescription()).toString());

        }
    }

    /**
     * ????????????
     *
     * @param stringBuilder
     * @param tighten       ??????
     * @param content
     */
    private void newLine(StringBuilder stringBuilder, int tighten, String content) {
        for (int i = 0; i < tighten; i++) {
            stringBuilder.append(SPACING);
        }
        stringBuilder.append(content).append(WRAP_WINDOWS);
    }

    private void addOrderTxt(StringBuilder stringBuilder, String catalogName, Set<String> orderTxtAdded) {
        if (!orderTxtAdded.contains(catalogName)) {
            newLine(stringBuilder, 1, ORDER_TXT_NAME);
            orderTxtAdded.add(catalogName);
        }
    }

    @RequestValidate(NotEmptyFields = {Dict.PROD_ID})
    @ApiOperation(value = "????????????????????????")
    @PostMapping(value = "/queryConfigAssets")
    public JsonResult queryConfigAssets(@RequestBody @ApiParam Map<String, String> requestParam) throws Exception {
        String prod_id = requestParam.get(Dict.PROD_ID);
        List result = prodAssetService.queryConfigAssetsByParam(prod_id, Arrays.asList("4", "5", "6", "7","8","9","10"));
        return JsonResultUtil.buildSuccess(result);
    }

    @RequestValidate(NotEmptyFields = {Dict.PROD_ID})
    @ApiOperation(value = "???????????????????????????")
    @PostMapping(value = "/queryDBAssets")
    public JsonResult queryDBAssets(@RequestBody @ApiParam Map<String, String> requestParam) throws Exception {
        String prod_id = requestParam.get(Dict.PROD_ID);
        List result = prodAssetService.queryConfigAssetsByParam(prod_id, Collections.singletonList("3"));
        return JsonResultUtil.buildSuccess(result);
    }

    @RequestValidate(NotEmptyFields = {Dict.PATH, Dict.FULL_PATH, Dict.MODULE_NAME})
    @ApiOperation(value = "??????minio??????")
    @PostMapping(value = "/downloadMinioFile")
    public JsonResult downloadMinioFile(@RequestBody @ApiParam Map<String, String> requestParam) {
        String minioPath = requestParam.get(Dict.FULL_PATH);
        String filePath = requestParam.get(Dict.PATH);
        String moduleName = requestParam.get(Dict.MODULE_NAME);
        fileService.downloadDocumentFile(filePath, minioPath, moduleName);
        return JsonResultUtil.buildSuccess();
    }

    /**
     * ebank-help????????????minio??????
     *
     * @return
     */
    @ApiOperation(value = "??????minio??????")
    @PostMapping(value = "/uploadMinioFile")
    public JsonResult uploadMinioFile(@RequestParam(value = "file") MultipartFile file) throws Exception {
        //msper-web-testapps-20210608_001-001.jar
        //???????????????????????????????????????-????????????-tag.jar
        fileService.uploadWarFile(file.getResource().getFilename(), file.getResource(), "fdev-release");
        return JsonResultUtil.buildSuccess();
    }


    @ApiOperation(value = "????????????????????????????????????")
    @PostMapping(value = "/deAutoUpload", consumes = {"multipart/form-data"})
    @RequestValidate(NotEmptyFields = {Dict.PROD_ID})
    public JsonResult deAutoUpload(@RequestParam(Dict.PROD_ID) String prod_id,
                                   @RequestParam(value = Dict.FILE_ENCODING, required = false) String file_encoding,
                                   @RequestParam(value = Dict.ASSET_CATALOG_NAME, required = false) String asset_catalog_name,
                                   @RequestParam(value = Dict.SOURCE_APPLICATION, required = false) String source_application,
                                   @RequestParam(value = Dict.RUNTIME_ENV, required = false) String runtime_env,
                                   @RequestParam(value = Dict.SEQ_NO, required = false) String seq_no,
                                   @RequestParam(value = Dict.CHILD_CATALOG, required = false) String child_catalog,
                                   @RequestParam(Dict.FILE) MultipartFile[] files) throws Exception {
        // ????????????????????????????????????,????????????
        ProdRecord prodRecord = prodRecordService.queryDetail(prod_id);
//        AssetCatalog assetCatalog = catalogService.queryAssetCatalogByName(prodRecord.getTemplate_id(), asset_catalog_name);
        ReleaseNode releaseNode = releaseNodeService.queryDetail(prodRecord.getRelease_node_name());
        if (CommonUtils.isNullOrEmpty(releaseNode)) {
            throw new FdevException(ErrorConstants.RELEASE_NODE_NOT_EXIST, new String[]{prodRecord.getRelease_node_name()});
        }
//        if (CommonUtils.isNullOrEmpty(asset_catalog_name)) {
//            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{Dict.ASSET_CATALOG_NAME});
//        }
        if (CommonUtils.isNullOrEmpty(files)) {
            throw new FdevException(ErrorConstants.PARAM_CANNOT_BE_EMPTY, new String[]{Dict.FILE});
        }
        for (MultipartFile file : files) {
            if (file.getOriginalFilename().contains(" ")) {
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"????????????????????????"});
            }
            // ?????????
            String filename = file.getOriginalFilename();
//            if (Dict.COMMONCONFIG.equals(asset_catalog_name)) {
//                filename = file.getOriginalFilename().substring(0, file.getOriginalFilename().indexOf('/'));
//                runtime_env = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('/') + 1);
//            } else {
//                filename = file.getOriginalFilename();
//            }
            // ?????????????????????????????????????????????
            ProdAsset savedProdAsset = prodAssetService.queryAssetByName(prod_id, asset_catalog_name, filename, runtime_env);
            if (!CommonUtils.isNullOrEmpty(savedProdAsset)) {
                throw new FdevException(ErrorConstants.REPET_INSERT_REEOR, new String[]{"??????????????????,?????????"});
            }
            // asset_catalog????????????commonconfig??????runtime_env????????????
            /*if (Dict.COMMONCONFIG.equals(asset_catalog_name)) {
                // ????????????
                if (CommonUtils.isNullOrEmpty(file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("/") + 1))) {
                    throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{Dict.RUNTIME_ENV});
                }
            }*/
            // ??????????????????????????????.sql???.sh????????????????????????
            if (!(filename.endsWith(".sql") || filename.endsWith(".sh"))) {
                seq_no = null;
            }
        }
        ProdAsset prodAsset = new ProdAsset();
        // ???????????????????????????????????????
        String source_application_name;
        // ??????source_application???????????????????????????????????????????????????
        if (CommonUtils.isNullOrEmpty(source_application)) {
            // ????????????????????? ????????????????????????,?????????????????????????????????????????????????????????????????????
            if (!(roleService.isGroupReleaseManager(CommonUtils.getSessionUser().getGroup_id())
                    || releaseNode.getRelease_manager().equals(CommonUtils.getSessionUser().getUser_name_en())
                    || releaseNode.getRelease_spdb_manager()
                    .equals(CommonUtils.getSessionUser().getUser_name_en()))) {
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"??????????????????????????????,??????????????????"});
            }
        } else {
            // ????????????????????????????????????????????????????????????????????????
            if (!(roleService.isApplicationManager(source_application)
                    || roleService.isAppSpdbManager(source_application))) {
                throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"??????????????????????????????,??????????????????"});
            }
            prodAsset.setSource_application(source_application);
            Map<String, Object> queryAPP = appService.queryAPPbyid(source_application);
            source_application_name = queryAPP == null ? "????????????" : (String) queryAPP.get("name_zh");
            prodAsset.setSource_application_name(source_application_name);
        }
        prodAsset.setProd_id(prod_id);
        prodAsset.setFile_encoding(file_encoding);
        prodAsset.setAsset_catalog_name(asset_catalog_name);
        prodAsset.setChild_catalog(child_catalog);
        // ????????????
        if (!CommonUtils.isNullOrEmpty(seq_no)) {
            List<ProdAsset> assetsWithSeqno = prodAssetService.queryAssetsWithSeqno(prod_id, asset_catalog_name);
            if (!CommonUtils.isNullOrEmpty(assetsWithSeqno)) {
                prodAsset.setSeq_no(String.valueOf(assetsWithSeqno.size() + 1));
            } else {
                prodAsset.setSeq_no("1");
            }
        }
        Map map;
        try {
            map = prodAssetService.deAutoCreate(prodAsset, files, child_catalog);
        } catch (Exception e) {
            logger.error("file upload failed:{}", e);
            throw e;
        }
        return JsonResultUtil.buildSuccess(map);
    }

    @RequestValidate(NotEmptyFields = {Dict.PROD_ID})
    @ApiOperation(value = "????????????????????????(??????????????????id?????????)")
    @PostMapping(value = "/queryDeAutoAssets")
    public JsonResult queryDeAutoAssets(@RequestBody @ApiParam Map<String, String> requestParam) throws Exception {
        String prod_id = requestParam.get(Dict.PROD_ID);
        List result = prodAssetService.queryAssetsByProdId(prod_id);
        return JsonResultUtil.buildSuccess(result);
    }

    @RequestValidate(NotEmptyFields = {Dict.PROD_ID})
    @ApiOperation(value = "??????????????????????????????")
    @PostMapping(value = "/queryDeAutoAllProdAssets")
    public JsonResult queryDeAutoAllProdAssets(@RequestBody @ApiParam Map<String, String> requestParam) throws Exception {
        // ????????????map
        Map<String, Object> retuen_map = new HashMap<>();
        String prod_id = requestParam.get(Dict.PROD_ID);
        ProdRecord prodRecord = prodRecordService.queryDetail(prod_id);
        if (!CommonUtils.isNullOrEmpty(prodRecord.getTemplate_properties())) {
            retuen_map.put(Dict.TEMPLATE_PROPERTIES, prodRecord.getTemplate_properties());
        }
//        Map<String, String> prodImages = prodRecordService.queryBeforePordImages(prodRecord.getRelease_node_name(), prodRecord.getVersion());
        Map<String, Map> prodAssets = new HashMap();
        // ????????????????????????
        List<Map> prodApplications = prodApplicationService.queryApplications(new ProdApplication(prod_id));
        Map<String, OptionalCatalog> catalogs = optionalCatalogService.queryOptionalCatalogs();
        Set<String> orderTxtAdded = new HashSet<>();
//        StringBuilder sb = new StringBuilder();
//        prodApplications.forEach(prodApplication -> {
//            Map applicationMap = new HashMap();
//            applicationMap.put(Dict.ASSET_NAME, prodApplication.get(Dict.APP_NAME_EN));
//            applicationMap.put(Dict.PRO_IMAGE_URI, prodApplication.get(Dict.PRO_IMAGE_URI));
//            String catalogName = (String) prodApplication.get(Dict.ASSET_CATALOG);
//            showOrderTxt(prodAssets, catalogName, catalogs.get(catalogName), orderTxtAdded);
//            assembleAssets(prodAssets, catalogName, catalogs.get(catalogName), applicationMap);
////            boolean isPackaged = !CommonUtils.isNullOrEmpty(prodImages)
////                    && !CommonUtils.isNullOrEmpty(prodImages.get(prodApplication.get(Dict.PRO_IMAGE_URI)));
////            if(isPackaged) {
////                sb.append("[").append(prodApplication.get(Dict.APP_NAME_EN)).append("]");
////            }
//        });
//        String application_tips = sb.toString();
//        if(!CommonUtils.isNullOrEmpty(application_tips)) {
//            retuen_map.put(Dict.APPLICATION_TIPS,
//                    new StringBuilder().append("??????").append(application_tips).append("???????????????????????????????????????????????????????????????????????????????????????").toString());
//        }

        // ??????????????????????????????
        Map<String, Object> sortedAssets = prodAssetService.querySortedAssets(prod_id);
        for (Iterator iterator = sortedAssets.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry sortedAssetsEntry = (Map.Entry) iterator.next();
            String catalogName = (String) sortedAssetsEntry.getKey();
            if (sortedAssetsEntry.getValue() instanceof List) { // ???????????????????????????
                List<ProdAsset> assets = (List<ProdAsset>) sortedAssetsEntry.getValue();
                assets.forEach(asset -> {
                    Map fileMap = new HashMap();
                    fileMap.put(Dict.ASSET_NAME, assetShowName(asset));
                    fileMap.put(Dict.ASSET_URL, asset.getFile_giturl());
                    if (!Util.isNullOrEmpty(asset.getSeq_no())) {
                        showOrderTxt(prodAssets, catalogName, catalogs.get(catalogName), orderTxtAdded);
                    }
                    assembleAssets(prodAssets, catalogName, catalogs.get(catalogName), fileMap);
                });
            } else if (sortedAssetsEntry.getValue() instanceof Map) {  //??????????????????
                Map<String, List> envProdFiles = (Map<String, List>) sortedAssetsEntry.getValue();
                // ???????????????????????????
                for (Iterator iter = envProdFiles.entrySet().iterator(); iter.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    String envName = (String) entry.getKey();
                    List<ProdAsset> assets = (List<ProdAsset>) entry.getValue();
                    Map<String, Map> envAssetsMap = new HashMap<>();
                    for (ProdAsset asset : assets) {
                        Map fileMap = new HashMap();
                        fileMap.put(Dict.ASSET_NAME, assetShowName(asset));
                        fileMap.put(Dict.ASSET_URL, asset.getFile_giturl());
                        assembleAssets(envAssetsMap, envName, null, fileMap);
                    }
                    for (Map map : envAssetsMap.values()) {
                        assembleAssets(prodAssets, catalogName, catalogs.get(catalogName), map);
                    }
                }
            }
        }

        // ????????????????????????
        Map paramMap = new HashMap<>();
        paramMap.put(Dict.PROD_ID, prod_id);
        List<Map<String, Object>> appScaleList = prodAppScaleService.queryAppScale(paramMap);
        if (!Util.isNullOrEmpty(appScaleList)) {
            Map<String, Map> orderMap = new HashMap();
            List<Map<String, String>> env_scales = (List<Map<String, String>>) appScaleList.get(0).get(Dict.ENV_SCALES);
            for (Map<String, String> map : env_scales) {
                Map childMap = new HashMap();
                childMap.put(Dict.ASSET_NAME, ORDER_TXT_NAME);
                assembleAssets(orderMap, map.get(Dict.ENV_NAME), null, childMap);
            }
            for (Map map : orderMap.values()) {
                OptionalCatalog optionalCatalog = new OptionalCatalog();
                optionalCatalog.setDescription("????????????");
                assembleAssets(prodAssets, Dict.DOCKER_SCALE, optionalCatalog, map);
            }
        }
        retuen_map.put(Dict.PROD_ASSETS, prodAssets.values());
        return JsonResultUtil.buildSuccess(retuen_map);
    }

    @ApiOperation(value = "????????????id??????????????????????????????")
    @PostMapping(value = "/queryAwsConfigByGroupId")
    public JsonResult queryAwsConfigByGroupId(@RequestBody @ApiParam Map<String, String> requestParam) throws Exception {
        String groupId = requestParam.get(Dict.GROUP_ID);
        List<AwsConfigure> result = prodAssetService.queryAwsConfigByGroupId(groupId);
        return JsonResultUtil.buildSuccess(result);
    }

}
