export function createJobModel() {
  let year = new Date().getFullYear();
  let month = new Date().getMonth() + 1;
  let day = new Date().getDate();
  let defalutDate = `${year}-${month > 9 ? month : '0' + month}-${
    day > 9 ? day : '0' + day
  }`;
  return {
    name: '',
    partyA: [],
    partyB: [],
    partyC: [],
    winName: '',
    changeNum: '',
    createUser: '',
    devStartDate: '',
    sitStartDate: defalutDate,
    uatStartDate: '',
    relStartDate: ''
  };
}

export function createReleaseNodeModel() {
  let year = new Date().getFullYear();
  let month = new Date().getMonth() + 1;
  let day = new Date().getDate();
  let defalutDate = `${year}-${month > 9 ? month : '0' + month}-${
    day > 9 ? day : '0' + day
  }`;
  return {
    releaseNodeName: '',
    releaseDate: defalutDate,
    release_date: defalutDate,
    owner_groupId: null,
    releaseManager: '',
    releaseSpdbManager: '',
    // releaseSpdbNo:'',
    gray_temp_id: [],
    proc_temp_id: [],
    uatEnvName: '',
    relEnvName: '',
    partyA: [],
    partyB: [],
    partyC: [],
    winName: '',
    changeNum: '',
    createUser: '',
    devStartDate: '',
    uatStartDate: '',
    relStartDate: '',
    release_node_name: '',
    release_manager: '',
    release_spdb_manager: '',
    uat_env_name: '',
    type: '1'
  };
}

export function formatRelease(release) {
  return release.map(releases => {
    return {
      releaseDate: releases.release_date,
      groupName: releases.owner_group_name,
      releaseNode: releases.release_node_name,
      create_user_name_cn: releases.create_user_name_cn,
      status: releases.node_status,
      createUser: releases.release_spdb_manager,
      releaseManager: releases.release_manager,
      releaseNodeName: releases.release_node_name,
      ownerGroupId: releases.owner_groupId,
      releaseSpdbManager: releases.release_spdb_manager,
      releaseSpdbNo: releases.release_spdb_no,
      uatEnvName: releases.uat_env_name,
      relEnvName: releases.rel_env_name,
      create_user: releases.create_user,
      node_status: releases.node_status,
      owner_groupId: releases.owner_groupId,
      owner_group_name: releases.owner_group_name,
      rel_env_name: releases.rel_env_name,
      release_date: releases.release_date,
      release_manager: releases.release_manager,
      release_node_name: releases.release_node_name,
      release_spdb_manager: releases.release_spdb_manager,
      release_spdb_no: releases.release_spdb_no,
      uat_env_name: releases.uat_env_name,
      _id: release._id
    };
  });
}

export function formatReleaseDetail(releaseNode) {
  return {
    ...releaseNode,
    releaseManager: releaseNode.release_manager,
    releaseNodeName: releaseNode.release_node_name,
    releaseDate: releaseNode.release_date,
    ownerGroupId: releaseNode.owner_groupId,
    releaseSpdbManager: releaseNode.release_spdb_manager,
    releaseSpdbNo: releaseNode.release_spdb_no,
    uatEnvName: releaseNode.uat_env_name,
    relEnvName: releaseNode.rel_env_name
  };
}

export function applyParams(release) {
  return [
    {
      app_name_en: '',
      app_name_zh: '',
      app_spdb_managers: '',
      app_dev_managers: '',
      release_branch: '',
      product_tag: '',
      devops_status: ''
    }
  ];
}

export function jobListParams(release) {
  return [
    {
      task_id: '',
      task_name: '',
      task_group: '',
      bank_master: '',
      release_branch: '',
      task_project: '',
      task_stage: ''
    }
  ];
}

export function releaseNoteAppModel() {
  return {
    application_name_en: '',
    application_name_cn: '',
    tag_name: '',
    application_type: '',
    application_id: '',
    catalog_type: '1',
    dev_managers_info: [],
    expand_info: {
      SHK1: '',
      SHK2: '',
      HFK1: '',
      HFK2: ''
    }
  };
}

export function releaseNoteNoTaskSqlModel() {
  return {
    fileName: '',
    fileContent: ''
  };
}

export function releaseNoteConfigModel() {
  return {
    module_name: 'commonconfig',
    module_ip: '',
    fileName: '',
    file_url: '',
    file_principal: '',
    principal_phone: '',
    diff_content: [],
    file_type: '1',
    safeguard_explain: '',
    diff_flag: '0'
  };
}

export function extPublishDialogModel() {
  return {
    type: '',
    executorId: '',
    transName: '',
    jobGroup: '',
    description: '',
    cronExpression: '',
    misfireInstr: '',
    fireTime: ''
  };
}

export function releaseNodeDetail() {
  return {
    create_user: '',
    node_status: '',
    ownerGroupId: '',
    owner_groupId: '',
    owner_group_name: '',
    relEnvName: '',
    rel_env_name: '',
    releaseDate: '',
    releaseManager: '',
    releaseNodeName: '',
    releaseSpdbManager: '',
    releaseSpdbNo: '',
    release_date: '',
    release_manager: '',
    release_node_name: '',
    release_spdb_manager: '',
    release_spdb_no: '',
    uatEnvName: '',
    uat_env_name: '',
    _id: ''
  };
}

export const filtersKey = {
  other_system: '??????????????????',
  script_alter: '????????????',
  data_base_alter: '???????????????',
  fire_wall_open: '???????????????',
  static_resource: '??????????????????',
  interface_alter: '????????????',
  ebank_common_alter: '??????????????????'
};

export function createTemplate() {
  return {
    owner_group: '',
    owner_system: null,
    template_type: '',
    owner_app: null,
    system_abbr: '',
    group_abbr: '',
    catalogs: [],
    resource_giturl: []
  };
}

// ????????????
export function newChangeModel() {
  return {
    release_node_name: '', // ??????????????????
    date: '', // ????????????
    prod_spdb_no: '', // ????????????
    version: '', // ????????????
    type: 'gray', // ????????????
    excel_template_url: null, // ??????????????????
    applications: [], // ????????????id??????
    plan_time: '18:00', // ??????????????????
    image_deliver_type: '1',
    system: null,
    vContainer: '',
    vGroup: '',
    vDate: '',
    Vtime: '',
    vEnd: '',
    owner_system_name: {},
    excel_template_name: {}
  };
}

// ??????????????????
export function newReleaseNoteModel() {
  return {
    release_node_name: '', // ??????????????????
    release_note_name: '', // ??????????????????
    date: '', // ????????????
    version: '', // ????????????
    type: 'gray', // ????????????
    plan_time: '18:00', // ??????????????????
    image_deliver_type: '1',
    leaseholder: '',
    system: null,
    vContainer: '',
    vGroup: '',
    vDate: '',
    Vtime: '',
    vEnd: '',
    note_batch: '',
    namespace: '2',
    owner_system_name: {}
  };
}

export const ImageDeliverType = {
  auto: '1', //???????????????
  deAuto: '0' //??????????????????
};

export const type = {
  '1': '???????????????',
  '2': '?????????????????????',
  '3': '???????????????',
  '4': '??????????????????',
  '5': '????????????????????????',
  '6': '???NAS??????????????????',
  '7': '??????????????????????????????',
  '8': 'esf???????????????',
  '9': '????????????????????????',
  '10': '?????????????????????????????????'
};

export const status = {
  '0': '???????????????',
  '1': '?????????????????????',
  '2': '???????????????',
  '3': '??????????????????',
  '4': '??????????????????'
};

export const product_type = {
  proc: '??????',
  gray: '??????'
};

export function testEnv() {
  return {
    uat_env_name: '',
    rel_env_name: ''
  };
}

export function updateChanges() {
  return {
    plan_time: '',
    prod_spdb_no: '',
    template: null,
    image_deliver_type: '0'
  };
}

export const catalogType = {
  '3': '???????????????',
  '4': '??????????????????',
  '5': '????????????????????????',
  '6': '???NAS??????????????????',
  '7': '??????????????????????????????',
  '8': 'esf???????????????',
  '9': '????????????????????????',
  '10': '?????????????????????????????????'
};

export const catalogTypeOptions = [
  {
    label: '???????????????',
    value: '3'
  },
  {
    label: '??????????????????',
    value: '4'
  },
  {
    label: '????????????????????????',
    value: '5'
  },
  {
    label: '???NAS??????????????????',
    value: '6'
  },
  {
    label: '??????????????????????????????',
    value: '7'
  },
  {
    label: 'esf???????????????',
    value: '8'
  },
  {
    label: '????????????????????????',
    value: '9'
  },
  {
    label: '?????????????????????????????????',
    value: '10'
  }
];

export function moduleTypeModel() {
  return {
    catalog_name: '',
    catalog_type: '',
    description: ''
  };
}
export function scriptParamsModel() {
  return {
    key: '',
    value: '',
    description: ''
  };
}
export function automationEnvModel() {
  return {
    env_name: '',
    description: ''
  };
}

export const typeNoteOptions = [
  { label: '???????????????????????????', value: 'docker' },
  { label: '???????????????', value: 'docker_restart' },
  { label: '????????????', value: 'docker_scale' },
  { label: '???????????????', value: 'docker_yaml' },
  { label: '????????????', value: 'stop_all' }
];

export const typeOptions = [
  { label: 'K1???K2??????????????????', value: '1' },
  { label: '???????????????', value: '2' },
  { label: '?????????????????????', value: '3' },
  { label: '????????????', value: '4' }
];
export const typeOptionsObj = {
  '1': 'K1???K2??????????????????',
  '2': '???????????????',
  '3': '?????????????????????',
  '4': '????????????'
};

export function reviewModel() {
  return {
    key: '',
    group: '',
    applicant: '',
    reviewStatus: ''
  };
}

export const taskStatus = [
  '?????????',
  '????????????',
  '????????????',
  '?????????????????????',
  '???????????????',
  '????????????????????????',
  '?????????'
];

export function releaseModel() {
  return {
    start_date: '',
    end_date: '',
    owner_groupId: ''
  };
}

export function releaseDialogModel() {
  return {
    release_date: '',
    owner_groupId: '',
    release_contact: []
  };
}

export const specialOptions = [
  { label: '??????', value: '' },
  { label: '???????????????', value: 'dataBaseAlter' },
  { label: '??????????????????', value: 'commonProfile' },
  { label: '????????????', value: 'new_add_sign' },
  { label: '?????????', value: 'specialCase' }
];

export function changeBatchDialogModel() {
  return {
    application_id: '',
    batch_id: '',
    applications: [],
    modify_reason: '',
    release_node_name: ''
  };
}

export const batchOptions = [
  { label: '????????????(20???????????????)', value: '1' },
  { label: '????????????(22?????????)', value: '2' },
  { label: '????????????(0?????????)', value: '3' }
];

export function batchArr() {
  return [
    {
      type: '????????????',
      batch: '1',
      children: []
    },
    {
      type: '????????????',
      batch: '2',
      children: []
    },
    {
      type: '????????????',
      batch: '3',
      children: []
    },
    {
      type: '????????????',
      batch: '4',
      children: []
    }
  ];
}

export const tagColor = {
  ????????????: 'indigo-1',
  ?????????: 'yellow-5',
  ?????????: 'red-12',
  ????????????: 'teal-4',
  ????????????: 'yellow-5',
  ????????????: 'red-12'
};

export const fileTypeOptions = [
  { label: '????????????????????????', value: '1' },
  { label: '????????????', value: '2' }
];

export const updateFileConfigQueryColumn = [
  {
    name: 'name_cn',
    label: '???????????????',
    field: 'name_zh',
    copy: true
  },
  {
    name: 'name_en',
    label: '???????????????',
    field: 'name_en',
    copy: true
  },
  {
    name: 'dev_managers',
    label: '???????????????',
    copy: true,
    field: row => row.dev_managers.map(item => item.user_name_cn).join('???')
  },
  {
    name: 'spdb_manager',
    label: '?????????????????????',
    copy: true,
    field: row => row.spdb_managers.map(item => item.user_name_cn).join('???')
  },
  {
    name: 'group',
    label: '????????????',
    copy: true,
    field: row => row.group.name
  },
  { name: 'gitlab', label: 'gitlab??????', field: 'git' },
  {
    name: 'branch',
    label: '?????????',
    field: 'branch'
  }
];

export const updateFileConfigAddColumn = [
  {
    name: 'application_name',
    label: '?????????',
    field: row =>
      row.isrisk === '1'
        ? row.application_name + '???????????????'
        : row.application_name,

    sortable: true
  },
  {
    name: 'dev_managers',
    label: '???????????????',
    field: row => row.dev_managers.join('???'),
    sortable: true
  },
  {
    name: 'config_type',
    label: '????????????',
    field: 'config_type',
    sortable: true
  },
  {
    name: 'release_node',
    label: '?????????????????????',
    field: 'release_node',
    sortable: true
  },
  {
    name: 'asset_name',
    label: '?????????????????????',
    field: 'asset_name',
    sortable: true
  },
  {
    name: 'status',
    label: '???????????????????????????',
    field: row => (row.status === '0' ? '???' : '???'),
    sortable: true
  },
  {
    name: 'operate',
    label: '??????'
  }
];

export const updateFileConfigQueryVisibleColumn = [
  'name_cn',
  'name_en',
  'dev_managers',
  'spdb_manager',
  'group',
  'gitlab',
  'branch'
];

export const updateFileConfigAddVisibleColumn = [
  'application_name',
  'dev_managers',
  'config_type',
  'release_node',
  'asset_name',
  'status',
  'operate'
];

export function createDependencyModel() {
  return {
    map: '',
    model: '',
    field: null
  };
}
export function createExamineModel() {
  return {
    group: '', // ?????????
    applicantName: '', // ?????????
    dbType: '', // ???????????????
    reviewers: '', // ???????????????
    reason: '', // ????????????
    plan_fire_time: '', // ????????????
    taskName: '', // ?????????
    docInfo: [] // ???????????????
  };
}
export const Prompt = {
  addedSuccessfully: '???????????????',
  modifiedSuccessfully: '?????????????????????'
};
export const AuditStatus = {
  pass: '??????',
  firstReviewReject: '????????????',
  seconedReviewReject: '????????????',
  archived: '?????????'
};
export const TaskStatus = {
  deStock: '0', // ???0??????????????????????????????????????????????????????
  stock: '1' // ???1???????????????????????????????????????????????????????????????
};
export const FileType = {
  '1': '?????????',
  '2': '?????????',
  '3': '?????????',
  '4': '?????????',
  '5': '?????????-????????????',
  '6': '?????????-?????????????????????',
  '7': '?????????-??????????????????'
};
