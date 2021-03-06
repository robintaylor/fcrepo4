/**
 * Copyright 2014 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.impl.services.functions;

import com.google.common.base.Function;

import javax.jcr.Repository;

/**
 * Get the default workspace from the repository configuration
 * (or "default", if no information is available)
 *
 * @author cbeer
 */
public class GetDefaultWorkspace implements Function<Repository, String> {

    public static final String DEFAULT_WORKSPACE_NAME = "default";

    @Override
    public String apply(final Repository repository) {

        if (repository == null) {
            return DEFAULT_WORKSPACE_NAME;
        } else if (repository instanceof org.modeshape.jcr.JcrRepository) {
            return ((org.modeshape.jcr.JcrRepository)repository).getConfiguration().getDefaultWorkspaceName();
        } else {
            return DEFAULT_WORKSPACE_NAME;
        }
    }
}
