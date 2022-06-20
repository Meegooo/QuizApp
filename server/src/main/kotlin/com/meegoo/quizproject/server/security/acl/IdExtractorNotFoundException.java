package com.meegoo.quizproject.server.security.acl;

import org.springframework.core.convert.ConversionException;

public class IdExtractorNotFoundException extends ConversionException {

	public IdExtractorNotFoundException(String message) {
		super(message);
	}
}
