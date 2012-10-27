package org.motechproject.security.repository;

import org.motechproject.security.domain.MotechRole;
import org.motechproject.security.domain.MotechRoleCouchdbImpl;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: lukasz
 * Date: 23.10.12
 * Time: 12:56
 * To change this template use File | Settings | File Templates.
 */
public interface AllMotechRoles {

    List<MotechRole> getRoles();

    void add(MotechRole role);

    MotechRole findByRoleName(String roleName);
}
