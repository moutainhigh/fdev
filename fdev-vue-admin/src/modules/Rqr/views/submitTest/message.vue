<template>
  <div>
    <div class="row mt20">
      <f-formitem
        v-for="(item, index) in messageList"
        :class="
          !item.value ||
          item.value === 'inner_test_result' ||
          item.value === 'systemName'
            ? 'border-bottom'
            : ''
        "
        :key="index"
        class="col-4 border-top"
        :label="item.label"
        profile
        label-auto
        bottom-page
        label-class="bg-indigogrey-0 q-px-lg q-py-sm self-stretch"
        label-style="height:42px;width:160px;"
        value-style="line-height:42px;"
        value-class="ellipsis q-px-lg"
      >
        <div
          v-if="
            item.value === 'oa_contact_no' || item.value === 'oa_contact_name'
          "
        >
          <div :title="testOrderDetail[item.value]" class="ellipsis">
            <router-link
              v-if="testOrderDetail[item.value]"
              :to="`/rqrmn/rqrProfile/${testOrderDetail.demand_id}`"
              class="link"
            >
              {{ testOrderDetail[item.value] }}
              <fdev-popup-proxy context-menu>
                <fdev-banner style="max-width:300px">
                  {{ testOrderDetail[item.value] }}
                </fdev-banner>
              </fdev-popup-proxy>
            </router-link>
            <span v-else>-</span>
          </div>
        </div>
        <div class="ellipsis" v-else :title="testOrderDetail[item.value]">
          {{
            item.value
              ? testOrderDetail[item.value]
                ? testOrderDetail[item.value]
                : '-'
              : ''
          }}
          <fdev-popup-proxy
            context-menu
            v-if="
              item.value === 'impl_unit_num' ||
                item.value === 'fdev_implement_unit_no'
            "
          >
            <fdev-banner style="max-width:300px">
              {{ testOrderDetail[item.value] }}
            </fdev-banner>
          </fdev-popup-proxy>
        </div>
      </f-formitem>
    </div>
    <div class="mt20">
      <div class="mb10 row items-center">
        <f-icon
          name="bell_s_f"
          class="text-primary mr10"
          :width="16"
          :height="16"
        ></f-icon>
        <span class="infoStyle">????????????</span>
      </div>
      <div class="row">
        <f-formitem
          v-for="(item, index) in testList"
          :class="[
            item.value === 'remark' ? 'border-bottom' : '',
            item.value === 'remark' ||
            item.value === 'test_content' ||
            item.value === 'test_environment'
              ? 'col-12'
              : 'col-4'
          ]"
          :key="index"
          class="border-top"
          :label="item.label"
          profile
          label-auto
          bottom-page
          label-class="bg-indigogrey-0 q-px-lg q-py-sm self-stretch"
          label-style="height:100%;width:160px;"
          value-style="line-height:42px;"
          value-class="ellipsis q-px-lg"
        >
          <div
            v-if="
              item.value === 'trans_interface_change' ||
                item.value === 'database_change' ||
                item.value === 'regress_test' ||
                item.value === 'client_change'
            "
          >
            {{
              testOrderDetail[item.value] === 'yes'
                ? '??????'
                : testOrderDetail[item.value] === 'no'
                ? '?????????'
                : '-'
            }}
          </div>
          <div
            v-else-if="
              item.value === 'test_content' || item.value === 'test_environment'
            "
            v-html="testOrderDetail[item.value]"
            style="white-space:pre-line;line-height:22px;padding: 10px 0;display:flex;align-items:center"
          ></div>
          <div v-else class="ellipsis" :title="testOrderDetail[item.value]">
            <div v-if="item.value">
              {{
                testOrderDetail[item.value] ? testOrderDetail[item.value] : '-'
              }}
              <fdev-popup-proxy
                context-menu
                v-if="
                  item.value === 'app_name' || item.value === 'test_environment'
                "
              >
                <fdev-banner style="max-width:300px">
                  {{ testOrderDetail[item.value] }}
                </fdev-banner>
              </fdev-popup-proxy>
            </div>
          </div>
        </f-formitem>
      </div>
    </div>
    <div class="mt20">
      <div class="mb10 row items-center">
        <f-icon
          name="bell_s_f"
          class="text-primary mr10"
          :width="16"
          :height="16"
        ></f-icon>
        <span class="infoStyle">????????????</span>
      </div>
      <div class="row">
        <f-formitem
          v-for="(item, index) in roleList"
          :class="[
            item.value === 'test_user_info' || item.value === 'submit_time'
              ? 'border-bottom'
              : '',
            item.col
          ]"
          :key="index"
          class="border-top"
          :label="item.label"
          profile
          label-auto
          bottom-page
          label-class="bg-indigogrey-0 q-px-lg q-py-sm self-stretch"
          label-style="height:100%;width:160px;"
          value-style="line-height:42px;"
          value-class="ellipsis q-px-lg"
        >
          <div
            class="row"
            v-if="
              (item.value === 'test_manager_info' ||
                item.value === 'test_cc_user_info' ||
                item.value === 'daily_cc_user_info') &&
                Array.isArray(testOrderDetail[item.value])
            "
          >
            <div
              :title="
                testOrderDetail[item.value]
                  .map(val => val.user_name_cn)
                  .join(',')
              "
              class="ellipsis"
              v-if="testOrderDetail[item.value].length > 0"
            >
              <span
                v-for="(itm, ind) in testOrderDetail[item.value]"
                :key="ind"
                :class="ind !== 0 ? 'q-ml-sm' : ''"
              >
                <span v-if="item.value === 'test_manager_info'">
                  {{ itm.user_name_cn }}
                </span>
                <router-link v-else :to="`/user/list/${itm.id}`" class="link">
                  {{ itm && itm.user_name_cn }}
                </router-link>
              </span>
            </div>
            <div v-else>-</div>
          </div>
          <div
            class="ellipsis row"
            v-else-if="
              item.value === 'test_user_info' ||
                item.value === 'create_user_info'
            "
          >
            <!-- <span>
              {{
                testOrderDetail[item.value]
                  ? testOrderDetail[item.value].user_name_cn
                  : '-'
              }}
            </span> -->
            <router-link
              v-if="testOrderDetail[item.value]"
              :to="`/user/list/${testOrderDetail[item.value].id}`"
              class="link"
            >
              {{
                testOrderDetail[item.value] &&
                  testOrderDetail[item.value].user_name_cn
              }}
            </router-link>
            <div v-else>-</div>
          </div>
          <div v-else class="ellipsis" :title="testOrderDetail[item.value]">
            {{
              testOrderDetail[item.value] ? testOrderDetail[item.value] : '-'
            }}
          </div>
        </f-formitem>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'message',
  props: ['testOrderDetail'],
  data() {
    return {
      messageList: [
        {
          label: '????????????',
          value: 'oa_contact_name'
        },
        {
          label: '????????????',
          value: 'oa_contact_no'
        },
        {
          label: 'IPMP????????????',
          value: 'impl_unit_num'
        },
        {
          label: '??????????????????',
          value: 'fdev_implement_unit_no'
        },
        {
          label: '????????????????????????',
          value: 'plan_test_start_date'
        },
        {
          label: '??????????????????',
          value: 'unit_test_result'
        },
        {
          label: '??????????????????',
          value: 'inner_test_result'
        },
        {
          label: '????????????',
          value: 'systemName'
        },
        {
          label: '',
          value: ''
        }
      ],
      testList: [
        {
          label: '??????????????????????????????',
          value: 'trans_interface_change'
        },
        {
          label: '???????????????????????????',
          value: 'database_change'
        },
        {
          label: '????????????????????????',
          value: 'regress_test'
        },
        {
          label: '???????????????????????????',
          value: 'client_change'
        },
        {
          label: '???????????????????????????',
          value: 'app_name'
        },
        {
          label: '????????????????????????',
          value: 'regress_test_range'
        },
        {
          label: '?????????????????????',
          value: 'client_download'
        },
        {
          label: '??????????????????????????????',
          value: 'system'
        },
        {
          label: '',
          value: ''
        },
        {
          label: '????????????',
          value: 'test_environment'
        },
        {
          label: '????????????',
          value: 'test_content'
        },
        {
          label: '??????',
          value: 'remark'
        }
      ],
      roleList: [
        {
          label: '????????????',
          col: 'col-12',
          value: 'test_manager_info'
        },
        {
          label: '??????????????????????????????',
          col: 'col-12',
          value: 'test_cc_user_info'
        },
        {
          label: '????????????????????????',
          col: 'col-12',
          value: 'daily_cc_user_info'
        },
        {
          label: '??????????????????',
          col: 'col-12',
          value: 'business_email'
        },
        {
          label: '????????????',
          col: 'col-12',
          value: 'developer'
        },
        {
          label: '?????????',
          col: 'col-4',
          value: 'create_user_info'
        },
        {
          label: '????????????',
          col: 'col-8',
          value: 'create_time'
        },
        {
          label: '?????????',
          col: 'col-4',
          value: 'test_user_info'
        },
        {
          label: '????????????',
          col: 'col-8',
          value: 'submit_time'
        }
      ]
    };
  }
};
</script>

<style lang="stylus" scoped>
border(align='all')
  border-top 1px solid #ddd if align == 'top' || align == 'all'
  border-bottom 1px solid #ddd if align == 'bottom' || align == 'all'
.border-top
  border('top')
.border-bottom
  border('bottom')
.border
  border()
.mt20
  margin-top 20px
.mr10
  margin-right 10px
.mb10
  margin-bottom 10px
.infoStyle
  font-size 14px
  font-weight 600
</style>
