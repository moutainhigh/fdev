package com.spdb.fdev.common;

import com.spdb.fdev.common.exception.FdevException;
import com.spdb.fdev.common.util.ErrorMessageUtil;
import com.spdb.fdev.common.util.JsonResultUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Iterator;

@RestControllerAdvice
public class FdevControllerAdvice implements InitializingBean{

    private Logger logger = LoggerFactory.getLogger(FdevControllerAdvice.class);

    @Value("${spring.application.name:unkonwn}")
    private String applicationName;

    @Autowired
    private ErrorMessageUtil errorMessageUtils;

    @ExceptionHandler(value = FdevException.class)
    public JsonResult fdevExceptionHandler(FdevException ex) {
        String errorMessage = ex.getMessage();
        if (errorMessage == null || "".equals(errorMessage)) {
            errorMessage = errorMessageUtils.get(ex);
            ex.setMessage(errorMessage);
        }
        String requestUri = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest().getRequestURI();
        logger.error(new StringBuilder("**** ").append(requestUri).append(" process error: ")
                .append(ex.getCode()).append(":").append(errorMessage).append(" ****").toString(), ex);
        JsonResult jsonResult = JsonResultUtil.buildError(ex.getCode(), errorMessage);
        return jsonResult;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public JsonResult handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        logger.error("missing request parameter! ", e);
        HttpStatus code = HttpStatus.BAD_REQUEST;
        JsonResult jsonResult = JsonResultUtil.buildError(code.toString(), "??????????????????");
        return jsonResult;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public JsonResult handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        logger.error("http message not readable! ", e);
        HttpStatus code = HttpStatus.BAD_REQUEST;
        JsonResult jsonResult = JsonResultUtil.buildError(code.toString(), "??????????????????");
        return jsonResult;
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public JsonResult methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException ex) {
        logger.error("method argument not valid! ", ex);
        HttpStatus code = HttpStatus.BAD_REQUEST;
        BindingResult bindingResult = ex.getBindingResult();
        String msg = "";
        if (bindingResult.hasErrors()) { // ???????????????????????????????????????
            Iterator<ObjectError> iterator = bindingResult.getAllErrors().iterator(); // ????????????????????????
            while (iterator.hasNext()) {
                ObjectError error = iterator.next();    // ?????????????????????
                msg = msg + error.getDefaultMessage() + ";";
            }
        }
        JsonResult jsonResult = JsonResultUtil.buildError(code.toString(), msg);
        return jsonResult;
    }

    @ExceptionHandler(value = Exception.class)
    public JsonResult exceptionHandler(Exception ex) {
        String errorMessage = ex.getMessage();
        String requestUri = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .getRequest().getRequestURI();
        logger.error(new StringBuilder("**** ").append(requestUri).append(" process error: ")
                .append(errorMessage == null ? "9999999" : errorMessage).append(" ****").toString(), ex);
        if(errorMessage == null || "".equals(errorMessage)){
            errorMessage = "??????????????????! :(";
        }
        JsonResult jsonResult = JsonResultUtil.buildError("9999999", errorMessage);
        return jsonResult;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        JsonResultUtil.setApplicationName(this.applicationName);
    }
}
