export default {
  fuser: {
    createAuthCode: '/fuser/api/auth/createAuthCode',
    forgetPassWord: '/fuser/api/auth/forgetPassWord',
    getVerifyCode: '/fuser/api/auth/getVerifyCode',
    login: '/fuser/api/auth/login',
    onceLogin: '/fuser/api/auth/onceLogin',
    queryOAuth: '/fuser/api/auth/queryOAuth',
    companyAdd: '/fuser/api/company/add',
    companyDelete: '/fuser/api/company/delete',
    companyQuery: '/fuser/api/company/query',
    companyUpdate: '/fuser/api/company/update',
    groupAdd: '/fuser/api/group/add',
    groupDelete: '/fuser/api/group/delete',
    groupQuery: '/fuser/api/group/query',
    queryByGroupId: '/fuser/api/group/queryByGroupId',
    queryChildGroupById: '/fuser/api/group/queryChildGroupById',
    groupUpdate: '/fuser/api/group/update',
    labelAdd: '/fuser/api/label/add',
    labelDelete: '/fuser/api/label/delete',
    labelQuery: '/fuser/api/label/query',
    permissionQuery: '/fuser/api/permission/query',
    permissionUpdate: '/fuser/api/permission/update',
    roleAdd: '/fuser/api/role/add',
    roleDelete: '/fuser/api/role/delete',
    roleQuery: '/fuser/api/role/query',
    roleUpdate: '/fuser/api/role/update',
    userAdd: '/fuser/api/user/add',
    currentUser: '/fuser/api/user/currentUser',
    userDelete: '/fuser/api/user/delete',
    userQuery: '/fuser/api/user/query',
    queryUserCoreData: '/fuser/api/user/queryUserCoreData',
    userUpdate: '/fuser/api/user/update',
    updateUserIsOnceLogin: '/fuser/api/user/updateUserIsOnceLogin',
    queryCommissionEvent: '/fuser/api/userCommissionEvent/queryCommissionEvent',
    updateLabelById: '/fuser/api/userCommissionEvent/updateLabelById',
    simulateUser: '/fuser/api/auth/simulateUser',
    resetPassword: '/fuser/api/auth/updatePassword',
    queryArea: '/fuser/api/user/queryArea',
    queryUserStatis: '/fuser/api/user/queryUserStatis',
    queryFunction: '/fuser/api/user/queryfunction',
    queryRank: '/fuser/api/user/queryrank',
    queryTaskNumByGroup: '/fuser/api/user/queryTaskNumByGroup',
    getJobUser: '/fuser/api/user/getJobUser',
    queryUserPagination: '/fuser/api/user/queryUser',
    queryReBuildGroupName: '/fuser/api/group/reBuildGroupName',
    queryGroup: '/fuser/api/group/queryGroup',
    queryApprovalList: '/fuser/api/approval/queryApprovalList',
    updateApprovalStatus: '/fuser/api/approval/updateApprovalStatus',
    queryRoleForLDAP: '/fuser/api/role/queryRoleForLDAP',
    updateGitToken: '/fuser/api/user/updateGitToken',
    queryIpmpLeadTeam: '/fuser/api/ipmpTeam/queryIpmpLeadTeam', //?????????????????????????????????
    queryIpmpUser: '/fuser/api/ipmpUser/queryIpmpUser', //??????ipmp????????????
    queryUserMenu: '/fuser/api/menu/queryUserMenu', // ???????????????????????????
    getAllUserAndRole: '/fuser/api/user/getAllUserAndRole' //??????????????????????????????????????????
  },
  fapp: {
    queryAppAscriptionGroup: '/fapp/api/app/queryAppAscriptionGroup',
    queryPipelinesWithJobsPage:
      '/fapp/api/gitlabapi/queryPipelinesWithJobsPage',
    getPipelineById: '/fapp/api/vip/getPipelineById',
    vipGetLog: '/fapp/api/vip/getLog',
    runPipeline: '/fapp/api/vip/runPipeline',
    cancelVipDeploy: '/fapp/api/vip/cancel',
    queryPipelinesList: '/fapp/api/vip/pipelines',
    queryWithEnv: '/fapp/api/app/queryWithEnv',
    appAdd: '/fapp/api/app/add',
    appDeleteById: '/fapp/api/app/deleteAppById',
    createPipelineSchedule: '/fapp/api/app/createPipelineSchedule',
    appQuery: '/fapp/api/app/query',
    queryAppNum: '/fapp/api/app/queryAppNum',
    appSave: '/fapp/api/app/save',
    saveByAsync: '/fapp/api/app/saveByAsync',
    appUpdate: '/fapp/api/app/update',
    createPipeline: '/fapp/api/gitlabapi/createPipeline',
    getGroup: '/fapp/api/gitlabapi/getGroup',
    getProjectBranchList: '/fapp/api/gitlabapi/getProjectBranchList',
    queryAllBranch: '/fapp/api/gitlabapi/queryAllBranch',
    queryAppNameEnByGitlabProjectId:
      '/fapp/api/gitlabapi/queryAppNameEnByGitlabProjectId',
    queryPipelinesWithJobs: '/fapp/api/gitlabapi/queryPipelinesWithJobs',
    queryProject: '/fapp/api/gitlabapi/queryProject',
    queryTraces: '/fapp/api/gitlabapi/queryTraces',
    gitlabciFindById: '/fapp/api/gitlabci/findbyid',
    gitlabciQuery: '/fapp/api/gitlabci/query',
    gitlabciSave: '/fapp/api/gitlabci/save',
    gitlabciUpdate: '/fapp/api/gitlabci/update',
    gitlabapigetGroupGit: '/fapp/api/gitlabapi/getGroupGit',
    typeQuery: '/fapp/api/type/query',
    findById: '/fapp/api/app/findbyid',
    queryAutoTest: '/fapp/api/autotest/query',
    updateAutoTest: '/fapp/api/autotest/update',
    queryMantisWithAppId: '/fapp/api/app/queryAppMantis',
    myAppQuery: '/fapp/api/app/queryMyApps',
    queryPagination: '/fapp/api/app/queryPagination',
    queryApps: '/fapp/api/app/queryApps',
    queryAppSystem: '/fapp/api/serviceSystem/querySystem',
    bindSystem: '/fapp/api/app/bindSystem',
    getTestFlag: '/fapp/api/app/getTestFlag' //??????????????????????????????
  },
  fenvconfig: {
    saveSitConfigProperties:
      '/fenvconfig/api/v2/configfile/saveSitConfigProperties',
    getVerifyCode: '/fenvconfig/api/v2/verifycode/getVerifyCode',
    exportDependencySearchResult:
      '/fenvconfig/api/v2/configfile/exportDependencySearchResult',
    previewConfigFile: '/fenvconfig/api/v2/configfile/previewConfigFile',
    queryConfigDependency:
      '/fenvconfig/api/v2/configfile/queryConfigDependency',
    queryConfigTemplate: '/fenvconfig/api/v2/configfile/queryConfigTemplate',
    saveConfigTemplate: '/fenvconfig/api/v2/configfile/saveConfigTemplate',
    envAdd: '/fenvconfig/api/v2/env/add',
    envDelete: '/fenvconfig/api/v2/env/delete',
    envQuery: '/fenvconfig/api/v2/env/query',
    queryEnvByAppId: '/fenvconfig/api/v2/env/queryEnvByAppId',
    queryByLabels: '/fenvconfig/api/v2/env/queryByLabels',
    queryByLabelsFuzzy: '/fenvconfig/api/v2/env/queryByLabelsFuzzy',
    envUpdate: '/fenvconfig/api/v2/env/update',
    modelAdd: '/fenvconfig/api/v2/model/add',
    modelTempAdd: '/fenvconfig/api/v2/modelTemplate/add',
    modelDelete: '/fenvconfig/api/v2/model/delete',
    modelQuery: '/fenvconfig/api/v2/model/query',
    modelTempQuery: '/fenvconfig/api/v2/modelTemplate/query',
    pirvateModelQuery: '/fenvconfig/api/v2/model/queryExcludePirvateModel',
    modelQueryFuzz: '/fenvconfig/api/v2/model/queryFuzz',
    queryModelCategory: '/fenvconfig/api/v2/model/queryModelCategory',
    modelUpdate: '/fenvconfig/api/v2/model/update',
    outsideTemplateSave:
      '/fenvconfig/api/v2/outSideTemplate/outsideTemplateSave',
    outSideTemplateQuery: '/fenvconfig/api/v2/outSideTemplate/query',
    outSideTemplateUpdate: '/fenvconfig/api/v2/outSideTemplate/update',
    copyModelEnv: '/fenvconfig/api/v2/var/CopyModelEnv',
    updatyeCopyModelEnv: '/fenvconfig/api/v2/var/UpdatyeCopyModelEnv',
    varDelete: '/fenvconfig/api/v2/var/delete',
    varQuery: '/fenvconfig/api/v2/var/query',
    queryVarByEnvAndType: '/fenvconfig/api/v2/var/queryVarByEnvAndType',
    queryAutoEnv: '/fenvconfig/api/v2/env/queryByLabelsFuzzy',
    saveModelEnv: '/fenvconfig/api/v2/modelEnvUpdateApply/save',
    queryModelMessage: '/fenvconfig/api/v2/modelEnvUpdateApply/list',
    compare: '/fenvconfig/api/v2/modelEnvUpdateApply/compare',
    finish: '/fenvconfig/api/v2/modelEnvUpdateApply/finish',
    downloadAppInfo: '/fenvconfig/api/v2/modelEnvUpdateApply/downloadAppInfo',
    updateModelMessage: '/fenvconfig/api/v2/modelEnvUpdateApply/update',
    cancelModelMessage: '/fenvconfig/api/v2/modelEnvUpdateApply/cancel',
    saveDevConfigProperties:
      '/fenvconfig/api/v2/configfile/saveDevConfigProperties',
    queryModelSetList: '/fenvconfig/api/v2/modelSet/list',
    deleteModelSet: '/fenvconfig/api/v2/modelSet/delete',
    getType: '/fenvconfig/api/v2/modelSet/getTemplate',
    getModles: '/fenvconfig/api/v2/modelSet/getModles',
    saveModels: '/fenvconfig/api/v2/modelSet/save',
    updateModels: '/fenvconfig/api/v2/modelSet/update',
    queryAppProInfo: '/fenvconfig/api/v2/appProInfo/query',
    getJsonSchema: '/fenvconfig/api/v2/jsonSchema/getJsonSchema',
    bindAppInfo: '/fenvconfig/api/v2/appProInfo/bind',
    queryDeploy: '/fenvconfig/api/v2/appProInfo/queryDeploy',
    queryAllLabels: '/fenvconfig/api/v2/env/queryAllLabels',
    queryEnvKey: '/fenvconfig/api/v2/model/queryEnvKey',
    queryDeployDetail: '/fenvconfig/api/v2/appProInfo/queryBindMsgByApp',
    queryRealTimeBindMsg: '/fenvconfig/api/v2/appProInfo/queryRealTimeBindMsg',
    queryProEnvByAppId: '/fenvconfig/api/v2/appEnv/queryProEnvByAppId',
    pageQuery: '/fenvconfig/api/v2/var/pageQuery',
    queryModelEnvByValue: '/fenvconfig/api/v2/var/queryModelEnvByValue',
    checkConnectionDocker:
      '/fenvconfig/api/v2/modelEnvUpdateApply/checkConnectionDocker',
    queryPage: '/fenvconfig/api/v2/model/pageQuery',
    queryTempPage: '/fenvconfig/api/v2/modelTemplate/pageQuery',
    queryTemplateContainsModel:
      '/fenvconfig/api/v2/modelSet/queryTemplateContainsModel',
    getMappingHistoryList: '/fenvconfig/api/v2/history/getMappingHistoryList',
    getMappingHistoryDetail:
      '/fenvconfig/api/v2/history/getMappingHistoryDetail',
    configFilePreview: '/fenvconfig/api/v2/configfile/configFilePreview',
    queryEnvKeyList: '/fenvconfig/api/v2/model/queryNoCiEnvKeyList',
    queryConfigModel: '/fenvconfig/api/v2/model/configModel',
    batchPreviewConfigFile:
      '/fenvconfig/api/v2/configfile/batchPreviewConfigFile',
    queryModelEnvByModelNameEn:
      '/fenvconfig/api/v2/var/queryModelEnvByModelNameEn',
    definedDeploy: '/fenvconfig/api/v2/appProInfo/definedDeploy'
  },
  finterface: {
    getInterfaceDetailById: '/finterface/api/interface/getInterfaceDetailById',
    getInterfacesUrl: '/finterface/api/interface/getInterfacesUrl',
    isAppManager: '/finterface/api/interface/isAppManager',
    isTaskManager: '/finterface/api/interface/isTaskManager',
    queryInterfaceDetailById:
      '/finterface/api/interface/queryInterfaceDetailById',
    queryInterfaceList: '/finterface/api/interface/queryInterfaceList',
    queryInterfaceRelation: '/finterface/api/interface/queryInterfaceRelation',
    queryInterfaceVersions: '/finterface/api/interface/queryInterfaceVersions',
    queryInterfacesList: '/finterface/api/interface/queryInterfacesList',
    queryTransByVersion: '/finterface/api/interface/queryTransByVersion',
    queryTransDetailById: '/finterface/api/interface/queryTransDetailById',
    queryTransList: '/finterface/api/interface/queryTransList',
    exportTransList: '/finterface/api/interface/exportTransList',
    scanInterface: '/finterface/api/interface/scanInterface',
    taskScanInterface: '/finterface/api/interface/taskScanInterface',
    updateTransTags: '/finterface/api/interface/updateTransTags',
    queryScanRecord: '/finterface/api/interface/queryScanRecord',
    queryTransRelation: '/finterface/api/interface/queryTransRelation',
    downloadRestRelationExcel:
      '/finterface/api/interface/downloadRestRelationExcel',
    updateParamDescription: '/finterface/api/interface/updateParamDescription',
    modifyParamDescription: '/finterface/api/interface/modifyParamDescription',
    queryApplicationList:
      '/finterface/api/interfaceApplication/queryApplicationList',
    updateApplicationStatus:
      '/finterface/api/interfaceApplication/updateApplicationStatus',
    interfaceCallRequest:
      '/finterface/api/interfaceApplication/interfaceCallRequest',
    isManagers: '/finterface/api/interfaceApplication/isManagers',
    queryIsNoApplyInterface:
      '/finterface/api/interfaceApplication/queryIsNoApplyInterface',
    queryInterfaceStatistics: '/finterface/api/interface/interfaceStatistics',
    queryRoutes: '/finterface/api/interface/queryRoutes',
    queryRoutesRelation: '/finterface/api/interface/queryRoutesRelation',
    queryRoutesDetail: '/finterface/api/interface/queryRoutesDetail',
    getServiceChainInfo: '/finterface/api/interface/getServiceChainInfo',
    queryAppJsonList: '/finterface/api/interface/queryAppJsonList',
    queryTotalJsonList: '/finterface/api/interface/queryTotalJsonList',
    queryTotalJsonHistory: '/finterface/api/interface/queryTotalJsonHistory',
    queryRoutesDetailVer: '/finterface/api/interface/queryRoutesDetailVer',
    queryYapiList: '/finterface/api/interface/yapiProjectList',
    importYapiProject: '/finterface/api/interface/importYapiProject',
    convertJsonSchema: '/finterface/api/interface/convertJsonSchema',
    queryYapiDetail: '/finterface/api/interface/yapiInterfaceList',
    deleteYapiProject: '/finterface/api/interface/deleteYapiProject',
    deleteYapiInterface: '/finterface/api/interface/deleteYapiInterface',
    importYapiInterface: '/finterface/api/interface/importYapiInterface'
  },
  fnotify: {
    queryAnnounce: '/fnotify/queryAnnounce',
    queryMessage: '/fnotify/queryMessage',
    sendAnnounce: '/fnotify/sendAnnounce',
    updateNotifyStatus: '/fnotify/updateNotifyStatus',
    queryMessageByType: '/fnotify/queryMessageByType'
  },
  frelease: {
    addManualNoteInfo: '/frelease/api/note/addManualNoteInfo', // ??????????????????????????????
    queryManualNoteInfo: '/frelease/api/note/queryManualNoteInfo', // ??????????????????????????????
    updateManualNoteInfo: '/frelease/api/note/updateManualNoteInfo', // ??????????????????????????????
    createManualNote: '/frelease/api/note/createManualNote', // ????????????????????????
    queryBatchAppIdByNoteId: '/frelease/api/batch/queryBatchAppIdByNoteId', // ??????????????????????????????id
    updateNoteBatchNo: '/frelease/api/batch/updateNoteBatchNo', // ????????????????????????
    createBatchTask: '/frelease/api/batch/createBatchTask', // ??????????????????
    updateBatchTask: '/frelease/api/batch/updateBatchTask', // ??????????????????
    deteleBatchTask: '/frelease/api/batch/deteleBatchTask', // ??????????????????
    queryBatchTask: '/frelease/api/batch/queryBatchTask', // ??????????????????
    addBatchTask: '/frelease/api/batch/addBatchTask', // ??????????????????
    queryBatchTaskList: '/frelease/api/batch/queryBatchTaskList', // ????????????????????????
    deleteNote: '/frelease/api/note/deleteNote',
    generateReleaseNotes: '/frelease/api/note/generateReleaseNotes', // ??????????????????
    addNoteSql: '/frelease/api/note/addNoteSql', // ???????????????????????????
    queryNoteSql: '/frelease/api/note/queryNoteSql', // ???????????????????????????
    deleteNoteSql: '/frelease/api/note/deleteNoteSql', // ???????????????????????????
    updateNoteSeqNo: '/frelease/api/note/updateNoteSeqNo', // ??????????????????
    lockNote: '/frelease/api/note/lockNote', // ??????????????????
    queryAllGroupAbbr: '/frelease/api/release/queryAllGroupAbbr', // ??????????????????
    updateNoteConfiguration: '/frelease/api/note/updateNoteConfiguration', // ??????????????????
    deleteNoteConfiguration: '/frelease/api/note/deleteNoteConfiguration', // ??????????????????
    addNoteConfiguration: '/frelease/api/note/addNoteConfiguration', // ??????????????????
    queryNoteConfiguration: '/frelease/api/note/queryNoteConfiguration', // ??????????????????
    queryNoteDetail: '/frelease/api/note/queryNoteDetail', // ??????????????????
    addNoteService: '/frelease/api/note/addNoteService', // ????????????????????????
    queryNoteService: '/frelease/api/note/queryNoteService', // ????????????????????????
    deleteNoteService: '/frelease/api/note/deleteNoteService', // ????????????????????????
    updateNoteService: '/frelease/api/note/updateNoteService', // ????????????????????????
    createNote: '/frelease/api/note/createNote', // ??????????????????
    queryReleaseNote: '/frelease/api/note/queryReleaseNote', // ????????????????????????
    queryRqrmntFileUri: '/frelease/api/rqrmnt/queryRqrmntFileUri', //???????????????????????????minio??????
    queryTaskRqrmntAlert: '/frelease/api/rqrmnt/queryTaskRqrmntAlert',
    queryAppTagPiplines: '/frelease/api/application/queryAppTagPiplines',
    queryTestRunPiplines: '/frelease/api/application/queryTestRunPiplines',
    queryApplicationTags: '/frelease/api/application/queryApplicationTags',
    queryApplications: '/frelease/api/application/queryApplications',
    queryComponent: '/frelease/api/application/queryComponent',
    queryTargetVersion: '/frelease/api/application/queryTargetVersion',
    queryTasksReviews: '/frelease/api/application/queryTasksReviews',
    sendEmailForTaskManagers:
      '/frelease/api/application/sendEmailForTaskManagers',
    setTestEnvs: '/frelease/api/application/setTestEnvs',
    packageFromTag: '/frelease/api/devops/packageFromTag',
    relDevops: '/frelease/api/devops/relDevops',
    queryImageTags: '/frelease/api/devopsrecord/queryImageTags',
    addAppScale: '/frelease/api/appscale/add',
    addApplication: '/frelease/api/release/addApplication',
    addGitlabAsset: '/frelease/api/release/addGitlabAsset',
    queryProfile: '/frelease/api/release/queryProfile',
    releaseAudit: '/frelease/api/release/audit',
    releaseCreate: '/frelease/api/release/create',
    releaseDelete: '/frelease/api/release/delete',
    deleteAppScale: '/frelease/api/appscale/delete',
    deleteApplication: '/frelease/api/release/deleteApplication',
    deleteAsset: '/frelease/api/release/deleteAsset',
    deleteAssets: '/frelease/api/release/deleteAssets',
    generateProdExcel: '/frelease/api/release/generateProdExcel',
    getReleaseVersion: '/frelease/api/release/getReleaseVersion',
    releaseQuery: '/frelease/api/release/query',
    queryAppScale: '/frelease/api/appscale/query',
    releaseQueryApplications: '/frelease/api/release/queryApplications',
    queryAssets: '/frelease/api/release/queryConfigAssets',
    queryDetail: '/frelease/api/release/queryDetail',
    queryGroupAbbr: '/frelease/api/release/queryGroupAbbr',
    queryPlan: '/frelease/api/release/queryPlan',
    queryProdInfo: '/frelease/api/release/queryProdInfo',
    queryResourceBranches: '/frelease/api/release/queryResourceBranches',
    queryResourceFiles: '/frelease/api/release/queryResourceFiles',
    queryResourceProjects: '/frelease/api/release/queryResourceProjects',
    querySysRlsInfo: '/frelease/api/release/querySysRlsInfo',
    setImageTag: '/frelease/api/release/setImageTag',
    setTemplate: '/frelease/api/release/setTemplate',
    trace: '/frelease/api/release/trace',
    releaseUpdate: '/frelease/api/release/update',
    updateAppScale: '/frelease/api/appscale/update',
    updateAssetSeqNo: '/frelease/api/release/updateAssetSeqNo',
    updateGroupAbbr: '/frelease/api/release/updateGroupAbbr',
    updateSysRlsInfo: '/frelease/api/release/updateSysRlsInfo',
    releaseNodeCreate: '/frelease/api/releasenode/create',
    getReleaseNodeName: '/frelease/api/releasenode/getReleaseNodeName',
    releaseNodeQueryDetail: '/frelease/api/releasenode/queryDetail',
    queryReleaseNodes: '/frelease/api/releasenode/queryReleaseNodes',
    releaseNodeUpdate: '/frelease/api/releasenode/update',
    taskAdd: '/frelease/api/task/add',
    taskAuditAdd: '/frelease/api/task/auditAdd',
    changeReleaseNode: '/frelease/api/task/changeReleaseNode',
    deleteTask: '/frelease/api/task/deleteTask',
    queryDetailByTaskId: '/frelease/api/task/queryDetailByTaskId',
    queryTasks: '/frelease/api/task/queryTasks',
    updateTaskArchived: '/frelease/api/task/updateTaskArchived',
    templateCreate: '/frelease/api/template/create',
    templateDelete: '/frelease/api/template/delete',
    templateQuery: '/frelease/api/template/query',
    templateQueryDetail: '/frelease/api/template/queryDetail',
    queryOptionalCatalog: '/frelease/api/template/queryOptionalCatalog',
    templateUpdate: '/frelease/api/template/update',
    queryAllProdAssets: '/frelease/api/release/queryAllProdAssets',
    queryDeAutoAllProdAssets: '/frelease/api/release/queryDeAutoAllProdAssets',
    exportProdDirection: '/frelease/api/release/exportProdDirection',
    tasksInSitStage: '/frelease/api/application/tasksInSitStage',
    queryGroupSysAbbr: '/frelease/api/release/queryGroupSysAbbr',
    updateGroupSysAbbr: '/frelease/api/release/updateGroupSysAbbr',
    queryModuleType: '/frelease/api/optionalcatalog/query',
    deleteModuleType: '/frelease/api/optionalcatalog/delete',
    addModuleType: '/frelease/api/optionalcatalog/add',
    updateModuleType: '/frelease/api/optionalcatalog/update',
    queryScript: '/frelease/api/automationparam/query',
    deleteScript: '/frelease/api/automationparam/delete',
    addScript: '/frelease/api/automationparam/add',
    updateScript: '/frelease/api/automationparam/update',
    queryAutomationEnv: '/frelease/api/automationenv/queryToProdScale',
    deleteAutomationEnv: '/frelease/api/automationenv/delete',
    addAutomationEnv: '/frelease/api/automationenv/add',
    updateAutomationEnv: '/frelease/api/automationenv/update',
    queryDBAssets: '/frelease/api/release/queryDBAssets',
    createTestrunBranch: '/frelease/api/testrun/createTestrunBranch',
    mergeTaskBranch: '/frelease/api/testrun/mergeTaskBranch',
    queryTaskByTestRunId: '/frelease/api/testrun/queryTaskByTestRunId',
    findByProdId: '/frelease/api/prodMediaFile/findByProdId',
    changeConfirmConf: '/frelease/api/application/confirmConfigChanges',
    changeReleaseConf: '/frelease/api/release/confirmConfigChanges',
    queryByReleaseNodeName: '/frelease/api/rqrmnt/queryByReleaseNodeName',
    deleteFile: '/frelease/api/rqrmnt/deleteFile',
    taskChangeNotise: '/frelease/api/rqrmnt/taskChangeNotise',
    downloadFiles: '/frelease/api/rqrmnt/download',
    addSourceReview: '/frelease/api/rqrmnt/addSourceReview',
    querySourceReviewDetail: '/frelease/api/rqrmnt/querySourceReviewDetail',
    addSystemTestFile: '/frelease/api/rqrmnt/addSystemTestFile',
    pullGrayBranch: '/frelease/api/task/pullGrayBranch',
    querySystem: '/frelease/api/template/querySystem',
    queryExcelTemplate: '/frelease/api/template/queryExcelTemplate',
    editFakeInfo: '/frelease/api/application/editFakeInfo',
    iOSOrAndroidAppPublish: '/frelease/api/application/release',
    createBigReleaseNode: '/frelease/api/releasenode/createBigReleaseNode',
    updateBigReleasenode: '/frelease/api/releasenode/updateBigReleaseNode',
    queryBigReleaseNodes: '/frelease/api/releasenode/queryBigReleaseNodes',
    queryBigReleaseNodeDetail:
      '/frelease/api/releasenode/queryBigReleaseNodeDetail',
    queryContactInfo: '/frelease/api/releasenode/queryContactInfo',
    queryRqrmntInfoList: '/frelease/api/rqrmnt/queryRqrmntInfoList',
    queryByReleaseDate: '/frelease/api/releaseCycle/queryByReleaseDate',
    updateBigReleaseDate: '/frelease/api/releaseCycle/update',
    queryProWantTasks: '/frelease/api/task/queryProWantTasks',
    queryBatchInfoByTaskId: '/frelease/api/releasebatch/queryBatchInfoByTaskId',
    addBatch: '/frelease/api/releasebatch/addBatch',
    queryRqrmntInfoListByType: '/frelease/api/rqrmnt/queryRqrmntInfoListByType',
    exportRqrmntInfoList: '/frelease/api/rqrmnt/exportRqrmntInfoList',
    updateRqrmntInfo: '/frelease/api/rqrmnt/updateRqrmntInfo',
    exportRqrmntInfoListByType:
      '/frelease/api/rqrmnt/exportRqrmntInfoListByType',
    queryBatchInfoByAppId: '/frelease/api/releasebatch/queryBatchInfoByAppId',
    queryReleaseSystem: 'frelease/api/rqrmnt/queryReleaseSystem',
    queryRqrmntInfoTasks: '/frelease/api/rqrmnt/queryRqrmntInfoTasks',
    exportSpecialRqrmntInfoList:
      '/frelease/api/rqrmnt/exportSpecialRqrmntInfoList',
    queryDeAutoAssets: '/frelease/api/release/queryDeAutoAssets',
    deAutoUpload: '/frelease/api/release/deAutoUpload',
    queryPackageTags: '/frelease/api/devopsrecord/queryPackageTags',
    setPackageTag: '/frelease/api/release/setPackageTag',
    queryConfigApplication: '/frelease/api/config/queryConfigApplication',
    addConfigApplication: '/frelease/api/config/addConfigApplication',
    checkConfigApplication: '/frelease/api/config/checkConfigApplication',
    createConfig: '/frelease/api/config/createConfig',
    deleteConfig: '/frelease/api/config/deleteConfig',
    queryConfigSum: '/frelease/api/config/queryConfigSum',
    confirmChanges: '/frelease/api/release/confirmChanges',
    deleteReleaseNode: '/frelease/api/releasenode/delete',
    querySystemDetailByProdId:
      '/frelease/api/release/querySystemDetailByProdId',
    queryDbPath: '/frelease/api/dbreview/queryDbPath',
    uploadAssets: '/frelease/api/dbreview/uploadAssets',
    addEsfCommonConfig: '/frelease/api/release/uploadAssets',
    updateEsfcommonconfigAssets:
      '/frelease/api/release/updateEsfcommonconfigAssets', // ??????esfcommonconfig
    queryProdDir: '/frelease/api/release/queryProdDir',
    updateProdDir: '/frelease/api/release/updateProdDir',
    updateProdDeploy: '/frelease/api/release/updateProdDeploy',
    queryDeployTypeByAppId: '/frelease/api/release/queryDeployTypeByAppId', // ????????????id??????????????????
    updateAwsAssetGroupId: '/frelease/api/release/updateAwsAssetGroupId', // ?????????????????????
    queryAwsConfigByGroupId: '/frelease/api/release/queryAwsConfigByGroupId', // ???????????????????????????????????????
    addRedLineScanReport: '/frelease/api/rqrmnt/addRedLineScanReport', // ????????????????????????
    whetherWriteOrder: '/frelease/api/dbreview/whetherWriteOrder', // ????????????Order
    queryReplicasnu: '/frelease/api/release/queryReplicasnu', // ???????????????
    updateReplicasnu: '/frelease/api/release/updateReplicasnu', // ???????????????
    addEsfRegistration: '/frelease/api/esf/addEsfRegistration', // esf??????
    queryEsfConfiguration: '/frelease/api/esf/queryEsfConfiguration', //  ??????????????????????????????????????????????????????
    delEsf: '/frelease/api/esf/delEsf', // esf??????
    updateEsf: '/frelease/api/esf/updateEsf', // esf??????
    queryAppStatus: '/frelease/api/esf/queryAppStatus', // ???????????????????????????
    queryAppsByAddEsf: '/frelease/api/esf/queryApps', // ??????esf???????????????
    queryThreeLevelGroups: '/frelease/api/releasenode/queryThreeLevelGroups' // ??????????????????????????????????????????
  },
  ftask: {
    queryAppDeployByTaskId: '/ftask/api/deploy/queryAppDeployByTaskId', //??????????????????????????????
    appDeploy: '/ftask/api/deploy/applayDeploy', //sit2????????????
    queryProjectBranchList: '/ftask/api/task/queryProjectBranchList',
    addApprove: '/ftask/api/releaseApprove/add',
    releaseApproveList: '/ftask/api/releaseApprove/releaseApproveList',
    exportApproveList: '/ftask/api/releaseApprove/exportApproveList',
    passApprove: '/ftask/api/releaseApprove/pass',
    refuseApprove: '/ftask/api/releaseApprove/refuse',
    downLoadReviewList: '/ftask/api/task/downLoadReviewList',
    queryGroupById: '/ftask/api/task/queryGroupById',
    queryAddTaskType: '/ftask/api/task/queryAddTaskType',
    putJiraIssues: '/ftask/api/jira/putJiraIssues',
    updateJiraIssues: '/ftask/api/jira/updateJiraIssues',
    saveTaskAndJiraIssues: '/ftask/api/jira/saveTaskAndJiraIssues',
    queryTestTask: '/ftask/api/jira/queryTestTask',
    uploadFilesRid: '/ftask/api/task/upload/files/rid',
    nextStage: '/ftask/api/task/nextStage',
    addNoCodeRelator: '/ftask/api/task/add/noCode/relator',
    delNoCodeRelator: '/ftask/api/task/delete/noCode/relator',
    deleteFileRid: '/ftask/api/task/delete/file/rid',
    noCodeRelator: '/ftask/api/task/finish/noCode/relator',
    addNocodeTask: '/ftask/api/task/create/nocode/task',
    updateNocodeInformation: '/ftask/api/task/confirm/update/nocodeInfo', //????????????
    queryMergeInfo: '/ftask/api/git/queryMergeInfo',
    queryDomains: '/fapp/api/serviceSystem/queryDomains',
    querySystem: '/fapp/api/serviceSystem/queryServiceSystem',
    taskAdd: '/ftask/api/task/add',
    taskDelete: '/ftask/api/task/delete',
    deleteTask: '/ftask/api/task/deleteTask',
    exportExcel: '/ftask/api/task/exportExcel',
    interactTest: '/ftask/api/task/interactTest',
    taskNewApp: '/ftask/api/task/newApp',
    newFeature: '/ftask/api/task/newFeature',
    putSitTest: '/ftask/api/task/putSitTest',
    putUatTest: '/ftask/api/task/putUatTest',
    taskQuery: '/ftask/api/task/query',
    queryBySubTask: '/ftask/api/task/queryBySubTask',
    queryByTerms: '/ftask/api/task/queryByTerms',
    queryByVague: '/ftask/api/task/queryByVague',
    queryDeleteTaskDetail: '/ftask/api/task/queryDeleteTaskDetail',
    queryEnvDetail: '/ftask/api/task/queryEnvDetail',
    checkSccOrCaas: '/ftask/api/task/checkSccOrCaas',
    queryMainTask: '/ftask/api/task/queryMainTask',
    queryTaskCByUnitNo: '/ftask/api/task/queryTaskCByUnitNo',
    queryTaskDetail: '/ftask/api/task/queryTaskDetail',
    queryTaskNum: '/ftask/api/task/queryTaskNum',
    queryTaskNumByApp: '/ftask/api/task/queryTaskNumByApp',
    queryTaskNumByGroup: '/ftask/api/task/queryTaskNumByGroup',
    queryTaskNumByGroupDate: '/ftask/api/task/queryTaskNumByGroupDate',
    queryTaskNumByMember: '/ftask/api/task/queryTaskNumByMember',
    queryTaskReview: '/ftask/api/task/queryTaskReview',
    queryTasksByIds: '/ftask/api/task/queryTasksByIds',
    queryUserTask: '/ftask/api/task/queryUserTask',
    taskUpdate: '/ftask/api/task/update',
    updateTaskReview: '/ftask/api/task/updateTaskReview',
    updateTaskStatus: '/ftask/api/task/updateTaskStatus',
    queryDocDetail: '/ftask/api/task/queryDocDetail',
    queryTestDetail: '/ftask/api/task/queryTestDetail',
    queryUATTestDetail: '/ftask/api/task/queryUATTestDetail',
    queryFtaskMantis: '/tmantis/mantisFdev/queryFtaskMantisAll',
    queryJiraIssues: '/ftask/api/jira/queryJiraIssues',
    abandonTask: '/ftask/api/task/updateTaskToDiscardInner',
    queryTaskStatis: '/ftask/api/task/queryTaskNumByMemberRQR',
    queryGroupStatis: '/ftask/api/task/queryTaskNumByGroupinAll',
    queryNotinlineTasksByAppId: '/ftask/api/task/queryNotinlineTasksByAppId',
    deleteFile: '/ftask/api/task/review/deleteFiles',
    queryReviewRecord: '/ftask/api/task/review/fuzzyQueryReviewRecord',
    updateReviewRecord: '/ftask/api/task/review/updateTaskReviewStatus',
    queryReviewRecordStatus: '/ftask/api/task/review/queryTaskReview',
    createFirstReview: '/ftask/api/task/review/createFirstReview',
    queryTasksReviews: '/ftask/api/task/queryTaskReview',
    queryTaskNumByUserIdsDate: '/ftask/api/task/queryTaskNumByUserIdsDate',
    updateState: '/ftask/api/task/updateState',
    saveReviewRecord: '/ftask/api/task/review/saveReviewRecord',
    queryReviewRecordHistory: '/ftask/api/task/review/queryReviewRecordHistory',
    taskNameJudge: '/ftask/api/task/taskNameJudge',
    queryReviewBasicMsg: '/ftask/api/task/review/queryReviewBasicMsg',
    addReviewIdea: '/ftask/api/task/review/addReviewIdea',
    queryByTaskIdNode: '/frelease/api/task/queryByTaskIdNode',
    testReportCreate: '/ftask/api/task/doc/testReportCreate',
    queryTestMergeInfo: '/ftask/api/git/queryTestMergeInfo',
    createTestRunMerge: '/ftask/api/task/createTestRunMerge',
    queryCommitTips: '/ftask/api/git/queryCommitTips',
    getCodeQuality: '/ftask/api/sonarqube/getCodeQuality',
    getScanProcess: '/ftask/api/sonarqube/getScanProcess',
    // iOSOrAndroidAppPackage: '/ftask/api/manage/generate/package',
    iOSOrAndroidAppPackage: '/ftask/api/manage/generate/packageFile',
    downloadSonarLog: '/ftask/api/sonarqube/downloadSonarLog',
    bafflePoint: '/ftask/api/manage/check/iam/properties',
    updateTaskDoc: '/ftask/api/task/doc/updateTaskDoc',
    deleteTaskDoc: '/ftask/api/task/doc/deleteTaskDoc',
    confirmBtn: '/ftask/api/task/update/confirmBtn',
    testKeyNote: '/ftask/api/task/update/testKeyNote',
    taskCardDisplay: '/ftask/api/task/taskCardDisplay',
    queryRqrDocInfo: '/ftask/api/task/queryRqrDocInfo',
    uploadDesignDoc: '/ftask/api/task/doc/uploadDesignDoc',
    queryTaskByDemandId: '/ftask/api/task/queryTaskByDemandId',
    queryReviewList: '/ftask/api/task/queryReviewList',
    queryGroupRqrmnt: '/ftask/api/task/queryGroupRqrmnt',
    addReview: '/ftask/api/task/review/addReview',
    queryJiraStoryByKey: '/ftask/api/jira/queryJiraStoryByKey',
    checkMountUnit: '/ftask/api/task/checkMountUnit',
    queryParamFile: '/ftask/api/manage/queryParamFile',
    queryPostponeTask: '/ftask/api/task/queryPostponeTask', // ??????????????????(???),
    downloadTemplateFile: '/ftask/api/task/doc/downloadTemplateFile',
    putSecurityTest: '/ftask/api/task/putSecurityTest',
    uploadSecurityTestDoc: '/ftask/api/task/doc/uploadSecurityTestDoc',
    queryAppDeploy: '/ftask/api/deploy/queryAppDeploy', //????????????????????????
    deployApps: '/ftask/api/deploy/deployApps', //????????????
    queryDeployTask: '/ftask/api/deploy/queryDeployTask', //????????????????????????????????????
    queryWhiteList: '/ftask/api/deploy/queryWhiteList', //sit???????????????
    exportDeployTask: '/ftask/api/deploy/export', //??????
    skipInnerTest: '/ftask/api/task/skipInnerTest' //??????????????????
  },
  fcomponent: {
    queryReleaseVersionComponent:
      '/fcomponent/api/component/queryReleaseVersion',
    queryReleaseVersionArchetype:
      '/fcomponent/api/archetype/queryReleaseVersion',
    queryLatestVersionComponent: '/fcomponent/api/component/queryLatestVersion',
    queryLatestVersionArchetype: '/fcomponent/api/archetype/queryLatestVersion',
    relDevopsComponent: '/fcomponent/api/component/relDevops',
    relDevopsArchetype: '/fcomponent/api/archetype/relDevops',
    relDevopsBaseImage: '/fcomponent/api/baseImage/relDevops',
    judgeTargetVersionComponent: '/fcomponent/api/component/judgeTargetVersion',
    judgeTargetVersionarchetype: '/fcomponent/api/archetype/judgeTargetVersion',
    queryMetaData: '/fcomponent/api/baseImage/queryMetaData',
    relasePackage: '/fcomponent/api/baseImage/relasePackage',
    packageTag: '/fcomponent/api/baseImage/packageTag',
    recoverInvalidRecord: '/fcomponent/api/baseImage/recoverInvalidRecord',
    updateBaseImage: '/fcomponent/api/baseImage/updateBaseImage',
    updateBaseImageRecord: '/fcomponent/api/baseImage/updateBaseImageRecord',
    addBaseImage: '/fcomponent/api/baseImage/addBaseImage',
    AddOptimizeBaseImageIssue:
      '/fcomponent/api/baseImage/optimizeBaseImageIssue',
    changeImageStage: '/fcomponent/api/baseImage/changeStage',
    queryImageIssueRecord: '/fcomponent/api/baseImage/queryIssueRecord',
    queryBaseImageIssueDetail:
      '/fcomponent/api/baseImage/queryBaseImageIssueDetail',
    queryBaseImageIssue: '/fcomponent/api/baseImage/queryBaseImageIssue',
    queryBaseImageRecord: '/fcomponent/api/baseImage/queryBaseImageRecord',
    queryBaseImageDetail: '/fcomponent/api/baseImage/queryBaseImageDetail',
    queryBaseImage: '/fcomponent/api/baseImage/queryBaseImage',
    saveConfigTemplate: '/fcomponent/api/config/saveConfigTemplate',
    queryConfigTemplate: '/fcomponent/api/config/queryConfigTemplate',
    queryComponent: '/fcomponent/api/component/queryComponents',
    addComponent: '/fcomponent/api/component/addComponent',
    queryComponentRecordHis:
      '/fcomponent/api/component/queryComponentRecordHis',
    updateComponent: '/fcomponent/api/component/updateComponent',
    updateComponentHistary: '/fcomponent/api/component/updateComponentHistary',
    optimizeComponent: '/fcomponent/api/component/optimizeComponent',
    changeStage: '/fcomponent/api/component/changeStage',
    queryIssueRecord: '/fcomponent/api/component/queryIssueRecord',
    queryComponentIssues: '/fcomponent/api/component/queryComponentIssues',
    queryComponentIssue: '/fcomponent/api/component/queryComponentIssueDetail',
    package: '/fcomponent/api/component/package',
    queryComponentDetail: '/fcomponent/api/component/queryComponentDetail',
    scanApplication: '/fcomponent/api/application/scanApplication',
    scanComponent: '/fcomponent/api/application/scanComponent',
    queryComponentHistory: '/fcomponent/api/component/queryComponentHistory',
    queryApplicatons: '/fcomponent/api/application/queryApplicatonsByComponent',
    queryComponents: '/fcomponent/api/application/queryComponentsByApplicaton',
    createComponent: '/fcomponent/api/component/createComponent',
    mailContent: '/fcomponent/api/mail/mailContent',
    queryArchetypes: '/fcomponent/api/archetype/queryArchetypes',
    queryMyArchetypes: '/fcomponent/api/archetype/queryMyArchetypes',
    queryMyMpassArchetypes:
      '/fcomponent/api/mpassarchetype/queryMyMpassArchetypes',
    queryArchetypeDetail: '/fcomponent/api/archetype/queryArchetypeDetail',
    updateArchetype: '/fcomponent/api/archetype/updateArchetype',
    addArchetype: '/fcomponent/api/archetype/addArchetype',
    queryArchetypeHistory: '/fcomponent/api/archetype/queryArchetypeHistory',
    queryMpassArchetypeHistory:
      '/fcomponent/api/mpassarchetype/queryMpassArchetypeHistory',
    queryArchetypeIssues: '/fcomponent/api/archetype/queryArchetypeIssues',
    optimizeArchetype: '/fcomponent/api/archetype/optimizeArchetype',
    archetypeChangeStage: '/fcomponent/api/archetype/changeStage',
    archetypePackage: '/fcomponent/api/archetype/package',
    queryArchetypeIssueRecord:
      '/fcomponent/api/archetype/queryArchetypeIssueRecord',
    queryArchetypeIssueDetail:
      '/fcomponent/api/archetype/queryArchetypeIssueDetail',
    updateArchetypeHistory: '/fcomponent/api/archetype/updateArchetypeHistory',
    queryComponentByArchetype:
      '/fcomponent/api/archetype/queryComponentByArchetype',
    queryApplicationsByArchetype:
      '/fcomponent/api/archetype/queryApplicatonsByArchetype',
    scanArchetype: '/fcomponent/api/archetype/scanArchetype',
    queryReleaseLog: '/fcomponent/api/component/queryReleaseLog',
    archetypeReleaseLog: '/fcomponent/api/archetype/queryReleaseLog',
    queryArchetypeTypes: '/fcomponent/api/archetype/queryArchetypeTypes',
    queryMyComponent: '/fcomponent/api/component/queryMyComponents',
    queryMyMpassComponents:
      '/fcomponent/api/mpasscomponent/queryMyMpassComponents',
    queryArchetypeFirstVersion: '/fcomponent/api/archetype/queryFirstVersion',
    queryComponentFirstVersion: '/fcomponent/api/component/queryFirstVersion',
    queryDependencyTree: '/fcomponent/api/component/queryDependencyTree',
    destroyComponentIssue: '/fcomponent/api/component/destroyComponentIssue',
    destroyArchetypeIssue: '/fcomponent/api/archetype/destroyArchetypeIssue',
    destroyBaseImageIssue: '/fcomponent/api/baseImage/destroyBaseImageIssue',
    queryFrameByComponent: '/fcomponent/api/application/queryFrameByComponent',
    exportExcelByComponent: '/fcomponent/api/export/exportExcelByComponent',
    queryMpassComponents: '/fcomponent/api/mpasscomponent/queryMpassComponents',
    addMpassComponent: '/fcomponent/api/mpasscomponent/addMpassComponent',
    updateMpassComponent: '/fcomponent/api/mpasscomponent/updateMpassComponent',
    queryWebcomByApplication:
      '/fcomponent/api/application/queryMpassComponentsByApplicaton',
    queryWebcomByComponent:
      '/fcomponent/api/application/queryApplicationByMpassComponent',
    scanMpassComByApp:
      '/fcomponent/api/application/scanMpassComponentByApplication',
    scanAppByMpassCom:
      '/fcomponent/api/application/scanApplicationByMpassComponent',
    queryMpassComponentDetail:
      '/fcomponent/api/mpasscomponent/queryMpassComponentDetail',
    queryMpassComHistory:
      '/fcomponent/api/mpasscomponent/queryMpassComponentHistary',
    scanMpassComHistory:
      '/fcomponent/api/mpasscomponent/scanMpassComponentHistory',
    updateMpassComHistory:
      '/fcomponent/api/mpasscomponent/updateMpassComponentHistary',
    queryMpassReleaseIssue:
      '/fcomponent/api/mpasscomponent/queryMpassReleaseIssue',
    queryMpassDevIssue: '/fcomponent/api/mpasscomponent/queryMpassDevIssue',
    queryMpassDefaultBranchAndVersion:
      '/fcomponent/api/mpasscomponent/defaultBranchAndVersion',
    addMpassReleaseIssue: '/fcomponent/api/mpasscomponent/addMpassReleaseIssue',
    addMpassDevIssue: '/fcomponent/api/mpasscomponent/addMpassDevIssue',
    queryMpassDevIssueDetail:
      '/fcomponent/api/mpasscomponent/queryMpassDevIssueDetail',
    queryMpassIssueRecord:
      '/fcomponent/api/mpasscomponent/queryMpassIssueRecord',
    queryMultiIssueRecord: '/fcomponent/api/component/queryMultiIssueRecord',
    devPackage: '/fcomponent/api/mpasscomponent/devPackage',
    changeMpassStage: '/fcomponent/api/mpasscomponent/changeStage',
    releasePackage: '/fcomponent/api/mpasscomponent/releasePackage',
    queryMpassArchetypes: '/fcomponent/api/mpassarchetype/queryMpassArchetypes',
    addMpassArchetype: '/fcomponent/api/mpassarchetype/addMpassArchetype',
    updateMpassArchetype: '/fcomponent/api/mpassarchetype/updateMpassArchetype',
    queryMpassArchetypeDetail:
      '/fcomponent/api/mpassarchetype/queryMpassArchetypeDetail',
    queryMpassArchetypeIssue:
      '/fcomponent/api/mpassarchetype/queryMpassArchetyepIssue',
    addMpassArchetypeIssue:
      '/fcomponent/api/mpassarchetype/addMpassArchetyepIssue',
    queryMpassArchetypeIssueDetail:
      '/fcomponent/api/mpassarchetype/queryMpassArchetyepIssueDetail',
    mpassArchetypePackage: '/fcomponent/api/mpassarchetype/package',
    queryIssueTag: '/fcomponent/api/mpassarchetype/queryIssueTag',
    changeMpassArchetypeStage: '/fcomponent/api/mpassarchetype/changeStage',
    updateMpassReleaseIssue:
      '/fcomponent/api/mpasscomponent/updateMpassReleaseIssue',
    destroyIssue: '/fcomponent/api/mpasscomponent/destroyIssue',
    updateMpassDevIssue: '/fcomponent/api/mpasscomponent/updateMpassDevIssue',
    queryMpassReleaseIssueDetail:
      '/fcomponent/api/mpasscomponent/queryMpassReleaseIssueDetail',
    queryApplicationByImage:
      '/fcomponent/api/application/queryApplicationByImage',
    scanImage: '/fcomponent/api/application/scanImage',
    queryFrameByImage: '/fcomponent/api/application/queryFrameByImage',
    queryTransgerReleaseIssue:
      '/fcomponent/api/mpasscomponent/queryTransgerReleaseIssue',
    devIssueTransger: '/fcomponent/api/mpasscomponent/devIssueTransger',
    queryNumByType: '/fcomponent/api/dashboard/queryNumByType',
    queryDataByType: '/fcomponent/api/dashboard/queryDataByType',
    queryIssueData: '/fcomponent/api/dashboard/queryIssueData',
    queryQrmntsData: '/fcomponent/api/dashboard/queryQrmntsData',
    queryIssueDelay: '/fcomponent/api/dashboard/queryIssueDelay',
    queryallComName: '/fcomponent/api/dashboard/allIsuue',
    destroyIssueServer: '/fcomponent/api/component/destroyIssue',
    devPackageServer: '/fcomponent/api/component/devPackage',
    addDevIssue: '/fcomponent/api/component/addDevIssue',
    updateReleaseIssue: '/fcomponent/api/component/updateReleaseIssue',
    addReleaseIssue: '/fcomponent/api/component/addReleaseIssue',
    defaultBranchAndVersion:
      '/fcomponent/api/component/defaultBranchAndVersion',
    releasePackageServer: '/fcomponent/api/component/releasePackage',
    queryAllComponents: '/fcomponent/api/component/queryAllComponents',
    queryAllArchetypeTypes: '/fcomponent/api/archetype/queryAllArchetypes',
    judgeTargetVersion: '/fcomponent/api/component/judgeTargetVersion',
    archetyVersion: '/fcomponent/api/archetype/judgeTargetVersion'
  },
  feds: {
    groupStatistics: '/feds/api/iams/groupStatistics',
    userStatistics: '/feds/api/iams/userStatistics'
  },
  fwebhook: {
    queryRunnerJobLog: '/fwebhook/api/webHook/getFdevRunnerJobLog',
    queryRunnerJobLogDetail: '/fwebhook/api/webHook/getFdevRunnerJobLogDetail',
    retryAutoTest: '/fwebhook/api/webHook/retryAutoTest'
  },
  tmantis: {
    queryFuserMantis: '/tmantis/mantisFdev/queryFuserMantisAll',
    updateFdevMantis: '/tmantis/mantisFdev/updateFdevMantis',
    queryProIssues: '/tmantis/proIssue/queryProIssues',
    countProIssues: '/tmantis/proIssue/countProIssues',
    queryUserProIssues: '/tmantis/proIssue/queryUserProIssues',
    queryTaskProIssues: '/tmantis/proIssue/queryTaskProIssues',
    queryIssueDetail: '/tmantis/proIssue/queryProIssueDetail',
    exportProIssues: '/tmantis/proIssue/exportProIssues',
    queryProIssueById: '/tmantis/proIssue/queryProIssueById',
    updateProIssue: '/tmantis/proIssue/updateProIssue',
    queryIssueFiles: '/tmantis/proIssue/queryIssueFiles',
    fileDownload: '/tmantis/proIssue/fileDownload',
    deleteFile: '/tmantis/proIssue/deleteFile',
    addFile: '/tmantis/proIssue/addFile',
    deleteProIssue: '/tmantis/proIssue/delete',
    updateAssignUser: '/tmantis/mantisFdev/updateFdevMantis',
    queryIssueDetailById: '/tmantis/mantis/queryIssueDetail',
    queryProByTeam: '/tmantis/proIssue/queryProByTeam',
    queryProIssueType: '/tmantis/proIssue/releaseNodeProIssueType',
    queryProIssueRate: '/tmantis/proIssue/releaseNodeProIssueRate'
  },
  frqrmnt: {
    queryImplUnitStatis: '/frqrmnt/api/implunits/queryImplUnitStatis',
    queryRedmineInfoByRedmineId: '/frqrmnt/api/implunits/queryUnitByRegexNum',
    queryAllDemands: 'frqrmnt/api/rqrmnts/queryRqrmnts'
  },
  fdocmanage: {
    createFolder: '/fdocmanage/api/wps/volumes/files/createFolder',
    queryFileList: '/fdocmanage/api/wps/volumes/files/list',
    deleteFile: '/fdocmanage/api/wps/deleteFile',
    queryPreviewLink: '/fdocmanage/api/wps/volumes/files/editor',
    exportExcelData: '/fdocmanage/api/file/filesDownload',
    deleteFileNew: '/fdocmanage/api/file/filesDelete',
    filesUpload: '/fdocmanage/api/file/filesUpload',
    downloadForWps: '/fdocmanage/api/wps/volumes/files/content',
    preview: '/fdocmanage/api/wps/volumes/files/preview'
  },
  torder: {
    queryTasks: '/torder/task/queryTasks',
    queryResourceManagement: '/torder/order/queryResourceManagement',
    queryTaskSitMsg: '/torder/inform/queryTaskSitMsg'
  },
  tplan: {
    addProIssue: '/tplan/mantis/addProIssue'
  },
  fdatabase: {
    queryAppByUser: '/fdatabase/api/database/queryAppByUser',
    queryUseRecordTable: '/fdatabase/api/dataDict/queryUseRecordTable',
    downloadTemplate: '/fdatabase/api/dataDict/downloadTemplate',
    queryFieldType: '/fdatabase/api/dataDict/queryFieldType',
    impDictRecords: '/fdatabase/api/dataDict/impDictRecords',
    addDictRecord: '/fdatabase/api/dataDict/addDictRecord',
    queryDictRecord: '/fdatabase/api/dataDict/queryDictRecord',
    querySystemNames: '/fdatabase/api/dataDict/querySystem',
    updateDictRecord: '/fdatabase/api/dataDict/updateDictRecord',
    databaseQueryDetail: '/fdatabase/api/database/queryDetail',
    queryManager: '/fdatabase/api/database/queryManager',
    queryName: '/fdatabase/api/database/queryName',
    queryDbType: '/fdatabase/api/database/queryDbType',
    queryAppName: '/fapp/api/app/queryApps',
    add: '/fdatabase/api/database/add',
    update: '/fdatabase/api/database/update',
    deleteDatabase: '/fdatabase/api/database/delete',
    queryInfo: '/fdatabase/api/database/queryInfo',
    queryDbName: '/fdatabase/api/database/queryDbName',
    download: '/fdatabase/api/database/loadTab',
    upload: '/fdatabase/api/file/upload',
    confirmAll: '/fdatabase/api/database/Confirm',
    addUseRecord: '/fdatabase/api/dataDict/addUseRecord',
    queryUseRecord: '/fdatabase/api/dataDict/queryUseRecord',
    updateUseRecord: '/fdatabase/api/dataDict/updateUseRecord'
  },
  fsonar: {
    projectAnalyses: '/fsonar/api/sonar/projectAnalyses',
    getProjectInfos: '/fsonar/api/sonar/getProjectInfo',
    getAnalysesHistory: '/fsonar/api/sonar/getAnalysesHistory',
    componentTree: '/fsonar/api/sonar/componentTree',
    searchProject: '/fsonar/api/sonar/searchProject',
    scanningFeatureBranch: '/fsonar/api/sonar/scanningFeatureBranch',
    featureProjectAnalyses: '/fsonar/api/sonar/featureProjectAnalyses',
    getProjectFeatureInfo: '/fsonar/api/sonar/getProjectFeatureInfo',
    featureComponentTree: '/fsonar/api/sonar/featureComponentTree'
  },
  fdemand: {
    queryDemandFile: '/fdemand/api/demand/queryDemandFile', //????????????????????????????????????
    //???????????????
    downLoadDemandReviewList: '/fdemand/api/design/downLoadReviewList',
    //??????fdev???????????????????????????id????????????????????????????????????
    queryByFdevNoAndDemandId:
      '/fdemand/api/implementUnit/queryByFdevNoAndDemandId',
    queryImpingDemandDashboard:
      '/fdemand/api/dashboard/queryImpingDemandDashboard',
    queryEndDemandDashboard: '/fdemand/api/dashboard/queryEndDemandDashboard',
    queryIntGroupId: '/fdemand/api/dashboard/queryIntGroupId',
    save: '/fdemand/api/demand/save',
    query: '/fdemand/api/demand/query', //??????????????????
    queryStatis: '/fdemand/api/dashboard/queryDemandStatis', //??????????????????
    queryImplUnitStatis: '/fdemand/api/dashboard/queryImplUnit', //????????????????????????
    queryGroupRqrmnt: '/fdemand/api/dashboard/queryGroupDemand', //??????????????????????????????????????????????????????
    queryDemandList: '/fdemand/api/demand/queryDemandList',
    update: '/fdemand/api/demand/update',
    deleteRqr: '/fdemand/api/demand/repeal',
    fileRqr: '/fdemand/api/demand/placeonfile',
    queryPaginationByDemandId:
      '/fdemand/api/implementUnit/queryPaginationByDemandId', //??????????????????????????????
    queryDemandInfoDetail: '/fdemand/api/demand/queryDemandInfoDetail',
    deleteUnitById: 'fdemand/api/ipmptask/deleteUnitById',
    deleteById: '/fdemand/api/implementUnit/deleteById',
    addImplementUnit: '/fdemand/api/implementUnit/add',
    supplyImplementUnit: '/fdemand/api/implementUnit/supply',
    updateImplementUnit: '/fdemand/api/implementUnit/update',
    search: '/fdemand/api/ipmptask/search',
    queryByGroupId: '/fdemand/api/ipmptask/queryByGroupId',
    queryUnitByIpmpTaskId: '/fdemand/api/ipmptask/queryUnitByIpmpTaskId',
    addIpmpTask: '/fdemand/api/ipmptask/addIpmpTask',
    addUnit: '/fdemand/api/ipmptask/addUnit',
    queryDemandDoc: '/fdemand/api/doc/queryDemandDoc',
    updateDemandDoc: '/fdemand/api/doc/updateDemandDoc',
    exportAssessExcel: '/fdemand/api/demand/exportAssessExcel',
    getDesignInfo: '/fdemand/api/design/getDesignInfo',
    uploadDesignDoc: '/fdemand/api/design/uploadDesignDoc',
    updateDesignStage: '/fdemand/api/design/updateDesignStage',
    updateDesignRemark: '/fdemand/api/design/updateDesignRemark',
    assess: '/fdemand/api/implementUnit/assess',
    queryImplByGroupAndDemandId:
      '/fdemand/api/implementUnit/queryImplByGroupAndDemandId',
    queryAvailableIpmpUnit: '/fdemand/api/implementUnit/queryAvailableIpmpUnit',
    defer: '/fdemand/api/demand/defer',
    recover: '/fdemand/api/demand/recover',
    exportDemandsExcel: '/fdemand/api/demand/exportDemandsExcel',
    deleteDemandDoc: '/fdemand/api/doc/deleteDemandDoc',
    updateByRecover: '/fdemand/api/implementUnit/updateByRecover',
    importDemandExcel: '/fdemand/api/demand/importDemandExcel',
    queryDemandByOaContactNo: '/fdemand/api/demand/queryDemandByOaContactNo',
    queryReviewList: '/fdemand/api/design/queryReviewList',
    exportModelExcel: '/fdemand/api/demand/exportModelExcel',
    updateImpl: '/fdemand/api/demand/updateImpl',
    queryPartInfo: '/fdemand/api/demand/queryPartInfo',
    queryIpmpUnitByDemandId: '/fdemand/api/ipmpUnit/queryIpmpUnitByDemandId', // ????????????id????????????????????????
    queryDetailByUnitNo: '/ftask/api/task/queryTaskByUnitNos', // ????????????????????????????????????
    queryIpmpUnitById: '/fdemand/api/ipmpUnit/queryIpmpUnitById', //??????????????????id????????????????????????
    updateIpmpUnit: '/fdemand/api/ipmpUnit/updateIpmpUnit', //??????????????????
    queryIpmpProject: '/fdemand/api/project/queryIpmpProject', //??????????????????????????????
    queryFdevImplUnitDetail:
      '/fdemand/api/implementUnit/queryFdevImplUnitDetail', // ????????????????????????
    queryTaskByIpmpUnitNo: '/fdemand/api/ipmpUnit/queryTaskByIpmpUnitNo', //????????????????????????????????????
    mount: '/fdemand/api/implementUnit/mount', //??????
    queryPaginationByIpmpUnitNo:
      '/fdemand/api/implementUnit/queryPaginationByIpmpUnitNo', //????????????????????????????????????
    assessButton: '/fdemand/api/implementUnit/assessButton', //????????????????????????????????????
    getWorker: '/fdemand/api/implementUnit/checkWork', // ?????????????????????????????????
    queryForSelect: '/fdemand/api/demand/queryForSelect' // ??????????????????????????????????????????
  },
  fgitwork: {
    getProjectUrl: '/fgitwork/api/work/getProjectUrl',
    queryGitlabCommitInfo:
      '/fgitwork/api/work/queryGitlabCommitInfoByGroupUser',
    queryGitlabCommitDetail: '/fgitwork/api/work/queryDetailInfo',
    getMergedInfo: '/fgitwork/api/getMergedInfo'
  },
  fconfigci: {
    queryCollectionPipelineList:
      '/fconfigci/api/pipeline/queryCollectionPipelineList', //?????????????????????????????????
    queryMinePipelineList: '/fconfigci/api/pipeline/queryMinePipelineList', //???????????????????????????
    queryAllPipelineList: '/fconfigci/api/pipeline/queryAllPipelineList', //???????????????????????????
    deletePipeline: '/fconfigci/api/pipeline/delete', //???????????????
    queryFdevciLogList: '/fconfigci/api/pipelineLog/queryFdevciLogList', //????????????fdev-ci????????????
    queryMinePipelineTemplateList:
      '/fconfigci/api/pipelineTemplate/queryMinePipelineTemplateList', //?????????????????????????????????
    queryAppPipelineList: '/fconfigci/api/pipeline/queryAppPipelineList', //?????????????????????????????????
    delTemplate: '/fconfigci/api/pipelineTemplate/delTemplate', //????????????
    updateFollowStatus: '/fconfigci/api/pipeline/updateFollowStatus', //???????????????/????????????
    queryPlugin: '/fconfigci/api/plugin/queryPluginList',
    delPlugin: '/fconfigci/api/plugin/delPlugin',
    addPlugin: '/fconfigci/api/plugin/addPlugin',
    editPlugin: '/fconfigci/api/plugin/editPlugin',
    queryPluginHistory: '/fconfigci/api/plugin/queryPluginHistory',
    queryReadDraft: '/fconfigci/api/pipeline/readDraft', //????????????
    queryPipelineDetailById: '/fconfigci/api/pipeline/queryById', //?????????????????????
    queryPipelineTempDetailById: '/fconfigci/api/pipelineTemplate/queryById', //???????????????????????????
    pipelineTemplateAdd: '/fconfigci/api/pipelineTemplate/add', //?????????????????????
    pipelineAdd: '/fconfigci/api/pipeline/add', //???????????????
    pipelineTemplateUpdate: '/fconfigci/api/pipelineTemplate/update', //?????????????????????
    pipelineUpdate: '/fconfigci/api/pipeline/update', //???????????????
    pipelineLogDetail: '/fconfigci/api/pipelineLog/queryPipelineDetail', //????????????????????????
    CIdraftSave: '/fconfigci/api/pipeline/saveDraft', //????????????
    triggerPipeline: 'fconfigci/api/pipeline/triggerPipeline', //?????????????????????
    queryImageList: 'fconfigci/api/pipeline/queryImageList', //??????????????????
    queryLogDetailById: '/fconfigci/api/pipelineLog/queryLogDetailById', //?????????job??????
    retryPipeline: 'fconfigci/api/pipeline/retryPipeline', //??????pipeline
    queryBranchesByPipelineId:
      '/fconfigci/api/pipeline/queryBranchesByPipelineId', //???????????????Id???????????????
    queryCategory: '/fconfigci/api/category/queryCategory', //??????????????????,
    queryPluginDetail: '/fconfigci/api/plugin/queryPluginDetail', //??????????????????
    copyPipeline: '/fconfigci/api/pipeline/copy', //???????????????
    retryJob: '/fconfigci/api/pipeline/retryJob', //????????????job
    saveAsPipelineTemplate: '/fconfigci/api/pipeline/saveAsPipelineTemplate', //????????????????????????
    queryMarkDown: '/fconfigci/api/plugin/queryMarkDown', //??????markDown??????
    stopPipeline: '/fconfigci/api/pipeline/stopPipeline',
    stopJob: '/fconfigci/api/pipeline/stopJob',
    copyPipelineTemplate: '/fconfigci/api/pipelineTemplate/copy',
    queryModelTemplateByContent:
      '/fconfigci/api/plugin/queryEntityTemplateByContent',
    queryTypeAndContent: '/fconfigci/api/pipeline/queryTypeAndContent', //queryContentByPathFormGit
    getMinioInfo: '/fconfigci/api/pipeline/getMinioInfo', //queryContentByPathFormMinio
    saveToMinio: '/fconfigci/api/pipeline/saveToMinio',
    getAllJobs: '/fconfigci/api/jobs/getAllJobs',
    getFullJobsByIds: '/fconfigci/api/jobs/getJobTemplateInfo',
    getAllRunnerCluster: '/fconfigci/api/v4/getAllRunnerCluster',
    queryTriggerRules: '/fconfigci/api/pipeline/queryTriggerRules',
    updateTriggerRules: '/fconfigci/api/pipeline/updateTriggerRules'
  },
  fblue: {
    queryDeployments: '/fblue/api/v2/query/deployment',
    proInfo: '/fblue/api/v2/query/proInfo',
    queryClusters: '/fblue/api/v2/query/clusters',
    queryCaasInfo: '/fblue/api/v2/query/caasDeploymentDetail',
    querySCCInfo: '/fblue/api/v2/query/sccDeploymentDetail'
  }
};
