package com.spdb.fdev.fdevenvconfig.base;


import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.fdevenvconfig.base.dict.Constants;
import com.spdb.fdev.fdevenvconfig.base.dict.Dict;
import com.spdb.fdev.fdevenvconfig.base.dict.ErrorConstants;
import com.spdb.fdev.fdevenvconfig.spdb.dao.ICommonDao;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @author csii_shenzy
 * @date 2019/7/5 14:16
 */
@Component
public class CommonValidate {

    private CommonValidate() {
    }

    public static void validateRepeatParamForUpdate(Object parm, String operator, String[] validateParms,
                                                    Class<?> clazz, ICommonDao commonDao) {
        Map<Object, Object> validateMap = new HashMap<>();
        if (parm instanceof Map) {
            validateMap = (Map) parm;
        } else {
            for (String validateParm : validateParms) {
                Object value;
                try {
                    value = CommonUtils.getGetMethod(parm, validateParm);
                    validateMap.put(validateParm, value);
                } catch (Exception ignored) {
                }
            }
        }
        List<?> commonQuery = commonDao.commonQuery(validateMap, operator, clazz);
        StringBuilder errorMsg = new StringBuilder();
        if (commonQuery != null && commonQuery.size() > 1) {
            Object object = commonQuery.get(0);
            for (String validateParm : validateParms) {
                try {
                    String existValue = (String) CommonUtils.getGetMethod(object, validateParm);
                    String nowValue = (String) CommonUtils.getGetMethod(parm, validateParm);
                    if (StringUtils.isNotBlank(existValue) && existValue.equals(nowValue)) {
                        errorMsg.append(CommonUtils.getFiledAnnotationVal(parm, validateParm, ApiModelProperty.class,
                                Dict.VALUE));
                        errorMsg.append("????????????,??????????????????");
                    }
                } catch (Exception ignored) {
                }

            }
        } else if (commonQuery != null && commonQuery.size() == 1) {
            Object object = commonQuery.get(0);
            Object queryId = CommonUtils.getGetMethod(object, Constants.ID);
            Object id = CommonUtils.getGetMethod(parm, Constants.ID);
            if (!CommonUtils.isNullOrEmpty(queryId) && !queryId.equals(id)) {
                for (String validateParm : validateParms) {
                    try {
                        String existValue = (String) CommonUtils.getGetMethod(object, validateParm);
                        String nowValue = (String) CommonUtils.getGetMethod(parm, validateParm);
                        if (StringUtils.isNotBlank(existValue) && existValue.equals(nowValue)) {
                            errorMsg.append(CommonUtils.getFiledAnnotationVal(parm, validateParm, ApiModelProperty.class,
                                    Constants.VALUE));
                            errorMsg.append("????????????,??????????????????");
                        }
                    } catch (Exception ignored) {
                    }

                }
            }
        }

        if (!"".equals(errorMsg.toString())) {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{errorMsg.toString()});
        }
    }

    public static void validateRepeatParam(Object parm, String operator, String[] validateParms, Class<?> clazz,
                                           ICommonDao commonDao) throws Exception {
        Map<Object, Object> validateMap = new HashMap<>();
        if (parm instanceof Map) {
            Map parmMap = (Map) parm;
            for (String validateParm : validateParms) {
                validateMap.put(validateParm, parmMap.get(validateParm));
            }
        } else {
            for (String validateParm : validateParms) {
                Object value;
                try {
                    value = CommonUtils.getGetMethod(parm, validateParm);
                    validateMap.put(validateParm, value);
                } catch (Exception ignored) {
                }
            }
        }
        List<?> commonQuery = commonDao.commonQuery(validateMap, operator, clazz);
        StringBuilder errorMsg = new StringBuilder();
        if (!commonQuery.isEmpty()) {
            Object object = commonQuery.get(0);
            for (String validateParm : validateParms) {
                try {
                    String existValue = (String) CommonUtils.getGetMethod(object, validateParm);
                    String nowValue = (String) CommonUtils.getGetMethod(parm, validateParm);
                    if (StringUtils.isNotBlank(existValue) && existValue.equals(nowValue)) {
                        errorMsg.append(CommonUtils.getFiledAnnotationVal(parm, validateParm, ApiModelProperty.class,
                                Constants.VALUE));
                        errorMsg.append("????????????,??????????????????");
                    }
                } catch (Exception ignored) {
                }
            }
        }

        if (!"".equals(errorMsg.toString())) {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{errorMsg.toString()});
        }

    }

    /**
     * ????????????env_key??????name_en????????????
     *
     * @param parmList ??????env_key
     * @param key      name_en
     */
    public static void validateRepeatParam(List<Object> parmList, String key) {
        Set<String> set = new HashSet<>();
        for (Object o : parmList) {
            Map<String, String> map = (Map<String, String>) o;
            if (set.contains(map.get(key))) {
                throw new FdevException(ErrorConstants.PARAM_ERROR,
                        new String[]{"??????KEY???" + key + "??????" + map.get(key) + "??????"});
            } else {
                String value = map.get(key);
                if (Constants.NAME_EN.equals(key)) {
                    Boolean flag = Pattern.matches(Constants.PATTERN_MODEL_NAME_EN, value);
                    if (!flag) {
                        String errorMsg = "??????env_key???" + key + "????????????????????????????????????";
                        throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{errorMsg});
                    }
                }
                set.add(map.get(key));
            }
        }
    }

    /**
     * ???????????????envKey??????propKey????????????(??????)
     * ???????????????envKey?????????????????????????????????
     *
     * @param parmList ???????????????envKey
     * @param key      propKey
     */
    public static void validateTemplateRepeatParam(List<Object> parmList, String key) {
        Set<String> set = new HashSet<>();
        for (Object o : parmList) {
            Map<String, String> map = (Map<String, String>) o;
            if (set.contains(map.get(key))) {
                throw new FdevException(ErrorConstants.PARAM_ERROR,
                        new String[]{"????????????ENVKEY???" + key + "??????" + map.get(key) + "??????"});
            } else {
                String value = map.get(key);
                if (Dict.PROPKEY.equals(key)) {
                    Boolean flag = Pattern.matches(Constants.PATTERN_MODEL_TEMPLATE_NAME_EN, value);
                    if (!flag) {
                        String errorMsg = "????????????envKey???" + key + "????????????????????????????????????";
                        throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{errorMsg});
                    }
                }
                set.add(map.get(key));
            }
        }
    }


    public static void validateModelType(String type) {
        if (!"deploy".equals(type) && !"runtime".equals(type)) {
            throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{"?????????????????????"});
        }

    }

    /**
     * ????????????????????????????????????????????????,?????????????????????????????????
     *
     * @param parm          ?????????
     * @param validateParms ???????????????????????????
     * @return ?????????????????????
     */
    public static String validateRepeatParamPattern(Object parm, String[] validateParms) {
        StringBuilder nameEn = new StringBuilder();
        if (!(parm instanceof Map)) {
            for (String validateParm : validateParms) {
                Object value;
                try {

                    value = CommonUtils.getGetMethod(parm, validateParm);
                    String v = (String) value;
                    if (Constants.TYPE.equals(validateParm)) {
                        if (Constants.COMM.equals(value)) {
                            continue;
                        }
                    }
                    boolean flag;
                    String errorMsg;
                    if (Constants.SUFFIX_NAME.equals(validateParm)) {
                        flag = Pattern.matches(Constants.PATTERN_SUFFIX_NAME, v);
                        errorMsg = "???????????????????????????-";
                    } else {
                        flag = Pattern.matches(Constants.PATTERN_MODEL_FILTER, v);
                        errorMsg = "????????????????????????";
                    }

                    if (!flag) {
                        errorMsg = validateParm + errorMsg;
                        throw new FdevException(ErrorConstants.PARAM_ERROR, new String[]{errorMsg});
                    }
                    if (nameEn.length() == 0) {
                        nameEn.append(v);
                    } else {
                        nameEn.append("_").append(v);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return nameEn.toString();
    }

}