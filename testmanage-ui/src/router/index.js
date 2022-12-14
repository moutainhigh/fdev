import Vue from 'vue';
import Router from 'vue-router';
import axios from 'axios';
import { oauthGetAuthorization } from '@/services/login';
import { getUserRole } from '@/common/utlis';

const BasicLayout = () => import('@/layout/BasicLayout');
const Login = () => import('@/pages/login/Login');
const TestOrder = () => import('@/pages/WorkOrder/TestOrder');
const TaskList = () => import('@/pages/WorkOrder/TaskList');
const Plan = () => import('@/pages/TestPlan/Plan');
const Reuse = () => import('@/pages/TestPlan/Reuse');
const CaseBase = () => import('@/pages/TestCase/CaseBase');
const UserAdmin = () => import('@/pages/Admin/UserAdmin');
const FunctionMenu = () => import('@/pages/Admin/FunctionMenu');
const GeneralApproval = () => import('@/pages/Admin/GeneralApproval');
const DiscardApproval = () => import('@/pages/Admin/DiscardApproval');
const PassApproval = () => import('@/pages/Admin/PassApproval');
const EffectiveApproval = () => import('@/pages/Admin/EffectiveApproval');
const Notice = () => import('@/pages/Admin/Notice');
const About = () => import('@/pages/Admin/About');
const testReportSIT = () => import('@/pages/Admin/testReportSIT');
const Message = () => import('@/pages/Admin/Message');
const PrintContent = () => import('@/pages/Admin/PrintContent');
const SubmittedText = () => import('@/pages/Admin/SubmittedText/List');
const SubmittedTextProfile = () =>
  import('@/pages/Admin/SubmittedText/Profile');
const HistoryOrder = () => import('@/pages/WorkOrder/HistoryOrder');
const QueryOrder = () => import('@/pages/WorkOrder/QueryOrder');
const TestCaseExecute = () => import('@/pages/TestCase/TestCaseExecute');
const UserChart = () => import('@/pages/Charts/UserChart');
const OrderChart = () => import('@/pages/Charts/OrderChart');
const DefectChart = () => import('@/pages/Charts/defectChart');
const ApprovalOrder = () => import('@/pages/WorkOrder/ApprovalOrder');
const WasteOrder = () => import('@/pages/WorkOrder/WasteOrder');
const GroupChart = () => import('@/pages/Charts/GroupChart');
const QualityChart = () => import('@/pages/Charts/QualityChart');
const MantisIssue = () => import('@/pages/Mantis/MantisIssue');
const ProductionsProblem = () => import('@/pages/Mantis/ProductionsProblem');
const TestData = () => import('@/pages/DataMenu/TestData');
const SystemContact = () => import('@/pages/DataMenu/SystemContact');
const CreditCardOL = () => import('@/pages/DataMenu/CreditCardOL');
const ESBOL = () => import('@/pages/DataMenu/ESBOL');
const MantisInfo = () => import('@/pages/Mantis//MantisInfo');
const AssignOrder = () => import('@/pages/AutoAssign//AssignOrder');
const UserProfile = () => import('@/pages/Admin/UserProfile');

const AssertData = () => import('@/pages/AutoTest/AssertData');
const CaseDetail = () => import('@/pages/AutoTest/CaseDetail');
const ComponentData = () => import('@/pages/AutoTest/ComponentData');
const ComponentDetail = () => import('@/pages/AutoTest/ComponentDetail');
const ElementDictionary = () => import('@/pages/AutoTest/ElementDictionary');
const ElementPosition = () => import('@/pages/AutoTest/ElementPosition');
const MenuInfo = () => import('@/pages/AutoTest/MenuInfo');
const TestCase = () => import('@/pages/AutoTest/TestCase');
const TestDataAuto = () => import('@/pages/AutoTest/TestData');
const UserInfo = () => import('@/pages/AutoTest/UserInfo');

Vue.use(Router);

axios.defaults.withCredentials = true;
Vue.prototype.$http = axios;
Vue.prototype.$http = axios.create({});

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/Login',
    name: 'Login',
    component: Login,
    meta: {
      name: '??????'
    }
  },
  {
    path: '/MantisInfo',
    name: 'MantisInfo',
    meta: {
      requireAuth: true,
      name: ''
    },
    component: MantisInfo
  },

  {
    path: '/Torder',
    component: BasicLayout,
    name: 'testPlatform',
    redirect: 'TestOrder',
    children: [
      {
        path: '/TestOrder',
        name: 'TestOrder',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: TestOrder
      },
      {
        path: '/TaskList',
        name: 'TaskList',
        meta: {
          requireAuth: true,
          name: '???????????????'
        },
        component: TaskList
      },
      {
        path: '/Plan',
        name: 'Plan',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: Plan
      },
      {
        path: '/CaseBase',
        name: 'CaseBase',
        meta: {
          requireAuth: true,
          name: '?????????'
        },
        component: CaseBase
      },
      {
        path: '/UserAdmin',
        name: 'UserAdmin',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: UserAdmin
      },
      {
        path: '/FunctionMenu',
        name: 'FunctionMenu',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: FunctionMenu
      },
      {
        path: '/GeneralApproval',
        name: 'GeneralApproval',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: GeneralApproval
      },
      {
        path: '/DiscardApproval',
        name: 'DiscardApproval',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: DiscardApproval
      },
      {
        path: '/PassApproval',
        name: 'PassApproval',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: PassApproval
      },
      {
        path: '/EffectiveApproval',
        name: 'EffectiveApproval',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: EffectiveApproval
      },
      {
        path: '/Notice',
        name: 'Notice',
        meta: {
          requireAuth: true,
          name: '??????'
        },
        component: Notice
      },
      {
        path: '/About',
        name: 'About',
        meta: {
          requireAuth: true,
          name: '??????'
        },
        component: About
      },
      {
        path: '/HistoryOrder',
        name: 'HistoryOrder',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: HistoryOrder
      },
      {
        path: '/QueryOrder',
        name: 'QueryOrder',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: QueryOrder
      },
      {
        path: '/reuse',
        name: 'Reuse',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: Reuse
      },
      {
        path: '/TestCaseExecute',
        name: 'TestCaseExecute',
        meta: {
          requireAuth: true,
          name: ''
        },
        component: TestCaseExecute
      },
      {
        path: '/UserChart',
        name: 'UserChart',
        meta: {
          requireAuth: true,
          name: '??????????????????'
        },
        component: UserChart
      },
      {
        path: '/OrderChart',
        name: 'OrderChart',
        meta: {
          requireAuth: true,
          name: '??????????????????'
        },
        component: OrderChart
      },
      {
        path: '/DefectChart',
        name: 'DefectChart',
        meta: {
          requireAuth: true,
          name: '??????????????????'
        },
        component: DefectChart
      },
      {
        path: '/GroupChart',
        name: 'GroupChart',
        meta: {
          requireAuth: true,
          name: '??????????????????'
        },
        component: GroupChart
      },
      {
        path: '/QualityChart',
        name: 'QualityChart',
        meta: {
          requireAuth: true,
          name: '????????????????????????'
        },
        component: QualityChart
      },
      {
        path: '/testReportSIT',
        name: 'testReportSIT',
        meta: {
          requireAuth: true,
          name: 'SIT????????????'
        },
        component: testReportSIT
      },
      {
        path: '/Message',
        name: 'Message',
        meta: {
          requireAuth: true,
          name: '??????'
        },
        component: Message
      },
      {
        path: '/PrintContent',
        name: 'PrintContent',
        meta: {
          requireAuth: true,
          name: 'SIT??????????????????'
        },
        component: PrintContent
      },
      {
        path: '/sitMsg',
        name: 'SubmittedText',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: SubmittedText
      },
      {
        path: '/sitMsg/:id',
        name: 'SubmittedTextProfile',
        props: route => ({
          id: route.params.id
        }),
        meta: {
          requireAuth: true,
          name: '??????????????????'
        },
        component: SubmittedTextProfile
      },
      {
        path: '/ApprovalOrder',
        name: 'ApprovalOrder',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: ApprovalOrder
      },
      {
        path: '/WasteOrder',
        name: 'WasteOrder',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: WasteOrder
      },
      {
        path: '/MantisIssue',
        name: 'MantisIssue',
        meta: {
          requireAuth: true,
          name: 'SIT??????'
        },
        component: MantisIssue
      },
      {
        path: '/ProductionsProblem',
        name: 'ProductionsProblem',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: ProductionsProblem
      },
      {
        path: '/TestData',
        name: 'TestData',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: TestData
      },
      {
        path: '/SystemContact',
        name: 'SystemContact',
        meta: {
          requireAuth: true,
          name: '???????????????'
        },
        component: SystemContact
      },
      {
        path: '/CreditCardOL',
        name: 'CreditCardOL',
        meta: {
          requireAuth: true,
          name: '?????????????????????'
        },
        component: CreditCardOL
      },
      {
        path: '/ESBOL',
        name: 'ESBOL',
        meta: {
          requireAuth: true,
          name: 'ESB????????????'
        },
        component: ESBOL
      },
      {
        path: '/AssignOrder',
        name: 'AssignOrder',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: AssignOrder
      },
      {
        path: '/userProfile',
        name: 'UserProfile',
        meta: {
          requireAuth: true,
          name: '????????????'
        },
        component: UserProfile
      },
      {
        path: '/AssertData',
        name: 'AssertData',
        meta: {
          requireAuth: true,
          name: '????????????????????????'
        },
        component: AssertData
      },
      {
        path: '/CaseDetail',
        name: 'CaseDetail',
        meta: {
          requireAuth: true,
          name: '??????????????????'
        },
        component: CaseDetail
      },
      {
        path: '/ComponentData',
        name: 'ComponentData',
        meta: {
          requireAuth: true,
          name: '??????????????????'
        },
        component: ComponentData
      },
      {
        path: '/ComponentDetail',
        name: 'ComponentDetail',
        meta: {
          requireAuth: true,
          name: '????????????????????????'
        },
        component: ComponentDetail
      },
      {
        path: '/ElementDictionary',
        name: 'ElementDictionary',
        meta: {
          requireAuth: true,
          name: '??????????????????????????????'
        },
        component: ElementDictionary
      },
      {
        path: '/ElementPosition',
        name: 'ElementPosition',
        meta: {
          requireAuth: true,
          name: '????????????????????????'
        },
        component: ElementPosition
      },
      {
        path: '/MenuInfo',
        name: 'MenuInfo',
        meta: {
          requireAuth: true,
          name: '??????????????????'
        },
        component: MenuInfo
      },
      {
        path: '/TestCase',
        name: 'TestCase',
        meta: {
          requireAuth: true,
          name: '??????????????????'
        },
        component: TestCase
      },
      {
        path: '/TestDataAuto',
        name: 'TestDataAuto',
        meta: {
          requireAuth: true,
          name: '??????????????????'
        },
        component: TestDataAuto
      },
      {
        path: '/UserInfo',
        name: 'UserInfo',
        meta: {
          requireAuth: true,
          name: '????????????????????????'
        },
        component: UserInfo
      }
    ]
  }
];

const router = new Router({
  routes
});

async function login(loginToken) {
  await oauthGetAuthorization({
    token: loginToken
  }).then(res => {
    sessionStorage.setItem('userInfo', JSON.stringify(res));
    let userRole = getUserRole();
    localStorage.setItem('userToken', res.userToken);
    sessionStorage.setItem('Trole', userRole);
    sessionStorage.setItem('TuserName', res.user_name_cn);
    sessionStorage.setItem('user_en_name', res.user_name_en);
    localStorage.setItem('user_en_name', res.user_name_en);
    sessionStorage.setItem('isAssessor', res.isAssessor);
    sessionStorage.setItem('userId', res.user_id);
    sessionStorage.setItem('mantisToken', res.mantis_token);
    sessionStorage.setItem('userRole', JSON.stringify(res.role));
    sessionStorage.setItem('groupName', res.group ? res.group.fullName : '');
    sessionStorage.setItem('group_id', res.group_id);
  });
}

router.beforeEach(async (to, from, next) => {
  if (to.path == '/TestOrder') {
    // ?????????????????????????????? ????????????TestOrder?????? Header????????????TestOrder?????????????????????  ???????????????????????????token
    let loginToken = to.query.token;
    if (loginToken) {
      if (sessionStorage.getItem('TuserName') == null) {
        await login(loginToken);
      }
    }
    if (
      localStorage.getItem('userToken') == null ||
      localStorage.getItem('userToken') == ''
    ) {
      if (loginToken) {
        await login(loginToken);
        next();
      } else {
        next({
          path: '/Login'
        });
      }
    }
  }

  if (to.matched.some(res => res.meta.requireAuth)) {
    //??????????????????????????????
    //???????????????????????? ????????????localStorage????????????sessionStorage
    if (
      localStorage.getItem('userToken') &&
      sessionStorage.getItem('TuserName')
    ) {
      next();
    } else {
      next({
        path: '/Login',
        query: {
          redirect: to.fullPath
        } //???????????????????????????????????????
      });
    }
  } else {
    next();
  }
});

export default router;
