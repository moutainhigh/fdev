package com.spdb.fdev.fdemand.spdb.notify.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.fdemand.base.dict.Constants;
import com.spdb.fdev.fdemand.base.dict.Dict;
import com.spdb.fdev.fdemand.base.dict.ErrorConstants;
import com.spdb.fdev.fdemand.base.utils.CommonUtils;
import com.spdb.fdev.fdemand.spdb.entity.DemandBaseInfo;
import com.spdb.fdev.fdemand.spdb.notify.INotifyStrategy;
import com.spdb.fdev.fdemand.spdb.service.IDemandBaseInfoService;
import com.spdb.fdev.fdemand.spdb.service.IMailService;
import com.spdb.fdev.fdemand.spdb.service.IRoleService;
import com.spdb.fdev.fdemand.spdb.unit.DesignUnit;
import com.spdb.fdev.transport.RestTransport;
@RefreshScope
@Lazy
@Component(Dict.UiAuditWaitImpl)
public class UiAuditWaitImpl implements INotifyStrategy{
	private static final Logger logger = LoggerFactory.getLogger(DemandDeferNotifyImpl.class);

    @Autowired
    private IDemandBaseInfoService demandBaseInfoService;

    @Autowired
    private IRoleService roleService;

    @Autowired
    private IMailService mailService;

    @Autowired
    private RestTransport restTransport;
    
    @Autowired
    private IRoleService fdevUserService;
    
    @Autowired
    private DesignUnit designUnit;


    @Value("${fdev.demand.ip}")
    private String demandIp;

    
    @Override
    public void doNotify(Map<String, Object> param) {
        try {
            String id = (String) param.get(Dict.ID);
            DemandBaseInfo demandBaseInfo = demandBaseInfoService.queryById(id);
            //1.??????????????????
            HashMap hashMap = new HashMap();
            String demand_type = demandBaseInfo.getDemand_type();
            String oa_contact_no = demandBaseInfo.getOa_contact_no();
            String demand_type_cn = new String();
            if ((Dict.BUSINESS).equalsIgnoreCase(demand_type)) {
            	demand_type_cn = "????????????";
			}else {
				throw new FdevException(ErrorConstants.DEMAND_TYPE_ERROR);
			}
            Map groupInfo = designUnit.getGroupInfo(new HashMap() {{
                put(Dict.ID, demandBaseInfo.getDemand_leader_group());
            }});
            String group = (String) groupInfo.get("parent_id");
            String sortNum = (String) groupInfo.get("sortNum");
            String emailGroup;
            if (sortNum.length()>3) {
            	String newSortNum = sortNum.substring(0, 3);
            	Map currentGroup = designUnit.getGroupInfo(new HashMap() {{
            		put("sortNum", newSortNum);
            	}});
            	emailGroup = (String) currentGroup.get("name");
			}else {
				emailGroup = (String) groupInfo.get("name");
			}
            hashMap.put(Dict.DEMAND_UI_URL, demandIp + "/fdev/#/rqrmn/designReviewRqr/"+demandBaseInfo.getId());//UI???????????????????????????
            String mailContent = mailService.getUiAuditWaitMailContent(hashMap);
            //2. ??????????????????????????????
            Set<String> users = new HashSet<>();
            users.add(demandBaseInfo.getUi_verify_user());//??????Ui??????????????????
            
            //3.?????????????????????
            Map<String, Object> mapId = new HashMap<>();
            mapId.put(Dict.IDS, users);
            mapId.put(Dict.REST_CODE, Dict.QUERYBYUSERCOREDATA);
            Map<String, Map> userMap = (Map<String, Map>) restTransport.submit(mapId);
            List<String> email_receivers = new ArrayList<>();
            Set<Map.Entry<String, Map>> entries = userMap.entrySet();
            if (!CommonUtils.isNullOrEmpty(entries)) {
                for (Map.Entry<String, Map> entry : entries) {
                    try {
                        String email = (String) entry.getValue().get(Dict.EMAIL);
                        email_receivers.add(email);
                    } catch (Exception e) {
                        logger.error("??????????????????????????????" + entry.getKey());
                    }
                }
            }
            //4.????????????
            HashMap<String, String> sendMap = new HashMap();
            sendMap.put(Dict.EMAIL_CONTENT, mailContent);
            String topic = "???"+emailGroup+"-"+demand_type_cn+"-"+oa_contact_no+"???"+"?????????????????????";
            mailService.sendEmail(topic, Dict.FDEMAND_UIAUDITWAIT_FTL, sendMap, email_receivers.toArray(new String[email_receivers.size()]));
        } catch (Exception e) {
            logger.error("Ui????????????????????????????????????{}", e.getStackTrace());
        }
    }
}