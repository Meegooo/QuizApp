/*
 * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.meegoo.quizproject.server.security.acl;

import com.meegoo.quizproject.server.data.entity.Account;
import com.meegoo.quizproject.server.data.entity.Group;
import org.springframework.security.access.hierarchicalroles.NullRoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GroupSidRetrievalStrategy implements SidRetrievalStrategy {

	private RoleHierarchy roleHierarchy = new NullRoleHierarchy();

	public GroupSidRetrievalStrategy() {
	}

	public GroupSidRetrievalStrategy(RoleHierarchy roleHierarchy) {
		Assert.notNull(roleHierarchy, "RoleHierarchy must not be null");
		this.roleHierarchy = roleHierarchy;
	}

	@Override
	public List<Sid> getSids(Authentication authentication) {
		Collection<? extends GrantedAuthority> authorities = this.roleHierarchy
				.getReachableGrantedAuthorities(authentication.getAuthorities());
		List<Sid> sids = new ArrayList<>(authorities.size() + 1);
		sids.add(new PrincipalSid(authentication));
		for (GrantedAuthority authority : authorities) {
			sids.add(new GrantedAuthoritySid(authority));
		}
		if (authentication.getPrincipal() instanceof Account) {

			for (Group group : ((Account) authentication.getPrincipal()).getGroups()) {
				sids.add(new GroupSid(group.getName()));
			}
		}
		return sids;
	}

}
