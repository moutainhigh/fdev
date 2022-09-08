package com.spdb.executor.action;

import com.csii.pe.action.ext.BaseExecutableAction;
import com.csii.pe.core.Context;
import com.csii.pe.spdb.common.dict.Dict;
import com.csii.pe.spdb.common.dict.ErrorConstants;
import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.transport.RestTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

public class FdevUnitWarnDelayAction extends BaseExecutableAction {

    private static Logger logger = LoggerFactory.getLogger(FdevUnitWarnDelayAction.class);

    @Autowired
    private RestTransport restTransport;

    @Override
    public void execute(Context context) {
        Map send = new HashMap();
        send.put(Dict.REST_CODE, "fdevUnitWarnDelay");
        try {
            restTransport.submit(send);
        } catch (Exception e) {
            logger.error("fail to fdevUnitWarnDelay");
            throw new FdevException(ErrorConstants.IMS0001);
        }
        logger.info("execute fdevUnitWarnDelay end");
    }

}
