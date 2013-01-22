package org.motechproject.commons.api;

import java.util.Map;

public interface DataProviderLookup {

    String getName();

    String toJSON();

    Object lookup(String type, Map<String, String> lookupFields);

    boolean supports(String type);

}
