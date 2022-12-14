import {
  saveConfigTemplate,
  queryConfigTemplate,
  queryComponent,
  queryAllComponents,
  queryAllArchetypeTypes,
  queryMetaData,
  relasePackage,
  packageTag,
  recoverInvalidRecord,
  updateBaseImage,
  updateBaseImageRecord,
  addBaseImage,
  AddOptimizeBaseImageIssue,
  changeImageStage,
  queryImageIssueRecord,
  queryBaseImageIssueDetail,
  queryBaseImageIssue,
  queryBaseImageRecord,
  queryBaseImageDetail,
  queryBaseImage,
  updateComponent,
  addComponent,
  queryComponentRecordHis,
  optimizeComponent,
  queryComponentDetail,
  scanApplication,
  scanComponent,
  queryComponentHistory,
  queryUsingStatusList,
  queryComponentIssues,
  queryComponentIssueDetail,
  packageLog,
  updateComponentHistary,
  queryIssueRecord,
  changeStage,
  queryComponentsByApplicaton,
  queryApplicatonsByComponent,
  createComponent,
  mailContent,
  scanHistory,
  queryArchetypes,
  queryMyArchetypes,
  queryMyMpassArchetypes,
  queryArchetypeDetail,
  addArchetype,
  updateArchetype,
  scanArchetypeHistory,
  queryArchetypeHistory,
  queryMpassArchetypeHistory,
  optimizeArchetype,
  queryArchetypeIssues,
  archetypeChangeStage,
  archetypePackage,
  queryArchetypeIssueRecord,
  queryArchetypeIssueDetail,
  updateArchetypeHistory,
  queryComponentByArchetype,
  queryApplicationsByArchetype,
  scanArchetype,
  queryReleaseLog,
  archetypeReleaseLog,
  queryArchetypeTypes,
  queryMyComponent,
  queryMyMpassComponents,
  queryComponentFirstVersion,
  queryArchetypeFirstVersion,
  queryDependencyTree,
  destroyComponentIssue,
  destroyArchetypeIssue,
  destroyBaseImageIssue,
  queryFrameByComponent,
  exportExcelByComponent,
  queryMpassComponents,
  addMpassComponent,
  updateMpassComponent,
  queryWebcomByApplication,
  queryWebcomByComponent,
  scanMpassComByApp,
  scanAppByMpassCom,
  queryMpassComponentDetail,
  queryMpassComHistory,
  scanMpassComHistory,
  updateMpassComHistory,
  queryMpassReleaseIssue,
  queryMpassDevIssue,
  queryMpassDefaultBranchAndVersion,
  addMpassReleaseIssue,
  addMpassDevIssue,
  queryMpassDevIssueDetail,
  queryMpassIssueRecord,
  queryMultiIssueRecord,
  devPackage,
  changeMpassStage,
  releasePackage,
  queryMpassArchetypes,
  addMpassArchetype,
  updateMpassArchetype,
  queryMpassArchetypeDetail,
  queryMpassArchetypeIssue,
  addMpassArchetypeIssue,
  queryMpassArchetypeIssueDetail,
  mpassArchetypePackage,
  queryIssueTag,
  changeMpassArchetypeStage,
  updateMpassReleaseIssue,
  destroyIssue,
  updateMpassDevIssue,
  queryMpassReleaseIssueDetail,
  queryApplicationByImage,
  scanImage,
  queryFrameByImage,
  queryTransgerReleaseIssue,
  devIssueTransger,
  destroyIssueServer,
  devPackageServer,
  addDevIssue,
  updateReleaseIssue,
  addReleaseIssue,
  defaultBranchAndVersion,
  releasePackageServer
} from '@/services/component';

import { resolveResponseError } from '@/utils/utils';
export default {
  namespaced: true,

  state: {
    queryMetaDataRes: {},
    queryImageIssueRecordList: [],
    imageIssueDetail: {},
    imageIssueList: [],
    imageRecordList: [],
    baseImageDetail: {},
    imageManageList: [],
    configTemplate: {},
    componentList: [],
    myComponentList: [],
    myWebComponentList: [],
    componentDetail: {},
    componentRecordHis: [],
    history: [],
    IssueRecord: [],
    usingStatusList: [],
    componentIssueDetail: [],
    optimizeList: [],
    usingStatuList: [],
    applicationsList: [],
    appsByComponent: [],
    emailContent: '',
    archetypeList: [],
    myArchetypeList: [],
    myWebArchetypeList: [],
    archetypeDetail: {},
    archetypeHistory: [],
    archetypeWebHistory: [],
    archetypeIssues: [],
    archetypePackageList: [],
    archetypeIssueDetail: {},
    archetypeRelative: [],
    releaseLog: {},
    archetypeReleaseLog: {},
    archetypeTypes: [],
    archetypeFirstVersion: '',
    componentFirstVersion: '',
    dependencyTree: '',
    frameByComponent: [],
    componentExport: null,
    mpassComponents: [],
    mpassComByApp: [],
    mpassComByCom: [],
    mpassComDetail: {},
    mpassHistory: [],
    mpassReleaseIssue: [],
    mpassDevIssue: [],
    mpassDefaultBranchAndVersion: {},
    mpassDevIssueDetail: {},
    mpassIssueRecord: [],
    multiIssueRecord: [],
    mpassRelDetail: {},
    mpassArchetypeList: [],
    mpassArchetypeDetail: {},
    mpassArchetypeIssue: [],
    mpassArchetypeIssueDetail: {},
    issueTag: [],
    result: {},
    frameByImage: {},
    transgerIssue: [],
    BranchAndVersion: []
  },

  getters: {
    parentComponents: state => {
      const { componentList } = state;
      return componentList.filter(v => v.type === '1');
    },
    notArchetypeFirstVersion: state => {
      return (
        state.archetypeFirstVersion !==
        state.archetypeIssueDetail.target_version
      );
    },
    notComponentFirstVersion: state => {
      if (state.componentFirstVersion) {
        return (
          state.componentFirstVersion !==
          state.componentIssueDetail.target_version
        );
      }
    }
  },

  actions: {
    // ???????????????????????????
    async queryAllComponents({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryAllComponents(payload)
        );
        commit('saveAllComponent', response);
      } catch (err) {
        commit('saveAllComponent', []);
      }
    },
    // ???????????????????????????
    async queryAllArchetypeTypes({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryAllArchetypeTypes(payload)
        );
        commit('saveAllArchetype', response);
      } catch (err) {
        commit('saveAllArchetype', []);
      }
    },
    // ??????????????????
    async queryComponent({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryComponent(payload)
        );
        commit('saveComponent', response);
      } catch (err) {
        commit('saveComponent', []);
      }
    },
    //?????????
    async queryMetaData({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryMetaData(payload)
        );
        commit('saveMetaData', response);
      } catch (err) {
        commit('saveMetaData', []);
      }
    },
    // ????????????????????????
    async relasePackage({ commit }, payload) {
      await resolveResponseError(() => relasePackage(payload));
    },
    // ??????????????????invalid?????????????????????
    async recoverInvalidRecord({ commit }, payload) {
      await resolveResponseError(() => recoverInvalidRecord(payload));
    },
    // ??????????????????
    async packageTag({ commit }, payload) {
      await resolveResponseError(() => packageTag(payload));
    },
    // ??????????????????
    async updateBaseImage({ commit }, payload) {
      await resolveResponseError(() => updateBaseImage(payload));
    },
    // ??????????????????
    async addBaseImage({ commit }, payload) {
      await resolveResponseError(() => addBaseImage(payload));
    },
    async AddOptimizeBaseImageIssue({ commit }, payload) {
      await resolveResponseError(() => AddOptimizeBaseImageIssue(payload));
    },
    // ??????????????????
    async queryBaseImage({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryBaseImage(payload)
        );
        commit('saveImageData', response);
      } catch (err) {
        commit('saveImageData', []);
      }
    },
    // ??????stage?????????
    async changeImageStage({ commit }, payload) {
      await resolveResponseError(() => changeImageStage(payload));
    },
    // ??????????????????
    async queryBaseImageIssueDetail({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryBaseImageIssueDetail(payload)
        );
        commit('saveImageIssueDetailData', response);
      } catch (err) {
        commit('saveImageIssueDetailData', []);
      }
    },
    // ????????????????????????
    async queryImageIssueRecord({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryImageIssueRecord(payload)
        );
        commit('saveImageIssueListData', response);
      } catch (err) {
        commit('saveImageIssueListData', []);
      }
    },
    // ????????????
    async queryBaseImageRecord({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryBaseImageRecord(payload)
        );
        commit('saveImageRecordData', response);
      } catch (err) {
        commit('saveImageRecordData', []);
      }
    },
    async queryBaseImageIssue({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryBaseImageIssue(payload)
        );
        commit('saveImageIssueData', response);
      } catch (err) {
        commit('saveImageIssueData', []);
      }
    },
    /* ???????????? */
    async updateComponent({ commit }, payload) {
      await resolveResponseError(() => updateComponent(payload));
    },
    /* ???????????? */
    async createComponent({ commit }, payload) {
      await resolveResponseError(() => createComponent(payload));
    },
    /* ???????????????????????? */
    async updateComponentHistary({ commit }, payload) {
      await resolveResponseError(() => updateComponentHistary(payload));
    },
    /* ???????????? */
    async addComponent({ commit }, payload) {
      await resolveResponseError(() => addComponent(payload));
    },
    async queryComponentRecordHis({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryComponentRecordHis(payload)
      );
      commit('saveComponentRecordHis', response);
    },
    /* ???????????????????????? */
    async changeStage({ commit }, payload) {
      await resolveResponseError(() => changeStage(payload));
    },
    /* ???????????? */
    async packageLog({ commit }, payload) {
      await resolveResponseError(() => packageLog(payload));
    },
    /* ?????????????????? */
    async optimizeComponent({ commit }, payload) {
      await resolveResponseError(() => optimizeComponent(payload));
    },

    /*????????????id, ??????????????????????????????*/
    async queryComponentIssues({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryComponentIssues(payload)
        );
        commit('saveOptimizeList', response);
      } catch (err) {
        commit('saveOptimizeList', []);
      }
    },
    // ????????????????????????
    async updateBaseImageRecord({ commit }, payload) {
      await resolveResponseError(() => updateBaseImageRecord(payload));
    },
    /* ?????????????????????????????? */
    async queryComponentIssueDetail({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryComponentIssueDetail(payload)
      );
      commit('saveComponentIssueDetail', response);
    },
    /* ???????????????id???????????????????????? */
    async queryComponentDetail({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryComponentDetail(payload)
      );
      commit('saveComponentDetail', response);
    },
    /* ???????????????id??????????????????????????? */
    async queryBaseImageDetail({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryBaseImageDetail(payload)
      );
      commit('saveBaseImageDetail', response);
    },
    /* ?????????????????? */
    async queryUsingStatusList({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryUsingStatusList(payload)
      );
      commit('saveUsingStatusList', response);
    },
    async queryApplicationByImage({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryApplicationByImage(payload)
      );
      commit('saveUsingStatusList', response);
    },
    /* ???????????????????????? */
    async queryComponentHistory({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryComponentHistory(payload)
      );
      commit('saveHistory', response);
    },
    /* ????????????????????????????????? */
    async queryIssueRecord({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryIssueRecord(payload)
      );
      commit('saveIssueRecord', response);
    },
    /* ?????????????????? */
    async scanApplication({ commit }, payload) {
      await resolveResponseError(() => scanApplication(payload));
    },
    /* ?????????????????? */
    async scanComponent({ commit }, payload) {
      await resolveResponseError(() => scanComponent(payload));
    },
    // ????????????????????????
    async scanImage({ commit }, payload) {
      await resolveResponseError(() => scanImage(payload));
    },
    /* ????????????-?????? */
    async queryComponentsByApplicaton({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryComponentsByApplicaton(payload)
      );
      commit('saveApplications', response);
    },
    /* ????????????-?????? */
    async queryApplicatonsByComponent({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryApplicatonsByComponent(payload)
      );
      commit('saveApplicatonsByComponent', response);
    },
    /* ???????????? */
    async mailContent({ commit }, payload) {
      const response = await resolveResponseError(() => mailContent(payload));
      commit('saveEmailContent', response);
    },
    /* ???????????? */
    async scanHistory({ commit }, payload) {
      await resolveResponseError(() => scanHistory(payload));
    },
    // ??????????????????
    async queryArchetypes({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryArchetypes(payload)
        );
        commit('saveArchetypeList', response);
      } catch (err) {
        commit('saveArchetypeList', []);
      }
    },
    // ????????????
    async queryConfigTemplate({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryConfigTemplate(payload)
        );
        commit('saveArcheConfigTemplate', response);
      } catch (err) {
        commit('saveArcheConfigTemplate', []);
      }
    },
    // ??????????????????
    async saveConfigTemplate({ commit }, payload) {
      const response = await resolveResponseError(() =>
        saveConfigTemplate(payload)
      );
      commit('saveResult', response);
    },
    // ????????????????????????--??????
    async queryMyArchetypes({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryMyArchetypes(payload)
        );
        commit('saveMyArchetypeList', response);
      } catch (err) {
        commit('saveMyArchetypeList', []);
      }
    },
    // ????????????????????????--??????
    async queryMyMpassArchetypes({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryMyMpassArchetypes(payload)
        );
        commit('saveMyWebArchetypeList', response);
      } catch (err) {
        commit('saveMyWebArchetypeList', []);
      }
    },
    /* ???????????????id???????????????????????? */
    async queryArchetypeDetail({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryArchetypeDetail(payload)
      );
      commit('saveArchetypeDetail', response);
    },
    /* ??????????????? */
    async updateArchetype({ commit }, payload) {
      await resolveResponseError(() => updateArchetype(payload));
    },
    /* ???????????? */
    async addArchetype({ commit }, payload) {
      await resolveResponseError(() => addArchetype(payload));
    },
    /* ?????????????????? */
    async scanArchetypeHistory({ commit }, payload) {
      await resolveResponseError(() => scanArchetypeHistory(payload));
    },
    /* ????????????????????????--?????? */
    async queryArchetypeHistory({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryArchetypeHistory(payload)
      );
      commit('saveArchetypeHistory', response);
    },
    /* ????????????????????????--?????? */
    async queryMpassArchetypeHistory({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryMpassArchetypeHistory(payload)
      );
      commit('saveWebArchetypeHistory', response);
    },
    /* ??????--?????????????????? */
    async optimizeArchetype({ commit }, payload) {
      await resolveResponseError(() => optimizeArchetype(payload));
    },
    /* ?????????????????? */
    async queryArchetypeIssues({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryArchetypeIssues(payload)
      );
      commit('saveArchetypeIssues', response);
    },
    /* ??????--???????????? */
    async archetypeChangeStage({ commit }, payload) {
      await resolveResponseError(() => archetypeChangeStage(payload));
    },
    /* ??????--?????? */
    async archetypePackage({ commit }, payload) {
      await resolveResponseError(() => archetypePackage(payload));
    },
    /* ??????--???????????? */
    async queryArchetypeIssueRecord({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryArchetypeIssueRecord(payload)
      );
      commit('saveArchetypeIssueRecord', response);
    },
    /* ??????--???????????? */
    async queryArchetypeIssueDetail({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryArchetypeIssueDetail(payload)
      );
      commit('saveArchetypeIssueDetail', response);
    },
    /* ???????????????????????? */
    async updateArchetypeHistory({ commit }, payload) {
      await resolveResponseError(() => updateArchetypeHistory(payload));
    },
    async scanArchetype({ commit }, payload) {
      await resolveResponseError(() => scanArchetype(payload));
    },
    async queryComponentByArchetype({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryComponentByArchetype(payload)
      );
      commit('saveArchetypeRelative', response);
    },
    async queryApplicationsByArchetype({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryApplicationsByArchetype(payload)
      );
      commit('saveArchetypeRelative', response);
    },
    async queryReleaseLog({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryReleaseLog(payload)
      );
      commit('saveReleaseLog', response);
    },
    async queryArchetypeReleaseLog({ commit }, payload) {
      const response = await resolveResponseError(() =>
        archetypeReleaseLog(payload)
      );
      commit('saveArchetypeReleaseLog', response);
    },

    /* ???????????????????????? */
    async queryArchetypeTypes({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryArchetypeTypes(payload)
      );
      commit('saveArchetypeTypes', response);
    },
    // ??????????????????--??????
    async queryMyComponent({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryMyComponent(payload)
        );
        commit('saveMyComponent', response);
      } catch (err) {
        commit('saveMyComponent', []);
      }
    },
    // ??????????????????--??????
    async queryMyMpassComponents({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryMyMpassComponents(payload)
        );
        commit('saveMyWebComponent', response);
      } catch (err) {
        commit('saveMyWebComponent', []);
      }
    },
    async queryArchetypeFirstVersion({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryArchetypeFirstVersion(payload)
      );
      commit('saveArchetypeFirstVersion', response);
    },
    async queryComponentFirstVersion({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryComponentFirstVersion(payload)
      );
      commit('saveComponentFirstVersion', response);
    },
    async queryDependencyTree({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryDependencyTree(payload)
      );
      commit('saveDependencyTree', response);
    },
    async destroyComponentIssue({ commit }, payload) {
      await resolveResponseError(() => destroyComponentIssue(payload));
    },
    async destroyArchetypeIssue({ commit }, payload) {
      await resolveResponseError(() => destroyArchetypeIssue(payload));
    },
    async destroyBaseImageIssue({ commit }, payload) {
      await resolveResponseError(() => destroyBaseImageIssue(payload));
    },
    async queryFrameByComponent({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryFrameByComponent(payload)
      );
      commit('saveFrameByComponent', response);
    },
    async exportExcelByComponent({ commit }, payload) {
      const response = await resolveResponseError(() =>
        exportExcelByComponent(payload)
      );
      commit('saveComponentExport', response);
    },
    async queryMpassComponents({ commit }, payload) {
      const res = await resolveResponseError(() =>
        queryMpassComponents(payload)
      );
      commit('saveMpassComponents', res);
    },
    async addMpassComponent({ commit }, payload) {
      await resolveResponseError(() => addMpassComponent(payload));
    },
    async updateMpassComponent({ commit }, payload) {
      await resolveResponseError(() => updateMpassComponent(payload));
    },
    async queryWebcomByApplication({ commit }, payload) {
      const res = await resolveResponseError(() =>
        queryWebcomByApplication(payload)
      );
      commit('saveMpassComByApp', res);
    },
    async queryWebcomByComponent({ commit }, payload) {
      const res = await resolveResponseError(() =>
        queryWebcomByComponent(payload)
      );
      commit('saveMpassComByCom', res);
    },
    async scanMpassComByApp({ commit }, payload) {
      await resolveResponseError(() => scanMpassComByApp(payload));
    },
    async scanAppByMpassCom({ commit }, payload) {
      await resolveResponseError(() => scanAppByMpassCom(payload));
    },
    async queryMpassComponentDetail({ commit }, payload) {
      const res = await resolveResponseError(() =>
        queryMpassComponentDetail(payload)
      );
      commit('saveMpassComDetail', res);
    },
    async queryMpassComHistory({ commit }, payload) {
      const res = await resolveResponseError(() =>
        queryMpassComHistory(payload)
      );
      commit('saveMpassComHistory', res);
    },
    async scanMpassComHistory({ commit }, payload) {
      await resolveResponseError(() => scanMpassComHistory(payload));
    },
    async updateMpassComHistory({ commit }, payload) {
      await resolveResponseError(() => updateMpassComHistory(payload));
    },
    async queryMpassReleaseIssue({ commit }, payload) {
      const res = await queryMpassReleaseIssue(payload);
      commit('saveMpassReleaseIssue', res);
    },
    async queryMpassDevIssue({ commit }, payload) {
      const res = await queryMpassDevIssue(payload);
      commit('saveMpassDevIssue', res);
    },
    // mpass????????????????????????????????????????????????????????????????????????????????????????????????
    async queryMpassDefaultBranchAndVersion({ commit }, payload) {
      const res = await resolveResponseError(() =>
        queryMpassDefaultBranchAndVersion(payload)
      );
      commit('saveMpassDefaultBranchAndVersion', res);
    },
    // mpass?????????????????????????????????????????????
    async addMpassReleaseIssue({ commit }, payload) {
      await resolveResponseError(() => addMpassReleaseIssue(payload));
    },
    // mpass?????????????????????????????????????????????????????????????????????
    async addMpassDevIssue({ commit }, payload) {
      await resolveResponseError(() => addMpassDevIssue(payload));
    },
    // todo
    async queryMpassDevIssueDetail({ commit }, payload) {
      const res = await resolveResponseError(() =>
        queryMpassDevIssueDetail(payload)
      );
      commit('saveMpassDevIssueDetail', res);
    },
    async queryMpassIssueRecord({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryMpassIssueRecord(payload)
      );
      commit('saveMpassIssueRecord', response);
    },
    async queryMultiIssueRecord({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryMultiIssueRecord(payload)
      );
      commit('saveMultiIssueRecord', response);
    },
    async devPackage({ commit }, payload) {
      await resolveResponseError(() => devPackage(payload));
    },
    async changeMpassStage({ commit }, payload) {
      await resolveResponseError(() => changeMpassStage(payload));
    },
    async changeMpassArchetypeStage({ commit }, payload) {
      await resolveResponseError(() => changeMpassArchetypeStage(payload));
    },
    async updateMpassReleaseIssue({ commit }, payload) {
      await resolveResponseError(() => updateMpassReleaseIssue(payload));
    },
    async destroyIssue({ commit }, payload) {
      await resolveResponseError(() => destroyIssue(payload));
    },
    async updateMpassDevIssue({ commit }, payload) {
      await resolveResponseError(() => updateMpassDevIssue(payload));
    },
    async queryMpassReleaseIssueDetail({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryMpassReleaseIssueDetail(payload)
      );
      commit('saveMpassRelDetail', response);
    },
    async releasePackage({ commit }, payload) {
      await resolveResponseError(() => releasePackage(payload));
    },
    async queryMpassArchetypes({ commit }, payload) {
      try {
        const response = await resolveResponseError(() =>
          queryMpassArchetypes(payload)
        );
        commit('saveMpassArchetypeList', response);
      } catch (err) {
        commit('saveMpassArchetypeList', []);
      }
    },
    async addMpassArchetype({ commit }, payload) {
      await resolveResponseError(() => addMpassArchetype(payload));
    },
    async updateMpassArchetype({ commit }, payload) {
      await resolveResponseError(() => updateMpassArchetype(payload));
    },
    async queryMpassArchetypeDetail({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryMpassArchetypeDetail(payload)
      );
      commit('saveMpassArchetypeDetail', response);
    },
    async queryMpassArchetypeIssue({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryMpassArchetypeIssue(payload)
      );
      commit('saveMpassArchetypeIssue', response);
    },
    async addMpassArchetypeIssue({ commit }, payload) {
      await resolveResponseError(() => addMpassArchetypeIssue(payload));
    },
    async queryMpassArchetypeIssueDetail({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryMpassArchetypeIssueDetail(payload)
      );
      commit('saveMpassArchetypeIssueDetail', response);
    },
    async mpassArchetypePackage({ commit }, payload) {
      await resolveResponseError(() => mpassArchetypePackage(payload));
    },
    async queryIssueTag({ commit }, payload) {
      const response = await resolveResponseError(() => queryIssueTag(payload));
      commit('saveIssueTag', response);
    },
    async queryFrameByImage({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryFrameByImage(payload)
      );
      commit('saveImageDetail', response);
    },
    async queryTransgerReleaseIssue({ commit }, payload) {
      const response = await resolveResponseError(() =>
        queryTransgerReleaseIssue(payload)
      );
      commit('saveTransgerIssue', response);
    },
    async devIssueTransger({ commit }, payload) {
      await resolveResponseError(() => devIssueTransger(payload));
    },
    async destroyIssueServer({ commit }, payload) {
      await resolveResponseError(() => destroyIssueServer(payload));
    },
    async devPackageServer({ commit }, payload) {
      await resolveResponseError(() => devPackageServer(payload));
    },
    async addDevIssue({ commit }, payload) {
      await resolveResponseError(() => addDevIssue(payload));
    },
    async updateReleaseIssue({ commit }, payload) {
      await resolveResponseError(() => updateReleaseIssue(payload));
    },
    async addReleaseIssue({ commit }, payload) {
      await resolveResponseError(() => addReleaseIssue(payload));
    },
    async defaultBranchAndVersion({ commit }, payload) {
      const response = await resolveResponseError(() =>
        defaultBranchAndVersion(payload)
      );
      commit('saveBranchAndVersion', response);
    },
    async releasePackageServer({ commit }, payload) {
      await resolveResponseError(() => releasePackageServer(payload));
    }
  },

  mutations: {
    saveImageIssueListData(state, payload) {
      state.queryImageIssueRecordList = payload;
    },
    // ?????????
    saveMetaData(state, payload) {
      state.queryMetaDataRes = payload;
    },
    saveImageIssueDetailData(state, payload) {
      state.imageIssueDetail = payload;
    },
    saveImageIssueData(state, payload) {
      state.imageIssueList = payload;
    },
    saveImageRecordData(state, payload) {
      state.imageRecordList = payload;
    },
    saveBaseImageDetail(state, payload) {
      state.baseImageDetail = payload;
    },
    saveImageData(state, payload) {
      state.imageManageList = payload;
    },
    saveArcheConfigTemplate(state, payload) {
      state.configTemplate = payload;
    },
    saveComponent(state, payload) {
      state.componentList = payload;
    },
    saveAllComponent(state, payload) {
      state.allComponentList = payload;
    },
    saveAllArchetype(state, payload) {
      state.allArchetypeList = payload;
    },
    saveMyComponent(state, payload) {
      state.myComponentList = payload;
    },
    saveMyWebComponent(state, payload) {
      state.myWebComponentList = payload;
    },
    saveComponentDetail(state, payload) {
      state.componentDetail = payload;
    },
    saveComponentRecordHis(state, payload) {
      state.componentRecordHis = payload;
    },
    saveHistory(state, payload) {
      state.history = payload;
    },
    saveIssueRecord(state, payload) {
      state.IssueRecord = payload;
    },
    saveUsingStatusList(state, payload) {
      state.usingStatusList = payload;
    },
    saveOptimizeList(state, payload) {
      state.optimizeList = payload;
    },
    saveComponentIssueDetail(state, payload) {
      state.componentIssueDetail = payload;
    },
    saveApplications(state, payload) {
      state.applicationsList = payload;
    },
    saveApplicatonsByComponent(state, payload) {
      state.appsByComponent = payload;
    },
    saveEmailContent(state, payload) {
      state.emailContent = payload;
    },
    saveArchetypeList(state, payload) {
      state.archetypeList = payload;
    },
    saveMyArchetypeList(state, payload) {
      state.myArchetypeList = payload;
    },
    saveMyWebArchetypeList(state, payload) {
      state.myWebArchetypeList = payload;
    },
    saveArchetypeDetail(state, payload) {
      state.archetypeDetail = payload;
    },
    saveArchetypeHistory(state, payload) {
      state.archetypeHistory = payload;
    },
    saveWebArchetypeHistory(state, payload) {
      state.archetypeWebHistory = payload;
    },
    saveArchetypeIssues(state, payload) {
      state.archetypeIssues = payload;
    },
    saveArchetypeIssueRecord(state, payload) {
      state.archetypePackageList = payload;
    },
    saveArchetypeIssueDetail(state, payload) {
      state.archetypeIssueDetail = payload;
    },
    saveArchetypeRelative(state, payload) {
      state.archetypeRelative = payload;
    },
    saveReleaseLog(state, payload) {
      state.releaseLog = payload;
    },
    saveArchetypeReleaseLog(state, payload) {
      state.archetypeReleaseLog = payload;
    },
    saveArchetypeTypes(state, payload) {
      state.archetypeTypes = payload;
    },
    saveArchetypeFirstVersion(state, payload) {
      state.archetypeFirstVersion = payload;
    },
    saveComponentFirstVersion(state, payload) {
      state.componentFirstVersion = payload;
    },
    saveDependencyTree(state, payload) {
      state.dependencyTree = payload;
    },
    saveFrameByComponent(state, payload) {
      state.frameByComponent = payload;
    },
    saveComponentExport(state, payload) {
      state.componentExport = payload;
    },
    saveMpassComponents(state, payload) {
      state.mpassComponents = payload;
    },
    saveMpassComByApp(state, payload) {
      state.mpassComByApp = payload;
    },
    saveMpassComByCom(state, payload) {
      state.mpassComByCom = payload;
    },
    saveMpassComDetail(state, payload) {
      state.mpassComDetail = payload;
    },
    saveMpassComHistory(state, payload) {
      state.mpassHistory = payload;
    },
    saveMpassReleaseIssue(state, payload) {
      state.mpassReleaseIssue = payload;
    },
    saveMpassDevIssue(state, payload) {
      state.mpassDevIssue = payload;
    },
    saveMpassDefaultBranchAndVersion(state, payload) {
      state.mpassDefaultBranchAndVersion = payload;
    },
    saveMpassDevIssueDetail(state, payload) {
      state.mpassDevIssueDetail = payload;
    },
    saveMpassIssueRecord(state, payload) {
      state.mpassIssueRecord = payload;
    },
    saveMultiIssueRecord(state, payload) {
      state.multiIssueRecord = payload;
    },
    saveMpassRelDetail(state, payload) {
      state.mpassRelDetail = payload;
    },
    saveMpassArchetypeList(state, payload) {
      state.mpassArchetypeList = payload;
    },
    saveMpassArchetypeDetail(state, payload) {
      state.mpassArchetypeDetail = payload;
    },
    saveMpassArchetypeIssue(state, payload) {
      state.mpassArchetypeIssue = payload;
    },
    saveMpassArchetypeIssueDetail(state, payload) {
      state.mpassArchetypeIssueDetail = payload;
    },
    saveIssueTag(state, payload) {
      state.issueTag = payload;
    },
    saveImageDetail(state, payload) {
      state.frameByImage = payload;
    },
    saveTransgerIssue(state, payload) {
      state.transgerIssue = payload;
    },
    saveBranchAndVersion(state, payload) {
      state.branchAndVersion = payload;
    },
    saveResult(state, payload) {
      const resultFormat = {
        formatError: '????????????',
        modelErrorList: '????????????',
        FiledErrorList: '????????????'
      };
      const result = payload ? {} : '';
      if (payload) {
        const keys = Object.keys(payload);
        keys.forEach(key => {
          if (payload[key].length > 0) {
            result[resultFormat[key]] = payload[key];
          }
        });
      }
      state.result = result;
    }
  }
};
