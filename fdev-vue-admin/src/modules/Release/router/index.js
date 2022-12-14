const Release = () => import('@/modules/Release/views/ReleaseList');
const BigReleaseDetail = () =>
  import('@/modules/Release/views/ReleaseBigWindow/Detail');
const Process = () =>
  import('@/modules/Release/views/ReleaseBigWindow/Process');
const Contact = () =>
  import('@/modules/Release/views/ReleaseBigWindow/Contact');
const Demand = () => import('@/modules/Release/views/ReleaseBigWindow/Demand');
const Test = () => import('@/modules/Release/views/ReleaseBigWindow/Test');
const Security = () =>
  import('@/modules/Release/views/ReleaseBigWindow/Security');
const Detail = () => import('@/modules/Release/views/Manage/Detail');
const ReleaseJob = () => import('@/modules/Release/views/Manage/JobList');
const TestRunAppList = () =>
  import('@/modules/Release/views/Manage/TestRunAppList');
const ReleaseAppList = () => import('@/modules/Release/views/Manage/AppList');
const ReleaseComponentList = () =>
  import('@/modules/Release/views/Manage/componentList');
const CreateUpdate = () =>
  import('@/modules/Release/views/Manage/createUpdate');
const AutoReleaseNote = () =>
  import('@/modules/Release/views/Manage/autoReleaseNote');
const ModifiedFiles = () =>
  import('@/modules/Release/views/Manage/ModifiedFiles');
const FileUpload = () => import('@/modules/Release/views/Manage/FileUpload');
const UpdateFileConfig = () =>
  import('@/modules/Release/views/Manage/UpdateFileConfig');
const AppScale = () => import('@/modules/Release/views/Manage/AppScale');
const UpdateDetail = () =>
  import('@/modules/Release/views/Manage/updateDetail');
const AutoReleaseNoteDetail = () =>
  import('@/modules/Release/views/Manage/autoReleaseNoteDetail');
const UpdateList = () => import('@/modules/Release/views/Manage/updateList');
const UpdateFileManage = () =>
  import('@/modules/Release/views/Manage/updateFileManage');
const ChangesPlans = () => import('@/modules/Release/views/ChangesPlans');

const DatabaseUpdate = () =>
  import('@/modules/Release/views/Manage/DatabaseUpdate');
const ParamsMaintain = () => import('@/modules/Release/views/ParamsMaintain');

const ChangeTemplate = () => import('@/modules/Release/views/ChangeTemplate');
const Profile = () => import('@/modules/Release/views/ChangeTemplate/Detail');
const ReviewRelatedItems = () =>
  import('@/modules/Release/views/ReviewRelatedItems');
const ReviewDetails = () => import('@/modules/Release/views/ReviewDetails');
const AutoReleaseNoteAppList = () =>
  import('@/modules/Release/views/Manage/autoReleaseNoteAppList');
const AutoReleaseNoteConfigFile = () =>
  import('@/modules/Release/views/Manage/autoReleaseNoteConfigFile');
const AutoReleaseNoteDatabase = () =>
  import('@/modules/Release/views/Manage/autoReleaseNoteDatabase');
const ExtPublishAutoRelease = () =>
  import('@/modules/Release/views/Manage/extPublishAutoRelease');
// ??????????????????
const ProductionProblemsList = () =>
  import('@/modules/Release/views/ProductionProblems/List');
const ProductionProblemsProfile = () =>
  import('@/modules/Release/views/ProductionProblems/Profile');
export default [
  {
    path: '/release',
    name: 'release',
    meta: {
      nameCn: '????????????',
      icon: 'portfolio',
      fstMenu: 'release'
    },
    children: [
      {
        path: 'list',
        name: 'releaseList',
        meta: {
          nameCn: '??????????????????',
          fstMenu: 'release',
          secMenu: 'releaseList',
          icon: 'releaseList'
        },
        component: Release
      },
      {
        path: 'changesPlans',
        name: 'changesPlans',
        meta: {
          nameCn: '????????????',
          fstMenu: 'release',
          secMenu: 'changesPlans',
          icon: 'changesPlans'
        },
        component: ChangesPlans
      },
      {
        path: 'changeTemplate',
        name: 'changeTemplate',
        meta: {
          nameCn: '??????????????????',
          fstMenu: 'release',
          secMenu: 'changeTemplate',
          icon: 'changeTemplate'
        },
        component: ChangeTemplate
      },
      {
        path: 'templateDetail/:id',
        name: 'templateDetail',
        meta: {
          nameCn: '??????',
          fstMenu: 'release',
          secMenu: 'templateDetail',
          hideInMenu: true
        },
        component: Profile
      },
      {
        path: 'ReviewDetails/:id',
        name: 'ReviewDetails',
        meta: {
          nameCn: '??????',
          fstMenu: 'release',
          secMenu: 'ReviewDetails',
          hideInMenu: true
        },
        component: ReviewDetails
      },
      {
        path: 'updateFileManage/:id',
        name: 'UpdateFileManage',
        meta: {
          nameCn: '??????????????????',
          fstMenu: 'release',
          secMenu: 'UpdateFileManage',
          hideInMenu: true
        },
        component: UpdateFileManage
      },
      {
        path: 'autoReleaseNoteDetail/:id',
        name: 'autoReleaseNoteDetail',
        meta: {
          nameCn: '??????????????????',
          fstMenu: 'release',
          secMenu: 'autoReleaseNoteDetail',
          hideInMenu: true
        },
        component: AutoReleaseNoteDetail,
        children: [
          {
            path: 'applist',
            meta: {
              nameCn: '????????????',
              fstMenu: 'release',
              secMenu: 'autoReleaseNoteDetail',
              hideInMenu: true
            },
            component: AutoReleaseNoteAppList
          },
          {
            path: 'configFile',
            meta: {
              nameCn: '????????????????????????',
              fstMenu: 'release',
              secMenu: 'autoReleaseNoteDetail',
              hideInMenu: true
            },
            component: AutoReleaseNoteConfigFile
          },
          {
            path: 'database',
            meta: {
              nameCn: '???????????????',
              fstMenu: 'release',
              secMenu: 'autoReleaseNoteDetail',
              hideInMenu: true
            },
            component: AutoReleaseNoteDatabase
          },
          {
            path: 'extPublish',
            meta: {
              nameCn: '??????????????????',
              fstMenu: 'release',
              secMenu: 'autoReleaseNoteDetail',
              hideInMenu: true
            },
            component: ExtPublishAutoRelease
          }
        ]
      },
      {
        path: 'updateDetail/:id',
        name: 'updateDetail',
        meta: {
          nameCn: '????????????',
          fstMenu: 'release',
          secMenu: 'updateDetail',
          hideInMenu: true
        },
        component: UpdateDetail,
        children: [
          {
            path: 'updateList',
            meta: {
              nameCn: '??????????????????',
              fstMenu: 'release',
              secMenu: 'updateDetail'
            },
            component: UpdateList
          },
          {
            path: 'FileUpload',
            meta: {
              nameCn: '??????????????????',
              fstMenu: 'release',
              secMenu: 'updateDetail'
            },
            component: FileUpload
          },
          {
            path: 'AppScale',
            meta: {
              nameCn: '????????????',
              fstMenu: 'release',
              secMenu: 'updateDetail'
            },
            component: AppScale
          },
          {
            path: 'DatabaseUpdate',
            meta: {
              nameCn: '???????????????',
              fstMenu: 'release',
              secMenu: 'updateDetail'
            },
            component: DatabaseUpdate
          },
          {
            path: 'extPublish',
            meta: {
              nameCn: '??????????????????',
              fstMenu: 'release'
            },
            component: ExtPublishAutoRelease
          }
        ]
      },
      {
        path: 'list/:id',
        name: 'list',
        meta: {
          nameCn: '??????',
          fstMenu: 'release',
          secMenu: 'list',
          hideInMenu: true
        },
        component: Detail,
        children: [
          {
            path: 'joblist',
            meta: {
              nameCn: '??????????????????',
              fstMenu: 'release',
              secMenu: 'list',
              hideInMenu: true
            },
            component: ReleaseJob
          },
          {
            path: 'testRunAppList',
            meta: {
              nameCn: '?????????????????????',
              fstMenu: 'release',
              secMenu: 'list',
              hideInMenu: true
            },
            component: TestRunAppList
          },
          {
            path: 'applist',
            meta: {
              nameCn: '??????????????????',
              fstMenu: 'release',
              secMenu: 'list',
              hideInMenu: true
            },
            component: ReleaseAppList
          },
          {
            path: 'componentlist',
            meta: {
              nameCn: '??????????????????',
              fstMenu: 'release',
              secMenu: 'list',
              hideInMenu: true
            },
            component: ReleaseComponentList
          },
          {
            path: 'archetypeList',
            meta: {
              nameCn: '??????????????????',
              fstMenu: 'release',
              secMenu: 'list',
              hideInMenu: true
            },
            component: ReleaseComponentList
          },
          {
            path: 'imageList',
            meta: {
              nameCn: '??????????????????',
              fstMenu: 'release',
              secMenu: 'list',
              hideInMenu: true
            },
            component: ReleaseComponentList
          },
          {
            path: 'CreateUpdate',
            meta: {
              nameCn: '????????????',
              fstMenu: 'release',
              secMenu: 'list',
              hideInMenu: true
            },
            component: CreateUpdate
          },
          {
            path: 'autoReleaseNote',
            meta: {
              nameCn: '????????????',
              fstMenu: 'release',
              secMenu: 'list',
              hideInMenu: true
            },
            component: AutoReleaseNote
          },
          {
            path: 'modifiedFiles',
            meta: {
              nameCn: '??????????????????',
              fstMenu: 'release',
              secMenu: 'list',
              hideInMenu: true
            },
            component: ModifiedFiles
          },
          {
            path: 'updateFileConfig',
            meta: {
              nameCn: '??????????????????',
              fstMenu: 'release',
              secMenu: 'list',
              hideInMenu: true
            },
            component: UpdateFileConfig
          }
        ]
      },
      {
        path: 'paramsMaintain',
        name: 'paramsMaintain',
        meta: {
          nameCn: '????????????',
          fstMenu: 'release',
          secMenu: 'paramsMaintain',
          icon: 'paramsMaintain'
        },
        component: ParamsMaintain
      },
      {
        path: 'reviewRelatedItems',
        name: 'reviewRelatedItems',
        meta: {
          nameCn: '?????????????????????',
          fstMenu: 'release',
          secMenu: 'reviewRelatedItems',
          icon: 'reviewRelatedItems'
        },
        component: ReviewRelatedItems
      },
      {
        path: 'BigReleaseDetail/:release_date',
        meta: {
          nameCn: '?????????????????????',
          fstMenu: 'release',
          secMenu: 'BigReleaseDetail',
          hideInMenu: true
        },
        component: BigReleaseDetail,
        name: 'BigReleaseDetail',
        children: [
          {
            name: 'Process',
            path: 'process',
            meta: {
              hideInMenu: true,
              nameCn: '??????????????????',
              fstMenu: 'release',
              secMenu: 'BigReleaseDetail'
            },
            component: Process
          },
          {
            name: 'Contact',
            path: 'contact',
            meta: {
              hideInMenu: true,
              nameCn: '???????????????/??????????????????',
              fstMenu: 'release',
              secMenu: 'BigReleaseDetail'
            },
            component: Contact
          },
          {
            name: 'Demand',
            path: 'demand',
            meta: {
              hideInMenu: true,
              nameCn: '????????????',
              fstMenu: 'release',
              secMenu: 'BigReleaseDetail'
            },
            component: Demand
          },
          {
            name: 'Test',
            path: 'test',
            meta: {
              hideInMenu: true,
              nameCn: '????????????',
              fstMenu: 'release',
              secMenu: 'BigReleaseDetail'
            },
            component: Test
          },
          {
            name: 'Security',
            path: 'security',
            meta: {
              hideInMenu: true,
              nameCn: '??????????????????',
              fstMenu: 'release',
              secMenu: 'BigReleaseDetail'
            },
            component: Security
          }
        ]
      },
      // ??????????????????
      {
        path: 'productionProblemsList',
        name: 'productionProblemsList',
        meta: {
          nameCn: '????????????',
          fstMenu: 'release',
          secMenu: 'productionProblemsList',
          icon: 'problemList'
        },
        component: ProductionProblemsList
      },
      {
        path: 'ProductionProblems/:id',
        name: 'ProductionProblemsProfile',
        meta: {
          nameCn: '??????????????????',
          hideInMenu: true,
          fstMenu: 'release',
          secMenu: 'problemList'
        },
        component: ProductionProblemsProfile
      }
    ]
  }
];
