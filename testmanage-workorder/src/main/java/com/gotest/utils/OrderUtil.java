package com.gotest.utils;

import com.gotest.dao.FdevSitMsgMapper;
import com.gotest.dao.GroupMapper;
import com.gotest.dict.Constants;
import com.gotest.dict.Dict;
import com.gotest.dict.ErrorConstants;
import com.gotest.domain.FdevSitMsg;
import com.gotest.domain.WorkOrder;
import com.gotest.service.IUserService;
import com.test.testmanagecommon.exception.FtmsException;
import com.test.testmanagecommon.transport.RestTransport;
import com.test.testmanagecommon.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RefreshScope
public class OrderUtil {
    private final static Logger logger = LoggerFactory.getLogger(OrderUtil.class);
    @Autowired
    private GroupMapper groupMapper;
    @Autowired
    private RestTransport restTransport;
    @Autowired
    private FdevSitMsgMapper fdevSitMsgMapper;
    @Autowired
    private IUserService userService;
    @Value("${user.assessor.role.id}")
    private String assessorRoleId;

    @Value("${user.testManager.role.id}")
    private String testManagerRoleId;

    @Value("${user.testLeader.role.id}")
    private String testLeaderRoleId;

    @Value("${user.securityTestLeader.role.id}")
    private String securityTestLeaderRoleId;

    @Value("${user.tester.role.id}")
    private String testerRoleId;

    @Value("${user.testAdmin.role.id}")
    private String testAdminRoleId;

    public static final String dateFormate = "yyyy/MM/dd";

    public static final String dateFormate2 = "yyyy-MM-dd";
    public List<String> strToListNoRole(Map map, String key){
        List<String> resList = new ArrayList<String>();
        try {
            resList = (List<String>) map.getOrDefault(key, new ArrayList<>());
        }catch (ClassCastException e){
            String values = (String) map.getOrDefault(key, Constants.FAIL_GET);
            resList.addAll(Arrays.asList(values.split(",")));
        }
        //????????????
        resList.remove("");
        return resList;
    }

    /**
     * ????????? ???????????????????????????????????????????????????????????????
     *  ??????????????????????????????String???
     *      ?????????????????????????????????????????????List?????????
     */
    public List<String> strToList(Map map, String key) throws Exception{
        List<String> resList = strToListNoRole(map, key);
        resList.remove("");
        FtmsException ftmsException;
        for(String userEnName : resList){
            ftmsException = new FtmsException(ErrorConstants.ROLE_ERROR, new String[]{userEnName});
            ftmsException.setMessage("?????????" + userEnName + " ????????????");
            Map<String, Object> user = userService.queryUserDetail(userEnName);
            if(CommonUtils.isNullOrEmpty(user)) {
                throw new FtmsException(ErrorConstants.DATA_NOT_EXIST, new String[] {userEnName});
            }
            List<String> role = (List<String>)user.get(Dict.ROLE_ID);
            if (key.equals(Dict.ADUITS)) {
                if(!role.contains(assessorRoleId)){
                    logger.error("?????????" + userEnName + " ????????????");
                    throw ftmsException;
                }
            }else if (key.equals(Dict.GROUPLEADER)){
                if(!role.contains(testLeaderRoleId) && !role.contains(testAdminRoleId)
                        && !role.contains(testManagerRoleId) && !role.contains(securityTestLeaderRoleId)){
                    logger.error("?????????" + userEnName + " ????????????");
                    throw ftmsException;
                }
            }else if (key.equals(Dict.TESTERS)){
                if (!role.contains(testerRoleId)){
                    logger.error("?????????" + userEnName + " ????????????");
                    throw ftmsException;
                }
            }
        }

        return resList;
    }


    /**
     *  ?????? workOrder?????????????????????????????????
     */
    public WorkOrder makeWorkOrder(Map map) throws Exception{
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWorkOrderNo((String) map.getOrDefault(Dict.WORKORDERNO, Constants.FAIL_GET));
        workOrder.setMainTaskNo((String) map.getOrDefault(Dict.MAINTASKNO, Constants.FAIL_GET));
        workOrder.setMainTaskName((String) map.getOrDefault(Dict.MAINTASKNAME, Constants.FAIL_GET));
        workOrder.setGroupId((String) map.getOrDefault(Dict.GROUPID, Constants.FAIL_GET));
        //---????????????????????????????????????????????????????????????
        workOrder.setStage((String) map.getOrDefault(Dict.STAGE, Constants.FAIL_GET));
        workOrder.setUnit((String) map.getOrDefault(Dict.UNIT, Constants.FAIL_GET));
        workOrder.setPlanSitDate((String) map.getOrDefault(Dict.PLANSITDATE, Constants.FAIL_GET));
        workOrder.setPlanUatDate((String) map.getOrDefault(Dict.PLANUATDATE, Constants.FAIL_GET));
        workOrder.setPlanProDate((String) map.getOrDefault(Dict.PLANPRODATE, Constants.FAIL_GET));
        workOrder.setWorkOrderFlag((String) map.getOrDefault(Dict.WORKORDERFLAG, Constants.FAIL_GET));
        workOrder.setRemark((String) map.getOrDefault(Dict.REMARK, Constants.FAIL_GET));
        workOrder.setCreateTime((Long) map.getOrDefault(Dict.CREATETIME, null));
        workOrder.setUnit((String) map.getOrDefault(Dict.UNIT, Constants.FAIL_GET));
        workOrder.setImageLink((String)map.getOrDefault(Dict.IMAGELINK, Constants.FAIL_GET));
        workOrder.setField3((String)map.getOrDefault("field3", Constants.FAIL_GET));
        //???????????????
        String workManager = (String) map.getOrDefault(Dict.WORKMANAGER, Constants.FAIL_GET);
        if (workManager != null && workManager.trim().length() != 0) {
            Map<String, Object> user = userService.queryUserDetail(workManager);
            List<String> role = (List<String>)user.get(Dict.ROLE_ID);
            if(role.contains(testManagerRoleId) || role.contains(testAdminRoleId)){
                workOrder.setWorkManager(workManager);
            }else{
                logger.error("?????????" + workManager + " ?????????????????????");
                FtmsException ftmsException = new FtmsException(ErrorConstants.ROLE_ERROR, new String[]{workManager});
                ftmsException.setMessage("?????????" + workManager + " ?????????????????????");
                throw ftmsException;
            }
        }
        //????????????
        if("6".equals(workOrder.getStage()) || "10".equals(workOrder.getStage()) || "13".equals(workOrder.getStage())) {
            List<String> aduits = this.strToList(map, Dict.ADUITS);
            workOrder.setField2(String.join(",", aduits));
        }
        //????????????
        workOrder.setOrderType((String)map.getOrDefault(Dict.ORDERTYPE, Constants.FAIL_GET));
        return workOrder;
    }

    /**
     * ??????????????????????????????????????????????????????????????????
     *
     * @param inputTime
     * @param startEndFlag
     * @return
     */
    public static Long dateStrToLong(String inputTime, boolean startEndFlag){
        Long outTime = null;
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            outTime = (simpleDateFormat.parse(inputTime + (startEndFlag?" 00:00:00":" 23:59:59"))).getTime()/1000;
        } catch (ParseException e) {
            return outTime;
        }
        return outTime;
    }

    //??????????????????????????????long
    public static Long dateStrToLong2(String inputTime){
        Long outTime = null;
        if(Util.isNullOrEmpty(inputTime)) {
            return outTime;
        }
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");
                outTime = (simpleDateFormat.parse(inputTime)).getTime() / 1000;
            } catch (ParseException e) {
                return outTime;
            }
        return outTime;
    }

    public static String timeStampToStr(long timeStamp, String dateFormate) {
    	String str = "";
    	try {
    	if(!MyUtil.isNullOrEmpty(timeStamp) && timeStamp != 0) {
    		SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormate);
    		str = dateFormat.format(timeStamp*1000);
    	}}catch(Exception e) {
    		logger.info("=======dateFormat Error");
    	}
    	return str;
    }


    /**
     * ????????????id???????????????
     * @param order
     * @return
     * @throws Exception
     */
    public List<FdevSitMsg> checkFdevTaskAndSitMsg(WorkOrder order) throws Exception {
        String flag = order.getWorkOrderFlag();
        String workNo = order.getWorkOrderNo();
        //??????fdev??????????????????????????????
        if("0".equals(flag)){
            logger.error("order does not come from fdev");
            throw new FtmsException(ErrorConstants.MAINTASKNO_NOT_EXIST_ERROR);
        }
        List<String> taskNos = fdevSitMsgMapper.queryTaskNoByOrder(workNo);
        List<FdevSitMsg> result = new ArrayList<>();
        for(String taskNo : taskNos){
            FdevSitMsg fdevSitMsg = fdevSitMsgMapper.queryLastFdevSitMsg(workNo, taskNo);
            result.add(fdevSitMsg);
        }
        return result;
    }

    /**
     * ??????fdev??????tag
     * @param origin ??????tag????????????
     * @param tag   ??????tag
     * @param delete    ???????????????tag
     * @param taskNo    ??????id
     * @throws Exception
     */
    public void updateFdevTag(List<String> origin, String tag, List<String> delete, String taskNo){
        //????????????tag
        if(!origin.contains(tag)){
            origin.add(tag);
        }
        //??????????????????tag
        delete.stream().forEach(e->{origin.remove(e);});
        //??????
        try {
            //???fdev????????????update????????????tag
            Map sendFdev = new HashMap();
            sendFdev.put(Dict.ID, taskNo);
            sendFdev.put(Dict.TAG, origin);
            sendFdev.put(Dict.REST_CODE, "updatetaskinner");
            restTransport.submitSourceBack(sendFdev);
        } catch (Exception e) {
            logger.error("fail to update fdev tag");
        }
    }

    public static String timeAddDay(String day) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = sdf.parse(day);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DATE, 1);
            Date newDate = calendar.getTime();
            return sdf.format(newDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String timeAddWeek(String day) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = sdf.parse(day);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.WEEK_OF_MONTH, 1);
            Date newDate = calendar.getTime();
            return sdf.format(newDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String timeAddmonth(String day) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = sdf.parse(day);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.MONTH, 1);
            Date newDate = calendar.getTime();
            return sdf.format(newDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * ?????????????????????????????????
     * @param orderType
     * @return
     */
    public static String getOrderTypeCH(String orderType) {
        switch (orderType) {
            case Constants.ORDERTYPE_FUNCTION:
                return Constants.ORDERTYPE_FUNCTION_CH;
            case Constants.ORDERTYPE_SECURITY:
                return Constants.ORDERTYPE_SECURITY_CH;
            default:
                return Constants.ORDERTYPE_FUNCTION_CH;
        }
    }
}
