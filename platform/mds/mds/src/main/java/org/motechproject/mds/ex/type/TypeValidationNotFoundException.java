package org.motechproject.mds.ex.type;

import org.motechproject.mds.ex.MdsException;

/**
 * The <code>TypeValidationNotFoundException</code> exception signals a situation in which a type validation
 * for given type does not exists in database.
 */
public class TypeValidationNotFoundException extends MdsException {

    private static final long serialVersionUID = -122606862837357703L;

    public TypeValidationNotFoundException(String message) {
        super(message);
    }
}
