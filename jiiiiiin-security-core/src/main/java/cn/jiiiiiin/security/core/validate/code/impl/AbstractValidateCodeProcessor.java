/**
 *
 */
package cn.jiiiiiin.security.core.validate.code.impl;

import cn.jiiiiiin.security.core.validate.code.*;
import cn.jiiiiiin.security.core.validate.code.entity.ValidateCode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.connect.web.HttpSessionSessionStrategy;
import org.springframework.social.connect.web.SessionStrategy;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Map;

import static cn.jiiiiiin.security.core.validate.code.ValidateCodeFilter.SESSION_KEY_VALIDATE_CODE;

/**
 * 抽象的图片验证码处理器
 *
 * @author zhailiang
 */
public abstract class AbstractValidateCodeProcessor<C extends ValidateCode> implements ValidateCodeProcessor {

    final static Logger L = LoggerFactory.getLogger(AbstractValidateCodeProcessor.class);

    private static final String VALIDATECODEPROCESSOR_CLASSNAME_SEPARATOR = ValidateCodeProcessor.class.getSimpleName();

    /**
     * 收集系统中所有的 {@link ValidateCodeGenerator} 接口的实现。
     */
    @Autowired
    private Map<String, ValidateCodeGenerator> validateCodeGenerators;

    private SessionStrategy sessionStrategy = new HttpSessionSessionStrategy();

    @Override
    public void create(ServletWebRequest request) throws Exception {
        C validateCode = generate(request);
        save(request, validateCode);
        send(request, validateCode);
    }

    /**
     * 生成校验码
     *
     * @param request
     * @return
     */
    @SuppressWarnings("unchecked")
    private C generate(ServletWebRequest request) {
        String type = getValidateCodeType(request).toString().toLowerCase();
        String generatorName = type + ValidateCodeGenerator.class.getSimpleName();
        ValidateCodeGenerator validateCodeGenerator = validateCodeGenerators.get(generatorName);
        if (validateCodeGenerator == null) {
            throw new ValidateCodeException("验证码生成器" + generatorName + "不存在");
        }
        return (C) validateCodeGenerator.generate(request);
    }

    /**
     * 保存校验码
     *
     * @param request
     * @param validateCode
     */
    private void save(ServletWebRequest request, C validateCode) {
//		ValidateCode code = new ValidateCode(validateCode.getCode(), validateCode.getExpireTime());
//		validateCodeRepository.save(request, code, getValidateCodeType(request));
        sessionStrategy.setAttribute(request, SESSION_KEY_VALIDATE_CODE, validateCode);
    }

    /**
     * 发送校验码，由子类实现
     *
     * @param request
     * @param validateCode
     * @throws Exception
     */
    protected abstract void send(ServletWebRequest request, C validateCode) throws Exception;

    /**
     * 根据请求的url获取校验码的类型
     *
     * @param request
     * @return
     */
    private ValidateCodeType getValidateCodeType(ServletWebRequest request) {
        String type = StringUtils.substringBefore(getClass().getSimpleName(), VALIDATECODEPROCESSOR_CLASSNAME_SEPARATOR);
        return ValidateCodeType.valueOf(type.toUpperCase());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void validate(ServletWebRequest request) {

		ValidateCodeType codeType = getValidateCodeType(request);

//		C codeInSession = (C) validateCodeRepository.get(request, codeType);
        // TODO
        final ValidateCode cacheRealValidateCode = (ValidateCode) sessionStrategy.getAttribute(request, SESSION_KEY_VALIDATE_CODE);

        // 请求传递过来的验证码
		String validateCode;
		try {
			validateCode = ServletRequestUtils.getStringParameter(request.getRequest(),
					codeType.getParamNameOnValidate());
		} catch (ServletRequestBindingException e) {
			throw new ValidateCodeException("获取验证码的值失败");
		}

        L.info("验证码 imageCode::code {} validateCode {}", cacheRealValidateCode, validateCode);
        if (StringUtils.isBlank(validateCode)) {
            throw new ValidateCodeException("验证码不能为空");
        }
        if (cacheRealValidateCode.isExpired()) {
            sessionStrategy.removeAttribute(request, SESSION_KEY_VALIDATE_CODE);
            throw new ValidateCodeException("验证码已经过期");
        }
        if (!StringUtils.equalsIgnoreCase(cacheRealValidateCode.getCode(), validateCode)) {
            throw new ValidateCodeException("验证码不匹配");
        }
        // 验证通过
        sessionStrategy.removeAttribute(request, SESSION_KEY_VALIDATE_CODE);

    }

}
