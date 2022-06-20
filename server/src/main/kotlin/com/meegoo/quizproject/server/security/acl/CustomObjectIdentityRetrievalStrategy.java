package com.meegoo.quizproject.server.security.acl;

import com.meegoo.quizproject.server.data.entity.Course;
import com.meegoo.quizproject.server.data.entity.Group;
import com.meegoo.quizproject.server.data.entity.Question;
import com.meegoo.quizproject.server.data.entity.Quiz;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;

public class CustomObjectIdentityRetrievalStrategy implements ObjectIdentityRetrievalStrategy {
	public ObjectIdentity getObjectIdentity(Object domainObject) {
		if (Course.class.equals(domainObject.getClass())) {
			return new ObjectIdentityImpl(Course.class, ((Course) domainObject).getId());
		} else if (Quiz.class.equals(domainObject.getClass())) {
			return new ObjectIdentityImpl(Quiz.class, ((Quiz) domainObject).getId());
		} else if (Group.class.equals(domainObject.getClass())) {
			return new ObjectIdentityImpl(Quiz.class, ((Group) domainObject).getId());
		} else throw new IdExtractorNotFoundException("Can't find ID extractor for class " + domainObject.getClass());
	}
}

